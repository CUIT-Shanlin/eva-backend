# DDD 重构计划书（面向“可低成本拆微服务”的模块化单体）

> 适用范围：本仓库 `eva-backend`（Spring Boot + Maven 多模块）。  
> 构建基线：**Java 17**（避免高版本 JDK 下的编译器/注解处理器兼容性问题）。  
> 离线验证示例：`mvn -o -pl eva-infra -am test -q -Dmaven.repo.local=.m2/repository`（网络受限场景优先）。  
> 目标：把当前“按技术分层”的结构，重构为“按业务域分模块”的 **DDD 模块化单体**，并在架构与边界上为未来拆分微服务预留低成本路径。

---

## 0. 背景与总体目标

### 0.1 当前事实（来自现有文档与代码）

- 业务核心：教师之间的听课/选课、评教任务、评教记录、统计导出、消息提醒、AI 评估报告。
- 数据核心表（`data/doc/*`）：
  - IAM：`sys_user`、`sys_role`、`sys_menu`、`sys_user_role`、`sys_role_menu`、LDAP 同步相关
  - 课表/课程：`semester`、`subject`、`course`、`cour_inf`、`course_type`、`course_type_course`
  - 评教：`eva_task`、`form_record`、`form_template`、`cour_one_eva_template`
  - 消息：`msg_tip`
  - 审计：`sys_log`、`sys_log_module`
- 关键业务规则（你已确认/文档明确）：
  - `week` 表示学期内单周周次（从开学日期 `start_date` 起算第 n 周）。
  - 同一课次允许多人评教（必须如此）。
  - 课程模板：只要有人评教过就锁定（不能再切换）。
  - “高分次数”阈值应可配置。
  - AI 报告维度：教师 + 学期（汇总全部课程，报告中可按课程分节）。
- 代码现状：已有 `eva-adapter / eva-app / eva-domain / eva-infra` 等技术分层模块；但“领域层”被 DTO/CO/Cmd 污染，跨域耦合明显。

### 0.2 重构目标（可验收）

1. **按业务域拆模块**（Bounded Context 为单位），形成可独立演进的模块化单体。
2. 落地 **六边形架构（Ports & Adapters）**：领域模型不依赖 Web/DB/LDAP/LLM。
3. 落地 **CQRS**：写模型（聚合）与读模型（投影/查询）解耦，复杂统计/看板可独立优化。
4. 通过 **领域事件** 串联跨域联动（消息、日志、统计、AI 报告），为未来“事件驱动微服务”铺路。
5. 以“最小切片”渐进重构：不推倒重来，不大改接口；每阶段可交付、可回滚。

---

## 1. 领域划分（Subdomain）

> 原则：围绕业务能力而非数据库表；以“变化原因一致”为标准聚合到同一子域。

| 子域 | 类型 | 核心能力 | 主要数据/接口线索 |
|---|---|---|---|
| 评教（Evaluation） | 核心域 | 评教任务/记录、达标统计、导出 | `eva_task`、`form_record`、导出接口、看板接口 |
| 课表与课程（Course Scheduling） | 支撑域 | 学期、课程、课次、导入课表、改课联动 | `semester/course/cour_inf`、课程操作说明、导入课表接口 |
| 表单模板（Form Template） | 支撑域 | 模板管理、课程模板锁定、快照统计 | `form_template`、`cour_one_eva_template`、模板切换说明 |
| 消息通知（Messaging） | 通用域 | 消息入库、WS 转发、逾期提醒 | `msg_tip`、消息业务说明 |
| 身份与权限（IAM） | 通用域 | 登录、用户/角色/菜单、LDAP 同步 | `sys_*`、LDAP 文档、鉴权注解 |
| 审计日志（Audit） | 通用域 | 操作日志与模块字典 | `sys_log/sys_log_module` |
| AI 报告（AI Report） | 支撑域 | 教师学期报告生成与导出 | AI 报告样例、LangChain4j 相关实现 |

---

## 2. 限界上下文（Bounded Context）划分与上下文地图（Context Map）

