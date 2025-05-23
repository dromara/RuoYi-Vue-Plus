package org.dromara.workflow.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.DictTypeDTO;
import org.dromara.common.core.service.DictService;
import org.dromara.warm.flow.ui.service.NodeExtService;
import org.dromara.warm.flow.ui.vo.NodeExt;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.ButtonPermissionEnum;
import org.dromara.workflow.common.enums.NodeExtEnum;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 流程设计器-节点扩展属性
 *
 * @author AprilWind
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwNodeExtServiceImpl implements NodeExtService {

    /**
     * 权限页code
     */
    private static final String PERMISSION_TAB = "wf_button_tab";

    /**
     * 权限页名称
     */
    private static final String PERMISSION_TAB_NAME = "权限";

    /**
     * 基础设置
     */
    private static final int TYPE_BASE_SETTING = 1;

    /**
     * 新页签
     */
    private static final int TYPE_NEW_TAB = 2;

    /**
     * 存储不同 dictType 对应的配置信息
     */
    private static final Map<String, Map<String, Object>> CHILD_NODE_MAP = new HashMap<>();

    static {
        CHILD_NODE_MAP.put(ButtonPermissionEnum.class.getSimpleName(),
            Map.of("label", "权限按钮", "type", 4, "must", false, "multiple", true));
    }

    private final DictService dictService;

    /**
     * 获取节点扩展属性
     *
     * @return 节点扩展属性列表
     */
    @Override
    public List<NodeExt> getNodeExt() {
        List<NodeExt> nodeExtList = new ArrayList<>();
        // 构建按钮权限页面
        nodeExtList.add(buildNodeExt(PERMISSION_TAB, PERMISSION_TAB_NAME, TYPE_NEW_TAB,
            List.of(ButtonPermissionEnum.class)));
        return nodeExtList;
    }

    /**
     * 构建一个 `NodeExt` 对象
     *
     * @param code    唯一编码
     * @param name    名称（新页签时，作为页签名称）
     * @param type    节点类型（1: 基础设置，2: 新页签）
     * @param sources 数据来源（枚举类或字典类型）
     * @return 构建的 `NodeExt` 对象
     */
    @SuppressWarnings("unchecked cast")
    private NodeExt buildNodeExt(String code, String name, int type, List<Object> sources) {
        NodeExt nodeExt = new NodeExt();
        nodeExt.setCode(code);
        nodeExt.setType(type);
        nodeExt.setName(name);
        nodeExt.setChilds(sources.stream()
            .map(source -> {
                if (source instanceof Class<?> clazz && NodeExtEnum.class.isAssignableFrom(clazz)) {
                    return buildChildNode((Class<? extends NodeExtEnum>) clazz);
                } else if (source instanceof String dictType) {
                    return buildChildNode(dictType);
                }
                return null;
            })
            .filter(ObjectUtil::isNotNull)
            .toList()
        );
        return nodeExt;
    }

    /**
     * 根据枚举类型构建一个 `ChildNode` 对象
     *
     * @param enumClass 枚举类，必须实现 `NodeExtEnum` 接口
     * @return 构建的 `ChildNode` 对象
     */
    private NodeExt.ChildNode buildChildNode(Class<? extends NodeExtEnum> enumClass) {
        if (!enumClass.isEnum()) {
            return null;
        }
        String simpleName = enumClass.getSimpleName();
        NodeExt.ChildNode childNode = buildChildNodeMap(simpleName);
        // 编码，此json中唯
        childNode.setCode(simpleName);
        // 字典，下拉框和复选框时用到
        childNode.setDict(Arrays.stream(enumClass.getEnumConstants())
            .map(NodeExtEnum.class::cast)
            .map(x ->
                new NodeExt.DictItem(x.getLabel(), x.getValue(), x.isSelected())
            ).toList());
        return childNode;
    }

    /**
     * 根据字典类型构建 `ChildNode` 对象
     *
     * @param dictType 字典类型
     * @return 构建的 `ChildNode` 对象
     */
    private NodeExt.ChildNode buildChildNode(String dictType) {
        DictTypeDTO dictTypeDTO = dictService.getDictType(dictType);
        if (ObjectUtil.isNull(dictTypeDTO)) {
            return null;
        }
        NodeExt.ChildNode childNode = buildChildNodeMap(dictType);
        // 编码，此json中唯一
        childNode.setCode(dictType);
        // label名称
        childNode.setLabel(dictTypeDTO.getDictName());
        // 描述
        childNode.setDesc(dictTypeDTO.getRemark());
        // 字典，下拉框和复选框时用到
        childNode.setDict(dictService.getDictData(dictType)
            .stream().map(x ->
                new NodeExt.DictItem(x.getDictLabel(), x.getDictValue(), Convert.toBool(x.getIsDefault(), false))
            ).toList());
        return childNode;
    }

    /**
     * 根据 CHILD_NODE_MAP 中的配置信息，构建一个基本的 ChildNode 对象
     * 该方法用于设置 ChildNode 的常规属性，例如 label、type、是否必填、是否多选等
     *
     * @param key CHILD_NODE_MAP 的 key
     * @return 返回构建好的 ChildNode 对象
     */
    private NodeExt.ChildNode buildChildNodeMap(String key) {
        NodeExt.ChildNode childNode = new NodeExt.ChildNode();
        Map<String, Object> map = CHILD_NODE_MAP.get(key);
        // label名称
        childNode.setLabel((String) map.get("label"));
        // 1：输入框 2：输入框 3：下拉框 4：选择框
        childNode.setType(Convert.toInt(map.get("type"), 1));
        // 是否必填
        childNode.setMust(Convert.toBool(map.get("must"), false));
        // 是否多选
        childNode.setMultiple(Convert.toBool(map.get("multiple"), true));
        return childNode;
    }

}
