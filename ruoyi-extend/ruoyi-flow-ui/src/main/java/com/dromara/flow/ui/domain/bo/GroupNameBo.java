package com.dromara.flow.ui.domain.bo;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import liquibase.repackaged.org.apache.commons.text.StringSubstitutor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 业务组名与流程组转换
 * Author: 土豆仙
 */
@Data
@NoArgsConstructor
public class GroupNameBo {

    /**
     * 默认占位符号 代表所有
     */
    private static final String STAR = "*";

    /**
     * 分割符号
     */
    private static final String DOT = ".";

    /**
     * 占位字符串定义 - 后期放入配置中，用于扩展
     */
    private static final String PLACEHOLDER_STRING = "${deptName}.${roleName}";

    /**
     * 取 定义中 ${}内的内容
     */
    private static final String pattern = "\\$\\{(.*?)}";

    /**
     * deptName：*  、roleName：*  ...
     */
    private static final Map<String, String> nameMapInit = new HashMap<>();

    /**
     * deptName：*  、roleName：*  ...
     */
    private Map<String, String> nameMap = new HashMap<>();

    /**
     * deptName、roleName ...
     */
    private static final List<String> nameList = new ArrayList<>();

    //初始化
    static {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(PLACEHOLDER_STRING);
        while (m.find()) {
            // ${}和 里面的内容
            String Name = m.group(1);
            nameList.add(Name);
            nameMapInit.put(Name, STAR);
        }
    }


    public GroupNameBo(String gourpName) {
        transferToBusiness(gourpName);
    }


    /**
     * @param gourpName 流程引擎编码转换（约定）
     */
    public void transferToBusiness(String gourpName) {

        List<String> split = StrUtil.split(gourpName, DOT);
        if (CollectionUtil.isNotEmpty(split) && split.size() <= nameList.size()) {

            //遍历
            for (int i = 0; i < split.size(); i++) {
                nameMap.put(nameList.get(i), split.get(i));
            }
        }
    }

    /**
     * @return 组名
     */
    public String getGourpname() {

        if (MapUtil.isEmpty(nameMap)) {
            nameMap.putAll(nameMapInit);
        }
        StringSubstitutor strSubstitutor = new StringSubstitutor(nameMap);


        return strSubstitutor.replace(PLACEHOLDER_STRING);
    }


    /**
     * @return 名称编码-值 键值对
     */
    public Map<String, String> getnameMap() {

        if (MapUtil.isEmpty(nameMap)) {
            nameMap.putAll(nameMapInit);
        }

        return nameMap;

    }

}
