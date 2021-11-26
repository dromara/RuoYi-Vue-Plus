package com.ruoyi.common.utils.poi;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExcelResult<T> {

    /** 数据对象list
     */
    private List<T> list;
    /** 错误信息列表 */
    private List<String> errorList;

    /**
     * 获取导入回执
     * @return 导入回执
     */
    public String getAnalysis() {
        int successCount = list.size();
        int errorCount = errorList.size();
        if (successCount == 0) {
            return "读取失败，未解析到数据";
        } else {
            if (errorList.size() == 0) {
                return StrUtil.format("恭喜您，全部读取成功！共{}条", successCount);
            } else {
                return StrUtil.format("部分读取成功，其中成功{}条，失败{}条，错误信息如下：<br/>{}",
                    successCount,
                    errorCount,
                    CollUtil.join(errorList, "<br/>"));
            }

        }
    }
}
