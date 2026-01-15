/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.util;

import com.aetheris.rag.entity.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限检查工具类。
 *
 * <p>提供统一的权限检查方法，消除ServiceImpl层的重复代码。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-15
 */
public final class PermissionCheckUtil {

  private PermissionCheckUtil() {
  }

  /**
   * 检查资源所有权。
   *
   * @param resource 资源实体
   * @param userId 用户ID
   * @throws IllegalArgumentException 用户无权访问该资源
   */
  public static void checkResourceOwnership(Resource resource, Long userId) {
    if (resource == null) {
      throw new IllegalArgumentException("资源不存在");
    }
    if (!resource.getUploadedBy().equals(userId)) {
      throw new IllegalArgumentException("无权操作此资源");
    }
  }

  /**
   * 批量检查资源所有权并过滤。
   *
   * @param resources 资源列表
   * @param userId 用户ID
   * @return 用户拥有的资源列表
   * @throws IllegalArgumentException 用户无权访问任何资源
   */
  public static List<Resource> filterOwnedResources(List<Resource> resources, Long userId) {
    List<Resource> ownedResources = resources.stream()
        .filter(r -> r.getUploadedBy().equals(userId))
        .collect(Collectors.toList());

    if (ownedResources.isEmpty()) {
      throw new IllegalArgumentException("无权操作这些资源");
    }

    return ownedResources;
  }
}