### 2.1 Bounded Context 列表（建议）

1. **BC-IAM**：认证、授权、LDAP 同步与本地用户信息。
2. **BC-Course**：学期/课程/课次/课程类型/课表导入与改课业务规则。
3. **BC-Template**：评教模板、课程模板快照、锁定策略。
4. **BC-Evaluation**（核心）：评教任务、评教记录、统计、导出。
5. **BC-Messaging**：消息生命周期（保存、推送、已读、展示/删除）、逾期提醒生成规则。
6. **BC-Audit**：操作日志记录与查询。
7. **BC-AIReport**：教师学期 AI 报告（聚合统计 + LLM 文案生成 + Word 导出）。

### 2.2 上下游关系（建议）

- BC-IAM（上游）→ 全部上下文：提供 `UserId/Role/Permission`。
- BC-Course（上游）→ BC-Evaluation：提供课次时间、冲突校验所需课表数据。
- BC-Template（上游）→ BC-Evaluation：提供任务关联的模板快照/模板解析；提供“锁定”判定依据。
- BC-Evaluation（发布事件）→ BC-Messaging/BC-Audit/BC-AIReport：任务分配/取消/提交评教触发联动。

### 2.3 集成策略（从单体到微服务的可迁移方案）

在模块化单体阶段：
- 同进程调用：**只允许应用层接口调用**（Application Service），不允许跨 BC 直接访问对方 DAL/Mapper。
- 跨 BC 数据：通过 **ACL（Anti-Corruption Layer）** 做模型转换。
- 跨 BC 联动：优先使用 **领域事件（本地事件总线）**，避免强耦合调用链。

在未来微服务阶段：
- 同样的“应用层接口”可自然替换为 HTTP/gRPC 客户端或消息订阅者实现（保持端口不变）。
- 事件从 Spring 本地事件切换到 MQ（Kafka/RabbitMQ）+ Outbox。

---

## 3. 目标架构：按业务模块的六边形架构（可拆分微服务）

### 3.1 目标 Maven 模块结构（建议新结构）

> 核心原则：**一个 BC 对应一组 Maven 模块**（domain/application/infrastructure/adapters），拆微服务时可整体迁移。

建议新增/调整为：

- `eva-bootstrap`：组合根（Spring Boot 启动、配置装配、跨模块组装）
- `bc-iam/`
  - `bc-iam-domain`：领域模型 + 端口（Ports）
  - `bc-iam-application`：用例编排（Commands/Queries）
  - `bc-iam-adapter-in`：HTTP/WS 入站（Controllers）
  - `bc-iam-adapter-out`：LDAP/DB 出站实现
- `bc-course/`（同上四件套）
- `bc-template/`（同上四件套）
- `bc-evaluation/`（同上四件套）
- `bc-messaging/`（同上四件套）
- `bc-audit/`（同上四件套）
- `bc-ai-report/`（同上四件套）
- `shared-kernel`（严格受控）：仅放“稳定且无业务语义”的通用类型（如 `PageRequest/PageResult`、`DomainEvent` 基类、`Clock`、`Result`）。

> 说明：你当前的 `eva-adapter/eva-app/eva-domain/eva-infra` 属于技术切片，下一步会逐步把代码搬运并收敛到以上 BC 结构。

### 3.2 六边形架构的层级职能（每个 BC 内一致）

**Domain（领域层）**
- 聚合/实体/值对象/领域服务/领域事件（只表达业务规则）
- 定义端口（Ports）：Repository、外部服务接口、时间/随机等可替换依赖
- 不依赖 Spring、不依赖 MyBatis、不依赖 Web DTO

**Application（应用层）**
- 用例编排：事务边界、权限/幂等等应用级规则
- CQRS：命令处理器（Command Handler）与查询服务（Query Service）
- 发布领域事件（建议 after-commit 发布）
- 不做复杂 SQL（查询可委托 QueryRepo，但要保持“读写分离”原则）

