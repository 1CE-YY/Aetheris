# 技术研究与决策

**功能**：学习资源检索与推荐 RAG 系统
**分支**：`001-rag-recommendation-system`
**日期**：2025-12-25

## 概述

本文档记录了实施计划 Phase 0 中的技术研究任务及其决策结果。每个研究任务包含：决策、理由、替代方案。

## 研究任务列表

### 1. LangChain4j 与 Redis Stack 集成最佳实践

#### 决策

使用 **LangChain4j 0.29+** 的 `RedisEmbeddingStore` 实现，配合 **Redis Stack 7.2+** 的 RediSearch 模块进行向量索引和相似度搜索。

#### 技术方案

**Redis 向量索引 Schema**：
```java
// 使用 Jedis 或 Lettuce 客户端创建索引
FT.CREATE chunk_idx ON JSON
  PREFIX 1 chunk:
  SCHEMA
    $.embedding VECTOR HNSW 6 {
      "TYPE": "FLOAT32",
      "DIM": 1536,          // 智谱 AI Embedding 维度
      "DISTANCE_METRIC": "COSINE",
      "INITIAL_CAP": 1000,
      "M": 16,
      "EF_CONSTRUCTION": 200
    }
    $.resourceId TAG
    $.chunkId TAG
    $.chunkIndex NUMERIC
    $.docType TAG
    $.tags TAG
```

**LangChain4j 集成配置**：
```java
EmbeddingStore embeddingStore = RedisEmbeddingStore.builder()
    .host("localhost")
    .port(6379)
    .indexName("chunk_idx")
    .prefix("chunk:")
    .build();

// 存储向量
Embedding embedding = embeddingModel.embed(text).content();
String id = UUID.randomUUID().toString();
embeddingStore.add(id, embedding, EmbeddingMatch.from(id, embedding, text));

// 相似度搜索
List<EmbeddingMatch<Text>> relevant = embeddingStore.findRelevant(
    queryEmbedding,
    topK,             // K 值
    minScore          // 最小相似度阈值（可选）
);
```

#### 理由

- **性能**：Redis Stack 的 HNSW 算法提供 O(log n) 搜索复杂度，适合小规模场景（100-1000 资源）
- **简化架构**：Redis 同时承担向量存储和缓存，避免引入第二套向量数据库（符合宪章二原则）
- **LangChain4j 生态**：LangChain4j 提供开箱即用的 Redis EmbeddingStore，减少集成工作量
- **可维护性**：Redis 是运维团队熟悉的中间件，监控和备份工具成熟

#### 替代方案

- **Pinecone/Weaviate/Qdrant**：专门的向量数据库，性能优秀但增加运维复杂度，违反宪章二原则（不引入第二套向量库）
- **PostgreSQL + pgvector**：可以统一存储，但向量搜索性能不如 Redis，且 MySQL 已是结构化存储（宪章二要求 MySQL + Redis）

---

### 2. 智谱 AI API 集成方案

#### 决策

使用 **LangChain4j 的 ZhipuAiEmbeddingModel** 和 **ZhipuAiStreamingChatModel**，通过统一的 ModelGateway 封装调用。

#### 技术方案

**ModelGateway 配置参数**（application.yml）：
```yaml
model-gateway:
  embedding:
    model-name: "embedding-3"        # 智谱 AI Embedding 模型
    base-url: "https://open.bigmodel.cn/api/paas/v4/"
    timeout: 30s                      # 超时时间
    max-tokens: 8192                  # 最大输入长度
    retry:
      max-attempts: 3                 # 最大重试次数
      backoff: 1s                     # 退避时间（指数增长）
      retry-on:                       # 重试条件
        - 429                         # 限流
        - 500                         # 服务器错误
        - 502                         # 网关错误
        - 503                         # 服务不可用
    rate-limit:
      permits-per-second: 10          # 并发限流
  chat:
    model-name: "glm-4.5-flash"         # 智谱 AI Chat 模型（高性能免费）
    temperature: 0.7                  # 创造性（0-1）
    top-p: 0.9
    max-tokens: 2048
    timeout: 60s
    retry:
      max-attempts: 2                 # Chat 重试次数较少（避免重复生成）
      backoff: 2s
      retry-on: [429, 500, 502, 503]
    rate-limit:
      permits-per-second: 5
```

