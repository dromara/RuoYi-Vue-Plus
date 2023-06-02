package com.dromara.flow.ui.domain.bo;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import liquibase.repackaged.org.apache.commons.text.StringSubstitutor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 业务组与流程组转换
 * Author: 土豆仙
 */
@NoArgsConstructor
public class GroupCodeBo {

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
    private static final String PLACEHOLDER_STRING = "${deptCode}.${roleCode}";

    /**
     * 取 定义中 ${}内的内容
     */
    private static final String pattern = "\\$\\{(.*?)}";

    /**
     * deptCode：*  、roleCode：*  ...
     */
    private Map<String, String> codeMap = new HashMap<>();

    /**
     * deptCode：*  、roleCode：*  ...
     */
    private static final Map<String, String> codeMapInit = new HashMap<>();

    /**
     * deptCode、roleCode ...
     */
    private static final List<String> codeList = new ArrayList<>();

    //初始化
    static {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(PLACEHOLDER_STRING);
        while (m.find()) {
            // ${}和 里面的内容
            String code = m.group(1);
            codeList.add(code);
            codeMapInit.put(code, STAR);
        }
    }


    public GroupCodeBo(String gourpCode) {
        transferToBusiness(gourpCode);
    }


    /**
     * @param gourpCode 流程引擎编码转换（约定）
     */
    public void transferToBusiness(String gourpCode) {

        List<String> split = StrUtil.split(gourpCode, DOT);
        if (CollectionUtil.isNotEmpty(split) && split.size() == codeList.size()) {

            //遍历
            for (int i = 0; i < codeList.size(); i++) {
                codeMap.put(codeList.get(i), split.get(i));
            }
        }
    }


    /**
     * @return 组编码
     */
    public String getGourpCode() {

        if (MapUtil.isEmpty(codeMap)) {
            codeMap.putAll(codeMapInit);
        }
        StringSubstitutor strSubstitutor = new StringSubstitutor(codeMap);


        return strSubstitutor.replace(PLACEHOLDER_STRING);
    }


    /**
     * @return 编码-值 键值对
     */
    public Map<String, String> getCodeMap() {

        if (MapUtil.isEmpty(codeMap)) {
            codeMap.putAll(codeMapInit);
        }

        return codeMap;

    }

}
