/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.dto.request.RebuildConfig;
import com.aetheris.rag.dto.response.RebuildResult;
import com.aetheris.rag.exception.ConflictException;
import com.aetheris.rag.exception.InternalServerException;
import com.aetheris.rag.gateway.EmbeddingGateway;
import com.aetheris.rag.mapper.ChunkMapper;
import com.aetheris.rag.mapper.ResourceMapper;
import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.entity.Resource;
import com.aetheris.rag.service.VectorService;
import com.aetheris.rag.util.VectorUtils;
import com.aetheris.rag.util.VectorizationStatusUtil;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

/**
 * 向量化服务实现类。
 *
 * <p>批量处理未向量化的切片，调用 EmbeddingGateway 获取向量，写入 Redis 向量索引。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

  private final ChunkMapper chunkMapper;
  private final ResourceMapper resourceMapper;
  private final EmbeddingGateway embeddingGateway;
  private final StringRedisTemplate redisTemplate;
  private final RedissonClient redissonClient;

  /** 向量维度（智谱 embedding-3，默认 2048 维） */
  @Value("${rag.vector.dimension:2048}")
  private int vectorSize;

  /** Redis 向量索引名称 */
  private static final String INDEX_NAME = "chunk_vector_index";

  /** 批量处理大小 */
  @Value("${rag.vectorization.batchSize:10}")
  private int batchSize;

  /** 向量索引是否已初始化 */
  private boolean indexInitialized = false;

  /** 全局重建锁 key */
  private static final String REBUILD_LOCK_KEY = "rebuild:global:lock";

  /** 索引操作锁 key */
  private static final String INDEX_LOCK_KEY = "rebuild:index:lock";

  /** 默认锁等待时间（秒） */
  private static final long LOCK_WAIT_TIME = 10;

  /** 默认锁持有时间（秒，2小时） */
  private static final long LOCK_LEASE_TIME = 7200;

  /** 取消标志 */
  private volatile boolean cancelled = false;

  @Override
  @PostConstruct
  public void initializeVectorIndex() {
    if (indexInitialized) {
      return;
    }

    try {
      // 检查索引是否已存在（使用 FT._LIST 命令，避免 FT.INFO 的浮点数解析问题）
      log.debug("检查向量索引是否存在: {}", INDEX_NAME);

      Boolean indexExists = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
        try {
          // 使用 FT._LIST 命令列出所有索引，避免解析浮点数问题
          Object result = connection.execute("FT._LIST");
          if (result != null) {
            String indexList = result.toString();
            // 检查索引名是否在列表中
            return indexList.contains(INDEX_NAME);
          }
          return false;
        } catch (Exception e) {
          // 任何异常都认为索引不存在
          log.debug("检查索引失败，可能是索引不存在: {}", e.getMessage());
          return false;
        }
      });

      if (indexExists) {
        log.info("向量索引已存在: {}", INDEX_NAME);
        indexInitialized = true;
        return;
      }

      // 创建向量索引
      log.info("创建向量索引: {}", INDEX_NAME);

      // 执行 FT.CREATE 命令
      String createResult = redisTemplate.execute((RedisCallback<String>) connection -> {
        // FT.CREATE index_name ON HASH PREFIX 1 chunk: SCHEMA ...
        // execute(String command, byte[]... args) - 第一个参数是String命令名
        // HNSW 参数：TYPE、DIM、DISTANCE_METRIC（共 6 个参数值，即 3 个键值对）
        Object result = connection.execute(
          "FT.CREATE",  // String 命令名
          INDEX_NAME.getBytes(),
          "ON".getBytes(), "HASH".getBytes(),
          "PREFIX".getBytes(), "1".getBytes(), "chunk:".getBytes(),
          "SCHEMA".getBytes(),
          "vector".getBytes(), "VECTOR".getBytes(), "HNSW".getBytes(), "6".getBytes(),
          "TYPE".getBytes(), "FLOAT32".getBytes(),
          "DIM".getBytes(), String.valueOf(vectorSize).getBytes(),
          "DISTANCE_METRIC".getBytes(), "COSINE".getBytes()
        );
        return result != null ? result.toString() : "OK";
      });

      log.info("向量索引创建成功: {}, result: {}", INDEX_NAME, createResult);
      indexInitialized = true;
    } catch (Exception e) {
      // 检查是否是索引已存在的错误（检查整个异常链）
      boolean indexAlreadyExists = false;
      Throwable cause = e;
      while (cause != null) {
        if (cause.getMessage() != null && cause.getMessage().contains("Index already exists")) {
          indexAlreadyExists = true;
          break;
        }
        cause = cause.getCause();
      }

      if (indexAlreadyExists) {
        log.info("向量索引已存在: {}", INDEX_NAME);
        indexInitialized = true;
      } else {
        log.error("初始化向量索引失败", e);
        throw new InternalServerException("向量索引初始化失败: " + e.getMessage(), e);
      }
    }
  }

  @Override
  public void vectorizeChunks(Long resourceId) {
    log.info("开始向量化资源切片: resourceId={}", resourceId);

    // 查询未向量化的切片
    List<Chunk> chunks = chunkMapper.findUnvectorizedByResourceId(resourceId);

    if (chunks.isEmpty()) {
      // ✅ 修复 1：验证资源是否真的有切片数据
      List<Chunk> allChunks = chunkMapper.findByResourceId(resourceId);

      if (allChunks.isEmpty()) {
        // 没有任何切片数据，保持 vectorized=false
        log.warn("资源没有任何切片数据，跳过向量化: resourceId={}", resourceId);
        return;
      }

      // 有切片数据且都已向量化，确认状态
      VectorizationStatusUtil.updateResourceVectorizationStatus(resourceMapper, resourceId, allChunks);
      boolean allVectorized = VectorizationStatusUtil.calculateVectorizationStatus(allChunks);
      if (allVectorized) {
        log.info("资源所有切片已向量化，状态已确认: resourceId={}, chunkCount={}", resourceId, allChunks.size());
      }
      return;
    }

    // 批量处理切片
    vectorizeBatch(chunks);

    // ✅ 修复 2：检查资源所有切片是否已向量化
    List<Chunk> allChunks = chunkMapper.findByResourceId(resourceId);

    // 关键修复：只有存在切片数据时才更新状态
    if (!allChunks.isEmpty()) {
      boolean allVectorized = VectorizationStatusUtil.calculateVectorizationStatus(allChunks);
      resourceMapper.updateChunkStatus(resourceId, allChunks.size(), allVectorized);

      if (allVectorized) {
        log.info("资源向量化完成: resourceId={}, chunkCount={}", resourceId, allChunks.size());
      } else {
        long vectorizedCount = allChunks.stream().filter(Chunk::getVectorized).count();
        log.warn("资源部分切片向量化失败: resourceId={}, 已向量化={}/{}", resourceId, vectorizedCount, allChunks.size());
      }
    } else {
      log.error("严重错误: 向量化后切片数据丢失: resourceId={}", resourceId);
      // 不更新 vectorized 状态，保持为 false
    }
  }

  @Override
  public void vectorizeAllUnvectorized() {
    log.info("开始批量向量化所有未向量化的切片");

    List<Chunk> chunks = chunkMapper.findUnvectorized();
    log.info("找到 {} 个未向量化的切片", chunks.size());

    // 分批处理
    for (int i = 0; i < chunks.size(); i += batchSize) {
      int end = Math.min(i + batchSize, chunks.size());
      List<Chunk> batch = chunks.subList(i, end);
      vectorizeBatch(batch);
    }
  }

  /**
   * 批量向量化切片。
   *
   * @param chunks 切片列表
   */
  private void vectorizeBatch(List<Chunk> chunks) {
    log.debug("批量向量化 {} 个切片", chunks.size());

    List<Long> vectorizedIds = new ArrayList<>();

    for (Chunk chunk : chunks) {
      try {
        // 调用 EmbeddingGateway 获取向量
        float[] vector = embeddingGateway.embed(chunk.getChunkText());

        // 写入 Redis 向量索引
        writeVectorToRedis(chunk, vector);

        vectorizedIds.add(chunk.getId());
        log.debug("切片向量化成功: chunkId={}", chunk.getId());
      } catch (Exception e) {
        log.error("切片向量化失败: chunkId={}", chunk.getId(), e);
      }
    }

    // 批量更新向量化状态
    if (!vectorizedIds.isEmpty()) {
      chunkMapper.batchUpdateVectorized(vectorizedIds, true);
      log.info("批量更新向量化状态: {} 个切片", vectorizedIds.size());
    }
  }

  /**
   * 将向量写入 Redis。
   *
   * @param chunk 切片
   * @param vector 向量
   */
  private void writeVectorToRedis(Chunk chunk, float[] vector) {
    try {
      // Redis key: chunk:{chunkId}
      String key = "chunk:" + chunk.getId();

      // 准备数据（文本字段）
      Map<String, String> fields = new HashMap<>();
      fields.put("chunkId", chunk.getId().toString());
      fields.put("resourceId", chunk.getResourceId().toString());
      fields.put("chunkIndex", chunk.getChunkIndex().toString());
      fields.put("chunkText", chunk.getChunkText());

      // 写入文本字段到 Redis Hash
      redisTemplate.opsForHash().putAll(key, fields);

      // 将向量转换为二进制格式（FLOAT32）
      byte[] vectorBytes = VectorUtils.toBytes(vector);

      // 使用底层 connection 写入向量字段（必须用 HSET 直接写入字节）
      redisTemplate.execute((RedisCallback<Object>) connection -> {
        connection.hSet(key.getBytes(), "vector".getBytes(), vectorBytes);
        return null;
      });

      // 设置过期时间（30天）
      redisTemplate.expire(key, Duration.ofDays(30));

      log.debug("向量已写入 Redis: chunkId={}, vectorSize={} bytes", chunk.getId(), vectorBytes.length);
    } catch (Exception e) {
      log.error("写入 Redis 向量失败: chunkId={}", chunk.getId(), e);
    }
  }


  @Override
  public void rebuildVectorIndex() {
    // 保持向后兼容，使用默认配置
    RebuildConfig defaultConfig = RebuildConfig.builder()
        .dropIndex(true)
        .rechunk(false)
        .batchSize(batchSize)
        .maxRetries(3)
        .skipOnError(true)
        .enableConcurrency(false)
        .build();

    try {
      rebuildVectorIndex(defaultConfig);
    } catch (Exception e) {
      throw new InternalServerException("重建向量索引失败: " + e.getMessage(), e);
    }
  }

  /**
   * 重建向量索引（使用配置）。
   *
   * @param config 重建配置
   * @return 重建结果
   * @throws Exception 如果重建失败
   */
  public RebuildResult rebuildVectorIndex(RebuildConfig config) throws Exception {
    long startTime = System.currentTimeMillis();

    // 1. 获取分布式锁
    RLock lock = redissonClient.getLock(REBUILD_LOCK_KEY);
    boolean lockAcquired = false;

    try {
      lockAcquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

      if (!lockAcquired) {
        throw new ConflictException("系统繁忙，已有重建任务正在执行");
      }

      // 2. 检查取消标志
      if (cancelled) {
        throw new ConflictException("重建任务已被取消");
      }

      log.info("========================================");
      log.info("开始轻量级重建向量索引");
      log.info("配置: dropIndex={}, batchSize={}, maxRetries={}, skipOnError={}, enableConcurrency={}",
          config.isDropIndex(), config.getBatchSize(), config.getMaxRetries(),
          config.isSkipOnError(), config.isEnableConcurrency());
      log.info("========================================");

      int successCount = 0;
      int failureCount = 0;
      List<Long> failedIds = new ArrayList<>();

      // 3. 删除旧索引
      if (config.isDropIndex()) {
        log.info("步骤 1/6: 删除旧索引...");
        dropIndex();
        log.info("✅ 旧索引已删除");
      }

      // 4. 清理Redis向量数据
      log.info("步骤 2/6: 清理Redis向量数据...");
      long deletedCount = clearVectorData();
      log.info("✅ 已清理 {} 个向量数据", deletedCount);

      // 5. 重置向量化状态
      log.info("步骤 3/6: 重置向量化状态...");
      int resetCount = chunkMapper.resetAllVectorized();
      int resetResourceCount = resourceMapper.resetAllVectorizedStatus();
      log.info("✅ 已重置 {} 个资源, {} 个切片的向量化状态",
          resetResourceCount, resetCount);

      // 6. 创建新索引
      log.info("步骤 4/6: 创建新索引...");
      indexInitialized = false;
      initializeVectorIndex();
      log.info("✅ 新索引已创建");

      // 7. 批量向量化
      log.info("步骤 5/6: 开始批量向量化...");
      VectorizeResult vectorizeResult = vectorizeAllWithRetry(config);
      successCount = vectorizeResult.getSuccessCount();
      failureCount = vectorizeResult.getFailureCount();
      failedIds = vectorizeResult.getFailedIds();
      log.info("✅ 向量化完成: 成功={}, 失败={}", successCount, failureCount);

      // 8. 验证
      log.info("步骤 6/6: 验证向量化状态...");
      int vectorizedCount = chunkMapper.countVectorized();
      int totalCount = chunkMapper.countTotal();
      log.info("✅ 向量化验证: {}/{} ({:.2f}%)",
          vectorizedCount, totalCount, (vectorizedCount * 100.0 / totalCount));

      // 9. 完成重建
      long duration = System.currentTimeMillis() - startTime;
      log.info("========================================");
      log.info("轻量级重建完成");
      log.info("成功: {}, 失败: {}, 耗时: {} ms", successCount, failureCount, duration);
      log.info("========================================");

      return RebuildResult.builder()
          .successCount(successCount)
          .failureCount(failureCount)
          .totalChunks(totalCount)
          .duration(duration)
          .failedResourceIds(failedIds)
          .build();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ConflictException("重建任务被取消", e);
    } catch (Exception e) {
      log.error("重建向量索引失败", e);
      throw new InternalServerException("重建向量索引失败: " + e.getMessage(), e);
    } finally {
      // 释放锁
      if (lockAcquired) {
        lock.unlock();
      }
      // 重置取消标志
      cancelled = false;
    }
  }

  @Override
  public boolean recalculateVectorizationStatus(Long resourceId) {
    log.info("重新计算向量化状态: resourceId={}", resourceId);

    try {
      // 1. 查询资源的所有切片
      List<Chunk> allChunks = chunkMapper.findByResourceId(resourceId);

      // 2. 查询当前资源状态
      Resource resource = resourceMapper.findById(resourceId);
      if (resource == null) {
        log.warn("资源不存在: resourceId={}", resourceId);
        return false;
      }

      int actualChunkCount = allChunks.size();
      long vectorizedCount = allChunks.stream().filter(Chunk::getVectorized).count();

      // ✅ 关键逻辑：只有存在切片时才标记为已向量化
      boolean allVectorized = VectorizationStatusUtil.calculateVectorizationStatus(allChunks);

      // 3. 检查是否需要修复
      boolean needsRepair = false;

      if (!resource.getChunkCount().equals(actualChunkCount)) {
        log.warn("切片数量不一致: resourceId={}, 数据库={}, 实际={}",
            resourceId, resource.getChunkCount(), actualChunkCount);
        needsRepair = true;
      }

      if (!resource.getVectorized().equals(allVectorized)) {
        log.warn("向量化状态不一致: resourceId={}, 数据库={}, 应该={}",
            resourceId, resource.getVectorized(), allVectorized);
        needsRepair = true;
      }

      // 4. 执行修复
      if (needsRepair) {
        resourceMapper.updateChunkStatus(resourceId, actualChunkCount, allVectorized);
        log.info("已修复向量化状态: resourceId={}, chunkCount={}, vectorized={}",
            resourceId, actualChunkCount, allVectorized);
        return true;
      } else {
        log.info("向量化状态正常，无需修复: resourceId={}", resourceId);
        return false;
      }
    } catch (Exception e) {
      log.error("重新计算向量化状态失败: resourceId={}", resourceId, e);
      return false;
    }
  }

  @Override
  public int repairAllVectorizationStatus() {
    log.info("开始批量修复所有资源的向量化状态...");

    // 1. 获取所有资源ID
    List<Resource> allResources = resourceMapper.findPaged(0, Integer.MAX_VALUE);
    log.info("找到 {} 个资源需要检查", allResources.size());

    int repairedCount = 0;
    for (Resource resource : allResources) {
      boolean repaired = recalculateVectorizationStatus(resource.getId());
      if (repaired) {
        repairedCount++;
      }
    }

    log.info("批量修复完成: 检查了 {} 个资源，修复了 {} 个资源", allResources.size(), repairedCount);
    return repairedCount;
  }

  /**
   * 批量向量化（支持重试和进度跟踪）。
   *
   * @param config 重建配置
   * @return 向量化结果
   * @throws Exception 如果向量化失败
   */
  private VectorizeResult vectorizeAllWithRetry(RebuildConfig config) throws Exception {
    int successCount = 0;
    int failureCount = 0;
    List<Long> failedIds = new ArrayList<>();

    // 分页查询未向量化的切片
    int offset = 0;
    int limit = config.getBatchSize();
    List<Chunk> batch;

    do {
      // 检查取消标志
      if (cancelled) {
        log.warn("检测到取消标志，停止向量化");
        break;
      }

      // 查询一批未向量化的切片
      batch = chunkMapper.findUnvectorizedPaged(offset, limit);

      if (batch.isEmpty()) {
        break;
      }

      // 并发或串行向量化
      if (config.isEnableConcurrency()) {
        VectorizeBatchResult batchResult = vectorizeBatchConcurrent(batch, config);
        successCount += batchResult.getSuccessCount();
        failureCount += batchResult.getFailureCount();
        failedIds.addAll(batchResult.getFailedIds());
      } else {
        VectorizeBatchResult batchResult = vectorizeBatchSequential(batch, config);
        successCount += batchResult.getSuccessCount();
        failureCount += batchResult.getFailureCount();
        failedIds.addAll(batchResult.getFailedIds());
      }

      offset += limit;

    } while (batch.size() == limit);

    return new VectorizeResult(successCount, failureCount, failedIds);
  }

  /**
   * 顺序向量化一批切片（带重试）。
   *
   * @param chunks 切片列表
   * @param config 重建配置
   * @return 批次结果
   */
  private VectorizeBatchResult vectorizeBatchSequential(List<Chunk> chunks, RebuildConfig config) {
    List<Long> successIds = new ArrayList<>();
    List<Long> failedIds = new ArrayList<>();

    for (Chunk chunk : chunks) {
      // 检查取消
      if (cancelled) {
        break;
      }

      boolean success = false;
      Exception lastError = null;

      // 重试逻辑
      for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
        try {
          // 获取向量
          float[] vector = embeddingGateway.embed(chunk.getChunkText());

          // 写入Redis
          writeVectorToRedis(chunk, vector);

          // 更新状态
          chunkMapper.updateVectorized(chunk.getId(), true);

          success = true;
          log.debug("切片向量化成功: chunkId={}", chunk.getId());
          break;

        } catch (Exception e) {
          lastError = e;
          log.warn("切片向量化失败(第{}次尝试): chunkId={}, error={}",
              attempt + 1, chunk.getId(), e.getMessage());

          if (attempt < config.getMaxRetries()) {
            // 指数退避
            try {
              Thread.sleep(1000L * (1L << attempt));
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              break;
            }
          }
        }
      }

      if (success) {
        successIds.add(chunk.getId());
      } else {
        failedIds.add(chunk.getId());
        log.error("切片向量化失败(已达最大重试次数): chunkId={}", chunk.getId(), lastError);

        if (!config.isSkipOnError()) {
          throw new InternalServerException(
              "切片向量化失败: chunkId=" + chunk.getId(), lastError);
        }
      }
    }

    // 更新资源状态
    updateResourceVectorizationStatus(chunks);

    return new VectorizeBatchResult(successIds.size(), failedIds.size(), failedIds);
  }

  /**
   * 并发向量化一批切片（使用虚拟线程）。
   *
   * @param chunks 切片列表
   * @param config 重建配置
   * @return 批次结果
   */
  private VectorizeBatchResult vectorizeBatchConcurrent(List<Chunk> chunks, RebuildConfig config) {
    List<Long> successIds = Collections.synchronizedList(new ArrayList<>());
    List<Long> failedIds = Collections.synchronizedList(new ArrayList<>());

    // 使用虚拟线程并发处理
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    List<Future<?>> futures = new ArrayList<>();

    try {
      for (Chunk chunk : chunks) {
        if (cancelled) {
          break;
        }

        // 提交任务到虚拟线程池
        Future<?> future = executor.submit(() -> {
          boolean success = false;
          Exception lastError = null;

          // 重试逻辑
          for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
            try {
              // 获取向量
              float[] vector = embeddingGateway.embed(chunk.getChunkText());

              // 写入Redis
              writeVectorToRedis(chunk, vector);

              // 更新状态
              chunkMapper.updateVectorized(chunk.getId(), true);

              successIds.add(chunk.getId());
              success = true;
              log.debug("切片向量化成功: chunkId={}", chunk.getId());
              break;

            } catch (Exception e) {
              lastError = e;
              log.warn("切片向量化失败(第{}次尝试): chunkId={}, error={}",
                  attempt + 1, chunk.getId(), e.getMessage());

              if (attempt < config.getMaxRetries()) {
                // 指数退避
                try {
                  Thread.sleep(1000L * (1L << attempt));
                } catch (InterruptedException ie) {
                  Thread.currentThread().interrupt();
                  break;
                }
              }
            }
          }

          if (!success) {
            failedIds.add(chunk.getId());
            log.error("切片向量化失败(已达最大重试次数): chunkId={}", chunk.getId(), lastError);

            if (!config.isSkipOnError()) {
              throw new InternalServerException(
                  "切片向量化失败: chunkId=" + chunk.getId(), lastError);
            }
          }
        });

        futures.add(future);
      }

      // 等待所有任务完成
      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (Exception e) {
          log.error("并发任务执行失败", e);
        }
      }

    } finally {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    // 更新资源状态
    updateResourceVectorizationStatus(chunks);

    return new VectorizeBatchResult(successIds.size(), failedIds.size(), failedIds);
  }

  /**
   * 更新资源的向量化状态。
   *
   * @param chunks 切片列表
   */
  private void updateResourceVectorizationStatus(List<Chunk> chunks) {
    // 按资源ID分组
    Map<Long, List<Chunk>> chunksByResource = new HashMap<>();
    for (Chunk chunk : chunks) {
      chunksByResource.computeIfAbsent(chunk.getResourceId(), k -> new ArrayList<>()).add(chunk);
    }

    // 更新每个资源的状态
    for (Map.Entry<Long, List<Chunk>> entry : chunksByResource.entrySet()) {
      Long resourceId = entry.getKey();
      List<Chunk> resourceChunks = entry.getValue();

      // 重新查询该资源的所有切片
      List<Chunk> allChunks = chunkMapper.findByResourceId(resourceId);
      boolean allVectorized = VectorizationStatusUtil.calculateVectorizationStatus(allChunks);

      if (allVectorized) {
        VectorizationStatusUtil.updateResourceVectorizationStatus(resourceMapper, resourceId, allChunks);
        log.debug("资源向量化完成: resourceId={}, chunkCount={}", resourceId, allChunks.size());
      }
    }
  }

  /**
   * 删除索引。
   */
  private void dropIndex() {
    RLock lock = redissonClient.getLock(INDEX_LOCK_KEY);

    try {
      lock.lock();

      redisTemplate.execute((RedisCallback<Object>) connection -> {
        connection.execute("FT.DROPINDEX", INDEX_NAME.getBytes(), "DD".getBytes());
        return null;
      });

      indexInitialized = false;
      log.info("向量索引已删除");

    } catch (Exception e) {
      log.warn("删除索引失败（可能不存在）: {}", e.getMessage());
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  /**
   * 清理Redis向量数据。
   *
   * @return 删除的数量
   */
  private long clearVectorData() {
    try {
      Set<String> keys = redisTemplate.keys("chunk:*");
      if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
        return keys.size();
      }
      return 0;
    } catch (Exception e) {
      log.error("清理向量数据失败", e);
      return 0;
    }
  }

  /**
   * 取消重建任务。
   *
   * @return 是否成功取消
   */
  public boolean cancelRebuild() {
    cancelled = true;
    log.info("已设置重建任务取消标志");
    return true;
  }

  /**
   * 向量化结果辅助类。
   */
  private static class VectorizeResult {
    private final int successCount;
    private final int failureCount;
    private final List<Long> failedIds;

    VectorizeResult(int successCount, int failureCount, List<Long> failedIds) {
      this.successCount = successCount;
      this.failureCount = failureCount;
      this.failedIds = failedIds;
    }

    int getSuccessCount() {
      return successCount;
    }

    int getFailureCount() {
      return failureCount;
    }

    List<Long> getFailedIds() {
      return failedIds;
    }
  }

  /**
   * 批次向量化结果辅助类。
   */
  private static class VectorizeBatchResult {
    private final int successCount;
    private final int failureCount;
    private final List<Long> failedIds;

    VectorizeBatchResult(int successCount, int failureCount, List<Long> failedIds) {
      this.successCount = successCount;
      this.failureCount = failureCount;
      this.failedIds = failedIds;
    }

    int getSuccessCount() {
      return successCount;
    }

    int getFailureCount() {
      return failureCount;
    }

    List<Long> getFailedIds() {
      return failedIds;
    }
  }
}
