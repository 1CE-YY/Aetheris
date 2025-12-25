# 数据模型设计

**功能**：学习资源检索与推荐 RAG 系统
**分支**：`001-rag-recommendation-system`
**日期**：2025-12-25
**技术栈**：MySQL 8 + Redis Stack + MyBatis + Lombok

## 概述

本文档定义系统的数据模型，包括 MySQL 关系型数据库表结构、Redis 向量索引 schema、实体类定义（使用 Lombok 注解）、以及 MyBatis Mapper 接口。

## 实体关系图（ERD）

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│    User     │ 1     N │ UserBehavior │ N     1 │  Resource   │
│  (用户)      │────────│  (行为记录)    │────────│  (学习资源)  │
└─────────────┘         └──────────────┘         └─────────────┘
       │                                                    │
       │ 1                                                1 │
       │                                                    │ N
┌──────────────┐                                  ┌─────────────┐
│ UserProfile  │                                  │   Chunk     │
│ (用户画像)    │                                  │  (切片)      │
└──────────────┘                                  └─────────────┘
                                                          │
                                                          │ N
                                                          │
                                                  ┌──────────────┐
                                                  │ EvalQuery    │
                                                  │(测试查询)     │
                                                  └──────────────┘

其他表：
- eval_runs（评测运行记录）
- recommendations（推荐结果快照，可选）
```

## MySQL 表结构

### 1. users（用户表）

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希（BCrypt）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_active_at TIMESTAMP NULL COMMENT '最后活跃时间',
    INDEX idx_email (email),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

### 2. resources（学习资源表）

```sql
CREATE TABLE resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '资源ID',
    title VARCHAR(200) NOT NULL COMMENT '资源标题',
    tags VARCHAR(500) COMMENT '标签/课程方向（逗号分隔，如：机器学习,深度学习）',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型（PDF/MARKDOWN）',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT COMMENT '文件大小（字节）',
    description TEXT COMMENT '资源描述',
    content_hash VARCHAR(64) NOT NULL UNIQUE COMMENT '内容哈希（SHA-256，用于去重）',
    uploaded_by BIGINT NOT NULL COMMENT '上传者用户ID',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    chunk_count INT DEFAULT 0 COMMENT '切片数量',
    vectorized BOOLEAN DEFAULT FALSE COMMENT '是否已向量化',
    INDEX idx_content_hash (content_hash),
    INDEX idx_uploaded_by (uploaded_by),
    INDEX idx_tags (tags(100)),
    INDEX idx_upload_time (upload_time),
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资源表';
```

### 3. resource_chunks（资源切片表）

```sql
CREATE TABLE resource_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '切片ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    chunk_index INT NOT NULL COMMENT '切片序号（从0开始）',
    chunk_text TEXT NOT NULL COMMENT '切片文本内容',
    location_info VARCHAR(500) COMMENT '定位信息（PDF页码范围/MD章节路径）',
    page_start INT COMMENT 'PDF起始页码（可选）',
    page_end INT COMMENT 'PDF结束页码（可选）',
    chapter_path VARCHAR(500) COMMENT 'Markdown章节路径（如：第一章>1.1节）',
    text_hash VARCHAR(64) NOT NULL COMMENT '文本哈希（SHA-256，用于Embedding缓存）',
    vectorized BOOLEAN DEFAULT FALSE COMMENT '是否已向量化',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_resource_chunk (resource_id, chunk_index),
    INDEX idx_resource_id (resource_id),
    INDEX idx_text_hash (text_hash),
    INDEX idx_vectorized (vectorized),
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源切片表';
```

### 4. user_behaviors（用户行为表）

```sql
CREATE TABLE user_behaviors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '行为ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    behavior_type VARCHAR(20) NOT NULL COMMENT '行为类型（QUERY/CLICK/FAVORITE）',
    resource_id BIGINT COMMENT '资源ID（CLICK/FAVORITE行为有值）',
    query_text TEXT COMMENT '查询文本（QUERY行为有值）',
    behavior_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '行为时间',
    session_id VARCHAR(100) COMMENT '会话ID（可选，用于分析）',
    INDEX idx_user_id (user_id),
    INDEX idx_behavior_time (behavior_time),
    INDEX idx_behavior_type (behavior_type),
    INDEX idx_user_time (user_id, behavior_time),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为表';
