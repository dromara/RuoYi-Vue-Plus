package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warm.flow.core.entity.Definition;
import com.warm.flow.core.enums.PublishStatus;
import com.warm.flow.core.service.DefService;
import com.warm.flow.core.utils.page.Page;
import com.warm.flow.orm.entity.FlowDefinition;
import com.warm.flow.orm.mapper.FlowDefinitionMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.vo.FlowDefinitionVo;
import org.dromara.workflow.service.IFlwDefinitionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 流程定义 服务层实现
 *
 * @author may
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwDefinitionServiceImpl implements IFlwDefinitionService {
    private final DefService defService;

    private final FlowDefinitionMapper flowDefinitionMapper;

    /**
     * 分页查询
     *
     * @param flowDefinition 参数
     * @return 返回分页列表
     */
    @Override
    public TableDataInfo<FlowDefinitionVo> page(FlowDefinition flowDefinition, PageQuery pageQuery) {
        Page<Definition> page = Page.pageOf(pageQuery.getPageNum(), pageQuery.getPageSize());
        page = defService.orderByCreateTime().desc().page(flowDefinition, page);
        TableDataInfo<FlowDefinitionVo> build = TableDataInfo.build();
        build.setRows(BeanUtil.copyToList(page.getList(), FlowDefinitionVo.class));
        build.setTotal(page.getTotal());
        return build;
    }

    /**
     * 获取历史流程定义列表
     *
     * @param flowCode 参数
     */
    @Override
    public List<FlowDefinitionVo> getHisListByKey(String flowCode) {
        LambdaQueryWrapper<FlowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowDefinition::getFlowCode, flowCode);
        wrapper.in(FlowDefinition::getIsPublish, Arrays.asList(PublishStatus.UNPUBLISHED.getKey(), PublishStatus.EXPIRED.getKey()));
        List<FlowDefinition> list = flowDefinitionMapper.selectList(wrapper);
        return BeanUtil.copyToList(list, FlowDefinitionVo.class);
    }

    /**
     * 导入流程定义
     *
     * @param file 文件
     */
    @Override
    public boolean importXml(MultipartFile file) {
        try {
            Definition definition = defService.importXml(file.getInputStream());
            List<FlowDefinition> list = flowDefinitionMapper.selectList(
                new LambdaQueryWrapper<FlowDefinition>().eq(FlowDefinition::getFlowCode, definition.getFlowCode()));
            List<FlowDefinition> definitionList = StreamUtils.filter(list, item -> !item.getId().equals(definition.getId()));
            if (CollUtil.isEmpty(definitionList)) {
                defService.publish(definition.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * 导出流程定义
     *
     * @param id       流程定义id
     * @param response 响应
     * @throws IOException 异常
     */
    @Override
    public void exportDefinition(Long id, HttpServletResponse response) throws IOException {
        Document document = defService.exportXml(id);
        // 设置生成xml的格式
        OutputFormat of = OutputFormat.createPrettyPrint();
        // 设置编码格式
        of.setEncoding("UTF-8");
        of.setIndent(true);
        of.setIndent("    ");
        of.setNewlines(true);

        // 创建一个xml文档编辑器
        XMLWriter writer = new XMLWriter(response.getOutputStream(), of);
        writer.setEscapeText(false);
        response.reset();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/x-msdownload");
        response.setHeader("Content-Disposition", "attachment;");
        writer.write(document);
        writer.close();
    }
}
