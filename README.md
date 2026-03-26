# Multi-core processing system (MCPS)

一个为Minecraft设计的多线程处理系统，旨在通过并行处理优化游戏性能。

## 架构概述

MCPS采用模块化架构设计，将不同功能划分为独立的组件，实现了高度的解耦和可维护性。

### 1. 核心组件

- **ThreadManager**：线程管理器，负责创建和管理工作线程池，支持自适应线程池大小调整、任务优先级调度和线程利用率监控
- **TaskScheduler/SmartTaskScheduler**：任务调度器，负责分配和管理游戏逻辑任务，后者基于智能优化算法
- **ThreadCommunication**：线程通信系统，实现不同线程之间的安全通信
- **ResourceManager**：资源管理器，管理共享资源并提供线程安全的访问机制
- **GameLogicProcessor/GameLogicExpander**：游戏逻辑处理器，实现核心游戏逻辑的并行处理
- **PerformanceMonitor**：性能监控器，收集和分析系统性能数据
- **ErrorHandler**：错误处理器，处理线程相关的错误和异常
- **CompatibilityManager**：兼容性管理器，确保与基础游戏和现有模组的兼容性
- **ConfigManager**：配置管理器，管理模组配置
- **FogManager**：雾管理器，控制雾效果
- **RenderManager**：渲染管理器，优化渲染流程，支持光线追踪优化、动态LOD系统和体素渲染优化
- **GPUManager**：GPU管理器，利用GPU加速计算
- **StorageManager**：存储管理器，管理世界数据存储，支持区块压缩、异步存储操作和分布式存储
- **NetworkManager**：网络管理器，优化网络通信
- **AIManager**：AI管理器，优化实体AI处理
- **NPCBehaviorManager**：NPC行为管理器，实现智能NPC行为，支持并行化实体AI决策、高效寻路算法和村民行为优化
- **ClusterManager**：集群管理器，支持跨服务器负载均衡
- **PlatformManager**：平台管理器，适配不同平台
- **DiagnosticManager**：诊断管理器，诊断系统问题
- **TestManager**：测试管理器，执行测试
- **CloudManager**：云服务管理器，提供云端性能监控、跨服务器资源共享和云备份与恢复功能
- **APIDocumentation**：API文档系统，生成和管理API文档
- **CompatibilityTestTool**：兼容性测试工具，检测模组间的兼容性
- **IntegrationHelper**：集成辅助库，提供模组集成功能
- **ModInteractionFramework**：模组交互框架，支持模组间的通信
- **ModLoadingOptimizer**：模组加载优化，实现并行加载和懒加载
- **RealTimePerformanceAnalyzer**：实时性能分析器，提供详细的性能监控和瓶颈检测
- **BottleneckDetector**：瓶颈检测工具，自动检测性能瓶颈并提供优化建议
- **OptimizationSuggestionSystem**：优化建议系统，基于硬件和游戏内容提供个性化优化建议
- **AdvancedPerformanceMonitor**：高级性能监控界面，提供详细的性能指标显示和实时性能图表
- **TaskSchedulerVisualizer**：任务调度可视化界面，实现任务执行可视化和线程利用率图表
- **OptimizationConfigScreen**：自定义优化配置界面，提供详细配置选项和基于硬件的预设
- **MCPSMainMenu**：MCPS主菜单，整合所有用户界面组件

### 2. 架构层次

- **底层**：线程管理和并发工具
- **中层**：任务调度和资源管理
- **上层**：游戏逻辑处理和性能监控
- **接口层**：与Minecraft和其他模组的集成
- **扩展层**：AI、GPU、集群等高级功能

## 系统特点

### 1. 高性能

- 基于线程池的并行处理
- 智能任务调度和负载均衡
- 智能优化的任务分配
- 优化的线程通信机制
- 实时性能监控和分析
- GPU加速支持
- 跨服务器负载均衡

### 2. 可靠性

- 完善的错误处理和恢复机制
- 线程安全的资源管理
- 防止数据竞争和死锁
- 稳定的系统运行保障
- 兼容性检查和处理

### 3. 功能丰富

- 雾管理和控制
- 渲染优化（光线追踪、动态LOD、体素渲染）
- 存储优化（区块压缩、异步存储、分布式存储）
- 网络优化
- AI优化（智能NPC行为、并行化实体AI、高效寻路）
- 智能任务调度
- 自适应线程池
- 平台适配
- 诊断和测试工具
- 云服务集成（云端性能监控、跨服务器资源共享、云备份与恢复）
- 模组生态系统（API文档、兼容性测试、集成工具、模组交互框架）
- 性能分析工具（实时性能分析、瓶颈检测、优化建议系统）

