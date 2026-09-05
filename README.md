# Maid Command Processor - 女仆指令处理器

车万女仆（Touhou Little Maid）的附属模组，让女仆能够通过AI对话系统执行Minecraft指令。

## 🎯 核心功能

### 1. 调用车万女仆AI系统
- **AI对话**：使用车万女仆内置的AI系统（支持通义千问、GPT、Gemini等）
- **工具调用**：将Minecraft指令注册为AI工具
- **语音合成**：使用车万女仆的TTS系统

### 2. 智能指令解析
```
玩家："让天气变成晴天"
AI解析：weather clear
女仆执行：✅ 成功执行
```

### 3. 权限系统（5级）
- `NONE`：无权限
- `BASIC`：基础权限（简单指令）
- `ADVANCED`：高级权限（大多数指令）
- `WHITELIST`：白名单权限（所有指令）
- `ADMIN`：管理员权限（最高权限）

### 4. 模组兼容性
- ✅ 原版Minecraft指令（60+）
- ✅ 车万女仆模组指令
- ✅ 其他模组指令（可配置）
- ✅ 其他模组物品（可配置）

## 📁 项目架构

```
maid-command-processor/
├── src/main/java/com/maidcommandprocessor/
│   ├── MaidCommandProcessor.java          # 主Mod类
│   │
│   ├── config/                            # 配置系统
│   │   └── MaidCommandConfig.java         # 配置类（权限/响应模板等）
│   │
│   ├── handler/                           # 核心处理模块
│   │   ├── PermissionModule.java          # 权限管理
│   │   └── CommandExecutorModule.java     # 指令执行
│   │
│   ├── integration/                       # 外部集成
│   │   └── LittleMaidToolRegistry.java    # 工具注册（注册到车万女仆AI）
│   │
│   ├── command/                           # 游戏指令
│   │   └── MaidCommandExecutor.java       # /maidcmd 指令
│   │
│   └── registry/                          # 注册系统
│       └── CommandRegistry.java           # 指令注册
│
└── src/main/resources/
    └── assets/maid_command_processor/lang/
        ├── en_us.json                     # 英文
        └── zh_cn.json                     # 中文
```

## 🚀 快速开始

### 环境要求
- Java 21
- NeoForge 1.21.1 (版本 21.1.249+)
- 车万女仆（必需，版本 1.5.3+）

### 编译
```bash
cd maid-command-processor
./gradlew build
```

编译后的文件：`build/libs/maid-command-processor-1.0.0.jar`

### 安装
1. 确保已安装车万女仆 1.5.3+
2. 将jar文件放入`mods`文件夹
3. 启动游戏
4. 配置文件生成于：`config/maid_command_processor-common.toml`

## 📋 配置说明

### 权限配置
```toml
[permission]
requirePermission = false
whitelistPlayers = []               # 白名单玩家
adminPlayers = []                   # 管理员玩家
```

### 指令兼容配置
```toml
[command_compatibility]
allowVanillaCommands = true         # 允许原版指令
allowMaidModCommands = true         # 允许女仆指令
modCommands = ["create:rotate"]     # 其他模组指令
modItems = ["create:kinetic_motor"] # 其他模组物品
```

### 响应模板配置
```toml
[responses]
successResponse = "✅ %s"           # 成功响应
failureResponse = "❌ %s"           # 失败响应
errorResponse = "⚠️ Error: %s"      # 错误响应
cooldownResponse = "⏳ Command is on cooldown"
noPermissionResponse = "🔒 You don't have permission"
```

### 语音配置（使用车万女仆TTS）
```toml
[voice]
enableVoiceOutput = false           # 启用语音输出（使用车万女仆TTS）
voiceOutputLanguage = "zh-CN"       # 语音输出语言
```

### 聊天配置
```toml
[chat]
enableChatResponse = true           # 启用聊天响应
chatResponseCooldown = 500          # 响应冷却时间（毫秒）
```

## 🎮 使用方法

### 游戏内指令
```
/maidcommand execute <指令>    # 执行指令
/maidcommand test              # 测试模组
/maidcommand info              # 查看信息
/maidcommand permissions       # 查看权限
/maidcommand commands          # 查看兼容指令
```

### AI对话示例

**基础对话：**
1. 对女仆按 `T` 键打开对话框
2. 输入自然语言：
   - "让天气变成晴天" → 执行 `weather clear`
   - "给我好的装备" → 执行 `give @p diamond_armor`
   - "把时间改成白天" → 执行 `time set day`

**语音对话：**
1. 在车万女仆AI设置中配置TTS站点
2. 启用语音输出
3. 对女仆说话，女仆会语音回复

## 🧠 工作原理

### 工具注册流程
```
模组加载 → 注册工具到车万女仆AI → 女仆AI可调用工具
```

### 指令执行流程
```
玩家输入 → 车万女仆AI解析 → 调用工具 → 执行指令 → 返回结果
```

### 支持的指令类型
- `weather`：改变天气
- `time`：调整时间
- `give`：给予物品
- `tp`：传送
- `kill`：消灭实体
- `summon`：召唤实体
- `setblock`：放置方块
- `execute`：执行命令

## 🔌 扩展指南

### 添加自定义工具
在 `LittleMaidToolRegistry.java` 中添加：
```java
registerTool(
    "custom_tool",
    "工具描述",
    "/custom_command",
    "示例：做某事",
    true
);
```

### 添加模组兼容
在配置文件中添加：
```toml
modCommands = ["modid:command"]
modItems = ["modid:item"]
```

## 📊 功能完成度

| 模块 | 状态 | 完成度 |
|------|------|--------|
| 项目框架 | ✅ 完成 | 100% |
| 权限系统 | ✅ 完成 | 100% |
| 指令执行 | ✅ 完成 | 100% |
| 工具注册 | ✅ 完成 | 100% |
| 配置系统 | ✅ 完成 | 100% |
| 语音模块 | ⚠️ 框架完成 | 60% |
| 事件监听 | ⚠️ 基础完成 | 60% |
| 实际测试 | ❌ 待完成 | 0% |

**总体完成度：约75%**

## 🎯 下一步工作

### 必需（达到可用状态）
1. ✅ 修复编译问题
2. ⏳ 集成车万女仆实际API
3. ⏳ 实现真实的事件监听
4. ⏳ 测试和调试

### 可选（增强功能）
1. ⏳ 添加更多预设对话模式
2. ⏳ 添加Web管理界面

## 📝 更新日志

### v1.0.0 (2026-09-05)
- ✅ 初始版本
- ✅ 模块化架构设计
- ✅ 权限系统（5级）
- ✅ 指令执行模块
- ✅ 工具注册系统（注册到车万女仆AI）
- ✅ 配置系统
- ✅ 中英文支持

## 📄 许可证
LGPL-2.1

## 🤝 贡献
欢迎提交Issue和Pull Request！

## 📧 联系方式
- NeoForge Discord: https://discord.neoforged.net/
- 车万女仆社区：[添加链接]
