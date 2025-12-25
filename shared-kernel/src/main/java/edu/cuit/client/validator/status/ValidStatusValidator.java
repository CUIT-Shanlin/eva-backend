package edu.cuit.client.validator.status;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 状态有效值校验器
 */
public class ValidStatusValidator implements ConstraintValidator<ValidStatus,Integer> {

    private int[] validValues;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) return true;
        for (int validValue : validValues) {
            if (validValue == value) return true;
        }
        return false;
    }

    @Override
    public void initialize(ValidStatus constraintAnnotation) {
        validValues = constraintAnnotation.value();
    }
}