**重试策略实现**：
```java
public class ModelRetryStrategy {

  private final int maxAttempts;
  private final Duration backoff;

  public <T> T executeWithRetry(Supplier<T> operation) {
    int attempts = 0;
    Exception lastException = null;

    while (attempts < maxAttempts) {
      try {
        return operation.get();
      } catch (ApiException e) {
        lastException = e;
        if (shouldRetry(e.getStatusCode())) {
          attempts++;
          Duration delay = backoff.multipliedBy((long) Math.pow(2, attempts - 1));
          Thread.sleep(delay.toMillis());
        } else {
          break;
        }
      }
    }

    throw new ModelException("API 调用失败，已重试 " + attempts + " 次", lastException);
  }

  private boolean shouldRetry(int statusCode) {
    return statusCode == 429 || statusCode == 500 || statusCode == 502
        || statusCode == 503;
  }
}
```

**日志脱敏实现**：
```java
public class LogSanitizer {

  private static final int MAX_INPUT_LENGTH = 200;

  public static String sanitize(String input) {
    if (input == null) {
      return null;
    }
    return input.length() > MAX_INPUT_LENGTH ? input.substring(0, MAX_INPUT_LENGTH)
        + "...[截断]" : input;
  }

  public static String maskApiKey(String apiKey) {
    return apiKey == null ? null : apiKey.substring(0, 8) + "****";
  }
}

// 使用示例
log.info("调用 Embedding API: model={}, input={}, timeout={}ms",
    modelName,
    LogSanitizer.sanitize(userInput),
    timeout.toMillis());
// 不记录 API key 或 Authorization 头
```

#### 理由

- **LangChain4j 内置支持**：ZhipuAi 模型已集成，无需手写 HTTP 客户端
- **统一配置管理**：通过 application.yml 集中管理所有模型参数（符合宪章四原则）
- **成本控制**：glm-4.5-flash 是高性能免费模型，成本为零，性能更强
- **降级策略**：Chat API 不可用时返回检索结果 + 引用摘要，不导致完全失败（符合宪章四原则）

#### 替代方案

- **直接调用智谱 AI HTTP API**：需要手写重试、限流、日志脱敏逻辑，增加开发工作量
- **使用其他 LLM（如 OpenAI GPT-4）**：成本高，且国内访问可能不稳定

---

### 3. PDF 文本提取与分页信息保留

#### 决策

使用 **Apache PDFBox 3.0** 逐页提取文本，记录每个 chunk 的起始页码和结束页码。

#### 技术方案

