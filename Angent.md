# Project Rules (/init)

## 项目概览
- 项目名称：DataGrip Row JSON Viewer
- 项目类型：JetBrains/DataGrip 插件（Kotlin）
- 构建系统：Gradle Kotlin DSL
- Java/Kotlin 目标版本：17
- 主要目标：在 DataGrip 结果集中将当前行展示为格式化 JSON（支持搜索与复制）

## 关键目录与文件
- `src/main/kotlin/com/example/datagripjson/`：核心业务代码
- `src/main/resources/META-INF/plugin.xml`：插件注册、Action、ToolWindow 配置
- `build.gradle.kts`：构建与平台依赖配置
- `README.md`：使用说明与安装方式

## 构建与验证
- Windows 构建插件：`./gradlew.bat buildPlugin`
- 通用构建命令：`./gradlew buildPlugin`
- 输出目录：`build/distributions/`

## 开发约束
- 遵循 SOLID / KISS / DRY / YAGNI
- 先读后改：修改前先理解当前实现与调用链
- 最小改动原则：只实现当前明确需求，不做超前设计
- 注释语言保持与仓库现有风格一致
- 不主动进行 Git 提交、推送、建分支，除非用户明确要求

## 工具与命令约定
- 文本搜索优先 `rg`；不可用时使用 PowerShell 等价命令
- 执行命令时路径使用双引号，优先正斜杠 `/` 兼容写法
- 变更后应进行最小可行验证（至少编译/构建通过）

## 风险操作确认
执行以下操作前必须获得明确确认：
- 文件删除/批量重写/系统级移动
- `git commit` / `git push` / `git reset --hard`
- 环境变量、系统权限、系统设置修改
- 数据库删除、结构变更、批量更新
- 向生产环境发送请求或传输敏感信息
- 全局安装/卸载依赖、升级核心依赖

确认模板：

```text
⚠️ 危险操作检测！
操作类型：[具体操作]
影响范围：[详细说明]
风险评估：[潜在后果]

请确认是否继续？[需要明确的"是"、"确认"、"继续"]
```

## 当前已知项目状态（初始化时）
- 当前分支：`main`
- 存在未提交改动：`src/main/resources/META-INF/plugin.xml`
- 插件 ID 已变更为：`io.github.cloud9527.rowjsonviewer`
