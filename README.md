# Maid Command Processor - 女仆指令处理器

[![License](https://img.shields.io/badge/license-LGPL--2.1-green)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://java.org/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.249-orange)](https://neoforged.net/)
[![Minecraft](https://img.shields.io/badge/MC-1.21.1-brightgreen)](https://minecraft.net/)

> ⚠️ **免责声明 / Disclaimer**  
> 本项目为新手个人借助AI开发的附属模组，代码质量和测试可能不够完善。**使用风险自负，包括但不限于游戏崩溃、存档损坏、指令执行异常等问题。**  
> This project is developed by a beginner with AI assistance. Code quality and testing may be insufficient. **Use at your own risk, including but not limited to game crashes, save corruption, or unexpected command behavior.**

车万女仆（Touhou Little Maid）的附属模组，让女仆能够通过AI对话系统智能执行Minecraft指令。

## 🎯 快速链接

- 📦 [版本发布](https://github.com/shuiyind/touhou-little-maid-command-processor/releases)
- 🔍 [问题反馈](https://github.com/shuiyind/touhou-little-maid-command-processor/issues)
- 💬 [讨论交流](https://github.com/shuiyind/touhou-little-maid-command-processor/discussions)

## 🎯 核心功能

### 1. AI 工具系统（5个自定义工具）
| 工具 | ID | 描述 |
|------|-----|------|
| **minecraft_command** | `minecraft_command` | 执行单个Minecraft指令 |
| **batch_command** | `batch_command` | 批量执行多条指令（自动优化） |
| **permission** | `maid_permission` | 权限管理（查询、设置、撤销） |
| **item_check** | `item_check` | 物品和附魔检查（支持MOD兼容） |
| **apply_effect** | `apply_effect` | 应用BUFF/DEBUFF效果（现实时间秒） |

### 2. 智能指令解析示例
```
玩家："给我力量3，24小时"
AI解析：apply_effect(target="@s", effectType="strength", duration=86400, amplifier=2)
女仆执行：✅ 成功应用力量效果（24现实小时）
```

### 3. 权限系统（4级）
- **NONE (0)** - 无权限：不能执行任何命令
- **BASIC (1)** - 初级管理：可以使用天气、时间、传送等基础命令
- **ADVANCED (2)** - 管理员：可以执行 /op、/gamemode 等高级命令，可撤销BASIC权限
- **ADMIN (3)** - 服务器之主：拥有所有权限，可管理其他玩家权限

**权限规则：**
- 默认玩家权限为 NONE
- 只有 ADMIN (3级) 可以设置任意玩家的权限
- ADVANCED (2级) 只能撤销 BASIC (1级) 的权限
- 单机玩家自动获得 ADMIN 权限
- 局域网用户需要手动授权

### 4. MOD 兼容性管理
- ✅ 原版Minecraft指令（60+）
- ✅ 车万女仆模组指令
- ✅ 其他模组指令（动态注册表查询 + 本地缓存）
- ✅ 主流MOD标准清单（AE2附属、匠魂、MEK等）

## 📁 项目架构

```
maid-command-processor/
├── src/main/java/com/maidcommandprocessor/
│   ├── MaidCommandProcessor.java          # 主Mod类
│   │
│   ├── ai/                                # AI工具系统
│   │   ├── MaidToolRegistry.java          # 工具注册中心
│   │   ├── MinecraftCommandTool.java      # 单命令执行
│   │   ├── BatchCommandTool.java          # 批量命令执行
│   │   ├── PermissionTool.java            # 权限管理工具
│   │   ├── ItemCheckTool.java             # 物品/附魔检查
│   │   ├── ApplyEffectTool.java           # BUFF/DEBUFF应用
│   │   └── AINegotiationEngine.java       # AI谈判引擎
│   │
│   ├── handler/                           # 核心处理模块
│   │   ├── CommandExecutorModule.java     # 命令执行（含NBT自动修复）
│   │   ├── CommandQueueModule.java        # 命令队列管理
│   │   ├── CompatibleCommandsModule.java  # 兼容指令解析
│   │   ├── MaidChatListener.java          # 聊天事件监听
│   │   └── PermissionModule.java          # 权限管理逻辑
│   │
│   ├── config/                            # 配置系统
│   │   ├── MaidCommandConfig.java         # 主配置文件
│   │   └── ModRegistryManager.java        # MOD兼容性管理器
│   │
│   ├── integration/                       # 外部集成
│   │   ├── LittleMaidIntegration.java     # 车万女仆API集成
│   │   ├── LittleMaidToolRegistry.java    # 工具注册（对接女仆AI）
│   │   └── AIChatIntegration.java         # AI对话集成
│   │
│   ├── command/                           # 游戏指令
│   │   └── MaidCommandExecutor.java       # /maidcmd 指令
│   │
│   ├── registry/                          # 注册系统
│   │   └── CommandRegistry.java           # 指令注册表
│   │
│   ├── feedback/                          # 反馈系统
│   │   └── MaidFeedbackSystem.java        # 玩家反馈收集
│   │
│   └── voice/                             # 语音模块
│       ├── VoiceInputModule.java          # 语音输入
│       └── VoiceOutputModule.java         # 语音输出（TTS）
│
└── src/main/resources/
    ├── maid_command_processor/mod_compatibility.json  # MOD兼容清单
    └── assets/maid_command_processor/lang/
        ├── en_us.json                     # 英文
        └── zh_cn.json                     # 中文
```

## 🚀 快速开始

### 系统要求
- **Java**: 21+
- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.249+
- **车万女仆 (Touhou Little Maid)**: 1.5.3+（必需依赖）

### 编译构建
```bash
# 克隆并进入项目目录
git clone <repository-url>
cd touhou-little-maid-command-processor

# 使用 Gradle 构建
./gradlew build

# Windows 用户使用：
.\gradlew.bat build
```

**输出文件**: `build/libs/maid_command_processor-1.0.0.jar`

### 安装步骤
1. 确保已安装 **车万女仆 (Touhou Little Maid) 1.5.3+**
2. 将编译后的 jar 文件放入 Minecraft 的 `mods` 文件夹
3. 启动游戏，模组会自动加载并生成配置文件
4. 配置文件位置：`config/maid_command_processor-common.toml`

### 依赖项
```toml
[[dependencies.maid_command_processor]]
modId = "touhoulittlemaid"
mandatory = true  # 必需依赖
versionRange = "[1.5.3,)"
ordering = "NONE"
side = "BOTH"

## 📋 配置说明

### 权限配置
```toml
[permission]
requirePermission = false                    # 是否需要权限检查
whitelistPlayers = []                        # 白名单玩家（自动ADMIN）
adminPlayers = []                            # 管理员玩家列表
```

### MOD兼容性配置
```toml
[mod_compatibility]
enableDynamicRegistry = true                 # 启用动态注册表查询
standardMods = ["ae2", "tinker", "mekanism"] # 标准MOD清单
customCommands = ["create:rotate"]           # 自定义模组指令
customItems = ["create:kinetic_motor"]       # 自定义模组物品
```

### 命令执行配置
```toml
[command_execution]
enableNbtAutoFix = true                      # 启用NBT标签自动修复
commandCooldown = 500                        # 命令冷却时间（毫秒）
enableCommandDedup = true                    # 启用命令去重
batchThreshold = 3                           # 批量命令阈值（超过此数量使用批量执行）
```

### 响应模板配置
```toml
[responses]
successResponse = "✅ %s"                   # 成功响应
failureResponse = "❌ %s"                   # 失败响应
errorResponse = "⚠️ Error: %s"              # 错误响应
cooldownResponse = "⏳ 指令冷却中，请稍等"   # 冷却响应
noPermissionResponse = "🔒 权限不足"        # 权限不足响应
```

### 语音配置（使用车万女仆TTS）
```toml
[voice]
enableVoiceOutput = false                    # 启用语音输出
voiceOutputLanguage = "zh-CN"                # 语音输出语言
```

### 聊天配置
```toml
[chat]
enableChatResponse = true                    # 启用聊天响应
chatResponseCooldown = 500                   # 响应冷却时间（毫秒）
```

## 🎮 使用方法

### AI对话示例

**基础指令：**
1. 对女仆按 `T` 键打开对话框
2. 输入自然语言：
   - "让天气变成晴天" → 执行 `weather clear`
   - "把时间改成白天" → 执行 `time set day`
   - "给我一把钻石剑" → 执行 `give @p diamond_sword 1`

**物品给予（自动优化）：**
```
玩家："给我原版最强附魔的钻石剑"
AI解析：
  1. 检查 diamond_sword 是否存在 ✅
  2. 检查附魔是否存在 ✅
  3. 执行 /give @p diamond_sword 1 {Enchantments:[...]}
女仆执行：✅ 成功获得附魔钻石剑
```

**BUFF/DEBUFF应用（现实时间）：**
```
玩家："给我力量3，24小时"
AI解析：apply_effect(target="@s", effectType="strength", duration=86400, amplifier=2)
女仆执行：✅ 已施加力量III效果（24现实小时）
```

**权限管理：**
```
玩家："查看我的权限"
AI解析：permission_check(target="@s")
女仆回复：=== 权限等级 ===
         玩家 'ShuiYinD' 的当前权限: 无权限 (等级 0)
         
         权限说明:
         - 无权限 (0): 不能执行任何命令
         - 初级管理 (1): 可以使用天气、时间、传送等基础命令
         - 管理员 (2): 可以执行 /op、/gamemode 等高级命令
         - 服务器之主 (3): 拥有所有权限，可以管理其他玩家权限
```

### 游戏内指令
```
/maidcommand execute <指令>    # 执行指令
/maidcommand test              # 测试模组
/maidcommand info              # 查看信息
/maidcommand permissions       # 查看权限
/maidcommand commands          # 查看兼容指令
```

## 🧠 核心特性

### 1. NBT 标签自动修复
AI生成的命令中NBT标签位置错误时自动修正：
```
错误格式：/give @p diamond{Enchantments:[...]} 1
正确格式：/give @p diamond 1 {Enchantments:[...]}
```

### 2. 智能批量执行
根据物品复杂度选择执行策略：
- **简单物品** → 单命令执行
- **复杂附魔** → 批量命令执行（优化性能）

### 3. MOD 兼容性检查
- **动态注册表查询**：运行时检测MOD是否存在
- **标准MOD清单**：预定义主流MOD（AE2、匠魂、MEK等）
- **本地缓存**：提升查询性能

### 4. 命令去重和冷却
- 防止AI重复执行相同命令
- 可配置的冷却时间（默认500ms）
- 智能命令队列管理

## 🔌 扩展指南

### 添加自定义工具
在 `MaidToolRegistry.java` 中注册：
```java
MyCustomTool myTool = new MyCustomTool();
ToolRegister.getAllTools().put(MyCustomTool.TOOL_ID, myTool);
customTools.add(myTool);
```

### 添加MOD兼容
在配置文件 `mod_compatibility.json` 中添加：
```json
{
  "modId": "my_mod",
  "items": ["my_mod:custom_item"],
  "effects": ["my_mod:custom_effect"]
}
```

## 📊 功能完成度

| 模块 | 状态 | 完成度 |
|------|------|--------|
| AI工具系统（5个工具） | ✅ 完成 | 100% |
| 权限系统（4级） | ✅ 完成 | 100% |
| 命令执行（含NBT修复） | ✅ 完成 | 100% |
| MOD兼容性管理 | ✅ 完成 | 100% |
| 批量命令优化 | ✅ 完成 | 100% |
| 配置系统 | ✅ 完成 | 100% |
| 中英文支持 | ✅ 完成 | 100% |
| 语音模块 | ⚠️ 框架完成 | 60% |
| 反馈系统 | ⚠️ 基础完成 | 50% |

**总体完成度：约85%**

## 🔨 CI/CD & 开发工具

### GitHub Actions
本项目使用 GitHub Actions 进行持续集成：
- **Gradle Build** - 自动构建和测试
- **CodeQL Analysis** - 静态代码安全分析
- **Issue Manager** - 自动化 Issue 管理

### 代码质量
- ✅ JaCoCo 代码覆盖率检测
- ✅ CodeQL 安全扫描
- ✅ Gradle Configuration Cache（加速构建）

## 📝 更新日志

### v1.0.0 (2026-09-10) - 当前版本
- ✅ AI工具系统（5个自定义工具）
- ✅ 4级权限管理系统
- ✅ NBT标签自动修复功能
- ✅ 智能批量命令执行策略
- ✅ MOD兼容性管理器（动态注册表 + 标准清单）
- ✅ 命令去重和冷却机制
- ✅ BUFF/DEBUFF应用工具（现实时间秒）
- ✅ 语音输入/输出模块框架
- ✅ 反馈系统基础框架
- ✅ 中英文双语支持
- ✅ Gradle构建配置完整

## 📄 许可证
[LGPL-2.1](LICENSE) - 自由软件许可证，允许修改和再分发。

## 🤝 贡献
欢迎提交 Issue 和 Pull Request！

### 开发环境搭建
```bash
# 克隆项目
git clone <repository-url>
cd touhou-little-maid-command-processor

# 构建项目
./gradlew build

# 运行客户端测试
./gradlew runClient

# 生成数据文件（可选）
./gradlew data

## 📧 反馈与问题
如有问题或建议，请在 [GitHub Issues](https://github.com/shuiyind/touhou-little-maid-command-processor/issues) 中提交！

---

*Made with ❤️ by [shuiyind](https://github.com/shuiyind)*
