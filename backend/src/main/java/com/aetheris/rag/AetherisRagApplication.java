package com.aetheris.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Aetheris RAG System - Main Application Entry Point
 *
 * <p>This application provides a learning resource retrieval and recommendation system using
 * RAG (Retrieval-Augmented Generation) architecture.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Semantic search using Redis Stack vector embeddings
 *   <li>RAG-based Q&A with citation sources
 *   <li>Personalized recommendations using lightweight user profiling
 *   <li>Support for PDF and Markdown document ingestion
 * </ul>
 *
 * <p>Technology Stack:
 *
 * <ul>
 *   <li>Spring Boot 3.5+ with Java 21 virtual threads
 *   <li>MyBatis for database access
 *   <li>LangChain4j for AI model integration (Zhipu AI)
 *   <li>Redis Stack for vector storage and caching
 *   <li>MySQL 8 for structured data
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
@SpringBootApplication
@EnableAsync
public class AetherisRagApplication {

  /**
   * Main entry point for the Aetheris RAG application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(AetherisRagApplication.class, args);
  }
}
