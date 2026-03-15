package org.dromara.common.oss.s3.io;

import org.dromara.common.oss.s3.exception.S3StorageException;

import java.io.FileOutputStream;
import java.io.IOException;
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

    private OutputStreamDownloadSubscriber(WritableByteChannel channel) {
        this.channel = channel;
    }

    private OutputStreamDownloadSubscriber(OutputStream out) {
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
        try (channel) {
            while (byteBuffer.hasRemaining()) {
                channel.write(byteBuffer);
            }
        } catch (IOException e) {
            throw S3StorageException.of(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (channel.isOpen()) {
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
        return new OutputStreamDownloadSubscriber(out);
    }

    /**
     * 创建一个输出流下载订阅器
     *
     * @param channel 可写字节通道
     * @return 输出流下载订阅器
     */
    public static OutputStreamDownloadSubscriber create(WritableByteChannel channel) {
        return new OutputStreamDownloadSubscriber(channel);
    }

}