**Adapters-In（入站适配器）**
- REST Controller / WebSocket endpoint
- DTO 解析、参数校验、鉴权注解、返回模型组装
- 将 DTO 转为 Command/Query 对象调用 Application

**Adapters-Out / Infrastructure（出站适配器）**
- DB/Mapper/DO、LDAP、LLM、文件系统、消息推送、第三方接口
- 实现 Domain 端口（Repository、ExternalService）

---

## 4. 领域模型设计（实体/聚合/值对象）——按 BC 给出落地方案

> 注意：这里给的是“领域模型（业务语义）”，不等同于“数据库表一一对应的贫血实体”。DB 表仅作为参考映射。

### 4.1 BC-Course（课表与课程）

**核心概念**
- 学期（Semester）：开学日期是“周次计算”的根。
- 科目（Subject）：课程名称与性质（理论/实验/其他）。
- 课程（Course）：某学期某老师教授某科目。
- 课次（CourseSession/ClassMeeting）：课程在某周某天某节次、地点上课。

**聚合建议**

1) `Semester` 聚合
- 实体：`Semester`
- 值对象：`SemesterId`、`AcademicYear`（如 2024-2025）、`StartDate`
- 领域服务：`AcademicCalendarService`
  - `LocalDate resolveDate(Semester, WeekNo, DayOfWeek)`（统一时间语义）

2) `Course` 聚合
- 实体：`Course`
- 值对象：`CourseId`、`TeacherId`、`SubjectId`、`CourseNature`
- 子实体/集合：`CourseSession`（可作为 `Course` 的内部集合，或单独聚合作为取舍）
- 关键不变量：
  - 同一学期同一老师同一科目是否允许多条？（需结合现状确认；若允许则引入 `CourseCode/Section` 之类区分）

3) `CourseType`（课程类型）聚合
- 实体：`CourseType`
- 关联：`CourseTypeAssignment`（类型与课程的绑定）

**关键业务规则落地（来自“课程操作说明”）**
- “改课程时段”触发：
  1) 给所有人发通知消息
  2) 撤回该课程所有未完成评教任务，并通知任务发起者
  3) 删除并重建该课程所有课次
- “删课”触发：
  1) 全体通知
  2) 删除/撤回相关评教任务并通知
  3) 删除评教记录、删除课次、逻辑删除课程

> 落地方式：这些是跨 BC 行为（消息/评教/日志），建议由 **BC-Course 的应用层用例**发出领域事件，让订阅者去执行联动。

---

### 4.2 BC-Template（评教表单模板与快照）

**聚合建议**

1) `FormTemplate` 聚合（对应 `form_template`）
- 实体：`FormTemplate`
- 值对象：`TemplateId`、`TemplateName`、`TemplateProps`（指标集合，含去重规则）、`TemplateScope`（理论/实验/非默认）
- 关键不变量：
  - 同一模板内指标名不可重复（重复时按规则规范化：如自动加序号，或拒绝并提示）

2) `CourseTemplateSnapshot` 聚合（对应 `cour_one_eva_template`）
- 实体：`CourseTemplateSnapshot`
- 值对象：`SemesterId`、`CourseId`、`SnapshotTemplate`（冻结后的模板 JSON）、`CourseStatistics`（结构化统计）
- 关键不变量（你确认）：**只要有人评教过就锁定**
  - 落地判定：当某课程本学期首次产生 `EvaluationRecord` 时，创建/更新快照并标记“锁定”。
  - “锁定”状态可以显式落库（推荐），或由“快照是否存在”推导（你现在的表结构更偏后者）。

---

### 4.3 BC-Evaluation（评教：核心域）

**聚合建议**

1) `EvaluationTask` 聚合（对应 `eva_task`）
- 实体：`EvaluationTask`
- 关键字段：
  - `TaskId`
  - `EvaluatorId`（评教老师）
  - `CourseSessionId`（被评课次 `cour_inf`）
  - `Status`（待执行/已执行/已撤回）
- 值对象：
  - `TaskStatus`
  - `TimeSlot`（来自课次，用于冲突校验；通过端口查询）
