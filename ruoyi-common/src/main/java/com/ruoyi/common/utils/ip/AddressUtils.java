package com.ruoyi.common.utils.ip;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;

/**
 * 获取地址类
 *
 * @author ruoyi
 */
@Slf4j
public class AddressUtils {

	// IP地址查询
	public static final String IP_URL = "http://whois.pconline.com.cn/ipJson.jsp";

	// 未知地址
	public static final String UNKNOWN = "XX XX";

	public static String getRealAddressByIP(String ip) {
		String address = UNKNOWN;
		// 内网不查询
		if (NetUtil.isInnerIP(ip)) {
			return "内网IP";
		}
		if (RuoYiConfig.isAddressEnabled()) {
			try {
				String rspStr = HttpUtil.createGet(IP_URL)
					.body("ip=" + ip + "&json=true", Constants.GBK)
					.execute()
					.body();
				if (StrUtil.isEmpty(rspStr)) {
					log.error("获取地理位置异常 {}", ip);
					return UNKNOWN;
				}
				JSONObject obj = JSONObject.parseObject(rspStr);
				String region = obj.getString("pro");
				String city = obj.getString("city");
				return String.format("%s %s", region, city);
			} catch (Exception e) {
				log.error("获取地理位置异常 {}", ip);
			}
		}
		return address;
	}
}
