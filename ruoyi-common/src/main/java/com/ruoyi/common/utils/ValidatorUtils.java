package com.ruoyi.common.utils;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

/**
 * Validator 校验框架工具
 *
 * @author Lion Li
 */
public class ValidatorUtils {

	private static final Validator VALID = Validation.buildDefaultValidatorFactory().getValidator();

	public static <T> void validate(T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> validate = VALID.validate(object, groups);
        if (!validate.isEmpty()) {
            throw new ConstraintViolationException("参数校验异常", validate);
        }
    }

}