- 关键不变量：
  - 同一 `EvaluatorId + CourseSessionId` 不应重复生成多条“进行中的任务”（应用层需幂等）
  - 任务“已执行”后不可取消（除非管理员有特殊能力，需定义业务规则）

2) `EvaluationRecord` 聚合（对应 `form_record`）
- 实体：`EvaluationRecord`
- 关联：`TaskId`
- 值对象：
  - `CriterionScore`（指标名 + 分数 1~100）
  - `TextComment`（详细评价）
  - `Topic`（课程主题）
- 关键不变量：
  - 一个任务最多产生一条有效记录（否则要定义“多次提交/覆盖”规则）
  - 分数范围、指标集合应与该任务关联模板一致

3) 领域服务 / 策略
- `TaskAssignmentPolicy`
  - 冲突校验：与本人教学课/其它任务时间冲突（由 BC-Course 提供课表查询端口）
  - 配额校验：被评老师被评次数上限（来自可配置 `EvaConfig`）
- `ScorePolicy`
  - 高分阈值 `highScoreThreshold`（可配置，默认 90）
  - 统计口径（课程平均分、教师学期汇总等）

---

### 4.4 BC-Messaging（消息）

**聚合建议**

1) `Message` 聚合（对应 `msg_tip`）
- 实体：`Message`
- 值对象：
  - `MessageType`（待办/通知/提醒/警告）
  - `MessageMode`（普通/评教）
  - `Recipient`（单人/广播）
  - `ReadStatus`（已读/未读、是否已展示）
- 关键规则（来自消息业务说明）：
  - 自动逾期消息：`senderId` 为 `-1/null`，同一 `taskId` 最多一条；在“连接 WS”和“拉取消息”时机创建或更新。

> 建议：把“逾期消息生成”作为 `OverdueReminderService`（应用层定时/触发器），并通过端口获取任务上课日期（由 BC-Course 计算）与任务状态（BC-Evaluation）。

---

### 4.5 BC-IAM（身份与权限）

**聚合建议**

1) `User` 聚合（对应 `sys_user`）
- 实体：`User`
- 值对象：`UserId`、`Username`、`Phone`、`Email`、`Department`、`ProfTitle`

2) `Role` 聚合、`Permission/Menu` 聚合（对应 `sys_role/sys_menu`）
- 关联：`UserRoleAssignment`、`RoleMenuAssignment`

3) `LdapSync` 作为领域服务（或应用服务）
- 外部系统通过端口 `LdapClientPort` 访问，避免 IAM 领域被 LDAP 细节污染。

---

### 4.6 BC-Audit（审计日志）

**聚合建议**

1) `AuditLog`（对应 `sys_log`）
- 值对象：`ModuleId`、`OperationType`（查增改删/其他）、`OperatorId`、`IpAddress`

> 审计通常属于通用域，不建议在各 BC 里直接写 DB；统一由事件订阅者记录。

---

### 4.7 BC-AIReport（AI 报告）

**聚合建议**

1) `TeacherSemesterReport` 聚合（建议新增领域模型，落库可选）
- 实体：`TeacherSemesterReport`
- 值对象：
  - `TeacherId`、`SemesterId`
  - `TotalEvaluations`、`HighScoreCount`（阈值来自配置）
  - `OverallSummary`（AI 总体评价）
  - `CourseReports[]`：每门课程的统计与分析（优点/缺点/建议）
- 端口：
  - `ReportFactsQueryPort`：从评教域读取结构化事实（统计结果、评价文本摘要等）
  - `LlmGeneratePort`：LLM 文案生成
  - `WordExportPort`：Word 导出（POI）

> 原则：LLM 只负责“语言生成”，事实计算必须在本地完成，避免“AI 幻觉”污染统计。

---

## 5. 领域事件（Domain Events）设计与落地

### 5.1 事件命名与负载原则

