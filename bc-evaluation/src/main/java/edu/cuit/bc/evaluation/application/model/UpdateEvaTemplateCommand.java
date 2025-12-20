package edu.cuit.bc.evaluation.application.model;

/**
 * 修改评教模板命令（写模型输入）。
 */
public record UpdateEvaTemplateCommand(
        Integer id,
        String name,
        String description,
        String props
) { }