**PDF 解析服务**：
```java
public class PdfDocumentProcessor {

  private static final int CHUNK_SIZE = 1000;  // 字符数
  private static final int OVERLAP = 200;      // 重叠字符数

  public List<Chunk> processPdf(Path filePath, String resourceId) throws IOException {
    List<Chunk> chunks = new ArrayList<>();
    try (PDDocument document = PDDocument.load(filePath.toFile())) {
      int totalPages = document.getNumberOfPages();

      // 第一步：提取所有页的文本
      List<PageContent> pages = new ArrayList<>();
      for (int pageNum = 0; pageNum < totalPages; pageNum++) {
        PDPage page = document.getPage(pageNum);
        String text = new PDFTextStripper().getText(document);
        // 提取单页文本（需要设置 startPage 和 endPage）
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageNum + 1);
        stripper.setEndPage(pageNum + 1);
        String pageText = stripper.getText(document).trim();
        pages.add(new PageContent(pageNum + 1, pageText));
      }

      // 第二步：合并页面文本并切分，同时记录页码范围
      StringBuilder buffer = new StringBuilder();
      int chunkIndex = 0;
      int startPage = 1;
      int currentPage = 1;

      for (PageContent pageContent : pages) {
        buffer.append(pageContent.text).append("\n");
        currentPage = pageContent.pageNum;

        // 当缓冲区达到 CHUNK_SIZE 时，创建 chunk
        if (buffer.length() >= CHUNK_SIZE) {
          String chunkText = buffer.substring(0, CHUNK_SIZE);
          String remaining = buffer.substring(CHUNK_SIZE);

          chunks.add(new Chunk(
              UUID.randomUUID().toString(),
              resourceId,
              chunkText,
              chunkIndex++,
              String.format("PDF 第 %d-%d 页", startPage, currentPage),
              "pdf"
          ));

          // 重置缓冲区，保留重叠部分
          buffer = new StringBuilder(remaining);
          if (remaining.length() < OVERLAP) {
            buffer.append("\n");
          }
          startPage = currentPage;  // 更新起始页
        }
      }

      // 处理最后剩余的文本
      if (buffer.length() > 0) {
        chunks.add(new Chunk(
            UUID.randomUUID().toString(),
            resourceId,
            buffer.toString(),
            chunkIndex,
            String.format("PDF 第 %d-%d 页", startPage, currentPage),
            "pdf"
        ));
      }
    }

    return chunks;
  }

  // 使用 Lombok @Data 而非 Java 21 Record
  @Data
  @AllArgsConstructor
  private static class PageContent {
    private int pageNum;
    private String text;
  }
}
```

#### 理由

- **页码精确追踪**：逐页提取后合并切分，可以准确定位每个 chunk 的页码范围
- **成熟稳定**：Apache PDFBox 是 Apache 顶级项目，社区活跃，文档完善
- **Java 生态**：与 Spring Boot 技术栈一致，无需跨语言集成

#### 替代方案

- **Apache Tika**：功能更强大（支持多种文档格式），但依赖更多，对于仅 PDF 场景过于重量级
- **iText**：商业许可证限制，AGPL 协议不利于商业使用

---

### 4. Markdown 解析与章节定位

#### 决策

使用 **CommonMark Java** 解析 Markdown AST，按 heading 层级切分，记录章节路径（如 "第一章 RAG 概述 > 1.1 定义"）。

#### 技术方案

**Markdown 解析服务**：
```java
public class MarkdownDocumentProcessor {

  private static final int CHUNK_SIZE = 1000;
  private static final int OVERLAP = 200;

  public List<Chunk> processMarkdown(Path filePath, String resourceId) throws IOException {
    String content = Files.readString(filePath, StandardCharsets.UTF_8);
    List<Chunk> chunks = new ArrayList<>();

    // 解析 Markdown AST
    Parser parser = Parser.builder().build();
    Node document = parser.parse(content);
    HeadingVisitor visitor = new HeadingVisitor();
    document.accept(visitor);

    // 获取按 heading 分割的章节
    List<Section> sections = visitor.getSections();

    // 对每个章节进行切分
    int chunkIndex = 0;
    for (Section section : sections) {
      String sectionText = section.text();
      String sectionPath = section.path();  // 如 "第一章 > 1.1 节"

      // 简单固定窗口切分
      int start = 0;
      while (start < sectionText.length()) {
        int end = Math.min(start + CHUNK_SIZE, sectionText.length());
        String chunkText = sectionText.substring(start, end);

        chunks.add(new Chunk(
            UUID.randomUUID().toString(),
            resourceId,
            chunkText,
            chunkIndex++,
            sectionPath,  // 章节路径作为定位信息
            "markdown"
        ));

        start = end - OVERLAP;
      }
    }

    return chunks;
  }

  private static class HeadingVisitor extends AbstractVisitor {

    private final List<Section> sections = new ArrayList<>();
    private final LinkedList<String> headingStack = new LinkedList<>();
    private final StringBuilder currentSectionText = new StringBuilder();

    public List<Section> getSections() {
      // 完成最后一个 section
      if (currentSectionText.length() > 0) {
        sections.add(new Section(
        String.join(" > ", headingStack),
        currentSectionText.toString()
    ));
  }
  return sections;
}

@Override
public void visit(Heading heading) {
  // 保存上一个 section
  if (currentSectionText.length() > 0) {
    sections.add(new Section(
        String.join(" > ", headingStack),
        currentSectionText.toString()
    ));
    currentSectionText.setLength(0);
  }

  // 更新 heading 栈
  int level = heading.getLevel();
  String title = ((Text) heading.getFirstChild()).getLiteral();
  updateHeadingStack(level, title);
}

@Override
public void visit(Text text) {
  currentSectionText.append(text.getLiteral());
}

private void updateHeadingStack(int level, String title) {
  // 移除更深层级的 heading
  while (headingStack.size() >= level) {
    headingStack.removeLast();
  }
  headingStack.add(title);
}
    }

  // 使用 Lombok @Data 而非 Java 21 Record
  @Data
  @AllArgsConstructor
  private static class Section {
    private String path;
    private String text;
  }
}
```

