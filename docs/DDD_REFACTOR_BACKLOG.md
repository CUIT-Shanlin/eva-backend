---
title: DDD 渐进式重构目标清单与行为框架
repo: eva-backend
branch: ddd
generated_at: 2025-12-18
scope: 全仓库（离线扫描 + 规则归纳）
---

# DDD 渐进式重构目标清单与行为框架

本文档用于后续若干个新会话的“行动框架（行为约束 + 执行模板）”与“待收敛目标池（Backlog）”。

> 说明：目标清单来自离线静态扫描（以 `GatewayImpl` 为主），属于“候选目标粗筛”。每次动手前仍需按调用链复核入口与行为约束。

---

## 1. 总目标（不变）

- 把当前“按技术分层”的单体，渐进式重构为“按业务限界上下文分模块”的 **DDD 模块化单体**。
- 先收敛入口与端口（六边形），再逐步语义化事件与读写模型；**不推倒重来**。
- 预留低成本拆微服务路径：**先拆服务，暂时共享库**（过渡期），小步快跑。

---

## 2. 本项目重构铁律（必须遵守）

1) **只做重构**：收敛调用链、抽离端口/用例、让旧 gateway 退化委托壳、事件化副作用。  
2) **行为不变**（验收标准）：
   - API 不变；
   - 异常类型/异常文案不变；
   - 消息文案不变；
   - 副作用时机不变（尤其是“事务提交后执行”）。
3) **不做业务优化/语义调整**：即便看到明显 bug/命名不佳，也先不动（避免“重构夹带需求”）。

---

## 3. 会话级行为框架（每个目标都按这个模板走）

### 3.1 目标选择（建议）

优先选满足任意一条的目标：
- 写流程（Command）且跨多表/多副作用（缓存失效、日志、消息、评教任务联动等）。
- 单个方法体积大（例如 > 50 LOC）且明显在 infra/gateway 承载业务流程。
- 处在高频入口（Controller → Service → Gateway）且后续计划拆分到某 BC。

次优先：纯查询（Query）的大方法（可以后置，避免过早 CQRS 化）。

### 3.2 标准落地步骤（必须拆分 commit）

每个目标严格按以下 6 步推进；**每一步完成后立即 `git commit`**：

1. **定位调用链（只读）**
   - 从 Controller/Service 入口定位到 gateway 方法；
   - 记录“输入/输出/异常文案/副作用（缓存、日志、消息）”的行为快照。
2. **BC 模块新增用例骨架（写侧优先）**
   - `Command/UseCase/Port(Repository)`：usecase 只做委托 + 必要的入参非空（按团队已有范式）。
   - 新增纯单测：只测 NPE 与“委托端口一次”，不测业务规则（规则以旧实现为准）。
3. **infra 端口适配器实现旧逻辑**
   - 新增 `eva-infra/.../bccxxx/adapter/*RepositoryImpl`；
   - 将旧 gateway 的业务流程 **原样搬运**（包含异常文案、删除顺序、日志、缓存等）。
4. **旧 gateway 退化为委托壳**
   - gateway 仅负责：构造 command → 调用 usecase → 返回历史签名（必要时 `return null`）。
   - 若历史对外签名为 `Void`，保持 `return null`。
5. **离线验证（Java 17 + 本地 Maven repo）**
   - `export JAVA_HOME=\"$HOME/.sdkman/candidates/java/17.0.17-zulu\" && export PATH=\"$JAVA_HOME/bin:$PATH\"`
   - `mvn -o -pl <bc-module> -am test -q -Dmaven.repo.local=.m2/repository`
   - `mvn -o -pl eva-infra -am -DskipTests test -q -Dmaven.repo.local=.m2/repository`
6. **沉淀文档（交接与回溯）**
   - 在 `NEXT_SESSION_HANDOFF.md` 追加闭环/commit；
   - 在本文件更新“目标池状态”（可选，若目标池变更明显再更新）。

### 3.3 风险控制清单（动手前必须逐条核对）

