/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dromara.warm.flow.service;
import org.dromara.warm.flow.entity.*;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;

import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.entity.FlowTask;
import org.dromara.warm.flow.entity.FlowUser;
import org.dromara.warm.flow.enums.UserType;
import org.dromara.warm.flow.service.WarmServiceImpl;


import org.dromara.common.core.utils.StreamUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;






import org.dromara.common.core.utils.SpringUtils;
import org.springframework.stereotype.Service;
import org.dromara.warm.flow.mapper.FlowUserMapper;




/**
 * 流程用户Service业务层处理
 *
 * @author xiarg
 * @since 2024/5/10 13:57
 */
@Service
public class UserService extends WarmServiceImpl<FlowUser> {
    public List<FlowUser> taskAddUsers(List<FlowTask> addTasks) {
        List<FlowUser> taskUserList = new ArrayList<>();
        if (CollUtil.isNotEmpty(addTasks)) {
            StreamUtils.toList(addTasks, task -> taskUserList.addAll(taskAddUser(task)));
        }
        return taskUserList;
    }

    public List<FlowUser> taskAddUser(FlowTask task) {
        // 遍历权限集合，生成流程节点的权限
        List<FlowUser> userList = StreamUtils.toList(task.getPermissionList()
            , permission -> structureUser(task.getId(), permission, UserType.APPROVAL.getKey()));
        task.setUserList(userList);
        return userList;
    }

    public void deleteByTaskIds(List<Long> ids) {
        getMapper().deleteByTaskIds(ids);
    }

    public List<String> getPermission(Long associated, String... types) {
        if (ArrayUtil.isEmpty(types)) {
            return StreamUtils.toList(list(new FlowUser().setAssociated(associated)), FlowUser::getProcessedBy);
        }
        if (types.length == 1) {
            return StreamUtils.toList(list(new FlowUser().setAssociated(associated).setType(types[0]))
                , FlowUser::getProcessedBy);
        }
        return StreamUtils.toList(getMapper().listByAssociatedAndTypes(Collections.singletonList(associated), types)
            , FlowUser::getProcessedBy);
    }

    public List<FlowUser> listByAssociatedAndTypes(Long associated, String... types) {
        if (ArrayUtil.isEmpty(types)) {
            return list(new FlowUser().setAssociated(associated));
        }
        if (types.length == 1) {
            return list(new FlowUser().setAssociated(associated).setType(types[0]));
        }
        return getMapper().listByAssociatedAndTypes(Collections.singletonList(associated), types);
    }

    public List<FlowUser> getByAssociateds(List<Long> associateds, String... types) {
        if (CollUtil.isNotEmpty(associateds) && associateds.size() == 1) {
            return listByAssociatedAndTypes(associateds.get(0), types);
        }
        return getMapper().listByAssociatedAndTypes(associateds, types);
    }

    public List<FlowUser> listByProcessedBys(Long associated, String processedBy, String... types) {
        if (ArrayUtil.isEmpty(types)) {
            return list(new FlowUser().setAssociated(associated).setProcessedBy(processedBy));
        }
        if (types.length == 1) {
            return list(new FlowUser().setAssociated(associated).setProcessedBy(processedBy).setType(types[0]));
        }
        return getMapper().listByProcessedBys(associated, Collections.singletonList(processedBy), types);
    }

    public List<FlowUser> getByProcessedBys(Long associated, List<String> processedBys, String... types) {
        if (CollUtil.isNotEmpty(processedBys) && processedBys.size() == 1) {
            return listByProcessedBys(associated, processedBys.get(0), types);
        }
        return getMapper().listByProcessedBys(associated, processedBys, types);
    }


    public boolean updatePermission(Long associated, List<String> permissions, String type, boolean clear,
                                    String handler) {
        // 判断是否clear，如果是true，则先删除当前关联id用户数据
        if (clear) {
            getMapper().delete(new FlowUser().setAssociated(associated).setCreateBy(handler));
        }
        // 再新增权限人
        saveBatch(StreamUtils.toList(permissions, permission -> structureUser(associated, permission, type, handler)));
        return true;
    }

    public List<FlowUser> structureUser(Long associated, List<String> permissionList, String type) {
        return StreamUtils.toList(permissionList, permission -> structureUser(associated, permission, type, null));
    }

    public FlowUser structureUser(Long associated, String permission, String type) {
        return structureUser(associated, permission, type, null);
    }

    public List<FlowUser> structureUser(Long associated, List<String> permissionList, String type, String handler) {
        return StreamUtils.toList(permissionList, permission -> structureUser(associated, permission, type, handler));
    }

    public FlowUser structureUser(Long associated, String permission, String type, String handler) {
        Date now = new Date();
        FlowUser user = new FlowUser()
            .setType(type)
            .setProcessedBy(permission)
            .setAssociated(associated)
            .setCreateBy(handler);
        FlowEngine.dataFillHandler().idFill(user);
        return user;
    }


    @Override
    public FlowUserMapper getMapper() {
        return SpringUtils.getBean(FlowUserMapper.class);
    }
}