package edu.cuit.common.validator.status;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 状态有效值校验器，目标值必须在value数组中存在才算有效
 * null值被看做有效
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidStatusValidator.class)
public @interface ValidStatus {

    /**
     * 有效值
     */
    int[] value() default {0,1};

    String message() default "状态值无效";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
