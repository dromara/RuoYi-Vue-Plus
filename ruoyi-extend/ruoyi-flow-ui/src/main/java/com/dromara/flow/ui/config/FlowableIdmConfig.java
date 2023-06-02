package com.dromara.flow.ui.config;


import com.dromara.flow.ui.config.idm.CustomGroupDataManager;
import com.dromara.flow.ui.config.idm.CustomPasswordEncoder;
import com.dromara.flow.ui.config.idm.CustomUserDataManager;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntityManagerImpl;
import org.flowable.idm.engine.impl.persistence.entity.UserEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.UserEntityManagerImpl;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.idm.FlowableIdmProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @Author: 土豆仙
 * @Date: 2021/9/28 19:30
 * @Description: 一旦使用视图效率不高，在这注入覆写类
 */
@Configuration
public class FlowableIdmConfig implements EngineConfigurationConfigurer<SpringIdmEngineConfiguration> {

    @Autowired
    private CustomUserDataManager customUserDataManager;

    @Autowired
    private CustomGroupDataManager customGroupDataManager;

    @Override
    public void configure(SpringIdmEngineConfiguration configuration) {

        //密码加密器和自建用户体系保持一致
        CustomPasswordEncoder bc = new CustomPasswordEncoder();
        configuration.setPasswordEncoder(bc);

        //用户
        configuration.setUserDataManager(customUserDataManager);
        UserEntityManager userEntityManager = configuration.getUserEntityManager();
        if (userEntityManager != null) {
            UserEntityManagerImpl userEntityManagerImpl = new UserEntityManagerImpl(configuration, customUserDataManager);
            configuration.setUserEntityManager(userEntityManagerImpl);
        }

        //用户-组关系
        /*configuration.setMembershipDataManager(customMembershipDataManager);
        configuration.setMembershipEntityManager(customMembershipEntityManager);*/


        //组
        configuration.setGroupDataManager(customGroupDataManager);
        GroupEntityManager groupEntityManager = configuration.getGroupEntityManager();
        if (groupEntityManager == null) {
            GroupEntityManagerImpl groupEntityManagerImpl = new GroupEntityManagerImpl(configuration, customGroupDataManager);
            configuration.setGroupEntityManager(groupEntityManagerImpl);
        }


    }


    @Bean
    public PasswordEncoder passwordEncoder(FlowableIdmProperties idmProperties) {

        PasswordEncoder encoder = new CustomPasswordEncoder();

        return encoder;
    }

  /*  @Bean
    public CustomGroupEntityManager customGroupEntityManager(IdmEngineConfiguration configuration) {
        return new CustomGroupEntityManager(configuration, configuration.getGroupDataManager());
    }*/


   /* @Bean
    public CustomUserEntityManager customUserEntityManager(IdmEngineConfiguration configuration) {
        return new CustomUserEntityManager(configuration, new CustomUserDataManager(configuration));
    }*/
}
