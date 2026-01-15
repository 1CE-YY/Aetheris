# Aetheris RAG - API 设计原则和架构说明

## 概述

本文档描述 Aetheris RAG 系统的 API 设计原则、Controller 层职责划分以及前后端 API 对应关系。

---

## Controller 层职责划分

### ResourceController
**职责**：资源的 CRUD 操作和基本查询

**端点列表**：
- `POST /api/resources` - 上传资源
- `GET /api/resources` - 查询资源列表
- `GET /api/resources/{id}` - 查询资源详情
- `PUT /api/resources/{id}` - 更新资源信息
- `DELETE /api/resources/{id}` - 删除资源
- `DELETE /api/resources/batch` - 批量删除资源
- `GET /api/resources/{id}/chunks` - 查询资源的切片列表

**设计说明**：
- 仅负责资源的基本管理操作
- 不包含向量化相关功能（已移至 VectorController）
- 保持职责单一，易于理解和维护

**文件位置**：
- `backend/src/main/java/com/aetheris/rag/controller/ResourceController.java`

---

### VectorController
**职责**：向量化和向量索引管理

**端点列表**：
- `POST /api/vectors/resources/{id}/vectorize` - 向量化指定资源（仅向量化）
- `POST /api/vectors/resources/{id}/reprocess` - 重新处理资源（切片+向量化）
- `POST /api/vectors/batch-vectorize` - 批量向量化
- `POST /api/vectors/vectorize-all` - 向量化所有未向量化的切片
- `GET /api/vectors/progress` - 查询向量化进度

**设计说明**：
- 独立管理所有向量化相关功能
- 提供智能判断：仅向量化 vs 重新处理
- 清晰的 RESTful 路径结构
- 异步执行向量化任务，避免阻塞

**文件位置**：
- `backend/src/main/java/com/aetheris/rag/controller/VectorController.java`

**API 说明**：

#### 1. 向量化指定资源
```
POST /api/vectors/resources/{id}/vectorize
```
**功能**：向量化指定资源的所有切片（不重新切片）

**前置条件**：
- 资源必须存在
- 资源必须有切片（chunkCount > 0）

**返回结果**：
```json
{
  "code": 200,
  "message": "向量化任务已触发",
  "data": "向量化任务已触发"
}
```

#### 2. 重新处理资源
```
POST /api/vectors/resources/{id}/reprocess
```
**功能**：删除旧切片，重新解析文档，生成新切片，并向量化

**前置条件**：
- 资源必须存在

**返回结果**：
```json
{
  "code": 200,
  "message": "重新处理完成，已生成 N 个切片并向量化",
  "data": "重新处理完成，已生成 N 个切片并向量化"
}
```

#### 3. 查询向量化进度
```
GET /api/vectors/progress
```
**功能**：查询向量化进度统计

**返回结果**：
```json
{
  "code": 200,
  "data": {
    "totalChunks": 1000,
    "vectorizedCount": 850,
    "unvectorizedCount": 150,
    "progress": 85.0
  }
}
```

---

### AdminController
**职责**：系统维护和管理功能

**端点列表**：
- `POST /api/admin/system/full-rebuild` - 完全重建系统
- `POST /api/admin/system/cancel-rebuild` - 取消重建任务
- `GET /api/admin/system/rebuild-status` - 查询重建状态

**设计说明**：
- 使用 ProcessingService 而非直接使用 VectorService
- 避免强制类型转换，保持面向接口编程
- 通过委托模式实现功能解耦

**文件位置**：
- `backend/src/main/java/com/aetheris/rag/controller/AdminController.java`

**重要变更**：
- ❌ 移除了对 `VectorServiceImpl` 的强制类型转换
- ✅ 改为使用 `ProcessingService` 的委托方法
- ✅ 符合面向接口编程原则

---

## Service 层依赖关系

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        Controller 层                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Resource   │  │   Vector     │  │   Admin      │      │
│  │  Controller  │  │  Controller  │  │  Controller  │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         ▼                  ▼                  ▼              │
└─────────┼──────────────────┼──────────────────┼──────────────┘
          │                  │                  │
          │         ┌────────┴────────┐         │
          │         │                 │         │
          ▼         ▼                 ▼         ▼
