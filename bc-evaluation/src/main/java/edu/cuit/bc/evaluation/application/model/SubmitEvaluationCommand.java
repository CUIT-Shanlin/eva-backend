package edu.cuit.bc.evaluation.application.model;

import java.util.List;

/**
 * 提交评教命令（写模型输入）。
 */
public record SubmitEvaluationCommand(
        Integer taskId,
        String textValue,
        List<FormPropValue> formPropsValues
) { }

