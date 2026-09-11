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
package org.dromara.warm.flow.ui.service;

import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.utils.StreamUtils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;

import org.dromara.warm.flow.dto.FlowPage;
import org.dromara.warm.flow.dto.Tree;
import org.dromara.warm.flow.utils.*;
import org.dromara.warm.flow.ui.dto.HandlerFunDto;
import org.dromara.warm.flow.ui.dto.HandlerQuery;
import org.dromara.warm.flow.ui.dto.TreeFunDto;
import org.dromara.warm.flow.ui.utils.TreeUtil;
import org.dromara.warm.flow.ui.vo.HandlerAuth;
import org.dromara.warm.flow.ui.vo.HandlerFeedBackVo;
import org.dromara.warm.flow.ui.vo.HandlerSelectVo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程设计器-获取办理人权限设置列表接口
 *
 * @author warm
 */
public interface HandlerSelectService {

    /**
     * 获取办理人权限设置列表tabs页签，如：用户、角色和部门等，可以返回其中一种或者多种，按业务需求决定
     *
     * @return tabs页签
     */
    List<String> getHandlerType();

    /**
     * 获取用户列表、角色列表、部门列表等，可以返回其中一种或者多种，按业务需求决定
     *
     * @param query 查询参数
     * @return 结果
     */
    HandlerSelectVo getHandlerSelect(HandlerQuery query);

    /**
     * 办理人权限名称回显，兼容老项目，新项目重写提高性能
     *
     * @param storageIds 入库主键集合
     * @return 结果
     */
    default List<HandlerFeedBackVo> handlerFeedback(List<String> storageIds) {
        List<HandlerFeedBackVo> handlerFeedBackVos = new ArrayList<>();
        if (CollUtil.isEmpty(storageIds)) {
            return handlerFeedBackVos;
        }
        Map<String, String> authMap = new HashMap<>();
        List<String> handlerTypes = getHandlerType();
        if (CollUtil.isEmpty(handlerTypes)) {
            return handlerFeedBackVos;
        }
        for (String handlerType : handlerTypes) {
            HandlerQuery handlerQuery = new HandlerQuery();
            handlerQuery.setHandlerType(handlerType);
            HandlerSelectVo handlerSelectVo = getHandlerSelect(handlerQuery);
            if (ObjectUtil.isNotNull(handlerSelectVo)) {
                FlowPage<HandlerAuth> handlerAuths = handlerSelectVo.getHandlerAuths();
                List<HandlerAuth> rows = handlerAuths.getRows();
                if (CollUtil.isNotEmpty(rows)) {
                    authMap.putAll(StreamUtils.toMap(rows, HandlerAuth::getStorageId, HandlerAuth::getHandlerName));
                }
            }
        }

        // 遍历storageIds，按照原本的顺序回显名称
        for (String storageId : storageIds) {
            handlerFeedBackVos.add(new HandlerFeedBackVo(storageId
                , MapUtil.isEmpty(authMap) ? "" : authMap.get(storageId)));
        }
        return handlerFeedBackVos;
    }

    default <T> HandlerSelectVo getHandlerSelectVo(HandlerFunDto<T> handlerFunDto) {
        List<HandlerAuth> handlerAuths = new ArrayList<>();
        // 遍历角色数据，封装为组件可识别的数据
        for (T obj : handlerFunDto.getList()) {
            handlerAuths.add(new HandlerAuth()
                .setStorageId(handlerFunDto.getStorageId() == null ? null : handlerFunDto.getStorageId().apply(obj))
                .setHandlerCode(handlerFunDto.getHandlerCode() == null ? null : handlerFunDto.getHandlerCode().apply(obj))
                .setHandlerName(handlerFunDto.getHandlerName() == null ? null : handlerFunDto.getHandlerName().apply(obj))
                .setCreateTime(handlerFunDto.getCreateTime() == null ? null : handlerFunDto.getCreateTime().apply(obj))
                .setGroupName(handlerFunDto.getGroupName() == null ? null : handlerFunDto.getGroupName().apply(obj)));
        }
        return getResult(handlerAuths, handlerFunDto.getTotal());
    }

    default <T, R> HandlerSelectVo getHandlerSelectVo(HandlerFunDto<T> handlerFunDto, TreeFunDto<R> treeFunDto) {
        HandlerSelectVo handlerSelectVo = getHandlerSelectVo(handlerFunDto);

        List<Tree> treeList = StreamUtils.toList(treeFunDto.getList(), org ->
            new Tree().setId(treeFunDto.getId() == null ? null : treeFunDto.getId().apply(org))
                .setName(treeFunDto.getName() == null ? null : treeFunDto.getName().apply(org))
                .setParentId(treeFunDto.getParentId() == null ? null : treeFunDto.getParentId().apply(org)));

        // 通过递归，构建树状结构
        return handlerSelectVo.setTreeSelections(TreeUtil.buildTree(treeList));
    }


    default HandlerSelectVo getResult(List<HandlerAuth> handlerAuths, long total) {
        return new HandlerSelectVo().setHandlerAuths(new FlowPage<HandlerAuth>()
            .setCode(HttpStatus.SUCCESS)
            .setMsg("查询成功")
            .setRows(handlerAuths)
            .setTotal(total));
    }

}
