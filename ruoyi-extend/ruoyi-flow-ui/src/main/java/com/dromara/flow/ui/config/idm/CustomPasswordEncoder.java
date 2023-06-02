package com.dromara.flow.ui.config.idm;

import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
*   @Author: 土豆仙
*   @Date: 2021/11/12 10:04
*   @Description: 密码加密器
*/
public class CustomPasswordEncoder implements PasswordEncoder,org.springframework.security.crypto.password.PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword, PasswordSalt passwordSalt) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 判断密码是否相同
     *
     * @param rawPassword 真实密码
     * @param encodedPassword 加密后字符
     * @return 结果
     */
    @Override
    public boolean isMatches(CharSequence rawPassword, String encodedPassword, PasswordSalt salt) {
      /*  BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();*/
        return true;
    }


    @Override
    public String encode(CharSequence rawPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        /*BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();*/
        return true;
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.upgradeEncoding(encodedPassword);
    }
}
