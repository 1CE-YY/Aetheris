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
import jakarta.validation.Valid;
import java.nio.file.Paths;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资源管理 REST 控制器。
 *
 * <p>提供资源上传、查询、切片查询等接口。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Slf4j
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

  private final ResourceService resourceService;

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

    // 上传资源（异常由 GlobalExceptionHandler 统一处理）
    Resource resource =
        resourceService.uploadResource(file, title, tags, description, userId);

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
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
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
  public ResponseEntity<ApiResponse<ResourceResponse>> getResourceById(@PathVariable Long id) {
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
      @PathVariable Long id,
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
      @PathVariable Long id) {
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
      @PathVariable Long id, Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    log.info("DELETE /api/resources/{} - userId={}", id, userId);

    try {
      // 删除资源（Service 层会检查权限）
      Resource deleted = resourceService.deleteResource(id, userId);

      return ResponseEntity.ok(
          ApiResponse.success(ResourceResponse.fromEntity(deleted), "删除成功"));
    } catch (RuntimeException e) {
      if (e.getMessage().contains("资源不存在")) {
        return ResponseEntity.notFound().build();
      }
      if (e.getMessage().contains("无权删除")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(403, e.getMessage()));
      }
      throw e;
    }
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

    List<Resource> deleted = resourceService.deleteResources(request.getIds(), userId);

    List<ResourceResponse> responses =
        deleted.stream().map(ResourceResponse::fromEntity).toList();

    return ResponseEntity.ok(
        ApiResponse.success(
            responses,
            String.format("批量删除完成，成功删除 %d 个资源", deleted.size())));
  }
}