- 命名：`<BC>.<Aggregate>.<VerbPastTense>`，如 `Evaluation.TaskAssigned`
- 负载：只携带“业务标识与必要快照”，避免塞入大对象/DTO
- 发布时机：建议 **事务提交后（after-commit）** 发布，确保订阅者读到一致数据

### 5.2 建议事件清单

**BC-Evaluation 发布**
- `Evaluation.TaskAssigned`：新任务创建（触发待办消息）
- `Evaluation.TaskCancelled`：任务撤回（触发消息撤销/通知）
- `Evaluation.Submitted`：提交评教（触发：任务完成、消息清理、模板锁定、统计刷新、AI 报告失效/重算）
- `Evaluation.RecordDeleted`：删除评教记录（触发统计刷新）

**BC-Course 发布**
- `Course.CourseScheduleChanged`：课程时段变更（触发撤回相关未完成任务 + 全体通知）
- `Course.CourseDeleted`：课程删除（触发任务/记录/课次清理与通知）

**BC-Template 发布**
- `Template.TemplateUpdated/Deleted/Created`
- `Template.CourseTemplateLocked`：首次评教导致锁定（可由 Evaluation 提交时触发，但状态归属模板域）

**BC-Messaging 发布（可选）**
- `Messaging.MessageCreated/Read/Deleted`

### 5.3 事件订阅者（跨域联动落地）

- `MessagingSubscriber`：处理任务分配/撤回/逾期提醒/课程变更通知
- `AuditSubscriber`：记录关键操作日志
- `TemplateSubscriber`：评教提交触发快照生成与锁定
- `AiReportSubscriber`：评教提交后标记“教师学期报告需重算”（同步或异步）

---

## 6. CQRS 设计（写模型/读模型分离）

### 6.1 写模型（Commands + 聚合）

写侧目标：保证业务不变量、可读性与可测试性。

建议用例（示例）：
- `AssignEvaluationTaskCommand`（发起评教任务）
- `CancelEvaluationTaskCommand`
- `SubmitEvaluationCommand`
- `ImportScheduleCommand`（导入课表）
- `ChangeCourseScheduleCommand`（改时段）
- `UpdateEvaConfigCommand`（含高分阈值）

写侧实现落点：
- Application：`*CommandHandler`（事务、权限、幂等、调用聚合）
- Domain：聚合方法 `task.cancel()`、`task.complete()` 等，抛出业务异常
- Out：Repository 保存聚合

### 6.2 读模型（Queries + Projections）

读侧目标：为列表/看板/统计提供高性能、低耦合查询。

读模型策略分三档（按成本递进）：

1) **轻量 CQRS（推荐起步）**：读写分离代码结构，但仍直接查现有表（复杂 SQL 在 QueryRepo）
- 优点：改动小，快速见效
- 缺点：统计查询可能慢

2) **投影表/物化视图（中期）**：针对看板/统计建立 `*_projection` 表，通过事件增量更新
- 优点：统计快、逻辑集中
- 缺点：需要数据迁移与一致性设计

3) **独立读库/搜索（后期）**：拆微服务后引入 Elastic/ClickHouse 等（视需求）

建议优先落地的投影（面向现有导出/看板）：
- `teacher_semester_evaluation_summary`：教师-学期维度（评教次数、被评次数、高分次数…）
- `course_semester_score_summary`：课程-学期维度（最低/均值/最高/课程平均分）

---

## 7. “按业务分模块”的重构步骤（从单体到可拆微服务）

> 你明确指出现有分层模式错误：因此计划以“业务 BC 模块化”为主线推进，而不是继续加深技术分层。

### Phase 0：建立可回归的安全网（1~3 天）

交付物：
- 关键用例回归清单 + 表征测试（characterization tests）
- 敏感配置治理：把密钥/密码移出版本库（环境变量/外部配置），并完成密钥轮换

验收：
- 不改变外部 API 行为下，关键链路稳定可复现

### Phase 1：模块骨架落地（3~5 天）

