package edu.cuit.bc.evaluation.application.model;

/**
 * 提交评教时的指标分值（保持字段名与现有 JSON 存储一致：prop/score）。
 */
public record FormPropValue(String prop, Number score) { }

