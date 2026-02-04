---
title: DDD 渐进式重构目标清单与行为框架
repo: eva-backend
branch: ddd
generated_at: 2025-12-18
updated_at: 2026-02-04
scope: 全仓库（离线扫描 + 规则归纳）
---

# DDD 渐进式重构目标清单与行为框架

本文档用于后续若干个新会话的“行动框架（行为约束 + 执行模板）”与“待收敛目标池（Backlog）”。

> 说明：目标清单来自离线静态扫描（以 `GatewayImpl` 为主），属于“候选目标粗筛”。每次动手前仍需按调用链复核入口与行为约束。
>
> 术语提醒（减少“gateway”历史命名带来的混淆）：
> - 旧 `*GatewayImpl` 在早期实现中经常同时承载“应用入口 + DB 访问 + 副作用”，并不严格等价于 DDD/六边形里的“出站网关（gateway）”。
> - 渐进式重构阶段以职责为准：入口逐步收敛到 `bc-*` 的 UseCase；DB/副作用搬运到 Port Adapter；旧 gateway 退化为委托壳（但保留缓存注解触发点，确保行为不变）。

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
   - 需求变更（2025-12-24）：逐步拆解 `eva-client`：新增/迁移的 BO/CO/DTO 等“边界协议对象”按业务归属放入对应 BC（优先 `bc-xxx/application` 子模块下的 `contract/dto` 包），避免继续往 `eva-client` 堆对象。
3. **infra 端口适配器实现旧逻辑**
   - 需求变更（2025-12-24）：新增适配器优先落在目标 BC 的 `infrastructure` 子模块（例如 `bc-xxx/infrastructure/...`）；
   - 过渡期如仍需落在 `eva-infra/.../bcxxx/adapter` 或历史过渡模块 `bc-xxx-infra`，必须在文档中标注“后续折叠归位”的计划与里程碑；
   - 将旧 gateway 的业务流程 **原样搬运**（包含异常文案、删除顺序、日志、缓存等）。
4. **旧 gateway 退化为委托壳**
   - gateway 仅负责：构造 command → 调用 usecase → 返回历史签名（必要时 `return null`）。
   - 若历史对外签名为 `Void`，保持 `return null`。
5. **离线验证（Java 17 + 本地 Maven repo）**
   - `export JAVA_HOME=\"$HOME/.sdkman/candidates/java/17.0.17-zulu\" && export PATH=\"$JAVA_HOME/bin:$PATH\"`
   - （本机 `mvnd` 权限兜底）如遇默认写入 `~/.m2/mvnd/...` 报 `AccessDenied`，可设置：`export MVND_DAEMON_STORAGE=\"$PWD/.mvnd/daemon\" && export MVND_REGISTRY=\"$PWD/.mvnd/registry\"`（`.mvnd/` 保持未跟踪，不要提交）
   - `mvnd -o -pl <bc-module> -am test -q -Dmaven.repo.local=.m2/repository`
   - `mvnd -o -pl eva-infra -am -DskipTests test -q -Dmaven.repo.local=.m2/repository`
6. **沉淀文档（交接与回溯）**
   - 在 `NEXT_SESSION_HANDOFF.md` 追加闭环/commit；
   - （2026-01-13 补充）已在 `NEXT_SESSION_HANDOFF.md` 更新 0.9/0.10/0.11：记录 `UserAuthServiceImpl` 归位到 `bc-iam-infra` 的闭环与 Serena `TimeoutError` 的降级口径，并将新会话 `F 推荐版` 提示词主线更新为 IAM 旧入口归位（`UserServiceImpl` → `BcIamConfiguration`），避免续接丢信息。
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

### 4.2 实施进度（滚动更新，按时间倒序）

> 说明：此处用于同步“Backlog → 已完成/进行中”的状态变化；具体闭环细节与验收约束以 `NEXT_SESSION_HANDOFF.md` 为准。

