package org.dromara.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.Charset;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Comparator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Maven模块重构工具
 * 基于Maven项目结构自动分析和重构，输出到指定目录
 * <p>
 * 使用说明：
 * 1. 修改 refactor-config.properties 配置文件
 * 2. 运行 refactorMavenModules() 方法执行重构
 *
 * @author whj
 */
@DisplayName("Maven模块重构工具")
public class MavenModuleRefactorTest {

    // 配置文件路径
    private static final String CONFIG_FILE = "refactor-config.properties";

    // 配置属性
    private Properties config;

    // 项目内部模块信息缓存
    private final Set<String> internalPackages = new HashSet<>();
    private final Set<String> internalArtifacts = new HashSet<>();
    private final Map<String, String> artifactToPackageMap = new HashMap<>();
    
    // 排除的依赖包缓存
    private final Set<String> excludePackages = new HashSet<>();

    // 跨平台配置缓存
    private String pathSeparator;
    private String lineSeparator;
    private String fileEncoding;

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        config = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("无法找到配置文件: " + CONFIG_FILE);
            }
            config.load(input);
            System.out.println("配置文件加载成功: " + CONFIG_FILE);

            // 初始化跨平台配置
            initializeCrossPlatformConfig();
            
            // 加载排除依赖配置
            loadExcludePackages();

        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取配置值
     */
    private String getConfig(String key) {
        return config.getProperty(key);
    }

    /**
     * 获取布尔配置值
     */
    private boolean getBooleanConfig(String key) {
        return Boolean.parseBoolean(config.getProperty(key, "false"));
    }

    /**
     * 初始化跨平台配置
     */
    private void initializeCrossPlatformConfig() {
        // 路径分隔符配置
        String pathSepStrategy = getConfig("path.separator.strategy");
        if ("auto".equals(pathSepStrategy) || pathSepStrategy == null) {
            this.pathSeparator = File.separator;
        } else if ("unix".equals(pathSepStrategy)) {
            this.pathSeparator = "/";
        } else if ("windows".equals(pathSepStrategy)) {
            this.pathSeparator = "\\";
        } else {
            this.pathSeparator = File.separator;
        }

        // 文件编码配置
        this.fileEncoding = getConfig("file.encoding");
        if (this.fileEncoding == null || this.fileEncoding.trim().isEmpty()) {
            this.fileEncoding = "UTF-8";
        }

        // 换行符配置
        String lineSepStrategy = getConfig("line.separator.strategy");
        if ("auto".equals(lineSepStrategy) || lineSepStrategy == null) {
            this.lineSeparator = System.lineSeparator();
        } else if ("unix".equals(lineSepStrategy)) {
            this.lineSeparator = "\n";
        } else if ("windows".equals(lineSepStrategy)) {
            this.lineSeparator = "\r\n";
        } else {
            this.lineSeparator = System.lineSeparator();
        }

        System.out.println("跨平台配置初始化完成:");
        System.out.println("  路径分隔符: " + this.pathSeparator);
        System.out.println("  文件编码: " + this.fileEncoding);
        System.out.println("  换行符: " + (this.lineSeparator.equals("\n") ? "LF" : "CRLF"));
    }
    
    /**
     * 加载排除依赖配置
     */
    private void loadExcludePackages() {
        excludePackages.clear();
        
        // 加载排除的依赖包配置
        String excludeConfig = getConfig("exclude.packages");
        if (excludeConfig != null && !excludeConfig.trim().isEmpty()) {
            String[] packages = excludeConfig.split(",");
            for (String pkg : packages) {
                String trimmedPkg = pkg.trim();
                if (!trimmedPkg.isEmpty()) {
                    excludePackages.add(trimmedPkg);
                    System.out.println("  排除依赖: " + trimmedPkg);
                }
            }
        }
        
        System.out.println("排除依赖配置加载完成，共 " + excludePackages.size() + " 个排除规则");
    }

    @Test
    @DisplayName("Maven模块重构")
    public void refactorMavenModules() {
        try {
            // 加载配置
            loadConfig();

            System.out.println("开始Maven模块重构...");

            // 验证配置
            validateConfig();

            // 分析项目依赖关系
            analyzeProjectDependencies();

            // 执行重构renamePackageDirectories
            executeRefactoring();

            System.out.println("\n=== 重构完成！===");
            System.out.println("重构后的项目位于: " + getConfig("output.project.root"));
            System.out.println("请在新目录中验证项目是否正常工作");

        } catch (Exception e) {
            System.err.println("重构过程中出现错误: " + e.getMessage());
            e.printStackTrace();
        }
    }



    /**
     * 验证配置
     */
    private void validateConfig() {
        String sourceRoot = getConfig("source.project.root");
        String outputRoot = getConfig("output.project.root");
        String oldGroupId = getConfig("old.groupId");
        String newGroupId = getConfig("new.groupId");

        if (sourceRoot == null || sourceRoot.trim().isEmpty()) {
            throw new RuntimeException("source.project.root 配置不能为空");
        }

        if (outputRoot == null || outputRoot.trim().isEmpty()) {
            throw new RuntimeException("output.project.root 配置不能为空");
        }

        if (!Files.exists(Paths.get(sourceRoot))) {
            throw new RuntimeException("源项目目录不存在: " + sourceRoot);
        }

        if (oldGroupId == null || oldGroupId.trim().isEmpty()) {
            throw new RuntimeException("old.groupId 配置不能为空");
        }

        if (newGroupId == null || newGroupId.trim().isEmpty()) {
            throw new RuntimeException("new.groupId 配置不能为空");
        }

        if (oldGroupId.equals(newGroupId)) {
            throw new RuntimeException("新旧GroupId不能相同");
        }

        System.out.println("配置验证通过");
    }

    /**
     * 分析项目依赖关系
     */
    private void analyzeProjectDependencies() {
        try {
            System.out.println("\n=== 分析项目依赖关系 ===");

            // 清空缓存
            internalPackages.clear();
            internalArtifacts.clear();
            artifactToPackageMap.clear();

            // 收集所有pom.xml文件
            List<Path> pomFiles = collectPomFiles();
            System.out.println("找到 " + pomFiles.size() + " 个pom.xml文件");

            // 分析每个pom文件
            for (Path pomFile : pomFiles) {
                analyzePomFile(pomFile);
            }

            // 如果没有分析出内部包，进行额外的包扫描
            if (internalPackages.isEmpty()) {
                System.out.println("未从POM文件分析出内部包，开始扫描Java源码...");
                scanJavaSourcesForInternalPackages();
            }

            System.out.println("分析完成:");
            System.out.println("- 内部模块: " + internalArtifacts.size() + " 个");
            System.out.println("- 内部包前缀: " + internalPackages.size() + " 个");
            if (!internalPackages.isEmpty()) {
                System.out.println("- 内部包列表: " + internalPackages);
            }

        } catch (Exception e) {
            System.err.println("依赖分析失败: " + e.getMessage());
            throw new RuntimeException("依赖分析失败", e);
        }
    }

    /**
     * 执行重构
     */
    private void executeRefactoring() throws IOException {
        String sourceRoot = getConfig("source.project.root");
        String outputRoot = getConfig("output.project.root");

        Path sourcePath = Paths.get(sourceRoot);
        Path outputPath = Paths.get(outputRoot);

        // 创建输出目录
        if (Files.exists(outputPath)) {
            System.out.println("输出目录已存在，将清空: " + outputPath);
            deleteDirectory(outputPath);
        }
        Files.createDirectories(outputPath);

        // 复制项目到输出目录
        System.out.println("\n=== 复制项目到输出目录 ===");
        copyProject(sourcePath, outputPath);

        // 修改输出目录中的文件
        System.out.println("\n=== 修改项目文件 ===");
        modifyProjectInOutputDirectory(outputPath);
    }

    /**
     * 复制项目到输出目录
     */
    private void copyProject(Path source, Path target) throws IOException {
        Set<String> excludeDirs = getExcludeDirectories();
        Set<String> excludeFiles = getExcludeFiles();

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // 添加空值检查，防止根目录导致的空指针异常
                Path fileName = dir.getFileName();
                if (fileName != null) {
                    String dirName = fileName.toString();
                    if (excludeDirs.contains(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }

                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // 添加空值检查，防止特殊文件导致的空指针异常
                Path fileName = file.getFileName();
                if (fileName == null) {
                    return FileVisitResult.CONTINUE;
                }

                String fileNameStr = fileName.toString();

                // 检查是否为排除文件
                if (shouldExcludeFile(fileNameStr, excludeFiles)) {
                    System.out.println("跳过排除文件: " + fileNameStr);
                    return FileVisitResult.CONTINUE;
                }

                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("项目复制完成");
    }

    /**
     * 修改输出目录中的项目文件
     */
    private void modifyProjectInOutputDirectory(Path projectRoot) throws IOException {
        // 1. 修改所有pom.xml文件
        List<Path> pomFiles = collectPomFilesInDirectory(projectRoot);
        System.out.println("修改 " + pomFiles.size() + " 个pom.xml文件");
        modifyPomFiles(pomFiles);

        // 2. 修改聚合POM文件中的modules引用
        System.out.println("修改聚合POM文件中的modules引用...");
        modifyAggregatorPomModules(pomFiles);

        // 3. 修改所有Java源文件
        List<Path> javaFiles = collectJavaFilesInDirectory(projectRoot);
        System.out.println("修改 " + javaFiles.size() + " 个Java文件");
        modifyJavaFiles(javaFiles);

        // 4. 修改配置文件
        List<Path> configFiles = collectConfigFilesInDirectory(projectRoot);
        System.out.println("修改Spring Boot配置文件...");
        modifyConfigFiles(configFiles);

        // 5. 重命名目录结构
        System.out.println("重命名目录结构");
        renameDirectoryStructure(projectRoot);

        // 6. 清理旧的包目录结构
        System.out.println("清理旧的包目录结构...");
        cleanupOldPackageDirectories(projectRoot);

        // 7. 多轮清理空目录
        if (getBooleanConfig("cleanup.empty.directories")) {
            System.out.println("开始多轮清理空目录...");

            // 进行多轮清理，确保彻底清除空目录
            for (int round = 1; round <= 3; round++) {
                System.out.println("第 " + round + " 轮清理空目录...");
                int deletedCount = cleanupEmptyDirectories(projectRoot);

                if (deletedCount == 0) {
                    System.out.println("第 " + round + " 轮未发现空目录，清理完成");
                    break;
                }
            }

            // 额外清理可能遗留的空包目录结构
            System.out.println("清理遗留的空包目录结构...");
            cleanupEmptyPackageStructure(projectRoot);
        }
    }

    /**
     * 收集pom.xml文件
     */
    private List<Path> collectPomFiles() throws IOException {
        return collectPomFilesInDirectory(Paths.get(getConfig("source.project.root")));
    }

    /**
     * 在指定目录收集pom.xml文件
     */
    private List<Path> collectPomFilesInDirectory(Path rootPath) throws IOException {
        List<Path> pomFiles = new ArrayList<>();
        Set<String> excludeDirs = getExcludeDirectories();

        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // 添加空值检查，防止根目录导致的空指针异常
                Path fileName = dir.getFileName();
                if (fileName != null) {
                    String dirName = fileName.toString();
                    if (excludeDirs.contains(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // 添加空值检查，防止特殊文件导致的空指针异常
                Path fileName = file.getFileName();
                if (fileName != null && fileName.toString().equals("pom.xml")) {
                    pomFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return pomFiles;
    }

    /**
     * 在指定目录收集Java文件
     */
    private List<Path> collectJavaFilesInDirectory(Path rootPath) throws IOException {
        List<Path> javaFiles = new ArrayList<>();
        Set<String> excludeDirs = getExcludeDirectories();
        Set<String> excludeFiles = getExcludeFiles();

        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // 添加空值检查，防止根目录导致的空指针异常
                Path fileName = dir.getFileName();
                if (fileName != null) {
                    String dirName = fileName.toString();
                    if (excludeDirs.contains(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // 添加空值检查，防止特殊文件导致的空指针异常
                Path fileName = file.getFileName();
                if (fileName != null) {
                    String fileNameStr = fileName.toString();
                    
                    // 检查文件是否在排除列表中
                    if (shouldExcludeFile(fileNameStr, excludeFiles)) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    if (fileNameStr.endsWith(".java")) {
                        javaFiles.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return javaFiles;
    }

    /**
     * 收集配置文件
     */
    private List<Path> collectConfigFilesInDirectory(Path directory) throws IOException {
        List<Path> configFiles = new ArrayList<>();
        Set<String> excludeDirs = getExcludeDirectories();
        Set<String> excludeFiles = getExcludeFiles();

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // 添加空值检查，防止根目录导致的空指针异常
                Path fileName = dir.getFileName();
                if (fileName != null) {
                    String dirName = fileName.toString();
                    if (excludeDirs.contains(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // 添加空值检查，防止特殊文件导致的空指针异常
                Path fileName = file.getFileName();
                if (fileName == null) {
                    return FileVisitResult.CONTINUE;
                }

                String fileNameStr = fileName.toString();
                String filePath = file.toString();
                
                // 检查文件是否在排除列表中
                if (shouldExcludeFile(fileNameStr, excludeFiles)) {
                    return FileVisitResult.CONTINUE;
                }

                // 收集Spring Boot自动配置文件和其他需要处理包名的文件
                if (fileNameStr.equals("org.springframework.boot.autoconfigure.AutoConfiguration.imports") ||
                    fileNameStr.equals("spring.factories") ||
                    fileNameStr.equals("application.yml") ||
                    fileNameStr.equals("application.yaml") ||
                    fileNameStr.equals("application.properties") ||
                    fileNameStr.equals("bootstrap.yml") ||
                    fileNameStr.equals("bootstrap.yaml") ||
                    fileNameStr.equals("bootstrap.properties") ||
                    fileNameStr.endsWith(".vm") ||  // 添加Velocity模板文件
                    fileNameStr.endsWith(".json") || // 添加JSON文件
                    fileNameStr.endsWith(".xml") ||  // 添加XML文件（除了pom.xml）
                    fileNameStr.endsWith(".ftl") ||  // 添加FreeMarker模板文件
                    fileNameStr.endsWith(".sql") ||  // 添加SQL文件
                    filePath.contains("META-INF") && (fileNameStr.endsWith(".imports") || fileNameStr.endsWith(".factories"))) {
                    // 排除pom.xml文件，因为它们有专门的处理方法
                    if (!fileNameStr.equals("pom.xml")) {
                        configFiles.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return configFiles;
    }

    /**
     * 分析单个pom文件
     */
    private void analyzePomFile(Path pomFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile.toFile());
            doc.getDocumentElement().normalize();

            // 获取当前模块的groupId和artifactId
            String groupId = getElementText(doc, "groupId");
            String artifactId = getElementText(doc, "artifactId");

            // 如果当前模块没有groupId，尝试从parent获取
            if (groupId == null || groupId.trim().isEmpty()) {
                Element parent = getFirstElement(doc, "parent");
                if (parent != null) {
                    groupId = getElementText(parent, "groupId");
                }
            }

            if (groupId != null && artifactId != null) {
                // 检查是否是项目内部模块
                String oldGroupId = getConfig("old.groupId");
                String oldArtifactPrefix = getConfig("old.artifactPrefix");

                if (groupId.equals(oldGroupId) ||
                    (oldArtifactPrefix != null && artifactId.startsWith(oldArtifactPrefix))) {

                    internalArtifacts.add(artifactId);

                    // 推断包名
                    String packageName = inferPackageName(pomFile, groupId, artifactId);
                    if (packageName != null) {
                        internalPackages.add(packageName);
                        artifactToPackageMap.put(artifactId, packageName);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("解析pom文件失败: " + pomFile + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 推断模块的包名
     */
    private String inferPackageName(Path pomFile, String groupId, String artifactId) {
        try {
            // 查找src/main/java目录
            Path moduleDir = pomFile.getParent();
            Path srcMainJava = moduleDir.resolve("src" + getPathSeparator() + "main" + getPathSeparator() + "java");

            Set<String> foundPackages = new HashSet<>();

            if (Files.exists(srcMainJava)) {
                // 查找所有Java文件并分析其包声明
                Files.walk(srcMainJava)
                    .filter(path -> path.toString().endsWith(".java"))
                    .limit(10) // 限制扫描文件数量，提高性能
                    .forEach(javaFile -> {
                        String packageName = extractPackageFromJavaFile(javaFile);
                        if (packageName != null && packageName.startsWith(groupId)) {
                            foundPackages.add(packageName);
                        }
                    });

                // 如果找到了包，返回最短的包名（通常是根包）
                if (!foundPackages.isEmpty()) {
                    return foundPackages.stream()
                        .min(Comparator.comparing(String::length))
                        .orElse(groupId);
                }
            }

            // 如果找不到Java文件，使用groupId作为包名
            return groupId;

        } catch (Exception e) {
            System.err.println("推断包名失败: " + pomFile + ", 错误: " + e.getMessage());
            return groupId;
        }
    }

    /**
     * 扫描Java源码以识别内部包
     * 当POM文件分析无法识别内部包时使用此方法
     */
    private void scanJavaSourcesForInternalPackages() {
        try {
            Path sourceRoot = Paths.get(getConfig("source.project.root"));
            String oldPackagePrefix = getConfig("old.packagePrefix");
            Set<String> foundPackages = new HashSet<>();

            // 查找所有Java源码目录
            Files.walkFileTree(sourceRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // 添加空值检查，防止根目录导致的空指针异常
                    Path fileName = dir.getFileName();
                    if (fileName != null) {
                        String dirName = fileName.toString();
                        Set<String> excludeDirs = getExcludeDirectories();
                        if (excludeDirs.contains(dirName)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        String packageName = extractPackageFromJavaFile(file);
                        if (packageName != null && packageName.startsWith(oldPackagePrefix)) {
                            // 提取包的根前缀
                            String[] parts = packageName.split("\\.");
                            if (parts.length >= 3) {
                                // 通常取前3段作为基础包名，如 com.ruoyi.xxx
                                String basePackage = String.join(".", Arrays.copyOf(parts, Math.min(3, parts.length)));
                                foundPackages.add(basePackage);
                            } else {
                                foundPackages.add(packageName);
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            // 将找到的包添加到内部包集合中
            internalPackages.addAll(foundPackages);

            System.out.println("从Java源码扫描到 " + foundPackages.size() + " 个内部包前缀: " + foundPackages);

        } catch (Exception e) {
            System.err.println("扫描Java源码失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从Java文件中提取包名
     */
    private String extractPackageFromJavaFile(Path javaFile) {
        try {
            String content = Files.readString(javaFile, Charset.forName(getFileEncoding()));
            Pattern pattern = Pattern.compile("package\\s+([a-zA-Z0-9_.]+)\\s*;");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    /**
     * 修改pom文件
     */
    private void modifyPomFiles(List<Path> pomFiles) {
        String oldGroupId = getConfig("old.groupId");
        String newGroupId = getConfig("new.groupId");
        String oldArtifactPrefix = getConfig("old.artifactPrefix");
        String newArtifactPrefix = getConfig("new.artifactPrefix");

        int modifiedCount = 0;

        for (Path pomFile : pomFiles) {
            try {
                String content = Files.readString(pomFile, Charset.forName(getFileEncoding()));
                String originalContent = content;
                String modifiedContent = content;

                // 修改groupId
                modifiedContent = modifiedContent.replaceAll(
                    "<groupId>" + Pattern.quote(oldGroupId) + "</groupId>",
                    "<groupId>" + newGroupId + "</groupId>"
                );

                // 修改artifactId中的前缀
                if (oldArtifactPrefix != null && newArtifactPrefix != null) {
                    Pattern artifactPattern = Pattern.compile("<artifactId>(" + Pattern.quote(oldArtifactPrefix) + "[^<]*)</artifactId>");
                    Matcher artifactMatcher = artifactPattern.matcher(modifiedContent);
                    StringBuffer sb = new StringBuffer();

                    while (artifactMatcher.find()) {
                        String oldArtifactId = artifactMatcher.group(1);
                        String newArtifactId = oldArtifactId.replace(oldArtifactPrefix, newArtifactPrefix);
                        artifactMatcher.appendReplacement(sb, "<artifactId>" + newArtifactId + "</artifactId>");
                    }
                    artifactMatcher.appendTail(sb);
                    modifiedContent = sb.toString();

                    // 修改依赖中的artifactId前缀
                    modifiedContent = modifiedContent.replaceAll(
                        "<artifactId>" + Pattern.quote(oldArtifactPrefix),
                        "<artifactId>" + newArtifactPrefix
                    );
                }

                // 只有内容发生变化时才写回文件
                if (!originalContent.equals(modifiedContent)) {
                    Files.writeString(pomFile, modifiedContent, Charset.forName(getFileEncoding()));
                    modifiedCount++;
                }

            } catch (IOException e) {
                System.err.println("修改pom文件失败: " + pomFile + ", 错误: " + e.getMessage());
            }
        }

        System.out.println("POM文件修改完成，共修改了 " + modifiedCount + " 个文件");
    }

    /**
     * 修改Java文件
     */
    private void modifyJavaFiles(List<Path> javaFiles) {
        String oldPackagePrefix = getConfig("old.packagePrefix");
        String newPackagePrefix = getConfig("new.packagePrefix");

        int modifiedCount = 0;

        for (Path javaFile : javaFiles) {
            try {
                String content = Files.readString(javaFile, Charset.forName(getFileEncoding()));
                String originalContent = content;
                String modifiedContent = modifyJavaFileIntelligently(content, oldPackagePrefix, newPackagePrefix);

                // 如果内容有变化，写回文件
                if (!originalContent.equals(modifiedContent)) {
                    Files.writeString(javaFile, modifiedContent, Charset.forName(getFileEncoding()));
                    modifiedCount++;
                }

            } catch (IOException e) {
                System.err.println("修改Java文件失败: " + javaFile + ", 错误: " + e.getMessage());
            }
        }

        System.out.println("Java文件修改完成，共修改了 " + modifiedCount + " 个文件");
    }

    /**
     * 修改配置文件
     */
    private void modifyConfigFiles(List<Path> configFiles) {
        String oldPackagePrefix = getConfig("old.packagePrefix");
        String newPackagePrefix = getConfig("new.packagePrefix");
        String oldGroupId = getConfig("old.groupId");
        String newGroupId = getConfig("new.groupId");
        String oldArtifactPrefix = getConfig("old.artifactPrefix");
        String newArtifactPrefix = getConfig("new.artifactPrefix");

        int modifiedCount = 0;

        for (Path configFile : configFiles) {
            try {
                String content = Files.readString(configFile, Charset.forName(getFileEncoding()));
                String originalContent = content;
                String modifiedContent = content;

                // 修改包名引用
                if (oldPackagePrefix != null && newPackagePrefix != null) {
                    // 修改包名，只修改项目内部包
                    modifiedContent = modifyConfigPackageReferences(modifiedContent, oldPackagePrefix, newPackagePrefix);
                }

                // 修改groupId引用
                if (oldGroupId != null && newGroupId != null) {
                    modifiedContent = modifiedContent.replace(oldGroupId, newGroupId);
                }

                // 修改artifactId引用
                if (oldArtifactPrefix != null && newArtifactPrefix != null) {
                    modifiedContent = modifiedContent.replaceAll(oldArtifactPrefix + "([a-zA-Z0-9-]*)", newArtifactPrefix + "$1");
                }

                // 如果内容有变化，写回文件
                if (!originalContent.equals(modifiedContent)) {
                    Files.writeString(configFile, modifiedContent, Charset.forName(getFileEncoding()));
                    modifiedCount++;
                    System.out.println("  修改配置文件: " + configFile.getFileName());
                }

            } catch (IOException e) {
                System.err.println("修改配置文件失败: " + configFile + ", 错误: " + e.getMessage());
            }
        }

        System.out.println("配置文件修改完成，共修改了 " + modifiedCount + " 个文件");
    }

    /**
     * 修改配置文件中的包名引用
     */
    private String modifyConfigPackageReferences(String content, String oldPackagePrefix, String newPackagePrefix) {
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            String modifiedLine = line;

            // 处理Spring Boot自动配置文件中的类名
            if (trimmedLine.startsWith(oldPackagePrefix) && isInternalPackage(trimmedLine)) {
                modifiedLine = applyGlobalPackageReplacementToLine(line, oldPackagePrefix, newPackagePrefix);
            }
            // 处理包含包名的行
            else if (trimmedLine.contains(oldPackagePrefix)) {
                // 对于配置文件，检查是否是包名配置
                if (isPackageConfiguration(trimmedLine, oldPackagePrefix)) {
                    modifiedLine = applyGlobalPackageReplacementToLine(line, oldPackagePrefix, newPackagePrefix);
                }
                // 对于其他文件类型（如.vm、.json、.xml等），直接应用包名映射
                else {
                    modifiedLine = applyPackageMappingToLine(line, oldPackagePrefix, newPackagePrefix);
                }
            }

            result.append(modifiedLine);
            if (!modifiedLine.equals(lines[lines.length - 1])) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * 判断是否是包名配置
     */
    /**
     * 对单行内容应用包名映射
     */
    private String applyPackageMappingToLine(String line, String oldPackagePrefix, String newPackagePrefix) {
        String modifiedLine = line;
        
        // 使用正则表达式匹配所有包名
        Pattern packagePattern = Pattern.compile("\\b" + Pattern.quote(oldPackagePrefix) + "(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*\\b");
        Matcher matcher = packagePattern.matcher(modifiedLine);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String foundPackage = matcher.group();
            boolean shouldExclude = false;
            
            // 检查是否应该排除这个包名
            for (String excludePackage : excludePackages) {
                if (foundPackage.equals(excludePackage) || foundPackage.startsWith(excludePackage + ".")) {
                    shouldExclude = true;
                    break;
                }
            }
            
            if (shouldExclude) {
                // 保持原样，不替换
                matcher.appendReplacement(sb, Matcher.quoteReplacement(foundPackage));
            } else if (isInternalPackage(foundPackage)) {
                // 应用通用前缀替换
                String newPackage = foundPackage.replace(oldPackagePrefix, newPackagePrefix);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(newPackage));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(foundPackage));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    private boolean isPackageConfiguration(String line, String packagePrefix) {
        String trimmed = line.trim();

        // 跳过注释行（支持多种注释格式）
        if (trimmed.startsWith("#") || trimmed.startsWith("//") || 
            trimmed.startsWith("/*") || trimmed.startsWith("<!--") ||
            trimmed.startsWith("##")) {
            return false;
        }

        // 对于YAML/Properties等配置文件，需要更严格的检查
        // 对于模板文件(.vm, .ftl)和其他文件(.json, .xml, .sql)，采用更宽松的检查
        boolean isConfigFile = line.contains(":") || line.contains("=");
        
        if (isConfigFile) {
            // 配置文件：检查是否包含完整的包名（以点分隔）
            Pattern packagePattern = Pattern.compile("\\b" + Pattern.quote(packagePrefix) + "\\.[a-zA-Z0-9_.]+\\b");
            Matcher matcher = packagePattern.matcher(line);

            if (matcher.find()) {
                String foundPackage = matcher.group();
                return isInternalPackage(foundPackage);
            }
        } else {
            // 模板文件和其他文件：只要包含包名就认为需要处理
            return line.contains(packagePrefix + ".");
        }

        return false;
    }

    /**
     * 修改Java文件内容
     */
    private String modifyJavaFileIntelligently(String content, String oldPackagePrefix, String newPackagePrefix) {
        String modifiedContent = content;

        // 1. 修改package声明
        Pattern packagePattern = Pattern.compile("package\\s+(" + Pattern.quote(oldPackagePrefix) + "[^;]*);\\s*");
        Matcher packageMatcher = packagePattern.matcher(modifiedContent);
        if (packageMatcher.find()) {
            String oldPackage = packageMatcher.group(1);
            String newPackage = applyPackageMapping(oldPackage, oldPackagePrefix, newPackagePrefix);
            modifiedContent = packageMatcher.replaceFirst("package " + newPackage + ";\n");
        }

        // 2. 修改import语句
        modifiedContent = modifyImportsIntelligently(modifiedContent, oldPackagePrefix, newPackagePrefix);
        
        // 3. 全局替换文件内容中的包名引用（排除已处理的package和import语句）
        modifiedContent = applyGlobalPackageReplacement(modifiedContent, oldPackagePrefix, newPackagePrefix);

        return modifiedContent;
    }

    /**
     * 修改import语句
     */
    /**
     * 智能修改import语句
     * 优先使用精确的包名映射，然后使用通用前缀替换
     */
    private String modifyImportsIntelligently(String content, String oldPackagePrefix, String newPackagePrefix) {
        Pattern importPattern = Pattern.compile("import\\s+(static\\s+)?([^;]+);\\s*");
        Matcher importMatcher = importPattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (importMatcher.find()) {
            String staticKeyword = importMatcher.group(1); // static关键字（可能为null）
            String importStatement = importMatcher.group(2); // 实际的包名和类名
            
            // 只修改项目内部包的import
            if (isInternalPackage(importStatement)) {
                String newImportStatement = applyPackageMapping(importStatement, oldPackagePrefix, newPackagePrefix);
                String staticPart = (staticKeyword != null) ? staticKeyword : "";
                importMatcher.appendReplacement(sb, "import " + staticPart + newImportStatement + ";\n");
            } else {
                importMatcher.appendReplacement(sb, importMatcher.group(0));
            }
        }
        importMatcher.appendTail(sb);

        return sb.toString();
    }
    
    /**
     * 对单行内容应用全局包名替换
     * 排除配置中的排除依赖包
     */
    private String applyGlobalPackageReplacementToLine(String line, String oldPackagePrefix, String newPackagePrefix) {
        String modifiedLine = line;
        
        // 使用正则表达式匹配所有包名
        Pattern packagePattern = Pattern.compile("\\b" + Pattern.quote(oldPackagePrefix) + "(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*\\b");
        Matcher matcher = packagePattern.matcher(modifiedLine);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String foundPackage = matcher.group();
            boolean shouldExclude = false;
            
            // 检查是否应该排除这个包名
            for (String excludePackage : excludePackages) {
                if (foundPackage.equals(excludePackage) || foundPackage.startsWith(excludePackage + ".")) {
                    shouldExclude = true;
                    break;
                }
            }
            
            if (shouldExclude) {
                // 保持原样，不替换
                matcher.appendReplacement(sb, Matcher.quoteReplacement(foundPackage));
            } else {
                // 应用通用前缀替换
                String replacedPackage = foundPackage.replace(oldPackagePrefix, newPackagePrefix);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacedPackage));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
 
     /**
     * 应用包名映射规则
     * 使用通用前缀替换
     */
    private String applyPackageMapping(String originalPackage, String oldPackagePrefix, String newPackagePrefix) {
        // 使用通用前缀替换
        if (originalPackage.startsWith(oldPackagePrefix)) {
            return originalPackage.replace(oldPackagePrefix, newPackagePrefix);
        }
        
        // 如果不匹配，返回原包名
        return originalPackage;
    }
    
    /**
     * 全局替换文件内容中的包名引用
     * 排除配置中的排除依赖包
     */
    private String applyGlobalPackageReplacement(String content, String oldPackagePrefix, String newPackagePrefix) {
        String modifiedContent = content;
        
        // 对所有以oldPackagePrefix开头的包名进行全局替换
        // 但要排除配置中的排除依赖包
        Pattern packageRefPattern = Pattern.compile(Pattern.quote(oldPackagePrefix) + "(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*");
        Matcher matcher = packageRefPattern.matcher(modifiedContent);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String foundPackage = matcher.group();
            boolean shouldExclude = false;
            
            // 检查是否应该排除这个包名
            for (String excludePackage : excludePackages) {
                if (foundPackage.equals(excludePackage) || foundPackage.startsWith(excludePackage + ".")) {
                    shouldExclude = true;
                    break;
                }
            }
            
            if (shouldExclude) {
                // 保持原样，不替换
                matcher.appendReplacement(sb, Matcher.quoteReplacement(foundPackage));
            } else {
                // 应用通用前缀替换
                String replacedPackage = foundPackage.replace(oldPackagePrefix, newPackagePrefix);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacedPackage));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    /**
     * 判断是否为项目内部包
     */
    /**
     * 判断是否为内部包
     * 检查通用前缀匹配，但排除配置中的排除依赖包
     */
    private boolean isInternalPackage(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return false;
        }

        // 0. 首先检查是否在排除列表中，如果是则不视为内部包
        for (String excludePackage : excludePackages) {
            if (excludePackage != null && !excludePackage.trim().isEmpty()) {
                if (packageName.equals(excludePackage) || packageName.startsWith(excludePackage + ".")) {
                    return false; // 在排除列表中，不是内部包
                }
            }
        }

        // 1. 检查通用前缀匹配
        String oldPackagePrefix = getConfig("old.packagePrefix");
        if (oldPackagePrefix != null && !oldPackagePrefix.trim().isEmpty()) {
            // 确保是精确匹配或子包，避免误判外部依赖
            if (packageName.equals(oldPackagePrefix) || packageName.startsWith(oldPackagePrefix + ".")) {
                return true;
            }
        }
        
        // 2. 如果有分析出的内部包，进行检查
         if (!internalPackages.isEmpty()) {
             return internalPackages.stream().anyMatch(pkg -> {
                  if (pkg == null || pkg.trim().isEmpty()) {
                      return false;
                  }
                  // 精确匹配或者是子包
                  return packageName.equals(pkg) || packageName.startsWith(pkg + ".");
              });
          }
          
          // 3. 如果没有任何匹配，返回false
          return false;
    }

    /**
     * 重命名目录结构
     */
    private void renameDirectoryStructure(Path projectRoot) throws IOException {
        // 重命名模块目录
        renameModuleDirectories(projectRoot);

        // 重命名包目录
        renamePackageDirectories(projectRoot);
    }

    /**
     * 重命名模块目录
     */
    private void renameModuleDirectories(Path projectRoot) throws IOException {
        String oldArtifactPrefix = getConfig("old.artifactPrefix");
        String newArtifactPrefix = getConfig("new.artifactPrefix");

        if (oldArtifactPrefix == null || newArtifactPrefix == null) {
            return;
        }

        // 收集需要重命名的目录
        List<Path> dirsToRename = new ArrayList<>();

        Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // 添加空值检查，防止根目录导致的空指针异常
                Path fileName = dir.getFileName();
                if (fileName == null) {
                    return FileVisitResult.CONTINUE;
                }

                String dirName = fileName.toString();

                // 检查是否是Maven模块目录（包含pom.xml且目录名匹配前缀）
                if (dirName.startsWith(oldArtifactPrefix) && Files.exists(dir.resolve("pom.xml"))) {
                    dirsToRename.add(dir);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        // 按深度排序，先重命名深层目录
        dirsToRename.sort((a, b) -> Integer.compare(b.getNameCount(), a.getNameCount()));

        int renamedCount = 0;
        for (Path dir : dirsToRename) {
            try {
                // 添加空值检查，防止特殊路径导致的空指针异常
                Path fileName = dir.getFileName();
                if (fileName == null) {
                    continue;
                }

                String dirName = fileName.toString();
                String newDirName = dirName.replace(oldArtifactPrefix, newArtifactPrefix);
                Path newDir = dir.getParent().resolve(newDirName);

                if (!Files.exists(newDir)) {
                    Files.move(dir, newDir);
                    renamedCount++;
                    System.out.println("  重命名模块目录: " + dirName + " -> " + newDirName);
                }
            } catch (IOException e) {
                System.err.println("重命名模块目录失败: " + dir + ", 错误: " + e.getMessage());
            }
        }

        System.out.println("模块目录重命名完成，共重命名了 " + renamedCount + " 个目录");
    }

    /**
     * 重命名包目录
     */
    private void renamePackageDirectories(Path projectRoot) throws IOException {
        String oldPackagePrefix = getConfig("old.packagePrefix");
        String newPackagePrefix = getConfig("new.packagePrefix");
        String separator = getPathSeparator();
        String oldPackagePath = oldPackagePrefix.replace(".", separator);
        String newPackagePath = newPackagePrefix.replace(".", separator);

        // 收集需要重命名的包目录和文件
        Map<Path, Path> itemsToMove = new HashMap<>();
        Set<String> excludeDirs = getExcludeDirectories();
        Set<Path> processedPaths = new HashSet<>();

        Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // 添加空值检查，防止根目录导致的空指针异常
                Path fileName = dir.getFileName();
                if (fileName != null) {
                    String dirName = fileName.toString();
                    if (excludeDirs.contains(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }

                String dirPath = dir.toString();
                // 检查是否是Java源码目录或资源目录中的旧包路径
                if (isPackageDirectory(dirPath, oldPackagePath)) {
                    String newPath = dirPath.replace(oldPackagePath, newPackagePath);
                    Path newDir = Paths.get(newPath);
                    itemsToMove.put(dir, newDir);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String filePath = file.toString();
                // 检查是否是资源目录中的文件需要移动到新包路径
                if (isPackageFile(filePath, oldPackagePath)) {
                    String newPath = filePath.replace(oldPackagePath, newPackagePath);
                    Path newFile = Paths.get(newPath);
                    itemsToMove.put(file, newFile);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        // 分离文件和目录，分别处理
        Map<Path, Path> filesToMove = new HashMap<>();
        Map<Path, Path> dirsToMove = new HashMap<>();

        for (Map.Entry<Path, Path> entry : itemsToMove.entrySet()) {
            if (Files.isDirectory(entry.getKey())) {
                dirsToMove.put(entry.getKey(), entry.getValue());
            } else {
                filesToMove.put(entry.getKey(), entry.getValue());
            }
        }

        int movedCount = 0;

        // 先处理文件
        for (Map.Entry<Path, Path> entry : filesToMove.entrySet()) {
            Path source = entry.getKey();
            Path target = entry.getValue();

            try {
                if (!Files.exists(target) && Files.exists(source)) {
                    Files.createDirectories(target.getParent());
                    Files.move(source, target);
                    System.out.println("  移动包文件: " + source.getFileName() + " -> " + target.getFileName());
                    movedCount++;
                }
            } catch (IOException e) {
                System.err.println("移动包文件失败: " + source + " -> " + target + ", 错误: " + e.getMessage());
            }
        }

        // 再处理目录，按深度排序，先处理深层目录
        List<Map.Entry<Path, Path>> sortedDirs = new ArrayList<>(dirsToMove.entrySet());
        sortedDirs.sort((a, b) -> Integer.compare(b.getKey().getNameCount(), a.getKey().getNameCount()));

        for (Map.Entry<Path, Path> entry : sortedDirs) {
            Path source = entry.getKey();
            Path target = entry.getValue();

            // 检查是否已经被处理或者是已处理目录的子目录
            if (processedPaths.contains(source) || isSubdirectoryOfProcessed(source, processedPaths)) {
                System.out.println("  跳过已处理的目录: " + source);
                continue;
            }

            try {
                if (!Files.exists(target) && Files.exists(source)) {
                    Files.createDirectories(target.getParent());

                    // 强制移动整个目录结构
                    moveDirectoryCompletely(source, target);
                    System.out.println("  完整移动包目录: " + source + " -> " + target);

                    processedPaths.add(source);
                    movedCount++;
                }
            } catch (IOException e) {
                System.err.println("移动包目录失败: " + source + " -> " + target + ", 错误: " + e.getMessage());
            }
        }

        System.out.println("包目录和文件重命名完成，共移动了 " + movedCount + " 个项目");
    }

    /**
     * 移动目录内容
     */
    private void moveDirectoryContents(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = targetDir.resolve(sourceDir.relativize(file));
                Files.createDirectories(targetFile.getParent());
                Files.move(file, targetFile);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(sourceDir)) {
                    Path targetSubDir = targetDir.resolve(sourceDir.relativize(dir));
                    Files.createDirectories(targetSubDir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 移动目录内容并删除源目录
     */
    private void moveDirectoryContentsAndDelete(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // 先移动所有内容
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = targetDir.resolve(sourceDir.relativize(file));
                Files.createDirectories(targetFile.getParent());
                Files.move(file, targetFile);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(sourceDir)) {
                    Path targetSubDir = targetDir.resolve(sourceDir.relativize(dir));
                    Files.createDirectories(targetSubDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                // 删除空的子目录（但不删除根源目录，稍后统一删除）
                if (!dir.equals(sourceDir)) {
                    try {
                        Files.delete(dir);
                    } catch (IOException e) {
                        // 如果目录不为空，忽略错误，稍后再次尝试
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        // 最后删除源目录
        try {
            Files.delete(sourceDir);
        } catch (IOException e) {
            // 如果目录仍然不为空，尝试递归删除
            deleteDirectory(sourceDir);
        }
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 修改聚合POM文件中的modules引用
     */
    private void modifyAggregatorPomModules(List<Path> pomFiles) {
        String oldArtifactPrefix = getConfig("old.artifactPrefix");
        String newArtifactPrefix = getConfig("new.artifactPrefix");

        if (oldArtifactPrefix == null || newArtifactPrefix == null) {
            return;
        }

        int modifiedCount = 0;

        for (Path pomFile : pomFiles) {
            try {
                String content = Files.readString(pomFile, Charset.forName(getFileEncoding()));
                String originalContent = content;

                // 修改<modules>标签中的模块引用
                Pattern modulePattern = Pattern.compile("<module>([^<]*" + Pattern.quote(oldArtifactPrefix) + "[^<]*)</module>");
                Matcher moduleMatcher = modulePattern.matcher(content);
                StringBuffer sb = new StringBuffer();

                while (moduleMatcher.find()) {
                    String oldModuleName = moduleMatcher.group(1);
                    String newModuleName = oldModuleName.replace(oldArtifactPrefix, newArtifactPrefix);
                    moduleMatcher.appendReplacement(sb, "<module>" + newModuleName + "</module>");
                }
                moduleMatcher.appendTail(sb);
                content = sb.toString();

                // 只有内容发生变化时才写回文件
                if (!originalContent.equals(content)) {
                    Files.writeString(pomFile, content, Charset.forName(getFileEncoding()));
                    modifiedCount++;
                    System.out.println("  修改聚合POM: " + pomFile.getFileName());
                }

            } catch (IOException e) {
                System.err.println("修改聚合POM文件失败: " + pomFile + ", 错误: " + e.getMessage());
            }
        }

        System.out.println("聚合POM文件修改完成，共修改了 " + modifiedCount + " 个文件");
    }

    /**
     * 清理旧的包目录结构
     */
    private void cleanupOldPackageDirectories(Path projectRoot) throws IOException {
        String oldPackagePrefix = getConfig("old.packagePrefix");
        String separator = getPathSeparator();
        String oldPackagePath = oldPackagePrefix.replace(".", separator);

        List<Path> oldPackageDirs = new ArrayList<>();
        Set<String> excludeDirs = getExcludeDirectories();
        Set<String> protectedDirs = getProtectedDirectories();

        // 收集所有旧的包目录和其他空的包目录
        Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // 添加空值检查，防止特殊路径导致的空指针异常
                Path fileName = dir.getFileName();
                if (fileName == null) {
                    return FileVisitResult.CONTINUE;
                }

                String dirName = fileName.toString();
                if (excludeDirs.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                String dirPath = dir.toString();
                // 检查是否是空的旧包目录或任何在java源码目录下的空目录
                boolean isOldPackageDir = (dirPath.contains("src" + separator + "main" + separator + "java" + separator + oldPackagePath) ||
                    dirPath.contains("src" + separator + "test" + separator + "java" + separator + oldPackagePath));

                boolean isInJavaSource = isInJavaSourceDirectory(dir);

                if (isOldPackageDir || isInJavaSource) {
                    try {
                        // 检查目录是否为空或只包含空的子目录
                        if (isEmptyOrOnlyEmptyDirs(dir)) {
                            // 检查是否受保护
                            if (!isStrictlyProtectedDirectory(dir, protectedDirs, projectRoot)) {
                                oldPackageDirs.add(dir);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("检查目录失败: " + dir + ", 错误: " + e.getMessage());
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        });

        // 按深度排序，先删除深层目录
        oldPackageDirs.sort((a, b) -> Integer.compare(b.getNameCount(), a.getNameCount()));

        int deletedCount = 0;
        for (Path dir : oldPackageDirs) {
            try {
                if (Files.exists(dir) && isEmptyOrOnlyEmptyDirs(dir)) {
                    Path parentDir = dir.getParent();
                    deleteDirectory(dir);
                    deletedCount++;
                    System.out.println("  删除空的旧包目录: " + projectRoot.relativize(dir));

                    // 清理可能变为空的父目录
                    if (parentDir != null) {
                        cleanupEmptyParentDirectories(parentDir, projectRoot);
                    }
                }
            } catch (IOException e) {
                System.err.println("删除旧包目录失败: " + dir + ", 错误: " + e.getMessage());
            }
        }

        System.out.println("旧包目录清理完成，共删除了 " + deletedCount + " 个空目录");
    }

    /**
     * 检查目录是否在Java源码目录下
     */
    private boolean isInJavaSourceDirectory(Path dir) {
        // 检查是否在java源码目录下
        Path current = dir;
        boolean foundJava = false;
        boolean foundMainOrTest = false;
        boolean foundSrc = false;

        while (current != null) {
            // 添加空值检查，防止根目录或特殊路径导致的空指针异常
            Path fileName = current.getFileName();
            if (fileName == null) {
                current = current.getParent();
                continue;
            }

            String name = fileName.toString();
            if (name.equals("java")) {
                foundJava = true;
            } else if (name.equals("main") || name.equals("test")) {
                foundMainOrTest = true;
            } else if (name.equals("src")) {
                foundSrc = true;
                break;
            }
            current = current.getParent();
        }

        return foundJava && foundMainOrTest && foundSrc;
    }

    /**
     * 检查目录是否为空或只包含空的子目录
     */
    private boolean isEmptyOrOnlyEmptyDirs(Path dir) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return false;
        }

        try (var stream = Files.list(dir)) {
            return stream.allMatch(path -> {
                try {
                    return Files.isDirectory(path) && isEmptyOrOnlyEmptyDirs(path);
                } catch (IOException e) {
                    return false;
                }
            });
        }
    }

    /**
     * 增强的空目录清理方法，递归清理空的父目录
     */
    private void cleanupEmptyParentDirectories(Path startDir, Path projectRoot) {
        Path currentDir = startDir;
        Set<String> protectedDirs = getProtectedDirectories();

        while (currentDir != null && !currentDir.equals(projectRoot)) {
            try {
                // 检查当前目录是否为空
                if (Files.exists(currentDir) && isDirectoryEmpty(currentDir)) {
                    // 检查是否受保护
                    if (!isStrictlyProtectedDirectory(currentDir, protectedDirs, projectRoot)) {
                        Files.delete(currentDir);
                        System.out.println("  删除空的父目录: " + projectRoot.relativize(currentDir));
                        // 继续检查上级目录
                        currentDir = currentDir.getParent();
                    } else {
                        break; // 遇到受保护的目录，停止清理
                    }
                } else {
                    break; // 目录不为空，停止清理
                }
            } catch (IOException e) {
                System.out.println("  警告：无法删除父目录 " + currentDir + ": " + e.getMessage());
                break;
            }
        }
    }

    /**
     * 清理遗留的空包目录结构
     * 专门处理包重构后可能遗留的空包目录层次结构
     */
    private void cleanupEmptyPackageStructure(Path projectRoot) {
        try {
            System.out.println("开始清理遗留的空包目录结构...");

            Set<String> protectedDirs = getProtectedDirectories();
            Set<String> excludeDirs = getExcludeDirectories();
            int deletedCount = 0;

            // 查找所有java源码目录
            List<Path> javaSourceDirs = new ArrayList<>();

            Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // 添加空值检查，防止根目录导致的空指针异常
                    Path fileName = dir.getFileName();
                    if (fileName == null) {
                        return FileVisitResult.CONTINUE;
                    }

                    String dirName = fileName.toString();
                    if (excludeDirs.contains(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    // 找到java目录
                    if (dirName.equals("java") && dir.getParent() != null) {
                        Path parentFileName = dir.getParent().getFileName();
                        if (parentFileName != null) {
                            String parentName = parentFileName.toString();
                            if (parentName.equals("main") || parentName.equals("test")) {
                                javaSourceDirs.add(dir);
                            }
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            // 对每个java源码目录进行深度清理
            for (Path javaDir : javaSourceDirs) {
                System.out.println("清理java目录: " + projectRoot.relativize(javaDir));
                deletedCount += cleanupEmptyPackageDirectoriesInPath(javaDir, projectRoot, protectedDirs);
            }

            System.out.println("空包目录结构清理完成，删除了 " + deletedCount + " 个空目录");

        } catch (Exception e) {
            System.err.println("清理空包目录结构时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 在指定路径下清理空的包目录
     */
    private int cleanupEmptyPackageDirectoriesInPath(Path basePath, Path projectRoot, Set<String> protectedDirs) {
        int deletedCount = 0;

        try {
            // 收集所有目录，按深度倒序排列
            List<Path> allDirs = new ArrayList<>();

            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    if (!dir.equals(basePath)) {
                        allDirs.add(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            // 按深度倒序排列，先处理深层目录
            allDirs.sort((a, b) -> Integer.compare(b.getNameCount(), a.getNameCount()));

            // 多轮清理，直到没有更多目录可以删除
            boolean hasDeleted;
            int round = 1;

            do {
                hasDeleted = false;

                for (Path dir : allDirs) {
                    try {
                        if (Files.exists(dir) && isDirectoryEmpty(dir)) {
                            // 检查是否受保护
                            if (!isStrictlyProtectedDirectory(dir, protectedDirs, projectRoot)) {
                                Files.delete(dir);
                                System.out.println("    删除空包目录: " + projectRoot.relativize(dir));
                                deletedCount++;
                                hasDeleted = true;
                            }
                        }
                    } catch (IOException e) {
                        // 忽略删除失败的情况，可能是权限问题或目录正在使用
                    }
                }

                round++;
                // 防止无限循环
                if (round > 3) {
                    break;
                }

            } while (hasDeleted);

        } catch (IOException e) {
            System.err.println("清理路径 " + basePath + " 时出错: " + e.getMessage());
        }

        return deletedCount;
    }

    /**
     * 获取排除目录集合
     */
    private Set<String> getExcludeDirectories() {
        String excludeConfig = getConfig("exclude.directories");
        if (excludeConfig == null || excludeConfig.trim().isEmpty()) {
            Set<String> defaultExcludes = new HashSet<>();
            defaultExcludes.add("target");
            defaultExcludes.add(".git");
            defaultExcludes.add(".idea");
            defaultExcludes.add("node_modules");
            return defaultExcludes;
        }
        Set<String> excludes = new HashSet<>();
        for (String dir : excludeConfig.split(",")) {
            excludes.add(dir.trim());
        }
        return excludes;
    }

    /**
     * 获取排除文件集合
     */
    private Set<String> getExcludeFiles() {
        String excludeConfig = getConfig("exclude.files");
        if (excludeConfig == null || excludeConfig.trim().isEmpty()) {
            Set<String> defaultExcludes = new HashSet<>();
            defaultExcludes.add(".flattened-pom.xml");
            return defaultExcludes;
        }
        Set<String> excludes = new HashSet<>();
        for (String file : excludeConfig.split(",")) {
            excludes.add(file.trim());
        }
        return excludes;
    }

    /**
     * 检查文件是否应该被排除
     */
    private boolean shouldExcludeFile(String fileName, Set<String> excludeFiles) {
        for (String excludePattern : excludeFiles) {
            if (excludePattern.contains("*")) {
                // 简单的通配符匹配
                String regex = excludePattern.replace("*", ".*");
                if (fileName.matches(regex)) {
                    return true;
                }
            } else {
                // 精确匹配
                if (fileName.equals(excludePattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 显示配置信息
     */
    private void displayConfigInfo() {
        System.out.println("=== 配置信息 ===");
        System.out.println("源项目: " + getConfig("source.project.root"));
        System.out.println("输出目录: " + getConfig("output.project.root"));
        System.out.println("旧GroupId: " + getConfig("old.groupId"));
        System.out.println("新GroupId: " + getConfig("new.groupId"));
        System.out.println("旧包前缀: " + getConfig("old.packagePrefix"));
        System.out.println("新包前缀: " + getConfig("new.packagePrefix"));
        System.out.println("旧模块前缀: " + getConfig("old.artifactPrefix"));
        System.out.println("新模块前缀: " + getConfig("new.artifactPrefix"));
        System.out.println();
    }

    /**
     * 显示将要修改的文件
     */
    private void displayFilesToModify() {
        try {
            List<Path> pomFiles = collectPomFiles();
            System.out.println("=== 将要修改的文件 ===");
            System.out.println("POM文件: " + pomFiles.size() + " 个");

            if (internalArtifacts != null && !internalArtifacts.isEmpty()) {
                System.out.println("\n内部模块:");
                for (String artifact : internalArtifacts) {
                    String packageName = artifactToPackageMap.get(artifact);
                    System.out.println("  - " + artifact + (packageName != null ? " -> " + packageName : ""));
                }
            }

            System.out.println();
        } catch (Exception e) {
            System.err.println("显示文件信息失败: " + e.getMessage());
        }
    }

    // XML辅助方法
    private String getElementText(Document doc, String tagName) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return null;
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return null;
    }

    private Element getFirstElement(Document doc, String tagName) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return (Element) nodeList.item(0);
        }
        return null;
    }

    /**
     * 精确匹配包路径，确保不会误匹配子路径
     */
    private boolean isExactPackagePathMatch(String fullPath, String packagePath) {
        if (!fullPath.contains(packagePath)) {
            return false;
        }

        int index = fullPath.indexOf(packagePath);
        if (index == -1) {
            return false;
        }

        // 检查包路径后面是否是路径分隔符或文件结尾
        int endIndex = index + packagePath.length();
        if (endIndex == fullPath.length()) {
            return true; // 路径完全匹配
        }

        char nextChar = fullPath.charAt(endIndex);
        return nextChar == getPathSeparator().charAt(0); // 后面必须是路径分隔符
    }

    /**
     * 检查路径是否是已处理路径的子目录
     */
    private boolean isSubdirectoryOfProcessed(Path path, Set<Path> processedPaths) {
        for (Path processedPath : processedPaths) {
            if (path.startsWith(processedPath) && !path.equals(processedPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查目录是否为空
     */
    private boolean isDirectoryEmpty(Path directory) {
        try (var stream = Files.list(directory)) {
            return !stream.findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 检查是否是包目录
     */
    private boolean isPackageDirectory(String dirPath, String packagePath) {
        // 使用配置的路径分隔符进行标准化
        String separator = getPathSeparator();
        String normalizedDirPath = dirPath.replace('/', separator.charAt(0)).replace('\\', separator.charAt(0));
        String normalizedPackagePath = packagePath.replace('/', separator.charAt(0)).replace('\\', separator.charAt(0));

        // 检查是否在标准的源码或资源目录中
        String[] standardPaths = {
            "src" + separator + "main" + separator + "java" + separator + normalizedPackagePath,
            "src" + separator + "test" + separator + "java" + separator + normalizedPackagePath,
            "src" + separator + "main" + separator + "resources" + separator + normalizedPackagePath,
            "src" + separator + "test" + separator + "resources" + separator + normalizedPackagePath
        };

        for (String standardPath : standardPaths) {
            if (isExactPackagePathMatch(normalizedDirPath, standardPath)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查是否是包文件
     */
    private boolean isPackageFile(String filePath, String packagePath) {
        // 使用配置的路径分隔符进行标准化
        String separator = getPathSeparator();
        String normalizedFilePath = filePath.replace('/', separator.charAt(0)).replace('\\', separator.charAt(0));
        String normalizedPackagePath = packagePath.replace('/', separator.charAt(0)).replace('\\', separator.charAt(0));

        // 检查是否在标准的源码或资源目录中
        String[] standardPaths = {
            "src" + separator + "main" + separator + "java" + separator + normalizedPackagePath,
            "src" + separator + "test" + separator + "java" + separator + normalizedPackagePath,
            "src" + separator + "main" + separator + "resources" + separator + normalizedPackagePath,
            "src" + separator + "test" + separator + "resources" + separator + normalizedPackagePath
        };

        for (String standardPath : standardPaths) {
            if (normalizedFilePath.contains(standardPath)) {
                // 确保文件在包路径内
                int index = normalizedFilePath.indexOf(standardPath);
                if (index >= 0) {
                    int endIndex = index + standardPath.length();
                    if (endIndex < normalizedFilePath.length()) {
                        char nextChar = normalizedFilePath.charAt(endIndex);
                        if (nextChar == getPathSeparator().charAt(0)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 完整移动目录，确保所有内容都被移动
     */
    private void moveDirectoryCompletely(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }

        // 创建目标目录
        Files.createDirectories(target);

        // 使用递归方式移动所有内容
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.createDirectories(targetFile.getParent());
                Files.move(file, targetFile);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                // 删除空目录（从最深层开始）
                if (!dir.equals(source)) {
                    try {
                        Files.delete(dir);
                    } catch (IOException e) {
                        // 如果目录不为空，可能还有其他文件，忽略错误
                        System.out.println("    警告：无法删除目录 " + dir + "，可能不为空");
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        // 最后删除源目录
        try {
            Files.delete(source);
        } catch (IOException e) {
            // 如果源目录仍然存在内容，强制删除
            System.out.println("    尝试强制删除源目录: " + source);
            deleteDirectoryRecursively(source);
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 清理空目录
     * 删除重构后遗留的空目录，但保护重要的目录结构
     * @return 删除的空目录数量
     */
    private int cleanupEmptyDirectories(Path projectRoot) {
        try {
            int maxDepth = Integer.parseInt(getConfig("cleanup.max.depth"));
            Set<String> protectedDirs = getProtectedDirectories();
            Set<String> excludeDirs = getExcludeDirectories();

            System.out.println("开始清理空目录，最大深度: " + maxDepth);
            System.out.println("保护目录: " + protectedDirs);

            // 多轮清理，直到没有更多空目录可以删除
            int totalDeletedCount = 0;
            boolean hasDeleted;
            int round = 1;

            do {
                hasDeleted = false;
                System.out.println("第 " + round + " 轮空目录清理...");

                // 收集所有空目录，按深度倒序排列（先删除深层目录）
                List<Path> emptyDirs = new ArrayList<>();

                Files.walkFileTree(projectRoot, EnumSet.noneOf(FileVisitOption.class), maxDepth, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        // 添加空值检查，防止根目录导致的空指针异常
                        Path fileName = dir.getFileName();
                        if (fileName == null) {
                            return FileVisitResult.CONTINUE;
                        }

                        String dirName = fileName.toString();
                        if (excludeDirs.contains(dirName)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        // 跳过项目根目录
                        if (dir.equals(projectRoot)) {
                            return FileVisitResult.CONTINUE;
                        }

                        // 检查是否是受保护的目录（只保护关键的结构目录）
                        if (isStrictlyProtectedDirectory(dir, protectedDirs, projectRoot)) {
                            return FileVisitResult.CONTINUE;
                        }

                        // 检查目录是否为空
                        if (isDirectoryEmpty(dir)) {
                            emptyDirs.add(dir);
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });

                // 按深度倒序排列，先删除深层目录
                emptyDirs.sort((a, b) -> Integer.compare(b.getNameCount(), a.getNameCount()));

                int roundDeletedCount = 0;
                for (Path emptyDir : emptyDirs) {
                    try {
                        // 再次检查目录是否为空（可能在删除其他目录时变为非空）
                        if (Files.exists(emptyDir) && isDirectoryEmpty(emptyDir)) {
                            Files.delete(emptyDir);
                            System.out.println("  删除空目录: " + projectRoot.relativize(emptyDir));
                            roundDeletedCount++;
                            hasDeleted = true;
                        }
                    } catch (IOException e) {
                        System.out.println("  警告：无法删除目录 " + emptyDir + ": " + e.getMessage());
                    }
                }

                totalDeletedCount += roundDeletedCount;
                System.out.println("第 " + round + " 轮删除了 " + roundDeletedCount + " 个空目录");
                round++;

                // 防止无限循环，最多执行5轮
                if (round > 5) {
                    break;
                }

            } while (hasDeleted);

            System.out.println("空目录清理完成，总共删除 " + totalDeletedCount + " 个空目录");
            return totalDeletedCount;

        } catch (Exception e) {
            System.err.println("清理空目录时出错: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取受保护的目录集合
     */
    private Set<String> getProtectedDirectories() {
        String protectedConfig = getConfig("protected.directories");
        if (protectedConfig == null || protectedConfig.trim().isEmpty()) {
            Set<String> defaultProtected = new HashSet<>();
            defaultProtected.add("src");
            defaultProtected.add("main");
            defaultProtected.add("test");
            defaultProtected.add("java");
            defaultProtected.add("resources");
            defaultProtected.add("webapp");
            defaultProtected.add("META-INF");
            defaultProtected.add("WEB-INF");
            return defaultProtected;
        }
        Set<String> dirProtected = new HashSet<>();
        for (String dir : protectedConfig.split(",")) {
            dirProtected.add(dir.trim());
        }
        return dirProtected;
    }

    /**
     * 检查目录是否受保护（原有方法，保持兼容性）
     */
    private boolean isProtectedDirectory(Path dir, Set<String> protectedDirs) {
        // 添加空值检查，防止根目录导致的空指针异常
        Path fileName = dir.getFileName();
        if (fileName != null) {
            String dirName = fileName.toString();
            if (protectedDirs.contains(dirName)) {
                return true;
            }
        }

        // 检查路径中是否包含受保护的目录名
        for (Path pathElement : dir) {
            if (protectedDirs.contains(pathElement.toString())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 严格检查目录是否受保护（用于空目录清理）
     * 只保护真正重要的结构目录，允许删除空的包目录
     */
    private boolean isStrictlyProtectedDirectory(Path dir, Set<String> protectedDirs, Path projectRoot) {
        // 添加空值检查，防止根目录导致的空指针异常
        Path fileName = dir.getFileName();
        if (fileName == null) {
            return false;
        }

        String dirName = fileName.toString();

        // 保护根级别的重要目录
        if (dir.getParent() != null && dir.getParent().equals(projectRoot) && protectedDirs.contains(dirName)) {
            return true;
        }

        // 保护src目录本身
        if (dirName.equals("src") && dir.getParent() != null) {
            Path parentDir = dir.getParent();
            // 检查是否是模块根目录下的src目录（包含pom.xml的目录）
            if (Files.exists(parentDir.resolve("pom.xml"))) {
                return true;
            }
        }

        // 保护main和test目录，但只在src目录下
        if ((dirName.equals("main") || dirName.equals("test")) &&
            dir.getParent() != null) {
            Path parentFileName = dir.getParent().getFileName();
            if (parentFileName != null && parentFileName.toString().equals("src")) {
                return true;
            }
        }

        // 保护java和resources目录，但只在main/test目录下
        if ((dirName.equals("java") || dirName.equals("resources")) &&
            dir.getParent() != null) {
            Path parentFileName = dir.getParent().getFileName();
            if (parentFileName != null) {
                String parentName = parentFileName.toString();
                if (parentName.equals("main") || parentName.equals("test")) {
                    return true;
                }
            }
        }

        // 保护META-INF和WEB-INF目录
        if (dirName.equals("META-INF") || dirName.equals("WEB-INF")) {
            return true;
        }

        return false;
    }

    /**
     * 获取跨平台路径分隔符
     */
    private String getPathSeparator() {
        return this.pathSeparator != null ? this.pathSeparator : File.separator;
    }

    /**
     * 获取跨平台换行符
     */
    private String getLineSeparator() {
        return this.lineSeparator != null ? this.lineSeparator : System.lineSeparator();
    }

    /**
     * 获取文件编码
     */
    private String getFileEncoding() {
        return this.fileEncoding != null ? this.fileEncoding : "UTF-8";
    }
}