交付物：
- 新建 `bc-*` 模块骨架（domain/application/adapters-in/out）
- `eva-bootstrap` 作为组合根（替代现在“start + 组件扫描”过度耦合）
- 建立跨 BC 调用规范：只能依赖对方 `*-application` 暴露的接口（或 `*-api`）

验收：
- 启动成功，原接口可用；新增模块不改变行为

### Phase 2：核心域先行——评教任务/提交评教（5~10 天）

交付物：
- 把“评教任务/记录”的核心规则收敛到 `bc-evaluation-domain`
- 用领域事件替代跨层硬编码：提交评教后触发消息清理、模板锁定、统计刷新
- 增加幂等与数据约束（应用层 + DB 唯一索引二选一或都做）

验收：
- 评教闭环（发起/撤回/提交/查询）一致，且逻辑更清晰可测

### Phase 3：课程变更联动（5~10 天）

交付物：
- 把“改时段/删课”按文档规则固化（并变成事件驱动联动）
- 收敛“课程时间/教室冲突”校验实现：以可复用组件/端口统一入口，避免重复 SQL 与文案漂移（行为保持不变）
- 旧 `*GatewayImpl` 逐步退化为“委托壳”，写侧业务流程仅保留在 BC-Course 的用例/端口适配器中
- QueryWrapper 片段收敛：优先提炼“时间段重叠/教室占用”等底层查询片段，减少重复与口径漂移风险（行为保持不变）
  - 实施建议：优先迁移写侧端口适配器（如改课/自助改课/分配评教），以最小改动方式替换重复 wrapper 片段，避免口径漂移
- 统一 week/day→日期时间算法在 BC-Course 内实现并提供端口

验收：
- 改课/删课对任务/消息的联动符合 `data/doc/04_关于课程业务操作的统一说明.md`

### Phase 4：模板锁定与快照（3~7 天）

交付物：
- 明确锁定判定：首次评教 -> 创建/更新 `CourseTemplateSnapshot`
- 严格禁止“已评教课程切换模板”（应用层拦截 + 前端提示）

验收：
- 锁定行为与管理端手册一致；历史数据兼容

### Phase 5：CQRS 强化（按性能需求推进）

交付物：
- 先做“轻量 CQRS”结构化（QueryRepo），再按看板与导出需要建设投影表
- 把“高分阈值”等统计口径集中到配置与策略

验收：
- 导出 Excel 与看板查询性能可控，口径一致

### Phase 6：AI 报告（教师+学期）产品化（3~10 天）

交付物：
- `TeacherSemesterReport` 生成：结构化事实（统计）→ LLM 文案 → Word 导出
- 支持缓存/异步（可选）：避免生成时间影响接口 SLA

验收：
- 报告结构与样例一致；事实一致、文案可追溯、可重复生成

---

## 8. 微服务演进策略（保证“低成本可拆”）

### 8.1 可拆分的前置条件（在单体阶段就要做到）

1) **BC 内聚**：一个 BC 的代码、配置、数据访问都在自己的模块里，不跨模块偷拿 Mapper。
2) **边界协议稳定**：跨 BC 只通过应用层接口或事件；DTO/CO 不向领域层渗透。
3) **事件语义清晰**：事件名与负载稳定，未来可搬到 MQ。
4) **数据所有权明确**：表归属某个 BC；其它 BC 通过 API/事件获取，不直接 join（单体阶段可暂时 join，但要“隔离在 QueryRepo”里，避免写侧依赖）。

### 8.2 拆分优先级（建议）

第一批最容易拆：
- `bc-messaging`（独立 WS/消息服务）
- `bc-ai-report`（异步生成，天然后台任务）

第二批：
- `bc-evaluation`（核心域，拆分收益大但要稳）
- `bc-course`（与 evaluation 关系紧，拆分需明确 API 与一致性策略）

IAM 可独立，但要考虑单点登录与权限同步成本。

---

## 9. 质量与技术债治理（针对“设计缺陷/代码缺陷很多”的现实）

### 9.1 技术债分类与处理策略