**已完成（更新至 2026-02-04）**
- ✅ S0.2 延伸（依赖收敛：eva-infra，保持行为不变）：在 Serena + `rg` 证伪 `eva-infra` 已从 root reactor 退场且源码仅 `package-info.java`（无业务逻辑、无外部依赖方）后，收敛 `eva-infra/pom.xml`：移除残留 `<dependencies>` 依赖声明（最小回归通过；落地：`47654a6a`）。
- ✅ IAM S0.2 延伸（前置，保持行为不变）：在 `bc-iam-contract` 新增 `UserStatusByUsernameQueryPort`，用于后续将 `ValidateUserLoginUseCase` 去 `UserEntity` 编译期依赖且保持对旧 `UserQueryGateway.findByUsername` 的调用次数/缓存触发点不变（最小回归通过；落地提交以 `git log -n 1 -- bc-iam/contract/src/main/java/edu/cuit/bc/iam/application/port/UserStatusByUsernameQueryPort.java` 为准）。
- ✅ IAM S0.2 延伸（前置，保持行为不变）：已补齐 `UserStatusByUsernameQueryPort` 的端口适配器实现；后续为避免注入歧义，该独立适配器已被删除并由 `UserEntityByUsernameQueryPortImpl` 统一承接（最小回归通过）。
- ✅ IAM S0.2 延伸（前置，保持行为不变）：在 `bc-iam-infrastructure` 的 `UserEntityByUsernameQueryPortImpl` 补齐实现 `UserStatusByUsernameQueryPort`（内部仍委托旧 `UserQueryGateway.findByUsername`；通过拆箱保持历史空值/NPE 表现不变；最小回归通过；落地提交以 `git log -n 1 -- bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserEntityByUsernameQueryPortImpl.java` 为准）。
- ✅ IAM S0.2 延伸（保持行为不变）：将 `ValidateUserLoginUseCase` 从编译期依赖 `UserEntity` 收敛为依赖 `UserStatusByUsernameQueryPort`（仍保持对旧 `UserQueryGateway.findByUsername` 的调用次数/缓存触发点不变；最小回归通过；落地提交以 `git log -n 1 -- bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/ValidateUserLoginUseCase.java` 为准）。
- ✅ IAM S0.2 延伸（保持行为不变）：将 `FindUserByIdUseCase` 去 `UserEntity` 编译期依赖，收敛为优先依赖 `bc-iam-contract` 端口 `UserEntityByIdQueryPort`（`execute` 使用泛型承接 `Optional<?>`；不改变旧 gateway 委托链路/缓存触发点；落地：`e13e1dc6`）。
- ✅ IAM S0.2 延伸（保持行为不变）：将 `FindUserByUsernameUseCase` 去 `UserEntity` 编译期依赖，收敛为优先依赖 `bc-iam-contract` 端口 `UserEntityByUsernameQueryPort`（`execute` 使用泛型承接 `Optional<?>`；不改变旧 gateway 委托链路/缓存触发点；落地：`e8f16843`）。
- ✅ IAM S0.2 延伸（保持行为不变）：将 `PageUserUseCase` 去 `UserEntity` 编译期依赖，收敛为优先依赖 `bc-iam-contract` 端口 `UserDirectoryPageQueryPort`（分页出参使用泛型承接 `PaginationResultEntity<?>`；不改变旧 gateway 委托链路/缓存触发点；落地：`4de644a7`）。
- ✅ IAM S0.2 延伸（保持行为不变）：将旧端口 `UserEntityQueryPort` 去 `UserEntity` 编译期依赖（返回类型收敛为 `Optional<?>/PaginationResultEntity<?>`，过渡期实际返回仍为 `UserEntity`；端口适配器逻辑不变；落地：`72d029e0`）。
- ✅ IAM S0.2 延伸（保持行为不变）：将 `FindUserByIdUseCaseTest` 去 `UserEntity` 编译期依赖（测试侧端口返回类型同步为通配符；不影响用例委托行为；落地：`d8804521`）。
- ✅ IAM S0.2 延伸（保持行为不变）：将 `FindUserByUsernameUseCaseTest` 去 `UserEntity` 编译期依赖（测试侧端口返回类型同步为通配符；不影响用例委托行为；落地：`0ebfb75c`）。
- ✅ IAM S0.2 延伸（保持行为不变）：将 `PageUserUseCaseTest` 去 `UserEntity` 编译期依赖（测试侧端口返回类型同步为通配符；不影响用例委托行为；落地：`934cf935`）。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：将 `bc-evaluation/infrastructure` 的 `EvaStatisticsExporter` 去 `UserEntity` 编译期依赖：不再 `import UserEntity`，改为对 `UserAllUserIdAndEntityByIdQueryPort.findById` 返回的 `Optional<?>` 做运行时类型判定（按类名/父类链）后再 `Optional.of(...)`，以保持历史“仅当返回值为 UserEntity 才参与后续逻辑”的分支语义不变；最小回归通过；落地：`4f4b190b`。
- ✅ S0.2 延伸（依赖方收敛前置，保持行为不变）：在 `eva-infra-shared` 的 `EvaConvertor` 增加桥接方法 `toEvaTaskEntityWithTeacherObject(...)`，用于后续让依赖方在不编译期引用 `UserEntity` 的情况下复用既有 `ToEvaTaskEntity` 映射逻辑（仅做类型桥接，不改变 teacher/courInf Supplier 的调用时机与次数）；最小回归通过；落地：`a8934ab1`。
- ✅ S0.2 延伸（依赖方收敛前置，保持行为不变）：在 `eva-infra-shared` 的 `UserConverter` 增加桥接方法 `toUserEntityObject(...)` + `userIdOf(Object)`，用于后续让依赖方在不编译期引用 `UserEntity` 的情况下复用既有 `toUserEntity(...)` 映射逻辑（仅做类型桥接，不改变 roles Supplier 的调用时机与次数）；最小回归通过；落地：`c173c7c2`。
- ✅ S0.2 延伸（依赖方收敛前置，保持行为不变）：为后续让 `bc-iam-infra` 的 `UserBasicQueryPortImpl` 去 `UserEntity` 编译期依赖（缓存命中时仍需读取 username/status），在 `eva-infra-shared` 的 `UserConverter` 增加桥接方法 `usernameOf(Object, boolean)` + `statusOf(Object, boolean)`（内部仍强转 `UserEntity` 调 getter；并通过“无业务意义形参”规避 MapStruct 编译期歧义）；最小回归通过；落地：`485cc5fb`。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：将 `bc-iam-infra` 的 `UserBasicQueryPortImpl` 去 `UserEntity` 编译期依赖：缓存命中时从 `Optional<?>` 取值并改走 `UserConverter.userIdOf/usernameOf/statusOf` 桥接方法读取字段（缓存命中/回源顺序与异常文案不变）；最小回归通过；落地：`42af63a3`。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：将 `bc-iam-infra` 的 `UserEntityQueryPortImpl` 去 `UserEntity` 编译期依赖：对外签名按端口口径收敛为 `Optional<?>/PaginationResultEntity<?>`，内部仍通过 `UserConverter.toUserEntityObject(...)` 承接旧映射与角色/菜单装配逻辑（查询/排序/异常文案不变）；最小回归通过；落地：`e8acbad3`。
- ✅ S0.2 延伸（依赖方收敛前置，保持行为不变）：为后续让 `bc-iam-infra` 的 `UserEntityByUsernameQueryPortImpl` 去 `UserEntity` 编译期依赖（权限/角色列表计算需读取 roles），在 `eva-infra-shared` 的 `UserConverter` 增加桥接方法 `rolesOf(Object, boolean)`（内部仍强转 `UserEntity` 调 `getRoles`；并通过“无业务意义形参”规避 MapStruct 编译期歧义）；最小回归通过；落地：`81408507`。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：将 `bc-iam-infra` 的 `UserEntityByUsernameQueryPortImpl` 去 `UserEntity` 编译期依赖：权限/角色列表计算改走 `UserConverter.statusOf/rolesOf` 桥接读取字段（保持仍委托旧 `UserQueryGateway.findByUsername` 以触发历史缓存/切面；异常与分支顺序不变）；最小回归通过；落地：`44d51d3e`。
- ✅ S0.2 延伸（依赖方收敛前置，保持行为不变）：在 `bc-iam-infra` 的 `UserBizConvertor` 增加桥接方法 `toUserDetailCOObject(Object)` / `toUnqualifiedUserInfoCOObject(Object, Integer)`（内部仍强转 `UserEntity`，仅做类型桥接，尽量保持历史空值/异常表现一致），用于后续让调用方在不编译期引用 `UserEntity` 的情况下复用既有映射逻辑；最小回归通过；落地：`c2454ab4`。
- ✅ S0.2 延伸（依赖方收敛前置，保持行为不变）：为后续让 `bc-iam-infra` 的 `RouterDetailFactory/UserServiceImpl` 去 `UserEntity` 编译期依赖，在 `eva-infra-shared` 的 `UserConverter` 增加桥接方法 `nameOf(Object, boolean)` / `permsOf(Object, boolean)` / `castUserEntityObject(Object, boolean)`（内部仍强转 `UserEntity` 调 getter；并通过“无业务意义形参”规避 MapStruct 编译期歧义；`castUserEntityObject` 用于尽量保持历史 ClassCastException 触发点一致）；最小回归通过；落地：`95c7bd8b`。
- ✅ IAM S0.2 延伸（依赖方收敛，保持行为不变）：将 `bc-iam-infra` 的 `RouterDetailFactory` 去 `UserEntity` 编译期依赖：入参从 `UserEntity` 收敛为 `Object`，并通过 `SpringUtil.getBean(UserConverter.class)` 调 `usernameOf/rolesOf/nameOf` 桥接读取字段（过滤逻辑/递归构造与异常表现不变；最小回归通过）；落地：`5d2f7512`。
- ✅ IAM S0.2 延伸（依赖方收敛，保持行为不变）：将 `bc-iam-infra` 的 `UserServiceImpl` 去 `UserEntity` 编译期依赖：`Optional<?>.map(UserEntity.class::cast)` 改走 `userConverter.castUserEntityObject(...)`，并将 `getUserInfo` 入参收敛为 `Object` 后通过 `RouterDetailFactory/UserBizConvertor/UserConverter` 桥接读取字段（缓存/日志/异常文案/副作用顺序不变；最小回归通过）；落地：`d901223c`。
- ✅ S0.2 延伸（依赖方收敛前置，保持行为不变）：在 `eva-infra-shared` 的 `UserConverter` 增加“Spring Bean + setName”桥接方法 `springUserEntityWithNameObject(Object)`（内部仍 `SpringUtil.getBean(UserEntity.class)` + 强转后 `setName`，尽量保持异常形态与副作用顺序一致），用于后续让 `bc-course/infrastructure` 的 `CourseQueryRepository` 去 `UserEntity` 编译期依赖；最小回归通过。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：将 `bc-course/infrastructure` 的 `CourseQueryRepository` 去 `UserEntity` 编译期依赖：不再 `import UserEntity`，改走 `UserConverter/CourseConvertor` 的桥接方法承接 teacher 相关类型桥接（不改变 DB 查询/遍历顺序；异常文案不变）；最小回归通过。
- ✅ S0.2 延伸（依赖方收敛前置，保持行为不变）：在 `eva-infra-shared` 的 `CourseConvertor` 增加桥接方法 `toCourseEntityWithTeacherObject(...)`，用于后续让依赖方在不编译期引用 `UserEntity` 的情况下复用既有 `toCourseEntity(...)` 映射逻辑（仅做类型桥接，不改变 teacher Supplier 的调用时机与次数）；最小回归通过；落地：`858521da`。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：将 `bc-evaluation/infrastructure` 的 `FillAverageScoreExporterDecorator` 去 `UserEntity` 编译期依赖，改为依赖 `bc-iam-contract` 的 `UserDetailQueryPort` + `UserDetailCO`（内部仍委托旧 `UserQueryGateway.findById`，缓存触发点不变；落地：`7a3ca8ed`）。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：将 `bc-evaluation/infrastructure` 的 `FillEvaRecordExporterDecorator` 去 `UserEntity` 编译期依赖，改为依赖 `bc-iam-contract` 的 `UserDetailQueryPort` + `UserDetailCO`（内部仍委托旧 `UserQueryGateway.findById`，缓存触发点不变；落地：`fba76459`）。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：将 `bc-evaluation/infrastructure` 的 `FillUserStatisticsExporterDecorator` 去 `UserEntity` 编译期依赖，改为依赖 `bc-iam-contract` 的 `UserDetailQueryPort` + `UserDetailCO`（内部仍委托旧 `UserQueryGateway.findById`，缓存触发点不变；落地：`8d59ea72`）。
- ✅ IAM 并行（按 10.3：补齐鉴权权限/角色查询最小端口（前置）；保持行为不变）：在 `bc-iam-contract` 新增 `UserPermissionAndRoleQueryPort`，用于后续将 `eva-infra-shared` 的 `StpInterfaceImpl` 去 `UserQueryGateway` 编译期依赖（仅新增接口，不改装配/不改行为；最小回归通过；落地提交以 `git log -n 1 -- bc-iam/contract/src/main/java/edu/cuit/bc/iam/application/port/UserPermissionAndRoleQueryPort.java` 为准）。
- ✅ IAM 并行（按 10.3：补齐端口适配器实现（鉴权权限/角色查询）；保持行为不变）：在 `bc-iam-infrastructure` 的 `UserEntityByUsernameQueryPortImpl` 补齐实现 `UserPermissionAndRoleQueryPort`，内部委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变（最小回归通过；落地提交以 `git log -n 1 -- bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserEntityByUsernameQueryPortImpl.java` 为准）。
- ✅ 编译闭合前置（保持行为不变）：在 `eva-infra-shared/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖，用于后续将 `StpInterfaceImpl` 从依赖 `UserQueryGateway` 收敛为依赖 contract 端口（最小回归通过；落地提交以 `git log -n 1 -- eva-infra-shared/pom.xml` 为准）。
- ✅ IAM 并行（按 10.3：StpInterfaceImpl 依赖收敛；保持行为不变）：将 `eva-infra-shared` 的 `StpInterfaceImpl` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 端口 `UserPermissionAndRoleQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway.findByUsername`；权限/角色筛选逻辑不变；最小回归通过；落地提交以 `git log -n 1 -- eva-infra-shared/src/main/java/edu/cuit/app/security/StpInterfaceImpl.java` 为准）。
- ✅ IAM 并行（按 10.3：MsgBizConvertor 依赖收敛；保持行为不变）：将 `eva-infra-shared` 的 `MsgBizConvertor` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 端口 `UserEntityByIdQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway.findById`；消息 recipient/sender lazy-load 语义不变；最小回归通过；落地提交以 `git log -n 1 -- eva-infra-shared/src/main/java/edu/cuit/app/convertor/MsgBizConvertor.java` 为准）。
- ✅ 测试过渡收敛（保持行为不变）：将 `start` 的 `MsgServiceImplTest` 收敛为直接使用 Port 版本构造（测试不再编译期依赖 `UserQueryGateway`；最小回归通过；落地提交以 `git log -n 1 -- start/src/test/java/edu/cuit/app/service/impl/MsgServiceImplTest.java` 为准）。
- ✅ 测试过渡收敛（保持行为不变）：将 `start` 的 `UserEvaServiceImplTest` 收敛为直接使用 Port 版本构造（测试不再编译期依赖 `UserQueryGateway`；最小回归通过；落地提交以 `git log -n 1 -- start/src/test/java/edu/cuit/app/service/impl/eva/UserEvaServiceImplTest.java` 为准）。
- ✅ 测试过渡（保持行为不变）：将 `start` 模块中的 `UserServiceImplTest` 从 mock `UserQueryGateway` 调整为 mock `bc-iam-contract` 端口（重点适配 `getOneUserScore` 使用的 `UserBasicQueryPort.findUsernameById`），以匹配 `UserServiceImpl` 构造参数收敛后的新依赖形态；最小回归通过；落地：`c30c4e0c`。
- ✅ IAM 并行（按 10.3：补齐用户目录/分页端口（前置）；保持行为不变）：在 `bc-iam-contract` 新增 `UserDirectoryPageQueryPort`（原误命名为 `UserDirectoryQueryPort`，与 `bc-iam/application` 同名端口冲突已更名；涵盖 `page/allUser/findAllUsername`；分页返回 `PaginationResultEntity<?>`，避免 contract 暴露旧领域实体触发 Maven 循环依赖风险），用于后续将 `bc-iam/infrastructure` 的 `UserServiceImpl` 去 `UserQueryGateway` 编译期依赖；最小回归通过；落地：`d7e7216e`。
- ✅ IAM 并行（按 10.3：补齐端口适配器实现；保持行为不变）：在 `bc-iam-infra` 新增 `UserDirectoryPageQueryPortImpl`，内部委托旧链路以保持缓存/切面触发点不变（后续注入已收敛为 `UserQueryCacheGateway`，见 2026-02-04 推进记录；用于闭合后续 `UserServiceImpl` 依赖收敛后的 Spring 装配）；最小回归通过；落地：`524d7ba3`。
- ✅ IAM 并行（按 10.3：UserServiceImpl 依赖收敛；保持行为不变）：将 `bc-iam/infrastructure` 的 `UserServiceImpl` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 端口 `UserEntityByIdQueryPort/UserEntityByUsernameQueryPort/UserBasicQueryPort/UserDirectoryPageQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway` 以保持缓存/切面触发点不变；方法调用次数/顺序不变；异常文案不变）；最小回归通过；落地：`cc6f6d63`。
- ✅ IAM 并行（按 10.3：去无用编译期依赖（DepartmentGatewayImpl）；保持行为不变）：清理 `DepartmentGatewayImpl` 中未使用的 `UserQueryGateway/QueryException` import，使该类不再编译期依赖旧 gateway（仅删 import；最小回归通过）；落地：`f29572d2`。
- ✅ IAM 并行（按 10.3：ValidateUserLoginUseCase 清理过渡构造；保持行为不变）：Serena 证实调用点仅剩 `UserAuthServiceImpl` 且已改走 Port 构造，因此移除 `ValidateUserLoginUseCase` 中 `@Deprecated` 旧构造与对 `UserQueryGateway` 的编译期依赖（运行时行为不变；异常文案/分支顺序不变）；最小回归通过；落地：`1b90f641`。
- ✅ IAM 并行（按 10.3：UserAuthServiceImpl 依赖收敛（过渡）；保持行为不变）：将 `bc-iam-infrastructure` 的 `UserAuthServiceImpl` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 最小端口 `UserEntityByUsernameQueryPort`（调用顺序不变：仍先 `ValidateUserLoginUseCase.execute` 再 `StpUtil.login`；内部仍委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变；异常文案不变）；最小回归通过；落地：`48d0eb7e`。
- ✅ IAM 并行（按 10.3：ValidateUserLoginUseCase 依赖收敛（过渡）；保持行为不变）：将 `bc-iam/application` 的 `ValidateUserLoginUseCase` 从依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 最小端口 `UserEntityByUsernameQueryPort`（内部仍由端口适配器委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变；异常文案与分支顺序不变）；同时保留一个 `@Deprecated` 旧构造用于过渡/回滚；最小回归通过；落地：`66329367`。
- ✅ IAM 并行（按 10.3：补齐按用户名查询用户实体端口（前置）；保持行为不变）：在 `bc-iam-contract` 新增最小端口 `UserEntityByUsernameQueryPort`（返回 `Optional<?>`，避免 contract 反向依赖旧领域实体导致 Maven 循环依赖；后续由端口适配器内部委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变）；最小回归通过；落地：`02827d19`。
- ✅ IAM 并行（按 10.3：补齐按用户名查询用户实体端口适配器（前置）；保持行为不变）：在 `bc-iam-infrastructure` 新增 `UserEntityByUsernameQueryPortImpl`，内部委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变（为后续将 `ValidateUserLoginUseCase` 去 `UserQueryGateway` 编译期依赖闭合装配前置）；最小回归通过；落地：`5c18aa08`。
- ✅ IAM 并行（按 10.3：EvaTaskServiceImpl 依赖收敛；保持行为不变）：将 `bc-evaluation/infrastructure` 的 `EvaTaskServiceImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort`（异常文案/副作用顺序不变；最小回归通过）；落地：`72bd00d9`。
- ✅ IAM 并行（按 10.3：评教导出链路收敛前置：组合端口补齐；保持行为不变）：在 `bc-iam-contract` 新增组合端口 `UserAllUserIdAndEntityByIdQueryPort`，并让 `bc-iam-infrastructure` 的 `UserAllUserIdQueryPortImpl` 同时实现 `findById`（内部仍委托旧 `UserQueryGateway.findAllUserId/findById` 以保持缓存/切面触发点不变），用于后续将 `bc-evaluation/infrastructure` 的 `EvaStatisticsExporter` 去 `UserQueryGateway` 编译期依赖且保持 `SpringUtil.getBean(...)` 次数/顺序不变；最小回归通过；落地：`1d6624d4`。
- ✅ IAM 并行（按 10.3：EvaStatisticsExporter 依赖收敛；保持行为不变）：将 `bc-evaluation/infrastructure` 的 `EvaStatisticsExporter` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 组合端口 `UserAllUserIdAndEntityByIdQueryPort`（通过 `UserQueryGatewayFacade` 保持调用形态不变；内部仍委托旧 `UserQueryGateway` 以保持缓存/切面触发点不变；且 `SpringUtil.getBean(...)` 次数/顺序不变）；最小回归通过；落地：`92c6554a`。
- ✅ S0.2 延伸（IAM：端口下沉：UserBasicQueryPort → bc-iam-contract，保持行为不变）：将 `UserBasicQueryPort` 从 `bc-iam/application` 下沉到 `bc-iam-contract`（保持 `package edu.cuit.bc.iam.application.port` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过）；落地：`739cb25f`。
- ✅ IAM 并行（按 10.3：新增 UserNameQueryPort，保持行为不变）：在 `bc-iam-contract` 新增 `UserNameQueryPort`（为后续收敛 AI 报告基础设施对 `UserQueryGateway` 的依赖做前置；仅新增接口，不改装配/不改行为；最小回归通过）；落地：`cfccf4ca`。
- ✅ IAM 并行（按 10.3：新增 UserNameQueryPortImpl，保持行为不变）：在 `bc-iam-infrastructure` 新增 `UserNameQueryPortImpl`，内部委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变（为后续 `AiReportAnalysisPortImpl` 依赖收敛闭合编译；最小回归通过）；落地：`8852b859`。
- ✅ IAM 并行（按 10.3：AiReportAnalysisPortImpl 依赖收敛（过渡），保持行为不变）：将 `bc-ai-report/infrastructure` 的 `AiReportAnalysisPortImpl` 从直接依赖 `UserQueryGateway` 收敛为依赖 `UserNameQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变；保留 `@Deprecated` 旧构造方式仅用于测试过渡；最小回归通过）；落地：`c374ae9b`。
- ✅ AI 报告（测试过渡收敛，保持行为不变）：将 `AiReportAnalysisPortImplTest` 从 mock `UserQueryGateway` 改为 mock `UserNameQueryPort` 并调用新构造方法（作为下一刀移除 `AiReportAnalysisPortImpl` 中 `@Deprecated` 过渡构造的前置；最小回归通过）；落地：`6e99c11b`。
- ✅ AI 报告（移除过渡构造，保持行为不变）：在测试已切到新构造的前提下，移除 `AiReportAnalysisPortImpl` 中 `@Deprecated` 旧构造与桥接逻辑，使该类彻底不再编译期依赖 `UserQueryGateway`（运行期仍通过 `UserNameQueryPortImpl -> UserQueryGateway.findById` 保持缓存/切面触发点不变；最小回归通过）；落地：`b2a6dc15`。
- ✅ 审计（编译闭合前置：bc-audit-infra 显式依赖 bc-iam-contract；保持行为不变）：在 `bc-audit/infrastructure/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖，用于为后续收敛 `LogInsertionPortImpl` 的依赖类型闭合编译（仅编译边界收敛；最小回归通过）；落地：`77bb15b2`。
- ✅ 审计（LogInsertionPortImpl 依赖收敛，保持行为不变）：将 `bc-audit/infrastructure` 的 `LogInsertionPortImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort`（调用链原本就最终委托 `UserBasicQueryPortImpl`，因此缓存命中/回源顺序与历史语义保持不变；最小回归通过）；落地：`065183ab`。
- ✅ 审计（LogGatewayImpl 依赖收敛，保持行为不变）：移除 `bc-audit/infrastructure` 的 `LogGatewayImpl` 中未使用的 `UserQueryGateway` 注入字段与 import（不影响任何业务逻辑/异常文案/副作用顺序；最小回归通过）；落地：`6d4b3661`。
- ✅ IAM 并行（按 10.3，编译闭合前置：bc-ai-report-infra 显式依赖 bc-iam-contract；保持行为不变）：在 `bc-ai-report/infrastructure/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖，用于为后续收敛 `AiReportUserIdQueryPortImpl` 的依赖类型闭合编译（仅编译边界收敛；最小回归通过）；落地：`bceb2576`。
- ✅ IAM 并行（按 10.3：AiReportUserIdQueryPortImpl 依赖收敛；保持行为不变）：将 `bc-ai-report/infrastructure` 的 `AiReportUserIdQueryPortImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort`（原链路本就最终委托 `UserBasicQueryPort`，因此缓存命中/回源顺序与历史语义保持不变）；最小回归通过；落地：`b16546ed`。
- ✅ IAM 并行（按 10.3：AvatarManager 依赖收敛；保持行为不变）：将 `bc-iam/infrastructure` 的 `AvatarManager` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort`（只替换该类实际用到的 `findUsernameById`；异常文案/文件 IO 行为不变；最小回归通过）；落地：`56872f30`。
- ✅ 评教（测试过渡，保持行为不变）：为后续将 `bc-evaluation/infrastructure` 的 `UserEvaServiceImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort` 做前置，先将 `start/src/test/java/edu/cuit/app/service/impl/eva/UserEvaServiceImplTest.java` 改为兼容“旧构造（UserQueryGateway）/新构造（UserBasicQueryPort）”（仅测试代码；最小回归通过）；落地：`93ac4799`。
- ✅ IAM 并行（按 10.3：UserEvaServiceImpl 依赖收敛；保持行为不变）：将 `bc-evaluation/infrastructure` 的 `UserEvaServiceImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort`（只替换该类实际用到的 `findIdByUsername`；异常文案与分支顺序不变；最小回归通过）；落地：`456e5346`。
- ✅ 评教（依赖收敛收尾，保持行为不变）：清理 `bc-evaluation/infrastructure` 的 `UserEvaServiceImpl` 残留未使用 `UserQueryGateway` import，使该类彻底不再编译期依赖旧 gateway（仅删 import；最小回归通过）；落地：`edc3f9c7`。
- ✅ 消息（编译闭合前置，保持行为不变）：为后续将 `bc-messaging` 的 `MessageQueryPortImpl` 对 `UserQueryGateway` 的编译期依赖收敛为依赖 `bc-iam-contract` 的最小 Port 做前置，在 `bc-messaging/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（仅显式化依赖边界；最小回归通过）；落地：`16a90a5e`。
- ✅ IAM 并行（按 10.3：消息查询端口补齐，保持行为不变）：在 `bc-iam-contract` 新增最小端口 `UserEntityByIdQueryPort`，并在 `bc-iam-infra` 新增 `UserEntityByIdQueryPortImpl` 内部委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变（返回类型为 `Optional<?>`，过渡期实际返回 `UserEntity`；用于避免 contract 反向依赖旧领域实体导致 Maven 循环依赖）；最小回归通过；落地：`7875e09e` / `2ea7d39f`。
- ✅ 消息（依赖收敛，保持行为不变）：将 `bc-messaging` 的 `MessageQueryPortImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserEntityByIdQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变；异常文案不变）；最小回归通过；落地：`17509393`。
- ✅ 消息（测试过渡，保持行为不变）：为后续收敛 `bc-evaluation/infrastructure` 的 `MsgServiceImpl` 对 `UserQueryGateway` 的编译期依赖，先将 `start/src/test/java/edu/cuit/app/service/impl/MsgServiceImplTest.java` 改为兼容“旧构造（UserQueryGateway）/新构造（Port）”（仅测试代码；最小回归通过）；落地：`f593b529`。
- ✅ IAM 并行（按 10.3：新增用户ID列表查询端口，保持行为不变）：在 `bc-iam-contract` 新增 `UserAllUserIdQueryPort`（为后续收敛 `bc-evaluation/infrastructure` 的 `MsgServiceImpl` 从依赖 `UserQueryGateway` 到依赖最小 Port 做前置；仅新增接口，不改装配/不改行为；最小回归通过）；落地：`f244c9d0`。
- ✅ IAM 并行（按 10.3：新增 UserAllUserIdQueryPortImpl，保持行为不变）：在 `bc-iam-infrastructure` 新增 `UserAllUserIdQueryPortImpl`，内部委托旧 `UserQueryGateway.findAllUserId()` 以保持缓存/切面触发点不变（为后续 `MsgServiceImpl` 依赖收敛闭合编译；最小回归通过）；落地：`daff2644`。
- ✅ IAM 并行（按 10.3：MsgServiceImpl 依赖收敛；保持行为不变）：将 `bc-evaluation/infrastructure` 的 `MsgServiceImpl` 从依赖 `UserQueryGateway` 收敛为依赖 IAM contract 端口 `UserBasicQueryPort + UserAllUserIdQueryPort`（广播消息的 `findAllUserId()` 由端口适配器委托旧 gateway 以保持缓存/切面触发点不变；异常文案/副作用顺序不变；最小回归通过）；落地：`b4456be9`。
- ✅ S0.2 延伸（依赖收敛：bc-course-infra 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-course/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-course/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`ff109643`。
- ✅ S0.2 延伸（依赖收敛：bc-evaluation-infra 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-evaluation/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-evaluation/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`6a882e4a`。
- ✅ S0.2 延伸（依赖收敛：bc-iam-infra 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-iam/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-iam/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`f65ce9a7`。
- ✅ S0.2 延伸（依赖收敛：bc-template-infra 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-template/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-template/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`a8b018a5`。
- ✅ S0.2 延伸（依赖收敛：bc-audit(infrastructure) 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-audit/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-audit/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`9cd9a96a`。
- ✅ S0.2 延伸（依赖收敛：bc-audit(application) 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-audit/application/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-audit/application/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`182c34cf`。
- ✅ S0.2 延伸（依赖收敛：shared-kernel 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `shared-kernel` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `shared-kernel/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`61106d70`。
- ✅ S0.2 延伸（依赖收敛：bc-evaluation-contract 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-evaluation/contract/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-evaluation/contract/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`841ba3c3`。
- ✅ S0.2 延伸（依赖收敛：bc-messaging-contract 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-messaging-contract/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-messaging-contract/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`f0047fb1`。
- ✅ S0.2 延伸（依赖收敛：bc-iam-contract 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-iam/contract/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-iam/contract/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`f623a290`。
- ✅ S0.2 延伸（依赖收敛：bc-ai-report-infra 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-ai-report/infrastructure/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-ai-report/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`770221ea`。
- ✅ S0.2 延伸（依赖收敛：bc-ai-report(application) 去无用测试依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-ai-report/application/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-ai-report/application/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地：`317f1859`。
- ✅ S0.2 延伸（依赖收敛：eva-domain 去 bc-iam-contract 编译期依赖，保持行为不变）：在 `SimpleRoleInfoCO` 已下沉到 `shared-kernel` 的前提下，Serena + `rg` 证伪 `eva-domain/src/main/java` 无其它 `bc-iam-contract` 类型引用后，收敛 `eva-domain/pom.xml`：移除对 `bc-iam-contract` 的 Maven 编译期依赖（最小回归通过）；落地：`49eadf1f`。
- ✅ S0.2 延伸（类型下沉：SimpleRoleInfoCO → shared-kernel，保持行为不变）：为后续收敛 `eva-domain` 对 `bc-iam-contract` 的编译期耦合并保持 `RoleQueryGateway`（跨 BC 复用）稳定，将 `SimpleRoleInfoCO` 从 `bc-iam-contract` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过）；落地：`a04dfd7c`。
- ✅ S0.2 延伸（shared-kernel：编译闭合前置，保持行为不变）：为后续将评教等旧领域 `@Entity` 类型逐类下沉到 `shared-kernel` 提供编译闭合支撑，在 `shared-kernel/pom.xml` 显式增加 `cola-component-domain-starter(optional)`（最小回归通过）；落地：`a77e1b71`。
- ✅ S0.2 延伸（AI 报告：编译闭合前置，保持行为不变）：为保持 AI 报告用例中 `SysException` 的异常语义不变且避免经由 `eva-domain` 间接依赖，在 `bc-ai-report/application/pom.xml` 显式增加 `cola-component-exception` 依赖（最小回归通过）；落地：`3e7fd3bd`。
- ✅ S0.2 延伸（端口下沉：EvaRecordScoreQueryPort → bc-evaluation-contract，保持行为不变）：为后续收敛 AI 报告等依赖方对 `bc-evaluation`（application jar）的编译期耦合，将 `EvaRecordScoreQueryPort` 从 `bc-evaluation/application` 下沉到 `bc-evaluation-contract`（保持 `package/签名` 不变，仅改变 Maven 模块归属；最小回归通过）；落地：`78f45ee2`。
- ✅ S0.2 延伸（依赖收敛：bc-ai-report(application) 去 eva-domain 编译期依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-ai-report/application/src/main/java` 无 `edu.cuit.domain.*` 引用后，收敛 `bc-ai-report/application/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖（最小回归通过）；落地：`53a61ee8`。
- ✅ S0.2 延伸（依赖收敛：start 去 JUnit4，保持行为不变）：在 Serena + `rg` 证伪 `start/src/test/java` 无 `org.junit.Test/org.junit.runner.*` 等 JUnit4 引用后，收敛 `start/pom.xml`：移除 `junit:junit`（JUnit4）依赖（最小回归通过）；落地：`e97a5205`。
- ✅ S1.1（eva-adapter 退场：root reactor 退场，保持行为不变）：在 Serena 证据化确认（当时）全仓库仅 `eva-adapter/pom.xml` 声明 `<artifactId>eva-adapter</artifactId>` 后，从根 `pom.xml` 的 reactor 中移除 `eva-adapter` 模块（最小回归通过）；落地：`86842a1f`。
- ✅ S1.1（eva-adapter 退场：删除模块 pom，保持行为不变）：在 `eva-adapter` 已从 root reactor 退场的前提下，删除 `eva-adapter/pom.xml`，使全仓库 `**/pom.xml` 不再出现 `<artifactId>eva-adapter</artifactId>`（最小回归通过）；落地：`ed244cad`。
- ✅ S0.2 延伸（依赖收敛：eva-domain 去 bc-evaluation-contract 编译期依赖，保持行为不变）：在 Serena 证据化确认 `eva-domain/src/main/java` 无评教 contract 类型引用后，收敛 `eva-domain/pom.xml`：移除对 `bc-evaluation-contract` 的 Maven 编译期依赖（最小回归通过）；落地：`ccbb1cf9`。
- ✅ S0.2 延伸（依赖收敛：eva-domain 去 spring-statemachine-core 编译期依赖，保持行为不变）：在 Serena + `rg` 证伪 `eva-domain/src/main/java` 无 `statemachine` 相关引用后，收敛 `eva-domain/pom.xml`：移除 `spring-statemachine-core` 的 Maven 编译期依赖（最小回归通过）；落地：`12c8d4bb`。
- ✅ S0.2 延伸（依赖收敛纠偏：bc-audit(application) 恢复 eva-domain 编译期依赖，保持行为不变）：此前仅按 `edu.cuit.domain.*` 引用面误判可去 `eva-domain`（`bf90c040`）；后续确认 `bc-audit/application` 仍引用 `edu.cuit.client.bo.SysLogBO`（定义于 `eva-domain`，包名保持不变），因此恢复 `bc-audit/application/pom.xml` 对 `eva-domain` 的 Maven 编译期依赖以闭合编译（最小回归通过）；纠偏落地：`b47d71ab`。
- ✅ S0.2 延伸（类型下沉：PaginationResultEntity → shared-kernel，保持行为不变）：为逐步减少各 BC（含 IAM）对 `eva-domain` 的编译期耦合，将通用分页实体 `edu.cuit.domain.entity.PaginationResultEntity` 从 `eva-domain` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过）；落地：`d31bb204`。
- ✅ S0.2 延伸（类型下沉：SysLogBO → shared-kernel，保持行为不变）：为进一步减少依赖方对 `eva-domain` 的编译期耦合，将日志 BO `edu.cuit.client.bo.SysLogBO` 从 `eva-domain` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过）；落地：`96de4244`。
- ✅ S0.2 延伸（依赖收敛：bc-audit(application) 去 eva-domain 编译期依赖，保持行为不变）：在 `SysLogBO` 已下沉到 `shared-kernel` 且 Serena + `rg` 证伪 `bc-audit/application/src/main/java` 无 `edu.cuit.domain.*` 引用后，收敛 `bc-audit/application/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖（最小回归通过）；落地：`0ee2a831`。
- ✅ S0.2 延伸（端口下沉：EvaRecordCountQueryPort → bc-evaluation-contract，保持行为不变）：将评教读侧端口接口 `EvaRecordCountQueryPort` 从 `bc-evaluation/application` 下沉到 `bc-evaluation-contract`（保持 `package edu.cuit.bc.evaluation.application.port` 不变），为后续收敛 `bc-iam-infra` 对 `bc-evaluation` 的编译期依赖做前置；最小回归通过；落地：`4c30b02c`。
- ✅ S0.2 延伸（依赖收敛：bc-iam-infra 去 bc-evaluation 编译期依赖，保持行为不变）：在 `EvaRecordCountQueryPort` 已下沉到 `bc-evaluation-contract` 后，收敛 `bc-iam/infrastructure/pom.xml`：移除对 `bc-evaluation` 的 Maven 编译期依赖，仅保留 `bc-evaluation-contract`（最小回归通过）；落地：`42a9e96c`。
- ✅ S0.2 延伸（IAM：去 `eva-domain` 前置，bc-iam-domain 编译闭合前置，保持行为不变）：按 `DDD_REFACTOR_PLAN.md` 10.3 的 IAM 小节“Step 0（pom）”先行，在 `bc-iam/domain/pom.xml` 补齐搬运归位所需的最小编译期依赖（`shared-kernel`、`cola-component-domain-starter`、`spring-context(provided)`、`lombok(provided)`），仅用于编译闭合，不引入新业务语义；最小回归通过；落地：`a3d048d0`。
- ✅ S0.2 延伸（IAM：去 `eva-domain` 前置，bc-iam(application) 显式依赖 bc-iam-domain，保持行为不变）：为保证后续“每次只搬运 1 个类”仍可编译闭合，在 `bc-iam/application/pom.xml` 显式增加对 `bc-iam-domain` 的编译期依赖（暂不移除 `eva-domain`；保持行为不变）；最小回归通过；落地：`aeaa8471`。
- ✅ S0.2 延伸（IAM：去 `eva-domain` 前置，bc-iam-domain 显式依赖 bc-iam-contract，保持行为不变）：为保证后续搬运“仅依赖 IAM cmd/CO 的 gateway 接口”可编译闭合，在 `bc-iam/domain/pom.xml` 显式增加对 `bc-iam-contract` 的编译期依赖（保持行为不变）；最小回归通过；落地：`2fc02fed`。
- ✅ S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）：将 `RoleUpdateGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属）；最小回归通过；落地：`95e37e8a`。
- ✅ S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）：将 `MenuUpdateGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属）；最小回归通过；落地：`c31a7a1e`。
- ✅ S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）：将 `DepartmentGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属）；最小回归通过；落地：`68128578`。
- ✅ S0.2 延伸（IAM：去 `eva-domain` 前置，编译闭合前置，保持行为不变）：为保证 `eva-domain` 内残留的 `UserEntity` 等类型在逐步搬运过程中仍可编译闭合，在 `eva-domain/pom.xml` 显式增加对 `bc-iam-domain` 的 Maven 编译期依赖（过渡期；保持行为不变）；最小回归通过；落地：`43e8b66e`。
- ✅ S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）：将 `UserUpdateGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属）；最小回归通过；落地：`6630277b`。
- ✅ S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）：将 `edu.cuit.domain.entity.user.biz.MenuEntity` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属）；最小回归通过；落地：`6d700911`。
- ✅ S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）：将 `edu.cuit.domain.gateway.user.MenuQueryGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属）；最小回归通过；落地：`9982af0a`。
- ✅ S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）：将 `edu.cuit.domain.entity.user.LdapPersonEntity` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属）；最小回归通过；落地：`eb36c6ce`。
- ✅ S0.2 延伸（依赖收敛：eva-infra-shared 去 bc-evaluation-contract 编译期依赖，保持行为不变）：在 Serena 证据化确认 `eva-infra-shared/src/main/java` 无评教 contract 类型引用后，收敛 `eva-infra-shared/pom.xml`：移除对 `bc-evaluation-contract` 的 Maven 编译期依赖（最小回归通过）；落地：`d28a5904`。
- ✅ S0.2 延伸（依赖收敛：bc-iam-contract 去 bc-evaluation-contract 编译期依赖，保持行为不变）：在 Serena 证据化确认 `bc-iam/contract/src/main/java` 无评教 contract 类型引用后，收敛 `bc-iam/contract/pom.xml`：移除对 `bc-evaluation-contract` 的 Maven 编译期依赖（最小回归通过）；落地：`dcf5849a`。（后续证实误判，已恢复依赖：`918c5d45`）
- ✅ S0.2 延伸（依赖收敛纠偏：bc-iam-contract 恢复 bc-evaluation-contract 编译期依赖，保持行为不变）：在 Serena 证据化确认 `IUserService#getOneUserScore` 仍返回 `UserSingleCourseScoreCO`（定义于 `bc-evaluation-contract`）后，恢复 `bc-iam/contract/pom.xml` 对 `bc-evaluation-contract` 的显式依赖（用于纠正 `dcf5849a` 的误判；最小回归通过）；落地：`918c5d45`。
- ✅ S1（IAM Controller：UserUpdateController 结构性收敛，保持行为不变）：在 `UserUpdateController` 抽取 `success()` 统一封装 `CommonResult.success()` 的返回表达，并修正少量参数空格格式以降低噪声（不改 URL/注解/异常/副作用顺序；最小回归通过）；落地：`5ee37fd2`。
- ✅ S1（IAM Controller：DepartmentController 结构性收敛，保持行为不变）：在 `DepartmentController` 抽取 `success(...)` 统一封装 `CommonResult.success(...)` 的返回表达（不改 URL/注解/异常/副作用顺序；最小回归通过）；落地：`fbc5fb74`。
- ✅ S1（IAM Controller：AuthenticationController 结构性收敛，保持行为不变）：在 `AuthenticationController` 抽取 `success(...)`/`success()` 统一封装 `CommonResult.success(...)` 的返回表达（不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地：`fd9e4d1c`。
- ✅ S1（IAM Controller：MenuUpdateController 结构性收敛，保持行为不变）：在 `MenuUpdateController` 抽取 `success()` 统一封装 `CommonResult.success()` 的返回表达（严格保持 `menuService.create(...)` → `LogUtils.logContent(...)` → `return` 的执行顺序不变；不改 URL/注解/权限/异常/日志/副作用顺序；最小回归通过）；落地：`44bc649d`。
- ✅ S1（IAM Controller：RoleUpdateController 结构性收敛，保持行为不变）：在 `RoleUpdateController` 抽取 `success()` 统一封装 `CommonResult.success()` 的返回表达（严格保持 `roleService.create(...)` → `LogUtils.logContent(...)` → `return` 的执行顺序不变；不改 URL/注解/权限/异常/日志/副作用顺序；最小回归通过）；落地：`c81eb2e0`。
- ✅ S1（IAM Controller：UserQueryController 小幅重构，保持行为不变）：对 `UserQueryController` 进行纯结构性整理（简化临时变量与返回包装；不改 URL/注解/异常/副作用顺序；最小回归通过）；落地：`a542abff`。
- ✅ S1（IAM Controller：MenuQueryController 小幅重构，保持行为不变）：对 `MenuQueryController` 进行纯结构性整理（简化临时变量与返回包装；不改 URL/注解/异常/副作用顺序；最小回归通过）；落地：`e388ae84`。
- ✅ S1（IAM Controller：RoleQueryController 小幅重构，保持行为不变）：对 `RoleQueryController` 进行纯结构性整理（简化临时变量与返回包装；不改 URL/注解/异常/副作用顺序；最小回归通过）；落地：`bb134377`。
- ✅ S1.1（eva-adapter 退场：组合根收敛，保持行为不变）：在 `eva-adapter` Controller 已清零且入口已归位各 BC 的前提下，移除 `start/pom.xml` 对 `eva-adapter` 的 Maven 依赖（最小回归通过）；落地：`92a70a9e`。
- ✅ S1（入口归位：LogController，保持行为不变）：将 `LogController` 从 `eva-adapter` 归位到 `bc-audit/infrastructure`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过）；落地：`b592cc0f`。
- ✅ S1（审计 Controller：LogController 结构性收敛，保持行为不变）：在 `LogController` 抽取泛型 `success(data)` 统一封装 `CommonResult.success(data)` 的返回表达（不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地：`14c9ab77`。
- ✅ S1（消息 Controller：MessageController 结构性收敛，保持行为不变）：在 `MessageController` 抽取 `success(data)`/`success()` 统一封装 `CommonResult.success(...)` 的返回表达（不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地：`9a3ef681`。
- ✅ S1（课程 Controller：SemesterController 结构性收敛，保持行为不变）：在 `SemesterController` 抽取泛型 `success(data)` 统一封装 `CommonResult.success(data)` 的返回表达（不改 URL/注解/异常/副作用顺序；最小回归通过）；落地：`13892e55`。
- ✅ S1（课程 Controller：ClassroomController 结构性收敛，保持行为不变）：在 `ClassroomController` 抽取 `success(data)` 统一封装 `CommonResult.success(data)` 的返回表达（不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地：`99eb17c0`。
- ✅ S1（课程 Controller：QueryCourseController 结构性收敛，保持行为不变）：在 `QueryCourseController` 抽取泛型 `success(data)` 统一封装 `CommonResult.success(data)` 的返回表达（不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地：`4c54337f`。
- ✅ S1（课程 Controller：QueryUserCourseController 结构性收敛，保持行为不变）：在 `QueryUserCourseController` 抽取泛型 `success(data)` 统一封装 `CommonResult.success(data)` 的返回表达（不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地：`c8ce3522`。
- ✅ S1（入口归位前置：bc-audit-infra 编译闭合，保持行为不变）：为后续将 `LogController` 从 `eva-adapter` 归位到 `bc-audit/infrastructure` 做编译闭合前置，在 `bc-audit/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`spring-boot-starter-validation`、`zym-spring-boot-starter-common`、`zym-spring-boot-starter-security`（运行时 classpath 已存在，仅显式化；最小回归通过）；落地：`2464d2b9`。
- ✅ S0.2 延伸（依赖收敛：eva-adapter 去 bc-audit 编译期依赖，保持行为不变）：在 Serena 证据化确认 `eva-adapter/src/main/java` 无源码引用审计类型后，收敛 `eva-adapter/pom.xml`：移除对 `bc-audit` 的 Maven 编译期依赖（最小回归通过）；落地：`3aa49c66`。
- ✅ S0.2 延伸（依赖收敛：eva-adapter 去 bc-ai-report 编译期依赖，保持行为不变）：在 Serena 证据化确认 `eva-adapter/src/main/java` 无 AI 报告相关源码引用后，收敛 `eva-adapter/pom.xml`：移除对 `bc-ai-report` 的 Maven 编译期依赖（最小回归通过）；落地：`a7f85ac7`。
- ✅ S0.2 延伸（依赖收敛：eva-adapter 去 bc-*contract/shared-kernel 编译期依赖，保持行为不变）：在 Serena 证据化确认 `eva-adapter/src/main/java` 无源码后，进一步收敛 `eva-adapter/pom.xml`：移除 `shared-kernel` 与 `bc-iam-contract` / `bc-evaluation-contract` / `bc-messaging-contract` 的 Maven 编译期依赖（最小回归通过）；落地：`84be3a4b`。
- ✅ S1（入口归位前置：bc-evaluation-infra 编译闭合，保持行为不变）：为后续归位 `EvaStatisticsController`，在 `bc-evaluation/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`、`bc-evaluation-contract`、`bc-ai-report`（仅编译闭合；最小回归通过）；落地：`a5e1eb52`。
- ✅ S1（入口归位：EvaStatisticsController，保持行为不变）：将 `EvaStatisticsController` 从 `eva-adapter` 归位到 `bc-evaluation-infra`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过）；落地：`f533261a`。
- ✅ S1 前置（支撑类归位：课程适配层时间计算工具，保持行为不变）：为后续归位 `QueryUserCourseController` 做编译闭合前置，将 `edu.cuit.adapter.controller.course.util.CalculateClassTime` 从 `eva-adapter` 下沉到 `shared-kernel`（保持 `package/逻辑` 不变，仅搬运归位；最小回归通过）；落地：`8a3f738c`。
- ✅ S1（入口归位：QueryCourseController，保持行为不变）：将 `QueryCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过）；落地：`1b9a6fc7`。
- ✅ S1（入口归位：QueryUserCourseController，保持行为不变）：将 `QueryUserCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过）；落地：`bc37fa17`。
- ✅ S1（入口归位：DeleteCourseController，保持行为不变）：将 `DeleteCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过）；落地：`4b6219b9`。
- ✅ S1（入口归位：UpdateCourseController，保持行为不变）：将 `UpdateCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过）；落地：`1d03d987`。
- ✅ S1（入口归位：SemesterController，保持行为不变）：将 `SemesterController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过）；落地：`0257ddd0`。
  - ✅ S1（入口归位：MenuUpdateController，保持行为不变）：将 `MenuUpdateController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过）；落地：`d29b565b`。
- ✅ S1（入口归位：RoleUpdateController，保持行为不变）：将 `RoleUpdateController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过）；落地：`80888bed`。
- ✅ S1（入口归位：DepartmentController，保持行为不变）：将 `DepartmentController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过）；落地：`3e66a7b4`。
- ✅ S1（入口归位前置：bc-course-infra 编译闭合，保持行为不变）：为后续归位 `ClassroomController`，在 `bc-course/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`（运行时 classpath 已存在，仅显式化；最小回归通过）；落地：`8915db14`。
- ✅ S1（入口归位：ClassroomController，保持行为不变）：将 `ClassroomController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package/接口签名/URL/注解` 与行为不变，仅搬运归位；最小回归通过）；落地：`132f32f5`。
- ✅ S1（入口归位前置：bc-iam-infra 编译闭合补齐，保持行为不变）：为后续归位 `UserQueryController`（依赖 `IEvaStatisticsService`）做前置，在 `bc-iam/infrastructure/pom.xml` 补齐 `bc-evaluation-contract`（保持行为不变；最小回归通过；落地：`0781952e`）。
- ✅ S1（入口归位前置：bc-iam-infra 编译闭合补齐，保持行为不变）：为后续归位 `UserUpdateController` 做前置，在 `bc-iam/infrastructure/pom.xml` 补齐 `cola-component-exception`、`shared-kernel`、`eva-base-common`（保持行为不变；最小回归通过；落地：`ddd5ff2a`）。
- ✅ S1（入口归位前置：bc-iam-infra 编译闭合，保持行为不变）：为后续将 `AuthenticationController` 从 `eva-adapter` 归位到 `bc-iam-infra`，先在 `bc-iam/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`、`commons-lang3`（仅编译闭合；最小回归通过；落地：`42d44f0b`）。
- ✅ S0.2 延伸（依赖收敛：组合根→eva-adapter，保持行为不变）：Serena 证伪 `start` 源码未引用 `edu.cuit.adapter.*` 后，将 `start/pom.xml` 对 `eva-adapter` 的依赖 scope 改为 `runtime`（运行期装配不变，仅收敛编译期边界；阶段性中间态；最小回归通过；落地：`045891d1`）。后续已在 `92a70a9e` 移除组合根对 `eva-adapter` 的 Maven 依赖（保持行为不变）。
- ✅ S1 前置（支撑类归位：评教配置 Service，保持行为不变）：将 `EvaConfigService` 从 `eva-app` 归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；类内容不变；最小回归通过；落地：`d7defd75`）。
- ✅ S1 前置（支撑类归位：课程详情查询 Exec，保持行为不变）：将 `UserCourseDetailQueryExec` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.operate.course.query` 不变；类内容不变；最小回归通过；落地：`00983fd2`）。
- ✅ S1 前置（支撑类归位：课程导入辅助 Exec，保持行为不变）：将 `FileImportExec` 从 `eva-app` 归位到 `bc-course-infra`（落点：`bc-course/infrastructure`；保持 `package edu.cuit.app.service.operate.course.update` 不变；仅搬运 + 清理未使用 import；最小回归通过；落地：`960dfbaf`）。
- ✅ S1 前置（支撑类归位：operate 包 package-info，保持行为不变）：将 `package-info.java` 从 `eva-app` 归位到 `bc-course-infra`（落点：`bc-course/infrastructure`；保持 `package edu.cuit.app.service.operate` 不变；仅搬运注释；最小回归通过；落地：`c8ddace5`）。
- ✅ S1 前置（支撑类归位：评教模板 Convertor，保持行为不变）：将 `EvaTemplateBizConvertor` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor.eva` 不变；类内容不变；最小回归通过；落地：`470078ba`）。
- ✅ S1 前置（支撑类归位：评教任务 Convertor，保持行为不变）：将 `EvaTaskBizConvertor` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor.eva` 不变；类内容不变；最小回归通过；落地：`80b3937c`）。
- ✅ S1 前置（支撑类归位：评教记录 Convertor，保持行为不变）：将 `EvaRecordBizConvertor` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor.eva` 不变；类内容不变；最小回归通过；落地：`b3a3dab2`）。
- ✅ S1 前置（支撑类归位：评教配置 Convertor，保持行为不变）：将 `EvaConfigBizConvertor` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor.eva` 不变；类内容不变；最小回归通过；落地：`5514463d`）。
- ✅ S1 前置（支撑类归位：评教切面配置，保持行为不变）：将 `AspectConfig` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.aop` 不变；类内容不变；最小回归通过；落地：`18901dab`）。
- ✅ S1 前置（支撑类归位：Sa-Token 权限加载，保持行为不变）：将 `StpInterfaceImpl` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.security` 不变；类内容不变；最小回归通过；落地：`bcce5582`）。
- ✅ S1 前置（支撑类归位：Sa-Token 拦截器配置，保持行为不变）：将 `SaTokenInterceptorConfig` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.config` 不变；类内容不变；最小回归通过；落地：`a14a4c68`）。
- ✅ S1 前置（支撑类归位：Sa-Token 配置，保持行为不变）：将 `SaTokenConfig` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.config` 不变；类内容不变；最小回归通过；落地：`c767663f`）。
- ✅ S0.2 延伸（消息契约：依赖收敛，保持行为不变）：在 Serena 证据化确认 `eva-app/src/main/java` 无消息契约引用面后，已收敛 `eva-app/pom.xml`：移除 `bc-messaging-contract` 的 Maven 编译期依赖，使 `eva-app` 对消息域编译期依赖收敛为 0（最小回归通过；落地：`b92314ef`）。
- ✅ S0.2 延伸（审计：编译闭合前置，保持行为不变）：在 `bc-audit/infrastructure/pom.xml` 补齐 `zym-spring-boot-starter-logging`，用于为后续归位 `LogBizConvertor/LogServiceImpl`（依赖 `LogManager/OperateLogBO`）做编译闭合前置（运行时 classpath 已存在，仅显式化；最小回归通过；落地：`e7e13736`）。
- ✅ S0.2 延伸（审计：组合根归位，保持行为不变）：将 `BcAuditConfiguration` 从 `eva-app` 归位到 `bc-audit-infra`（保持 `package edu.cuit.app.config` 不变；Bean 装配/副作用顺序不变；最小回归通过；落地：`5a4d726b`）。
- ✅ S0.2 延伸（审计：归位支撑类，保持行为不变）：将 `LogBizConvertor` 从 `eva-app` 归位到 `bc-audit-infra`（保持 `package edu.cuit.app.convertor` 不变；最小回归通过；落地：`99960c7f`）。
- ✅ S0.2 延伸（审计：旧入口壳归位，保持行为不变）：将 `LogServiceImpl` 从 `eva-app` 归位到 `bc-audit-infra`（保持 `package edu.cuit.app.service.impl` 不变；最小回归通过；落地：`d0af2bac`）。
- ✅ S0.2 延伸（审计：装配责任上推，保持行为不变）：在 `start/pom.xml` 增加对 `bc-audit-infra` 的 `runtime` 依赖，使组合根显式承接审计基础设施运行时装配责任（与原 transitive 结果等价，仅显式化；最小回归通过；落地：`d6d9c480`）。
- ✅ S0.2 延伸（AI 报告：装配责任上推，保持行为不变）：在 `start/pom.xml` 增加对 `bc-ai-report-infra` 的 `runtime` 依赖，使组合根显式承接 AI 报告基础设施运行时装配责任（与原 transitive 结果等价，仅显式化；最小回归通过；落地：`08862a4b`）。
- ✅ S0.2 延伸（AI 报告：依赖收敛，保持行为不变）：在 Serena 证据化确认 `eva-app/src/main/java` 无 AI 相关直引后，收敛 `eva-app/pom.xml`：移除对 `bc-ai-report` 与 `bc-ai-report-infra` 的编译期依赖；运行期装配由组合根 `start` 显式兜底（最小回归通过；落地：`2a4736c0`）。
- ✅ S0.2 延伸（websocket：装配责任上推，保持行为不变）：在 `start/pom.xml` 增加对 `spring-boot-starter-websocket` 的依赖，使组合根显式承接 websocket 运行时依赖（与原 transitive 结果等价，仅显式化；最小回归通过；落地：`97b543b1`）。
- ✅ S0.2 延伸（websocket：支撑类归位，保持行为不变）：将 websocket 通道处理器 `MessageChannel` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.websocket` 不变；最小回归通过；落地：`0fbc4aef`）。
- ✅ S0.2 延伸（websocket：支撑类归位，保持行为不变）：将 URI 解析支撑类 `UriUtils` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.util` 不变；最小回归通过；落地：`c1a10d2d`）。
- ✅ S0.2 延伸（websocket：编译闭合前置，保持行为不变）：为后续归位 `WebSocketInterceptor`（依赖 `cn.dev33.satoken.stp.StpUtil`）做编译闭合前置，在 `eva-infra-shared/pom.xml` 补齐 `zym-spring-boot-starter-security`（运行时 classpath 已存在，仅显式化；最小回归通过；落地：`fa2faf1d`）。
- ✅ S0.2 延伸（websocket：配置/拦截器归位，保持行为不变）：将 `WebSocketInterceptor` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.config` 不变；最小回归通过；落地：`df06f0e6`）。
- ✅ S0.2 延伸（websocket：配置归位，保持行为不变）：将 `WebSocketConfig` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.config` 不变；最小回归通过；落地：`e03e60f9`）。
- ✅ S0.2 延伸（websocket：依赖收敛，保持行为不变）：在 Serena 证伪 `eva-app/src/main/java` 不再 `import org.springframework.web.socket.*` 后，收敛 `eva-app/pom.xml`：移除 `spring-boot-starter-websocket` 的编译期依赖（运行期由组合根 `start` 显式兜底；最小回归通过；落地：`4213a95a`）。
  - ✅ S0.2 延伸（审计：依赖收敛，保持行为不变）：在 `rg` 证伪 `eva-app/src/main/java` 不再 `import edu.cuit.bc.audit.*` 后，收敛 `eva-app/pom.xml`：移除对 `bc-audit` 与 `bc-audit-infra` 的编译期依赖；运行期装配由组合根 `start` 显式兜底（最小回归通过；落地：`b3ea5f58`）。
