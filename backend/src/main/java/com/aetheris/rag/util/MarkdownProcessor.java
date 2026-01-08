/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.util;

import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.exception.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.springframework.stereotype.Component;

/**
 * Markdown 文档处理器。
 *
 * <p>使用 CommonMark 解析 Markdown AST，记录每个切片的章节路径。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Slf4j
@Component
public class MarkdownProcessor {

  /** 默认切片大小（字符数） */
  private static final int DEFAULT_CHUNK_SIZE = 1000;

  /** 默认切片重叠大小（字符数） */
  private static final int DEFAULT_CHUNK_OVERLAP = 200;

  /**
   * 处理 Markdown 文件，生成文本切片。
   *
   * @param filePath Markdown 文件路径
   * @param chunkSize 切片大小（字符数）
   * @param chunkOverlap 切片重叠大小（字符数）
   * @return 切片列表
   * @throws IOException 如果文件读取失败
   */
  public List<Chunk> process(String filePath, int chunkSize, int chunkOverlap)
      throws IOException {
    log.info("开始处理 Markdown 文件: {}", filePath);

    List<Chunk> chunks = new ArrayList<>();

    // 读取文件内容
    String content = Files.readString(Path.of(filePath));
    log.debug("Markdown 文件长度: {} 字符", content.length());

    // 解析 AST
    Parser parser = Parser.builder().build();
    Node document = parser.parse(content);

    // 提取文本块和章节路径
    List<TextBlock> textBlocks = extractTextBlocks(document);
    log.debug("提取到 {} 个文本块", textBlocks.size());

    // 验证内容是否为空
    if (textBlocks.isEmpty()) {
      log.warn("Markdown 文档内容为空: {}", filePath);
      throw new BadRequestException("Markdown 文档内容为空，请确保文件包含有效的文本内容");
    }

    // 按章节路径合并文本并切片
    int chunkIndex = 0;
    StringBuilder currentText = new StringBuilder();
    List<String> currentChapterPath = new ArrayList<>();

    for (TextBlock block : textBlocks) {
      // 更新章节路径
      if (block.getHeadingLevel() > 0) {
        // 移除同级或更深层级的章节
        while (!currentChapterPath.isEmpty()
            && currentChapterPath.size() >= block.getHeadingLevel()) {
          currentChapterPath.remove(currentChapterPath.size() - 1);
        }
        // 添加当前章节
        currentChapterPath.add(block.getText());
      }

      // 检查是否需要创建新切片
      if (currentText.length() + block.getText().length() > chunkSize && currentText.length() > 0) {
        // 创建切片
        String chapterPath = String.join(">", currentChapterPath);
        Chunk chunk =
            Chunk.builder()
                .chunkIndex(chunkIndex++)
                .chunkText(currentText.toString())
                .locationInfo(chapterPath.isEmpty() ? "文档开头" : chapterPath)
                .chapterPath(chapterPath.isEmpty() ? null : chapterPath)
                .build();

        chunks.add(chunk);
        log.debug("创建切片 {}: 章节 {}, 长度 {}", chunkIndex - 1, chapterPath, currentText.length());

        // 处理重叠
        String overlapText = getOverlapText(currentText.toString(), chunkOverlap);
        currentText = new StringBuilder(overlapText);
      }

      currentText.append(block.getText());
    }

    // 处理最后一个切片
    if (currentText.length() > 0) {
      String chapterPath = String.join(">", currentChapterPath);
      Chunk chunk =
          Chunk.builder()
              .chunkIndex(chunkIndex++)
              .chunkText(currentText.toString())
              .locationInfo(chapterPath.isEmpty() ? "文档开头" : chapterPath)
              .chapterPath(chapterPath.isEmpty() ? null : chapterPath)
              .build();

      chunks.add(chunk);
      log.debug("创建切片 {}: 章节 {}, 长度 {}", chunkIndex - 1, chapterPath, currentText.length());
    }

    log.info("Markdown 处理完成，共生成 {} 个切片", chunks.size());
    return chunks;
  }

  /**
   * 使用默认参数处理 Markdown 文件。
   *
   * @param filePath Markdown 文件路径
   * @return 切片列表
   * @throws IOException 如果文件读取失败
   */
  public List<Chunk> process(String filePath) throws IOException {
    return process(filePath, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
  }

  /**
   * 提取文本块和章节路径。
   *
   * @param document Markdown AST 根节点
   * @return 文本块列表
   */
  private List<TextBlock> extractTextBlocks(Node document) {
    List<TextBlock> blocks = new ArrayList<>();
    extractTextBlocksRecursive(document, blocks);
    return blocks;
  }

  /**
   * 递归提取文本块。
   *
   * @param node 当前节点
   * @param blocks 文本块列表
   */
  private void extractTextBlocksRecursive(Node node, List<TextBlock> blocks) {
    if (node instanceof Heading) {
      Heading heading = (Heading) node;
      String text = extractText(heading);
      blocks.add(new TextBlock(text, heading.getLevel()));
    } else if (node instanceof Paragraph) {
      Paragraph paragraph = (Paragraph) node;
      String text = extractText(paragraph);
      if (!text.isEmpty()) {
        blocks.add(new TextBlock(text, 0));
      }
    } else if (node instanceof BulletList) {
      BulletList list = (BulletList) node;
      String text = extractText(list);
      if (!text.isEmpty()) {
        blocks.add(new TextBlock(text, 0));
      }
    } else if (node instanceof OrderedList) {
      OrderedList list = (OrderedList) node;
      String text = extractText(list);
      if (!text.isEmpty()) {
        blocks.add(new TextBlock(text, 0));
      }
    } else if (node instanceof FencedCodeBlock) {
      FencedCodeBlock codeBlock = (FencedCodeBlock) node;
      String text = codeBlock.getLiteral();
      if (!text.isEmpty()) {
        blocks.add(new TextBlock(text, 0));
      }
    }

    // 递归处理子节点
    Node child = node.getFirstChild();
    while (child != null) {
      extractTextBlocksRecursive(child, blocks);
      child = child.getNext();
    }
  }

  /**
   * 提取节点的文本内容。
   *
   * @param node 节点
   * @return 文本内容
   */
  private String extractText(Node node) {
    StringBuilder sb = new StringBuilder();
    TextAccumulator accumulator = new TextAccumulator(sb);
    node.accept(accumulator);
    return sb.toString().trim();
  }

  /**
   * 获取文本的重叠部分。
   *
   * @param text 原文本
   * @param overlapSize 重叠大小
   * @return 重叠文本
   */
  private String getOverlapText(String text, int overlapSize) {
    if (text.length() <= overlapSize) {
      return text;
    }
    return text.substring(text.length() - overlapSize);
  }

  /** 文本块记录（内部类）。 */
  @Data
  @AllArgsConstructor
  private static class TextBlock {
    private String text;
    private int headingLevel;
  }

  /** 文本累积器（内部类）。 */
  private static class TextAccumulator extends AbstractVisitor {
    private final StringBuilder sb;

    TextAccumulator(StringBuilder sb) {
      this.sb = sb;
    }

    @Override
    public void visit(Text text) {
      sb.append(text.getLiteral());
    }

    @Override
    public void visit(SoftLineBreak softLineBreak) {
      sb.append(" ");
    }

    @Override
    public void visit(HardLineBreak hardLineBreak) {
      sb.append("\n");
    }
  }
}