- **架构债（优先）**：跨 BC 直接访问 DAL、领域层依赖 DTO、静态工具散落、职责混乱  
  → 通过“模块边界 + 端口 + 事件”逐步收敛
- **一致性债（高风险）**：并发下重复任务、模板锁定不严、统计口径不一致  
  → 幂等 + 唯一索引 + 统一策略类
- **安全债（最高优先级）**：明文密钥、弱配置管理  
  → 立刻迁移到环境变量/密钥管理，并轮换
- **可测试性债（长期收益）**：缺少用例级测试、难以回归  
  → 表征测试 + 端口替身（Fake/InMemory）

### 9.2 验收“不可退化”指标（建议）

- 每个 Phase 至少新增/固化一组用例级自动化回归
- 关键业务规则（week 计算、模板锁定、多人评教）必须有测试覆盖
- 关键接口响应结构不变（除非明确版本升级）
- 回归测试避免依赖外部服务/本地文件，保证可重复执行

---

## 10. 本计划的下一步落地建议（已确认基础前提）

### 10.1 已确认的基础前提（历史决策，后续不再反复确认）

- 微服务演进策略：**先拆服务，暂时共享库**（过渡期）。
- 时间语义：`cour_inf.day` 中 `day=1` 表示 **周一**（周一是一周开始）。

### 10.2 下一步优先顺序（保持“写侧优先 + 行为不变”）

1) ✅ **评教任务发布写侧收敛**：把 `EvaUpdateGatewayImpl.postEvaTask` 收敛到 `bc-evaluation` 用例 + 端口，跨域副作用（消息/日志/缓存）按“事务提交后事件”固化（行为不变；落地提交：`8e434fe1/ca69b131/e9043f96`）。
2) ✅ **评教删除写侧收敛**：把 `EvaDeleteGatewayImpl.deleteEvaRecord/deleteEvaTemplate` 收敛到 `bc-evaluation`（行为不变；落地提交：`ea928055/07b65663/05900142`）。
3) ✅ **课程读侧渐进收敛**：为 `CourseQueryGatewayImpl` 引入 `QueryPort/QueryRepo`（先结构化，再考虑 CQRS 投影表；落地提交：`ba8f2003`）。
4) ✅ **评教读侧渐进收敛**：为 `EvaQueryGatewayImpl` 抽取 `EvaQueryRepo`，gateway 退化为委托壳（保持统计口径与异常文案不变；落地提交：`02f4167d`）。
5) **评教读侧进一步解耦**：按用例维度拆分 QueryService（任务/记录/统计/模板），将 query 端口逐步迁到 `bc-evaluation` 应用层，`eva-infra` 仅保留实现（行为不变）。  
   - 进展：已拆分统计/导出、任务、记录、模板查询端口（`EvaStatisticsQueryPort` / `EvaTaskQueryPort` / `EvaRecordQueryPort` / `EvaTemplateQueryPort`），应用层开始迁移，行为保持不变；旧 `EvaQueryGatewayImpl` 已移除。

### 10.3 未完成清单（滚动，供下一会话排期）

- 消息域：`MsgGatewayImpl` 仍为 CRUD 入口；其中“删除/已读”写侧、`queryMsg/queryTargetAmountMsg` 查询读侧、`insertMessage` 写侧已收敛到 `bc-messaging`，剩余 `updateMsgDisplay` 仍在旧 gateway（待继续收敛）。
- 课程域：`CourseUpdateGatewayImpl.isImported` 仍保留在旧 gateway（偏查询/校验）。
- IAM 域：`UserUpdateGatewayImpl.assignRole/createUser` 等仍未 BC 化。
- AI 报告 / 审计日志：尚未模块化到 `bc-ai-report` / `bc-audit`。
- 读侧：`EvaQueryRepo` 仍为大聚合 QueryRepo，需继续拆分。

---

## 11. 当前已落地里程碑（滚动）

> 说明：本节用于让新会话快速判断“已经落地到哪里”，避免重复劳动；详细提交清单以 `NEXT_SESSION_HANDOFF.md` 为准。

