/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.controller;

import com.aetheris.rag.common.response.ApiResponse;
import com.aetheris.rag.common.response.PageResponse;
import com.aetheris.rag.dto.request.ResourceUploadRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
   * @param request 上传请求
   * @param authentication 认证信息
   * @return 资源响应
   * @throws Exception 如果文件处理失败（由 GlobalExceptionHandler 统一处理）
   */
  @PostMapping
  public ResponseEntity<ApiResponse<ResourceResponse>> uploadResource(
      @Valid @RequestBody ResourceUploadRequest request, Authentication authentication)
      throws Exception {
    Long userId = (Long) authentication.getPrincipal();
    log.info("POST /api/resources - userId={}, title={}", userId, request.getTitle());

    // 转换文件路径
    java.nio.file.Path filePath = Paths.get(request.getFilePath());

    // 上传资源（异常由 GlobalExceptionHandler 统一处理）
    Resource resource =
        resourceService.uploadResource(
            filePath, request.getTitle(), request.getTags(), request.getDescription(), userId);

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
}
