/**
 * IAM 数据访问层（DAL）- MyBatis Mapper 包。
 *
 * <p>本包用于承接从 {@code eva-infra} 渐进式抽离的 IAM Mapper 接口与 XML（保持包名稳定，避免影响 mapper namespace）。
 * <p>强约束：仅做重构与归属迁移，不改变业务语义；缓存/日志/异常文案/副作用顺序完全不变。
 */
package edu.cuit.infra.dal.database.mapper.user;