### 4. 可扩展性

- 模块化设计
- 插件系统
- 事件系统
- 兼容性钩子
- 开发者API

## 安装和配置

### 安装

1. 将MCPS模组文件放入Minecraft的mods文件夹
2. 确保已安装Fabric Loader和Fabric API
3. 启动游戏，MCPS会自动初始化

### 配置

MCPS使用默认配置即可正常运行，无需额外配置。系统会根据硬件自动调整线程池大小。

## 使用方法

### 基本使用

MCPS会自动处理游戏逻辑的并行化，无需玩家进行任何操作。系统会在后台自动优化游戏性能。

### 雾管理命令
- `/fog cull enable`：启用雾剔除
- `/fog cull disable`：禁用雾剔除
- `/fog cull toggle`：切换雾剔除状态
- `/fog cull status`：查看雾剔除状态

### 云服务命令

- `/cloud backup create`：创建手动备份
- `/cloud backup list`：列出所有备份
- `/cloud backup restore <backupId>`：从指定备份恢复
- `/cloud backup cleanup`：清理旧备份
- `/cloud performance status`：查看性能状态
- `/cloud resource status`：查看资源状态

### 生态系统命令

- `/ecosystem api generate`：生成API文档
- `/ecosystem api list`：列出已注册的API类
- `/ecosystem compatibility test`：运行兼容性测试
- `/ecosystem compatibility report`：生成兼容性报告
- `/ecosystem mods list`：列出已注册的模组
- `/ecosystem mods info <modId>`：获取模组信息

### 性能分析命令

- `/performance analyze`：执行性能分析
- `/performance bottleneck detect`：检测性能瓶颈
- `/performance bottleneck list`：列出检测到的瓶颈
- `/performance suggestions generate`：生成优化建议
- `/performance suggestions list`：列出优化建议
- `/performance metrics reset`：重置性能指标
- `/performance metrics list`：列出性能指标

### 开发者API

#### 1. 任务调度

```java
// 获取任务调度器
TaskScheduler scheduler = MCPSMod.getInstance().getTaskScheduler();

// 调度任务
scheduler.scheduleTask("world_generation", () -> {
    // 任务逻辑
}, "task_name");

// 调度带返回值的任务
Future<Result> future = scheduler.scheduleTaskWithResult("entity_ai", () -> {
    // 任务逻辑
    return new Result();
}, "task_name");
```

#### 2. 线程通信

```java
// 获取线程通信系统
ThreadCommunication communication = MCPSMod.getInstance().getThreadCommunication();

// 发送消息
communication.sendMessage("main", "message_type", data);

// 接收消息
ThreadCommunication.Message message = communication.receiveMessage("main");
```

#### 3. 资源管理

```java
// 获取资源管理器
ResourceManager manager = MCPSMod.getInstance().getResourceManager();

// 注册资源
manager.registerResource("resource_name", resourceObject);

// 获取资源
ResourceType resource = manager.getResource("resource_name");

// 更新资源
manager.updateResource("resource_name", newResourceObject);
```

#### 4. 性能监控

```java
// 获取性能监控器
PerformanceMonitor monitor = MCPSMod.getInstance().getPerformanceMonitor();

// 记录任务时间
monitor.recordTaskTime("task_name", durationMs);

// 记录指标
monitor.recordMetric("metric_name", value);

// 记录线程活动
monitor.recordThreadActivity("thread_name", operations);
```

#### 5. 兼容性管理

```java
// 获取兼容性管理器
CompatibilityManager manager = MCPSMod.getInstance().getCompatibilityManager();

// 检查模组兼容性
boolean compatible = manager.isModCompatible("mod_id");

// 注册兼容性钩子
manager.registerCompatibilityHook("mod_id", () -> {
    // 兼容性处理逻辑
});
```

## 开发指南

### 1. 项目结构

```
src/main/java/com/qituo/mcps/
├── ai/                # AI 管理器
├── api/               # 模组 API
├── client/            # 客户端相关
├── cluster/           # 集群管理
├── compatibility/     # 兼容性管理
├── config/            # 配置管理
├── core/              # 核心功能
├── diagnostic/        # 诊断工具
├── error/             # 错误处理
├── gpu/               # GPU 管理
├── mixin/             # Mixin 注入
├── monitor/           # 性能监控
├── network/           # 网络管理
├── platform/          # 平台适配
├── render/            # 渲染管理
├── storage/           # 存储管理
├── task/              # 任务调度
├── test/              # 测试工具
├── thread/            # 线程管理
├── cloud/             # 云服务管理
└── integration/       # 模组生态系统
```

