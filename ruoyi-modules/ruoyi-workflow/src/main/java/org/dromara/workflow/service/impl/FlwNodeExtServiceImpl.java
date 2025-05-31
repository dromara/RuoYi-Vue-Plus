package org.dromara.workflow.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.DictTypeDTO;
import org.dromara.common.core.service.DictService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.warm.flow.ui.service.NodeExtService;
import org.dromara.warm.flow.ui.vo.NodeExt;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.ButtonPermissionEnum;
import org.dromara.workflow.common.enums.NodeExtEnum;
import org.dromara.workflow.domain.vo.ButtonPermissionVo;
import org.dromara.workflow.service.IFlwNodeExtService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 流程设计器-节点扩展属性
 *
 * @author AprilWind
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwNodeExtServiceImpl implements NodeExtService, IFlwNodeExtService {

    /**
     * 存储不同 dictType 对应的配置信息
     */
    private static final Map<String, ButtonPermission> CHILD_NODE_MAP = new HashMap<>();

    record ButtonPermission(String label, Integer type, Boolean must, Boolean multiple) {
    }

    static {
        CHILD_NODE_MAP.put(ButtonPermissionEnum.class.getSimpleName(),
            new ButtonPermission("权限按钮", 4, false, true));
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
        nodeExtList.add(buildNodeExt("wf_button_tab", "权限", 2,
            List.of(ButtonPermissionEnum.class)));
        // 自定义构建 规则参考 NodeExt 与 warm-flow文档说明
        // nodeExtList.add(buildNodeExt("xxx_xxx", "xxx", 1, List);
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
        ButtonPermission bp = CHILD_NODE_MAP.get(key);
        if (bp == null) {
            childNode.setType(1);
            childNode.setMust(false);
            childNode.setMultiple(true);
            return childNode;
        }
        // label名称
        childNode.setLabel(bp.label());
        // 1：输入框 2：输入框 3：下拉框 4：选择框
        childNode.setType(bp.type());
        // 是否必填
        childNode.setMust(bp.must());
        // 是否多选
        childNode.setMultiple(bp.multiple());
        return childNode;
    }

    /**
     * 从扩展属性构建按钮权限列表：根据 ext 中记录的权限值，标记每个按钮是否勾选
     *
     * @param ext 扩展属性 JSON 字符串
     * @return 按钮权限 VO 列表
     */
    @Override
    public List<ButtonPermissionVo> buildButtonPermissionsFromExt(String ext) {
        // 解析 ext 为 Map<code, Set<value>>，用于标记权限
        Map<String, Set<String>> permissionMap = JsonUtils.parseArray(ext, ButtonPermissionVo.class)
            .stream()
            .collect(Collectors.toMap(
                ButtonPermissionVo::getCode,
                item -> StringUtils.splitList(item.getValue()).stream()
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toSet()),
                (a, b) -> b,
                HashMap::new
            ));

        // 构建按钮权限列表，标记哪些按钮在 permissionMap 中出现（表示已勾选）
        return buildPermissionsFromSources(permissionMap, List.of(ButtonPermissionEnum.class));
    }

    /**
     * 将权限映射与按钮权限来源（枚举类或字典类型）进行匹配，生成权限视图列表
     * <p>
     * 使用说明：
     * - sources 支持传入多个来源类型，支持 NodeExtEnum 枚举类 或 字典类型字符串（dictType）
     * - 若需要扩展更多按钮权限，只需在 sources 中新增对应的枚举类或字典类型
     * <p>
     * 示例：
     * buildPermissionsFromSources(permissionMap, List.of(ButtonPermissionEnum.class, "custom_button_dict"));
     *
     * @param permissionMap 权限映射
     * @param sources       枚举类或字典类型列表
     * @return 按钮权限视图对象列表
     */
    @SuppressWarnings("unchecked cast")
    private List<ButtonPermissionVo> buildPermissionsFromSources(Map<String, Set<String>> permissionMap, List<Object> sources) {
        return sources.stream()
            .flatMap(source -> {
                if (source instanceof Class<?> clazz && NodeExtEnum.class.isAssignableFrom(clazz)) {
                    Set<String> selectedSet = permissionMap.getOrDefault(clazz.getSimpleName(), Collections.emptySet());
                    return extractDictItems(this.buildChildNode((Class<? extends NodeExtEnum>) clazz), selectedSet).stream();
                } else if (source instanceof String dictType) {
                    Set<String> selectedSet = permissionMap.getOrDefault(dictType, Collections.emptySet());
                    return extractDictItems(this.buildChildNode(dictType), selectedSet).stream();
                }
                return Stream.empty();
            }).toList();
    }

    /**
     * 从节点子项中提取字典项，并构建按钮权限视图对象列表
     *
     * @param childNode   子节点
     * @param selectedSet 已选中的值集
     * @return 按钮权限视图对象列表
     */
    private List<ButtonPermissionVo> extractDictItems(NodeExt.ChildNode childNode, Set<String> selectedSet) {
        return Optional.ofNullable(childNode)
            .map(NodeExt.ChildNode::getDict)
            .orElse(List.of())
            .stream()
            .map(dict -> new ButtonPermissionVo(dict.getValue(), selectedSet.contains(dict.getValue())))
            .toList();
    }

}
