package com.ruoyi.framework.handler;

import com.ruoyi.common.utils.StringUtils;
import org.redisson.api.NameMapper;

/*
 * redis缓存key前缀处理
 * @author ye
 * @create 2022/7/14 17:44
 */
public class KeyPrefixHandler implements NameMapper {
  
  private final String keyPrefix;
  
  //前缀为空 则返回空前缀
  public KeyPrefixHandler(String keyPrefix) {
    this.keyPrefix = StringUtils.isBlank(keyPrefix) ? "" : keyPrefix + ":";
  }
  
  //增加前缀
  @Override
  public String map(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    if (StringUtils.isBlank(keyPrefix)) {
      return name;
    }
    if (!name.startsWith(keyPrefix)) {
      return keyPrefix + name;
    } else {
      return name;
    }
  }
  
  //去除前缀
  @Override
  public String unmap(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    if (StringUtils.isBlank(keyPrefix)) {
      return name;
    }
    if (name.startsWith(keyPrefix)) {
      return name.substring(keyPrefix.length());
    }
    return name;
  }
}
