package org.dromara.system.api;

import org.dromara.system.api.domain.OssDTO;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 通用 OSS服务
 *
 * @author Lion Li
 */
public interface OssService {

    /**
     * 通过ossId查询对应的url
     *
     * @param ossIds ossId串逗号分隔
     * @return url串逗号分隔
     */
    String selectUrlByIds(String ossIds);

    /**
     * 通过ossId查询列表
     *
     * @param ossIds ossId串逗号分隔
     * @return 列表
     */
    List<OssDTO> selectByIds(String ossIds);

    /**
     * 通过ossId查询文件信息
     *
     * @param ossId 文件ID
     * @return 文件信息
     */
    OssDTO selectById(Long ossId);

    /**
     * 通过ossId集合查询文件信息
     *
     * @param ossIds 文件ID 集合
     * @return 文件信息列表，不存在的文件不返回
     */
    List<OssDTO> selectListByIds(Collection<Long> ossIds);

    /**
     * 通过ossId获取文件输入流
     *
     * @param ossId 文件ID，不能为空
     * @return 文件输入流
     */
    InputStream getInputStream(Long ossId);

    /**
     * 根据文件ID读取内容，在回调内完成解析或复制
     *
     * @param ossId  文件ID，不能为空
     * @param reader 接收文件信息和输入流的处理函数，不能为空
     * @param <T>    处理结果类型
     * @return 回调处理结果，可以为null
     */
    <T> T readFile(Long ossId, BiFunction<OssDTO, InputStream, T> reader);

}
