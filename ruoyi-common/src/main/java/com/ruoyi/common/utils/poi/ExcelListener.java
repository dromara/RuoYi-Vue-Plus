package com.ruoyi.common.utils.poi;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.utils.ValidatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.validation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公共excel监听类
 * @param <T>
 */
public class ExcelListener<T> extends AnalysisEventListener<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelListener.class);
    /** 数据对象list */
    private final List<T> list = new ArrayList<>();
    /** 错误信息列表 */
    private final List<String> errorList = new ArrayList<>();
    /** 遇到异常是否跳出导入，默认为是 */
    private Boolean skipException = Boolean.TRUE;
    /** 是否Validator检验，默认为是 */
    private Boolean isValidate = Boolean.TRUE;
    /**
     * 导入回执
     */
    private final ExcelResult<T> excelResult = new ExcelResult<>();

    public ExcelListener() {

    }

    public ExcelListener(boolean isValidate, boolean skipException) {
        this.isValidate = isValidate;
        this.skipException = skipException;
    }

    /**
     * 处理异常
     *
     * @param exception ExcelDataConvertException
     * @param context excel上下文
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        // 如果是某一个单元格的转换异常 能获取到具体行号
        // 如果要获取头的信息 配合doAfterAllAnalysedHeadMap使用
        String errMsg = null;
        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException excelDataConvertException = (ExcelDataConvertException)exception;
            errMsg = StrUtil.format("第{}行-第{}列解析异常<br/>", excelDataConvertException.getRowIndex() + 1,
                    excelDataConvertException.getColumnIndex() + 1);
            LOGGER.error(errMsg);
        }
        if (exception instanceof ConstraintViolationException) {
            ConstraintViolationException constraintViolationException = (ConstraintViolationException)exception;
            Set<ConstraintViolation<?>> constraintViolations = constraintViolationException.getConstraintViolations();
            String constraintViolationsMsg= CollUtil.join(constraintViolations
                    .stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.toList()),
                ",");
            errMsg = StrUtil.format("第{}行数据校验异常:{}", context.readRowHolder().getRowIndex() + 1,
                constraintViolationsMsg);
            LOGGER.error(errMsg);
        }
        errorList.add(errMsg);
        if (!skipException){
            throw new ExcelAnalysisException(errMsg);
        }
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        LOGGER.debug("解析到一条头数据:{}", JSON.toJSONString(headMap));
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        if (isValidate) {
            ValidatorUtils.validate(data);
        }
        list.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        excelResult.setList(list);
        excelResult.setErrorList(errorList);
        LOGGER.debug("所有数据解析完成！");
    }

    /**
     * 获取导入数据
     * @return 导入数据
     */
    public List<T> getList() {
        return list;
    }

    public ExcelResult<T> getExcelResult() {
        return excelResult;
    }

}
