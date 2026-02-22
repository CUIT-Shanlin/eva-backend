# DDD 重构计划书（面向“可低成本拆微服务”的模块化单体）

> 适用范围：本仓库 `eva-backend`（Spring Boot + Maven 多模块）。  
> 构建基线：**Java 17**（避免高版本 JDK 下的编译器/注解处理器兼容性问题）。  
> 离线验证示例：`mvnd -o -pl eva-infra -am test -q -Dmaven.repo.local=.m2/repository`（网络受限场景优先）。  
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
- 代码现状：已有 `eva-adapter / eva-app / eva-domain / eva-infra` 等技术分层模块；但“领域层”被 DTO/CO/Cmd 污染，跨域耦合明显；且 `eva-client` 承载了大量 BO/CO/DTO（边界协议对象），导致“业务归属不清 + 复用边界失控”。

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

> 核心原则（需求变更，2025-12-24）：在 **root 聚合**里，**每个 BC 只占用一个顶层聚合模块**（例如 `bc-iam/`）。  
> 该 BC 顶层模块自身通常是 `pom` packaging，用来聚合其内部子模块；BC 内部再按职责拆为 `domain/application/infrastructure`（必要时再细分 adapter-in/out，但过渡期可先并入 `infrastructure`）。  
> 这样做的目的：root `pom.xml` 的 `<modules>` 保持“一个 BC 一条”，避免历史平铺过渡模块（如 `bc-*-infra`）继续膨胀，后续“折叠归位/拆微服务”成本更可控。

建议新增/调整为：

- `start`：组合根（Spring Boot 启动、配置装配、跨模块组装）
- `bc-iam/`（顶层：聚合模块）
  - `bc-iam/domain`（artifactId 示例：`bc-iam-domain`）：领域模型 + 端口（Ports）
  - `bc-iam/application`（artifactId 示例：`bc-iam-application`）：用例编排（Commands/Queries）+ 边界协议对象（`contract/dto`）
  - `bc-iam/infrastructure`（artifactId 示例：`bc-iam-infrastructure`）：出站适配器（DB/Mapper/LDAP/缓存/日志等），实现 Ports
  - （可选）`bc-iam/adapter`：如确需把入站 Controller 从 `eva-adapter` 折叠归位，可再拆 `adapter-in` 子模块；过渡期可先不做
- `bc-course/`、`bc-template/`、`bc-evaluation/`、`bc-messaging/`、`bc-audit/`、`bc-ai-report/`（结构同上）
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
- 新对话启动方式：优先复制 `NEXT_SESSION_HANDOFF.md` 的 **0.11 推荐版（主线优先）**（不固化 commitId），并按其中顺序阅读与执行。
- JDK 管理：本仓库已改为使用 mise 管理 JDK（已提供 `mise.toml`，固定 `java@temurin-17`）；为避免 shell/非交互环境的 `java` 指向漂移，建议所有构建/测试命令统一用 `mise exec java@temurin-17 -- <cmd>`（例如 `mise exec java@temurin-17 -- mvn ...` / `mise exec java@temurin-17 -- mvnd ...`）；如需让交互 shell 的 `java` 直接生效再执行 `mise use java@temurin-17`（可选）。

### 10.2 下一步优先顺序（保持“写侧优先 + 行为不变”）

> 路线选择（更新至 2026-02-22）：采用 **方案 B（严格）** —— 以“DoD 可验证”的方式彻底退场 `eva-*` 技术切片（详见 10.5.B）。跨 BC 只允许通过 `*-contract` + `shared-kernel` 对外接口调用；当前已完成 `eva-*` reactor 清零 + 目录退场收口（`eva-base/eva-infra/eva-infra-dal/...` 均已退场闭环，详见本节补充进展）。后续主线建议从“技术切片退场”切换回“写侧优先的业务重构主线”（从 Backlog 4.3/第 6 节选 1 个可单刀闭环的目标继续推进）。

> 滚动口径（更新至 2026-02-04）：✅ `eva-app` 已完成退场闭环（源码清零 + 组合根 `start` 去依赖 + root reactor 移除 + 删除 `eva-app/pom.xml`）；✅ `eva-adapter` 残留 `*Controller` 已清零，且组合根 `start` 已移除对 `eva-adapter` 的 Maven 依赖（保持行为不变）；✅ `eva-adapter` 已从 root reactor 退场（root `pom.xml` 移除 `<module>eva-adapter</module>`；最小回归通过；落地：`86842a1f`）；✅ 已删除 `eva-adapter/pom.xml`（全仓库 `**/pom.xml` 不再出现 `<artifactId>eva-adapter</artifactId>`；最小回归通过；落地：`ed244cad`）；🎯 下一步主线：并行推进“依赖方 `pom.xml` 编译期依赖收敛”，并评估是否需要删除 `eva-adapter/` 空目录（需独立提交；保持行为不变）。
> 补充（更新至 2026-02-04，保持行为不变）：主线已转向 **bc-course S0.2 延伸：逐类把课程域类型从 `eva-domain` 归位到 `bc-course-domain`（保持 `package` 不变）**，以缩小 `eva-domain` 表面积并为后续“依赖方去 `eva-domain`”创造前置条件（下一刀建议见 10.3 的 bc-course 小节）。IAM 的 “S0.2 延伸（依赖方收敛）” 已阶段性闭环，作为并行任务保留历史记录与回溯口径。
> 新会话续接方式：优先复制 `NEXT_SESSION_HANDOFF.md` 的 0.11「推荐版（主线优先）」并按 0.10 的“下一步拆分与里程碑/提交点”顺序执行，避免遗漏约束与回归命令。
> 工作区保护（保持行为不变）：若出现“已暂存新增 + 未暂存改动”的混合状态，禁止 `reset/checkout`；优先用 `git commit -- <path>` 做范围隔离，逐步恢复到“单文件一刀闭环”。

- ✅ 补充进展（2026-02-21，保持行为不变，读侧收敛，单类）：课程域读侧 `CourseRecommendExce` 已将“仅为拿课程ID列表而先查 CourseDO 再映射”的残留点，收敛为调用最小端口 `CourseIdsByCourseWrapperDirectQueryPort/CourseIdsByTeacherIdAndSemesterIdQueryPort`（保持查询条件、结果顺序与空值语义不变；最小回归通过；落地：`53601b8c`）。
- ✅ 补充进展（2026-02-21，保持行为不变，读侧收敛，单类）：课程域读侧仓储 `CourseQueryRepository` 已将“按 semesterId 获取课程ID列表/按 teacherId+semesterId 获取课程ID列表”的实现，从 `CourseMapper.selectList(...).map(CourseDO::getId)` 收敛为调用最小端口 `CourseIdsBySemesterIdQueryPort/CourseIdsByTeacherIdAndSemesterIdQueryPort`（保持查询条件、结果顺序与空值语义不变；最小回归通过；落地：`02b94cde`）。
- ✅ 补充进展（2026-02-21，保持行为不变，口径清理，单类）：清理 `bc-course/infrastructure` 的 `CourseImportExce` 中历史注释残留的旧实现（避免 `rg`/Serena 盘点时被“注释命中”干扰，减少口径漂移；最小回归通过；落地：`1be6955b`）。
- ✅ 补充进展（2026-02-21，保持行为不变，配置读取去重复，单类）：`bc-evaluation/infrastructure` 的 `EvaConfigGatewayImpl` 抽取 `applyIntIfPresent(...)` 统一 `readConfig` 中 JSON 数值字段读取/赋值逻辑（字段赋值顺序、异常/日志文案与副作用顺序不变；最小回归通过；落地：`d275fee7`）。
- ✅ 补充进展（2026-02-21，保持行为不变，日志映射去重复，单类）：`bc-audit/infrastructure` 的 `LogGatewayImpl` 提炼 `toSysLogEntityList(...)`，保持 `page` 内日志映射顺序不变并简化 `insertLog` 异步 lambda（SQL 条件/排序、异常文案与副作用顺序不变；最小回归通过；落地：`63565d73`）。
- ✅ 补充进展（2026-02-21，保持行为不变，口径清理，单类）：`bc-course/infrastructure` 的 `CourseUpdateGatewayImpl` 统一 import/字段/方法缩进并移除无用 import，同时将 `addCourse` 的注释更正为“保留空实现（行为不变）”（不改变业务语义/异常文案/副作用顺序；最小回归通过；落地：`0c41b4de`）。
- ✅ 补充进展（2026-02-21，保持行为不变，日志链路去重复，单类）：`bc-audit/infrastructure` 的 `LogServiceImpl` 抽取 `buildSysLogBO(...)` / `resolveModuleIdOrThrow(...)`，用于稳定日志模块ID解析逻辑（异常文案、日志文案与副作用顺序不变；最小回归通过；落地：`a4c14240`）。
- ✅ 补充进展（2026-02-22，保持行为不变，依赖边界收敛，单 `pom.xml`）：移除 `bc-evaluation/infrastructure/pom.xml` 对 `bc-course-infra` 的 Maven 编译期依赖，避免评教域编译期绑定课程域实现侧（Serena 证据化：评教域基础设施内无课程 DAL Mapper 引用；最小回归通过；落地：`375c671f`）。
- ✅ 补充进展（2026-02-22，保持行为不变，读侧去重复，单类）：`bc-evaluation/infrastructure` 的 `EvaRecordQueryRepository` 抽取 `rethrowInvocationTargetException(...)` 复用 sysUserMapper 反射调用异常解包逻辑，保持对 `RuntimeException/Error` 的原样抛出语义不变（最小回归通过；落地：`479ce62a`）。
- ✅ 补充进展（2026-02-22，保持行为不变，读侧去重复，单类）：`bc-evaluation/infrastructure` 的 `EvaTaskQueryRepository` 抽取 `rethrowInvocationTargetException(...)` 复用 sysUserMapper 反射调用异常解包逻辑，保持对 `RuntimeException/Error` 的原样抛出语义不变（最小回归通过；落地：`84f5d6b0`）。
- ✅ 补充进展（2026-02-22，保持行为不变，写侧去重复，单类）：`bc-evaluation/infrastructure` 的 `PostEvaTaskRepositoryImpl` 抽取 `rethrowInvocationTargetException(...)` 复用反射调用异常解包逻辑，保持对 `RuntimeException/Error` 的原样抛出语义不变（最小回归通过；落地：`0fcca08c`）。
- ✅ 补充进展（2026-02-22，保持行为不变，写侧去重复，单类）：`bc-evaluation/infrastructure` 的 `SubmitEvaluationRepositoryImpl` 抽取 `rethrowInvocationTargetException(...)` 复用反射调用异常解包逻辑，保持对 `RuntimeException/Error` 的原样抛出语义不变（最小回归通过；落地：`135ebc71`）。
- ✅ 补充进展（2026-02-22，保持行为不变，依赖边界收敛前置，单类）：`bc-iam/infrastructure` 的 `UserServiceImpl.getOneUserScore` 将课程域查询调用点提炼为私有方法 `getSelfTeachCourseInfoByUserId/findEvaScoreByCourseId`，保持异常文案与调用顺序不变（最小回归通过；落地：`254e0bca`）。
- ✅ 补充进展（2026-02-22，保持行为不变，写侧去重复，单类）：`bc-evaluation/infrastructure` 的 `DeleteEvaRecordRepositoryImpl` 将删除记录后的“模板清理判断 + 缓存失效”后置流程提炼为私有方法（不改任何查询条件/异常文案/日志文案与副作用顺序；最小回归通过；落地：`4ef05cb2`）。
- ✅ 补充进展（2026-02-22，保持行为不变，写侧去重复，单类）：`bc-evaluation/infrastructure` 的 `EvaDeleteGatewayImpl` 将 `deleteEvaRecord/deleteEvaTemplate` 内重复的异常映射逻辑提炼为私有方法（`@Transactional` 仍在公开方法；异常文案与副作用顺序不变；最小回归通过；落地：`6126ddcb`）。
- ✅ 补充进展（2026-02-22，保持行为不变，写侧去重复，单类）：`bc-evaluation/infrastructure` 的 `DeleteEvaTemplateRepositoryImpl` 将模板删除后的缓存失效调用提炼为私有方法 `invalidateTemplateCaches(...)`（异常文案、日志文案与副作用顺序不变；最小回归通过；落地：`479c5500`）。
- ✅ 补充进展（2026-02-22，保持行为不变，写侧去重复，单类）：`bc-evaluation/infrastructure` 的 `EvaDeleteGatewayImpl.deleteAllTaskByTea` 将“按 teacherId 查询任务 + 删除任务”的 Mapper 调用提炼为私有方法，保持查询条件、删除顺序与返回值语义不变（最小回归通过；落地：`cfb51628`）。
- ✅ 补充进展（2026-02-22，保持行为不变，写侧去重复，单类）：`bc-evaluation/infrastructure` 的 `UpdateEvaTemplateRepositoryImpl` 将模板修改后的缓存失效调用提炼为私有方法 `invalidateTemplateCaches(...)`，保持调用顺序与参数构造口径不变（最小回归通过；落地：`c5f859ff`）。
- ✅ 补充进展（2026-02-22，保持行为不变，写侧去重复，单类）：`bc-evaluation/infrastructure` 的 `SubmitEvaluationRepositoryImpl` 将提交评教后的缓存失效逻辑提炼为私有方法 `invalidateCachesAfterSaveEvaluation(...)`，保持缓存 key、失效顺序与 `evaluatorName` 补齐口径不变（最小回归通过；落地：`02fb49f1`）。
- ✅ 补充进展（2026-02-22，保持行为不变，写侧去重复，单类）：`bc-evaluation/infrastructure` 的 `PostEvaTaskRepositoryImpl` 将发起评教任务后的缓存失效逻辑提炼为私有方法 `invalidateTaskListCaches(...)`，保持缓存 key、失效顺序与教师名解析口径不变（最小回归通过；落地：`59c0a974`）。
- ✅ 补充进展（2026-02-22，保持行为不变，写侧口径清理，单类）：`bc-evaluation/infrastructure` 的 `EvaUpdateGatewayImpl` 在 `cancelEvaTaskById` 内抽取 `invalidateTaskListBySemester/invalidateTaskListByTeacher`，用于减少重复并稳定缓存失效调用点（缓存 key、异常文案与副作用顺序不变；最小回归通过；落地：`c7f38a55`）。
- 🎯 下一刀建议（更新至 2026-02-21；保持行为不变；单类；写侧优先）：从 Backlog 4.3/第 6 节候选中选 1 个写侧目标继续推进（例如优先在 `bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java` 或 `bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/eva/EvaUpdateGatewayImpl.java` 中挑 1 个方法簇做“旧 gateway 退化为委托壳”收敛）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧前置，单类）：在 `bc-course/application` 新增最小查询端口 `CourseIdsByTeacherIdQueryPort`（后续用于将评教写侧 `PostEvaTaskRepositoryImpl` 内对课程域 `CourseMapper.selectList(eq teacher_id)` 的跨 BC 直连按“补适配器（单类）→ 改调用侧（单类）”逐步收敛为调用端口；最小回归通过；落地：`cdc79886`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourseIdsByTeacherIdQueryPortImpl`（内部直接委托 `CourseMapper.selectList(eq teacher_id)` 并映射 `CourseDO::getId`，保持结果顺序/空值语义不变；最小回归通过；落地：`80747dea`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧收敛，单类）：收敛评教写侧调用点：`bc-evaluation/infrastructure` 的 `PostEvaTaskRepositoryImpl` 已将“按 teacherId 查询课程 id 列表”的课程域 DAL 直连收敛为调用 `CourseIdsByTeacherIdQueryPort.findCourseIdsByTeacherId(...)`（端口适配器仍原样委托 `CourseMapper`，确保查询条件/结果顺序/异常文案/副作用顺序不变；最小回归通过；落地：`a7dd8c26`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧前置，单类）：在 `bc-course/application` 新增最小查询端口 `SemesterStartDateQueryPort`（后续用于将评教写侧 `PostEvaTaskRepositoryImpl` 内对课程域 `SemesterMapper.selectById` 的跨 BC 直连按“补适配器（单类）→ 改调用侧（单类）”逐步收敛为调用端口；最小回归通过；落地：`b82e9190`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `SemesterStartDateQueryPortImpl`（内部直接委托 `SemesterMapper.selectById` 并映射 `SemesterDO::getStartDate`，保持空值语义不变；最小回归通过；落地：`013429b6`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧收敛，单类）：收敛评教写侧调用点：`bc-evaluation/infrastructure` 的 `PostEvaTaskRepositoryImpl` 已将“查询学期开始日期”的课程域 `SemesterMapper.selectById(...).getStartDate()` 直连收敛为调用 `SemesterStartDateQueryPort.findStartDateBySemesterId(...)`（端口适配器仍委托 `SemesterMapper`，确保计算逻辑/异常文案/副作用顺序不变；最小回归通过；落地：`69dbc07b`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧前置，单类）：在 `bc-course/application` 新增最小查询端口 `CourseTeacherAndSemesterQueryPort`（后续用于将评教写侧 `PostEvaTaskRepositoryImpl` 内对课程域 `CourseMapper.selectById` 的跨 BC 直连按“补适配器（单类）→ 改调用侧（单类）”逐步收敛为调用端口；最小回归通过；落地：`4833b878`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourseTeacherAndSemesterQueryPortImpl`（内部直接委托 `CourseMapper.selectById` 并映射 `CourseDO.teacherId/semesterId`，保持空值语义不变；最小回归通过；落地：`1d18b31c`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧收敛，单类）：收敛评教写侧调用点：`bc-evaluation/infrastructure` 的 `PostEvaTaskRepositoryImpl` 已将“查询课程教师与学期信息”的课程域 `CourseMapper.selectById` 直连收敛为调用 `CourseTeacherAndSemesterQueryPort.findByCourseId(...)`（端口适配器仍委托 `CourseMapper`，确保异常文案/副作用顺序/空值语义不变；最小回归通过；落地：`88afcf64`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧收敛，单类）：收敛评教写侧调用点：`bc-evaluation/infrastructure` 的 `SubmitEvaluationRepositoryImpl` 已清零对课程域 `CourseMapper.selectById` 的编译期直连，改为调用 `CourseTeacherAndSemesterQueryPort` + `CourseTemplateIdQueryPort` 查询 `semesterId/templateId`（确保异常文案、缓存失效与副作用顺序不变；最小回归通过；落地：`7abb02df`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧收敛，单类）：收敛评教写侧调用点：`bc-evaluation/infrastructure` 的 `DeleteEvaRecordRepositoryImpl` 已清零对课程域 `CourseMapper.selectById` 的编译期直连，改为调用 `CourseTeacherAndSemesterQueryPort.findByCourseId(...)` 做课程存在性校验（确保异常文案、日志与副作用顺序不变；最小回归通过；落地：`b99b4073`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧前置，单类）：在 `bc-course/application` 新增最小查询端口 `CourseIdByTemplateIdQueryPort`（后续用于将评教写侧 `DeleteEvaTemplateRepositoryImpl` 内对课程域 `CourseMapper.selectOne(eq templateId)` 的跨 BC 直连按“补适配器（单类）→ 改调用侧（单类）”逐步收敛为调用端口；最小回归通过；落地：`5cf5e405`）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourseIdByTemplateIdQueryPortImpl`（内部直接委托 `CourseMapper.selectOne(eq templateId)` 并映射返回 `courseId`，保持空值语义不变；最小回归通过；落地：`15d17b6f`）。
- 🎯 下一步建议（保持行为不变）：评教读侧 `EvaTaskQueryRepository/EvaRecordQueryRepository/EvaStatisticsQueryRepository` 已完成去课程域 `CourseMapper/SemesterMapper/SubjectMapper` 编译期直连（分别改走 `CourseAndSemesterObjectDirectQueryPort` / `SubjectObjectDirectQueryPort`），且已将“按 teacherId(+semesterId) 获取课程ID列表”的实现进一步收敛为调用最小端口 `CourseIdsByTeacherIdQueryPort/CourseIdsByTeacherIdAndSemesterIdQueryPort`（保持查询条件、结果顺序与空值语义不变）。评教读侧中“按 semesterId / teacherId 拿课程ID列表”的场景也已开始收敛为调用最小端口 `CourseIdsBySemesterIdQueryPort` / `CourseIdsByTeacherIdQueryPort`（保持行为不变）。下一刀建议继续在评教读侧中盘点同类“仅为拿课程ID列表而先查对象再映射”的残留点（优先从 `EvaStatisticsQueryRepository` 之外的 QueryRepo 开始），逐刀改走最小端口（每刀只改 1 个类闭环）。
- ✅ 补充进展（2026-02-20，保持行为不变，写侧收敛，单类）：收敛评教写侧调用点：`bc-evaluation/infrastructure` 的 `DeleteEvaTemplateRepositoryImpl` 已清零对课程域 `CourseMapper.selectOne(eq templateId)` 的编译期直连，改为调用 `CourseIdByTemplateIdQueryPort.findCourseIdByTemplateId(...)` 判断模板是否被课程分配（确保异常文案、缓存失效与副作用顺序不变；最小回归通过；落地：`85a191f2`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧前置，单类）：在 `bc-course/application` 新增最小查询端口 `CourseIdsByTeacherIdAndSemesterIdQueryPort`（后续用于将评教读侧 QueryRepo 内对课程域 `CourseMapper.selectList(eq teacher_id, semester_id)` 的跨 BC 直连按“补适配器（单类）→ 改调用侧（单类）”逐步收敛为调用端口；最小回归通过；落地：`bf647e27`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧前置，单类）：在 `bc-course/application` 新增课程/学期基础对象直查端口 `CourseAndSemesterObjectDirectQueryPort`（用于评教读侧 QueryRepo 去 `CourseMapper/SemesterMapper` 编译期直连；仅接口，无实现/无调用点，避免行为漂移；最小回归通过；落地：`c389a801`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧前置，单类）：在 `bc-course/application` 新增科目基础对象直查端口 `SubjectObjectDirectQueryPort`（用于后续将评教读侧 QueryRepo 去 `SubjectMapper` 编译期直连；仅接口，无实现/无调用点，避免行为漂移；最小回归通过；落地：`a68c1b74`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `SubjectObjectDirectQueryPortImpl`（内部仅原样委托 `SubjectMapper.selectList/selectById`，保持查询条件、结果顺序与空值语义不变；最小回归通过；落地：`1f645712`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧收敛，单类）：收敛评教读侧调用点：`bc-evaluation/infrastructure` 的 `EvaTaskQueryRepository` 已清零对课程域 `SubjectMapper` 的编译期直连，改为调用 `SubjectObjectDirectQueryPort` 完成科目查询（确保查询次数/顺序与空值语义不变；最小回归通过；落地：`0b11f2c4`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧收敛，单类）：收敛评教读侧调用点：`bc-evaluation/infrastructure` 的 `EvaRecordQueryRepository` 已清零对课程域 `SubjectMapper` 的编译期直连，改为调用 `SubjectObjectDirectQueryPort` 完成科目查询（确保查询次数/顺序与空值语义不变；最小回归通过；落地：`cfae3eba`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourseAndSemesterObjectDirectQueryPortImpl`（内部仅原样委托 `CourseMapper/SemesterMapper` 的查询方法，保持查询条件、结果顺序与空值语义不变；最小回归通过；落地：`0241e079`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧收敛，单类）：收敛评教读侧调用点：`bc-evaluation/infrastructure` 的 `EvaTaskQueryRepository` 已清零对课程域 `CourseMapper/SemesterMapper` 的编译期直连，改为调用 `CourseAndSemesterObjectDirectQueryPort` 完成课程/学期查询（确保异常文案、查询条件与副作用顺序不变；最小回归通过；落地：`ccc64867`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧收敛，单类）：收敛评教读侧调用点：`bc-evaluation/infrastructure` 的 `EvaRecordQueryRepository` 已清零对课程域 `CourseMapper/SemesterMapper` 的编译期直连，改为调用 `CourseAndSemesterObjectDirectQueryPort` 完成课程/学期查询（确保异常文案、查询条件与副作用顺序不变；最小回归通过；落地：`75494f6c`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧收敛，单类）：收敛评教读侧调用点：`bc-evaluation/infrastructure` 的 `EvaStatisticsQueryRepository` 已清零对课程域 `CourseMapper` 的编译期直连，改为调用 `CourseAndSemesterObjectDirectQueryPort` 完成课程查询（确保异常文案、查询条件与副作用顺序不变；最小回归通过；落地：`f4e5b4a9`）。
- ✅ 补充进展（2026-02-20，保持行为不变，评教侧收敛，单类）：收敛评教撤回任务缓存失效 key 计算：`bc-evaluation/infrastructure` 的 `EvaUpdateGatewayImpl.cancelEvaTaskById(...)` 已清零对课程域 `CourseMapper.selectById(courseId).getSemesterId()` 的编译期直连，改为调用 `CourseAndSemesterObjectDirectQueryPort.findCourseById(courseId).getSemesterId()`（缓存失效 key 与副作用顺序不变；最小回归通过；落地：`63e3d2dc`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧前置，单类）：在 `bc-course/application` 新增最小查询端口 `CourseIdsBySemesterIdQueryPort`（后续用于将评教读侧 `EvaTemplateQueryRepository.getEvaTaskIdS(semId)` 内对课程域 `CourseMapper.selectList(eq semester_id)` 的跨 BC 直连按“补适配器（单类）→ 改调用侧（单类）”逐步收敛为调用端口；最小回归通过；落地：`183aae12`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourseIdsBySemesterIdQueryPortImpl`（内部直接委托 `CourseMapper.selectList(eq semester_id)` 并映射 `CourseDO::getId` 返回课程ID列表，保持结果顺序/空值语义不变；最小回归通过；落地：`d91536a0`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧收敛，单类）：收敛评教读侧调用点：`bc-evaluation/infrastructure` 的 `EvaTemplateQueryRepository` 已将 `CourseMapper.selectList(eq semester_id)` 直连收敛为调用 `CourseIdsBySemesterIdQueryPort.findCourseIdsBySemesterId(...)`（确保异常文案、缓存 key 与副作用顺序不变；最小回归通过；落地：`2772e6be`）。
- ✅ 补充进展（2026-02-21，保持行为不变，读侧收敛，单类）：收敛评教统计读侧调用点：`bc-evaluation/infrastructure` 的 `EvaStatisticsQueryRepository.getEvaTaskIdS(semId)` 已将 `CourseAndSemesterObjectDirectQueryPort.findCourseList(eq semester_id)` 的“查 CourseDO 列表再映射 id”收敛为调用 `CourseIdsBySemesterIdQueryPort.findCourseIdsBySemesterId(...)` 获取课程ID列表（确保查询条件、结果顺序与空值语义不变；最小回归通过；落地：`265c1301`）。
- ✅ 补充进展（2026-02-21，保持行为不变，读侧收敛，单类）：收敛评教统计读侧调用点：`bc-evaluation/infrastructure` 的 `EvaStatisticsQueryRepository.getEvaEdNumByTeacherIdAndLocalTime(...)` 已将 `CourseAndSemesterObjectDirectQueryPort.findCourseList(eq teacher_id)` 的“查 CourseDO 列表再映射 id”收敛为调用 `CourseIdsByTeacherIdQueryPort.findCourseIdsByTeacherId(...)` 获取课程ID列表（确保查询条件、结果顺序与空值语义不变；最小回归通过；落地：`2875c013`）。
- ✅ 补充进展（2026-02-21，保持行为不变，读侧前置，单类）：在 `bc-course/application` 新增查询端口 `CourseIdsByCourseWrapperDirectQueryPort`，用于在调用方已构造 MyBatis-Plus 条件（Wrapper）的场景下直接获取课程ID列表，以便收敛跨 BC 读侧的 `CourseDO::getId` 映射残留点（不引入新副作用；最小回归通过；落地：`de139ba9`）。
- ✅ 补充进展（2026-02-21，保持行为不变，读侧前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourseIdsByCourseWrapperDirectQueryPortImpl`，内部仅委托 `CourseMapper.selectList(...)` 并映射 `CourseDO::getId` 返回课程ID列表（保持查询条件、结果顺序与空值语义不变；最小回归通过；落地：`cc79d929`）。
- ✅ 补充进展（2026-02-21，保持行为不变，读侧收敛，单类）：收敛评教任务读侧调用点：`bc-evaluation/infrastructure` 的 `EvaTaskQueryRepository` 已将 `pageEvaUnfinishedTask/evaSelfTaskInfo` 中“查 CourseDO 列表再映射 id”的实现收敛为调用 `CourseIdsByCourseWrapperDirectQueryPort.findCourseIds(...)` 获取课程ID列表（保持 Wrapper 构造、查询条件、结果顺序与空值语义不变；最小回归通过；落地：`7c5a70eb`）。
- ✅ 补充进展（2026-02-21，保持行为不变，读侧收敛，单类）：收敛评教记录读侧调用点：`bc-evaluation/infrastructure` 的 `EvaRecordQueryRepository` 已将 `pageEvaRecord/getEvaLogInfo` 中“查 CourseDO 列表再映射 id”的实现收敛为调用 `CourseIdsByCourseWrapperDirectQueryPort.findCourseIds(...)` 获取课程ID列表（保持 Wrapper 构造、查询条件、结果顺序与空值语义不变；最小回归通过；落地：`7d3b1844`）。
- ✅ 补充进展（2026-02-21，保持行为不变，写侧收敛，单类）：收敛听课/评教老师分配调用点：`bc-course/infrastructure` 的 `AssignEvaTeachersRepositoryImpl.judgeAlsoHasCourse(...)` 已将 `CourseMapper.selectList(eq semester_id, in teacher_id)` 的“查 CourseDO 列表再映射 id”收敛为调用 `CourseIdsByCourseWrapperDirectQueryPort.findCourseIds(...)` 获取课程ID列表（保持 Wrapper 构造、查询条件、结果顺序与空值语义不变；最小回归通过；落地：`85768287`）。
- ✅ 补充进展（2026-02-20，保持行为不变，读侧收敛，单类）：收敛评教读侧调用点：`bc-evaluation/infrastructure` 的 `EvaTemplateQueryRepository` 已清零对课程域 `CourseMapper.selectById` 的编译期直连，改为调用 `CourseTeacherAndSemesterQueryPort` + `CourseTemplateIdQueryPort` 获取 `templateId`（确保异常文案、缓存 key 与副作用顺序不变；最小回归通过；落地：`437a449e`）。
- ✅ 补充进展（2026-02-20，保持行为不变，依赖收敛，单 pom）：收敛 `bc-iam/contract/pom.xml`：移除对 `eva-base-common` 的 Maven 编译期依赖（Serena 证伪 `bc-iam/contract` 范围内仅 `pom.xml` 命中 `eva-base-common`；最小回归通过；落地：`fc801525`）。
- ✅ 补充进展（2026-02-20，保持行为不变，依赖收敛，单 pom）：收敛 `bc-iam/infrastructure/pom.xml`：移除对 `eva-base-common` 的 Maven 编译期依赖（Serena 证伪 `bc-iam/infrastructure` 范围内仅 `pom.xml` 命中 `eva-base-common`；最小回归通过；落地：`13398a74`）。
- ✅ 补充进展（2026-02-20，保持行为不变，reactor 退场，单 pom）：收敛 `eva-base/pom.xml`：从 `<modules>` 中移除 `<module>eva-base-common</module>`（前置：依赖方已清零；最小回归通过；落地：`dc8d949d`）。
- ✅ 补充进展（2026-02-20，保持行为不变，目录退场前置，单文件）：删除 `eva-base/eva-base-common/pom.xml`（前置：全仓库 `**/pom.xml` 不再出现 `<artifactId>eva-base-common</artifactId>`；最小回归通过；落地：`5ece67c3`）。
- ✅ 补充进展（2026-02-20，保持行为不变，reactor 退场，单 pom）：收敛 root `pom.xml`：移除 `<module>eva-base</module>`（前置：`eva-base-common` 依赖点清零 + `eva-base-common` module/pom 已退场；最小回归通过；落地：`786fc543`）。
- ✅ 补充进展（2026-02-20，保持行为不变，目录退场前置，单文件）：删除 `eva-base/eva-base-config/pom.xml`（前置：全仓库 `**/pom.xml` 内 `<artifactId>eva-base-config</artifactId>` 仅命中其本体；最小回归通过；落地：`1ba75ddf`）。
- ✅ 补充进展（2026-02-20，保持行为不变，目录退场前置，单文件）：删除 `eva-base/pom.xml`（前置：全仓库 `**/pom.xml` 内 `<artifactId>eva-base</artifactId>` 仅命中其本体；最小回归通过；落地：`45770eec`）。
- ✅ 补充进展（2026-02-20，保持行为不变，目录退场，单目录）：清理 `eva-base/` 目录本身（目录内仅保留过被忽略的构建产物；最小回归通过；落地：`4eb50feb`；注：Git 不跟踪空目录/忽略产物，因此使用空提交记录该动作）。
- ✅ 补充进展（2026-02-20，保持行为不变，目录退场，单目录）：清理 `eva-app/` 目录本身（目录内仅剩被忽略的构建产物与 IDE 元数据；最小回归通过；落地：`3990a3b7`；注：Git 不跟踪空目录/忽略产物，因此使用空提交记录该动作）。
- ✅ 补充进展（2026-02-20，保持行为不变，目录退场，单目录）：清理 `eva-adapter/` 目录本身（目录内仅剩被忽略的构建产物与 IDE 元数据；最小回归通过；落地：`ec620b56`；注：Git 不跟踪空目录/忽略产物，因此使用空提交记录该动作）。
- ✅ 补充进展（2026-02-20，保持行为不变，目录退场前置，单文件）：删除 `eva-infra/src/main/java/edu/cuit/infra/convertor/package-info.java`（`eva-infra` 目录退场收口；最小回归通过；落地：`4f9391ab`）。
- ✅ 补充进展（2026-02-20，保持行为不变，目录退场前置，单文件）：删除 `eva-infra/src/main/java/edu/cuit/infra/dal/package-info.java`（`eva-infra` 目录退场收口；最小回归通过；落地：`4d41b7c7`）。
- ✅ 补充进展（2026-02-20，保持行为不变，目录退场前置，单文件）：删除 `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/package-info.java`（`eva-infra` 目录退场收口；最小回归通过；落地：`0b4bc7f8`）。
- ✅ 补充进展（2026-02-20，保持行为不变，目录退场，单目录）：清理 `eva-infra/` 目录本身（前置：`git ls-files eva-infra` 为 0；目录内仅剩被忽略的构建产物与 IDE 元数据；最小回归通过；落地：`50622ef2`；注：Git 不跟踪空目录/忽略产物，因此使用空提交记录该动作）。
- ✅ 补充进展（2026-02-20，保持行为不变，目录退场，单目录）：清理 `eva-infra-dal/` 目录本身（前置：`git ls-files eva-infra-dal` 为 0；Serena 列目录确认无任何 `*.java`；按约束先尝试 `mvnd`（初始化异常失败）后降级 `mvn` 最小回归通过；落地：`1a5a9fb2`；注：Git 不跟踪空目录/忽略产物，因此使用空提交记录该动作）。
- ✅ 补充进展（2026-02-19，保持行为不变，引用面收敛前置，单类）：在 `bc-course/application` 新增最小 Port：`SingleCourseCoConvertPort`（承接 `SingleCourseEntity -> SingleCourseCO` 的转换能力，用于后续收敛 `bc-evaluation/infrastructure` 对 `CourseBizConvertor` 的直接依赖；最小回归通过；落地：`32c458e7`）。
- ✅ 补充进展（2026-02-19，保持行为不变，引用面收敛前置，单类）：在 `bc-course/infrastructure` 新增 Port Adapter：`SingleCourseCoConvertPortImpl`（内部直接委托 `CourseBizConvertor.toSingleCourseCO(...)`，确保映射行为不变；最小回归通过；落地：`fd28bbb9`）。
- ✅ 补充进展（2026-02-19，保持行为不变，引用面收敛前置，单类）：收敛评教侧调用点：`bc-evaluation/infrastructure` 的 `MsgServiceImpl` 改为依赖 `SingleCourseCoConvertPort`（不再直接调用 `CourseBizConvertor` 完成 `SingleCourseEntity -> SingleCourseCO` 转换；异常/日志/副作用顺序不变；最小回归通过；落地：`65a2e261`）。
- ✅ 补充进展（2026-02-19，保持行为不变，引用面收敛前置，单类）：清理 `bc-evaluation/**` 对 `CourseBizConvertor` 的编译期类型依赖：`MsgServiceImpl` 的测试构造器重载改为接收 `Object` 并通过反射委托构造 `SingleCourseCoConvertPort`（用于下一刀搬运 `CourseBizConvertor` 归位；最小回归通过；落地：`9f7d27b0`）。
- ✅ 补充进展（2026-02-19，保持行为不变，Convertor 归位，单类搬运）：将 `CourseBizConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-course/infrastructure`（保持 `package` 与类内容不变；口径更新：`eva-infra-shared` Java 余量由 `3` 变更为 `2`；最小回归通过；落地：`a082b812`）。
- ✅ 补充进展（2026-02-19，保持行为不变，引用面收敛前置，单类）：在 `bc-course/application` 新增最小 Port：`CourseEntityConvertPort`（用于后续收敛 `bc-evaluation/infrastructure` 对 `CourseConvertor` 的直接依赖；保留 `teacher` 的 `Supplier<?>` 桥接口径；最小回归通过；落地：`0a95da8f`）。
- ✅ 补充进展（2026-02-19，保持行为不变，引用面收敛前置，单类）：在 `bc-course/infrastructure` 新增 Port Adapter：`CourseEntityConvertPortImpl`（内部直接委托 `CourseConvertor`，确保映射与调用时机不变；最小回归通过；落地：`eb597366`）。
- ✅ 补充进展（2026-02-19，保持行为不变，引用面收敛，单类）：收敛评教读侧调用点：`EvaTaskQueryRepository` 改为依赖 `CourseEntityConvertPort`（不再直接注入 `CourseConvertor`；最小回归通过；落地：`c2c34a9a`）。
- ✅ 补充进展（2026-02-19，保持行为不变，引用面收敛，单类）：收敛评教读侧调用点：`EvaRecordQueryRepository` 改为依赖 `CourseEntityConvertPort`（不再直接注入 `CourseConvertor`；最小回归通过；落地：`307946d4`）。
- ✅ 补充进展（2026-02-19，保持行为不变，Convertor 归位，单类搬运）：将 `CourseConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-course/infrastructure`（保持 `package` 与类内容不变；口径更新：`eva-infra-shared` Java 余量由 `2` 变更为 `1`；最小回归通过；落地：`e91badc4`）。
- ✅ 补充进展（2026-02-19，保持行为不变，Convertor 收尾前置，单类）：在 `bc-iam/application` 新增最小 Port：`UserEntityFieldExtractPort`（仅暴露外部 BC 实际使用的 `userIdOf(Object)` + `springUserEntityWithNameObject(Object)`，用于后续收敛 `bc-course/bc-evaluation` 对 `UserConverter` 的直接依赖；最小回归通过；落地：`e5b37188`）。
- ✅ 补充进展（2026-02-19，保持行为不变，Convertor 收尾前置，单类）：在 `bc-iam/infrastructure` 新增 Port Adapter：`UserEntityFieldExtractPortImpl`（内部直接委托 `UserConverter.userIdOf/springUserEntityWithNameObject`，保持异常/空值/调用顺序不变；本刀不改调用侧；最小回归通过；落地：`7e682804`）。
- ✅ 补充进展（2026-02-19，保持行为不变，Convertor 归位，单类搬运）：将 `UserConverter` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.convertor.user` 与类内容不变；口径更新：`eva-infra-shared` Java 余量由 `1` 变更为 `0`；最小回归通过；落地：`8d5e580c`）。
- ✅ 补充进展（2026-02-19，保持行为不变，编译闭合前置，单 pom）：在 `bc-evaluation/infrastructure/pom.xml` 增加 `bc-iam` Maven 依赖，用于承接后续 `bc-evaluation/infrastructure` 调用侧对 `UserEntityFieldExtractPort` 的编译期引用（最小回归通过；落地：`4b239adf`）。
- ✅ 补充进展（2026-02-19，保持行为不变，依赖收敛，单 pom）：收敛 `bc-evaluation/infrastructure/pom.xml`：移除对 `eva-infra-shared` 的 Maven 编译期依赖（仅收敛编译边界；最小回归通过；落地：`61d305e0`）。
- ✅ 补充进展（2026-02-19，保持行为不变，依赖收敛，单 pom）：收敛 `bc-iam/infrastructure/pom.xml`：移除对 `eva-infra-shared` 的 Maven 编译期依赖（仅收敛编译边界；最小回归通过；落地：`2c405c32`）。
- ✅ 补充进展（2026-02-19，保持行为不变，依赖收敛，单 pom）：收敛 root `pom.xml`：从 reactor 移除 `<module>eva-infra-shared</module>`（仅改变聚合构建边界；最小回归通过；落地：`3676b534`）。
- ✅ 补充进展（2026-02-19，保持行为不变，依赖收敛，单文件）：删除 `eva-infra-shared/pom.xml`（前置：全仓库 `**/pom.xml` 不再出现对该模块的依赖引用；最小回归通过；落地：`8aad22a2`）。
- ✅ 补充进展（2026-02-19，保持行为不变，引用面收敛，单类）：收敛评教读侧调用点：`EvaTaskQueryRepository` 改为依赖 `UserEntityFieldExtractPort`（不再直接注入 `UserConverter`；Port Adapter 内部仍委托 `UserConverter.userIdOf`；最小回归通过；落地：`5128a78d`）。
- ✅ 补充进展（2026-02-19，保持行为不变，引用面收敛，单类）：收敛评教读侧调用点：`EvaRecordQueryRepository` 改为依赖 `UserEntityFieldExtractPort`（不再直接注入 `UserConverter`；Port Adapter 内部仍委托 `UserConverter.userIdOf`；最小回归通过；落地：`ba84a0ca`）。
- ✅ 补充进展（2026-02-19，保持行为不变，编译闭合前置，单 pom）：在 `bc-course/infrastructure/pom.xml` 增加 `bc-iam` Maven 依赖，用于承接后续 `bc-course/infrastructure` 调用侧对 `UserEntityFieldExtractPort` 的编译期引用（最小回归通过；落地：`3ca758c2`）。
- ✅ 补充进展（2026-02-19，保持行为不变，引用面收敛，单类）：收敛课程读侧调用点：`CourseQueryRepository` 改为依赖 `UserEntityFieldExtractPort`（不再直接注入 `UserConverter`；Port Adapter 内部仍委托 `UserConverter.springUserEntityWithNameObject`；最小回归通过；落地：`9eabba63`）。
- 补充进展（2026-02-06，保持行为不变，支撑类归位）：将 `CourInfTimeOverlapQuery` 从 `eva-infra-shared` 进一步归位到 `bc-course/infrastructure`（保持 `package` 不变；Serena：引用面仅命中 `bc-course/infrastructure` 的 3 个适配器；最小回归通过；落地：`ea6c99e9`）。
- ✅ 补充进展（2026-02-19，保持行为不变，依赖收敛，单 pom）：收敛 `bc-course/infrastructure/pom.xml`：移除对 `eva-infra-shared` 的依赖（最小回归通过；落地：`a96900c5`）。
- ✅ 补充进展（2026-02-19，保持行为不变，编译闭合补强，单 pom）：为避免“增量构建未触发编译”掩盖依赖缺口，收敛 `bc-course/infrastructure/pom.xml`：显式补齐 `org.mapstruct:mapstruct` + `bc-evaluation-domain(provided)` + `bc-messaging-contract(provided)`（仅闭合编译边界；最小回归通过；落地：`6ee4e485`）。
- ✅ 补充进展（2026-02-19，保持行为不变，编译闭合纠偏，单 pom）：为避免“增量构建未触发重编译”掩盖依赖缺口，收敛 `bc-iam/infrastructure/pom.xml`：显式补齐 `org.mapstruct:mapstruct`、`org.springframework.ldap:spring-ldap-core`、`org.springframework.data:spring-data-ldap`、`bc-course-domain(provided)`（仅闭合编译边界；最小回归通过；落地：`03dc4f4e`）。
- ✅ 补充进展（2026-02-19，保持行为不变，`eva-base` 退场前置，单类搬运）：将 `GenericPattern` 从 `eva-base/eva-base-common` 下沉到 `shared-kernel`（保持 `package edu.cuit.common.enums` 与常量内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`4be17a62`）。
- ✅ 补充进展（2026-02-19，保持行为不变，`eva-base` 退场前置，单类搬运）：将 `LogModule` 从 `eva-base/eva-base-common` 下沉到 `shared-kernel`（保持 `package edu.cuit.common.enums` 与常量内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`77f44292`）。
- 补充进展（2026-02-12，保持行为不变，编译闭合前置，单 pom）：为后续将 `QueryUtils` 从 `eva-infra-shared` 归位到 `eva-infra-dal`，在 `eva-infra-dal/pom.xml` 显式增加对 `shared-kernel` 的 Maven 编译期依赖（最小回归通过；落地：`996b6990`）。
- 补充进展（2026-02-12，保持行为不变，编译闭合前置，单 pom）：为后续将 `EntityFactory` 从 `eva-infra-shared` 归位到 `eva-infra-dal` 做准备，在 `eva-infra-dal/pom.xml` 显式补齐 `hutool-all`、`cola-component-exception`、`mapstruct` 依赖（不改变业务语义/副作用顺序；最小回归通过；落地：`6546c548`）。
- ✅ 补充进展（2026-02-12，保持行为不变；单类闭环）：已将 `EntityFactory` 从 `eva-infra-shared` 搬运归位到 `eva-infra-dal`（保持 `package edu.cuit.infra.convertor` 与类内容不变；不引入新缓存/切面副作用；最小回归通过；落地：`eba15e92`）。
- ✅ 补充进展（2026-02-17，保持行为不变，编译闭合前置，单 pom）：为后续将 `EntityFactory` 从 `eva-infra-dal` 继续下沉到 `shared-kernel` 做准备，在 `shared-kernel/pom.xml` 补齐 `mapstruct(optional)` 依赖（仅用于源码编译闭合；最小回归通过；落地：`a0030694`）。
- ✅ 补充进展（2026-02-17，保持行为不变；单类闭环）：已将 `EntityFactory` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.convertor` 与类内容不变，仅改变 Maven 模块归属；口径更新：`eva-infra-dal` Java 余量由 `1` 变更为 `0`；最小回归通过；落地：`86754419`）。
- ✅ 补充进展（2026-02-17，保持行为不变，编译闭合前置，单 pom）：为后续下沉 LDAP DO/Repo（含 Spring LDAP ODM 注解）做准备，在 `shared-kernel/pom.xml` 补齐 `spring-ldap-core(optional)` 依赖（最小回归通过；落地：`473b48a7`）。
- ✅ 补充进展（2026-02-17，保持行为不变，编译闭合前置，单 pom）：为后续下沉 LDAP Repo `LdapGroupRepo`（依赖 `LdapRepository`）做准备，在 `shared-kernel/pom.xml` 补齐 `spring-data-ldap(optional)` 依赖（最小回归通过；落地：`9dad61a8`）。
- ✅ 补充进展（2026-02-17，保持行为不变，shared-kernel 下沉，单类闭环）：已将 LDAP Repo `LdapGroupRepo` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.ldap.repo` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过；落地：`7ff087ad`）。
- ✅ 补充进展（2026-02-17，保持行为不变，shared-kernel 下沉，单类闭环）：已将 LDAP 配置类 `EvaLdapProperties` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.property` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；口径更新：`eva-infra-shared` Java 余量由 `8` 变更为 `7`；落地：`666a1b6d`）。
- ✅ 补充进展（2026-02-17，保持行为不变，shared-kernel 下沉，单类闭环）：已将 LDAP 数据对象 `LdapGroupDO` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.ldap.dataobject` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；口径更新：`eva-infra-shared` Java 余量由 `7` 变更为 `6`；落地：`03ecd906`）。
- ✅ 补充进展（2026-02-17，保持行为不变，依赖收敛，单 pom）：收敛 `eva-infra-shared/pom.xml`：移除对 `eva-infra-dal` 的 Maven 编译期直依赖（最小回归通过；落地：`5975ab10`）。
- ✅ 补充进展（2026-02-17，保持行为不变，依赖收敛，单 pom）：收敛 `bc-template/infrastructure/pom.xml`：移除对 `eva-infra-shared` 的依赖，并改为显式依赖 `shared-kernel` 以维持编译闭合（Serena 证伪 `bc-template/**` 无 `eva-infra-shared` 代码引用；最小回归通过；落地：`b93f8719`）。
- ✅ 补充进展（2026-02-17，保持行为不变，依赖收敛，单 pom）：收敛 `bc-audit/infrastructure/pom.xml`：移除对 `eva-infra-shared` 的依赖，并改为显式依赖 `shared-kernel` 以维持编译闭合（Serena 证伪 `bc-audit/**` 无 `eva-infra-shared` 残留支撑类引用；最小回归通过；落地：`10b0af75`）。
- ✅ 补充进展（2026-02-17，保持行为不变，依赖收敛，单 pom）：收敛 `bc-messaging/pom.xml`：移除对 `eva-infra-shared` 的依赖（Serena 证伪 `bc-messaging/**` 无 `eva-infra-shared` 残留支撑类引用；最小回归通过；落地：`bdd9527d`）。
- ✅ 补充进展（2026-02-17，保持行为不变，编译闭合纠偏，单 pom）：为避免依赖传递导致的“隐式编译期依赖”，在 `bc-messaging/pom.xml` 显式补齐 `spring-boot-starter-websocket` 与 `org.mapstruct:mapstruct(${mapstruct.version})`（最小回归通过；落地：`bf0c3455`）。
- ✅ 补充进展（2026-02-17，保持行为不变，编译闭合纠偏，单 pom）：为避免依赖传递导致的“隐式编译期依赖”，在 `bc-audit/infrastructure/pom.xml` 显式补齐 `org.mapstruct:mapstruct(${mapstruct.version})`（最小回归通过；落地：`5e6721c9`）。
- ✅ 补充进展（2026-02-17，保持行为不变，reactor 退场，单 pom）：从 root `pom.xml` 的 `<modules>` 中移除 `<module>eva-infra-dal</module>`（最小回归通过；落地：`dfe4e5f3`）。
- ✅ 补充进展（2026-02-17，保持行为不变，目录退场前置，单文件）：删除 `eva-infra-dal/pom.xml`（最小回归通过；落地：`1c037aeb`）。
- ✅ 补充进展（2026-02-17，保持行为不变，引用面收敛，单类）：清理 `bc-course/infrastructure` 的 `CourseQueryRepository` 无用 import `MenuConvertor`（最小回归通过；落地：`4b9f5855`）。
- ✅ 补充进展（2026-02-17，保持行为不变，支撑类归位，单类）：将 `MenuConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package` 不变；最小回归通过；落地：`06690eee`）。
- ✅ 补充进展（2026-02-12，保持行为不变；单资源闭环）：已将 `SysUserMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`3dad6ef7`）。
- ✅ 补充进展（2026-02-12，保持行为不变，跨 BC 直连清零前置，单 pom）：在 `bc-evaluation/infrastructure/pom.xml` 显式增加对 `bc-course` 的 Maven 编译期依赖，用于让评教侧编译期引用课程域查询端口 `CourseIdByCourInfIdQueryPort`（运行期仍由组合根装配 `bc-course-infra` 的实现；最小回归通过；落地：`f2188237`）。
- ✅ 补充进展（2026-02-15，保持行为不变，编译闭合前置，单 pom）：为后续将课程域 `CourseMapper/SemesterMapper/CourInfMapper/SubjectMapper` 从 `eva-infra-dal` 逐类归位到 `bc-course-infra` 做准备，在 `bc-evaluation/infrastructure/pom.xml` 显式增加对 `bc-course-infra` 的 Maven 编译期依赖（最小回归通过；落地：`eb0bbbec`）。
- ✅ 补充进展（2026-02-15，保持行为不变，DAL 拆散试点，逐类归位）：将课程域 `CourseMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course-infra`（源码路径归位到 `bc-course/infrastructure`；保持 `package edu.cuit.infra.dal.database.mapper.course` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过；落地：`049be80f`）。
- ✅ 补充进展（2026-02-15，保持行为不变，DAL 拆散试点，逐类归位）：将课程域 `SemesterMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course-infra`（源码路径归位到 `bc-course/infrastructure`；保持 `package edu.cuit.infra.dal.database.mapper.course` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过；落地：`83c40eaa`）。
- ✅ 补充进展（2026-02-15，保持行为不变，DAL 拆散试点，逐类归位）：将课程域 `CourInfMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course-infra`（源码路径归位到 `bc-course/infrastructure`；保持 `package edu.cuit.infra.dal.database.mapper.course` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过；落地：`7837b0fb`）。
- ✅ 补充进展（2026-02-15，保持行为不变，DAL 拆散试点，逐类归位）：将课程域 `SubjectMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course-infra`（源码路径归位到 `bc-course/infrastructure`；保持 `package edu.cuit.infra.dal.database.mapper.course` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过；落地：`3cf31869`）。
- ✅ 补充进展（2026-02-15，保持行为不变，前置解耦，单类）：为后续归位评教侧 `FormTemplateMapper` 做准备，已将 `eva-infra-shared` 的 `CourseBizConvertor` 去 `FormTemplateMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射 `selectById(Serializable)`；保持调用次数/顺序不变；最小回归通过；落地：`cd014391`）。
- ✅ 补充进展（2026-02-15，保持行为不变，DAL 拆散试点，逐类归位）：将评教侧 `CourOneEvaTemplateMapper` 从 `eva-infra-dal` 搬运归位到 `bc-evaluation-infra`（源码路径归位到 `bc-evaluation/infrastructure`；保持 `package edu.cuit.infra.dal.database.mapper.eva` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过；落地：`d0eb9fa3`）。
- ✅ 补充进展（2026-02-15，保持行为不变，DAL 拆散试点，逐类归位）：将评教侧 `FormTemplateMapper` 从 `eva-infra-dal` 搬运归位到 `bc-evaluation-infra`（源码路径归位到 `bc-evaluation/infrastructure`；保持 `package edu.cuit.infra.dal.database.mapper.eva` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过；落地：`46b77dc4`）。
- ✅ 补充进展（2026-02-15，保持行为不变，SysUserMapper 归位前置，单类）：为后续将 IAM 侧 `SysUserMapper` 从 `eva-infra-dal` 归位到 `bc-iam/infrastructure` 做准备，已先移除 `CourseConvertor` 中对 `SysUserMapper` 的无用 import（未参与 `@Mapper(uses=...)` 且无任何符号引用；最小回归通过；落地：`1e0af235`）。
- ✅ 补充进展（2026-02-15，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-evaluation/infrastructure` 的 `EvaUpdateGatewayImpl` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)` + `getName()`；缓存失效 key 与副作用顺序不变；最小回归通过；落地：`364c63a0`）。
- ✅ 补充进展（2026-02-15，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-evaluation/infrastructure` 的 `DeleteEvaRecordRepositoryImpl` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)` + `getName()`；`LogUtils.logContent(...)` 文案与调用时机保持不变；最小回归通过；落地：`c0f94380`）。
- ✅ 补充进展（2026-02-15，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-evaluation/infrastructure` 的 `PostEvaTaskRepositoryImpl` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)` + `getId()`/`getName()`；任务发布流程、缓存 key 与副作用顺序不变；最小回归通过；落地：`89fbc439`）。
- ✅ 补充进展（2026-02-15，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-evaluation/infrastructure` 的 `SubmitEvaluationRepositoryImpl` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)` + `getName()`；缓存失效 key 与副作用顺序不变；最小回归通过；落地：`055db608`）。
- ✅ 补充进展（2026-02-15，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-evaluation/infrastructure` 的 `EvaTaskQueryRepository` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)` / `selectList(...)` + `getId()`/`getName()`；查询/遍历顺序、异常文案与副作用顺序不变；最小回归通过；落地：`7caaec02`）。
- ✅ 补充进展（2026-02-15，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-evaluation/infrastructure` 的 `EvaStatisticsQueryRepository` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)` / `selectList(...)` + `getId()`/`getName()`/`getDepartment()`；查询/遍历顺序、缓存 key 与副作用顺序不变；最小回归通过；落地：`28338204`）。
- ✅ 补充进展（2026-02-15，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-evaluation/infrastructure` 的 `EvaRecordQueryRepository` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)` / `selectList(...)`；查询/遍历顺序、异常文案与副作用顺序不变；最小回归通过；落地：`0e190e6c`）。
- ✅ 补充进展（2026-02-15，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-course/infrastructure` 的 `AssignEvaTeachersRepositoryImpl` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)`；日志/异常文案/副作用顺序不变；最小回归通过；落地：`712c4eb7`）。
- ✅ 补充进展（2026-02-16，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-course/infrastructure` 的 `DeleteCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)`；日志/异常文案/副作用顺序不变；最小回归通过；落地：`9dd8a7d1`）。
- ✅ 补充进展（2026-02-16，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-course/infrastructure` 的 `DeleteCoursesRepositoryImpl` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)`；日志/异常文案/副作用顺序不变；最小回归通过；落地：`b193156d`）。
- ✅ 补充进展（2026-02-16，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-course/infrastructure` 的 `DeleteSelfCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectOne(Wrapper)`；异常文案/日志文案/副作用顺序不变；最小回归通过；落地：`71060e69`）。
- ✅ 补充进展（2026-02-16，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-course/infrastructure` 的 `UpdateCourseInfoRepositoryImpl` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)`；异常文案/缓存失效 key 与副作用顺序不变；最小回归通过；落地：`d5415b0a`）。
- ✅ 补充进展（2026-02-16，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-course/infrastructure` 的 `UpdateSelfCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectOne(Wrapper)` / `selectById(Serializable)`；异常文案、缓存/日志与副作用顺序完全不变；最小回归通过；落地：`17c4bd19`）。
- ✅ 补充进展（2026-02-16，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-course/infrastructure` 的 `UpdateSingleCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)`；异常文案、缓存/日志与副作用顺序完全不变；最小回归通过；落地：`15135886`）。
- ✅ 补充进展（2026-02-16，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-course/infrastructure` 的 `CourseImportExce` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectOne(Wrapper)`；异常文案、缓存/日志与副作用顺序完全不变；最小回归通过；落地：`e3cf8426`）。
- ✅ 补充进展（2026-02-16，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-course/infrastructure` 的 `CourseRecommendExce` 去 `SysUserMapper` 编译期依赖（改为按 `beanName` 注入 `Object` + 反射调用 `selectOne(Wrapper)` / `selectById(Serializable)` / `selectList(Wrapper)`；异常文案、缓存/日志与副作用顺序完全不变；最小回归通过；落地：`cccf259b`）。
- ✅ 补充进展（2026-02-16，保持行为不变，SysUserMapper 归位前置，单类）：为后续归位 IAM 侧 `SysUserMapper` 做准备，已将 `bc-course/infrastructure` 的 `CourseQueryRepository` 去 `SysUserMapper` 编译期依赖（改为 `@Qualifier("sysUserMapper") Object` + 反射调用 `selectOne(Wrapper)` / `selectList(Wrapper)` / `selectById(Serializable)`；异常文案与副作用顺序完全不变；最小回归通过；落地：`ec1da722`）。
- ✅ 补充进展（2026-02-16，保持行为不变，dal 拆散试点，单类）：已将 `SysUserMapper.java` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package` 与接口签名不变，并同步删除旧位置同名类，避免 classpath 重复；最小回归通过；落地：`ff591e46`）。
- ✅ 补充进展（2026-02-16，保持行为不变，shared-kernel 下沉，单类）：已将 `SysMenuDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.user` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`a9141bfe`）。
- ✅ 补充进展（2026-02-16，保持行为不变，shared-kernel 下沉，单类）：已将 `SysRoleDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.user` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`c5ba98b1`）。
- ✅ 补充进展（2026-02-16，保持行为不变，shared-kernel 下沉，单类）：已将 `SysUserDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.user` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`31e157cd`）。
- ✅ 补充进展（2026-02-17，保持行为不变，shared-kernel 下沉，单类）：已将 `SubjectDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`1a6ff62c`）。
- ✅ 补充进展（2026-02-17，保持行为不变，shared-kernel 下沉，单类）：已将 `CourOneEvaTemplateDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.eva` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`e48c63b4`）。
- ✅ 补充进展（2026-02-17，保持行为不变，shared-kernel 下沉，单类）：已将 `FormTemplateDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.eva` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`6b66340b`）。
- ✅ 补充进展（2026-02-17，保持行为不变，shared-kernel 下沉，单类）：已将 `FormRecordDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.eva` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`83b44804`）。
- ✅ 补充进展（2026-02-17，保持行为不变，shared-kernel 下沉，单类）：已将 `EvaTaskDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.eva` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`49fa7eef`）。
- ✅ 补充进展（2026-02-17，保持行为不变，shared-kernel 下沉，单类）：已将 `CourseTypeDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`6c66a6dc`）。
- ✅ 补充进展（2026-02-16，保持行为不变，shared-kernel 下沉，单类）：已将 `CourInfDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`542e0231`）。
- ✅ 补充进展（2026-02-16，保持行为不变，shared-kernel 下沉，单类）：已将 `CourseDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`d822340e`）。
- ✅ 补充进展（2026-02-16，保持行为不变，shared-kernel 下沉，单类）：已将 `SemesterDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`e0680e8a`）。
  - 🎯 下一刀建议（更新至 2026-02-17；每次只改 1 个文件；保持行为不变）：`eva-infra-dal` Java 仅剩 `EntityFactory`（`edu.cuit.infra.convertor.EntityFactory`），Serena 证据化引用面跨多个 BC 与 `eva-infra-shared`，不满足“单 BC/单模块归位”的条件；建议将其下沉到 `shared-kernel` 作为共享（保持 `package` 不变；若 `shared-kernel/pom.xml` 缺依赖则先做“单 pom 前置”）。
- ✅ 补充进展（2026-02-12，保持行为不变，方案 1：跨 BC 直连清零，单类）：收敛评教侧对课程域 `CourInfMapper.selectById` 的跨 BC 直连：`EvaTemplateQueryRepository.getTaskTemplate(...)` 改为调用课程域查询端口 `CourseIdByCourInfIdQueryPort.findCourseIdByCourInfId(...)`（异常文案与分支顺序不变；最小回归通过；落地：`67755034`）。
- ✅ 补充进展（2026-02-12，保持行为不变；跨 BC 直连清零前置，单类）：在 `bc-course/application` 新增最小查询端口 `CourseIdByCourInfIdQueryPort`（用于后续收敛其它 BC 对 `CourInfMapper` 的直连为通过端口查询 `cour_inf.id -> course_id`；最小回归通过；落地：`777ec8a9`）。
- ✅ 补充进展（2026-02-12，保持行为不变；跨 BC 直连清零前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourseIdByCourInfIdQueryPortImpl`（内部仅 `CourInfMapper.selectById`，不改变业务语义/副作用顺序；最小回归通过；落地：`f0c0f020`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零，单类）：收敛评教写侧对课程域 `CourInfMapper.selectById` 的跨 BC 直连：`SubmitEvaluationRepositoryImpl.loadContext/saveEvaluation` 改为调用课程域查询端口 `CourseIdByCourInfIdQueryPort.findCourseIdByCourInfId(...)`（异常文案与查询顺序不变；最小回归通过；落地：`176ed9d4`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零前置，单类）：在 `bc-course/application` 新增查询端口 `CourInfTimeSlotQueryPort`，用于以端口方式查询 cour_inf 时间片（id/courseId/week/day/start/end）（后续用于替换评教写侧 `CourInfMapper.selectById/selectList` 的时间冲突判定；最小回归通过；落地：`ef2ab821`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourInfTimeSlotQueryPortImpl`，用于承接 `CourInfTimeSlotQueryPort`（内部仅 `CourInfMapper.selectById/selectList`；最小回归通过；落地：`343e3ecf`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零，单类）：收敛评教写侧 `PostEvaTaskRepositoryImpl.create(...)` 对课程域 `CourInfMapper.selectById/selectList` 的跨 BC 直连：改为调用课程域查询端口 `CourInfTimeSlotQueryPort.findByCourInfId/findByCourseIds/findByCourInfIds`（时间冲突判定逻辑/异常文案/查询次数与顺序不变；最小回归通过；落地：`50943f8c`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零，单类）：收敛评教侧撤回任务对课程域 `CourInfMapper.selectById` 的跨 BC 直连：`EvaUpdateGatewayImpl.cancelEvaTaskById(...)` 改为调用课程域查询端口 `CourseIdByCourInfIdQueryPort.findCourseIdByCourInfId(...)`（缓存失效 key 与副作用顺序不变；最小回归通过；落地：`6592d4ba`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零，单类）：收敛评教写侧删除评教记录对课程域 `CourInfMapper.selectById/selectList` 的跨 BC 直连：`DeleteEvaRecordRepositoryImpl.delete(...)` 改为调用课程域查询端口 `CourInfTimeSlotQueryPort.findByCourInfId(...)` + `CourInfIdsByCourseIdsQueryPort.findCourInfIdsByCourseIds(...)`（异常文案/分支/查询次数与顺序不变；最小回归通过；落地：`d2472370`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零前置，单类）：在 `bc-course/application` 新增查询端口 `CourInfObjectDirectQueryPort`，用于以端口方式直查 cour_inf 对象（返回 `Object`，避免端口暴露 DAL DataObject；后续用于替换评教读侧对 `CourInfMapper` 的直连；最小回归通过；落地：`22fba0be`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourInfObjectDirectQueryPortImpl`，用于承接 `CourInfObjectDirectQueryPort`（内部仅 `CourInfMapper.selectById/selectList`；最小回归通过；落地：`74cda7d2`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零，单类）：收敛评教读侧任务查询对课程域 `CourInfMapper.selectById/selectList(in course_id)` 的跨 BC 直连：`EvaTaskQueryRepository` 改为调用课程域查询端口 `CourInfObjectDirectQueryPort.findById/findByCourseIds(...)`（异常文案/分支/查询次数与顺序不变；最小回归通过；落地：`aec56fc9`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零，单类）：收敛评教读侧记录查询对课程域 `CourInfMapper.selectList(...)` 的跨 BC 直连：`EvaRecordQueryRepository` 改为调用课程域查询端口 `CourInfObjectDirectQueryPort.findByIds/findByCourseIds(...)`（异常文案/分支/查询次数与顺序不变；最小回归通过；落地：`13889255`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零前置，单类）：在 `bc-course/application` 新增查询端口 `CourInfIdsByCourseIdsQueryPort`，用于以端口方式查询 `course_id -> cour_inf.idS`（替换评教侧 `CourInfMapper.selectList(in course_id)`；最小回归通过；落地：`fef8b5aa`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourInfIdsByCourseIdsQueryPortImpl`，用于承接 `CourInfIdsByCourseIdsQueryPort`（内部仅 `CourInfMapper.selectList(in course_id)`；最小回归通过；落地：`8df151b6`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零，单类）：收敛评教侧对课程域 `CourInfMapper.selectList(in course_id)` 的跨 BC 直连：`EvaTemplateQueryRepository.getEvaTaskIdS(...)` 改为调用课程域查询端口 `CourInfIdsByCourseIdsQueryPort.findCourInfIdsByCourseIds(...)`（查询次数与顺序不变；最小回归通过；落地：`1bac7ffc`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零，单类）：收敛评教侧对课程域 `CourInfMapper.selectList(in course_id)` 的跨 BC 直连：`EvaStatisticsQueryRepository.getEvaTaskIdS(...)`/统计相关查询改为调用课程域查询端口 `CourInfIdsByCourseIdsQueryPort.findCourInfIdsByCourseIds(...)`（查询次数与顺序不变；最小回归通过；落地：`28dd1e6b`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 1：跨 BC 直连清零收尾，单类）：收敛 `bc-template/**` 对课程域 `CourInfMapper.selectList(eq course_id)` 的跨 BC 直连：`CourseTemplateLockQueryPortImpl` 改为调用课程域查询端口 `CourInfIdsByCourseIdsQueryPort.findCourInfIdsByCourseIds(...)`（查询次数与顺序不变；最小回归通过；落地：`f81b4661`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 B：依赖前置，单 pom）：为后续把共享 Excel/POI 工具从 `eva-infra-shared` 下沉到 `shared-kernel` 做准备，在 `bc-course/infrastructure/pom.xml` 显式增加对 `shared-kernel` 的 Maven 编译期依赖（最小回归通过；落地：`80a1aec7`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 B：依赖前置，单 pom）：为后续把 `ExcelUtils` 下沉到 `shared-kernel` 做编译闭合前置，在 `shared-kernel/pom.xml` 补齐 `cola-component-exception`、`hutool-all`、`poi/poi-ooxml` 依赖（最小回归通过；落地：`b4641433`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 B：共享工具下沉，单类）：将共享 Excel/POI 工具 `ExcelUtils` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package` 与类内容不变；最小回归通过；落地：`31615f43`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 B：依赖前置，单 pom）：为后续将 `*CacheConstants` 等含 Spring 注解的共享常量类下沉到 `shared-kernel` 做准备，在 `shared-kernel/pom.xml` 预置 `spring-context(optional)` 编译期依赖（最小回归通过；落地：`21a7176b`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 B：共享常量下沉，单类）：将课程缓存键常量 `CourseCacheConstants` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package` 与 bean 名称 `courseCacheConstants` 不变；最小回归通过；落地：`16a07a6b`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 B：共享常量下沉，单类）：将评教缓存键常量 `EvaCacheConstants` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package` 与 bean 名称 `evaCacheConstants` 不变；最小回归通过；落地：`f1fac0f6`）。
- ✅ 补充进展（2026-02-12，保持行为不变；方案 B：共享常量下沉，单类）：将用户/权限缓存键常量 `UserCacheConstants` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package` 与 bean 名称 `userCacheConstants` 不变；最小回归通过；落地：`5f1447e5`）。
- ✅ 补充进展（2026-02-13，保持行为不变；方案 B：`CourseFormat` 下沉前置，单 pom）：`shared-kernel/pom.xml` 已补齐 `zym-spring-boot-starter-jdbc` 与 `jackson-databind`，用于承接 `CourseFormat` 的 `QueryWrapper/ObjectMapper` 编译期依赖（最小回归通过；落地：`322bb315`）。
- ✅ 证据化结论（2026-02-13，保持行为不变）：Serena 显示 `CourseFormat.selectCourOneEvaTemplateDO(...)` 仍被 `ICourseDetailServiceImpl` 调用；且 `CourseFormat` 已完成“依赖解耦前置”（`@Qualifier("courOneEvaTemplateMapper") Object` + 反射 `selectOne/getFormTemplate`，移除对 `CourOneEvaTemplateMapper/DO` 的编译期强依赖；落地：`8b4f69e2`）。
- ✅ 补充进展（2026-02-13，保持行为不变；方案 B：单类搬运）：已将 `CourseFormat` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.gateway.impl.course.operate` 与类内容不变；最小回归通过；落地：`dff4e751`）。
- ✅ 证据化补充（2026-02-13，保持行为不变）：`eva-infra-shared` 剩余类中，`CourseBizConvertor/CourseConvertor/UserConverter` 在 shared 内仅自引用；LDAP 子簇（`LdapGroupRepo/LdapConstants/EvaLdapUtils`）仍互相依赖（其中配置类 `EvaLdapProperties` 已下沉到 `shared-kernel`：`666a1b6d`）。
- ✅ 证据化补充（2026-02-17，保持行为不变）：LDAP 子簇已完成 `LdapGroupRepo` 下沉到 `shared-kernel`（落地：`7ff087ad`）；为满足“单类一刀”且避免 Maven 循环依赖，已先将 `LdapConstants` 解耦为不再编译期引用 `EvaLdapUtils`（仍会触发 `EvaLdapUtils` 类初始化以保持副作用顺序不变；落地：`45fa9651`），并已将 `LdapConstants` 下沉到 `shared-kernel`（落地：`3dc2e8ff`）；随后已将 `EvaLdapUtils` 从 `eva-infra-shared` 下沉到 `shared-kernel`（落地：`05cc3039`）。LDAP 子簇在 `eva-infra-shared` 侧已清零（保持包名不变）。
- ✅ 补充进展（2026-02-13，保持行为不变；Serena 证据化 + 风险评估）：`CourseConvertor` 引用面虽跨 `bc-course/**` 与 `bc-evaluation/**`，但 import 仍依赖 `eva-infra-dal` 内 `EntityFactory/DO/Mapper`；`shared-kernel` 当前不具备完整编译闭合，且若直接补 `shared-kernel -> eva-infra-dal` 将与现有 `eva-infra-dal -> shared-kernel` 形成循环依赖，因此本刀判定风险超阈值。
- ✅ 补充进展（2026-02-13，保持行为不变；降级执行，单资源闭环）：已将 `CourInfMapper.xml` 从 `eva-infra-dal` 归位到 `bc-course/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`4eb6681c`）。
- ✅ 补充进展（2026-02-13，保持行为不变；单资源闭环）：已将 `SubjectMapper.xml` 从 `eva-infra-dal` 归位到 `bc-course/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`92374d53`）。
- ✅ 补充进展（2026-02-14，保持行为不变；单资源闭环）：已将 `CourseMapper.xml` 从 `eva-infra-dal` 归位到 `bc-course/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`cccf39ec`）。
- ✅ 补充进展（2026-02-14，保持行为不变；单资源闭环）：已将 `SemesterMapper.xml` 从 `eva-infra-dal` 归位到 `bc-course/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`29d9b208`）。
- ✅ 补充进展（2026-02-14，保持行为不变；单资源闭环）：已将 `CourOneEvaTemplateMapper.xml` 从 `eva-infra-dal` 归位到 `bc-evaluation/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`7aba53e1`）。
- ✅ 补充进展（2026-02-14，保持行为不变；单资源闭环）：已将 `EvaTemplateMapper.xml` 从 `eva-infra-dal` 归位到 `bc-evaluation/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`fa3d9181`）。
- ✅ 补充进展（2026-02-14，保持行为不变；单资源闭环）：已将 `FormRecordMapper.xml` 从 `eva-infra-dal` 归位到 `bc-evaluation/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`e9e32fdf`）。
  - ✅ 补充进展（2026-02-13，保持行为不变；跨 BC 直连清零，单类闭环）：`AddNotExistCoursesDetailsRepositoryImpl` 已清零对评教侧 `FormTemplateMapper` 的编译期直连（`Object` + `@Qualifier` + 反射 `selectOne`，保持 MyBatis 语义不变）；最小回归通过；落地：`e7e444a1`。
- ✅ 补充进展（2026-02-13，保持行为不变；跨 BC 直连清零，单类闭环）：`DeleteSelfCourseRepositoryImpl` 已清零对评教侧 `EvaTaskMapper/FormRecordMapper` 的编译期直连（`Object` + `@Qualifier` + 反射 `selectList/delete`，保持 MyBatis 语义不变）；最小回归通过；落地：`bbc32ae1`。
- ✅ 补充进展（2026-02-13，保持行为不变；跨 BC 直连清零，单类闭环）：`UpdateSelfCourseRepositoryImpl` 已清零对评教侧 `EvaTaskMapper` 的编译期直连（`Object` + `@Qualifier` + 反射 `selectList/delete`，保持 MyBatis 语义不变）；最小回归通过；落地：`db6d6165`。
- ✅ 补充进展（2026-02-13，保持行为不变；跨 BC 直连清零，单类闭环）：`CourseImportExce` 已清零对评教侧 `EvaTaskMapper/FormRecordMapper/CourOneEvaTemplateMapper/FormTemplateMapper` 的编译期直连（`Object` + `@Qualifier` + 反射 `selectList/delete/deleteBatchIds/selectOne/selectById`，保持 MyBatis 语义不变）；最小回归通过；落地：`0dfe5cd6`。
- ✅ 补充进展（2026-02-13，保持行为不变；跨 BC 直连清零，单类闭环）：`CourseQueryRepository` 已清零对评教侧 `CourOneEvaTemplateMapper/EvaTaskMapper/FormRecordMapper/FormTemplateMapper` 的编译期直连（`Object` + `@Qualifier` + 反射 `selectOne/selectList/selectCount`，保持 MyBatis 语义不变）；最小回归通过；落地：`be3389a2`。
- ✅ 本会话收口结论（2026-02-13，保持行为不变）：`bc-course/**` 对评教侧 `infra.dal.database.mapper.eva.*` 编译期 import 已清零。
- ✅ 补充进展（2026-02-13，保持行为不变，DAL 拆散试点，逐类归位）：将评教侧 `EvaTaskMapper` 从 `eva-infra-dal` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.eva` 不变；最小回归通过；落地：`b7049e4c`）。
- ✅ 补充进展（2026-02-14，保持行为不变；多类小簇闭环）：为承接 `EvaTaskMapper/FormRecordMapper` 已归位到 `bc-evaluation/infrastructure` 的事实，课程侧与模板侧对评教 Mapper 的编译期直连改为按 `beanName` 注入 `Object` + 反射调用（保持 MyBatis 调用语义/异常文案/副作用顺序不变）；最小回归通过；落地：`8bf5c164`。
- ✅ 会话收口快照（2026-02-13，保持行为不变）：当前仍保留 `eva-infra-dal/eva-infra-shared/eva-base` 三个 reactor 模块；余量为 `eva-infra-dal`=`23` Java + `0` XML、`eva-infra-shared`=`9` Java、`eva-base-common`=`2` Java。
- ✅ 会话收口结论（2026-02-13，保持行为不变）：要达成“BC 相互独立、仅通过 contract/port 调用”，当前关键阻塞是跨 BC DAL 直连与共享依赖边界：`bc-evaluation/**` 对课程 mapper import 尚有 0 处（已按端口替换完成）；`bc-template/**` 对评教 mapper import 尚有 0 处（已清零编译期 import）；`bc-course/**` 对评教 mapper import 尚有 0 处（已清零编译期 import）；多 BC 仍显式依赖 `eva-infra-shared`。
- 补充进展（2026-02-12，保持行为不变，支撑类归位）：将 `QueryUtils` 从 `eva-infra-shared` 搬运归位到 `eva-infra-dal`（保持 `package edu.cuit.infra.util` 不变；类内容不变；最小回归通过；落地：`e653338f`）。
- ✅ 补充进展（2026-02-16，保持行为不变，shared-kernel 下沉，单类）：将 `QueryUtils` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`f1638d20`）。
- 补充进展（2026-02-12，保持行为不变，支撑类归位）：将 `PaginationConverter` 从 `eva-infra-shared` 搬运归位到 `eva-infra-dal`（保持 `package edu.cuit.infra.convertor` 不变；类内容不变；最小回归通过；落地：`d2ca2d80`）。
- ✅ 补充进展（2026-02-16，保持行为不变，shared-kernel 下沉，单类）：将 `PaginationConverter` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`0427f1d4`）。
- 补充进展（2026-02-12，保持行为不变，支撑类归位）：将 `PaginationBizConvertor` 从 `eva-infra-shared` 搬运归位到 `eva-infra-dal`（保持 `package edu.cuit.app.convertor` 不变；类内容不变；Serena：引用面命中 `bc-audit/bc-course/bc-iam/start`；最小回归通过；落地：`2b950a06`）。
- ✅ 补充进展（2026-02-16，保持行为不变，shared-kernel 下沉，单类）：将 `PaginationBizConvertor` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`67a61098`）。
- 补充进展（2026-02-06，保持行为不变，依赖收敛，单 pom）：收敛 `bc-audit/infrastructure/pom.xml`：移除对 `eva-infra-dal` 的冗余 Maven 直依赖（前置：`eva-infra-shared/pom.xml` 已显式依赖 `eva-infra-dal`；最小回归通过；落地：`91fd39a9`）。
- 补充进展（2026-02-06，保持行为不变，依赖收敛，单 pom）：收敛 `bc-evaluation/infrastructure/pom.xml`：移除对 `eva-infra-dal` 的冗余 Maven 直依赖（前置：`eva-infra-shared/pom.xml` 已显式依赖 `eva-infra-dal`；最小回归通过；落地：`a0b5a359`）。
- 补充进展（2026-02-06，保持行为不变，依赖收敛，单 pom）：收敛 `bc-iam/infrastructure/pom.xml`：移除对 `eva-infra-dal` 的冗余 Maven 直依赖（前置：`eva-infra-shared/pom.xml` 已显式依赖 `eva-infra-dal`；最小回归通过；落地：`d7caa268`）。
- 补充进展（2026-02-06，保持行为不变，依赖收敛，单 pom）：收敛 `bc-messaging/pom.xml`：移除对 `eva-infra-dal` 的冗余 Maven 直依赖（前置：`eva-infra-shared/pom.xml` 已显式依赖 `eva-infra-dal`；最小回归通过；落地：`ab25db93`）。
- 补充进展（2026-02-06，保持行为不变，依赖收敛，单 pom）：收敛 `bc-messaging/pom.xml`：移除冗余 `spring-context` Maven 直依赖（前置：已依赖 `spring-boot-starter-web`，其传递承接 `spring-context`；最小回归通过；落地：`5da009c9`）。
- 补充进展（2026-02-06，保持行为不变，依赖收敛，单 pom）：收敛 `bc-template/infrastructure/pom.xml`：将对 `eva-infra-dal` 的 Maven 编译期直依赖替换为依赖 `eva-infra-shared`（其显式依赖 `eva-infra-dal`，因此编译/运行期 classpath 与行为不变；最小回归通过；落地：`204aef24`）。
- 补充进展（2026-02-06，保持行为不变，依赖收敛，单 pom）：收敛 `bc-template/application/pom.xml`：在 Serena 证伪 `bc-template/application/src/main/java` 无 Lombok 引用后，移除冗余 `lombok(provided)` 依赖（最小回归通过；落地：`e91844c2`）。
- 补充进展（2026-02-06，保持行为不变，依赖收敛，单 pom）：收敛 `bc-course/application/pom.xml`：在 Serena 证伪 `bc-course/application/src` 无 Lombok 引用后，移除冗余 `lombok(provided)` 依赖（最小回归通过；落地：`c38e30f0`）。
- 补充进展（2026-02-11，保持行为不变，编译闭合前置，单 pom）：在 `bc-evaluation/infrastructure/pom.xml` 补齐对 `bc-messaging` 的 Maven 编译期依赖，用于承接后续将 `WebsocketManager` 从 `eva-infra-shared` 归位到 `bc-messaging`（最小回归通过；落地：`4dd1b34f`）。
- 补充进展（2026-02-11，保持行为不变，支撑类归位，逐类归位）：将 `WebsocketManager` 从 `eva-infra-shared` 搬运归位到 `bc-messaging`（保持 `package` 不变；最小回归通过；落地：`bf78d276`）。
- 补充进展（2026-02-12，保持行为不变，支撑类归位，逐类归位）：将 `EvaConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package` 不变；Serena：引用面仅命中 `bc-evaluation/infrastructure`；最小回归通过；落地：`4df4e9b8`）。
- 补充进展（2026-02-11，保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `EvaTaskMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-evaluation/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`ad2e7d25`）。
- 补充进展（2026-02-11，保持行为不变，DAL 拆散试点，逐类归位）：将 `SysRoleMapper` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.user` 不变；Serena：引用面仅命中 `bc-iam/infrastructure`；最小回归通过；落地：`60b87404`）。
- 补充进展（2026-02-11，保持行为不变，DAL 拆散试点，逐类归位）：将 `SysUserRoleMapper` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.user` 不变；Serena：引用面仅命中 `bc-iam/infrastructure`；最小回归通过；落地：`1f93141c`）。
- 补充进展（2026-02-11，保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `SysUserRoleMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`0cd5da04`）。
- 补充进展（2026-02-11，保持行为不变，依赖收敛前置，逐类推进）：在 `bc-iam-contract` 新增端口 `UserEntityObjectByIdDirectQueryPort`，用于后续把其它 BC 的“跨 BC 直连 IAM 表（sys_user/sys_user_role/sys_role）”改造为通过 IAM 对外端口调用（约束：实现方不得引入新的缓存/切面副作用；最小回归通过；落地：`51be7465`）。
- 补充进展（2026-02-11，保持行为不变，依赖收敛前置，逐类推进）：在 `bc-iam-infra` 新增端口适配器 `UserEntityObjectByIdDirectQueryPortImpl`，内部原样复刻“直连 sys_user/sys_user_role/sys_role”的查询与装配逻辑（约束：不引入新的缓存/切面副作用；最小回归通过；落地：`2c9fb7e7`）。
- 补充进展（2026-02-11，保持行为不变，依赖收敛，逐类推进）：将 `bc-audit` 的 `LogGatewayImpl` 从“跨 BC 直连 IAM 表（sys_user/sys_user_role/sys_role）”收敛为依赖 `bc-iam-contract` 端口 `UserEntityObjectByIdDirectQueryPort`（内部仍保持原 SQL 与装配逻辑，不引入新的缓存/切面副作用；最小回归通过；落地：`fdd7078e`）。
- 补充进展（2026-02-11，保持行为不变，依赖收敛，逐类推进）：将 `bc-course` 的 `CourseQueryRepository.toUserEntity` 从“跨 BC 直连 IAM role 表（sys_user_role/sys_role）”收敛为依赖 `bc-iam-contract` 端口 `UserEntityObjectByIdDirectQueryPort`（仍保留对 `sys_user` 的直接查询；异常文案保持不变；不引入新的缓存/切面副作用；最小回归通过；落地：`c22bc75d`）。
- 补充进展（2026-02-11，保持行为不变，依赖收敛，逐类推进）：将 `bc-evaluation` 的 `EvaTaskQueryRepository.toUserEntity` 从“跨 BC 直连 IAM role 表（sys_user_role/sys_role）”收敛为依赖 `bc-iam-contract` 端口 `UserEntityObjectByIdDirectQueryPort`（异常文案保持不变；不引入新的缓存/切面副作用；最小回归通过；落地：`6c5d6bce`）。
- 补充进展（2026-02-06，保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogModuleMapper` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 `package` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-audit/infrastructure`；最小回归通过；落地：`c901e3a6`）。
- 补充进展（2026-02-06，保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogMapper` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 `package` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-audit/infrastructure`；最小回归通过；落地：`5de32a6c`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogDO` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 `package` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-audit/infrastructure`；最小回归通过；落地：`7de33487`）。
- 补充进展（2026-02-06，保持行为不变，支撑类归位，逐类归位）：将 `LogConverter` 从 `eva-infra-shared` 搬运归位到 `bc-audit/infrastructure`（保持 `package` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-audit/infrastructure`；最小回归通过；落地：`02c85909`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogModuleDO` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 `package` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-audit/infrastructure`；最小回归通过；落地：`960d2bbb`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，逐类归位）：将 `SysRoleMenuMapper` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.user` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-iam/infrastructure` 的 `MenuWritePortImpl/RoleWritePortImpl/UserMenuCacheInvalidationPortImpl`；最小回归通过；落地：`f98ee5c2`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，逐类归位）：将 `SysRoleMenuDO` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.database.dataobject.user` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-iam/infrastructure` 的 `MenuWritePortImpl/RoleWritePortImpl/UserMenuCacheInvalidationPortImpl/RoleQueryGatewayImpl`；最小回归通过；落地：`49fcbda7`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `SysRoleMenuMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`db81d674`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，逐类归位）：将 `CourseTypeCourseMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.course` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-course/infrastructure`；最小回归通过；落地：`2e1cd36e`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `CourseTypeCourseMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`45bc05d6`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，逐类归位）：将 `CourseTypeCourseDO` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-course/infrastructure`；最小回归通过；落地：`8f410b14`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，逐类归位）：将 `CourseTypeMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.course` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-course/infrastructure`；最小回归通过；落地：`241b75de`）。
- ✅ 证据化结论（2026-02-11，保持行为不变，DAL 拆散约束）：`SysRoleMapper` 当前引用面仅命中 `bc-iam/infrastructure`（Serena 证伪），已满足“仅单 BC 引用”的归位前提；因此将其从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（最小回归通过；落地：`60b87404`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 MyBatis `namespace/resultMap type` 与 SQL 不变，且资源路径仍为 `mapper/**`；最小回归通过；落地：`b6f05784`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogModuleMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 MyBatis `namespace/resultMap type` 与 SQL 不变，且资源路径仍为 `mapper/**`；最小回归通过；落地：`f6c7897c`）。
- 补充进展（2026-02-07，保持行为不变，支撑类归位，逐类归位）：将 `MsgConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-messaging`（保持 `package edu.cuit.infra.convertor` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-messaging`；最小回归通过；落地：`312756c7`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，逐类归位）：将 `MsgTipMapper` 从 `eva-infra-dal` 搬运归位到 `bc-messaging`（保持 `package edu.cuit.infra.dal.database.mapper` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-messaging`；最小回归通过；落地：`4af9f9fc`）。
- 补充进展（2026-02-07，保持行为不变，DAL 拆散试点，逐类归位）：将 `MsgTipDO` 从 `eva-infra-dal` 搬运归位到 `bc-messaging`（保持 `package edu.cuit.infra.dal.database.dataobject` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-messaging`；最小回归通过；落地：`87b38a55`）。
- 下一步建议（2026-02-06，保持行为不变，单 pom 主线）：继续推进“依赖方 `pom.xml` 编译期依赖收敛”，优先从 `bc-*/application` 或 `bc-*/contract` 入手，尝试移除冗余 `lombok(provided)` / 无用 `junit-jupiter(test)`；移除前必须 Serena 证伪源码无引用面，并跑最小回归后闭环（提交 + 三文档同步 + push）。
- 下一步建议（保持行为不变，单 pom 主线）：继续推进“依赖方 `pom.xml` 编译期依赖收敛”，优先围绕 `eva-infra-shared` / `eva-infra-dal` 做“Serena 证伪无引用面 → 再移除/降 scope”的闭环（每步只改 1 个 `pom.xml`，避免装配与依赖边界一次性大改导致回滚困难）。
- 补充进展（2026-01-17，保持行为不变，Controller 归位前置）：为后续将 `AuthenticationController` 从 `eva-adapter` 归位到 `bc-iam-infra`，先在 `bc-iam/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`、`commons-lang3`（仅编译闭合；最小回归通过；落地：`42d44f0b`）。
- 补充进展（2026-01-17，保持行为不变，Controller 归位前置）：为后续归位 `UserUpdateController`（依赖 `SysException/LogModule/ValidStatus`）做编译闭合前置，在 `bc-iam/infrastructure/pom.xml` 补齐 `cola-component-exception`、`shared-kernel`、`eva-base-common`（保持行为不变；最小回归通过；落地：`ddd5ff2a`）。
- 补充进展（2026-01-17，保持行为不变，Controller 归位前置）：为后续归位 `UserQueryController`（依赖 `IEvaStatisticsService`）做编译闭合前置，在 `bc-iam/infrastructure/pom.xml` 补齐 `bc-evaluation-contract`（保持行为不变；最小回归通过；落地：`0781952e`）。
- 补充进展（2026-01-17，保持行为不变，依赖收敛：组合根→eva-adapter）：Serena 证伪 `start` 源码未引用 `edu.cuit.adapter.*` 后，将 `start/pom.xml` 对 `eva-adapter` 的依赖 scope 改为 `runtime`（运行期装配不变，仅收敛编译期边界；最小回归通过；落地：`045891d1`；阶段性中间态）。
- 补充进展（2026-01-26，保持行为不变，组合根收敛：start 去 eva-adapter）：在 `eva-adapter` Controller 已清零且入口已归位各 BC 的前提下，移除 `start/pom.xml` 对 `eva-adapter` 的 Maven 依赖（最小回归通过；落地：`92a70a9e`）。
- 补充进展（2026-01-26，保持行为不变，组合根收敛：root reactor 去 eva-adapter）：在 Serena 证据化确认（当时）全仓库仅 `eva-adapter/pom.xml` 声明 `<artifactId>eva-adapter</artifactId>` 后，从根 `pom.xml` 的 reactor 中移除 `eva-adapter` 模块（最小回归通过；落地：`86842a1f`）。
- 补充进展（2026-01-26，保持行为不变，组合根收敛：删除 eva-adapter/pom.xml）：在 `eva-adapter` 已从 root reactor 退场的前提下，删除 `eva-adapter/pom.xml`，使全仓库 `**/pom.xml` 不再出现 `<artifactId>eva-adapter</artifactId>`（最小回归通过；落地：`ed244cad`）。
- 补充进展（2026-01-26，保持行为不变，依赖收敛：eva-domain 去 bc-evaluation-contract）：在 Serena 证据化确认 `eva-domain/src/main/java` 无评教 contract 类型引用后，收敛 `eva-domain/pom.xml`：移除对 `bc-evaluation-contract` 的 Maven 编译期依赖（最小回归通过；落地：`ccbb1cf9`）。
- 补充进展（2026-01-28，保持行为不变，依赖收敛：eva-domain 去 spring-statemachine-core）：在 Serena + `rg` 证伪 `eva-domain/src/main/java` 无 `statemachine` 相关引用后，收敛 `eva-domain/pom.xml`：移除 `spring-statemachine-core` 的 Maven 编译期依赖（最小回归通过；落地：`12c8d4bb`）。
- 补充进展（2026-01-28，保持行为不变，依赖收敛纠偏：bc-audit(application) 恢复 eva-domain）：此前仅按 `edu.cuit.domain.*` 引用面误判可去 `eva-domain`（`bf90c040`）；后续确认 `bc-audit/application` 仍引用 `edu.cuit.client.bo.SysLogBO`（定义于 `eva-domain`，包名保持不变），因此恢复 `bc-audit/application/pom.xml` 对 `eva-domain` 的 Maven 编译期依赖以闭合编译（最小回归通过；纠偏落地：`b47d71ab`）。
- 补充进展（2026-01-28，保持行为不变，类型下沉：PaginationResultEntity → shared-kernel）：为逐步减少各 BC（含 IAM）对 `eva-domain` 的编译期耦合，将通用分页实体 `edu.cuit.domain.entity.PaginationResultEntity` 从 `eva-domain` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`d31bb204`）。
- 补充进展（2026-01-28，保持行为不变，类型下沉：SysLogBO → shared-kernel）：为进一步减少依赖方对 `eva-domain` 的编译期耦合，将日志 BO `edu.cuit.client.bo.SysLogBO` 从 `eva-domain` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`96de4244`）。
- 补充进展（2026-01-28，保持行为不变，依赖收敛：bc-audit(application) 去 eva-domain）：在 `SysLogBO` 已下沉到 `shared-kernel` 且 Serena + `rg` 证伪 `bc-audit/application/src/main/java` 无 `edu.cuit.domain.*` 引用后，收敛 `bc-audit/application/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖（最小回归通过；落地：`0ee2a831`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-ai-report(application) 去 eva-domain）：在 Serena + `rg` 证伪 `bc-ai-report/application/src/main/java` 无 `edu.cuit.domain.*` 引用，且 `edu.cuit.client.api.ai/edu.cuit.client.bo.ai` 已由 `bc-ai-report(application)` 内部承载后，收敛 `bc-ai-report/application/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖（最小回归通过；落地：`53a61ee8`）。
- 补充进展（2026-01-30，保持行为不变，编译闭合前置：bc-ai-report(application) 显式依赖 cola-component-exception）：`ExportAiReportDocByUsernameUseCase` 仍显式抛出 `SysException`，为避免经由 `eva-domain` 间接依赖导致口径漂移，在 `bc-ai-report/application/pom.xml` 显式增加 `cola-component-exception` 依赖以闭合编译（最小回归通过；落地：`3e7fd3bd`）。
- 补充进展（2026-01-30，保持行为不变，编译闭合前置：shared-kernel 显式依赖 cola-component-domain-starter(optional)）：为后续将评教等旧领域 `@Entity` 类型逐类下沉到 `shared-kernel` 提供编译闭合支撑，在 `shared-kernel/pom.xml` 显式增加 `cola-component-domain-starter(optional)`（最小回归通过；落地：`a77e1b71`）。
- 补充进展（2026-01-30，保持行为不变，类型下沉：SimpleRoleInfoCO → shared-kernel）：为后续收敛 `eva-domain` 对 `bc-iam-contract` 的编译期耦合并保持 `RoleQueryGateway`（跨 BC 复用）稳定，将 `SimpleRoleInfoCO` 从 `bc-iam-contract` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`a04dfd7c`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：eva-domain 去 bc-iam-contract）：在 `SimpleRoleInfoCO` 已下沉到 `shared-kernel` 的前提下，Serena + `rg` 证伪 `eva-domain/src/main/java` 无其它 `bc-iam-contract` 类型引用后，收敛 `eva-domain/pom.xml`：移除对 `bc-iam-contract` 的 Maven 编译期依赖（最小回归通过；落地：`49eadf1f`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：start 去 JUnit4）：Serena + `rg` 证伪 `start/src/test/java` 无 JUnit4（`org.junit.Test/org.junit.runner.*`）引用后，收敛 `start/pom.xml`：移除 `junit:junit`（JUnit4）依赖（最小回归通过；落地：`e97a5205`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-ai-report-infra 去无用测试依赖）：Serena + `rg` 证伪 `bc-ai-report/infrastructure/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-ai-report/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`770221ea`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-ai-report(application) 去无用测试依赖）：Serena + `rg` 证伪 `bc-ai-report/application/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-ai-report/application/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`317f1859`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-iam-contract 去无用测试依赖）：Serena + `rg` 证伪 `bc-iam/contract/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-iam/contract/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`f623a290`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-messaging-contract 去无用测试依赖）：Serena + `rg` 证伪 `bc-messaging-contract/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-messaging-contract/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`f0047fb1`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-evaluation-contract 去无用测试依赖）：Serena + `rg` 证伪 `bc-evaluation/contract/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-evaluation/contract/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`841ba3c3`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：shared-kernel 去无用测试依赖）：Serena + `rg` 证伪 `shared-kernel` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `shared-kernel/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`61106d70`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-audit(application) 去无用测试依赖）：Serena + `rg` 证伪 `bc-audit/application/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-audit/application/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`182c34cf`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-audit(infrastructure) 去无用测试依赖）：Serena + `rg` 证伪 `bc-audit/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-audit/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`9cd9a96a`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-template-infra 去无用测试依赖）：Serena + `rg` 证伪 `bc-template/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-template/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`a8b018a5`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-iam-infra 去无用测试依赖）：Serena + `rg` 证伪 `bc-iam/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-iam/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`f65ce9a7`）。
- 补充进展（2026-01-31，保持行为不变，端口下沉：UserBasicQueryPort → bc-iam-contract）：为减少依赖方对 IAM 应用层 jar（`bc-iam`）的编译期绑定，将 `UserBasicQueryPort` 从 `bc-iam/application` 下沉到 `bc-iam-contract`（保持 `package edu.cuit.bc.iam.application.port` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过；落地：`739cb25f`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-evaluation-infra 去无用测试依赖）：Serena + `rg` 证伪 `bc-evaluation/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-evaluation/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`6a882e4a`）。
- 补充进展（2026-01-30，保持行为不变，依赖收敛：bc-course-infra 去无用测试依赖）：Serena + `rg` 证伪 `bc-course/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-course/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过；落地：`ff109643`）。
- 补充进展（2026-01-28，保持行为不变，端口下沉：EvaRecordCountQueryPort → bc-evaluation-contract）：为后续收敛 IAM 对评教应用层的编译期依赖，在 Serena 证据化确认引用面后，将 `EvaRecordCountQueryPort` 从 `bc-evaluation/application` 下沉到 `bc-evaluation-contract`（保持 `package edu.cuit.bc.evaluation.application.port` 不变；最小回归通过；落地：`4c30b02c`）。
- 补充进展（2026-01-30，保持行为不变，端口下沉：EvaRecordScoreQueryPort → bc-evaluation-contract）：为后续收敛 AI 报告等依赖方对 `bc-evaluation`（application jar）的编译期耦合，先将 `EvaRecordScoreQueryPort` 从 `bc-evaluation/application` 下沉到 `bc-evaluation-contract`（保持 `package edu.cuit.bc.evaluation.application.port` 与接口签名不变；最小回归通过；落地：`78f45ee2`）。
- 补充进展（2026-01-28，保持行为不变，依赖收敛：bc-iam-infra 去 bc-evaluation 编译期依赖）：在 `EvaRecordCountQueryPort` 已下沉到 `bc-evaluation-contract` 的前提下，收敛 `bc-iam/infrastructure/pom.xml`：移除对 `bc-evaluation` 的 Maven 编译期依赖，仅保留 `bc-evaluation-contract`（最小回归通过；落地：`42a9e96c`）。
- 补充进展（2026-01-26，保持行为不变，依赖收敛：eva-infra-shared 去 bc-evaluation-contract）：在 Serena 证据化确认 `eva-infra-shared/src/main/java` 无评教 contract 类型引用后，收敛 `eva-infra-shared/pom.xml`：移除对 `bc-evaluation-contract` 的 Maven 编译期依赖（最小回归通过；落地：`d28a5904`）。
- 补充进展（2026-01-26，保持行为不变，依赖收敛：bc-iam-contract 去 bc-evaluation-contract）：在 Serena 证据化确认 `bc-iam/contract/src/main/java` 无评教 contract 类型引用后，收敛 `bc-iam/contract/pom.xml`：移除对 `bc-evaluation-contract` 的 Maven 编译期依赖（最小回归通过；落地：`dcf5849a`）。（后续证实误判，已恢复依赖：`918c5d45`）
- 补充进展（2026-01-27，保持行为不变，依赖收敛纠偏：bc-iam-contract 恢复 bc-evaluation-contract）：在 Serena 证据化确认 `IUserService#getOneUserScore` 仍返回 `UserSingleCourseScoreCO`（定义于 `bc-evaluation-contract`）后，恢复 `bc-iam/contract/pom.xml` 对 `bc-evaluation-contract` 的显式依赖（用于纠正 `dcf5849a` 的误判；最小回归通过；落地：`918c5d45`）。
- 补充进展（2026-01-27，保持行为不变，Controller 小幅重构：UserUpdateController）：在 `UserUpdateController` 抽取 `success()` 统一封装 `CommonResult.success()` 的返回表达，并修正少量参数空格格式以降低噪声（不改 URL/注解/异常/副作用顺序；最小回归通过；落地：`5ee37fd2`）。
- 补充进展（2026-01-27，保持行为不变，Controller 小幅重构：DepartmentController）：在 `DepartmentController` 抽取 `success(...)` 统一封装 `CommonResult.success(...)` 的返回表达（不改 URL/注解/异常/副作用顺序；最小回归通过；落地：`fbc5fb74`）。
- 补充进展（2026-01-26，保持行为不变，Controller 小幅重构：UserQueryController）：对 `UserQueryController` 进行纯结构性整理（简化临时变量与返回包装；不改 URL/注解/异常/副作用顺序；最小回归通过；落地：`a542abff`）。
- 补充进展（2026-01-26，保持行为不变，Controller 小幅重构：MenuQueryController）：对 `MenuQueryController` 进行纯结构性整理（简化临时变量与返回包装；不改 URL/注解/异常/副作用顺序；最小回归通过；落地：`e388ae84`）。
- 补充进展（2026-01-26，保持行为不变，Controller 小幅重构：RoleQueryController）：对 `RoleQueryController` 进行纯结构性整理（简化临时变量与返回包装；不改 URL/注解/异常/副作用顺序；最小回归通过；落地：`bb134377`）。
- 补充进展（2026-01-10，保持行为不变）：基础设施旧 `*GatewayImpl` 归位已阶段性闭环，`eva-infra/src/main/java/edu/cuit/infra/gateway/impl` 下残留已清零（详见 `NEXT_SESSION_HANDOFF.md` 0.10 与 `docs/DDD_REFACTOR_BACKLOG.md` 4.3）。下一会话不再投入该方向，避免重复劳动。
- 补充进展（2026-01-15，保持行为不变，装配责任上推：AI 报告）：已在 `start/pom.xml` 显式增加 `bc-ai-report-infra(runtime)`（落地：`08862a4b`），用于为后续收敛 `eva-app` 的 AI 报告编译期依赖边界做前置（保持行为不变）。
- 补充进展（2026-01-15，保持行为不变，依赖收敛：AI 报告）：在 Serena 证据化确认 `eva-app/src/main/java` 无 AI 相关直引后，已收敛 `eva-app/pom.xml`：移除对 `bc-ai-report` 与 `bc-ai-report-infra` 的 Maven 编译期依赖（运行期装配由组合根 `start` 显式兜底；落地：`2a4736c0`）。
- 补充进展（2026-01-19，保持行为不变，Controller 归位前置：消息）：为后续将 `MessageController` 从 `eva-adapter` 归位到 `bc-messaging` 做编译闭合前置，在 `bc-messaging/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`、`zym-spring-boot-starter-security`、`shared-kernel`（仅编译闭合；最小回归通过；落地：`aa7d57bb`）。
- 补充进展（2026-01-19，保持行为不变，Controller 归位前置：审计）：为后续将 `LogController` 从 `eva-adapter` 归位到 `bc-audit/infrastructure` 做编译闭合前置，在 `bc-audit/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`spring-boot-starter-validation`、`zym-spring-boot-starter-common`、`zym-spring-boot-starter-security`（运行时 classpath 已存在，仅显式化；最小回归通过；落地：`2464d2b9`）。
- 补充进展（2026-01-19，保持行为不变，入口归位：审计日志入口）：将 `LogController` 从 `eva-adapter` 归位到 `bc-audit/infrastructure`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过；落地：`b592cc0f`）。
- 补充进展（2026-01-26，保持行为不变，依赖收敛：eva-adapter 去 bc-audit）：在 Serena 证据化确认 `eva-adapter/src/main/java` 已无审计入口源码后，收敛 `eva-adapter/pom.xml`：移除对 `bc-audit` 的 Maven 编译期依赖（最小回归通过；落地：`3aa49c66`）。
- 补充进展（2026-01-26，保持行为不变，依赖收敛：eva-adapter 去 bc-ai-report）：在 Serena 证据化确认 `eva-adapter/src/main/java` 无 AI 报告相关源码后，收敛 `eva-adapter/pom.xml`：移除对 `bc-ai-report` 的 Maven 编译期依赖（最小回归通过；落地：`a7f85ac7`）。
- 补充进展（2026-01-26，保持行为不变，依赖收敛：eva-adapter 去 bc-*contract/shared-kernel）：在 Serena 证据化确认 `eva-adapter/src/main/java` 无源码后，进一步收敛 `eva-adapter/pom.xml`：移除 `shared-kernel` 与 `bc-iam-contract` / `bc-evaluation-contract` / `bc-messaging-contract` 的 Maven 编译期依赖（最小回归通过；落地：`84be3a4b`）。
- 补充进展（2026-01-15，保持行为不变，装配责任上推：websocket）：已在 `start/pom.xml` 显式增加 `spring-boot-starter-websocket`（落地：`97b543b1`），用于为后续收敛 `eva-app` 的 websocket 编译期依赖边界做前置（保持行为不变）。
- 补充进展（2026-01-15，保持行为不变，支撑类归位：websocket）：将 `MessageChannel` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.websocket` 不变；最小回归通过；落地：`0fbc4aef`），用于为后续归位 websocket 配置与依赖收敛做前置。（后续进一步瘦身：`MessageChannel` 已从 `eva-infra-shared` 归位到 `bc-messaging`，保持行为不变；落地：`10248c53`）
- 补充进展（2026-01-15，保持行为不变，支撑类归位：websocket）：将 `UriUtils` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.util` 不变；最小回归通过；落地：`c1a10d2d`），用于为后续归位 `WebSocketInterceptor` 做编译闭合前置。（后续进一步瘦身：`UriUtils` 已从 `eva-infra-shared` 归位到 `bc-messaging`，保持行为不变；落地：`3febc475`）
- 补充进展（2026-01-15，保持行为不变，编译闭合前置：websocket）：为后续归位 `WebSocketInterceptor`（依赖 `cn.dev33.satoken.stp.StpUtil`）做编译闭合前置，已在 `eva-infra-shared/pom.xml` 补齐 `zym-spring-boot-starter-security`（运行时 classpath 已存在，仅显式化；最小回归通过；落地：`fa2faf1d`）。
- 补充进展（2026-01-15，保持行为不变，配置/拦截器归位：websocket）：将 `WebSocketInterceptor` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.config` 不变；最小回归通过；落地：`df06f0e6`），用于继续收敛 `eva-app` 对 websocket 的编译期耦合面。（后续进一步瘦身：`WebSocketInterceptor` 已从 `eva-infra-shared` 归位到 `bc-messaging`，保持行为不变；落地：`3015ba57`）
- 补充进展（2026-01-15，保持行为不变，配置归位：websocket）：将 `WebSocketConfig` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.config` 不变；最小回归通过；落地：`e03e60f9`），用于继续收敛 `eva-app` 对 websocket 的编译期耦合面。（后续进一步瘦身：`WebSocketConfig` 已从 `eva-infra-shared` 归位到 `bc-messaging`，保持行为不变；落地：`eb110825`）
- 补充进展（2026-01-15，保持行为不变，依赖收敛：websocket）：在 Serena 证伪 `eva-app/src/main/java` 不再 `import org.springframework.web.socket.*` 后，已收敛 `eva-app/pom.xml` 并移除 `spring-boot-starter-websocket` 的编译期依赖（运行期由组合根 `start` 显式兜底；最小回归通过；落地：`4213a95a`）。
- ✅ 已完成（更新至 2026-01-16，保持行为不变，依赖收敛：消息契约）：在 Serena + `rg` 证伪 `eva-app/src/main/java` 无消息契约引用面后，已收敛 `eva-app/pom.xml` 并移除 `bc-messaging-contract` 的 Maven 编译期依赖（最小回归通过；落地：`b92314ef`）。
- 下一步建议（更新至 2026-01-11，保持行为不变）：优先收敛 `eva-app` 残留 `*ServiceImpl`（每次只改 1 个类），逐步把业务编排归位到各 BC 的 UseCase，让旧入口退化为“切面/登录态解析/委托壳”（验收仍以最小回归为准）。
- 补充进展（2026-01-11，保持行为不变，入口壳收敛：角色写侧）：已将 `eva-app` 的 `RoleServiceImpl` 写侧方法改为委托 `bc-iam` 的 UseCase（`UpdateRoleInfoUseCase/UpdateRoleStatusUseCase/AssignRolePermsUseCase/CreateRoleUseCase/DeleteRoleUseCase/DeleteMultipleRoleUseCase`），减少对旧 `RoleUpdateGateway` 的直耦合（事务边界仍由旧入口承接；异常文案/缓存失效/日志顺序与副作用完全不变；最小回归通过；落地：`a71efb84`）。
- 补充进展（2026-01-11，保持行为不变，入口壳收敛：菜单写侧）：已将 `eva-app` 的 `MenuServiceImpl` 写侧方法改为委托 `bc-iam` 的 UseCase（`UpdateMenuInfoUseCase/CreateMenuUseCase/DeleteMenuUseCase/DeleteMultipleMenuUseCase`），减少对旧 `MenuUpdateGateway` 的直耦合（事务边界仍由旧入口承接；异常文案/缓存失效/日志顺序与副作用完全不变；最小回归通过；落地：`905baf9f`）。
- 补充进展（2026-01-12，保持行为不变，入口壳收敛：审计日志写侧）：已将 `eva-app` 的 `LogServiceImpl.registerListener` 中“插入日志”链路改为委托 `bc-audit` 的 `InsertLogUseCase`，并保持异步执行语义与副作用顺序完全不变（最小回归通过；落地：`cdb885b0`）。
- 补充进展（2026-01-12，保持行为不变，入口壳收敛：用户写侧）：已将 `eva-app` 的 `UserServiceImpl` 写侧对 `UserUpdateGateway` 的调用改为直接委托 `bc-iam` 的 `UpdateUserInfoUseCase/UpdateUserStatusUseCase/AssignRoleUseCase/CreateUserUseCase/DeleteUserUseCase`（保留登录态解析/密码修改/登出/副作用顺序完全不变；读侧仍保留 `UserQueryGateway` 以保持缓存切面触发点不变；最小回归通过；落地：`2b095a69`）。
- 补充进展（2026-01-12，保持行为不变，入口壳收敛：登录写侧）：已将 `eva-app` 的 `UserAuthServiceImpl.login` 中“LDAP 鉴权 + 用户状态校验”链路下沉到 `bc-iam` 的用例 `ValidateUserLoginUseCase`，旧入口退化为“登录态解析（`StpUtil.isLogin`）+ 委托用例 + `StpUtil.login` + 返回 token”的壳（异常文案/分支顺序完全不变；最小回归通过；落地：`d7c93768`）。
- 补充进展（2026-01-12，保持行为不变，消息入口收敛准备）：为让 `eva-app` 的 `MsgServiceImpl` 后续能直接委托 `bc-messaging` 的 UseCase（保持行为不变），先在 `eva-app/pom.xml` 补齐对 `bc-messaging` 的编译期依赖（运行时 classpath 已包含该模块；最小回归通过；落地：`02d338a9`）。
- 补充进展（2026-01-12，保持行为不变，入口壳收敛：消息）：将 `eva-app` 的 `MsgServiceImpl` 中对消息查询/标记已读/展示状态/删除/落库的调用，改为直接委托 `bc-messaging` 的 `QueryMessageUseCase/MarkMessageReadUseCase/UpdateMessageDisplayUseCase/DeleteMessageUseCase/InsertMessageUseCase`（保持 `checkAndGetUserId()` 先执行；副作用顺序与异常文案完全不变；最小回归通过；落地：`28ba21e4`）。
- 补充进展（2026-01-12，保持行为不变，SemesterServiceImpl 收敛准备）：在 `bc-course/application/pom.xml` 补齐对 `eva-domain` 的编译期依赖，为后续新增学期查询用例并让 `eva-app` 旧入口委托 UseCase 做前置（最小回归通过；落地：`d5ea0d96`）。
- 补充进展（2026-01-12，保持行为不变，SemesterServiceImpl 收敛准备）：在 `bc-course/application` 新增学期查询用例 `SemesterQueryUseCase`（当前仅委托 `SemesterGateway`，不改业务语义；最小回归通过；落地：`7d8323b5`）。
- 补充进展（2026-01-12，保持行为不变，SemesterServiceImpl 收敛准备）：在 `bc-course` 组合根 `BcCourseConfiguration` 补齐 `SemesterQueryUseCase` 的 Bean 装配（不改业务语义；最小回归通过；落地：`a93e2bda`）。
- 补充进展（2026-01-12，保持行为不变，SemesterServiceImpl 收敛准备）：为后续让 `eva-app` 的 `SemesterServiceImpl` 直接委托学期查询用例，在 `eva-app/pom.xml` 补齐对 `bc-course` 的编译期依赖（不改业务语义；最小回归通过；落地：`9f61788b`）。
- 补充进展（2026-01-12，保持行为不变，入口壳收敛：学期）：将 `eva-app` 的 `SemesterServiceImpl` 从直接调用 `SemesterGateway` 改为委托 `SemesterQueryUseCase`（事务边界仍由旧入口承接；异常文案与副作用顺序不变；最小回归通过；落地：`292eb1f2`）。
- 补充进展（2026-01-12，保持行为不变，DepartmentServiceImpl 收敛准备）：在 `bc-iam` 新增院系查询用例 `DepartmentQueryUseCase`（当前仅委托 `DepartmentGateway.getAll()`，不改业务语义；最小回归通过；落地：`78fd4b0e`）。
- 补充进展（2026-01-12，保持行为不变，DepartmentServiceImpl 收敛准备）：在 `BcIamConfiguration` 补齐 `DepartmentQueryUseCase` 的 Bean 装配（不改业务语义；最小回归通过；落地：`1cc7cc8a`）。
- 补充进展（2026-01-12，保持行为不变，入口壳收敛：院系）：将 `eva-app` 的 `DepartmentServiceImpl` 从直接调用 `DepartmentGateway` 改为委托 `DepartmentQueryUseCase`（异常文案与副作用顺序不变；最小回归通过；落地：`d9e4b7d6`）。
- 补充进展（2026-01-12，保持行为不变，ClassroomServiceImpl 收敛准备）：在 `bc-course` 新增教室查询用例 `ClassroomQueryUseCase`（当前仅委托 `ClassroomGateway.getAll()`，不改业务语义；最小回归通过；落地：`09822993`）。
- 补充进展（2026-01-12，保持行为不变，ClassroomServiceImpl 收敛准备）：在 `BcCourseConfiguration` 补齐 `ClassroomQueryUseCase` 的 Bean 装配（不改业务语义；最小回归通过；落地：`abdfe122`）。
- 补充进展（2026-01-12，保持行为不变，入口壳收敛：教室）：将 `eva-app` 的 `ClassroomServiceImpl` 从直接调用 `ClassroomGateway` 改为委托 `ClassroomQueryUseCase`（异常文案与副作用顺序不变；最小回归通过；落地：`20361679`）。
- ✅ 补充进展（2026-02-04，保持行为不变，编译闭合前置：bc-course-domain）：为后续将 `edu.cuit.domain.gateway.*` 从 `eva-domain` 逐类归位到 `bc-course-domain`，在 `bc-course/domain/pom.xml` 补齐 `spring-context(provided)`（仅编译期依赖；最小回归通过；落地：`3ab4b3de`）。
- ✅ 补充进展（2026-02-04，保持行为不变，类型归位：教室网关接口）：Serena 证伪 `ClassroomGateway` 引用面仅在 `bc-course/**` 后，已将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与接口签名/注解不变；最小回归通过；落地：`11ac2be6`）。
- ✅ 补充进展（2026-02-04，保持行为不变，类型归位：学期网关接口）：Serena 证伪 `SemesterGateway` 引用面覆盖 `bc-course/**` 与 `eva-infra-shared`（AOP 依赖）后，已将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与接口签名/注解不变；最小回归通过；落地：`30d1c98a`）。补充：为保证编译闭合，已在 `bc-course/domain/pom.xml` 补齐 `shared-kernel` 依赖（承接 `SemesterCO`；落地：`6353d17e`），并在 `eva-infra-shared/pom.xml` 显式依赖 `bc-course-domain`（过渡期；落地：`46bf4c15`）。
- ✅ 补充进展（2026-02-04，保持行为不变，类型归位：课程类型实体）：Serena 证伪 `CourseTypeEntity` 引用面集中在课程域（含 `CourseQueryGateway` 签名）后，已将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与类内容不变；最小回归通过；落地：`97a8e3f3`）。
- ✅ 补充进展（2026-02-04，保持行为不变，编译闭合前置：eva-domain→bc-course-domain）：为后续逐类把课程域类型从 `eva-domain` 归位到 `bc-course-domain`（且避免 `eva-domain` 内残留接口签名引用导致编译失败），在 `eva-domain/pom.xml` 显式增加对 `bc-course-domain` 的 Maven 编译期依赖（过渡期；最小回归通过；落地：`c425f384`）。
- ✅ 补充进展（2026-02-04，保持行为不变，编译闭合前置：bc-course-domain→cola/lombok）：为后续归位 `CourseTypeEntity` 等带 `@Entity`/Lombok 的课程域实体做编译闭合前置，在 `bc-course/domain/pom.xml` 补齐 `cola-component-domain-starter` 与 `lombok(provided)`（最小回归通过；落地：`2c0626e8`）。
- 补充进展（2026-01-12，保持行为不变，ICourseTypeServiceImpl 收敛准备）：在 `bc-course/application` 新增课程类型用例 `CourseTypeUseCase`（读写合并；手写 `CourseTypeEntity` → `CourseType` 映射与 `PaginationQueryResultCO` 组装，不引入 `eva-infra-shared`；对齐旧入口逻辑与返回语义；最小回归通过；落地：`325f221a`）。
- 补充进展（2026-01-12，保持行为不变，ICourseTypeServiceImpl 收敛准备）：在 `BcCourseConfiguration` 补齐 `CourseTypeUseCase` 的 Bean 装配（保持行为不变；最小回归通过；落地：`55eb322e`）。
- 补充进展（2026-01-12，保持行为不变，入口壳收敛：课程类型）：将 `eva-app` 的 `ICourseTypeServiceImpl` 退化为纯委托壳，改为委托 `CourseTypeUseCase`（保持 `id==null` 语义与 `updateCoursesType` 返回 `null`（`Void`）等行为不变；最小回归通过；落地：`1aebda24`）。
- 现状快照（更新至 2026-01-19，保持行为不变）：`eva-app` 已退场（组合根去依赖：`0a9ff564`；reactor 移除：`b5f15a4b`；删除 `eva-app/pom.xml`：`4bfa9d40`）。`eva-adapter` 下残留 `*Controller` 为 **0 个**（已将 `AuthenticationController`、`UserUpdateController`、`UserQueryController`、`RoleQueryController`、`MenuQueryController`、`MenuUpdateController`、`RoleUpdateController`、`DepartmentController`、`ClassroomController`、`SemesterController`、`QueryCourseController`、`QueryUserCourseController`、`DeleteCourseController`、`UpdateCourseController`、`EvaStatisticsController`、`EvaConfigQueryController`、`EvaQueryController`、`EvaConfigUpdateController`、`UpdateEvaController`、`DeleteEvaController`、`MessageController`、`LogController` 归位到对应 BC，落地：`94a00022`、`367f781d`、`f7c5d219`、`e7d51beb`、`76e19a47`、`d29b565b`、`80888bed`、`3e66a7b4`、`132f32f5`、`0257ddd0`、`1b9a6fc7`、`bc37fa17`、`4b6219b9`、`1d03d987`、`f533261a`、`5e7537ef`、`d101ce07`、`b5530281`、`3972b7e4`、`d1471ff5`、`7b076019`、`b592cc0f`），且历史上曾补齐 `eva-adapter/pom.xml` 的编译依赖闭合以避免增量编译掩盖问题（`56273162`；现模块已退场并删除 pom）。下一刀建议转向“BC 内 Controller 入口壳结构性收敛 + 依赖方 pom 收敛（含纠偏）”（保持行为不变）。
- 下一会话建议（保持行为不变；每次只改 1 个类或 1 个 `pom.xml`）：优先继续推进 `bc-iam` 的 Controller 入口壳结构性收敛（每次 1 个 Controller），并并行推进“依赖方 `pom.xml` 编译期依赖收敛”（移除前必须 Serena + `rg` 双证据，避免误判导致构建漂移）。
- 补充进展（2026-01-19，保持行为不变，Controller 归位前置）：为后续归位 `ClassroomController` 做编译闭合前置，在 `bc-course/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`（运行期 classpath 已存在，仅显式化；最小回归通过；落地：`8915db14`）。
- 补充进展（2026-01-19，保持行为不变，入口归位：教室查询入口）：将 `ClassroomController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 不变；最小回归通过；落地：`132f32f5`）。
- 补充进展（2026-01-19，保持行为不变，Controller 归位前置）：为后续归位 `QueryUserCourseController`（依赖 `CalculateClassTime`）做编译闭合前置，将 `edu.cuit.adapter.controller.course.util.CalculateClassTime` 从 `eva-adapter` 下沉到 `shared-kernel`（保持 `package/逻辑` 不变，仅搬运归位；最小回归通过；落地：`8a3f738c`）。
- 补充进展（2026-01-19，保持行为不变，Controller 归位前置）：为后续将 `EvaStatisticsController` 从 `eva-adapter` 归位到 `bc-evaluation/infrastructure` 做编译闭合前置，在 `bc-evaluation/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`、`bc-evaluation-contract`、`bc-ai-report`（仅编译闭合；最小回归通过；落地：`a5e1eb52`）。
- 补充进展（2026-01-19，保持行为不变，入口归位：学期查询入口）：将 `SemesterController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 不变；最小回归通过；落地：`0257ddd0`）。
- 补充进展（2026-01-19，保持行为不变，入口归位：课程信息查询入口）：将 `QueryCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 不变；最小回归通过；落地：`1b9a6fc7`）。
- 补充进展（2026-01-19，保持行为不变，入口归位：用户课程查询入口）：将 `QueryUserCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 不变；最小回归通过；落地：`bc37fa17`）。
- 补充进展（2026-01-19，保持行为不变，入口归位：课程删除入口）：将 `DeleteCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 不变；最小回归通过；落地：`4b6219b9`）。
- 补充进展（2026-01-19，保持行为不变，入口归位：课程写入口）：将 `UpdateCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 不变；最小回归通过；落地：`1d03d987`）。
- 补充进展（2026-01-12，保持行为不变，Controller 收敛起步）：在 `UserUpdateController` 提取 `currentUserId()` 以减少适配层编排噪声（保持 `StpUtil.getLoginId()` 调用顺序与次数不变；最小回归通过；落地：`09cb6454`）。
- 补充进展（2026-01-12，保持行为不变，Controller 收敛：登录入口）：在 `AuthenticationController` 收敛 `login` 表达式，用“局部变量 + return”显式固化 `userAuthService.login(...)` → `CommonResult.success(...)` 的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`102a0cbb`）。
- 补充进展（2026-01-12，保持行为不变，Controller 收敛：用户查询入口）：在 `UserQueryController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`18e0bb29`）。
- 补充进展（2026-01-12，保持行为不变，Controller 收敛：角色查询入口）：在 `RoleQueryController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`fa04e48e`）。
- 补充进展（2026-01-12，保持行为不变，Controller 收敛：菜单查询入口）：在 `MenuQueryController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`8521f72c`）。
- 补充进展（2026-01-12，保持行为不变，Controller 收敛：菜单写入口）：在 `MenuUpdateController.create` 收敛日志内容构造表达式，保持 `menuService.create(...)` → `LogUtils.logContent(...)` → `CommonResult.success()` 顺序不变（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`9a2fc3ff`）。
- 补充进展（2026-01-12，保持行为不变，Controller 收敛：角色写入口）：在 `RoleUpdateController.create` 收敛日志内容构造表达式，保持 `roleService.create(...)` → `LogUtils.logContent(...)` → `CommonResult.success()` 顺序不变（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`9f07698a`）。
- 补充进展（2026-01-12，保持行为不变，Controller 收敛：学期查询入口）：在 `SemesterController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`24e5ad3f`）。
- 补充进展（2026-01-12，保持行为不变，Controller 收敛：院系查询入口）：在 `DepartmentController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`e57c0e41`）。
- 补充进展（2026-01-12，保持行为不变，Controller 收敛：教室查询入口）：在 `ClassroomController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`f1606018`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：课程查询入口）：在 `QueryCourseController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`0a691bb1`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：用户-课程查询入口）：在 `QueryUserCourseController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`d49976cf`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：课程写入口）：在 `UpdateCourseController` 收敛返回表达式与日志内容构造写法（日志内容仍在 `CommonResult.success(..., supplier)` 内部构造；不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`58b1b763`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：课程删除入口）：在 `DeleteCourseController` 清理无用 import，并收敛参数/调用的格式表达（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`20e1214c`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：评教统计查询入口）：在 `EvaStatisticsController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`7c465c5d`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：评教查询入口）：在 `EvaQueryController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`74a34133`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：评教写入口）：在 `UpdateEvaController` 清理无用 import，并收敛日志内容构造写法（仍保持 `service` 调用 → `LogUtils.logContent(...)` → `CommonResult.success(null)` 的顺序不变；不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`87fc0475`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：评教删除入口）：在 `DeleteEvaController` 清理未使用注入依赖与无用 import，并抽取 `success()` 收敛 `CommonResult.success(null)` 的重复表达（仍保持 `service` 调用 → `CommonResult.success(null)` 的执行顺序不变；不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`7809cc2d`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：评教配置查询入口）：在 `EvaConfigQueryController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；权限注解保持不变；最小回归通过；落地：`d055c271`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：评教配置修改入口）：在 `EvaConfigUpdateController` 收敛返回表达式，抽取 `success()` 以显式固化“先调用 service → 再 `CommonResult.success()`”的执行顺序（不改异常文案/返回结构/副作用顺序；权限注解保持不变；最小回归通过；落地：`c7ab9633`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：消息入口）：在 `MessageController` 将查询接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`aa99c775`）。
- 补充进展（2026-01-13，保持行为不变，Controller 收敛：日志入口）：在 `LogController` 将查询接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回结构/副作用顺序；最小回归通过；落地：`e19278a6`）。
- 补充进展（2026-01-13，保持行为不变，S0.2 延伸，每次只改 1 个 `pom.xml`）：已在 `start/pom.xml` 显式增加对 `eva-app` 的 `runtime` 依赖，使组合根承接装配责任的前置条件落地（最小回归通过；落地：`0a69dfb6`）。随后已在 Serena + `rg` 证伪 `eva-adapter` 不再引用 `edu.cuit.app.*` 实现类型后，移除 `eva-adapter/pom.xml` 对 `eva-app` 的 Maven 依赖，以减少编译期耦合（保持行为不变；最小回归通过；落地：`f5980fcc`）。（后续已在 `0a9ff564` 将组合根依赖从 `eva-app` 替换为 `eva-infra(runtime)` 并移除 `start` 对 `eva-app` 的依赖；并已在 `1e2ffa89` 移除组合根对 `eva-infra(runtime)` 的兜底依赖。）
  - ✅ 已完成（保持行为不变）：Serena + `rg` 已证伪 `eva-app` 不再直接依赖 `bc-course` 的 UseCase 实现类型（口径：`rg -n '^import edu\\.cuit\\.bc\\.course' eva-app/src/main/java` 证伪为 0），并已收敛 `eva-app/pom.xml`：移除对 `bc-course` 的编译期依赖（`shared-kernel` 显式依赖保留；最小回归通过；落地：`dca806fa`）。
  - 补充（保持行为不变，装配责任上推）：已将 `start/pom.xml` 中 `bc-course-infra` 的依赖范围从 `test` 调整为 `runtime`，使课程域基础设施的运行时依赖由组合根显式兜底（最小回归通过；落地：`2a442587`）。后续可在确认组合根已兜底后，再评估是否能移除 `eva-app/pom.xml` 中对 `bc-course-infra` 的 `runtime` 依赖（每次只改 1 个 `pom.xml`；保持行为不变）。
  - 补充（保持行为不变，装配责任上推）：在组合根已显式兜底后，已移除 `eva-app/pom.xml` 中对 `bc-course-infra` 的 `runtime` 依赖，使课程域基础设施的运行时 classpath 更清晰地由组合根承接（最小回归通过；落地：`9e7bd82d`）。
  - 补充（保持行为不变，依赖收敛：消息）：为进一步收敛 `eva-app` 对消息域的编译期耦合，已将 `MsgServiceImpl` 内部对 `bc-messaging` 用例实现类型的直接依赖改为委托 `MsgGateway`（`MsgGatewayImpl` 仍由 `bc-messaging` 在运行时提供，并继续内部委托 `MessageUseCaseFacade`/UseCase，确保行为不变）；为后续在 `eva-app/pom.xml` 去 `bc-messaging` 编译期依赖做前置（最小回归通过；落地：`35b8eb90`）。
  - 补充（保持行为不变，依赖收敛：消息）：在上述前置完成后，已移除 `eva-app/pom.xml` 中对 `bc-messaging` 的 Maven 依赖，仅保留 `bc-messaging-contract` 承载协议对象；消息域运行时装配由组合根 `start` 显式兜底（最小回归通过；落地：`afbe2e6c`）。
  - 补充（保持行为不变，依赖收敛：课程）：将 `SemesterServiceImpl` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl` 不变；仍实现 `ISemesterService` 并委托 `SemesterQueryUseCase`；保留事务边界与异常/副作用顺序；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-course` 的编译期耦合面；最小回归通过；落地：`8eddc643`）。
  - 补充（保持行为不变，依赖收敛：课程）：将 `ClassroomServiceImpl` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl` 不变；仍实现 `IClassroomService` 并委托 `ClassroomQueryUseCase`；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-course` 的编译期耦合面；最小回归通过；落地：`4bc8cfb1`）。
  - 补充（保持行为不变，依赖收敛：课程）：将 `ICourseTypeServiceImpl` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变；仍实现 `ICourseTypeService` 并委托 `CourseTypeUseCase`；保持 `id==null` 语义与 `updateCoursesType` 返回 `null`（`Void`）语义不变；最小回归通过；落地：`8a4a6774`）。
  - 补充（保持行为不变，依赖收敛：模板）：将 `BcTemplateConfiguration` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.config` 不变；`CourseTemplateLockService` Bean 定义与注入不变；用于后续收敛 `eva-app` 对 `bc-template` 的编译期依赖面；最小回归通过；落地：`0fbd5d63`）。
  - ✅ 已完成（保持行为不变，依赖收敛：模板）：Serena + `rg` 证伪 `eva-app` 不再引用 `edu.cuit.bc.template.*` 后，已收敛 `eva-app/pom.xml`：移除对 `bc-template` 的编译期依赖（最小回归通过；落地：`c53fd53d`）。
  - 补充（保持行为不变，依赖收敛：模板）：为降低基础设施实现侧的依赖层级，将模板锁定查询端口 `CourseTemplateLockQueryPort` 从 `bc-template/application` 下沉到 `bc-template-domain`（保持 `package edu.cuit.bc.template.application.port` 不变；最小回归通过；落地：`d0fa4878`）。
  - ✅ 已完成（保持行为不变，依赖收敛：模板）：在 Serena + `rg` 证伪 `eva-infra` 仅引用 `CourseTemplateLockQueryPort` 接口类型后，已收敛 `eva-infra/pom.xml`：将对 `bc-template` 的 Maven 依赖替换为 `bc-template-domain`（版本不变；最小回归通过；落地：`5910762e`）。
  - ✅ 已完成（保持行为不变，依赖收敛：模板）：在 Serena 证据化确认 `bc-template-infra` 当前无源码（仅 `pom.xml`）后，已收敛 `bc-template/infrastructure/pom.xml`：将对 `bc-template` 的 Maven 依赖替换为 `bc-template-domain`（版本不变；最小回归通过；落地：`aee98f9b`）。
  - ✅ 已完成（保持行为不变，编译闭合前置：模板）：为后续将 `CourseTemplateLockQueryPortImpl` 从 `eva-infra` 归位到 `bc-template-infra` 做前置，已在 `bc-template/infrastructure/pom.xml` 显式增加对 `eva-infra-dal` 的 Maven 编译期依赖（仅用于闭合 `Mapper/DO/QueryWrapper` 依赖；不改业务语义/装配/副作用顺序）；最小回归通过；落地：`4f819e13`。补充更新（2026-02-06，保持行为不变）：`bc-template-infra` 已改为依赖 `eva-infra-shared` 显式传递承接 `eva-infra-dal`，不再直依赖 `eva-infra-dal`（最小回归通过；落地：`204aef24`）。
  - ✅ 已完成（保持行为不变，装配责任上推：模板）：为后续 `CourseTemplateLockQueryPortImpl` 归位后仍可被组合根装配，已在 `start/pom.xml` 显式增加对 `bc-template-infra` 的 `runtime` 依赖（保持行为不变，仅上推依赖边界；最小回归通过；落地：`d51975fb`）。
  - ✅ 已完成（保持行为不变，基础设施端口实现归位：模板）：将 `CourseTemplateLockQueryPortImpl` 从 `eva-infra` 搬运归位到 `bc-template-infra`（保持 `package` 与类内容不变，仅改变 Maven 模块归属）；最小回归通过；落地：`9b46d5a7`。
  - ✅ 已完成（保持行为不变，装配责任收敛：组合根）：在 `start/pom.xml` 移除对 `eva-infra` 的 `runtime` 依赖（保持行为不变，仅收敛依赖边界）；最小回归通过；落地：`1e2ffa89`。
  - ✅ 已完成（保持行为不变，依赖收敛：课程相关前置）：为后续让 `bc-course/application` 可收敛对 `bc-template` 应用层 jar 的编译期依赖，将模板锁定服务 `CourseTemplateLockService` 从 `bc-template/application` 下沉到 `bc-template-domain`（保持 `package edu.cuit.bc.template.application` 与代码不变；最小回归通过；落地：`8a1319df`）。
  - ✅ 已完成（保持行为不变，依赖收敛：课程）：在 Serena + `rg` 证伪 `bc-course/application` 仅引用 `CourseTemplateLockService/CourseTemplateLockQueryPort/TemplateLockedException` 后，已收敛 `bc-course/application/pom.xml`：将对 `bc-template` 的 Maven 依赖替换为 `bc-template-domain`（版本不变；最小回归通过；落地：`2de83046`）。
  - ✅ 已完成（保持行为不变，依赖收敛：eva-infra）：在 Serena + `rg` 证伪 `eva-infra/src/main/java` 未引用 `bc-evaluation/bc-iam/bc-audit` 相关类型后，已收敛 `eva-infra/pom.xml`：移除对 `bc-evaluation` / `bc-iam` / `bc-audit` 的 Maven 编译期依赖（最小回归通过；落地：`023d63be`）。
  - ✅ 已完成（保持行为不变，依赖归位：bc-ai-report-infra）：Serena 证据化确认 `bc-ai-report/infrastructure` 的 `AiReportAnalysisPortImpl` 引用评教 application port `EvaRecordExportQueryPort` 后，已在 `bc-ai-report/infrastructure/pom.xml` 显式补齐对 `bc-evaluation` 的编译期依赖（避免经由 `bc-ai-report(application)` 传递；为后续从 `bc-ai-report/application/pom.xml` 下沉/移除该依赖做前置；最小回归通过；落地：`c0f78068`）。
  - ✅ 已完成（保持行为不变，依赖收敛：bc-ai-report application）：在 Serena + `rg` 证伪 `bc-ai-report/application/src/main/java` 未引用 `edu.cuit.bc.evaluation.*` 相关类型后，已收敛 `bc-ai-report/application/pom.xml`：移除对 `bc-evaluation` 的 Maven 编译期依赖，由 `bc-ai-report-infra` 显式承接该依赖（最小回归通过；落地：`87179f19`）。
  - ✅ 已完成（保持行为不变，IAM 旧入口归位：部门查询）：将 `DepartmentServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package` 不变；仍实现 `IDepartmentService` 并委托 `DepartmentQueryUseCase`；最小回归通过；落地：`68dea36a`）。
  - ✅ 已完成（保持行为不变，IAM 支撑类归位：菜单 Convertor）：为后续将 `MenuServiceImpl` 从 `eva-app` 归位到 `bc-iam-infra` 做前置，先将 `MenuBizConvertor` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package` 不变；最小回归通过；落地：`6298e5a7`）。
  - ✅ 已完成（保持行为不变，IAM 旧入口归位：菜单）：将 `MenuServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package` 不变；仍实现 `IMenuService` 并保留事务边界；最小回归通过；落地：`6aef1d96`）。
  - ✅ 已完成（保持行为不变，IAM 支撑类归位：角色 Convertor）：为后续将 `RoleServiceImpl/UserServiceImpl` 从 `eva-app` 归位到 `bc-iam-infra` 做前置，先将 `RoleBizConvertor` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package` 不变；最小回归通过；落地：`cf0773ac`）。
  - ✅ 已完成（保持行为不变，IAM 旧入口归位：角色）：将 `RoleServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package` 不变；仍实现 `IRoleService` 并保留事务边界；最小回归通过；落地：`3011ab83`）。
  - ✅ 已完成（保持行为不变，IAM 编译闭合前置：安全依赖）：为后续将 `UserAuthServiceImpl` 从 `eva-app` 归位到 `bc-iam-infra` 做前置，已在 `bc-iam/infrastructure/pom.xml` 补齐 `zym-spring-boot-starter-security`（最小回归通过；落地：`bded148a`）。
  - ✅ 已完成（保持行为不变，IAM 旧入口归位：登录）：将 `UserAuthServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl.user` 不变；类内容不变；最小回归通过；落地：`b2d885a7`）。
  - ✅ 已完成（保持行为不变，IAM 支撑类归位：头像配置属性）：为后续归位 `AvatarManager`/`UserServiceImpl` 做编译闭合前置，将 `AvatarProperties` 从 `eva-infra` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.infra.property` 不变；类内容不变；最小回归通过；落地：`bb97f037`）。
  - ✅ 已完成（保持行为不变，IAM 支撑类归位：头像管理）：为后续归位 `UserServiceImpl` 做编译闭合前置，将 `AvatarManager` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app` 不变；类内容不变；最小回归通过；落地：`50ab6c9e`）。
  - ✅ 已完成（保持行为不变，IAM 支撑类归位：用户 Convertor）：为后续归位 `UserServiceImpl` 做编译闭合前置，将 `UserBizConvertor` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.convertor.user` 不变；类内容不变；最小回归通过；落地：`10eeb4f3`）。
  - ✅ 已完成（保持行为不变，IAM 支撑类归位：路由工厂）：为后续归位 `UserServiceImpl` 做编译闭合前置，将 `RouterDetailFactory` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.factory.user` 不变；类内容不变；最小回归通过；落地：`be21d2a6`）。
  - ✅ 已完成（保持行为不变，IAM 编译闭合前置：评教端口依赖）：为后续归位 `UserServiceImpl` 做前置，在 `bc-iam/infrastructure/pom.xml` 补齐对 `bc-evaluation` 的编译期依赖（原因：`UserServiceImpl` 依赖 `EvaRecordCountQueryPort`；最小回归通过；落地：`8f5fc4ca`）。
  - ✅ 已完成（保持行为不变，IAM 旧入口归位：用户管理）：将 `UserServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl.user` 不变；类内容不变；最小回归通过；落地：`c4552031`）。
  - ✅ 已完成（保持行为不变，组合根归位：BcIamConfiguration）：将 `BcIamConfiguration` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.config` 不变；Bean 装配/副作用顺序不变；最小回归通过；落地：`14cd4108`）。
  - ✅ 已完成（保持行为不变，IAM 依赖收敛：eva-app 去 bc-iam 编译期依赖）：在 Serena + `rg` 证伪 `eva-app/src/main/java` 不再直接引用 `edu.cuit.bc.iam.*` 后，收敛 `eva-app/pom.xml`：移除对 `bc-iam` 的 Maven 编译期依赖（保留 `bc-iam-infra` 以闭合运行期装配；最小回归通过；落地：`290f2b82`）。
  - ✅ 已完成（保持行为不变，装配责任上推：start 显式依赖 bc-iam-infra）：为后续收敛 `eva-app` 的运行期装配依赖边界，在 `start/pom.xml` 增加对 `bc-iam-infra` 的 `runtime` 依赖（与原 transitive 结果等价，仅显式化；最小回归通过；落地：`8a5df2d0`）。
  - ✅ 已完成（保持行为不变，依赖收敛：eva-app 去 bc-iam-infra 依赖）：在 Serena 证据化确认 `eva-app/src/main/java` 不再引用 `edu.cuit.app.*` 的 IAM 实现类后，收敛 `eva-app/pom.xml`：移除对 `bc-iam-infra` 的依赖；运行期装配由组合根 `start` 显式兜底（最小回归通过；落地：`e7b46f36`）。
  - ✅ 已完成（保持行为不变，评教组合根归位：BcEvaluationConfiguration）：将 `BcEvaluationConfiguration` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.config` 不变；Bean 装配/副作用顺序不变；用于后续收敛 `eva-app` 对 `bc-evaluation-infra` 的依赖边界；最小回归通过；落地：`c3f7fc56`）。
  - ✅ 已完成（保持行为不变，装配责任上推：start 显式依赖 bc-evaluation-infra）：为后续收敛 `eva-app` 的运行期装配依赖边界，在 `start/pom.xml` 增加对 `bc-evaluation-infra` 的 `runtime` 依赖（与原 transitive 结果等价，仅显式化；最小回归通过；落地：`0f20d0cd`）。
  - ✅ 已完成（保持行为不变，依赖收敛：eva-app 去 bc-evaluation-infra 依赖）：在 Serena + `rg` 证伪 `eva-app/src/main/java` 不再直接引用 `edu.cuit.infra.bcevaluation.*` 实现类型后，收敛 `eva-app/pom.xml`：移除对 `bc-evaluation-infra` 的依赖；运行期装配由组合根 `start` 显式兜底（最小回归通过；落地：`e9feeb56`）。
  - ✅ 已完成（保持行为不变，评教事件发布器归位：SpringAfterCommitDomainEventPublisher）：将 `SpringAfterCommitDomainEventPublisher` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.event` 不变；`@Component` 装配语义不变；事务提交后发布逻辑不变；最小回归通过；落地：`1b9e275c`）。
  - ✅ 已完成（保持行为不变，评教监听器归位前置：bc-messaging-contract 依赖补齐）：为后续归位评教事务事件监听器（需要 `IMsgService` 契约类型）做编译闭合前置，在 `bc-evaluation-infra/pom.xml` 补齐对 `bc-messaging-contract` 的编译期依赖（不引入实现侧依赖；最小回归通过；落地：`bcb8df45`）。
  - ✅ 已完成（保持行为不变，评教旧入口壳归位前置：security 依赖补齐）：为后续归位 `EvaTaskServiceImpl/UserEvaServiceImpl`（依赖 `StpUtil` 登录态解析）做编译闭合前置，在 `bc-evaluation-infra/pom.xml` 补齐 `zym-spring-boot-starter-security` 依赖（运行时 classpath 已存在；保持行为不变；最小回归通过；落地：`202a6386`）。
  - ✅ 已完成（保持行为不变，评教事务事件监听器归位：EvaluationSubmittedMessageCleanupListener）：将 `EvaluationSubmittedMessageCleanupListener` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.event.listener` 不变；监听器触发点/调用顺序不变；注入类型收窄为 `IMsgService` 以避免依赖实现类；最小回归通过；落地：`314c5d6b`）。
  - ✅ 已完成（保持行为不变，评教事务事件监听器归位：EvaluationTaskPostedMessageSenderListener）：将 `EvaluationTaskPostedMessageSenderListener` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.event.listener` 不变；监听器触发点/调用顺序不变；注入类型收窄为 `IMsgService` 以避免依赖实现类；最小回归通过；落地：`c9fbe6ef`）。
  - ✅ 已完成（保持行为不变，评教旧入口壳归位：EvaStatisticsServiceImpl）：将 `EvaStatisticsServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；`@CheckSemId` 触发点与委托顺序不变；类内容不变；最小回归通过；落地：`6db29b33`）。
  - ✅ 已完成（保持行为不变，评教旧入口壳归位：EvaTemplateServiceImpl）：将 `EvaTemplateServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；异常转换与返回 `null` 语义不变；类内容不变；最小回归通过；落地：`c63b9875`）。
  - ✅ 已完成（保持行为不变，评教旧入口壳归位：EvaRecordServiceImpl）：将 `EvaRecordServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；异常转换与返回 `null` 语义不变；`@Transactional` 与表单值映射顺序不变；最小回归通过；落地：`c19b32fc`）。
  - ✅ 已完成（保持行为不变，评教旧入口壳归位：EvaTaskServiceImpl）：将 `EvaTaskServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；`@Transactional`/异常文案/日志输出与副作用顺序不变；消息依赖类型收窄为 `IMsgService` 以避免引用实现类；最小回归通过；落地：`1aaff86f`）。

- 补充进展（2026-01-05，S0.2 起步，保持行为不变）：已将学期 CO `SemesterCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`77126c4a`）。
- 补充进展（2026-01-05，S0.2 持续推进，保持行为不变）：已将通用学期入参 `Term` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`23bff82f`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将课程查询 Query（`CourseQuery/CourseConditionalQuery/MobileCourseQuery`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`e479ce0e`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将导入课表 BO `CourseExcelBO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`1f47a032`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将课程写侧命令对象子簇（`AlignTeacherCmd/UpdateCourseTypeCmd/UpdateCoursesCmd/UpdateCoursesToTypeCmd/UpdateSingleCourseCmd`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`0978b3cb`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将课程 CO 子簇（`SubjectCO/SelfTeachCourseCO/SelfTeachCourseTimeCO/SelfTeachCourseTimeInfoCO`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`87d8c692`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将课程写侧剩余命令对象（`UpdateCourseCmd/AddCoursesAndCourInfoCmd/UpdateCourseInfoAndTimeCmd`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`0d18e4ad`），从而完成 `edu.cuit.client.dto.cmd.course/*` 的迁移闭包。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将推荐课程 CO `RecommendCourseCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`24595a53`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将评教模板 CO `EvaTemplateCO` 从 `bc-evaluation-contract` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`34579fe0`），用于解除课程 CO（`CourseModelCO`）的类型依赖阻塞，支撑后续迁移 `CourseDetailCO`。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将课程详情相关 CO（`TeacherInfoCO/CourseModelCO/CourseDetailCO`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`4dbcb2de`），进一步消除 eva-domain 对 bc-course 的类型依赖。
- 补充进展（2026-01-06，S0.2 收尾，保持行为不变）：Serena 证伪 `eva-domain` 已不再需要 `bc-course` 提供的 `edu.cuit.client.*` 类型后，移除 `eva-domain/pom.xml` 对 `bc-course` 的 Maven 依赖，改为显式依赖 `shared-kernel`（最小回归通过；落地：`01b36508`）。
- 补充进展（2026-01-06，S0.2 依赖收敛补齐，保持行为不变）：将课程用户侧接口 `IUserCourseService`（以及其出参 `SimpleSubjectResultCO`）迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`e2a697f1`），从而移除 `bc-ai-report-infra` 对 `bc-course` 的显式编译期依赖（避免依赖回潮）。
- 补充进展（2026-01-06，S0.2 延伸，保持行为不变）：已将评教域 CO `CourseScoreCO/EvaTeacherInfoCO` 从 `bc-evaluation/contract` 迁移到 `shared-kernel`（保持 `package` 不变），并移除 `bc-course/application` 对 `bc-evaluation-contract` 的编译期依赖（最小回归通过；落地：`bc30e9de`）。
- 补充进展（2026-01-06，S0.2 延伸，保持行为不变）：已将单节课详情 CO `SingleCourseDetailCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`95b01a07`）。
- 补充进展（2026-01-06，S0.2 延伸，保持行为不变）：已将单课次衍生详情 CO `ModifySingleCourseDetailCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`1e9be81d`）。
- 补充进展（2026-01-06，S0.2 延伸，保持行为不变）：已将课程 API 接口 `ICourseService/ICourseTypeService` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`4dbeb55f`）。
- 补充进展（2026-01-06，S0.2 延伸，保持行为不变）：已将课程详情接口 `ICourseDetailService` 从 `bc-course/application` 迁移到 `shared-kernel`，并将其签名依赖的 `SimpleCourseResultCO` 一并下沉（均保持 `package` 不变；最小回归通过；落地：`f9ccc6e9`）。
- 补充进展（2026-01-07，S0.2 延伸（课程域基础设施归位起步），保持行为不变）：将 `AddCourseTypeRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，行为不变）；并在 `bc-course-infra` 补齐 `zym-spring-boot-starter-cache` 编译期依赖以闭合 `LocalCacheManager`（最小回归通过；落地：`8426d4f2`）。
- 补充进展（2026-01-07，S0.2 延伸（课程域基础设施归位推进），保持行为不变）：将 `UpdateCoursesTypeRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，行为不变；最小回归通过；落地：`12d16c6a`）。
- 补充进展（2026-01-07，S0.2 延伸（课程域基础设施归位推进），保持行为不变）：将 `UpdateCourseInfoRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，行为不变；最小回归通过；落地：`eb940498`）。
- 补充进展（2026-01-07，S0.2 延伸（课程域基础设施归位推进），保持行为不变）：将 `DeleteSelfCourseRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，行为不变；最小回归通过；落地：`73ed7c7d`）。
- 补充进展（2026-01-07，S0.2 延伸（课程域基础设施归位推进），保持行为不变）：将 `UpdateSingleCourseRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，行为不变）；为闭合编译期依赖，将 `CourInfTimeOverlapQuery` 归位到 `eva-infra-shared`（保持 `package` 不变）并在 `bc-course-infra` 补齐 `zym-spring-boot-starter-logging` 编译期依赖以闭合 `LogUtils`（最小回归通过；落地：`1a01e827`）。补充更新（2026-02-06，保持行为不变）：`CourInfTimeOverlapQuery` 已进一步从 `eva-infra-shared` 归位到 `bc-course/infrastructure`（保持 `package` 不变；Serena：引用面仅命中 `bc-course/infrastructure` 的 3 个适配器（`AssignEvaTeachersRepositoryImpl/UpdateSelfCourseRepositoryImpl/UpdateSingleCourseRepositoryImpl`）；最小回归通过；落地：`ea6c99e9`）。
- 补充进展（2026-01-07，S0.2 延伸（课程域基础设施归位推进），保持行为不变）：将 `UpdateSelfCourseRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，行为不变）；为闭合编译期依赖，当时将 `ClassroomOccupancyChecker` 临时归位到 `eva-infra-shared`（保持 `package` 不变；最小回归通过；落地：`3d1dd4f1`）。补充更新（2026-02-06，保持行为不变）：`ClassroomOccupancyChecker` 已进一步从 `eva-infra-shared` 归位到 `bc-course/infrastructure`（保持 `package` 不变；最小回归通过；落地：`b1db3422`）。
- 补充进展（2026-01-07，S0.2 延伸（课程域基础设施归位批量推进试点），保持行为不变）：按“选项 2（2 类同簇）”试点，将 `AddExistCoursesDetailsRepositoryImpl` 与 `AddNotExistCoursesDetailsRepositoryImpl` 从 `eva-infra` 批量归位到 `bc-course/infrastructure`（仅搬运文件，行为不变）；并试点引入 IDEA MCP `get_file_problems(errorsOnly=true)` 作为搬运后快速预检（不替代最小回归；最小回归通过；落地：`bd042ea9`）。
- 补充进展（2026-01-07，S0.2 延伸（课程域基础设施归位推进，按“选项 2（2 类同簇）”），保持行为不变）：将 `DeleteCourseRepositoryImpl` 与 `DeleteCoursesRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，行为不变；最小回归通过；落地：`df4ac6ca`）。
- 补充进展（2026-01-07，S0.2 延伸（课程域基础设施归位推进，按“选项 2（2 类同簇）”），保持行为不变）：将 `DeleteCourseTypeRepositoryImpl` 与 `UpdateCourseTypeRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，行为不变；最小回归通过；落地：`33844ce0`）。

**下一步建议（S0.1，保持行为不变；每步只改 1 个小包/小类簇）：**
1) 用 Serena 盘点 `eva-domain` 的 `import edu.cuit.client.*` 清单，并证伪“每个类型当前由哪个模块提供”（避免凭直觉改依赖）。
2) 聚焦一个高收益收敛口：**收敛 `eva-domain` 对 `bc-course`（应用层 jar）的编译期依赖面**。做法：把仍落在 `bc-course/application` 下、但本质是“边界协议对象”（`edu.cuit.client.*` 的 BO/CO/Query/Cmd）的小簇，逐步迁移到 `shared-kernel`（优先保持 `package` 不变以降风险）。
3) 当 Serena 证伪 `eva-domain` 不再引用 “仅由 `bc-course` 提供的 `edu.cuit.client.*` 类型” 后，再独立提交移除 `eva-domain/pom.xml` 对 `bc-course` 的依赖（保持行为不变；确保装配/编译闭合）。
4) 每步闭环：Serena → 最小回归 → commit → 同步三文档（保持行为不变）。

1) ✅ **评教任务发布写侧收敛**：把 `EvaUpdateGatewayImpl.postEvaTask` 收敛到 `bc-evaluation` 用例 + 端口，跨域副作用（消息/日志/缓存）按“事务提交后事件”固化（行为不变；落地提交：`8e434fe1/ca69b131/e9043f96`）。
2) ✅ **评教删除写侧收敛**：把 `EvaDeleteGatewayImpl.deleteEvaRecord/deleteEvaTemplate` 收敛到 `bc-evaluation`（行为不变；落地提交：`ea928055/07b65663/05900142`）。
3) ✅ **课程读侧渐进收敛**：为 `CourseQueryGatewayImpl` 引入 `QueryPort/QueryRepo`（先结构化，再考虑 CQRS 投影表；落地提交：`ba8f2003`）。
4) ✅ **评教读侧渐进收敛**：为 `EvaQueryGatewayImpl` 抽取 `EvaQueryRepo`，gateway 退化为委托壳（保持统计口径与异常文案不变；落地提交：`02f4167d`）。
5) **评教读侧进一步解耦**：按用例维度拆分 QueryService（任务/记录/统计/模板），将 query 端口逐步迁到 `bc-evaluation` 应用层，`eva-infra` 仅保留实现（行为不变）。  
   - 阶段性策略微调（2025-12-29）：允许“微调”（仅结构性重构；不改业务语义；缓存/日志/异常文案/副作用顺序完全不变）；短期优先把“评教读侧解耦（A → B）”主线推进到可验收里程碑，S0 折叠不再作为近期新增试点目标（避免分散注意力），仅做已在进行中的收尾与文档维护。
   - 补充进展（2025-12-29）：导出基类 `EvaStatisticsExporter` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（保持包名不变），并在 `bc-evaluation-infra` 补齐 `bc-course/bc-iam-contract` 依赖以闭合编译类型（保持行为不变；最小回归通过；落地：`e8ca391c`）。
   - 补充进展（2025-12-29）：导出装饰器 `FillAverageScoreExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（保持包名不变；保持行为不变；最小回归通过；落地：`4e150984`）。
   - 补充进展（2025-12-29）：导出装饰器 `FillEvaRecordExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（保持包名不变；保持行为不变；最小回归通过；落地：`b3afcb11`）。
   - 补充进展（2025-12-29）：导出装饰器 `FillUserStatisticsExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（保持包名不变；保持行为不变；最小回归通过；落地：`e83600f6`）。
   - 补充进展（2025-12-29）：统计导出工厂 `EvaStatisticsExcelFactory` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（导出异常文案/日志输出完全一致；保持行为不变；最小回归通过；落地：`5b2c2223`）。
   - 补充进展（2025-12-29）：统计导出端口装配已切换：`BcEvaluationConfiguration.evaStatisticsExportPort()` 已改为委托 `bc-evaluation-infra` 的 `EvaStatisticsExportPortImpl`（内部仍调用 `EvaStatisticsExcelFactory.createExcelData`；保持行为不变；最小回归通过；落地：`565552fa`）。
   - 补充进展（2025-12-29）：将 POI 工具类 `ExcelUtils` 从 `eva-app` 迁移到 `eva-infra-shared`（保持包名不变）并补齐 `poi/poi-ooxml` 依赖，为后续导出实现从 `eva-app` 归位到基础设施模块扫清“循环依赖”风险（保持行为不变；最小回归通过；落地：`04009c85`）。
   - 补充进展（2025-12-29）：在 `EvaStatisticsQueryUseCase` 内部收敛 `type` 分支分发为 `dispatchByType(...)`，减少重复分支判断，避免后续继续归位方法簇时出现分支口径漂移（保持行为不变；最小回归通过；落地：`38ce9ece`）。
   - 补充进展（2025-12-29）：统计导出 `exportEvaStatistics` 调用归位到 `EvaStatisticsQueryUseCase`：引入导出端口 `EvaStatisticsExportPort`（Bean 仍委托既有 `EvaStatisticsExcelFactory.createExcelData`），旧入口 `EvaStatisticsServiceImpl.exportEvaStatistics` 退化为纯委托壳（保持 `@CheckSemId` 触发点与异常文案/日志顺序不变；最小回归通过；落地：`0d15de60`）。
   - 下一步建议（统计导出基础设施归位，保持行为不变；每次只迁 1 个类 + 最小回归）：✅ 已完成 `BcEvaluationConfiguration.evaStatisticsExportPort()` 委托切换（`565552fa`）；✅ 已完成 `eva-app/pom.xml` 移除 `poi/poi-ooxml` Maven 直依赖（课表解析已迁移到 `bc-course-infra`，保持行为不变；落地：`383dbf33`）。
   - 补充进展（2025-12-29）：课表 Excel/POI 解析已进一步“端口化”：新增 `CourseExcelResolvePort`（`bc-course/application`）并由 `bc-course-infra` 提供适配器实现，`eva-app` 调用侧改为依赖端口，移除对解析实现的直耦合（保持行为不变；落地：`5a7cd0a0`）。
   - 下一步建议（主线切换，保持行为不变）：`bc-messaging` 已阶段性闭环（依赖收敛后半段证伪 + 运行时装配由 `start` 承接），本主线不继续；如需推进仅做 S0 结构折叠/依赖证伪（见 `NEXT_SESSION_HANDOFF.md` 0.9，保持行为不变）。
   - 进展：已拆分统计/导出、任务、记录、模板查询端口（`EvaStatisticsQueryPort` / `EvaTaskQueryPort` / `EvaRecordQueryPort` / `EvaTemplateQueryPort`），应用层开始迁移，行为保持不变；旧 `EvaQueryGatewayImpl` 已移除；并已引入 `bc-evaluation-infra` 承接评教读侧查询实现（QueryPortImpl + QueryRepo/Repository，保持包名/行为不变；落地：`be6dc05c`）。
   - 补充进展（2025-12-27）：为后续“按用例进一步收窄依赖类型”做准备，已细分统计 QueryPort：新增 `EvaStatisticsOverviewQueryPort/EvaStatisticsTrendQueryPort/EvaStatisticsUnqualifiedUserQueryPort`，并让 `EvaStatisticsQueryPort` `extends` 以上子端口（仅接口拆分，不改实现/不改装配；保持行为不变；落地：`a1d6ccab`）。
   - 补充进展（2025-12-28）：工程噪音收敛（dev 环境 MyBatis 日志）：将 `application-dev.yml` 中 MyBatis-Plus 的 `log-impl` 从 `StdOutImpl` 切换为 `Slf4jImpl`，避免 SQL 调试日志直出 stdout（仅 dev profile，生产不变；最小回归通过；落地：`cb3a4620`）。
   - 补充进展（2025-12-28）：工程噪音收敛（dev/test 非法入参打印）：将 `application-dev.yml/application-test.yml` 中 `common.print-illegal-arguments` 从 `true` 调整为 `false`，减少控制台噪音（仅 dev/test profile；不改业务逻辑；最小回归通过；落地：`21ba35dd`）。
   - 补充进展（2025-12-28）：已在 `EvaStatisticsServiceImpl` 落地“依赖类型收窄”：由聚合接口 `EvaStatisticsQueryPort` 改为按用例簇注入三个子端口（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`c19d8801`）。
	   - 补充进展（2025-12-28）：已在统计导出侧（`EvaStatisticsExporter`）落地“依赖类型收窄”：静态初始化中将统计端口由 `EvaStatisticsQueryPort` 收窄为 `EvaStatisticsOverviewQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；保持行为不变；最小回归通过；落地：`9b3c4e6a`）。
	   - 补充进展（2025-12-28）：统计导出链路子端口补齐：新增 `EvaStatisticsCountAbEvaQueryPort`，并让 `EvaStatisticsOverviewQueryPort` `extends` 该子端口（仅接口细分，不改实现/不改装配；保持行为不变；最小回归通过；落地：`24b13138`）。
	   - 补充进展（2025-12-28）：统计导出基类依赖类型收窄：将导出基类 `EvaStatisticsExporter` 静态初始化中获取统计端口的依赖类型从 `EvaStatisticsOverviewQueryPort` 收窄为 `EvaStatisticsCountAbEvaQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；保持行为不变；最小回归通过；落地：`7337d378`）。
	   - 补充进展（2025-12-28）：已开始将统计读侧用例归位到 `bc-evaluation`：新增 `EvaStatisticsQueryUseCase`（当前为委托壳，不改变分支/异常文案/阈值计算），并在 `BcEvaluationConfiguration` 完成装配；`EvaStatisticsServiceImpl` 退化为委托该用例（保持行为不变；最小回归通过；落地：`db09d87b`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分起步：新增得分子端口 `EvaRecordScoreQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`4e47ffe3`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordScoreQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`0c7e7d13`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：新增分页子端口 `EvaRecordPagingQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`e4f0efe9`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordPagingQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`76976c0b`）。
	   - 补充进展（2025-12-28）：记录读侧依赖类型收窄：`EvaRecordServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordPagingQueryPort/EvaRecordScoreQueryPort`（不改业务逻辑/异常文案；最小回归通过；落地：`39a4bafe`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：新增用户日志子端口 `EvaRecordUserLogQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`fcac9324`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordUserLogQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`1e025e48`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：新增按课程查询子端口 `EvaRecordCourseQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`e9034541`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordCourseQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`b8efeaf5`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：新增数量统计子端口 `EvaRecordCountQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`db876379`）。
		   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordCountQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`0d562206`）。
		   - 补充进展（2025-12-28）：记录导出链路子端口补齐：新增 `EvaRecordExportQueryPort`（组合 `EvaRecordCourseQueryPort/EvaRecordScoreQueryPort`），并让聚合端口 `EvaRecordQueryPort` `extends` 该子端口（仅新增接口+继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`5df35c36`）。
		   - 补充进展（2025-12-28）：记录导出链路依赖类型收窄：将导出基类 `EvaStatisticsExporter` 静态初始化中获取记录端口的依赖类型从 `EvaRecordQueryPort` 收窄为 `EvaRecordExportQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；保持行为不变；最小回归通过；落地：`682bf081`）。
		   - 补充进展（2025-12-28）：记录读侧依赖类型收窄：`UserServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordCountQueryPort`（不改业务逻辑/异常文案；补充单测；最小回归通过；落地：`8b24d2f8`）。
		   - 补充进展（2025-12-28）：记录读侧依赖类型收窄：`MsgServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordCountQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`147d486b`）。
		   - 补充进展（2025-12-28）：记录读侧依赖类型收窄：`UserEvaServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordUserLogQueryPort/EvaRecordScoreQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`80886841`）。
			   - 补充进展（2025-12-28）：任务读侧 QueryPort 细分起步：新增单任务信息子端口 `EvaTaskInfoQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`26b79c3a`）。
			   - 补充进展（2025-12-28）：任务读侧聚合端口继承子端口：让 `EvaTaskQueryPort` `extends EvaTaskInfoQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`56834293`）。
			   - 补充进展（2025-12-28）：任务读侧依赖类型收窄：`MsgServiceImpl` 由注入 `EvaTaskQueryPort` 收窄为 `EvaTaskInfoQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`7aa49e7f`）。
			   - 补充进展（2025-12-28）：任务读侧 QueryPort 细分起步：新增分页子端口 `EvaTaskPagingQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`f0a172d1`）。
			   - 补充进展（2025-12-28）：任务读侧聚合端口继承子端口：让 `EvaTaskQueryPort` `extends EvaTaskPagingQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`2fd9d24e`）。
				   - 补充进展（2025-12-28）：任务读侧 QueryPort 细分：新增本人任务/数量统计子端口 `EvaTaskSelfQueryPort/EvaTaskCountQueryPort`，并让 `EvaTaskQueryPort` `extends` 这些子端口（仅新增接口+继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`9d5064fc`）。
				   - 补充进展（2025-12-28）：任务读侧依赖类型收窄：`EvaTaskServiceImpl` 由注入 `EvaTaskQueryPort` 收窄为 `EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskInfoQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`4b22f059`）。
				   - 补充进展（2026-01-01）：任务读侧用例归位深化（分页）：新增 `EvaTaskQueryUseCase` 并将旧入口 `EvaTaskServiceImpl.pageEvaUnfinishedTask` 退化为纯委托壳，把“分页查询 + 实体→CO 组装 + 分页结果组装”归位到 UseCase（`@CheckSemId` 触发点仍保留在旧入口；保持行为不变；最小回归通过；落地：`d67f0ace`）。
				   - 补充进展（2026-01-02）：任务读侧用例归位深化（单任务详情）：将旧入口 `EvaTaskServiceImpl.oneEvaTaskInfo` 退化为纯委托壳，并把 “单任务查询 + 懒加载顺序对齐的实体→CO 组装” 归位到 `EvaTaskQueryUseCase`（异常文案不变；保持行为不变；最小回归通过；落地：`94736365`）。
				   - 补充进展（2026-01-02）：任务读侧用例归位深化（本人任务列表）：将旧入口 `EvaTaskServiceImpl.evaSelfTaskInfo` 的“任务列表查询 + 懒加载顺序对齐的实体→CO 组装”归位到 `EvaTaskQueryUseCase`；旧入口仍保留 `@CheckSemId` 与当前用户解析（`StpUtil` + `userQueryGateway`）并委托 UseCase（异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地：`1ac196c6`）。
					   - 补充进展（2025-12-28）：模板读侧 QueryPort 细分起步：新增分页/全量/按任务取模板子端口 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`，并让 `EvaTemplateQueryPort` `extends` 这些子端口（仅新增接口+继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`a14d3c53`）。
					   - 补充进展（2025-12-28）：模板读侧依赖类型收窄：`EvaTemplateServiceImpl` 由注入 `EvaTemplateQueryPort` 收窄为 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`b86db7e4`）。
					   - 补充进展（2026-01-01）：模板读侧用例归位深化（分页）：新增 `EvaTemplateQueryUseCase` 并将旧入口 `EvaTemplateServiceImpl.pageEvaTemplate` 退化为纯委托壳，把“实体→CO 组装 + 分页结果组装”归位到 UseCase（`@CheckSemId` 触发点仍保留在旧入口；时间格式/分页字段赋值顺序不变；保持行为不变；最小回归通过；落地：`afcb4ff7`）。
					   - 补充进展（2026-01-01）：模板读侧用例归位深化（按任务取模板）：将旧入口 `EvaTemplateServiceImpl.evaTemplateByTaskId` 退化为纯委托壳，并把 “按任务取模板 + 空结果兜底 JSON” 归位到 `EvaTemplateQueryUseCase`（`@CheckSemId` 触发点仍保留在旧入口；保持行为不变；最小回归通过；落地：`f98a9eed`）。
					   - 补充进展（2026-01-02）：模板读侧用例归位深化（全量模板列表）：将旧入口 `EvaTemplateServiceImpl.evaAllTemplate` 退化为纯委托壳，并把 “全量模板查询 + 结果组装” 归位到 `EvaTemplateQueryUseCase`（保持行为不变；最小回归通过；落地：`cd8e6ecb`）。
				   - 补充进展（2025-12-28）：模板读侧引用面盘点结论/证伪：使用 Serena 盘点 `EvaTemplateQueryPort` 在全仓库的引用面，除端口定义外仅剩实现侧 `EvaTemplateQueryPortImpl`；应用层未发现其它对聚合端口的注入点/调用点，因此模板主题的“端口细分 + 服务层依赖类型收窄”阶段可视为已闭合（保持行为不变；以 `NEXT_SESSION_HANDOFF.md` 的 0.9 证据记录为准）。
				   - 补充进展（2025-12-28）：统计读侧用例归位深化：将 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 的 `type` 分支选择归位到 `EvaStatisticsQueryUseCase.pageUnqualifiedUser`（`@CheckSemId` 触发点仍保留在旧入口；异常文案不变；保持行为不变；最小回归通过；落地：`22dccc70`）。
			   - 下一步建议（方向 A → B，保持行为不变）：先将记录/任务/模板按同套路做“子 QueryPort 接口 + `extends`”与 `eva-app` 注入类型收窄；并将 `EvaStatisticsQueryUseCase` 逐步从委托壳演进为“统计读侧用例编排落点”（每次只迁 1 个方法簇，且 `@CheckSemId` 触发点仍保留在旧入口）。
				   - 下一步建议（阶段性，保持行为不变）：评教读侧 D1 已覆盖 `eva-app/src/main/java/edu/cuit/app/service/impl/eva` 下 5 个入口（统计/记录/任务/模板/用户评教记录），后续建议将同套路复制到 `bc-course` 的读侧入口（优先 `ICourseServiceImpl` 的“纯查询”方法簇）。✅ 已起步：新增 `CourseQueryUseCase` 并将 `courseNum/courseTimeDetail/getDate` 退化为委托壳（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地：`4b06187f`）。✅ 进一步起步：新增 `CourseDetailQueryUseCase` 并将 `getCourseDetail` 退化为委托壳（异常文案保持不变；最小回归通过；落地：`d045c79e`）。✅ 进一步起步：新增 `TimeCourseQueryUseCase` 并将 `getTimeCourse` 退化为委托壳（`StpUtil.getLoginId()` 仍保留在旧入口，保持行为不变；最小回归通过；落地：`4454ecae`）。✅ 写侧起步：将 `allocateTeacher` 退化为委托壳并引入 `AllocateTeacherUseCase`（`AfterCommitEventPublisher` 发布顺序不变；最小回归通过；落地：`6e20721b`）。✅ 写侧起步：将 `deleteCourses` 退化为委托壳并引入 `DeleteCoursesEntryUseCase`（`AfterCommitEventPublisher` 发布顺序不变；最小回归通过；落地：`d53b287a`）。✅ 写侧继续：已完成 `ICourseServiceImpl.updateSingleCourse` 入口用例归位（保持两次 `StpUtil.getLoginId()` 调用位置/顺序与 AfterCommit 发布顺序完全不变；最小回归通过；落地以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。✅ 写侧继续：已完成 `ICourseServiceImpl.addNotExistCoursesDetails` 入口用例归位（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地：`5a73fb75`）。✅ 写侧继续：已完成 `ICourseServiceImpl.addExistCoursesDetails` 入口用例归位（保持行为不变；最小回归通过；落地：`a5a9c777`）。✅ 写侧继续：已完成 `IUserCourseServiceImpl.deleteSelfCourse` 入口用例归位（保持两次 `StpUtil.getLoginId()` 调用位置/顺序与 AfterCommit 发布顺序完全不变；最小回归通过；落地：`76845038`）。✅ 写侧继续：已完成 `IUserCourseServiceImpl.updateSelfCourse` 入口用例归位（保持两次 `StpUtil.getLoginId()` 调用位置/顺序与 AfterCommit 发布顺序完全不变，且 `CourseOperationMessageMode.TASK_LINKED` 参数不变；最小回归通过；落地：`2d1327d3`）。✅ 写侧继续：已完成 `IUserCourseServiceImpl.importCourse` 的 `importCourseFile(...)` 调用点端口化（保持解析/异常/AfterCommit 顺序不变；最小回归通过；落地：`054b511d`）。✅ 写侧继续：已完成 `ICourseDetailServiceImpl.updateCourse` 的 `updateCourse(...)` 调用点端口化（保持 `@CheckSemId`、异常转换与 AfterCommit 顺序不变；最小回归通过；落地：`bcf17d7f`）。✅ 写侧继续：已完成 `ICourseDetailServiceImpl.updateCourses` 入口用例归位（保持 `@CheckSemId/@Transactional` 触发点与异常转换不变；最小回归通过；落地：`849ed92e`）。✅ 写侧继续：已完成 `ICourseDetailServiceImpl.delete` 入口用例归位（保持 `@CheckSemId` 触发点与 `deleteCourse(...) → userId 查询 → publishAfterCommit(...)` 顺序不变；最小回归通过；落地：`e38463c2`）。✅ 写侧继续：已完成 `ICourseDetailServiceImpl.addCourse` 入口用例归位（保持 `@CheckSemId` 触发点与 `courseUpdateGateway.addCourse(semId)` 调用链不变；最小回归通过；落地：`5c989ace`）。✅ S0 收尾（依赖收窄）：已清理 `ICourseServiceImpl` 中残留的未使用注入依赖（`CourseQueryGateway/CourseUpdateGateway` 仅声明无调用点；保持行为不变；最小回归通过；落地：`9577cd85`）。✅ S0 收尾（依赖收窄）：已清理 `IUserCourseServiceImpl` 中残留的未使用注入依赖（`CourseDeleteGateway/MsgServiceImpl/MsgResult` 仅声明无调用点；保持行为不变；最小回归通过；落地：`402affc2`）。✅ S0 收尾（依赖收窄）：将 `IUserCourseServiceImpl.isImported` 的依赖从 `CourseUpdateGateway` 收敛为直接依赖 `IsCourseImportedUseCase`（原 gateway 本身已委托该用例；本次仅收窄注入依赖，不改业务语义；保持行为不变；最小回归通过；落地：`25aad45a`）。✅ S0（旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.updateCourseType`：新增 `UpdateCourseTypeEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`785974a6`）。✅ S0（旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.addCourseType`：新增 `AddCourseTypeEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`34e9a0a8`）。✅ S0（旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.updateCoursesType`：新增 `UpdateCoursesTypeEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`709dc5b6`）。✅ S0（旧 gateway 压扁为委托壳）：进一步压扁 `CourseDeleteGatewayImpl.deleteCourseType`：新增 `DeleteCourseTypeEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`cf747b9c`）。✅ S0（旧 gateway 压扁为委托壳）：进一步压扁 `CourseDeleteGatewayImpl.deleteCourse`：新增 `DeleteCourseGatewayEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`dfd977fe`）。✅ S0（旧 gateway 压扁为委托壳）：进一步压扁 `CourseDeleteGatewayImpl.deleteCourses`：新增 `DeleteCoursesGatewayEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`6428e685`）。✅ S0（旧 gateway 压扁为委托壳）：进一步压扁 `CourseDeleteGatewayImpl.deleteSelfCourse`：新增 `DeleteSelfCourseGatewayEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`c0268b14`）。✅ S0（旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.assignTeacher`：新增 `AssignTeacherGatewayEntryUseCase` 并让旧 gateway 不再构造命令（保持事务边界/异常文案/副作用顺序完全不变；最小回归通过；落地：`0b85c612`）。✅ S0（旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.updateCourse`：新增 `UpdateCourseGatewayEntryUseCase` 并让旧 gateway 不再构造 Command（保持事务边界/异常文案/副作用顺序完全不变；最小回归通过；落地：`c31df92c`）。下一步建议：继续压扁 `CourseUpdateGatewayImpl.updateSingleCourse`（Serena：调用点为 `UpdateSingleCoursePortImpl.updateSingleCourse`；每次只改 1 个方法，保持行为不变）。
			     - 记录主题建议顺序：✅ `MsgServiceImpl`（已收窄依赖：`EvaRecordCountQueryPort`）→ ✅ `UserEvaServiceImpl`（已收窄依赖：`EvaRecordUserLogQueryPort/EvaRecordScoreQueryPort`）→ 视测试可控性再处理导出/AI 报告链路对记录端口的依赖收窄（保持行为不变；相关类可能涉及 `StpUtil` 静态登录态，单测需提前规划“可重复”的登录态注入策略）。
			     - 任务主题建议顺序（保持行为不变）：✅ `MsgServiceImpl` 与 ✅ `EvaTaskServiceImpl` 的任务端口依赖类型已完成收窄；（模板主题）✅ 子 QueryPort 与 ✅ `EvaTemplateServiceImpl` 依赖类型收窄已完成。下一步建议：如需继续推进模板读侧解耦，可盘点是否存在其它引用 `EvaTemplateQueryPort` 的应用层类并逐一收窄（每次只改 1 个类 + 最小回归/可运行单测）。
	6) ✅ **IAM 写侧继续收敛**：`UserUpdateGatewayImpl.deleteUser` 已收敛到 `bc-iam`（含 LDAP 删除、角色解绑、缓存失效/日志等副作用，保持行为不变；落地提交：`5f08151c/e23c810a/cccd75a3/2846c689`）。
7) ✅ **系统管理读侧渐进收敛**：`UserQueryGatewayImpl` 的用户查询能力已收敛到 `bc-iam`（保持行为不变；落地提交：`3e6f2cb2/8c245098/92a9beb3`、`9f664229/38384628/de662d1c/8a74faf5`、`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`）。
8) ✅ **系统管理写侧继续收敛**：`RoleUpdateGatewayImpl.assignPerms/deleteMultipleRole` 与菜单变更触发的缓存失效（`MenuUpdateGatewayImpl.handleUserMenuCache`）已收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
   - 落地提交链：`f8838951/91d13db4/7fce88b8/d6e3bed1/4d650166/46e666f9`
9) ✅ **系统管理写侧继续收敛（菜单/角色写侧主链路）**：
   - 菜单写侧主链路（`MenuUpdateGatewayImpl.updateMenuInfo/deleteMenu/deleteMultipleMenu/createMenu`）已收敛到 `bc-iam`（保持行为不变；落地提交：`f022c415`）。
   - 角色写侧剩余入口（`RoleUpdateGatewayImpl.updateRoleInfo/updateRoleStatus/deleteRole/createRole`）已收敛到 `bc-iam`（保持行为不变；落地提交：`64fadb20`）。
10) **AI 报告 / 审计日志模块化（建议）**：启动 `bc-ai-report` / `bc-audit` 最小骨架，并优先挑选 1 条写链路按同套路收敛（保持行为不变）。
   - 进展：已完成骨架与组合根 wiring（提交点 A：`a30a1ff9`）；已完成审计日志写链路收敛（提交点 B：`b0b72263`）；已完成 AI 报告导出链路收敛（提交点 B2：`c68b3174`）；已完成导出旧入口进一步退化为纯委托壳（提交点 B3：`7f4b3358`）。
   - 补充进展（2025-12-27）：已将 AI 报告导出实现（`AiReportDocExportPortImpl` + `AiReportExporter`）从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；落地提交：`d1262c32`）。
   - 补充进展（2025-12-27）：已将 AI 报告 analysis 端口适配器 `AiReportAnalysisPortImpl` 从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；落地提交：`6f34e894`）。
   - 补充进展（2025-12-27）：已将 AI 报告 username → userId 查询端口适配器 `AiReportUserIdQueryPortImpl` 从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；落地提交：`e2a608e2`）。
   - 补充进展（2025-12-27）：已将 AI 相关基础设施 `edu.cuit.infra.ai.*`（模型 Bean 配置、提示词常量、消息工具等）从 `eva-infra` 归位到 `bc-ai-report`，并移除 `bc-ai-report` → `eva-infra` 编译期依赖（保持 `package` 不变；保持行为不变；落地提交：`e2f2a7ff`）。
   - 补充进展（2025-12-27）：已将 `bc-ai-report` 组合根 `BcAiReportConfiguration` 从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；Bean 定义与 `@Lazy` 环断策略不变；保持行为不变；落地提交：`58c2f055`）。
   - 补充进展（2025-12-27）：已将 `@CheckSemId` 注解 `edu.cuit.app.aop.CheckSemId` 从 `eva-app` 下沉到 `shared-kernel`（保持 `package` 不变；切面匹配表达式不变；保持行为不变；落地提交：`1c595052`）。
   - 补充进展（2025-12-27）：已将 AI 报告旧入口 `AiCourseAnalysisService` 从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持 `@Service/@CheckSemId` 触发点不变；保持行为不变；落地提交：`ca321a20`）。
   - 补充进展（2025-12-27）：已用 Serena 对全仓库“剩余落库/记录写链路”做证据化盘点并证伪（证据清单见 `NEXT_SESSION_HANDOFF.md` 的 0.9）；因此条目 25 的后续重点切换为 **S0 折叠 `bc-ai-report`**（仅搬运/依赖收敛，保持行为不变）。
11) ✅ **BC 自包含三层结构试点（`bc-iam`，阶段 1：适配器归属）**：已引入平铺过渡模块 `bc-iam-infra` 并接入组合根，且已将 `bciam/adapter/*` 端口适配器从 `eva-infra` 迁移到 `bc-iam-infra`（保持行为不变；落地提交：`42a6f66f/070068ec/03ceb685/02b3e8aa/6b9d2ce7/5aecc747/1c3d4b8c`）。
   - 说明：阶段 2（IAM DAL 抽离 + shared 拆分 + 去依赖）已完成，`bc-iam-infra` 已不再依赖 `eva-infra`（落地提交：`2ad911ea`；保持行为不变）。

### 10.4 术语澄清与最终目标结构（减少“gateway”混淆）

> 背景：项目早期参考 COLA 命名把大量类叫做 `*GatewayImpl`，但其中一部分事实上同时扮演了“应用入口 + DB 访问 + 副作用”，导致“gateway=DB gateway”的直觉与 DDD/六边形的职责划分冲突。
>
> 渐进式重构期间，本计划将严格按职责描述，而不被历史命名绑架：

- **UseCase（应用层用例）**：BC 的写侧入口（`bc-*/application/usecase`），不直接依赖 DB，只依赖 Port。
- **Port（应用层端口）**：UseCase 的出站依赖抽象（`bc-*/application/port`）。
- **Port Adapter（基础设施适配器）**：实现 Port、搬运旧 DB/副作用流程。过渡期可能放在 `eva-infra/.../bc*/adapter` 或历史过渡模块 `bc-*-infra`；**需求变更后**不再新增 `bc-*-infra` 平铺模块，新增适配器优先归位到目标 BC 的 `infrastructure` 子模块（见下方“最终目标结构”）。
- **旧 gateway（委托壳 / 兼容入口）**：历史 `eva-infra/.../*GatewayImpl`，对外接口不变（尤其缓存注解触发点），内部逐步退化为委托到 UseCase。

最终目标（你期望的“完美契合 DDD”）建议按 BC 自包含推进（优先 `bc-iam` 试点）：
- 每个 BC 至少在 **package** 上自包含三层：`<bc>.domain` / `<bc>.application` / `<bc>.infrastructure`。
- **需求变更（2025-12-24）**：每个 BC 在仓库中只占用 **一个顶层目录/聚合模块**（例如 `bc-iam/`），其内部按职责拆为 `domain/application/infrastructure` **子模块**（例如 `bc-iam/domain`、`bc-iam/application`、`bc-iam/infrastructure`），并由 `start`/组合根统一装配。历史上已存在的 `bc-iam-infra`、`bc-evaluation-infra` 等平铺模块作为过渡形态保留，后续按“折叠归位”里程碑迁入对应 BC 的内部子模块。
- **需求补充（2025-12-24）**：逐步拆解 `eva-client`：将 `edu.cuit.client.*` 下的 BO/CO/DTO 等对象按业务归属迁入对应 BC（优先放在 BC 的 `application` 子模块下的 `contract/dto` 包，避免领域层污染；**允许改包名**以完成归位）；确实跨 BC 复用的对象再沉淀到 shared-kernel；最终让 `eva-client` 退出主干依赖。
- `eva-*` 技术切片逐步退场：最终仅保留 shared-kernel、统一启动/装配与跨 BC 的极少量集成胶水（严格受控）。

### 10.5 `eva-*` 技术切片退场/整合到 BC 的路线（长期，保持行为不变）

#### 10.5.B 方案 B（严格）：彻底退场 `eva-*`（保持行为不变）

> 目标（严格）：最终仓库内仅保留 `bc-*` + `shared-kernel` + 组合根 `start`（以及必要的聚合父 pom）；跨 BC 只通过 `*-contract` + `shared-kernel` 的对外接口调用；不再存在任何 `eva-*` 模块、Maven 依赖或目录残留。

- DoD（可复现口径，任一不满足则视为未完成）：
  1) root reactor 清零：`rg -n '<module>eva-' pom.xml` 无命中
  2) Maven 依赖/坐标清零（排除父 POM `eva-backend`）：`rg -n -P '<artifactId>eva-(?!backend)' --glob '**/pom.xml' .` 无命中
  3) 目录退场：`fd -t d '^eva-' . -d 2` 无命中
- 推进顺序（遵守“每次只改 1 个类 / 1 个 XML / 1 个 pom”与“每步闭环”）：
  1) **先补依赖前置（pom）**：让目标类在迁移后仍可被依赖方编译/测试看见（例如把 bc 专属能力收敛到对应 BC 的 `*-infra`；跨 BC 共享能力先落到 `shared-kernel`）。
  2) **再逐文件搬运归位**：`eva-infra-shared`/`eva-infra-dal` 的残留按“归属最明确、引用面最窄”优先；搬运时保持 `package`/类内容/异常文案/副作用顺序完全不变。
  3) **后置移除旧依赖（pom）**：Serena 证据化 + `rg` 兜底证据确认“无引用面”后，再移除依赖方对 `eva-*` 的编译期依赖。
  4) **最后 reactor 退场与目录清理**：当且仅当 1~3 全部证据化满足，再移除 `<module>eva-*` 并按“一次只改 1 个文件”删除残留目录文件，确保可回滚。

> 回答“什么时候可以把 `eva-*` 模块整合进各业务 `bc-*`”的统一口径：**不是某个固定日期，而是一组可验证的前置条件**。避免在入口/装配尚未收敛时“硬搬模块”导致装配顺序、切面触发点或副作用顺序漂移。

**阶段定义（从“可以开始”到“可以移除模块”）：**

1) **可以开始做整合（进入 S1）的前置条件**
   - ✅ 业务入口已按“用例 + 端口 + 端口适配器 + 委托壳”模式持续推进，且每次只迁 1 个入口方法簇并闭环（Serena → 最小回归 → 提交 → 三文档同步）。
   - ✅ 核心 BC 的结构折叠（S0）已完成或接近完成（`bc-iam/bc-evaluation/bc-course/bc-template/bc-ai-report/bc-audit` 已完成阶段性折叠；`bc-messaging` 尚未折叠时不建议启动“全量整合 eva-*”）。
   - ✅ 组合根装配责任清晰：运行时装配以 `start` 为主，`eva-app` 仅作为过渡装配层（可逐步抽薄，但不要在同一提交里同时“迁入口 + 迁装配”）。

2) **可以移除 `eva-app` / `eva-adapter` 的判定标准（建议的 DoD）**
   - `eva-adapter`：Controller 仅承载 HTTP 协议适配与参数校验；业务编排已全部进入对应 `bc-*/application`（或由 `bc-*` 的入口类承接），并且 Controller 不再直接依赖 `eva-infra` 的实现细节。
     - 推荐的“可复现证据口径”（保持行为不变）：`fd -t f 'Controller\\.java$' eva-adapter/src/main/java | wc -l` 逐步收敛为 0；且 `rg -n '<module>eva-adapter</module>' pom.xml` 命中为 0（reactor 移除后），并确保组合根装配责任已由 `start` 或对应 BC 的 configuration 承接。
   - `eva-app`：不再包含任何“业务入口实现”（大量 `*ServiceImpl` 只剩委托壳或已归位），`@CheckSemId` 触发点与 `StpUtil.getLoginId()` 调用次数/顺序在迁移后仍可逐项对照证伪；装配要么迁入 `start`，要么迁入对应 BC 的 configuration（保持 Bean 名称与初始化顺序不变）。

3) **可以移除 `eva-domain` / `eva-infra*` / `eva-base*` 的判定标准（建议的 DoD）**
   - 前置：相关能力已按 BC 归位（domain/application/infrastructure）或下沉到 `shared-kernel`（仅限跨 BC 协议/横切通用），且依赖方已完成编译期依赖收敛（先 Serena 证据化盘点，再逐个 `pom.xml` 收敛，保持行为不变）。
   - 证据口径（保持行为不变）：`rg -n '<module>eva-' pom.xml` 命中为 0，且 `rg -n '<artifactId>eva-(domain|infra|infra-shared|infra-dal|base|base-common|base-config)</artifactId>' --glob '**/pom.xml' .` 不再出现“依赖方 dependency 声明”（允许剩余模块自身 artifactId 声明）；组合根 `start/pom.xml` 也不再需要显式 `eva-infra(runtime)` 兜底。

> **口径澄清（回答“什么时候可以把 eva-* 全部整合进 bc-*”）**：建议把“整合”拆成两层目标，避免把共享技术模块误当成业务 BC 的一部分硬塞进去导致循环依赖：
>
> 1) **业务整合（推荐优先）**：把“业务域类型/接口”从 `eva-domain` 逐类归位到对应 `bc-*/domain`（保持 `package` 不变），并在证伪无引用后移除 `eva-domain`；这一步完成后，业务层面的 “eva-* 退场” 已达成大半。
> 2) **命名整合（可选，后置）**：`eva-infra-dal/eva-infra-shared/eva-base` 多为跨 BC 的共享技术能力（DAL/Convertor/通用工具/配置），不建议强行归入某个单一 BC；若目标是“仓库里不再出现 eva- 前缀模块”，更稳妥的做法是 **先把业务相关实现搬走/瘦身**，再评估“保留为共享平台模块并改名（artifactId 变更）”或“进一步下沉到 shared-kernel/各 BC infrastructure”。

**方案 1（业务整合优先）的可执行规划（2026-02-12 起采用，保持行为不变）**

> 目标定义：先达成“BC 相互独立（业务维度）”—— **跨 BC 交互只允许通过 `*-contract` 的端口/DTO（或共享协议 `shared-kernel`）**，不允许通过“直连对方表/直依赖对方基础设施实现类”完成业务；平台共享能力（DAL/Convertor/通用工具/配置）可以暂留，但其表面积必须持续收敛并保持“无业务语义”。

1) **里程碑 M0：统一验收口径（度量先行）**
   - DoD（可复现证据）：
     - reactor 模块快照：`rg -n '<module>eva-' pom.xml`
     - 依赖方快照：`rg -n '<artifactId>eva-(infra-dal|infra-shared|base-common|base-config)</artifactId>' --glob '**/pom.xml' bc-* | head`
     - 存量快照：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`、`if [ -d eva-infra-dal/src/main/resources ]; then fd -t f -e xml . eva-infra-dal/src/main/resources | wc -l; else echo 0; fi`、`fd -t f -e java . eva-infra-shared/src/main/java | wc -l`（注意：`eva-infra-dal/src/main/resources` 目录可能不存在，按“目录不存在=0”口径）

2) **里程碑 M1：跨 BC 直连清零（持续滚动，写侧优先）**
   - 做法：对每一个“跨 BC 直连表/Mapper/DO”的点，按“每次只改 1 个类”收敛为调用目标 BC 的 `*-contract` 端口（端口适配器落在目标 BC `infrastructure`，内部原样复刻 SQL/装配逻辑；缓存/日志/异常文案/副作用顺序不变）。
   - 验收：Serena 证伪指定范围无目标 Mapper/DO 引用点（例如已完成的 IAM `sys_user_role/sys_role` 清零口径）。

3) **里程碑 M2：依赖边界收敛到 contract（逐 pom，避免环依赖）**
   - 做法：依赖方 `pom.xml` 只保留“闭合编译/装配所需”的依赖；跨 BC 依赖一律优先收敛为 `*-contract` 或 `shared-kernel`，禁止依赖对方 `infrastructure` 模块。
   - 验收：对每个被收敛的依赖，保留 Serena 证据化“引用面为 0”的结论，并以最小回归闭环。

4) **里程碑 M3：平台模块治理（不强行塞进单一 BC，先瘦身再改名）**
   - `eva-infra-dal`：继续按“仅单 BC 引用才允许归位”的规则拆散 Mapper/DO/XML（每次只改 1 个类或 1 个 XML）；无法证伪为“单 BC 引用”的，暂留为共享 DAL 能力。
   - `eva-infra-shared`：持续把“仅单 BC 引用”的支撑类/Convertor 归位到目标 BC（或下沉到 `eva-infra-dal/shared-kernel`），并记录阻塞点（典型：跨 BC Convertor、LDAP 风险簇）。
   - `eva-base-common`：作为跨 BC 协议/枚举的承载，优先下沉到 `shared-kernel`（保持 `package` 不变），再逐个依赖方 `pom.xml` 收敛；避免在“业务整合尚未稳定”阶段引入大范围依赖变更。

5) **可选里程碑 M4：命名整合（仓库不出现 eva- 前缀）**
   - 前置：先完成 M1/M2/M3（平台模块已“无业务语义 + 低表面积 + 依赖边界清晰”）。
   - 做法：评估将平台模块统一改名为 `platform-*`/`shared-*`（artifactId 变更）或进一步下沉；此项不作为“BC 业务独立”的阻塞条件。

  - 现状评估（更新至 2026-02-19，保持行为不变）：
    - `eva-app`：已完成退场闭环（源码清零 + 组合根 `start` 去依赖 + root reactor 移除 + 删除 `eva-app/pom.xml`）；无需再围绕 `eva-app` 做依赖收敛或入口迁移。
    - `eva-adapter`：已完成退场闭环（残留 Controller 清零 + 组合根去依赖 + root reactor 移除 + 删除模块 pom）；证据口径：`fd -t f 'Controller\\.java$' eva-adapter/src/main/java | wc -l` 为 0，且 `rg -n '<module>eva-adapter</module>' pom.xml` 命中为 0，且 `rg -n '<artifactId>eva-adapter</artifactId>' --glob '**/pom.xml' .` 命中为 0（保持行为不变）。
		    - `eva-infra-dal`：已从 root reactor 退场，且已删除 `eva-infra-dal/pom.xml`；当前目录存量剩余 `0` 个 Java + `0` 个 XML 未归位（可复现口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l` + `if [ -d eva-infra-dal/src/main/resources ]; then fd -t f -e xml . eva-infra-dal/src/main/resources | wc -l; else echo 0; fi`；注意：`eva-infra-dal/src/main/resources` 目录可能不存在，按“目录不存在=0”口径）。`EntityFactory` 已下沉到 `shared-kernel`（保持行为不变；详见 10.2/4.2）。`course_type` 候选已完成两刀：`CourseTypeMapper`（落地：`241b75de`）+ `CourseTypeMapper.xml`（落地：`158f0bd2`）已归位到 `bc-course/infrastructure`（保持 `package/namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变）。课程域 `subject` 候选已补齐 1 刀：`SubjectMapper.xml`（落地：`92374d53`）已归位到 `bc-course/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变）。`sys_menu` 候选已完成两刀：`SysMenuMapper.xml`（落地：`920c17d1`）+ `SysMenuMapper`（落地：`b53615e5`）已归位到 `bc-iam/infrastructure`（保持 `package/namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变）。评教 `eva_task` 候选已补齐 2 刀：`EvaTaskMapper.xml`（落地：`ad2e7d25`）+ `EvaTaskMapper`（落地：`b7049e4c`）已归位到 `bc-evaluation/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变）。评教 `form_record` 候选已补齐 1 刀：`FormRecordMapper`（落地：`8927067c`）已归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.eva` 不变；最小回归通过）。`sys_role` 候选已补齐 2 刀：`SysRoleMapper`（落地：`60b87404`）+ `SysRoleMapper.xml`（落地：`aa1d7c6b`）已归位到 `bc-iam/infrastructure`（保持 `package/namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变）。`sys_user_role` 候选已补齐 2 刀：`SysUserRoleMapper`（落地：`1f93141c`）+ `SysUserRoleMapper.xml`（落地：`0cd5da04`）已归位到 `bc-iam/infrastructure`（保持 `package/namespace/resultMap type` 与资源路径 `mapper/**` 不变）。`sys_user` 候选已补齐 1 刀：`SysUserMapper.xml`（落地：`3dad6ef7`）已归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变）。
			    - `eva-infra-shared`：已完成退场闭环（保持行为不变）：源码已清零（Java=`0`；口径：`fd -t f -e java . eva-infra-shared/src/main/java | wc -l`），且 `UserConverter` 已按“最小 Port 收敛跨 BC 使用点 → 单类搬运归位”的路线归位到 `bc-iam/infrastructure`（详见 `NEXT_SESSION_HANDOFF.md` 0.9/0.10 与 10.2 的补充进展）；依赖方逐 pom 去依赖已完成，root reactor 已移除 module，且已删除 `eva-infra-shared/pom.xml`。目录清理可后置为单独一刀（若为空目录）。
    - `eva-base*`：作为共享底座仍存在（root reactor 仍包含 `eva-base`），`eva-base-common` 源码已清零（`GenericPattern/LogModule` 已下沉到 `shared-kernel`，Java=0），但仍有 BC 直依赖（例如 `bc-iam/contract`、`bc-iam/infrastructure` 仍显式依赖 `eva-base-common`）。当前主线应转为“逐 pom 去依赖 → reactor 退场 → 删除 pom”（保持行为不变）。
    - `eva-base`：当前已成为“彻底退场 `eva-*`（方案 B）”的唯一阻塞点。处理路径应严格按“小步迁移/证伪引用 → 再收敛单个 pom → 再评估移除模块”的节奏推进，避免一次性移除引发编译边界漂移（保持行为不变）。
	      - 可复现现状口径（更新至 2026-02-15，保持行为不变）：
		        - root reactor 仍包含：`eva-base`（口径：`rg -n '<module>eva-' pom.xml`；`eva-infra-dal`/`eva-infra-shared` 已从 reactor 退场，保持行为不变）。
	        - 已闭环快照复核（保持行为不变）：`msg_tip`/`sys_role_menu`/`course_type_course` 相关 `Mapper/DO/XML` 均已在目标 BC 内各命中 1，且 `eva-infra-dal` 下 0 命中（口径：`fd -t f ... | wc -l`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
	        - `eva-domain`：已从 root reactor 退场且编译期依赖方已清零（口径：`rg -n '<artifactId>eva-domain</artifactId>' --glob '**/pom.xml' .` 预期无命中）。
	        - ✅ 依赖收敛（单 pom，保持行为不变）：已在 Serena 证伪 `eva-domain/src/main/java` 无课程域引用面后，移除 `eva-domain/pom.xml` 对 `bc-course-domain` 的编译期依赖（落地：`ec4107e4`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
        - ✅ 逐类归位（保持行为不变）：已将 `CourOneEvaTemplateEntity/EvaTaskEntity/EvaRecordEntity` 从 `eva-domain` 归位到 `bc-evaluation-domain`（保持 `package` 不变；详见 `NEXT_SESSION_HANDOFF.md` 0.9），使 `eva-domain` 的“课程域引用面”彻底归零并为后续继续搬运 `edu.cuit.domain.gateway.eva.*` 打通编译闭合前置。
        - ✅ 编译闭合补强（2026-02-06，保持行为不变）：已在 `bc-evaluation/domain/pom.xml` 增加 `spring-context(provided)`，用于承接 `edu.cuit.domain.gateway.eva.*` 上的 `@Component` 注解（落地：`132f6fc0`）。
        - IAM 并行（10.3）侧：依赖方对 `UserQueryGateway` 的编译期依赖已阶段性收敛为 `bc-iam-contract` 最小 Port；当前在 `bc-*` 范围内应不再出现 `UserQueryGateway` 引用（可复现口径：`rg -n \"\\bUserQueryGateway\\b\" --glob \"*.java\" bc-* | head` 应命中为 0）。
        - ✅ 编译闭合前置（2026-02-03，保持行为不变）：为后续在统计导出侧（`EvaStatisticsExporter`）保持 `SpringUtil.getBean(...)` 次数/顺序不变地完成依赖收敛，在 `bc-iam-contract` 新增组合端口 `UserAllUserIdAndEntityByIdQueryPort`，并让 `bc-iam-infra` 的 `UserAllUserIdQueryPortImpl` 同时实现 `findById`（内部仍委托旧 `UserQueryGateway.findAllUserId/findById` 以保持缓存/切面触发点不变）；最小回归通过；落地：`1d6624d4`。
        - ✅ 编译闭合前置（2026-02-03，保持行为不变）：为后续将 `bc-iam/application`（如 `ValidateUserLoginUseCase`）去 `UserQueryGateway` 编译期依赖做前置，在 `bc-iam-contract` 新增最小端口 `UserEntityByUsernameQueryPort`（返回 `Optional<?>` 以避免 Maven 循环依赖；后续由端口适配器内部委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变）；最小回归通过；落地：`02827d19`。
        - ✅ 编译闭合前置（2026-02-03，保持行为不变）：为后续将 `bc-iam/infrastructure` 的 `UserServiceImpl` 去 `UserQueryGateway` 编译期依赖做前置，在 `bc-iam-contract` 新增 `UserDirectoryPageQueryPort`（原误命名为 `UserDirectoryQueryPort`，与 `bc-iam/application` 同名端口冲突已更名；涵盖 `page/allUser/findAllUsername`；分页返回 `PaginationResultEntity<?>`，避免 contract 暴露旧领域实体触发 Maven 循环依赖风险）；最小回归通过；落地：`d7e7216e`。
        - ✅ 编译闭合前置（2026-02-03，保持行为不变）：已在 `bc-iam-infra` 新增 `UserDirectoryPageQueryPortImpl`，内部委托旧链路以保持缓存/切面触发点不变（后续注入已收敛为 `UserQueryCacheGateway`，见 2026-02-04 推进记录；用于闭合后续 `UserServiceImpl` 依赖收敛后的 Spring 装配）；最小回归通过；落地：`524d7ba3`。
        - ✅ 已完成（2026-02-03，保持行为不变）：已将 `bc-iam/infrastructure` 的 `UserServiceImpl` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 端口 `UserEntityByIdQueryPort/UserEntityByUsernameQueryPort/UserBasicQueryPort/UserDirectoryPageQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway` 以保持缓存/切面触发点不变；方法调用次数/顺序不变；异常文案不变）；最小回归通过；落地：`cc6f6d63`。
        - ✅ 编译闭合前置（2026-02-03，保持行为不变）：已在 `bc-iam-infra` 新增 `UserEntityByUsernameQueryPortImpl`，内部委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变（用于闭合后续 `ValidateUserLoginUseCase` 依赖收敛后的 Spring 装配）；最小回归通过；落地：`5c18aa08`。
        - ✅ 已完成（2026-02-03，保持行为不变）：已将 `bc-iam/application` 的 `ValidateUserLoginUseCase` 依赖从 `UserQueryGateway` 收敛为 `bc-iam-contract` 最小端口 `UserEntityByUsernameQueryPort`（内部仍由端口适配器委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变；异常文案与分支顺序不变）；为确保可回滚，保留一个 `@Deprecated` 旧构造仅用于过渡；最小回归通过；落地：`66329367`。
        - ✅ 已完成（2026-02-03，保持行为不变）：在 Serena 证实 `ValidateUserLoginUseCase` 的调用点已全部改走 Port 构造后，移除其 `@Deprecated` 旧构造与对 `UserQueryGateway` 的编译期依赖，使 `bc-iam/application` 的登录校验用例完全不再编译期绑定旧 gateway；最小回归通过；落地：`1b90f641`。

        - ✅ 补充进展（2026-02-20，保持行为不变，读侧前置，单类）：已在 `bc-course/infrastructure` 补齐端口适配器 `CourseIdsByTeacherIdAndSemesterIdQueryPortImpl`（内部仅委托 `CourseMapper.selectList(eq teacher_id, semester_id)` 并映射 `CourseDO::getId` 返回课程ID列表，保持结果顺序/空值语义不变；最小回归通过；落地：`7c8fc78f`）。
        - ✅ 补充进展（2026-02-20，保持行为不变，读侧收敛，单类）：收敛评教统计读侧调用点：`bc-evaluation/infrastructure` 的 `EvaStatisticsQueryRepository.getEvaEdNumByTeacherId` 已在 `semId!=null` 分支改为调用 `CourseIdsByTeacherIdAndSemesterIdQueryPort` 获取课程ID列表（保持查询条件/结果顺序/空值语义不变；最小回归通过；落地：`c3dd9744`）。
        - ✅ 补充进展（2026-02-20，保持行为不变，读侧收敛，单类）：收敛评教记录读侧调用点：`bc-evaluation/infrastructure` 的 `EvaRecordQueryRepository.getEvaEdLogInfo` 已在 `courseId` 为空且 `semId!=null` 分支改为调用 `CourseIdsByTeacherIdAndSemesterIdQueryPort` 获取课程ID列表（保持查询条件/结果顺序/空值语义不变；最小回归通过；落地：`8c8a4ece`）。
        - ✅ 补充进展（2026-02-20，保持行为不变，读侧收敛，单类）：收敛评教统计读侧调用点：`bc-evaluation/infrastructure` 的 `EvaStatisticsQueryRepository.getEvaEdNumByTeacherId` 已在 `semId==null` 分支改为调用 `CourseIdsByTeacherIdQueryPort` 获取课程ID列表（保持查询条件/结果顺序/空值语义不变；最小回归通过；落地：`35b720e5`）。
        - ✅ 补充进展（2026-02-20，保持行为不变，读侧收敛，单类）：收敛评教记录读侧调用点：`bc-evaluation/infrastructure` 的 `EvaRecordQueryRepository.getEvaEdLogInfo` 已在 `courseId` 为空且 `semId==null` 分支改为调用 `CourseIdsByTeacherIdQueryPort` 获取课程ID列表（保持查询条件/结果顺序/空值语义不变；最小回归通过；落地：`510a4962`）。
        - ✅ 已完成（2026-02-03，保持行为不变）：已将登录入口 `bc-iam-infra` 的 `UserAuthServiceImpl` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 最小端口 `UserEntityByUsernameQueryPort`（调用顺序不变：仍先 `ValidateUserLoginUseCase.execute` 再 `StpUtil.login`；内部仍委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变；异常文案不变）；最小回归通过；落地：`48d0eb7e`。
        - ✅ 已完成（2026-02-03，保持行为不变）：清理 `DepartmentGatewayImpl` 中未使用的 `UserQueryGateway` import，使该类不再编译期依赖旧 gateway（仅删 import；最小回归通过）；落地：`f29572d2`。
        - ✅ 编译闭合前置（2026-02-02，保持行为不变）：为后续收敛 `bc-messaging` 的 `MessageQueryPortImpl` 对 `UserQueryGateway` 的编译期依赖，在 `bc-messaging/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（仅显式化依赖边界；最小回归通过；落地：`16a90a5e`）。
        - ✅ 已完成（2026-02-02，保持行为不变）：已在 `bc-iam-contract` 新增最小端口 `UserEntityByIdQueryPort` 并在 `bc-iam-infra` 补齐 `UserEntityByIdQueryPortImpl`（内部委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变），并将 `bc-messaging` 的 `MessageQueryPortImpl` 从依赖 `UserQueryGateway` 收敛为依赖该端口（异常文案不变；最小回归通过；落地：`7875e09e` / `2ea7d39f` / `17509393`）。
        - ✅ 已完成（2026-02-03，保持行为不变）：已将 `bc-evaluation/infrastructure` 的 `EvaTaskServiceImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort`（异常文案/副作用顺序不变；最小回归通过；落地：`72bd00d9`）。
        - ✅ 已完成（2026-02-03，保持行为不变）：已将 `bc-evaluation/infrastructure` 的统计导出基类 `EvaStatisticsExporter` 去 `UserQueryGateway` 编译期依赖，改为依赖 `bc-iam-contract` 组合端口 `UserAllUserIdAndEntityByIdQueryPort`（通过 `UserQueryGatewayFacade` 保持 `userQueryGateway.findAllUserId/findById` 调用形态不变，内部仍委托旧 `UserQueryGateway` 以保持缓存/切面触发点不变；且 `SpringUtil.getBean(...)` 次数/顺序不变）；最小回归通过；落地：`92c6554a`。
        - ✅ 已完成（2026-02-04，保持行为不变）：将 `bc-evaluation/infrastructure` 的统计导出基类 `EvaStatisticsExporter` 去 `UserEntity` 编译期依赖：不再 `import UserEntity`，改为对 `UserAllUserIdAndEntityByIdQueryPort.findById` 返回的 `Optional<?>` 做运行时类型判定（按类名/父类链）后再 `Optional.of(...)`，以保持历史分支语义与 `SpringUtil.getBean(...)` 次数/顺序不变；最小回归通过；落地：`4f4b190b`。
        - ✅ 已完成（2026-02-04，保持行为不变）：在 `eva-infra-shared` 的 `EvaConvertor` 增加桥接方法 `toEvaTaskEntityWithTeacherObject(...)`，用于后续让依赖方在不编译期引用 `UserEntity` 的情况下复用既有 `ToEvaTaskEntity` 映射逻辑（仅做类型桥接，不改变 teacher/courInf Supplier 的调用时机与次数）；最小回归通过；落地：`a8934ab1`。
        - ✅ 已完成（2026-02-04，保持行为不变）：在 `eva-infra-shared` 的 `UserConverter` 增加桥接方法 `toUserEntityObject(...)` + `userIdOf(Object)`（仅做类型桥接，不改变 roles Supplier 的调用时机与次数）；最小回归通过；落地：`c173c7c2`。
        - ✅ 已完成（2026-02-04，保持行为不变）：为后续让 `bc-iam-infra` 的 `UserBasicQueryPortImpl` 去 `UserEntity` 编译期依赖（缓存命中时仍需读取 username/status），在 `eva-infra-shared` 的 `UserConverter` 增加桥接方法 `usernameOf(Object, boolean)` + `statusOf(Object, boolean)`（内部仍强转 `UserEntity` 调 getter；并通过“无业务意义形参”规避 MapStruct 编译期歧义）；最小回归通过；落地：`485cc5fb`。
        - ✅ 已完成（2026-02-04，保持行为不变）：将 `bc-iam-infra` 的 `UserBasicQueryPortImpl` 去 `UserEntity` 编译期依赖：缓存命中时从 `Optional<?>` 取值并改走 `UserConverter.userIdOf/usernameOf/statusOf` 桥接方法读取字段（缓存命中/回源顺序与异常文案不变）；最小回归通过；落地：`42af63a3`。
        - ✅ 已完成（2026-02-04，保持行为不变）：将 `bc-iam-infra` 的 `UserEntityQueryPortImpl` 去 `UserEntity` 编译期依赖：对外签名与 `bc-iam/application` 端口口径一致（`Optional<?>/PaginationResultEntity<?>`），内部仍通过 `UserConverter.toUserEntityObject(...)` 承接旧映射与角色/菜单装配逻辑，查询/排序/异常文案不变；最小回归通过；落地：`e8acbad3`。
        - ✅ 已完成（2026-02-04，保持行为不变）：为后续让 `bc-iam-infra` 的 `UserEntityByUsernameQueryPortImpl` 去 `UserEntity` 编译期依赖（权限/角色列表计算需读取 roles），在 `eva-infra-shared` 的 `UserConverter` 增加桥接方法 `rolesOf(Object, boolean)`（内部仍强转 `UserEntity` 调 `getRoles`；并通过“无业务意义形参”规避 MapStruct 编译期歧义）；最小回归通过；落地：`81408507`。
        - ✅ 已完成（2026-02-04，保持行为不变）：将 `bc-iam-infra` 的 `UserEntityByUsernameQueryPortImpl` 去 `UserEntity` 编译期依赖：`findStatusByUsername/findPermissionListByUsername/findRoleListByUsername` 改走 `UserConverter.statusOf/rolesOf` 桥接读取字段（保持仍委托旧 `UserQueryGateway.findByUsername` 以触发历史缓存/切面；异常与分支顺序不变）；最小回归通过；落地：`44d51d3e`。
        - ✅ 已完成（2026-02-04，保持行为不变）：为后续让 `bc-iam-infra` 的 `UserServiceImpl` 去 `UserEntity` 编译期依赖（返回用户信息需复用映射），在 `bc-iam-infra` 的 `UserBizConvertor` 增加桥接方法 `toUserDetailCOObject(Object)` / `toUnqualifiedUserInfoCOObject(Object, Integer)`（内部仍强转 `UserEntity`，仅做类型桥接，尽量保持历史空值/异常表现一致）；最小回归通过；落地：`c2454ab4`。
        - ✅ 已完成（2026-02-04，保持行为不变）：为后续让 `bc-iam-infra` 的 `RouterDetailFactory/UserServiceImpl` 去 `UserEntity` 编译期依赖，在 `eva-infra-shared` 的 `UserConverter` 增加桥接方法 `nameOf(Object, boolean)` / `permsOf(Object, boolean)` / `castUserEntityObject(Object, boolean)`（内部仍强转 `UserEntity` 调 getter；并通过“无业务意义形参”规避 MapStruct 编译期歧义；`castUserEntityObject` 用于尽量保持历史 ClassCastException 触发点一致）；最小回归通过；落地：`95c7bd8b`。
        - ✅ 已完成（2026-02-04，保持行为不变）：在 `eva-infra-shared` 的 `CourseConvertor` 增加桥接方法 `toCourseEntityWithTeacherObject(...)`（仅做类型桥接，不改变 teacher Supplier 的调用时机与次数）；最小回归通过；落地：`858521da`。
        - ✅ 已完成（2026-02-04，保持行为不变）：将 `bc-evaluation/infrastructure` 的 `EvaTaskQueryRepository` 去 `UserEntity` 编译期依赖（不改变 DB 查询/遍历顺序；异常文案不变）；最小回归通过；落地：`7f198610`。
        - ✅ 已完成（2026-02-04，保持行为不变）：将 `bc-evaluation/infrastructure` 的 `EvaRecordQueryRepository` 去 `UserEntity` 编译期依赖（不改变 DB 查询/遍历顺序；异常文案不变）；最小回归通过；落地：`9cbcb858`。
        - ✅ 已完成（2026-02-04，保持行为不变）：在 `MsgConvertor` 增加桥接方法 `toMsgEntityWithUserObject(...)`，用于后续让 `bc-messaging` 的 `MessageQueryPortImpl` 去 `UserEntity` 编译期依赖（仅做类型桥接，不改变 sender/recipient Supplier 的调用时机与次数）；最小回归通过；落地：`8254a430`。（注：`MsgConvertor` 当前已归位到 `bc-messaging`，保持 `package` 不变。）
        - ✅ 已完成（2026-02-04，保持行为不变）：已将 `bc-messaging` 的 `MessageQueryPortImpl` 去 `UserEntity` 编译期依赖：调用侧改走 `msgConvertor.toMsgEntityWithUserObject(...)` 并移除 `UserEntity` import（异常文案不变；`findById` 调用次数/顺序不变）；最小回归通过；落地：`51301d23`。
        - ✅ 已完成（2026-02-04，保持行为不变）：在 `eva-infra-shared` 的 `LogConverter` 增加桥接方法 `toLogEntityWithUserObject(...)`，用于后续让 `bc-audit` 的 `LogGatewayImpl` 去 `UserEntity` 编译期依赖（仅做类型桥接，不改变 user 获取时机与次数）；最小回归通过；落地：`8fa053ed`。
        - ✅ 已完成（2026-02-04，保持行为不变）：将 `bc-audit` 的 `LogGatewayImpl` 去 `UserEntity` 编译期依赖：调用侧改走 `userConverter.toUserEntityObject(...)` + `logConverter.toLogEntityWithUserObject(...)` 并移除 `UserEntity` import（异常文案/查询/遍历顺序不变）；最小回归通过；落地：`a86f6520`。
        - ✅ 已完成（2026-02-04，保持行为不变）：将 `bc-iam-infra` 的 `RouterDetailFactory` 入参从 `UserEntity` 收敛为 `Object`，并通过 `SpringUtil.getBean(UserConverter.class)` 调 `usernameOf/rolesOf/nameOf` 桥接读取字段（过滤逻辑/递归构造与异常表现不变）；最小回归通过；落地：`5d2f7512`。
        - ✅ 已完成（2026-02-04，保持行为不变）：已将非 IAM 侧残留的 `UserEntity` 编译期依赖（`bc-course/infrastructure` 的 `CourseQueryRepository`）按“前置桥接 → 改调用侧”的两刀闭环完成：
          1) `UserConverter.springUserEntityWithNameObject(Object)`（内部仍强转 `UserEntity` 并调用 `SpringUtil.getBean(UserEntity.class)`，尽量保持异常形态与副作用顺序一致）；
          2) `CourseQueryRepository` 去 `UserEntity` import，并改走 `userConverter.toUserEntityObject(...)`、`courseConvertor.toCourseEntityWithTeacherObject(...)` 与上述桥接方法，保持缓存/查询/遍历顺序与异常文案不变。
        - ✅ 已完成（2026-02-04，保持行为不变）：将 `bc-iam-infra` 的 `UserServiceImpl` 去 `UserEntity` 编译期依赖：`Optional<?>.map(UserEntity.class::cast)` 改走 `userConverter.castUserEntityObject(...)`，并将 `getUserInfo` 入参收敛为 `Object` 后通过 `RouterDetailFactory/UserBizConvertor/UserConverter` 桥接读取字段（缓存/日志/异常文案/副作用顺序不变）；最小回归通过；落地：`d901223c`。
        - ✅ 已完成（2026-02-04，保持行为不变，推进）：`UserQueryGatewayImpl` 已去 `UserEntity` 编译期依赖：移除 `import UserEntity`，并将 `findById/findByUsername/page` 返回类型收敛为 `Optional<?>/PaginationResultEntity<?>`（方法体与 `@LocalCached` 触发点不变；最小回归通过；落地：`8cba32b8`）。
        - ✅ 已完成（2026-02-04，保持行为不变，证据化盘点）：用 Serena 盘点 `UserQueryGatewayImpl` 的对外签名/`@LocalCached` 触发点与调用面；详见 `docs/DDD_REFACTOR_BACKLOG.md` 4.3（证据化起点：`c4a32bde`）。
        - ✅ 已完成（2026-02-04，保持行为不变，前置铺垫）：在 `bc-iam/infrastructure` 新增内部过渡接口 `UserQueryCacheGateway`（返回 `Optional<?>/PaginationResultEntity<?>`），用于后续逐类将端口适配器从编译期依赖旧 `UserQueryGateway`（eva-domain）收敛为依赖内部接口，同时仍保证调用最终进入旧 `UserQueryGatewayImpl` 以触发 `@LocalCached` 缓存/切面入口（最小回归通过；落地：`dc49f903`）。
        - ✅ 已完成（2026-02-04，保持行为不变，前置铺垫）：已让旧 `UserQueryGatewayImpl` 实现 `UserQueryCacheGateway`，使后续端口适配器可“只改注入类型”而不改变实际委托路径与缓存触发点（最小回归通过；落地：`2970b80d`）。
        - ✅ 已完成（2026-02-04，保持行为不变，推进）：`UserQueryGatewayImpl` 已不再显式实现旧 `UserQueryGateway`，仅保留实现内部接口 `UserQueryCacheGateway`；`@LocalCached` 触发点与方法体保持不变（最小回归通过；落地：`fb3b2e40`）。
        - ✅ 已完成（2026-02-04，保持行为不变，推进）：已删除 `eva-domain` 中无引用的旧 `UserQueryGateway` 接口（此前已完成签名收敛；最小回归通过；落地：`93a7723d`）。
        - ✅ 已完成（2026-02-04，保持行为不变，推进）：已将 `UserEntityByIdQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `findById`，确保调用仍触发 `UserQueryGatewayImpl` 上的 `@LocalCached`（最小回归通过；落地：`e854fcbe`）。
        - ✅ 已完成（2026-02-04，保持行为不变，推进）：已将 `UserEntityByUsernameQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `findByUsername`，确保调用仍触发 `UserQueryGatewayImpl` 上的 `@LocalCached`（最小回归通过；落地：`ec31d96c`）。
        - ✅ 已完成（2026-02-04，保持行为不变，推进）：已将 `UserAllUserIdQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `findAllUserId/findById`，确保调用仍触发 `UserQueryGatewayImpl` 上的 `@LocalCached`（最小回归通过；落地：`c0c05def`）。
        - ✅ 已完成（2026-02-04，保持行为不变，推进）：已将 `UserNameQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `findById` 并通过强转读取 `name`，确保调用仍触发 `UserQueryGatewayImpl` 上的 `@LocalCached`（最小回归通过；落地：`1b91181f`）。
        - ✅ 已完成（2026-02-04，保持行为不变，推进）：已将 `UserDetailQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `findById` 并通过强转读取 `id/username/name`，确保调用仍触发 `UserQueryGatewayImpl` 上的 `@LocalCached`（最小回归通过；落地：`5d85516b`）。
        - ✅ 已完成（2026-02-04，保持行为不变，推进）：已将 `UserDirectoryPageQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `page/allUser/findAllUsername`，确保调用仍进入旧 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过；落地：`d5335f4a`）。
        - ✅ 已完成（2026-02-04，保持行为不变，推进）：已将 `UserEntityQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `getUserRoleIds`，确保调用仍进入旧 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过；落地：`b7373cbb`）。
        - 量化快照（口径=可复现命令）：`eva-domain` 0 个（已退场）、`eva-infra-dal` 0 个（已退场）、`eva-infra-shared` 0 个（已退场）、`eva-base-common` 0 个（源码已清零，类型已下沉到 `shared-kernel`；但依赖方 pom 仍需逐个清理）。root reactor 当前仍包含 `eva-base`（口径：`rg -n '<module>eva-' pom.xml`）。
      - ✅ 已完成（2026-02-04，保持行为不变，依赖收敛：eva-infra）：在 Serena + `rg` 证伪 `eva-infra` 已从 root reactor 退场且源码仅 `package-info.java`（无业务逻辑、无外部依赖方）后，收敛 `eva-infra/pom.xml`：移除残留 `<dependencies>` 依赖声明（最小回归通过；落地：`47654a6a`）。
      - ✅ 已完成（保持行为不变；每次只改 1 个 `pom.xml`）：在 Serena + `rg` 证伪 “全仓库已无 `eva-infra` 的 dependency 声明”后，已从 root reactor 移除 `<module>eva-infra</module>`（每步闭环；落地：`0aab4516`）。
      - ✅ 已完成（保持行为不变；单 pom）：已删除 `eva-infra/pom.xml`（前置：reactor 已退场且无依赖方；最小回归通过；落地：`6c9b6224`）。
      - 下一步（可选，保持行为不变）：若仍希望清理 `eva-infra/` 目录（当前仅剩 3 个 `package-info.java`），建议按“每次只改 1 个文件”逐步删除并回归，避免与其它变更混在同一提交里。

3) **可以移除 `eva-infra` 的判定标准（建议的 DoD）**
   - `eva-infra` 中旧 `*GatewayImpl` 已全部退化为委托壳或迁入对应 `bc-*/infrastructure`（或其过渡落点），不再承担“业务编排”。
   - `eva-infra-dal` / `eva-infra-shared`：在 **方案 B（严格）** 下不允许长期保留，必须逐步按 BC 归位或下沉到 `shared-kernel`（或拆成 `*-contract`）后删除；避免把“技术切片”永久化为新的共享大包。

**节奏建议（避免一次性大迁移）：**

- 先把“入口方法簇”按 BC 迁完（A→B），让 `eva-app` 业务实现逐步清空；再做“模块移除/目录折叠”类变更。
- 每次只做一个维度：要么迁入口（业务代码），要么迁装配/模块结构（wiring/Maven），避免同时改动导致回滚困难。
- 盘点清单落点：`docs/DDD_REFACTOR_BACKLOG.md` 的 4.3（未收敛清单）用于持续维护“仍在 eva-* 的入口/旧网关候选”。
- 建议的 S1 落地顺序（保持行为不变；每步只改 1 个类或 1 个 `pom.xml`）：
  1) ✅ 已完成：`eva-adapter` 退场关键闭环（`*Controller` 清零 + 组合根去依赖 + root reactor 移除 + 删除模块 pom），后续不再围绕 `eva-adapter` 投入“收敛入口”类工作，避免重复劳动。
  2) 优先推进“入口壳结构性收敛（Controller）”：按 BC 粒度（建议先 `bc-iam`）逐个 Controller 做纯结构性重构（收敛返回表达式/抽私有方法/清理无用 import），严格保持 URL/注解/异常文案/副作用顺序不变。
  3) 并行推进“依赖方 `pom.xml` 编译期依赖收敛（含纠偏）”：每次只改 1 个 pom；移除依赖前必须 Serena 证据化“无引用面”，并用 `rg` 留一条可复现兜底证据，避免增量编译/缓存掩盖问题导致误判（本周已出现一次，需要作为流程强约束固化）。
  4) 后置再考虑 `eva-domain/eva-infra*`：它们仍被多个 BC 编译期依赖，属于“跨 BC 共享/过渡模块”；需要先完成端口归属与依赖边界收敛，避免硬整合导致循环依赖与装配漂移。

### 10.3 未完成清单（滚动，供下一会话排期）

#### IAM 并行（10.3）：清理其它 BC 直连 IAM 表（`sys_user_role/sys_role`）（保持行为不变）

> 目标：让“其它 BC 的基础设施层”不再跨 BC 直连 IAM role 表，改为统一依赖 `bc-iam-contract` 的最小 Port，并由 `bc-iam/infrastructure` 端口适配器承接原 SQL/装配逻辑（不引入新的缓存/切面副作用）。

- ✅ 已完成：`bc-audit` 的 `LogGatewayImpl` 已改走 `UserEntityObjectByIdDirectQueryPort`（落地：`fdd7078e`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成：`bc-course` 的 `CourseQueryRepository.toUserEntity` 已改走 `UserEntityObjectByIdDirectQueryPort`（落地：`c22bc75d`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成：`bc-evaluation` 的 `EvaTaskQueryRepository.toUserEntity` 已改走 `UserEntityObjectByIdDirectQueryPort`（落地：`6c5d6bce`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变；每次只改 1 个类闭环）：`bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/bcevaluation/query/EvaRecordQueryRepository.java` 已不再直连 `SysUserRoleMapper/SysRoleMapper`，改走 `UserEntityObjectByIdDirectQueryPort`（异常文案保持不变；最小回归通过；落地：`78787eee`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 阶段性关闭（保持行为不变）：Serena 证伪 `bc-*`（排除 `bc-iam/**`）范围内无 `SysUserRoleMapper`/`SysRoleMapper` 引用点；后续若新增引用点，继续按“每次只改 1 个类”闭环收敛为调用 `UserEntityObjectByIdDirectQueryPort`。

#### bc-audit（Audit）S0.2 延伸：`eva-infra-dal` 按 BC 拆散试点（`sys_log`，保持行为不变）

> 目标：将“仅审计域使用”的 DAL（Mapper/DO/XML）逐文件归位到 `bc-audit/infrastructure`，把 `eva-infra-dal` 的表面积逐步收敛为“跨 BC 共享的最小集合”。每次只改 1 个类/1 个资源文件，并严格闭环（Serena → 最小回归 → 提交 → 三文档同步 → push）。

- ✅ 已完成：`SysLogModuleMapper`、`SysLogMapper` 已归位到 `bc-audit/infrastructure`（详见 10.2 / `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成：`SysLogDO/SysLogModuleDO` 与 `SysLogMapper.xml/SysLogModuleMapper.xml` 已全部归位到 `bc-audit/infrastructure`（保持 `package/namespace/resultMap type/SQL` 与资源路径 `mapper/**` 不变）。
- ✅ 依赖收敛证伪（保持行为不变；单 pom）：Serena 证据化确认 `bc-audit/infrastructure` 仍直接使用 `eva-infra-shared` 内类型（`UserConverter/EntityFactory`）；其中 `PaginationConverter` 已下沉到 `shared-kernel`（`0427f1d4`）、`QueryUtils` 已下沉到 `shared-kernel`（`f1638d20`），因此均不再作为“必须保留 eva-infra-shared 依赖”的证据项。结论仍是：当前暂不可移除 `bc-audit/infrastructure` 对 `eva-infra-shared` 的依赖（详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- 约束（保持行为不变）：Java `package`、MyBatis XML `namespace`、`resultMap type` 指向的 FQCN、资源路径 `mapper/**` 均保持不变。

#### bc-messaging（消息）S0.2 延伸：websocket 支撑类逐类归位（保持行为不变）

> 目标：把 `eva-infra-shared` 中“仅消息域/评教消息入口使用”的 websocket 支撑类逐类归位到 `bc-messaging`，不改业务语义、不改装配行为；每次只改 1 个类或 1 个 `pom.xml`，严格闭环（Serena → 最小回归 → 提交 → 三文档同步 → push）。

- ✅ 已完成（单 pom）：在 `bc-evaluation/infrastructure/pom.xml` 补齐对 `bc-messaging` 的 Maven 编译期依赖，用于承接后续 `WebsocketManager` 归位（最小回归通过；落地：`4dd1b34f`）。
- ✅ 已完成（逐类归位）：将 `WebsocketManager` 从 `eva-infra-shared` 搬运归位到 `bc-messaging`（保持 `package` 与类内容不变；最小回归通过；落地：`bf78d276`）。

#### bc-course（Course）S0.2：课程域类型逐类归位（`eva-domain` → `bc-course-domain`，保持行为不变）

> 背景：`eva-domain` 仍承载部分课程域实体/接口，导致多个 BC 仍需经由 `eva-domain` 承接编译期类型。当前采用“每次只搬运 1 个类 + 必要的单 pom 编译闭合前置”的节奏，逐步把**仅课程域承载**的类型归位到 `bc-course-domain`，并通过过渡期依赖保持全仓库编译闭合（不改业务语义）。

- ✅ 已完成（保持行为不变）：`ClassroomGateway`、`SemesterGateway`、`CourseTypeEntity` 已归位 `bc-course-domain`；为编译闭合已补齐 `bc-course/domain/pom.xml` 的必要依赖，并在过渡期为 `eva-domain`/`eva-infra-shared` 补齐对 `bc-course-domain` 的编译期依赖（详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`SubjectEntity` 已归位 `bc-course-domain`（保持 `package` 与类内容不变；最小回归通过；落地：`449e08d1`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`SemesterEntity` 已归位 `bc-course-domain`（保持 `package` 与类内容不变；最小回归通过；落地：`f4721306`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`CourseDeleteGateway` 已归位 `bc-course-domain`（保持 `package` 与接口签名/注解不变；最小回归通过；落地：`2ec12495`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`CourseUpdateGateway` 已归位 `bc-course-domain`（保持 `package` 与接口签名/注解不变；最小回归通过；落地：`73ccfff8`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变，编译闭合前置）：为后续归位 `CourseEntity/SingleCourseEntity` 提供编译闭合支撑，已在 `bc-course/domain/pom.xml` 显式增加对 `bc-iam-domain` 的 Maven 编译期依赖（承接 `CourseEntity` 对 `UserEntity` 的类型引用；最小回归通过；落地：`fa7e2270`）。
- ✅ 已完成（保持行为不变）：`CourseEntity` 已归位 `bc-course-domain`（保持 `package` 与类内容不变；最小回归通过；落地：`c94151f7`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`SingleCourseEntity` 已归位 `bc-course-domain`（保持 `package` 与类内容不变；最小回归通过；落地：`16db55ff`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`CourseQueryGateway` 已归位 `bc-course-domain`（保持 `package` 与接口签名/注解不变；最小回归通过；落地：`e5d56d1b`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 补充进展（2026-02-06，保持行为不变，编译闭合前置）：为后续将 `eva-domain` 内残留的评教实体（`CourOneEvaTemplateEntity/EvaTaskEntity/EvaRecordEntity`）逐类归位到 `bc-evaluation-domain` 做准备，已先在 `bc-evaluation/domain/pom.xml` 补齐最小编译期依赖（避免逐类搬运时反复补依赖；落地：`c5117a1a`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 补充进展（2026-02-06，保持行为不变，编译闭合前置）：为确保逐类搬运过程中 `eva-domain` 仍可编译闭合，已在 `eva-domain/pom.xml` 增加对 `bc-evaluation-domain` 的 Maven 编译期依赖（落地：`0bfbf450`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类闭环）：已将 `CourOneEvaTemplateEntity` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`616f925c`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类闭环）：已将 `EvaTaskEntity` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`c6cb11c4`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类闭环）：已将 `EvaRecordEntity` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`f4ceb140`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（2026-02-06，保持行为不变；每次只改 1 个 `pom.xml` 闭环）：已在 Serena 证伪 `eva-domain/src/main/java` 无课程域引用面后，收敛 `eva-domain/pom.xml`：移除对 `bc-course-domain` 的 Maven 编译期依赖（最小回归通过；落地：`ec4107e4`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类闭环）：已将 `EvaDeleteGateway` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/签名/注解不变；最小回归通过；落地：`b5f8f5fe`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类）：`EvaUpdateGateway` 已从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/签名/注解不变；最小回归通过；落地：`ba43d0a4`）。
- ✅ 补充进展（2026-02-06，保持行为不变，编译闭合前置）：为后续归位 `EvaConfigEntity`（其 `clone()` 依赖 `SpringUtil`）做准备，已在 `bc-evaluation/domain/pom.xml` 增加 `hutool-all` 编译期依赖（仅编译闭合；最小回归通过；落地：`5c4d3efe`）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类）：`EvaConfigEntity` 已从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变；最小回归通过；落地：`0c7f6aae`）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类）：`EvaConfigGateway` 已从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/签名/注解不变；最小回归通过；落地：`1aaff7d5`）。
- ✅ 补充进展（2026-02-06，保持行为不变，编译闭合前置）：为后续归位 `EvaTemplateEntity` 并最终收敛 `bc-evaluation/application` 去 `eva-domain` 编译期依赖做准备，已在 `bc-evaluation/application/pom.xml` 增加对 `bc-evaluation-domain` 的 Maven 编译期依赖（仅编译闭合；最小回归通过；落地：`9637eca1`）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类）：`EvaTemplateEntity` 已从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变；最小回归通过；落地：`ee79ffac`）。
- ✅ 已完成（2026-02-06，保持行为不变；每次只改 1 个 `pom.xml`）：在 Serena 证伪 `bc-evaluation/application` 无 `eva-domain` 残留引用面后，收敛 `bc-evaluation/application/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖，并补齐 `cola-component-exception` 以保持 `com.alibaba.cola.exception.*` 编译闭合（最小回归通过；落地：`9f4eaa06`）。
- ✅ 已完成（2026-02-06，保持行为不变；每次只改 1 个 `pom.xml`）：在 Serena 证伪 `bc-course/application` 无 `eva-domain` 残留引用面后，收敛 `bc-course/application/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖（最小回归通过；落地：`464a4d73`）。
- ✅ 补充进展（2026-02-06，保持行为不变，编译闭合前置）：为后续归位 `SysLogEntity/SysLogModuleEntity` 做准备，已在 `bc-audit/domain/pom.xml` 补齐最小编译期依赖（`bc-iam-domain`、`cola-component-domain-starter`、`lombok(provided)`；仅编译闭合；最小回归通过；落地：`63c8c5ca`）。
- ✅ 补充进展（2026-02-06，保持行为不变，编译闭合前置）：为确保逐类归位审计实体期间 `eva-domain` 仍可编译闭合，已在 `eva-domain/pom.xml` 增加对 `bc-audit-domain` 的 Maven 编译期依赖（过渡期；保持 `package` 不变；最小回归通过；落地：`90054971`）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类）：已将 `SysLogModuleEntity` 从 `eva-domain` 搬运归位到 `bc-audit-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`1f8675f1`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类）：已将 `SysLogEntity` 从 `eva-domain` 搬运归位到 `bc-audit-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`9efe8f6e`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类）：已将 `LogGateway` 从 `eva-domain` 搬运归位到 `bc-audit-domain`（保持 `package`/接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`44417e03`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 会话收尾（2026-02-06，保持行为不变）：已补齐 `SysLogModuleEntity` 归位的“三文档同步 + push”闭环（落地：`31e4d11c`）；并复核确认 `EvaUpdateGateway` 当前已归位 `bc-evaluation-domain`、无需重复搬运（Serena：引用面命中 `EvaTaskServiceImpl` + `EvaUpdateGatewayImpl`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类）：已将 `DynamicConfigGateway` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`9e2096fc`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个 `pom.xml`）：为后续归位 `MsgEntity` 做编译闭合前置，已收敛 `bc-messaging-contract/pom.xml` 并补齐承接 `MsgEntity` 所需的最小编译期依赖（`bc-iam-domain`、`cola-component-domain-starter`；仅用于编译闭合；最小回归通过；落地：`51d5a042`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类）：已将 `MsgEntity` 从 `eva-domain` 搬运归位到 `bc-messaging-contract`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`79d68fc3`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类）：已将 `MsgGateway` 从 `eva-domain` 搬运归位到 `bc-messaging-contract`（保持 `package`/接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`eaf62606`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个类）：已将 `MessageUseCaseFacade` 从 `eva-domain` 搬运归位到 `bc-messaging-contract`（保持 `package`/接口签名不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`31681de9`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 进展（2026-02-06，保持行为不变；每次只改 1 个 `pom.xml`）：在 Serena 证伪 `bc-messaging/src/main/java` 无 `eva-domain` 残留引用面后，已收敛 `bc-messaging/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖（仅收敛编译边界；最小回归通过；落地：`acecb5af`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（2026-02-06，保持行为不变；每次只改 1 个 `pom.xml`）：在 Serena 证伪 `eva-infra-shared/src/main/java` 无 `eva-domain` 残留引用面后，收敛 `eva-infra-shared/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖，并补齐对 `bc-course-domain` / `bc-evaluation-domain` / `bc-audit-domain` / `bc-iam-domain` 的显式依赖以保持编译闭合（最小回归通过；落地：`0585d6fb`）。
- ✅ 已完成（2026-02-06，保持行为不变；每次只改 1 个 `pom.xml`）：为修复 `eva-infra-shared/src/main/java` 中 `SysException/BizException` 的编译闭合，已在 `eva-infra-shared/pom.xml` 补齐 `cola-component-exception` 编译期依赖（最小回归通过；落地：`776ab171`）。
- ✅ 已完成（2026-02-06，保持行为不变；每次只改 1 个 `pom.xml`）：在确认除 `eva-domain/pom.xml` 自身外，全仓库 `**/pom.xml` 不再出现 `<artifactId>eva-domain</artifactId>` 后，从根 `pom.xml` 的 reactor 中移除 `<module>eva-domain</module>`（仅改变聚合构建边界；最小回归通过；落地：`6b907bc1`）。
- ✅ 已完成（2026-02-06，保持行为不变；每次只改 1 个 `pom.xml`）：删除 `eva-domain/pom.xml`（前置：reactor 已无 `<module>eva-domain</module>`；最小回归通过；落地：`c0035b03`）。
- 🎯 下一刀建议（保持行为不变）：若 `eva-domain/` 目录仍存在，则评估是否删除空目录（建议独立提交，避免与其它变更混在同一提交里）。
  - ⚠️ 现状说明（保持行为不变）：`CourseEntity/SingleCourseEntity` 字段仍编译期依赖 `UserEntity`（`bc-iam-domain`），因此 `bc-course-domain` 当前保留对 `bc-iam-domain` 的编译期依赖作为过渡。后续若需去耦合，建议以“下沉最小 User 协议到 `*-contract`/`shared-kernel`”为方向另起小步证伪与落地，避免牵连行为漂移。

#### bc-iam（IAM）S1：Controller 入口壳结构性收敛（保持行为不变）

> 背景：`bc-iam/infrastructure` 下的 Controller 仅承载 HTTP 协议适配与参数校验；本阶段只做“结构性收敛”（收敛返回包装表达式/抽取 `success()` 等），用于降低适配层编排噪声，但 **不改业务语义**，且缓存/日志/异常文案/副作用顺序完全不变。

- ✅ 已完成（保持行为不变）：`UserUpdateController`（落地：`5ee37fd2`）、`DepartmentController`（落地：`fbc5fb74`）、`AuthenticationController`（落地：`fd9e4d1c`）、`MenuUpdateController`（落地：`44bc649d`）、`RoleUpdateController`（落地：`c81eb2e0`）。
- 验收口径（每步闭环）：Serena（符号级定位 + 引用分析）→ 最小回归（`mvnd -pl start -am test ...`）→ `git commit` → 同步三文档 → `git commit` → `git push`。

#### IAM 并行（10.3）：继续清理 `UserQueryGateway` 编译期依赖（优先 eva-infra-shared，保持行为不变）

> 背景：目前 `bc-iam/**` 端口适配器已完成“注入类型收敛”（统一改注入 `UserQueryCacheGateway`），且旧 `UserQueryGatewayImpl` 已不再显式实现旧 `UserQueryGateway`；缓存/切面触发点仍由 `UserQueryGatewayImpl` 承载（行为不变）。但**依赖方**仍应逐步只依赖 `bc-iam-contract` 最小 Port。

- ✅ 已完成（保持行为不变）：`bc-iam/infrastructure` 的 `UserServiceImpl` 已从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 端口（详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 补充进展（保持行为不变）：已在 `bc-iam-contract` 新增鉴权侧最小端口 `UserPermissionAndRoleQueryPort`，用于后续将 `eva-infra-shared` 的 `StpInterfaceImpl` 去 `UserQueryGateway` 编译期依赖（端口适配器内部仍将委托旧 `UserQueryGateway.findByUsername`，以保持缓存/切面触发点不变；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 补充进展（保持行为不变）：已在 `bc-iam-infrastructure` 的 `UserEntityByUsernameQueryPortImpl` 补齐实现 `UserPermissionAndRoleQueryPort`（内部仍委托旧 `UserQueryGateway.findByUsername`；权限/角色筛选逻辑与 `StpInterfaceImpl` 现有实现一致；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 补充进展（保持行为不变，编译闭合前置）：已在 `eva-infra-shared/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖，用于后续将 `StpInterfaceImpl` 从依赖 `UserQueryGateway` 收敛为依赖 contract 端口（详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`eva-infra-shared/src/main/java/edu/cuit/app/security/StpInterfaceImpl.java` 已从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 端口（详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`eva-infra-shared/src/main/java/edu/cuit/app/convertor/MsgBizConvertor.java` 已从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 端口 `UserEntityByIdQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway.findById`；消息 recipient/sender lazy-load 语义不变；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`bc-iam/application` 的 `FindUserByIdUseCase` 已去 `UserEntity` 编译期依赖，并收敛为优先依赖 `bc-iam-contract` 端口 `UserEntityByIdQueryPort`（`execute` 使用泛型承接 `Optional<?>`；不改变旧 gateway 委托链路/缓存触发点；落地：`e13e1dc6`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`bc-iam/application` 的 `FindUserByUsernameUseCase` 已去 `UserEntity` 编译期依赖，并收敛为优先依赖 `bc-iam-contract` 端口 `UserEntityByUsernameQueryPort`（`execute` 使用泛型承接 `Optional<?>`；不改变旧 gateway 委托链路/缓存触发点；落地：`e8f16843`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`bc-iam/application` 的 `PageUserUseCase` 已去 `UserEntity` 编译期依赖，并收敛为优先依赖 `bc-iam-contract` 端口 `UserDirectoryPageQueryPort`（分页出参使用泛型承接 `PaginationResultEntity<?>`；不改变旧 gateway 委托链路/缓存触发点；落地：`4de644a7`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`bc-iam/application` 的旧端口 `UserEntityQueryPort` 已去 `UserEntity` 编译期依赖（返回类型收敛为 `Optional<?>/PaginationResultEntity<?>`；过渡期实际返回仍为 `UserEntity`；端口适配器逻辑不变；落地：`72d029e0`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已完成（保持行为不变）：`bc-iam/application` 的 `FindUserByIdUseCaseTest` 已去 `UserEntity` 编译期依赖（测试侧端口返回类型同步为通配符；不影响用例委托行为；落地：`d8804521`）。
- ✅ 已完成（保持行为不变）：`bc-iam/application` 的 `FindUserByUsernameUseCaseTest` 已去 `UserEntity` 编译期依赖（测试侧端口返回类型同步为通配符；不影响用例委托行为；落地：`0ebfb75c`）。
- ✅ 已完成（保持行为不变）：`bc-iam/application` 的 `PageUserUseCaseTest` 已去 `UserEntity` 编译期依赖（测试侧端口返回类型同步为通配符；不影响用例委托行为；落地：`934cf935`）。
- ✅ 已完成（保持行为不变，依赖方收敛样例）：`bc-evaluation/infrastructure` 的 `FillAverageScoreExporterDecorator` 已去 `UserEntity` 编译期依赖，改为依赖 `bc-iam-contract` 的 `UserDetailQueryPort` + `UserDetailCO`（内部仍委托旧 `UserQueryGateway.findById`，缓存触发点不变；落地：`7a3ca8ed`）。
- ✅ 已完成（保持行为不变，依赖方收敛样例）：`bc-evaluation/infrastructure` 的 `FillEvaRecordExporterDecorator` 已去 `UserEntity` 编译期依赖，改为依赖 `bc-iam-contract` 的 `UserDetailQueryPort` + `UserDetailCO`（内部仍委托旧 `UserQueryGateway.findById`，缓存触发点不变；落地：`fba76459`）。
- ✅ 已完成（保持行为不变，依赖方收敛样例）：`bc-evaluation/infrastructure` 的 `FillUserStatisticsExporterDecorator` 已去 `UserEntity` 编译期依赖，改为依赖 `bc-iam-contract` 的 `UserDetailQueryPort` + `UserDetailCO`（内部仍委托旧 `UserQueryGateway.findById`，缓存触发点不变；落地：`8d59ea72`）。
- ✅ 补充进展（保持行为不变，测试过渡收敛）：`start` 的 `MsgServiceImplTest` 已不再兼容旧构造与反射逻辑，直接使用 Port 版本构造，从而不再编译期依赖 `UserQueryGateway`（详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 补充进展（保持行为不变，测试过渡收敛）：`start` 的 `UserEvaServiceImplTest` 已不再兼容旧构造与反射逻辑，直接使用 Port 版本构造，从而不再编译期依赖 `UserQueryGateway`（详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
- ✅ 已证伪（更新至 2026-02-04，保持行为不变）：`eva-infra-shared/src/main/java` 已无 `UserQueryGateway` 引用面；下一刀建议转入 `bc-iam/application` 去 `UserEntity` 编译期依赖（见下一小节）。

#### bc-iam（IAM）S0.2 延伸：收敛 `bc-iam(application)` 对 `eva-domain` 的编译期依赖（保持行为不变）

> 背景：历史上 `bc-iam/application/src/main/java` 直接 `import edu.cuit.domain.*`（例如 `LdapPersonGateway/DepartmentGateway/PaginationResultEntity` 等），因此曾依赖 `eva-domain`。截至 2026-02-04，相关类型已逐步归位到 `bc-iam-domain/shared-kernel`，且 `bc-iam/application/pom.xml` 已完成移除 `eva-domain` 编译期依赖（保持行为不变；见下方落地记录）。
>
> 目标：把“仅 IAM 使用”的 `edu.cuit.domain.entity.user.*` / `edu.cuit.domain.gateway.user.*` 等类型逐步归位到 `bc-iam-domain`（保持 `package` 不变，仅改变 Maven 模块归属），最终让 `bc-iam(application)` 仅依赖 `bc-iam-domain`（以及必要的 `shared-kernel/contract`），并在证伪引用面后收敛 `pom.xml`（保持行为不变）。

- 落地拆分（每步只改 1 个类或 1 个 `pom.xml`，并严格闭环）：
  0) ✅（pom）先改 `bc-iam/domain/pom.xml`：补齐“搬运归位所需的最小编译期依赖”（例如 Lombok（provided）、必要的 Spring 注解依赖等），仅用于编译闭合，不引入新业务语义。（已完成：`a3d048d0`）
  0.1) ✅（pom）为保证后续“每次只搬运 1 个类”时仍可编译闭合：先在 `bc-iam/application/pom.xml` 显式增加对 `bc-iam-domain` 的 Maven 编译期依赖（暂不移除 `eva-domain`，保持行为不变）。（已完成：`aeaa8471`）
  0.2) ✅（pom）为保证后续搬运 `edu.cuit.domain.gateway.user.*`（其签名仍依赖 IAM 的 cmd/CO）时可编译闭合：在 `bc-iam/domain/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（保持行为不变）。（已完成：`2fc02fed`）
  0.3) ✅（pom）为保证 `eva-domain` 内残留的 `UserEntity` 等类型在逐步搬运过程中仍可编译闭合：在 `eva-domain/pom.xml` 显式增加对 `bc-iam-domain` 的 Maven 编译期依赖（过渡期；保持行为不变）。（已完成：`43e8b66e`）
  1) （类）选择一个**引用面仅在 IAM** 的 `edu.cuit.domain.*` 类型（优先纯 POJO/纯接口，且依赖最少），用 Serena 证据化引用面后，将该类从 `eva-domain` 搬运到 `bc-iam/domain`（保持 `package` 与类内容不变），并删除 `eva-domain` 原文件，确保全仓库同名 FQCN 仅存在一份。
     - ✅ 已完成（保持行为不变）：`RoleUpdateGateway` → `bc-iam-domain`（落地：`95e37e8a`）。
     - ✅ 已完成（保持行为不变）：`MenuUpdateGateway` → `bc-iam-domain`（落地：`c31a7a1e`）。
     - ✅ 已完成（保持行为不变）：`DepartmentGateway` → `bc-iam-domain`（落地：`68128578`）。
     - ✅ 已完成（保持行为不变）：`UserUpdateGateway` → `bc-iam-domain`（落地：`6630277b`）。
     - ✅ 已完成（保持行为不变）：`MenuEntity` → `bc-iam-domain`（落地：`6d700911`）。
     - ✅ 已完成（保持行为不变）：`MenuQueryGateway` → `bc-iam-domain`（落地：`9982af0a`）。
     - ✅ 已完成（保持行为不变）：`LdapPersonEntity` → `bc-iam-domain`（落地：`eb36c6ce`）。
     - ✅ 已完成（保持行为不变）：`LdapPersonGateway` → `bc-iam-domain`（落地：`ce85525d`）。
     - ✅ 已完成（保持行为不变）：`RoleEntity` → `bc-iam-domain`（保持 `package` 与类内容不变，仅搬运归位；最小回归通过；落地：`6f290793`）。
     - ✅ 已完成（保持行为不变）：`UserEntity` → `bc-iam-domain`（保持 `package` 与类内容不变，仅搬运归位；最小回归通过；落地：`68840131`）。
     - ✅ 已完成（保持行为不变）：`RoleQueryGateway` → `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅搬运归位；最小回归通过；落地：`4b3efbf7`）。
     - 下一刀建议（保持行为不变）：继续按 Serena 证据化盘点，逐类归位“仅 IAM 使用”的 `edu.cuit.domain.*` 子集（优先纯 POJO/纯接口）。注意：`RoleEntity` 当前仍存在跨 BC 引用面（过渡期先保持 `package` 稳定），后续应再评估其最终承载面。
     - ✅ 补充进展（保持行为不变，前置）：为避免登录校验引入额外查询次数/缓存触发点，已在 `bc-iam-contract` 新增按用户名查询用户状态最小端口 `UserStatusByUsernameQueryPort`，用于后续将 `ValidateUserLoginUseCase` 去 `UserEntity` 编译期依赖（详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
     - ✅ 补充进展（保持行为不变，端口适配器实现）：已为 `UserStatusByUsernameQueryPort` 补齐端口适配器实现（内部委托旧 `UserQueryGateway.findByUsername`）；为避免未来注入歧义，当前由 `UserEntityByUsernameQueryPortImpl` 统一承接该端口能力。
     - ✅ 补充进展（保持行为不变，适配器复用）：已在 `UserEntityByUsernameQueryPortImpl` 补齐实现 `UserStatusByUsernameQueryPort`，用于后续让 `ValidateUserLoginUseCase` 在不改调用方的前提下去 `UserEntity` 编译期依赖（详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
  2) （类）重复上一步：每次只搬运 1 个类，逐步清空“bc-iam 所需且仅 bc-iam 使用”的 `edu.cuit.domain.*` 子集。
  3) （pom）当 Serena + `rg` 证伪 `bc-iam/application` 不再需要 `eva-domain` 提供的任何类型后，再改 `bc-iam/application/pom.xml`：移除 `eva-domain`，改为显式依赖 `bc-iam-domain`（保持行为不变）。
     - ✅ 已完成（保持行为不变）：`bc-iam/application/pom.xml` 已移除对 `eva-domain` 的 Maven 编译期依赖，并补齐缺失的 `cola-component-exception` 编译期依赖以保持编译闭合（最小回归通过；落地：`04e8b671`）。

  - 关键风险控制（必须写进流程，避免“把共享类型误归位到单一 BC”）：
  - 若某个 `edu.cuit.domain.*` 类型被多个 BC 复用，则**不得**直接归位到 `bc-iam-domain`；应重新评估其真实归属：跨 BC 协议 → `shared-kernel`；跨 BC 业务语义 → 明确归属后再迁（或保持在 `eva-domain` 过渡）。
  - 对带有 Spring 注解/框架依赖的接口（例如历史上在 interface 上存在 `@Component` 的情况），搬运时不改注解/行为；如需补齐编译期依赖，优先通过“先改 1 个 pom.xml 补齐依赖”单步闭环完成。

> 补充进展（2026-01-31，保持行为不变，端口下沉）：为减少依赖方对 IAM 应用层 jar（`bc-iam`）的编译期绑定，已将 `UserBasicQueryPort` 从 `bc-iam/application` 下沉到 `bc-iam-contract`（保持 `package edu.cuit.bc.iam.application.port` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过；落地：`739cb25f`）。
>
> 补充进展（2026-01-31，保持行为不变，编译闭合前置）：为后续将 `bc-ai-report/infrastructure` 的 `AiReportUserIdQueryPortImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort` 做前置，已在 `bc-ai-report/infrastructure/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（仅编译边界收敛；最小回归通过；落地：`bceb2576`）。
>
> ✅ 已完成（2026-01-31，保持行为不变；每次只改 1 个类闭环）：已将 `bc-ai-report/infrastructure` 的 `AiReportUserIdQueryPortImpl` 对 `edu.cuit.domain.gateway.user.UserQueryGateway` 的依赖收敛为依赖 `UserBasicQueryPort`（只替换该类实际用到的方法；行为不变）；落地：`b16546ed`。依赖方可通过 `bc-iam-contract` 获取同名端口类型，同时不提前触碰跨 BC 复用的 `UserQueryGateway/UserEntity` 归属设计。
>
> 补充进展（2026-01-31，保持行为不变，端口补齐前置）：为后续继续收敛 `bc-ai-report/infrastructure` 的 IAM 依赖（例如 `AiReportAnalysisPortImpl` 中的“按 userId 获取 teacherName”），已在 `bc-iam-contract` 新增 `UserNameQueryPort`（仅新增接口，不改装配/不改行为；落地：`cfccf4ca`）。
>
> 补充进展（2026-01-31，保持行为不变，端口适配器实现补齐）：已在 `bc-iam-infra` 新增 `UserNameQueryPortImpl`，内部委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变（落地：`8852b859`）。
>
> ✅ 进展（2026-01-31，保持行为不变；每次只改 1 个类闭环）：已将 `bc-ai-report/infrastructure` 的 `AiReportAnalysisPortImpl` 从直接依赖 `UserQueryGateway` 收敛为依赖 `UserNameQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变；保留 `@Deprecated` 旧构造方式仅用于测试过渡）；落地：`c374ae9b`。
>
> 补充进展（2026-01-31，保持行为不变，测试过渡收敛）：已将 `AiReportAnalysisPortImplTest` 从 mock `UserQueryGateway` 改为 mock `UserNameQueryPort` 并调用新构造方法（作为下一刀移除 `AiReportAnalysisPortImpl` 中 `@Deprecated` 过渡构造的前置）；落地：`6e99c11b`。
>
> ✅ 进展（2026-02-01，保持行为不变；每次只改 1 个类闭环）：在测试已切到新构造的前提下，已移除 `AiReportAnalysisPortImpl` 中 `@Deprecated` 旧构造与桥接逻辑，使该类彻底不再编译期依赖 `UserQueryGateway`（运行期仍通过 `UserNameQueryPortImpl -> UserQueryGateway.findById` 保持缓存/切面触发点不变）；落地：`b2a6dc15`。

> 补充进展（2026-02-01，保持行为不变，编译闭合前置）：为后续将 `bc-course/infrastructure` 的 `ICourseServiceImpl` 等从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort` 做前置，已在 `bc-course/infrastructure/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（仅编译边界收敛；最小回归通过；落地：`7b10d159`）。

> ✅ 进展（2026-02-01，保持行为不变；每次只改 1 个类闭环）：已将 `bc-course/infrastructure` 的 `ICourseServiceImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（只替换该类实际用到的方法；异常文案/副作用顺序不变；最小回归通过；落地：`24f141a1`）。

> ✅ 进展（2026-02-01，保持行为不变；每次只改 1 个类闭环）：已将 `bc-course/infrastructure` 的 `ICourseDetailServiceImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（只替换该类实际用到的方法；异常文案/副作用顺序不变；最小回归通过；落地：`d6c1d692`）。

> ✅ 进展（2026-02-02，保持行为不变；每次只改 1 个类闭环）：已将 `bc-course/infrastructure` 的 `IUserCourseServiceImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（只替换该类实际用到的方法；异常文案/副作用顺序不变；最小回归通过；落地：`4dca490e`）。

> ✅ 进展（2026-02-02，保持行为不变；每次只改 1 个类闭环）：已将 `bc-iam/infrastructure` 的 `AvatarManager` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（只替换该类实际用到的 `findUsernameById`；异常文案/文件 IO 行为不变；最小回归通过；落地：`56872f30`）。

> ✅ 进展（2026-02-02，保持行为不变，测试过渡）：为后续收敛 `bc-evaluation/infrastructure` 的 `UserEvaServiceImpl` 对 `UserQueryGateway` 的编译期依赖，先将 `start` 模块中的 `UserEvaServiceImplTest` 改为兼容“旧构造（UserQueryGateway）/新构造（UserBasicQueryPort）”（仅测试代码；最小回归通过；落地：`93ac4799`）。

> ✅ 进展（2026-02-02，保持行为不变；每次只改 1 个类闭环）：已将 `bc-evaluation/infrastructure` 的 `UserEvaServiceImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（只替换该类实际用到的 `findIdByUsername`；异常文案与分支顺序不变；最小回归通过；落地：`456e5346`）。

> ✅ 进展（2026-02-02，保持行为不变；每次只改 1 个类闭环）：清理 `bc-evaluation/infrastructure` 的 `UserEvaServiceImpl` 中残留未使用 `UserQueryGateway` import，使该类彻底不再编译期依赖旧 gateway（仅删 import；最小回归通过；落地：`edc3f9c7`）。

> ✅ 进展（2026-02-02，保持行为不变，测试过渡）：为后续收敛 `bc-evaluation/infrastructure` 的 `MsgServiceImpl` 对 `UserQueryGateway` 的编译期依赖，先将 `start` 模块中的 `MsgServiceImplTest` 改为兼容“旧构造（UserQueryGateway）/新构造（Port）”（仅测试代码；最小回归通过；落地：`f593b529`）。

> ✅ 进展（2026-02-02，保持行为不变，端口补齐前置）：为后续收敛 `bc-evaluation/infrastructure` 的 `MsgServiceImpl` 对 `UserQueryGateway.findAllUserId()` 的依赖，已在 `bc-iam-contract` 新增用户ID列表查询端口 `UserAllUserIdQueryPort`（仅新增接口，不改装配/不改行为；最小回归通过；落地：`f244c9d0`）。

> ✅ 进展（2026-02-02，保持行为不变，端口适配器实现补齐）：已在 `bc-iam-infra` 新增 `UserAllUserIdQueryPortImpl`，内部委托旧 `UserQueryGateway.findAllUserId()` 以保持缓存/切面触发点不变（最小回归通过；落地：`daff2644`）。

> ✅ 进展（2026-02-02，保持行为不变；每次只改 1 个类闭环）：已将 `bc-evaluation/infrastructure` 的 `MsgServiceImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 IAM contract 端口 `UserBasicQueryPort + UserAllUserIdQueryPort`（广播消息的 `findAllUserId()` 由端口适配器委托旧 gateway 以保持缓存/切面触发点不变；异常文案/副作用顺序不变；最小回归通过；落地：`b4456be9`）。

> 补充进展（2026-02-01，保持行为不变，编译闭合前置）：为后续将审计域 `LogInsertionPortImpl` 对 `UserQueryGateway` 的依赖收敛为依赖 IAM contract 端口做前置，已在 `bc-audit/infrastructure/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（仅编译边界收敛；最小回归通过；落地：`77bb15b2`）。

> ✅ 进展（2026-02-01，保持行为不变；每次只改 1 个类闭环）：已将审计域 `LogInsertionPortImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort`（调用链原本就最终委托 `UserBasicQueryPortImpl`，因此缓存命中/回源顺序与历史语义保持不变）；落地：`065183ab`。

> ✅ 进展（2026-02-01，保持行为不变；每次只改 1 个类闭环）：已移除审计域 `LogGatewayImpl` 中未使用的 `UserQueryGateway` 注入字段与 import（不影响任何业务逻辑/异常文案/副作用顺序）；落地：`6d4b3661`。

#### bc-audit（审计）S1：Controller 入口壳结构性收敛（保持行为不变）

> 背景：审计域的 Controller 同样遵循“仅结构性收敛、不改语义”的原则（不改 URL/注解/权限/异常文案/副作用顺序），用于降低入口壳噪声。

- ✅ 已完成（保持行为不变）：`LogController`（落地：`14c9ab77`）。

#### bc-ai-report（AI 报告）S0.2 延伸：收敛 `bc-ai-report-infra` 对 `bc-evaluation` 的编译期依赖（保持行为不变）

> 目标：让 AI 报告基础设施侧尽量只依赖评教 `contract`（`bc-evaluation-contract`），避免编译期绑定评教 `application jar`（`bc-evaluation`）。
>
> 当前阻塞（更新至 2026-01-30，保持行为不变）：Serena 证据化确认 `bc-ai-report/infrastructure` 的 `AiReportAnalysisPortImpl` 仍依赖 `EvaRecordExportQueryPort`，而该端口当前仍定义于 `bc-evaluation/application`，并通过 `EvaRecordCourseQueryPort` 引入 `EvaRecordEntity` 等旧领域实体（`eva-domain`）。因此现阶段无法仅通过“单 pom 替换”将 `bc-ai-report-infra` 的 `bc-evaluation` 依赖直接收敛为 `bc-evaluation-contract`，否则会引入 `contract` 反向依赖或类型重复风险。

> 建议解法（后置，先证据化再拆）：先盘点“导出链路读侧端口”依赖的实体类型归属（`eva-domain` vs `shared-kernel` vs `contract`），再决定是继续下沉端口/实体到 `contract`，还是保持过渡并延后收敛 `pom.xml`。
>
> 补充踩坑（2026-01-31，保持行为不变）：尝试让 `bc-evaluation-contract` 直接依赖 `eva-domain` 以承载 `EvaRecordEntity` 相关签名会触发 Maven 循环依赖：`bc-evaluation-contract -> eva-domain -> bc-iam-domain -> bc-iam-contract -> bc-evaluation-contract`（其中 `bc-iam-contract` 必须显式依赖 `bc-evaluation-contract`）。因此推进“评教导出端口下沉”需要先拆解/下沉签名依赖的旧领域实体，或调整接口签名归属再推进。

#### S0.2 延伸（并行主线）：清理“无测试源码模块”的无用测试依赖（保持行为不变）

> 目标：在不改业务语义的前提下，逐个模块收敛 `pom.xml` 中“未使用的测试依赖”（典型：模块无 `src/test/java` 且源码无 `org.junit.jupiter.*` 引用，但仍声明 `junit-jupiter(test)`）。
>
> 执行原则（强约束）：每次只改 1 个 `pom.xml`；移除前必须 **Serena + `rg` 双证据**；每步闭环：最小回归 → `git commit` → 三文档同步 → `git commit` → `git push`。
>
> 可复现证据口径：
> - `rg -n "<artifactId>junit-jupiter</artifactId>" --glob "**/pom.xml" .`（盘点候选）
> - 对单个候选模块：`fd -t d src/test <module>` + `rg -n "org\\.junit\\.jupiter" <module>/src`

#### bc-messaging（消息）S1：Controller 入口壳结构性收敛（保持行为不变）

> 背景：消息域的 Controller 同样只做结构性收敛（收敛返回包装表达式/抽取 `success()` 等），不改业务语义与副作用顺序。

- ✅ 已完成（保持行为不变）：`MessageController`（落地：`9a3ef681`）。

#### bc-course（课程）S1：Controller 入口壳结构性收敛（保持行为不变）

> 背景：课程域的 Controller 同样只做结构性收敛（收敛返回包装表达式/抽取 `success()` 等），不改业务语义与副作用顺序。

- ✅ 已完成（保持行为不变）：`SemesterController`（落地：`13892e55`）、`ClassroomController`（落地：`99eb17c0`）。
- ✅ 已完成（保持行为不变）：`QueryCourseController`（落地：`4c54337f`）。
- ✅ 已完成（保持行为不变）：`QueryUserCourseController`（落地：`c8ce3522`）。

#### bc-evaluation（评教）S0.2 延伸：收敛 `eva-app` 对 `bc-evaluation` 的编译期依赖（保持行为不变）

> 背景：为避免 `eva-app` 继续承担评教基础设施/装配责任，已完成三步前置（均保持行为不变）：`BcEvaluationConfiguration` 已从 `eva-app` 归位到 `bc-evaluation-infra`（`c3f7fc56`）；组合根 `start` 已显式依赖 `bc-evaluation-infra(runtime)`（`0f20d0cd`）；`eva-app/pom.xml` 已移除对 `bc-evaluation-infra` 的依赖（`e9feeb56`）。

- ✅ 已完成（更新至 2026-01-14；保持行为不变）：已将 `eva-app/src/main/java` 中仍 `import edu.cuit.bc.evaluation.*` 的残留类全部归位到 `bc-evaluation-infra`（保持 `package` 不变），并在 `rg -n '^import\\s+edu\\.cuit\\.bc\\.evaluation' eva-app/src/main/java` 命中为 0 后，完成收敛 `eva-app/pom.xml`：移除对 `bc-evaluation`（application jar）的 Maven 编译期依赖（保持行为不变）。落地要点：`UserEvaServiceImpl`（`f4238a5c`）、`MsgServiceImpl`（`5dea9347`）、`eva-app/pom.xml` 去 `bc-evaluation`（`2b42db5d`）；支撑类前置：`WebsocketManager/MsgBizConvertor` 已归位到 `eva-infra-shared` 并补齐 websocket 依赖（`82609bda/406186ae/c69f494f`）。

#### bc-audit（审计）S0.2 延伸：收敛 `eva-app` 对 `bc-audit` 的编译期依赖（保持行为不变）

> 背景：当前 `eva-app` 仍直接依赖 `bc-audit` 应用层类型（例如 `InsertLogUseCase`），且仍残留审计旧入口类（如 `LogServiceImpl`），导致 `eva-app/pom.xml` 仍声明 `bc-audit` / `bc-audit-infra` 依赖。
> 目标：按“旧入口/组合根逐个归位 → 再收敛 pom 依赖”的套路，逐步清空 `eva-app/src/main/java` 对 `edu.cuit.bc.audit.*` 的直接引用面，并在证伪为 0 后评估 `eva-app/pom.xml` 去 `bc-audit` 编译期依赖（每次只改 1 个类或 1 个 pom；保持行为不变）。

  - 证据口径（新会话先复核，避免口径漂移）：`rg -n '^import\\s+edu\\.cuit\\.bc\\.audit' eva-app/src/main/java`（当前应为 0）。
  - 建议顺序（每次只改 1 个类闭环，保持行为不变）：
  1) ✅ 已完成：归位 `eva-app/src/main/java/edu/cuit/app/config/BcAuditConfiguration.java` → `bc-audit-infra`（保持 `package edu.cuit.app.config` 不变；Bean 定义/装配顺序不变；落地：`5a4d726b`）。
  1.1) ✅ 编译闭合前置（保持行为不变）：为后续归位 `LogBizConvertor/LogServiceImpl`（依赖 `LogManager/OperateLogBO`），已在 `bc-audit/infrastructure/pom.xml` 补齐 `zym-spring-boot-starter-logging`（运行时 classpath 已存在，仅显式化；落地：`e7e13736`）。
  1.2) ✅ 支撑类前置（保持行为不变）：将 `LogBizConvertor` 从 `eva-app` 归位到 `bc-audit-infra`（保持 `package edu.cuit.app.convertor` 不变；MapStruct 映射规则/表达式不变；落地：`99960c7f`），用于为后续归位 `LogServiceImpl` 闭合依赖。
  2) ✅ 已完成：归位 `eva-app/src/main/java/edu/cuit/app/service/impl/LogServiceImpl.java` → `bc-audit-infra`（保持 `package` 不变；事务/日志/异常文案/副作用顺序不变；落地：`d0af2bac`）。
  3) ✅ 已完成：在 `rg` 证伪 `eva-app/src/main/java` 不再 `import edu.cuit.bc.audit.*` 后，已完成收敛 `eva-app/pom.xml`：移除对 `bc-audit`（以及 `bc-audit-infra`）的编译期依赖（保持行为不变；落地：`b3ea5f58`）。运行期装配由组合根 `start` 显式兜底：
     - ✅ `start/pom.xml` 已显式增加 `bc-audit-infra(runtime)`（落地：`d6d9c480`）。

#### bc-course（课程）S0.2 延伸：协议承载面继续收敛（保持行为不变）

> 背景：S0.2 的“主目标”（`eva-domain` 去 `bc-course` 编译期依赖）已闭环；但 `bc-course/application` 曾长期承载一批 `edu.cuit.client.*` 协议接口/对象，且部分签名依赖评教域 DTO（如 `CourseScoreCO/EvaTeacherInfoCO`）。本阶段已按路线 A 将该簇逐步下沉到 `shared-kernel`（均保持 `package` 不变；保持行为不变），并移除 `bc-course/application` 对 `bc-evaluation-contract` 的编译期依赖，避免“课程协议签名反向依赖评教 contract”。下一步主线切换为：在协议承载面已进入 `shared-kernel` 的前提下，逐个模块收敛对 `bc-course` 的编译期依赖（每次只改 1 个 `pom.xml`，便于回滚）。

- 下一步（建议每步只改 1 个小包/小类簇，保持行为不变）：
  1) ✅ Serena 证据化盘点：`edu.cuit.client.api.course` 下残留接口（`ICourseDetailService/ICourseService/ICourseTypeService`）以及其签名依赖的“跨 BC”类型落点与引用面（已完成；证据与结论以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  2) ✅ 路线 A（推荐，最小改动）：已将确属“跨 BC 协议对象”的 `edu.cuit.client.dto.clientobject.eva` 小簇（`CourseScoreCO/EvaTeacherInfoCO`）迁到 `shared-kernel`（保持 `package` 不变；落地：`bc30e9de`）。
  3) ✅ 路线 A（继续推进，保持行为不变）：已将 `SingleCourseDetailCO/ModifySingleCourseDetailCO/SimpleCourseResultCO` 与课程 API 接口（`ICourseDetailService/ICourseService/ICourseTypeService`）逐步下沉到 `shared-kernel`（均保持 `package` 不变；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  4) ✅ 延伸主线（依赖方收敛，保持行为不变）：已完成四处“依赖方收敛”闭环：`bc-evaluation-infra` 去 `bc-course` 编译期依赖（落地：`a0bcf74f`）、`eva-infra-shared` 去 `bc-course` 编译期依赖（落地：`6ab3837a`）、`eva-adapter` 去 `bc-course` 传递依赖并显式依赖 `shared-kernel`（落地：`f8ff84f5`）以及 `eva-infra` 去 `bc-course` 编译期依赖（落地：`8d806bf0`）。补齐：为闭合 `bc-evaluation-infra` 对 `ISemesterService` 的编译期引用，已将 `edu.cuit.client.api.ISemesterService` 从 `bc-course/application` 下沉到 `shared-kernel`（保持 `package` 不变；落地：`c22802ff`）。同时已启动“课程域基础设施归位”：将 `edu.cuit.infra.bccourse.adapter/*PortImpl` 从 `eva-infra` 迁移到 `bc-course-infra`（保持 `package` 不变；落地：`c4179654`），并将 `ClassroomCacheConstants` 归位到 `eva-infra-shared` 以便后续迁移 `*RepositoryImpl`（落地：`c22802ff`）。补充证伪（2026-01-08）：Serena 证据化确认 `eva-app` 仍大量引用 `edu.cuit.bc.course.*`（组合根/旧入口/旧网关对用例/端口/异常的直接依赖），因此暂不可将 `eva-app/pom.xml` 的 `bc-course` 依赖替换为 `shared-kernel`。并行推进：课程域 `*RepositoryImpl` 已完成归位清零（以 `NEXT_SESSION_HANDOFF.md` 0.9 为准），为后续继续削减课程域残留实现承载面与去依赖创造前提。
     - 补充阻塞（保持行为不变）：`eva-app` 当前仍大量引用 `edu.cuit.bc.course.*`（组合根/旧入口对用例/端口/异常的直接依赖），因此暂不满足“仅使用 `edu.cuit.client.*`”的前提；需先按里程碑继续收敛旧入口/装配与依赖面，再评估 `eva-app` 去 `bc-course` 依赖（证据与记录以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  5) 路线 B（后置，结构更清晰但成本更高）：若后续发现 `shared-kernel` 承载课程域接口/CO 规模继续膨胀，可新增 `bc-course-contract`（或更中立的 contract 模块）承载这些接口/CO，避免继续膨胀 `shared-kernel`；再逐步把依赖方从 `shared-kernel` 切到 contract（保持 `package`/行为不变）。
  6) ✅ 并行支线（课程域基础设施归位，保持行为不变）：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 已完成归位，残留清零（以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  7) ✅ 延伸主线（实现承载面继续收敛，保持行为不变）：Serena 证伪：`CourseImportExce/CourseRecommendExce` 的引用点均仅位于 `bc-course/infrastructure`，因此将其从 `eva-infra-shared` 归位到 `bc-course/infrastructure`（保持 `package` 不变，仅搬运与编译闭合；落地：`d3b9247e`）；`CourseFormat` 被课程/评教/旧入口复用，继续留在 `eva-infra-shared`。
  8) ✅ 并行主线（依赖方编译期依赖继续收敛，证伪结论，保持行为不变）：本次重新盘点显示：除 `eva-app` 外，未发现仍显式依赖 `bc-course` 且满足“仅使用 `edu.cuit.client.*` 类型”的模块，因此本会话无可执行的 `pom.xml` 依赖替换点；而 `eva-app` 仍存在大量 `edu.cuit.bc.course.*` 引用，暂不满足替换前提。下一步应先继续收敛 `eva-app` 对课程域用例/端口/异常的直接引用，再回到“逐个模块依赖替换”。
  9) ✅ 延伸主线（继续削减 `eva-app` 课程域编译期引用面，保持行为不变）：将课程域组合根 `BcCourseConfiguration` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.config` 不变；Bean 定义/装配顺序不变；落地：`49477dd1`）。该步用于为后续 `eva-app/pom.xml` 去 `bc-course` 依赖创造前置条件。
  10) ✅ 延伸主线（为后续继续归位端口适配器/旧入口做准备，保持行为不变）：将课程域 `CourseBizConvertor` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor.course` 不变；仅搬运/编译闭合；落地：`eec5d45c`），以便后续把依赖该转换器的课程域端口适配器/旧入口逐步归位到 `bc-course-infra`，继续削减 `eva-app` 的课程域编译期引用面。
  11) ✅ 延伸主线（课程读侧端口适配器归位，保持行为不变）：将 `CourseDetailQueryPortImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.bccourse.adapter` 不变；实现逻辑不变；落地：`250002d5`），继续削减 `eva-app` 的课程域编译期引用面。
  12) ✅ 延伸主线（事务提交后事件发布器归位，保持行为不变）：将 `AfterCommitEventPublisher` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package edu.cuit.app.event` 不变；逻辑不变；落地：`fc85f548`），用于为后续归位课程旧入口/端口适配器时闭合依赖，避免 `bc-course-infra` 反向依赖 `eva-app`。
  13) ✅ 延伸主线（分页转换器归位，保持行为不变）：将 `PaginationBizConvertor` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor` 不变；逻辑不变；落地：`c8c17225`），用于为后续归位课程旧入口/其它旧入口时闭合依赖，避免基础设施模块反向依赖 `eva-app`。
  14) ✅ 延伸主线（课程 Controller 注入收敛到接口，保持行为不变）：为避免 Controller 编译期绑定 `eva-app` 的实现类，先在 `eva-adapter` 的课程相关 Controller 子簇试点：将注入类型从 `edu.cuit.app.service.impl.course.*ServiceImpl` 收窄为 `shared-kernel` 下的 `edu.cuit.client.api.course.*Service` 接口（Spring 注入目标不变，仅减少编译期耦合；落地：`47a6b06c`）。
  15) ✅ 延伸主线（课程旧入口归位：ICourseServiceImpl，保持行为不变）：将 `ICourseServiceImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变，仅搬运与编译闭合；不改业务语义/异常文案/副作用顺序）。为闭合 `StpUtil`（Sa-Token）编译期依赖，在 `bc-course-infra` 补齐 `zym-spring-boot-starter-security`（运行时 classpath 已存在，保持行为不变；落地：`2b5bcecb`）。
  16) ✅ 延伸主线（课程旧入口归位：IUserCourseServiceImpl，保持行为不变）：将 `IUserCourseServiceImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变；仅搬运与编译闭合，不改业务语义/异常文案/副作用顺序）。为避免 `bc-course-infra` 反向依赖 `eva-app`，将 `IUserCourseServiceImpl` 对 `UserCourseDetailQueryExec/FileImportExec` 的依赖收敛为类内私有方法（逻辑逐行对齐原实现；保持行为不变；落地：`79a351c3`）。
  17) ✅ 延伸主线（课程旧入口归位：ICourseDetailServiceImpl，保持行为不变）：将 `ICourseDetailServiceImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变，仅搬运与编译闭合；不改业务语义/异常文案/副作用顺序；落地：`bd85a006`）。
  18) ✅ 延伸主线（依赖方编译期依赖收敛：eva-app 去 bc-course，保持行为不变）：在 Serena + `rg` 证伪 `eva-app` 不再引用 `edu.cuit.bc.course.*` 后，已完成将 `eva-app/pom.xml` 的 `bc-course` 编译期依赖替换为 `shared-kernel`（即移除 `bc-course` 依赖；每次只改 1 个 `pom.xml`；最小回归通过；落地：`6fe8ffc8`）。

#### bc-messaging（消息域）后置规划（仅规划，不落地；保持行为不变）

> 背景：截至 2026-01-02，`bc-messaging` 已完成组合根/监听器/端口适配器归位与依赖收敛关键环节（见 `NEXT_SESSION_HANDOFF.md` 0.9），本段保留为“后置结构折叠/依赖证伪”清单（保持行为不变）。
> 目标：若后续需要继续推进，优先做 S0 结构折叠与依赖面证伪；当前会话不落地“新增折叠试点”类提交，避免分散主线（保持行为不变）。

- 现状散落点（Serena 证据化盘点，2025-12-29）：
  - 组合根：✅ 已归位到 `bc-messaging/src/main/java/edu/cuit/app/config/BcMessagingConfiguration.java`（保持 `package` 不变；落地：`4e3e2cf2`）。
  - 应用侧监听器：✅ `bc-messaging/src/main/java/edu/cuit/app/bcmessaging/CourseOperationSideEffectsListener.java`（落地：`22ee30e7`）；✅ `bc-messaging/src/main/java/edu/cuit/app/bcmessaging/CourseTeacherTaskMessagesListener.java`（落地：`0987f96f`）
  - 应用侧端口适配器：✅ `bc-messaging/src/main/java/edu/cuit/app/bcmessaging/adapter/CourseBroadcastPortAdapter.java`（落地：`84ee070a`）；✅ `bc-messaging/src/main/java/edu/cuit/app/bcmessaging/adapter/TeacherTaskMessagePortAdapter.java`（落地：`9ea14cff`）；✅ `bc-messaging/src/main/java/edu/cuit/app/bcmessaging/adapter/EvaMessageCleanupPortAdapter.java`（落地：`73ab3f3c`；依赖类型收窄为 `IMsgService` 以避免 Maven 循环依赖，行为不变）
  - 基础设施端口适配器：✅ `bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageDeletionPortImpl.java`、✅ `bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageReadPortImpl.java`、✅ `bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageDisplayPortImpl.java`、✅ `bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageInsertionPortImpl.java`、✅ `bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageQueryPortImpl.java`（均已归位）。
  - 对应应用层端口（用于建立“端口→适配器”映射）：`bc-messaging/src/main/java/edu/cuit/bc/messaging/application/port/*Port.java`
  - 迁移前置：为避免应用侧端口适配器对 `eva-app` 的编译期耦合导致 Maven 循环依赖，已先将消息发送组装类 `MsgResult` 从 `eva-app` 归位到 `bc-messaging-contract`（保持包名不变；落地：`31878b61`）。
  - 迁移前置（DAL/Convertor 归位，保持行为不变）：消息表数据对象 `MsgTipDO` 与 Mapper `MsgTipMapper`（含 `MsgTipMapper.xml`）已从 `eva-infra` 归位到 `eva-infra-dal`（保持 `package/namespace` 不变；后续已进一步将 `MsgTipMapper` 归位到 `bc-messaging`：`4af9f9fc`，将 `MsgTipDO` 归位到 `bc-messaging`：`87b38a55`；均仅改变 Maven 模块归属）；消息转换器 `MsgConvertor` 已从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package` 不变；后续已进一步归位到 `bc-messaging`，仅改变 Maven 模块归属：`312756c7`）。本阶段基础设施端口适配器已全部归位完成，后续可转入“依赖收敛/结构折叠”等更高收益里程碑（仍保持行为不变）。
  - ✅ S0.2 延伸（DAL 拆散试点：消息 `msg_tip` 收尾，保持行为不变，单资源闭环）：`MsgTipMapper.xml` 已从 `eva-infra-dal/src/main/resources/mapper/MsgTipMapper.xml` 归位到 `bc-messaging/src/main/resources/mapper/MsgTipMapper.xml`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`5c5ab5e0`）。
  - 依赖收敛准备（事件枚举下沉到 contract，保持行为不变）：将 `CourseOperationMessageMode` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；落地：`b2247e7f`）。
  - 依赖收敛准备（事件载荷下沉到 contract，保持行为不变）：将 `CourseOperationSideEffectsEvent` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；落地：`ea2c0d9b`）。
  - 依赖收敛准备（事件载荷下沉到 contract，保持行为不变）：将 `CourseTeacherTaskMessagesEvent` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；落地：`12f43323`）。
  - 依赖收敛（应用侧编译期依赖面收窄，保持行为不变）：`eva-app` 对消息域的 Maven 编译期依赖已收敛为 0（先收敛为仅依赖 `bc-messaging-contract`：`d3aeb3ab`；后续在证伪无引用后移除 `bc-messaging-contract`：`b92314ef`）。
- 下一步建议（依赖收敛后半段，保持行为不变）：✅ 已完成：在 `start/pom.xml` 补齐 `bc-messaging` 的 `runtime` 依赖（保持行为不变；落地：`f23254ec`）；✅ 已完成：移除 `eva-infra/pom.xml` 中 `bc-messaging` 的 `runtime` 依赖，把“运行时装配责任”上推到组合根 `start`（保持行为不变；落地：`507f95b2`）。✅ 已完成后置建议：使用 Serena 证伪 `eva-infra` 无 `bcmessaging` / `edu.cuit.bc.messaging` / `bc-messaging` 的编译期引用，且运行时装配由 `start` 承接（证据见 `NEXT_SESSION_HANDOFF.md` 0.9；保持行为不变）。

- 建议拆分提交（每步：Serena 符号级引用分析 → 最小回归 → commit → 三文档同步）：
  1) **盘点与证据化（只改文档）**：用 Serena 列出 `eva-app/eva-infra` 中与消息域相关的散落点与引用面（优先：`BcMessagingConfiguration`、`CourseOperationSideEffectsListener`、`CourseTeacherTaskMessagesListener`、`eva-app/.../bcmessaging/adapter/*`、`eva-infra/.../bcmessaging/adapter/*`），形成“迁移清单 + 风险点”。
  2) ✅ **组合根归位**：已将 `eva-app/src/main/java/edu/cuit/app/config/BcMessagingConfiguration.java` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.config` 不变；Bean 定义/装配顺序不变；最小回归通过；落地：`4e3e2cf2`）。
  3) **应用侧适配器/监听器归位**：将 `eva-app/src/main/java/edu/cuit/app/bcmessaging/*` 与 `eva-app/src/main/java/edu/cuit/app/bcmessaging/adapter/*` 逐个迁移到 `bc-messaging`（建议保持 `package` 不变；每次只迁 1 个类并补齐/复用可运行回归）。补充进展：已完成监听器 `CourseOperationSideEffectsListener`（`22ee30e7`）与 `CourseTeacherTaskMessagesListener`（`0987f96f`）迁移。
  4) **基础设施端口适配器归位（后置）**：将 `eva-infra/src/main/java/edu/cuit/infra/bcmessaging/adapter/*PortImpl.java` 逐步迁移到 `bc-messaging` 的基础设施落点（理想形态为 `bc-messaging/infrastructure` 子模块；在当前“主线优先、不新增试点”策略下，先仅规划与盘点依赖闭包，待评教读侧阶段性收敛后再落地）。
  5) **依赖收敛（后置）**：在迁移端口适配器后，逐步让 `bc-messaging` 的编译期依赖从 `eva-infra` 收敛为更小的 `eva-infra-dal` + `eva-infra-shared`（或完全无 `eva-infra` 依赖），并保持 `start` 装配不变。

- 下一步建议（从下一会话起；每步 1 次最小回归 + 1 次提交 + 文档同步；保持行为不变）：
		  - 结构性里程碑 S0（需求变更，2025-12-24）：将“BC=一个顶层聚合模块、内部 `domain/application/infrastructure` 为子模块”的结构落地到真实目录与 Maven 结构中，并把历史平铺过渡模块（`bc-iam-infra`、`bc-evaluation-infra` 等）折叠归位到对应 BC 内部子模块（每步可回滚；保持行为不变）。
			  - ✅ 进展（2025-12-25）：已以 `bc-iam` 为试点落地 `bc-iam-parent` + `domain/application/infrastructure` 子模块，并折叠归位 `bc-iam-infra` 到 `bc-iam/infrastructure`（artifactId/包名保持不变；最小回归通过；落地提交：`0b5c5383`）。
			  - ✅ 进展（2025-12-27）：已以 `bc-template` 为试点落地 `bc-template-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-template`；包名不变；最小回归通过；落地提交：`65091516`）。
				  - ✅ 进展（2025-12-27）：已以 `bc-course` 为试点落地 `bc-course-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-course`；包名不变；最小回归通过；落地提交：`e90ad03b`）。
				  - ✅ 进展（2025-12-27）：已以 `bc-ai-report` 为试点完成 S0 阶段 1：引入 `bc-ai-report-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-ai-report`；保持 `package` 不变；最小回归通过；落地提交：`e14f4f7a`）。
				  - ✅ 进展（2025-12-27）：已完成 S0 阶段 2：将 AI 报告端口适配器/导出实现/AI 基础设施搬运到 `bc-ai-report/infrastructure` 子模块，并补齐 `eva-app` → `bc-ai-report-infra` 依赖以保证装配（保持行为不变；最小回归通过；落地提交：`444c7aca`）。
					  - ✅ 进展（2025-12-27）：已以 `bc-audit` 为试点完成 S0 阶段 1：引入 `bc-audit-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-audit`；保持 `package` 不变；最小回归通过；落地提交：`81594308`）。
					  - ✅ 进展（2025-12-27）：已完成 `bc-audit` 的 S0 阶段 2：将 `edu.cuit.infra.bcaudit.adapter.LogInsertionPortImpl` 从 `eva-infra` 搬运到 `bc-audit/infrastructure` 子模块，并补齐 `eva-app` → `bc-audit-infra` 依赖以保证装配（保持行为不变；最小回归通过；落地提交：`d7858d7a`）。
					  - ✅ 进展（2025-12-27）：已完成 `bc-audit` 的 S0 阶段 3（可选）：抽离 `sys_log` 相关 DAL（`SysLog*DO/Mapper/XML`）到 `eva-infra-dal`、抽离 `LogConverter` 到 `eva-infra-shared`，并移除 `bc-audit-infra` → `eva-infra` 的过渡依赖（保持行为不变；最小回归通过；落地提交：`06ec6f3d`）。
						  - 下一步建议（仍保持行为不变，可选）：在确认装配稳定后，继续推进评教读侧解耦（方向 A → B）：
									  - A（继续收窄依赖）：✅ 已证伪“`eva-app` 仍注入记录聚合端口 `EvaRecordQueryPort`”（除端口定义与实现外无引用）。后续若出现新的应用层引用点（尤其是导出装饰器/扩展点），再按“先补齐子端口组合（仅新增接口 + `extends`）→ 再收窄注入类型”的同套路处理（每次只改 1 个类 + 1 个可运行单测）。
										  - ✅ 进展（2026-01-01，记录读侧依赖收窄）：`EvaRecordServiceImpl.pageEvaRecord` 内联分页结果组装，移除对 `PaginationBizConvertor` 的注入依赖（分页字段赋值顺序/异常文案/循环副作用顺序不变；保持行为不变；最小回归通过；落地提交：`55103de1`）。
										  - ✅ 进展（2026-01-01，记录读侧用例归位深化/D1）：新增 `EvaRecordQueryUseCase`，并将 `EvaRecordServiceImpl.pageEvaRecord` 退化为纯委托壳，把“实体→CO 组装 + 平均分填充 + 分页组装”的编排逻辑归位到 UseCase（保持 `@CheckSemId` 触发点不变；异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地提交：`86772f59`）。
										  - ✅ 进展（2026-01-01，记录读侧用例归位深化/D1）：对齐 `EvaRecordQueryUseCase` 内部“实体→CO 组装”的求值顺序，避免提前触发 `Supplier` 缓存加载导致副作用顺序漂移（保持行为不变；最小回归通过；落地提交：`10991314`）。
										  - ✅ 进展（2026-01-02，用户评教记录读侧用例归位深化/D1）：新增 `UserEvaQueryUseCase`，并将旧入口 `UserEvaServiceImpl.getEvaLogInfo/getEvaLoggingInfo` 退化为纯委托壳（保留 `@CheckSemId` 与当前用户解析：`StpUtil` + `userQueryGateway`；异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地提交：`96e65019`）。
										  - ✅ 进展（2025-12-28，统计读侧用例归位深化）：将 `EvaStatisticsServiceImpl.getTargetAmountUnqualifiedUser` 的 `type` 分支选择与阈值选择归位到 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser`（保持 `@CheckSemId` 触发点仍在旧入口；异常文案与副作用顺序不变；保持行为不变；最小回归通过；落地提交：`5b20d44e`）。
										  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `evaScoreStatisticsInfoOrEmpty`，将 “`Optional.empty` → `new EvaScoreInfoCO()`” 的空对象兜底先归位到用例层（保持行为不变；为下一步旧入口退化为纯委托壳做准备；最小回归通过；落地提交：`bce01df2`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.evaScoreStatisticsInfo` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaScoreStatisticsInfoOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`1bf3a4fe`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `evaTemplateSituationOrEmpty`，将 “`Optional.empty` → `new EvaSituationCO()`” 的空对象兜底先归位到用例层（保持行为不变；为下一步旧入口退化为纯委托壳做准备；最小回归通过；落地提交：`89b6b1ee`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.evaTemplateSituation` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaTemplateSituationOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`78abf1a1`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `evaWeekAddOrEmpty`，将 “`Optional.empty` → `new EvaWeekAddCO()`” 的空对象兜底先归位到用例层（保持行为不变；为下一步旧入口退化为纯委托壳做准备；最小回归通过；落地提交：`5a8ac076`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.evaWeekAdd` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaWeekAddOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`2a92ca0b`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `getEvaDataOrEmpty`，将 “`Optional.empty` → `new PastTimeEvaDetailCO()`” 的空对象兜底先归位到用例层（保持行为不变；为下一步旧入口退化为纯委托壳做准备；最小回归通过；落地提交：`1180a0f7`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：为 `EvaStatisticsQueryUseCase.getEvaDataOrEmpty` 补齐 `Optional.empty` 时返回空对象的读侧用例级测试，并保留阈值读取顺序与委托顺序验证（保持行为不变；最小回归通过；落地提交：`a44a9bba`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.getEvaData` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.getEvaDataOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`b59db93d`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `getTargetAmountUnqualifiedUserOrEmpty`，将 “`Optional.empty` → `new UnqualifiedUserResultCO().setTotal(0).setDataArr(List.of())`” 的空对象兜底先归位到用例层（保持行为不变；为下一步旧入口退化为纯委托壳做准备；最小回归通过；落地提交：`0ac65fb4`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.getTargetAmountUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`b931b247`）。
										  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：为 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty(type=1)` 补齐空结果兜底的读侧用例级测试（保持行为不变；最小回归通过；落地提交：`8f3e7afe`）。
										  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：为 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty(invalidType)` 补齐异常文案不变的读侧用例级测试，并确保不触发查询端口调用（保持行为不变；最小回归通过；落地提交：`0cb2caec`）。
										  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.pageUnqualifiedUserAsPaginationQueryResult`，并移除对 `PaginationBizConvertor` 的依赖，从而把分页结果组装彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`f4f3fcde`）。
											  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `pageUnqualifiedUserAsPaginationQueryResult`，把“`PaginationResultEntity` → `PaginationQueryResultCO`”的分页结果组装逻辑先归位到用例层，为下一步旧入口 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 退化为纯委托壳并去除 `PaginationBizConvertor` 依赖做准备（保持行为不变；最小回归通过；落地提交：`e97615e1`）。
											  - 建议切分路径（仍保持行为不变，可回滚）：统计用例归位遵循“每次只迁 1 个方法簇”的节奏；`pageUnqualifiedUser`（已完成）→ `getTargetAmountUnqualifiedUser`（已完成）→ ✅ `getTargetAmountUnqualifiedUser` 的空对象兜底归位到 UseCase（补齐重载：`0ac65fb4`；旧入口委托：`b931b247`）→ ✅ `getEvaData` 的阈值计算/参数组装归位（已完成；落地：`8f4c07c5`）→ ✅ `getEvaData` 的空对象兜底归位到 UseCase（补齐重载：`1180a0f7`；旧入口委托：`b59db93d`）→ ✅ 补齐 unqualifiedUser 的“参数组装重载”（读取 `EvaConfigGateway.getEvaConfig()`；落地：`0a2fec4d`）→ ✅ 旧入口 `EvaStatisticsServiceImpl` 委托重载并去除对 `EvaConfigGateway` 的直接依赖（已完成；落地：`21f6ad5b`）→ ✅ `evaScoreStatisticsInfo` 的空对象兜底归位到 UseCase（补齐重载：`bce01df2`；旧入口委托：`1bf3a4fe`）→ ✅ `evaTemplateSituation` 的空对象兜底归位到 UseCase（补齐重载：`89b6b1ee`；旧入口委托：`78abf1a1`）→ ✅ `evaWeekAdd` 的空对象兜底归位到 UseCase（补齐重载：`5a8ac076`；旧入口委托：`2a92ca0b`）→ 下一步继续推进 B：将统计读侧其它方法簇的“默认值兜底/空对象组装”逐步归位到 `EvaStatisticsQueryUseCase`（每次只迁 1 个方法；旧入口仍保留 `@CheckSemId` 触发点不变）。
										  - 下一步建议（保持行为不变，每次只改 1 个类 + 1 个可运行回归）：可选低风险收尾：将 `EvaStatisticsServiceImpl.exportEvaStatistics` 也退化为委托壳，改为调用 `EvaStatisticsQueryUseCase` 新增的导出用例方法（内部仍调用 `EvaStatisticsExcelFactory.createExcelData(semId)`；保持 `@CheckSemId` 触发点不变）。
								  - ✅ 进展（2025-12-28，记录读侧依赖类型收窄—AI 报告分析）：将 AI 报告分析端口适配器 `AiReportAnalysisPortImpl` 对记录端口的依赖类型从聚合接口 `EvaRecordQueryPort` 收窄为子端口 `EvaRecordExportQueryPort`（保持行为不变；最小回归通过；落地提交：`4fe38934`）。
							  - 工具提示：若 Serena MCP 工具调用持续 `TimeoutError`，需要在交接文档中记录降级原因，并用本地 `rg` 提供可复现的引用证据（仍不改变业务语义）；同时在下一会话优先排查恢复 Serena，以回到“符号级引用分析”流程。
					  - 结构性里程碑 S0.1（需求变更，2025-12-24）：逐步拆解 `eva-client`：按 BC 归属迁移 BO/CO/DTO（允许改包名以归位到 BC 的 `application/contract/dto`）；新增对象不再进入 `eva-client`；跨 BC 通用对象沉淀到 shared-kernel（每步可回滚；保持行为不变）。
				    - ✅ 进展（2025-12-25）：已在 `bc-iam` 下新增 `bc-iam-contract` 子模块，并将 IAM 协议对象（`api/user/*` + `dto/cmd/user/*`）从 `eva-client` 迁移到 `bc-iam/contract`（包名归位到 `edu.cuit.bc.iam.application.contract...`；全仓库引用已更新；最小回归通过；落地提交：`dc3727fa`）。
		    - 下一步（建议顺序，仍按“每步=最小回归+提交+三文档同步”）：
		      1) ✅ 以 `bc-evaluation` 作为 S0 试点：折叠 `bc-evaluation-infra` → `bc-evaluation/infrastructure`，落地 `bc-evaluation-parent` + `domain/application/infrastructure`（已完成；落地提交：`4db04d1c`）。
			      2) 在 `bc-iam-contract` 继续迁移 IAM 专属协议对象：优先 `dto/clientobject/user/*` 与 IAM 查询条件。
				         - 进展：已迁移 `UserInfoCO/UserDetailCO/RoleInfoCO/SimpleRoleInfoCO/MenuCO/GenericMenuSectionCO/RouterDetailCO/RouterMeta` 到 `edu.cuit.bc.iam.application.contract.dto.clientobject.user`（保持行为不变；落地提交：`c1a51199`）。
				         - 进展：已迁移 IAM 查询条件 `MenuConditionalQuery` 到 `bc-iam-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`1eda37c9`）。
				         - 进展（2025-12-26）：已将 IAM 专属接口 `IDepartmentService` 从 `eva-client` 迁移到 `bc-iam-contract`（包名归位到 `edu.cuit.bc.iam.application.contract.api.department`；保持行为不变；最小回归通过；落地提交：`656dc36e`）。
				         - 进展：已在“可证实不再需要”的前提下移除 `bc-iam/application` → `eva-client` 直依赖（保持行为不变；最小回归通过；落地提交：`7371ab96`）。
							         - ✅ 进展（2025-12-27）：已对 `eva-domain` 仍存在的 `import edu.cuit.client.*` 做“来源证伪”（Serena 盘点清单 → 逐项确认类型定义文件实际归属模块；包名保持不变）。结论：`eva-client` 不再提供业务类型；`eva-domain` 的 `edu.cuit.client.*` 引用类型分别由 `shared-kernel` / 各 BC contract / `bc-course` / `eva-domain` 自身承载（保持行为不变）。
				         - 进展（2025-12-26）：已将课程写侧命令 `edu.cuit.client.dto.cmd.course/*` 与导入课表 BO `CourseExcelBO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变），并为 `eva-infra-shared` 增加 `bc-course` 显式依赖以闭合编译依赖（保持行为不变；最小回归通过；落地提交：`8a591703`）。
				         - 进展（2025-12-26）：已将学期协议接口 `ISemesterService` 与学期 CO `SemesterCO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`7b5997c1`）。
				         - 进展（2025-12-26）：已将通用学期入参 `Term` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`f401dcb9`）。
				         - 进展（2025-12-26）：已将课程查询 Query 对象 `CourseQuery/CourseConditionalQuery/MobileCourseQuery` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`84a6a536`）。
				         - 进展（2025-12-27）：已将教室接口 `IClassroomService` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`59471a96`）；后续为支撑“依赖方去 `bc-course`”，已进一步下沉到 `shared-kernel`（保持 `package` 不变；保持行为不变；落地提交：`38f58e0a`）。
				         - 进展（2025-12-27）：已将 AI 接口与 BO（`IAiCourseAnalysisService/AiAnalysisBO/AiCourseSuggestionBO`）从 `eva-client` 迁移到 `bc-ai-report`，并移除 `bc-ai-report` → `eva-client` 依赖（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`badb9db6`）。
				         - 进展（2025-12-27）：已将 `EvaProp` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`4feabdd0`）。
				         - 进展（2025-12-26）：已移除 `eva-domain` → `eva-client` 的 Maven **直依赖**（保持行为不变；最小回归通过；落地提交：`9ff21249`）。
				         - 进展（2025-12-26）：已将消息入参 DTO `GenericRequestMsg` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`8fc7db99`）。
				         - 进展（2025-12-26）：已将课程数据对象 `CoursePeriod/CourseType` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`5629bd2a`）。
				         - 进展（2025-12-26）：已将课程 CO `SingleCourseCO` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ccc82092`）。
				         - 进展（2025-12-26）：已将课程域 `clientobject/course` 残留 CO（`ModifySingleCourseDetailCO/RecommendCourseCO/SelfTeachCourseCO/SelfTeachCourseTimeCO/SelfTeachCourseTimeInfoCO/SubjectCO/TeacherInfoCO`）以及 `SimpleCourseResultCO/SimpleSubjectResultCO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ce1a0a90`）。
				         - 进展（2025-12-26）：已将消息域 response DTO（`GenericResponseMsg/EvaResponseMsg`）从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ecb8cee5`）。
				         - 进展（2025-12-26）：已将消息域 `IMsgService/SendMessageCmd/MessageBO` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`431a5a23`）。
				         - 进展（2025-12-26）：已将 `SendWarningMsgCmd` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e6aa5913`）。
				         - 进展（2025-12-26）：已将消息域 clientobject `EvaMsgCO` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`2f257a86`）。
						         - ✅ 进展（2025-12-27）：已在完成来源证伪的前提下选择方案 B，并从 root reactor 移除 `eva-client` 模块（保持行为不变；最小回归通过；落地提交：`ce07d75f`）；并进一步从仓库移除 `eva-client/` 目录（保持行为不变；落地提交：`de25e9fb`）。
			      3) 在 `bc-evaluation/contract` 继续迁移评教域协议对象：优先 `edu.cuit.client.dto.clientobject.eva.*` 与 `UnqualifiedUserConditionalQuery`（建议先保持 `package` 不变以降风险；保持行为不变）。
			         - 进展：已将评教统计接口 `IEvaStatisticsService` + `UnqualifiedUserInfoCO/UnqualifiedUserResultCO` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；落地提交：`978e3535`）。
			         - 进展：已将 `edu.cuit.client.dto.clientobject.eva.*` + `UnqualifiedUserConditionalQuery` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变）；并将评教 API 接口（`IEvaConfigService/IEvaRecordService/IEvaTaskService/IEvaTemplateService/IUserEvaService`）迁移到 `bc-evaluation/contract`（保持 `package` 不变）；同时将课程域协议接口与 CO（`edu.cuit.client.api.course.*`、`CourseDetailCO/CourseModelCO/SingleCourseDetailCO`）从 `eva-client` 迁移到 `bc-course`，避免 `eva-client` 反向依赖评教 CO（保持行为不变；最小回归通过；落地提交：`6eb0125d`）。
				         - 进展：已将评教查询条件 `EvaTaskConditionalQuery/EvaLogConditionalQuery` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`d02d5522`）。
				         - 进展：已将评教 `dto/cmd/eva/*` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`2273ad61`）。
				         - 进展：已将 `EvaConfig` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`438d38bf`）。
				         - 进展：已将 `DateEvaNumCO/TimeEvaNumCO/MoreDateEvaNumCO/SimpleEvaPercentCO/SimplePercentCO/FormPropCO` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`c2d8a8b1`）。
				         - 进展：已将课程时间模型 `dto/data/course/CourseTime` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`5f21b5ce`）。
				         - 进展：已在“可证实不再需要”的前提下移除 `bc-evaluation-contract` → `eva-client` 直依赖（保持行为不变；最小回归通过；落地提交：`cf2001ef`）。
				         - 进展：已在“可证实不再需要”的前提下移除 `bc-evaluation/application` → `eva-client` 直依赖（保持行为不变；最小回归通过；落地提交：`10e8eb0b`）。
				         - 进展（2025-12-26）：Serena 盘点确认 `eva-client` 下评教专属目录（`api/eva`、`dto/cmd/eva`、`dto/clientobject/eva`）已迁空（盘点闭环；最小回归通过；落地提交：`e643bac9`）。
				         - 下一步（建议顺序，保持行为不变）：继续迁移评教域仍留在 `eva-client` 的协议对象（P1.2 继续推进；建议优先保持 `package` 不变以降风险）。若评教专属目录已迁空，则继续清理评教 BC 其它子模块（如 `bc-evaluation/domain` / `bc-evaluation/infrastructure`）对 `eva-client` 的直依赖：先用 Serena 盘点引用面，再逐步移除 Maven 依赖（每步最小回归+提交+三文档同步）。
				      4) 在 `bc-audit` 继续迁移审计日志协议对象（保持行为不变）：
				         - 进展：已将审计日志协议对象 `ILogService/OperateLogCO/LogModuleCO` 从 `eva-client` 迁移到 `bc-audit`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e1dbf2d4`）。
				         - 进展（2025-12-26）：已用 Serena 盘点确认 `bc-audit` 对 `eva-client` 的实际依赖面仅剩 `SysLogBO`（`PagingQuery/GenericConditionalQuery/PaginationQueryResultCO` 已在 `shared-kernel`，`ILogService/OperateLogCO/LogModuleCO` 已在 `bc-audit`）。
				         - 进展（2025-12-26）：已将 `SysLogBO` 从 `eva-client` 迁移到 `eva-domain`（保持 `package edu.cuit.client.bo` 不变；保持行为不变），为移除 `bc-audit` → `eva-client` 直依赖做准备。
				         - 进展（2025-12-26）：已移除 `bc-audit` → `eva-client` 的 Maven **直依赖**（保持行为不变；最小回归通过）。
				         - 备注：P1.2-3 已闭环（本条后续仅做“进一步削减依赖路径”的可选推进）。
				         - 下一步：继续评估 `eva-domain`（以及 `eva-infra` 侧）对 `eva-client` 的残留依赖是否仅为已迁移到 `shared-kernel` 的通用类型；若“可证实不再需要”，再逐步削减对 `eva-client` 的依赖路径（保持行为不变；每步=最小回归+提交+三文档同步）。
				      5) 对 `PagingQuery/GenericConditionalQuery/SimpleResultCO/PaginationQueryResultCO` 等通用对象先用 Serena 盘点引用范围：跨 BC 则沉淀 shared-kernel，避免误迁到某个 BC。
				         - 进展：已新增 `shared-kernel` 子模块，并将 `PagingQuery/ConditionalQuery/GenericConditionalQuery/SimpleResultCO/PaginationQueryResultCO` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`a25815b2`）。
				         - 进展：`bc-iam-contract` / `bc-evaluation-contract` 已增加对 `shared-kernel` 的直依赖（暂保留 `eva-client` 以可回滚；最小回归通过；落地提交：`3a0ac086`）。
				         - 进展：已将 `ValidStatus/ValidStatusValidator` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`686de369`）。
				         - 进展：`bc-iam-contract` 已去除对 `eva-client` 的直依赖（保持行为不变；最小回归通过；落地提交：`8d673c17`）。
				         - 下一步：逐步让各 BC 的 `contract` 在“可证实不再需要”的前提下分阶段削减对 `eva-client` 的依赖范围（保持行为不变）。
  - ✅ 提交点 0（纯文档闭环）：补齐“条目 25”的定义/边界与验收口径（只改文档，不改代码；落地提交：`1adc80bd`），避免新会话对 24/25/26 的分界理解不一致
  - ✅ 提交点 A：启动 `bc-ai-report` / `bc-audit` 最小骨架并接入组合根（仅落点，不迁业务语义；落地提交：`a30a1ff9`）
  - ✅ 提交点 B：为 AI 报告或审计日志挑选 1 条写链路，按“用例 + 端口 + 适配器 + 旧 gateway 委托壳”收敛（审计日志写入：`LogGatewayImpl.insertLog`；保持行为不变；落地提交：`b0b72263`）
  - ✅ 提交点 C（统计主题，第一步）：读侧拆分 `EvaQueryRepo` 的统计接口为 `EvaStatisticsQueryRepo`，并让 `EvaStatisticsQueryPortImpl` 依赖收敛（统计口径不变；落地提交：`d5b07247`）
  - ✅ 提交点 C2（记录主题）：读侧拆分 `EvaQueryRepo` 的记录接口为 `EvaRecordQueryRepo`，并让 `EvaRecordQueryPortImpl` 依赖收敛（口径/异常文案不变；落地提交：`cae1a15c`）
  - ✅ 提交点 C3（任务主题）：读侧拆分 `EvaQueryRepo` 的任务接口为 `EvaTaskQueryRepo`，并让 `EvaTaskQueryPortImpl` 依赖收敛（口径/异常文案不变；落地提交：`82427967`）
  - ✅ 提交点 C4（模板主题）：读侧拆分 `EvaQueryRepo` 的模板接口为 `EvaTemplateQueryRepo`，并让 `EvaTemplateQueryPortImpl` 依赖收敛（口径/异常文案不变；落地提交：`889ec9b0`）
  - ✅ 提交点 B2（AI 报告写链路，导出）：`AiCourseAnalysisService.exportDocData` 收敛为“用例 + 端口 + 端口适配器 + 旧入口委托壳”（日志/异常文案不变；落地提交：`c68b3174`）
  - ✅ 提交点 B3（AI 报告写链路，导出）：`AiCourseAnalysisService.exportDocData` 进一步退化为“纯委托壳”（日志/异常文案不变；落地提交：`7f4b3358`）
  - ✅ 提交点 B4（AI 报告写链路，analysis）：`AiCourseAnalysisService.analysis` 收敛为“用例 + 端口 + 端口适配器 + 旧入口委托壳”（保持 `@CheckSemId` 切面触发点不变；日志/异常文案不变；落地提交：`a8150e7f`）
  - ✅ 提交点 B5（AI 报告写链路，用户名解析）：`ExportAiReportDocByUsernameUseCase` 将 username → userId 查询抽为 `bc-ai-report` 端口 + `eva-app` 端口适配器（异常文案与日志顺序保持不变；落地提交：`d7df8657`）
  - ✅ 提交点 C5-1（统计主题实现拆分）：新增 `EvaStatisticsQueryRepository` 承接 `EvaStatisticsQueryRepo` 实现，`EvaQueryRepository` 的统计方法退化为委托（口径/异常文案不变；落地提交：`9e0a8d28`）
  - ✅ 提交点 C5-2（记录主题实现拆分）：新增 `EvaRecordQueryRepository` 承接 `EvaRecordQueryRepo` 实现，`EvaQueryRepository` 的记录方法退化为委托（口径/异常文案不变；落地提交：`985f7802`）
  - ✅ 提交点 C5-3（任务主题实现拆分）：新增 `EvaTaskQueryRepository` 承接 `EvaTaskQueryRepo` 实现，`EvaQueryRepository` 的任务方法退化为委托（口径/异常文案不变；落地提交：`d467c65e`）
  - ✅ 提交点 C5-4（模板主题实现拆分）：新增 `EvaTemplateQueryRepository` 承接 `EvaTemplateQueryRepo` 实现，`EvaQueryRepository` 的模板方法退化为委托（口径/异常文案不变；落地提交：`a550675a`）
  - ✅ 提交点 D1（`bc-evaluation-infra` 阶段 1：评教读侧查询迁移 + DAL/shared 拆分）：引入 `bc-evaluation-infra` 并迁移评教读侧查询（QueryPortImpl + QueryRepo/Repository），同时将 course/eva DAL（DO/Mapper/XML）迁移到 `eva-infra-dal`、将 `CourseConvertor`/`EvaConvertor`/`EvaCacheConstants`/`CourseFormat` 迁移到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`be6dc05c`）
- ✅ 提交点 D2（`bc-evaluation-infra` 阶段 2：评教写侧端口适配器迁移）：迁移 `eva-infra/src/main/java/edu/cuit/infra/bcevaluation/repository/*` 到 `bc-evaluation-infra`；并将 `CalculateClassTime` 迁移到 `eva-infra-shared` 以保持 `bc-evaluation-infra` 不依赖 `eva-infra`（保持包名/行为不变；落地提交：`24e7f6c9`）
- ✅ 提交点 C-1（后续，可选，读侧门面加固）：清理 `EvaQueryRepository` 中已无引用的历史私有实现/冗余依赖，使其成为纯委托壳（保持口径/异常文案不变；落地提交：`73fc6c14`；三文档同步：`083b5807`）
- ✅ 提交点 C-2（后续，可选，读侧仓储瘦身）：继续盘点评教读侧四主题仓储（`bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/bcevaluation/query/*QueryRepository.java`）残留的历史私有实现/冗余依赖，按“每次只删可证实无引用代码”的原则逐步清理（仍保持口径/异常文案不变；每步最小回归+提交+三文档同步；进展：已完成 C-2-1：`e2a2a717`；已完成 C-2-2：`8b76375f`；已完成 C-2-3：`4a317344`；已完成 C-2-4：`dba6e31d`；已完成 C-2-5：`5c1a03bc`；结论：四主题仓储未发现可证实无引用项，因此关闭 C-2）
- 提交点 C（后续，可选，剩余）：在 C5-4 完成后，视情况继续盘点评教读侧 query 实现残留私有工具/实体组装并内聚到对应主题仓储（仍保持口径/异常文案不变）

#### 条目 25（定义 / 边界 / 验收口径）

> 定位：条目 25 是“AI 报告 / 审计日志模块化试点”的会话推进索引，用于承接提交点 A/B（而提交点 C 属于读侧拆分，不属于条目 25 的范围）。

- **定义**：AI 报告 / 审计日志模块化试点（目标是提供 BC 落点与 wiring，并按既有套路先收敛 1 条写链路，行为保持不变）。
- **边界**：
  - **包含**：提交点 A + 提交点 B。
  - **不包含**：提交点 C；以及任何业务语义调整（缓存/日志/异常文案/副作用顺序完全不变）。
- **验收口径**：
  1) 组合根 wiring 完整，不引入 Maven 循环依赖；  
  2) 提交点 B 所选链路满足“UseCase + Port + Port Adapter + 旧 gateway 委托壳”的收敛套路；  
  3) 最小回归通过（命令以 `NEXT_SESSION_HANDOFF.md` 0.10 为准）。

- IAM 域：开始引入 `bc-iam`，已收敛 `UserUpdateGatewayImpl.assignRole/createUser/updateInfo/updateStatus/deleteUser`（落地提交：`16ff60b6/b65d311f/a707ab86`、`c3aa8739/a3232b78/a26e01b3/9e7d46dd`、`38c31541/6ce61024/db0fd6a3/cb789e21`、`e3fcdbf0/8e82e01f/eb54e13e`、`5f08151c/e23c810a/cccd75a3/2846c689`，保持行为不变）。
- 系统管理读侧：`UserQueryGatewayImpl.fileUserEntity` 与基础查询能力（`findIdByUsername/findUsernameById/getUserStatus/isUsernameExist`）已收敛到 `bc-iam`（保持行为不变；落地提交：`3e6f2cb2/8c245098/92a9beb3`、`9f664229/38384628/de662d1c/8a74faf5`）。
- 系统管理读侧：`UserQueryGatewayImpl.findAllUserId/findAllUsername/allUser/getUserRoleIds` 已收敛到 `bc-iam`（保持行为不变；落地提交：`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`）。
- 系统管理写侧：角色写侧剩余入口已收敛到 `bc-iam`（保持行为不变；落地提交：`64fadb20`）。
- ✅ 中期里程碑（已完成）：`bc-iam-infra` 子模块骨架已接入组合根并完成 `bciam/adapter/*` 迁移，且阶段 2（IAM DAL 抽离 + shared 拆分 + 去依赖）已闭环（保持行为不变；阶段 1 落地：`42a6f66f/070068ec/03ceb685/02b3e8aa/6b9d2ce7/5aecc747/1c3d4b8c`；阶段 2 去依赖落地：`2ad911ea`）。
  - 阶段 2.1（已完成）：已用 Serena 盘点 `bc-iam-infra` 适配器对 IAM DAL 的直接依赖清单（后续迁移以此清单为准，避免漏搬/多搬）：
    - Mapper：`SysUserMapper`、`SysUserRoleMapper`、`SysRoleMapper`、`SysRoleMenuMapper`、`SysMenuMapper`
    - DO：`SysUserDO`、`SysUserRoleDO`、`SysRoleDO`、`SysRoleMenuDO`、`SysMenuDO`（其中 `SysUserDO/SysRoleDO/SysMenuDO` 已下沉到 `shared-kernel`，落地：`31e157cd`/`c5ba98b1`/`a9141bfe`）
    - XML：`eva-infra-dal/src/main/resources/mapper/user/SysUserMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysUserRoleMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysRoleMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysRoleMenuMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysMenuMapper.xml`
  - 阶段 2.2（已完成）：已在 `bc-iam-infra` 创建 DAL 包路径与资源目录骨架（不迁代码，仅提供后续迁移落点；保持行为不变）：
    - Java：`bc-iam-infra/src/main/java/edu/cuit/infra/dal/database/dataobject/user/package-info.java`、`bc-iam-infra/src/main/java/edu/cuit/infra/dal/database/mapper/user/package-info.java`
    - Resources：`bc-iam-infra/src/main/resources/mapper/user/.gitkeep`
  - 阶段 2.3（已完成）：新增共享 DAL 子模块 `eva-infra-dal`，并先迁移 `SysUser*`（DO/Mapper/XML）到该模块（保持包名/namespace/SQL 不变；保持行为不变）。
    - 说明：Serena 引用分析确认 `SysUserMapper` 被 `eva-infra` 内多个模块直接使用；若直接迁入 `bc-iam-infra` 并从 `eva-infra` 删除会引入 Maven 循环依赖风险，因此先抽离为共享 DAL 模块，以最小可回滚方式推进。
    - 新模块：`eva-infra-dal/pom.xml`
    - Java：`shared-kernel/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysUserDO.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysUserRoleDO.java`；`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysUserMapper.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysUserRoleMapper.java`
    - XML：`eva-infra-dal/src/main/resources/mapper/user/SysUserMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysUserRoleMapper.xml`
  - 阶段 2.4（已完成）：继续迁移 `SysRole*`/`SysRoleMenu*`（DO/Mapper/XML）到共享模块 `eva-infra-dal`（保持包名/namespace/SQL 不变；保持行为不变）。
    - Java：`shared-kernel/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysRoleDO.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysRoleMenuDO.java`；`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysRoleMapper.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysRoleMenuMapper.java`
    - XML：`bc-iam/infrastructure/src/main/resources/mapper/user/SysRoleMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysRoleMenuMapper.xml`
  - 阶段 2.5（已完成）：继续迁移 `SysMenu*`（DO/Mapper/XML）到共享模块 `eva-infra-dal`（保持包名/namespace/SQL 不变；保持行为不变）。
    - Java：`shared-kernel/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysMenuDO.java`；`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysMenuMapper.java`
    - XML：`bc-iam/infrastructure/src/main/resources/mapper/user/SysMenuMapper.xml`
  - 阶段 2.6（已完成）：盘点 `bc-iam-infra` 仍依赖 `eva-infra` 的类型清单（为后续移除依赖做最小闭包拆分输入；保持行为不变）：
    - 转换器：`edu.cuit.infra.convertor.PaginationConverter`（已迁至 `eva-infra-shared`：`54d5fecd`）、`edu.cuit.infra.convertor.user.{MenuConvertor,RoleConverter,UserConverter}`（已迁至 `eva-infra-shared`：`6c798f1b`）、`edu.cuit.infra.convertor.user.LdapUserConvertor`（已迁至 `eva-infra-shared`：`0dc0ddc8`）
    - 缓存常量：`edu.cuit.infra.enums.cache.{UserCacheConstants,CourseCacheConstants}`
    - LDAP：`edu.cuit.infra.dal.ldap.{dataobject,repo}.*`（已迁至 `eva-infra-shared`：`aca70b8b`；后续 `LdapGroupDO` 已继续下沉到 `shared-kernel`，保持包名不变：`03ecd906`）、`edu.cuit.infra.util.EvaLdapUtils` + `edu.cuit.infra.enums.LdapConstants`（已迁至 `eva-infra-shared`：`3165180c`），其中 `edu.cuit.infra.property.EvaLdapProperties` 已继续下沉到 `shared-kernel`（保持包名不变：`666a1b6d`）
    - 工具：`edu.cuit.infra.util.QueryUtils`
  - 阶段 2.7（已完成）：新增 shared 子模块骨架 `eva-infra-shared`（不迁代码，仅作为后续从 `eva-infra` 抽离 Converter/LDAP/缓存常量/工具等的落点；保持行为不变）。
    - 新模块：`eva-infra-shared/pom.xml`
  - 阶段 2.8（已完成）：迁移 IAM 相关缓存常量到 `eva-infra-shared`（保持包名不变；保持行为不变）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/enums/cache/UserCacheConstants.java`、`CourseCacheConstants.java`
    - 依赖：`eva-infra-shared/pom.xml` 增加 `spring-context`（保留 `@Component`）；`eva-infra/pom.xml` 与 `bc-iam-infra/pom.xml` 增加对 `eva-infra-shared` 的依赖
  - 阶段 2.9（已完成）：迁移查询工具 `QueryUtils` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/util/QueryUtils.java`
    - 依赖：`eva-infra-shared/pom.xml` 增加对 `eva-client` 与 `zym-spring-boot-starter-jdbc` 的依赖（历史）；进展（2025-12-27）：已移除 `eva-infra-shared` → `eva-client` 直依赖（保持行为不变；最小回归通过；`9437bb12`）。
  - 阶段 2.10（已完成）：迁移 `EntityFactory` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/EntityFactory.java`
    - 后续（2026-02-12，保持行为不变）：已从 `eva-infra-shared` 下沉到 `eva-infra-dal`（Java：`eva-infra-dal/src/main/java/edu/cuit/infra/convertor/EntityFactory.java`；落地：`eba15e92`）。
    - 依赖：`eva-infra-shared/pom.xml` 增加 `mapstruct-plus-spring-boot-starter`；并增加对 `eva-domain` 的依赖以保留 `hutool SpringUtil` 与 `cola SysException` 的依赖来源
  - 阶段 2.11（已完成）：迁移 `PaginationConverter` 到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`54d5fecd`）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/PaginationConverter.java`
	    - 依赖：`eva-infra-shared/pom.xml` 增加对 `eva-infra-dal` 的依赖以保持编译闭包（历史记录；后续 2026-02-17 已移除该依赖，保持行为不变）
  - 阶段 2.12（已完成）：迁移 `convertor.user` 的非 LDAP 转换器到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`6c798f1b`）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/user/{MenuConvertor,RoleConverter,UserConverter}.java`
  - 阶段 2.13（已完成）：迁移 LDAP DO/Repo 到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`aca70b8b`）。
	    - Java：`shared-kernel/src/main/java/edu/cuit/infra/dal/ldap/repo/LdapGroupRepo.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/ldap/repo/LdapPersonRepo.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/ldap/dataobject/LdapPersonDO.java`
	    - 后续（2026-02-17，保持行为不变）：`LdapGroupDO` 已从 `eva-infra-shared` 下沉到 `shared-kernel`（Java：`shared-kernel/src/main/java/edu/cuit/infra/dal/ldap/dataobject/LdapGroupDO.java`；落地：`03ecd906`）。
	    - 后续（2026-02-17，保持行为不变）：`LdapGroupRepo` 已从 `eva-infra-shared` 下沉到 `shared-kernel`（Java：`shared-kernel/src/main/java/edu/cuit/infra/dal/ldap/repo/LdapGroupRepo.java`；落地：`7ff087ad`）。
	    - 依赖：`eva-infra-shared/pom.xml` 增加 `spring-boot-starter-data-ldap`
  - 阶段 2.14（已完成）：迁移 `EvaLdapUtils` 相关类型到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`3165180c`）。
	    - Java：`shared-kernel/src/main/java/edu/cuit/infra/util/EvaLdapUtils.java`、`shared-kernel/src/main/java/edu/cuit/infra/enums/LdapConstants.java`、`shared-kernel/src/main/java/edu/cuit/infra/property/EvaLdapProperties.java`
  - 阶段 2.15（已完成）：迁移 `LdapUserConvertor` 到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`0dc0ddc8`）。
	    - Java：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/convertor/user/LdapUserConvertor.java`
  - 阶段 2.16（已完成）：移除 `bc-iam-infra` 对 `eva-infra` 的 Maven 依赖（补齐 cache/logging 编译依赖；保持行为不变；落地提交：`2ad911ea`）。
- AI 报告 / 审计日志：已启动 `bc-ai-report` / `bc-audit` 骨架并接入组合根；审计日志协议对象（`ILogService/OperateLogCO/LogModuleCO`）已从 `eva-client` 迁移到 `bc-audit`（保持行为不变；落地提交：`e1dbf2d4`）。
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
- `bc-messaging`：消息查询读侧已收敛（`queryMsg/queryTargetAmountMsg`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`05a2381b`）。
- `bc-messaging`：消息插入写侧已收敛（`insertMessage`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`8445bc41`）。
- `bc-messaging`：消息展示状态写侧已收敛（`updateMsgDisplay`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`c315fa22`）。
- `bc-messaging`：旧 `MsgGatewayImpl` 已全量退化为委托壳（CRUD 能力全部委托到 `bc-messaging`，行为不变），且已从 `eva-infra` 归位到 `bc-messaging`（保持 `package` 不变；落地提交：`8ffcfe35`）。
- `bc-audit`：基础设施能力继续归位（保持行为不变）：`LogGatewayImpl` 已从 `eva-infra` 归位到 `bc-audit-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；最小回归通过；落地提交：`673a19e3`）。
- `bc-iam`：用户创建/分配角色写侧已收敛（`createUser/assignRole`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`16ff60b6/b65d311f/a707ab86`、`c3aa8739/a3232b78/a26e01b3/9e7d46dd`）。
- `bc-iam`：用户信息更新写侧已收敛（`updateInfo`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`38c31541/6ce61024/db0fd6a3/cb789e21`）。
- `bc-iam`：用户状态更新写侧已收敛（`updateStatus`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`e3fcdbf0/8e82e01f/eb54e13e`；行为快照与下一步拆分详见 `NEXT_SESSION_HANDOFF.md`，补充提交：`e4b94add`）。
- `bc-iam`：用户删除写侧已收敛（`deleteUser`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`5f08151c/e23c810a/cccd75a3/2846c689`）。
- `bc-iam`：用户查询装配已收敛（`fileUserEntity`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`3e6f2cb2/8c245098/92a9beb3`）。
- `bc-iam`：用户基础查询已收敛（`findIdByUsername/findUsernameById/getUserStatus/isUsernameExist`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`9f664229/38384628/de662d1c/8a74faf5`）。
- `bc-iam`：用户目录查询已收敛（`findAllUserId/findAllUsername/allUser/getUserRoleIds`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`）。
- `bc-iam`：基础设施能力继续归位（保持行为不变）：`LdapPersonGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；最小回归通过；落地提交：`1ff96d75`）。
- `bc-iam`：基础设施能力继续归位（保持行为不变）：`MenuQueryGatewayImpl`/`MenuUpdateGatewayImpl`/`RoleQueryGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package edu.cuit.infra.gateway.impl.user` 不变；最小回归通过；落地提交：`a7cb96e9/09574045/457b6780`）。
- `bc-iam`：基础设施能力继续归位（保持行为不变）：`DepartmentGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；最小回归通过；落地提交：`acb13124`）。
- `bc-course`：多条课程写链路已收敛（导入课表、改课/自助课表、删课、课程类型、课次新增等），旧 gateway 逐步退化为委托壳。
- `bc-course`：基础设施能力继续归位（保持行为不变）：`ClassroomGatewayImpl` 已从 `eva-infra` 归位到 `bc-course-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；最小回归通过；落地提交：`26b183d5`）。
- `bc-course`：课程导入状态查询 `CourseUpdateGatewayImpl.isImported` 已收敛（QueryPort + 用例 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`4ed055a2/495287c8/f3e8e3cc`）。
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
- `eva-infra-shared`：支撑类继续归位（保持行为不变）：`SemesterConverter` 已从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package edu.cuit.infra.convertor` 不变；最小回归通过；落地提交：`6c9e1d39`）。
- `eva-infra-shared`：支撑类继续归位（保持行为不变）：`StaticConfigProperties` 已从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package edu.cuit.infra.property` 不变；最小回归通过；落地提交：`5a26af43`）。
- 评教读侧用例级回归测试已补充（固化统计口径；落地提交：`a48cf044`）。
- `start`：回归测试稳定化（去除本地文件/外部服务依赖；落地提交：`daf343ef`）。
- `evaluation`：评教模板新增/修改写侧已收敛到 `bc-evaluation`（新增用例 + 端口 + `eva-infra` 端口适配器，并切换 `eva-app` 入口；落地提交：`ea03dbd3`）。
- `evaluation`：清理旧 `EvaUpdateGatewayImpl.putEvaTemplate` 遗留实现（提交评教写侧入口已在 `bc-evaluation`，避免旧代码回潮；落地提交：`12279f3f`）。
- `bc-course`：基础设施能力继续归位（保持行为不变）：`SemesterGatewayImpl` 已从 `eva-infra` 归位到 `bc-course-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；最小回归通过；落地提交：`30e6a160`）。
- `bc-iam`：基础设施能力继续归位（保持行为不变）：`RoleUpdateGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.user` 不变；最小回归通过；落地提交：`1826ac99`）。
- `bc-iam`：基础设施能力继续归位（保持行为不变）：`UserQueryGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.user` 不变；最小回归通过；落地提交：`b9d8e6b8`）。
- `bc-iam`：基础设施能力继续归位（保持行为不变）：`UserUpdateGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.user` 不变；最小回归通过；落地提交：`69b72d86`）。
- `bc-evaluation`：基础设施能力继续归位（保持行为不变）：`EvaConfigGatewayImpl` 已从 `eva-infra` 归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.eva` 不变；最小回归通过；落地提交：`9a4e28aa`）。
- `bc-evaluation`：基础设施能力继续归位（保持行为不变）：`EvaDeleteGatewayImpl` 已从 `eva-infra` 归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.eva` 不变；最小回归通过；落地提交：`e8e73845`）。
- `bc-evaluation`：基础设施能力继续归位（保持行为不变）：`EvaUpdateGatewayImpl` 已从 `eva-infra` 归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.eva` 不变；最小回归通过；落地提交：`cbb3801a`）。
- 下一步建议（基础设施旧 gateway 归位，保持行为不变）：✅ 本阶段已闭环：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl` 下残留旧 `*GatewayImpl.java` 已清零（以 `NEXT_SESSION_HANDOFF.md` 0.10 盘点口径为准；细节见 `docs/DDD_REFACTOR_BACKLOG.md` 4.3）。
- 冲突校验底层片段已收敛：
  - 教室占用冲突：`ClassroomOccupancyChecker`
  - 时间段重叠：`CourInfTimeOverlapQuery`
