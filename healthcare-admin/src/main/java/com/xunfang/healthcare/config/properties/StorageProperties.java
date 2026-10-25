package com.xunfang.healthcare.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置。当前使用本地磁盘实现。
 */
@Data
@ConfigurationProperties(prefix = "healthcare.storage")
public class StorageProperties {

    /**
     * 本地存储根目录。
     */
    private String localPath = "./storage";

    /**
     * 对外访问前缀。
     */
    private String urlPrefix = "/storage";
}
