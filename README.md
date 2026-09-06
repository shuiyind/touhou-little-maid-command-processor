# Maid Command Processor - 女仆指令处理器

车万女仆（Touhou Little Maid）的附属模组，让女仆能够通过AI对话系统智能执行Minecraft指令。

## 🎯 核心功能

### 1. AI 工具系统（5个自定义工具）
- **`minecraft_command`** - 执行单个Minecraft指令
- **`batch_command`** - 批量执行多条指令（自动优化）
- **`permission`** - 权限管理（查询、设置、撤销）
- **`item_check`** - 物品和附魔检查（支持MOD兼容）
- **`apply_effect`** - 应用BUFF/DEBUFF效果（现实时间秒）

### 2. 智能指令解析
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

## 📝 更新日志

### v1.1.0 (2026-09-05) - 当前版本
- ✅ 新增 `apply_effect` 工具（BUFF/DEBUFF应用）
- ✅ 新增 NBT标签自动修复功能
- ✅ 持续时间改为现实时间（秒）
- ✅ 优化权限查询返回消息格式
- ✅ 智能批量命令执行策略
- ✅ MOD兼容性管理器（动态注册表 + 标准清单）
- ✅ 命令去重和冷却机制

### v1.0.0 (2026-09-05) - 初始版本
- ✅ 模块化架构设计
- ✅ 基础AI工具系统
- ✅ 权限系统（4级）
- ✅ 命令执行模块
- ✅ 配置系统
- ✅ 中英文支持

## 📄 许可证
LGPL-2.1

## 🤝 贡献
欢迎提交Issue和Pull Request！

## 📧 反馈与问题
如有问题或建议，请在 GitHub Issues 中提交！# Test
