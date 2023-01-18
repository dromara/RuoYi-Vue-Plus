package com.ruoyi.test;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.demo.domain.TestDemo;
import com.ruoyi.demo.mapper.TestDemoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 加密单元测试案例
 *
 * @author 老马
 * @date 2023-01-12 08:53
 */
@SpringBootTest
@SaIgnore
@DisplayName("加密测试")
public class EncryptUnitTest {

    @Resource
    private TestDemoMapper demoMapper;

    @Test
    public void testCrypt() {
        TestDemo demo = new TestDemo();
        demo.setTestKey("测试的key");
        demo.setValue("测试的value");
        this.demoMapper.insert(demo);
        System.out.println(demo);
        TestDemo testDemo = this.demoMapper.selectById(demo.getId());
        System.out.println(testDemo);
    }

}
