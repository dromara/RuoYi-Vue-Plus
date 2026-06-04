package org.dromara.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.*;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.common.oss.client.OssClient;
import org.dromara.common.oss.enums.AccessPolicy;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.oss.model.Options;
import org.dromara.common.oss.model.PutObjectResult;
import org.dromara.system.api.OssService;
import org.dromara.system.api.domain.OssDTO;
import org.dromara.system.domain.SysOss;
import org.dromara.system.domain.SysOssExt;
import org.dromara.system.domain.bo.SysOssBo;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.mapper.SysOssMapper;
import org.dromara.system.service.ISysOssService;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 文件上传 服务层实现
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysOssServiceImpl implements ISysOssService, OssService {

    private final SysOssMapper ossMapper;

    /**
     * 查询OSS对象存储列表
     *
     * @param bo        OSS对象存储分页查询对象
     * @param pageQuery 分页查询实体类
     * @return 结果
     */
    @Override
    public PageResult<SysOssVo> queryPageList(SysOssBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysOss> lqw = buildQueryWrapper(bo);
        Page<SysOssVo> result = ossMapper.selectVoPage(pageQuery.build(), lqw);
        List<SysOssVo> filterResult = StreamUtils.toList(result.getRecords(), this::matchingUrl);
        result.setRecords(filterResult);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 根据一组 ossIds 获取对应的 SysOssVo 列表
     *
     * @param ossIds 一组文件在数据库中的唯一标识集合
     * @return 包含 SysOssVo 对象的列表
     */
    @Override
    public List<SysOssVo> listByIds(Collection<Long> ossIds) {
        SysOssServiceImpl ossService = SpringUtils.getAopProxy(this);
        List<Supplier<SysOssVo>> suppliers = ossIds.stream().map(id -> (Supplier<SysOssVo>) () -> {
            SysOssVo vo = ossService.getById(id);
            if (ObjectUtil.isNotNull(vo)) {
                try {
                    return this.matchingUrl(vo);
                } catch (Exception ignored) {
                    // 如果oss异常无法连接则将数据直接返回
                    return vo;
                }
            }
            return null;
        }).toList();
        List<SysOssVo> list = ThreadUtils.virtualSubmitAll(suppliers);
        list.removeAll(Collections.singleton(null));
        return list;
    }

    /**
     * 根据一组 ossIds 获取对应文件的 URL 列表
     *
     * @param ossIds 以逗号分隔的 ossId 字符串
     * @return 以逗号分隔的文件 URL 字符串
     */
    @Override
    public String selectUrlByIds(String ossIds) {
        List<Long> ids = StringUtils.splitTo(ossIds, Convert::toLong);
        SysOssServiceImpl ossService = SpringUtils.getAopProxy(this);
        List<Supplier<String>> suppliers = ids.stream().map(id -> (Supplier<String>) () -> {
            SysOssVo vo = ossService.getById(id);
            if (ObjectUtil.isNotNull(vo)) {
                try {
                    return this.matchingUrl(vo).getUrl();
                } catch (Exception ignored) {
                    // 如果oss异常无法连接则将数据直接返回
                    return vo.getUrl();
                }
            }
            return null;
        }).toList();
        List<String> list = ThreadUtils.virtualSubmitAll(suppliers);
        list.removeAll(Collections.singleton(null));
        return StringUtils.joinComma(list);
    }

    /**
     * 根据逗号分隔的文件主键列表查询文件传输对象集合。
     *
     * @param ossIds 逗号分隔的文件主键字符串
     * @return 文件传输对象列表
     */
    @Override
    public List<OssDTO> selectByIds(String ossIds) {
        List<Long> ids = StringUtils.splitTo(ossIds, Convert::toLong);
        var ossService = SpringUtils.getAopProxy(this);
        List<Supplier<OssDTO>> suppliers = ids.stream().map(id -> (Supplier<OssDTO>) () -> {
            SysOssVo vo = ossService.getById(id);
            if (ObjectUtil.isNotNull(vo)) {
                try {
                    vo.setUrl(this.matchingUrl(vo).getUrl());
                    return BeanUtil.toBean(vo, OssDTO.class);
                } catch (Exception ignored) {
                    // 如果oss异常无法连接则将数据直接返回
                    return BeanUtil.toBean(vo, OssDTO.class);
                }
            }
            return null;
        }).toList();
        List<OssDTO> list = ThreadUtils.virtualSubmitAll(suppliers);
        list.removeAll(Collections.singleton(null));
        return list;
    }

    /**
     * 构造 OSS 文件列表查询条件。
     *
     * @param bo 文件筛选条件
     * @return 包含文件名、后缀、归属服务和创建时间区间的查询包装器
     */
    private LambdaQueryWrapper<SysOss> buildQueryWrapper(SysOssBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SysOss.class)
            .likeIfText(SysOss::getFileName, bo.getFileName())
            .likeIfText(SysOss::getOriginalName, bo.getOriginalName())
            .eqIfText(SysOss::getFileSuffix, bo.getFileSuffix())
            .eqIfText(SysOss::getUrl, bo.getUrl())
            .betweenParams(SysOss::getCreateTime, params, "beginCreateTime", "endCreateTime")
            .eqIfPresent(SysOss::getCreateBy, bo.getCreateBy())
            .eqIfText(SysOss::getService, bo.getService())
            .orderByAsc(SysOss::getOssId)
            .build();
    }

    /**
     * 根据 ossId 从缓存或数据库中获取 SysOssVo 对象
     *
     * @param ossId 文件在数据库中的唯一标识
     * @return SysOssVo 对象，包含文件信息
     */
    @Cacheable(cacheNames = CacheNames.SYS_OSS, key = "#ossId")
    @Override
    public SysOssVo getById(Long ossId) {
        return ossMapper.selectVoById(ossId);
    }


    /**
     * 文件下载方法，支持一次性下载完整文件
     *
     * @param ossId OSS对象ID
     */
    @Override
    public ResponseEntity<byte[]> download(Long ossId) {
        SysOssVo sysOss = SpringUtils.getAopProxy(this).getById(ossId);
        if (ObjectUtil.isNull(sysOss)) {
            throw new ServiceException("文件数据不存在!");
        }
        String percentEncodedFileName = FileUtils.percentEncode(sysOss.getOriginalName());
        return OssFactory.instance(sysOss.getService())
            .download(sysOss.getFileName(), (result, inputStream) -> {
                // 尝试解析媒体类型，如果解析失败，则使用 application/octet-stream
                MediaType mediaType;
                try {
                    mediaType = MediaType.parseMediaType(result.contentType());
                } catch (Exception e) {
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                }
                // 构建响应实体
                return ResponseEntity.ok()
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition,download-filename")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=%s;filename*=utf-8''%s".formatted(percentEncodedFileName, percentEncodedFileName))
                    .header("download-filename", percentEncodedFileName)
                    .contentType(mediaType)
                    .contentLength(result.size())
                    .body(IoUtil.readBytes(inputStream));
            });

    }

    /**
     * 上传 MultipartFile 到对象存储服务，并保存文件信息到数据库
     *
     * @param file 要上传的 MultipartFile 对象
     * @return 上传成功后的 SysOssVo 对象，包含文件信息
     * @throws ServiceException 如果上传过程中发生异常，则抛出 ServiceException 异常
     */
    @Override
    public SysOssVo upload(MultipartFile file) {
        if (ObjectUtil.isNull(file) || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        String originalfileName = file.getOriginalFilename();
        String suffix = StringUtils.substring(originalfileName, originalfileName.lastIndexOf("."), originalfileName.length());
        OssClient instance = OssFactory.instance();
        String pathKey = instance.buildPathKey(originalfileName);
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectResult result = instance.upload(pathKey, inputStream, file.getSize(), Options.builder().setContentType(file.getContentType()));
            SysOssExt ext1 = new SysOssExt();
            ext1.setFileSize(file.getSize());
            ext1.setContentType(file.getContentType());
            // 保存文件信息
            return buildResultEntity(originalfileName, suffix, instance.clientId(), result, ext1);
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 上传文件到对象存储服务，并保存文件信息到数据库
     *
     * @param file 要上传的文件对象
     * @return 上传成功后的 SysOssVo 对象，包含文件信息
     */
    @Override
    public SysOssVo upload(File file) {
        if (ObjectUtil.isNull(file) || !file.isFile() || file.length() <= 0) {
            throw new ServiceException("上传文件不能为空");
        }
        String originalfileName = file.getName();
        String suffix = StringUtils.substring(originalfileName, originalfileName.lastIndexOf("."), originalfileName.length());
        OssClient instance = OssFactory.instance();
        String pathKey = instance.buildPathKey(originalfileName);
        PutObjectResult result = instance.upload(pathKey, file, Options.builder().setContentType(FileUtils.getMimeType(file.toPath())));
        SysOssExt ext1 = new SysOssExt();
        ext1.setFileSize(result.size());
        // 保存文件信息
        return buildResultEntity(originalfileName, suffix, instance.clientId(), result, ext1);
    }

    /**
     * 组装上传结果并持久化文件元数据。
     *
     * @param originalfileName 原始文件名
     * @param suffix           文件后缀
     * @param configKey        存储配置标识
     * @param result           上传结果
     * @param ext1             扩展属性对象
     * @return 持久化后的文件信息视图
     */
    @NotNull
    private SysOssVo buildResultEntity(String originalfileName, String suffix, String configKey, PutObjectResult result, SysOssExt ext1) {
        SysOss oss = new SysOss();
        oss.setUrl(result.url());
        oss.setFileSuffix(suffix);
        oss.setFileName(result.key());
        oss.setOriginalName(originalfileName);
        oss.setService(configKey);
        oss.setExt1(JsonUtils.toJsonString(ext1));
        ossMapper.insert(oss);
        SysOssVo sysOssVo = MapstructUtils.convert(oss, SysOssVo.class);
        return this.matchingUrl(sysOssVo);
    }

    /**
     * 删除OSS对象存储
     *
     * @param ids     OSS对象ID串
     * @param isValid 判断是否需要校验
     * @return 结果
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 做一些业务上的校验,判断是否需要校验
        }
        List<SysOss> list = ossMapper.selectByIds(ids);
        for (SysOss sysOss : list) {
            OssFactory.instance(sysOss.getService()).delete(sysOss.getFileName());
        }
        return ossMapper.deleteByIds(ids) > 0;
    }

    /**
     * 桶类型为 private 的URL 修改为临时URL时长为120s
     *
     * @param oss OSS对象
     * @return oss 匹配Url的OSS对象
     */
    private SysOssVo matchingUrl(SysOssVo oss) {
        OssClient instance = OssFactory.instance(oss.getService());
        // 仅修改桶类型为 private 的URL，临时URL时长为120s
        if (instance.verifyConfig(config -> AccessPolicy.PRIVATE.equals(config.accessControlPolicyConfig().accessPolicy()))) {
            oss.setUrl(instance.presignGetUrl(oss.getFileName(), Duration.ofSeconds(120)));
        }
        return oss;
    }
}
