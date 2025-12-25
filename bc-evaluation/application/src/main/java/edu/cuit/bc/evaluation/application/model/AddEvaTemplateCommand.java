package edu.cuit.bc.evaluation.application.model;

/**
 * 新增评教模板命令（写模型输入）。
 */
public record AddEvaTemplateCommand(
        String name,
        String description,
        String props
) { }

