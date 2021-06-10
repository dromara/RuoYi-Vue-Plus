package com.ruoyi.framework.config.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * swagger 配置属性
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "swagger")
public class SwaggerProperties {

    /**
     * 验证码类型
     */
    private Boolean enabled;
	/**
	 * 设置请求的统一前缀
	 */
	private String pathMapping;
    /**
     * 验证码类别
     */
    private String title;
    /**
     * 数字验证码位数
     */
    private String description;
    /**
     * 字符验证码长度
     */
    private String version;

	/**
	 * 联系方式
	 */
    private Contact contact;

    @Data
	@NoArgsConstructor
	public static class Contact{

		/**
		 * 联系人
		 **/
		private String name;
		/**
		 * 联系人url
		 **/
		private String url;
		/**
		 * 联系人email
		 **/
		private String email;

	}

}
