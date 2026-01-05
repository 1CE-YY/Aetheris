package com.aetheris.rag.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;

/**
 * .env 文件自动加载配置。
 *
 * <p>在 Spring Boot 启动时自动加载项目根目录的 .env 文件
 * 优先级：.env 文件 > 系统环境变量 > application.yml 默认值
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class DotenvConfig {

    @PostConstruct
    public void loadEnvFile() {
        // 查找 .env 文件（向上查找最多3级目录）
        File envFile = findEnvFile();

        if (envFile != null && envFile.exists()) {
            try {
                Dotenv dotenv = Dotenv.configure()
                    .directory(envFile.getParent())
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

                // 将 .env 文件中的变量设置为系统属性
                dotenv.entries().forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    // 优先级：.env > 系统环境变量
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                        log.debug("从 .env 加载变量: {}={}", key, maskValue(value));
                    }
                });

                log.info("✅ .env 文件已加载: {}", envFile.getAbsolutePath());

            } catch (DotenvException e) {
                log.warn("⚠️  .env 文件加载失败: {}", e.getMessage());
            }
        } else {
            log.info("ℹ️  .env 文件不存在，使用 application.yml 默认配置");
        }
    }

    /**
     * 查找 .env 文件（向上查找最多3级目录）
     */
    private File findEnvFile() {
        File currentDir = new File(System.getProperty("user.dir"));

        for (int i = 0; i < 3; i++) {
            File envFile = new File(currentDir, ".env");
            if (envFile.exists()) {
                return envFile;
            }
            currentDir = currentDir.getParentFile();
            if (currentDir == null) {
                break;
            }
        }

        return null;
    }

    /**
     * 隐藏敏感值（用于日志输出）
     */
    private String maskValue(String value) {
        if (value == null || value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }
}