┌─────────────────────────────────────────────────────────────┐
│                         Service 层                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Resource   │  │    Vector    │  │  Processing  │      │
│  │   Service    │  │    Service   │  │   Service    │      │
│  │              │  │              │  │              │      │
│  │ 资源CRUD     │  │ 向量化操作   │  │ 流程协调     │      │
│  │ 元数据管理   │  │ 索引管理     │  │ 复合操作     │      │
│  └──────────────┘  └──────────────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            ▼                                 │
│                 ┌──────────────────┐                         │
│                 │ DocumentService  │                         │
│                 │  (文档解析)      │                         │
│                 └──────────────────┘                         │
└─────────────────────────────────────────────────────────────┘
```

### 依赖原则

1. **单向依赖，无循环依赖**
   - Controller → Service
   - ProcessingService → {ResourceService, VectorService, DocumentService}
   - ResourceService、VectorService、DocumentService 互不依赖

2. **ProcessingService 作为唯一的协调者**
   - 复杂操作（上传、删除、重建）由 ProcessingService 协调
   - Controller 只调用对应的 Service，不直接协调多个 Service

3. **职责清晰**
   - ResourceService：资源 CRUD 和元数据管理
   - VectorService：向量化和索引管理
   - DocumentService：文档解析和切片生成
   - ProcessingService：流程协调

---

## API 路径设计原则

### 1. RESTful 路径设计
- API 路径反映资源类型和操作类型
- 使用名词复数形式表示资源（resources、vectors）
- 使用 HTTP 方法表示操作类型（GET、POST、PUT、DELETE）

### 2. 职责明确分离
- 不同功能的 API 放在不同的 Controller 中
- 避免单个 Controller 承担过多职责
- 通过路径前缀清晰划分功能模块（/api/resources、/api/vectors、/api/admin）

### 3. 版本化管理
- 通过路径结构清晰划分功能模块
- 便于未来版本演进（如 /api/v2/resources）

### 4. 协调层解耦
- 使用 ProcessingService 协调多个 Service
- 保持 Controller 层的简洁
- 避免在 Controller 层进行复杂业务逻辑

---

## 前后端 API 对应关系

| 前端 Service | 后端 Controller | API 路径前缀 | 功能说明 |
|-------------|---------------|-------------|---------|
| ResourceService | ResourceController | /api/resources | 资源 CRUD 操作 |
| VectorService | VectorController | /api/vectors | 向量化操作 |
| AuthService | AuthController | /api/auth | 用户认证 |
| ChatService | ChatController | /api/chat | RAG 问答 |
| - | AdminController | /api/admin | 系统管理 |

### 前端 Service 文件位置

| 前端 Service | 文件路径 |
|-------------|---------|
| ResourceService | `frontend/src/services/resource.service.ts` |
| VectorService | `frontend/src/services/vector.service.ts` |
| AuthService | `frontend/src/services/auth.service.ts` |
| ChatService | `frontend/src/services/chat.service.ts` |

---

## 重要架构变更说明

### 变更 1：创建 VectorController

**变更前**：
- 向量化功能分散在 ResourceController 和 AdminController 中
- API 路径不统一（/api/resources/{id}/vectorize）
- 职责不清晰

**变更后**：
- 创建独立的 VectorController
- API 路径统一为 /api/vectors/*
- 职责清晰，易于扩展

### 变更 2：移除强制类型转换

**变更前**：
```java
// AdminController.java
boolean cancelled = ((VectorServiceImpl) vectorService).cancelRebuild();
```

**变更后**：
```java
// AdminController.java
boolean cancelled = processingService.cancelRebuild();
```

**收益**：
- 符合面向接口编程原则
- 避免强制类型转换
- 提高代码可维护性

### 变更 3：前端智能判断

**变更前**：
- 前端调用 `POST /api/resources/{id}/vectorize`
- 需要用户手动判断是否需要重新处理

**变更后**：
- 前端根据 `resource.chunkCount` 智能选择操作
- 有切片：调用 `VectorService.vectorizeResource()`
- 无切片：调用 `VectorService.reprocessResource()`

**代码示例**：
```typescript
const handleRevectorize = async () => {
  const hasChunks = resource.value.chunkCount > 0

  if (hasChunks) {
    await VectorService.vectorizeResource(resource.value.id)
  } else {
    await VectorService.reprocessResource(resource.value.id)
  }
}
```

---

## 扩展指南

### 如何添加新的向量化相关 API

1. **后端**：
   - 在 `VectorController.java` 中添加新的端点
   - 在 `VectorService.java` 接口中添加方法签名
   - 在 `VectorServiceImpl.java` 中实现方法

2. **前端**：
   - 在 `frontend/src/services/vector.service.ts` 中添加静态方法
   - 在对应的 Vue 组件中调用 `VectorService` 方法

### 如何添加新的资源管理 API

1. **后端**：
   - 在 `ResourceController.java` 中添加新的端点
   - 在 `ResourceService.java` 接口中添加方法签名
   - 在 `ResourceServiceImpl.java` 中实现方法

2. **前端**：
   - 在 `frontend/src/services/resource.service.ts` 中添加静态方法
   - 在对应的 Vue 组件中调用 `ResourceService` 方法

---

## 循环依赖问题修复

### 问题描述

在重构过程中，出现了以下循环依赖：

```
adminController
┌─────┐
|  processingServiceImpl
↑     ↓
|  vectorServiceImpl (field private ProcessingService)
└─────┘
```

**根本原因**：
- `ProcessingServiceImpl` 通过构造函数注入 `VectorService`
- `VectorServiceImpl` 通过 `@Autowired` 字段注入 `ProcessingService`（但实际未使用）
- 同时还通过 `@Autowired` 注入了 `DocumentService`（实际已使用）

### 解决方案

1. **移除未使用的依赖**：
   - 移除 `VectorServiceImpl` 中的 `ProcessingService` 依赖（第 69-70 行）
   - 该依赖从未被使用，是冗余的

2. **改为构造函数注入**：
   - 将 `ResourceService` 和 `DocumentService` 改为通过 `@RequiredArgsConstructor` 构造函数注入
   - 移除 `@Autowired` 注解

3. **依赖链重构后的结构**：
   ```
   ProcessingServiceImpl → VectorService
   VectorServiceImpl → DocumentService
   DocumentServiceImpl → {PdfProcessor, MarkdownProcessor}
   ```
   - 依赖链是单向的，无循环
   - `DocumentServiceImpl` 只依赖独立的处理器，不会造成循环依赖

### 修改后的代码

**VectorServiceImpl.java**:
```java
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

  private final ChunkMapper chunkMapper;
  private final ResourceMapper resourceMapper;
  private final EmbeddingGateway embeddingGateway;
  private final StringRedisTemplate redisTemplate;
  private final RedissonClient redissonClient;
  private final ResourceService resourceService;
  private final DocumentService documentService;  // 通过构造函数注入

  // 移除了 @Autowired private ProcessingService processingService;
  // 移除了 @Autowired private ResourceService resourceService;
  // 移除了 @Autowired private DocumentService documentService;
}
```

### 最佳实践

1. **使用构造函数注入**：
   - 优先使用 `@RequiredArgsConstructor` 进行构造函数注入
   - 避免使用 `@Autowired` 字段注入
   - 所有依赖声明为 `final`

2. **定期检查依赖关系**：
   - 使用 Spring Boot 的启动报告识别循环依赖
   - 定期审查 Service 层的依赖关系

3. **移除未使用的依赖**：
   - 及时清理未使用的依赖注入
   - 避免为了"可能需要"而提前注入依赖

---

## 总结

### 优化收益

1. **✅ 消除强制类型转换**：AdminController 面向接口编程
2. **✅ 职责清晰化**：VectorController 独立管理向量化操作
3. **✅ RESTful API 设计**：API 路径清晰反映功能模块
4. **✅ 易于扩展**：未来向量化相关功能都在 VectorController 中
5. **✅ 前后端职责对应**：前端 VectorService 对应后端 VectorController
6. **✅ 架构文档化**：创建 API 设计原则文档，便于团队理解

### 最佳实践

1. **遵循单一职责原则**：每个 Controller 只负责一个领域的功能
2. **面向接口编程**：避免强制类型转换
3. **使用委托模式**：通过 ProcessingService 协调复杂操作
4. **保持 API 路径一致性**：使用清晰的路径结构
5. **前后端分离**：前端 Service 类对应后端 Controller

---

**文档版本**：v1.0.0
**最后更新**：2026-01-15
**作者**：Aetheris RAG Team
