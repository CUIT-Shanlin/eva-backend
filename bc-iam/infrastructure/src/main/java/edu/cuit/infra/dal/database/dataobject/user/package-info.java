/**
 * IAM 数据访问层（DAL）- DataObject（DO）包。
 *
 * <p>本包用于承接从 {@code eva-infra} 渐进式抽离的 IAM 表 DO（保持包名稳定，避免影响现有 MyBatis-Plus 运行时行为）。
 * <p>强约束：仅做重构与归属迁移，不改变业务语义；缓存/日志/异常文案/副作用顺序完全不变。
 */
package edu.cuit.infra.dal.database.dataobject.user;