#### 理由

- **结构化定位**：按章节切分比固定窗口更符合文档逻辑，便于用户定位
- **CommonMark 标准**：CommonMark 是 Markdown 的规范实现，兼容性好
- **轻量级**：仅依赖 `org.commonmark:commonmark`，体积小

#### 替代方案

- **固定窗口切分**：不保留章节结构，用户体验差（如无法定位到"第三章"）
- **Flexmark**：功能更多（支持 GFM、表格），但对于基础 Markdown 解析过于重量级

---

### 5. 文本哈希与缓存 Key 设计

#### 决策

使用 **SHA-256 哈希算法**，对**规范化文本**（去除冗余空白、统一换行符）计算哈希值作为缓存 key。

#### 技术方案

**HashUtil 工具类**：
```java
public class HashUtil {
    private static final MessageDigest DIGEST;

    static {
        try {
            DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 不可用", e);
        }
    }

    /**
     * 计算文本的缓存 key（基于规范化文本的 SHA-256 哈希）
     */
    public static String hashText(String text) {
        if (text == null) return null;

        String normalized = TextNormalizer.normalize(text);
        byte[] hash = DIGEST.digest(normalized.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}

public class TextNormalizer {
    /**
     * 规范化文本：去除冗余空白、统一换行符、去除首尾空白
     */
    public static String normalize(String text) {
        if (text == null) return null;

        // 统一换行符为 \n
        String normalized = text.replace("\r\n", "\n")
                               .replace("\r", "\n");

        // 去除行首行尾空白
        normalized = Arrays.stream(normalized.split("\n"))
                          .map(String::trim)
                          .filter(line -> !line.isEmpty())
                          .collect(Collectors.joining("\n"));

        // 合并多个连续空格为单个空格
        normalized = normalized.replaceAll("\\s+", " ");

        return normalized.trim();
    }
}
```

**缓存 key 设计**：
- **Embedding 缓存 key**：`embedding:{sha256(normalizedText)}`
- **TopK 结果缓存 key**：`search:{sha256(query)}:{topK}:{filtersHash}:{version}`
  - `filtersHash`：过滤条件的哈希（如 tags 过滤）
  - `version`：索引版本号（资源更新后递增，用于缓存失效）

#### 理由

- **稳定性**：规范化后的文本哈希唯一且稳定，避免因空格差异导致的重复计算
- **性能**：SHA-256 计算速度快，冲突概率极低
- **可复现性**：相同文本必然生成相同哈希，便于调试和测试

#### 替代方案

- **直接哈希原始文本**：空格差异导致相同内容重复计算，浪费成本
- **MD5 哈希**：MD5 已被证明不安全（虽然哈希冲突仍极低），SHA-256 更规范

---

### 6. 用户画像：滑动平均 vs 加权平均

#### 决策

**MVP 阶段采用滑动平均**（最近 N 次查询的 Embedding 简单平均），后续迭代可扩展加权平均（点击行为权重 > 查询行为权重）。

#### 技术方案

