package com.dromara.flow.ui.config.idm;


import com.dromara.flow.ui.domain.bo.GroupCodeBo;
import com.dromara.flow.ui.domain.bo.GroupNameBo;
import com.dromara.flow.ui.domain.bo.GroupQueryBo;
import com.dromara.flow.ui.mapper.CustomMybatisMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cmd.AbstractCustomSqlExecution;
import org.flowable.idm.api.Group;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.GroupQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntity;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.AbstractIdmDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.GroupDataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @Author: 土豆仙
 * @Date: 2021/11/12 10:05
 * @Description: 覆写组查询
 */
@Slf4j
@Component
public class CustomGroupDataManager extends AbstractIdmDataManager<GroupEntity> implements GroupDataManager {


    public CustomGroupDataManager(IdmEngineConfiguration idmEngineConfiguration) {
        super(idmEngineConfiguration);
    }

    @Autowired
    @Lazy
    private ManagementService managementService;

    @Override
    public Class<? extends GroupEntity> getManagedEntityClass() {
        return GroupEntityImpl.class;
    }

    @Override
    public GroupEntity create() {
        return new GroupEntityImpl();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findGroupByQueryCriteria(GroupQueryImpl query) {
        if (log.isDebugEnabled()) {
            log.debug("已重写自定义组查询：findGroupByQueryCriteria");
        }
        boolean isReturnList = true;
        GroupQueryBo groupQueryBo = transformQueryParamToGroupDTO(query);
        if (StringUtils.isNotBlank(query.getId())) {
            groupQueryBo.setReturnList(false);
            isReturnList = false;
        }

        List<Group> groups = managementService.executeCustomSql(new AbstractCustomSqlExecution<CustomMybatisMapper, List<Group>>(CustomMybatisMapper.class) {
            @Override
            public List<Group> execute(CustomMybatisMapper customMybatisMapper) {
                return customMybatisMapper.findGroupList(groupQueryBo);
            }
        });

        return groups;
        //return getDbSqlSession().selectList("selectGroupByQueryCriteria", query, getManagedEntityClass());
    }

    private GroupQueryBo transformQueryParamToGroupDTO(GroupQueryImpl query) {
        GroupQueryBo groupQueryBo = new GroupQueryBo();
        //约定  groupId  解析为  部门code.角色code
        List<String> groupIds = new ArrayList<>();
        if (StringUtils.isNotEmpty(query.getId())) {
            groupIds.add(query.getId());
        }
        if (CollectionUtils.isNotEmpty(query.getIds())) {
            groupIds.addAll(query.getIds());
        }
        List<GroupCodeBo> groupBoList = groupIds.stream()
            .map(GroupCodeBo::new)
            .collect(Collectors.toList());

        groupQueryBo.setGroupCodeBos(groupBoList);

        //约定  groupName 解析为   区域name/部门name/角色name
        groupQueryBo.setGroupNameBo(new GroupNameBo(query.getName()));


        //userId
        List<String> userNames = new ArrayList<>();
        if (StringUtils.isNotEmpty(query.getUserId())) {
            userNames.add(query.getUserId());
        }

        if (CollectionUtils.isNotEmpty(query.getIds())) {
            userNames.addAll(query.getIds());
        }

        groupQueryBo.setUserNames(userNames);

        return groupQueryBo;
    }

    @Override
    public long findGroupCountByQueryCriteria(GroupQueryImpl query) {
        if (log.isDebugEnabled()) {
            log.debug("已重写自定义组查询：findGroupCountByQueryCriteria");
        }
        //return (Long) getDbSqlSession().selectOne("selectGroupCountByQueryCriteria", query);
        List<Group> groupByQueryCriteria = findGroupByQueryCriteria(query);
        return groupByQueryCriteria.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findGroupsByUser(String userId) {
        if (log.isDebugEnabled()) {
            log.debug("已重写自定义组查询：findGroupsByUser");
        }
        GroupQueryImpl groupQuery = new GroupQueryImpl();
        groupQuery.groupMember(userId);
        List<Group> groupByQueryCriteria = findGroupByQueryCriteria(groupQuery);
        return groupByQueryCriteria;
        //return getDbSqlSession().selectList("selectGroupsByUserId", userId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Group> findGroupsByPrivilegeId(String privilegeId) {
        if (log.isDebugEnabled()) {
            log.debug("未重写自定义组查询：findGroupsByPrivilegeId");
        }
        return getDbSqlSession().selectList("selectGroupsWithPrivilegeId", privilegeId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findGroupsByNativeQuery(Map<String, Object> parameterMap) {
        if (log.isDebugEnabled()) {
            log.debug("未重写自定义组查询：findGroupsByNativeQuery");
        }
        return getDbSqlSession().selectListWithRawParameter("selectGroupByNativeQuery", parameterMap);
    }

    @Override
    public long findGroupCountByNativeQuery(Map<String, Object> parameterMap) {
        if (log.isDebugEnabled()) {
            log.debug("未重写自定义组查询：findGroupCountByNativeQuery");
        }
        return (Long) getDbSqlSession().selectOne("selectGroupCountByNativeQuery", parameterMap);
    }
}
