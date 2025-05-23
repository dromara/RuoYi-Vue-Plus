package org.dromara.workflow.service;

import org.dromara.common.core.domain.dto.UserDTO;

import java.util.List;

/**
 * 流程设计器-获取办理人
 *
 * @author AprilWind
 */
public interface IFlwTaskAssigneeService {

    /**
     * 根据存储标识符（storageId）解析分配类型和ID，并获取对应的用户列表
     *
     * @param storageId 包含分配类型和ID的字符串（例如 "user:123" 或 "role:456"）
     * @return 与分配类型和ID匹配的用户列表，如果格式无效则返回空列表
     */
    List<UserDTO> fetchUsersByStorageId(String storageId);

}