- `@Transactional` 行为：避免“同类 self-invocation”绕过代理；事务边界尽量保持在原入口（Controller/Service/Gateway）层级不变。
- 事件副作用：如涉及消息/撤回评教任务等，必须确保在 **事务提交后** 触发（既有 `AfterCommitEventPublisher` 机制优先复用）。
- 缓存失效：key 与时机保持不变（特别是 `COURSE_TYPE_LIST`、`COURSE_LIST_BY_SEM`、`TASK_LIST_BY_SEM` 等）。
- 日志：`LogUtils.logContent(...)` 的文案与触发时机保持不变。
- 异常：异常类型（例如 `UpdateException/QueryException`）与文案保持不变。

---

## 4. 待收敛目标池（Backlog）

### 4.1 说明（如何读这个 Backlog）

本清单按“候选目标粗筛”生成，优先列出：
- `eva-infra/.../*GatewayImpl.java` 中 **未出现** “历史路径：收敛到 bc-” 标记，且
  - 方法体积较大（>=30 LOC），或
  - 明显包含 DB/Mapper 操作（>=15 LOC）。

对每个目标，建议后续补充（会话执行时再补）：
- Controller/Service 入口；
- I/O 与异常文案；
- 副作用（缓存、日志、消息）清单；
- 推荐 BC（例如 `bc-course/bc-evaluation/bc-messaging/...`）。

---

## 5. 候选目标（按模块/文件归类）

> 注意：以下“LOC”是粗略估算，主要用于优先级排序，不作为验收依据。

### 5.1 课程域（course）

#### A) `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`

写侧尚未收敛（仍在 infra 内承载流程/校验/DB）：
- `JudgeCourseTime`（~98 LOC，含 DB/Mapper）：时间冲突判定/教室冲突等规则（建议后续收敛到 `bc-course`）。
- `JudgeCourseType`（~41 LOC，含 DB/Mapper）：课程类型相关判定（建议后续收敛到 `bc-course`）。
- `addNotExistCoursesDetails`（~62 LOC，含 DB/Mapper）：自助课表创建“课程+科目+类型+课次”联动写流程（高优先级，跨多表）。
- `addExistCoursesDetails`（~22 LOC，含 DB/Mapper）：自助课表给已存在课程追加课次（中优先级）。
- `isImported`（~19 LOC，含 DB/Mapper）：按学期/课程性质判断是否已导入（偏查询/校验）。
- `toJudge/judgeAlsoHasLocation/getDifference` 等私有辅助（随主流程一起迁移更稳）。

#### B) `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseQueryGatewayImpl.java`

读侧尚未收敛（大量 query/组装逻辑仍在 gateway）：
- `toCourseTime`（~86 LOC，含 DB/Mapper）：课程时间组装与映射（可后置，避免过早 CQRS）。
- `getPeriodCourse`（~64 LOC）：周期课程/课表视图组装（建议后续拆为 QueryPort/ReadModel）。
- `findEvaScore`（~56 LOC，含 DB/Mapper）：课程评教分数统计（与评教域耦合，后续可考虑走 `bc-evaluation` 的 read side）。
- `getCourseInfo/page/getCourseList/getPeriodInfo/toGetCourseTime/getUserCourseDetail/getSelfCourseInfo/pageCourseType/getWeekCourses` 等（30~51 LOC 级别）。

建议策略：
- 先继续写侧（Command）收敛；读侧（Query）可以在写侧稳定后再逐步抽 `QueryPort`。

---

### 5.2 评教域（eva）

#### A) `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/eva/EvaUpdateGatewayImpl.java`

写侧高价值目标（体积大、跨表）：
- `postEvaTask`（~145 LOC，含 DB/Mapper）：发布评教任务（高优先级，跨任务/消息/记录/缓存）。
- `putEvaTemplate`（~47 LOC，含 DB/Mapper）：提交评教（注意：提交评教的主链路已在 `bc-evaluation` 有用例，需核对该 gateway 仍承担的内容）。
- `updateEvaTemplate`（~24 LOC，含 DB/Mapper）：模板修改（可能涉及锁定规则，需确保“锁定”行为不变）。

#### B) `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/eva/EvaDeleteGatewayImpl.java`