- ✅ S0.2 延伸（评教：旧入口壳归位收尾，保持行为不变）：将 `UserEvaServiceImpl`、`MsgServiceImpl` 从 `eva-app` 归位到 `bc-evaluation-infra`（保持 `package` 不变；行为不变；最小回归通过；落地：`f4238a5c` / `5dea9347`）。
- ✅ S0.2 延伸（评教：归位支撑类与编译闭合前置，保持行为不变）：为归位消息入口壳链路做编译闭合前置，在 `eva-infra-shared/pom.xml` 补齐 websocket 相关依赖，并将 `WebsocketManager`、`MsgBizConvertor` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package` 不变；最小回归通过；落地：`82609bda` / `406186ae` / `c69f494f`）。
- ✅ S0.2 延伸（评教：依赖收敛，保持行为不变）：在 `rg` 证伪 `eva-app/src/main/java` 不再引用 `edu.cuit.bc.evaluation.*` 后，收敛 `eva-app/pom.xml`：移除对 `bc-evaluation`（application jar）的编译期依赖（最小回归通过；落地：`2b42db5d`）。
- ✅ S0.2 延伸（评教：依赖收敛，保持行为不变）：在 Serena + `rg` 证伪 `eva-app/src/main/java` 不再直接引用 `edu.cuit.infra.bcevaluation.*` 实现类型后，收敛 `eva-app/pom.xml`：移除对 `bc-evaluation-infra` 的依赖；运行期装配由组合根 `start` 显式兜底（最小回归通过；落地：`e9feeb56`）。
- ✅ S0.2 延伸（评教：装配责任上推，保持行为不变）：在 `start/pom.xml` 增加对 `bc-evaluation-infra` 的 `runtime` 依赖，使组合根显式承接评教基础设施运行时装配责任（与原 transitive 结果等价，仅显式化；最小回归通过；落地：`0f20d0cd`）。
- ✅ S0.2 延伸（评教：组合根归位，保持行为不变）：将 `BcEvaluationConfiguration` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.config` 不变；Bean 装配/副作用顺序不变；用于后续收敛 `eva-app` 对 `bc-evaluation-infra` 的依赖边界；最小回归通过；落地：`c3f7fc56`）。
- ✅ S0.2 延伸（评教：事件发布器归位，保持行为不变）：将 `SpringAfterCommitDomainEventPublisher` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.event` 不变；`@Component` 装配语义不变；事务提交后发布逻辑不变；最小回归通过；落地：`1b9e275c`）。
- ✅ S0.2 延伸（评教：监听器归位前置，保持行为不变）：为后续归位评教事务事件监听器（需要 `IMsgService` 契约类型）做编译闭合前置，在 `bc-evaluation-infra/pom.xml` 补齐对 `bc-messaging-contract` 的编译期依赖（不引入实现侧依赖；最小回归通过；落地：`bcb8df45`）。
- ✅ S0.2 延伸（评教：旧入口壳归位前置，保持行为不变）：为后续归位 `EvaTaskServiceImpl/UserEvaServiceImpl`（依赖 `StpUtil` 登录态解析）做编译闭合前置，在 `bc-evaluation-infra/pom.xml` 补齐 `zym-spring-boot-starter-security` 依赖（运行时 classpath 已存在；保持行为不变；最小回归通过；落地：`202a6386`）。
- ✅ S0.2 延伸（评教：事务事件监听器归位，保持行为不变）：将 `EvaluationSubmittedMessageCleanupListener` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.event.listener` 不变；监听器触发点/调用顺序不变；注入类型收窄为 `IMsgService` 以避免依赖实现类；最小回归通过；落地：`314c5d6b`）。
- ✅ S0.2 延伸（评教：事务事件监听器归位，保持行为不变）：将 `EvaluationTaskPostedMessageSenderListener` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.event.listener` 不变；监听器触发点/调用顺序不变；注入类型收窄为 `IMsgService` 以避免依赖实现类；最小回归通过；落地：`c9fbe6ef`）。
- ✅ S0.2 延伸（评教：旧入口壳归位，保持行为不变）：将 `EvaStatisticsServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；`@CheckSemId` 触发点与委托顺序不变；类内容不变；最小回归通过；落地：`6db29b33`）。
- ✅ S0.2 延伸（评教：旧入口壳归位，保持行为不变）：将 `EvaTemplateServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；异常转换与返回 `null` 语义不变；类内容不变；最小回归通过；落地：`c63b9875`）。
- ✅ S0.2 延伸（评教：旧入口壳归位，保持行为不变）：将 `EvaRecordServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；异常转换与返回 `null` 语义不变；`@Transactional` 与表单值映射顺序不变；最小回归通过；落地：`c19b32fc`）。
- ✅ S0.2 延伸（评教：旧入口壳归位，保持行为不变）：将 `EvaTaskServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；`@Transactional`/异常文案/日志输出与副作用顺序不变；消息依赖类型收窄为 `IMsgService` 以避免引用实现类；最小回归通过；落地：`1aaff86f`）。
- ✅ S0.2 延伸（依赖方 `pom.xml` 依赖收敛，保持行为不变）：在 `start/pom.xml` 显式增加对 `eva-app` 的 `runtime` 依赖，使组合根承接装配责任的前置条件落地（最小回归通过；落地：`0a69dfb6`）。（后续已在 `0a9ff564` 将组合根依赖从 `eva-app` 替换为 `eva-infra(runtime)` 并移除 `start` 对 `eva-app` 的依赖；并已在 `1e2ffa89` 移除组合根对 `eva-infra(runtime)` 的兜底依赖。）
- ✅ S0.2 延伸（依赖方 `pom.xml` 依赖收敛，保持行为不变）：移除 `start/pom.xml` 对 `eva-app` 的依赖，并显式引入 `eva-infra(runtime)` 以保持运行期 classpath 不变（最小回归通过；落地：`0a9ff564`）。
- ✅ S1 前置（模块退场准备：reactor 移除 eva-app，保持行为不变）：在 Serena + `rg` 证据化确认全仓库已无对 `eva-app` 的 Maven 依赖后，从根 `pom.xml` 的 reactor 中移除 `eva-app` 模块（最小回归通过；落地：`b5f15a4b`）。
- ✅ S1 前置（模块退场收尾：删除 eva-app/pom.xml，保持行为不变）：在确认 `eva-app/` 目录仅剩 `pom.xml` 且已不在 reactor 后，删除 `eva-app/pom.xml`（最小回归通过；落地：`4bfa9d40`）。
- ✅ S1 前置（编译闭合：eva-adapter 依赖补齐，保持行为不变）：触碰任意 Controller 会触发 `eva-adapter` 全量重新编译，暴露其 `pom.xml` 缺少编译期依赖的问题；因此收敛 `eva-adapter/pom.xml` 并补齐所需的最小编译依赖闭合（最小回归通过；落地：`56273162`）。
- ✅ S0.2 延伸（依赖方 `pom.xml` 依赖收敛，保持行为不变）：在 Serena + `rg` 证伪 `eva-adapter` 不再引用 `edu.cuit.app.*` 实现类型后，移除 `eva-adapter/pom.xml` 对 `eva-app` 的 Maven 依赖以减少编译期耦合（最小回归通过；落地：`f5980fcc`）。
- ✅ S0.2 延伸（依赖方 `pom.xml` 依赖收敛，保持行为不变）：将 `start/pom.xml` 中 `bc-course-infra` 的依赖范围从 `test` 调整为 `runtime`，把课程域基础设施的运行时依赖显式上推到组合根（最小回归通过；落地：`2a442587`）。
- ✅ S0.2 延伸（依赖方 `pom.xml` 依赖收敛，保持行为不变）：在组合根已显式兜底 `bc-course-infra` 运行时依赖后，移除 `eva-app/pom.xml` 中对 `bc-course-infra` 的 `runtime` 依赖，使装配责任更清晰地由组合根承接（最小回归通过；落地：`9e7bd82d`）。
- ✅ S0.2 延伸（消息：依赖收敛前置，保持行为不变）：将 `eva-app` 的 `MsgServiceImpl` 内部对 `bc-messaging` 用例实现类型的直接依赖改为委托 `MsgGateway`（其实现 `MsgGatewayImpl` 仍由 `bc-messaging` 提供并内部委托 `MessageUseCaseFacade`/UseCase，确保行为不变），为后续收敛 `eva-app/pom.xml` 中 `bc-messaging` 的编译期依赖做前置（最小回归通过；落地：`35b8eb90`）。
- ✅ S0.2 延伸（消息：依赖收敛，保持行为不变）：在上述前置完成后，移除 `eva-app/pom.xml` 中对 `bc-messaging` 的 Maven 依赖，仅保留 `bc-messaging-contract` 承载协议对象；运行时装配由组合根 `start` 显式兜底（最小回归通过；落地：`afbe2e6c`）。
- ✅ S0.2 延伸（课程：旧入口归位，学期，保持行为不变）：将 `SemesterServiceImpl` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl` 不变；仍实现 `ISemesterService` 并委托 `SemesterQueryUseCase`；保留事务边界；最小回归通过）；落地：`8eddc643`。
- ✅ S0.2 延伸（课程：旧入口归位，教室，保持行为不变）：将 `ClassroomServiceImpl` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl` 不变；仍实现 `IClassroomService` 并委托 `ClassroomQueryUseCase`；最小回归通过）；落地：`4bc8cfb1`。
- ✅ S0.2 延伸（课程：旧入口归位，课程类型，保持行为不变）：将 `ICourseTypeServiceImpl` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变；仍实现 `ICourseTypeService` 并委托 `CourseTypeUseCase`；保持 `id==null` 语义与 `updateCoursesType` 返回 `null`（`Void`）语义不变；最小回归通过）；落地：`8a4a6774`。
- ✅ S0.2 延伸（依赖方收敛：eva-app 去 bc-course 编译期依赖，保持行为不变）：Serena + `rg` 证据化确认：`eva-app` 已不再直接 `import edu.cuit.bc.course.application.usecase.*`（口径：`rg -n '^import edu\\.cuit\\.bc\\.course' eva-app/src/main/java` 证伪为 0）后，已收敛 `eva-app/pom.xml`：移除对 `bc-course` 的编译期依赖（`shared-kernel` 显式依赖保留；最小回归通过）；落地：`dca806fa`。
- ✅ S0.2 延伸（ICourseTypeServiceImpl 收敛准备：用例骨架，保持行为不变）：在 `bc-course/application` 新增课程类型用例 `CourseTypeUseCase`（读写合并；手写 `CourseTypeEntity` → `CourseType` 映射与 `PaginationQueryResultCO` 组装，不引入 `eva-infra-shared`；对齐旧入口逻辑与返回语义；最小回归通过）；落地：`325f221a`。
- ✅ S0.2 延伸（ICourseTypeServiceImpl 收敛准备：用例装配，保持行为不变）：在 `BcCourseConfiguration` 补齐 `CourseTypeUseCase` 的 Bean 装配（保持行为不变；最小回归通过）；落地：`55eb322e`。
- ✅ S0.2 延伸（入口壳收敛：课程类型，保持行为不变）：将 `eva-app` 的 `ICourseTypeServiceImpl` 退化为纯委托壳，改为委托 `CourseTypeUseCase`（保持 `id==null` 语义与 `updateCoursesType` 返回 `null`（`Void`）等行为不变；最小回归通过）；落地：`1aebda24`。
- ✅ S0.2 延伸（模板：组合根归位，保持行为不变）：将 `BcTemplateConfiguration` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.config` 不变；`CourseTemplateLockService` Bean 定义与注入不变；最小回归通过）；落地：`0fbd5d63`。
- ✅ S0.2 延伸（依赖方收敛：eva-app 去 bc-template 编译期依赖，保持行为不变）：Serena + `rg` 证伪 `eva-app` 不再引用 `edu.cuit.bc.template.*` 后，收敛 `eva-app/pom.xml`：移除对 `bc-template` 的编译期依赖（最小回归通过）；落地：`c53fd53d`。
- ✅ S0.2 延伸（模板：端口下沉以降低依赖层级，保持行为不变）：将 `CourseTemplateLockQueryPort` 从 `bc-template/application` 下沉到 `bc-template-domain`（保持 `package edu.cuit.bc.template.application.port` 不变；最小回归通过）；落地：`d0fa4878`。
- ✅ S0.2 延伸（依赖方收敛：eva-infra 去 bc-template 编译期依赖，保持行为不变）：在 Serena + `rg` 证伪 `eva-infra` 仅引用 `CourseTemplateLockQueryPort` 接口类型后，收敛 `eva-infra/pom.xml`：将对 `bc-template` 的 Maven 依赖替换为 `bc-template-domain`（版本不变；最小回归通过）；落地：`5910762e`。
- ✅ S0.2 延伸（依赖方收敛：eva-infra 去 bc-evaluation/bc-iam/bc-audit 编译期依赖，保持行为不变）：在 Serena + `rg` 证伪 `eva-infra/src/main/java` 未引用 `bc-evaluation/bc-iam/bc-audit` 相关类型后，收敛 `eva-infra/pom.xml`：移除对 `bc-evaluation` / `bc-iam` / `bc-audit` 的 Maven 编译期依赖（最小回归通过）；落地：`023d63be`。
- ✅ S0.2 延伸（依赖归位：bc-ai-report-infra 显式依赖 bc-evaluation，保持行为不变）：Serena 证据化确认 `bc-ai-report/infrastructure` 的 `AiReportAnalysisPortImpl` 引用评教 application port `EvaRecordExportQueryPort` 后，已在 `bc-ai-report/infrastructure/pom.xml` 补齐对 `bc-evaluation` 的编译期依赖（避免经由 `bc-ai-report(application)` 传递；最小回归通过）；落地：`c0f78068`。
- ✅ S0.2 延伸（依赖收敛：bc-ai-report(application) 去 bc-evaluation 编译期依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-ai-report/application/src/main/java` 未引用 `edu.cuit.bc.evaluation.*` 相关类型后，收敛 `bc-ai-report/application/pom.xml`：移除对 `bc-evaluation` 的 Maven 编译期依赖，并由 `bc-ai-report-infra` 显式承接该依赖（最小回归通过）；落地：`87179f19`。
- ✅ S0.2 延伸（IAM：旧入口归位，部门查询，保持行为不变）：将 `DepartmentServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl` 不变；仍实现 `IDepartmentService` 并委托 `DepartmentQueryUseCase`；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-iam` 的编译期耦合面；最小回归通过）；落地：`68dea36a`。
- ✅ S0.2 延伸（IAM：支撑类归位，菜单 Convertor，保持行为不变）：为后续将 `MenuServiceImpl` 从 `eva-app` 归位到 `bc-iam-infra` 做前置，先将 `MenuBizConvertor` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.convertor.user` 不变；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-iam` 的编译期耦合面；最小回归通过）；落地：`6298e5a7`。
- ✅ S0.2 延伸（IAM：旧入口归位，菜单，保持行为不变）：将 `MenuServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl.user` 不变；仍实现 `IMenuService` 并保留事务边界；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-iam` 的编译期耦合面；最小回归通过）；落地：`6aef1d96`。
- ✅ S0.2 延伸（IAM：支撑类归位，角色 Convertor，保持行为不变）：为后续将 `RoleServiceImpl/UserServiceImpl` 从 `eva-app` 归位到 `bc-iam-infra` 做前置，先将 `RoleBizConvertor` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.convertor.user` 不变；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-iam` 的编译期耦合面；最小回归通过）；落地：`cf0773ac`。
- ✅ S0.2 延伸（IAM：旧入口归位，角色，保持行为不变）：将 `RoleServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl.user` 不变；仍实现 `IRoleService` 并保留事务边界；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-iam` 的编译期耦合面；最小回归通过）；落地：`3011ab83`。
- ✅ S0.2 延伸（IAM：编译闭合前置，安全依赖，保持行为不变）：为后续将 `UserAuthServiceImpl` 从 `eva-app` 归位到 `bc-iam-infra` 做前置，在 `bc-iam/infrastructure/pom.xml` 补齐 `zym-spring-boot-starter-security`（与 `eva-app` 同源；最小回归通过）；落地：`bded148a`。
- ✅ S0.2 延伸（IAM：旧入口归位，登录，保持行为不变）：将 `UserAuthServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl.user` 不变；类内容不变；最小回归通过）；落地：`b2d885a7`。
- ✅ S0.2 延伸（IAM：支撑类归位，头像配置属性，保持行为不变）：为后续归位 `AvatarManager`/`UserServiceImpl` 做编译闭合前置，将 `AvatarProperties` 从 `eva-infra` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.infra.property` 不变；类内容不变；最小回归通过）；落地：`bb97f037`。
- ✅ S0.2 延伸（IAM：支撑类归位，头像管理，保持行为不变）：为后续归位 `UserServiceImpl` 做编译闭合前置，将 `AvatarManager` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app` 不变；类内容不变；最小回归通过）；落地：`50ab6c9e`。
- ✅ S0.2 延伸（IAM：支撑类归位，用户 Convertor，保持行为不变）：为后续归位 `UserServiceImpl` 做编译闭合前置，将 `UserBizConvertor` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.convertor.user` 不变；类内容不变；最小回归通过）；落地：`10eeb4f3`。
- ✅ S0.2 延伸（IAM：支撑类归位，路由工厂，保持行为不变）：为后续归位 `UserServiceImpl` 做编译闭合前置，将 `RouterDetailFactory` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.factory.user` 不变；类内容不变；最小回归通过）；落地：`be21d2a6`。
- ✅ S0.2 延伸（IAM：编译闭合前置，评教端口依赖，保持行为不变）：为后续归位 `UserServiceImpl` 做前置，在 `bc-iam/infrastructure/pom.xml` 补齐对 `bc-evaluation` 的编译期依赖（原因：`UserServiceImpl` 依赖 `EvaRecordCountQueryPort`；最小回归通过）；落地：`8f5fc4ca`。
- ✅ S0.2 延伸（IAM：旧入口归位，用户管理，保持行为不变）：将 `UserServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl.user` 不变；类内容不变；事务/异常文案/副作用顺序完全不变；最小回归通过）；落地：`c4552031`。
- ✅ S0.2 延伸（IAM：组合根归位，BcIamConfiguration，保持行为不变）：将 `BcIamConfiguration` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.config` 不变；Bean 装配/副作用顺序不变；最小回归通过）；落地：`14cd4108`。
- ✅ S0.2 延伸（依赖方收敛：eva-app 去 bc-iam 编译期依赖，保持行为不变）：在 Serena + `rg` 证伪 `eva-app/src/main/java` 不再直接引用 `edu.cuit.bc.iam.*` 后，收敛 `eva-app/pom.xml`：移除对 `bc-iam` 的 Maven 编译期依赖（保留 `bc-iam-infra` 以闭合运行期装配；最小回归通过）；落地：`290f2b82`。
- ✅ S0.2 延伸（装配责任上推：start 显式依赖 bc-iam-infra，保持行为不变）：为后续收敛 `eva-app` 的运行期装配依赖边界，在 `start/pom.xml` 增加对 `bc-iam-infra` 的 `runtime` 依赖（与原 transitive 结果等价，仅显式化；最小回归通过）；落地：`8a5df2d0`。
- ✅ S0.2 延伸（依赖方收敛：eva-app 去 bc-iam-infra 依赖，保持行为不变）：在 Serena 证据化确认 `eva-app/src/main/java` 不再引用 `edu.cuit.app.*` 的 IAM 实现类后，收敛 `eva-app/pom.xml`：移除对 `bc-iam-infra` 的依赖；运行期装配由组合根 `start` 显式兜底（最小回归通过）；落地：`e7b46f36`。
- ✅ S0.2 延伸（依赖方收敛：bc-template-infra 去 bc-template 编译期依赖，保持行为不变）：在 Serena 证据化确认 `bc-template-infra` 当前无源码（仅 `pom.xml`）后，收敛 `bc-template/infrastructure/pom.xml`：将对 `bc-template` 的 Maven 依赖替换为 `bc-template-domain`（版本不变；最小回归通过）；落地：`aee98f9b`。
- ✅ S1（模板：编译闭合前置，保持行为不变）：为后续将 `CourseTemplateLockQueryPortImpl` 从 `eva-infra` 归位到 `bc-template-infra` 做前置，已在 `bc-template/infrastructure/pom.xml` 显式增加对 `eva-infra-dal` 的 Maven 编译期依赖（仅用于闭合 `Mapper/DO/QueryWrapper` 依赖）；最小回归通过；落地：`4f819e13`。
- ✅ S1（模板：装配责任上推，保持行为不变）：为后续 `CourseTemplateLockQueryPortImpl` 归位后仍可在运行期被组合根装配，已在 `start/pom.xml` 显式增加对 `bc-template-infra` 的 `runtime` 依赖（保持行为不变，仅上推依赖边界）；最小回归通过；落地：`d51975fb`。
- ✅ S1（模板：基础设施端口实现归位，保持行为不变）：将 `CourseTemplateLockQueryPortImpl` 从 `eva-infra` 搬运归位到 `bc-template-infra`（保持 `package` 与类内容不变，仅改变 Maven 模块归属）；最小回归通过；落地：`9b46d5a7`。
- ✅ S1（装配责任收敛：组合根去 `eva-infra(runtime)`，保持行为不变）：在 `start/pom.xml` 移除对 `eva-infra` 的 `runtime` 依赖（保持行为不变，仅收敛依赖边界）；最小回归通过；落地：`1e2ffa89`。
- ✅ S0.2 延伸（课程相关前置：模板锁定服务下沉，保持行为不变）：为后续让 `bc-course/application` 可收敛对 `bc-template` 应用层 jar 的编译期依赖，将模板锁定服务 `CourseTemplateLockService` 从 `bc-template/application` 下沉到 `bc-template-domain`（保持 `package edu.cuit.bc.template.application` 与代码不变；最小回归通过）；落地：`8a1319df`。
- ✅ S0.2 延伸（依赖方收敛：bc-course/application 去 bc-template 编译期依赖，保持行为不变）：在 Serena + `rg` 证伪 `bc-course/application` 仅引用 `CourseTemplateLockService/CourseTemplateLockQueryPort/TemplateLockedException` 后，收敛 `bc-course/application/pom.xml`：将对 `bc-template` 的 Maven 依赖替换为 `bc-template-domain`（版本不变；最小回归通过）；落地：`2de83046`。
- ✅ S0.2 延伸（入口壳收敛：Controller 起步，保持行为不变）：在 `UserUpdateController` 提取 `currentUserId()` 以减少适配层编排噪声（保持 `StpUtil.getLoginId()` 调用顺序与次数不变；最小回归通过）；落地：`09cb6454`。
- ✅ S0.2 延伸（入口壳收敛：Controller，登录入口，保持行为不变）：在 `AuthenticationController` 收敛 `login` 表达式，用“局部变量 + return”显式固化 `userAuthService.login(...)` → `CommonResult.success(...)` 的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`102a0cbb`。
- ✅ S0.2 延伸（入口壳收敛：Controller，用户查询入口，保持行为不变）：在 `UserQueryController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`18e0bb29`。
- ✅ S0.2 延伸（入口壳收敛：Controller，角色查询入口，保持行为不变）：在 `RoleQueryController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`fa04e48e`。
- ✅ S0.2 延伸（入口壳收敛：Controller，菜单查询入口，保持行为不变）：在 `MenuQueryController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`8521f72c`。
- ✅ S0.2 延伸（入口壳收敛：Controller，菜单写入口，保持行为不变）：在 `MenuUpdateController.create` 收敛日志内容构造表达式，保持 `menuService.create(...)` → `LogUtils.logContent(...)` → `CommonResult.success()` 顺序不变（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`9a2fc3ff`。
- ✅ S0.2 延伸（入口壳收敛：Controller，角色写入口，保持行为不变）：在 `RoleUpdateController.create` 收敛日志内容构造表达式，保持 `roleService.create(...)` → `LogUtils.logContent(...)` → `CommonResult.success()` 顺序不变（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`9f07698a`。
- ✅ S0.2 延伸（入口壳收敛：Controller，学期查询入口，保持行为不变）：在 `SemesterController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`24e5ad3f`。
- ✅ S0.2 延伸（入口壳收敛：Controller，院系查询入口，保持行为不变）：在 `DepartmentController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`e57c0e41`。
- ✅ S0.2 延伸（入口壳收敛：Controller，教室查询入口，保持行为不变）：在 `ClassroomController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`f1606018`。
- ✅ S0.2 延伸（入口壳收敛：Controller，课程查询入口，保持行为不变）：在 `QueryCourseController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`0a691bb1`。
- ✅ S0.2 延伸（入口壳收敛：Controller，用户-课程查询入口，保持行为不变）：在 `QueryUserCourseController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`d49976cf`。
- ✅ S0.2 延伸（入口壳收敛：Controller，课程写入口，保持行为不变）：在 `UpdateCourseController` 收敛返回表达式与日志内容构造写法（日志内容仍在 `CommonResult.success(..., supplier)` 内部构造；不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`58b1b763`。
- ✅ S0.2 延伸（入口壳收敛：Controller，课程删除入口，保持行为不变）：在 `DeleteCourseController` 清理无用 import，并收敛参数/调用的格式表达（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`20e1214c`。
- ✅ S0.2 延伸（入口壳收敛：Controller，评教统计查询入口，保持行为不变）：在 `EvaStatisticsController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`7c465c5d`。
- ✅ S0.2 延伸（入口壳收敛：Controller，评教查询入口，保持行为不变）：在 `EvaQueryController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`74a34133`。
- ✅ S0.2 延伸（入口壳收敛：Controller，评教写入口，保持行为不变）：在 `UpdateEvaController` 清理无用 import，并收敛日志内容构造写法（仍保持 `service` 调用 → `LogUtils.logContent(...)` → `CommonResult.success(null)` 的顺序不变；不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`87fc0475`。
- ✅ S0.2 延伸（入口壳收敛：Controller，评教删除入口，保持行为不变）：在 `DeleteEvaController` 清理未使用注入依赖与无用 import，并抽取 `success()` 收敛 `CommonResult.success(null)` 的重复表达（仍保持 `service` 调用 → `CommonResult.success(null)` 的执行顺序不变；不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`7809cc2d`。
- ✅ S0.2 延伸（入口壳收敛：Controller，评教配置查询入口，保持行为不变）：在 `EvaConfigQueryController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回结构/副作用顺序；权限注解保持不变；最小回归通过）；落地：`d055c271`。
- ✅ S0.2 延伸（入口壳收敛：Controller，评教配置修改入口，保持行为不变）：在 `EvaConfigUpdateController` 收敛返回表达式，抽取 `success()` 以显式固化“先调用 service → 再 `CommonResult.success()`”的执行顺序（不改异常文案/返回结构/副作用顺序；权限注解保持不变；最小回归通过）；落地：`c7ab9633`。
- ✅ S0.2 延伸（入口壳收敛：Controller，消息入口，保持行为不变）：在 `MessageController` 将查询接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`aa99c775`。
- ✅ S0.2 延伸（入口壳收敛：Controller，日志入口，保持行为不变）：在 `LogController` 将查询接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回结构/副作用顺序；最小回归通过）；落地：`e19278a6`。
- ✅ S0.2 延伸（入口壳收敛：角色写侧，保持行为不变）：将 `eva-app` 的 `RoleServiceImpl` 写侧方法（`updateInfo/updateStatus/assignPerm/create/delete/multipleDelete`）改为委托 `bc-iam` 的 UseCase（`UpdateRoleInfoUseCase/UpdateRoleStatusUseCase/AssignRolePermsUseCase/CreateRoleUseCase/DeleteRoleUseCase/DeleteMultipleRoleUseCase`），减少对旧 `RoleUpdateGateway` 的直耦合（事务边界仍由旧入口承接；异常文案/缓存失效/日志顺序与副作用完全不变；最小回归通过；落地：`a71efb84`）。
- ✅ S0.2 延伸（入口壳收敛：菜单写侧，保持行为不变）：将 `eva-app` 的 `MenuServiceImpl` 写侧方法（`update/create/delete/multipleDelete`）改为委托 `bc-iam` 的 UseCase（`UpdateMenuInfoUseCase/CreateMenuUseCase/DeleteMenuUseCase/DeleteMultipleMenuUseCase`），减少对旧 `MenuUpdateGateway` 的直耦合（事务边界仍由旧入口承接；异常文案/缓存失效/日志顺序与副作用完全不变；最小回归通过；落地：`905baf9f`）。
- ✅ S0.2 延伸（入口壳收敛：审计日志写侧，保持行为不变）：将 `eva-app` 的 `LogServiceImpl.registerListener` 中“插入日志”链路改为委托 `bc-audit` 的 `InsertLogUseCase`，并保持异步执行语义与副作用顺序完全不变（最小回归通过；落地：`cdb885b0`）。
- ✅ S0.2 延伸（入口壳收敛：用户写侧，保持行为不变）：将 `eva-app` 的 `UserServiceImpl` 写侧对 `UserUpdateGateway` 的调用改为直接委托 `bc-iam` 的 `UpdateUserInfoUseCase/UpdateUserStatusUseCase/AssignRoleUseCase/CreateUserUseCase/DeleteUserUseCase`（保留登录态解析/密码修改/登出/副作用顺序完全不变；读侧仍保留 `UserQueryGateway` 以保持缓存切面触发点不变；最小回归通过；落地：`2b095a69`）。
- ✅ S0.2 延伸（入口壳收敛：登录写侧，保持行为不变）：将 `eva-app` 的 `UserAuthServiceImpl.login` 中“LDAP 鉴权 + 用户状态校验”链路下沉到 `bc-iam` 的用例 `ValidateUserLoginUseCase`，旧入口退化为“登录态解析（`StpUtil.isLogin`）+ 委托用例 + `StpUtil.login` + 返回 token”的壳（异常文案/分支顺序完全不变；最小回归通过；落地：`d7c93768`）。
- ✅ S0.2 延伸（入口壳收敛：消息，保持行为不变）：将 `eva-app` 的 `MsgServiceImpl` 中对消息查询/标记已读/展示状态/删除/落库的调用，改为直接委托 `bc-messaging` 的 `QueryMessageUseCase/MarkMessageReadUseCase/UpdateMessageDisplayUseCase/DeleteMessageUseCase/InsertMessageUseCase`（保持 `checkAndGetUserId()` 先执行；副作用顺序与异常文案完全不变；最小回归通过；落地：`28ba21e4`）。
- ✅ S0.2 延伸（SemesterServiceImpl 收敛准备：依赖补齐，保持行为不变）：在 `bc-course/application/pom.xml` 补齐对 `eva-domain` 的编译期依赖，为后续新增学期查询用例并收敛 `SemesterServiceImpl` 做前置（最小回归通过；落地：`d5ea0d96`）。
- ✅ S0.2 延伸（SemesterServiceImpl 收敛准备：用例骨架，保持行为不变）：在 `bc-course/application` 新增学期查询用例 `SemesterQueryUseCase`（当前仅委托 `SemesterGateway`，不改业务语义；最小回归通过；落地：`7d8323b5`）。
- ✅ S0.2 延伸（SemesterServiceImpl 收敛准备：用例装配，保持行为不变）：在 `bc-course` 组合根 `BcCourseConfiguration` 补齐 `SemesterQueryUseCase` 的 Bean 装配（不改业务语义；最小回归通过；落地：`a93e2bda`）。
- ✅ S0.2 延伸（SemesterServiceImpl 收敛准备：依赖补齐，保持行为不变）：为后续让 `eva-app` 的 `SemesterServiceImpl` 能直接委托学期查询用例，在 `eva-app/pom.xml` 补齐对 `bc-course` 的编译期依赖（不改业务语义；最小回归通过；落地：`9f61788b`）。
- ✅ S0.2 延伸（入口壳收敛：学期，保持行为不变）：将 `eva-app` 的 `SemesterServiceImpl` 从直接调用 `SemesterGateway` 改为委托 `SemesterQueryUseCase`（保留事务边界；异常文案与副作用顺序不变；最小回归通过；落地：`292eb1f2`）。
- ✅ S0.2 延伸（DepartmentServiceImpl 收敛准备：用例骨架，保持行为不变）：在 `bc-iam` 新增院系查询用例 `DepartmentQueryUseCase`（当前仅委托 `DepartmentGateway.getAll()`，不改业务语义；最小回归通过；落地：`78fd4b0e`）。
- ✅ S0.2 延伸（DepartmentServiceImpl 收敛准备：用例装配，保持行为不变）：在 `BcIamConfiguration` 补齐 `DepartmentQueryUseCase` 的 Bean 装配（不改业务语义；最小回归通过；落地：`1cc7cc8a`）。
- ✅ S0.2 延伸（入口壳收敛：院系，保持行为不变）：将 `eva-app` 的 `DepartmentServiceImpl` 从直接调用 `DepartmentGateway` 改为委托 `DepartmentQueryUseCase`（异常文案与副作用顺序不变；最小回归通过；落地：`d9e4b7d6`）。
- ✅ S0.2 延伸（ClassroomServiceImpl 收敛准备：用例骨架，保持行为不变）：在 `bc-course` 新增教室查询用例 `ClassroomQueryUseCase`（当前仅委托 `ClassroomGateway.getAll()`，不改业务语义；最小回归通过；落地：`09822993`）。
- ✅ S0.2 延伸（ClassroomServiceImpl 收敛准备：用例装配，保持行为不变）：在 `BcCourseConfiguration` 补齐 `ClassroomQueryUseCase` 的 Bean 装配（不改业务语义；最小回归通过；落地：`abdfe122`）。
- ✅ S0.2 延伸（入口壳收敛：教室，保持行为不变）：将 `eva-app` 的 `ClassroomServiceImpl` 从直接调用 `ClassroomGateway` 改为委托 `ClassroomQueryUseCase`（异常文案与副作用顺序不变；最小回归通过；落地：`20361679`）。
- ✅ S0.2 延伸（课程 Controller 注入收敛到接口，保持行为不变）：在 `eva-adapter` 的课程相关 Controller 子簇试点，将注入类型从 `edu.cuit.app.service.impl.course.*ServiceImpl` 收窄为 `shared-kernel` 下的 `edu.cuit.client.api.course.*Service` 接口（Serena 证伪：各接口均只有一个 `@Service` 实现类；Spring 注入目标不变，仅减少编译期耦合；最小回归通过；落地：`47a6b06c`）。
- ✅ S0.2 延伸（课程旧入口归位：ICourseServiceImpl，保持行为不变）：将 `ICourseServiceImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变，仅搬运与编译闭合；不改业务语义/异常文案/副作用顺序）。为闭合 `StpUtil`（Sa-Token）编译期依赖，在 `bc-course-infra` 补齐 `zym-spring-boot-starter-security`（运行时 classpath 已存在，保持行为不变；最小回归通过；落地：`2b5bcecb`）。
- ✅ S0.2 延伸（课程旧入口归位：IUserCourseServiceImpl，保持行为不变）：将 `IUserCourseServiceImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变，仅搬运与编译闭合；不改业务语义/异常文案/副作用顺序）。为避免 `bc-course-infra` 反向依赖 `eva-app`，将 `IUserCourseServiceImpl` 对 `UserCourseDetailQueryExec/FileImportExec` 的依赖收敛为类内私有方法（逻辑逐行对齐原实现；保持行为不变；最小回归通过；落地：`79a351c3`）。
- ✅ S0.2 延伸（课程旧入口归位：ICourseDetailServiceImpl，保持行为不变）：将 `ICourseDetailServiceImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变，仅搬运与编译闭合；不改业务语义/异常文案/副作用顺序；最小回归通过；落地：`bd85a006`）。
- ✅ S0.2 延伸（依赖方收敛：eva-app 去 bc-course 编译期依赖，保持行为不变）：Serena + `rg` 证伪 `eva-app` 不再引用 `edu.cuit.bc.course.*` 后，已将 `eva-app/pom.xml` 中对 `bc-course` 的编译期依赖替换为 `shared-kernel`（即移除 `bc-course` 依赖；每次只改 1 个 `pom.xml`；最小回归通过；落地：`6fe8ffc8`）。
- ✅ 基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）：将 `DepartmentGatewayImpl` 从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；仅搬运/编译闭合；最小回归通过；落地：`acb13124`）。
- ✅ 基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）：将 `ClassroomGatewayImpl` 从 `eva-infra` 归位到 `bc-course-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；仅搬运/编译闭合；最小回归通过；落地：`26b183d5`）。
- ✅ 基础设施（S1 退场候选：支撑类归位，保持行为不变）：将 `SemesterConverter` 从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package edu.cuit.infra.convertor` 不变；仅搬运；最小回归通过；落地：`6c9e1d39`）。
- ✅ 基础设施（S1 退场候选：支撑类归位，保持行为不变）：将 `StaticConfigProperties` 从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package edu.cuit.infra.property` 不变；仅搬运；最小回归通过；落地：`5a26af43`）。
- ✅ 基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）：将 `SemesterGatewayImpl` 从 `eva-infra` 归位到 `bc-course-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；仅搬运/编译闭合；最小回归通过；落地：`30e6a160`）。
- ✅ 基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）：将 `MsgGatewayImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.gateway.impl` 不变；仅搬运/编译闭合；最小回归通过；落地：`8ffcfe35`）。
- ✅ 基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）：将 `LogGatewayImpl` 从 `eva-infra` 归位到 `bc-audit-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；仅搬运/编译闭合；最小回归通过；落地：`673a19e3`）。
- ✅ 基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）：将 `LdapPersonGatewayImpl` 从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；仅搬运/编译闭合；最小回归通过；落地：`1ff96d75`）。
- ✅ 基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）：将 `MenuQueryGatewayImpl`/`MenuUpdateGatewayImpl`/`RoleQueryGatewayImpl` 从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package edu.cuit.infra.gateway.impl.user` 不变；仅搬运/编译闭合；最小回归通过；落地：`a7cb96e9/09574045/457b6780`）。
- ✅ S0.2 延伸（分页转换器归位，保持行为不变）：将通用分页业务对象转换器 `PaginationBizConvertor` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor` 不变；逻辑不变；最小回归通过；落地：`c8c17225`），用于为后续归位课程旧入口/其它旧入口时闭合依赖并避免基础设施模块反向依赖 `eva-app`。
- ✅ S0.2 延伸（事务提交后事件发布器归位，保持行为不变）：将通用“事务提交后发布事件”发布器 `AfterCommitEventPublisher` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package edu.cuit.app.event` 不变；逻辑不变；最小回归通过；落地：`fc85f548`），用于为后续归位课程旧入口/端口适配器时闭合依赖并避免 `bc-course-infra` 反向依赖 `eva-app`。
- ✅ S0.2 延伸（课程读侧端口适配器归位，保持行为不变）：将 `CourseDetailQueryPortImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.bccourse.adapter` 不变；实现逻辑不变；最小回归通过；落地：`250002d5`），用于继续削减 `eva-app` 的课程域编译期引用面（细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（课程域转换器归位，保持行为不变）：将 `CourseBizConvertor` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor.course` 不变；仅搬运/编译闭合；最小回归通过；落地：`eec5d45c`），用于为后续继续归位课程域端口适配器/旧入口并削减 `eva-app` 课程域引用面提供依赖闭包。
- ✅ S0.2 延伸（课程域组合根归位，保持行为不变）：将课程域组合根 `BcCourseConfiguration` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.config` 不变；Bean 定义/装配顺序不变；最小回归通过；落地：`49477dd1`），用于继续收敛 `eva-app` 对课程 BC 的编译期引用面（细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（课程读侧查询接口归位，保持行为不变）：Serena 证伪 `CourseQueryRepo` 仅由课程读侧委托壳/查询仓储使用后，将 `CourseQueryRepo` 从 `eva-infra-shared` 归位到 `bc-course/infrastructure`（保持 `package` 不变；最小回归通过；落地：`5101a341`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（课程读侧查询实现归位，保持行为不变）：将 `CourseQueryRepository` 从 `eva-infra` 归位到 `bc-course/infrastructure`，并将其依赖的 `CourseRecommendExce` 从 `eva-infra` 迁移到 `eva-infra-shared` 以闭合编译期依赖（均保持 `package` 不变；最小回归通过；落地：`881e1d12`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（课程旧 gateway 归位，保持行为不变）：将 `CourseQueryGatewayImpl/CourseUpdateGatewayImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`；`CourseQueryRepo` 先从 `eva-infra` 迁移到 `eva-infra-shared` 再归位到 `bc-course/infrastructure` 以闭合编译期依赖（均保持 `package` 不变；最小回归通过；落地：`d438e060/5101a341`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：Serena 证伪 `eva-infra` 未引用 `edu.cuit.bc.course.*` 后，将 `eva-infra/pom.xml` 的 `bc-course` 依赖替换为 `shared-kernel`（每次只改 1 个 `pom.xml`；最小回归通过；落地：`8d806bf0`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（依赖方收敛补齐，保持行为不变）：为闭合 `eva-adapter` 对 `edu.cuit.client.api.IClassroomService` 的编译期引用，将 `IClassroomService` 从 `bc-course/application` 下沉到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`38f58e0a`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：Serena 证伪 `eva-adapter` 未引用 `edu.cuit.bc.course.*`，因此在 `eva-adapter/pom.xml` 排除经由 `eva-app` 传递的 `bc-course`，并改为显式依赖 `shared-kernel`（最小回归通过；落地：`f8ff84f5`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（课程域基础设施归位，保持行为不变）：将 `ChangeCourseTemplateRepositoryImpl/ImportCourseFileRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`，并将 `CourseImportExce` 归位到 `eva-infra-shared` 以闭合编译依赖；`eva-infra/.../bccourse/adapter/*RepositoryImpl` 残留由 3 减至 1（落地：`33032890`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（课程域基础设施归位：operate 子簇继续收敛，保持行为不变）：Serena 证伪 `CourseImportExce/CourseRecommendExce` 仅课程域使用后，将其从 `eva-infra-shared` 归位到 `bc-course/infrastructure`（保持 `package` 不变，仅搬运与编译闭合；最小回归通过；落地：`d3b9247e`）；`CourseFormat` 跨 BC 复用继续留在 `eva-infra-shared`。
- ✅ S0.2 延伸（依赖方收敛，证伪，保持行为不变）：Serena 证据化确认 `eva-app` 仍大量引用 `edu.cuit.bc.course.*`，不满足“仅使用 `edu.cuit.client.*`”前提，因此本阶段未进行 `eva-app/pom.xml` 的依赖替换（细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（依赖方收敛：证伪“当前无更多可替换 pom”，保持行为不变）：本次重新盘点显示：除 `eva-app` 外，未发现仍显式依赖 `bc-course` 且满足“仅使用 `edu.cuit.client.*` 类型”的模块，因此本会话无可执行的 `pom.xml` 依赖替换点；下一步需先继续收敛 `eva-app` 对课程域用例/端口/异常的直接引用，再回到“逐个模块依赖替换”。
- ✅ bc-course（课程，写侧入口归位继续，方向 A → B）：已完成 `ICourseDetailServiceImpl.updateCourses/delete/addCourse` 的入口用例归位/调用点端口化（保持 `@CheckSemId`/事务边界/异常文案/副作用顺序完全不变；落地：`849ed92e/e38463c2/5c989ace`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ bc-course（课程，S0 收尾：依赖收窄）：已清理旧入口 `ICourseServiceImpl` / `IUserCourseServiceImpl` 中可证实“仅声明无调用点”的残留注入依赖，并将 `IUserCourseServiceImpl.isImported` 的依赖从 `CourseUpdateGateway` 收敛为直接依赖 `IsCourseImportedUseCase`（保持行为不变；落地：`9577cd85/402affc2/25aad45a`）。
- ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：已完成 `CourseUpdateGatewayImpl.updateCourseType/addCourseType` 压扁样例（旧 gateway 仅保留事务边界与委托调用；保持行为不变；落地：`785974a6/34e9a0a8`）。
- ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.assignTeacher`：新增 `AssignTeacherGatewayEntryUseCase`，并让旧 gateway 不再构造命令（Serena：调用点为 `AllocateTeacherPortImpl.allocateTeacher`；保持事务边界/异常文案/副作用顺序完全不变；最小回归通过；落地：`0b85c612`）。
- ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.updateCourse`：新增 `UpdateCourseGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 异常转换 + 委托调用（Serena：调用点为 `UpdateCoursePortImpl.updateCourse`；保持行为不变；最小回归通过；落地：`c31df92c`）。
- ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.updateCourses`：旧 gateway 不再构造 Command，仅保留事务边界 + 异常转换 + 委托调用（委托 `UpdateCoursesEntryUseCase`；Serena：未发现 `courseUpdateGateway.updateCourses(...)` 调用点；保持行为不变；最小回归通过；落地：`84dffcc2`）。
- ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.importCourseFile`：新增 `ImportCourseFileGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 异常转换 + 委托调用（Serena：调用点为 `ImportCourseFilePortImpl.importCourseFile`；保持行为不变；最小回归通过；落地：`5e93a08a`）。
- ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.updateSingleCourse`：新增 `UpdateSingleCourseGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 异常转换 + 委托调用（Serena：调用点为 `UpdateSingleCoursePortImpl.updateSingleCourse`；保持行为不变；最小回归通过；落地：`9eea1a54`）。
- ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.updateSelfCourse`：新增 `UpdateSelfCourseGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 委托调用（Serena：调用点为 `UpdateSelfCoursePortImpl.updateSelfCourse`；保持行为不变；最小回归通过；落地：`c0f30c1f`）。
- ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.addNotExistCoursesDetails`：新增 `AddNotExistCoursesDetailsGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 委托调用（Serena：调用点为 `AddNotExistCoursesDetailsPortImpl.addNotExistCoursesDetails`；保持行为不变；最小回归通过；落地：`62d48ee6`）。
- ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.addExistCoursesDetails`：新增 `AddExistCoursesDetailsGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 委托调用（Serena：调用点为 `AddExistCoursesDetailsPortImpl.addExistCoursesDetails`；保持行为不变；最小回归通过；落地：`de34a308`）。
- ✅ S0.2（依赖面收敛，保持行为不变）：将学期 CO `SemesterCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`77126c4a`）。
- ✅ S0.2（依赖面收敛，保持行为不变）：将通用学期入参 `Term` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`23bff82f`）。
- ✅ S0.2 延伸（bc-course 协议承载面收敛，保持行为不变）：将评教域 CO `CourseScoreCO/EvaTeacherInfoCO` 从 `bc-evaluation/contract` 迁移到 `shared-kernel`（保持 `package` 不变），并移除 `bc-course/application` 对 `bc-evaluation-contract` 的编译期依赖（最小回归通过；落地：`bc30e9de`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（bc-course 协议承载面收敛，保持行为不变）：将单节课详情 CO `SingleCourseDetailCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`95b01a07`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（bc-course 协议承载面收敛，保持行为不变）：将单课次衍生详情 CO `ModifySingleCourseDetailCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`1e9be81d`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（bc-course 协议承载面收敛，保持行为不变）：将课程 API 接口 `ICourseService/ICourseTypeService` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`4dbeb55f`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2 延伸（bc-course 协议承载面收敛，保持行为不变）：将课程详情接口 `ICourseDetailService` 从 `bc-course/application` 迁移到 `shared-kernel`，并将其签名依赖 `SimpleCourseResultCO` 一并下沉（均保持 `package` 不变；最小回归通过；落地：`f9ccc6e9`；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
- ✅ S0.2（依赖面收敛补齐，保持行为不变）：将课程用户侧接口 `IUserCourseService`（以及其出参 `SimpleSubjectResultCO`）迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`e2a697f1`），从而移除 `bc-ai-report-infra` 对 `bc-course` 的显式编译期依赖（避免依赖回潮）。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：Serena 证伪 `bc-evaluation-infra` 未引用 `edu.cuit.bc.course.*` 内部实现类/包，仅使用 `edu.cuit.client.*` 课程域 API；因此将 `bc-evaluation/infrastructure/pom.xml` 中对 `bc-course` 的编译期依赖替换为显式依赖 `shared-kernel`（最小回归通过；落地：`a0bcf74f`）。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：Serena 证伪 `eva-infra-shared` 未引用 `edu.cuit.bc.course.*` 内部实现类/包，仅使用已由 `shared-kernel` 承载的 `edu.cuit.client.*` 类型；因此将 `eva-infra-shared/pom.xml` 中对 `bc-course` 的编译期依赖替换为显式依赖 `shared-kernel`（最小回归通过；落地：`6ab3837a`）。
- ✅ S0.2 延伸（课程域基础设施归位起步，保持行为不变）：将 `edu.cuit.infra.bccourse.adapter` 下 15 个无缓存/无事务注解的 `*PortImpl` 从 `eva-infra` 迁移到 `bc-course-infra`（仅搬运文件，`package` 不变；行为不变），为后续 `eva-infra` 去 `bc-course` 依赖铺路（最小回归通过；落地：`c4179654`）。
- ✅ S0.2 延伸（依赖方收敛补齐，保持行为不变）：将学期 API `edu.cuit.client.api.ISemesterService` 从 `bc-course/application` 下沉到 `shared-kernel`（保持 `package` 不变；行为不变），以闭合 `bc-evaluation-infra` 的编译期引用（最小回归通过；落地：`c22802ff`）。
- ✅ S0.2 延伸（课程域基础设施归位前置，保持行为不变）：将 `ClassroomCacheConstants` 从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package` 与 Bean 名称不变；行为不变），为后续迁移课程 `*RepositoryImpl` 出 `eva-infra` 做依赖闭包准备（最小回归通过；落地：`c22802ff`）。
- ✅ S0.2 延伸（课程域基础设施归位起步，保持行为不变）：将 `AddCourseTypeRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，缓存/事务/异常文案完全不变），并在 `bc-course-infra` 补齐 `zym-spring-boot-starter-cache` 编译期依赖以闭合 `LocalCacheManager`（最小回归通过；落地：`8426d4f2`）。
- ✅ S0.2 延伸（课程域基础设施归位推进，保持行为不变）：将 `UpdateCoursesTypeRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，事务/异常文案/副作用顺序完全不变；最小回归通过；落地：`12d16c6a`）。
- ✅ S0.2 延伸（课程域基础设施归位推进，保持行为不变）：将 `UpdateCourseInfoRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，事务/异常文案/副作用顺序完全不变；最小回归通过；落地：`eb940498`）。
- ✅ S0.2 延伸（课程域基础设施归位推进，保持行为不变）：将 `DeleteSelfCourseRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，事务/异常文案/副作用顺序完全不变；最小回归通过；落地：`73ed7c7d`）。
- ✅ S0.2 延伸（课程域基础设施归位推进，保持行为不变）：将 `UpdateSingleCourseRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，事务/日志/异常文案/副作用顺序完全不变）；为闭合编译期依赖，将 `CourInfTimeOverlapQuery` 归位到 `eva-infra-shared`（保持 `package` 不变）并在 `bc-course-infra` 补齐 `zym-spring-boot-starter-logging` 编译期依赖以闭合 `LogUtils`（最小回归通过；落地：`1a01e827`）。
- ✅ S0.2 延伸（课程域基础设施归位推进，保持行为不变）：将 `UpdateSelfCourseRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，事务/异常文案/副作用顺序完全不变）；为闭合编译期依赖，将 `ClassroomOccupancyChecker` 归位到 `eva-infra-shared`（保持 `package` 不变；最小回归通过；落地：`3d1dd4f1`）。
- ✅ S0.2 延伸（课程域基础设施归位批量推进试点，保持行为不变）：按“选项 2（2 类同簇）”试点，将 `AddExistCoursesDetailsRepositoryImpl` 与 `AddNotExistCoursesDetailsRepositoryImpl` 从 `eva-infra` 批量归位到 `bc-course/infrastructure`（仅搬运文件，事务/日志/异常文案/副作用顺序完全不变）；并试点引入 IDEA MCP `get_file_problems(errorsOnly=true)` 作为搬运后快速预检（不替代最小回归；最小回归通过；落地：`bd042ea9`）。
- ✅ S0.2 延伸（课程域基础设施归位推进，按“选项 2（2 类同簇）”，保持行为不变）：将 `DeleteCourseRepositoryImpl` 与 `DeleteCoursesRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，事务/日志/异常文案/副作用顺序完全不变；最小回归通过；落地：`df4ac6ca`）。
- ✅ S0.2 延伸（课程域基础设施归位推进，按“选项 2（2 类同簇）”，保持行为不变）：将 `DeleteCourseTypeRepositoryImpl` 与 `UpdateCourseTypeRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，事务/日志/异常文案/副作用顺序完全不变；最小回归通过；落地：`33844ce0`）。
- ✅ S0.2 延伸（依赖方收敛，保持行为不变）：Serena 证伪 `eva-infra` 未引用 `edu.cuit.bc.course.*` 后，将 `eva-infra/pom.xml` 的 `bc-course` 依赖替换为 `shared-kernel`（每次只改 1 个 `pom.xml`；最小回归通过；落地：`8d806bf0`）。
- ✅ 规划与证据化（保持行为不变）：用 Serena 盘点 bc-course 写侧 `@CheckSemId` 入口清单与 `eva-infra` 旧 `*GatewayImpl` 候选清单，并落盘到本文件 `4.3`，作为后续 S1/S2 退场/排期依据；同时修正文档中 `bc-messaging` 的主线口径为“后置仅做结构折叠/依赖证伪”（不改业务语义；最小回归通过；落地提交以 `git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md` 为准）。
- 评教用户读侧（D1：用例归位深化—去评教/被评教记录）：新增 `UserEvaQueryUseCase` 并将旧入口 `UserEvaServiceImpl.getEvaLogInfo/getEvaLoggingInfo` 退化为纯委托壳（旧入口仍保留 `@CheckSemId` 与当前用户解析：`StpUtil` + `userQueryGateway`；异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地提交：`96e65019`）。
- 评教任务读侧用例归位深化（本人任务列表）：将旧入口 `EvaTaskServiceImpl.evaSelfTaskInfo` 的“任务列表查询 + 懒加载顺序对齐的实体→CO 组装”归位到 `EvaTaskQueryUseCase`；旧入口仍保留 `@CheckSemId` 与当前用户解析（`StpUtil` + `userQueryGateway`）并委托 UseCase（异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地提交：`1ac196c6`）。
- 评教任务读侧用例归位深化（单任务详情）：将旧入口 `EvaTaskServiceImpl.oneEvaTaskInfo` 退化为纯委托壳，并把 “单任务查询 + 懒加载顺序对齐的实体→CO 组装” 归位到 `EvaTaskQueryUseCase`（异常文案不变；保持行为不变；最小回归通过；落地提交：`94736365`）。
- 评教模板读侧用例归位深化（全量模板列表）：将旧入口 `EvaTemplateServiceImpl.evaAllTemplate` 退化为纯委托壳，并把 “全量模板查询 + 结果组装” 归位到 `EvaTemplateQueryUseCase`（保持行为不变；最小回归通过；落地提交：`cd8e6ecb`）。
- 评教模板读侧用例归位深化（按任务取模板）：将旧入口 `EvaTemplateServiceImpl.evaTemplateByTaskId` 退化为纯委托壳，并把 “按任务取模板 + 空结果兜底 JSON” 归位到 `EvaTemplateQueryUseCase`（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`f98a9eed`）。
- 评教任务读侧用例归位深化（分页）：新增 `EvaTaskQueryUseCase` 并将旧入口 `EvaTaskServiceImpl.pageEvaUnfinishedTask` 退化为纯委托壳，把“分页查询 + 实体→CO 组装 + 分页结果组装”归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`d67f0ace`）。
- 评教模板读侧用例归位深化（分页）：新增 `EvaTemplateQueryUseCase` 并将旧入口 `EvaTemplateServiceImpl.pageEvaTemplate` 退化为纯委托壳，把“实体→CO 组装 + 分页结果组装”归位到 UseCase（保持 `@CheckSemId` 触发点不变；时间格式/分页字段赋值顺序不变；保持行为不变；最小回归通过；落地提交：`afcb4ff7`）。
- bc-messaging（消息域）：依赖收敛后半段（运行时装配上推准备）：在 `start/pom.xml` 增加对 `bc-messaging` 的 `runtime` 依赖（保持行为不变；最小回归通过；落地提交：`f23254ec`）。
- bc-messaging（消息域）：依赖收敛后半段（运行时装配责任上推）：移除 `eva-infra/pom.xml` 对 `bc-messaging` 的 `runtime` 依赖（保持行为不变；最小回归通过；落地提交：`507f95b2`）。
- bc-messaging（消息域）：依赖收敛后半段（兜底依赖证伪）：使用 Serena 证伪 `eva-infra` 无 `bcmessaging` / `edu.cuit.bc.messaging` / `bc-messaging` 的编译期引用，且运行时装配由 `start` 承接（证据见 `NEXT_SESSION_HANDOFF.md` 0.9；保持行为不变；最小回归通过）。
- bc-course（课程）：读侧入口用例归位起步（方向 A → B）：新增 `CourseQueryUseCase` + `CourseScheduleQueryPort`，并将旧入口 `ICourseServiceImpl.courseNum/courseTimeDetail/getDate` 退化为委托壳（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`4b06187f`）。
- bc-course（课程）：读侧入口用例归位起步（方向 A → B）：单节课详情：新增 `CourseDetailQueryUseCase` + `CourseDetailQueryPort`，并将旧入口 `ICourseServiceImpl.getCourseDetail` 退化为委托壳（异常文案保持不变；最小回归通过；落地提交：`d045c79e`）。
- bc-course（课程）：读侧入口用例归位起步（方向 A → B）：指定时间段课程：新增 `TimeCourseQueryUseCase` + `TimeCourseQueryPort`，并将旧入口 `ICourseServiceImpl.getTimeCourse` 退化为委托壳（保留 `StpUtil.getLoginId()` 解析与 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`4454ecae`）。
- bc-course（课程）：写侧入口用例归位起步（方向 A → B）：分配评教老师：新增 `AllocateTeacherUseCase` + `AllocateTeacherPort`，并将旧入口 `ICourseServiceImpl.allocateTeacher` 退化为委托壳（保持 `@CheckSemId` 与 AfterCommit 发布顺序不变；保持行为不变；最小回归通过；落地提交：`6e20721b`）。
- bc-course（课程）：写侧入口用例归位起步（方向 A → B）：批量删课：新增 `DeleteCoursesEntryUseCase` + `DeleteCoursesPort`，并将旧入口 `ICourseServiceImpl.deleteCourses` 退化为委托壳（保持 `@CheckSemId` 与 AfterCommit 发布顺序不变；保持行为不变；最小回归通过；落地提交：`d53b287a`）。
- bc-course（课程）：写侧入口用例归位继续（方向 A → B）：单节课修改：新增 `UpdateSingleCourseEntryUseCase` + `UpdateSingleCoursePort`，并将旧入口 `ICourseServiceImpl.updateSingleCourse` 退化为委托壳（保持 `@CheckSemId`、两次 `StpUtil.getLoginId()` 调用位置/顺序与 AfterCommit 发布顺序完全不变；保持行为不变；最小回归通过；落地提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准）。
- bc-course（课程）：写侧入口用例归位继续（方向 A → B）：批量新建多节课（新课程）：新增 `AddNotExistCoursesDetailsEntryUseCase` + `AddNotExistCoursesDetailsPort`，并将旧入口 `ICourseServiceImpl.addNotExistCoursesDetails` 保留 `@CheckSemId` 且退化为委托壳；端口适配器委托既有 `CourseUpdateGateway.addNotExistCoursesDetails(...)`（复用旧事务边界/异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地提交：`5a73fb75`）。
- bc-course（课程）：写侧入口用例归位继续（方向 A → B）：批量新建多节课（已有课程）：新增 `AddExistCoursesDetailsEntryUseCase` + `AddExistCoursesDetailsPort`，并将旧入口 `ICourseServiceImpl.addExistCoursesDetails` 退化为委托壳；端口适配器委托既有 `CourseUpdateGateway.addExistCoursesDetails(...)`（复用旧事务边界/异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地提交：`a5a9c777`）。
- bc-course（课程）：写侧入口用例归位继续（方向 A → B）：教师自助删课：新增 `DeleteSelfCourseEntryUseCase` + `DeleteSelfCoursePort`，并将旧入口 `IUserCourseServiceImpl.deleteSelfCourse` 退化为委托壳（保持两次 `StpUtil.getLoginId()` 调用位置/顺序与 AfterCommit 发布顺序完全不变；保持行为不变；最小回归通过；落地提交：`76845038`）。
- bc-course（课程）：写侧入口用例归位继续（方向 A → B）：教师自助改课：新增 `UpdateSelfCourseEntryUseCase` + `UpdateSelfCoursePort`，并将旧入口 `IUserCourseServiceImpl.updateSelfCourse` 退化为委托壳（保持两次 `StpUtil.getLoginId()` 调用位置/顺序与 AfterCommit 发布顺序完全不变，且 `CourseOperationMessageMode.TASK_LINKED` 参数不变；保持行为不变；最小回归通过；落地提交：`2d1327d3`）。
- bc-course（课程）：写侧入口用例归位继续（方向 A → B）：导入课表：新增 `ImportCourseFileEntryUseCase` + `ImportCourseFilePort` 并将 `IUserCourseServiceImpl.importCourse` 内的 `importCourseFile(...)` 调用点端口化（保留解析/`type` 分支/异常文案/异常类型与 AfterCommit 发布顺序不变；保持行为不变；最小回归通过；落地提交：`054b511d`）。
- bc-course（课程）：写侧入口用例归位继续（方向 A → B）：修改课程信息：新增 `UpdateCourseEntryUseCase` + `UpdateCoursePort` 并将 `ICourseDetailServiceImpl.updateCourse` 内的 `updateCourse(...)` 调用点端口化（保持 `@CheckSemId`、异常转换与 AfterCommit 发布顺序不变；保持行为不变；最小回归通过；落地提交：`bcf17d7f`）。
- 评教记录读侧（依赖收窄，小步）：`EvaRecordServiceImpl.pageEvaRecord` 内联分页结果组装，移除对 `PaginationBizConvertor` 的注入依赖（分页字段赋值顺序/异常文案/循环副作用顺序不变；保持行为不变；最小回归通过；落地提交：`55103de1`）。
- 评教记录读侧（D1：用例归位深化）：新增 `EvaRecordQueryUseCase` 并将 `EvaRecordServiceImpl.pageEvaRecord` 退化为纯委托壳，把“实体→CO 组装 + 平均分填充 + 分页组装”编排逻辑归位到 UseCase（保持 `@CheckSemId` 触发点不变；异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地提交：`86772f59`）。
- 评教记录读侧（D1：顺序对齐加固）：对齐 `EvaRecordQueryUseCase` 内部“实体→CO 组装”的求值顺序，避免提前触发 `Supplier` 缓存加载导致副作用顺序漂移（保持行为不变；最小回归通过；落地提交：`10991314`）。
- bc-messaging（消息域）：组合根归位：将 `BcMessagingConfiguration` 从 `eva-app` 迁移到 `bc-messaging`（保持 `package` 不变；最小回归通过；落地提交：`4e3e2cf2`）。
- bc-messaging（消息域）：监听器归位（课程副作用）：将 `CourseOperationSideEffectsListener` 从 `eva-app` 迁移到 `bc-messaging`（保持 `package` 不变；最小回归通过；落地提交：`22ee30e7`）。
- bc-messaging（消息域）：监听器归位（课程教师任务消息）：将 `CourseTeacherTaskMessagesListener` 从 `eva-app` 迁移到 `bc-messaging`（保持 `package` 不变；最小回归通过；落地提交：`0987f96f`）。
- bc-messaging（消息域）：支撑类归位（消息发送组装）：将 `MsgResult` 从 `eva-app` 迁移到 `bc-messaging-contract`（保持 `package` 不变；最小回归通过；落地提交：`31878b61`）。
- bc-messaging（消息域）：应用侧端口适配器归位（课程广播）：将 `CourseBroadcastPortAdapter` 从 `eva-app` 迁移到 `bc-messaging`（保持 `package` 不变；最小回归通过；落地提交：`84ee070a`）。
- bc-messaging（消息域）：应用侧端口适配器归位（教师任务消息）：将 `TeacherTaskMessagePortAdapter` 从 `eva-app` 迁移到 `bc-messaging`（保持 `package` 不变；最小回归通过；落地提交：`9ea14cff`）。
- bc-messaging（消息域）：应用侧端口适配器归位（评教消息清理）：将 `EvaMessageCleanupPortAdapter` 从 `eva-app` 迁移到 `bc-messaging`（保持 `package` 不变；依赖类型从 `MsgServiceImpl` 收窄为 `IMsgService` 以避免 Maven 循环依赖；最小回归通过；落地提交：`73ab3f3c`）。
- bc-messaging（消息域）：依赖收敛准备（事件枚举下沉到 contract）：将 `CourseOperationMessageMode` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；仅类归位不改语义；最小回归通过；落地提交：`b2247e7f`）。
- bc-messaging（消息域）：依赖收敛准备（事件载荷下沉到 contract）：将 `CourseOperationSideEffectsEvent` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；仅类归位不改语义；最小回归通过；落地提交：`ea2c0d9b`）。
- bc-messaging（消息域）：依赖收敛准备（事件载荷下沉到 contract）：将 `CourseTeacherTaskMessagesEvent` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；仅类归位不改语义；最小回归通过；落地提交：`12f43323`）。
- bc-messaging（消息域）：依赖收敛（应用侧编译期依赖面收窄）：`eva-app` 已将对 `bc-messaging` 的编译期依赖收敛为仅依赖 `bc-messaging-contract`（仅用于事件载荷类型；保持行为不变；最小回归通过；落地提交：`d3aeb3ab`）。
- bc-messaging（消息域）：基础设施端口适配器归位前置（DAL 归位）：将消息表数据对象 `MsgTipDO` 从 `eva-infra` 归位到 `eva-infra-dal`（保持 `package` 不变；仅类归位不改语义；最小回归通过；落地提交：`47ea52da`）。
- bc-messaging（消息域）：基础设施端口适配器归位前置（DAL 归位）：将消息表 Mapper `MsgTipMapper`（含 `MsgTipMapper.xml`）从 `eva-infra` 归位到 `eva-infra-dal`（保持 `package/namespace` 不变；仅类与资源归位不改语义；最小回归通过；落地提交：`5225ed43`）。
- bc-messaging（消息域）：基础设施端口适配器归位（消息删除）：将 `MessageDeletionPortImpl` 从 `eva-infra` 迁移到 `bc-messaging`（保持 `package` 不变；并在 `bc-messaging` 补齐对 `eva-infra-dal` 的依赖以闭合编译；最小回归通过；落地提交：`631779b9`）。
- bc-messaging（消息域）：基础设施端口适配器归位（消息已读）：将 `MessageReadPortImpl` 从 `eva-infra` 迁移到 `bc-messaging`（保持 `package` 不变；异常文案/更新条件/校验顺序不变；最小回归通过；落地提交：`9b911048`）。
- bc-messaging（消息域）：基础设施端口适配器归位（消息显示状态）：将 `MessageDisplayPortImpl` 从 `eva-infra` 迁移到 `bc-messaging`（保持 `package` 不变；异常文案/更新条件/校验顺序不变；最小回归通过；落地提交：`6e10866e`）。
- bc-messaging（消息域）：基础设施端口适配器归位前置（Convertor 归位）：将 `MsgConvertor` 从 `eva-infra` 归位到 `eva-infra-shared`，并补齐 `eva-infra-shared` 对 `bc-messaging-contract` 的依赖以闭合 `GenericRequestMsg` 类型引用（保持 `package` 不变；最小回归通过；落地提交：`740bdabb`）。
- bc-messaging（消息域）：基础设施端口适配器归位（消息新增）：将 `MessageInsertionPortImpl` 从 `eva-infra` 迁移到 `bc-messaging`（保持 `package` 不变；插入后回填 `id/createTime` 的顺序不变；并在 `bc-messaging` 补齐对 `eva-infra-shared` 的依赖以闭合 `MsgConvertor` 类型引用；最小回归通过；落地提交：`4725a00e`）。
- bc-messaging（消息域）：基础设施端口适配器归位（消息查询）：将 `MessageQueryPortImpl` 从 `eva-infra` 迁移到 `bc-messaging`（保持 `package` 不变；查询条件/排序/异常文案不变；最小回归通过；落地提交：`b93f43de`）。
- bc-course（课程）：课表 Excel/POI 解析端口化：新增 `CourseExcelResolvePort`（`bc-course/application`），由 `bc-course-infra` 提供适配器 `CourseExcelResolvePortImpl`（内部仍复用 `CourseExcelResolver`，确保异常文案与日志行为不变）；`eva-app` 调用侧改为依赖端口，移除对解析实现的直接依赖（保持行为不变；最小回归通过；落地提交：`5a7cd0a0`）。
- 评教读侧进一步解耦（统计导出端口装配委托切换）：将 `BcEvaluationConfiguration.evaStatisticsExportPort()` 从直接委托 `EvaStatisticsExcelFactory::createExcelData` 切换为委托 `bc-evaluation-infra` 的 `EvaStatisticsExportPortImpl`（内部仍调用 `EvaStatisticsExcelFactory.createExcelData`；保持行为不变；最小回归通过；落地提交：`565552fa`）。
- 评教读侧进一步解耦（导出基础设施归位：迁移 EvaStatisticsExcelFactory）：将统计导出工厂 `EvaStatisticsExcelFactory` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持行为不变；导出异常文案/日志输出完全一致；最小回归通过；落地提交：`5b2c2223`）。
- 评教读侧进一步解耦（导出基础设施归位：迁移 FillUserStatisticsExporterDecorator）：将导出装饰器 `FillUserStatisticsExporterDecorator` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e83600f6`）。
- 评教读侧进一步解耦（导出基础设施归位：迁移 FillEvaRecordExporterDecorator）：将导出装饰器 `FillEvaRecordExporterDecorator` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`b3afcb11`）。
- 评教读侧进一步解耦（导出基础设施归位：迁移 FillAverageScoreExporterDecorator）：将导出装饰器 `FillAverageScoreExporterDecorator` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`4e150984`）。
- 评教读侧进一步解耦（导出基础设施归位：迁移 EvaStatisticsExporter）：将导出基类 `EvaStatisticsExporter` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package` 不变），并补齐 `bc-evaluation-infra` 对 `bc-course/bc-iam-contract` 的编译依赖以闭合类型引用（保持行为不变；最小回归通过；落地提交：`e8ca391c`）。
- 评教读侧进一步解耦（导出基础设施归位准备：ExcelUtils 迁移）：将 POI 工具类 `ExcelUtils` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package` 不变），并在 `eva-infra-shared` 补齐 `poi/poi-ooxml` 依赖，为后续导出实现归位扫清“循环依赖”风险（保持行为不变；最小回归通过；落地提交：`04009c85`）。
- 评教读侧用例归位深化（统计：exportEvaStatistics 导出链路委托 UseCase）：引入统计导出端口 `EvaStatisticsExportPort`（Bean 委托既有 `EvaStatisticsExcelFactory.createExcelData`），并将旧入口 `EvaStatisticsServiceImpl.exportEvaStatistics` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.exportEvaStatistics`（保持 `@CheckSemId` 触发点与异常文案/日志顺序不变；最小回归通过；落地提交：`0d15de60`）。
- 评教读侧用例归位深化（统计：UseCase 内部 type 分支分发逻辑收敛）：在 `EvaStatisticsQueryUseCase` 抽出 `dispatchByType(...)`，统一复用 `type==0/type==1/否则抛 SysException("type是10以外的值")` 的分发逻辑，减少重复分支判断（只重构不改业务语义/异常文案；最小回归通过；落地提交：`38ce9ece`）。
- 工程噪音收敛（dev 环境 MyBatis 日志）：将 `application-dev.yml` 中 MyBatis-Plus 的 `log-impl` 从 `org.apache.ibatis.logging.stdout.StdOutImpl` 切换为 `org.apache.ibatis.logging.slf4j.Slf4jImpl`，避免 SQL 调试日志直出 stdout（仅 dev profile，生产不变；最小回归通过；落地提交：`cb3a4620`）。
- 工程噪音收敛（dev/test 非法入参打印）：将 `application-dev.yml/application-test.yml` 中 `common.print-illegal-arguments` 从 `true` 调整为 `false`，减少控制台噪音（仅 dev/test profile；不改业务逻辑；最小回归通过；落地提交：`21ba35dd`）。
- 评教读侧进一步解耦（统计：导出链路子端口补齐—CountAbEva）：新增统计读侧子端口 `EvaStatisticsCountAbEvaQueryPort` 并让 `EvaStatisticsOverviewQueryPort` `extends` 该子端口（仅接口细分，不改实现/不改装配；保持行为不变；最小回归通过；落地提交：`24b13138`）。
- 评教读侧进一步解耦（统计：导出基类依赖类型收窄—CountAbEva）：将导出基类 `EvaStatisticsExporter` 静态初始化中获取统计端口的依赖类型从 `EvaStatisticsOverviewQueryPort` 收窄为 `EvaStatisticsCountAbEvaQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；保持行为不变；最小回归通过；落地提交：`7337d378`）。
- 评教读侧用例归位深化（统计：pageUnqualifiedUser 旧入口退化为委托壳—分页结果组装）：将旧入口 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.pageUnqualifiedUserAsPaginationQueryResult`，并移除对 `PaginationBizConvertor` 的依赖，从而把分页结果组装彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`f4f3fcde`）。
- 评教读侧用例归位深化（统计：pageUnqualifiedUser 分页结果组装归位起步）：在 `EvaStatisticsQueryUseCase` 新增 `pageUnqualifiedUserAsPaginationQueryResult`，把“`PaginationResultEntity` → `PaginationQueryResultCO`”的分页结果组装逻辑先归位到用例层，为下一步旧入口 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 退化为纯委托壳做准备（保持行为不变；最小回归通过；落地提交：`e97615e1`）。
- 评教读侧用例归位深化（统计：evaScoreStatisticsInfo 空对象兜底重载归位起步）：在 `EvaStatisticsQueryUseCase` 新增 `evaScoreStatisticsInfoOrEmpty`，将 “`Optional.empty` → `new EvaScoreInfoCO()`” 的空对象兜底先归位到用例层（保持行为不变；最小回归通过；落地提交：`bce01df2`）。
- 评教读侧用例归位深化（统计：evaScoreStatisticsInfo 旧入口退化为委托壳）：将旧入口 `EvaStatisticsServiceImpl.evaScoreStatisticsInfo` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaScoreStatisticsInfoOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`1bf3a4fe`）。
- 评教读侧用例归位深化（统计：evaTemplateSituation 空对象兜底重载归位起步）：在 `EvaStatisticsQueryUseCase` 新增 `evaTemplateSituationOrEmpty`，将 “`Optional.empty` → `new EvaSituationCO()`” 的空对象兜底先归位到用例层（保持行为不变；最小回归通过；落地提交：`89b6b1ee`）。
- 评教读侧用例归位深化（统计：evaTemplateSituation 旧入口退化为委托壳）：将旧入口 `EvaStatisticsServiceImpl.evaTemplateSituation` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaTemplateSituationOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`78abf1a1`）。
- 评教读侧用例归位深化（统计：evaWeekAdd 空对象兜底重载归位起步）：在 `EvaStatisticsQueryUseCase` 新增 `evaWeekAddOrEmpty`，将 “`Optional.empty` → `new EvaWeekAddCO()`” 的空对象兜底先归位到用例层（保持行为不变；最小回归通过；落地提交：`5a8ac076`）。
- 评教读侧用例归位深化（统计：evaWeekAdd 旧入口退化为委托壳）：将旧入口 `EvaStatisticsServiceImpl.evaWeekAdd` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaWeekAddOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`2a92ca0b`）。
- 评教读侧用例归位深化（统计：getEvaData 空对象兜底重载归位起步）：在 `EvaStatisticsQueryUseCase` 新增 `getEvaDataOrEmpty`，将 “`Optional.empty` → `new PastTimeEvaDetailCO()`” 的空对象兜底先归位到用例层（保持行为不变；最小回归通过；落地提交：`1180a0f7`）。
- 评教读侧用例归位深化（统计：读测补齐—getEvaDataOrEmpty 空结果兜底）：为 `EvaStatisticsQueryUseCase.getEvaDataOrEmpty` 补齐 `Optional.empty` 时返回空对象的读侧用例级测试，并保留阈值读取顺序与委托顺序验证（保持行为不变；最小回归通过；落地提交：`a44a9bba`）。
- 评教读侧用例归位深化（统计：读测补齐—getTargetAmountUnqualifiedUserOrEmpty 非法 type）：为 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty(invalidType)` 补齐异常文案不变的读侧用例级测试，并确保不触发查询端口调用（保持行为不变；最小回归通过；落地提交：`0cb2caec`）。
- 评教读侧用例归位深化（统计：getEvaData 旧入口退化为委托壳）：将旧入口 `EvaStatisticsServiceImpl.getEvaData` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.getEvaDataOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`b59db93d`）。
- 评教读侧用例归位深化（统计：getTargetAmountUnqualifiedUser 空对象兜底重载归位起步）：在 `EvaStatisticsQueryUseCase` 新增 `getTargetAmountUnqualifiedUserOrEmpty`，将 “`Optional.empty` → `new UnqualifiedUserResultCO().setTotal(0).setDataArr(List.of())`” 的空对象兜底先归位到用例层（保持行为不变；最小回归通过；落地提交：`0ac65fb4`）。
- 评教读侧用例归位深化（统计：getTargetAmountUnqualifiedUser 旧入口退化为委托壳）：将旧入口 `EvaStatisticsServiceImpl.getTargetAmountUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`b931b247`）。
- 评教读侧用例归位深化（统计：读测补齐—getTargetAmountUnqualifiedUserOrEmpty type=1）：为 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty(type=1)` 补齐空结果兜底的读侧用例级测试（保持行为不变；最小回归通过；落地提交：`8f3e7afe`）。
- 条目 25（AI 报告写侧，组合根 wiring 归位）：将 `BcAiReportConfiguration` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package` 不变；Bean 定义与 `@Lazy` 环断策略不变；保持行为不变；最小回归通过；落地提交：`58c2f055`）。
- 条目 25（AI 报告写侧，`@CheckSemId` 注解下沉）：将 `edu.cuit.app.aop.CheckSemId` 从 `eva-app` 迁移到 `shared-kernel`（保持 `package` 不变；切面匹配表达式不变；保持行为不变；最小回归通过；落地提交：`1c595052`）。
- 条目 25（AI 报告写侧，旧入口归位）：将 `AiCourseAnalysisService` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package` 不变；保持 `@Service/@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`ca321a20`）。
- S0（`bc-ai-report` 试点，结构折叠，阶段 1）：将 `bc-ai-report` 折叠为 `bc-ai-report-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-ai-report`；保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e14f4f7a`）。
- S0（`bc-ai-report` 试点，结构折叠，阶段 2）：将端口适配器/导出实现/AI 基础设施归位 `bc-ai-report/infrastructure` 子模块，并补齐 `eva-app` → `bc-ai-report-infra` 依赖以保证装配（保持行为不变；最小回归通过；落地提交：`444c7aca`）。
- S0（`bc-audit` 试点，结构折叠，阶段 1）：将 `bc-audit` 折叠为 `bc-audit-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-audit`；保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`81594308`）。
- S0（`bc-audit` 试点，结构折叠，阶段 2）：将审计日志写链路的端口适配器 `edu.cuit.infra.bcaudit.adapter.LogInsertionPortImpl` 从 `eva-infra` 搬运到 `bc-audit/infrastructure` 子模块，并补齐 `eva-app` → `bc-audit-infra` 依赖以保证装配（保持行为不变；最小回归通过；落地提交：`d7858d7a`）。
- S0（`bc-audit` 试点，结构折叠，阶段 3，可选）：将 `sys_log` 相关 DAL（`SysLog*DO/Mapper/XML`）迁移到 `eva-infra-dal`，将 `LogConverter` 迁移到 `eva-infra-shared`，并将 `bc-audit-infra` Maven 依赖由 `eva-infra` 收敛为 `eva-infra-dal` + `eva-infra-shared`（保持包名/namespace/SQL 不变；保持行为不变；最小回归通过；落地提交：`06ec6f3d`）。
- S0（`bc-template` 试点，结构折叠）：将 `bc-template` 折叠为 `bc-template-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-template`；包名不变；保持行为不变；最小回归通过；落地提交：`65091516`）。
- S0（`bc-course` 试点，结构折叠）：将 `bc-course` 折叠为 `bc-course-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-course`；包名不变；保持行为不变；最小回归通过；落地提交：`e90ad03b`）。
- 条目 25（AI 报告写侧，导出链路实现归位）：将 `AiReportDocExportPortImpl` 与 `AiReportExporter` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`d1262c32`）。
- 条目 25（AI 报告写侧，analysis 链路实现归位）：将 `AiReportAnalysisPortImpl` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`6f34e894`）。
- 条目 25（AI 报告写侧，username → userId 链路实现归位）：将 `AiReportUserIdQueryPortImpl` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e2a608e2`）。
- 条目 25（AI 报告写侧，基础设施归位 + 依赖收敛）：将 `edu.cuit.infra.ai.*` 从 `eva-infra` 迁移到 `bc-ai-report`，并移除 `bc-ai-report` → `eva-infra` 编译期依赖（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e2f2a7ff`）。
- S0.1（状态复盘，保持行为不变）：全仓库 Maven 依赖面已不再引用 `eva-client`；root reactor 已移除 `eva-client` 模块；仓库中已移除 `eva-client/` 目录（需要回滚通过 Git 提交点即可）；`eva-domain` 中仍存在 `import edu.cuit.client.*`，但对应类型均已由 `shared-kernel` / 各 BC contract / `eva-domain` 自身承载（包名保持不变）。
- S0.1（收尾盘点，来源证伪；保持行为不变）：Serena 盘点 `eva-domain` 内所有 `import edu.cuit.client.*` 并逐项确认类型定义**不在** `eva-client`（其模块/目录已退出主干），而分别落在 `shared-kernel` / `bc-course` / `bc-evaluation/contract` / `bc-iam/contract` / `bc-messaging-contract` / `eva-domain`（包名保持不变）。
- S0.1（`eva-client` 退出 root reactor；保持行为不变）：选择方案 B：从 root `pom.xml` 的 `<modules>` 移除 `eva-client`（最小回归通过；落地提交：`ce07d75f`）。
- S0.1（更彻底清理；保持行为不变）：从仓库移除 `eva-client/` 目录（最小回归通过；落地提交：`de25e9fb`）。
- S0.1（通用对象沉淀 shared-kernel，单课次 CO）：将课程 CO `SingleCourseCO` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ccc82092`）。
- S0.1（通用对象沉淀 shared-kernel，课程时间段/类型）：将课程数据对象 `CoursePeriod/CourseType` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`5629bd2a`）。
- S0.1（消息协议继续拆 `eva-client`）：将消息入参 DTO `GenericRequestMsg` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`8fc7db99`）。
- S0.1（课程协议继续拆 `eva-client`）：将课程查询 Query 对象 `CourseQuery/CourseConditionalQuery/MobileCourseQuery` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`84a6a536`）。
- S0.1（课程协议继续拆 `eva-client`）：将通用学期入参 `Term` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`f401dcb9`）。
- S0.1（课程协议继续拆 `eva-client`）：将学期协议接口 `ISemesterService` 与学期 CO `SemesterCO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`7b5997c1`）。
- S0.1（收敛依赖：`eva-domain` 去 `eva-client` 直依赖）：在“可证实不再需要”的前提下移除 `eva-domain` → `eva-client` Maven **直依赖**（保持行为不变；最小回归通过；落地提交：`9ff21249`）。
- S0.1（课程协议继续拆 `eva-client`）：将课程写侧命令 `edu.cuit.client.dto.cmd.course/*` 与导入课表 BO `CourseExcelBO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变）；并为 `eva-infra-shared` 补齐 `bc-course` 显式依赖以闭合编译依赖（保持行为不变；最小回归通过；落地提交：`8a591703`）。
- S0.1（IAM 继续推进，去 `eva-client` 残留）：迁移 `IDepartmentService` 从 `eva-client` 到 `bc-iam-contract`（`edu.cuit.bc.iam.application.contract.api.department`），并更新 `DepartmentController/DepartmentServiceImpl` 引用（保持行为不变；最小回归通过；落地提交：`656dc36e`）。
- P1.2（评教域继续拆 `eva-client`，盘点）：Serena 盘点确认 `eva-client` 下评教专属目录（`api/eva`、`dto/cmd/eva`、`dto/clientobject/eva`）已迁空（盘点闭环）；评教 BC 内部引用的 `edu.cuit.client.*` 仅作为“协议包名”，其物理归属已在 `bc-evaluation-contract/shared-kernel`（保持行为不变；最小回归通过；落地提交：`e643bac9`）。
- P1.2-3（审计日志协议继续拆 `eva-client`，盘点）：Serena 盘点确认 `bc-audit` 对 `eva-client` 的实际依赖面仅剩 `SysLogBO`（`PagingQuery/GenericConditionalQuery/PaginationQueryResultCO` 已在 `shared-kernel`；`ILogService/OperateLogCO/LogModuleCO` 已在 `bc-audit`；保持行为不变；最小回归通过；落地提交：`ba21bbea`）。
- P1.2-3（审计日志协议继续拆 `eva-client`，小簇迁移）：迁移 `SysLogBO` 从 `eva-client` 到 `eva-domain`（保持 `package edu.cuit.client.bo` 不变；保持行为不变；最小回归通过；落地提交：`734a3741`）。
- P1.2-3（审计日志协议继续拆 `eva-client`，去直依赖）：移除 `bc-audit` → `eva-client` Maven 直依赖（保持行为不变；最小回归通过；落地提交：`2fcb257c`）。
- P1.2-2（审计日志协议继续拆 `eva-client`）：迁移 `ILogService/OperateLogCO/LogModuleCO` 从 `eva-client` 到 `bc-audit`（保持 `package` 不变；保持行为不变），并为 `bc-audit` 增加 `shared-kernel` 显式依赖以闭合编译依赖（最小回归通过；落地提交：`e1dbf2d4`）。
- S0.1-7（IAM application 去 `eva-client` 直依赖）：Serena 盘点 `bc-iam/application` 对 `edu.cuit.client.*` 的引用面后，移除 `bc-iam/application` → `eva-client` 的直依赖（保持行为不变；最小回归通过；落地提交：`7371ab96`）。
- P1.2-1（评教 application 去 `eva-client` 直依赖）：Serena 盘点 `bc-evaluation/application` 对 `edu.cuit.client.*` 的引用面后，移除 `bc-evaluation/application` → `eva-client` 的直依赖（保持行为不变；最小回归通过；落地提交：`10e8eb0b`）。
- S0.1-4（评教 contract 收敛依赖，继续推进）：迁移 `DateEvaNumCO/TimeEvaNumCO/MoreDateEvaNumCO/SimpleEvaPercentCO/SimplePercentCO/FormPropCO` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`c2d8a8b1`）。
- S0.1-5（评教 contract 收敛依赖，课程时间模型）：迁移 `dto/data/course/CourseTime` 到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`5f21b5ce`）。
- S0.1-6（评教 contract 去 `eva-client` 直依赖）：移除 `bc-evaluation-contract` → `eva-client` 的直依赖，并补齐 `bc-iam-contract` 的显式依赖（避免隐式经由其它模块传递；保持行为不变；最小回归通过；落地提交：`cf2001ef`）。

**已完成（2025-12-25）**
- 结构性里程碑 S0（`bc-iam` 试点，阶段 1）：将 `bc-iam` 折叠为顶层聚合模块 `bc-iam-parent`，并在其内部落地 `domain/application/infrastructure` 子模块；同时将历史平铺模块 `bc-iam-infra` 折叠归位到 `bc-iam/infrastructure`（artifactId/包名保持不变；最小回归通过；落地提交：`0b5c5383`）。
- 结构性里程碑 S0（`bc-evaluation` 试点）：将 `bc-evaluation` 折叠为顶层聚合模块 `bc-evaluation-parent`，并在其内部落地 `domain/application/infrastructure` 子模块；同时将历史平铺模块 `bc-evaluation-infra` 折叠归位到 `bc-evaluation/infrastructure`（artifactId/包名保持不变；最小回归通过；落地提交：`4db04d1c`）。
- 结构性里程碑 S0.1（拆解 `eva-client`，`bc-iam` 先行）：将 IAM 协议对象（`api/user/*` + `dto/cmd/user/*`）从 `eva-client` 迁移到 `bc-iam/contract`（包名归位到 `edu.cuit.bc.iam.application.contract...`；全仓库引用更新；最小回归通过；落地提交：`dc3727fa`）。
- 结构性里程碑 S0.1（拆解 `eva-client`，`bc-iam` 继续推进）：迁移 `dto/clientobject/user/*` 中与 IAM 直接相关的 8 个 CO（`UserInfoCO/UserDetailCO/RoleInfoCO/SimpleRoleInfoCO/MenuCO/GenericMenuSectionCO/RouterDetailCO/RouterMeta`）到 `bc-iam-contract`（包名归位到 `edu.cuit.bc.iam.application.contract.dto.clientobject.user`；全仓库引用更新；最小回归通过；落地提交：`c1a51199`）。
- P1（评教统计协议归属，继续拆 `eva-client`）：新增 `bc-evaluation-contract` 子模块，并迁移评教统计接口 `IEvaStatisticsService` + 未达标用户协议对象 `UnqualifiedUserInfoCO/UnqualifiedUserResultCO` 到 `bc-evaluation/contract`（保持 `package` 不变；最小回归通过；落地提交：`978e3535`）。
- P1.1（评教域继续拆 `eva-client`，协议对象归属）：迁移 `edu.cuit.client.dto.clientobject.eva.*` + `UnqualifiedUserConditionalQuery` 到 `bc-evaluation/contract`（保持 `package` 不变）；并将评教 API 接口（`IEvaConfigService/IEvaRecordService/IEvaTaskService/IEvaTemplateService/IUserEvaService`）从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变）；同时将课程域协议接口与 CO（`edu.cuit.client.api.course.*`、`CourseDetailCO/CourseModelCO/SingleCourseDetailCO`）从 `eva-client` 迁移到 `bc-course`，以避免 `eva-client` 反向依赖评教 CO（保持行为不变；最小回归通过；落地提交：`6eb0125d`）。
- S0.1（通用对象沉淀 shared-kernel，避免误迁）：用 Serena 盘点 `PagingQuery/GenericConditionalQuery/SimpleResultCO/PaginationQueryResultCO` 的跨 BC 引用范围，确认跨 BC 复用后，新增 `shared-kernel` 子模块并将上述通用类型（含 `ConditionalQuery`）从 `eva-client` 迁移到 `shared-kernel`（保持 `package edu.cuit.client.*` 不变；保持行为不变；最小回归通过；落地提交：`a25815b2`）。
- S0.1（收敛依赖，第一步）：`bc-iam-contract` / `bc-evaluation-contract` 已增加对 `shared-kernel` 的直依赖（暂保留 `eva-client` 以可回滚；最小回归通过；落地提交：`3a0ac086`）。
- S0.1（IAM 继续推进）：迁移 IAM 查询条件 `MenuConditionalQuery` 到 `bc-iam-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`1eda37c9`）。
- P1.2（评教域继续拆 `eva-client`）：迁移评教查询条件 `EvaTaskConditionalQuery/EvaLogConditionalQuery` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`d02d5522`）。
- S0.1（通用对象沉淀 shared-kernel，继续收敛）：迁移 `ValidStatus/ValidStatusValidator` 到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`686de369`）。
- S0.1（收敛依赖，第二步）：`bc-iam-contract` 已去除对 `eva-client` 的直依赖（保持行为不变；最小回归通过；落地提交：`8d673c17`）。
- S0.1-3（评教 contract 去依赖前置）：迁移评教 `dto/cmd/eva/*` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`2273ad61`）。
- S0.1-3（评教 contract 去依赖前置）：迁移 `EvaConfig` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`438d38bf`）。
  - 下一步（滚动计划）：逐步让各 BC 的 `contract` 在“可证实不再需要”的前提下分阶段削减对 `eva-client` 的依赖范围（建议拆分：先加直依赖，再去 `eva-client`；每步可回滚；保持行为不变）。
    - 评教域建议优先清单（当前主要剩余）：清理评教 BC 其它模块对 `eva-client` 的直依赖（例如 `bc-evaluation/application`；先用 Serena 盘点引用面，再逐步移除；保持行为不变）。

**已完成（2025-12-24）**
- 文档：需求变更（BC 模块组织方式）：BC 采用“单顶层聚合模块 + 内部 `domain/application/infrastructure` 子模块”组织方式，不再新增 `bc-*-infra` 平铺模块；历史平铺模块后续按里程碑折叠归位（仅文档口径调整；落地提交：`940b65ad`）。
- 提交点 B4（AI 报告写链路，analysis）：`AiCourseAnalysisService.analysis` 收敛为“用例 + 端口 + 端口适配器 + 旧入口委托壳”（保持 `@CheckSemId` 切面触发点不变；日志/异常文案不变；落地提交：`a8150e7f`）。
- 提交点 B5（AI 报告写链路，用户名解析）：`ExportAiReportDocByUsernameUseCase` 将 username → userId 查询抽为 `bc-ai-report` 端口 + `eva-app` 端口适配器（异常文案与日志顺序保持不变；落地提交：`d7df8657`）。
- 提交点 C-2-5（后续可选，读侧仓储瘦身）：使用 Serena 盘点评教读侧四主题仓储（`bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/bcevaluation/query/*QueryRepository.java`）的私有字段/私有方法/内部类引用，未发现“仅定义未被调用”的可删项；因此将 C-2 视为“已无进一步可证实无引用项”并关闭（保持行为不变；落地提交：`5c1a03bc`）。

**已完成（2025-12-23）**
- 条目 25 / 提交点 0：补齐“条目 25”的定义/边界与验收口径（只改文档，不改代码；落地提交：`1adc80bd`）。
- 条目 25 / 提交点 A：启动 `bc-ai-report` / `bc-audit` 最小 Maven 子模块骨架并接入组合根（仅落点与 wiring，不迁业务语义；落地提交：`a30a1ff9`）。
- 条目 25 / 提交点 B：审计日志写入 `LogGatewayImpl.insertLog` 收敛为“用例 + 端口 + 适配器 + 旧 gateway 委托壳”（保持行为不变；落地提交：`b0b72263`）。
- 提交点 B2（AI 报告写链路，导出）：`AiCourseAnalysisService.exportDocData` 收敛为“用例 + 端口 + 端口适配器 + 旧入口委托壳”（日志/异常文案不变；落地提交：`c68b3174`）。
- 提交点 B3（AI 报告写链路，导出）：`AiCourseAnalysisService.exportDocData` 进一步退化为“纯委托壳”（日志/异常文案不变；落地提交：`7f4b3358`）。
- 提交点 C5-1（读侧实现继续拆，统计主题）：新增 `EvaStatisticsQueryRepository` 承接 `EvaStatisticsQueryRepo` 实现，`EvaQueryRepository` 的统计方法退化为委托（口径/异常文案不变；落地提交：`9e0a8d28`）。
- 提交点 C5-2（读侧实现继续拆，记录主题）：新增 `EvaRecordQueryRepository` 承接 `EvaRecordQueryRepo` 实现，`EvaQueryRepository` 的记录方法退化为委托（口径/异常文案不变；落地提交：`985f7802`）。
- 提交点 C5-3（读侧实现继续拆，任务主题）：新增 `EvaTaskQueryRepository` 承接 `EvaTaskQueryRepo` 实现，`EvaQueryRepository` 的任务方法退化为委托（口径/异常文案不变；落地提交：`d467c65e`）。
- 提交点 C5-4（读侧实现继续拆，模板主题）：新增 `EvaTemplateQueryRepository` 承接 `EvaTemplateQueryRepo` 实现，`EvaQueryRepository` 的模板方法退化为委托（口径/异常文案不变；落地提交：`a550675a`）。
- 提交点 D1（`bc-evaluation-infra` 阶段 1）：引入 `bc-evaluation-infra` 并迁移评教读侧查询实现；同时将 course/eva DAL（DO/Mapper/XML）迁移到 `eva-infra-dal`、将 `CourseConvertor`/`EvaConvertor`/`EvaCacheConstants`/`CourseFormat` 迁移到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`be6dc05c`）。
- 提交点 D2（`bc-evaluation-infra` 阶段 2）：迁移评教写侧 Repo（`eva-infra/src/main/java/edu/cuit/infra/bcevaluation/repository/*`）到 `bc-evaluation-infra`；并将 `CalculateClassTime` 迁移到 `eva-infra-shared` 以保持 `bc-evaluation-infra` 不依赖 `eva-infra`（保持包名/行为不变；落地提交：`24e7f6c9`）。
- 提交点 C-1（后续可选，读侧门面加固）：清理 `EvaQueryRepository` 中已无引用的历史私有实现/冗余依赖，使其成为纯委托壳（保持口径/异常文案不变；落地提交：`73fc6c14`；三文档同步：`083b5807`）。
- 提交点 C-2-1（后续可选，读侧仓储瘦身）：清理 `EvaRecordQueryRepository` 无用 import（保持行为不变；落地提交：`e2a2a717`）。
- 提交点 C-2-2（后续可选，读侧仓储瘦身）：清理 `EvaRecordQueryRepository` 冗余通配 import（保持行为不变；落地提交：`8b76375f`）。
- 提交点 C-2-3（后续可选，读侧仓储瘦身）：清理 `EvaQueryRepo` 冗余 import（保持行为不变；落地提交：`4a317344`）。
- 提交点 C-2-4（后续可选，读侧仓储瘦身）：清理 `EvaStatisticsQueryRepo` 通配 import（保持行为不变；落地提交：`dba6e31d`）。
- 提交点 C（统计主题，第一步）：已从 `EvaQueryRepo` 抽出 `EvaStatisticsQueryRepo`，并将 `EvaStatisticsQueryPortImpl` 的依赖收敛到该接口（统计口径/异常文案不变；落地提交：`d5b07247`）。
- 提交点 C2（记录主题，第一步）：已从 `EvaQueryRepo` 抽出 `EvaRecordQueryRepo`，并将 `EvaRecordQueryPortImpl` 的依赖收敛到该接口（口径/异常文案不变；落地提交：`cae1a15c`）。
- 提交点 C3（任务主题，第一步）：已从 `EvaQueryRepo` 抽出 `EvaTaskQueryRepo`，并将 `EvaTaskQueryPortImpl` 的依赖收敛到该接口（口径/异常文案不变；落地提交：`82427967`）。
- 提交点 C4（模板主题，第一步）：已从 `EvaQueryRepo` 抽出 `EvaTemplateQueryRepo`，并将 `EvaTemplateQueryPortImpl` 的依赖收敛到该接口（口径/异常文案不变；落地提交：`889ec9b0`）。
- 文档：会话交接/计划三文档同步提交链（按发生顺序）：`bd9c6d7e/3d77f9e0/c0f7362b/61b0dfa4/68895003/ebff7002/4e52d74c/53832c45/c285701f/095979c8/a0e870f5/24e7f6c9/679076b7/73fc6c14/083b5807/73241fa2/3054dede/3bc127a5/965f551b`（之后以 `HEAD` 为准）。

**已完成（2025-12-22）**
- `bc-iam-infra` 阶段 2（IAM DAL 抽离 + shared 拆分 + 去依赖）已闭环完成（关键落地：`2ad911ea`；细节见 `NEXT_SESSION_HANDOFF.md`；保持行为不变）。
- `bc-iam-infra` 阶段 2（IAM DAL 抽离）：已将 `PaginationConverter` 迁移到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`54d5fecd`）。
- `bc-iam-infra` 阶段 2（IAM DAL 抽离）：已将 `MenuConvertor/RoleConverter/UserConverter` 迁移到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`6c798f1b`）。
- `bc-iam-infra` 阶段 2（IAM DAL 抽离）：已将 `edu.cuit.infra.dal.ldap.*`（DO/Repo）迁移到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`aca70b8b`）。
- `bc-iam-infra` 阶段 2（IAM DAL 抽离）：已将 `EvaLdapUtils/LdapConstants/EvaLdapProperties` 迁移到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`3165180c`）。
- `bc-iam-infra` 阶段 2（IAM DAL 抽离）：已将 `LdapUserConvertor` 迁移到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`0dc0ddc8`）。
- `bc-iam-infra` 阶段 2（IAM DAL 抽离）：已移除 `bc-iam-infra` → `eva-infra` 的 Maven 依赖（保持行为不变；落地提交：`2ad911ea`）。

**已完成（2025-12-21）**
- IAM 写侧继续收敛（保持行为不变）：
  - 用户删除：`UserUpdateGatewayImpl.deleteUser` 收敛到 `bc-iam`（落地提交：`5f08151c/e23c810a/cccd75a3/2846c689`）。
- 系统管理读侧渐进收敛（保持行为不变）：
  - 用户查询装配：`UserQueryGatewayImpl.fileUserEntity` 收敛到 `bc-iam`（落地提交：`3e6f2cb2/8c245098/92a9beb3`）。
- 系统管理写侧继续收敛（保持行为不变）：
  - 角色/菜单缓存与权限变更副作用收敛到 `bc-iam`：`RoleUpdateGatewayImpl.assignPerms/deleteMultipleRole`、`MenuUpdateGatewayImpl.handleUserMenuCache`（落地提交链：`f8838951/91d13db4/7fce88b8/d6e3bed1/4d650166/46e666f9`）。
  - 菜单写侧主链路收敛到 `bc-iam`：`MenuUpdateGatewayImpl.updateMenuInfo/deleteMenu/deleteMultipleMenu/createMenu`（落地提交：`f022c415`）。
  - 角色写侧剩余入口收敛到 `bc-iam`：`RoleUpdateGatewayImpl.updateRoleInfo/updateRoleStatus/deleteRole/createRole`（落地提交：`64fadb20`）。
- 中期里程碑推进（保持行为不变）：
  - 引入 `bc-iam-infra` Maven 子模块骨架并接入组合根，为后续迁移 `bciam/adapter/*` 提供落点（落地提交：`42a6f66f`）。
  - 迁移 `UserBasicQueryPortImpl` 到 `bc-iam-infra`（包名/类名/行为保持不变；落地提交：`070068ec`）。
  - 迁移 `RoleWritePortImpl` 到 `bc-iam-infra`（包名/类名/行为保持不变；落地提交：`03ceb685`）。
  - 迁移 `UserMenuCacheInvalidationPortImpl` 到 `bc-iam-infra`（包名/类名/行为保持不变；落地提交：`02b3e8aa`）。
  - 迁移 `MenuWritePortImpl` 到 `bc-iam-infra`（包名/类名/行为保持不变；落地提交：`6b9d2ce7`）。
  - 迁移用户写侧端口适配器到 `bc-iam-infra`：`UserCreationPortImpl/UserInfoUpdatePortImpl/UserStatusUpdatePortImpl/UserDeletionPortImpl`（落地提交：`5aecc747`）。
  - 迁移用户查询/分配端口适配器到 `bc-iam-infra`：`UserRoleAssignmentPortImpl/UserDirectoryQueryPortImpl/UserEntityQueryPortImpl`（落地提交：`1c3d4b8c`）。

**已完成（2025-12-20）**
- 评教写侧进一步收敛（保持行为不变）：
  - 评教模板新增/修改：收敛到 `bc-evaluation`（落地提交：`ea03dbd3`）。
  - 清理旧提交评教遗留实现：移除 `EvaUpdateGatewayImpl.putEvaTemplate`（落地提交：`12279f3f`）。
- 消息域写侧阶段性收敛（保持行为不变）：
  - 消息删除：收敛到 `bc-messaging`（落地提交：`22cb60eb`）。
  - 消息已读：收敛到 `bc-messaging`（落地提交：`dd7483aa`）。
- 消息域读侧进一步收敛（保持行为不变）：
  - 消息查询：`queryMsg/queryTargetAmountMsg` 收敛到 `bc-messaging`（落地提交：`05a2381b`）。
- 消息域写侧进一步收敛（保持行为不变）：
  - 消息插入：`insertMessage` 收敛到 `bc-messaging`（落地提交：`8445bc41`）。
- 消息域写侧进一步收敛（保持行为不变）：
  - 消息展示状态：`updateMsgDisplay` 收敛到 `bc-messaging`（落地提交：`c315fa22`）。
- 课程域查询/校验进一步收敛（保持行为不变）：
  - 课表导入状态查询：`CourseUpdateGatewayImpl.isImported` 收敛到 `bc-course`（落地提交：`4ed055a2/495287c8/f3e8e3cc`）。
- IAM 写侧继续收敛（保持行为不变）：
  - 用户创建：`UserUpdateGatewayImpl.createUser` 收敛到 `bc-iam`（落地提交：`c3aa8739/a3232b78/a26e01b3/9e7d46dd`）。
  - 用户信息更新：`UserUpdateGatewayImpl.updateInfo` 收敛到 `bc-iam`（落地提交：`38c31541/6ce61024/db0fd6a3/cb789e21`）。
  - 用户状态更新：`UserUpdateGatewayImpl.updateStatus` 收敛到 `bc-iam`（落地提交：`e3fcdbf0/8e82e01f/eb54e13e`；交接补充：`e4b94add`）。

**已完成（2025-12-19）**
- 评教写侧收敛（保持行为不变）：
  - 评教任务发布：`EvaUpdateGatewayImpl.postEvaTask` 收敛到 `bc-evaluation`（落地提交：`8e434fe1/ca69b131/e9043f96`）。
  - 评教删除：`EvaDeleteGatewayImpl.deleteEvaRecord/deleteEvaTemplate` 收敛到 `bc-evaluation`（落地提交：`ea928055/07b65663/05900142`）。
- 课程读侧收敛（保持行为不变）：
  - `CourseQueryGatewayImpl` 退化委托壳 + 抽取 `CourseQueryRepo/CourseQueryRepository`（抽取：`ba8f2003`；后续归位：`881e1d12`）。
- 评教读侧收敛（保持行为不变）：
  - `EvaQueryGatewayImpl` 退化委托壳 + 抽取 `EvaQueryRepo/EvaQueryRepository`（落地提交：`02f4167d`）。
  - 进一步拆分统计/任务/记录/模板查询端口，并让旧 `EvaQueryGatewayImpl` 委托到细分端口（落地提交：`bb391698/570fecb6/ec5191a3/3e5da427`）。
  - 旧 `EvaQueryGateway`/`EvaQueryGatewayImpl` 已移除（落地提交：`5c74083e`）。
- 测试稳定化（回归可重复执行）：
  - `start` 模块测试去除外部依赖（本地文件/Redis/LDAP/数据库），改为内存用例（落地提交：`daf343ef`）。

**已完成（2025-12-18）**
- 冲突校验收敛（保持行为不变）：
  - 教室占用冲突：提炼 `ClassroomOccupancyChecker` 并在“新增课次 / 新建课程明细 / 自助改课”等端口适配器复用（异常文案与 SQL 条件保持不变）。
  - 时间段重叠：提炼 `CourInfTimeOverlapQuery` 并迁移“改课 / 自助改课 / 分配评教”等端口适配器复用（异常文案与 SQL 条件保持不变）。
- 旧 gateway 退化加固：清理 `CourseUpdateGatewayImpl` 中已无引用的历史私有逻辑（防止“旧 gateway 继续承载业务流程”回潮）。
- 构建基线固化：补充 **Java 17** 为构建基线，并在离线模式下完成 `eva-infra` 的 `mvnd -o -pl eva-infra -am test` 验证。

---

### 4.3 未收敛清单（滚动）

> 说明：以下是仍在旧 gateway/技术切片中的能力，优先级按“写侧优先 + 影响范围”排序。

（新增，更新至 2026-02-04，保持行为不变）

- **S1（IAM：Controller 入口壳结构性收敛，保持行为不变）**：
  - ✅ 已完成：`UserUpdateController`（落地：`5ee37fd2`）、`DepartmentController`（落地：`fbc5fb74`）、`AuthenticationController`（落地：`fd9e4d1c`）、`MenuUpdateController`（落地：`44bc649d`）、`RoleUpdateController`（落地：`c81eb2e0`）。

- **IAM 并行（10.3：继续清理 `UserQueryGateway` 编译期依赖，保持行为不变）**：
  - ✅ 已完成：`bc-iam/infrastructure` 的 `UserServiceImpl` 已从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 最小 Port（并已同步测试过渡；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
  - ✅ 已完成（更新至 2026-02-04，保持行为不变）：`eva-infra-shared` 的 `StpInterfaceImpl` / `MsgBizConvertor` 已去 `UserQueryGateway` 编译期依赖并收敛为依赖 `bc-iam-contract` 最小 Port；端口适配器内部仍委托旧 `UserQueryGateway` 以保持缓存/切面触发点不变。可复现证伪口径：`rg -n "\\bUserQueryGateway\\b" eva-infra-shared/src/main/java` 应无命中（详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
  - ✅ 已完成（保持行为不变）：`bc-iam/application` 的 `FindUserByIdUseCase` 已去 `UserEntity` 编译期依赖并收敛为依赖 `bc-iam-contract` 端口 `UserEntityByIdQueryPort`（`execute` 使用泛型承接 `Optional<?>`；落地：`e13e1dc6`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
  - ✅ 已完成（保持行为不变）：`bc-iam/application` 的 `FindUserByUsernameUseCase` 已去 `UserEntity` 编译期依赖并收敛为依赖 `bc-iam-contract` 端口 `UserEntityByUsernameQueryPort`（`execute` 使用泛型承接 `Optional<?>`；落地：`e8f16843`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
  - ✅ 已完成（保持行为不变）：`bc-iam/application` 的 `PageUserUseCase` 已去 `UserEntity` 编译期依赖并收敛为依赖 `bc-iam-contract` 端口 `UserDirectoryPageQueryPort`（分页出参使用泛型承接 `PaginationResultEntity<?>`；落地：`4de644a7`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。
  - ✅ 已完成（保持行为不变）：`bc-iam/application` 的旧端口 `UserEntityQueryPort` 已去 `UserEntity` 编译期依赖（返回类型收敛为 `Optional<?>/PaginationResultEntity<?>`；过渡期实际返回仍为 `UserEntity`；端口适配器逻辑不变；落地：`72d029e0`；详见 `NEXT_SESSION_HANDOFF.md` 0.9）。

- **S0.2 延伸（IAM：依赖收敛 + 去 `eva-domain` 前置，保持行为不变）**：
  - ✅ 已完成（端口下沉，保持行为不变）：`EvaRecordCountQueryPort` 已从 `bc-evaluation/application` 下沉到 `bc-evaluation-contract`（保持 `package` 不变；落地：`4c30b02c`）。
  - ✅ 已完成（依赖收敛，保持行为不变）：`bc-iam/infrastructure/pom.xml` 已移除对 `bc-evaluation`（application jar）的编译期依赖，仅保留 `bc-evaluation-contract`（落地：`42a9e96c`）。
  - ✅ 已完成（编译闭合前置，保持行为不变）：`bc-iam/domain/pom.xml` 已补齐后续搬运归位所需的最小编译期依赖（`shared-kernel`、`cola-component-domain-starter`、`spring-context(provided)`、`lombok(provided)`），仅用于编译闭合，不引入新业务语义（落地：`a3d048d0`）。
  - ✅ 已完成（编译闭合前置，保持行为不变）：`bc-iam/domain/pom.xml` 已显式依赖 `bc-iam-contract`，用于为后续搬运“仅依赖 IAM cmd/CO”的 `edu.cuit.domain.gateway.user.*` 接口做编译闭合前置（落地：`2fc02fed`）。
  - ✅ 已完成（逐类搬运，保持行为不变）：`RoleUpdateGateway` 已从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变；落地：`95e37e8a`）。
  - ✅ 已完成（逐类搬运，保持行为不变）：`MenuUpdateGateway` 已从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变；落地：`c31a7a1e`）。
  - ✅ 已完成（逐类搬运，保持行为不变）：`DepartmentGateway` 已从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变；落地：`68128578`）。
  - ✅ 已完成（编译闭合前置，保持行为不变）：`eva-domain/pom.xml` 已显式依赖 `bc-iam-domain`（过渡期），用于为后续逐类搬运 `UserUpdateGateway/UserQueryGateway/MenuQueryGateway` 以及 `UserEntity/MenuEntity/RoleEntity` 等残留类型提供编译闭合支撑（落地：`43e8b66e`）。
  - ✅ 已完成（逐类搬运，保持行为不变）：`UserUpdateGateway` 已从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变；落地：`6630277b`）。
  - ✅ 已完成（逐类搬运，保持行为不变）：`MenuEntity` 已从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与类内容不变；落地：`6d700911`）。
  - ✅ 已完成（逐类搬运，保持行为不变）：`MenuQueryGateway` 已从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变；落地：`9982af0a`）。
  - ✅ 已完成（逐类搬运，保持行为不变）：`LdapPersonEntity` 已从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与类内容不变；落地：`eb36c6ce`）。
  - ✅ 已完成（编译闭合前置，保持行为不变）：`bc-iam/application/pom.xml` 已显式依赖 `bc-iam-domain`（暂不移除 `eva-domain`），用于为后续“逐个搬运 `edu.cuit.domain.*` 类型到 `bc-iam-domain`”做前置（落地：`aeaa8471`）。
  - ⏳ 未完成（核心阻塞，保持行为不变）：`bc-iam/application` 仍 `import edu.cuit.domain.*`，因此暂不能直接移除其 `pom.xml` 对 `eva-domain` 的编译期依赖；下一步按“每次只改 1 个类”的闭环节奏，先选择一个**引用面仅在 IAM** 的 `edu.cuit.domain.*` 类型并逐个归位到 `bc-iam-domain`（保持 `package` 不变），再在证伪引用面后收敛 `pom.xml` 依赖边界（详见 `DDD_REFACTOR_PLAN.md` 10.3 的 IAM 小节）。补充：更新至 2026-02-04，`UserQueryGateway` 已删除且 `bc-iam/infrastructure` 已清零对其编译期依赖；对 `RoleQueryGateway/RoleEntity` 的“跨 BC 复用”假设需以证据为准——截至 2026-02-04，Serena + `rg` 复核显示 `RoleQueryGateway` 的引用面仅在 `bc-iam/**`，因此它可以被纳入“单类搬运归位”的下一刀候选（仍需在执行前再次证伪；保持行为不变）。
  - 🎯 下一刀建议（保持行为不变；每次只改 1 个类闭环）：优先选择一个“仅需要基础用户查询（username/userId 等）”的依赖方（例如 `bc-ai-report/infrastructure/src/main/java/edu/cuit/app/bcaireport/adapter/AiReportUserIdQueryPortImpl.java`），将其对 `edu.cuit.domain.gateway.user.UserQueryGateway` 的依赖收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（只替换该类实际用到的方法；行为不变）。这样依赖方可通过 `bc-iam-contract` 获取同名端口类型，减少对 `eva-domain` 的编译期绑定，同时不提前触碰跨 BC 复用的 `UserQueryGateway/UserEntity` 归属设计。

- **S0.2 延伸（Evaluation：依赖方继续去 `UserEntity` 编译期依赖，保持行为不变）**：
  - ✅ 已完成：评教统计导出基类 `EvaStatisticsExporter` 已去 `UserEntity` 编译期依赖（见 `NEXT_SESSION_HANDOFF.md` 0.9；落地：`4f4b190b`）。
  - ✅ 已完成（前置）：`EvaConvertor` 已补齐桥接方法 `toEvaTaskEntityWithTeacherObject(...)`（仅类型桥接，不改 Supplier 调用时机/次数；落地：`a8934ab1`）。
  - ✅ 已完成（前置）：`UserConverter` 已补齐桥接方法 `toUserEntityObject(...)` + `userIdOf(Object)`（仅类型桥接，不改变 roles Supplier 调用时机/次数；落地：`c173c7c2`）。
  - ✅ 已完成（前置）：`CourseConvertor` 已补齐桥接方法 `toCourseEntityWithTeacherObject(...)`（仅类型桥接，不改变 teacher Supplier 调用时机/次数；落地：`858521da`）。
  - ✅ 已完成（主线，保持行为不变）：`EvaTaskQueryRepository` 已去 `UserEntity` 编译期依赖（落地：`7f198610`）。
  - ✅ 已完成（主线，保持行为不变）：`EvaRecordQueryRepository` 已去 `UserEntity` 编译期依赖（落地：`9cbcb858`）。
  - ✅ 证据化（可复现，更新至 2026-02-04，保持行为不变）：`bc-evaluation/infrastructure/src/main/java` 已清零 `UserEntity` import（口径：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-evaluation/infrastructure/src/main/java` 应命中为 0）。

- **S0.2 延伸（Messaging：依赖方继续去 `UserEntity` 编译期依赖，保持行为不变）**：
  - ✅ 已完成（前置，保持行为不变）：在 `eva-infra-shared` 的 `MsgConvertor` 增加桥接方法 `toMsgEntityWithUserObject(...)`（仅类型桥接，不改变 sender/recipient Supplier 调用时机与次数；落地：`8254a430`）。
  - ✅ 已完成（主线，保持行为不变）：`bc-messaging` 的 `MessageQueryPortImpl` 已去 `UserEntity` 编译期依赖：调用侧改走 `msgConvertor.toMsgEntityWithUserObject(...)` 并移除 `UserEntity` import（异常文案/`findById` 调用次数与顺序不变）；落地：`51301d23`。
    - 证据口径（可复现，更新至 2026-02-04）：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-messaging/src/main/java` 应命中为 0。

- **S0.2 延伸（Audit：依赖方继续去 `UserEntity` 编译期依赖，保持行为不变）**：
  - ✅ 已完成（前置，保持行为不变）：在 `eva-infra-shared` 的 `LogConverter` 增加桥接方法 `toLogEntityWithUserObject(...)`，用于后续让 `bc-audit` 的 `LogGatewayImpl` 去 `UserEntity` 编译期依赖（仅类型桥接，不改变 user 获取时机与次数；最小回归通过；落地：`8fa053ed`）。
  - ✅ 已完成（主线，保持行为不变）：`bc-audit` 的 `LogGatewayImpl` 已去 `UserEntity` 编译期依赖：调用侧改走 `userConverter.toUserEntityObject(...)` + `logConverter.toLogEntityWithUserObject(...)` 并移除 `UserEntity` import（异常文案/查询/遍历顺序不变）；落地：`a86f6520`。
    - 证据口径（可复现，更新至 2026-02-04）：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-audit/infrastructure/src/main/java` 应命中为 0。

- **S0.2 延伸（Course：依赖方继续去 `UserEntity` 编译期依赖，保持行为不变）**：
  - ✅ 已完成（更新至 2026-02-04，保持行为不变）：`bc-course/infrastructure` 的 `CourseQueryRepository` 已去 `UserEntity` 编译期依赖：
    - `SpringUtil.getBean(UserEntity.class)` + `setName` 改走 `UserConverter.springUserEntityWithNameObject(Object)`；
    - `UserEntity`/`Supplier<UserEntity>` 收敛为 `Object`/`Supplier<?>`，并改走 `UserConverter/CourseConvertor` 的桥接方法；
    - 证据口径：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-course/infrastructure/src/main/java` 应命中为 0。

- **S0.2 延伸（IAM：继续去 `UserEntity` 编译期依赖，保持行为不变）**：
  - ✅ 已完成（更新至 2026-02-04，保持行为不变）：`bc-iam/infrastructure` 已清零对旧 `UserEntity` 的编译期依赖：
    - 口径：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-iam/infrastructure/src/main/java --glob "*.java"`（应命中为 0）。
    - 证据化盘点（Serena，更新至 2026-02-04，保持行为不变）：原残留点为旧委托壳 `UserQueryGatewayImpl`（承载缓存/切面触发点），现已通过“收敛返回类型泛型 + 移除 import”完成去依赖（最小回归通过；落地：`8cba32b8`）：
      - 文件：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/UserQueryGatewayImpl.java`
      - 对外签名（保持行为不变；实现为委托到 bc-iam 用例）：
        - ✅ 带 `@LocalCached` 的缓存触发点（切面入口）：`findById(Integer): Optional<?>`、`findByUsername(String): Optional<?>`、`findAllUserId(): List<Integer>`、`findAllUsername(): List<String>`、`allUser(): List<SimpleResultCO>`、`getUserRoleIds(Integer): List<Integer>`
        - 其余非缓存方法：`findIdByUsername(String): Optional<Integer>`、`findUsernameById(Integer): Optional<String>`、`page(PagingQuery<GenericConditionalQuery>): PaginationResultEntity<?>`、`isUsernameExist(String): Boolean`、`getUserStatus(Integer): Optional<Integer>`
      - 当前“调用面/注入点”（保持行为不变）：端口适配器侧对旧 `UserQueryGateway` 的引用（import/字段注入）已清零；`UserQueryGatewayImpl` 已不再显式实现旧接口，但仍承载 `@LocalCached` 缓存/切面触发点（行为不变）。
      - 结论（保持行为不变）：本阶段已通过“收敛方法返回类型泛型为通配符”的方式完成 `bc-iam/infrastructure` 对 `UserEntity` 的编译期清零；后续若目标是“让 `eva-domain` 的 `UserEntity` 退出/迁移归属”，仍需另行拆步推进，且必须确保 `@LocalCached` 触发点与缓存命中/回源语义不漂移（高风险）。
      - ✅ 同步推进（保持行为不变）：已删除 `eva-domain` 中无引用的旧 `UserQueryGateway` 接口（此前已完成签名收敛；最小回归通过；落地：`93a7723d`）。
      - 可复现快照（兜底证据）：`rg -n "import\\s+edu\\.cuit\\.domain\\.gateway\\.user\\.UserQueryGateway;" bc-iam/infrastructure/src/main/java --glob "*.java"` 应命中为 0（端口适配器与旧 gateway 已不再显式引用旧接口）。
      - 前置铺垫（更新至 2026-02-04，保持行为不变）：已新增 `bc-iam/infrastructure` 内部过渡接口 `edu.cuit.infra.gateway.user.UserQueryCacheGateway`（返回 `Optional<?>/PaginationResultEntity<?>`），用于后续逐类将端口适配器从编译期依赖旧 `UserQueryGateway`（eva-domain）收敛为依赖内部接口，同时仍保证调用最终进入旧 `UserQueryGatewayImpl` 以触发 `@LocalCached` 缓存/切面入口；落地：`dc49f903`。
      - 前置铺垫（更新至 2026-02-04，保持行为不变）：已让旧 `UserQueryGatewayImpl` 同时实现 `UserQueryCacheGateway`，使后续端口适配器可把注入类型收敛为内部接口而不改变实际委托路径与缓存触发点（最小回归通过）；落地：`2970b80d`。
      - 推进（更新至 2026-02-04，保持行为不变）：`UserQueryGatewayImpl` 已不再显式实现旧 `UserQueryGateway`，仅保留实现内部接口 `UserQueryCacheGateway`；`@LocalCached` 触发点与方法体保持不变（最小回归通过；落地：`fb3b2e40`）。
      - 推进（更新至 2026-02-04，保持行为不变）：已将 `UserEntityByIdQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`（方法体仍委托 `findById`），确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过）；落地：`e854fcbe`。
      - 推进（更新至 2026-02-04，保持行为不变）：已将 `UserEntityByUsernameQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`（方法体仍委托 `findByUsername`），确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过）；落地：`ec31d96c`。
      - 推进（更新至 2026-02-04，保持行为不变）：已将 `UserAllUserIdQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`（方法体仍委托 `findAllUserId/findById`），确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过）；落地：`c0c05def`。
      - 推进（更新至 2026-02-04，保持行为不变）：已将 `UserNameQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`（方法体仍委托 `findById` 并通过强转读取 `name`），确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过）；落地：`1b91181f`。
      - 推进（更新至 2026-02-04，保持行为不变）：已将 `UserDetailQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`（方法体仍委托 `findById` 并通过强转读取 `id/username/name`），确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过）；落地：`5d85516b`。
      - ✅ 已完成（2026-02-04，保持行为不变）：已将 `UserDirectoryPageQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `page/allUser/findAllUsername`，确保调用最终进入旧 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过；落地：`d5335f4a`）。
      - ✅ 已完成（2026-02-04，保持行为不变）：已将 `UserEntityQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `getUserRoleIds`，确保调用最终进入旧 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过；落地：`b7373cbb`）。

- **S0.2 延伸（并行主线：依赖方 `pom.xml` 收敛，保持行为不变）**：
  - ✅ 已复核（更新至 2026-02-04，保持行为不变）：当前全仓库 `junit-jupiter(test)` 仅出现在以下模块，且均存在 `src/test/java` 与 `org.junit.jupiter` 引用，因此暂无“无测试源码模块”的单 `pom.xml` 清理目标：
    - `bc-messaging/pom.xml`
    - `bc-evaluation/application/pom.xml`
    - `bc-course/application/pom.xml`
    - `bc-template/application/pom.xml`
    - `bc-iam/application/pom.xml`
    - 可复现证据：`rg -n "<artifactId>junit-jupiter</artifactId>" --glob "**/pom.xml" .`
  - 后续若出现新候选（保持行为不变；每次只改 1 个 `pom.xml`）：移除前必须 Serena + `rg` 双证据，并跑最小回归闭环。推荐单模块证伪口径：`test -d <module>/src/test/java`（应为 false） + `rg -n "org\\.junit\\.jupiter" <module>/src`（应为空）。
  - ✅ 已完成（编译闭合前置，保持行为不变）：`bc-course/infrastructure/pom.xml` 已显式依赖 `bc-iam-contract`，用于为后续将 `bc-course-infrastructure` 的 `ICourseServiceImpl` 等从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort` 做前置（落地：`7b10d159`）。

- **S0.2 延伸（AI 报告 infra：依赖方 pom 收敛评估，保持行为不变）**：
  - ⏸️ 阻塞（更新至 2026-01-30，保持行为不变）：候选想把 `bc-ai-report/infrastructure/pom.xml` 中对 `bc-evaluation`（application jar）的编译期依赖收敛为仅依赖 `bc-evaluation-contract`；但 `AiReportAnalysisPortImpl` 仍依赖 `EvaRecordExportQueryPort`，而该端口当前仍定义于 `bc-evaluation/application`，并通过 `EvaRecordCourseQueryPort` 引入 `EvaRecordEntity` 等旧领域实体（`eva-domain`）。因此现阶段不建议直接做“单 pom 替换”，以免引入 `contract` 反向依赖或类型重复风险。可复现口径：`rg -n "EvaRecordExportQueryPort" bc-ai-report/infrastructure/src/main/java/edu/cuit/app/bcaireport/adapter/AiReportAnalysisPortImpl.java` 与 `rg -n "interface\\s+EvaRecordExportQueryPort\\b" bc-evaluation/application/src/main/java/edu/cuit/bc/evaluation/application/port/EvaRecordExportQueryPort.java`。

- **S0.2 延伸（审计域：收敛 `eva-app` 对 `bc-audit` 的编译期耦合面，保持行为不变）**：
  - ✅ 已完成：将 `eva-app/src/main/java/edu/cuit/app/config/BcAuditConfiguration.java` 归位到 `bc-audit-infra`（保持 `package edu.cuit.app.config` 不变；Bean 装配/副作用顺序不变；落地：`5a4d726b`）。
  - ✅ 已完成（编译闭合前置，保持行为不变）：在 `bc-audit/infrastructure/pom.xml` 补齐 `zym-spring-boot-starter-logging`，用于为后续归位 `LogBizConvertor/LogServiceImpl`（依赖 `LogManager/OperateLogBO`）做编译闭合前置（运行时 classpath 已存在，仅显式化；落地：`e7e13736`）。
  - ✅ 已完成（支撑类前置，保持行为不变）：将 `LogBizConvertor` 从 `eva-app` 归位到 `bc-audit-infra`（保持 `package edu.cuit.app.convertor` 不变；落地：`99960c7f`）。
  - ✅ 已完成：将 `eva-app/src/main/java/edu/cuit/app/service/impl/LogServiceImpl.java` 归位到 `bc-audit-infra`（保持 `package` 不变；异常文案/日志与副作用顺序不变；落地：`d0af2bac`）。
  - 验收口径：`rg -n '^import\\s+edu\\.cuit\\.bc\\.audit' eva-app/src/main/java` 命中为 0 后，再评估是否可收敛 `eva-app/pom.xml` 的 `bc-audit`（以及 `bc-audit-infra`）编译期依赖（每次只改 1 个 `pom.xml`；保持行为不变）。若运行时装配可能受影响，先在 `start/pom.xml` 显式增加 `bc-audit-infra(runtime)` 兜底（每次只改 1 个 pom）。

- **S0.2 延伸（课程域：继续削减 `eva-app` 的课程旧入口实现承载面）**：
  - ✅ 已完成：课程相关 Controller 注入已从 `*ServiceImpl` 收窄为 `shared-kernel` 下的 `edu.cuit.client.api.course.*Service` 接口（避免 Controller 编译期绑定实现类；保持行为不变；落地以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  - ✅ 已完成：`ICourseServiceImpl` 已从 `eva-app` 归位到 `bc-course-infra`（保持 `package` 不变，仅搬运与编译闭合；保持行为不变；落地以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  - ✅ 已完成（依赖收敛，保持行为不变）：`bc-course/infrastructure` 的 `ICourseServiceImpl` 已将对 `edu.cuit.domain.gateway.user.UserQueryGateway` 的编译期依赖收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（最小回归通过；落地：`24f141a1`）。
  - ✅ 已完成：`IUserCourseServiceImpl` 已从 `eva-app` 归位到 `bc-course-infra`（保持 `package` 不变，仅搬运与编译闭合；保持行为不变；落地以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  - ✅ 已完成：`ICourseDetailServiceImpl` 已从 `eva-app` 归位到 `bc-course-infra`（保持 `package` 不变，仅搬运与编译闭合；保持行为不变；落地以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  - ✅ 已完成（依赖收敛，保持行为不变）：`bc-course/infrastructure` 的 `ICourseDetailServiceImpl` 已将对 `edu.cuit.domain.gateway.user.UserQueryGateway` 的编译期依赖收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（最小回归通过；落地：`d6c1d692`）。
  - ✅ 已完成（依赖收敛，保持行为不变）：`bc-course/infrastructure` 的 `IUserCourseServiceImpl` 已将对 `edu.cuit.domain.gateway.user.UserQueryGateway` 的编译期依赖收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（最小回归通过；落地：`4dca490e`）。
  - ✅ 已完成：`ClassroomServiceImpl` 已从 `eva-app` 归位到 `bc-course-infra`（保持 `package` 不变，仅搬运与编译闭合；保持行为不变；落地以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  - ✅ 已完成：`ICourseTypeServiceImpl` 已从 `eva-app` 归位到 `bc-course-infra`（保持 `package` 不变，仅搬运与编译闭合；保持行为不变；落地以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  - ✅ 已完成：收敛 `eva-app/pom.xml`：移除对 `bc-course` 的编译期依赖（`shared-kernel` 显式依赖保留；保持行为不变；最小回归通过；落地：`dca806fa`）。

0) **S0.2（已闭环，后续延伸，保持行为不变）：收敛 `eva-domain` 对 `bc-course` 的编译期依赖面**
   - 背景：`eva-domain/pom.xml` **此前**依赖 `bc-course`（应用层 jar）。核心原因是 `eva-domain` 引用一批 `edu.cuit.client.*` 协议对象，而这些类型的定义文件当时仍落在 `bc-course/application`（Serena 证据化盘点）。
	   - ✅ 进展（2026-01-06）：Serena 证伪后已移除 `eva-domain/pom.xml` 对 `bc-course` 的 Maven 依赖，改为显式依赖 `shared-kernel`（保持行为不变；最小回归通过；落地：`01b36508`）。
	   - ✅ 补充进展（2026-01-06）：`IUserCourseService` 已迁移至 `shared-kernel`，从而使 `bc-ai-report-infra` 不再需要显式依赖 `bc-course`（保持行为不变；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
   - 证据（示例，均保持 `package` 不变）：
     - ✅ `edu.cuit.client.dto.clientobject.SemesterCO`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/clientobject/SemesterCO.java`（落地：`77126c4a`）
     - ✅ `edu.cuit.client.dto.data.Term`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/data/Term.java`（落地：`23bff82f`）
     - ✅ `edu.cuit.client.api.course.IUserCourseService`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/api/course/IUserCourseService.java`（落地：`e2a697f1`）
     - ✅ `edu.cuit.client.dto.clientobject.SimpleSubjectResultCO`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/clientobject/SimpleSubjectResultCO.java`（落地：`e2a697f1`）
     - ✅ `edu.cuit.client.dto.query.CourseQuery`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/query/CourseQuery.java`（落地：`e479ce0e`）
     - ✅ `edu.cuit.client.dto.query.condition.CourseConditionalQuery`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/query/condition/CourseConditionalQuery.java`（落地：`e479ce0e`）
     - ✅ `edu.cuit.client.dto.query.condition.MobileCourseQuery`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/query/condition/MobileCourseQuery.java`（落地：`e479ce0e`）
     - ✅ `edu.cuit.client.bo.CourseExcelBO`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/bo/CourseExcelBO.java`（落地：`1f47a032`）
     - ✅ `edu.cuit.client.dto.cmd.course`（全部 8 个 Cmd）：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/cmd/course/`（落地：`0978b3cb`、`0d18e4ad`）
     - ✅ `edu.cuit.client.dto.clientobject.course` 子簇（`SubjectCO/SelfTeachCourseCO/SelfTeachCourseTimeCO/SelfTeachCourseTimeInfoCO`）：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/clientobject/course/`（落地：`87d8c692`）
     - ✅ `edu.cuit.client.dto.clientobject.course.RecommendCourseCO`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/clientobject/course/RecommendCourseCO.java`（落地：`24595a53`）
     - ✅ `edu.cuit.client.dto.clientobject.eva.EvaTemplateCO`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/clientobject/eva/EvaTemplateCO.java`（落地：`34579fe0`）
     - ✅ `edu.cuit.client.dto.clientobject.course.CourseDetailCO`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/clientobject/course/CourseDetailCO.java`（落地：`4dbcb2de`）
     - ✅ `edu.cuit.client.dto.clientobject.course.CourseModelCO`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/clientobject/course/CourseModelCO.java`（落地：`4dbcb2de`）
     - ✅ `edu.cuit.client.dto.clientobject.course.TeacherInfoCO`：已迁移至 `shared-kernel/src/main/java/edu/cuit/client/dto/clientobject/course/TeacherInfoCO.java`（落地：`4dbcb2de`）
     - （以及其它 `edu.cuit.client.dto.clientobject.course/*`，此处不逐项展开，避免文档噪声）
		    - 计划（每步只迁 1 个小包/小类簇；每步闭环=Serena→最小回归→提交→三文档同步；保持行为不变）：
		      1) ✅ 已完成：先把以上“阻塞 `eva-domain` 去 `bc-course` 依赖”的协议对象按小簇迁移到 `shared-kernel`（优先保持 `package` 不变以降风险）。
		      2) ✅ 已完成：Serena 证伪 `eva-domain` 不再引用“仅由 `bc-course` 提供的 `edu.cuit.client.*` 类型”后，独立提交移除 `eva-domain/pom.xml` 对 `bc-course` 的依赖（保持行为不变）。
		      3) ✅ 已完成（S0.2 延伸，保持行为不变）：继续收敛 `bc-course/application` 的“协议承载面”，将课程域接口/CO（`edu.cuit.client.api.course` 下残留接口 `ICourseDetailService/ICourseService/ICourseTypeService` 及其签名依赖的跨 BC DTO，如 `CourseScoreCO/EvaTeacherInfoCO/SingleCourseDetailCO/SimpleCourseResultCO` 等）逐步下沉到 `shared-kernel`（均保持 `package` 不变），并移除 `bc-course/application` 对 `bc-evaluation-contract` 的编译期依赖，避免跨 BC 编译期耦合与 Maven 环依赖（细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
		      4) ⏳ 后续延伸（保持行为不变）：在课程域协议已进入 `shared-kernel` 的前提下，逐个模块收敛对 `bc-course` 的编译期依赖（每次只改 1 个 `pom.xml`）：先用 Serena 证伪该模块是否引用 `bc-course` 内部实现类；若仅使用 `edu.cuit.client.*` 类型，则将其对 `bc-course` 的编译期依赖替换为显式依赖 `shared-kernel`（每步闭环：Serena → 最小回归 → commit → 三文档同步）。
		      5) ✅ 已完成（课程域基础设施归位，保持行为不变）：已将 `eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 全部归位到 `bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/`（每次只迁 1 个类/小簇；允许“仅补齐编译期依赖”以闭合构建，不改业务语义）。已推进：`AddCourseTypeRepositoryImpl`（`8426d4f2`）、`UpdateCoursesTypeRepositoryImpl`（`12d16c6a`）、`UpdateCourseInfoRepositoryImpl`（`eb940498`）、`DeleteSelfCourseRepositoryImpl`（`73ed7c7d`）、`UpdateSingleCourseRepositoryImpl`（`1a01e827`）、`UpdateSelfCourseRepositoryImpl`（`3d1dd4f1`）、`AddExistCoursesDetailsRepositoryImpl`（`bd042ea9`）、`AddNotExistCoursesDetailsRepositoryImpl`（`bd042ea9`）、`DeleteCourseRepositoryImpl`（`df4ac6ca`）、`DeleteCoursesRepositoryImpl`（`df4ac6ca`）、`DeleteCourseTypeRepositoryImpl`（`33844ce0`）、`UpdateCourseTypeRepositoryImpl`（`33844ce0`）、`ChangeCourseTemplateRepositoryImpl`（`33032890`）、`ImportCourseFileRepositoryImpl`（`33032890`）、`AssignEvaTeachersRepositoryImpl`（`7f5beed9`）已归位；当前 `eva-infra` 的该目录残留已清零（以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。

1) AI 报告 / 审计日志（条目 25）：已完成提交点 A（模块骨架 + 组合根 wiring；落地提交：`a30a1ff9`），已完成提交点 B（审计日志写入 `LogGatewayImpl.insertLog`；落地提交：`b0b72263`）  
   - 补充进展（条目 25 之外）：已完成提交点 B2（AI 报告导出链路收敛；落地提交：`c68b3174`）；已完成提交点 B3（旧入口进一步退化为纯委托壳；落地提交：`7f4b3358`）。
   - 补充进展（条目 25 之外）：已将导出链路实现（`AiReportDocExportPortImpl` + `AiReportExporter`）从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；落地提交：`d1262c32`）。
   - 补充进展（条目 25 之外）：已将 analysis 端口适配器 `AiReportAnalysisPortImpl` 从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；落地提交：`6f34e894`）。
   - 补充进展（条目 25 之外）：已将 username → userId 查询端口适配器 `AiReportUserIdQueryPortImpl` 从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；落地提交：`e2a608e2`）。
   - 补充进展（条目 25 之外）：已将 `edu.cuit.infra.ai.*` 从 `eva-infra` 归位到 `bc-ai-report`，并移除 `bc-ai-report` → `eva-infra` 编译期依赖（保持 `package` 不变；保持行为不变；落地提交：`e2f2a7ff`）。
   - 补充进展（S0，2025-12-27）：`bc-ai-report` 已完成结构折叠阶段 1/2：引入 `bc-ai-report-parent` + 内部子模块；端口适配器/导出实现/AI 基础设施已归位 `bc-ai-report/infrastructure`，并补齐 `eva-app` → `bc-ai-report-infra` 依赖（保持行为不变；落地提交：`e14f4f7a`、`444c7aca`）。
   - 补充进展（S0，2025-12-27）：`bc-audit` 已完成结构折叠阶段 1/2：引入 `bc-audit-parent` + 内部子模块；并将 `LogInsertionPortImpl` 归位 `bc-audit/infrastructure`，补齐 `eva-app` → `bc-audit-infra` 依赖（保持行为不变；落地提交：`81594308`、`d7858d7a`）。
   - ✅ 进展（2025-12-27）：已完成 `sys_log` 相关 DAL/Converter 抽离，并移除 `bc-audit-infra` → `eva-infra` 过渡依赖（保持行为不变；落地提交：`06ec6f3d`）。
   - 边界：条目 25 = 提交点 A + 提交点 B；不包含提交点 C（读侧 `EvaQueryRepo` 拆分）。
   - 验收：缓存/日志/异常文案/副作用顺序完全不变 + 最小回归通过（以 `NEXT_SESSION_HANDOFF.md` 为准）。
2) 读侧：`EvaQueryRepository` 的实现侧已按主题拆分并退化为委托壳，且读侧查询实现已迁移到 `bc-evaluation-infra`（保持口径/异常文案不变；接口拆分：`d5b07247/cae1a15c/82427967/889ec9b0`；实现拆分：`9e0a8d28/985f7802/d467c65e/a550675a`；迁移落地：`be6dc05c`），并已完成读侧门面加固（清理 `EvaQueryRepository` 为纯委托壳；落地：`73fc6c14`；三文档同步：`083b5807`）；C-2（读侧仓储瘦身）已完成盘点并关闭（未发现可证实无引用项；落地：`5c1a03bc`）。
	   - 下一步建议（仍保持行为不变）：按用例维度继续细化 QueryService/QueryPort，优先从 `EvaStatisticsQueryPort` 这类“方法簇较大”的端口开始（先新增子 Port + `extends`，不改实现/不改装配；再逐步收窄上层依赖类型，避免一次性大改）；并继续推进统计读侧用例归位深化：✅ `getEvaData` 的阈值计算/参数组装已归位 `EvaStatisticsQueryUseCase`（落地：`8f4c07c5`）；✅ 已补齐 unqualifiedUser 的“参数组装重载”（读取 `EvaConfigGateway.getEvaConfig()`；落地：`0a2fec4d`）；✅ 旧入口 `EvaStatisticsServiceImpl` 已委托 UseCase 重载并去除对 `EvaConfigGateway` 的直接依赖（落地：`21f6ad5b`）。方向 B 的下一步建议（每次只迁 1 个方法；旧入口仍保留 `@CheckSemId` 触发点不变）：✅ `evaScoreStatisticsInfo` 空对象兜底归位（补齐重载：`bce01df2`；旧入口委托：`1bf3a4fe`）→ ✅ `evaTemplateSituation` 空对象兜底归位（补齐重载：`89b6b1ee`；旧入口委托：`78abf1a1`）→ ✅ `evaWeekAdd` 空对象兜底归位（补齐重载：`5a8ac076`；旧入口委托：`2a92ca0b`）→ 下一步选择统计读侧下一簇继续归位（仍按“每次只迁 1 个方法簇”的节奏）。
   - ✅ 进展（2025-12-27）：已将 `EvaStatisticsQueryPort` 细分为 `EvaStatisticsOverviewQueryPort/EvaStatisticsTrendQueryPort/EvaStatisticsUnqualifiedUserQueryPort`，并让 `EvaStatisticsQueryPort` `extends` 以上子端口（仅接口拆分，不改实现/不改装配；保持行为不变；最小回归通过；落地：`a1d6ccab`）。
   - ✅ 进展（2025-12-28）：已将 `EvaStatisticsServiceImpl` 对统计端口的依赖类型收窄为三个子端口（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`c19d8801`）。
	   - ✅ 进展（2025-12-28）：已将统计导出侧 `EvaStatisticsExporter` 静态初始化中获取的统计端口由 `EvaStatisticsQueryPort` 收窄为 `EvaStatisticsOverviewQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；保持行为不变；最小回归通过；落地：`9b3c4e6a`）。
	   - ✅ 进展（2025-12-28）：已在 `bc-evaluation` 应用层新增统计读侧用例 `EvaStatisticsQueryUseCase`（当前为委托壳），并在 `BcEvaluationConfiguration` 完成装配；`EvaStatisticsServiceImpl` 改为委托该用例（保持行为不变；最小回归通过；落地：`db09d87b`）。
	   - ✅ 进展（2025-12-28）：记录读侧 QueryPort 细分起步：新增得分子端口 `EvaRecordScoreQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`4e47ffe3`）。
	   - ✅ 进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordScoreQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`0c7e7d13`）。
	   - ✅ 进展（2025-12-28）：记录读侧 QueryPort 细分：新增分页子端口 `EvaRecordPagingQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`e4f0efe9`）。
	   - ✅ 进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordPagingQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`76976c0b`）。
	   - ✅ 进展（2025-12-28）：记录读侧依赖类型收窄：`EvaRecordServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordPagingQueryPort/EvaRecordScoreQueryPort`（不改业务逻辑/异常文案；最小回归通过；落地：`39a4bafe`）。
	   - ✅ 进展（2025-12-28）：记录读侧 QueryPort 细分：新增用户日志子端口 `EvaRecordUserLogQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`fcac9324`）。
	   - ✅ 进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordUserLogQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`1e025e48`）。
	   - ✅ 进展（2025-12-28）：记录读侧 QueryPort 细分：新增按课程查询子端口 `EvaRecordCourseQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`e9034541`）。
	   - ✅ 进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordCourseQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`b8efeaf5`）。
	   - ✅ 进展（2025-12-28）：记录读侧 QueryPort 细分：新增数量统计子端口 `EvaRecordCountQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`db876379`）。
			   - ✅ 进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordCountQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`0d562206`）。
			   - ✅ 进展（2025-12-28）：记录读侧依赖类型收窄：`UserServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordCountQueryPort`（不改业务逻辑/异常文案；补充单测；最小回归通过；落地：`8b24d2f8`）。
			   - ✅ 进展（2025-12-28）：记录读侧依赖类型收窄：`MsgServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordCountQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`147d486b`）。
			   - ✅ 进展（2025-12-28）：记录读侧依赖类型收窄：`UserEvaServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordUserLogQueryPort/EvaRecordScoreQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`80886841`）。
			   - ✅ 进展（2025-12-28）：任务读侧 QueryPort 细分起步：新增单任务信息子端口 `EvaTaskInfoQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`26b79c3a`）。
			   - ✅ 进展（2025-12-28）：任务读侧聚合端口继承子端口：让 `EvaTaskQueryPort` `extends EvaTaskInfoQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`56834293`）。
			   - ✅ 进展（2025-12-28）：任务读侧依赖类型收窄：`MsgServiceImpl` 由注入 `EvaTaskQueryPort` 收窄为 `EvaTaskInfoQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`7aa49e7f`）。
			   - ✅ 进展（2025-12-28）：任务读侧 QueryPort 细分起步：新增分页子端口 `EvaTaskPagingQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`f0a172d1`）。
			   - ✅ 进展（2025-12-28）：任务读侧聚合端口继承子端口：让 `EvaTaskQueryPort` `extends EvaTaskPagingQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`2fd9d24e`）。
			   - ✅ 进展（2025-12-28）：任务读侧 QueryPort 细分：新增本人任务/数量统计子端口 `EvaTaskSelfQueryPort/EvaTaskCountQueryPort`，并让 `EvaTaskQueryPort` `extends` 这些子端口（仅新增接口+继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`9d5064fc`）。
			   - ✅ 进展（2025-12-28）：任务读侧依赖类型收窄：`EvaTaskServiceImpl` 由注入 `EvaTaskQueryPort` 收窄为 `EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskInfoQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`4b22f059`）。
			   - ✅ 进展（2025-12-28）：模板读侧 QueryPort 细分起步：新增分页/全量/按任务取模板子端口 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`，并让 `EvaTemplateQueryPort` `extends` 这些子端口（仅新增接口+继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`a14d3c53`）。
			   - ✅ 进展（2025-12-28）：模板读侧依赖类型收窄：`EvaTemplateServiceImpl` 由注入 `EvaTemplateQueryPort` 收窄为 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`b86db7e4`）。
			   - ✅ 进展（2025-12-28）：模板读侧引用面盘点结论/证伪：使用 Serena 盘点 `EvaTemplateQueryPort` 在全仓库的引用面，除端口定义外仅剩实现侧 `EvaTemplateQueryPortImpl`；应用层未发现其它对聚合端口的注入点/调用点，因此模板主题的“端口细分 + 服务层依赖类型收窄”阶段可视为已闭合（保持行为不变；证据记录见 `NEXT_SESSION_HANDOFF.md` 0.9）。
				   - ✅ 进展（2025-12-28）：统计读侧用例归位深化：将 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 的 `type` 分支选择归位到 `EvaStatisticsQueryUseCase.pageUnqualifiedUser`（`@CheckSemId` 触发点仍保留在旧入口；异常文案不变；保持行为不变；最小回归通过；落地：`22dccc70`）。
				   - ✅ 进展（2025-12-28）：统计读侧用例归位深化：将 `EvaStatisticsServiceImpl.getTargetAmountUnqualifiedUser` 的 `type` 分支选择与阈值选择归位到 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser`（`@CheckSemId` 触发点仍保留在旧入口；异常文案不变；保持行为不变；最小回归通过；落地：`5b20d44e`）。
				   - ✅ 进展（2025-12-28）：统计读侧用例归位深化：将 `EvaStatisticsServiceImpl.getEvaData` 的阈值读取与参数组装归位到 `EvaStatisticsQueryUseCase.getEvaData(semId, num)`（保持阈值读取顺序不变；`@CheckSemId` 触发点仍在旧入口；保持行为不变；最小回归通过；落地：`8f4c07c5`）。
				   - ✅ 进展（2025-12-28）：统计读侧用例归位深化：在 `EvaStatisticsQueryUseCase` 新增 unqualifiedUser 的“参数组装重载”（`pageUnqualifiedUser(semId, type, query)` / `getTargetAmountUnqualifiedUser(semId, type, num, error)`），内部统一读取 `EvaConfigGateway.getEvaConfig()` 并委托既有实现（保持行为不变；最小回归通过；落地：`0a2fec4d`）。
				   - ✅ 进展（2025-12-28）：统计读侧用例归位深化：旧入口 `EvaStatisticsServiceImpl.pageUnqualifiedUser/getTargetAmountUnqualifiedUser` 已委托 UseCase 重载并移除对 `EvaConfigGateway` 的直接依赖（保持 `@CheckSemId` 触发点与异常文案/副作用顺序不变；最小回归通过；落地：`21f6ad5b`）。
			   - ✅ 进展（2025-12-28）：记录导出链路子端口补齐：新增 `EvaRecordExportQueryPort`（组合 `EvaRecordCourseQueryPort/EvaRecordScoreQueryPort`），并让聚合端口 `EvaRecordQueryPort` `extends` 该子端口（仅新增接口+继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`5df35c36`）。
			   - ✅ 进展（2025-12-28）：记录导出链路依赖类型收窄：将导出基类 `EvaStatisticsExporter` 静态初始化中获取记录端口的依赖类型从 `EvaRecordQueryPort` 收窄为 `EvaRecordExportQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；保持行为不变；最小回归通过；落地：`682bf081`）。
			   - 下一步建议（方向 A → B，保持行为不变）：将“统计”这套模式复制到记录/任务/模板（先细分子 QueryPort，再逐个收窄 `eva-app` 依赖类型）；并逐步把统计用例编排归位到 `EvaStatisticsQueryUseCase`（每次只迁 1 个方法簇）。
				     - 下一步建议（记录主题，依赖收窄优先级）：✅ `MsgServiceImpl`（已收窄依赖：`EvaRecordCountQueryPort`）→ ✅ `UserEvaServiceImpl`（已收窄依赖：`EvaRecordUserLogQueryPort/EvaRecordScoreQueryPort`）→ ✅ `AiReportAnalysisPortImpl`（已收窄依赖：`EvaRecordExportQueryPort`）→ 视测试可控性再考虑导出链路其它装饰器/扩展点（若涉及 `StpUtil` 静态登录态，单测需提前规划“可重复”的登录态注入策略）。
			     - 下一步建议（任务主题，依赖收窄优先级，保持行为不变）：✅ `MsgServiceImpl` 与 ✅ `EvaTaskServiceImpl` 的任务端口依赖类型已完成收窄；（模板主题）✅ 子 QueryPort 与 ✅ `EvaTemplateServiceImpl` 依赖类型收窄已完成。下一步建议：如需继续推进模板读侧解耦，可盘点是否存在其它引用 `EvaTemplateQueryPort` 的应用层类并逐一收窄（每次只改 1 个类 + 最小回归/可运行单测）。

3) 写侧（bc-course，方向 A → B，保持行为不变）：`eva-app` 仍存在多处 `@CheckSemId` 写侧入口（需继续按“每次只迁 1 个入口方法簇”推进）。
   - 入口清单（Serena 盘点，2026-01-02）：
     - `edu.cuit.app.service.impl.course.ICourseServiceImpl`：写侧已闭环 `updateSingleCourse/allocateTeacher/deleteCourses/addNotExistCoursesDetails/addExistCoursesDetails`。
   - `edu.cuit.app.service.impl.course.IUserCourseServiceImpl`：写侧已闭环（`deleteSelfCourse/updateSelfCourse` 已入口用例归位；`importCourse` 已完成 `importCourseFile(...)` 调用点端口化）。
     - `edu.cuit.app.service.impl.course.ICourseDetailServiceImpl`：写侧入口 `updateCourse/updateCourses/delete/addCourse` 已完成调用点端口化/入口用例归位；该类的写侧簇可视为阶段性闭环（保持行为不变）。
   - ✅ 进展（保持行为不变；每次只改 1 个方法）：已完成 “旧 gateway 压扁为委托壳” 的 S0：压扁 `CourseUpdateGatewayImpl.updateCoursesType`（链路短；目标：旧 gateway 仅保留事务边界与委托调用；落地：`709dc5b6`）。
   - ✅ 进展（保持行为不变；每次只改 1 个方法）：已进一步压扁 `CourseDeleteGatewayImpl.deleteCourseType`：新增 `DeleteCourseTypeEntryUseCase`，旧 gateway 不再构造命令，仅保留事务边界与委托调用（落地：`cf747b9c`）。
   - ✅ 进展（保持行为不变；每次只改 1 个方法）：已进一步压扁 `CourseDeleteGatewayImpl.deleteCourse`：新增 `DeleteCourseGatewayEntryUseCase`，旧 gateway 不再构造命令，仅保留事务边界与委托调用（落地：`dfd977fe`）。下一步建议：继续压扁 `CourseDeleteGatewayImpl.deleteCourses`（Serena：调用点为 `DeleteCoursesPortImpl.deleteCourses`；每次只改 1 个方法，保持行为不变）。
   - ✅ 进展（保持行为不变；每次只改 1 个方法）：已进一步压扁 `CourseDeleteGatewayImpl.deleteCourses`：新增 `DeleteCoursesGatewayEntryUseCase`，旧 gateway 不再构造命令，仅保留事务边界与委托调用（落地：`6428e685`）。
   - ✅ 进展（保持行为不变；每次只改 1 个方法）：已进一步压扁 `CourseDeleteGatewayImpl.deleteSelfCourse`：新增 `DeleteSelfCourseGatewayEntryUseCase`，旧 gateway 不再构造命令，仅保留事务边界与委托调用（落地：`c0268b14`）。
   - ✅ 进展（保持行为不变；每次只改 1 个方法）：已压扁 `CourseUpdateGatewayImpl.assignTeacher`：新增 `AssignTeacherGatewayEntryUseCase`，旧 gateway 不再构造命令（落地：`0b85c612`）。下一步建议：继续压扁 `CourseUpdateGatewayImpl.updateCourse`（Serena：调用点为 `UpdateCoursePortImpl.updateCourse`；每次只改 1 个方法，保持行为不变）。

4) 基础设施（S1 退场候选，保持行为不变）：`eva-infra` 仍存在多处旧 `*GatewayImpl`（需逐个用 Serena 证伪其剩余方法是否仅为委托壳；以及评估“归属到哪个 BC / shared-kernel / 继续保留在共享技术模块”）。
   - 候选清单（Serena 盘点，2026-01-10 更新；滚动修订）：✅ 已清零。
     - 补充进展（2026-01-10，保持行为不变）：`DepartmentGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package` 不变；最小回归通过；落地：`acb13124`）。
     - 补充进展（2026-01-10，保持行为不变）：`ClassroomGatewayImpl` 已从 `eva-infra` 归位到 `bc-course-infra`（保持 `package` 不变；最小回归通过；落地：`26b183d5`）。
     - 补充进展（2026-01-10，保持行为不变）：为后续归位 `SemesterGatewayImpl` 做编译闭合前置，`SemesterConverter` 已从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package edu.cuit.infra.convertor` 不变；最小回归通过；落地：`6c9e1d39`）。
     - 补充进展（2026-01-10，保持行为不变）：为后续归位评教配置旧 gateway（`EvaConfigGatewayImpl`）做编译闭合前置，`StaticConfigProperties` 已从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package edu.cuit.infra.property` 不变；最小回归通过；落地：`5a26af43`）。
     - 补充进展（2026-01-10，保持行为不变）：`EvaConfigGatewayImpl` 已从 `eva-infra` 归位到 `bc-evaluation/infrastructure`（保持 `package` 不变；最小回归通过；落地：`9a4e28aa`）。
     - 补充进展（2026-01-10，保持行为不变）：`SemesterGatewayImpl` 已从 `eva-infra` 归位到 `bc-course-infra`（保持 `package` 不变；最小回归通过；落地：`30e6a160`）。
     - 补充进展（2026-01-10，保持行为不变）：`MsgGatewayImpl` 已从 `eva-infra` 归位到 `bc-messaging`（保持 `package` 不变；最小回归通过；落地：`8ffcfe35`）。
     - 补充进展（2026-01-10，保持行为不变）：`LogGatewayImpl` 已从 `eva-infra` 归位到 `bc-audit-infra`（保持 `package` 不变；最小回归通过；落地：`673a19e3`）。
     - 补充进展（2026-01-10，保持行为不变）：`LdapPersonGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package` 不变；最小回归通过；落地：`1ff96d75`）。
     - 补充进展（2026-01-10，保持行为不变）：`MenuQueryGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package` 不变；最小回归通过；落地：`a7cb96e9`）。
     - 补充进展（2026-01-10，保持行为不变）：`MenuUpdateGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package` 不变；最小回归通过；落地：`09574045`）。
     - 补充进展（2026-01-10，保持行为不变）：`RoleQueryGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package` 不变；最小回归通过；落地：`457b6780`）。
     - 补充进展（2026-01-10，保持行为不变）：`RoleUpdateGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam/infrastructure`（保持 `package` 不变；最小回归通过；落地：`1826ac99`）。
     - 补充进展（2026-01-10，保持行为不变）：`UserQueryGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam/infrastructure`（保持 `package` 不变；最小回归通过；落地：`b9d8e6b8`）。
     - 补充进展（2026-01-10，保持行为不变）：`UserUpdateGatewayImpl` 已从 `eva-infra` 归位到 `bc-iam/infrastructure`（保持 `package` 不变；最小回归通过；落地：`69b72d86`）。
   - 补充：`CourseDeleteGatewayImpl`、`CourseQueryGatewayImpl`、`CourseUpdateGatewayImpl` 已归位到 `bc-course/infrastructure`（保持 `package` 不变；细节见 `NEXT_SESSION_HANDOFF.md` 0.9；落地：`38f58e0a/d438e060`），因此不再计入 `eva-infra` 残留。

---

## 5. 候选目标（按模块/文件归类）

> 注意：以下“LOC”是粗略估算，主要用于优先级排序，不作为验收依据。

### 5.1 课程域（course）

#### A) `bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`

写侧目标说明（已在最近会话完成收敛/清理，作为回溯保留）：
- ✅ `addNotExistCoursesDetails/addExistCoursesDetails/updateSelfCourse/updateCourse/updateCourses/updateSingleCourse` 等已收敛为“委托壳”，主写逻辑迁移到 `bc-course` 用例 + `eva-infra` 端口适配器（行为保持不变）。
- ✅ `JudgeCourseTime/JudgeCourseType/toJudge/getDifference` 等遗留私有逻辑已清理（避免旧 gateway 回潮承载业务流程）。
- ⏳ `isImported` 仍保留在 gateway（偏查询/校验），可在读侧收敛阶段再统一处理。

#### B) `bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseQueryGatewayImpl.java`

✅ 已收敛（先结构化 QueryRepo，行为不变）：
- `CourseQueryGatewayImpl` 已退化为委托壳（读侧入口不变）。
- 复杂查询/组装逻辑已抽取到：
  - `bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/query/CourseQueryRepo.java`
  - `bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/query/CourseQueryRepository.java`
- 落地提交：`ba8f2003/881e1d12/5101a341`

建议策略：
- 后续如需继续优化：可按“查询主题”拆 QueryService（例如：课表视图/评教统计/移动端周期课表），或再引入 CQRS 投影表（后置）。

---

### 5.2 评教域（eva）

#### A) `bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/eva/EvaUpdateGatewayImpl.java`

写侧高价值目标（体积大、跨表）：
- ✅ `postEvaTask`（~145 LOC，含 DB/Mapper）：发布评教任务已收敛到 `bc-evaluation`（落地提交：`8e434fe1/ca69b131/e9043f96`）。
- ✅ `putEvaTemplate`（~47 LOC，含 DB/Mapper）：旧实现已清理（提交评教主链路已在 `bc-evaluation`；落地提交：`12279f3f`）。
- ✅ `updateEvaTemplate/addEvaTemplate`（模板新增/修改，含 DB/Mapper）：已收敛到 `bc-evaluation`（落地提交：`ea03dbd3`）。

#### B) `bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/eva/EvaDeleteGatewayImpl.java`

删改写侧目标：
- ✅ `deleteEvaRecord`（~57 LOC，含 DB/Mapper）已收敛到 `bc-evaluation`（落地提交：`ea928055/07b65663/05900142`）。
- ✅ `deleteEvaTemplate`（~33 LOC，含 DB/Mapper）已收敛到 `bc-evaluation`（落地提交：`ea928055/07b65663/05900142`）。

#### C) （历史路径，已移除）`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/eva/EvaQueryGatewayImpl.java`

历史方法簇（旧 gateway 中曾包含，现已拆分/迁移到 QueryPort/QueryRepo）：
- `pageEvaRecord`（~148 LOC）
- `getEvaData`（~94 LOC）
- `evaScoreStatisticsInfo/getEvaLogInfo/evaSelfTaskInfo/evaTemplateSituation/pageEvaUnfinishedTask/getEvaEdLogInfo` 等（60~90 LOC）
- 大量统计/报表相关方法（30~50 LOC）

进展：
- ✅ 已抽取 `EvaQueryRepo/EvaQueryRepository`，`EvaQueryGatewayImpl` 退化委托壳（落地提交：`02f4167d`）。
- ✅ 已按“统计/任务/记录/模板”拆分查询端口，并让旧 gateway 委托到细分端口（落地提交：`bb391698/570fecb6/ec5191a3/3e5da427`）。
- ✅ 旧 `EvaQueryGateway`/`EvaQueryGatewayImpl` 已移除。

建议策略：
- 未完成清单：继续按用例维度细化 QueryService（任务/记录/模板），并评估 `EvaQueryRepo` 的内部拆分策略（保持统计口径不变）。

---

### 5.3 消息域（msg）

#### A) `bc-messaging/src/main/java/edu/cuit/infra/gateway/impl/MsgGatewayImpl.java`

目前多为 CRUD/状态位更新（单方法体积不大），但仍属于“未模块化”的领域能力：
- `queryMsg/queryTargetAmountMsg/updateMsgDisplay/updateMsgRead/updateMultipleMsgRead/insertMessage/deleteMessage/deleteTargetTypeMessage` 等。

建议策略：
- 若后续要继续做“跨域副作用事件化”，可逐步把消息发送/撤回/查询统一收敛到 `bc-messaging` 的端口与用例，旧 gateway 委托化。
- 进展：删除/已读写侧已收敛到 `bc-messaging`（落地提交：`22cb60eb/dd7483aa`），查询读侧 `queryMsg/queryTargetAmountMsg` 已收敛到 `bc-messaging`（落地提交：`05a2381b`），剩余 `insert/display` 可作为下一阶段目标。
- 进展：删除/已读写侧已收敛到 `bc-messaging`（落地提交：`22cb60eb/dd7483aa`），查询读侧 `queryMsg/queryTargetAmountMsg` 已收敛到 `bc-messaging`（落地提交：`05a2381b`），插入写侧 `insertMessage` 已收敛到 `bc-messaging`（落地提交：`8445bc41`），剩余 `display` 可作为下一阶段目标。
- 进展：删除/已读写侧已收敛到 `bc-messaging`（落地提交：`22cb60eb/dd7483aa`），查询读侧 `queryMsg/queryTargetAmountMsg`、插入写侧 `insertMessage`、展示状态写侧 `updateMsgDisplay` 已收敛到 `bc-messaging`（落地提交：`05a2381b/8445bc41/c315fa22`），旧 `MsgGatewayImpl` 已全量退化为委托壳（行为不变），且已从 `eva-infra` 归位到 `bc-messaging`（落地：`8ffcfe35`）。

---

### 5.4 系统管理（用户/角色/菜单）

这些模块多为典型 IAM/权限模型 CRUD，当前主要仍在 `eva-infra` gateway + mapper。

候选目标（体积/复杂度相对更高者）：
- ✅ `eva-infra/.../user/UserUpdateGatewayImpl.assignRole`（~30 LOC，已收敛到 `bc-iam`；落地提交：`16ff60b6/b65d311f/a707ab86`）
- ✅ `eva-infra/.../user/UserUpdateGatewayImpl.createUser`（~23 LOC，已收敛到 `bc-iam`；落地提交：`c3aa8739/a3232b78/a26e01b3/9e7d46dd`）
- ✅ `eva-infra/.../user/UserQueryGatewayImpl.fileUserEntity`（~30 LOC，已收敛到 `bc-iam`；落地提交：`3e6f2cb2/8c245098/92a9beb3`）
- ✅ `eva-infra/.../user/UserQueryGatewayImpl.findIdByUsername/findUsernameById/getUserStatus/isUsernameExist`（~20~40 LOC，已收敛到 `bc-iam`；保持行为不变；落地提交：`9f664229/38384628/de662d1c/8a74faf5`）
- ✅ `eva-infra/.../user/UserQueryGatewayImpl.findAllUserId/findAllUsername/allUser/getUserRoleIds`（~15~30 LOC，已收敛到 `bc-iam`；保持行为不变；落地提交：`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`）
- ✅ `eva-infra/.../user/MenuUpdateGatewayImpl.handleUserMenuCache`（~19 LOC，已收敛到 `bc-iam`；保持行为不变；落地提交链：`f8838951/91d13db4/7fce88b8/d6e3bed1/4d650166/46e666f9`）
- ✅ `eva-infra/.../user/RoleUpdateGatewayImpl.assignPerms/deleteMultipleRole`（~17~18 LOC，已收敛到 `bc-iam`；保持行为不变；落地提交链：`f8838951/91d13db4/7fce88b8/d6e3bed1/4d650166/46e666f9`）

建议策略：
- 若拆 BC，可考虑独立 `bc-iam`（或 `bc-user`）来承载权限/用户域用例与端口；否则保持为“薄 CRUD gateway”，优先级可低于课程/评教主链路。

---

### 5.5 日志与配置

#### A) `bc-audit/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/LogGatewayImpl.java`
- `page`（~21 LOC，含 DB/Mapper）
- `toSysLogEntity`（~16 LOC，含 DB/Mapper）

#### B) `bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/eva/EvaConfigGatewayImpl.java`
- `readConfig`（~30 LOC，含 DB/Mapper）
- `writeConfig`（~17 LOC，含 DB/Mapper）

建议策略：
- 这些通常是“支撑域”，可后置；若要拆 BC，可考虑 `bc-config` / `bc-log`。

---

## 6. “下一批行动”建议（不含实现，只给路线）

阶段性策略微调（2025-12-29）：
- ✅ 复核口径（2026-01-26，不改业务语义）：`spring-boot-starter-websocket` 仅由组合根 `start` 显式承接；并已将 `EvaConfigBizConvertor`、`EvaRecordBizConvertor`、`EvaTaskBizConvertor`、`EvaTemplateBizConvertor` 从 `eva-app` 归位到 `eva-infra-shared`（保持行为不变），并将 `EvaConfigService` 从 `eva-app` 归位到 `bc-evaluation-infra`（保持行为不变），并将 `UserCourseDetailQueryExec`、`FileImportExec`、`package-info.java` 从 `eva-app` 归位到 `bc-course-infra`（保持行为不变）。当前 `eva-app` 已退场（组合根去依赖：`0a9ff564`；reactor 移除：`b5f15a4b`；删除 `eva-app/pom.xml`：`4bfa9d40`）；`eva-adapter` 残留 Controller 口径以 `NEXT_SESSION_HANDOFF.md` 0.10.2 为准（当前 0 个，已清零）；组合根 `start` 已移除对 `eva-adapter` 的 Maven 依赖（落地：`92a70a9e`；保持行为不变）。补充：✅ 已完成消息入口归位前置（保持行为不变）：为后续将 `MessageController` 从 `eva-adapter` 归位到 `bc-messaging` 做编译闭合前置，在 `bc-messaging/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`、`zym-spring-boot-starter-security`、`shared-kernel`（落地：`aa7d57bb`）。补充：✅ 已完成审计日志入口归位（保持行为不变）：`LogController` 已从 `eva-adapter` 归位到 `bc-audit/infrastructure`（编译闭合前置：`2464d2b9`；入口归位：`b592cc0f`）。
- ✅ 允许“微调”：仅限结构性重构（收窄依赖/拆接口/移动默认值兜底），**不改业务语义**；缓存/日志/异常文案/副作用顺序完全不变。
  - 🎯 下一批主线建议（更新至 2026-01-29，保持行为不变）：✅ 已闭环：**S1：`eva-adapter` 退场**（root reactor 移除 + `start` 去依赖 + 删除模块 pom；口径见 `NEXT_SESSION_HANDOFF.md` 0.9/0.10）；✅ 已闭环：**依赖收敛纠偏**（`bc-iam-contract` 已恢复对 `bc-evaluation-contract` 的显式依赖，避免误判导致编译漂移；见 `NEXT_SESSION_HANDOFF.md` 0.9）；✅ 已补充：**依赖收敛（单 pom）**：`eva-domain` 已移除未使用的 `spring-statemachine-core`（保持行为不变；口径见 `NEXT_SESSION_HANDOFF.md` 0.9）；✅ 已补充：**IAM 依赖收敛 + 去 `eva-domain` 前置拆解**：已逐类归位 `MenuQueryGateway`、`LdapPersonEntity`、`LdapPersonGateway` 到 `bc-iam-domain`（保持 `package`/签名不变；见 `NEXT_SESSION_HANDOFF.md` 0.9）；下一刀建议：继续按 Serena 证据化盘点，逐类归位“仅 IAM 使用”的 `edu.cuit.domain.*` 子集（避免提前触碰跨 BC 复用的 `RoleEntity/RoleQueryGateway`）。
  - 🎯 补充建议（更新至 2026-02-02，保持行为不变）：当前“`eva-*` 全量整合（从 root reactor 移除）”仍被 `eva-domain/eva-infra-dal/eva-infra-shared/eva-base` 阻塞（多个 `bc-*` 仍编译期依赖它们）。建议先按最小成本把阻塞面收敛到“仅技术共享”：
    1) **IAM 并行（10.3）**：继续将依赖方对 `UserQueryGateway` 的编译期依赖收敛为 `bc-iam-contract` 的最小 Port（优先目标：先用 Serena 证据化盘点 `bc-*` 的残留引用点后择一闭环；每次只改 1 个类；注意 `SpringUtil.getBean(...)` 次数/顺序不变）。
    2) **`eva-domain` 拆解归位（长期主线）**：优先选择“仅单 BC 引用”的 `edu.cuit.domain.gateway/*` / `edu.cuit.domain.entity/*` 逐类迁入对应 `bc-*/domain`（保持 `package` 不变，仅改变 Maven 模块归属），再按“单 pom 闭环”逐个模块移除对 `eva-domain` 的编译期依赖；跨 BC 复用类型先留在 `eva-domain` 过渡，避免误归属导致反向依赖与循环依赖。
    - 🧱 S1（`eva-*` 技术切片整合，最小试点；保持行为不变）：`CourseTemplateLockQueryPortImpl` 已从 `eva-infra` 归位到 `bc-template-infra`（落地：`9b46d5a7`），组合根 `start` 已移除 `eva-infra(runtime)` 兜底依赖（落地：`1e2ffa89`），当前 `eva-infra` 已无业务实现类（仅剩 3 个 `package-info.java`），且已在 Serena + `rg` 证伪无依赖后从 root reactor 移除 `<module>eva-infra</module>`（落地：`0aab4516`；保持行为不变）。下一刀建议（可选，独立提交，保持行为不变）：评估是否删除 `eva-infra/` 目录与 `eva-infra/pom.xml`（当前已不再参与 reactor/无依赖方）。
    - 下一刀建议（保持行为不变；每次只改 1 个类或 1 个 `pom.xml`）：将主线从“入口壳结构性收敛（Controller）”逐步切换为 **S0.2 延伸（依赖方 pom 收敛）+ `eva-domain` 拆解归位**：
      1) 依赖方 `pom.xml` 收敛：优先移除无用依赖/将依赖收窄到 `*-contract`/`shared-kernel`（移除前必须 Serena + `rg` 双证据，避免误删导致增量编译误判）。
      2) `eva-domain` 拆解归位：按 BC 逐步把 `edu.cuit.domain.*` 的类型/接口归位到对应 `bc-*/domain`（必要时保持 `package` 不变以保证行为与序列化不变），并在证伪无引用后逐个模块移除对 `eva-domain` 的编译期依赖；为后续“整合/移除 `eva-*` reactor 模块”创造前置条件（验收口径见 `DDD_REFACTOR_PLAN.md` 10.5）。
    - ✅ 已完成（保持行为不变）：Audit 侧 `LogController` 已从 `eva-adapter` 归位到 `bc-audit/infrastructure`（编译闭合前置：`2464d2b9`；入口归位：`b592cc0f`）。
    - 更新（2026-01-19，保持行为不变）：✅ 已完成：`EvaConfigQueryController`（`5e7537ef`）、`EvaQueryController`（`d101ce07`）、`EvaConfigUpdateController`（`b5530281`）、`UpdateEvaController`（`3972b7e4`）、`DeleteEvaController`（`d1471ff5`）、`MessageController`（`7b076019`）与 `LogController`（`b592cc0f`）已从 `eva-adapter` 归位到对应 BC（保持行为不变）；`eva-adapter` 残留 Controller 已清零（口径见 `NEXT_SESSION_HANDOFF.md` 0.10.2）。
    - 并行落点（建议，保持行为不变；每次只改 1 个类或 1 个 pom）：继续以 `bc-iam` 为样例推进“入口壳结构性收敛（Controller）”，并将“依赖方 pom 收敛”作为并行主线（但务必先证据化，避免误删依赖导致 `contract` 签名不一致）。
  - ✅ 主线口径更新（滚动）：`bc-messaging` 的“归位 + 依赖收敛”已阶段性闭环；`bc-course` 的 S0（旧 gateway 压扁为委托壳）已推进到阶段性闭环（见 4.2/4.3 与 `NEXT_SESSION_HANDOFF.md` 0.9）。当前下一批主线：**S0.2 延伸（收敛 `bc-course` 的协议承载面 + 收敛依赖方对 `bc-course` 的编译期依赖）**，按“先 Serena 证据化 → 再小步迁移协议对象到 `shared-kernel` / 依赖替换 `pom.xml` → 最小回归 → 提交 → 三文档同步”的节奏推进（保持行为不变）。
  - ✅ 已完成（bc-course，实现承载面，保持行为不变）：Serena 证伪 `eva-infra-shared/src/main/java/edu/cuit/infra/gateway/impl/course/operate/` 下 `CourseImportExce/CourseRecommendExce` 仅课程域使用后，已归位到 `bc-course/infrastructure`（保持 `package` 不变，仅搬运与编译闭合；落地：`d3b9247e`）；`CourseFormat` 跨 BC 复用继续留在 `eva-infra-shared`。下一簇建议：进入“依赖方编译期依赖收敛”，逐个模块证伪后每次只改 1 个 `pom.xml`，将 `bc-course` 替换为 `shared-kernel`（保持行为不变）。
 - ✅ 并行证伪（依赖方编译期依赖收敛，保持行为不变）：最新盘点显示：当前未发现仍显式依赖 `bc-course` 且满足“仅使用 `edu.cuit.client.*` 类型”的模块；且 `eva-app` 已完成去 `bc-course` 编译期依赖（落地：`6fe8ffc8`）。因此“收敛依赖方对 `bc-course` 的编译期依赖”短期内暂无可执行的 `pom.xml` 依赖替换点（保持行为不变）。
			  - ✅ 并行主线（基础设施 S1 退场候选：旧 gateway 归位，保持行为不变）：已完成 `DepartmentGatewayImpl` → `bc-iam-infra`（`acb13124`）、`ClassroomGatewayImpl` → `bc-course-infra`（`26b183d5`）、`SemesterGatewayImpl` → `bc-course-infra`（`30e6a160`）；且为闭合编译期依赖已先归位 `SemesterConverter` → `eva-infra-shared`（`6c9e1d39`）。补充：评教/IAM 的残留旧 `*GatewayImpl` 已归位到对应 BC 的 `infrastructure` 子模块，且 `eva-infra/src/main/java/edu/cuit/infra/gateway/impl` 下残留已清零（详见 `NEXT_SESSION_HANDOFF.md` 0.10）。下一步建议：转向 **S1（eva-* 退场/整合）前置**（优先清空 `eva-app` 的残留支撑类，并规划 `eva-adapter` Controller 的迁移/承接方式；保持行为不变）。
				  - 下一步建议（入口壳收敛阶段性闭环，保持行为不变）：`eva-app` 下 `*ServiceImpl` 已清零（口径：`fd -t f 'ServiceImpl\\.java$' eva-app/src/main/java | wc -l` → 0），但 `eva-app` 仍残留少量支撑类（口径：`fd -t f -e java . eva-app/src/main/java | wc -l`）。下一步建议：进入 S1 前置，优先从 Sa-Token 配置/切面/Convertor 等“更独立、装配风险更低”的类开始逐个归位（每次只搬 1 个类；保持行为不变）。
			  - ✅ 补充更新（2026-01-13，保持行为不变）：模板锁定相关类型（`CourseTemplateLockQueryPort/CourseTemplateLockService/TemplateLockedException`）已下沉到 `bc-template-domain`（包名不变），且 `eva-infra`、`bc-course(application)` 等模块已将对 `bc-template` 的编译期依赖收敛为 `bc-template-domain`（最小回归通过；详见 4.2 与 `NEXT_SESSION_HANDOFF.md` 0.9/0.10）。下一刀建议：✅ 已完成：收敛 `eva-infra/pom.xml` 并移除对 `bc-evaluation` / `bc-iam` / `bc-audit` 的编译期依赖（Serena + `rg` 证伪仅类型引用；最小回归通过；落地：`023d63be`）。同时 ✅ 已完成：已将 `bc-ai-report` 的评教依赖从 application 层“下沉归位”到基础设施层：`bc-ai-report-infra` 显式依赖 `bc-evaluation`（落地：`c0f78068`），并在证伪 application 无引用后移除 `bc-ai-report/application/pom.xml` 对 `bc-evaluation` 的编译期依赖（落地：`87179f19`）。同时 ✅ 已完成：IAM 侧为收敛 `eva-app` → `bc-iam` 编译期耦合做前置：旧入口 `DepartmentServiceImpl` 已从 `eva-app` 归位到 `bc-iam-infra`（落地：`68dea36a`），菜单入口链路已完成归位：`MenuBizConvertor`（`6298e5a7`）与 `MenuServiceImpl`（`6aef1d96`）已归位到 `bc-iam-infra`；角色入口链路继续推进：`RoleBizConvertor` 已归位到 `bc-iam-infra`（落地：`cf0773ac`），并进一步将 `RoleServiceImpl` 归位到 `bc-iam-infra`（落地：`3011ab83`）。此外，为下一刀搬运 `UserAuthServiceImpl` 做编译闭合前置，已在 `bc-iam-infra/pom.xml` 补齐 `zym-spring-boot-starter-security`（落地：`bded148a`）。后续建议：继续按“先搬运支撑类 → 再搬运入口壳 → 再收敛 pom”的节奏推进（保持行为不变）。
		    - 每步闭环模板：Serena 证据化盘点（符号定位/引用分析）→ 最小回归 → 提交（代码）→ 三文档同步 → 提交（文档）→ push（保持行为不变）。
		    - 新对话开启提示词：优先复制 `NEXT_SESSION_HANDOFF.md` 的 0.11「E 专用简版」（收敛 `eva-adapter` 残留 `*Controller`），避免继续使用历史 B/C/D 简版导致目标漂移。
	    - 补充（保持行为不变）：为让 `MsgServiceImpl` 能直接委托 `bc-messaging` 的 UseCase，已先在 `eva-app/pom.xml` 补齐对 `bc-messaging` 的编译期依赖（运行时 classpath 已包含该模块；最小回归通过；落地：`02d338a9`）。
	- ✅ 下一步小簇建议（bc-messaging，保持行为不变）：按 `DDD_REFACTOR_PLAN.md` 10.3 路线推进（先组合根 → 再监听器/应用侧适配器 → 最后基础设施端口适配器与依赖收敛）。
	  - ✅ 已完成：组合根 `BcMessagingConfiguration`（`4e3e2cf2`）；✅ 已完成：监听器 `CourseOperationSideEffectsListener`（`22ee30e7`）；✅ 已完成：监听器 `CourseTeacherTaskMessagesListener`（`0987f96f`）
	  - ✅ 已完成：支撑类 `MsgResult`（`31878b61`，当前位于 `bc-messaging-contract`）；✅ 已完成：应用侧端口适配器 `CourseBroadcastPortAdapter`（`84ee070a`）；✅ 已完成：应用侧端口适配器 `TeacherTaskMessagePortAdapter`（`9ea14cff`）；✅ 已完成：应用侧端口适配器 `EvaMessageCleanupPortAdapter`（`73ab3f3c`）。
  - ✅ 已完成（前置，DAL/Convertor 归位）：`MsgTipDO/MsgTipMapper(+xml)` → `eva-infra-dal`；`MsgConvertor` → `eva-infra-shared`（保持 `package/namespace` 不变）。
  - ✅ 已完成（基础设施端口适配器归位）：`MessageDeletionPortImpl/MessageReadPortImpl/MessageDisplayPortImpl/MessageInsertionPortImpl/MessageQueryPortImpl` → `bc-messaging`（保持 `package` 不变；保持行为不变）。
  - ✅ 依赖收敛准备（事件枚举下沉到 contract）：`CourseOperationMessageMode` → `bc-messaging-contract`（保持 `package` 不变；保持行为不变；`b2247e7f`）。
  - ✅ 依赖收敛准备（事件载荷下沉到 contract）：`CourseOperationSideEffectsEvent` → `bc-messaging-contract`（保持 `package` 不变；保持行为不变；`ea2c0d9b`）。
  - ✅ 依赖收敛准备（事件载荷下沉到 contract）：`CourseTeacherTaskMessagesEvent` → `bc-messaging-contract`（保持 `package` 不变；保持行为不变；`12f43323`）。
  - ✅ 依赖收敛（应用侧编译期依赖面收窄）：`eva-app` → `bc-messaging-contract`（替换 `eva-app` 对 `bc-messaging` 的编译期依赖；保持行为不变；`d3aeb3ab`）。
  - 下一步建议（依赖收敛后半段，保持行为不变）：✅ 已完成：在 `start/pom.xml` 补齐 `bc-messaging` 的 `runtime` 依赖（保持行为不变；落地：`f23254ec`）；✅ 已完成：移除 `eva-infra/pom.xml` 中 `bc-messaging` 的 `runtime` 依赖，把“运行时装配责任”上推到组合根 `start`（保持行为不变；落地：`507f95b2`）。后置建议：用 Serena 再次证伪 `eva-infra` 不再需要 `bc-messaging` 作为运行时兜底依赖。
- 下一步小簇建议（bc-course，S0：旧 gateway 压扁为委托壳，保持行为不变）：读侧已覆盖 `courseNum/courseTimeDetail/getDate/getCourseDetail/getTimeCourse`，写侧已覆盖 `allocateTeacher/deleteCourses/updateSingleCourse/addNotExistCoursesDetails/addExistCoursesDetails/deleteSelfCourse/updateSelfCourse/importCourse/updateCourse/updateCourses/delete/addCourse`（见 4.2）。S0 收尾（依赖收窄）已完成，且已完成样例压扁 `CourseUpdateGatewayImpl.updateCourseType/addCourseType/updateCoursesType`（其中 `updateCoursesType`：Serena 证实唯一调用点为 `ICourseTypeServiceImpl.updateCoursesType`；落地：`709dc5b6`），并已进一步压扁 `CourseDeleteGatewayImpl.deleteCourseType/deleteCourse/deleteCourses/deleteSelfCourse`（旧 gateway 不再构造命令；落地：`cf747b9c/dfd977fe/6428e685/c0268b14`），且已压扁 `CourseUpdateGatewayImpl.assignTeacher`（旧 gateway 不再构造命令；落地：`0b85c612`），并已压扁 `CourseUpdateGatewayImpl.updateCourse`（Serena：调用点为 `UpdateCoursePortImpl.updateCourse`；旧 gateway 不再构造 Command；落地：`c31df92c`）、`CourseUpdateGatewayImpl.updateCourses`（旧 gateway 不再构造 Command；落地：`84dffcc2`）、`CourseUpdateGatewayImpl.importCourseFile`（Serena：调用点为 `ImportCourseFilePortImpl.importCourseFile`；旧 gateway 不再构造 Command；落地：`5e93a08a`）、`CourseUpdateGatewayImpl.updateSingleCourse`（Serena：调用点为 `UpdateSingleCoursePortImpl.updateSingleCourse`；旧 gateway 不再构造 Command；落地：`9eea1a54`）、`CourseUpdateGatewayImpl.updateSelfCourse`（Serena：调用点为 `UpdateSelfCoursePortImpl.updateSelfCourse`；旧 gateway 不再构造 Command；落地：`c0f30c1f`）、`CourseUpdateGatewayImpl.addNotExistCoursesDetails`（Serena：调用点为 `AddNotExistCoursesDetailsPortImpl.addNotExistCoursesDetails`；旧 gateway 不再构造 Command；落地：`62d48ee6`）与 `CourseUpdateGatewayImpl.addExistCoursesDetails`（Serena：调用点为 `AddExistCoursesDetailsPortImpl.addExistCoursesDetails`；旧 gateway 不再构造 Command；落地：`de34a308`）。下一步建议：进入 **S0.1（收敛 `eva-domain` → `eva-client` 依赖）**（保持行为不变；每次只改 1 个类/1 个小簇）。
  - 新会话续接提示词：见 `NEXT_SESSION_HANDOFF.md` 的 0.11 推荐版（Controller 优先，不固化 commitId）。
- 中长期建议（S1，保持行为不变）：**`eva-*` 技术切片退场/整合到各 BC**。统一口径与前置条件见 `DDD_REFACTOR_PLAN.md` 10.5。建议节奏：先把入口方法簇按 BC 迁完并让旧入口/旧 gateway 退化为委托壳（A→B），再做装配与 Maven/目录结构层面的“模块移除/折叠”类改动，避免一次性大迁移导致回滚困难与副作用顺序漂移。

如果继续按“写侧优先”的策略推进，下一批候选（高 → 低）建议是：

0) 结构性里程碑（需求变更，2025-12-24）：将“BC=一个顶层聚合模块、内部 `domain/application/infrastructure` 为子模块”的结构落地到目录与 Maven 结构中，并把历史平铺过渡模块（`bc-iam-infra`、`bc-evaluation-infra` 等）折叠归位到对应 BC 内部子模块（每步可回滚；保持行为不变）。  
0.1) 结构性里程碑（需求变更，2025-12-24）：逐步拆解 `eva-client`：按 BC 归属迁移 BO/CO/DTO；新增对象不再进入 `eva-client`；跨 BC 通用对象沉淀到 shared-kernel（每步可回滚；保持行为不变）。  
0.2) ✅ S0.1（拆 `eva-client` 的收尾，建议优先）：已闭环：对 `eva-domain` 仍存在的 `import edu.cuit.client.*` 做“来源证伪”（Serena 盘点清单 → 逐项确认类型文件实际归属模块；包名保持不变），并已从 root reactor 移除 `eva-client`（`ce07d75f`），随后从仓库移除 `eva-client/` 目录（`de25e9fb`）（每步最小回归+提交+三文档同步；保持行为不变）。  
   - 下一步小簇建议（从低风险到高收益，仍保持行为不变）：
     - ✅ 进展（2025-12-26）：课程域 `clientobject/course` 残留 CO（`ModifySingleCourseDetailCO/RecommendCourseCO/SelfTeachCourseCO/SelfTeachCourseTimeCO/SelfTeachCourseTimeInfoCO/SubjectCO/TeacherInfoCO`）以及 `SimpleCourseResultCO/SimpleSubjectResultCO` 已从 `eva-client` 归位到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ce1a0a90`）。
     - ✅ 进展（2025-12-26）：消息域 response DTO：`GenericResponseMsg/EvaResponseMsg` 已迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ecb8cee5`；其对 `SingleCourseCO` 的依赖由 `shared-kernel` 提供，避免引入 `bc-course` 反向依赖）。
     - ✅ 进展（2025-12-26）：消息域残留对象 `IMsgService/SendMessageCmd/MessageBO` 已归位到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`431a5a23`）。
     - ✅ 进展（2025-12-26）：消息域残留对象 `SendWarningMsgCmd` 已归位到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e6aa5913`）。
     - ✅ 进展（2025-12-26）：消息域 clientobject `EvaMsgCO` 已归位到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`2f257a86`）。
     - ✅ 进展（2025-12-27）：已将 AI 接口与 BO（`IAiCourseAnalysisService/AiAnalysisBO/AiCourseSuggestionBO`）从 `eva-client` 迁移到 `bc-ai-report`，并移除 `bc-ai-report` → `eva-client` 依赖（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`badb9db6`）。
     - ✅ 进展（2025-12-27）：已将 `EvaProp` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`4feabdd0`）。
     - ✅ 进展（2025-12-27）：已移除 `eva-infra-shared` → `eva-client` Maven 直依赖（保持行为不变；最小回归通过；落地提交：`9437bb12`）。
     - ✅ 已闭环：`eva-domain` 的 `import edu.cuit.client.*` 已完成来源证伪；`eva-client` 已退出 root reactor，且目录已从仓库移除（保持行为不变）。
