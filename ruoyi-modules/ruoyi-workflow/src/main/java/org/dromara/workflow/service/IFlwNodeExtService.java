package org.dromara.workflow.service;

import org.dromara.workflow.domain.vo.ButtonPermissionVo;

import java.util.List;

/**
 * 流程节点扩展属性 服务层
 *
 * @author AprilWind
 */
public interface IFlwNodeExtService {

    /**
     * 从扩展属性构建按钮权限列表：根据 ext 中记录的权限值，标记每个按钮是否勾选
     *
     * @param ext 扩展属性 JSON 字符串
     * @return 按钮权限 VO 列表
     */
    List<ButtonPermissionVo> buildButtonPermissionsFromExt(String ext);

}
