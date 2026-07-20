package org.dromara.workflow.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.dromara.warm.flow.core.exception.FlowException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 工作流失败异常
 *
 * @author Lion Li
 */
@Slf4j
@RestControllerAdvice
public class FlowExceptionHandler {

    /**
     * 工作流失败异常。
     *
     * @param e       异常信息
     * @param request 当前请求
     * @return 统一失败响应
     */
    @ExceptionHandler(FlowException.class)
    public R<Void> handleFlowException(FlowException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}'", requestURI, e);
        return R.fail(e.getMessage());
    }

}