1) AI 报告：已完成“剩余保存/落库/记录写链路”证据化盘点并证伪（证据清单见 `NEXT_SESSION_HANDOFF.md` 0.9）。后续将该方向的重点切换为 **S0 折叠 `bc-ai-report`**（仅搬运/依赖收敛，保持行为不变）。  
	   - 进展（2025-12-27）：已完成 S0 阶段 1：引入 `bc-ai-report-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-ai-report`；保持行为不变；落地：`e14f4f7a`）。下一步：继续把端口适配器/导出实现/AI 基础设施搬运到 `bc-ai-report/infrastructure` 子模块，并补齐装配依赖（保持行为不变）。
	   - 进展（2025-12-27）：已完成 S0 阶段 2：端口适配器/导出实现/AI 基础设施已归位 `bc-ai-report/infrastructure` 子模块，并补齐 `eva-app` → `bc-ai-report-infra` 依赖（保持行为不变；落地：`444c7aca`）。
2) 评教 BC 自包含三层结构：已完成阶段 1（读侧查询迁移：`be6dc05c`）与阶段 2（写侧 Repo 迁移：`24e7f6c9`），并已完成读侧门面加固 C-1（清理 `EvaQueryRepository` 为纯委托壳：`73fc6c14`）。C-2（读侧仓储瘦身）已完成盘点并关闭（落地：`5c1a03bc`）。  
3) bc-messaging（消息域）组合根/适配器归位（主线）：已完成“散落点证据化盘点 + 可回滚路线”文档化（见 `DDD_REFACTOR_PLAN.md` 第 10.3 节），并已完成组合根/监听器/应用侧端口适配器与基础设施端口适配器（含前置 DAL/Convertor 归位）归位到 `bc-messaging`（保持行为不变；每步最小回归+提交+三文档同步）。下一步建议：优先做 **依赖收敛**（尽量减少 `bc-messaging` 的编译期依赖面，只保留闭合编译/装配所必需的依赖），再评估是否需要推进 **结构折叠（S0）**（仅搬运/依赖收敛，保持行为不变）。

