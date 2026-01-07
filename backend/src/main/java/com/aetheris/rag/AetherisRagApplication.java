package com.aetheris.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aetheris RAG 系统 - 主应用程序入口点
 *
 * <p>此应用程序提供使用 RAG（检索增强生成）架构的学习资源检索和推荐系统。
 *
 * <p>主要功能：
 *
 * <ul>
 *   <li>使用 Redis Stack 向量嵌入进行语义搜索
 *   <li>基于 RAG 的问答，带有引用来源
 *   <li>使用轻量级用户画像的个性化推荐
 *   <li>支持 PDF 和 Markdown 文档入库
 * </ul>
 *
 * <p>技术栈：
 *
 * <ul>
 *   <li>Spring Boot 3.5+ 和 Java 21 虚拟线程
 *   <li>MyBatis 用于数据库访问
 *   <li>LangChain4j 用于 AI 模型集成（智谱 AI）
 *   <li>Redis Stack 用于向量存储和缓存
 *   <li>MySQL 8 用于结构化数据
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AetherisRagApplication {

  /**
   * Aetheris RAG 应用程序的主入口点。
   *
   * @param args 命令行参数
   */
  public static void main(String[] args) {
    SpringApplication.run(AetherisRagApplication.class, args);
  }
}