```

### 5. user_profiles（用户画像表）

```sql
CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '画像ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    profile_vector JSON NOT NULL COMMENT '画像向量（JSON数组，浮点数列表）',
    window_size INT DEFAULT 10 COMMENT '行为窗口大小（最近N次行为）',
    query_count INT DEFAULT 0 COMMENT '查询行为次数',
    click_count INT DEFAULT 0 COMMENT '点击行为次数',
    favorite_count INT DEFAULT 0 COMMENT '收藏行为次数',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_updated_at (updated_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像表';
```

**说明**：`profile_vector` 使用 MySQL 8 的 JSON 类型存储浮点数数组，例如 `[0.123, -0.456, 0.789, ...]`。虽然 Redis 存储向量索引，MySQL 画像用于备份和离线分析。

### 6. eval_queries（评测查询表）

```sql
CREATE TABLE eval_queries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '查询ID',
    query_id VARCHAR(50) NOT NULL UNIQUE COMMENT '查询标识（如 q001）',
    query_text TEXT NOT NULL COMMENT '查询文本',
    relevant_resources JSON NOT NULL COMMENT '相关资源ID列表（JSON数组）',
    created_by VARCHAR(100) DEFAULT 'system' COMMENT '创建者',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_query_id (query_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评测查询表';
```

**示例**：`relevant_resources` 字段存储 `["resource-uuid-001", "resource-uuid-005"]`

### 7. eval_runs（评测运行记录表）

```sql
CREATE TABLE eval_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '运行ID',
    run_name VARCHAR(200) NOT NULL COMMENT '运行名称',
    config JSON NOT NULL COMMENT '参数配置（chunkSize、overlap、topK等）',
    use_profile BOOLEAN DEFAULT FALSE COMMENT '是否使用用户画像',
    metrics JSON NOT NULL COMMENT '性能指标（Precision@K、Recall@K、平均时延、P95时延）',
    run_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '运行时间',
    INDEX idx_run_time (run_time),
    INDEX idx_use_profile (use_profile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评测运行记录表';
```

**示例**：
```json
{
  "config": {
    "chunkSize": 1000,
    "overlap": 200,
    "topK": 5,
    "embeddingModel": "embedding-v2",
    "chatModel": "glm-4-flash"
  },
  "metrics": {
    "precision@5": 0.65,
    "recall@10": 0.72,
    "avgLatencyMs": 2300,
    "p95LatencyMs": 3800
  }
}
```

### 8. recommendations（推荐结果快照表，可选）

```sql
CREATE TABLE recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '推荐ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resource_id BIGINT NOT NULL COMMENT '推荐资源ID',
    reason TEXT COMMENT '推荐理由',
    suggestion TEXT COMMENT '学习建议',
    score DOUBLE COMMENT '推荐分数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐结果快照表（可选，用于离线分析）';
```

## Redis 向量索引 Schema

### RediSearch Vector Index

```redis
# 创建 chunk 向量索引
FT.CREATE chunk_idx ON JSON
  PREFIX 1 chunk:
  SCHEMA
    $.embedding VECTOR HNSW 6 {
      "TYPE": "FLOAT32",
      "DIM": 1536,           # 智谱 AI Embedding 维度
      "DISTANCE_METRIC": "COSINE",
      "INITIAL_CAP": 1000,
      "M": 16,
      "EF_CONSTRUCTION": 200
    }
    $.resourceId NUMERIC
    $.chunkId NUMERIC
    $.chunkIndex NUMERIC
    $.docType TAG
    $.tags TAG
```

### Redis 数据结构

#### 1. Chunk 向量数据

```
Key: chunk:{chunkId}
Value (JSON):
{
  "embedding": [0.123, -0.456, ...],  # 1536 维浮点数数组
  "resourceId": 123,
  "chunkId": 456,
  "chunkIndex": 5,
  "docType": "pdf",
  "tags": ["machine-learning", "rag"]
}
```

#### 2. Embedding 缓存

```
Key: embedding:{sha256(normalizedText)}
Value (JSON):
{
  "embedding": [0.123, -0.456, ...],
  "modelName": "embedding-v2",
  "createdAt": 1735104000000
}
TTL: 30 天
```

#### 3. TopK 检索结果缓存

```
Key: search:{sha256(query)}:{topK}:{filtersHash}:{version}
Value (JSON):
{
  "chunks": [
    {
      "chunkId": 456,
      "resourceId": 123,
      "score": 0.85
    }
  ],
  "cachedAt": 1735104000000
}
TTL: 1 小时（可配置）
```

#### 4. 用户画像缓存（可选）

```
Key: profile:{userId}
Value (JSON):
{
  "profileVector": [0.123, -0.456, ...],
  "windowSize": 10,
  "updatedAt": 1735104000000
}
TTL: 7 天
```

## 实体类定义（使用 Lombok）

### User.java

```java
package com.aetheris.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastActiveAt;
}
```

### Resource.java

```java
package com.aetheris.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 学习资源实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    private Long id;
    private String title;
    private String tags;              // 逗号分隔："机器学习,深度学习"
    private String fileType;          // PDF, MARKDOWN
    private String filePath;
    private Long fileSize;
    private String description;
    private String contentHash;       // SHA-256
    private Long uploadedBy;
    private Instant uploadTime;
    private Integer chunkCount;
    private Boolean vectorized;
}
```

### Chunk.java

```java
package com.aetheris.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 资源切片实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {
    private Long id;
    private Long resourceId;
    private Integer chunkIndex;
    private String chunkText;
    private String locationInfo;      // "PDF 第 12-14 页" / "第一章>1.1节"
    private Integer pageStart;
    private Integer pageEnd;
    private String chapterPath;
    private String textHash;          // SHA-256
    private Boolean vectorized;
    private Instant createdAt;
}
```

### UserBehavior.java

```java
package com.aetheris.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户行为实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehavior {
    private Long id;
    private Long userId;
    private BehaviorType behaviorType; // QUERY, CLICK, FAVORITE
    private Long resourceId;
    private String queryText;
    private Instant behaviorTime;
    private String sessionId;

    public enum BehaviorType {
        QUERY, CLICK, FAVORITE
    }
}
```

### UserProfile.java

```java
package com.aetheris.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 用户画像实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private Long id;
    private Long userId;
    private List<Double> profileVector;  // 画像向量（JSON存储）
    private Integer windowSize;
    private Integer queryCount;
    private Integer clickCount;
    private Integer favoriteCount;
    private Instant updatedAt;
}
```

### EvalQuery.java

```java
package com.aetheris.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 评测查询实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalQuery {
    private Long id;
    private String queryId;            // "q001"
    private String queryText;
    private List<String> relevantResources;  // 相关资源ID列表
    private String createdBy;
    private Instant createdAt;
}
```

### EvalRun.java

```java
package com.aetheris.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 评测运行记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalRun {
    private Long id;
    private String runName;
    private Map<String, Object> config;   // 参数配置
    private Boolean useProfile;
    private Map<String, Object> metrics;  // 性能指标
    private Instant runTime;
}
```

## MyBatis Mapper 接口

### UserMapper.java

```java
package com.aetheris.rag.mapper;