补充说明（避免后续会话口径漂移）：

- 结构性里程碑 0（S0）建议拆分步骤（每步最小回归 + 提交 + 三文档同步）：
  1) 先选 **单个 BC 试点**（推荐从 `bc-iam` 或 `bc-evaluation` 开始）：将 `bc-xxx/pom.xml` 改为聚合 `pom`（仅改 Maven/目录，不改业务语义）。
  2) 在 `bc-xxx/` 下创建 `domain/application/infrastructure` 三个子模块目录与 `pom.xml`，并把现有 `bc-xxx` 源码按职责“搬运归位”（只搬运/改包引用，逻辑不改；允许改包名）。
  3) 将历史平铺过渡模块 `bc-xxx-infra` 的源码迁入 `bc-xxx/infrastructure` 子模块，root `pom.xml` 仅保留 `<module>bc-xxx</module>`（实现“一个 BC 一条 module”）。
  4) 每个 BC 闭环后，再按相同套路推进下一个 BC；避免一次性全仓库搬迁导致回滚困难。
- 结构性里程碑 0.1（S0.1，拆 `eva-client`）建议拆分步骤：
  1) 用 Serena 盘点全仓库 `edu.cuit.client.*` 包下对象在各 BC/技术切片中的引用分布，先形成“归属映射表”（按 BC：IAM/Course/Evaluation/Template/Messaging/Audit/AIReport…）。
  2) **先迁移边界协议对象**（BO/CO/DTO/Query/Cmd）：按 BC 归属迁入 BC 的 `application` 子模块下 `contract/dto`（允许改包名，以归位到 `edu.cuit.bc.xxx.application.contract...`）。
  3) 对确实跨 BC 复用的通用对象（如分页、通用查询条件、通用返回体等）再沉淀到 shared-kernel（严格控范围，避免变成“新 eva-client”）。
  4) 迁移顺序建议：先从依赖面最集中的 `bc-iam` 开始（例如 `NewUserCmd/UpdateUserCmd/PagingQuery/GenericConditionalQuery/SimpleResultCO` 等），每次只迁一个小包/小类簇，确保可回滚。

---

## 7. 与交接文档的关系

- “已完成闭环与踩坑记录”以 `NEXT_SESSION_HANDOFF.md` 为准（每次会话结束必须更新）。
- 本文件用于“长期 Backlog 与统一执行模板”，避免每个新会话重复盘点。
- 新对话开启提示词：优先复制 `NEXT_SESSION_HANDOFF.md` 的 **0.11 推荐版（Controller 优先）**（不固化 commitId），避免提示词随会话滚动产生遗漏。