**用户画像计算逻辑**：
```java
@Service
public class UserProfileService {
    private static final int WINDOW_SIZE = 10;  // 最近 10 次查询

    @Autowired
    private UserBehaviorRepository behaviorRepository;

    @Autowired
    private EmbeddingGateway embeddingGateway;

    /**
     * 更新用户画像（查询行为触发）
     */
    public void updateProfileOnQuery(Long userId, String query) {
        // 1. 对查询文本进行 Embedding
        float[] queryEmbedding = embeddingGateway.embed(query);

        // 2. 获取最近 N 次查询行为
        List<UserBehavior> recentBehaviors = behaviorRepository
            .findRecentQueries(userId, WINDOW_SIZE);

        // 3. 计算滑动平均
        float[] profileEmbedding = computeSlidingAverage(recentBehaviors, queryEmbedding);

        // 4. 保存或更新用户画像
        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .profileVector(profileEmbedding)
            .windowSize(WINDOW_SIZE)
            .updatedAt(Instant.now())
            .build();

        userProfileRepository.save(profile);
    }

    /**
     * 计算滑动平均（MVP：简单平均）
     */
    private float[] computeSlidingAverage(List<UserBehavior> behaviors, float[] newEmbedding) {
        int dimension = newEmbedding.length;
        float[] sum = new float[dimension];
        int count = behaviors.size() + 1;  // 包括新查询

        // 累加历史查询的 Embedding（从缓存或重新计算）
        for (UserBehavior behavior : behaviors) {
            float[] embedding = getEmbeddingFromBehavior(behavior);
            for (int i = 0; i < dimension; i++) {
                sum[i] += embedding[i];
            }
        }

        // 加上新查询的 Embedding
        for (int i = 0; i < dimension; i++) {
            sum[i] += newEmbedding[i];
        }

        // 计算平均
        float[] average = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            average[i] = sum[i] / count;
        }

        return average;
    }

    /**
     * 计算加权平均（后续迭代：点击权重 > 查询权重）
     */
    private float[] computeWeightedAverage(List<UserBehavior> behaviors, float[] newEmbedding) {
        Map<String, Float> weights = Map.of(
            "QUERY", 1.0f,
            "CLICK", 2.0f,
            "FAVORITE", 3.0f
        );

        // 类似滑动平均，但每个行为乘以权重
        // ... (实现略)
    }
}
```

**画像更新触发条件**：
- 用户提交查询后立即更新画像（异步后台执行）
- 点击或收藏资源后更新画像（异步后台执行）

#### 理由

- **MVP 简单性**：滑动平均算法简单易懂，无需调参（权重值）
- **效果可验证**：可通过离线评测对比"有画像 vs 无画像"的 Precision@K 提升来验证有效性
- **可扩展性**：后续迭代可轻松升级为加权平均或指数衰减

#### 替代方案

- **加权平均（MVP 阶段）**：需要确定权重值（如查询=1.0、点击=2.0），增加调参工作量
- **指数衰减（EWMA）**：更复杂，适合长期画像，MVP 阶段过于复杂

---

### 7. 离线评测数据集格式与评估指标

#### 决策

测试数据集采用 **JSON 格式**，评估指标计算 **Precision@K** 和 **Recall@K**，对比"无画像"和"有画像"两种推荐策略。

#### 技术方案

**测试数据集格式**（test_dataset.json）：
```json
{
  "version": "1.0",
  "created_at": "2025-12-25T00:00:00Z",
  "queries": [
    {
      "query_id": "q001",
      "text": "什么是 RAG？",
      "relevant_resources": [
        "resource-uuid-001",  // 标注为相关的资源 ID
        "resource-uuid-005"
      ]
    },
    {
      "query_id": "q002",
      "text": "深度学习的常用优化算法有哪些？",
      "relevant_resources": [
        "resource-uuid-010",
        "resource-uuid-015",
        "resource-uuid-020"
      ]
    }
  ]
}
```

