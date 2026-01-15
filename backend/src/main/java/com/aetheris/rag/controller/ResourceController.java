/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.controller;

import com.aetheris.rag.common.response.ApiResponse;
import com.aetheris.rag.common.response.PageResponse;
import com.aetheris.rag.dto.request.ResourceUpdateRequest;
import com.aetheris.rag.dto.response.ChunkResponse;
import com.aetheris.rag.dto.response.ResourceResponse;
import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.entity.Resource;
import com.aetheris.rag.service.ResourceService;
import com.aetheris.rag.service.ProcessingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.nio.file.Paths;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资源管理 REST 控制器。
 *
 * <p>提供资源的CRUD操作和基本查询功能。
 *
 * <h3>端点分类：</h3>
 * <ul>
 *   <li><b>资源CRUD</b>：上传、查询、更新、删除</li>
 *   <li><b>切片查询</b>：获取资源的切片列表</li>
 * </ul>
 *
 * <p><b>架构说明：</b>向量化相关功能已移至 {@link VectorController}，
 * 通过 ProcessingService 协调 VectorService 和 DocumentService，
 * 保持了服务层的职责分离。
 *
 * @author Aetheris Team
 * @version 2.0.0
 * @since 2025-12-30
 */
@Slf4j
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Validated
public class ResourceController {

  private final ResourceService resourceService;
  private final ProcessingService processingService;

  /**
   * 上传资源。
   *
   * @param file 上传的文件
   * @param title 资源标题
   * @param tags 标签（可选）
   * @param description 描述（可选）
   * @param authentication 认证信息
   * @return 资源响应
   * @throws Exception 如果文件处理失败（由 GlobalExceptionHandler 统一处理）
   */
  @PostMapping
  public ResponseEntity<ApiResponse<ResourceResponse>> uploadResource(
      @RequestParam("file") MultipartFile file,
      @RequestParam("title") String title,
      @RequestParam(value = "tags", required = false) String tags,
      @RequestParam(value = "description", required = false) String description,
      Authentication authentication)
      throws Exception {
    Long userId = (Long) authentication.getPrincipal();
    log.info("POST /api/resources - userId={}, title={}, file={}", userId, title, file.getOriginalFilename());

    // 上传资源（包含完整的文件保存 + 文档解析 + 切片生成 + 向量化流程）
    // 异常由 GlobalExceptionHandler 统一处理
    Resource resource =
        processingService.processResourceUpload(file, title, tags, description, userId);

    ResourceResponse resourceResponse = ResourceResponse.fromEntity(resource);
    ApiResponse<ResourceResponse> response =
        ApiResponse.success(resourceResponse, "上传成功");

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 获取资源列表（分页）。
   *
   * @param page 页码（从 0 开始，默认 0）
   * @param size 每页大小（默认 10）
   * @return 资源列表（分页）
   */
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<ResourceResponse>>> getResourceList(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    log.info("GET /api/resources - page={}, size={}", page, size);

    List<Resource> resources = resourceService.getResourceList(page * size, size);
    Long total = resourceService.getResourceCount();

    List<ResourceResponse> resourceResponses =
        resources.stream().map(ResourceResponse::fromEntity).toList();

    PageResponse<ResourceResponse> pageResponse =
        PageResponse.of(resourceResponses, total, page, size);

    return ResponseEntity.ok(ApiResponse.success(pageResponse));
  }

  /**
   * 获取资源详情。
   *
   * @param id 资源ID
   * @return 资源响应
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ResourceResponse>> getResourceById(@PathVariable @Min(1) Long id) {
    log.info("GET /api/resources/{}", id);

    Resource resource = resourceService.getResourceById(id);
    if (resource == null) {
      return ResponseEntity.notFound().build();
    }

    ResourceResponse resourceResponse = ResourceResponse.fromEntity(resource);
    return ResponseEntity.ok(ApiResponse.success(resourceResponse));
  }

  /**
   * 更新资源信息。
   *
   * @param id 资源ID
   * @param request 更新请求
   * @param authentication 认证信息
   * @return 更新后的资源响应
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
      @PathVariable @Min(1) Long id,
      @Valid @RequestBody ResourceUpdateRequest request,
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("PUT /api/resources/{} - userId={}, title={}", id, userId, request.getTitle());

    // 权限检查：只能修改自己上传的资源
    Resource existing = resourceService.getResourceById(id);
    if (existing == null) {
      return ResponseEntity.notFound().build();
    }
    if (!userId.equals(existing.getUploadedBy())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error(403, "无权修改此资源"));
    }

    // 更新资源
    Resource updated =
        resourceService.updateResource(id, request.getTitle(), request.getTags(), request.getDescription());

    ResourceResponse resourceResponse = ResourceResponse.fromEntity(updated);
    return ResponseEntity.ok(ApiResponse.success(resourceResponse, "更新成功"));
  }

  /**
   * 获取资源的切片列表。
   *
   * @param id 资源ID
   * @return 切片列表
   */
  @GetMapping("/{id}/chunks")
  public ResponseEntity<ApiResponse<List<ChunkResponse>>> getChunksByResourceId(
      @PathVariable @Min(1) Long id) {
    log.info("GET /api/resources/{}/chunks", id);

    List<Chunk> chunks = resourceService.getChunksByResourceId(id);
    List<ChunkResponse> chunkResponses =
        chunks.stream().map(ChunkResponse::fromEntity).toList();

    return ResponseEntity.ok(ApiResponse.success(chunkResponses));
  }

  /**
   * 删除资源。
   *
   * @param id 资源ID
   * @param authentication 认证信息
   * @return 删除的资源响应
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<ResourceResponse>> deleteResource(
      @PathVariable @Min(1) Long id, Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    log.info("DELETE /api/resources/{} - userId={}", id, userId);

    // 使用 ProcessingService 处理完整的删除流程（向量数据 + 切片 + 资源记录 + 物理文件）
    Resource deleted = processingService.processResourceDeletion(id, userId);

    return ResponseEntity.ok(
        ApiResponse.success(ResourceResponse.fromEntity(deleted), "删除成功"));
  }

  /**
   * 批量删除资源。
   *
   * @param request 批量删除请求
   * @param authentication 认证信息
   * @return 删除的资源响应列表
   */
  @DeleteMapping("/batch")
  public ResponseEntity<ApiResponse<List<ResourceResponse>>> deleteResources(
      @RequestBody com.aetheris.rag.dto.request.BatchDeleteRequest request,
      Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    log.info(
        "DELETE /api/resources/batch - userId={}, count={}",
        userId,
        request.getIds().size());

    // 使用 ProcessingService 处理完整的批量删除流程（向量数据 + 切片 + 资源记录 + 物理文件）
    List<Resource> deleted = processingService.processBatchResourceDeletion(request.getIds(), userId);

    List<ResourceResponse> responses =
        deleted.stream().map(ResourceResponse::fromEntity).toList();

    return ResponseEntity.ok(
        ApiResponse.success(
            responses,
            String.format("批量删除完成，成功删除 %d 个资源", deleted.size())));
  }
}
