# RuoYi-Vue-Plus 多仓库 Git 管理手册

## 目录
1. [项目架构说明](#项目架构说明)
2. [初始化设置](#初始化设置)
3. [日常操作流程](#日常操作流程)
4. [冲突处理](#冲突处理)
5. [命令速查表](#命令速查表)
6. [注意事项](#注意事项)

## 项目架构说明
- **主仓库**：`ruoyi-vue-plus`
  - 同步源：`https://github.com/dromara/RuoYi-Vue-Plus`
  - 推送目标：`https://github.com/figo990/RuoYi-Vue-Plus`
- **子模块**：`ruoyi-plus-soybean`
  - 同步源：`https://gitee.com/xlsea/ruoyi-plus-soybean`
  - 与主仓库一起推送到 `figo990/RuoYi-Vue-Plus`

## 初始化设置
```bash
# 1. 克隆主仓库
git clone https://github.com/figo990/RuoYi-Vue-Plus.git ruoyi-vue-plus
cd ruoyi-vue-plus

# 2. 添加官方仓库为上游源
git remote add upstream https://github.com/dromara/RuoYi-Vue-Plus.git

# 3. 初始化子模块
git submodule add https://gitee.com/xlsea/ruoyi-plus-soybean.git ruoyi-plus-soybean
```

## 日常操作流程
### 同步主仓库更新
```bash
git fetch upstream
git merge upstream/main
git push origin main
```

### 同步子模块更新
```bash
cd ruoyi-plus-soybean
git pull origin main
cd ..
git add ruoyi-plus-soybean
git commit -m "Update submodule"
git push origin main
```

### 提交本地修改
```bash
# 修改主仓库
git add .
git commit -m "修改说明"
git push origin main

# 修改子模块
cd ruoyi-plus-soybean
git add .
git commit -m "子模块修改说明"
git push origin main
cd ..
git add ruoyi-plus-soybean
git commit -m "更新子模块引用"
git push origin main
```

## 冲突处理
### 主仓库冲突
```bash
git fetch upstream
git merge upstream/main
# 手动解决冲突后
git add .
git commit -m "解决冲突"
git push origin main
```

### 子模块冲突
```bash
cd ruoyi-plus-soybean
git pull origin main
# 手动解决冲突后
git add .
git commit -m "解决子模块冲突"
git push origin main
cd ..
git add ruoyi-plus-soybean
git commit -m "更新子模块引用"
git push origin main
```

## 命令速查表
| 操作           | 命令                                                                                |
| -------------- | ----------------------------------------------------------------------------------- |
| 拉取官方更新   | `git fetch upstream && git merge upstream/main`                                     |
| 推送主仓库修改 | `git add . && git commit -m "msg" && git push origin main`                          |
| 更新子模块     | `cd ruoyi-plus-soybean && git pull origin main`                                     |
| 提交子模块修改 | `cd ruoyi-plus-soybean && git add . && git commit -m "msg" && git push origin main` |

## 注意事项
1. 首次克隆后需初始化子模块：
   ```bash
   git submodule update --init --recursive
   ```
2. 子模块修改需单独提交到其仓库
3. 保持主仓库和子模块分支一致（推荐使用`main`分支）