# Multi-core processing system (MCPS)

一个为Minecraft设计的多线程处理系统，旨在通过并行处理优化游戏性能。

## 架构概述

MCPS采用分层架构设计，主要包含以下核心组件：

### 1. 核心组件

- **ThreadManager**：线程管理器，负责创建和管理工作线程池
- **TaskScheduler**：任务调度器，负责分配和管理游戏逻辑任务
- **ThreadCommunication**：线程通信系统，实现不同线程之间的安全通信
- **ResourceManager**：资源管理器，管理共享资源并提供线程安全的访问机制
- **GameLogicProcessor**：游戏逻辑处理器，实现核心游戏逻辑的并行处理
- **PerformanceMonitor**：性能监控器，收集和分析系统性能数据
- **ErrorHandler**：错误处理器，处理线程相关的错误和异常
- **CompatibilityManager**：兼容性管理器，确保与基础游戏和现有模组的兼容性

### 2. 架构层次

- **底层**：线程管理和并发工具
- **中层**：任务调度和资源管理
- **上层**：游戏逻辑处理和性能监控
- **接口层**：与Minecraft和其他模组的集成

## 系统特点

### 1. 高性能
- 基于线程池的并行处理
- 智能任务调度和负载均衡
- 优化的线程通信机制
- 实时性能监控和分析

### 2. 可靠性
- 完善的错误处理和恢复机制
- 线程安全的资源管理
- 防止数据竞争和死锁
- 稳定的系统运行保障

### 3. 兼容性
- 与基础Minecraft游戏完全兼容
- 支持与其他模组的集成
- 提供兼容性检查和钩子机制
- 可扩展性强，支持自定义扩展

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
├── MCPSMod.java              # 模组主类
├── ThreadManager.java        # 线程管理器
├── TaskScheduler.java        # 任务调度器
├── ThreadCommunication.java  # 线程通信系统
├── ResourceManager.java      # 资源管理器
├── GameLogicProcessor.java   # 游戏逻辑处理器
├── PerformanceMonitor.java   # 性能监控器
├── ErrorHandler.java         # 错误处理器
├── CompatibilityManager.java # 兼容性管理器
├── ConcurrencyUtils.java     # 并发工具类
└── MCPSTest.java            # 测试类
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

### 3. 性能优化建议

- **任务粒度**：将大型任务拆分为 smaller, manageable subtasks
- **线程数**：根据硬件配置调整线程池大小
- **资源共享**：使用ResourceManager管理共享资源
- **通信开销**：减少线程间通信频率，使用批量处理
- **错误处理**：妥善处理线程异常，避免影响整个系统

## 性能测试

### 测试场景
1. **单线程 vs 多线程**：比较不同线程配置下的性能差异
2. **大型红石机器**：测试复杂红石结构的处理性能
3. **大量实体**：测试大量实体同时活动时的性能
4. **世界生成**：测试区块生成和加载性能
5. **服务器负载**：测试多玩家同时在线时的性能

### 性能指标
- **TPS**：每秒tick数
- **MSPT**：每tick毫秒数
- **CPU利用率**：多核心CPU的利用情况
- **内存使用**：内存占用和垃圾回收情况
- **线程负载**：各线程的工作负载分布

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

### 日志分析
MCPS会在游戏日志中输出详细的性能和错误信息，可通过分析日志定位问题。

## 未来计划

1. **更智能的任务调度算法**：基于实时负载动态调整任务分配
2. **GPU加速支持**：利用GPU进行某些计算密集型任务
3. **更多游戏逻辑的并行化**：扩展到更多游戏系统
4. **跨服务器负载均衡**：支持多服务器集群
5. **AI优化**：使用机器学习优化线程调度

## 许可证

MCPS使用QSUP 1.0.0许可证，详见LICENSE文件。

## 贡献

欢迎提交问题和拉取请求，帮助改进MCPS系统。

## 联系方式

- 作者：Qituo, Yifei
- 项目地址：[Multi-core processing system](https://github.com/19136644525lxy/Multi-core-processing-system)
