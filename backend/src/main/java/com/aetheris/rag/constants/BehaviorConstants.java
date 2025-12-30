/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.constants;

/**
 * 用户行为相关常量。
 *
 * <p>统一管理用户行为类型和权重的常量，避免硬编码。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
public final class BehaviorConstants {

  /** 私有构造函数，防止实例化 */
  private BehaviorConstants() {}

  /**
   * 行为类型常量。
   *
   * <p>对应 {@link com.aetheris.rag.model.UserBehavior.BehaviorType} 枚举值
   */
  public static final class Type {

    /** 私有构造函数 */
    private Type() {}

    /** 查询行为 */
    public static final String QUERY = "QUERY";

    /** 点击行为 */
    public static final String CLICK = "CLICK";

    /** 收藏行为 */
    public static final String FAVORITE = "FAVORITE";
  }

  /**
   * 行为权重常量。
   *
   * <p>用于计算用户画像，权重越高表示该行为对用户兴趣的影响越大
   */
  public static final class Weight {

    /** 私有构造函数 */
    private Weight() {}

    /** 查询行为权重（基础权重） */
    public static final double QUERY = 1.0;

    /** 点击行为权重（中等权重） */
    public static final double CLICK = 2.0;

    /** 收藏行为权重（高权重） */
    public static final double FAVORITE = 3.0;
  }

  /**
   * 查询限制常量。
   *
   * <p>用于分页查询时的默认限制
   */
  public static final class Limit {

    /** 私有构造函数 */
    private Limit() {}

    /** 默认查询数量 */
    public static final int DEFAULT = 10;

    /** 最大查询数量 */
    public static final int MAX = 100;
  }
}
