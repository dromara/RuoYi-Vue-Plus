package org.dromara.common.oss.io;

import org.dromara.common.oss.exception.S3StorageException;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.function.Consumer;

/**
 * 输出流下载订阅器
 *
 * @author 秋辞未寒
 */
public class OutputStreamDownloadSubscriber implements Consumer<ByteBuffer>, AutoCloseable {

    private final WritableByteChannel channel;

    private final boolean allowAutoClose;

    private OutputStreamDownloadSubscriber(WritableByteChannel channel, boolean allowAutoClose) {
        this.channel = channel;
        this.allowAutoClose = allowAutoClose;
    }

    private OutputStreamDownloadSubscriber(OutputStream out, boolean allowAutoClose) {
        this.allowAutoClose = allowAutoClose;
        // 创建可写入的字节通道
        if (out instanceof FileOutputStream outputStream) {
            // 如果是文件输入流，直接获取文件输出流的 Channel
            channel = outputStream.getChannel();
        } else {
            channel = Channels.newChannel(out);
        }
    }

    @Override
    public void accept(ByteBuffer byteBuffer) {
        try {
            while (byteBuffer.hasRemaining()) {
                channel.write(byteBuffer);
            }
        } catch (Exception e) {
            throw S3StorageException.form(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (channel.isOpen() && allowAutoClose) {
            channel.close();
        }
    }

    /**
     * 创建一个输出流下载订阅器
     *
     * @param out 输出流
     * @return 输出流下载订阅器
     */
    public static OutputStreamDownloadSubscriber create(OutputStream out) {
        return create(out, false);
    }

    /**
     * 创建一个输出流下载订阅器
     *
     * @param out            输出流
     * @param allowAutoClose 是否允许自动关闭流
     * @return 输出流下载订阅器
     */
    public static OutputStreamDownloadSubscriber create(OutputStream out, boolean allowAutoClose) {
        return new OutputStreamDownloadSubscriber(out, allowAutoClose);
    }

    /**
     * 创建一个输出流下载订阅器
     *
     * @param channel 可写字节通道
     * @return 输出流下载订阅器
     */
    public static OutputStreamDownloadSubscriber create(WritableByteChannel channel) {
        return create(channel, false);
    }

    /**
     * 创建一个输出流下载订阅器
     *
     * @param channel        可写字节通道
     * @param allowAutoClose 是否允许自动关闭流
     * @return 输出流下载订阅器
     */
    public static OutputStreamDownloadSubscriber create(WritableByteChannel channel, boolean allowAutoClose) {
        return new OutputStreamDownloadSubscriber(channel, allowAutoClose);
    }

}