import com.aetheris.rag.model.User;
import org.apache.ibatis.annotations.*;

import java.time.Instant;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO users (username, email, password_hash, created_at, updated_at) " +
            "VALUES (#{username}, #{email}, #{passwordHash}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM users WHERE email = #{email}")
    User findByEmail(String email);

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Update("UPDATE users SET last_active_at = #{lastActiveAt} WHERE id = #{id}")
    int updateLastActiveAt(@Param("id") Long id, @Param("lastActiveAt") Instant lastActiveAt);
}
```

### ResourceMapper.java

```java
package com.aetheris.rag.mapper;

import com.aetheris.rag.model.Resource;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ResourceMapper {

    @Insert("INSERT INTO resources (title, tags, file_type, file_path, file_size, description, " +
            "content_hash, uploaded_by, upload_time) " +
            "VALUES (#{title}, #{tags}, #{fileType}, #{filePath}, #{fileSize}, #{description}, " +
            "#{contentHash}, #{uploadedBy}, #{uploadTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Resource resource);

    @Select("SELECT * FROM resources WHERE id = #{id}")
    Resource findById(Long id);

    @Select("SELECT * FROM resources WHERE content_hash = #{contentHash}")
    Resource findByContentHash(String contentHash);

    @Select("SELECT * FROM resources WHERE uploaded_by = #{userId} ORDER BY upload_time DESC LIMIT #{limit}")
    List<Resource> findByUploader(@Param("userId") Long userId, @Param("limit") int limit);

    @Update("UPDATE resources SET chunk_count = #{chunkCount}, vectorized = #{vectorized} WHERE id = #{id}")
    int updateChunkStatus(Resource resource);

    @Select("SELECT * FROM resources ORDER BY upload_time DESC LIMIT #{offset}, #{limit}")
    List<Resource> listPaged(@Param("offset") int offset, @Param("limit") int limit);
}
```

### ChunkMapper.java

```java
package com.aetheris.rag.mapper;

import com.aetheris.rag.model.Chunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChunkMapper {

    @Insert("INSERT INTO resource_chunks (resource_id, chunk_index, chunk_text, location_info, " +
            "page_start, page_end, chapter_path, text_hash, vectorized) " +
            "VALUES (#{resourceId}, #{chunkIndex}, #{chunkText}, #{locationInfo}, " +
            "#{pageStart}, #{pageEnd}, #{chapterPath}, #{textHash}, #{vectorized})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Chunk chunk);

    @Select("SELECT * FROM resource_chunks WHERE id = #{id}")
    Chunk findById(Long id);

    @Select("SELECT * FROM resource_chunks WHERE resource_id = #{resourceId} ORDER BY chunk_index")
    List<Chunk> findByResourceId(Long resourceId);

    @Select("SELECT * FROM resource_chunks WHERE text_hash = #{textHash} LIMIT 1")
    Chunk findByTextHash(String textHash);

    @Select("SELECT * FROM resource_chunks WHERE resource_id = #{resourceId} AND vectorized = FALSE LIMIT #{limit}")
    List<Chunk> findUnvectorizedByResourceId(@Param("resourceId") Long resourceId, @Param("limit") int limit);

    @Update("UPDATE resource_chunks SET vectorized = TRUE WHERE id = #{id}")
    int markVectorized(Long id);

    @Delete("DELETE FROM resource_chunks WHERE resource_id = #{resourceId}")
    int deleteByResourceId(Long resourceId);
}
```

### UserBehaviorMapper.java

```java
package com.aetheris.rag.mapper;

import com.aetheris.rag.model.UserBehavior;
import org.apache.ibatis.annotations.*;

import java.time.Instant;
import java.util.List;

@Mapper
public interface UserBehaviorMapper {

    @Insert("INSERT INTO user_behaviors (user_id, behavior_type, resource_id, query_text, behavior_time, session_id) " +
            "VALUES (#{userId}, #{behaviorType}, #{resourceId}, #{queryText}, #{behaviorTime}, #{sessionId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserBehavior behavior);

    @Select("SELECT * FROM user_behaviors WHERE user_id = #{userId} AND behavior_type = 'QUERY' " +
            "ORDER BY behavior_time DESC LIMIT #{limit}")
    List<UserBehavior> findRecentQueries(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT * FROM user_behaviors WHERE user_id = #{userId} " +
            "AND behavior_time >= #{startTime} ORDER BY behavior_time DESC")
    List<UserBehavior> findByUserIdAndTimeRange(@Param("userId") Long userId,
                                                @Param("startTime") Instant startTime);

    @Select("SELECT COUNT(*) FROM user_behaviors WHERE user_id = #{userId} AND behavior_type = #{type}")
    int countByType(@Param("userId") Long userId, @Param("type") UserBehavior.BehaviorType type);
}
```

### UserProfileMapper.java

```java
package com.aetheris.rag.mapper;

import com.aetheris.rag.model.UserProfile;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserProfileMapper {

    @Insert("INSERT INTO user_profiles (user_id, profile_vector, window_size, query_count, " +
            "click_count, favorite_count) " +
            "VALUES (#{userId}, #{profileVector}, #{windowSize}, #{queryCount}, " +
            "#{clickCount}, #{favoriteCount}) " +
            "ON DUPLICATE KEY UPDATE " +
            "profile_vector = VALUES(profile_vector), " +
            "window_size = VALUES(window_size), " +
            "query_count = VALUES(query_count), " +
            "click_count = VALUES(click_count), " +
            "favorite_count = VALUES(favorite_count), " +
            "updated_at = CURRENT_TIMESTAMP")
    int upsert(UserProfile profile);

    @Select("SELECT * FROM user_profiles WHERE user_id = #{userId}")
    UserProfile findByUserId(Long userId);

    @Update("UPDATE user_profiles SET query_count = query_count + 1 WHERE user_id = #{userId}")
    int incrementQueryCount(Long userId);

    @Update("UPDATE user_profiles SET click_count = click_count + 1 WHERE user_id = #{userId}")
    int incrementClickCount(Long userId);
}
```

### EvalMapper.java

```java
package com.aetheris.rag.mapper;

import com.aetheris.rag.model.EvalQuery;
import com.aetheris.rag.model.EvalRun;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EvalMapper {

    @Insert("INSERT INTO eval_queries (query_id, query_text, relevant_resources, created_by) " +
            "VALUES (#{queryId}, #{queryText}, #{relevantResources}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertQuery(EvalQuery query);

    @Select("SELECT * FROM eval_queries ORDER BY id")
    List<EvalQuery> findAllQueries();

    @Insert("INSERT INTO eval_runs (run_name, config, use_profile, metrics) " +
            "VALUES (#{runName}, #{config}, #{useProfile}, #{metrics})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRun(EvalRun run);
}
```

## 数据完整性规则

### 唯一约束

1. **users.email**：邮箱唯一
2. **users.username**：用户名唯一
3. **resources.content_hash**：内容哈希唯一（防止重复上传）
4. **resource_chunks.(resource_id, chunk_index)**：同一资源的切片序号唯一
5. **user_profiles.user_id**：每个用户只有一个画像
6. **eval_queries.query_id**：查询ID唯一

### 外键约束

1. **resources.uploaded_by → users.id**：级联删除（用户删除时，其上传的资源也删除）
2. **resource_chunks.resource_id → resources.id**：级联删除（资源删除时，切片也删除）
3. **user_behaviors.user_id → users.id**：级联删除
4. **user_behaviors.resource_id → resources.id**：置空（资源删除时，行为记录保留但资源ID置空）
5. **user_profiles.user_id → users.id**：级联删除
6. **recommendations.user_id → users.id**：级联删除
7. **recommendations.resource_id → resources.id**：级联删除

### 索引策略

#### 查询优化索引

- **users.email、users.username**：登录查询优化
- **resources.content_hash**：去重检查优化
- **resources.uploaded_by、resources.upload_time**：用户资源列表查询
- **resource_chunks.resource_id、resource_chunks.vectorized**：切片查询和向量化状态查询
- **user_behaviors.(user_id, behavior_time)**：用户行为时间范围查询（画像更新）
- **user_behaviors.behavior_type**：按类型统计行为次数
- **user_profiles.updated_at**：画像更新时间查询
- **eval_runs.run_time、eval_runs.use_profile**：评测历史查询

## 数据迁移策略

### Flyway 迁移脚本

**V1__init_schema.sql**：包含上述所有表的 CREATE 语句

**V2__add_indexes.sql**：补充索引（如果需要）

**V3__add_recommendations_table.sql**：可选的推荐结果快照表

### 迁移执行

在 `application.yml` 中配置：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

## 总结

本数据模型设计遵循以下原则：

1. **规范化设计**：遵循第三范式，避免数据冗余
2. **性能优化**：合理使用索引，支持高频查询场景
3. **可扩展性**：使用 JSON 类型存储灵活数据（画像向量、评测配置）
4. **数据完整性**：唯一约束、外键约束、级联删除策略
5. **企业级实践**：标准 MyBatis（不使用 MyBatis-Plus）、Lombok、Guava
6. **宪章合规**：
   - MySQL 存储结构化数据，Redis 存储向量与缓存（原则二）
   - content_hash、text_hash 用于幂等和缓存（原则三）
   - 保留完整的定位信息（page_start、page_end、chapter_path）用于可追溯（原则五）

所有表结构支持 MVP 阶段的核心功能，并为后续迭代（推荐快照、画像优化）预留扩展空间。