删改写侧目标：
- `deleteEvaRecord`（~57 LOC，含 DB/Mapper）
- `deleteEvaTemplate`（~33 LOC，含 DB/Mapper）

#### C) `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/eva/EvaQueryGatewayImpl.java`

读侧目标（很多统计/分页/聚合仍在 gateway）：
- `pageEvaRecord`（~148 LOC）
- `getEvaData`（~94 LOC）
- `evaScoreStatisticsInfo/getEvaLogInfo/evaSelfTaskInfo/evaTemplateSituation/pageEvaUnfinishedTask/getEvaEdLogInfo` 等（60~90 LOC）
- 大量统计/报表相关方法（30~50 LOC）

建议策略：
- 评教写侧优先收敛（`postEvaTask` 等），读侧统计可以后置，避免“边重构边改统计口径”。

---

### 5.3 消息域（msg）

#### A) `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/MsgGatewayImpl.java`

目前多为 CRUD/状态位更新（单方法体积不大），但仍属于“未模块化”的领域能力：
- `queryMsg/queryTargetAmountMsg/updateMsgDisplay/updateMsgRead/updateMultipleMsgRead/insertMessage/deleteMessage/deleteTargetTypeMessage` 等。

建议策略：
- 若后续要继续做“跨域副作用事件化”，可逐步把消息发送/撤回/查询统一收敛到 `bc-messaging` 的端口与用例，旧 gateway 委托化。

---

### 5.4 系统管理（用户/角色/菜单）

这些模块多为典型 IAM/权限模型 CRUD，当前主要仍在 `eva-infra` gateway + mapper。

候选目标（体积/复杂度相对更高者）：
- `eva-infra/.../user/UserUpdateGatewayImpl.assignRole`（~30 LOC）
- `eva-infra/.../user/UserUpdateGatewayImpl.createUser`（~23 LOC）
- `eva-infra/.../user/UserQueryGatewayImpl.fileUserEntity`（~30 LOC）
- `eva-infra/.../user/MenuUpdateGatewayImpl.handleUserMenuCache`（~19 LOC）
- `eva-infra/.../user/RoleUpdateGatewayImpl.assignPerms/deleteMultipleRole`（~17~18 LOC）

建议策略：
- 若拆 BC，可考虑独立 `bc-iam`（或 `bc-user`）来承载权限/用户域用例与端口；否则保持为“薄 CRUD gateway”，优先级可低于课程/评教主链路。

---

### 5.5 日志与配置

#### A) `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/LogGatewayImpl.java`
- `page`（~21 LOC，含 DB/Mapper）
- `toSysLogEntity`（~16 LOC，含 DB/Mapper）

#### B) `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/eva/EvaConfigGatewayImpl.java`
- `readConfig`（~30 LOC，含 DB/Mapper）
- `writeConfig`（~17 LOC，含 DB/Mapper）

建议策略：
- 这些通常是“支撑域”，可后置；若要拆 BC，可考虑 `bc-config` / `bc-log`。

---

## 6. “下一批行动”建议（不含实现，只给路线）

如果继续按“写侧优先”的策略推进，下一批候选（高 → 低）建议是：

1) `CourseUpdateGatewayImpl.addNotExistCoursesDetails`（课程自助新增整条写链路，跨多表）  
2) `CourseUpdateGatewayImpl.JudgeCourseTime` / `JudgeCourseType`（时间与类型规则收敛，后续复用面大）  
3) `EvaUpdateGatewayImpl.postEvaTask`（评教任务发布，跨多表/缓存/可能涉及消息）  
4) `EvaDeleteGatewayImpl.deleteEvaRecord/deleteEvaTemplate`（写侧删除链路）  
5) 读侧：`CourseQueryGatewayImpl` 与 `EvaQueryGatewayImpl`（统计/分页/聚合，后置）  

---

## 7. 与交接文档的关系

- “已完成闭环与踩坑记录”以 `NEXT_SESSION_HANDOFF.md` 为准（每次会话结束必须更新）。
- 本文件用于“长期 Backlog 与统一执行模板”，避免每个新会话重复盘点。