**评估指标计算**：
```java
@Service
public class EvalService {
    /**
     * 计算离线评测指标
     */
    public EvalMetrics evaluate(List<TestQuery> testQueries, boolean useProfile) {
        int totalQueries = testQueries.size();
        double precisionSum = 0.0;
        double recallSum = 0.0;

        for (TestQuery testQuery : testQueries) {
            // 执行检索（有画像或无画像）
            SearchResult result = useProfile
                ? searchWithProfile(testQuery.getText())
                : searchWithoutProfile(testQuery.getText());

            // 获取 TopK 结果的资源 ID 列表
            Set<String> retrievedResources = result.getChunks().stream()
                .map(Chunk::getResourceId)
                .collect(Collectors.toSet());

            // 获取标注的相关资源 ID 集合
            Set<String> relevantResources = Set.of(testQuery.getRelevantResources());

            // 计算 Precision@K 和 Recall@K
            int truePositives = Sets.intersection(retrievedResources, relevantResources).size();
            int retrievedCount = retrievedResources.size();
            int relevantCount = relevantResources.size();

            double precision = retrievedCount > 0
                ? (double) truePositives / retrievedCount
                : 0.0;
            double recall = relevantCount > 0
                ? (double) truePositives / relevantCount
                : 0.0;

            precisionSum += precision;
            recallSum += recall;
        }

        // 计算平均指标
        double avgPrecision = precisionSum / totalQueries;
        double avgRecall = recallSum / totalQueries;

        return new EvalMetrics(avgPrecision, avgRecall);
    }

    /**
     * 对比两种推荐策略
     */
    public ComparisonResult compareStrategies(List<TestQuery> testQueries) {
        EvalMetrics withoutProfile = evaluate(testQueries, false);
        EvalMetrics withProfile = evaluate(testQueries, true);

        double precisionImprovement = (withProfile.precision() - withoutProfile.precision())
                                      / withoutProfile.precision() * 100.0;

        return new ComparisonResult(
            withoutProfile,
            withProfile,
            precisionImprovement
        );
    }
}
```

**评测报告格式**（Markdown）：
```markdown
# RAG 系统离线评测报告

**日期**：2025-12-25
**参数配置**：
- chunkSize: 1000
- overlap: 200
- topK: 5
- embeddingModel: embedding-3
- chatModel: glm-4.5-flash

## 语义检索指标

- Precision@5: 0.65
- Recall@10: 0.72

## 推荐效果对比

| 策略 | Precision@10 | Recall@10 |
|------|-------------|-----------|
| 无画像（仅查询） | 0.50 | 0.60 |
| 有画像（滑动平均） | 0.58 | 0.68 |
| **提升** | **+16%** | **+13.3%** |

## 性能指标

- 平均响应时间: 2.3 秒
- P95 响应时间: 3.8 秒
```

#### 理由

- **JSON 格式**：易于人类编辑和机器解析，支持版本控制
- **标准指标**：Precision@K 和 Recall@K 是信息检索的标准指标，易于理解和对比
- **可复现性**：记录参数配置和指标，支持复现实验

#### 替代方案

- **YAML 格式**：更简洁，但 JSON 解析库更多，工具支持更广泛
- **数据库存储**：增加部署复杂度，MVP 阶段使用文件即可

---

## 总结

本研究阶段的 7 个研究任务均已确定技术方案，所有决策遵循以下原则：

1. **性能优先**：Redis Stack 向量索引、Embedding 缓存、可度量的性能指标
2. **存储分离**：MySQL 存储结构化数据，Redis 存储向量与缓存
3. **缓存与幂等**：文本哈希缓存、资源去重、chunk 幂等入库
4. **模型网关**：统一的 ModelGateway、日志脱敏、降级策略
5. **可追溯性**：完整的引用来源（resourceId、chunkId、chunkIndex、定位信息、snippet）
6. **MVP 迭代**：滑动平均画像（后续可扩展加权平均）、简单切分（后续可优化）
7. **测试验收**：单元测试、集成测试、离线评测、可配置参数

所有技术选型均为成熟、稳定的开源或商业方案，符合 Aetheris 项目宪章要求。