- `bc-evaluation`：提交评教写侧已收敛（提交评教后消息清理等副作用事件化，事务提交后触发）。
- `bc-evaluation`：评教任务发布写侧已收敛（用例 + 端口 + `eva-infra` 端口适配器 + 提交后消息事件；落地提交：`8e434fe1/ca69b131/e9043f96`）。
- `bc-evaluation`：评教删除写侧已收敛（用例 + `eva-infra` 端口适配器 + 应用层入口；落地提交：`ea928055/07b65663/05900142`）。
- `bc-template`：课程模板锁定校验已落地（已评教则禁止切换模板）。
- `bc-messaging`：课程操作副作用（通知/撤回评教消息）已事件化并统一在提交后处理。
- `bc-messaging`：消息删除/已读写侧已收敛（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`22cb60eb/dd7483aa`）。
- `bc-messaging`：消息查询读侧已收敛（`queryMsg/queryTargetAmountMsg`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：见本次提交）。
- `bc-messaging`：消息插入写侧已收敛（`insertMessage`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：见本次提交）。
- `bc-course`：多条课程写链路已收敛（导入课表、改课/自助课表、删课、课程类型、课次新增等），旧 gateway 逐步退化为委托壳。
- `course`：课程读侧已结构化（`CourseQueryGatewayImpl` 退化委托壳 + `CourseQueryRepo` 抽取；落地提交：`ba8f2003`）。
- `evaluation`：评教读侧已结构化（`EvaQueryGatewayImpl` 退化委托壳 + `EvaQueryRepo` 抽取；落地提交：`02f4167d`）。
- `evaluation`：评教读侧进一步解耦第一步（统计/导出查询端口 `EvaStatisticsQueryPort` 已从网关拆分并迁移应用层，行为保持不变）。
- `evaluation`：评教读侧进一步解耦补充（任务查询端口 `EvaTaskQueryPort` 已拆分并迁移应用层，行为保持不变）。
- `evaluation`：评教读侧进一步解耦扩展（记录/模板查询端口 `EvaRecordQueryPort` / `EvaTemplateQueryPort` 已拆分并迁移应用层，行为保持不变）。
- `evaluation`：补齐 `bc-evaluation` 对 `eva-client` / `eva-domain` 的依赖，保障 QueryPort 编译通过。
- `evaluation`：补齐 `bc-evaluation` 内部依赖版本号（`eva-client` / `eva-domain`），保证 Maven 解析一致。
- `evaluation`：修正统计查询端口 DTO 包名，确保编译通过。
- `evaluation`：修正统计查询端口实现 DTO 包名，确保基础设施模块编译通过。
- `evaluation`：旧 `EvaQueryGatewayImpl` 进一步收敛，委托到细分 QueryPort，避免直接依赖 QueryRepo。
- 验证：最小回归用例 `EvaRecordServiceImplTest` / `EvaStatisticsServiceImplTest` 已通过（离线）。
- 规则：下一个会话每个步骤结束都需跑最小回归用例并记录结果。
- `evaluation`：旧 `EvaQueryGateway`/`EvaQueryGatewayImpl` 已移除，避免继续引入旧网关依赖。
- 评教读侧用例级回归测试已补充（固化统计口径；落地提交：`a48cf044`）。
- `start`：回归测试稳定化（去除本地文件/外部服务依赖；落地提交：`daf343ef`）。
- `evaluation`：评教模板新增/修改写侧已收敛到 `bc-evaluation`（新增用例 + 端口 + `eva-infra` 端口适配器，并切换 `eva-app` 入口；落地提交：`ea03dbd3`）。
- `evaluation`：清理旧 `EvaUpdateGatewayImpl.putEvaTemplate` 遗留实现（提交评教写侧入口已在 `bc-evaluation`，避免旧代码回潮；落地提交：`12279f3f`）。
- 冲突校验底层片段已收敛：
  - 教室占用冲突：`ClassroomOccupancyChecker`
  - 时间段重叠：`CourInfTimeOverlapQuery`
