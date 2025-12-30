package com.aetheris.rag.gateway;

/**
 * AI 模型操作的网关接口。
 *
 * <p>此接口定义了所有 AI 模型交互的契约，为嵌入和聊天操作提供单一出口。
 * 这种集中式方法确保：
 *
 * <ul>
 *   <li>一致的错误处理和重试逻辑
 *   <li>统一的速率限制和超时管理
 *   <li>用于成本优化的缓存
 *   <li>安全和日志策略
 *   <li>服务降级的回退策略
 * </ul>
 *
 * <p><strong>重要：</strong> 所有业务代码必须使用此网关，而不是直接调用模型 API。
 * 这由项目宪法的原则 4（模型访问）强制执行。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
public interface ModelGateway {

  /**
   * 为给定文本生成嵌入向量。
   *
   * <p>此方法：
   *
   * <ul>
   *   <li>首先检查缓存以避免冗余 API 调用
   *   <li>对瞬态故障实现重试逻辑
   *   <li>强制执行速率限制
   *   <li>清理日志以保护敏感数据
   * </ul>
   *
   * @param text 要嵌入的输入文本
   * @return 嵌入向量（浮点数组）
   * @throws ModelException 如果嵌入操作在所有重试后失败
   */
  float[] embed(String text);

  /**
   * 使用 LLM 生成聊天响应。
   *
   * <p>此方法：
   *
   * <ul>
   *   <li>对瞬态故障实现重试逻辑
   *   <li>强制执行速率限制
   *   <li>提供服务降级的回退策略
   *   <li>清理日志以保护敏感数据
   * </ul>
   *
   * @param prompt 输入提示
   * @return 生成的响应文本
   * @throws ModelException 如果聊天操作在所有重试后失败（除非启用了回退）
   */
  String chat(String prompt);

  /**
   * 使用系统消息生成聊天响应。
   *
   * <p>与 {@link #chat(String)} 相同，但包含系统消息以指导模型的行为。
   *
   * @param systemMessage 系统消息（例如，指令）
   * @param userMessage 用户消息
   * @return 生成的响应文本
   * @throws ModelException 如果聊天操作在所有重试后失败
   */
  String chat(String systemMessage, String userMessage);
}