### 2. 扩展系统

#### 添加新的任务队列

```java
// 在TaskScheduler.initialize()方法中添加
createTaskQueue("new_task_type");
```

#### 注册兼容性钩子

```java
// 在模组初始化时注册
CompatibilityManager.COMPATIBILITY_CHECK.register(manager -> {
    // 兼容性检查和处理
});
```

#### 扩展游戏逻辑处理器

```java
// 创建自定义游戏逻辑处理器
public class CustomGameLogicProcessor extends GameLogicProcessor {
    @Override
    public void initialize(MinecraftServer server) {
        super.initialize(server);
        // 自定义初始化逻辑
    }
    
    // 重写或添加新的处理方法
}
```

#### 注册插件

```java
// 创建插件类
public class MyPlugin implements MCPSPlugin {
    @Override
    public void onLoad(MCPSAPI api) {
        // 插件初始化
    }
    
    @Override
    public void onUnload() {
        // 插件卸载
    }
}

// 注册插件
PluginManager.getInstance().registerPlugin(new MyPlugin());
```

### 3. 性能优化建议

- **任务粒度**：将大型任务拆分为 smaller, manageable subtasks
- **线程数**：根据硬件配置调整线程池大小
- **资源共享**：使用ResourceManager管理共享资源
- **通信开销**：减少线程间通信频率，使用批量处理
- **错误处理**：妥善处理线程异常，避免影响整个系统
- **渲染优化**：使用RenderManager优化渲染流程
- **存储优化**：使用StorageManager优化存储操作
- **网络优化**：使用NetworkManager优化网络通信

## 性能测试

### 测试场景

1. **单线程 vs 多线程**：比较不同线程配置下的性能差异
2. **大型红石机器**：测试复杂红石结构的处理性能
3. **大量实体**：测试大量实体同时活动时的性能
4. **世界生成**：测试区块生成和加载性能
5. **服务器负载**：测试多玩家同时在线时的性能
6. **渲染性能**：测试不同渲染设置下的性能

### 性能指标

- **TPS**：每秒tick数
- **MSPT**：每tick毫秒数
- **CPU利用率**：多核心CPU的利用情况
- **内存使用**：内存占用和垃圾回收情况
- **线程负载**：各线程的工作负载分布
- **GPU使用率**：GPU的利用情况
- **网络延迟**：网络通信延迟
- **存储IO**：存储读写性能

## 故障排除

### 常见问题

1. **线程死锁**
   - 症状：游戏卡住，CPU使用率高
   - 解决：检查资源锁定顺序，避免循环依赖
2. **内存泄漏**
   - 症状：内存使用持续增长
   - 解决：检查资源释放，使用内存分析工具
3. **性能下降**
   - 症状：游戏运行越来越慢
   - 解决：检查任务队列积压，优化任务处理逻辑
4. **模组冲突**
   - 症状：与其他模组不兼容
   - 解决：使用CompatibilityManager检查冲突，添加兼容性钩子
5. **世界加载缓慢**
   - 症状：加载世界时卡住或速度慢
   - 解决：检查StorageManager配置，优化存储操作

### 日志分析

MCPS会在游戏日志中输出详细的性能和错误信息，可通过分析日志定位问题。

## 未来计划

1. **更智能的任务调度算法**：基于实时负载动态调整任务分配
2. **GPU加速支持**：利用GPU进行更多计算密集型任务
3. **更多游戏逻辑的并行化**：扩展到更多游戏系统
4. **跨服务器负载均衡**：支持多服务器集群
5. **AI优化**：使用机器学习优化线程调度和实体AI
6. **更高级的渲染优化**：实现更细粒度的渲染任务分解
7. **动态资源分配**：根据系统负载动态调整资源分配
8. **自动化测试**：实现更完善的自动化测试系统
9. **更多平台支持**：支持更多Minecraft版本和模组加载器
10. **更详细的性能分析工具**：提供更详细的性能分析和优化建议

## 许可证

MCPS使用QSUP 1.0.0许可证，详见LICENSE文件。

## 贡献

欢迎提交问题和拉取请求，帮助改进MCPS系统。

## 联系方式

- 作者：Qituo, Yifei
- 项目地址：[Multi-core processing system](https://github.com/19136644525lxy/Multi-core-processing-system)

