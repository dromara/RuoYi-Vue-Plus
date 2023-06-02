package com.dromara.flow.ui.config.idm;


import com.dromara.flow.ui.domain.bo.GroupCodeBo;
import com.dromara.flow.ui.domain.bo.UserQueryBo;
import com.dromara.flow.ui.mapper.CustomMybatisMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cmd.AbstractCustomSqlExecution;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.UserQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.UserEntity;
import org.flowable.idm.engine.impl.persistence.entity.UserEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.AbstractIdmDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.UserDataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @Author: 土豆仙
 * @Date: 2021/11/12 10:07
 * @Description: 覆写用户信息查询
 */
@Slf4j
@Component
public class CustomUserDataManager extends AbstractIdmDataManager<UserEntity> implements UserDataManager {
    public CustomUserDataManager(IdmEngineConfiguration idmEngineConfiguration) {
        super(idmEngineConfiguration);
    }

    @Autowired
    @Lazy
    private ManagementService managementService;

    @Override
    public Class<? extends UserEntity> getManagedEntityClass() {
        return UserEntityImpl.class;
    }

    @Override
    public UserEntity create() {
        return new UserEntityImpl();
    }

    /**
     *
     * @param query 流程引擎查询参数
     * @return 流程引擎展示视图
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<User> findUserByQueryCriteria(UserQueryImpl query) {
        if (log.isDebugEnabled()) {
            log.debug("已重写自定义组查询：findUserByQueryCriteria");
        }
        UserQueryBo userQueryBo = transformQueryImplToUserQueryBo(query);

        List<User> users = managementService.executeCustomSql(new AbstractCustomSqlExecution<CustomMybatisMapper, List<User>>(CustomMybatisMapper.class) {
            @Override
            public List<User> execute(CustomMybatisMapper customMybatisMapper) {
                return customMybatisMapper.selectUserList(userQueryBo);
            }
        });

        return users;
    }

    private UserQueryBo transformQueryImplToUserQueryBo(UserQueryImpl query) {
        UserQueryBo userQueryBo = new UserQueryBo();

        //设置userName
        if (StringUtils.isNotBlank(query.getId())) {
            userQueryBo.setUserName(query.getId());
        } else {
            userQueryBo.setUserName(query.getIdIgnoreCase());
        }

        userQueryBo.setUserNames(query.getIds());

        //设置nickName
        if (StringUtils.isNotBlank(query.getFirstName()) || StringUtils.isNotBlank(query.getLastName())) {
            String nickName = StringUtils.join(query.getFirstName(), query.getLastName());
            userQueryBo.setNickName(nickName);
        } else {
            userQueryBo.setNickName(query.getDisplayName());
        }

        //设置email
        userQueryBo.setEmail(query.getEmail());
        userQueryBo.setEmailLike(query.getEmailLike());


        //处理组映射
        List<GroupCodeBo> groupBoList = new ArrayList<>();
        if (StringUtils.isNotEmpty(query.getGroupId())) {
            groupBoList.add(new GroupCodeBo(query.getGroupId()));
        }
        if (!CollectionUtils.isEmpty(query.getGroupIds())) {

            List<GroupCodeBo> collect = query.getGroupIds().stream()
                .map(GroupCodeBo::new)
                .collect(Collectors.toList());

            groupBoList.addAll(collect);
        }
        userQueryBo.setGroupBoList(groupBoList);
        return userQueryBo;
    }

    @Override
    public long findUserCountByQueryCriteria(UserQueryImpl query) {
        if (log.isDebugEnabled()) {
            log.debug("已重写自定义组查询：findUserCountByQueryCriteria");
        }
        // return (Long) getDbSqlSession().selectOne("selectUserCountByQueryCriteria", query);
        return findUserByQueryCriteria(query).size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<User> findUsersByPrivilegeId(String privilegeId) {
        if (log.isDebugEnabled()) {
            log.debug("未重写自定义组查询：findUsersByPrivilegeId");
        }
        return getDbSqlSession().selectList("selectUsersWithPrivilegeId", privilegeId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<User> findUsersByNativeQuery(Map<String, Object> parameterMap) {
        if (log.isDebugEnabled()) {
            log.debug("未重写自定义组查询：findUsersByNativeQuery");
        }
        return getDbSqlSession().selectListWithRawParameter("selectUserByNativeQuery", parameterMap);
    }

    @Override
    public long findUserCountByNativeQuery(Map<String, Object> parameterMap) {
        if (log.isDebugEnabled()) {
            log.debug("未重写自定义组查询：findUserCountByNativeQuery");
        }
        return (Long) getDbSqlSession().selectOne("selectUserCountByNativeQuery", parameterMap);
    }
}
