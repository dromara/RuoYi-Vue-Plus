package org.dromara.common.excel.core;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.event.AnalysisEventListener;
import org.apache.fesod.sheet.exception.ExcelAnalysisException;
import org.apache.fesod.sheet.exception.ExcelDataConvertException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.json.utils.JsonUtils;

import java.util.Map;
import java.util.Set;

/**
 * Excel 导入监听
 *
 * @author Yjoioooo
 * @author Lion Li
 */
@Slf4j
@NoArgsConstructor
public class DefaultExcelListener<T> extends AnalysisEventListener<T> implements ExcelListener<T> {

    /**
     * 是否Validator检验，默认为是
     */
    private Boolean isValidate = Boolean.TRUE;

    /**
     * excel 表头数据
     */
    private Map<Integer, String> headMap;

    /**
     * 导入回执
     */
    private final ExcelResult<T> excelResult = new DefaultExcelResult<>();

    /**
     * 发生异常时是否立即终止读取，默认保持原有快速失败行为
     */
    private Boolean failFast = Boolean.TRUE;

    /**
     * 构造 Excel 导入监听器。
     *
     * @param isValidate 是否执行 Validator 校验
     */
    public DefaultExcelListener(boolean isValidate) {
        this.isValidate = isValidate;
    }

    /**
     * 构造 Excel 导入监听器。
     *
     * @param isValidate 是否执行 Validator 校验
     * @param failFast   发生异常时是否立即终止读取
     */
    public DefaultExcelListener(boolean isValidate, boolean failFast) {
        this.isValidate = isValidate;
        this.failFast = failFast;
    }

    /**
     * 处理异常
     *
     * @param exception ExcelDataConvertException
     * @param context   Excel 上下文
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        String errMsg = null;
        if (exception instanceof ExcelDataConvertException excelDataConvertException) {
            // 如果是某一个单元格的转换异常 能获取到具体行号
            Integer rowIndex = excelDataConvertException.getRowIndex();
            Integer columnIndex = excelDataConvertException.getColumnIndex();
            errMsg = StrUtil.format("第{}行-第{}列-表头{}: 解析异常<br/>",
                rowIndex + 1, columnIndex + 1, headMap == null ? "" : headMap.get(columnIndex));
            if (log.isDebugEnabled()) {
                log.warn(errMsg);
            }
        }
        if (exception instanceof ConstraintViolationException constraintViolationException) {
            Set<ConstraintViolation<?>> constraintViolations = constraintViolationException.getConstraintViolations();
            String constraintViolationsMsg = StreamUtils.join(constraintViolations, ConstraintViolation::getMessage, ", ");
            errMsg = StrUtil.format("第{}行数据校验异常: {}", context.readRowHolder().getRowIndex() + 1, constraintViolationsMsg);
            if (log.isDebugEnabled()) {
                log.warn(errMsg);
            }
        }
        if (errMsg == null) {
            errMsg = StrUtil.format("第{}行数据异常: {}", context.readRowHolder().getRowIndex() + 1, exception.getMessage());
            log.warn(errMsg, exception);
        }
        excelResult.getErrorList().add(errMsg);
        if (failFast) {
            throw new ExcelAnalysisException(errMsg);
        }
    }

    /**
     * 记录 Excel 表头信息。
     *
     * @param headMap 表头映射
     * @param context 解析上下文
     */
    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        this.headMap = headMap;
        log.debug("解析到一条表头数据: {}", JsonUtils.toJsonString(headMap));
    }

    /**
     * 处理一行导入数据。
     *
     * @param data    行数据
     * @param context 解析上下文
     */
    @Override
    public void invoke(T data, AnalysisContext context) {
        if (isValidate) {
            ValidatorUtils.validate(data);
        }
        excelResult.getList().add(data);
    }

    /**
     * 全部数据解析完成回调。
     *
     * @param context 解析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.debug("所有数据解析完成！");
    }

    /**
     * 获取 Excel 导入解析结果。
     *
     * @return 导入解析结果
     */
    @Override
    public ExcelResult<T> getExcelResult() {
        return excelResult;
    }

}
