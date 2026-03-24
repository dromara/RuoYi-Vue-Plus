package org.dromara.common.oss.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 可选项
 *
 * @author 秋辞未寒
 */
@Data
@EqualsAndHashCode
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Options {

    /**
     * 文件长度
     */
    private Long length;

    /**
     * 文件MD5摘要
     */
    private String md5Digest;

    /**
     * 内容类型
     */
    private String contentType;

    /**
     * 元数据
     */
    private Map<String, String> metadata;

    /**
     * 传输监听器
     */
    private Collection<TransferListener> transferListeners;

    /**
     * 添加元数据项
     */
    public Options addMetadataItem(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * 添加监听器
     */
    public Options addTransferListener(TransferListener transferListener) {
        if (this.transferListeners == null) {
            this.transferListeners = new ArrayList<>();
        }
        this.transferListeners.add(transferListener);
        return this;
    }

    /**
     * 创建可选项对象
     */
    public static Options builder() {
        return new Options();
    }
}
