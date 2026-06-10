package org.dromara.system.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.system.domain.SysOssExt;
import org.dromara.system.domain.bo.SysOssBo;
import org.dromara.system.domain.vo.SysOssVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * 文件上传 服务层
 *
 * @author Lion Li
 */
public interface ISysOssService {

    /**
     * 查询OSS对象存储列表
     *
     * @param sysOss    OSS对象存储分页查询对象
     * @param pageQuery 分页查询实体类
     * @return 结果
     */
    PageResult<SysOssVo> queryPageList(SysOssBo sysOss, PageQuery pageQuery);

    /**
     * 根据一组 ossIds 获取对应的 SysOssVo 列表
     *
     * @param ossIds 一组文件在数据库中的唯一标识集合
     * @return 包含 SysOssVo 对象的列表
     */
    List<SysOssVo> listByIds(Collection<Long> ossIds);

    /**
     * 根据 ossId 从缓存或数据库中获取 SysOssVo 对象
     *
     * @param ossId 文件在数据库中的唯一标识
     * @return SysOssVo 对象，包含文件信息
     */
    SysOssVo getById(Long ossId);

    /**
     * 上传 MultipartFile 到对象存储服务，并保存文件信息到数据库
     *
     * @param file 要上传的 MultipartFile 对象
     * @param ossExt 扩展信息
     * @return 上传成功后的 SysOssVo 对象，包含文件信息
     */
    SysOssVo upload(MultipartFile file, SysOssExt ossExt);

    /**
     * 上传文件到对象存储服务，并保存文件信息到数据库
     *
     * @param file 要上传的文件对象
     * @param ossExt 扩展信息
     * @return 上传成功后的 SysOssVo 对象，包含文件信息
     */
    SysOssVo upload(File file, SysOssExt ossExt);

    /**
     * 文件下载方法，支持一次性下载完整文件
     *
     * @param ossId OSS对象ID
     */
    ResponseEntity<byte[]> download(Long ossId);

    /**
     * 删除OSS对象存储
     *
     * @param ids     OSS对象ID串
     * @param isValid 判断是否需要校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
