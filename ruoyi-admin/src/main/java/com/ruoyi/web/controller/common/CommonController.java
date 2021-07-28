package com.ruoyi.web.controller.common;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * 通用请求处理
 *
 * @author ruoyi
 */
@RestController
public class CommonController
{
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    /**
     * 通用下载请求
     *
     * @param fileName 文件名称
     * @param delete 是否删除
     */
    @GetMapping("common/download")
    public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request)
    {
        try
        {
            if (!FileUtils.checkAllowDownload(fileName))
            {
                throw new Exception(StrUtil.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = RuoYiConfig.getDownloadPath() + fileName;
			File file = new File(filePath);
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
			FileUtils.writeToStream(file, response.getOutputStream());
            if (delete)
            {
				FileUtils.del(file);
            }
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 本地资源通用下载
     */
    @GetMapping("/common/download/resource")
    public void resourceDownload(String resource, HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        try
        {
            if (!FileUtils.checkAllowDownload(resource))
            {
                throw new Exception(StrUtil.format("资源文件({})非法，不允许下载。 ", resource));
            }
            // 本地资源路径
            String localPath = RuoYiConfig.getProfile();
            // 数据库资源地址
            String downloadPath = localPath + StrUtil.subAfter(resource, Constants.RESOURCE_PREFIX,false);
            // 下载名称
            String downloadName = StrUtil.subAfter(downloadPath, "/",true);
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
			File file = new File(downloadPath);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeToStream(file, response.getOutputStream());
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }
}
