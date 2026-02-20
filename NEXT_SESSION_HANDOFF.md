# 会话交接摘要（用于新对话继续）

> 仓库：`/home/lystran/programming/java/web/eva-backend`  
> 当前分支：`ddd`  
> 目标方向：把当前“按技术分层”的单体，逐步重构为“按业务限界上下文分模块”的 **DDD 模块化单体**，并预留低成本拆微服务路径（先拆服务、暂时共享库，小步快跑）。

---

## 0.0 术语约定（用于减少“gateway”历史命名带来的混淆）

> 背景：本项目早期参考 COLA 命名把很多类叫做 `*GatewayImpl`，但其中相当一部分同时承载了“应用入口 + DB 访问 + 副作用（缓存/日志）”，逐步演化成大泥球。
>
> 为了渐进式重构期间沟通一致，本仓库后续文档使用以下约定（不强制立刻改代码命名，但会在文档里严格区分）：

- **旧 gateway（LegacyGateway / 委托壳）**：历史遗留的 `eva-infra/.../*GatewayImpl`，对外接口不变（尤其是缓存注解/切面触发点必须保持），内部逐步退化为“委托到 UseCase”的壳。
- **UseCase（应用层用例）**：放在 `bc-*/application/usecase`，只做“业务用例入口与编排”，不直接依赖 DB。
- **Port（应用层端口）**：放在 `bc-*/application/port`，表达 UseCase 的出站依赖（持久化/外部系统/缓存等）。
- **Port Adapter（基础设施端口适配器）**：实现 Port 并原样搬运旧 DB/副作用流程（保持行为不变）。过渡期可能落在 `eva-infra/.../bc*/adapter` 或历史过渡模块 `bc-*-infra`；**需求变更后**不再新增 `bc-*-infra` 平铺模块，新增适配器优先归位到目标 BC 的 `infrastructure` 子模块（见下方“最终形态”）。
- **最终形态（目标，2025-12-24 需求变更）**：每个 BC 在仓库中只占用 **一个顶层目录/聚合模块**（例如 `bc-iam/`、`bc-evaluation/`），其内部按职责拆为 `domain/application/infrastructure` **子模块**（或至少 package 结构完整）；`eva-*` 技术切片逐步退场或仅保留 shared-kernel/统一装配与极少量跨 BC 胶水。为保持可回滚，当前已存在的 `bc-*-infra` 作为过渡形态保留一段时间，后续按里程碑“折叠归位”到对应 BC 内部子模块。
- **需求补充（2025-12-24）**：逐步拆解 `eva-client`：将 `edu.cuit.client.*` 下的 BO/CO/DTO 等“边界协议对象”按业务归属迁入对应 BC（优先放在 BC 的 `application` 子模块下的 `contract/dto` 包，避免领域层污染；**允许改包名**以完成归位）；确实跨 BC 复用的对象再沉淀到 shared-kernel，最终让 `eva-client` 退出主干依赖。

## 0.9 本次会话增量总结（滚动，按时间倒序，更新至 `HEAD`）

**2026-02-20（课程域：新增查询端口 `CourseIdsByTeacherIdQueryPort`，为评教写侧去 `CourseMapper` 直连做前置；保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`bc-evaluation/infrastructure` 的写侧端口适配器（例如 `PostEvaTaskRepositoryImpl`）仍存在对课程域 DAL `CourseMapper.selectList(eq teacher_id)` 的编译期直连，用于读取“教师所授课程 id 列表”；当前 `bc-course/application` 未提供等价查询端口。
- ✅ 执行（单类，保持行为不变）：在 `bc-course/application` 新增最小查询端口 `CourseIdsByTeacherIdQueryPort`（仅定义接口，不引入实现与调用点，避免行为漂移；后续按“补适配器（单类）→ 改调用侧（单类）”继续推进）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`cdc79886`。

**2026-02-20（DoD 目录退场收口：单目录清理 `eva-infra-dal/`，保持行为不变）**
- ✅ 前置证据（保持行为不变）：`git ls-files eva-infra-dal` 已为 0；Serena 列目录确认仅剩 `.flattened-pom.xml`、`target/`、`src/` 等构建残留（无任何 `*.java` / `*.xml`），不影响编译与运行期行为。
- ✅ 执行（单目录，保持行为不变）：删除 `eva-infra-dal/` 目录本身，推进 `fd -t d '^eva-' . -d 2` 的“目录退场”口径收口。
- 🧪 最小回归通过（Java17）：按约束先尝试 `mvnd`（启动阶段 `ExceptionInInitializerError`），已降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`1a5a9fb2`（空提交，仅用于记录“目录清理”动作，Git 不跟踪空目录/忽略产物）。

**2026-02-20（DoD 目录退场收口：单目录清理 `eva-infra/`，保持行为不变）**
- ✅ 前置证据（保持行为不变）：`git ls-files eva-infra` 已为 0；目录内仅剩 `.classpath/.project/.settings/target/.flattened-pom.xml` 等被忽略的构建产物与 IDE 元数据，不影响编译与运行期行为。
- ✅ 执行（单目录，保持行为不变）：删除 `eva-infra/` 目录本身，推进 `fd -t d '^eva-' . -d 2` 的“目录退场”口径收口。
- 🧪 最小回归通过（Java17）：按约束使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`50622ef2`（空提交，仅用于记录“目录清理”动作，Git 不跟踪空目录/忽略产物）。

**2026-02-20（DoD 目录退场收口：单文件删除 `eva-infra` 残留 `package-info.java`（gateway-impl），保持行为不变）**
- ✅ 前置证据（保持行为不变）：`eva-infra` 已从 root reactor 退场且 `pom.xml` 已删除；当前 `git ls-files eva-infra` 仅剩该残留文件；其内容仅为注释 + `package` 声明（无任何包级注解）。
- ✅ 执行（单文件，保持行为不变）：删除 `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/package-info.java`，继续缩小 `eva-infra/` tracked 文件表面积，为后续目录退场收口铺路。
- 🧪 最小回归通过（Java17）：按约束使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`0b4bc7f8`。

**2026-02-20（DoD 目录退场收口：单文件删除 `eva-infra` 残留 `package-info.java`（dal），保持行为不变）**
- ✅ 前置证据（保持行为不变）：`eva-infra` 已从 root reactor 退场且 `pom.xml` 已删除，目录中仅剩历史残留的 `package-info.java`（无业务语义）。
- ✅ 执行（单文件，保持行为不变）：删除 `eva-infra/src/main/java/edu/cuit/infra/dal/package-info.java`，缩小 `eva-infra/` tracked 文件表面积。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`4d41b7c7`。

**2026-02-20（DoD 目录退场收口：单文件删除 `eva-infra` 残留 `package-info.java`（convertor），保持行为不变）**
- ✅ 前置证据（保持行为不变）：`eva-infra` 已从 root reactor 退场且 `pom.xml` 已删除，目录中仅剩历史残留的 `package-info.java`（无业务语义）。
- ✅ 执行（单文件，保持行为不变）：删除 `eva-infra/src/main/java/edu/cuit/infra/convertor/package-info.java`，缩小 `eva-infra/` tracked 文件表面积。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`4f9391ab`。

**2026-02-20（DoD 目录退场收口：单目录清理 `eva-adapter/`，保持行为不变）**
- ✅ 前置证据（保持行为不变）：`eva-adapter` 无任何 tracked 文件（`git ls-files eva-adapter` 为 0）；目录内仅为被 `.gitignore` 忽略的构建产物与 IDE 元数据（如 `target/`、`.flattened-pom.xml`、`.settings/` 等）。
- ✅ 执行（单目录，保持行为不变）：删除 `eva-adapter/` 目录本身，推进 `fd -t d '^eva-' . -d 2` 的“目录退场”口径收口。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`ec620b56`（空提交，仅用于记录“目录清理”动作，Git 不跟踪空目录/忽略产物）。

**2026-02-20（DoD 目录退场收口：单目录清理 `eva-app/`，保持行为不变）**
- ✅ 前置证据（保持行为不变）：`eva-app` 无任何 tracked 文件（`git ls-files eva-app` 为 0）；目录内仅为被 `.gitignore` 忽略的构建产物与 IDE 元数据（如 `target/`、`.flattened-pom.xml`、`.settings/` 等）。
- ✅ 执行（单目录，保持行为不变）：删除 `eva-app/` 目录本身，推进 `fd -t d '^eva-' . -d 2` 的“目录退场”口径收口。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`3990a3b7`（空提交，仅用于记录“目录清理”动作，Git 不跟踪空目录/忽略产物）。

**2026-02-20（DoD 目录退场收口：单目录清理 `eva-base/`，保持行为不变）**
- ✅ 前置证据（保持行为不变）：`eva-base` 已从 root reactor 退场，且其下全部 `pom.xml` 已删除；目录中剩余内容仅为被 `.gitignore` 忽略的构建产物（如 `target/`、`.flattened-pom.xml`、IDE 元数据等），不影响运行时与编译行为。
- ✅ 执行（单目录，保持行为不变）：删除 `eva-base/` 目录本身，推进 `fd -t d '^eva-' . -d 2` 的“目录退场”口径收口。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`4eb50feb`（空提交，仅用于记录“目录清理”动作，Git 不跟踪空目录/忽略产物）。

**2026-02-20（`eva-base` 目录收口：单文件删除 `eva-base/pom.xml`，保持行为不变）**
- ✅ 前置证据（保持行为不变）：`eva-base/` 下仅剩 `pom.xml` 一个文件（其余 `eva-base-common/eva-base-config` 的 `pom.xml` 已删除）；且全仓库 `**/pom.xml` 中 `<artifactId>eva-base</artifactId>` 仅命中该文件本体。
- ✅ 执行（单文件，保持行为不变）：删除 `eva-base/pom.xml`，为后续按“单目录一刀”清理 `eva-base/` 空目录残留铺路。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`45770eec`。

**2026-02-20（`eva-base` 目录收口：单文件删除 `eva-base/eva-base-config/pom.xml`，保持行为不变）**
- ✅ 前置证据（保持行为不变）：全仓库 `**/pom.xml` 内 `<artifactId>eva-base-config</artifactId>` 仅命中其本体；且 root reactor 已移除 `<module>eva-base</module>`，该模块不再参与构建。
- ✅ 执行（单文件，保持行为不变）：删除 `eva-base/eva-base-config/pom.xml`，为后续按“单目录一刀”清理 `eva-base/` 空目录残留、满足 `fd -t d '^eva-' . -d 2` 口径铺路。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`1ba75ddf`。

**2026-02-20（`eva-base` 退场收尾：单 pom 从 root reactor 移除 `<module>eva-base</module>`，保持行为不变）**
- ✅ 前置证据（保持行为不变）：全仓库 `**/pom.xml` 已无任何 `<artifactId>eva-base-common</artifactId>` 命中；`eva-base` 内已移除 `eva-base-common` module 且其 `pom.xml` 已删除。
- ✅ 执行（单 pom，保持行为不变）：收敛 root `pom.xml`：移除 `<module>eva-base</module>`，使 `rg -n "<module>eva-" pom.xml` 无命中。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`786fc543`。

**2026-02-20（`eva-base-common` 退场收尾：单文件删除 `eva-base/eva-base-common/pom.xml`，保持行为不变）**
- ✅ 前置证据（保持行为不变）：`eva-base` reactor 已移除 `eva-base-common` module；且全仓库不再存在任何依赖方对 `eva-base-common` 的显式依赖点。
- ✅ 执行（单文件，保持行为不变）：删除 `eva-base/eva-base-common/pom.xml`（确保 `**/pom.xml` 不再出现 `<artifactId>eva-base-common</artifactId>`，为后续 root reactor 移除 `eva-base` 铺路）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`5ece67c3`。

**2026-02-20（`eva-base-common` 退场收尾：单 pom 将 `eva-base` reactor 移除 `eva-base-common` module，保持行为不变）**
- ✅ 前置证据（保持行为不变）：全仓库 `**/pom.xml` 已无任何“依赖方”对 `eva-base-common` 的显式依赖点（口径：`rg -n "<artifactId>eva-base-common</artifactId>" --glob "**/pom.xml" . | sort` 仅命中 `eva-base/eva-base-common/pom.xml` 本体）。
- ✅ 执行（单 pom，保持行为不变）：收敛 `eva-base/pom.xml`：从 `<modules>` 中移除 `<module>eva-base-common</module>`，为后续删除 `eva-base/eva-base-common/pom.xml` 铺路。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`dc8d949d`。

**2026-02-20（`eva-base-common` 退场收尾：单 pom 移除 `bc-iam/infrastructure` 对 `eva-base-common` 的依赖，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`bc-iam/infrastructure` 范围内仅 `pom.xml` 命中 `eva-base-common`；代码侧对 `LogModule` 的引用由 `shared-kernel` 承接且 `package` 不变。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`13398a74`。

**2026-02-20（`eva-base-common` 退场收尾：单 pom 移除 `bc-iam/contract` 对 `eva-base-common` 的依赖，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`bc-iam/contract` 范围内仅 `pom.xml` 命中 `eva-base-common`；代码侧命中 `GenericPattern`，已由 `shared-kernel` 承接且 `package` 不变。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`fc801525`。

**2026-02-19（`eva-base-common` 退场前置：单类搬运 `LogModule` 下沉到 `shared-kernel`，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`LogModule` 引用面广泛，主要用于各 BC Controller 的 `@OperateLog(module = LogModule.*)`；搬运后保持 `package edu.cuit.common.enums` 不变，调用侧无需改 import。
- ✅ 执行（单类搬运，保持行为不变）：将 `LogModule` 从 `eva-base/eva-base-common` 下沉到 `shared-kernel`（保持常量值与接口注释不变，仅改变 Maven 模块归属），为后续“逐 pom 去 `eva-base-common` 依赖”铺路。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`77f44292`。

**2026-02-19（`eva-base-common` 退场前置：单类搬运 `GenericPattern` 下沉到 `shared-kernel`，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`GenericPattern` 的引用面命中 `bc-iam/contract` 的 `UpdateUserCmd`；且搬运后 `package edu.cuit.common.enums` 不变，调用侧无需改 import。
- ✅ 执行（单类搬运，保持行为不变）：将 `GenericPattern` 从 `eva-base/eva-base-common` 搬运下沉到 `shared-kernel`（保持常量内容与类注释不变，仅改变 Maven 模块归属），为后续“逐 pom 去 `eva-base-common` 依赖”铺路。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`4be17a62`。

**2026-02-19（编译闭合纠偏：单 pom，`bc-iam/infrastructure` 显式补齐 MapStruct + LDAP + 课程域 gateway 编译依赖，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`bc-iam/infrastructure/src/main/java` 存在 `import org.mapstruct.*`；`LdapPersonDO` 使用 `org.springframework.ldap.odm.annotations.*`；且 `UserServiceImpl` 依赖 `edu.cuit.domain.gateway.course.CourseQueryGateway`（定义位于 `bc-course-domain`）。
- ✅ 执行（单 pom，保持行为不变）：在 `bc-iam/infrastructure/pom.xml` 显式补齐 `org.mapstruct:mapstruct`、`org.springframework.ldap:spring-ldap-core`、`org.springframework.data:spring-data-ldap`、`edu.cuit:bc-course-domain(provided)`，避免“增量构建未触发重编译”掩盖依赖缺口（仅闭合编译边界；运行期仍由组合根装配）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`03dc4f4e`。

**2026-02-19（编译闭合补强：单 pom，`bc-course/infrastructure` 显式补齐 MapStruct + 跨 BC contract 依赖，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`bc-course/infrastructure/src/main/java` 存在 `import org.mapstruct.*`（`CourseConvertor/CourseBizConvertor/SemesterConverter` 等）；且在触发全量编译时暴露出对 `bc-evaluation-domain`（`EvaConfigGateway`）与 `bc-messaging-contract`（`edu.cuit.bc.messaging.application.event.*`、`edu.cuit.client.*` 消息载荷类型）的编译期缺口。
- ✅ 执行（单 pom，保持行为不变）：在 `bc-course/infrastructure/pom.xml` 显式补齐 `org.mapstruct:mapstruct`、`edu.cuit:bc-evaluation-domain(provided)`、`edu.cuit:bc-messaging-contract(provided)` 以闭合编译边界（仅编译期需要；运行期仍由组合根装配）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`6ee4e485`。

**2026-02-19（`eva-infra-shared` 退场收尾：第 5 刀（单文件），删除 `eva-infra-shared/pom.xml`，保持行为不变）**
- ✅ 前置校验（保持行为不变）：全仓库 `**/pom.xml` 中 `<artifactId>eva-infra-shared</artifactId>` 仅命中 `eva-infra-shared/pom.xml` 自身（依赖方已清零）。
- ✅ 执行（单文件，保持行为不变）：删除 `eva-infra-shared/pom.xml`（`eva-infra-shared` 已从 root reactor 退场，删除 pom 不影响聚合构建边界）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`8aad22a2`。

**2026-02-19（`eva-infra-shared` 退场收尾：第 4 刀（单 pom），root `pom.xml` reactor 移除 `<module>eva-infra-shared</module>`，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：定位到 root `pom.xml` 的 `<modules>` 中仍包含 `<module>eva-infra-shared</module>`。
- ✅ 执行（单 pom，保持行为不变）：从 root `pom.xml` 的 reactor 移除 `<module>eva-infra-shared</module>`（仅改变聚合构建边界；不改业务语义/副作用顺序）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`3676b534`。

**2026-02-19（`eva-infra-shared` 依赖收敛：第 3 刀（单 pom），`bc-iam/infrastructure` 移除 `eva-infra-shared` 依赖，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：定位到 `bc-iam/infrastructure/pom.xml` 存在 `eva-infra-shared` Maven 依赖；且 `eva-infra-shared` 源码已清零（Java=`0`）。
- ✅ 执行（单 pom，保持行为不变）：从 `bc-iam/infrastructure/pom.xml` 移除 `edu.cuit:eva-infra-shared:${revision}` 依赖（仅收敛 Maven 编译边界；不改业务语义/副作用顺序）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`2c405c32`。

**2026-02-19（`eva-infra-shared` 依赖收敛：第 2 刀（单 pom），`bc-evaluation/infrastructure` 移除 `eva-infra-shared` 依赖，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：定位到 `bc-evaluation/infrastructure/pom.xml` 存在 `eva-infra-shared` Maven 依赖；且 `eva-infra-shared` 源码已清零（Java=`0`）。
- ✅ 执行（单 pom，保持行为不变）：从 `bc-evaluation/infrastructure/pom.xml` 移除 `edu.cuit:eva-infra-shared:${revision}` 依赖（仅收敛 Maven 编译边界；不改业务语义/副作用顺序）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`61d305e0`。

**2026-02-19（`eva-infra-shared` 依赖收敛：第 1 刀（单 pom），`bc-course-infra` 移除 `eva-infra-shared` 依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`UserConverter` 已归位到 `bc-iam/infrastructure`，且 `bc-course/**` 已不再引用 `eva-infra-shared` 中任何类型，因此可开始按 pom 逐点收敛 `eva-infra-shared` 依赖，避免“空模块依赖”掩盖后续 reactor 退场判断。
- ✅ 执行（单 pom，保持行为不变）：从 `bc-course/infrastructure/pom.xml` 移除 `edu.cuit:eva-infra-shared:${revision}` 依赖（仅收敛 Maven 编译边界；不改业务语义/副作用顺序）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`a96900c5`。

**2026-02-19（`eva-infra-shared` 收尾：单类搬运 `UserConverter` 归位到 `bc-iam/infrastructure`，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`UserConverter` 跨 BC 引用面已收敛为仅命中 `bc-iam/infrastructure`（`bc-course/**`、`bc-evaluation/**` 已证伪无引用），满足“单 BC 归位”的前置条件。
- ✅ 执行（单类搬运，保持行为不变）：将 `UserConverter` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.convertor.user` 与类内容不变，仅改变 Maven 模块归属），避免同 FQCN 多份导致 classpath 冲突。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。另：本刀因 `eva-infra-shared` 源码清零，需先 `mvn -pl :eva-infra-shared clean` 清理旧的 MapStruct `generated-sources`（仅构建产物，不入库）。
- 📌 代码落地：`8d5e580c`。

**2026-02-19（`UserConverter` 收尾：第 5 刀（单类），`CourseQueryRepository` 改注入 `UserEntityFieldExtractPort`，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`CourseQueryRepository` 内对 `UserConverter` 的唯一方法使用点为 `springUserEntityWithNameObject(Object)`（用于构造 teacher 的 `Supplier<?>` 桥对象）。
- ✅ 执行（单类，保持行为不变）：将 `bc-course/infrastructure` 的 `CourseQueryRepository` 依赖从 `UserConverter` 收敛为注入 `UserEntityFieldExtractPort`，并将 `springUserEntityWithNameObject` 调用改为走该 Port（Port Adapter 内部仍委托 `UserConverter.springUserEntityWithNameObject`，确保空值/异常/调用顺序不变）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`9eabba63`。

**2026-02-19（`UserConverter` 收尾：第 5 刀前置（编译闭合，单 pom），`bc-course-infra` 增加对 `bc-iam` 的依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：后续将 `bc-course/infrastructure` 的 `CourseQueryRepository` 从直接注入 `UserConverter` 收敛为注入 `bc-iam/application` 的 Port `UserEntityFieldExtractPort`（用于桥接 `springUserEntityWithNameObject`），因此需前置补齐 `bc-course-infra` 对 `bc-iam` 的 Maven 编译期依赖以闭合编译边界。
- ✅ 执行（单 pom，保持行为不变）：`bc-course/infrastructure/pom.xml` 增加 `edu.cuit:bc-iam:${revision}` 依赖（仅编译边界前置；不改任何业务语义/副作用顺序）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`3ca758c2`。

**2026-02-19（`UserConverter` 收尾：第 4 刀（单类），`EvaRecordQueryRepository` 改注入 `UserEntityFieldExtractPort`，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`EvaRecordQueryRepository` 内对 `UserConverter` 的唯一方法使用点为 `userIdOf(Object)`（用于在 `getEvaTaskEntities` 里按 teacherId 过滤匹配）。
- ✅ 执行（单类，保持行为不变）：将 `bc-evaluation/infrastructure` 的 `EvaRecordQueryRepository` 依赖从 `UserConverter` 收敛为注入 `UserEntityFieldExtractPort`，并将 `userIdOf` 调用改为走该 Port（Port Adapter 内部仍委托 `UserConverter.userIdOf`，确保空值/异常/调用顺序不变）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`ba84a0ca`。

**2026-02-19（`UserConverter` 收尾：第 3 刀（单类），`EvaTaskQueryRepository` 改注入 `UserEntityFieldExtractPort`，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`EvaTaskQueryRepository` 内对 `UserConverter` 的唯一方法使用点为 `userIdOf(Object)`（用于在 `getEvaTaskEntities` 里按 teacherId 过滤匹配）。
- ✅ 执行（单类，保持行为不变）：将 `bc-evaluation/infrastructure` 的 `EvaTaskQueryRepository` 依赖从 `UserConverter` 收敛为注入 `UserEntityFieldExtractPort`，并将 `userIdOf` 调用改为走该 Port（Port Adapter 内部仍委托 `UserConverter.userIdOf`，确保空值/异常/调用顺序不变）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`5128a78d`。

**2026-02-19（`UserConverter` 收尾：第 3 刀前置（编译闭合，单 pom），`bc-evaluation-infra` 增加对 `bc-iam` 的依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：后续将 `bc-evaluation/infrastructure` 的 `EvaTaskQueryRepository/EvaRecordQueryRepository` 从直接注入 `UserConverter` 收敛为注入 `bc-iam/application` 的 Port `UserEntityFieldExtractPort`，因此需前置补齐 `bc-evaluation-infra` 对 `bc-iam` 的 Maven 编译期依赖以闭合编译边界。
- ✅ 执行（单 pom，保持行为不变）：`bc-evaluation/infrastructure/pom.xml` 增加 `edu.cuit:bc-iam:${revision}` 依赖（仅编译边界前置；不改任何业务语义/副作用顺序）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`4b239adf`。

**2026-02-19（`UserConverter` 收尾：第 2 刀（Port Adapter），新增 `UserEntityFieldExtractPortImpl`（委托 `UserConverter`），保持行为不变）**
- ✅ 执行（单类，保持行为不变）：在 `bc-iam/infrastructure` 新增 Port Adapter：`edu.cuit.infra.bciam.adapter.UserEntityFieldExtractPortImpl`，实现 `UserEntityFieldExtractPort` 并内部直接委托 `UserConverter.userIdOf/springUserEntityWithNameObject`，保持异常/空值/调用顺序不变（本刀不改任何调用侧）。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`7e682804`。

**2026-02-19（`UserConverter` 收尾：第 1 刀（Port 定义），新增最小 Port `UserEntityFieldExtractPort`，保持行为不变）**
- ✅ Serena（证据化，保持行为不变）：`UserConverter` 引用面跨 `bc-course/infrastructure`（`CourseQueryRepository`）、`bc-evaluation/infrastructure`（`EvaTaskQueryRepository/EvaRecordQueryRepository`）与 `bc-iam/infrastructure`；外部 BC 实际使用的方法集合收敛为 `userIdOf(Object)` + `springUserEntityWithNameObject(Object)`。
- ✅ 执行（单类，保持行为不变）：在 `bc-iam/application` 新增最小 Port：`edu.cuit.bc.iam.application.port.UserEntityFieldExtractPort`，仅包含 `userIdOf(Object)` 与 `springUserEntityWithNameObject(Object)` 两个桥接方法，为后续“Port Adapter 委托 `UserConverter` + 调用侧逐点替换”做前置。
- 🧪 最小回归通过（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📌 代码落地：`e5b37188`。

**2026-02-19（交接巡检 + 口径修正：`eva-infra-shared` 余量=1，剩余阻塞为 `UserConverter`；保持行为不变）**
- ✅ 基线确认：分支 `ddd`；`git merge-base --is-ancestor 2e4c4923 HEAD` 退出码为 `0`；工作区仅存在未跟踪 `.mvnd/`、`.ssh_known_hosts`（不要提交）。
- 📊 证据快照（可复现）：`eva-infra-dal` Java=0、XML=0；`eva-infra-shared` Java=1（仅 `UserConverter`）；`bc-template/bc-course` 对 `infra.dal.database.mapper.eva` 命中=0。
- ✅ Serena（证据化）：`CourseBizConvertor/CourseConvertor` 已归位到 `bc-course/infrastructure` 且引用面不再跨 BC；当前剩余 `UserConverter` 引用面仍跨 `bc-course/bc-evaluation/bc-iam`，因此 `eva-infra-shared` 的最终退场需按“先 Port 收敛跨 BC 使用点 → 再单类搬运归位 → 再逐 pom 去依赖 → reactor 退场”推进。
- 🧪 最小回归（Java17）：`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`；已按约束降级使用 `mvn` 完成最小回归（`EvaRecordServiceImplTest/EvaStatisticsServiceImplTest` 通过）。
- 📝 文档同步：已更新 `NEXT_SESSION_HANDOFF.md`（0.10.1/0.11 提示词与主线）、`DDD_REFACTOR_PLAN.md`（`eva-infra-shared` 现状口径）、`docs/DDD_REFACTOR_BACKLOG.md`（4.3/6 的未收敛清单与下一刀建议），确保下个会话不会按旧口径误判。

**2026-02-19（bc-course/infrastructure 单类搬运：`CourseConvertor` 归位到 bc-course，保持行为不变）**
- ✅ 目标（保持行为不变）：完成 `CourseConvertor` 退场路线的“引用面收敛 + 单类搬运归位”，为后续 `eva-infra-shared` 退场创造条件（避免跨 BC 共享 MapStruct Convertor）。
- ✅ Serena（证据化，保持行为不变）：`CourseConvertor` 引用面已收敛为仅 `bc-course/infrastructure`（评教侧 `EvaTaskQueryRepository/EvaRecordQueryRepository` 已改为依赖 `CourseEntityConvertPort`，不再直接注入 `CourseConvertor`）。
- ✅ 执行（单类搬运，保持行为不变）：将 `CourseConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.convertor.course` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-shared` Java 余量由 `2` 变更为 `1`（仅剩 `UserConverter`）。
- 📌 代码落地：`e91badc4`。

**2026-02-19（bc-evaluation/infrastructure 单类：`EvaRecordQueryRepository` 改为依赖 `CourseEntityConvertPort`，保持行为不变）**
- ✅ 目标（保持行为不变）：继续收敛评教读侧对 `CourseConvertor` 的跨 BC 直接依赖，覆盖 `EvaRecordQueryRepository` 这一调用点。
- ✅ 执行（单类，保持行为不变）：将 `EvaRecordQueryRepository` 的注入从 `CourseConvertor` 改为 `CourseEntityConvertPort`，并逐点将 `toSingleCourseEntity/toSemesterEntity/toSubjectEntity/toCourseEntityWithTeacherObject` 调用改为走该 Port（由 bc-course 端口适配器委托既有 `CourseConvertor` 承接，确保映射与调用时机不变）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`307946d4`。

**2026-02-19（bc-evaluation/infrastructure 单类：`EvaTaskQueryRepository` 改为依赖 `CourseEntityConvertPort`，保持行为不变）**
- ✅ 目标（保持行为不变）：开始收敛评教读侧对 `CourseConvertor` 的跨 BC 直接依赖，优先从调用面更集中的 `EvaTaskQueryRepository` 入手。
- ✅ 执行（单类，保持行为不变）：将 `EvaTaskQueryRepository` 的注入从 `CourseConvertor` 改为 `CourseEntityConvertPort`，并逐点将 `toSingleCourseEntity/toSemesterEntity/toSubjectEntity/toCourseEntityWithTeacherObject` 调用改为走该 Port（由 bc-course 端口适配器委托既有 `CourseConvertor` 承接，确保映射与调用时机不变）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`c2c34a9a`。

**2026-02-19（bc-course/infrastructure 单类：新增 `CourseEntityConvertPortImpl`，保持行为不变）**
- ✅ 目标（保持行为不变）：完成 `CourseConvertor` 退场路线的第 2 刀，为后续替换评教侧 `EvaTaskQueryRepository/EvaRecordQueryRepository` 的调用侧注入做装配落点前置。
- ✅ 执行（单类，保持行为不变）：在 `bc-course/infrastructure` 新增端口适配器 `edu.cuit.app.bccourse.adapter.CourseEntityConvertPortImpl`，内部 **直接委托** `CourseConvertor` 的 `toCourseEntityWithTeacherObject/toSemesterEntity/toSubjectEntity/toSingleCourseEntity`，确保映射与调用时机一致。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`eb597366`。

**2026-02-19（bc-course/application 单类：新增 `CourseEntityConvertPort`，保持行为不变）**
- ✅ 目标（保持行为不变）：为后续收敛 `bc-evaluation/infrastructure` 对 `CourseConvertor` 的直接依赖做前置，将“课程 DO → 课程实体/单节课实体”的转换能力收敛为 `bc-course/application` 的最小 Port（不引入 `UserEntity` 编译期依赖，保留 `teacher` 的 `Supplier<?>` 桥接口径）。
- ✅ 执行（单类，保持行为不变）：新增 `bc-course/application` Port：`edu.cuit.bc.course.application.port.CourseEntityConvertPort`，承接 `toCourseEntityWithTeacherObject/toSemesterEntity/toSubjectEntity/toSingleCourseEntity`（签名与既有 `CourseConvertor` 对齐，便于后续调用侧替换）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`0a95da8f`。

**2026-02-19（bc-course/infrastructure 单类搬运：`CourseBizConvertor` 归位到 bc-course，保持行为不变）**
- ✅ 目标（保持行为不变）：完成 Convertor 退场路线的“搬运归位”节点，确保 `CourseBizConvertor` 的引用面已收敛为 `bc-course/**`（另有 `start` 测试 mock），不再跨 BC 复用。
- ✅ Serena（证据化，保持行为不变）：`CourseBizConvertor` 引用面已不再命中 `bc-evaluation/**`（评教侧调用点已收敛为 `SingleCourseCoConvertPort`；测试构造器重载也已去类型依赖）。
- ✅ 执行（单类搬运，保持行为不变）：将 `CourseBizConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.app.convertor.course` 与类内容不变，仅改变 Maven 模块归属；避免同 FQCN 多份导致 classpath 冲突）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-shared` Java 余量由 `3` 变更为 `2`（剩余：`CourseConvertor/UserConverter`）。
- 📌 代码落地：`a082b812`。

**2026-02-19（bc-evaluation/infrastructure 单类：清理 `MsgServiceImpl` 对 `CourseBizConvertor` 的编译期类型依赖，保持行为不变）**
- ✅ 目标（保持行为不变）：为下一刀“搬运 `CourseBizConvertor` 归位到 `bc-course/infrastructure`”做前置，确保 `bc-evaluation/**` 不再编译期依赖该 Convertor 的类型。
- ✅ 执行（单类，保持行为不变）：移除 `MsgServiceImpl` 对 `CourseBizConvertor` 的 import 与构造器参数类型依赖；测试构造器重载改为接收 `Object` 并通过反射调用 `toSingleCourseCO(SingleCourseEntity,Integer)` 来构造 `SingleCourseCoConvertPort`，避免对 Convertor 形成编译期耦合（异常文案/日志/副作用顺序不变）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`9f7d27b0`。

**2026-02-19（bc-evaluation/infrastructure 单类：`MsgServiceImpl` 改为依赖 `SingleCourseCoConvertPort`，保持行为不变）**
- ✅ 目标（保持行为不变）：完成 Convertor 退场路线的第 3 刀，将评教侧 `MsgServiceImpl` 对课程转换的依赖从“直接注入 `CourseBizConvertor`”收敛为“注入 `SingleCourseCoConvertPort` 并调用”（转换仍由 bc-course 端口适配器委托既有 Convertor 承接）。
- ✅ 执行（单类，保持行为不变）：`MsgServiceImpl.toEvaResponseMsg/getSingleCourseByTaskId` 的 `SingleCourseEntity -> SingleCourseCO` 转换改为调用 `singleCourseCoConvertPort.toSingleCourseCO(...)`；同时用显式构造器替代 Lombok 生成构造器，并用 `@Autowired` 标记 Spring 注入入口，避免多构造器导致装配歧义（异常文案/日志文案/副作用顺序不变）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`65a2e261`。

**2026-02-19（bc-course/application 单类：新增 `SingleCourseCoConvertPort`，保持行为不变）**
- ✅ 目标（保持行为不变）：为后续把 `bc-evaluation/infrastructure` 对 `CourseBizConvertor` 的依赖收敛为“调用 `bc-course/application` Port 拿 `SingleCourseCO`”做前置（避免跨 BC 直接注入 Convertor）。
- ✅ Serena（证据化，保持行为不变）：`CourseBizConvertor` 在 `bc-evaluation/**` 的引用面仅命中 `MsgServiceImpl`（另有 `start` 的 `MsgServiceImplTest` mock）；其余命中集中在 `bc-course/infrastructure`。
- ✅ 执行（单类，保持行为不变）：新增 `bc-course/application` Port：`edu.cuit.bc.course.application.port.SingleCourseCoConvertPort`，方法签名：`SingleCourseCO toSingleCourseCO(SingleCourseEntity singleCourseEntity, Integer evaNum)`。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`32c458e7`。

**2026-02-19（bc-course/infrastructure 单类：新增 `SingleCourseCoConvertPortImpl`，保持行为不变）**
- ✅ 目标（保持行为不变）：完成第 2 刀，为后续替换 `bc-evaluation/infrastructure` 调用侧做装配落点前置（调用链路将从“直接注入 Convertor”逐步收敛为“注入 Port 并由 bc-course 端口适配器承接”）。
- ✅ 执行（单类，保持行为不变）：在 `bc-course/infrastructure` 新增端口适配器 `edu.cuit.app.bccourse.adapter.SingleCourseCoConvertPortImpl`，内部 **直接委托** `CourseBizConvertor.toSingleCourseCO(...)`，确保映射行为一致。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；`mvnd` 启动阶段仍报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`fd28bbb9`。

**2026-02-19（仅文档：证据口径复核 + Serena 证据化盘点 + 下一步计划明确；代码/行为不变）**
- ✅ 起手复核（必须，口径固化）：`git branch --show-current`=`ddd`；`git merge-base --is-ancestor 2e4c4923 HEAD` 退出码=0；当前交接基线：`git rev-parse --short HEAD`=`27ceb374`；文档基线：`git log -n 1 -- NEXT_SESSION_HANDOFF.md` 指向同一提交（以 Git 为准，不在文内固化 commitId）。
- 📊 证据口径快照（避免口径漂移）：`eva-infra-dal` Java=0、XML=0；`eva-infra-shared` Java=3；`bc-template/**` 与 `bc-course/**` 对 `infra.dal.database.mapper.eva.*` 的编译期直连行数=0；stash 仍为 3 条（后续仅允许按“指定文件路径”逐刀 restore，禁止整包 pop）。
- 🧾 `eva-infra-shared` 当前残留（保持行为不变）：仅剩 3 个 Convertor（MapStruct），文件清单：
  - `eva-infra-shared/src/main/java/edu/cuit/app/convertor/course/CourseBizConvertor.java`
  - `eva-infra-shared/src/main/java/edu/cuit/infra/convertor/course/CourseConvertor.java`
  - `eva-infra-shared/src/main/java/edu/cuit/infra/convertor/user/UserConverter.java`
- ✅ Serena 证据化（引用面，避免猜测；保持行为不变）：
  - `CourseBizConvertor`：引用面命中 `bc-course/infrastructure`（`CourseDetailQueryPortImpl/ICourseDetailServiceImpl/IUserCourseServiceImpl/UserCourseDetailQueryExec` 等）与 `bc-evaluation/infrastructure`（`MsgServiceImpl`），以及 `start` 的 `MsgServiceImplTest`（mock）。
  - `CourseConvertor`：引用面命中 `bc-course/infrastructure`（`CourseQueryRepository`、`CourseImportExce` 等）与 `bc-evaluation/infrastructure`（`EvaRecordQueryRepository/EvaTaskQueryRepository`）。
  - `UserConverter`：引用面命中 `bc-iam/infrastructure`（`UserServiceImpl/RouterDetailFactory` 与多处适配器）、`bc-course/infrastructure`（`CourseQueryRepository`）与 `bc-evaluation/infrastructure`（`EvaRecordQueryRepository/EvaTaskQueryRepository`）。
- ⚠️ 关键结论（重要阻塞，保持行为不变）：上述 3 个 Convertor **不能直接下沉到 `shared-kernel`** —— 因其编译期依赖 `bc-course-domain/bc-iam-domain` 的实体类型（例如 `CourseEntity/SingleCourseEntity/UserEntity/MenuEntity/RoleEntity`），而 `bc-course-domain` 已显式依赖 `shared-kernel`（Maven 依赖链可复现），若让 `shared-kernel` 反向依赖 `bc-*-domain` 将形成 Maven 环依赖。
- 🎯 下一阶段主线（计划更新，保持行为不变）：将“瘦身 `eva-infra-shared`”从“直接搬运 Convertor”调整为“**先消除跨 BC 复用**”：
  - 先按 M1/M2 原则，用 `*-contract` Port 把 `bc-evaluation/**`、`bc-course/**` 对 `UserConverter/CourseConvertor/CourseBizConvertor` 的依赖逐点收敛为“调用目标 BC 的端口拿 CO/DTO”，每次只改 1 个文件闭环；
  - 待引用面收敛为“单 BC 独占”后，再把 Convertor 逐个归位到对应 BC 的 `infrastructure`（预计：`UserConverter` → `bc-iam`；`CourseConvertor/CourseBizConvertor` → `bc-course`），最终 `eva-infra-shared` 才具备退场条件（详见 0.10/0.11 的新主线提示词）。

**2026-02-17（shared-kernel 下沉，单类：`EvaLdapUtils` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：LDAP 子簇已完成 `EvaLdapProperties/LdapGroupDO/LdapGroupRepo/LdapConstants` 下沉到 `shared-kernel`，此时 `EvaLdapUtils` 的编译期依赖均已在 `shared-kernel` 内闭合；其引用面跨 `bc-iam/infrastructure` 与 `shared-kernel` 内多处 LDAP 类型，不满足“单 BC 归位”，因此按既定主线下沉到 `shared-kernel`（保持 `package`/类内容/静态初始化副作用顺序不变）。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-iam/infrastructure`（`LdapPersonGatewayImpl/UserDeletionPortImpl/UserUpdateGatewayImpl/LdapUserConvertor` 等）以及 `LdapConstants` 静态初始化链路。
- ✅ 执行（单类，保持行为不变）：将 `EvaLdapUtils` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.util` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-shared` Java 余量由 `4` 变更为 `3`（口径：`fd -t f -e java . eva-infra-shared/src/main/java | wc -l`）。
- 📌 代码落地：`05cc3039`。

**2026-02-17（shared-kernel 下沉，单类：`LdapConstants` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：在完成“`LdapConstants` 去 `EvaLdapUtils` 编译期互引”的解耦前置后，`LdapConstants` 已具备“单类搬运不引入 Maven 循环依赖”的条件；其引用面跨 `bc-iam/infrastructure` 与 `EvaLdapUtils`，不满足“单 BC 归位”，因此按既定主线下沉到 `shared-kernel`（保持 `package`/类内容不变）。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-iam/infrastructure`（`LdapPersonGatewayImpl`）与 `shared-kernel`（`EvaLdapUtils`）。
- ✅ 执行（单类，保持行为不变）：将 `LdapConstants` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.enums` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-shared` Java 余量由 `5` 变更为 `4`（口径：`fd -t f -e java . eva-infra-shared/src/main/java | wc -l`）。
- 📌 代码落地：`3dc2e8ff`。

**2026-02-17（LDAP 子簇解耦前置，单类：`LdapConstants` 去 `EvaLdapUtils` 编译期互引，保持行为不变）**
- ✅ 背景（保持行为不变）：`LdapConstants` 的静态初始化块直接访问 `EvaLdapUtils.evaLdapProperties`，导致 `LdapConstants` ↔ `EvaLdapUtils` 出现编译期互引；在“单类一刀”约束下，无法直接把 `LdapConstants` 或 `EvaLdapUtils` 单独下沉到 `shared-kernel`（会引入 Maven 循环依赖）。
- ✅ Serena（证据化，保持行为不变）：`LdapConstants` 存在 `import edu.cuit.infra.util.EvaLdapUtils;` 且静态块读取 `EvaLdapUtils.evaLdapProperties`；而 `EvaLdapUtils` 方法体又读取 `LdapConstants.USER_BASE_DN/GROUP_BASE_DN`。
- ✅ 执行（单类，保持行为不变）：将 `LdapConstants` 的静态初始化改为通过 `Class.forName("edu.cuit.infra.util.EvaLdapUtils") + 反射读取 public static 字段 evaLdapProperties` 获取配置值，**仍会触发 `EvaLdapUtils` 类初始化**，从而保持 `SpringUtil.getBean(...)` 的副作用顺序不变；同时消除对 `EvaLdapUtils` 的编译期依赖，为下一刀“单类搬运”铺路。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`45fa9651`。

**2026-02-17（shared-kernel 下沉，单类：`LdapGroupRepo` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`LdapGroupRepo` 为 Spring Data LDAP 的仓储接口（依赖 `LdapRepository`），被 `bc-iam/infrastructure` 的 `LdapPersonGatewayImpl` 与 `EvaLdapUtils` 复用；引用面跨模块，不满足“单 BC 归位”条件，因此按既定主线下沉到 `shared-kernel`（保持 `package`/签名不变）。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-iam/infrastructure`（`LdapPersonGatewayImpl` 注入）与 `shared-kernel`（`EvaLdapUtils` 静态初始化取 Bean）。
- ✅ 执行（单类，保持行为不变）：将 `LdapGroupRepo` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.ldap.repo` 与接口签名不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-shared` Java 余量由 `6` 变更为 `5`（口径：`fd -t f -e java . eva-infra-shared/src/main/java | wc -l`）。
- 📌 代码落地：`7ff087ad`。

**2026-02-17（编译闭合前置，单 pom：`shared-kernel` 补齐 `spring-data-ldap(optional)`，保持行为不变）**
- ✅ 背景（保持行为不变）：为后续将 LDAP Repo `LdapGroupRepo` 从 `eva-infra-shared` 下沉到 `shared-kernel` 做准备，其源码依赖 `org.springframework.data.ldap.repository.LdapRepository`，需要在 `shared-kernel` 侧显式具备 Spring Data LDAP 的编译期依赖来源。
- ✅ Serena（证据化，保持行为不变）：`LdapGroupRepo` 存在 `import org.springframework.data.ldap.repository.LdapRepository;`，因此下沉前需补齐 `spring-data-ldap`（或等价依赖）以闭合编译。
- ✅ 执行（单 pom，保持行为不变）：在 `shared-kernel/pom.xml` 增加 `org.springframework.data:spring-data-ldap`（`optional=true`）作为编译闭合前置，不改变任何业务逻辑。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`9dad61a8`。

**2026-02-17（shared-kernel 下沉，单类：`LdapGroupDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`LdapGroupDO` 为 LDAP ODM 注解数据对象，被 `bc-iam/infrastructure` 与 `eva-infra-shared` 的 LDAP Repo/工具类复用；引用面跨模块，不满足“单 BC 归位”条件，因此更适合下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-iam/infrastructure` 的 `LdapPersonGatewayImpl`（`Optional<LdapGroupDO>`）、`shared-kernel` 的 `LdapGroupRepo`、以及 `shared-kernel` 的 `EvaLdapUtils`。
- ✅ 执行（单类，保持行为不变）：将 `LdapGroupDO` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.ldap.dataobject` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次按约束使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-shared` Java 余量由 `7` 变更为 `6`（口径：`fd -t f -e java . eva-infra-shared/src/main/java | wc -l`）。
- 📌 代码落地：`03ecd906`。

**2026-02-17（编译闭合前置，单 pom：`shared-kernel` 补齐 `spring-ldap-core(optional)`，保持行为不变）**
- ✅ 背景（保持行为不变）：为后续将 LDAP DO/Repo（使用 Spring LDAP ODM 注解）从 `eva-infra-shared` 继续下沉到 `shared-kernel` 做准备，需要在 `shared-kernel` 显式具备 `org.springframework.ldap.odm.annotations.*` 等注解依赖。
- ✅ Serena（证据化，保持行为不变）：`LdapGroupDO` 使用 `@Entry/@Attribute/@Id`（Spring LDAP ODM 注解），因此下沉前需补齐 `spring-ldap-core` 编译依赖来源。
- ✅ 执行（单 pom，保持行为不变）：在 `shared-kernel/pom.xml` 增加 `org.springframework.ldap:spring-ldap-core`（`optional=true`）作为编译闭合前置，不改变任何业务逻辑。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次仍按约束使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`473b48a7`。

**2026-02-17（shared-kernel 下沉，单类：`EvaLdapProperties` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`EvaLdapProperties` 为独立配置类（`@ConfigurationProperties`），仅被 LDAP 工具类 `EvaLdapUtils` 引用，适合作为“单类下沉 shared-kernel”的低风险瘦身切入点。
- ✅ Serena（证据化，保持行为不变）：引用面仅命中 `shared-kernel` 的 `EvaLdapUtils`（未命中其它 BC/模块），因此下沉不会扩大 BC 依赖边界。
- ✅ 执行（单类，保持行为不变）：将 `EvaLdapProperties` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.property` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次仍按约束使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-shared` Java 余量由 `8` 变更为 `7`（口径：`fd -t f -e java . eva-infra-shared/src/main/java | wc -l`）。
- 📌 代码落地：`666a1b6d`。

**2026-02-17（编译闭合纠偏，单 pom：为 `bc-audit-infra` 显式补齐 mapstruct 依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`bc-audit/infrastructure` 中 `LogBizConvertor/LogConverter` 使用 `org.mapstruct.*` 注解；此前在一次完整重编译的最小回归中暴露出缺失 MapStruct 编译期依赖的错误（属于“依赖传递掩盖”的隐患）。
- ✅ Serena（证据化，保持行为不变）：在 `bc-audit/**` 范围内可复现 `import org.mapstruct.*` 的引用点（`LogBizConvertor` 与 `LogConverter`）。
- ✅ 执行（单 pom，保持行为不变）：在 `bc-audit/infrastructure/pom.xml` 显式增加 `org.mapstruct:mapstruct(${mapstruct.version})`，用于恢复编译闭合；不引入任何业务逻辑改动。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次仍按约束使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`5e6721c9`。

**2026-02-17（编译闭合纠偏，单 pom：为 `bc-messaging` 显式补齐 websocket/mapstruct 依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`bc-messaging/pom.xml` 移除 `eva-infra-shared` 后，存在“之前依赖传递带来的编译期依赖隐含”问题；在一次完整重编译（最小回归）中暴露出缺失 `org.mapstruct.*` 与 `org.springframework.web.socket.*` 的编译错误。
- ✅ 执行（单 pom，保持行为不变）：在 `bc-messaging/pom.xml` 显式补齐 `spring-boot-starter-websocket` 与 `org.mapstruct:mapstruct(${mapstruct.version})`，用于恢复编译闭合；不引入任何业务逻辑改动。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次仍按约束使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`bf0c3455`。

**2026-02-17（依赖收敛，单 pom：`bc-messaging` 去 `eva-infra-shared` 依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`bc-messaging` 已完成 websocket 支撑类（`MessageChannel/UriUtils/WebsocketManager`）与消息 Convertor/Mapper/DO 的归位；其编译期不再需要 `eva-infra-shared` 提供任何类型。
- ✅ Serena（证据化，保持行为不变）：在 `bc-messaging/**` 范围内未发现对 `edu.cuit.infra.convertor.user.*` / `edu.cuit.infra.convertor.course.*` / `edu.cuit.infra.dal.ldap.*` 等 `eva-infra-shared` 残留支撑类的引用。
- ✅ 执行（单 pom，保持行为不变）：移除 `bc-messaging/pom.xml` 中 `eva-infra-shared` 依赖（保留 `shared-kernel` 显式依赖以维持编译闭合）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次仍按约束使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`bdd9527d`。

**2026-02-17（依赖收敛，单 pom：`bc-audit-infra` 去 `eva-infra-shared` 依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：审计侧 `bc-audit/infrastructure` 已完成 `SysLog*Mapper/DO` 与 `LogConverter` 归位；其对 `QueryUtils/PaginationConverter/EntityFactory` 等通用支撑类的引用当前均由 `shared-kernel` 承接，因此 `eva-infra-shared` 依赖可收敛。
- ✅ Serena（证据化，保持行为不变）：在 `bc-audit/**` 范围内未发现对 `edu.cuit.infra.convertor.user.*` / `edu.cuit.infra.dal.ldap.*` 等 `eva-infra-shared` 残留支撑类的引用；现有 `import edu.cuit.infra.convertor.EntityFactory`、`import edu.cuit.infra.util.QueryUtils`、`import edu.cuit.infra.convertor.PaginationConverter` 均已由 `shared-kernel` 提供。
- ✅ 执行（单 pom，保持行为不变）：将 `bc-audit/infrastructure/pom.xml` 中 `eva-infra-shared` 依赖移除，并改为显式依赖 `shared-kernel` 以维持编译闭合。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次仍按约束使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`10b0af75`。

**2026-02-17（依赖收敛，单 pom：`bc-template-infra` 去 `eva-infra-shared` 依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`bc-template/infrastructure` 未引用 `eva-infra-shared` 内的 Convertor/LDAP 支撑类；此前依赖仅用于“经由传递依赖拿到 shared-kernel 的 DO 类型”。
- ✅ Serena（证据化，保持行为不变）：在 `bc-template/**` 范围内未发现对 `edu.cuit.infra.convertor.*` / `edu.cuit.infra.dal.ldap.*` / `edu.cuit.infra.util.EvaLdapUtils` 等的引用；现有 `import edu.cuit.infra.dal.database.dataobject.eva.*` 实际来源为 `shared-kernel`。
- ✅ 执行（单 pom，保持行为不变）：将 `bc-template/infrastructure/pom.xml` 中 `eva-infra-shared` 依赖移除，并改为显式依赖 `shared-kernel` 以维持编译闭合。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次仍按约束使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`b93f8719`。

**2026-02-17（支撑类归位，单类：`MenuConvertor` 归位到 `bc-iam/infrastructure`，保持行为不变）**
- ✅ 背景（保持行为不变）：在前置清理 `bc-course/infrastructure` 中对 `MenuConvertor` 的无用 import 后，Serena 证据化确认 `MenuConvertor` 引用面仅命中 IAM 基础设施（`bc-iam/infrastructure`），满足“单 BC 归位”条件。
- ✅ 执行（单类，保持行为不变）：将 `MenuConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.convertor.user` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-shared` Java 余量由 `9` 变更为 `8`（口径：`fd -t f -e java . eva-infra-shared/src/main/java | wc -l`）。
- 📌 代码落地：`06690eee`。

**2026-02-17（引用面收敛，单类：移除 `CourseQueryRepository` 无用 `MenuConvertor` import，保持行为不变）**
- ✅ 背景（保持行为不变）：Serena 证伪 `bc-course/infrastructure` 的 `CourseQueryRepository` 中 `MenuConvertor` 仅出现在 import，未被任何字段/方法引用；该无用 import 会在引用面统计时造成“跨 BC 引用面扩大”的误判。
- ✅ 执行（单类，保持行为不变）：移除 `CourseQueryRepository` 对 `MenuConvertor` 的无用 import，不改变任何逻辑与副作用顺序。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`4b9f5855`。

**2026-02-17（目录退场前置，单文件：删除 `eva-infra-dal/pom.xml`，保持行为不变）**
- ✅ 背景（保持行为不变）：`eva-infra-dal` 已从 root reactor 退场，且全仓库 `**/pom.xml` 已无对 `eva-infra-dal` 的 Maven 依赖；因此其模块 `pom.xml` 仅作为历史遗留入口，继续保留反而会干扰“目录退场/依赖清零”的证据口径。
- ✅ 执行（单文件，保持行为不变）：删除 `eva-infra-dal/pom.xml`（不改任何业务语义与副作用顺序）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`1c037aeb`。

**2026-02-17（reactor 退场，单 pom：root `pom.xml` 移除 `<module>eva-infra-dal</module>`，保持行为不变）**
- ✅ 背景（保持行为不变）：Serena 证伪全仓库 `**/pom.xml` 已无对 `eva-infra-dal` 的 `<artifactId>eva-infra-dal</artifactId>` 依赖；且 `eva-infra-dal` Java/XML 残留已清零，因此可将其从 root reactor 退场，降低后续“目录退场/依赖清零”工作量与误判风险。
- ✅ 执行（单 pom，保持行为不变）：从根 `pom.xml` 的 `<modules>` 中移除 `eva-infra-dal`，不改任何业务语义与副作用顺序。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`dfe4e5f3`。

**2026-02-17（依赖收敛，单 pom：`eva-infra-shared/pom.xml` 去 `eva-infra-dal` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：Serena 证据化确认 `eva-infra-shared/src/main/java` 仍存在 `import edu.cuit.infra.dal.*`（主要为 `dataobject.*` 包名稳定性与 LDAP 子簇），但相关类型已下沉至 `shared-kernel` 或归位到对应 BC；因此 `eva-infra-shared` 不再需要通过 Maven 直依赖 `eva-infra-dal` 来获得编译期类型。
- ✅ 执行（单 pom，保持行为不变）：移除 `eva-infra-shared/pom.xml` 中对 `eva-infra-dal` 的 Maven 编译期直依赖，避免无效依赖掩盖后续“reactor 退场”判断。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`5975ab10`。

**2026-02-17（shared-kernel 下沉，单类：`EntityFactory` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：Serena 证据化确认 `EntityFactory` 引用面跨 `bc-iam/**`、`bc-evaluation/**`、`bc-audit/**`、`bc-messaging/**` 且仍被 `eva-infra-shared` 多个 Convertor/BizConvertor 使用，不满足“单 BC/单模块归位”的条件。
- ✅ 执行（单类，保持行为不变）：将 `EntityFactory` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.convertor` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；本次 `mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `1` 变更为 `0`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`86754419`。

**2026-02-17（编译闭合前置，单 pom：`shared-kernel/pom.xml` 补齐 `mapstruct(optional)`，保持行为不变）**
- ✅ 背景（保持行为不变）：下一刀计划将 `edu.cuit.infra.convertor.EntityFactory` 下沉到 `shared-kernel`（引用面跨多个 BC），但其源码依赖 MapStruct 注解（`@TargetType/@Named`）。为避免“单类搬运”时出现编译断裂，先在 `shared-kernel/pom.xml` 补齐 `org.mapstruct:mapstruct(optional)` 依赖。
- 🧪 最小回归通过（Java17）：按 0.11 命令执行；`mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📌 代码落地：`a0030694`。

**2026-02-17（shared-kernel 下沉，单类：`CourseTypeDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`CourseTypeDO` 被课程域 `bc-course/**` 多处基础设施实现使用，且 `eva-infra-shared` 的 `CourseConvertor` 也依赖该类型（用于类型转换/装配），不满足“单 BC 引用才允许归位到对应 infrastructure”的约束；为缩减 `eva-infra-dal` 表面积并避免后续搬运引发编译断裂，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-course/infrastructure`（课程类型增删改查/课程查询/导入等链路）与 `eva-infra-shared`（`CourseConvertor`）等；未发现其它 BC（如 `bc-evaluation/**`）的直接引用点。
- ✅ 执行（单类，保持行为不变）：将 `CourseTypeDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：命令以 0.11 为准；本次 `mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `2` 变更为 `1`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`6c66a6dc`。

**2026-02-17（shared-kernel 下沉，单类：`EvaTaskDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`EvaTaskDO` 被评教域（`bc-evaluation/**`）、课程域（`bc-course/**`）与模板域（`bc-template/**`）多处基础设施实现复用，属于“跨 BC 共享的数据对象”；为缩减 `eva-infra-dal` 表面积，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-evaluation/infrastructure`（任务发布/撤回/查询/统计链路）、`bc-course/infrastructure`（课程写侧分配/删除/查询/推荐链路）、`bc-template/infrastructure`（模板锁定链路）等；因此不满足“单 BC 引用才允许归位到对应 infrastructure”的约束。
- ✅ 执行（单类，保持行为不变）：将 `EvaTaskDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.eva` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：命令以 0.11 为准；本次 `mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `3` 变更为 `2`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`49fa7eef`。

**2026-02-17（shared-kernel 下沉，单类：`FormRecordDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`FormRecordDO` 被评教域（`bc-evaluation/**`）、课程域（`bc-course/**`）与模板域（`bc-template/**`）多处基础设施实现复用，属于“跨 BC 共享的数据对象”；为缩减 `eva-infra-dal` 表面积，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-evaluation/infrastructure`（记录查询/统计/提交评教等链路）、`bc-course/infrastructure`（课程删除/查询/导入链路）、`bc-template/infrastructure`（模板锁定链路）等；因此不满足“单 BC 引用才允许归位到对应 infrastructure”的约束。
- ✅ 执行（单类，保持行为不变）：将 `FormRecordDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.eva` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：命令以 0.11 为准；本次 `mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `4` 变更为 `3`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`83b44804`。

**2026-02-17（shared-kernel 下沉，单类：`FormTemplateDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`FormTemplateDO` 被课程域（`bc-course/**`）与评教域（`bc-evaluation/**`）多处基础设施实现复用，且 `eva-infra-shared` 的 `CourseBizConvertor` 也引用该类型，属于“跨 BC 共享的数据对象”；为缩减 `eva-infra-dal` 表面积，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-evaluation/infrastructure`（模板增删改查/提交评教链路）、`bc-course/infrastructure`（课程查询/导入链路）、`eva-infra-shared`（`CourseBizConvertor`）等；因此不满足“单 BC 引用才允许归位到对应 infrastructure”的约束。
- ✅ 执行（单类，保持行为不变）：将 `FormTemplateDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.eva` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：命令以 0.11 为准；本次 `mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `5` 变更为 `4`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`6b66340b`。

**2026-02-17（shared-kernel 下沉，单类：`CourOneEvaTemplateDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`CourOneEvaTemplateDO` 被课程域（`bc-course/**`）、模板域（`bc-template/**`）与评教域（`bc-evaluation/**`）多处基础设施实现复用，属于“跨 BC 共享的数据对象”；为缩减 `eva-infra-dal` 表面积，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-evaluation/infrastructure`（模板查询/提交评教等链路）、`bc-course/infrastructure`（课程查询/导入链路）、`bc-template/infrastructure`（模板锁定链路）与 `eva-infra-shared`（`CourseConvertor`）等；因此不满足“单 BC 引用才允许归位到对应 infrastructure”的约束。
- ✅ 执行（单类，保持行为不变）：将 `CourOneEvaTemplateDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.eva` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：命令以 0.11 为准；本次 `mvnd` 仍在启动阶段报 `java.lang.ExceptionInInitializerError`，已按约束降级使用 `mvn` 完成最小回归（测试用例集保持不变）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `6` 变更为 `5`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`e48c63b4`。

**2026-02-17（shared-kernel 下沉，单类：`SubjectDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`SubjectDO` 被 `bc-course/**` 与 `bc-evaluation/**` 多处基础设施实现复用，属于“跨 BC 共享的数据对象”；为缩减 `eva-infra-dal` 表面积，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-course/infrastructure`（导入/写侧仓储/查询等链路）、`bc-evaluation/infrastructure`（任务/记录/统计查询仓储）等；未发现必须保留在 `eva-infra-dal` 的编译期约束。
- ✅ 执行（单类，保持行为不变）：将 `SubjectDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `7` 变更为 `6`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`1a6ff62c`。

**2026-02-16（shared-kernel 下沉，单类：`SemesterDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`SemesterDO` 同时被 `bc-course/**` 与 `bc-evaluation/**` 多处基础设施实现复用（并被 `eva-infra-shared` 的 `CourseConvertor` 承接），属于“跨 BC 共享的数据对象”；为缩减 `eva-infra-dal` 表面积，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-course/infrastructure`（导入/查询/推荐等链路）、`bc-evaluation/infrastructure`（任务发布链路）、`eva-infra-shared`（`CourseConvertor`）等；未发现必须保留在 `eva-infra-dal` 的编译期约束。
- ✅ 执行（单类，保持行为不变）：将 `SemesterDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `8` 变更为 `7`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`e0680e8a`。

**2026-02-16（shared-kernel 下沉，单类：`CourseDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`CourseDO` 被 `bc-course/**` 写侧仓储/端口适配器与 `bc-evaluation/**` 查询仓储复用，属于“跨 BC 共享的数据对象”；为缩减 `eva-infra-dal` 表面积，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-course/infrastructure`（课程写侧仓储与 `CourseMapper`）、`bc-evaluation/infrastructure`（任务/记录/统计/模板查询仓储）等；未发现必须保留在 `eva-infra-dal` 的编译期约束。
- ✅ 执行（单类，保持行为不变）：将 `CourseDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `9` 变更为 `8`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`d822340e`。

**2026-02-16（shared-kernel 下沉，单类：`CourInfDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`CourInfDO` 被 `bc-course/**` 写侧仓储/端口适配器与 `bc-evaluation/**` 读侧查询仓储/端口查询结果承接复用（并被测试使用），属于“跨 BC 共享的数据对象”；为缩减 `eva-infra-dal` 表面积，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-course/infrastructure`、`bc-evaluation/infrastructure` 与 `start` 测试等；未发现必须保留在 `eva-infra-dal` 的编译期约束。
- ✅ 执行（单类，保持行为不变）：将 `CourInfDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `10` 变更为 `9`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`542e0231`。

**2026-02-16（shared-kernel 下沉，单类：`SysUserDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserDO` 被 IAM 基础设施（用户增删改查/LDAP 转换等）与 `eva-infra-shared` 的 MapStruct 转换器复用，属于“跨 BC 共享的数据对象”；为缩减 `eva-infra-dal` 表面积，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-iam/infrastructure`（`User*PortImpl`/`SysUserMapper`/`UserUpdateGatewayImpl`/`DepartmentGatewayImpl`/`LdapUserConvertor` 等）与 `eva-infra-shared`（`UserConverter`/`CourseConvertor`），未发现必须保留在 `eva-infra-dal` 的编译期约束。
- ✅ 执行（单类，保持行为不变）：将 `SysUserDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.user` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `11` 变更为 `10`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`31e157cd`。

**2026-02-16（shared-kernel 下沉，单类：`SysRoleDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysRoleDO` 被 IAM 写侧/读侧基础设施与 `eva-infra-shared` 的 MapStruct 转换器复用，属于“跨 BC 共享的数据对象”；为缩减 `eva-infra-dal` 表面积，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-iam/infrastructure`（`RoleWritePortImpl`/`RoleQueryGatewayImpl`/`UserQueryGatewayImpl` 等）与 `eva-infra-shared`（`UserConverter`），未发现其它 BC 直接引用。
- ✅ 执行（单类，保持行为不变）：将 `SysRoleDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.user` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `12` 变更为 `11`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`c5ba98b1`。

**2026-02-16（shared-kernel 下沉，单类：`SysMenuDO` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysMenuDO` 同时被 IAM 写侧/读侧基础设施与 `eva-infra-shared` 的 MapStruct 转换器复用，属于“跨 BC 共享的数据对象”；为缩减 `eva-infra-dal` 表面积，将其下沉到 `shared-kernel`。
- ✅ Serena（证据化，保持行为不变）：引用面命中 `bc-iam/infrastructure`（`MenuWritePortImpl`/`MenuQueryGatewayImpl`/`RoleQueryGatewayImpl`/`SysMenuMapper` 等）与 `eva-infra-shared`（`MenuConvertor`/`UserConverter`），未发现其它 BC 直接引用。
- ✅ 执行（单类，保持行为不变）：将 `SysMenuDO` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.dal.database.dataobject.user` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `13` 变更为 `12`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`a9141bfe`。

**2026-02-16（shared-kernel 下沉，单类：`QueryUtils` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`QueryUtils` 被多个 BC 的查询仓储/旧 gateway 复用，属于跨 BC 共享的查询工具类；为缩减 `eva-*` 技术切片表面积，将其下沉到 `shared-kernel`。
- ✅ 执行（单类，保持行为不变）：将 `QueryUtils` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.util` 与类内容不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`f1638d20`。

**2026-02-16（shared-kernel 下沉，单类：`PaginationConverter` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`PaginationConverter` 被多个 BC 查询仓储/旧 gateway 复用，属于跨 BC 共享的分页转换器；为缩减 `eva-*` 技术切片表面积，将其下沉到 `shared-kernel`。
- ✅ 执行（单类，保持行为不变）：将 `PaginationConverter` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.convertor`、类内容与 Spring 注解不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`0427f1d4`。

**2026-02-16（shared-kernel 下沉，单类：`PaginationBizConvertor` 归位到 `shared-kernel`，保持行为不变）**
- ✅ 背景（保持行为不变）：`PaginationBizConvertor` 被 `bc-audit/bc-course/bc-iam/start` 多处复用，属于“跨 BC 共享的纯转换器”更适合沉淀到 `shared-kernel`，以减少 `eva-*` 技术切片表面积。
- ✅ 执行（单类，保持行为不变）：将 `PaginationBizConvertor` 从 `eva-infra-dal` 下沉到 `shared-kernel`（保持 `package edu.cuit.app.convertor`、类内容与 Spring 注解不变，仅改变 Maven 模块归属）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`67a61098`。

**2026-02-16（dal 拆散试点，单类：`SysUserMapper.java` 归位到 `bc-iam/infrastructure`，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper.xml` 已归位到 `bc-iam/infrastructure`，但此前 `SysUserMapper.java` 仍在 `eva-infra-dal`。为推进 “`eva-infra-dal` 按 BC 拆散” 且避免 classpath 重复，需将 `SysUserMapper.java` 单类搬运归位到 `bc-iam/infrastructure` 并删除旧位置同名类。
- ✅ 执行（单类，保持行为不变）：将 `SysUserMapper.java` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.user`、接口签名与注解不变）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`ff591e46`。

**2026-02-16（SysUserMapper 归位前置，单类：`CourseQueryRepository` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`CourseQueryRepository` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectOne(Wrapper)` / `selectList(Wrapper)` / `selectById(Serializable)`，以保持异常文案与副作用顺序完全不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`ec1da722`。

**2026-02-16（SysUserMapper 归位前置，单类：`CourseRecommendExce` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`CourseRecommendExce` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectOne(Wrapper)` / `selectById(Serializable)` / `selectList(Wrapper)`，以保持异常文案、缓存/日志与副作用顺序完全不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`cccf259b`。

**2026-02-16（SysUserMapper 归位前置，单类：`CourseImportExce` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`CourseImportExce` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectOne(Wrapper)`，以保持异常文案、缓存/日志与副作用顺序完全不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`e3cf8426`。

**2026-02-16（SysUserMapper 归位前置，单类：`UpdateSingleCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`UpdateSingleCourseRepositoryImpl` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectById(Serializable)`，以保持异常文案、缓存/日志与副作用顺序完全不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`15135886`。

**2026-02-16（SysUserMapper 归位前置，单类：`UpdateSelfCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`UpdateSelfCourseRepositoryImpl` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectOne(Wrapper)` / `selectById(Serializable)`，以保持异常文案、缓存/日志与副作用顺序完全不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`17c4bd19`。

**2026-02-16（SysUserMapper 归位前置，单类：`UpdateCourseInfoRepositoryImpl` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`UpdateCourseInfoRepositoryImpl` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectById(Serializable)`，以保持异常文案、缓存失效 key 与副作用顺序不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`d5415b0a`。

**2026-02-16（SysUserMapper 归位前置，单类：`DeleteSelfCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`DeleteSelfCourseRepositoryImpl` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectOne(Wrapper)`，以保持异常文案、日志文案与副作用顺序不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`71060e69`。

**2026-02-16（SysUserMapper 归位前置，单类：`DeleteCoursesRepositoryImpl` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`DeleteCoursesRepositoryImpl` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectById(Serializable)`，以保持异常文案、日志文案与副作用顺序不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`b193156d`。

**2026-02-16（SysUserMapper 归位前置，单类：`DeleteCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`DeleteCourseRepositoryImpl` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectById(Serializable)`，以保持日志文案、异常文案与副作用顺序不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`9dd8a7d1`。

**2026-02-15（SysUserMapper 归位前置，单类：`AssignEvaTeachersRepositoryImpl` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`AssignEvaTeachersRepositoryImpl` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectById(Serializable)`，以保持日志文案、异常文案与副作用顺序不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`712c4eb7`。

**2026-02-15（SysUserMapper 归位前置，单类：`EvaRecordQueryRepository` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`EvaRecordQueryRepository` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectById(Serializable)` / `selectList(...)`，以保持查询/遍历顺序、异常文案与副作用顺序不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`0e190e6c`。
- 📌 文档同步与推送已完成：代码提交 `0e190e6c`；文档提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（本次收尾已 push）。

**2026-02-15（SysUserMapper 归位前置，单类：`EvaStatisticsQueryRepository` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`EvaStatisticsQueryRepository` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectById(Serializable)` / `selectList(...)` + `getId()`/`getName()`/`getDepartment()`，以保持查询/遍历顺序、缓存 key 与副作用顺序不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`28338204`。
- 📌 文档同步与推送已完成：代码提交 `28338204`；文档提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（本次收尾已 push）。

**2026-02-15（SysUserMapper 归位前置，单类：`EvaTaskQueryRepository` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`EvaTaskQueryRepository` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectById(Serializable)` / `selectList(...)` + `getId()`/`getName()`，以保持查询/遍历顺序、异常文案与副作用顺序不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`7caaec02`。
- 📌 文档同步与推送已完成：代码提交 `7caaec02`；文档提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（本次收尾已 push）。

**2026-02-15（SysUserMapper 归位前置，单类：`SubmitEvaluationRepositoryImpl` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`SubmitEvaluationRepositoryImpl` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectById(Serializable)` + `getName()`；缓存失效 key 与副作用顺序保持不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`055db608`。
- 📌 文档同步与推送已完成：代码提交 `055db608`；文档提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（本次收尾已 push）。

**2026-02-15（SysUserMapper 归位前置，单类：`PostEvaTaskRepositoryImpl` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`PostEvaTaskRepositoryImpl` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`），并通过反射调用 `selectById(Serializable)` + `getId()`/`getName()`，以保持查询次数/缓存 key 与副作用顺序不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`89fbc439`。
- 📌 文档同步与推送已完成：代码提交 `89fbc439`；文档提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（本次收尾已 push）。

**2026-02-15（SysUserMapper 归位前置，单类：`DeleteEvaRecordRepositoryImpl` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`DeleteEvaRecordRepositoryImpl` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`）并通过反射调用 `selectById(Serializable)` + `getName()`；`LogUtils.logContent(...)` 的文案与调用时机保持不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`c0f94380`。
- 📌 文档同步与推送已完成：代码提交 `c0f94380`；文档提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（本次收尾已 push）。

**2026-02-15（SysUserMapper 归位前置，单类：`EvaUpdateGatewayImpl` 去 `SysUserMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。归位前必须逐个清理非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`EvaUpdateGatewayImpl` 将 `SysUserMapper` 注入改为按 `beanName` 注入 `Object`（`@Qualifier("sysUserMapper")`）并通过反射调用 `selectById(Serializable)` + `getName()`；`localCacheManager.invalidateCache(...)` 的 key 计算与调用顺序保持不变。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`364c63a0`。
- 📌 文档同步与推送已完成：代码提交 `364c63a0`；文档提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（本次收尾已 push）。

**2026-02-15（SysUserMapper 归位前置，单类：`CourseConvertor` 去 `SysUserMapper` import，保持行为不变）**
- ✅ 背景（保持行为不变）：`SysUserMapper` 当前仍在 `eva-infra-dal`，但其归位目标是 `bc-iam/infrastructure`。在归位前必须清理所有非 IAM 模块对其编译期依赖，否则会导致编译失败。
- ✅ 执行（单类，保持行为不变）：`CourseConvertor` 中仅存在 `SysUserMapper` 的无用 import（未参与 `@Mapper(uses=...)`，亦无任何符号引用）。本刀仅移除该 import，避免后续搬运 `SysUserMapper` 时 `eva-infra-shared` 编译失败。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`1e0af235`。

**2026-02-15（DAL 拆散试点，逐类归位：`FormTemplateMapper` → `bc-evaluation-infra`，保持行为不变）**
- ✅ 类搬运：将 `FormTemplateMapper` 从 `eva-infra-dal` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.eva` 不变，仅改变 Maven 模块归属；避免 classpath 重复）。
- ✅ Serena（证据化）：引用面命中 `bc-evaluation/infrastructure`（模板增删改/查询、提交评教等）；
  且为解除 `eva-infra-shared` 的编译期阻塞，已先行完成 `CourseBizConvertor` 去 `FormTemplateMapper` 编译期依赖（见同日条目 `cd014391`，保持行为不变）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `18` 变更为 `17`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`46b77dc4`。

**2026-02-15（前置解耦，单类：`CourseBizConvertor` 去 `FormTemplateMapper` 编译期依赖，保持行为不变）**
- ✅ 背景（保持行为不变）：`FormTemplateMapper` 仍被 `eva-infra-shared` 的 `CourseBizConvertor` 编译期引用；若直接将 `FormTemplateMapper` 从 `eva-infra-dal` 归位到 `bc-evaluation/infrastructure`，会导致 `eva-infra-shared` 编译失败（且若改 pom 会引入循环依赖，禁止）。
- ✅ 执行（单类，保持行为不变）：将 `CourseBizConvertor` 的 `FormTemplateMapper` 注入改为按 `beanName` 注入 `Object` + 反射调用 `selectById(Serializable)`，以解除编译期依赖但保持 MyBatis 调用语义不变（`selectById` 调用次数/顺序保持不变）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`cd014391`。

**2026-02-15（DAL 拆散试点，逐类归位：`CourOneEvaTemplateMapper` → `bc-evaluation-infra`，保持行为不变）**
- ✅ 类搬运：将 `CourOneEvaTemplateMapper` 从 `eva-infra-dal` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.eva` 不变，仅改变 Maven 模块归属；避免 classpath 重复）。
- ✅ Serena（证据化）：引用面命中 `bc-evaluation/infrastructure`（评教写侧/读侧仓储）与 `start` 测试（Mock）；组合根仍按既有方式装配，不改任何业务语义/副作用顺序。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `19` 变更为 `18`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`d0eb9fa3`。

**2026-02-15（DAL 拆散试点，逐类归位：`SubjectMapper` → `bc-course-infra`，保持行为不变）**
- ✅ 类搬运：将 `SubjectMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.course` 不变，仅改变 Maven 模块归属；避免 classpath 重复）。
- ✅ Serena（证据化）：引用面命中 `bc-course/infrastructure` 与 `bc-evaluation/infrastructure`（课程增删改、导入/查询与评教读侧查询等）；评教侧已具备 `bc-course-infra` 的 Maven 依赖前置。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `20` 变更为 `19`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`3cf31869`。

**2026-02-15（DAL 拆散试点，逐类归位：`CourInfMapper` → `bc-course-infra`，保持行为不变）**
- ✅ 类搬运：将 `CourInfMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.course` 不变，仅改变 Maven 模块归属；避免 classpath 重复）。
- ✅ Serena（证据化）：引用面集中在 `bc-course/infrastructure`（课程增删改、时间片查询端口适配器等）与 `start` 测试（Mock），且 `start` 已显式依赖 `bc-course-infra`，因此无需额外 pom 前置。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `21` 变更为 `20`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`7837b0fb`。

**2026-02-15（DAL 拆散试点，逐类归位：`SemesterMapper` → `bc-course-infra`，保持行为不变）**
- ✅ 类搬运：将 `SemesterMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.course` 不变，仅改变 Maven 模块归属；避免 classpath 重复）。
- ✅ Serena（证据化）：引用面命中 `bc-course/infrastructure` 与 `bc-evaluation/infrastructure`（主要为课程导入/查询与评教任务发布/读侧查询）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `22` 变更为 `21`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`83c40eaa`。

**2026-02-15（DAL 拆散试点，逐类归位：`CourseMapper` → `bc-course-infra`，保持行为不变）**
- ✅ 类搬运：将 `CourseMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.course` 不变，仅改变 Maven 模块归属；避免 classpath 重复）。
- ✅ Serena（证据化）：引用面命中 `bc-course/infrastructure` 与 `bc-evaluation/infrastructure`；其中评教侧对 `bc-course-infra` 的依赖前置已在 `eb0bbbec` 完成（用于满足“单文件一刀”约束）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📊 口径更新（保持行为不变）：`eva-infra-dal` Java 余量由 `23` 变更为 `22`（口径：`fd -t f -e java . eva-infra-dal/src/main/java | wc -l`）。
- 📌 代码落地：`049be80f`。

**2026-02-15（编译闭合前置，单 pom：评教 infra 前置依赖 `bc-course-infra`，保持行为不变）**
- ✅ Serena（证据化）：确认 `bc-evaluation/infrastructure` 存在对课程域 `CourseMapper/SemesterMapper/CourInfMapper/SubjectMapper` 的编译期引用面；为满足“每次只改 1 个类/1 个 pom”的闭环约束，需先做依赖前置再逐类搬运。
- ✅ 依赖前置（单 pom）：在 `bc-evaluation/infrastructure/pom.xml` 显式增加对 `bc-course-infra` 的 Maven 编译期依赖（不改任何业务语义；缓存/日志/异常文案/副作用顺序完全不变）。
- 🧪 最小回归通过（Java17 + mvnd）：命令以 0.11 为准（`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`）。
- 📌 代码落地：`eb0bbbec`。

**2026-02-15（阶段性：进度汇报 + 排期固化 + 三文档同步；以下基线为当时记录，便于回溯）**
- ✅ 基线确认（以命令输出为准）：当前分支 `ddd`；`git rev-parse --short HEAD`=`1070453a`；`git merge-base --is-ancestor 2e4c4923 HEAD` 退出码为 `0`。
- ✅ 工作区状态：无已暂存/未暂存改动；仅存在未跟踪目录/文件：`.mvnd/`、`.ssh_known_hosts`（不要提交）。
- 📦 未落地变更仍在 stash（后续必须按“单文件/单资源”逐刀 restore 闭环，严禁 `reset/checkout` 粗暴清理）：
  - `stash@{0}`：`wip: course-query-ports + eva-query-ports（不含XML）`
  - `stash@{1}`：`wip: remaining after mapper-decouple`
  - `stash@{2}`：`wip: dal-xml-relocate + course-query-ports`（历史整包，可能与已落地提交重复，restore 需更谨慎）
- 📊 口径快照（避免口径漂移，详见 0.10）：root reactor 仍包含 `eva-infra-dal/eva-infra-shared/eva-base`；`eva-infra-dal` 当前仍有 `7` 个 Java、`eva-infra-shared` 仍有 `9` 个 Java、`eva-base-common` 仍有 `2` 个 Java；且 `eva-infra-dal/src/main/resources` 已不存在（所有 XML 已按单资源归位到各 BC，后续证据命令需用“目录不存在=0”的口径）。
- 🎯 下一阶段主线（保持行为不变）：承接“课程/评教 5 个 DAL XML 已归位”，开始逐类把 `eva-infra-dal` 的 `Mapper/DO`（以及少量公共工具类）按 BC 归位到 `bc-course/bc-evaluation/bc-iam`，并逐步解除各 BC 对 `eva-infra-shared` 的依赖面；排期建议见 0.10。

**2026-02-14（单资源闭环：`FormRecordMapper.xml` 归位到 `bc-evaluation-infra`，保持 MyBatis 行为不变）**
- ✅ 资源搬运：`FormRecordMapper.xml` 已从 `eva-infra-dal` 搬运归位到 `bc-evaluation/infrastructure`，保持 MyBatis XML `namespace/resultMap type/SQL` 与资源路径 `mapper/**` 不变，并同步删除旧位置同名文件以避免 classpath 重复导致启动失败。
- 🧪 最小回归通过（Java17 + mvnd）：`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`。
- 📌 代码落地：`e9e32fdf`。
- ✅ 里程碑：本轮“课程/评教 5 个 DAL XML 按 BC 归位”已全部闭环（剩余 WIP 仍在 stash，可继续按单类/单资源逐刀恢复）。

**2026-02-14（单资源闭环：`EvaTemplateMapper.xml` 归位到 `bc-evaluation-infra`，保持 MyBatis 行为不变）**
- ✅ 资源搬运：`EvaTemplateMapper.xml` 已从 `eva-infra-dal` 搬运归位到 `bc-evaluation/infrastructure`，保持 MyBatis XML `namespace/resultMap type/SQL` 与资源路径 `mapper/**` 不变，并同步删除旧位置同名文件以避免 classpath 重复导致启动失败。
- 🧪 最小回归通过（Java17 + mvnd）：`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`。
- 📌 代码落地：`fa3d9181`。
- ⏭️ 下一刀建议（同口径，单资源闭环）：继续搬运最后 1 个 `FormRecordMapper.xml`（并同步删除 `eva-infra-dal` 旧位置同名文件，避免 classpath 重复）。

**2026-02-14（单资源闭环：`CourOneEvaTemplateMapper.xml` 归位到 `bc-evaluation-infra`，保持 MyBatis 行为不变）**
- ✅ 资源搬运：`CourOneEvaTemplateMapper.xml` 已从 `eva-infra-dal` 搬运归位到 `bc-evaluation/infrastructure`，保持 MyBatis XML `namespace/resultMap type/SQL` 与资源路径 `mapper/**` 不变，并同步删除旧位置同名文件以避免 classpath 重复导致启动失败。
- 🧪 最小回归通过（Java17 + mvnd）：`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`。
- 📌 代码落地：`7aba53e1`。
- ⏭️ 下一刀建议（同口径，单资源闭环）：继续按 `EvaTemplateMapper.xml` → `FormRecordMapper.xml` 逐个搬运（均需同步删除 `eva-infra-dal` 旧位置同名文件，避免 classpath 重复）。

**2026-02-14（单资源闭环：`SemesterMapper.xml` 归位到 `bc-course-infra`，保持 MyBatis 行为不变）**
- ✅ 资源搬运：`SemesterMapper.xml` 已从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`，保持 MyBatis XML `namespace/resultMap type/SQL` 与资源路径 `mapper/**` 不变，并同步删除旧位置同名文件以避免 classpath 重复导致启动失败。
- 🧪 最小回归通过（Java17 + mvnd）：`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`。
- 📌 代码落地：`29d9b208`。
- ⏭️ 下一刀建议（同口径，单资源闭环）：继续按 `CourOneEvaTemplateMapper.xml` → `EvaTemplateMapper.xml` → `FormRecordMapper.xml` 逐个搬运（均需同步删除 `eva-infra-dal` 旧位置同名文件，避免 classpath 重复）。

**2026-02-14（单资源闭环：`CourseMapper.xml` 归位到 `bc-course-infra`，保持 MyBatis 行为不变）**
- ✅ 资源搬运：`CourseMapper.xml` 已从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`，保持 MyBatis XML `namespace/resultMap type/SQL` 与资源路径 `mapper/**` 不变，并同步删除旧位置同名文件以避免 classpath 重复导致启动失败。
- 🧪 最小回归通过（Java17 + mvnd）：`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`。
- 📌 代码落地：`cccf39ec`。
- ⏭️ 下一刀建议（同口径，单资源闭环）：继续按 `SemesterMapper.xml` → `CourOneEvaTemplateMapper.xml` → `EvaTemplateMapper.xml` → `FormRecordMapper.xml` 逐个搬运；剩余 WIP 仍在 stash（仅建议按“指定文件路径”精确 restore，避免整包 apply 造成冲突）。

**2026-02-14（恢复可编译回归基线：跨 BC 评教 Mapper 编译期直连清零，保持行为不变）**
- ✅ 课程侧（`bc-course-infra`）：`AssignEvaTeachersRepositoryImpl/DeleteCourseRepositoryImpl/DeleteCoursesRepositoryImpl/UpdateSingleCourseRepositoryImpl/CourseRecommendExce` 不再编译期依赖评教侧 `Mapper` 类型（`edu.cuit.infra.dal.database.mapper.eva.*`），改为按 `beanName` 注入 `Object` + 反射调用对应 MyBatis 方法（保持 SQL/异常文案/缓存/日志与副作用顺序不变）。
- ✅ 模板侧（`bc-template-infra`）：`CourseTemplateLockQueryPortImpl` 同步去除对评教侧 `Mapper` 类型的编译期依赖，改为按 `beanName` 注入 `Object` + 反射调用（保持行为不变）。
- 🧪 最小回归通过（Java17 + mvnd）：`mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`。
- 📌 代码落地：`8bf5c164`。
- 🧷 剩余未闭环 WIP 已整体封存到 stash：`wip: remaining after mapper-decouple`（后续按“单资源 XML/单类”逐刀恢复、回归、提交、推送）。

**2026-02-13（交接补充：本会话仅核验口径 + 落盘，未新增闭环提交）**
- 📌 本会话起手核验（必须）：`git branch --show-current` 输出 `ddd`；`git merge-base --is-ancestor 2e4c4923 HEAD` 退出码为 `0`。
- 📊 证据口径快照（避免口径漂移；命令见新对话提示词 A）：
  - root reactor：`pom.xml` 仍含 `eva-infra-dal/eva-infra-shared/eva-base`
  - `eva-infra-dal/src/main/resources` XML 数：`0`
  - `bc-template` 对 `infra.dal.database.mapper.eva.*`：`0`
  - `bc-course` 对 `infra.dal.database.mapper.eva.*`：`0`（`0` 个文件）
- ⚠️ 工作区（未闭环，严禁 `reset/checkout`）：当前存在“已暂存新增文件 + 未暂存改动（含资源删除）”混合状态；后续必须用 `git commit -- <path>` 做范围隔离，逐步恢复到“单文件一刀闭环”。
  - 已暂存新增（index）：`bc-course/application/**/port/*QueryPort.java`（9 个）+ `bc-course/infrastructure/**/*QueryPortImpl.java`（9 个）+ `bc-course/infrastructure/**/mapper/course/*Mapper.xml`（2 个）+ `bc-evaluation/infrastructure/**/mapper/eva/*Mapper.xml`（3 个）
  - 未暂存改动（working tree）：`.gitignore`、`bc-course/infrastructure`（`AssignEvaTeachersRepositoryImpl/DeleteCourseRepositoryImpl/DeleteCoursesRepositoryImpl/UpdateSingleCourseRepositoryImpl/CourseRecommendExce`）、`bc-evaluation/infrastructure`（`Eva*QueryRepository`、`DeleteEva*RepositoryImpl`、`PostEvaTaskRepositoryImpl`、`SubmitEvaluationRepositoryImpl`、`EvaUpdateGatewayImpl`）、`bc-template/infrastructure`（`CourseTemplateLockQueryPortImpl`），以及 `eva-infra-dal/src/main/resources/mapper/**` 删除（5 个 XML）。
  - 下一步建议（保持行为不变）：优先把“资源 XML 归位”做成可编译闭合的单资源闭环（先同时 stage 对应 delete + add，再最小回归），再继续拆端口/适配器。

**2026-02-13（本会话收口总览：跨 BC Mapper 直连清零收尾，保持行为不变）**
- ✅ 本会话新增闭环（保持行为不变，均已跑最小回归并 push）：`AddNotExistCoursesDetailsRepositoryImpl`（`e7e444a1`）→ `DeleteSelfCourseRepositoryImpl`（`bbc32ae1`）→ `UpdateSelfCourseRepositoryImpl`（`db6d6165`）。
- ✅ 本会话新增闭环（保持行为不变）：`CourseImportExce` 清零对评教侧 `EvaTaskMapper/FormRecordMapper/CourOneEvaTemplateMapper/FormTemplateMapper` 的编译期直连（`Object` + `@Qualifier` + 反射 `selectList/delete/deleteBatchIds/selectOne/selectById`，保持 MyBatis 语义不变）；最小回归通过；代码落地：`0dfe5cd6`。
- ✅ 本会话新增闭环（保持行为不变）：`CourseQueryRepository` 清零对评教侧 `CourOneEvaTemplateMapper/EvaTaskMapper/FormRecordMapper/FormTemplateMapper` 的编译期直连（`Object` + `@Qualifier` + 反射 `selectOne/selectList/selectCount`，保持 MyBatis 语义不变）；最小回归通过；代码落地：`be3389a2`。
- 📊 证据口径快照（保持行为不变；以 `git rev-parse --short HEAD` 为准）：`bc-course/**` 对评教侧 `infra.dal.database.mapper.eva.*` 编译期 import 当前为 `0` 处（`0` 个文件）。
- ✅ 阶段性完成（保持行为不变）：`bc-course/**` 对评教侧 `infra.dal.database.mapper.eva.*` 编译期直连已清零。
- ⚠️ 工作区状态（重要，避免误操作）：当前仍处于“已暂存新增文件 + 未暂存代码改动”的混合状态（口径：`git status -sb` + `git diff --cached --name-status` + `git diff --name-status`）。请不要 `reset/checkout`；后续提交务必使用 `git commit -- <path>` 进行范围隔离。

**2026-02-13（本会话：单类闭环补一刀，保持行为不变）**
- ✅ 课程侧：`UpdateSelfCourseRepositoryImpl` 清零对评教侧 `EvaTaskMapper` 的编译期直连（`Object` + `@Qualifier` + 反射 `selectList/delete`，保持 MyBatis 语义不变）；最小回归通过；代码落地：`db6d6165`。
- 📊 口径更新（保持行为不变）：`bc-course/**` 对评教 Mapper import 由 `9` 处（3 个文件）降至 `8` 处（2 个文件）（口径命令见 0.11）。

**2026-02-13（本会话：单类闭环补一刀，保持行为不变）**
- ✅ 课程侧：`DeleteSelfCourseRepositoryImpl` 清零对评教侧 `EvaTaskMapper/FormRecordMapper` 的编译期直连（`Object` + `@Qualifier` + 反射 `selectList/delete`，保持 MyBatis 语义不变）；最小回归通过；代码落地：`bbc32ae1`。
- 📊 口径更新（保持行为不变）：`bc-course/**` 对评教 Mapper import 由 `11` 处（4 个文件）降至 `9` 处（3 个文件）（口径命令见 0.11）。

**2026-02-13（本会话：单类闭环补一刀，保持行为不变）**
- ✅ 课程侧：`AddNotExistCoursesDetailsRepositoryImpl` 清零对评教侧 `FormTemplateMapper` 的编译期直连（`Object` + `@Qualifier` + 反射 `selectOne`，保持 MyBatis 语义不变）；最小回归通过；代码落地：`e7e444a1`。
- 📊 口径更新（保持行为不变）：`bc-course/**` 对评教 Mapper import 由 `12` 处（5 个文件）降至 `11` 处（4 个文件）（口径命令见 0.11）。

**2026-02-13（本会话：保持行为不变，方案 B 主线继续）**
- ✅ 会话收口快照（保持行为不变）：当前 root reactor 仍包含 `eva-infra-dal/eva-infra-shared/eva-base`（口径：`rg -n '<module>eva-' pom.xml`）；`eva-infra-dal` 余量为 `24` 个 Java + `0` 个 XML；`eva-infra-shared` 余量为 `9` 个 Java；`eva-base-common` 余量为 `2` 个 Java（口径见 0.10.1）。
- ✅ 会话收口结论（保持行为不变）：已确认“完全独立（仅通过 contract/port 调用）”的关键阻塞仍是跨 BC 的 DAL 直连与共享模块依赖面：`bc-evaluation/**` 对课程 Mapper import 尚有 0 处（已按端口替换完成），`bc-template/**` 对评教 Mapper import 尚有 0 处（已清零编译期 import），`bc-course/**` 对评教 Mapper import 尚有 0 处（已清零编译期 import）；且 `bc-course/bc-evaluation/bc-iam/bc-audit/bc-template/bc-messaging` 仍编译期依赖 `eva-infra-shared`。
- ⚠️ 工作区状态（重要，避免下个会话误判）：本会话仍处于“已暂存新增文件 + 未暂存代码改动”的混合状态，且尚有大量变更未提交（另已补 3 刀并提交：`e7e444a1`、`bbc32ae1`、`db6d6165`）。其中“新增端口/适配器/XML 归位文件”已 `git add`（可用 `git diff --cached --name-status` 复核）；其余 Java 改动仍在工作区未暂存（可用 `git diff --name-status` 复核）。
- ✅ 本会话新增进展（保持行为不变，跨 BC 直连清零继续推进）：
  - 评教侧：`EvaRecordQueryRepository` / `EvaStatisticsQueryRepository` / `EvaUpdateGatewayImpl` 进一步清理对课程域 `CourseMapper` 的直连，统一改为走课程域直查端口 `CourseObjectDirectQueryPort`（并补齐 `EvaLogConditionalQuery` 的 `List<Long>` → `List<Integer>` 转换以保持编译闭合）。
  - 模板侧：`CourseTemplateLockQueryPortImpl` 已清零对评教侧 `infra.dal.database.mapper.eva.*` 的编译期直连（`Object` + `@Qualifier` + 反射调用，保持 MyBatis 语义不变）。
  - 课程侧：已在 `CourseRecommendExce`、`DeleteCourseRepositoryImpl`、`UpdateSingleCourseRepositoryImpl`、`AssignEvaTeachersRepositoryImpl`、`DeleteCoursesRepositoryImpl` 清零对评教侧 `EvaTaskMapper/FormRecordMapper` 的编译期直连（同样采用 `Object` + `@Qualifier` + 反射调用；保持行为不变）。
  - 回归说明：`mvnd -pl start -am test -Dtest=...EvaRecordServiceImplTest,EvaStatisticsServiceImplTest ...` 通过；`mvn -pl :start -am test -Dtest=edu.cuit.infra.bctemplate.CourseTemplateLockQueryPortImplTest ...` 通过。若 `mvnd` 遇到 `~/.m2/mvnd/.../registry.bin AccessDenied`，优先按 0.11 的环境变量把 registry 指向工作区 `.mvnd/`，仍不行则降级用 `mvn` 跑最小回归（保持行为不变）。
- ✅ 已完成（保持行为不变，Serena 证据化 + 风险评估）：已复核 `CourseConvertor` 的引用面与 import 依赖：引用面跨 `bc-course/**` 与 `bc-evaluation/**`；同时其 import 仍依赖 `EntityFactory` 与多项 `DO/Mapper`（当前位于 `eva-infra-dal`）。`shared-kernel/pom.xml` 当前不具备完整编译闭合，且若直接补 `shared-kernel -> eva-infra-dal` 将与现有 `eva-infra-dal -> shared-kernel` 形成循环依赖，因此本刀判定“风险超阈值”（保持行为不变）。
- ✅ 已完成（保持行为不变，降级执行，单资源闭环）：将 `CourInfMapper.xml` 从 `eva-infra-dal` 归位到 `bc-course/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；Serena：`CourInfMapper` 引用面集中在 `bc-course/**`（另含 `start` 单测 mock）；最小回归通过；代码落地：`4eb6681c`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，单资源闭环）：将 `SubjectMapper.xml` 从 `eva-infra-dal` 归位到 `bc-course/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；Serena：`SubjectMapper` 引用面跨 `bc-course/**` 与评教读侧 `bc-evaluation/**`；最小回归通过；代码落地：`92374d53`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将评教侧 `EvaTaskMapper` 从 `eva-infra-dal` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.eva` 不变；最小回归通过；代码落地：`b7049e4c`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将评教侧 `FormRecordMapper` 从 `eva-infra-dal` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.eva` 不变；最小回归通过；代码落地：`8927067c`）。
- ✅ 已完成（保持行为不变，方案 B：依赖前置，单 pom）：为后续将 `CourseFormat` 从 `eva-infra-shared` 下沉到 `shared-kernel` 做编译闭合前置，在 `shared-kernel/pom.xml` 补齐 `zym-spring-boot-starter-jdbc` 与 `jackson-databind` 依赖（最小回归通过；代码落地：`322bb315`）。
- ✅ 已完成（保持行为不变，Serena 证据化）：`CourseFormat` 引用面确认跨 `bc-course/**` 与 `bc-evaluation/**`；并确认实例方法 `selectCourOneEvaTemplateDO(...)` 仍被 `ICourseDetailServiceImpl.pageCoursesInfo(...)` 调用，当前不可按“纯静态工具类”裁剪。
- ✅ 已完成（保持行为不变，方案 B：单类解耦前置）：`CourseFormat` 已将对 `CourOneEvaTemplateMapper/CourOneEvaTemplateDO` 的编译期强依赖收敛为反射调用（`@Qualifier("courOneEvaTemplateMapper") Object` + 反射 `selectOne/getFormTemplate`）；异常文案“类型转换异常”、查询条件与副作用顺序不变（最小回归通过；代码落地：`8b4f69e2`）。
- ✅ 已完成（保持行为不变，方案 B：单类搬运）：将 `CourseFormat` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package edu.cuit.infra.gateway.impl.course.operate` 与类内容不变；最小回归通过；代码落地：`dff4e751`）。
- ✅ 已完成（保持行为不变，闭环同步）：已完成三文档同步（`NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md`）并推送（文档落地：`871bc5e0`；分支：`ddd`）。

**2026-02-12（本会话：保持行为不变，继续瘦身共享基础设施）**
- ✅ 已完成（保持行为不变，方案 B：共享常量下沉，单类）：将用户/权限缓存键常量 `UserCacheConstants` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package` 与 bean 名称 `userCacheConstants` 不变；最小回归通过；代码落地：`5f1447e5`）。
- ✅ 已完成（保持行为不变，方案 B：共享常量下沉，单类）：将评教缓存键常量 `EvaCacheConstants` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package` 与 bean 名称 `evaCacheConstants` 不变；最小回归通过；代码落地：`f1fac0f6`）。
- ✅ 已完成（保持行为不变，方案 B：共享常量下沉，单类）：将课程缓存键常量 `CourseCacheConstants` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package` 与 bean 名称 `courseCacheConstants` 不变；最小回归通过；代码落地：`16a07a6b`）。
- ✅ 已完成（保持行为不变，方案 B：依赖前置，单 pom）：为后续将 `*CacheConstants` 等含 Spring 注解的共享常量类下沉到 `shared-kernel` 做准备，在 `shared-kernel/pom.xml` 预置 `spring-context(optional)` 编译期依赖（最小回归通过；代码落地：`21a7176b`）。
- ✅ 已完成（保持行为不变，方案 B：共享工具下沉，单类）：将共享 Excel/POI 工具 `ExcelUtils` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package` 与类内容不变；最小回归通过；代码落地：`31615f43`）。
- ✅ 已完成（保持行为不变，方案 B：依赖前置，单 pom）：为后续把共享 Excel/POI 工具（如 `ExcelUtils`）下沉到 `shared-kernel` 做准备，在 `shared-kernel/pom.xml` 补齐 `cola-component-exception`、`hutool-all`、`poi/poi-ooxml` 依赖（最小回归通过；代码落地：`b4641433`）。
- ✅ 已完成（保持行为不变，方案 B：依赖前置，单 pom）：为后续将共享 Excel/POI 工具从 `eva-infra-shared` 下沉到 `shared-kernel` 做准备，在 `bc-course/infrastructure/pom.xml` 显式增加对 `shared-kernel` 的 Maven 编译期依赖（最小回归通过；代码落地：`80a1aec7`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类）：收敛评教读侧记录查询对课程域 `CourInfMapper.selectList(...)` 的跨 BC 直连：`EvaRecordQueryRepository` 改为调用课程域查询端口 `CourInfObjectDirectQueryPort.findByIds/findByCourseIds(...)`（异常文案/分支/查询次数与顺序不变；最小回归通过；代码落地：`13889255`）。
- ✅ 已完成（保持行为不变，方案 1：cour_inf 跨 BC 直连清零收尾，单类）：收敛 `bc-template/infrastructure` 的课程模板锁定查询对课程域 `CourInfMapper.selectList(eq course_id)` 的跨 BC 直连：`CourseTemplateLockQueryPortImpl` 改为调用课程域查询端口 `CourInfIdsByCourseIdsQueryPort.findCourInfIdsByCourseIds(...)`（查询次数与顺序不变；最小回归通过；代码落地：`f81b4661`）。
- ✅ 已闭环（保持行为不变，协作口径补强）：本会话上述变更均已按“Serena → 最小回归 → 代码提交 → 三文档同步 → 文档提交 → push”完成；最小回归命令以 0.10/0.11 为准；注意不要提交工作区未跟踪的 `.mvnd/`、`.ssh_known_hosts`。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类）：收敛评教读侧任务查询对课程域 `CourInfMapper.selectById/selectList(in course_id)` 的跨 BC 直连：`EvaTaskQueryRepository` 改为调用课程域查询端口 `CourInfObjectDirectQueryPort.findById/findByCourseIds(...)`（异常文案/分支/查询次数与顺序不变；最小回归通过；代码落地：`aec56fc9`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类前置）：在 `bc-course/infrastructure` 新增端口适配器 `CourInfObjectDirectQueryPortImpl`，用于承接 `CourInfObjectDirectQueryPort`（内部仅 `CourInfMapper.selectById/selectList`；最小回归通过；代码落地：`74cda7d2`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类前置）：在 `bc-course/application` 新增查询端口 `CourInfObjectDirectQueryPort`，用于以端口方式直查 cour_inf 对象（返回 `Object`，避免端口暴露 DAL DataObject；后续用于替换评教读侧对 `CourInfMapper` 的直连；最小回归通过；代码落地：`22fba0be`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类）：收敛评教写侧删除评教记录对课程域 `CourInfMapper.selectById/selectList` 的跨 BC 直连：`DeleteEvaRecordRepositoryImpl.delete(...)` 改为调用课程域查询端口 `CourInfTimeSlotQueryPort.findByCourInfId(...)` + `CourInfIdsByCourseIdsQueryPort.findCourInfIdsByCourseIds(...)`（异常文案/分支/查询次数与顺序不变；最小回归通过；代码落地：`d2472370`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类）：收敛评教侧撤回任务对课程域 `CourInfMapper.selectById` 的跨 BC 直连：`EvaUpdateGatewayImpl.cancelEvaTaskById(...)` 改为调用课程域查询端口 `CourseIdByCourInfIdQueryPort.findCourseIdByCourInfId(...)`（缓存失效 key 与副作用顺序不变；最小回归通过；代码落地：`6592d4ba`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类）：收敛评教写侧 `PostEvaTaskRepositoryImpl.create(...)` 对课程域 `CourInfMapper.selectById/selectList` 的跨 BC 直连：改为调用课程域查询端口 `CourInfTimeSlotQueryPort.findByCourInfId/findByCourseIds/findByCourInfIds`（时间冲突判定逻辑/异常文案/查询次数与顺序不变；最小回归通过；代码落地：`50943f8c`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类前置）：在 `bc-course/infrastructure` 新增端口适配器 `CourInfTimeSlotQueryPortImpl`，用于承接 `CourInfTimeSlotQueryPort`（内部仅 `CourInfMapper.selectById/selectList`；最小回归通过；代码落地：`343e3ecf`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类前置）：在 `bc-course/application` 新增查询端口 `CourInfTimeSlotQueryPort`，用于以端口方式查询 cour_inf 时间片（id/courseId/week/day/start/end）（后续用于替换评教写侧 `CourInfMapper.selectById/selectList` 的时间冲突判定；最小回归通过；代码落地：`ef2ab821`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类）：收敛评教写侧对课程域 `CourInfMapper.selectById` 的跨 BC 直连：`SubmitEvaluationRepositoryImpl.loadContext/saveEvaluation` 改为调用课程域查询端口 `CourseIdByCourInfIdQueryPort.findCourseIdByCourInfId(...)`（异常文案与查询顺序不变；最小回归通过；代码落地：`176ed9d4`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类）：收敛评教侧对课程域 `CourInfMapper.selectList(in course_id)` 的跨 BC 直连：`EvaStatisticsQueryRepository.getEvaTaskIdS(...)`/统计相关查询改为调用课程域查询端口 `CourInfIdsByCourseIdsQueryPort.findCourInfIdsByCourseIds(...)`（查询次数与顺序不变；最小回归通过；代码落地：`28dd1e6b`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类）：在 `bc-course/application` 新增查询端口 `CourInfIdsByCourseIdsQueryPort`，用于以端口方式查询 `course_id -> cour_inf.idS`（替换评教侧 `CourInfMapper.selectList(in course_id)`；最小回归通过；代码落地：`fef8b5aa`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourInfIdsByCourseIdsQueryPortImpl`，用于承接 `CourInfIdsByCourseIdsQueryPort`（内部仅 `CourInfMapper.selectList(in course_id)`；最小回归通过；代码落地：`8df151b6`）。
- ✅ 已完成（保持行为不变，方案 1：继续清零评教侧对课程域 cour_inf 的跨 BC 直连，单类）：收敛评教侧对课程域 `CourInfMapper.selectList(in course_id)` 的跨 BC 直连：`EvaTemplateQueryRepository.getEvaTaskIdS(...)` 改为调用课程域查询端口 `CourInfIdsByCourseIdsQueryPort.findCourInfIdsByCourseIds(...)`（查询次数与顺序不变；最小回归通过；代码落地：`1bac7ffc`）。
- ✅ 已完成（保持行为不变，跨 BC 直连清零前置，单类）：在 `bc-course/infrastructure` 新增端口适配器 `CourseIdByCourInfIdQueryPortImpl`，用于承接 `CourseIdByCourInfIdQueryPort`（内部仅 `CourInfMapper.selectById` 查询 `cour_inf.id -> course_id`；不改变业务语义/副作用顺序；最小回归通过；代码落地：`f0c0f020`）。
- ✅ 已完成（保持行为不变，跨 BC 直连清零前置，单类）：在 `bc-course/application` 新增最小查询端口 `CourseIdByCourInfIdQueryPort`，用于后续让其它 BC 以端口方式查询 `cour_inf.id -> course_id`（避免跨 BC 直连 `CourInfMapper`；不改变任何业务语义/副作用顺序；最小回归通过；代码落地：`777ec8a9`）。
- ✅ 已完成（保持行为不变，跨 BC 直连清零前置，单 pom）：在 `bc-evaluation/infrastructure/pom.xml` 显式增加对 `bc-course` 的 Maven 编译期依赖，用于让评教侧编译期引用课程域查询端口 `CourseIdByCourInfIdQueryPort`（运行期仍由组合根装配 `bc-course-infra` 的实现；最小回归通过；代码落地：`f2188237`）。
- ✅ 已完成（保持行为不变，跨 BC 直连清零，单类）：收敛评教侧对课程域 `CourInfMapper.selectById` 的跨 BC 直连：`EvaTemplateQueryRepository.getTaskTemplate(...)` 改为调用课程域查询端口 `CourseIdByCourInfIdQueryPort.findCourseIdByCourInfId(...)`（异常文案“并没有找到相关课程详情”与分支顺序不变；最小回归通过；代码落地：`67755034`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点：IAM `sys_user`，单资源闭环）：将 `SysUserMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；代码落地：`3dad6ef7`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `EntityFactory` 从 `eva-infra-shared` 搬运归位到 `eva-infra-dal`（保持 `package edu.cuit.infra.convertor` 不变；类内容不变；最小回归通过；代码落地：`eba15e92`）。
- ✅ 已完成（保持行为不变，编译闭合前置，单 pom）：为后续将 `EntityFactory` 从 `eva-infra-shared` 归位到 `eva-infra-dal` 做准备，在 `eva-infra-dal/pom.xml` 显式补齐 `hutool-all`、`cola-component-exception`、`mapstruct` 依赖（不改变任何业务语义/副作用顺序；最小回归通过；代码落地：`6546c548`）。
- ✅ 已完成（保持行为不变，证据化结论）：Serena 证伪 `bc-*`（排除 `bc-iam/**`）范围内无 `SysUserRoleMapper`/`SysRoleMapper` 引用点；标志“其它 BC 对 IAM role 表（`sys_user_role/sys_role`）的跨 BC 直连”阶段性清零。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `PaginationBizConvertor` 从 `eva-infra-shared` 搬运归位到 `eva-infra-dal`（保持 `package edu.cuit.app.convertor` 不变；类内容不变；Serena：引用面命中 `bc-audit/bc-course/bc-iam/start`；最小回归通过；代码落地：`2b950a06`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `PaginationConverter` 从 `eva-infra-shared` 搬运归位到 `eva-infra-dal`（保持 `package edu.cuit.infra.convertor` 不变；类内容不变；Serena：引用面命中 `bc-audit/bc-course/bc-evaluation/bc-iam`；最小回归通过；代码落地：`d2ca2d80`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `QueryUtils` 从 `eva-infra-shared` 搬运归位到 `eva-infra-dal`（保持 `package edu.cuit.infra.util` 不变；类内容不变；Serena：引用面命中 `bc-audit/bc-course/bc-evaluation/bc-iam/start`；最小回归通过；代码落地：`e653338f`）。
- ✅ 已完成（保持行为不变，编译闭合前置，单 pom）：在 `eva-infra-dal/pom.xml` 显式增加对 `shared-kernel` 的 Maven 编译期依赖，用于承接后续将 `QueryUtils` 从 `eva-infra-shared` 归位到 `eva-infra-dal`（最小回归通过；代码落地：`996b6990`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `RoleConverter` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.convertor.user` 不变；类内容不变；Serena：引用面仅命中 `bc-iam/infrastructure`；最小回归通过；代码落地：`340c5ba8`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysUserRoleDO` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.database.dataobject.user` 不变；类内容不变；Serena：引用面仅命中 `bc-iam/infrastructure`；最小回归通过；代码落地：`539cd792`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `EvaConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.convertor.eva` 不变；类内容不变；Serena：引用面仅命中 `bc-evaluation/infrastructure` 的读侧查询仓储；最小回归通过；代码落地：`4df4e9b8`）。

**2026-02-11（本会话：保持行为不变，继续瘦身共享基础设施）**
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `EvaTemplateBizConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.app.convertor.eva` 不变；类内容不变；Serena：引用面为空，且 `rg -n "EvaTemplateBizConvertor" .` 仅命中其自身与文档；`@Mapper(componentModel = "spring")` 由 Spring 扫描装配；最小回归通过；代码落地：`ecac6910`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `AspectConfig` 从 `eva-infra-shared` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.app.aop` 不变；类内容不变；Serena：引用面为空，且 `rg -n "AspectConfig" .` 仅命中其自身与文档；`@Aspect/@Component` 由 Spring 扫描装配；最小回归通过；代码落地：`33dbaf6f`）。
- ✅ 已完成（保持行为不变，DAL（LDAP）拆散试点，逐类归位）：将 `LdapPersonDO` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.ldap.dataobject` 不变；类内容不变；Serena：引用面仅命中 `bc-iam/infrastructure`；最小回归通过；代码落地：`2120b80d`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `WebSocketConfig` 从 `eva-infra-shared` 搬运归位到 `bc-messaging`（保持 `package edu.cuit.app.config` 不变；类内容不变；Serena：引用面为空，且 `rg -n "WebSocketConfig" .` 仅命中其自身与文档；`@Configuration` 由 Spring 扫描装配；最小回归通过；代码落地：`eb110825`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `WebSocketInterceptor` 从 `eva-infra-shared` 搬运归位到 `bc-messaging`（保持 `package edu.cuit.app.config` 不变；类内容不变；Serena：引用面仅命中 `bc-messaging` 的 `WebSocketConfig`；最小回归通过；代码落地：`3015ba57`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `UriUtils` 从 `eva-infra-shared` 搬运归位到 `bc-messaging`（保持 `package edu.cuit.app.util` 不变；类内容不变；Serena：引用面仅命中 `bc-messaging` 的 `WebSocketInterceptor`；最小回归通过；代码落地：`3febc475`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `MessageChannel` 从 `eva-infra-shared` 搬运归位到 `bc-messaging`（保持 `package edu.cuit.app.websocket` 不变；类内容不变；Serena：引用面仅命中 `bc-messaging` 的 `WebSocketConfig`；最小回归通过；代码落地：`10248c53`）。
- ✅ 已完成（保持行为不变，编译闭合前置，单 pom）：在 `bc-evaluation/infrastructure/pom.xml` 补齐对 `bc-messaging` 的 Maven 编译期依赖，用于承接后续将 `WebsocketManager` 从 `eva-infra-shared` 归位到 `bc-messaging`（最小回归通过；代码落地：`4dd1b34f`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `WebsocketManager` 从 `eva-infra-shared` 搬运归位到 `bc-messaging`（保持 `package edu.cuit.app.websocket` 不变；类内容不变；Serena：引用面命中 `bc-messaging/MessageChannel`、`bc-evaluation/infrastructure/MsgServiceImpl` 与 `start` 测试；最小回归通过；代码落地：`bf78d276`）。
- ✅ 已完成（保持行为不变，依赖收敛，逐类推进，先 IAM）：将 `bc-evaluation` 的 `EvaTaskQueryRepository.toUserEntity` 从“跨 BC 直连 IAM role 表（sys_user_role/sys_role）”收敛为依赖 `bc-iam-contract` 端口 `UserEntityObjectByIdDirectQueryPort`（异常文案保持不变；不引入新的缓存/切面副作用；最小回归通过；代码落地：`6c5d6bce`；三文档同步：`5dc70832`）。
- ✅ 已完成（保持行为不变，依赖收敛，逐类推进，先 IAM）：将 `bc-evaluation/infrastructure` 的 `EvaRecordQueryRepository.toUserEntity` 从“跨 BC 直连 IAM role 表（sys_user_role/sys_role）”收敛为依赖 `bc-iam-contract` 端口 `UserEntityObjectByIdDirectQueryPort`（Serena：类内 `SysUserRoleMapper/SysRoleMapper` 引用点清零；异常文案保持不变；不引入新的缓存/切面副作用；最小回归通过；代码落地：`78787eee`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysRoleMapper` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.user` 不变；Serena：引用面仅命中 `bc-iam/infrastructure`；最小回归通过；代码落地：`60b87404`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `SysRoleMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；代码落地：`aa1d7c6b`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysUserRoleMapper` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.user` 不变；Serena：引用面仅命中 `bc-iam/infrastructure`；最小回归通过；代码落地：`1f93141c`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `SysUserRoleMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；代码落地：`0cd5da04`）。
- ⚠️ 环境提示（不影响业务语义，仅影响协作效率）：当前环境下直连 GitHub SSH `22` 端口可能被阻断，`git push` 可能长时间无输出卡住；若出现该问题，建议使用 `ssh.github.com:443` 推送（示例：`GIT_SSH_COMMAND='ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o Hostname=ssh.github.com -p 443' git push origin ddd`）。
- 🧾 补充（保持行为不变，仅协作提效）：若 `git push` 在 `ssh.github.com:443` 下仍无输出卡住，建议加 `BatchMode=yes` 并禁止交互提示以便尽快失败/重试：`GIT_TERMINAL_PROMPT=0 GIT_SSH_COMMAND='ssh -o BatchMode=yes -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o Hostname=ssh.github.com -p 443' git push origin ddd`。

**2026-02-10（本会话：保持行为不变，继续瘦身共享基础设施）**
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `EvaTaskBizConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.app.convertor.eva` 不变；类内容不变；Serena：引用面为空，且 `rg -n "EvaTaskBizConvertor" .` 仅命中其自身与文档；`@Mapper(componentModel = "spring")` 由 Spring 扫描装配；最小回归通过；代码落地：`f3a2cf7f`）。

**2026-02-07（本会话：保持行为不变，继续瘦身共享基础设施）**
- 🧾 快照证据复核（保持行为不变，仅证据与文档同步）：root reactor 仍包含 `eva-infra-dal` / `eva-infra-shared` / `eva-base`（口径：`rg -n "<module>eva-" pom.xml`）；已闭环项计数复核：`msg_tip`（`MsgTipMapper/MsgTipDO/MsgTipMapper.xml` 各命中 1，且 `eva-infra-dal` 下 0 命中）、`sys_role_menu`（`SysRoleMenuMapper/SysRoleMenuDO/SysRoleMenuMapper.xml` 各命中 1，且 `eva-infra-dal` 下 0 命中）、`course_type_course`（`CourseTypeCourseMapper/CourseTypeCourseDO/CourseTypeCourseMapper.xml` 各命中 1，且 `eva-infra-dal` 下 0 命中）（口径：`fd -t f ... | wc -l`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `SaTokenConfig` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.app.config` 不变，仅改变 Maven 模块归属；Serena：未发现显式 import 引用面（`@Configuration` 由 Spring 扫描加载）；最小回归通过；代码落地：`fb3fe49d`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `SaTokenInterceptorConfig` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.app.config` 不变，仅改变 Maven 模块归属；Serena：未发现显式引用面（`@Configuration` 由 Spring 扫描加载）；最小回归通过；代码落地：`78b831d9`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `StpInterfaceImpl` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.app.security` 不变，仅改变 Maven 模块归属；Serena：无显式引用面（`@Component` 由 Spring 扫描装配）；最小回归通过；代码落地：`192e790c`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `MsgBizConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.app.convertor` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-evaluation/infrastructure` 的 `MsgServiceImpl`（另有 `start` 单测 mock）；最小回归通过；代码落地：`7077924e`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `EvaConfigBizConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.app.convertor.eva` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-evaluation/infrastructure` 的 `EvaConfigService`；最小回归通过；代码落地：`3d374b20`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `EvaRecordBizConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.app.convertor.eva` 不变，仅改变 Maven 模块归属；Serena：未发现显式引用面；最小回归通过；代码落地：`6a5430cb`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `AfterCommitEventPublisher` 从 `eva-infra-shared` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.app.event` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-course/infrastructure`；最小回归通过；代码落地：`352b1680`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `SemesterConverter` 从 `eva-infra-shared` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.convertor` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-course/infrastructure` 的 `SemesterGatewayImpl`；最小回归通过；代码落地：`99f78d40`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `ClassroomCacheConstants` 从 `eva-infra-shared` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.enums.cache` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-course/infrastructure`；最小回归通过；代码落地：`eb41025e`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `CalculateClassTime` 从 `eva-infra-shared` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.eva.util` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-evaluation/infrastructure` 的 `PostEvaTaskRepositoryImpl`；最小回归通过；代码落地：`01b13b20`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `AvatarProperties` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.property` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-iam/infrastructure` 的 `AvatarManager`；最小回归通过；代码落地：`e3e9a1e4`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `StaticConfigProperties` 从 `eva-infra-shared` 搬运归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.property` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-evaluation/infrastructure` 的 `EvaConfigGatewayImpl`；最小回归通过；代码落地：`3004217d`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `LdapUserConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.convertor.user` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-iam/infrastructure`；最小回归通过；代码落地：`4d09f8da`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `LdapPersonRepo` 从 `eva-infra-shared` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.ldap.repo` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-iam/infrastructure`；最小回归通过；代码落地：`ed120804`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `SysMenuMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；代码落地：`920c17d1`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysMenuMapper` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.user` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-iam/infrastructure`；最小回归通过；代码落地：`b53615e5`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `CourseTypeMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.course` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-course/infrastructure`；最小回归通过；代码落地：`241b75de`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `CourseTypeMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；代码落地：`158f0bd2`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `CourseTypeCourseDO` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.dal.database.dataobject.course` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-course/infrastructure`；最小回归通过；代码落地：`8f410b14`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `CourseTypeCourseMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 MyBatis `namespace/resultMap type` 指向的 FQCN、SQL 与资源路径 `mapper/**` 不变；最小回归通过；代码落地：`45bc05d6`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `EvaTaskMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-evaluation/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；Serena：全仓库无对 `EvaTaskMapper.xml` 的文本引用点；最小回归通过；代码落地：`ad2e7d25`）。
- ✅ 已完成（保持行为不变，编译闭合前置，逐类推进）：在 `bc-iam-contract` 新增端口 `UserEntityObjectByIdDirectQueryPort`，用于后续把 `bc-audit` 等 BC 的“跨 BC 直连 IAM 表（sys_user/sys_user_role/sys_role）”改造为通过 IAM 对外端口调用（约束：实现方不得引入新的缓存/切面副作用；最小回归通过；代码落地：`51be7465`）。
- ✅ 已完成（保持行为不变，编译闭合前置，逐类推进）：在 `bc-iam-infra` 新增端口适配器 `UserEntityObjectByIdDirectQueryPortImpl`，内部原样复刻“直连 sys_user/sys_user_role/sys_role”的查询与装配逻辑（约束：不引入新的缓存/切面副作用；最小回归通过；代码落地：`2c9fb7e7`）。
- ✅ 已完成（保持行为不变，依赖收敛，逐类推进）：将 `bc-audit` 的 `LogGatewayImpl` 从“跨 BC 直连 IAM 表（sys_user/sys_user_role/sys_role）”收敛为依赖 `bc-iam-contract` 端口 `UserEntityObjectByIdDirectQueryPort`（内部仍保持原 SQL 与装配逻辑，不引入新的缓存/切面副作用；最小回归通过；代码落地：`fdd7078e`）。
- ✅ 已完成（保持行为不变，依赖收敛，逐类推进）：将 `bc-course` 的 `CourseQueryRepository.toUserEntity` 从“跨 BC 直连 IAM role 表（sys_user_role/sys_role）”收敛为依赖 `bc-iam-contract` 端口 `UserEntityObjectByIdDirectQueryPort`（仍保留对 `sys_user` 的直接查询；异常文案保持不变；不引入新的缓存/切面副作用；最小回归通过；代码落地：`c22bc75d`）。
- ✅ 已完成（保持行为不变，依赖收敛，逐类推进）：将 `bc-evaluation` 的 `EvaTaskQueryRepository.toUserEntity` 从“跨 BC 直连 IAM role 表（sys_user_role/sys_role）”收敛为依赖 `bc-iam-contract` 端口 `UserEntityObjectByIdDirectQueryPort`（异常文案保持不变；不引入新的缓存/切面副作用；最小回归通过；代码落地：`6c5d6bce`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `CourseTypeCourseMapper` 从 `eva-infra-dal` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.course` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-course/infrastructure`；最小回归通过；代码落地：`2e1cd36e`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `SysRoleMenuMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type` 指向的 FQCN、SQL 与资源路径 `mapper/**` 不变；最小回归通过；代码落地：`db81d674`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysRoleMenuDO` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.database.dataobject.user` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-iam/infrastructure` 的 `MenuWritePortImpl/RoleWritePortImpl/UserMenuCacheInvalidationPortImpl/RoleQueryGatewayImpl`；最小回归通过；代码落地：`49fcbda7`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysRoleMenuMapper` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.user` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-iam/infrastructure` 的 `MenuWritePortImpl/RoleWritePortImpl/UserMenuCacheInvalidationPortImpl`；最小回归通过；代码落地：`f98ee5c2`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，单资源闭环，逐文件归位）：将 `MsgTipMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-messaging`（保持 MyBatis `namespace/resultMap type` 指向的 FQCN、SQL 与资源路径 `mapper/**` 不变；Serena：`MsgTipMapper` 引用面仅命中 `bc-messaging` 的 `Message*PortImpl`；最小回归通过；代码落地：`5c5ab5e0`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `MsgTipDO` 从 `eva-infra-dal` 搬运归位到 `bc-messaging`（保持 `package edu.cuit.infra.dal.database.dataobject` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-messaging`；最小回归通过；代码落地：`87b38a55`；三文档同步：`29823ba6`）。
- ✅ 已完成（保持行为不变，逐类归位）：将 `MsgConvertor` 从 `eva-infra-shared` 搬运归位到 `bc-messaging`（保持 `package edu.cuit.infra.convertor` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-messaging` 的 `MessageInsertionPortImpl/MessageQueryPortImpl`；最小回归通过；代码落地：`312756c7`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `MsgTipMapper` 从 `eva-infra-dal` 搬运归位到 `bc-messaging`（保持 `package edu.cuit.infra.dal.database.mapper` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-messaging` 的 `Message*PortImpl`；最小回归通过；代码落地：`4af9f9fc`）。

**2026-02-06（本会话收尾：文档闭环 + 状态核验；保持行为不变）**
- ✅ 已完成（保持行为不变，依赖收敛，单 pom）：收敛 `bc-audit/infrastructure/pom.xml`：移除对 `eva-infra-dal` 的冗余 Maven 直依赖（前置：`eva-infra-shared/pom.xml` 已显式依赖 `eva-infra-dal`；因此编译/运行期 classpath 与行为不变；最小回归通过；代码落地：`91fd39a9`）。
- ✅ 已完成（保持行为不变，依赖收敛，单 pom）：收敛 `bc-evaluation/infrastructure/pom.xml`：移除对 `eva-infra-dal` 的冗余 Maven 直依赖（前置：`eva-infra-shared/pom.xml` 已显式依赖 `eva-infra-dal`；因此编译/运行期 classpath 与行为不变；最小回归通过；代码落地：`a0b5a359`）。
- ✅ 已完成（保持行为不变，依赖收敛，单 pom）：收敛 `bc-iam/infrastructure/pom.xml`：移除对 `eva-infra-dal` 的冗余 Maven 直依赖（前置：`eva-infra-shared/pom.xml` 已显式依赖 `eva-infra-dal`；因此编译/运行期 classpath 与行为不变；最小回归通过；代码落地：`d7caa268`）。
- ✅ 已完成（保持行为不变，依赖收敛，单 pom）：收敛 `bc-messaging/pom.xml`：移除对 `eva-infra-dal` 的冗余 Maven 直依赖（前置：`eva-infra-shared/pom.xml` 已显式依赖 `eva-infra-dal`；因此编译/运行期 classpath 与行为不变；最小回归通过；代码落地：`ab25db93`）。
- ✅ 已完成（保持行为不变，依赖收敛，单 pom）：收敛 `bc-messaging/pom.xml`：移除冗余 `spring-context` Maven 直依赖（前置：已依赖 `spring-boot-starter-web`，其传递承接 `spring-context`；最小回归通过；代码落地：`5da009c9`）。
- ✅ 已完成（保持行为不变，依赖收敛，单 pom）：收敛 `bc-template/infrastructure/pom.xml`：将对 `eva-infra-dal` 的 Maven 编译期直依赖替换为依赖 `eva-infra-shared`（其显式依赖 `eva-infra-dal`，因此编译/运行期 classpath 与行为不变；最小回归通过；代码落地：`204aef24`）。
- ✅ 已完成（保持行为不变，依赖收敛，单 pom）：收敛 `bc-template/application/pom.xml`：在 Serena 证伪 `bc-template/application/src/main/java` 无 Lombok 引用后，移除冗余 `lombok(provided)` 依赖（编译/运行期 classpath 与行为不变；最小回归通过；代码落地：`e91844c2`）。
- ✅ 已完成（保持行为不变，依赖收敛，单 pom）：收敛 `bc-course/application/pom.xml`：在 Serena 证伪 `bc-course/application/src` 无 Lombok 引用后，移除冗余 `lombok(provided)` 依赖（编译/运行期 classpath 与行为不变；最小回归通过；代码落地：`c38e30f0`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogModuleMapper` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.log` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-audit/infrastructure` 的 `LogGatewayImpl`；最小回归通过；代码落地：`c901e3a6`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogMapper` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 `package edu.cuit.infra.dal.database.mapper.log` 不变，仅改变 Maven 模块归属；Serena：引用面命中 `bc-audit/infrastructure` 的 `LogInsertionPortImpl/LogGatewayImpl`；最小回归通过；代码落地：`5de32a6c`）。
- ✅ 已完成（保持行为不变，支撑类归位，逐类归位）：将 `LogConverter` 从 `eva-infra-shared` 搬运归位到 `bc-audit/infrastructure`（保持 `package edu.cuit.infra.convertor` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-audit/infrastructure` 的 `LogInsertionPortImpl/LogGatewayImpl`；最小回归通过；代码落地：`02c85909`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogDO` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 `package edu.cuit.infra.dal.database.dataobject.log` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-audit/infrastructure`；最小回归通过；代码落地：`7de33487`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogModuleDO` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 `package edu.cuit.infra.dal.database.dataobject.log` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `bc-audit/infrastructure`；最小回归通过；代码落地：`960d2bbb`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 MyBatis `<mapper namespace>` / `resultMap type` / SQL 不变，且资源路径仍为 `mapper/**`；最小回归通过；代码落地：`b6f05784`）。
- ✅ 已完成（保持行为不变，DAL 拆散试点，逐类归位）：将 `SysLogModuleMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-audit/infrastructure`（保持 MyBatis `<mapper namespace>` / `resultMap type` / SQL 不变，且资源路径仍为 `mapper/**`；最小回归通过；代码落地：`f6c7897c`）。
- ✅ 已完成（保持行为不变，依赖收敛证伪，单 pom）：Serena 证据化确认 `bc-audit/infrastructure` 仍直接使用 `eva-infra-shared` 内类型（`QueryUtils/PaginationConverter/UserConverter/RoleConverter/EntityFactory`），因此暂不可移除该依赖；已将结论记录于 `bc-audit/infrastructure/pom.xml`（最小回归通过；代码落地：`a0073972`）。
- ✅ 本会话提交链（按发生顺序，便于回溯/回滚；保持行为不变）：`c38e30f0`（代码：bc-course/application 去 lombok）→ `d28f0cff`（三文档同步）→ `ec6b200e`（docs：纠偏 eva-* 整合现状与路线）→ `c901e3a6`（代码：SysLogModuleMapper 归位）→ `7534337b`（三文档同步）→ `5de32a6c`（代码：SysLogMapper 归位）→ `221a4050`（三文档同步）→ `02c85909`（代码：LogConverter 归位）→ `84998087`（三文档同步）→ `7de33487`（代码：SysLogDO 归位）→ `ced4eee7`（三文档同步）→ `960d2bbb`（代码：SysLogModuleDO 归位）→ `f4ed59c6`（三文档同步）→ `b6f05784`（代码：SysLogMapper.xml 归位）→ `e339a3b2`（三文档同步）→ `f6c7897c`（代码：SysLogModuleMapper.xml 归位）→ `61321256`（三文档同步）→ `a0073972`（代码：记录 `eva-infra-shared` 依赖收敛证伪结论）。后续提交见本节 2026-02-07 条目（文档提交不在此处固化 commitId）。
- ✅ 已完成（保持行为不变，逐类归位）：将 `CourInfTimeOverlapQuery` 从 `eva-infra-shared` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.bccourse.support` 与类内容不变；Serena：引用面仅命中 `bc-course/infrastructure` 的 3 个适配器（`AssignEvaTeachersRepositoryImpl/UpdateSelfCourseRepositoryImpl/UpdateSingleCourseRepositoryImpl`）；最小回归通过；代码落地：`ea6c99e9`；文档闭环：`2466ead3`）。
- ✅ 已完成（保持行为不变，逐类归位）：将 `ClassroomOccupancyChecker` 从 `eva-infra-shared` 搬运归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.bccourse.support` 与类内容不变；Serena：引用面仅命中 `bc-course/infrastructure` 的 3 个适配器；最小回归通过；落地：`b1db3422`）。
- ✅ 已完成（保持行为不变，编译闭合补强，单 pom）：`eva-infra-shared/pom.xml` 补齐 `cola-component-exception` 编译期依赖（承接 `SysException/BizException`；最小回归通过；落地：`776ab171`）。
- ✅ 已完成（保持行为不变，收尾清理，单 pom）：删除 `eva-infra/pom.xml`（前置：root reactor 已移除 `<module>eva-infra</module>` 且无依赖方；最小回归通过；落地：`6c9b6224`）。
- ✅ 已完成（保持行为不变，收尾清理，单 pom）：删除 `eva-domain/pom.xml`（前置：root reactor 已移除 `<module>eva-domain</module>`；最小回归通过；落地：`c0035b03`）。
- ✅ 已完成（保持行为不变，reactor 退场，单 pom）：收敛根 `pom.xml`：从 reactor 中移除 `<module>eva-domain</module>`（仅改变聚合构建边界；最小回归通过；落地：`6b907bc1`）。
- ✅ 已完成（保持行为不变，依赖收敛，单 pom）：收敛 `eva-infra-shared/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖，并补齐对 `bc-course-domain` / `bc-evaluation-domain` / `bc-audit-domain` / `bc-iam-domain` 的显式依赖以保持编译闭合（最小回归通过；落地：`0585d6fb`）。
- ✅ 状态核验（rg，保持行为不变）：全仓库 `**/pom.xml` 不再出现 `<artifactId>eva-domain</artifactId>`（口径：`rg -n "<artifactId>eva-domain</artifactId>" --glob "**/pom.xml" .`）。
- ✅ 已完成（保持行为不变）：补齐 `SysLogModuleEntity` 归位到 `bc-audit-domain` 的“三文档同步 + push”闭环（文档落地：`31e4d11c`；对应代码提交：`1f8675f1`）。
- ✅ 状态核验（Serena，保持行为不变）：`EvaUpdateGateway` 当前 FQCN 唯一位于 `bc-evaluation/domain/src/main/java/edu/cuit/domain/gateway/eva/EvaUpdateGateway.java`；引用面命中 `bc-evaluation/infrastructure` 的旧入口壳 `EvaTaskServiceImpl` 与旧 gateway 实现 `EvaUpdateGatewayImpl`，无需重复搬运。
- ✅ 快照证据（防回归，保持行为不变）：`rg -n "<module>eva-domain</module>" pom.xml` 无命中。
- ✅ 证据化盘点（Serena，保持行为不变）：`SysLogEntity` 引用面集中于审计日志链路（`LogGateway`/`LogGatewayImpl`/`LogServiceImpl`/`LogBizConvertor`/`LogConverter`），未发现跨 BC 复用。
- ✅ 已完成（保持行为不变）：将 `SysLogEntity` 从 `eva-domain` 搬运归位到 `bc-audit-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；落地：`9efe8f6e`）。
- ✅ 证据化盘点（Serena，保持行为不变）：`LogGateway` 引用面集中于审计日志链路（`LogServiceImpl`/`LogGatewayImpl`），未发现跨 BC 复用。
- ✅ 已完成（保持行为不变）：将 `LogGateway` 从 `eva-domain` 搬运归位到 `bc-audit-domain`（保持 `package`/接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；落地：`44417e03`）。
- ✅ 证据化盘点（Serena + rg，保持行为不变）：`DynamicConfigGateway` 仅在 `eva-domain` 自身出现（Serena 引用面为空；`rg -n "\\bDynamicConfigGateway\\b" --glob "*.java" .` 仅命中其自身文件），未发现跨 BC 复用。
- ✅ 已完成（保持行为不变）：将 `DynamicConfigGateway` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；落地：`9e2096fc`）。
- ✅ 证据化盘点（Serena，保持行为不变）：`MsgEntity` 引用面覆盖 `bc-messaging/**`（UseCase/Port/FacadeImpl/PortImpl/测试）、`bc-evaluation/infrastructure`（`MsgServiceImpl`）、`eva-infra-shared`（`MsgBizConvertor`）与 `bc-messaging`（`MsgConvertor`）以及 `eva-domain` 残留接口（`MsgGateway/MessageUseCaseFacade`）；且其类签名对外暴露 `UserEntity` 并使用 `@Entity`，因此后续搬运时需保证承接模块具备 `bc-iam-domain` 与 `cola-component-domain-starter` 的编译闭合。
- ✅ 已完成（保持行为不变，编译闭合前置）：为后续归位 `MsgEntity` 做准备，已收敛 `bc-messaging-contract/pom.xml` 并补齐承接 `MsgEntity` 所需的最小编译期依赖（`bc-iam-domain`、`cola-component-domain-starter`；仅用于编译闭合；落地：`51d5a042`）。
- ✅ 已完成（保持行为不变）：将 `MsgEntity` 从 `eva-domain` 搬运归位到 `bc-messaging-contract`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；落地：`79d68fc3`）。
- ✅ 已完成（保持行为不变）：将 `MsgGateway` 从 `eva-domain` 搬运归位到 `bc-messaging-contract`（保持 `package`/接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；落地：`eaf62606`）。
- ✅ 已完成（保持行为不变）：将 `MessageUseCaseFacade` 从 `eva-domain` 搬运归位到 `bc-messaging-contract`（保持 `package`/接口签名不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；落地：`31681de9`）。
- ✅ 依赖收敛（单 pom，保持行为不变）：在 `eva-domain/src/main/java` 已清零的前提下，已收敛 `bc-messaging/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖（仅收敛编译边界；落地：`acecb5af`）。
- ✅ 最小回归（保持行为不变）：已执行 `mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest ...`，`BUILD SUCCESS`（对应代码提交：`acecb5af`；本会话该回归后未再修改代码）。

**2026-02-06（前置：bc-audit-domain 编译闭合补强（承接 SysLog* 实体），保持行为不变）**
- ✅ 已完成（保持行为不变）：为后续逐类将 `SysLogEntity/SysLogModuleEntity` 从 `eva-domain` 搬运归位到 `bc-audit-domain` 做编译闭合前置，在 `bc-audit/domain/pom.xml` 补齐最小编译期依赖（`bc-iam-domain`、`cola-component-domain-starter`、`lombok(provided)`；仅编译闭合；最小回归通过；落地：`63c8c5ca`）。

**2026-02-06（前置：eva-domain 编译闭合过渡依赖（支撑逐类归位审计实体），保持行为不变）**
- ✅ 已完成（保持行为不变）：为确保在逐类将 `SysLogEntity/SysLogModuleEntity` 从 `eva-domain` 搬运归位到 `bc-audit-domain` 的过程中，全仓库仍可编译闭合，已在 `eva-domain/pom.xml` 增加对 `bc-audit-domain` 的 Maven 编译期依赖（过渡期；保持 `package` 不变；最小回归通过；落地：`90054971`）。

**2026-02-06（逐类归位：`SysLogModuleEntity` → `bc-audit-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证据化盘点 `SysLogModuleEntity` 引用面（命中：`LogGateway`/`LogGatewayImpl`、`LogConverter`、`LogBizConvertor`、`LogServiceImpl` 等审计链路）后，将其从 `eva-domain` 搬运归位到 `bc-audit-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`1f8675f1`）。

**2026-02-06（依赖收敛（单 pom）：`bc-course/application` 去 `eva-domain` 编译期依赖；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `bc-course/application/src/main/java` 仅依赖 `bc-course-domain` 内的 `SemesterGateway/ClassroomGateway/Course*Gateway` 与 `shared-kernel` 的 `PaginationResultEntity`，且无 `eva-domain` 残留类型引用面后，收敛 `bc-course/application/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖（最小回归通过；落地：`464a4d73`）。

**2026-02-06（依赖收敛（单 pom）：`bc-evaluation/application` 去 `eva-domain` 编译期依赖；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `bc-evaluation/application/src/main/java` 无对 `eva-domain` 残留类型（`MsgEntity/SysLog*/MsgGateway/LogGateway/DynamicConfigGateway/MessageUseCaseFacade`）的引用面后，收敛 `bc-evaluation/application/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖，并补齐 `cola-component-exception` 以保持 `com.alibaba.cola.exception.*` 编译闭合（最小回归通过；落地：`9f4eaa06`）。

**2026-02-06（逐类归位：`EvaTemplateEntity` → `bc-evaluation-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证据化盘点 `EvaTemplateEntity` 引用面（命中：评教模板读侧端口 `EvaTemplate*QueryPort`、读侧用例 `EvaTemplateQueryUseCase`、查询仓储 `EvaTemplateQueryRepository`、委托壳 `EvaQueryRepository`，以及 `eva-infra-shared` 的 `EvaConvertor/EvaTemplateBizConvertor`）后，将其从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`ee79ffac`）。

**2026-02-06（前置：bc-evaluation/application 编译闭合补强（承接逐类归位到 bc-evaluation-domain 的类型），保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证据化确认 `bc-evaluation/application` 仍依赖 `EvaTemplateEntity`（当时仍在 `eva-domain`）后，为后续逐类归位 `EvaTemplateEntity` 并最终收敛 `bc-evaluation/application` 去 `eva-domain` 依赖做编译闭合前置，在 `bc-evaluation/application/pom.xml` 增加对 `bc-evaluation-domain` 的 Maven 编译期依赖（仅编译闭合；最小回归通过；落地：`9637eca1`）。

**2026-02-06（逐类归位：`EvaConfigGateway` → `bc-evaluation-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证据化盘点 `EvaConfigGateway` 引用面（命中：`bc-evaluation` 读侧用例 `EvaStatisticsQueryUseCase`、`bc-evaluation/infrastructure` 旧入口壳 `EvaTaskServiceImpl`/`EvaConfigService`/`BcEvaluationConfiguration`、旧 gateway 实现 `EvaConfigGatewayImpl`，以及 AI/课程侧引用与 `start` 单测）后，将其从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`1aaff7d5`）。

**2026-02-06（逐类归位：`EvaConfigEntity` → `bc-evaluation-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证据化盘点 `EvaConfigEntity` 引用面（命中：评教统计用例 `EvaStatisticsQueryUseCase`、旧配置 gateway 实现 `EvaConfigGatewayImpl`、评教配置门面 `EvaConfigGateway`、以及 `eva-infra-shared` 的 `EvaConfigBizConvertor` 与 `start` 单测等）后，将其从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`0c7f6aae`）。

**2026-02-06（前置：bc-evaluation-domain 编译闭合补强（承接 EvaConfigEntity 的 SpringUtil），保持行为不变）**
- ✅ 已完成（保持行为不变）：为后续逐类将 `EvaConfigEntity` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain` 做编译闭合前置，在 `bc-evaluation/domain/pom.xml` 增加 `hutool-all` 编译期依赖（`EvaConfigEntity#clone()` 依赖 `SpringUtil`；仅编译闭合；最小回归通过；落地：`5c4d3efe`）。

**2026-02-06（逐类归位：`EvaUpdateGateway` → `bc-evaluation-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证据化盘点 `EvaUpdateGateway` 引用面（命中：`bc-evaluation/infrastructure` 下旧入口壳 `EvaTaskServiceImpl` 与旧 gateway 实现 `EvaUpdateGatewayImpl`；全仓库仅 3 处引用）后，将其从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`ba43d0a4`）。
- ✅ 快照（防回归，预期无命中）：`rg -n "<artifactId>eva-domain</artifactId>" --glob "**/pom.xml" .`（本次实际无命中）。

**2026-02-06（逐类归位：`EvaDeleteGateway` → `bc-evaluation-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证据化盘点 `EvaDeleteGateway` 引用面（主要在 `bc-evaluation-infra` 旧入口壳与旧 gateway 实现）后，将其从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`b5f8f5fe`）。

**2026-02-06（前置：bc-evaluation-domain 编译闭合补强（承接 @Component），保持行为不变）**
- ✅ 已完成（保持行为不变）：在 `bc-evaluation/domain/pom.xml` 增加 `spring-context(provided)` 编译期依赖，用于承接后续逐类归位到 `bc-evaluation-domain` 的 `edu.cuit.domain.gateway.eva.*` 接口上的 `@Component` 注解（仅编译闭合；最小回归通过；落地：`132f6fc0`）。

**2026-02-06（依赖收敛（单 pom）：`eva-domain` 去 `bc-course-domain` 编译期依赖；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `eva-domain/src/main/java` 已无 `edu.cuit.domain.(entity|gateway).course.*` 引用面后，收敛 `eva-domain/pom.xml`：移除对 `bc-course-domain` 的 Maven 编译期依赖（最小回归通过；落地：`ec4107e4`）。

**2026-02-06（逐类归位：`EvaRecordEntity` → `bc-evaluation-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：将 `EvaRecordEntity` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`f4ceb140`）。

**2026-02-06（逐类归位：`EvaTaskEntity` → `bc-evaluation-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：将 `EvaTaskEntity` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`c6cb11c4`）。

**2026-02-06（逐类归位：`CourOneEvaTemplateEntity` → `bc-evaluation-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：在已补齐编译闭合依赖且 `eva-domain` 已增加 `bc-evaluation-domain` 过渡依赖的前提下，将 `CourOneEvaTemplateEntity` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`616f925c`）。

**2026-02-06（前置：eva-domain 编译闭合过渡依赖（支撑逐类搬运评教实体），保持行为不变）**
- ✅ 已完成（保持行为不变）：在 `eva-domain/pom.xml` 增加对 `bc-evaluation-domain` 的 Maven 编译期依赖，用于在逐类将 `CourOneEvaTemplateEntity/EvaTaskEntity/EvaRecordEntity` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain` 的过程中维持全仓库编译闭合（保持 `package` 不变；最小回归通过；落地：`0bfbf450`）。

**2026-02-06（前置：bc-evaluation-domain 编译闭合支撑（为逐类搬运评教实体做准备），保持行为不变）**
- ✅ 已完成（保持行为不变）：在 `bc-evaluation/domain/pom.xml` 补齐最小编译期依赖（`bc-course-domain`、`bc-iam-domain`、`cola-component-domain-starter`、`lombok(provided)`），用于承接后续从 `eva-domain` 逐类归位的 `edu.cuit.domain.entity.eva.*` 实体（保持 `package` 不变；最小回归通过；落地：`c5117a1a`）。

**2026-02-05（下一刀：证伪并归位 `CourseQueryGateway` → `bc-course-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `CourseQueryGateway` 引用面覆盖 `bc-course/**`（端口适配器/旧 `CourseQueryGatewayImpl`）、`bc-evaluation/**`、`bc-iam/**`、`eva-infra-shared`（Convertor）与 `start` 单测后，将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`e5d56d1b`）。

**2026-02-05（下一刀：证伪并归位 `SingleCourseEntity` → `bc-course-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `SingleCourseEntity` 引用面集中在课程域与评教读侧（含 `CourseQueryGateway` 签名引用、`eva-infra-shared` Convertor、`start` 测试引用等）后，将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`16db55ff`）。

**2026-02-05（下一刀：证伪并归位 `CourseEntity` → `bc-course-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `CourseEntity` 引用面集中在课程域与评教读侧（含 `CourseQueryGateway` 签名引用、`eva-infra-shared` Convertor、`start` 测试引用等）后，将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`c94151f7`）。

**2026-02-05（前置：bc-course-domain 编译闭合支撑（为归位 CourseEntity 做准备），保持行为不变）**
- ✅ 已完成（保持行为不变）：为后续将 `CourseEntity/SingleCourseEntity` 从 `eva-domain` 逐类归位到 `bc-course-domain` 提供编译闭合前置，在 `bc-course/domain/pom.xml` 显式增加对 `bc-iam-domain` 的 Maven 编译期依赖（承接 `CourseEntity` 对 `UserEntity` 的类型引用；最小回归通过；落地：`fa7e2270`）。

**2026-02-05（下一刀：证伪并归位 `CourseUpdateGateway` → `bc-course-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `CourseUpdateGateway` 引用面集中在课程域（`bc-course/**`，含端口适配器与旧 `CourseUpdateGatewayImpl`）后，将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`73ccfff8`）。

**2026-02-05（下一刀：证伪并归位 `CourseDeleteGateway` → `bc-course-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `CourseDeleteGateway` 引用面集中在课程域（`bc-course/**`，含 `CourseTypeUseCase` 签名引用与端口适配器/旧 gateway 实现）后，将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`2ec12495`）。

**2026-02-05（下一刀：证伪并归位 `SemesterEntity` → `bc-course-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `SemesterEntity` 引用面覆盖 `bc-course/**` 与 `bc-evaluation/**`（查询侧）以及 `eva-domain/eva-infra-shared` 的签名引用后，将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`f4721306`）。

**2026-02-05（下一刀：证伪并归位 `SubjectEntity` → `bc-course-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `SubjectEntity` 引用面集中在课程域（含 `CourseQueryGateway`/`CourseConvertor`/`CourseBizConvertor`/`start` 测试签名引用）后，将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；确保全仓库该 FQCN 仅存在一份；最小回归通过；落地：`449e08d1`）。

**2026-02-04（前置：eva-domain 编译闭合支撑（bc-course），保持行为不变）**
- ✅ 已完成（保持行为不变）：为后续逐类把课程域类型从 `eva-domain` 归位到 `bc-course-domain`（且避免 `eva-domain` 内残留接口签名引用导致编译失败），在 `eva-domain/pom.xml` 显式增加对 `bc-course-domain` 的 Maven 编译期依赖（过渡期；最小回归通过；落地：`c425f384`）。

**2026-02-04（前置：bc-course-domain 编译闭合支撑（课程实体），保持行为不变）**
- ✅ 已完成（保持行为不变）：为后续归位 `CourseTypeEntity` 等带 `@Entity`/Lombok 的课程域实体做编译闭合前置，在 `bc-course/domain/pom.xml` 补齐 `cola-component-domain-starter` 与 `lombok(provided)`（最小回归通过；落地：`2c0626e8`）。

**2026-02-04（下一刀：证伪并归位 `CourseTypeEntity` → `bc-course-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `CourseTypeEntity` 引用面集中在课程域（含 `CourseQueryGateway` 签名）后，将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与类内容不变；最小回归通过；落地：`97a8e3f3`）。

**2026-02-04（下一刀：证伪并归位 `SemesterGateway` → `bc-course-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `SemesterGateway` 引用面覆盖 `bc-course/**` 与 `eva-infra-shared`（AOP 依赖）后，将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与接口签名/注解不变；最小回归通过；落地：`30d1c98a`）。

**2026-02-04（前置：bc-course-domain / eva-infra-shared 编译闭合支撑；保持行为不变）**
- ✅ 已完成（保持行为不变）：为后续归位 `SemesterGateway` 做编译闭合前置，在 `bc-course/domain/pom.xml` 补齐 `shared-kernel` 编译期依赖（承接 `SemesterCO`；最小回归通过；落地：`6353d17e`）。
- ✅ 已完成（保持行为不变）：为避免归位后 `eva-infra-shared` 编译失败，在 `eva-infra-shared/pom.xml` 显式依赖 `bc-course-domain`（过渡期；最小回归通过；落地：`46bf4c15`）。

**2026-02-04（下一刀：证伪并归位 `ClassroomGateway` → `bc-course-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `ClassroomGateway` 引用面仅在 `bc-course/**` 后，将其从 `eva-domain` 搬运归位到 `bc-course-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属；最小回归通过；落地：`11ac2be6`）。

**2026-02-04（前置：bc-course-domain 编译闭合支撑；保持行为不变）**
- ✅ 已完成（保持行为不变）：为后续逐类搬运 `edu.cuit.domain.*` 到 `bc-course-domain` 做编译闭合前置，在 `bc-course/domain/pom.xml` 补齐 `spring-context(provided)`（仅编译期依赖，运行期 classpath 不变；最小回归通过；落地：`3ab4b3de`）。

**2026-02-04（推进：归位 `UserEntity` → `bc-iam-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：将 `UserEntity` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`68840131`）。

**2026-02-04（S0.2 延伸：收敛 `bc-iam(application)` → `eva-domain` 编译期依赖；保持行为不变）**
- ✅ 已完成（保持行为不变）：在 Serena + 可复现 `rg` 证据化确认 `bc-iam/application` 仅剩的 `edu.cuit.domain.*` 引用已由 `bc-iam-domain/shared-kernel` 提供后，收敛 `bc-iam/application/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖，并补齐缺失的 `cola-component-exception` 编译期依赖以保持编译闭合（最小回归通过；落地：`04e8b671`）。

**2026-02-04（下一刀：证伪并归位 `RoleQueryGateway` → `bc-iam-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：Serena 证伪 `RoleQueryGateway` 引用面仅在 `bc-iam/**` 后，将其从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属；最小回归通过；落地：`4b3efbf7`）。

**2026-02-04（前置：归位 `RoleEntity` → `bc-iam-domain`；保持行为不变）**
- ✅ 已完成（保持行为不变）：将 `RoleEntity` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`6f290793`）。

**2026-02-04（推进：端口适配器依赖收敛为内部缓存接口；保持行为不变）**
- ✅ 已完成（保持行为不变）：将 `bc-iam/infrastructure` 的 `UserEntityByIdQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `findById`，确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过；落地：`e854fcbe`）。
- ✅ 已完成（保持行为不变）：将 `bc-iam/infrastructure` 的 `UserEntityByUsernameQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `findByUsername`，确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过；落地：`ec31d96c`）。
- ✅ 已完成（保持行为不变）：将 `bc-iam/infrastructure` 的 `UserAllUserIdQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `findAllUserId/findById`，确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过；落地：`c0c05def`）。
- ✅ 已完成（保持行为不变）：将 `bc-iam/infrastructure` 的 `UserNameQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `findById` 并通过强转读取 `name`，确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过；落地：`1b91181f`）。
- ✅ 已完成（保持行为不变）：将 `bc-iam/infrastructure` 的 `UserDetailQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `findById` 并通过强转读取 `id/username/name`，确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过；落地：`5d85516b`）。
- ✅ 已完成（保持行为不变）：将 `bc-iam/infrastructure` 的 `UserDirectoryPageQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `page/allUser/findAllUsername`，确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过；落地：`d5335f4a`）。
- ✅ 已完成（保持行为不变）：将 `bc-iam/infrastructure` 的 `UserEntityQueryPortImpl` 的注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway`，方法体仍委托 `getUserRoleIds`，确保调用最终进入 `UserQueryGatewayImpl` 触发 `@LocalCached`（最小回归通过；落地：`b7373cbb`）。
- ✅ 已完成（保持行为不变）：`UserQueryGatewayImpl` 已不再显式实现旧 `UserQueryGateway`，仅保留实现内部接口 `UserQueryCacheGateway`；`@LocalCached` 触发点与方法体保持不变（最小回归通过；落地：`fb3b2e40`）。
- ✅ 已完成（保持行为不变）：`UserQueryGatewayImpl` 已去 `UserEntity` 编译期依赖：移除 `import UserEntity`，并将 `findById/findByUsername/page` 返回类型收敛为 `Optional<?>/PaginationResultEntity<?>`（方法体与 `@LocalCached` 触发点不变；最小回归通过；落地：`8cba32b8`）。
- ✅ 已完成（保持行为不变）：`eva-domain` 的旧 `UserQueryGateway` 接口已去 `UserEntity` 泛型暴露：将 `findById/findByUsername/page` 返回类型收敛为 `Optional<?>/PaginationResultEntity<?>`（接口无引用面；最小回归通过；落地：`6ae17638`）。
- ✅ 已完成（保持行为不变）：已删除 `eva-domain` 中无引用的旧 `UserQueryGateway` 接口文件（最小回归通过；落地：`93a7723d`）。

**2026-02-04（前置：UserQueryGateway 收尾铺垫——旧 gateway 兼容内部过渡接口；保持行为不变）**
- ✅ 已完成（保持行为不变）：`UserQueryGatewayImpl` 现在同时实现 `UserQueryCacheGateway`，使后续端口适配器可把注入类型从 `UserQueryGateway` 收敛为内部接口而不改变实际委托路径与 `@LocalCached` 缓存触发点（最小回归通过；落地：`2970b80d`）。

**2026-02-04（前置：UserQueryGateway 收尾铺垫——新增内部过渡缓存接口；保持行为不变）**
- ✅ 新增（保持行为不变）：在 `bc-iam/infrastructure` 新增内部过渡接口 `edu.cuit.infra.gateway.user.UserQueryCacheGateway`（返回类型使用 `Optional<?>/PaginationResultEntity<?>`），用于后续逐类将端口适配器从编译期依赖旧 `UserQueryGateway`（eva-domain）收敛为依赖内部接口，同时仍保证调用最终进入旧 `UserQueryGatewayImpl` 以触发 `@LocalCached` 缓存/切面入口（最小回归通过；落地：`dc49f903`）。

**2026-02-04（证据化盘点：UserQueryGatewayImpl 缓存触发点与调用面；保持行为不变）**
- ✅ 已证据化（Serena + `rg` 兜底）：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/UserQueryGatewayImpl.java` 仍承载 `@LocalCached` 缓存触发点；当前 `bc-iam/infrastructure` 内对旧 `UserQueryGateway` 的 import/注入引用面已清零（含 `UserQueryGatewayImpl` 已不再显式实现旧接口；落地：`fb3b2e40`），且 `bc-iam/infrastructure` 已清零 `UserEntity` import（落地：`8cba32b8`）；详见 `docs/DDD_REFACTOR_BACKLOG.md` 4.3（证据化起点：`c4a32bde`）。
- ✅ 结论（保持行为不变）：本阶段已实现 “BC 范围清零 `UserEntity` import” 的编译期收敛目标（通过通配符返回类型承接实际返回值，缓存 key/area 与命中/回源语义不变）。后续若要进一步让 `eva-domain` 的 `UserEntity` 退出/迁移归属，需要另行拆步推进（高风险），且必须确保 `@LocalCached` 触发点与缓存语义不漂移。

**2026-02-04（补充：S0.2 延伸——收敛 `eva-infra/pom.xml` 残留依赖声明；保持行为不变）**
- ✅ 依赖收敛（保持行为不变）：`eva-infra` 已从 root reactor 退场且源码仅 `package-info.java`（无业务逻辑、无外部依赖方）；在 Serena + `rg` 证伪后，清理 `eva-infra/pom.xml` 残留 `<dependencies>` 声明（最小回归通过）；落地：`47654a6a`。

**2026-02-04（本次会话：IAM S0.2 延伸——继续收敛依赖方对 `UserEntity` 的编译期依赖；保持行为不变）**
- ✅ 本次会话提交链（按发生顺序，便于回溯/回滚）：`51301d23`（消息查询适配器去 `UserEntity` 代码）→ `f3fb4743`（三文档同步）→ `8fa053ed`（`LogConverter` 桥接前置）→ `c52a99fc`（三文档同步）→ `a86f6520`（`LogGatewayImpl` 去 `UserEntity` 代码）→ `902bb3ab`（三文档同步）→ `687aea3e`（`UserConverter` 补齐 SpringBean+setName 桥接）→ `4accbabf`（三文档同步）→ `f0655267`（`CourseQueryRepository` 去 `UserEntity` 编译期依赖）→ `485cc5fb`（`UserConverter` 补齐 username/status 桥接）→ `9e9f00af`（三文档同步）→ `42af63a3`（`UserBasicQueryPortImpl` 去 `UserEntity` 编译期依赖）→ `5d2f7512`（`RouterDetailFactory` 入参收敛为 `Object`）→ `721051b9`（三文档同步）→ `d901223c`（`UserServiceImpl` 去 `UserEntity` 编译期依赖）→ `6d6435c6`（三文档同步；以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `git log -n 1 -- DDD_REFACTOR_PLAN.md` / `git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md` 为准）。
- ✅ **IAM S0.2 延伸（前置：UserConverter 桥接方法，保持行为不变）**：在 `eva-infra-shared` 的 `UserConverter` 增加桥接方法 `toUserEntityObject(...)`（返回 `Object`）与 `userIdOf(Object)`（返回 `Integer`；内部仍使用 `UserEntity` 做强转，以尽量保持历史空值/异常表现一致），用于后续让评教读侧仓储去 `UserEntity` 编译期依赖（最小回归通过）；落地提交：`c173c7c2`。
- ✅ **IAM S0.2 延伸（前置：UserConverter SpringBean+setName 桥接，保持行为不变）**：在 `eva-infra-shared` 的 `UserConverter` 增加桥接方法 `springUserEntityWithNameObject(Object)`（内部仍 `SpringUtil.getBean(UserEntity.class)` + `((String) name)` 强转后 `setName`），用于后续让 `bc-course/infrastructure` 的 `CourseQueryRepository` 去 `UserEntity` 编译期依赖时避免引入反射导致异常形态漂移（最小回归通过）；落地提交：`687aea3e`。
- ✅ **IAM S0.2 延伸（前置：UserConverter 补齐 username/status 桥接，保持行为不变）**：为后续让 `bc-iam-infra` 的 `UserBasicQueryPortImpl` 去 `UserEntity` 编译期依赖（缓存命中时仍需读取 username/status），在 `UserConverter` 增加桥接方法 `usernameOf(Object, boolean)` + `statusOf(Object, boolean)`；内部仍强转 `UserEntity` 调 getter，以尽量保持历史异常形态一致；同时为避免 MapStruct 将其误判为通用类型转换导致编译期歧义，刻意保留无业务意义的形参（最小回归通过）；落地提交：`485cc5fb`。
- ✅ **IAM S0.2 延伸（IAM 基础查询端口去 UserEntity：UserBasicQueryPortImpl，保持行为不变）**：将 `bc-iam-infra` 的 `UserBasicQueryPortImpl` 去 `UserEntity` 编译期依赖：缓存命中时从 `Optional<?>` 取值并改走 `UserConverter.userIdOf/usernameOf/statusOf` 桥接方法读取字段（缓存命中/回源顺序与异常文案不变；最小回归通过）；落地提交：`42af63a3`。
- ✅ **IAM S0.2 延伸（IAM 实体查询端口去 UserEntity：UserEntityQueryPortImpl，保持行为不变）**：将 `bc-iam-infra` 的 `UserEntityQueryPortImpl` 去 `UserEntity` 编译期依赖：对外签名与 `bc-iam/application` 端口口径一致（`Optional<?>/PaginationResultEntity<?>`），内部仍通过 `UserConverter.toUserEntityObject(...)` 承接旧映射与角色/菜单装配逻辑，查询/排序/异常文案不变（最小回归通过）；落地提交：`e8acbad3`。
- ✅ **IAM S0.2 延伸（前置：UserConverter 补齐 roles 桥接，保持行为不变）**：为后续让 `bc-iam-infra` 的 `UserEntityByUsernameQueryPortImpl` 去 `UserEntity` 编译期依赖（权限/角色列表计算需读取 roles），在 `UserConverter` 增加桥接方法 `rolesOf(Object, boolean)`（内部仍强转 `UserEntity` 调 `getRoles`；并通过“无业务意义形参”规避 MapStruct 编译期歧义）；最小回归通过；落地提交：`81408507`。
- ✅ **IAM S0.2 延伸（IAM 按用户名查询端口去 UserEntity：UserEntityByUsernameQueryPortImpl，保持行为不变）**：将 `bc-iam-infra` 的 `UserEntityByUsernameQueryPortImpl` 去 `UserEntity` 编译期依赖：`findStatusByUsername/findPermissionListByUsername/findRoleListByUsername` 改走 `UserConverter.statusOf/rolesOf` 桥接读取字段（保持仍委托旧 `UserQueryGateway.findByUsername` 以触发历史缓存/切面；异常与分支顺序不变；最小回归通过）；落地提交：`44d51d3e`。
- ✅ **IAM S0.2 延伸（前置：UserBizConvertor 补齐 Object 桥接，保持行为不变）**：在 `bc-iam-infra` 的 `UserBizConvertor` 增加桥接方法 `toUserDetailCOObject(Object)` / `toUnqualifiedUserInfoCOObject(Object, Integer)`（内部仍强转 `UserEntity`，仅做类型桥接，尽量保持历史空值/异常表现一致），用于后续让调用方在不编译期引用 `UserEntity` 的情况下复用既有映射逻辑（最小回归通过）；落地提交：`c2454ab4`。
- ✅ **IAM S0.2 延伸（前置：UserConverter 补齐 name/perms/cast 桥接，保持行为不变）**：为后续让 `bc-iam-infra` 的 `RouterDetailFactory/UserServiceImpl` 去 `UserEntity` 编译期依赖，在 `UserConverter` 增加桥接方法 `nameOf(Object, boolean)` / `permsOf(Object, boolean)` / `castUserEntityObject(Object, boolean)`（内部仍强转 `UserEntity` 调 getter；并通过“无业务意义形参”规避 MapStruct 编译期歧义；`castUserEntityObject` 用于尽量保持历史 ClassCastException 触发点一致）；最小回归通过；落地提交：`95c7bd8b`。
- ✅ **IAM S0.2 延伸（RouterDetailFactory 去 UserEntity 编译期依赖，保持行为不变）**：将 `bc-iam-infra` 的 `RouterDetailFactory` 入参从 `UserEntity` 收敛为 `Object`，并通过 `SpringUtil.getBean(UserConverter.class)` 调用 `usernameOf/rolesOf/nameOf` 桥接读取字段（过滤逻辑/递归构造与异常表现不变；最小回归通过）；落地提交：`5d2f7512`。
- ✅ **IAM S0.2 延伸（UserServiceImpl 去 UserEntity 编译期依赖，保持行为不变）**：将 `bc-iam-infra` 的 `UserServiceImpl` 去 `UserEntity` 编译期依赖：`Optional<?>.map(UserEntity.class::cast)` 改走 `userConverter.castUserEntityObject(...)` 以尽量保持历史 ClassCastException 触发点一致；`getUserInfo` 入参从 `UserEntity` 收敛为 `Object`，并改走 `RouterDetailFactory.createRouterDetail(Object)` + `userBizConvertor.toUserDetailCOObject(Object)` + `userConverter.rolesOf/permsOf` 桥接读取字段（缓存/日志/异常文案/副作用顺序不变；最小回归通过）；落地提交：`d901223c`。
- ✅ **IAM S0.2 延伸（课程读侧仓储去 UserEntity：CourseQueryRepository，保持行为不变）**：将 `bc-course/infrastructure` 的 `CourseQueryRepository` 去 `UserEntity` 编译期依赖：不再 `import UserEntity`，并将 `Supplier<UserEntity>` 收敛为 `Supplier<?>`，调用侧改走 `userConverter.toUserEntityObject(...)` / `courseConvertor.toCourseEntityWithTeacherObject(...)`；同时将显式 `SpringUtil.getBean(UserEntity.class)` + `setName` 改为调用 `userConverter.springUserEntityWithNameObject(...)`（缓存/查询/遍历顺序与异常文案不变；最小回归通过）；落地提交：`f0655267`。
- ✅ **IAM S0.2 延伸（前置：CourseConvertor 桥接方法，保持行为不变）**：在 `eva-infra-shared` 的 `CourseConvertor` 增加桥接方法 `toCourseEntityWithTeacherObject(...)`（teacher Supplier 改为 `Supplier<?>`，内部仍桥接到既有 `toCourseEntity(...)` 并对 teacher 做 `UserEntity` 强转；仅做类型桥接，不改变 Supplier 调用时机/次数），用于后续让评教读侧仓储去 `UserEntity` 编译期依赖（最小回归通过）；落地提交：`858521da`。
- ✅ **IAM S0.2 延伸（评教读侧仓储去 UserEntity：任务主题，保持行为不变）**：将 `bc-evaluation/infrastructure` 的 `EvaTaskQueryRepository` 去 `UserEntity` 编译期依赖：不再 `import UserEntity`，改为通过 `UserConverter.userIdOf(Object)` 与 `EvaConvertor/CourseConvertor` 桥接方法承接 teacher 相关类型桥接（不改变 DB 查询/遍历顺序；异常文案不变；最小回归通过）；落地提交：`7f198610`。
- ✅ **IAM S0.2 延伸（评教读侧仓储去 UserEntity：记录主题，保持行为不变）**：将 `bc-evaluation/infrastructure` 的 `EvaRecordQueryRepository` 去 `UserEntity` 编译期依赖：不再 `import UserEntity`，改为通过 `UserConverter.userIdOf(Object)` 与 `EvaConvertor/CourseConvertor` 桥接方法承接 teacher 相关类型桥接（不改变 DB 查询/遍历顺序；异常文案不变；最小回归通过）；落地提交：`9cbcb858`。
- ✅ **IAM S0.2 延伸（前置：MsgConvertor 桥接方法，保持行为不变）**：在 `MsgConvertor` 增加桥接方法 `toMsgEntityWithUserObject(...)`，用于后续让 `bc-messaging` 的 `MessageQueryPortImpl` 去 `UserEntity` 编译期依赖（仅做类型桥接，不改变 sender/recipient Supplier 的调用时机与次数；最小回归通过）；落地提交：`8254a430`。（注：`MsgConvertor` 当前已归位到 `bc-messaging`，保持 `package` 不变。）
- ✅ **IAM S0.2 延伸（消息：MessageQueryPortImpl 去 UserEntity 编译期依赖，保持行为不变）**：将 `bc-messaging` 的 `MessageQueryPortImpl` 改走 `msgConvertor.toMsgEntityWithUserObject(...)` 并移除 `UserEntity` import（异常文案不变；`userEntityByIdQueryPort.findById(...)` 调用次数/顺序不变；最小回归通过）；落地提交：`51301d23`。证据口径：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-messaging/src/main/java` 命中应为 0。
- ✅ **S0.2 延伸（审计：LogConverter 桥接方法，保持行为不变）**：在 `eva-infra-shared` 的 `LogConverter` 增加桥接方法 `toLogEntityWithUserObject(...)`，用于后续让 `bc-audit` 的 `LogGatewayImpl` 去 `UserEntity` 编译期依赖（仅做类型桥接，不改变 user 获取时机与次数；最小回归通过）；落地提交：`8fa053ed`。
- ✅ **S0.2 延伸（审计：LogGatewayImpl 去 UserEntity 编译期依赖，保持行为不变）**：将 `bc-audit` 的 `LogGatewayImpl` 改走 `userConverter.toUserEntityObject(...)` + `logConverter.toLogEntityWithUserObject(...)` 并移除 `UserEntity` import（异常文案/查询/遍历顺序不变；最小回归通过）；落地提交：`a86f6520`。证据口径：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-audit/infrastructure/src/main/java` 命中应为 0。
- ✅ **测试过渡收敛（保持行为不变）**：将 `start` 模块的 `MsgServiceImplTest` 从兼容“旧构造（UserQueryGateway）/新构造（Port）”的反射方式，收敛为直接使用 Port 版本构造（测试不再编译期依赖 `UserQueryGateway`；最小回归通过）；落地提交：`de9d24a6`。
- ✅ **测试过渡收敛（保持行为不变）**：将 `start` 模块的 `UserEvaServiceImplTest` 收敛为直接使用 Port 版本构造（测试不再编译期依赖 `UserQueryGateway`；最小回归通过）；落地提交：`75fbb71f`。
- ✅ **IAM S0.2 延伸（前置：按用户名查询用户状态最小端口，保持行为不变）**：在 `bc-iam-contract` 新增 `UserStatusByUsernameQueryPort`（用于后续将 `ValidateUserLoginUseCase` 去 `UserEntity` 编译期依赖且保持对旧 `UserQueryGateway.findByUsername` 的调用次数/缓存触发点不变；最小回归通过）；落地提交：`21cbf908`。
- ✅ **IAM S0.2 延伸（前置：补齐端口适配器实现，保持行为不变）**：已在 `bc-iam-infrastructure` 落地 `UserStatusByUsernameQueryPort` 的端口适配器实现；后续为避免注入歧义，该独立适配器已被删除并由 `UserEntityByUsernameQueryPortImpl` 统一承接（最小回归通过；详见本节后续条目）。
- ✅ **IAM S0.2 延伸（前置：复用按用户名查询适配器补齐状态查询能力，保持行为不变）**：在 `bc-iam-infrastructure` 的 `UserEntityByUsernameQueryPortImpl` 补齐实现 `UserStatusByUsernameQueryPort`（内部仍委托旧 `UserQueryGateway.findByUsername`；通过拆箱保持历史空值/NPE 表现不变；最小回归通过）；落地提交：`2b152f5f`。
- ✅ **IAM S0.2 延伸（保持行为不变）**：将 `bc-iam/application` 的 `ValidateUserLoginUseCase` 从编译期依赖 `UserEntity` 收敛为依赖 `UserStatusByUsernameQueryPort`（仍通过 `UserEntityByUsernameQueryPortImpl -> UserQueryGateway.findByUsername` 保持调用次数/缓存触发点不变；异常文案/分支顺序不变；最小回归通过）；落地提交：`61821514`。
- ✅ **IAM S0.2 延伸（保持行为不变）**：将 `bc-iam/application` 的 `FindUserByIdUseCase` 去 `UserEntity` 编译期依赖，收敛为优先依赖 `bc-iam-contract` 端口 `UserEntityByIdQueryPort`（`execute` 使用泛型承接 `Optional<?>`；wiring/旧 gateway 委托链路不变；最小回归通过）；落地提交：`e13e1dc6`。
- ✅ **IAM S0.2 延伸（保持行为不变）**：将 `bc-iam/application` 的 `FindUserByUsernameUseCase` 去 `UserEntity` 编译期依赖，收敛为优先依赖 `bc-iam-contract` 端口 `UserEntityByUsernameQueryPort`（`execute` 使用泛型承接 `Optional<?>`；wiring/旧 gateway 委托链路不变；最小回归通过）；落地提交：`e8f16843`。
- ✅ **IAM S0.2 延伸（保持行为不变）**：将 `bc-iam/application` 的 `PageUserUseCase` 去 `UserEntity` 编译期依赖，收敛为优先依赖 `bc-iam-contract` 端口 `UserDirectoryPageQueryPort`（分页出参使用泛型承接 `PaginationResultEntity<?>`；wiring/旧 gateway 委托链路不变；最小回归通过）；落地提交：`4de644a7`。
- ✅ **IAM S0.2 延伸（保持行为不变）**：将 `bc-iam/application` 的旧端口 `UserEntityQueryPort` 去 `UserEntity` 编译期依赖（返回类型收敛为 `Optional<?>/PaginationResultEntity<?>`，过渡期实际返回仍为 `UserEntity`；端口适配器逻辑不变）；最小回归通过；落地提交：`72d029e0`。
- ✅ **IAM S0.2 延伸（保持行为不变）**：将 `bc-iam/application` 的 `FindUserByIdUseCaseTest` 去 `UserEntity` 编译期依赖（测试侧端口返回类型同步为通配符；不影响被测用例委托行为）；最小回归通过；落地提交：`d8804521`。
- ✅ **IAM S0.2 延伸（保持行为不变）**：将 `bc-iam/application` 的 `FindUserByUsernameUseCaseTest` 去 `UserEntity` 编译期依赖（测试侧端口返回类型同步为通配符；不影响被测用例委托行为）；最小回归通过；落地提交：`0ebfb75c`。
- ✅ **IAM S0.2 延伸（保持行为不变）**：将 `bc-iam/application` 的 `PageUserUseCaseTest` 去 `UserEntity` 编译期依赖（测试侧端口返回类型同步为通配符；不影响被测用例委托行为）；最小回归通过；落地提交：`934cf935`。
- ✅ **S0.2 延伸（依赖方收敛，保持行为不变）**：将 `bc-evaluation/infrastructure` 的 `FillAverageScoreExporterDecorator` 去 `UserEntity` 编译期依赖，改为依赖 `bc-iam-contract` 的 `UserDetailQueryPort` + `UserDetailCO`（内部仍委托旧 `UserQueryGateway.findById`，缓存触发点不变；最小回归通过）；落地提交：`7a3ca8ed`。
- ✅ **S0.2 延伸（依赖方收敛，保持行为不变）**：将 `bc-evaluation/infrastructure` 的 `FillEvaRecordExporterDecorator` 去 `UserEntity` 编译期依赖，改为依赖 `bc-iam-contract` 的 `UserDetailQueryPort` + `UserDetailCO`（内部仍委托旧 `UserQueryGateway.findById`，缓存触发点不变；最小回归通过）；落地提交：`fba76459`。
- ✅ **S0.2 延伸（依赖方收敛，保持行为不变）**：将 `bc-evaluation/infrastructure` 的 `FillUserStatisticsExporterDecorator` 去 `UserEntity` 编译期依赖，改为依赖 `bc-iam-contract` 的 `UserDetailQueryPort` + `UserDetailCO`（内部仍委托旧 `UserQueryGateway.findById`，缓存触发点不变；最小回归通过）；落地提交：`8d59ea72`。
- ✅ **S0.2 延伸（依赖方收敛，保持行为不变）**：将 `bc-evaluation/infrastructure` 的统计导出基类 `EvaStatisticsExporter` 去 `UserEntity` 编译期依赖：不再 `import UserEntity`，改为对 `UserAllUserIdAndEntityByIdQueryPort.findById` 返回的 `Optional<?>` 做运行时类型判定（按类名/父类链）后再 `Optional.of(...)`，以保持历史“仅当返回值为 UserEntity 才参与后续逻辑”的分支语义不变（端口适配器内部仍委托旧 `UserQueryGateway.findById`，缓存/切面触发点不变）；最小回归通过；落地提交：`4f4b190b`。
- ✅ **S0.2 延伸（依赖方收敛前置，保持行为不变）**：在 `eva-infra-shared` 的 `EvaConvertor` 增加桥接方法 `toEvaTaskEntityWithTeacherObject(...)`，用于后续让依赖方在不编译期引用 `UserEntity` 的情况下复用既有 `ToEvaTaskEntity` 映射逻辑（仅做类型桥接，不改变 teacher/courInf Supplier 的调用时机与次数）；最小回归通过；落地提交：`a8934ab1`。

**2026-02-03（本次会话：IAM 并行（10.3）：评教旧入口去 `UserQueryGateway` 编译期依赖；保持行为不变）**
- ✅ **IAM 并行（按 10.3：补齐鉴权权限/角色查询最小端口（前置），保持行为不变）**：在 `bc-iam-contract` 新增 `UserPermissionAndRoleQueryPort`（为后续 `eva-infra-shared` 的 `StpInterfaceImpl` 去 `UserQueryGateway` 编译期依赖做前置；仅新增接口，不改装配/不改行为；最小回归通过）；落地提交：`315c118d`。
- ✅ **IAM 并行（按 10.3：补齐端口适配器实现（鉴权权限/角色查询），保持行为不变）**：在 `bc-iam-infra` 的 `UserEntityByUsernameQueryPortImpl` 补齐实现 `UserPermissionAndRoleQueryPort`（内部仍委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变；权限/角色筛选逻辑与 `StpInterfaceImpl` 现有实现一致；最小回归通过）；落地提交：`47d20ff9`。
- ✅ **编译闭合前置（保持行为不变）**：为后续 `eva-infra-shared` 的 `StpInterfaceImpl` 依赖收敛做准备，在 `eva-infra-shared/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（仅编译边界显式化；运行时行为不变；最小回归通过）；落地提交：`9d90a1a7`。
- ✅ **IAM 并行（按 10.3：StpInterfaceImpl 依赖收敛；保持行为不变）**：将 `eva-infra-shared` 的 `StpInterfaceImpl` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 端口 `UserPermissionAndRoleQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变；权限/角色筛选逻辑不变；最小回归通过）；落地提交：`f28c212d`。
- ✅ **IAM 并行（按 10.3：MsgBizConvertor 依赖收敛；保持行为不变）**：将 `eva-infra-shared` 的 `MsgBizConvertor` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 端口 `UserEntityByIdQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变；消息 recipient/sender 的 lazy-load 语义不变；最小回归通过）；落地提交：`f036a2a4`。
- ✅ **测试过渡（保持行为不变）**：将 `start` 模块中的 `UserServiceImplTest` 从 mock `UserQueryGateway` 调整为 mock `bc-iam-contract` 端口（重点适配 `getOneUserScore` 使用的 `UserBasicQueryPort.findUsernameById`），以匹配 `UserServiceImpl` 构造参数收敛后的新依赖形态；最小回归通过；落地提交：`c30c4e0c`。
- ✅ **IAM 并行（按 10.3：UserServiceImpl 依赖收敛，保持行为不变）**：将 `bc-iam/infrastructure` 的 `UserServiceImpl` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 端口 `UserEntityByIdQueryPort/UserEntityByUsernameQueryPort/UserBasicQueryPort/UserDirectoryPageQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway` 以保持缓存/切面触发点不变；方法调用次数/顺序不变；异常文案不变）；最小回归通过；落地提交：`cc6f6d63`。
- ✅ **IAM 并行（按 10.3：补齐用户目录/分页端口（前置），保持行为不变）**：在 `bc-iam-contract` 新增 `UserDirectoryPageQueryPort`（原误命名为 `UserDirectoryQueryPort`，与 `bc-iam/application` 同名端口冲突已更名；涵盖 `page/allUser/findAllUsername`；分页返回 `PaginationResultEntity<?>`，避免 contract 暴露旧领域实体触发 Maven 循环依赖风险），用于后续将 `bc-iam/infrastructure` 的 `UserServiceImpl` 去 `UserQueryGateway` 编译期依赖；最小回归通过；落地提交：`d7e7216e`。
- ✅ **IAM 并行（按 10.3：补齐端口适配器实现，保持行为不变）**：在 `bc-iam-infra` 新增 `UserDirectoryPageQueryPortImpl`，内部委托旧链路以保持缓存/切面触发点不变（后续注入已收敛为 `UserQueryCacheGateway`，见 2026-02-04 推进记录；用于闭合后续 `UserServiceImpl` 依赖收敛后的 Spring 装配）；最小回归通过；落地提交：`524d7ba3`。
- ✅ **IAM 并行（按 10.3：去无用编译期依赖（DepartmentGatewayImpl），保持行为不变）**：清理 `DepartmentGatewayImpl` 中未使用的 `UserQueryGateway/QueryException` import，使该类不再编译期依赖旧 gateway（仅删 import；行为不变；最小回归通过）；落地提交：`f29572d2`。
- ✅ **IAM 并行（按 10.3：登录校验用例清理过渡构造（保持行为不变）**：Serena 证实 `ValidateUserLoginUseCase` 仅剩 `UserAuthServiceImpl` 一个调用点且已改走 Port 构造，因此移除 `ValidateUserLoginUseCase` 中 `@Deprecated` 旧构造与对 `UserQueryGateway` 的编译期依赖（运行时行为不变；异常文案/分支顺序不变）；最小回归通过；落地提交：`1b90f641`。
- ✅ **IAM 并行（按 10.3：登录入口依赖收敛（过渡），保持行为不变）**：将 `bc-iam-infra` 的 `UserAuthServiceImpl` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 最小端口 `UserEntityByUsernameQueryPort`（调用顺序不变：仍先 `ValidateUserLoginUseCase.execute` 再 `StpUtil.login`；内部仍委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变；异常文案不变）；最小回归通过；落地提交：`48d0eb7e`。
- ✅ **IAM 并行（按 10.3：登录校验用例依赖收敛（过渡），保持行为不变）**：将 `bc-iam/application` 的 `ValidateUserLoginUseCase` 从依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 最小端口 `UserEntityByUsernameQueryPort`（内部仍由端口适配器委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变；异常文案与分支顺序不变）；同时保留一个 `@Deprecated` 旧构造用于过渡/回滚；最小回归通过；落地提交：`66329367`。
- ✅ **IAM 并行（按 10.3：补齐按用户名查询用户实体端口（前置），保持行为不变）**：在 `bc-iam-contract` 新增最小端口 `UserEntityByUsernameQueryPort`（返回 `Optional<?>`，避免 contract 反向依赖旧领域实体导致 Maven 循环依赖；后续由端口适配器内部委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变）；最小回归通过；落地提交：`02827d19`。
- ✅ **IAM 并行（按 10.3：补齐按用户名查询用户实体端口适配器（前置），保持行为不变）**：在 `bc-iam-infra` 新增 `UserEntityByUsernameQueryPortImpl`，内部委托旧 `UserQueryGateway.findByUsername` 以保持缓存/切面触发点不变（为后续将 `ValidateUserLoginUseCase` 去 `UserQueryGateway` 编译期依赖闭合装配前置）；最小回归通过；落地提交：`5c18aa08`。
- ✅ **评教（依赖收敛：EvaTaskServiceImpl → UserBasicQueryPort，保持行为不变）**：将 `bc-evaluation/infrastructure` 的 `EvaTaskServiceImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（端口适配器内部仍委托旧 gateway 以保持缓存/切面触发点不变；异常文案/副作用顺序不变；最小回归通过）；落地提交：`72bd00d9`。
- ✅ **IAM 并行（按 10.3：评教导出链路收敛前置：组合端口补齐，保持行为不变）**：在 `bc-iam-contract` 新增组合端口 `UserAllUserIdAndEntityByIdQueryPort`，并让 `bc-iam-infra` 的 `UserAllUserIdQueryPortImpl` 同时实现 `findById`（内部仍委托旧 `UserQueryGateway.findAllUserId/findById` 以保持缓存/切面触发点不变），用于后续将 `bc-evaluation/infrastructure` 的 `EvaStatisticsExporter` 去 `UserQueryGateway` 编译期依赖且保持 `SpringUtil.getBean(...)` 次数/顺序不变；最小回归通过；落地提交：`1d6624d4`。
- ✅ **评教（依赖收敛：EvaStatisticsExporter → bc-iam-contract 组合端口，保持行为不变）**：将 `bc-evaluation/infrastructure` 的 `EvaStatisticsExporter` 从编译期依赖 `UserQueryGateway` 收敛为依赖 `bc-iam-contract` 组合端口 `UserAllUserIdAndEntityByIdQueryPort`（通过 `UserQueryGatewayFacade` 保持 `userQueryGateway.findAllUserId/findById` 调用形态不变，内部仍委托旧 `UserQueryGateway` 以保持缓存/切面触发点不变；且 `SpringUtil.getBean(...)` 次数/顺序不变）；最小回归通过；落地提交：`92c6554a`。

**2026-02-02（本次会话：S0.2 延伸（IAM：端口下沉以收敛编译期边界）；保持行为不变）**
- ✅ **IAM 并行（按 10.3：消息查询端口补齐，保持行为不变）**：为后续收敛 `bc-messaging` 的 `MessageQueryPortImpl` 对 `UserQueryGateway` 的编译期依赖做闭环，在 `bc-iam-contract` 新增最小端口 `UserEntityByIdQueryPort`（避免 contract 反向依赖旧领域实体导致 Maven 循环依赖；返回类型为 `Optional<?>`，过渡期实际返回 `UserEntity`），并在 `bc-iam-infra` 新增 `UserEntityByIdQueryPortImpl` 内部委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变；最小回归通过；落地提交：`7875e09e` / `2ea7d39f`。
- ✅ **消息（依赖收敛，保持行为不变）**：将 `bc-messaging` 的 `MessageQueryPortImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserEntityByIdQueryPort`（端口适配器内部仍委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变；异常文案不变）；最小回归通过；落地提交：`17509393`。
- ✅ **消息（编译闭合前置，保持行为不变）**：为后续将 `bc-messaging` 的 `MessageQueryPortImpl` 对 `UserQueryGateway` 的编译期依赖收敛为依赖 `bc-iam-contract` 的最小 Port 做前置，在 `bc-messaging/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（仅显式化依赖边界；最小回归通过）；落地提交：`16a90a5e`。
- ⚠️ **踩坑记录（必须避免）**：不要尝试让 `bc-iam-contract` 直接依赖 `eva-domain` 来“返回 UserEntity”，会触发 Maven reactor 循环依赖：`eva-domain -> bc-iam-domain -> bc-iam-contract -> eva-domain`（已在本次会话用 `mvnd` 复现报错）。本次采用的过渡方案是“contract 端口返回 `Optional<?>` + 端口适配器委托旧 `UserQueryGateway`”，以保持行为不变且避免循环依赖。
- ✅ **评教（依赖收敛收尾，保持行为不变）**：清理 `bc-evaluation/infrastructure` 的 `UserEvaServiceImpl` 残留未使用 `UserQueryGateway` import，使该类彻底不再编译期依赖旧 gateway（仅删 import；行为不变；最小回归通过）；落地提交：`edc3f9c7`。
- 🧾 **S0.2 延伸（junit-jupiter 口径复核，保持行为不变）**：Serena 盘点 `**/pom.xml` 中 `junit-jupiter` 仅出现在 `bc-iam/application`、`bc-course/application`、`bc-evaluation/application`、`bc-template/application`、`bc-messaging`，且均存在 `src/test/java` 测试源码，因此本次未产生“无测试源码模块”的可清理项。
- ✅ **评教（测试过渡，保持行为不变）**：为后续将 `bc-evaluation/infrastructure` 的 `UserEvaServiceImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort` 做前置，先将 `start/src/test/java/edu/cuit/app/service/impl/eva/UserEvaServiceImplTest.java` 改为同时兼容“旧构造（UserQueryGateway）/新构造（UserBasicQueryPort）”（仅测试代码；最小回归通过）；落地提交：`93ac4799`。
- ✅ **消息（测试过渡，保持行为不变）**：为后续收敛 `bc-evaluation/infrastructure` 的 `MsgServiceImpl` 对 `UserQueryGateway` 的编译期依赖，先将 `start/src/test/java/edu/cuit/app/service/impl/MsgServiceImplTest.java` 改为兼容“旧构造（UserQueryGateway）/新构造（Port）”（仅测试代码；最小回归通过）；落地提交：`f593b529`。
- ✅ **IAM 并行（按 10.3：新增用户ID列表查询端口，保持行为不变）**：在 `bc-iam-contract` 新增 `UserAllUserIdQueryPort`（为后续收敛 `bc-evaluation/infrastructure` 的 `MsgServiceImpl` 从依赖 `UserQueryGateway` 到依赖最小 Port 做前置；仅新增接口，不改装配/不改行为；最小回归通过）；落地提交：`f244c9d0`。
- ✅ **IAM 并行（按 10.3：补齐端口适配器实现，保持行为不变）**：在 `bc-iam-infra` 新增 `UserAllUserIdQueryPortImpl`，内部委托旧 `UserQueryGateway.findAllUserId()` 以保持缓存/切面触发点不变（为后续 `MsgServiceImpl` 依赖收敛闭合编译与运行时装配）；最小回归通过；落地提交：`daff2644`。
- ✅ **IAM 并行（按 10.3：MsgServiceImpl 依赖收敛；保持行为不变）**：将 `bc-evaluation/infrastructure` 的 `MsgServiceImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 IAM contract 端口 `UserBasicQueryPort + UserAllUserIdQueryPort`（广播消息的 `findAllUserId()` 由端口适配器委托旧 gateway 以保持缓存/切面触发点不变；异常文案/副作用顺序不变；最小回归通过）；落地提交：`b4456be9`。
- ✅ **IAM 并行（按 10.3：UserEvaServiceImpl 依赖收敛；保持行为不变）**：将 `bc-evaluation/infrastructure` 的 `UserEvaServiceImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（仅替换该类实际用到的 `findIdByUsername`；异常文案与分支顺序不变；最小回归通过）；落地提交：`456e5346`。
- ✅ **S0.2 延伸（IAM：端口下沉：UserBasicQueryPort → bc-iam-contract，保持行为不变）**：为减少依赖方对 IAM 应用层 jar（`bc-iam`）的编译期绑定，将 `UserBasicQueryPort` 从 `bc-iam/application` 下沉到 `bc-iam-contract`（保持 `package edu.cuit.bc.iam.application.port` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过）；落地提交：`739cb25f`。
- ✅ **IAM 并行（按 10.3，编译闭合前置：bc-ai-report-infra 显式依赖 bc-iam-contract；保持行为不变）**：为后续将 `bc-ai-report/infrastructure` 的 `AiReportUserIdQueryPortImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort` 做前置，在 `bc-ai-report/infrastructure/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（仅编译边界收敛；最小回归通过）；落地提交：`bceb2576`。
- ✅ **IAM 并行（按 10.3：AiReportUserIdQueryPortImpl 依赖收敛；保持行为不变）**：将 `bc-ai-report/infrastructure` 的 `AiReportUserIdQueryPortImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（原链路 `UserQueryGateway.findIdByUsername` 本就最终委托 `UserBasicQueryPort`，因此缓存命中/回源顺序与历史语义保持不变）；最小回归通过；落地提交：`b16546ed`。
- ✅ **IAM 并行（按 10.3：新增用户名查询端口，保持行为不变）**：为后续将 `bc-ai-report/infrastructure` 的 `AiReportAnalysisPortImpl` 从依赖 `UserQueryGateway` 收敛为依赖 IAM contract 端口做前置，在 `bc-iam-contract` 新增 `UserNameQueryPort`（仅新增接口，不改装配/不改行为；最小回归通过）；落地提交：`cfccf4ca`。
- ✅ **IAM 并行（按 10.3：补齐端口适配器实现，保持行为不变）**：在 `bc-iam-infra` 新增 `UserNameQueryPortImpl`，内部委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变，用于为后续 `AiReportAnalysisPortImpl` 依赖收敛闭合编译与运行时装配；最小回归通过；落地提交：`8852b859`。
- ✅ **IAM 并行（按 10.3：AiReportAnalysisPortImpl 依赖收敛（过渡），保持行为不变）**：将 `bc-ai-report/infrastructure` 的 `AiReportAnalysisPortImpl` 从直接依赖 `UserQueryGateway` 收敛为依赖 `UserNameQueryPort`（Spring 注入改走 contract 端口；端口适配器内部仍委托旧 `UserQueryGateway.findById` 以保持缓存/切面触发点不变；保留一个 `@Deprecated` 旧构造方式仅用于测试过渡）；最小回归通过；落地提交：`c374ae9b`。
- ✅ **AI 报告（测试过渡收敛，保持行为不变）**：将 `AiReportAnalysisPortImplTest` 从 mock `UserQueryGateway` 改为 mock `UserNameQueryPort` 并调用新构造方法，作为下一刀移除 `AiReportAnalysisPortImpl` 中 `@Deprecated` 过渡构造的前置（最小回归通过）；落地提交：`6e99c11b`。
- ✅ **AI 报告（移除过渡构造，保持行为不变）**：在 `AiReportAnalysisPortImplTest` 已切到新构造的前提下，移除 `bc-ai-report/infrastructure` 的 `AiReportAnalysisPortImpl` 中 `@Deprecated` 旧构造与桥接逻辑，使该类彻底不再编译期依赖 `UserQueryGateway`（运行期仍通过 `UserNameQueryPortImpl -> UserQueryGateway.findById` 保持缓存/切面触发点不变）；最小回归通过；落地提交：`b2a6dc15`。
- ✅ **审计（编译闭合前置：bc-audit-infra 显式依赖 bc-iam-contract；保持行为不变）**：为后续将 `bc-audit/infrastructure` 的 `LogInsertionPortImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort` 做前置，在 `bc-audit/infrastructure/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（仅编译边界收敛；最小回归通过）；落地提交：`77bb15b2`。
- ✅ **IAM 并行（按 10.3，编译闭合前置：bc-course-infra 显式依赖 bc-iam-contract；保持行为不变）**：为后续将 `bc-course/infrastructure` 的 `ICourseServiceImpl` 等从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort` 做前置，在 `bc-course/infrastructure/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（仅编译边界收敛；最小回归通过）；落地提交：`7b10d159`。
- ✅ **IAM 并行（按 10.3：ICourseServiceImpl 依赖收敛；保持行为不变）**：将 `bc-course/infrastructure` 的 `ICourseServiceImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（原链路 `UserQueryGateway.findIdByUsername` 本就最终委托 `UserBasicQueryPort`，因此缓存命中/回源顺序与历史语义保持不变）；最小回归通过；落地提交：`24f141a1`。
- ✅ **IAM 并行（按 10.3：ICourseDetailServiceImpl 依赖收敛；保持行为不变）**：将 `bc-course/infrastructure` 的 `ICourseDetailServiceImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（原链路 `UserQueryGateway.findIdByUsername` 本就最终委托 `UserBasicQueryPort`，因此缓存命中/回源顺序与历史语义保持不变）；最小回归通过；落地提交：`d6c1d692`。
- ✅ **IAM 并行（按 10.3：IUserCourseServiceImpl 依赖收敛；保持行为不变）**：将 `bc-course/infrastructure` 的 `IUserCourseServiceImpl` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（原链路 `UserQueryGateway.findIdByUsername` 本就最终委托 `UserBasicQueryPort`，因此缓存命中/回源顺序与历史语义保持不变）；最小回归通过；落地提交：`4dca490e`。
- ✅ **IAM 并行（按 10.3：AvatarManager 依赖收敛；保持行为不变）**：将 `bc-iam/infrastructure` 的 `AvatarManager` 从依赖 `edu.cuit.domain.gateway.user.UserQueryGateway` 收敛为依赖 `edu.cuit.bc.iam.application.port.UserBasicQueryPort`（仅替换该类实际用到的 `findUsernameById`；异常文案/副作用顺序不变；最小回归通过）；落地提交：`56872f30`。
- ✅ **审计（LogInsertionPortImpl 依赖收敛，保持行为不变）**：将 `bc-audit/infrastructure` 的 `LogInsertionPortImpl` 从依赖 `UserQueryGateway` 收敛为依赖 `UserBasicQueryPort`（该调用链原本就最终委托 `UserBasicQueryPortImpl`，因此缓存命中/回源顺序与历史语义保持不变）；最小回归通过；落地提交：`065183ab`。
- ✅ **审计（LogGatewayImpl 依赖收敛，保持行为不变）**：移除 `bc-audit/infrastructure` 的 `LogGatewayImpl` 中未使用的 `UserQueryGateway` 注入字段与 import（不影响任何业务逻辑/异常文案/副作用顺序）；最小回归通过；落地提交：`6d4b3661`。
- ⚠️ **踩坑记录（评教导出端口下沉的阻塞：Maven 循环依赖）**：尝试让 `bc-evaluation-contract` 直接依赖 `eva-domain` 以承载 `EvaRecordEntity` 相关签名会触发循环：`bc-evaluation-contract -> eva-domain -> bc-iam-domain -> bc-iam-contract -> bc-evaluation-contract`（其中 `bc-iam-contract` 必须显式依赖 `bc-evaluation-contract`）。因此“将 `EvaRecordExportQueryPort/EvaRecordCourseQueryPort` 下沉到 `bc-evaluation-contract`”需要先拆解/下沉签名依赖的旧领域实体（或调整接口签名归属），再逐步推进。

**2026-01-30（本次会话：S1（`eva-*` 技术切片整合）试点前置：模板基础设施编译闭合；保持行为不变）**
- ✅ **S0.2 延伸（依赖收敛：bc-course-infra 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `bc-course/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-course/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`ff109643`。
- ✅ **S0.2 延伸（依赖收敛：bc-evaluation-infra 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `bc-evaluation/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-evaluation/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`6a882e4a`。
- ✅ **S0.2 延伸（依赖收敛：bc-iam-infra 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `bc-iam/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-iam/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`f65ce9a7`。
- ✅ **S0.2 延伸（依赖收敛：bc-template-infra 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `bc-template/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-template/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`a8b018a5`。
- ✅ **S0.2 延伸（依赖收敛：bc-audit(infrastructure) 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `bc-audit/infrastructure` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `bc-audit/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`9cd9a96a`。
- ✅ **S0.2 延伸（依赖收敛：bc-audit(application) 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `bc-audit/application/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-audit/application/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`182c34cf`。
- ✅ **S0.2 延伸（依赖收敛：shared-kernel 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `shared-kernel` 无 `src/test` 且源码无 `org.junit.jupiter.*` 引用后，收敛 `shared-kernel/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`61106d70`。
- ✅ **S0.2 延伸（依赖收敛：bc-evaluation-contract 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `bc-evaluation/contract/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-evaluation/contract/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`841ba3c3`。
- ✅ **S0.2 延伸（依赖收敛：bc-messaging-contract 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `bc-messaging-contract/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-messaging-contract/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`f0047fb1`。
- ✅ **S0.2 延伸（依赖收敛：eva-domain 去 bc-iam-contract 编译期依赖，保持行为不变）**：在 `SimpleRoleInfoCO` 已下沉到 `shared-kernel` 的前提下，Serena + `rg` 证伪 `eva-domain/src/main/java` 不再需要 `bc-iam-contract` 提供的类型后，收敛 `eva-domain/pom.xml`：移除对 `bc-iam-contract` 的 Maven 编译期依赖（最小回归通过）；落地提交：`49eadf1f`。
- ✅ **S0.2 延伸（类型下沉：SimpleRoleInfoCO → shared-kernel，保持行为不变）**：为后续收敛 `eva-domain` 对 `bc-iam-contract` 的编译期耦合并保持 `RoleQueryGateway`（跨 BC 复用）稳定，将 `edu.cuit.bc.iam.application.contract.dto.clientobject.user.SimpleRoleInfoCO` 从 `bc-iam-contract` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过）；落地提交：`a04dfd7c`。
- ✅ **S0.2 延伸（shared-kernel：编译闭合前置，保持行为不变）**：为后续将评教等旧领域 `@Entity` 类型逐类下沉到 `shared-kernel` 提供编译闭合支撑，在 `shared-kernel/pom.xml` 显式增加 `cola-component-domain-starter(optional)`（不改变业务语义/装配/副作用顺序；最小回归通过）；落地提交：`a77e1b71`。
- ✅ **S0.2 延伸（AI 报告：编译闭合前置，保持行为不变）**：为保持 AI 报告用例中 `SysException` 的异常语义不变且避免经由 `eva-domain` 间接依赖，在 `bc-ai-report/application/pom.xml` 显式增加 `cola-component-exception` 依赖（最小回归通过）；落地提交：`3e7fd3bd`。
- ✅ **S0.2 延伸（评教：端口下沉，保持行为不变）**：为后续收敛 AI 报告/组合根等依赖方对 `bc-evaluation`（application jar）的编译期耦合，先将 `EvaRecordScoreQueryPort` 从 `bc-evaluation/application` 下沉到 `bc-evaluation-contract`（保持 `package edu.cuit.bc.evaluation.application.port` 与接口签名不变，仅改变 Maven 模块归属；最小回归通过）；落地提交：`78f45ee2`。
- ✅ **S0.2 延伸（AI 报告：依赖收敛，保持行为不变）**：在 Serena + `rg` 证伪 `bc-ai-report/application/src/main/java` 无 `edu.cuit.domain.*` 引用，且 `edu.cuit.client.api.ai/edu.cuit.client.bo.ai` 已由 `bc-ai-report(application)` 内部承载后，收敛 `bc-ai-report/application/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖（最小回归通过）；落地提交：`53a61ee8`。
- ⏸️ **S0.2 延伸（AI 报告 infra：依赖收敛评估，保持行为不变）**：目标是将 `bc-ai-report/infrastructure/pom.xml` 中对 `bc-evaluation`（application jar）的编译期依赖收敛为仅依赖 `bc-evaluation-contract`；但 Serena 证据化确认 `AiReportAnalysisPortImpl` 仍 `import edu.cuit.bc.evaluation.application.port.EvaRecordExportQueryPort`，且该端口接口当前仍定义于 `bc-evaluation/application`（并通过 `EvaRecordCourseQueryPort` 依赖 `EvaRecordEntity` 等旧领域实体），因此本次暂缓收敛以避免引入 `contract` 反向依赖或类型重复。可复现口径：`rg -n "EvaRecordExportQueryPort" bc-ai-report/infrastructure/src/main/java/edu/cuit/app/bcaireport/adapter/AiReportAnalysisPortImpl.java` 与 `rg -n "interface\\s+EvaRecordExportQueryPort\\b" bc-evaluation/application/src/main/java/edu/cuit/bc/evaluation/application/port/EvaRecordExportQueryPort.java`。
- ✅ **S0.2 延伸（依赖收敛：start 去 JUnit4，保持行为不变）**：Serena + `rg` 证伪 `start/src/test/java` 无 `org.junit.Test/org.junit.runner.*` 等 JUnit4 引用后，收敛 `start/pom.xml`：移除 `junit:junit`（JUnit4）依赖（最小回归通过）；落地提交：`e97a5205`。
- ✅ **S0.2 延伸（依赖收敛：bc-ai-report-infra 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `bc-ai-report/infrastructure/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-ai-report/infrastructure/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`770221ea`。
- ✅ **S0.2 延伸（依赖收敛：bc-ai-report(application) 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `bc-ai-report/application/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-ai-report/application/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`317f1859`。
- ✅ **S0.2 延伸（依赖收敛：bc-iam-contract 去无用测试依赖，保持行为不变）**：Serena + `rg` 证伪 `bc-iam/contract/src` 无 `org.junit.jupiter.*` 引用且该模块当前无 `src/test/java` 后，收敛 `bc-iam/contract/pom.xml`：移除 `junit-jupiter(test)` 依赖（最小回归通过）；落地提交：`f623a290`。
- ✅ **S1（`eva-infra` 退场：root reactor 移除模块，保持行为不变）**：在 Serena + `rg` 证据化确认全仓库已无对 `eva-infra` 的 Maven dependency 声明（仅 `eva-infra/pom.xml` 自身 artifactId 声明），且组合根 `start` 已移除 `eva-infra(runtime)` 兜底依赖后，从根 `pom.xml` 的 reactor 中移除 `<module>eva-infra</module>`（保持行为不变）；最小回归通过；落地提交：`0aab4516`。
- ✅ **S1（模板：编译闭合前置，保持行为不变）**：为后续将 `CourseTemplateLockQueryPortImpl` 从 `eva-infra` 归位到 `bc-template-infra` 做前置，在 `bc-template/infrastructure/pom.xml` 显式增加对 `eva-infra-dal` 的 Maven 编译期依赖（仅用于闭合 `Mapper/DO/QueryWrapper` 等依赖；不改业务语义/装配/副作用顺序）；最小回归通过；落地提交：`4f819e13`。补充更新（2026-02-06，保持行为不变）：`bc-template-infra` 已改为依赖 `eva-infra-shared` 显式传递承接 `eva-infra-dal`，不再直依赖 `eva-infra-dal`（最小回归通过；落地：`204aef24`）。
- ✅ **S1（模板：装配责任上推，保持行为不变）**：为后续 `CourseTemplateLockQueryPortImpl` 归位后仍可在运行期被组合根装配，在 `start/pom.xml` 显式增加对 `bc-template-infra` 的 `runtime` 依赖（与后续移除 `eva-infra` 的方向一致；保持行为不变）；最小回归通过；落地提交：`d51975fb`。
- ✅ **S1（模板：基础设施端口实现归位，保持行为不变）**：将 `CourseTemplateLockQueryPortImpl` 从 `eva-infra` 搬运归位到 `bc-template-infra`（保持 `package` 与类内容不变，仅改变 Maven 模块归属）；最小回归通过；落地提交：`9b46d5a7`。
- ✅ **S1（装配责任收敛：组合根去 `eva-infra(runtime)`，保持行为不变）**：在 Serena + `rg` 证据化确认 `eva-infra` 已无业务实现类，且 `start` 运行时装配已由各 `bc-*-infra(runtime)` 显式承接后，移除 `start/pom.xml` 对 `eva-infra` 的 `runtime` 依赖（保持行为不变，仅收敛依赖边界）；最小回归通过；落地提交：`1e2ffa89`。

**2026-01-29（本次会话：IAM 前置拆解（pom），保持行为不变）**
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，编译闭合前置，保持行为不变）**：在 `bc-iam/application/pom.xml` 显式增加对 `bc-iam-domain` 的 Maven 编译期依赖（暂不移除 `eva-domain`，不改业务语义/装配/副作用顺序），用于为下一刀“逐个搬运 `edu.cuit.domain.*` 类型到 `bc-iam-domain`（保持 `package` 不变）”做前置；最小回归通过；落地提交：`aeaa8471`。
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，编译闭合前置，保持行为不变）**：在 `bc-iam/domain/pom.xml` 显式增加对 `bc-iam-contract` 的 Maven 编译期依赖（不改业务语义/装配/副作用顺序），用于为下一刀搬运“仅依赖 IAM cmd/CO”的 `edu.cuit.domain.gateway.user.*` 接口（如 `UserUpdateGateway/RoleUpdateGateway`）做前置；最小回归通过；落地提交：`2fc02fed`。
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）**：用 Serena 证据化确认 `RoleUpdateGateway` 引用面仅在 IAM 基础设施实现中后，将 `edu.cuit.domain.gateway.user.RoleUpdateGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属）；最小回归通过；落地提交：`95e37e8a`。
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）**：用 Serena 证据化确认 `MenuUpdateGateway` 引用面仅在 IAM 基础设施实现中后，将 `edu.cuit.domain.gateway.user.MenuUpdateGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属）；最小回归通过；落地提交：`c31a7a1e`。
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）**：用 Serena 证据化确认 `DepartmentGateway` 引用面仅在 IAM（UseCase/配置/基础设施实现）中后，将 `edu.cuit.domain.gateway.DepartmentGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属；并移除无用 import 以闭合编译）；最小回归通过；落地提交：`68128578`。
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，编译闭合前置，保持行为不变）**：由于 `eva-domain` 内的 `UserEntity` 仍直接引用 `UserUpdateGateway/MenuQueryGateway` 等类型（Serena 证据化），为保持后续“每次只搬运 1 个类”仍可编译闭合，在 `eva-domain/pom.xml` 显式增加对 `bc-iam-domain` 的 Maven 编译期依赖（过渡期；不改业务语义/装配/副作用顺序）；最小回归通过；落地提交：`43e8b66e`。
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）**：用 Serena 证据化确认 `UserUpdateGateway` 引用面在 IAM 基础设施实现、`eva-domain` 残留实体与 `start` 单测中均存在后，将 `edu.cuit.domain.gateway.user.UserUpdateGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属）；最小回归通过；落地提交：`6630277b`。
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）**：用 Serena 证据化确认 `MenuEntity` 引用面集中在 IAM（Convertor/安全鉴权/菜单查询）与 `eva-domain` 残留实体中后，将 `edu.cuit.domain.entity.user.biz.MenuEntity` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属）；最小回归通过；落地提交：`6d700911`。
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）**：用 Serena 证据化确认 `MenuQueryGateway` 引用面在 IAM（旧入口/基础设施实现）、`eva-domain` 残留实体与 `start` 单测中均存在后，将 `edu.cuit.domain.gateway.user.MenuQueryGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属）；最小回归通过；落地提交：`9982af0a`。
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）**：用 Serena 证据化确认 `LdapPersonEntity` 引用面集中在 IAM（LDAP 适配器实现、Convertor 与用例编排）与 `eva-infra-shared` 的 LDAP Convertor 中后，将 `edu.cuit.domain.entity.user.LdapPersonEntity` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属）；最小回归通过；落地提交：`eb36c6ce`。
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，逐类搬运，保持行为不变）**：用 Serena 证据化确认 `LdapPersonGateway` 引用面集中在 IAM（UseCase/基础设施实现）与 `start` 单测后，将 `edu.cuit.domain.gateway.user.LdapPersonGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 与接口签名/注解不变，仅改变 Maven 模块归属）；最小回归通过；落地提交：`ce85525d`。
- 🧾 **文档同步（本会话续，保持行为不变）**：已分别完成三文档同步与 `git push`：`docs: 同步 MenuQueryGateway 归位进度`（`ea3679f5`）、`docs: 同步 LdapPersonEntity 归位进度`（`c3d275d4`）。
- ✅ **踩坑记录（已解决，避免复现）**：此前若在 `RoleEntity` 尚未归位/编译闭合未补齐的情况下直接归位 `RoleQueryGateway`，会因其接口签名依赖 `RoleEntity` 导致 `bc-iam-domain` 编译失败。现已按“先归位实体（含 `RoleEntity/UserEntity`）→ 再归位接口（`RoleQueryGateway`）→ 再收敛 pom”的顺序闭环（见本文件 0.9 里 2026-02-04 的归位条目），后续不再作为阻塞点。

**2026-01-28（本次会话：S0.2 延伸（依赖收敛前置）；保持行为不变）**
- ✅ **S0.2 延伸（端口下沉：EvaRecordCountQueryPort → bc-evaluation-contract，保持行为不变）**：在 Serena 证据化确认引用面（`bc-iam-infra` 的 `UserServiceImpl`、`bc-evaluation-infra` 的 `MsgServiceImpl`、`start` 单测等）后，将 `EvaRecordCountQueryPort` 从 `bc-evaluation/application` 下沉到 `bc-evaluation-contract`（保持 `package edu.cuit.bc.evaluation.application.port` 不变），为后续收敛 `bc-iam/infrastructure/pom.xml` 去 `bc-evaluation` 编译期依赖创造前置；最小回归通过；落地提交：`4c30b02c`。
- ✅ **S0.2 延伸（依赖收敛：bc-iam-infra 去 bc-evaluation 编译期依赖，保持行为不变）**：在 `EvaRecordCountQueryPort` 已下沉到 `bc-evaluation-contract` 且 Serena + `rg` 证伪 `bc-iam/infrastructure/src/main/java` 未引用其他 `bc-evaluation` 应用层类型后，收敛 `bc-iam/infrastructure/pom.xml`：移除对 `bc-evaluation` 的 Maven 编译期依赖，仅保留 `bc-evaluation-contract`（最小回归通过）；落地提交：`42a9e96c`。
- ✅ **S0.2 延伸（IAM：去 `eva-domain` 前置，bc-iam-domain 编译闭合前置，保持行为不变）**：按 `DDD_REFACTOR_PLAN.md` 10.3 的 IAM 小节“Step 0（pom）”先行，在 `bc-iam/domain/pom.xml` 补齐后续搬运 `edu.cuit.domain.entity.user.*` / `edu.cuit.domain.gateway.user.*` 所需的最小编译期依赖（`shared-kernel`、`cola-component-domain-starter`、`spring-context(provided)`、`lombok(provided)`），仅用于编译闭合，不引入新业务语义；最小回归通过；落地提交：`a3d048d0`。
  - 备注（过程记录，便于下次排障）：本次在 `mvnd` 增量/并行编译过程中出现过一次**偶发编译失败**（现象类似注解处理器生成文件冲突），重跑同一最小回归命令后通过；未改变任何业务语义/装配结果。若下次重现，建议在同一步增加 `mvnd -DtrimStackTrace=false` 以保留更完整错误上下文。

**2026-01-27（本次会话：S0.2 延伸（依赖收敛纠偏）；保持行为不变）**
- ✅ **S0.2 延伸（依赖收敛纠偏：bc-iam-contract 恢复 bc-evaluation-contract 编译期依赖，保持行为不变）**：在 Serena 证据化确认 `IUserService#getOneUserScore` 仍返回 `UserSingleCourseScoreCO`（定义于 `bc-evaluation-contract`）后，恢复 `bc-iam/contract/pom.xml` 对 `bc-evaluation-contract` 的显式依赖（用于纠正 `dcf5849a` 的误判；最小回归通过）；落地提交：`918c5d45`。
- ✅ **S1（IAM Controller：UserUpdateController 结构性收敛，保持行为不变）**：抽取 `success()` 统一封装 `CommonResult.success()` 的返回表达；并修正少量参数空格格式以降低噪声（不改 URL/注解/异常/副作用顺序；最小回归通过）；落地提交：`5ee37fd2`。
- ✅ **S1（IAM Controller：DepartmentController 结构性收敛，保持行为不变）**：抽取 `success(...)` 统一封装 `CommonResult.success(...)` 的返回表达（仍保持“先调用 service → 再返回包装”的执行顺序不变；不改 URL/注解/异常/副作用顺序；最小回归通过）；落地提交：`fbc5fb74`。
- ✅ **S1（IAM Controller：AuthenticationController 结构性收敛，保持行为不变）**：抽取 `success(...)`/`success()` 统一封装 `CommonResult.success(...)` 的返回表达（仍保持“先调用 service → 再返回包装”的执行顺序不变；不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地提交：`fd9e4d1c`。
- ✅ **S1（IAM Controller：MenuUpdateController 结构性收敛，保持行为不变）**：抽取 `success()` 统一封装 `CommonResult.success()` 的返回表达（严格保持 `menuService.create(...)` → `LogUtils.logContent(...)` → `return` 的执行顺序不变；不改 URL/注解/权限/异常/日志/副作用顺序；最小回归通过）；落地提交：`44bc649d`。
- ✅ **S1（IAM Controller：RoleUpdateController 结构性收敛，保持行为不变）**：抽取 `success()` 统一封装 `CommonResult.success()` 的返回表达（严格保持 `roleService.create(...)` → `LogUtils.logContent(...)` → `return` 的执行顺序不变；不改 URL/注解/权限/异常/日志/副作用顺序；最小回归通过）；落地提交：`c81eb2e0`。

**2026-01-28（本次会话：S1（其他 BC Controller 收敛）；保持行为不变）**
- ✅ **S1（审计 Controller：LogController 结构性收敛，保持行为不变）**：抽取泛型 `success(data)` 统一封装 `CommonResult.success(data)` 的返回表达（仍保持“先调用 service → 再返回包装”的执行顺序不变；不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地提交：`14c9ab77`。
- ✅ **S1（消息 Controller：MessageController 结构性收敛，保持行为不变）**：抽取 `success(data)`/`success()` 统一封装 `CommonResult.success(...)` 的返回表达（仍保持“先调用 service → 再返回包装”的执行顺序不变；不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地提交：`9a3ef681`。
- ✅ **S1（课程 Controller：SemesterController 结构性收敛，保持行为不变）**：抽取泛型 `success(data)` 统一封装 `CommonResult.success(data)` 的返回表达（仍保持“先调用 service → 再返回包装”的执行顺序不变；不改 URL/注解/异常/副作用顺序；最小回归通过）；落地提交：`13892e55`。
- ✅ **S1（课程 Controller：ClassroomController 结构性收敛，保持行为不变）**：抽取 `success(data)` 统一封装 `CommonResult.success(data)` 的返回表达（仍保持“先调用 service → 再返回包装”的执行顺序不变；不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地提交：`99eb17c0`。
- ✅ **S1（课程 Controller：QueryCourseController 结构性收敛，保持行为不变）**：抽取泛型 `success(data)` 统一封装 `CommonResult.success(data)` 的返回表达（仍保持“先调用 service → 再返回包装”的执行顺序不变；不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地提交：`4c54337f`。
- ✅ **S1（课程 Controller：QueryUserCourseController 结构性收敛，保持行为不变）**：抽取泛型 `success(data)` 统一封装 `CommonResult.success(data)` 的返回表达（仍保持“先调用 service → 再返回包装”的执行顺序不变；不改 URL/注解/权限/异常/副作用顺序；最小回归通过）；落地提交：`c8ce3522`。

**2026-01-26（本次会话：S1.1（eva-adapter 退场）+ S0.2 延伸（依赖方 pom 收敛）；保持行为不变）**
- ✅ **S1.1（eva-adapter 退场：root reactor 移除模块，保持行为不变）**：在 Serena 证据化确认（当时）全仓库仅 `eva-adapter/pom.xml` 声明 `<artifactId>eva-adapter</artifactId>`，且根 `pom.xml` 仅残留 `<module>eva-adapter</module>` 的前提下，从根 `pom.xml` 的 reactor 中移除 `eva-adapter` 模块（最小回归通过）；落地提交：`86842a1f`。
- ✅ **S1.1（eva-adapter 退场：删除模块 pom，保持行为不变）**：在 `eva-adapter` 已从 root reactor 退场的前提下，删除 `eva-adapter/pom.xml`，使全仓库 `**/pom.xml` 中不再出现 `<artifactId>eva-adapter</artifactId>`（最小回归通过）；落地提交：`ed244cad`。
 - ✅ **S1.1（组合根收敛：start 去 eva-adapter 依赖，保持行为不变）**：在 Serena 证据化确认 `eva-adapter/src/main/java` 已无源码，且审计日志入口 `LogController` 已归位 `bc-audit-infra` 并由组合根显式依赖闭合运行期装配后，移除 `start/pom.xml` 对 `eva-adapter` 的 Maven 依赖（最小回归通过）；落地提交：`92a70a9e`。
- ✅ **S0.2 延伸（依赖收敛：eva-adapter 去 bc-audit 编译期依赖，保持行为不变）**：在 Serena 证据化确认 `eva-adapter/src/main/java` 无源码引用 `ILogService/OperateLogCO/LogModuleCO` 等审计类型后，收敛 `eva-adapter/pom.xml`：移除对 `bc-audit` 的 Maven 编译期依赖（最小回归通过）；落地提交：`3aa49c66`。
- ✅ **S0.2 延伸（依赖收敛：eva-adapter 去 bc-ai-report 编译期依赖，保持行为不变）**：在 Serena 证据化确认 `eva-adapter/src/main/java` 无 AI 报告相关源码引用后，收敛 `eva-adapter/pom.xml`：移除对 `bc-ai-report` 的 Maven 编译期依赖（最小回归通过）；落地提交：`a7f85ac7`。
- ✅ **S0.2 延伸（依赖收敛：eva-adapter 去 bc-*contract/shared-kernel 编译期依赖，保持行为不变）**：在 Serena 证据化确认 `eva-adapter/src/main/java` 无源码后，进一步收敛 `eva-adapter/pom.xml`：移除 `shared-kernel` 与 `bc-iam-contract` / `bc-evaluation-contract` / `bc-messaging-contract` 的 Maven 编译期依赖（最小回归通过）；落地提交：`84be3a4b`。
- ✅ **S0.2 延伸（依赖收敛：eva-domain 去 bc-evaluation-contract 编译期依赖，保持行为不变）**：在 Serena 证据化确认 `eva-domain/src/main/java` 无评教 contract 类型引用后，收敛 `eva-domain/pom.xml`：移除对 `bc-evaluation-contract` 的 Maven 编译期依赖（最小回归通过）；落地提交：`ccbb1cf9`。
- ✅ **S0.2 延伸（依赖收敛：eva-infra-shared 去 bc-evaluation-contract 编译期依赖，保持行为不变）**：在 Serena 证据化确认 `eva-infra-shared/src/main/java` 无评教 contract 类型引用后，收敛 `eva-infra-shared/pom.xml`：移除对 `bc-evaluation-contract` 的 Maven 编译期依赖（最小回归通过）；落地提交：`d28a5904`。
- ✅ **S0.2 延伸（依赖收敛：bc-iam-contract 去 bc-evaluation-contract 编译期依赖，保持行为不变）**：在 Serena 证据化确认 `bc-iam/contract/src/main/java` 无评教 contract 类型引用后，收敛 `bc-iam/contract/pom.xml`：移除对 `bc-evaluation-contract` 的 Maven 编译期依赖（最小回归通过）；落地提交：`dcf5849a`。（后续证实误判，已恢复依赖：`918c5d45`）
- ✅ **S1（IAM Controller：UserQueryController 小幅重构，保持行为不变）**：对 `UserQueryController` 进行纯结构性整理（简化临时变量与返回包装；不改 URL/注解/异常/副作用顺序；最小回归通过）；落地提交：`a542abff`。
- ✅ **S1（IAM Controller：MenuQueryController 小幅重构，保持行为不变）**：对 `MenuQueryController` 进行纯结构性整理（简化临时变量与返回包装；不改 URL/注解/异常/副作用顺序；最小回归通过）；落地提交：`e388ae84`。
- ✅ **S1（IAM Controller：RoleQueryController 小幅重构，保持行为不变）**：对 `RoleQueryController` 进行纯结构性整理（简化临时变量与返回包装；不改 URL/注解/异常/副作用顺序；最小回归通过）；落地提交：`bb134377`。

**2026-01-19（本次会话：S1 主线（收敛 eva-adapter 残留 Controller）；保持行为不变）**
- ✅ **S1（入口归位：LogController，保持行为不变）**：将 `LogController` 从 `eva-adapter` 归位到 `bc-audit/infrastructure`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；Serena 证伪无“代码级引用点”，仅由 Spring 扫描发现；最小回归通过）；落地提交：`b592cc0f`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位前置：bc-audit-infra 编译闭合，保持行为不变）**：为后续将 `LogController` 从 `eva-adapter` 归位到 `bc-audit/infrastructure` 做编译闭合前置，在 `bc-audit/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`spring-boot-starter-validation`、`zym-spring-boot-starter-common`、`zym-spring-boot-starter-security`（运行时 classpath 已存在，仅显式化；最小回归通过）；落地提交：`2464d2b9`。
- ✅ **S1（入口归位：MessageController，保持行为不变）**：将 `MessageController` 从 `eva-adapter` 归位到 `bc-messaging`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；Serena 证伪无“代码级引用点”，仅由 Spring 扫描发现；最小回归通过）；落地提交：`7b076019`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位前置：bc-messaging 编译闭合，保持行为不变）**：为后续将 `MessageController` 从 `eva-adapter` 归位到 `bc-messaging` 做编译闭合前置，在 `bc-messaging/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`、`zym-spring-boot-starter-security`、`shared-kernel`（仅编译闭合；最小回归通过）；落地提交：`aa7d57bb`。
- ✅ **S1（入口归位：DeleteEvaController，保持行为不变）**：将 `DeleteEvaController` 从 `eva-adapter` 归位到 `bc-evaluation/infrastructure`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；Serena 证伪无“代码级引用点”，仅由 Spring 扫描发现；最小回归通过）；落地提交：`d1471ff5`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位：UpdateEvaController，保持行为不变）**：将 `UpdateEvaController` 从 `eva-adapter` 归位到 `bc-evaluation/infrastructure`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；Serena 证伪无“代码级引用点”，仅由 Spring 扫描发现；最小回归通过）；落地提交：`3972b7e4`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位：EvaConfigUpdateController，保持行为不变）**：将 `EvaConfigUpdateController` 从 `eva-adapter` 归位到 `bc-evaluation/infrastructure`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；Serena 证伪无“代码级引用点”，仅由 Spring 扫描发现；最小回归通过）；落地提交：`b5530281`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位：EvaQueryController，保持行为不变）**：将 `EvaQueryController` 从 `eva-adapter` 归位到 `bc-evaluation/infrastructure`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；Serena 证伪无“代码级引用点”，仅由 Spring 扫描发现；最小回归通过）；落地提交：`d101ce07`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位：EvaConfigQueryController，保持行为不变）**：将 `EvaConfigQueryController` 从 `eva-adapter` 归位到 `bc-evaluation/infrastructure`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；Serena 证伪无“代码级引用点”，仅由 Spring 扫描发现；最小回归通过）；落地提交：`5e7537ef`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位：EvaStatisticsController，保持行为不变）**：将 `EvaStatisticsController` 从 `eva-adapter` 归位到 `bc-evaluation-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`f533261a`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位前置：bc-evaluation-infra 编译闭合，保持行为不变）**：为后续将 `EvaStatisticsController` 从 `eva-adapter` 归位到 `bc-evaluation/infrastructure` 做前置，在 `bc-evaluation/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`、`bc-evaluation-contract`、`bc-ai-report`（仅编译闭合；最小回归通过）；落地提交：`a5e1eb52`。
- ✅ **S1（入口归位：UpdateCourseController，保持行为不变）**：将 `UpdateCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`1d03d987`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位：DeleteCourseController，保持行为不变）**：将 `DeleteCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`4b6219b9`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位：QueryUserCourseController，保持行为不变）**：将 `QueryUserCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`bc37fa17`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1 前置（支撑类归位：课程适配层时间计算工具，保持行为不变）**：为后续将 `QueryUserCourseController` 从 `eva-adapter` 归位到 `bc-course-infra` 做编译闭合前置，将 `edu.cuit.adapter.controller.course.util.CalculateClassTime` 从 `eva-adapter` 下沉到 `shared-kernel`（保持 `package` 与逻辑不变，仅搬运归位；最小回归通过）；落地提交：`8a3f738c`。
- ✅ **S1（入口归位：QueryCourseController，保持行为不变）**：将 `QueryCourseController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`1b9a6fc7`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位：SemesterController，保持行为不变）**：将 `SemesterController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`0257ddd0`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位：RoleUpdateController，保持行为不变）**：将 `RoleUpdateController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`80888bed`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位：DepartmentController，保持行为不变）**：将 `DepartmentController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`3e66a7b4`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。
- ✅ **S1（入口归位前置：bc-course-infra 编译闭合，保持行为不变）**：为后续将 `ClassroomController` 从 `eva-adapter` 归位到 `bc-course-infra` 做前置，在 `bc-course/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`（运行期 classpath 已存在，仅显式化编译期依赖；最小回归通过）；落地提交：`8915db14`。
- ✅ **S1（入口归位：ClassroomController，保持行为不变）**：将 `ClassroomController` 从 `eva-adapter` 归位到 `bc-course-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`132f32f5`。`eva-adapter` 残留 Controller 口径以 0.10.2 为准。

**2026-01-17（本次会话：S1 前置推进（清空 eva-app 残留支撑类）；保持行为不变）**
- ✅ **基线复核（用于避免口径漂移）**：当前分支 `ddd`；`git merge-base --is-ancestor 2e4c4923 HEAD` 退出码为 0；本次会话最终基线以 `git rev-parse --short HEAD` 为准。
- ✅ **S1（入口归位前置：bc-iam-infra 编译闭合，保持行为不变）**：为后续将 `AuthenticationController` 从 `eva-adapter` 归位到 `bc-iam-infra` 做前置，在 `bc-iam/infrastructure/pom.xml` 补齐 `spring-boot-starter-web`、`zym-spring-boot-starter-common`、`commons-lang3`（运行期 classpath 已存在，仅显式化编译期依赖；最小回归通过）；落地提交：`42d44f0b`。
- ✅ **S1（入口归位前置：bc-iam-infra 编译闭合补齐，保持行为不变）**：为后续归位 `UserUpdateController` 做前置，在 `bc-iam/infrastructure/pom.xml` 补齐 `cola-component-exception`、`shared-kernel`、`eva-base-common`（避免依赖隐式传递导致后续收敛漂移；保持行为不变；最小回归通过）；落地提交：`ddd5ff2a`。
- ✅ **S1（入口归位前置：bc-iam-infra 编译闭合补齐，保持行为不变）**：为后续归位 `UserQueryController`（依赖 `IEvaStatisticsService`）做前置，在 `bc-iam/infrastructure/pom.xml` 补齐 `bc-evaluation-contract`（保持行为不变；最小回归通过）；落地提交：`0781952e`。
- ✅ **S1（入口归位：AuthenticationController，保持行为不变）**：将 `AuthenticationController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`94a00022`。
- ✅ **S1（入口归位：UserUpdateController，保持行为不变）**：将 `UserUpdateController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`367f781d`。当前 `eva-adapter` 残留 Controller 口径更新为 **20**（见 0.10.1）。
- ✅ **S1（入口归位：UserQueryController，保持行为不变）**：将 `UserQueryController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`f7c5d219`。当前 `eva-adapter` 残留 Controller 口径更新为 **19**（见 0.10.1）。
- ✅ **S1（入口归位：RoleQueryController，保持行为不变）**：将 `RoleQueryController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`e7d51beb`。当前 `eva-adapter` 残留 Controller 口径更新为 **18**（见 0.10.1）。
- ✅ **S0.2 延伸（依赖方 pom 收敛：组合根收敛 start→eva-adapter 编译期依赖，保持行为不变）**：Serena 证伪 `start/src/main/java` 与 `start/src/test/java` 未引用 `edu.cuit.adapter.*` 后，将 `start/pom.xml` 对 `eva-adapter` 的依赖 scope 改为 `runtime`（运行期装配不变，仅收敛编译期边界；阶段性中间态）；最小回归通过；落地提交：`045891d1`。（后续已在 `92a70a9e` 移除组合根对 `eva-adapter` 的 Maven 依赖）
- ✅ **S0.2 延伸闭环项复核（保持行为不变）**：
  - Serena 证伪：`eva-app/src/main/java` 未发现 `org.springframework.web.socket`、`edu.cuit.client.api.msg`、`edu.cuit.bc.evaluation.*` 相关引用面（命中为 0）。
  - `rg` 口径复核：`spring-boot-starter-websocket` 仅命中 `start/pom.xml`；`eva-app` 已从组合根与 reactor 退场（见下方提交点）。
- ✅ **S1 前置（支撑类归位：评教配置 Convertor，保持行为不变）**：将 `EvaConfigBizConvertor` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor.eva` 不变；类内容不变；最小回归通过）；落地提交：`5514463d`。
- ✅ **S1 前置（支撑类归位：评教记录 Convertor，保持行为不变）**：将 `EvaRecordBizConvertor` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor.eva` 不变；类内容不变；最小回归通过）；落地提交：`b3a3dab2`。
- ✅ **S1 前置（支撑类归位：评教任务 Convertor，保持行为不变）**：将 `EvaTaskBizConvertor` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor.eva` 不变；类内容不变；最小回归通过）；落地提交：`80b3937c`。
- ✅ **S1 前置（支撑类归位：评教模板 Convertor，保持行为不变）**：将 `EvaTemplateBizConvertor` 从 `eva-app` 归位到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor.eva` 不变；类内容不变；最小回归通过）；落地提交：`470078ba`。
- ✅ **S1 前置（支撑类归位：评教配置 Service，保持行为不变）**：将 `EvaConfigService` 从 `eva-app` 归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；类内容不变；最小回归通过）；落地提交：`d7defd75`。
- ✅ **S1 前置（支撑类归位：课程详情查询 Exec，保持行为不变）**：将 `UserCourseDetailQueryExec` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.operate.course.query` 不变；类内容不变；最小回归通过）；落地提交：`00983fd2`。
- ✅ **S1 前置（支撑类归位：课程导入辅助 Exec，保持行为不变）**：将 `FileImportExec` 从 `eva-app` 归位到 `bc-course-infra`（落点：`bc-course/infrastructure`；保持 `package edu.cuit.app.service.operate.course.update` 不变；仅做搬运 + 清理未使用 import；行为不变；最小回归通过）；落地提交：`960dfbaf`。
- ✅ **S1 前置（支撑类归位：operate 包 package-info，保持行为不变）**：将 `package-info.java` 从 `eva-app` 归位到 `bc-course-infra`（落点：`bc-course/infrastructure`；保持 `package edu.cuit.app.service.operate` 不变；仅搬运注释；行为不变；最小回归通过）；落地提交：`c8ddace5`。
- ✅ **S0.2 延伸（依赖方 pom 收敛：组合根去 eva-app 依赖，保持行为不变）**：在 Serena 证据化盘点 `start/pom.xml` 依赖闭包后，移除 `start/pom.xml` 对 `eva-app` 的依赖，并显式引入 `eva-infra(runtime)` 以保持运行期 classpath 不变；最小回归通过；落地提交：`0a9ff564`。（后续已在 `1e2ffa89` 将组合根的 `eva-infra(runtime)` 兜底依赖移除。）
- ✅ **S1 前置（模块退场准备：reactor 移除 eva-app，保持行为不变）**：在 Serena + `rg` 证据化确认全仓库已无对 `eva-app` 的 Maven 依赖后，从根 `pom.xml` 的 reactor 中移除 `eva-app` 模块；最小回归通过；落地提交：`b5f15a4b`。
- ✅ **S1 前置（模块退场收尾：删除 eva-app/pom.xml，保持行为不变）**：在确认 `eva-app/` 目录仅剩 `pom.xml` 且已不在 reactor 后，删除 `eva-app/pom.xml`；最小回归通过；落地提交：`4bfa9d40`。
- ✅ **S1 前置（编译闭合：eva-adapter 依赖补齐，保持行为不变）**：触碰任意 Controller 会触发 `eva-adapter` 全量重新编译，暴露其 `pom.xml` 缺少编译期依赖的问题；因此收敛 `eva-adapter/pom.xml` 并补齐所需的最小编译依赖闭合（仅为编译与运行时加载注解/DTO/契约类型；不改业务语义）；最小回归通过；落地提交：`56273162`。
- 📌 **当前仍未完成（进入 S1 前置的硬事实口径，保持行为不变）**：
  - `eva-adapter` 仍残留 7 个 `*Controller.java`（口径：`fd -t f 'Controller\\.java$' eva-adapter/src/main/java | wc -l`）。
- 🎯 **下一步建议（保持行为不变；每步只改 1 个类或 1 个 pom）**：
  1) ✅ 已完成：`eva-adapter` 残留 `*Controller` 已清零，且模块已从 reactor 退场并删除 `pom.xml`（保持行为不变；详见 0.9）。下一刀建议：转向 `bc-iam` 的 Controller 入口壳结构性收敛（每次只改 1 个类），并并行推进依赖方 `pom.xml` 编译期依赖收敛（移除前必须 Serena + `rg` 双证据，避免误判）。
  2) 并行主线（依赖方 pom 收敛，单 pom）：继续挑选 1 个依赖方模块（优先 `eva-domain` / `eva-infra-shared`）做“Serena 证据化盘点 → 单 pom 收敛”（保持行为不变）。

**2026-01-16（本次会话：S0.2 延伸（依赖收敛：eva-app 去 bc-messaging-contract），保持行为不变）**
- ✅ **消息契约（依赖收敛：eva-app 去 bc-messaging-contract 编译期依赖，保持行为不变）**：Serena 证据化确认 `eva-app/src/main/java` 未引用消息契约关键类型（例如 `IMsgService`、`SendMessageCmd`）后，收敛 `eva-app/pom.xml`：移除 `bc-messaging-contract` 的 Maven 编译期依赖（最小回归通过）；落地提交：`b92314ef`。
- ✅ **S1 前置（支撑类归位：Sa-Token 配置，保持行为不变）**：将 `SaTokenConfig` 从 `eva-app` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.app.config` 不变；类内容不变；最小回归通过）；落地提交：`c767663f`。
- ✅ **S1 前置（支撑类归位：Sa-Token 拦截器配置，保持行为不变）**：将 `SaTokenInterceptorConfig` 从 `eva-app` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.app.config` 不变；类内容不变；最小回归通过）；落地提交：`a14a4c68`。
- ✅ **S1 前置（支撑类归位：Sa-Token 权限加载，保持行为不变）**：将 `StpInterfaceImpl` 从 `eva-app` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.app.security` 不变；类内容不变；最小回归通过）；落地提交：`bcce5582`。
- ✅ **S1 前置（支撑类归位：评教切面配置，保持行为不变）**：将 `AspectConfig` 从 `eva-app` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.app.aop` 不变；类内容不变；最小回归通过；落地提交：`18901dab`）。（后续进一步瘦身：`AspectConfig` 已从 `eva-infra-shared` 归位到 `bc-course/infrastructure`，保持行为不变；落地：`33dbaf6f`）
- ✅ **S0.2 延伸（依赖收敛：eva-domain 去 spring-statemachine-core 编译期依赖，保持行为不变）**：在 Serena + `rg` 证伪 `eva-domain/src/main/java` 无 `statemachine` 相关引用后，收敛 `eva-domain/pom.xml`：移除 `spring-statemachine-core` Maven 依赖（最小回归通过）；落地提交：`12c8d4bb`。
- 🔁 **S0.2 延伸（依赖收敛纠偏：bc-audit(application) 恢复 eva-domain 编译期依赖，保持行为不变）**：此前仅按 `edu.cuit.domain.*` 引用面误判可去 `eva-domain`（落地：`bf90c040`）；后续在最小回归触发重新编译时暴露：`bc-audit/application` 仍引用 `edu.cuit.client.bo.SysLogBO`（定义于 `eva-domain/src/main/java/edu/cuit/client/bo/SysLogBO.java`，包名保持不变）。因此恢复 `bc-audit/application/pom.xml` 对 `eva-domain` 的 Maven 编译期依赖以闭合编译（最小回归通过）；纠偏提交：`b47d71ab`。
- ✅ **S0.2 延伸（类型下沉：PaginationResultEntity 归位 shared-kernel，保持行为不变）**：为逐步减少各 BC（含 IAM）对 `eva-domain` 的编译期耦合，将通用分页实体 `edu.cuit.domain.entity.PaginationResultEntity` 从 `eva-domain` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过）；落地提交：`d31bb204`。
- ✅ **S0.2 延伸（类型下沉：SysLogBO 归位 shared-kernel，保持行为不变）**：为进一步减少依赖方对 `eva-domain` 的编译期耦合，将日志 BO `edu.cuit.client.bo.SysLogBO` 从 `eva-domain` 下沉到 `shared-kernel`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过）；落地提交：`96de4244`。
- ✅ **S0.2 延伸（依赖收敛：bc-audit(application) 去 eva-domain 编译期依赖，保持行为不变）**：在 `SysLogBO` 已下沉到 `shared-kernel` 且 Serena + `rg` 证伪 `bc-audit/application/src/main/java` 无 `edu.cuit.domain.*` 引用后，收敛 `bc-audit/application/pom.xml`：移除对 `eva-domain` 的 Maven 编译期依赖（最小回归通过）；落地提交：`0ee2a831`。

**2026-01-15（本次会话：S0.2 延伸（websocket 闭环 + 审计/AI 依赖收敛），保持行为不变）**
- ✅ **websocket（依赖收敛：eva-app 去 websocket starter 编译期依赖，保持行为不变）**：在 Serena 证伪 `eva-app/src/main/java` 不再 `import org.springframework.web.socket.*` 后，已收敛 `eva-app/pom.xml`：移除 `spring-boot-starter-websocket` 的编译期依赖；运行期由组合根 `start` 显式兜底（最小回归通过）；落地提交：`4213a95a`。
- ✅ **websocket（配置归位：WebSocketConfig，保持行为不变）**：将 `WebSocketConfig` 从 `eva-app` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.app.config` 不变；类内容不变；最小回归通过）；落地提交：`e03e60f9`。
- ✅ **websocket（配置/拦截器归位：WebSocketInterceptor，保持行为不变）**：将 `WebSocketInterceptor` 从 `eva-app` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.app.config` 不变；类内容不变；最小回归通过）；落地提交：`df06f0e6`。
- ✅ **websocket（编译闭合前置：eva-infra-shared 补齐 security 依赖，保持行为不变）**：为后续归位 `WebSocketInterceptor`（依赖 `cn.dev33.satoken.stp.StpUtil`）做编译闭合前置，在 `eva-infra-shared/pom.xml` 补齐 `zym-spring-boot-starter-security` 依赖（运行时 classpath 已存在，仅显式化；最小回归通过）；落地提交：`fa2faf1d`。
- ✅ **审计（组合根归位：BcAuditConfiguration，保持行为不变）**：将 `BcAuditConfiguration` 从 `eva-app` 搬运归位到 `bc-audit-infra`（保持 `package edu.cuit.app.config` 不变；Bean 定义/装配顺序不变；最小回归通过）；落地提交：`5a4d726b`。
- ✅ **审计（编译闭合前置：bc-audit-infra 补齐 logging 依赖，保持行为不变）**：为后续归位 `LogServiceImpl/LogBizConvertor`（依赖 `LogManager/OperateLogBO`）做编译闭合前置，在 `bc-audit/infrastructure/pom.xml` 补齐 `zym-spring-boot-starter-logging` 依赖（运行时 classpath 已存在，仅显式化；最小回归通过）；落地提交：`e7e13736`。
- ✅ **审计（归位支撑类：LogBizConvertor，保持行为不变）**：将 `LogBizConvertor` 从 `eva-app` 搬运归位到 `bc-audit-infra`（保持 `package edu.cuit.app.convertor` 不变；MapStruct 映射规则/表达式不变；最小回归通过）；落地提交：`99960c7f`。
- ✅ **审计（旧入口壳归位：LogServiceImpl，保持行为不变）**：将 `LogServiceImpl` 从 `eva-app` 搬运归位到 `bc-audit-infra`（保持 `package edu.cuit.app.service.impl` 不变；`@PostConstruct` 注册监听器逻辑不变；异步执行语义/异常文案/日志与副作用顺序完全不变；最小回归通过）；落地提交：`d0af2bac`。
- ✅ **审计（装配责任上推：start 显式依赖 bc-audit-infra，保持行为不变）**：为后续收敛 `eva-app/pom.xml` 的审计依赖边界，已在 `start/pom.xml` 增加对 `bc-audit-infra` 的 `runtime` 依赖（与原 transitive 结果等价，仅显式化；最小回归通过）；落地提交：`d6d9c480`。
- ✅ **审计（依赖收敛：eva-app 去 bc-audit/bc-audit-infra 编译期依赖，保持行为不变）**：在 `rg` 证伪 `eva-app/src/main/java` 不再 `import edu.cuit.bc.audit.*` 后，收敛 `eva-app/pom.xml`：移除对 `bc-audit`（application jar）与 `bc-audit-infra` 的 Maven 编译期依赖；运行期装配由组合根 `start` 显式兜底（最小回归通过）；落地提交：`b3ea5f58`。
- ✅ **AI 报告（装配责任上推：start 显式依赖 bc-ai-report-infra，保持行为不变）**：为后续收敛 `eva-app/pom.xml` 的 AI 报告依赖边界，已在 `start/pom.xml` 增加对 `bc-ai-report-infra` 的 `runtime` 依赖（与原 transitive 结果等价，仅显式化；最小回归通过）；落地提交：`08862a4b`。
- ✅ **AI 报告（依赖收敛：eva-app 去 bc-ai-report/bc-ai-report-infra 编译期依赖，保持行为不变）**：在 Serena 证据化确认 `eva-app/src/main/java` 无 AI 相关直引后，收敛 `eva-app/pom.xml`：移除对 `bc-ai-report`（application jar）与 `bc-ai-report-infra` 的 Maven 编译期依赖；运行期装配由组合根 `start` 显式兜底（最小回归通过）；落地提交：`2a4736c0`。
- ✅ **websocket（装配责任上推：start 显式依赖 websocket starter，保持行为不变）**：为后续收敛 `eva-app/pom.xml` 的 websocket 依赖边界，已在 `start/pom.xml` 增加对 `spring-boot-starter-websocket` 的依赖（与原 transitive 结果等价，仅显式化；最小回归通过）；落地提交：`97b543b1`。
- ✅ **websocket（支撑类归位：MessageChannel，保持行为不变）**：将 `MessageChannel` 从 `eva-app` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.app.websocket` 不变；类内容不变；最小回归通过）；落地提交：`0fbc4aef`。
- ✅ **websocket（支撑类归位：UriUtils，保持行为不变）**：将 `UriUtils` 从 `eva-app` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.app.util` 不变；类内容不变；用于为后续归位 `WebSocketInterceptor` 做编译闭合前置；最小回归通过）；落地提交：`c1a10d2d`。
- ✅ 最小回归通过（Java17）：命令见 0.10。

**2026-01-14（本次会话：评教 S0.2 延伸闭环（旧入口归位 → 依赖收敛前置），保持行为不变）**
- ✅ **评教（旧入口壳归位：UserEvaServiceImpl，保持行为不变）**：将 `UserEvaServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；类内容不变；`@CheckSemId` 触发点、`StpUtil` 登录态解析次数与顺序、异常文案与副作用顺序完全不变；最小回归通过）；落地提交：`f4238a5c`。
- ✅ **评教（旧入口壳归位支撑前置：websocket 依赖补齐，保持行为不变）**：为后续归位 `MsgServiceImpl`（依赖 `WebsocketManager`）做编译闭合前置，在 `eva-infra-shared/pom.xml` 补齐 `spring-websocket` 与 `commons-lang3` 依赖（运行时 classpath 已存在；保持行为不变；最小回归通过）；落地提交：`82609bda`。
- ✅ **评教（旧入口壳归位支撑前置：WebsocketManager 归位，保持行为不变）**：将 `WebsocketManager` 从 `eva-app` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.app.websocket` 不变；类内容不变；异常文案/日志内容与调用顺序不变；最小回归通过）；落地提交：`406186ae`。
- ✅ **评教（旧入口壳归位支撑前置：MsgBizConvertor 归位，保持行为不变）**：将 `MsgBizConvertor` 从 `eva-app` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor` 不变；类内容不变；MapStruct 映射规则/表达式与异常文案不变；最小回归通过）；落地提交：`c69f494f`。
- ✅ **评教（旧入口壳归位：MsgServiceImpl，保持行为不变）**：将 `MsgServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl` 不变；类内容不变；事务边界/异常文案/日志与 websocket 推送副作用顺序完全不变；最小回归通过）；落地提交：`5dea9347`。
- ✅ **评教（依赖收敛：eva-app 去 bc-evaluation 编译期依赖，保持行为不变）**：在 Serena + `rg` 证伪 `eva-app/src/main/java` 已不再引用 `edu.cuit.bc.evaluation.*` 后，收敛 `eva-app/pom.xml`：移除对 `bc-evaluation`（application jar）的 Maven 编译期依赖（证据口径：`rg -n '^import\\s+edu\\.cuit\\.bc\\.evaluation' eva-app/src/main/java` → 0）；最小回归通过；落地提交：`2b42db5d`。
- ✅ 最小回归通过（Java17）：命令见 0.10。

**2026-01-13（补充进展：评教依赖收敛补齐（S0.2 延伸），保持行为不变）**
- ✅ **评教（装配责任上推：start 显式依赖 bc-evaluation-infra，保持行为不变）**：在 `start/pom.xml` 增加对 `bc-evaluation-infra` 的 `runtime` 依赖（与原 transitive 结果等价，仅显式化；最小回归通过）；落地提交：`0f20d0cd`。
- ✅ **评教（依赖收敛：eva-app 去 bc-evaluation-infra 依赖，保持行为不变）**：在 Serena + `rg` 证伪 `eva-app/src/main/java` 不再直接引用 `edu.cuit.infra.bcevaluation.*` 实现类型后，收敛 `eva-app/pom.xml`：移除对 `bc-evaluation-infra` 的依赖；运行期装配由组合根 `start` 显式兜底（最小回归通过）；证据口径：`rg -n '^import\\s+edu\\.cuit\\.infra\\.bcevaluation' eva-app/src/main/java` → 0；落地提交：`e9feeb56`。
- ✅ **评教（事件发布器归位：SpringAfterCommitDomainEventPublisher，保持行为不变）**：将 `SpringAfterCommitDomainEventPublisher` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.event` 不变；`@Component` 装配语义不变；事务提交后发布逻辑不变；最小回归通过）；落地提交：`1b9e275c`。
- ✅ **评教（监听器归位前置：bc-messaging-contract 依赖补齐，保持行为不变）**：为后续归位评教事务事件监听器（需要 `IMsgService` 契约类型）做编译闭合前置，在 `bc-evaluation-infra/pom.xml` 补齐对 `bc-messaging-contract` 的编译期依赖（不引入实现侧依赖；最小回归通过）；落地提交：`bcb8df45`。
- ✅ **评教（旧入口壳归位前置：security 依赖补齐，保持行为不变）**：为后续归位 `EvaTaskServiceImpl/UserEvaServiceImpl`（依赖 `StpUtil` 登录态解析）做编译闭合前置，在 `bc-evaluation-infra/pom.xml` 补齐 `zym-spring-boot-starter-security` 依赖（运行时 classpath 已存在；保持行为不变；最小回归通过）；落地提交：`202a6386`。
- ✅ **评教（事务事件监听器归位：EvaluationSubmittedMessageCleanupListener，保持行为不变）**：将 `EvaluationSubmittedMessageCleanupListener` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.event.listener` 不变；监听器触发点/调用顺序不变；仅将注入类型收窄为 `IMsgService` 以避免依赖实现类；最小回归通过）；落地提交：`314c5d6b`。
- ✅ **评教（事务事件监听器归位：EvaluationTaskPostedMessageSenderListener，保持行为不变）**：将 `EvaluationTaskPostedMessageSenderListener` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.event.listener` 不变；监听器触发点/调用顺序不变；仅将注入类型收窄为 `IMsgService` 以避免依赖实现类；最小回归通过）；落地提交：`c9fbe6ef`。
- ✅ **评教（旧入口壳归位：EvaStatisticsServiceImpl，保持行为不变）**：将 `EvaStatisticsServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；`@CheckSemId` 触发点与委托顺序不变；类内容不变；最小回归通过）；落地提交：`6db29b33`。
- ✅ **评教（旧入口壳归位：EvaTemplateServiceImpl，保持行为不变）**：将 `EvaTemplateServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；异常转换与返回 `null` 语义不变；类内容不变；最小回归通过）；落地提交：`c63b9875`。
- ✅ **评教（旧入口壳归位：EvaRecordServiceImpl，保持行为不变）**：将 `EvaRecordServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；异常转换与返回 `null` 语义不变；`@Transactional` 与表单值映射顺序不变；最小回归通过）；落地提交：`c19b32fc`。
- ✅ **评教（旧入口壳归位：EvaTaskServiceImpl，保持行为不变）**：将 `EvaTaskServiceImpl` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.service.impl.eva` 不变；`@Transactional`/异常文案/日志输出与副作用顺序不变；消息依赖类型收窄为 `IMsgService` 以避免引用实现类；最小回归通过）；落地提交：`1aaff86f`。
- ✅ 最小回归通过（Java17）：命令见 0.10。

**2026-01-13（本次会话：S0.2 延伸（IAM 旧入口归位补齐：UserAuthServiceImpl）+ 交接口径加固，保持行为不变）**
- ✅ **IAM（旧入口归位：登录，保持行为不变）**：将 `UserAuthServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl.user` 不变；类内容不变；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-iam` 的编译期耦合面；最小回归通过）；落地提交：`b2d885a7`。
- ✅ **IAM（旧入口归位：支撑类前置，头像配置属性，保持行为不变）**：将 `AvatarProperties` 从 `eva-infra` 搬运归位到 `eva-infra-shared`（保持 `package edu.cuit.infra.property` 不变；类内容不变；用于后续归位 `AvatarManager`/`UserServiceImpl` 做编译闭合前置；最小回归通过）；落地提交：`bb97f037`。
- ✅ **IAM（旧入口归位：支撑类前置，头像管理，保持行为不变）**：将 `AvatarManager` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app` 不变；类内容不变；用于后续归位 `UserServiceImpl` 做编译闭合前置；最小回归通过）；落地提交：`50ab6c9e`。
- ✅ **IAM（旧入口归位：支撑类前置，用户 Convertor，保持行为不变）**：将 `UserBizConvertor` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.convertor.user` 不变；类内容不变；用于后续归位 `UserServiceImpl` 做编译闭合前置；最小回归通过）；落地提交：`10eeb4f3`。
- ✅ **IAM（旧入口归位：支撑类前置，路由工厂，保持行为不变）**：将 `RouterDetailFactory` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.factory.user` 不变；类内容不变；用于后续归位 `UserServiceImpl` 做编译闭合前置；最小回归通过）；落地提交：`be21d2a6`。
- ✅ **IAM（旧入口归位：编译闭合前置，评教端口依赖，保持行为不变）**：为后续归位 `UserServiceImpl` 做前置，在 `bc-iam/infrastructure/pom.xml` 补齐对 `bc-evaluation` 的编译期依赖（原因：`UserServiceImpl` 依赖 `EvaRecordCountQueryPort`；最小回归通过）；落地提交：`8f5fc4ca`。
- ✅ **IAM（旧入口归位：用户管理，保持行为不变）**：将 `UserServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl.user` 不变；类内容不变；事务/异常文案/副作用顺序完全不变；最小回归通过）；落地提交：`c4552031`。
- ✅ **IAM（组合根归位：BcIamConfiguration，保持行为不变）**：将 `BcIamConfiguration` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.config` 不变；Bean 装配/副作用顺序不变；最小回归通过）；落地提交：`14cd4108`。
- ✅ **IAM（依赖收敛：eva-app 去 bc-iam 编译期依赖，保持行为不变）**：在 Serena + `rg` 证伪 `eva-app/src/main/java` 不再直接引用 `edu.cuit.bc.iam.*` 后，收敛 `eva-app/pom.xml`：移除对 `bc-iam` 的 Maven 编译期依赖（保留 `bc-iam-infra` 以闭合运行期装配；最小回归通过）；证据口径：`rg -n '^import\\s+edu\\.cuit\\.bc\\.iam' eva-app/src/main/java` → 0；落地提交：`290f2b82`。
- ✅ **IAM（装配责任上推：start 显式依赖 bc-iam-infra，保持行为不变）**：为后续收敛 `eva-app` 的运行期装配依赖边界，在 `start/pom.xml` 增加对 `bc-iam-infra` 的 `runtime` 依赖（与原 transitive 结果等价，仅显式化；最小回归通过）；落地提交：`8a5df2d0`。
- ✅ **IAM（依赖收敛：eva-app 去 bc-iam-infra 编译期依赖，保持行为不变）**：在 Serena 证据化确认 `eva-app/src/main/java` 不再引用 `edu.cuit.app.*` 的 IAM 实现类后，收敛 `eva-app/pom.xml`：移除对 `bc-iam-infra` 的依赖；运行期装配由组合根 `start` 显式兜底（最小回归通过）；落地提交：`e7b46f36`。
- ✅ **评教（组合根归位：BcEvaluationConfiguration，保持行为不变）**：将 `BcEvaluationConfiguration` 从 `eva-app` 搬运归位到 `bc-evaluation-infra`（保持 `package edu.cuit.app.config` 不变；Bean 装配/副作用顺序不变；用于后续收敛 `eva-app` 对 `bc-evaluation-infra` 的依赖边界；最小回归通过）；落地提交：`c3f7fc56`。
- ✅ **评教（装配责任上推：start 显式依赖 bc-evaluation-infra，保持行为不变）**：为后续收敛 `eva-app` 的运行期装配依赖边界，在 `start/pom.xml` 增加对 `bc-evaluation-infra` 的 `runtime` 依赖（与原 transitive 结果等价，仅显式化；最小回归通过）；落地提交：`0f20d0cd`。
- ✅ **评教（依赖收敛：eva-app 去 bc-evaluation-infra 依赖，保持行为不变）**：在 Serena + `rg` 证伪 `eva-app/src/main/java` 不再直接引用 `edu.cuit.infra.bcevaluation.*` 实现类型后，收敛 `eva-app/pom.xml`：移除对 `bc-evaluation-infra` 的依赖；运行期装配由组合根 `start` 显式兜底（最小回归通过）；证据口径：`rg -n '^import\\s+edu\\.cuit\\.infra\\.bcevaluation' eva-app/src/main/java` → 0；落地提交：`e9feeb56`。
- ✅ 最小回归通过（Java17）：命令见 0.10。
- ⚠️ **MCP Serena 状态（符号级定位/引用分析）**：本会话推进到“搬运 `UserAuthServiceImpl`”时，Serena 出现持续 `TimeoutError`（`mcp__serena__initial_instructions` / `mcp__serena__check_onboarding_performed` 等均超时），因此该步按铁律**临时降级**为本地 `rg` 证据化校验；下一会话优先排查恢复 Serena 后再回到“符号级引用分析”的标准口径。可复现证据（均在本仓库 `ddd` 分支执行）：
  - `rg -n --column "class\\s+UserAuthServiceImpl\\b" .` → `bc-iam/infrastructure/src/main/java/edu/cuit/app/service/impl/user/UserAuthServiceImpl.java:16`
  - `rg -n --column "\\bUserAuthServiceImpl\\b" eva-app/src/main/java` → 0（旧模块不再包含该实现类）
- 🧾 文档同步：已将上述变更与“Serena 超时降级口径”同步到 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md`（以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准，不在文内固化 commitId）。
- 下一步（保持行为不变；每次只改 1 个类闭环）：继续按顺序推进 IAM 旧入口归位：`UserServiceImpl` → `BcIamConfiguration`（详见 0.10）。

**2026-01-13（补充：课程域去编译期耦合推进——学期/教室/课程类型旧入口归位 + eva-app 去 bc-course 编译期依赖，保持行为不变）**
- ✅ **课程（依赖收敛前置：旧入口归位，学期，保持行为不变）**：将 `SemesterServiceImpl` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl` 不变；仍实现 `ISemesterService` 并委托 `SemesterQueryUseCase`；保留 `@Transactional`；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-course` 的编译期耦合面；最小回归通过）；落地提交：`8eddc643`。
- ✅ **课程（依赖收敛前置：旧入口归位，教室，保持行为不变）**：将 `ClassroomServiceImpl` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl` 不变；仍实现 `IClassroomService` 并委托 `ClassroomQueryUseCase`；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-course` 的编译期耦合面；缓存/日志/异常文案/副作用顺序完全不变；最小回归通过）；落地提交：`4bc8cfb1`。
- ✅ **课程（依赖收敛前置：旧入口归位，课程类型，保持行为不变）**：将 `ICourseTypeServiceImpl` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变；仍实现 `ICourseTypeService` 并委托 `CourseTypeUseCase`；`id==null` 语义与 `updateCoursesType` 返回 `null`（`Void`）语义保持不变；缓存/日志/异常文案/副作用顺序完全不变；最小回归通过）；落地提交：`8a4a6774`。
- ✅ **课程（依赖收敛：eva-app 去 bc-course 编译期依赖，保持行为不变）**：在 Serena + `rg` 证伪 `eva-app` 不再 `import edu.cuit.bc.course.*`（口径：`rg -n '^import edu\\.cuit\\.bc\\.course' eva-app/src/main/java` 证伪为 0）后，收敛 `eva-app/pom.xml`：移除对 `bc-course` 的编译期依赖（`shared-kernel` 显式依赖保留；最小回归通过）；落地提交：`dca806fa`。
- ✅ **模板（依赖收敛前置：组合根归位，保持行为不变）**：将 `BcTemplateConfiguration` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.config` 不变；`CourseTemplateLockService` Bean 定义与注入不变；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-template` 的编译期耦合面；最小回归通过）；落地提交：`0fbd5d63`。
- ✅ **模板（依赖收敛：eva-app 去 bc-template 编译期依赖，保持行为不变）**：在 Serena + `rg` 证伪 `eva-app` 不再引用 `edu.cuit.bc.template.*` 后，收敛 `eva-app/pom.xml`：移除对 `bc-template` 的编译期依赖（运行时仍由 `bc-course-infra` 等模块提供；最小回归通过）；落地提交：`c53fd53d`。
- ✅ **模板（依赖收敛：端口下沉以降低依赖层级，保持行为不变）**：将 `CourseTemplateLockQueryPort` 从 `bc-template/application` 下沉到 `bc-template-domain`（保持 `package edu.cuit.bc.template.application.port` 不变；调用/行为不变），用于后续让基础设施实现侧（如 `eva-infra`）不必编译期依赖 `bc-template` 应用层；最小回归通过；落地提交：`d0fa4878`。
- ✅ **模板（依赖收敛：eva-infra 去 bc-template 编译期依赖，保持行为不变）**：在 Serena + `rg` 证伪 `eva-infra` 仅引用模板锁定查询端口接口 `CourseTemplateLockQueryPort`（无实现/副作用耦合）后，收敛 `eva-infra/pom.xml`：将对 `bc-template` 的 Maven 依赖替换为 `bc-template-domain`（版本不变；最小回归通过）；落地提交：`5910762e`。
- ✅ **模板（依赖收敛：bc-template-infra 去 bc-template 编译期依赖，保持行为不变）**：在 Serena 证据化确认 `bc-template-infra` 当前仅包含 `pom.xml`（暂无源码/副作用）后，收敛 `bc-template/infrastructure/pom.xml`：将对 `bc-template` 的 Maven 依赖替换为 `bc-template-domain`（版本不变；最小回归通过）；落地提交：`aee98f9b`。
- ✅ **课程（依赖收敛前置：模板锁定服务下沉，保持行为不变）**：为后续让 `bc-course/application` 可收敛对 `bc-template` 应用层 jar 的编译期依赖，将 `CourseTemplateLockService` 从 `bc-template/application` 下沉到 `bc-template-domain`（保持 `package edu.cuit.bc.template.application` 与代码不变；调用/行为不变；最小回归通过）；落地提交：`8a1319df`。
- ✅ **课程（依赖收敛：bc-course/application 去 bc-template 编译期依赖，保持行为不变）**：在 Serena + `rg` 证伪 `bc-course/application` 仅引用 `CourseTemplateLockService/CourseTemplateLockQueryPort/TemplateLockedException`（均已归属 `bc-template-domain`）后，收敛 `bc-course/application/pom.xml`：将对 `bc-template` 的 Maven 依赖替换为 `bc-template-domain`（版本不变；最小回归通过）；落地提交：`2de83046`。
- ✅ **S0.2 延伸（依赖方收敛：eva-infra 去 bc-evaluation/bc-iam/bc-audit 编译期依赖，保持行为不变）**：在 Serena + `rg` 证伪 `eva-infra/src/main/java` 未引用 `edu.cuit.bc.evaluation|edu.cuit.bc.iam|edu.cuit.bc.audit` 相关类型后，收敛 `eva-infra/pom.xml`：移除对 `bc-evaluation` / `bc-iam` / `bc-audit` 的 Maven 编译期依赖（最小回归通过）；落地提交：`023d63be`。
- ✅ **S0.2 延伸（依赖归位：bc-ai-report-infra 显式依赖 bc-evaluation，保持行为不变）**：Serena 证据化确认 `bc-ai-report/infrastructure` 的 `AiReportAnalysisPortImpl` 引用评教 application port `EvaRecordExportQueryPort` 后，为避免经由 `bc-ai-report(application)` 传递依赖，已在 `bc-ai-report/infrastructure/pom.xml` 补齐对 `bc-evaluation` 的编译期依赖（最小回归通过）；落地提交：`c0f78068`。
- ✅ **S0.2 延伸（依赖收敛：bc-ai-report(application) 去 bc-evaluation 编译期依赖，保持行为不变）**：在 Serena + `rg` 证伪 `bc-ai-report/application/src/main/java` 未引用 `edu.cuit.bc.evaluation.*` 相关类型后，收敛 `bc-ai-report/application/pom.xml`：移除对 `bc-evaluation` 的 Maven 编译期依赖，并由 `bc-ai-report-infra` 显式承接该依赖（最小回归通过）；落地提交：`87179f19`。
- ✅ **S0.2 延伸（IAM：旧入口归位，部门查询，保持行为不变）**：将 `DepartmentServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl` 不变；仍实现 `IDepartmentService` 并委托 `DepartmentQueryUseCase`；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-iam` 的编译期耦合面；最小回归通过）；落地提交：`68dea36a`。
- ✅ **S0.2 延伸（IAM：支撑类归位，菜单 Convertor，保持行为不变）**：为后续将 `MenuServiceImpl` 从 `eva-app` 归位到 `bc-iam-infra` 做前置，先将 `MenuBizConvertor` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.convertor.user` 不变；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-iam` 的编译期耦合面；最小回归通过）；落地提交：`6298e5a7`。
- ✅ **S0.2 延伸（IAM：旧入口归位，菜单，保持行为不变）**：将 `MenuServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl.user` 不变；仍实现 `IMenuService` 并保持 `@Transactional` 事务边界不变；内部仍按“查询网关 → convertor 组装 / 委托 UseCase”的既有顺序执行；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-iam` 的编译期耦合面；最小回归通过）；落地提交：`6aef1d96`。
- ✅ **S0.2 延伸（IAM：支撑类归位，角色 Convertor，保持行为不变）**：为后续将 `RoleServiceImpl/UserServiceImpl` 从 `eva-app` 归位到 `bc-iam-infra` 做前置，先将 `RoleBizConvertor` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.convertor.user` 不变；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-iam` 的编译期耦合面；最小回归通过）；落地提交：`cf0773ac`。
- ✅ **S0.2 延伸（IAM：旧入口归位，角色，保持行为不变）**：将 `RoleServiceImpl` 从 `eva-app` 搬运归位到 `bc-iam-infra`（保持 `package edu.cuit.app.service.impl.user` 不变；仍实现 `IRoleService` 并保持 `@Transactional` 事务边界不变；内部仍按“查询网关 → convertor/pagination 组装 / 委托 UseCase”的既有顺序执行；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-iam` 的编译期耦合面；最小回归通过）；落地提交：`3011ab83`。
- ✅ **S0.2 延伸（IAM：编译闭合前置，安全依赖，保持行为不变）**：为后续将 `UserAuthServiceImpl` 从 `eva-app` 归位到 `bc-iam-infra` 做前置，在 `bc-iam/infrastructure/pom.xml` 补齐 `zym-spring-boot-starter-security`（与 `eva-app` 同源；避免新增运行时差异；最小回归通过）；落地提交：`bded148a`。
- 🧾 文档同步：已将上述变更同步到 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md`（以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准，不在文内固化 commitId）。

**2026-01-13（本次会话：Controller 收敛推进（课程 + 评教 + 消息 + 日志） + S0.2 延伸（依赖方 pom 收敛），保持行为不变）**
- 📌 本会话增量：按“每次只改 1 个 Controller”的模板持续收敛 `eva-adapter` 课程/评教相关 `*Controller`，Controller 仅做协议适配与参数校验；不引入新副作用；并保持 **缓存/日志/异常文案/副作用顺序完全不变**。
- 🧾 本会话新增闭环（代码，按时间顺序；保持行为不变）：`0a691bb1`（QueryCourse）→ `d49976cf`（QueryUserCourse）→ `58b1b763`（UpdateCourse）→ `20e1214c`（DeleteCourse）→ `7c465c5d`（EvaStatistics）→ `74a34133`（EvaQuery）→ `87fc0475`（UpdateEva）→ `7809cc2d`（DeleteEva）→ `d055c271`（EvaConfigQuery）→ `c7ab9633`（EvaConfigUpdate）→ `aa99c775`（Message）→ `e19278a6`（Log）。
- 🧾 本会话提交链（按发生顺序，便于回溯/回滚；文档同步提交不在文内固化 commitId）：`7809cc2d` →（docs：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准）→ `d055c271` →（docs：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准）→ `c7ab9633` →（docs：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准）→ `aa99c775` →（docs：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准）→ `e19278a6` →（docs：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准）。
- 🧾 文档同步：已将上述变更同步到 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md`，以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在文内固化 commitId）。
- ✅ **S0.2 延伸（依赖方 pom 收敛：组合根显式承接 eva-app 装配责任，保持行为不变）**：Serena 证实：`start` 当前显式依赖 `eva-adapter`，`eva-app` 仅经由 `eva-adapter/pom.xml` 传递进入 classpath；因此在 `start/pom.xml` 增加对 `eva-app` 的 `runtime` 依赖，作为后续收敛 `eva-adapter` 编译期耦合的前置（不改业务语义/装配顺序；最小回归通过）；落地提交：`0a69dfb6`。（后续已在 `0a9ff564` 将组合根依赖从 `eva-app` 替换为 `eva-infra(runtime)`，并移除 `start` 对 `eva-app` 的依赖；并已在 `1e2ffa89` 移除组合根对 `eva-infra(runtime)` 的兜底依赖。）
- ✅ **S0.2 延伸（依赖方 pom 收敛：eva-adapter 去 eva-app 编译期依赖，保持行为不变）**：Serena + `rg` 证伪 `eva-adapter` 不再引用 `edu.cuit.app.*` 的实现类型后，移除 `eva-adapter/pom.xml` 对 `eva-app` 的 Maven 依赖（减少编译期耦合；不改业务语义/装配顺序；最小回归通过）；落地提交：`f5980fcc`。
- ✅ **S0.2 延伸（依赖方 pom 收敛：组合根上推课程域基础设施运行时依赖，保持行为不变）**：为进一步让组合根显式承接运行时装配责任，将 `start/pom.xml` 中 `bc-course-infra` 的依赖范围从 `test` 调整为 `runtime`（此前其运行时 classpath 已经由 `eva-app` 的 `runtime` 依赖闭合，因此该步仅做“显式上推”，不改业务语义/装配；最小回归通过）；落地提交：`2a442587`。
- ✅ **S0.2 延伸（依赖方 pom 收敛：eva-app 去 bc-course-infra 运行时兜底依赖，保持行为不变）**：在组合根 `start` 已显式兜底 `bc-course-infra` 运行时依赖的前提下，移除 `eva-app/pom.xml` 中对 `bc-course-infra` 的 `runtime` 依赖，进一步收敛“运行时装配责任”到组合根（不改业务语义/装配顺序；最小回归通过）；落地提交：`9e7bd82d`。
- ✅ **消息（依赖收敛前置：旧入口改为委托 MsgGateway，保持行为不变）**：将 `MsgServiceImpl` 内部对 `QueryMessageUseCase/MarkMessageReadUseCase/UpdateMessageDisplayUseCase/DeleteMessageUseCase/InsertMessageUseCase` 的直接依赖改回为委托 `MsgGateway`（其实现 `MsgGatewayImpl` 仍由 `bc-messaging` 提供并内部委托 `MessageUseCaseFacade`，因此行为不变）；该步用于移除 `eva-app` 对 `bc-messaging` 的编译期依赖阻塞（最小回归通过）；落地提交：`35b8eb90`。
- ✅ **消息（依赖收敛：eva-app 去 bc-messaging 编译期依赖，保持行为不变）**：在 `MsgServiceImpl` 已不再依赖消息域用例实现类型的前提下，移除 `eva-app/pom.xml` 中对 `bc-messaging` 的 Maven 依赖，仅保留 `bc-messaging-contract` 承载协议对象（组合根 `start` 已显式承接 `bc-messaging` 的运行时装配责任；保持行为不变；最小回归通过）；落地提交：`afbe2e6c`。
- ✅ **课程（依赖收敛前置：旧入口归位，学期，保持行为不变）**：将 `SemesterServiceImpl` 从 `eva-app` 搬运归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl` 不变；仍实现 `ISemesterService` 并委托 `SemesterQueryUseCase`；保留 `@Transactional`；仅改变类所在 Maven 模块以减少 `eva-app` → `bc-course` 的编译期耦合面；最小回归通过）；落地提交：`8eddc643`。
- ✅ **S0.2 延伸（课程：依赖收敛，eva-app 去 bc-course 编译期依赖，保持行为不变）**：在 Serena + `rg` 证伪 `eva-app` 不再 `import edu.cuit.bc.course.*`（口径：`rg -n '^import edu\\.cuit\\.bc\\.course' eva-app/src/main/java` 证伪为 0）后，已收敛 `eva-app/pom.xml`：移除对 `bc-course` 的编译期依赖（`shared-kernel` 显式依赖保留；最小回归通过）；落地提交：`dca806fa`。
- ✅ **课程（课程查询，Controller：固化返回包装顺序表达，保持行为不变）**：在 `QueryCourseController` 将多个接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与登录态/权限注解保持不变）；最小回归通过；落地提交：`0a691bb1`。
- ✅ **课程（用户-课程查询，Controller：固化返回包装顺序表达，保持行为不变）**：在 `QueryUserCourseController` 将多个接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与登录态/权限注解保持不变）；最小回归通过；落地提交：`d49976cf`。
- ✅ **课程（课程写入口，Controller：固化返回/日志表达，保持行为不变）**：在 `UpdateCourseController` 收敛返回表达式与日志内容构造写法（日志内容仍在 `CommonResult.success(..., supplier)` 内部构造；不改异常文案/返回体结构/副作用顺序；参数校验与权限注解保持不变）；最小回归通过；落地提交：`58b1b763`。
- ✅ **课程（课程删除入口，Controller：收敛表达与无用 import，保持行为不变）**：在 `DeleteCourseController` 清理无用 import，并收敛参数/调用的格式表达（不改异常文案/返回体结构/副作用顺序；参数校验与权限注解保持不变）；最小回归通过；落地提交：`20e1214c`。
- ✅ **评教（统计查询入口，Controller：固化返回包装顺序表达，保持行为不变）**：在 `EvaStatisticsController` 将多个接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与登录态/权限注解保持不变）；最小回归通过；落地提交：`7c465c5d`。
- ✅ **评教（查询入口，Controller：固化返回包装顺序表达，保持行为不变）**：在 `EvaQueryController` 将多个接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与登录态/权限注解保持不变）；最小回归通过；落地提交：`74a34133`。
- ✅ **评教（写入口，Controller：收敛日志表达与无用 import，保持行为不变）**：在 `UpdateEvaController` 清理无用 import，并收敛日志内容构造写法（仍保持 `service` 调用 → `LogUtils.logContent(...)` → `CommonResult.success(null)` 的顺序不变；不改异常文案/返回体结构/副作用顺序；参数校验与登录态/权限注解保持不变）；最小回归通过；落地提交：`87fc0475`。
- ✅ **评教（删除入口，Controller：收敛依赖与返回表达，保持行为不变）**：在 `DeleteEvaController` 清理未使用注入依赖与无用 import，并抽取 `success()` 收敛 `CommonResult.success(null)` 的重复表达（仍保持 `service` 调用 → `CommonResult.success(null)` 的执行顺序不变；不改异常文案/返回体结构/副作用顺序；参数校验与权限注解保持不变）；最小回归通过；落地提交：`7809cc2d`。
- ✅ **评教（配置查询入口，Controller：固化返回包装顺序表达，保持行为不变）**：在 `EvaConfigQueryController` 收敛返回表达式，用“局部变量 + return”显式固化“先调用 service → 再 `CommonResult.success(...)`”的执行顺序（不改异常文案/返回体结构/副作用顺序；权限注解保持不变）；最小回归通过；落地提交：`d055c271`。
- ✅ **评教（配置修改入口，Controller：固化返回包装顺序表达，保持行为不变）**：在 `EvaConfigUpdateController` 收敛返回表达式，抽取 `success()` 以显式固化“先调用 service → 再 `CommonResult.success()`”的执行顺序（不改异常文案/返回体结构/副作用顺序；参数校验与权限注解保持不变）；最小回归通过；落地提交：`c7ab9633`。
- ✅ **消息（入口，Controller：固化返回包装顺序表达，保持行为不变）**：在 `MessageController` 将查询接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与登录态/权限注解保持不变）；最小回归通过；落地提交：`aa99c775`。
- ✅ **日志（入口，Controller：固化返回包装顺序表达，保持行为不变）**：在 `LogController` 将查询接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与权限注解保持不变）；最小回归通过；落地提交：`e19278a6`。
- 📌 下一刀建议（保持行为不变）：转入 **S0.2 延伸（依赖方 `pom.xml` 依赖收敛）**：优先挑选“仅使用 `edu.cuit.client.*` 类型”的依赖方模块，将对业务实现模块的编译期依赖替换为 `shared-kernel`（每步只改 1 个 `pom.xml`，并保持行为不变）。

**2026-01-12（本次会话：Controller 收敛起步）**
- 📌 本次会话主线：持续收敛 `eva-adapter` 残留 `*Controller`（每次只改 1 个 Controller；保持行为不变）；本次共推进 **10 个** Controller（含查询/写入口与学期/院系/教室）。
- ✅ **IAM（用户，Controller：减少适配层编排噪声，保持行为不变）**：在 `UserUpdateController` 提取 `currentUserId()`（仍保持 `StpUtil.getLoginId()` → `userService.getIdByUsername(...)` 的调用顺序与次数不变；异常文案与日志不变）；最小回归通过；落地提交：`09cb6454`。
- ✅ **IAM（认证，Controller：固化 login 返回顺序表达，保持行为不变）**：在 `AuthenticationController` 将 `login` 的内联调用收敛为“先 `userAuthService.login(loginCmd)` → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与登录态注解保持不变）；最小回归通过；落地提交：`102a0cbb`。
- ✅ **IAM（用户，Controller：固化返回包装顺序表达，保持行为不变）**：在 `UserQueryController` 将多个接口的返回表达式统一收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与权限注解保持不变）；最小回归通过；落地提交：`18e0bb29`。
- ✅ **IAM（角色，Controller：固化返回包装顺序表达，保持行为不变）**：在 `RoleQueryController` 将多个接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与权限注解保持不变）；最小回归通过；落地提交：`fa04e48e`。
- ✅ **IAM（菜单，Controller：固化返回包装顺序表达，保持行为不变）**：在 `MenuQueryController` 将多个接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与权限注解保持不变）；最小回归通过；落地提交：`8521f72c`。
- ✅ **IAM（菜单，Controller：固化日志内容构造表达，保持行为不变）**：在 `MenuUpdateController.create` 将 `LogUtils.logContent(newMenuCmd.getName() + " 权限")` 收敛为局部变量写法（保持 `menuService.create(...)` → `LogUtils.logContent(...)` → `CommonResult.success()` 顺序不变；不改异常文案/返回体结构/副作用顺序；参数校验与权限注解保持不变）；最小回归通过；落地提交：`9a2fc3ff`。
- ✅ **IAM（角色，Controller：固化日志内容构造表达，保持行为不变）**：在 `RoleUpdateController.create` 将 `LogUtils.logContent(newRoleCmd.getRoleName() + "角色")` 收敛为局部变量写法（保持 `roleService.create(...)` → `LogUtils.logContent(...)` → `CommonResult.success()` 顺序不变；不改异常文案/返回体结构/副作用顺序；参数校验与权限注解保持不变）；最小回归通过；落地提交：`9f07698a`。
- ✅ **课程（学期，Controller：固化返回包装顺序表达，保持行为不变）**：在 `SemesterController` 将多个接口的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与登录态/权限注解保持不变）；最小回归通过；落地提交：`24e5ad3f`。
- ✅ **IAM（院系，Controller：固化返回包装顺序表达，保持行为不变）**：在 `DepartmentController` 将 `all` 的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；参数校验与登录态/权限注解保持不变）；最小回归通过；落地提交：`e57c0e41`。
- ✅ **课程（教室，Controller：固化返回包装顺序表达，保持行为不变）**：在 `ClassroomController` 将 `getAll` 的返回表达式收敛为“先调用 service → 再 `CommonResult.success(...)`”的显式两步写法（不改异常文案/返回体结构/副作用顺序；登录态注解保持不变）；最小回归通过；落地提交：`f1606018`。
- ✅ **docs（主线口径更新：Controller 优先，保持行为不变）**：补齐 “Controller 收敛的推进模板/推荐顺序/下一刀（`AuthenticationController`）” 的交接说明，并在 0.11 新增「E 专用简版（Controller 优先）」提示词（与 `DDD_REFACTOR_PLAN.md`、`docs/DDD_REFACTOR_BACKLOG.md` 同步；不在文内固化 commitId）。
- 🔁 **交接口径复核（2026-01-17，保持行为不变）**：本次会话补齐“可复现证据口径”并修正 0.10.1 的推荐顺序标记口径（✅ 仅表示“已归位迁出 eva-adapter”，避免与“仅做表达式收敛”混淆）。当前 `eva-adapter` 残留 Controller 口径仍为 **18**（见 0.10.1 的清单与可复现命令），下一会话建议优先归位 IAM 侧 `MenuQueryController` → `MenuUpdateController` → `RoleUpdateController`（每次只迁 1 个类；保持行为不变）。
- ✅ **S1（入口归位：MenuQueryController，保持行为不变）**：将 `MenuQueryController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`76e19a47`。当前 `eva-adapter` 残留 Controller 口径更新为 **17**（见 0.10.2）。
- ✅ **S1（入口归位：MenuUpdateController，保持行为不变）**：将 `MenuUpdateController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`d29b565b`。当前 `eva-adapter` 残留 Controller 口径更新为 **16**（见 0.10.2）。
- ✅ **S1（入口归位：RoleUpdateController，保持行为不变）**：将 `RoleUpdateController` 从 `eva-adapter` 归位到 `bc-iam-infra`（保持 `package`/接口签名/URL/注解与行为不变，仅搬运归位；最小回归通过）；落地提交：`80888bed`。当前 `eva-adapter` 残留 Controller 口径更新为 **15**（见 0.10.2）。

**2026-01-12（本次会话：ICourseTypeServiceImpl 收敛准备）**
- ✅ **课程（课程类型，用例骨架，保持行为不变）**：在 `bc-course` 新增课程类型用例 `CourseTypeUseCase`（读写合并；手写 `CourseTypeEntity` → `CourseType` 映射与 `PaginationQueryResultCO` 组装，不引入 `eva-infra-shared`；对齐旧入口逻辑与返回语义；最小回归通过）；落地提交：`325f221a`。
- ✅ **课程（课程类型，用例装配，保持行为不变）**：在 `BcCourseConfiguration` 补齐 `CourseTypeUseCase` 的 Bean 装配（保持行为不变；最小回归通过）；落地提交：`55eb322e`。
- ✅ **课程（课程类型，入口壳收敛：委托用例，保持行为不变）**：将 `eva-app` 的 `ICourseTypeServiceImpl` 退化为纯委托壳，改为委托 `CourseTypeUseCase`（保持 `id==null` → 传 `null` ids；`updateCoursesType` 仍返回 `null`（`Void`）；最小回归通过）；落地提交：`1aebda24`。

**2026-01-12（本次会话：ClassroomServiceImpl 收敛完成）**
- ✅ **课程（教室，用例骨架，保持行为不变）**：在 `bc-course` 新增教室查询用例 `ClassroomQueryUseCase`（当前仅委托 `ClassroomGateway.getAll()`，不改业务语义；最小回归通过）；落地提交：`09822993`。
- ✅ **课程（教室，用例装配，保持行为不变）**：在 `BcCourseConfiguration` 补齐 `ClassroomQueryUseCase` 的 Bean 装配（不改业务语义；最小回归通过）；落地提交：`abdfe122`。
- ✅ **课程（教室，入口壳收敛：委托用例，保持行为不变）**：将 `eva-app` 的 `ClassroomServiceImpl` 从直接调用 `ClassroomGateway` 改为委托 `ClassroomQueryUseCase`（异常文案与副作用顺序不变；最小回归通过）；落地提交：`20361679`。

**2026-01-12（本次会话：DepartmentServiceImpl 收敛准备）**
- ✅ **IAM（院系，用例骨架，保持行为不变）**：在 `bc-iam` 新增院系查询用例 `DepartmentQueryUseCase`（当前仅委托 `DepartmentGateway.getAll()`，不改业务语义；最小回归通过）；落地提交：`78fd4b0e`。
- ✅ **IAM（院系，用例装配，保持行为不变）**：在 `BcIamConfiguration` 补齐 `DepartmentQueryUseCase` 的 Bean 装配（不改业务语义；最小回归通过）；落地提交：`1cc7cc8a`。
- ✅ **IAM（院系，入口壳收敛：委托用例，保持行为不变）**：将 `eva-app` 的 `DepartmentServiceImpl` 从直接调用 `DepartmentGateway` 改为委托 `DepartmentQueryUseCase`（异常文案与副作用顺序不变；最小回归通过）；落地提交：`d9e4b7d6`。

**2026-01-12（本次会话：SemesterServiceImpl 收敛准备）**
- ✅ **课程（学期，编译依赖准备，保持行为不变）**：在 `bc-course/application/pom.xml` 补齐对 `eva-domain` 的编译期依赖，以便后续在 `bc-course` 应用层新增学期查询用例并复用 `SemesterGateway`（不改业务语义；最小回归通过）；落地提交：`d5ea0d96`。
- ✅ **课程（学期，用例骨架，保持行为不变）**：在 `bc-course/application` 新增学期查询用例 `SemesterQueryUseCase`（当前仅委托 `SemesterGateway`，不改业务语义；最小回归通过）；落地提交：`7d8323b5`。
- ✅ **课程（学期，用例装配，保持行为不变）**：在 `bc-course` 组合根 `BcCourseConfiguration` 补齐 `SemesterQueryUseCase` 的 Bean 装配（不改业务语义；最小回归通过）；落地提交：`a93e2bda`。
- ✅ **课程（学期，编译依赖准备，保持行为不变）**：为后续让 `eva-app` 的 `SemesterServiceImpl` 能直接委托学期查询用例，在 `eva-app/pom.xml` 补齐对 `bc-course` 的编译期依赖（不改业务语义；最小回归通过）；落地提交：`9f61788b`。
- ✅ **课程（学期，入口壳收敛：委托用例，保持行为不变）**：将 `eva-app` 的 `SemesterServiceImpl` 从直接调用 `SemesterGateway` 改为委托 `bc-course` 的 `SemesterQueryUseCase`（保留 `@Transactional`；异常文案与副作用顺序不变；最小回归通过）；落地提交：`292eb1f2`。

**2026-01-12（本次会话：MsgServiceImpl 收敛 + 文档同步）**
- ✅ **消息（编译依赖准备，保持行为不变）**：为让 `eva-app` 的 `MsgServiceImpl` 能直接委托 `bc-messaging` 的 UseCase（而非继续只经由旧 `MsgGateway` 间接转发），先在 `eva-app/pom.xml` 补齐对 `bc-messaging` 的编译期依赖（不改任何业务语义；运行时 classpath 已包含该模块；最小回归通过）；落地提交：`02d338a9`。
- ✅ **消息（入口壳收敛：委托用例，保持行为不变）**：将 `eva-app` 的 `MsgServiceImpl` 中对消息查询/标记已读/展示状态/删除/落库的调用，改为直接委托 `bc-messaging` 的 `QueryMessageUseCase/MarkMessageReadUseCase/UpdateMessageDisplayUseCase/DeleteMessageUseCase/InsertMessageUseCase`（保持 `checkAndGetUserId()` 先执行；副作用顺序与异常文案完全不变）；最小回归通过；落地提交：`28ba21e4`。

**2026-01-12（本次会话：LogServiceImpl/UserServiceImpl/UserAuthServiceImpl 写侧收敛 + 文档同步）**
- ✅ **审计日志（入口壳收敛：日志写侧，保持行为不变）**：将 `eva-app` 的 `LogServiceImpl.registerListener` 中“插入日志”链路退化为委托 `bc-audit` 的 `InsertLogUseCase`（保持异步执行语义：仍使用 `CompletableFuture.runAsync(..., executor)`；异常文案/日志顺序与副作用顺序完全不变）；最小回归通过；落地提交：`cdb885b0`。
- ✅ **IAM（入口壳收敛：用户写侧，保持行为不变）**：将 `eva-app` 的 `UserServiceImpl` 写侧对 `UserUpdateGateway` 的调用改为直接委托 `bc-iam` 的 `UpdateUserInfoUseCase/UpdateUserStatusUseCase/AssignRoleUseCase/CreateUserUseCase/DeleteUserUseCase`（保留登录态解析/密码修改/登出/副作用顺序完全不变；读侧仍保留 `UserQueryGateway` 以保持缓存切面触发点不变）；最小回归通过；落地提交：`2b095a69`。
- ✅ **IAM（入口壳收敛：登录写侧，保持行为不变）**：将 `eva-app` 的 `UserAuthServiceImpl.login` 中“LDAP 鉴权 + 用户状态校验”链路下沉到 `bc-iam` 的用例 `ValidateUserLoginUseCase`，旧入口退化为“登录态解析（`StpUtil.isLogin`）+ 委托用例 + `StpUtil.login` + 返回 token”的壳（异常文案/分支顺序完全不变）；最小回归通过；落地提交：`d7c93768`。
- ✅ **最小回归（保持行为不变）**：`JAVA_HOME=/home/lystran/.sdkman/candidates/java/17.0.17-zulu PATH=$JAVA_HOME/bin:$PATH mvnd -pl start -am test -Dmaven.repo.local=.m2/repository`。
- ✅ **文档同步（保持行为不变）**：同步更新 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md`。

**2026-01-11（本次会话：RoleServiceImpl/MenuServiceImpl 写侧收敛 + 文档同步）**
- ✅ **基线确认（可复现）**：分支 `ddd`；以 `git rev-parse --short HEAD` 输出为“当前交接基线”；`git merge-base --is-ancestor 2e4c4923 HEAD` 退出码为 `0`；交接文档基线以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在文内固化 commitId）。
- ✅ **Serena 状态确认（已恢复）**：本会话可正常使用 Serena 做“符号级定位 + 引用分析”（`activate_project/check_onboarding_performed/find_symbol/find_referencing_symbols`），无需降级。
- ✅ **IAM（入口壳收敛：角色写侧，保持行为不变）**：将 `eva-app` 的 `RoleServiceImpl` 写侧方法（`updateInfo/updateStatus/assignPerm/create/delete/multipleDelete`）退化为“仅委托 UseCase”的壳：改为委托 `bc-iam` 的 `UpdateRoleInfoUseCase/UpdateRoleStatusUseCase/AssignRolePermsUseCase/CreateRoleUseCase/DeleteRoleUseCase/DeleteMultipleRoleUseCase`（事务边界仍由旧入口承接；异常文案/缓存失效/日志顺序与副作用完全不变）；最小回归通过；落地提交：`a71efb84`。
- ✅ **IAM（入口壳收敛：菜单写侧，保持行为不变）**：将 `eva-app` 的 `MenuServiceImpl` 写侧方法（`update/create/delete/multipleDelete`）退化为“仅委托 UseCase”的壳：改为委托 `bc-iam` 的 `UpdateMenuInfoUseCase/CreateMenuUseCase/DeleteMenuUseCase/DeleteMultipleMenuUseCase`（事务边界仍由旧入口承接；异常文案/缓存失效/日志顺序与副作用完全不变）；最小回归通过；落地提交：`905baf9f`。
- ✅ **最小回归（保持行为不变）**：`JAVA_HOME=/home/lystran/.sdkman/candidates/java/17.0.17-zulu PATH=$JAVA_HOME/bin:$PATH mvnd -pl start -am test -Dmaven.repo.local=.m2/repository`。
- ✅ **文档同步（保持行为不变）**：同步更新 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md`。

**2026-01-10（本次会话）**
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `SemesterGatewayImpl` 从 `eva-infra` 归位到 `bc-course-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；`getAll/getNow/getSemesterInfo/selectSemester` 的异常文案与分支语义完全不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`30e6a160`）。
- ✅ **基础设施（S1 退场候选：支撑类归位，保持行为不变）**：为后续归位 `SemesterGatewayImpl`（学期查询）到 `bc-course-infra` 做编译闭合前置，将 `SemesterConverter` 从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package edu.cuit.infra.convertor` 不变；仅 `git mv` 搬运；MapStruct `@Mapper` 定义不变；保持行为不变）；最小回归通过；落地提交：`6c9e1d39`。
- ✅ **基础设施（S1 退场候选：支撑类归位，保持行为不变）**：为后续归位评教配置旧 gateway（`EvaConfigGatewayImpl`）到 `bc-evaluation/infrastructure` 做编译闭合前置，将 `StaticConfigProperties` 从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package edu.cuit.infra.property` 不变；仅 `git mv` 搬运；保持行为不变）；最小回归通过；落地提交：`5a26af43`。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `EvaConfigGatewayImpl` 从 `eva-infra` 归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.eva` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`9a4e28aa`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `EvaDeleteGatewayImpl` 从 `eva-infra` 归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.eva` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`e8e73845`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `EvaUpdateGatewayImpl` 从 `eva-infra` 归位到 `bc-evaluation/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.eva` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`cbb3801a`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `ClassroomGatewayImpl` 从 `eva-infra` 归位到 `bc-course-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；缓存注解/缓存 key 表达式/查询字段/排序/去重逻辑完全不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`26b183d5`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `DepartmentGatewayImpl` 从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；缓存注解/缓存 key 表达式/异常语义完全不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`acb13124`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `MsgGatewayImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.gateway.impl` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`8ffcfe35`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `LogGatewayImpl` 从 `eva-infra` 归位到 `bc-audit-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`673a19e3`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `LdapPersonGatewayImpl` 从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package edu.cuit.infra.gateway.impl` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`1ff96d75`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `MenuQueryGatewayImpl` 从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package edu.cuit.infra.gateway.impl.user` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`a7cb96e9`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `MenuUpdateGatewayImpl` 从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package edu.cuit.infra.gateway.impl.user` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`09574045`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `RoleQueryGatewayImpl` 从 `eva-infra` 归位到 `bc-iam-infra`（保持 `package edu.cuit.infra.gateway.impl.user` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`457b6780`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `RoleUpdateGatewayImpl` 从 `eva-infra` 归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.user` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`1826ac99`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `UserQueryGatewayImpl` 从 `eva-infra` 归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.user` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`b9d8e6b8`）。
- ✅ **基础设施（S1 退场候选：旧 gateway 归位，保持行为不变）**：将 `UserUpdateGatewayImpl` 从 `eva-infra` 归位到 `bc-iam/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.user` 不变；仅 `git mv` 搬运与编译闭合；最小回归通过；落地提交：`69b72d86`）。

**2026-01-09（本次会话）**
- ✅ **S0.2 延伸（依赖方收敛：eva-app 去 bc-course 编译期依赖，保持行为不变）**：在 `ICourseServiceImpl/IUserCourseServiceImpl/ICourseDetailServiceImpl` 已全部归位到 `bc-course-infra` 且 Serena + `rg` 证伪 `eva-app` 不再引用 `edu.cuit.bc.course.*` 的前提下，将 `eva-app/pom.xml` 中对 `bc-course` 的编译期依赖替换为 `shared-kernel`（即移除 `bc-course` 依赖；`shared-kernel` 依赖已存在；每次只改 1 个 `pom.xml`；保持行为不变）；最小回归通过；落地提交：`6fe8ffc8`。
- ✅ **S0.2 延伸（课程旧入口归位：ICourseDetailServiceImpl，保持行为不变）**：将课程旧入口实现类 `ICourseDetailServiceImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变；仅 `git mv` 搬运与编译闭合；逻辑/异常文案/副作用顺序完全不变）；最小回归通过；落地提交：`bd85a006`。
- ✅ **S0.2 延伸（课程旧入口归位：IUserCourseServiceImpl，保持行为不变）**：将课程旧入口实现类 `IUserCourseServiceImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变；以 `git mv` 搬运为主并闭合编译期依赖；逻辑/异常文案/副作用顺序完全不变）。为避免 `bc-course-infra` 反向依赖 `eva-app`，将 `IUserCourseServiceImpl` 对 `UserCourseDetailQueryExec/FileImportExec` 的依赖收敛为类内私有方法（实现逻辑逐行对齐原有实现；保持行为不变）；最小回归通过；落地提交：`79a351c3`。
- ✅ **S0.2 延伸（课程 Controller 注入收敛到接口，保持行为不变）**：为继续收敛依赖方对 `bc-course` 的编译期耦合，先在 `eva-adapter` 的课程相关 Controller 子簇试点：将注入类型从 `edu.cuit.app.service.impl.course.*ServiceImpl` 收窄为 `shared-kernel` 下的 `edu.cuit.client.api.course.*Service` 接口（Serena 证伪：`ICourseService/ICourseDetailService/ICourseTypeService/IUserCourseService` 均只有一个 `@Service` 实现类）；Spring 注入目标不变，仅减少编译期绑定实现类的耦合；最小回归通过；落地提交：`47a6b06c`。
- ✅ **S0.2 延伸（课程旧入口归位：ICourseServiceImpl，保持行为不变）**：在 Controller 已不再注入实现类的前提下，先把课程旧入口实现类 `ICourseServiceImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变，仅 `git mv` 搬运与编译闭合；逻辑/异常文案/副作用顺序完全不变）。由于该旧入口依赖 `StpUtil`（Sa-Token 登录态读取），为保证 `bc-course-infra` 编译闭合补齐 `zym-spring-boot-starter-security` 依赖（运行时 classpath 已存在，仅模块内补齐编译期依赖；保持行为不变）；最小回归通过；落地提交：`2b5bcecb`。

**2026-01-08（本次会话）**
- ✅ **S0.2 延伸（分页转换器归位：PaginationBizConvertor，保持行为不变）**：将通用分页业务对象转换器 `PaginationBizConvertor` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor` 不变；逻辑不变；最小回归通过；落地提交：`c8c17225`），用于为后续归位课程旧入口/其它旧入口时闭合依赖，避免基础设施模块反向依赖 `eva-app`。
- ✅ **S0.2 延伸（事务提交后事件发布器归位：AfterCommitEventPublisher，保持行为不变）**：将通用“事务提交后发布事件”发布器 `AfterCommitEventPublisher` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package edu.cuit.app.event` 不变；逻辑不变；最小回归通过；落地提交：`fc85f548`），用于为后续归位课程旧入口/端口适配器时闭合依赖，避免 `bc-course-infra` 反向依赖 `eva-app`。
- ✅ **S0.2 延伸（课程读侧端口适配器归位：CourseDetailQueryPortImpl，保持行为不变）**：将 `CourseDetailQueryPortImpl` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.bccourse.adapter` 不变；实现逻辑/空列表兜底/副作用顺序完全不变；最小回归通过；落地提交：`250002d5`）。该步用于继续削减 `eva-app` 对课程 BC 的直接编译期引用面。
- ✅ **S0.2 延伸（课程域转换器归位：CourseBizConvertor，保持行为不变）**：为后续把课程域端口适配器/旧入口从 `eva-app` 继续归位到 `bc-course-infra` 做准备，将 `CourseBizConvertor` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package edu.cuit.app.convertor.course` 不变；仅搬运文件/编译闭合；最小回归通过；落地提交：`eec5d45c`）。
- ✅ **S0.2 延伸（课程域组合根归位：BcCourseConfiguration，保持行为不变）**：将课程域组合根 `BcCourseConfiguration` 从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.config` 不变；Bean 定义/装配顺序不变；最小回归通过；落地提交：`49477dd1`）。该步用于继续削减 `eva-app` 对课程 BC 的直接编译期引用面，为后续 `eva-app/pom.xml` 去 `bc-course` 依赖创造前提。
- ✅ **docs（统计口径校准，保持行为不变）**：使用 Serena 重新盘点 `eva-infra/src/main/java/edu/cuit/infra/gateway/impl` 下残留 `*GatewayImpl.java` 数量为 **15**（不含已归位 `bc-course/infrastructure` 的 `Course*GatewayImpl`），据此修正本文件 0.10 的历史统计口径（仅文档更新，不改代码）。
- ✅ **S0.2 延伸（课程读侧查询接口归位：CourseQueryRepo，保持行为不变）**：Serena 证伪 `CourseQueryRepo` 仅由课程读侧委托壳/查询仓储使用后，将 `CourseQueryRepo` 从 `eva-infra-shared` 归位到 `bc-course/infrastructure`（保持 `package` 不变；最小回归通过；落地提交：`5101a341`）。
- ✅ **S0.2 延伸（课程读侧查询实现归位：QueryRepository，保持行为不变）**：将 `CourseQueryRepository` 从 `eva-infra` 归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.bccourse.query` 不变，仅搬运文件/编译闭合，不改任何业务语义）；为闭合编译期依赖，将 `CourseRecommendExce` 从 `eva-infra` 迁移到 `eva-infra-shared`（保持 `package` 不变；最小回归通过；落地提交：`881e1d12`）。
- ✅ **S0.2 延伸（依赖方收敛：eva-infra 去 bc-course 编译期依赖，保持行为不变）**：Serena 证伪 `eva-infra` 未引用 `edu.cuit.bc.course.*` 后，将 `eva-infra/pom.xml` 的 `bc-course` 依赖替换为 `shared-kernel`（每次只改 1 个 `pom.xml`；保持行为不变；最小回归通过；落地提交：`8d806bf0`）。
- ✅ **S0.2 延伸（课程旧 gateway 归位：Query/Update，保持行为不变）**：将 `CourseQueryGatewayImpl/CourseUpdateGatewayImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，`package`/事务边界/异常文案/副作用顺序完全不变）；为闭合编译期依赖，先将 `CourseQueryRepo` 从 `eva-infra` 迁移到 `eva-infra-shared`，后续再归位到 `bc-course/infrastructure`（均保持 `package` 不变；最小回归通过；落地提交：`d438e060/5101a341`）。
- ✅ **S0.2 延伸（课程域基础设施归位：RepositoryImpl 推进，保持行为不变）**：将 `ChangeCourseTemplateRepositoryImpl/ImportCourseFileRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，`package`/事务边界/日志/异常文案/副作用顺序完全不变）；为闭合编译期依赖，将 `CourseImportExce` 从 `eva-infra` 迁移到 `eva-infra-shared`（保持 `package` 不变）并在 `eva-infra-shared` 补齐 `zym-spring-boot-starter-cache`（`LocalCacheManager`）；并用 Serena 证伪：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留由 3 减至 1（最小回归通过；落地提交：`33032890`）。
- ✅ **S0.2 延伸（课程域基础设施归位：operate 子簇继续收敛，保持行为不变）**：Serena 证伪：`eva-infra-shared/src/main/java/edu/cuit/infra/gateway/impl/course/operate/` 下 `CourseImportExce/CourseRecommendExce` 的引用点均仅位于 `bc-course/infrastructure`，因此将其从 `eva-infra-shared` 归位到 `bc-course/infrastructure`（保持 `package edu.cuit.infra.gateway.impl.course.operate` 不变，仅搬运文件/编译闭合；最小回归通过；落地提交：`d3b9247e`）。同时确认 `CourseFormat` 被课程/评教/旧入口复用，继续留在 `eva-infra-shared`。
- ✅ **S0.2 延伸（依赖方收敛：证伪 `eva-app` 暂不可去 `bc-course`，保持行为不变）**：Serena 证据化确认：`eva-app` 仍大量引用 `edu.cuit.bc.course.*`（典型落点：`BcCourseConfiguration` 与 `ICourse*ServiceImpl/IUserCourseServiceImpl` 对 `bc-course` 用例/端口/异常的直接依赖），因此不满足“仅使用 `edu.cuit.client.*` 类型”的前提；本阶段不改 `eva-app/pom.xml` 的 `bc-course` 依赖，避免引入编译/装配缺失（最小回归通过）。
- ✅ **S0.2 延伸（依赖方收敛：证伪“当前无更多可替换 pom”，保持行为不变）**：本次重新盘点：除 `eva-app` 外，未发现仍显式依赖 `bc-course` 且满足“仅使用 `edu.cuit.client.*` 类型”的模块；因此本会话未新增 `pom.xml` 依赖替换提交点（保持行为不变）。下一步需先继续收敛 `eva-app` 对 `edu.cuit.bc.course.*` 的直接引用，再回到“逐个模块依赖替换”。
- ✅ **S0.2 延伸（课程域基础设施归位：RepositoryImpl 收尾，保持行为不变）**：将 `AssignEvaTeachersRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，`package`/事务边界/日志/异常文案/副作用顺序完全不变）；并用 Serena 证伪：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留由 1 清零（最小回归通过；落地提交：`7f5beed9`）。
- ✅ **S0.2 延伸（依赖方收敛：eva-adapter 去 bc-course 传递依赖，保持行为不变）**：Serena 证伪 `eva-adapter` 未引用 `edu.cuit.bc.course.*` 内部实现类/包，仅使用 `edu.cuit.client.*` 协议类型；因此在 `eva-adapter/pom.xml` 对 `eva-app` 的依赖上排除传递的 `bc-course`，并显式依赖 `shared-kernel`（最小回归通过；落地提交：`f8ff84f5`）。
- ✅ **S0.2 延伸（依赖方收敛补齐：教室接口下沉 shared-kernel + 旧网关归位，保持行为不变）**：为闭合 `eva-adapter` 对 `edu.cuit.client.api.IClassroomService` 的编译期引用，将 `IClassroomService` 从 `bc-course/application` 下沉到 `shared-kernel`（保持 `package` 不变）；同时将旧 `CourseDeleteGatewayImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（保持 `package` 不变），以缩小 `eva-infra` 对 `bc-course` 用例的实现承载面（最小回归通过；落地提交：`38f58e0a`）。

**2026-01-07（本次会话）**
- ✅ **S0.2 延伸（课程域基础设施归位：RepositoryImpl 推进，保持行为不变）**：将 `DeleteCourseTypeRepositoryImpl/UpdateCourseTypeRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，`package`/事务边界/异常文案/副作用顺序完全不变）；并用 Serena 证伪：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留由 5 减至 3（最小回归通过；落地提交：`33844ce0`）。
- ✅ **S0.2 延伸（课程域基础设施归位：RepositoryImpl 推进，保持行为不变）**：将 `DeleteCourseRepositoryImpl/DeleteCoursesRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，`package`/事务边界/异常文案/副作用顺序完全不变）；并用 Serena 证伪：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留由 7 减至 5（最小回归通过；落地提交：`df4ac6ca`）。
- ✅ **docs（交接与计划同步，保持行为不变）**：同步本次会话“RepositoryImpl 归位推进进度 + 残留清单 + 下一步里程碑/提交点 + 新对话开启提示词（含 IDEA MCP 预检用法）”到三文档（`NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md`；最小回归通过；落地提交：`22e8d970`、`88f94467`）。
- ✅ **S0.2 延伸（依赖方收敛补齐：学期 API 下沉 shared-kernel，保持行为不变）**：为闭合 `bc-evaluation-infra` 对 `edu.cuit.client.api.ISemesterService` 的编译期引用，将 `ISemesterService` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；行为不变），从而确保 `bc-evaluation-infra` 无需再经由 `bc-course` 才能编译通过（最小回归通过；落地提交：`c22802ff`）。
- ✅ **S0.2 延伸（课程域基础设施归位前置：缓存常量归位，保持行为不变）**：将 `ClassroomCacheConstants` 从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package` 与 Spring Bean 名称 `classroomCacheConstants` 不变；行为不变），为后续迁移 `bccourse adapter/*RepositoryImpl` 出 `eva-infra` 做依赖闭包准备（最小回归通过；落地提交：`c22802ff`）。
- ✅ **S0.2 延伸（课程域基础设施归位起步，保持行为不变）**：将 `edu.cuit.infra.bccourse.adapter` 下 15 个无缓存/无事务注解的 `*PortImpl` 从 `eva-infra` 迁移到 `bc-course-infra`（仅搬运文件，`package` 不变；行为不变），为后续 `eva-infra` 去 `bc-course` 编译期依赖铺路（最小回归通过；落地提交：`c4179654`）。
- ✅ **S0.2 延伸（课程域基础设施归位：RepositoryImpl 起步，保持行为不变）**：将 `AddCourseTypeRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，`package`/缓存失效/事务边界/异常文案完全不变）；为闭合编译期依赖，在 `bc-course-infra` 补齐 `zym-spring-boot-starter-cache`（`LocalCacheManager`）依赖；并用 Serena 证伪：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留由 15 减至 14（最小回归通过；落地提交：`8426d4f2`）。
- ✅ **S0.2 延伸（课程域基础设施归位：RepositoryImpl 推进，保持行为不变）**：将 `UpdateCoursesTypeRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，`package`/事务边界/异常文案/副作用顺序完全不变）；并用 Serena 证伪：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留由 14 减至 13（最小回归通过；落地提交：`12d16c6a`）。
- ✅ **S0.2 延伸（课程域基础设施归位：RepositoryImpl 推进，保持行为不变）**：将 `UpdateCourseInfoRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，`package`/事务边界/异常文案/副作用顺序完全不变）；并用 Serena 证伪：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留由 13 减至 12（最小回归通过；落地提交：`eb940498`）。
- ✅ **S0.2 延伸（课程域基础设施归位：RepositoryImpl 推进，保持行为不变）**：将 `DeleteSelfCourseRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，`package`/事务边界/异常文案/副作用顺序完全不变）；并用 Serena 证伪：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留由 12 减至 11（最小回归通过；落地提交：`73ed7c7d`）。
- ✅ **S0.2 延伸（课程域基础设施归位：RepositoryImpl 推进，保持行为不变）**：将 `UpdateSingleCourseRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，`package`/事务边界/日志/异常文案/副作用顺序完全不变）；为闭合编译期依赖，将 `CourInfTimeOverlapQuery` 归位到 `eva-infra-shared`（保持 `package` 不变）并在 `bc-course-infra` 补齐 `zym-spring-boot-starter-logging` 编译期依赖以闭合 `LogUtils`；并用 Serena 证伪：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留由 11 减至 10（最小回归通过；落地提交：`1a01e827`）。
- ✅ **S0.2 延伸（课程域基础设施归位：RepositoryImpl 推进，保持行为不变）**：将 `UpdateSelfCourseRepositoryImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（仅搬运文件，`package`/事务边界/异常文案/副作用顺序完全不变）；为闭合编译期依赖，将 `ClassroomOccupancyChecker` 归位到 `eva-infra-shared`（保持 `package` 不变）；并用 Serena 证伪：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留由 10 减至 9（最小回归通过；落地提交：`3d1dd4f1`）。
- ✅ **S0.2 延伸（课程域基础设施归位：RepositoryImpl 批量推进试点，保持行为不变）**：按“选项 2（2 类同簇）”试点，将 `AddExistCoursesDetailsRepositoryImpl` 与 `AddNotExistCoursesDetailsRepositoryImpl` 从 `eva-infra` 批量归位到 `bc-course/infrastructure`（仅搬运文件，`package`/事务边界/日志/异常文案/副作用顺序完全不变）；并试点引入 IDEA MCP `get_file_problems(errorsOnly=true)` 做搬运后快速预检（不替代最小回归）；再用 Serena 证伪：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留由 9 减至 7（最小回归通过；落地提交：`bd042ea9`）。

**2026-01-06（本次会话）**
- ✅ **S0.2 延伸（依赖方收敛，证伪：eva-infra 暂不可去 bc-course，保持行为不变）**：Serena 证据化确认：`eva-infra` 仍大量引用 `edu.cuit.bc.course.*`（课程域用例/端口/异常；落点包括 `infra/bccourse/adapter/*` 与旧 `Course*GatewayImpl`），因此不满足“仅使用 `edu.cuit.client.*`”的前提，**本阶段不可**将 `eva-infra/pom.xml` 的 `bc-course` 依赖替换为 `shared-kernel`（避免引入编译错误/装配缺失）。下一步应先按里程碑把这批课程域基础设施/委托壳逐步归位到 `bc-course-infra`（或 `bc-course/infrastructure`），再评估去依赖（保持行为不变）。
- ✅ **S0.2 延伸（依赖方收敛：bc-course 编译期依赖削减，保持行为不变）**：Serena 证伪 `eva-infra-shared` 未引用 `edu.cuit.bc.course.*` 内部实现类/包，仅使用已由 `shared-kernel` 承载的 `edu.cuit.client.*` 类型（例如 `CourseExcelBO/CourseDetailCO/UpdateCourseCmd/CoursePeriod/CourseType` 等）；因此将 `eva-infra-shared/pom.xml` 中对 `bc-course` 的编译期依赖替换为显式依赖 `shared-kernel`（最小回归通过；落地提交：`6ab3837a`）。
- ✅ **S0.2 延伸（依赖方收敛：bc-course 编译期依赖削减，保持行为不变）**：Serena 证伪 `bc-evaluation-infra` 未引用 `edu.cuit.bc.course.*` 内部实现类/包，仅使用 `edu.cuit.client.*` 课程域 API；因此将 `bc-evaluation/infrastructure/pom.xml` 中对 `bc-course` 的编译期依赖替换为显式依赖 `shared-kernel`（最小回归通过；落地提交：`a0bcf74f`）。
- ✅ **S0.2 延伸（bc-course 协议承载面收敛，保持行为不变）**：将课程详情接口 `ICourseDetailService` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变），并将其签名依赖的 `SimpleCourseResultCO` 一并下沉 `shared-kernel` 以闭合依赖（最小回归通过；落地提交：`f9ccc6e9`）。
- ✅ **S0.2 延伸（bc-course 协议承载面收敛，保持行为不变）**：将课程 API 接口 `ICourseService/ICourseTypeService` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；仅接口定义搬运，不改业务语义；最小回归通过；落地提交：`4dbeb55f`）。
- ✅ **S0.2 延伸（bc-course 协议承载面收敛，保持行为不变）**：将单课次衍生详情 CO `ModifySingleCourseDetailCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；Serena 证据化：仓库内未发现引用点，属于协议残留；最小回归通过；落地提交：`1e9be81d`）。
- ✅ **S0.2 延伸（bc-course 协议承载面收敛，保持行为不变）**：将单节课详情 CO `SingleCourseDetailCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变），继续削减 `bc-course` 的“协议承载面”（最小回归通过；落地提交：`95b01a07`）。
- ✅ **S0.2 延伸（bc-course 协议承载面收敛，保持行为不变）**：将评教域 CO `CourseScoreCO/EvaTeacherInfoCO` 从 `bc-evaluation/contract` 迁移到 `shared-kernel`（保持 `package` 不变），并移除 `bc-course/application` 对 `bc-evaluation-contract` 的编译期依赖（其在应用层仅剩这两处类型引用；避免“课程协议签名依赖评教 contract”导致的跨 BC 编译期耦合；最小回归通过，含补充 `UserServiceImplTest`；落地提交：`bc30e9de`）。
- ✅ **S0.2（依赖面收敛，保持行为不变）**：将课程查询 Query（`CourseQuery/CourseConditionalQuery/MobileCourseQuery`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地提交：`e479ce0e`），继续削减 `eva-domain` 依赖 `bc-course` 的原因集合。
- ✅ **S0.2（依赖面收敛，保持行为不变）**：将导入课表 BO `CourseExcelBO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地提交：`1f47a032`），继续削减 `eva-domain` 依赖 `bc-course` 的原因集合。
- ✅ **S0.2（依赖面收敛，保持行为不变）**：将课程写侧命令对象子簇（`AlignTeacherCmd/UpdateCourseTypeCmd/UpdateCoursesCmd/UpdateCoursesToTypeCmd/UpdateSingleCourseCmd`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地提交：`0978b3cb`），继续削减 `eva-domain` 依赖 `bc-course` 的原因集合。
- ✅ **S0.2（依赖面收敛，保持行为不变）**：将课程 CO 子簇（`SubjectCO/SelfTeachCourseCO/SelfTeachCourseTimeCO/SelfTeachCourseTimeInfoCO`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地提交：`87d8c692`），为后续迁移剩余课程 cmd 做前置准备。
- ✅ **S0.2（依赖面收敛，保持行为不变）**：将课程写侧剩余命令对象（`UpdateCourseCmd/AddCoursesAndCourInfoCmd/UpdateCourseInfoAndTimeCmd`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地提交：`0d18e4ad`），从而完成 `edu.cuit.client.dto.cmd.course/*` 的迁移闭包。
- ✅ **S0.2（依赖面收敛，保持行为不变）**：将推荐课程 CO `RecommendCourseCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地提交：`24595a53`），继续削减 `eva-domain` 依赖 `bc-course` 的原因集合。
- ✅ **S0.2（依赖面收敛，保持行为不变）**：将评教模板 CO `EvaTemplateCO` 从 `bc-evaluation-contract` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地提交：`34579fe0`），用于解除课程 CO（`CourseModelCO`）的类型依赖阻塞，支撑后续迁移 `CourseDetailCO`。
- ✅ **S0.2（依赖面收敛，保持行为不变）**：将课程详情相关 CO（`TeacherInfoCO/CourseModelCO/CourseDetailCO`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地提交：`4dbcb2de`），进一步消除 eva-domain 对 bc-course 的类型依赖。
- ✅ **S0.2（依赖面收敛，保持行为不变）**：Serena 证伪 `eva-domain` 已不再需要 `bc-course` 提供的 `edu.cuit.client.*` 类型后，移除 `eva-domain/pom.xml` 对 `bc-course` 的 Maven 依赖，改为显式依赖 `shared-kernel`（最小回归通过；落地提交：`01b36508`）。
  - 补充（保持行为不变）：`bc-ai-report-infra` 原先通过 `eva-domain` 间接获得 `IUserCourseService` 类型依赖；本次在 `bc-ai-report-infra/pom.xml` 补齐对 `bc-course` 的显式依赖以闭合编译依赖（同一提交：`01b36508`）。
- ✅ **依赖收敛（保持行为不变）**：将课程用户侧接口 `IUserCourseService`（以及其出参 `SimpleSubjectResultCO`）迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地提交：`e2a697f1`），从而移除 `bc-ai-report-infra` 对 `bc-course` 的显式编译期依赖（避免依赖回潮）。
- ✅ **docs（交接与计划同步，保持行为不变）**：补齐本次“依赖收敛补齐点”的三文档同步（`NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md`），并按铁律再次执行最小回归（Java17；命令见 0.10；`BUILD SUCCESS`）；落地提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准。

**2026-01-05（本次会话）**
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：压扁 `CourseUpdateGatewayImpl.addExistCoursesDetails`：新增 `AddExistCoursesDetailsGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 委托调用（Serena：调用点为 `AddExistCoursesDetailsPortImpl.addExistCoursesDetails`；保持行为不变；最小回归通过；落地提交：`de34a308`）。
- ✅ **S0.2（依赖面收敛，保持行为不变）**：将学期 CO `SemesterCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地提交：`77126c4a`），为后续移除 `eva-domain/pom.xml` 对 `bc-course` 的依赖做前置准备。
- ✅ **S0.2（依赖面收敛，保持行为不变）**：将通用学期入参 `Term` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地提交：`23bff82f`），继续削减 `eva-domain` 依赖 `bc-course` 的原因集合。

**2026-01-04（本次会话）**
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：压扁 `CourseUpdateGatewayImpl.addNotExistCoursesDetails`：新增 `AddNotExistCoursesDetailsGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 委托调用（Serena：调用点为 `AddNotExistCoursesDetailsPortImpl.addNotExistCoursesDetails`；副作用顺序完全不变；最小回归通过；落地提交：`62d48ee6`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：压扁 `CourseUpdateGatewayImpl.updateSelfCourse`：新增 `UpdateSelfCourseGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 委托调用（Serena：调用点为 `UpdateSelfCoursePortImpl.updateSelfCourse`；副作用顺序完全不变；最小回归通过；落地提交：`c0f30c1f`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：压扁 `CourseUpdateGatewayImpl.updateSingleCourse`：新增 `UpdateSingleCourseGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 异常转换 + 委托调用（Serena：调用点为 `UpdateSingleCoursePortImpl.updateSingleCourse`；异常文案/副作用顺序完全不变；最小回归通过；落地提交：`9eea1a54`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：压扁 `CourseUpdateGatewayImpl.importCourseFile`：新增 `ImportCourseFileGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 异常转换 + 委托调用（Serena：调用点为 `ImportCourseFilePortImpl.importCourseFile`；异常文案/副作用顺序完全不变；最小回归通过；落地提交：`5e93a08a`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：压扁 `CourseUpdateGatewayImpl.updateCourses`：旧 gateway 不再构造 Command，仅保留事务边界 + 异常转换 + 委托调用（委托 `UpdateCoursesEntryUseCase`；Serena：未发现 `courseUpdateGateway.updateCourses(...)` 调用点，应用入口已直连用例；保持行为不变；最小回归通过；落地提交：`84dffcc2`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：压扁 `CourseUpdateGatewayImpl.updateCourse`：新增 `UpdateCourseGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 异常转换 + 委托调用（Serena：调用点为 `UpdateCoursePortImpl.updateCourse`；异常文案/副作用顺序完全不变；最小回归通过；落地提交：`c31df92c`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：进一步压扁 `CourseDeleteGatewayImpl.deleteSelfCourse`：新增 `DeleteSelfCourseGatewayEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地提交：`c0268b14`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：进一步压扁 `CourseDeleteGatewayImpl.deleteCourses`：新增 `DeleteCoursesGatewayEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地提交：`6428e685`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：进一步压扁 `CourseDeleteGatewayImpl.deleteCourse`：新增 `DeleteCourseGatewayEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地提交：`dfd977fe`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：进一步压扁 `CourseDeleteGatewayImpl.deleteCourseType`：新增 `DeleteCourseTypeEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地提交：`cf747b9c`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：压扁 `CourseUpdateGatewayImpl.updateCoursesType`：新增 `UpdateCoursesTypeEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地提交：`709dc5b6`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：压扁 `CourseUpdateGatewayImpl.assignTeacher`：新增 `AssignTeacherGatewayEntryUseCase`，并让旧 gateway 不再构造命令（旧 gateway 仍保留事务边界与异常转换；异常文案/副作用顺序完全不变；最小回归通过；落地提交：`0b85c612`）。
- ✅ **docs（交接与计划完善，保持行为不变）**：补齐“当前重构进度汇报 / 未完成清单 / `eva-*` 退场 DoD / 下一步主线口径（bc-course S0）”，并更新 0.11 新会话提示词以便下个会话直接续接（最小回归通过；落地提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准）。

**2026-01-02（本次会话）**
- ✅ **评教用户读侧（D1：用例归位深化—去评教/被评教记录）**：新增 `UserEvaQueryUseCase` 并将旧入口 `UserEvaServiceImpl.getEvaLogInfo/getEvaLoggingInfo` 退化为纯委托壳（旧入口仍保留 `@CheckSemId` 与当前用户解析：`StpUtil` + `userQueryGateway`；异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地提交：`96e65019`）。
- ✅ **评教任务读侧（D1：用例归位深化—本人任务列表）**：将 `EvaTaskServiceImpl.evaSelfTaskInfo` 的“任务列表查询 + 懒加载顺序对齐的实体→CO 组装”归位到 `EvaTaskQueryUseCase`；旧入口仍保留 `@CheckSemId` 与当前用户解析（`StpUtil` + `userQueryGateway`）并委托 UseCase（异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地提交：`1ac196c6`）。
- ✅ **评教任务读侧（D1：用例归位深化—单任务详情）**：将 `EvaTaskServiceImpl.oneEvaTaskInfo` 退化为纯委托壳，并把 “单任务查询 + 懒加载顺序对齐的实体→CO 组装” 归位到 `EvaTaskQueryUseCase`（异常文案不变；保持行为不变；最小回归通过；落地提交：`94736365`）。
- ✅ **评教模板读侧（D1：用例归位深化—全量模板列表）**：将 `EvaTemplateServiceImpl.evaAllTemplate` 退化为纯委托壳，并把 “全量模板查询 + 结果组装” 归位到 `EvaTemplateQueryUseCase`（保持行为不变；最小回归通过；落地提交：`cd8e6ecb`）。
- ✅ **bc-messaging（消息域）：依赖收敛后半段（兜底依赖证伪）**：使用 Serena 证伪 `eva-infra` 中不存在 `bcmessaging` / `edu.cuit.bc.messaging` / `bc-messaging` 的编译期引用；并确认运行时装配由 `start` 承接（`start/pom.xml` 含 `bc-messaging` 的 `runtime` 依赖，且 `@SpringBootApplication` 默认扫描覆盖 `edu.cuit.app.config.BcMessagingConfiguration`；保持行为不变）；最小回归通过；文档闭环提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准。
- ✅ **bc-course（课程）读侧入口用例归位起步（方向 A → B）**：新增 `CourseQueryUseCase` + `CourseScheduleQueryPort`，并将旧入口 `ICourseServiceImpl.courseNum/courseTimeDetail/getDate` 保留 `@CheckSemId` 且退化为纯委托壳（逻辑仍委托既有 `CourseQueryGateway`，保持行为不变）；同时对齐 `ICourseService.courseNum` 参数顺序为 `(week, semId)` 以与控制器调用一致（避免“两个 Integer 入参”导致口径漂移；保持行为不变）；最小回归通过；落地提交：`4b06187f`。
- ✅ **bc-course（课程）读侧入口用例归位起步（方向 A → B）：单节课详情**：新增 `CourseDetailQueryUseCase` + `CourseDetailQueryPort`，并将旧入口 `ICourseServiceImpl.getCourseDetail` 保留 `@CheckSemId` 且退化为纯委托壳；内部仍按“先查 evaUsers（空则 new ArrayList）→ 再查详情 → convertor 组装 → 空则抛 QueryException(\"这节课不存在\")”的既有顺序执行（保持异常文案与行为不变）；最小回归通过；落地提交：`d045c79e`。
- ✅ **bc-course（课程）读侧入口用例归位起步（方向 A → B）：指定时间段课程**：新增 `TimeCourseQueryUseCase` + `TimeCourseQueryPort`，并将旧入口 `ICourseServiceImpl.getTimeCourse` 保留 `@CheckSemId` 与 `StpUtil.getLoginId()` 解析（保持调用次数与顺序不变）且退化为委托壳；用例层仅接收 `userName` 并委托既有 `CourseQueryGateway.getPeriodCourse(...)`（保持行为不变）；最小回归通过；落地提交：`4454ecae`。
- ✅ **bc-course（课程）写侧入口用例归位起步（方向 A → B）：分配评教老师**：新增 `AllocateTeacherUseCase` + `AllocateTeacherPort`，并将旧入口 `ICourseServiceImpl.allocateTeacher` 保留 `@CheckSemId`、`StpUtil.getLoginId()` 与 AfterCommit 发布事件的顺序不变（先分配/落库→再解析 operatorUserId→再 publishAfterCommit）；端口适配器委托既有 `CourseUpdateGateway.assignTeacher(...)`（保持异常文案/副作用顺序不变）；最小回归通过；落地提交：`6e20721b`。
- ✅ **bc-course（课程）写侧入口用例归位起步（方向 A → B）：批量删课**：新增 `DeleteCoursesEntryUseCase` + `DeleteCoursesPort`，并将旧入口 `ICourseServiceImpl.deleteCourses` 保留 `@CheckSemId`、`StpUtil.getLoginId()` 与 AfterCommit 发布事件的顺序不变（先删除/落库→再解析 operatorUserId→再 publishAfterCommit）；端口适配器委托既有 `CourseDeleteGateway.deleteCourses(...)`（其内部仍委托 `bc-course DeleteCoursesUseCase`，保持行为不变）；最小回归通过；落地提交：`d53b287a`。
- ✅ **bc-course（课程）写侧入口用例归位继续（方向 A → B）：单节课修改**：新增 `UpdateSingleCourseEntryUseCase` + `UpdateSingleCoursePort`，并将旧入口 `ICourseServiceImpl.updateSingleCourse` 保留 `@CheckSemId` 且退化为委托壳；`eva-infra` 新增端口适配器并委托既有 `courseUpdateGateway.updateSingleCourse(userName, semId, cmd)`；严格保持 `StpUtil.getLoginId()` 调用次数与顺序不变（用户名解析 → 用例/网关调用 → 再次 `StpUtil.getLoginId()` 查询 operatorUserId → AfterCommit 发布）；异常文案/副作用顺序完全不变；最小回归通过；文档闭环提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准。
- ✅ **bc-course（课程）写侧入口用例归位继续（方向 A → B）：批量新建多节课（新课程）**：新增 `AddNotExistCoursesDetailsEntryUseCase` + `AddNotExistCoursesDetailsPort`，并将旧入口 `ICourseServiceImpl.addNotExistCoursesDetails` 保留 `@CheckSemId` 且退化为委托壳；端口适配器委托既有 `CourseUpdateGateway.addNotExistCoursesDetails(...)`（复用旧事务边界与旧异常文案/副作用顺序；保持行为不变）；最小回归通过；落地提交：`5a73fb75`。
- ✅ **bc-course（课程）写侧入口用例归位继续（方向 A → B）：批量新建多节课（已有课程）**：新增 `AddExistCoursesDetailsEntryUseCase` + `AddExistCoursesDetailsPort`，并将旧入口 `ICourseServiceImpl.addExistCoursesDetails` 退化为委托壳；端口适配器委托既有 `CourseUpdateGateway.addExistCoursesDetails(...)`（复用旧事务边界与旧异常文案/副作用顺序；保持行为不变）；最小回归通过；落地提交：`a5a9c777`。
- ✅ **bc-course（课程）写侧入口用例归位继续（方向 A → B）：教师自助删课**：新增 `DeleteSelfCourseEntryUseCase` + `DeleteSelfCoursePort`，并将旧入口 `IUserCourseServiceImpl.deleteSelfCourse` 退化为委托壳；端口适配器委托既有 `CourseDeleteGateway.deleteSelfCourse(...)`（复用旧事务边界与旧异常文案/副作用顺序；保持行为不变）；最小回归通过；落地提交：`76845038`。
- ✅ **bc-course（课程）写侧入口用例归位继续（方向 A → B）：教师自助改课**：新增 `UpdateSelfCourseEntryUseCase` + `UpdateSelfCoursePort`，并将旧入口 `IUserCourseServiceImpl.updateSelfCourse` 退化为委托壳；端口适配器委托既有 `CourseUpdateGateway.updateSelfCourse(...)`（复用旧事务边界与旧异常文案/副作用顺序；保持 `CourseOperationMessageMode.TASK_LINKED` 参数不变；保持行为不变）；最小回归通过；落地提交：`2d1327d3`。
- ✅ **bc-course（课程）写侧入口用例归位继续（方向 A → B）：导入课表**：新增 `ImportCourseFileEntryUseCase` + `ImportCourseFilePort` 并将 `IUserCourseServiceImpl.importCourse` 内的 `importCourseFile(...)` 调用点端口化（保留解析/`type` 分支/异常文案/异常类型与 AfterCommit 发布顺序不变；保持行为不变）；最小回归通过；落地提交：`054b511d`。
- ✅ **bc-course（课程）写侧入口用例归位继续（方向 A → B）：修改课程信息**：新增 `UpdateCourseEntryUseCase` + `UpdateCoursePort` 并将 `ICourseDetailServiceImpl.updateCourse` 内的 `updateCourse(...)` 调用点端口化（保持 `@CheckSemId` 触发点与异常转换顺序不变；保持 AfterCommit 发布顺序不变；保持行为不变）；最小回归通过；落地提交：`bcf17d7f`。
- ✅ **bc-course（课程）写侧入口用例归位继续（方向 A → B）：批量修改课程模板**：新增 `UpdateCoursesEntryUseCase` 并将旧入口 `ICourseDetailServiceImpl.updateCourses` 保留 `@CheckSemId/@Transactional` 且退化为委托壳；内部仍复用既有 `ChangeCourseTemplateUseCase` 的校验与落库顺序，并保持异常转换为 `UpdateException` 的异常类型/文案不变；保持行为不变；最小回归通过；落地提交：`849ed92e`。
- ✅ **bc-course（课程）写侧入口用例归位继续（方向 A → B）：删除课程**：新增 `DeleteCourseEntryUseCase` + `DeleteCoursePort` 并将旧入口 `ICourseDetailServiceImpl.delete` 保留 `@CheckSemId` 且退化为委托壳；严格保持 `deleteCourse(...) → userId 查询 → publishAfterCommit(...)` 的既有顺序不变，且异常文案/异常类型不变（仍为 `QueryException("请先登录")`）；保持行为不变；最小回归通过；落地提交：`e38463c2`。
- ✅ **bc-course（课程）写侧入口用例归位继续（方向 A → B）：新增课程**：新增 `AddCourseEntryUseCase` + `AddCoursePort` 并将旧入口 `ICourseDetailServiceImpl.addCourse` 保留 `@CheckSemId` 且退化为委托壳；保持 `courseUpdateGateway.addCourse(semId)` 的既有调用链与副作用顺序不变；保持行为不变；最小回归通过；落地提交：`5c989ace`。
- ✅ **bc-course（课程，S0 收尾：依赖收窄）**：清理旧入口 `ICourseServiceImpl` 中残留的未使用注入依赖（`CourseQueryGateway/CourseUpdateGateway` 仅声明无调用点），以降低装配耦合风险（保持行为不变；最小回归通过；落地提交：`9577cd85`）。
- ✅ **bc-course（课程，S0 收尾：依赖收窄）**：清理旧入口 `IUserCourseServiceImpl` 中残留的未使用注入依赖（`CourseDeleteGateway/MsgServiceImpl/MsgResult` 仅声明无调用点），以降低装配耦合风险（保持行为不变；最小回归通过；落地提交：`402affc2`）。
- ✅ **bc-course（课程，S0 收尾：依赖收窄）**：将旧入口 `IUserCourseServiceImpl.isImported` 的依赖从 `CourseUpdateGateway` 收敛为直接依赖 `IsCourseImportedUseCase`（原 gateway 本身已委托该用例；本次仅收窄注入依赖，不改业务语义；保持行为不变；最小回归通过；落地提交：`25aad45a`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：压扁 `CourseUpdateGatewayImpl.updateCourseType`：新增 `UpdateCourseTypeEntryUseCase` 并让旧 gateway 仅保留 `@Transactional` 边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地提交：`785974a6`）。
- ✅ **bc-course（课程，S0：旧 gateway 压扁为委托壳）**：压扁 `CourseUpdateGatewayImpl.addCourseType`：新增 `AddCourseTypeEntryUseCase` 并让旧 gateway 仅保留 `@Transactional` 边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地提交：`34e9a0a8`）。
- ✅ **规划与证据化（不改业务语义）**：补齐 `DDD_REFACTOR_PLAN.md` 的 `10.5`（`eva-*` 技术切片退场/整合到 BC 的前置条件与 DoD），并用 Serena 盘点 `eva-app` 中仍存在的 bc-course 写侧 `@CheckSemId` 入口与 `eva-infra` 旧 `*GatewayImpl` 候选清单，已落盘到 `docs/DDD_REFACTOR_BACKLOG.md` 的 `4.3`（用于后续 S1/S2 排期与退场证伪；保持行为不变）。
- ✅ **文档口径修正（不改业务语义）**：将 `DDD_REFACTOR_PLAN.md` 中“`bc-messaging` 作为近期主线”的旧建议修正为“已阶段性闭环、后置仅做结构折叠/依赖证伪”，避免下一会话误切主线（保持行为不变）。

**2026-01-01（本次会话）**
- ✅ **评教模板读侧（D1：用例归位深化—按任务取模板）**：将 `EvaTemplateServiceImpl.evaTemplateByTaskId` 退化为纯委托壳，并把 “按任务取模板 + 空结果兜底 JSON” 归位到 `EvaTemplateQueryUseCase`（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`f98a9eed`）。
- ✅ **评教任务读侧（D1：用例归位深化—分页）**：新增 `EvaTaskQueryUseCase` 并将 `EvaTaskServiceImpl.pageEvaUnfinishedTask` 退化为纯委托壳，把“分页查询 + 实体→CO 组装 + 分页结果组装”归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`d67f0ace`）。
- ✅ **评教模板读侧（D1：用例归位深化—分页）**：新增 `EvaTemplateQueryUseCase` 并将 `EvaTemplateServiceImpl.pageEvaTemplate` 退化为纯委托壳（保持 `@CheckSemId` 触发点不变；时间格式/分页字段赋值顺序不变；保持行为不变；最小回归通过；落地提交：`afcb4ff7`）。
- ✅ **bc-messaging（消息域）：依赖收敛后半段（运行时装配上推准备）**：在 `start/pom.xml` 增加对 `bc-messaging` 的 `runtime` 依赖（保持行为不变；最小回归通过；落地提交：`f23254ec`）。
- ✅ **bc-messaging（消息域）：依赖收敛后半段（运行时装配责任上推）**：移除 `eva-infra/pom.xml` 对 `bc-messaging` 的 `runtime` 依赖，把“运行时装配责任”从 `eva-infra` 上推到组合根 `start`（保持行为不变；最小回归通过；落地提交：`507f95b2`）。
- ✅ **评教记录读侧（依赖收窄，小步）**：`EvaRecordServiceImpl.pageEvaRecord` 内联分页结果组装，移除对 `PaginationBizConvertor` 的注入依赖（分页字段赋值顺序/异常文案/循环副作用顺序不变；最小回归通过；落地提交：`55103de1`）。
- ✅ **评教记录读侧（D1：用例归位深化）**：新增 `EvaRecordQueryUseCase` 并将 `EvaRecordServiceImpl.pageEvaRecord` 退化为纯委托壳，把“实体→CO 组装 + 平均分填充 + 分页组装”编排逻辑归位到 UseCase（保持 `@CheckSemId` 触发点不变；异常文案/副作用顺序不变；最小回归通过；落地提交：`86772f59`）。
- ✅ **评教记录读侧（D1：顺序对齐加固）**：对齐 `EvaRecordQueryUseCase` 内部“实体→CO 组装”的求值顺序，避免提前触发 `Supplier` 缓存加载导致副作用顺序漂移（保持行为不变；最小回归通过；落地提交：`10991314`）。

**2025-12-30（本次会话）**
- ✅ **bc-messaging（消息域）：依赖收敛准备（事件枚举下沉到 contract）**：将 `CourseOperationMessageMode` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`b2247e7f`）。
- ✅ **bc-messaging（消息域）：依赖收敛准备（事件载荷下沉到 contract）**：将 `CourseOperationSideEffectsEvent` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`ea2c0d9b`）。
- ✅ **bc-messaging（消息域）：依赖收敛准备（事件载荷下沉到 contract）**：将 `CourseTeacherTaskMessagesEvent` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`12f43323`）。
- ✅ **bc-messaging（消息域）：依赖收敛（应用侧编译期依赖面收窄）**：`eva-app` 已将对 `bc-messaging` 的编译期依赖收敛为仅依赖 `bc-messaging-contract`（Serena 证据：`eva-app` 仅引用 `edu.cuit.bc.messaging.application.event.*`；保持行为不变；最小回归通过；落地提交：`d3aeb3ab`）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位前置（DAL 归位）**：将消息表数据对象 `MsgTipDO` 从 `eva-infra` 归位到 `eva-infra-dal`（保持 `package edu.cuit.infra.dal.database.dataobject` 不变；仅类归位，不改任何业务语义；为后续把 `eva-infra/.../bcmessaging/adapter/*PortImpl` 逐个归位到 `bc-messaging` 并把依赖收敛到 `eva-infra-dal` 预置；最小回归通过；落地提交以 `git log -n 1 -- eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/dataobject/MsgTipDO.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位前置（DAL 归位）**：将消息表 Mapper `MsgTipMapper`（以及对应 `MsgTipMapper.xml`）从 `eva-infra` 归位到 `eva-infra-dal`（保持 `package edu.cuit.infra.dal.database.mapper` 不变；XML namespace/SQL 文案/字段映射不变；仅类与资源归位，不改任何业务语义；为后续逐个搬运 `Message*PortImpl` 并把依赖收敛到 `eva-infra-dal` 预置；最小回归通过；落地提交以 `git log -n 1 -- eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/mapper/MsgTipMapper.java` 为准）。
- （后续收敛，保持行为不变）：`MsgTipMapper` 已进一步从 `eva-infra-dal` 归位到 `bc-messaging`（保持 `package` 不变，仅改变 Maven 模块归属；最小回归通过；落地：`4af9f9fc`；对应文件：`bc-messaging/src/main/java/edu/cuit/infra/dal/database/mapper/MsgTipMapper.java`）。
- （后续收敛，保持行为不变）：`MsgTipDO` 已进一步从 `eva-infra-dal` 归位到 `bc-messaging`（保持 `package` 不变，仅改变 Maven 模块归属；最小回归通过；落地：`87b38a55`；对应文件：`bc-messaging/src/main/java/edu/cuit/infra/dal/database/dataobject/MsgTipDO.java`）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位（消息删除）**：将 `MessageDeletionPortImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.bcmessaging.adapter` 不变；删除条件与调用顺序完全不变；并在 `bc-messaging` 补齐对 `eva-infra-dal` 的依赖以闭合 `MsgTipDO/MsgTipMapper` 与 MyBatis-Plus API；最小回归通过；落地提交以 `git log -n 1 -- bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageDeletionPortImpl.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位（消息已读）**：将 `MessageReadPortImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.bcmessaging.adapter` 不变；校验逻辑/异常文案/更新条件与调用顺序完全不变；最小回归通过；落地提交以 `git log -n 1 -- bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageReadPortImpl.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位（消息显示状态）**：将 `MessageDisplayPortImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.bcmessaging.adapter` 不变；校验逻辑/异常文案/更新条件与调用顺序完全不变；最小回归通过；落地提交以 `git log -n 1 -- bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageDisplayPortImpl.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位前置（Convertor 归位）**：将 MapStruct 转换器 `MsgConvertor` 从 `eva-infra` 归位到 `eva-infra-shared`，并在 `eva-infra-shared` 补齐对 `bc-messaging-contract` 的依赖以闭合 `GenericRequestMsg` 类型引用（保持 `package edu.cuit.infra.convertor` 不变；保持行为不变；最小回归通过；落地提交以 `git log -n 1 -- bc-messaging/src/main/java/edu/cuit/infra/convertor/MsgConvertor.java` 为准；后续已进一步将 `MsgConvertor` 归位到 `bc-messaging`，仅改变 Maven 模块归属，行为不变）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位（消息新增）**：将 `MessageInsertionPortImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.bcmessaging.adapter` 不变；插入后回填 `id` 与 `createTime` 的顺序不变；并在 `bc-messaging` 补齐对 `eva-infra-shared` 的依赖以闭合 `MsgConvertor` 类型引用；最小回归通过；落地提交以 `git log -n 1 -- bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageInsertionPortImpl.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位（消息查询）**：将 `MessageQueryPortImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.bcmessaging.adapter` 不变；查询条件/排序/异常文案完全不变；最小回归通过；落地提交以 `git log -n 1 -- bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageQueryPortImpl.java` 为准）。

**2025-12-29（本次会话）**
- ✅ **bc-course（课程）：课表 Excel/POI 解析归位**：将 `eva-app` 内的课表解析实现（`edu.cuit.app.poi.course.*`）整体迁移到 `bc-course-infra`（保持 `package` 不变；异常文案/日志输出/副作用顺序完全不变），并补齐 `bc-course-infra` 对 `eva-infra-shared` 的依赖以复用 `ExcelUtils`（保持行为不变；最小回归通过；落地提交：`383dbf33`；删除旧文件并收尾：`5a7cd0a0`）。
- ✅ **bc-course（课程）：端口化与依赖收敛**：新增课表解析端口 `CourseExcelResolvePort`（`bc-course/application`），由 `bc-course-infra` 提供适配器 `CourseExcelResolvePortImpl`（内部仍复用 `CourseExcelResolver`，确保异常文案与日志不变）；`IUserCourseServiceImpl.importCourse` 改为依赖端口，移除对 `CourseExcelResolver` 的直接依赖；同时将 `eva-app` 对 `bc-course-infra` 的依赖收敛为 `runtime`，并在 `start` 测试侧补齐 `bc-course-infra` 测试依赖以保证 `CourseResolverTest` 编译（保持行为不变；最小回归通过；落地提交：`5a7cd0a0`）。
- ✅ **评教读侧进一步解耦（统计导出端口装配委托切换）**：将 `BcEvaluationConfiguration.evaStatisticsExportPort()` 从直接委托 `EvaStatisticsExcelFactory::createExcelData` 切换为委托 `bc-evaluation-infra` 的端口适配器 `EvaStatisticsExportPortImpl`（其内部仍调用 `EvaStatisticsExcelFactory.createExcelData`；保持行为不变；最小回归通过；落地提交：`565552fa`）。
- ✅ **评教读侧进一步解耦（导出基础设施归位：迁移 EvaStatisticsExcelFactory）**：将统计导出工厂 `EvaStatisticsExcelFactory` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持行为不变；导出异常文案/日志输出完全一致；最小回归通过；落地提交：`5b2c2223`）。
- ✅ **评教读侧进一步解耦（导出基础设施归位：迁移 FillUserStatisticsExporterDecorator）**：将导出装饰器 `FillUserStatisticsExporterDecorator` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package edu.cuit.app.poi.eva` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`e83600f6`）。
- ✅ **评教读侧进一步解耦（导出基础设施归位：迁移 FillEvaRecordExporterDecorator）**：将导出装饰器 `FillEvaRecordExporterDecorator` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package edu.cuit.app.poi.eva` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`b3afcb11`）。
- ✅ **评教读侧进一步解耦（导出基础设施归位：迁移 FillAverageScoreExporterDecorator）**：将导出装饰器 `FillAverageScoreExporterDecorator` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package edu.cuit.app.poi.eva` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`4e150984`）。
- ✅ **评教读侧进一步解耦（导出基础设施归位：迁移 EvaStatisticsExporter）**：将导出基类 `EvaStatisticsExporter` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package edu.cuit.app.poi.eva` 不变），并在 `bc-evaluation-infra` 补齐对 `bc-course/bc-iam-contract` 的编译依赖以闭合类型引用（仅类归位+依赖闭包，不改任何业务语义；静态初始化 `SpringUtil.getBean(...)` 次数/顺序不变；最小回归通过；落地提交：`e8ca391c`）。
- ✅ **MCP Serena 状态（符号级定位/引用分析）**：本会话使用 Serena 成功完成符号级定位与引用分析（未再复现 `TimeoutError`）。如后续出现 `TimeoutError`，允许临时降级为本地 `rg` 证据，但必须在本节记录“降级原因 + 可复现 rg 证据”，并在下一会话优先排查恢复。当前可复现证据（均在本仓库 `ddd` 分支执行）：
  - `rg -n --column "class\\s+EvaStatisticsExporter\\b" .` → `bc-evaluation/infrastructure/src/main/java/edu/cuit/app/poi/eva/EvaStatisticsExporter.java:24`
  - `rg -n --column "class\\s+FillUserStatisticsExporterDecorator\\b" .` → `bc-evaluation/infrastructure/src/main/java/edu/cuit/app/poi/eva/FillUserStatisticsExporterDecorator.java:19`
  - `rg -n --column "class\\s+EvaStatisticsExcelFactory\\b" .` → `bc-evaluation/infrastructure/src/main/java/edu/cuit/app/poi/eva/EvaStatisticsExcelFactory.java:13`
  - `rg -n --column "\\bExcelUtils\\b" .`：显示导出链路与课表解析均依赖 `ExcelUtils`（已归位到 `eva-infra-shared`，包名保持不变）。
- ✅ **评教读侧进一步解耦（导出基础设施归位准备：ExcelUtils 迁移）**：将 POI 工具类 `ExcelUtils` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package edu.cuit.app.poi.util` 不变），并在 `eva-infra-shared` 补齐 `poi/poi-ooxml` 依赖，为后续把 `EvaStatisticsExporter` 等导出实现从 `eva-app` 归位到更合理落点扫清“循环依赖”风险（保持行为不变；最小回归通过；落地提交：`04009c85`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—exportEvaStatistics 导出链路）**：引入统计导出端口 `EvaStatisticsExportPort`（由 `BcEvaluationConfiguration` 提供 Bean：委托既有 `EvaStatisticsExcelFactory.createExcelData`），并将旧入口 `EvaStatisticsServiceImpl.exportEvaStatistics` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.exportEvaStatistics`（保持 `@CheckSemId` 触发点不变；导出异常文案/日志与副作用顺序完全不变；最小回归通过；落地提交：`0d15de60`）。
- ✅ **评教读侧用例归位深化（统计：UseCase 内部 type 分支分发逻辑收敛）**：在 `EvaStatisticsQueryUseCase` 抽出 `dispatchByType(...)`，统一复用 `type==0/type==1/否则抛 SysException("type是10以外的值")` 的分发逻辑，减少重复分支判断，避免后续继续归位方法簇时出现分支口径漂移（只重构不改业务语义/异常文案；最小回归通过；落地提交：`38ce9ece`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—pageUnqualifiedUser 分页结果组装）**：将旧入口 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.pageUnqualifiedUserAsPaginationQueryResult`，并移除对 `PaginationBizConvertor` 的依赖（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`f4f3fcde`）。
- ✅ **评教读侧用例归位深化（统计：pageUnqualifiedUser 分页结果组装归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `pageUnqualifiedUserAsPaginationQueryResult`，把“`PaginationResultEntity` → `PaginationQueryResultCO`”的分页结果组装逻辑先归位到用例层，为下一步旧入口 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 退化为纯委托壳做准备（保持行为不变；最小回归通过；落地提交：`e97615e1`）。
- ✅ **bc-messaging 后置规划证据化（仅文档）**：补齐消息域“组合根/监听器/端口适配器散落点”的可回滚迁移路线与证据化路径清单（仅文档，不改代码；保持行为不变；落地提交：`4b05f515`；详见 `DDD_REFACTOR_PLAN.md` 第 10.3 节）。
- ✅ **文档同步与路线微调固化**：补齐“结构 DDD vs 语义 DDD”“何时可收敛 eva-*”的阶段性判断，并记录“允许微调 + 主线优先、暂不新增 S0 折叠试点提交”的策略说明，确保下会话续接不丢失（仅文档口径，不改业务语义）。
- ✅ **评教读侧用例归位深化（统计：evaScoreStatisticsInfo 空对象兜底重载归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `evaScoreStatisticsInfoOrEmpty`，先把 `Optional.empty` → `new EvaScoreInfoCO()` 的兜底逻辑归位到用例层（保持行为不变；为下一步让旧入口 `EvaStatisticsServiceImpl` 退化为纯委托壳做准备；最小回归通过；落地提交：`bce01df2`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—evaScoreStatisticsInfo）**：将 `EvaStatisticsServiceImpl.evaScoreStatisticsInfo` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaScoreStatisticsInfoOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`1bf3a4fe`）。
- ✅ **评教读侧用例归位深化（统计：evaTemplateSituation 空对象兜底重载归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `evaTemplateSituationOrEmpty`，先把 `Optional.empty` → `new EvaSituationCO()` 的兜底逻辑归位到用例层（保持行为不变；为下一步让旧入口 `EvaStatisticsServiceImpl` 退化为纯委托壳做准备；最小回归通过；落地提交：`89b6b1ee`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—evaTemplateSituation）**：将 `EvaStatisticsServiceImpl.evaTemplateSituation` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaTemplateSituationOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`78abf1a1`）。
- ✅ **评教读侧用例归位深化（统计：evaWeekAdd 空对象兜底重载归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `evaWeekAddOrEmpty`，先把 `Optional.empty` → `new EvaWeekAddCO()` 的兜底逻辑归位到用例层（保持行为不变；为下一步让旧入口 `EvaStatisticsServiceImpl` 退化为纯委托壳做准备；最小回归通过；落地提交：`5a8ac076`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—evaWeekAdd）**：将 `EvaStatisticsServiceImpl.evaWeekAdd` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaWeekAddOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`2a92ca0b`）。
- ✅ **评教读侧用例归位深化（统计：getEvaData 空对象兜底重载归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `getEvaDataOrEmpty`，先把 `Optional.empty` → `new PastTimeEvaDetailCO()` 的空对象兜底归位到用例层（保持行为不变；为下一步让旧入口 `EvaStatisticsServiceImpl` 退化为纯委托壳做准备；最小回归通过；落地提交：`1180a0f7`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—getEvaData）**：将 `EvaStatisticsServiceImpl.getEvaData` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.getEvaDataOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`b59db93d`）。
- ✅ **评教读侧用例归位深化（统计：getTargetAmountUnqualifiedUser 空对象兜底重载归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `getTargetAmountUnqualifiedUserOrEmpty`，先把 “`Optional.empty` → `new UnqualifiedUserResultCO().setTotal(0).setDataArr(List.of())`” 的空对象兜底归位到用例层（保持行为不变；为下一步让旧入口 `EvaStatisticsServiceImpl` 退化为纯委托壳做准备；最小回归通过；落地提交：`0ac65fb4`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—getTargetAmountUnqualifiedUser）**：将 `EvaStatisticsServiceImpl.getTargetAmountUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`b931b247`）。
- ✅ **评教读侧用例归位深化（统计：读测补齐—getTargetAmountUnqualifiedUserOrEmpty type=1）**：为 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty(type=1)` 补齐空结果兜底的读侧用例级测试（保持行为不变；最小回归通过；落地提交：`8f3e7afe`）。
- ✅ **评教读侧用例归位深化（统计：读测补齐—getEvaDataOrEmpty 空结果兜底）**：为 `EvaStatisticsQueryUseCase.getEvaDataOrEmpty` 补齐 `Optional.empty` 时返回空对象的读侧用例级测试，并保留阈值读取顺序与委托顺序验证（保持行为不变；最小回归通过；落地提交：`a44a9bba`）。
- ✅ **评教读侧用例归位深化（统计：读测补齐—getTargetAmountUnqualifiedUserOrEmpty 非法 type）**：为 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty(invalidType)` 补齐异常文案不变的读侧用例级测试，并确保不触发查询端口调用（保持行为不变；最小回归通过；落地提交：`0cb2caec`）。
- ✅ **docs（交接与计划同步）**：本次会话已按“每步闭环”将关键变更与下一步计划同步到三文档；如需定位最新同步提交，使用 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `git log -n 1 -- DDD_REFACTOR_PLAN.md` / `git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md`。

**2025-12-28（本次会话）**
- ✅ **评教读侧用例归位深化（统计：旧入口去除 EvaConfigGateway 直依赖—unqualifiedUser）**：将 `EvaStatisticsServiceImpl.pageUnqualifiedUser/getTargetAmountUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase` 的重载方法（由 UseCase 内部统一读取 `EvaConfigGateway.getEvaConfig()` 并委托既有实现），从而移除旧入口对 `EvaConfigGateway` 的直接依赖（保持 `@CheckSemId` 触发点不变；异常文案/副作用顺序不变；最小回归通过；落地提交：`21f6ad5b`）。
- ✅ **评教读侧用例归位深化（统计：unqualifiedUser 参数组装归位起步）**：在 `EvaStatisticsQueryUseCase` 新增重载方法 `pageUnqualifiedUser(semId, type, query)` 与 `getTargetAmountUnqualifiedUser(semId, type, num, error)`，内部统一读取 `EvaConfigGateway.getEvaConfig()` 并委托既有实现（不改业务语义/异常文案/副作用顺序；为下一步让 `EvaStatisticsServiceImpl` 去除对 `EvaConfigGateway` 的直接依赖做准备；最小回归 + `EvaStatisticsQueryUseCaseTest` 通过；落地提交：`0a2fec4d`）。
- ✅ **评教读侧用例归位深化（统计：getEvaData 阈值计算/参数组装归位）**：将 `EvaStatisticsServiceImpl.getEvaData` 中阈值读取（`EvaConfigGateway.getMinEvaNum/getMinBeEvaNum`）与参数组装归位到 `EvaStatisticsQueryUseCase.getEvaData(semId, num)`（保持阈值读取顺序不变；旧入口 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`8f4c07c5`）。
- ✅ **docs（交接与计划同步）**：本次会话已按“每步闭环”将关键变更与下一步计划同步到三文档；如需定位最新同步提交，使用 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `git log -n 1 -- DDD_REFACTOR_PLAN.md` / `git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md`。
- ✅ **评教读侧进一步解耦（统计：导出基类依赖类型收窄—CountAbEva）**：将导出基类 `EvaStatisticsExporter` 静态初始化中获取统计端口的依赖类型从 `EvaStatisticsOverviewQueryPort` 收窄为子端口 `EvaStatisticsCountAbEvaQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；不改任何业务语义；最小回归通过；落地提交：`7337d378`）。
- ✅ **评教读侧进一步解耦（统计：导出链路子端口补齐—CountAbEva）**：新增统计读侧子端口 `EvaStatisticsCountAbEvaQueryPort` 并让 `EvaStatisticsOverviewQueryPort` `extends` 该子端口（仅接口细分，不改实现/不改装配；保持行为不变；最小回归通过；落地提交：`24b13138`）。
- ✅ **工程噪音收敛（dev 环境 MyBatis 日志）**：将 `application-dev.yml` 中 MyBatis-Plus 的 `log-impl` 从 `org.apache.ibatis.logging.stdout.StdOutImpl` 切换为 `org.apache.ibatis.logging.slf4j.Slf4jImpl`，避免 SQL 调试日志直出 stdout（仅 dev profile，生产不变；最小回归通过；落地提交：`cb3a4620`）。
- ✅ **工程噪音收敛（dev/test 非法入参打印）**：将 `application-dev.yml/application-test.yml` 中 `common.print-illegal-arguments` 从 `true` 调整为 `false`，减少控制台噪音（仅 dev/test profile；不改业务逻辑；最小回归通过；落地提交：`21ba35dd`）。
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄—AI 报告分析）**：将 AI 报告分析端口适配器 `AiReportAnalysisPortImpl` 对记录端口的依赖类型从聚合接口 `EvaRecordQueryPort` 收窄为子端口 `EvaRecordExportQueryPort`（仅收窄依赖类型，不改调用逻辑；保持行为不变；最小回归通过；落地提交：`4fe38934`）。
- ✅ **评教读侧用例归位深化（统计：未达标用户目标数分支选择归位）**：将 `EvaStatisticsServiceImpl.getTargetAmountUnqualifiedUser` 的 `type` 分支选择与阈值选择归位到 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser`（旧入口 `@CheckSemId` 触发点不变；异常文案 `type是10以外的值` 不变；保持行为不变；最小回归通过；落地提交：`5b20d44e`）。
- ✅ **评教读侧用例归位深化（统计：未达标用户分页分支选择归位）**：将 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 的 `type` 分支选择逻辑归位到 `EvaStatisticsQueryUseCase.pageUnqualifiedUser`（`@CheckSemId` 触发点仍保留在旧入口；异常文案 `type是10以外的值` 不变；保持行为不变；最小回归通过；落地提交：`22dccc70`）。
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄—导出基类）**：将导出基类 `EvaStatisticsExporter` 静态初始化中获取记录端口的依赖类型从聚合接口 `EvaRecordQueryPort` 收窄为子端口 `EvaRecordExportQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；不改任何业务语义；最小回归通过；落地提交：`682bf081`）。
- ✅ **评教读侧进一步解耦（记录：导出链路子端口补齐—组合端口）**：新增记录导出链路子端口 `EvaRecordExportQueryPort`（组合 `EvaRecordCourseQueryPort/EvaRecordScoreQueryPort`），并让聚合端口 `EvaRecordQueryPort` `extends` 该子端口（仅新增接口+继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`5df35c36`）。
- ✅ **评教读侧进一步解耦（模板：引用面盘点结论/证伪）**：使用 Serena 盘点 `EvaTemplateQueryPort` 在全仓库的引用面，除端口定义外仅剩 `EvaTemplateQueryPortImpl` 实现侧引用；应用层（`eva-app`）未发现其它对聚合端口的注入点/调用点，因此模板主题的“端口细分 + 依赖类型收窄（服务层）”阶段可视为已闭合（保持行为不变；证据：Serena `find_referencing_symbols/search_for_pattern` 结果；最小回归通过；落地提交：`e67fc47d`）。
- ✅ **评教读侧进一步解耦（模板：依赖类型收窄—模板服务）**：将 `EvaTemplateServiceImpl` 对模板端口的依赖从聚合接口 `EvaTemplateQueryPort` 收窄为三个子端口 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型与调用点；保持行为不变；最小回归通过；落地提交：`b86db7e4`）。
- ✅ **评教读侧进一步解耦（模板：子端口接口细分—分页/全量/按任务取模板）**：新增模板读侧子端口 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`，并让 `EvaTemplateQueryPort` `extends` 这些子端口（仅新增接口+继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`a14d3c53`）。
- ✅ **评教读侧进一步解耦（任务：依赖类型收窄—任务服务）**：将 `EvaTaskServiceImpl` 对任务端口的依赖从聚合接口 `EvaTaskQueryPort` 收窄为三个子端口 `EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskInfoQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型与调用点；保持行为不变；最小回归通过；落地提交：`4b22f059`）。
- ✅ **评教读侧进一步解耦（任务：子端口接口细分—本人任务/数量统计）**：新增任务读侧子端口 `EvaTaskSelfQueryPort/EvaTaskCountQueryPort`，并让 `EvaTaskQueryPort` `extends` 这些子端口（仅新增接口+继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`9d5064fc`）。
- ✅ **评教读侧进一步解耦（任务：子端口接口细分起步—分页）**：新增任务读侧分页查询子端口 `EvaTaskPagingQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`f0a172d1`）。
- ✅ **评教读侧进一步解耦（任务：聚合端口继承子端口—分页）**：让 `EvaTaskQueryPort` `extends EvaTaskPagingQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`2fd9d24e`）。
- ✅ **评教读侧进一步解耦（任务：子端口接口细分起步—单任务信息）**：新增任务读侧“单任务信息/任务名称”子端口 `EvaTaskInfoQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`26b79c3a`）。
- ✅ **评教读侧进一步解耦（任务：聚合端口继承子端口—单任务信息）**：让 `EvaTaskQueryPort` `extends EvaTaskInfoQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`56834293`）。
- ✅ **评教读侧进一步解耦（任务：依赖类型收窄—消息）**：将 `MsgServiceImpl` 对任务端口的依赖从聚合接口 `EvaTaskQueryPort` 收窄为子端口 `EvaTaskInfoQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型；保持行为不变；最小回归通过；落地提交：`7aa49e7f`）。
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄—用户评教日志）**：将 `UserEvaServiceImpl` 对记录端口的依赖从聚合接口 `EvaRecordQueryPort` 收窄为两个子端口 `EvaRecordUserLogQueryPort/EvaRecordScoreQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型；保持行为不变；最小回归通过；落地提交：`80886841`）。
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄—消息）**：将 `MsgServiceImpl` 对记录端口的依赖从聚合接口 `EvaRecordQueryPort` 收窄为子端口 `EvaRecordCountQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型；保持行为不变；最小回归通过；落地提交：`147d486b`）。
- ✅ **本次会话总览（方向 A：记录/任务读侧细分 + 依赖收窄）**：
  - 记录主题：完成记录读侧 QueryPort 细分为 5 个子端口（得分/分页/用户日志/按课程/数量统计），并让 `EvaRecordQueryPort` `extends` 这些子端口；同时完成依赖类型收窄：`EvaRecordServiceImpl` → `EvaRecordPagingQueryPort/EvaRecordScoreQueryPort`，`UserServiceImpl` → `EvaRecordCountQueryPort`，`MsgServiceImpl` → `EvaRecordCountQueryPort`，`UserEvaServiceImpl` → `EvaRecordUserLogQueryPort/EvaRecordScoreQueryPort`（保持行为不变）。
  - 任务主题：复制“统计套路”推进：新增子端口 `EvaTaskInfoQueryPort/EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskCountQueryPort` 并让 `EvaTaskQueryPort` `extends` 这些子端口；同时完成依赖类型收窄：`MsgServiceImpl` → `EvaTaskInfoQueryPort`，`EvaTaskServiceImpl` → `EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskInfoQueryPort`（保持行为不变）。
  - 模板主题：复制“统计套路”起步：新增子端口 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort` 并让 `EvaTemplateQueryPort` `extends` 这些子端口；同时完成依赖类型收窄：`EvaTemplateServiceImpl` → `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`（保持行为不变）。
  - 导出链路（记录）：为保持导出类静态初始化的 `SpringUtil.getBean(...)` 次数/顺序不变，先补齐组合子端口 `EvaRecordExportQueryPort` 并让 `EvaRecordQueryPort` `extends`；再将导出基类 `EvaStatisticsExporter` 的记录端口依赖类型收窄为 `EvaRecordExportQueryPort`（保持行为不变）。
  - 关键落地提交（按主题归类）：
    - 记录：`4e47ffe3/e4f0efe9/fcac9324/e9034541/db876379/39a4bafe/8b24d2f8/147d486b/80886841/5df35c36/682bf081`
    - 任务：`26b79c3a/56834293/f0a172d1/2fd9d24e/7aa49e7f/9d5064fc/4b22f059`
    - 模板：`a14d3c53/b86db7e4`
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄—用户得分）**：将 `UserServiceImpl` 对记录端口的依赖从聚合接口 `EvaRecordQueryPort` 收窄为 `EvaRecordCountQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型；补充单测 `UserServiceImplTest`；最小回归与单测通过；落地提交：`8b24d2f8`）。
- ✅ **评教读侧进一步解耦（记录：聚合端口继承子端口—数量统计）**：让 `EvaRecordQueryPort` `extends EvaRecordCountQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`0d562206`）。
- ✅ **评教读侧进一步解耦（记录：子端口接口细分—数量统计）**：新增记录读侧数量统计子端口 `EvaRecordCountQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`db876379`）。
- ✅ **评教读侧进一步解耦（记录：聚合端口继承子端口—按课程）**：让 `EvaRecordQueryPort` `extends EvaRecordCourseQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`b8efeaf5`）。
- ✅ **评教读侧进一步解耦（记录：子端口接口细分—按课程）**：新增记录读侧按课程查询子端口 `EvaRecordCourseQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`e9034541`）。
- ✅ **评教读侧进一步解耦（记录：聚合端口继承子端口—用户日志）**：让 `EvaRecordQueryPort` `extends EvaRecordUserLogQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`1e025e48`）。
- ✅ **评教读侧进一步解耦（记录：子端口接口细分—用户日志）**：新增记录读侧用户日志子端口 `EvaRecordUserLogQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`fcac9324`）。
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄）**：将 `EvaRecordServiceImpl` 对记录端口的依赖从聚合接口 `EvaRecordQueryPort` 收窄为两个子端口 `EvaRecordPagingQueryPort/EvaRecordScoreQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型；最小回归通过；落地提交：`39a4bafe`）。
- ✅ **评教读侧进一步解耦（记录：聚合端口继承子端口—分页）**：让 `EvaRecordQueryPort` `extends EvaRecordPagingQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`76976c0b`）。
- ✅ **评教读侧进一步解耦（记录：子端口接口细分—分页）**：新增记录读侧分页子端口 `EvaRecordPagingQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`e4f0efe9`）。
- ✅ **评教读侧进一步解耦（记录：聚合端口继承子端口）**：让 `EvaRecordQueryPort` `extends EvaRecordScoreQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`0c7e7d13`）。
- ✅ **评教读侧进一步解耦（记录：子端口接口细分起步）**：新增记录读侧得分子端口 `EvaRecordScoreQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`4e47ffe3`）。
- ✅ **评教读侧进一步解耦（统计：依赖类型收窄）**：将 `EvaStatisticsServiceImpl` 对统计端口的依赖从聚合接口 `EvaStatisticsQueryPort` 收窄为三个子端口 `EvaStatisticsOverviewQueryPort/EvaStatisticsTrendQueryPort/EvaStatisticsUnqualifiedUserQueryPort`（不改任何业务逻辑/异常文案；仅调整依赖类型；行为不变；最小回归通过；落地提交：`c19d8801`）。
- ✅ **评教读侧进一步解耦（统计导出：依赖类型收窄）**：将导出侧静态初始化中获取的统计端口由 `EvaStatisticsQueryPort` 收窄为 `EvaStatisticsOverviewQueryPort`（保持 `SpringUtil.getBean(...)` 的调用次数与顺序不变；不改业务逻辑；行为不变；最小回归通过；落地提交：`9b3c4e6a`）。
- ✅ **评教读侧进一步解耦（统计：UseCase 归位起步）**：在 `bc-evaluation` 应用层新增统计读侧用例 `EvaStatisticsQueryUseCase`，并在 `BcEvaluationConfiguration` 装配为 Bean；`EvaStatisticsServiceImpl` 改为委托该用例完成端口调用（不改现有分支/异常文案与阈值计算逻辑；行为不变；最小回归通过；落地提交：`db09d87b`）。

**2025-12-27（本次会话）**
- ✅ **S0（结构性里程碑：`bc-ai-report` 折叠归位，阶段 1）**：将 `bc-ai-report` 折叠为 `bc-ai-report-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-ai-report`；仅搬运/依赖收敛，不改业务语义；最小回归通过；落地提交：`e14f4f7a`）。
  - 说明：本阶段先完成 Maven/目录结构折叠与源码物理搬运（保持 `package` 不变），当前仍将端口适配器与 AI 基础设施暂留在 `application` 子模块；下一步再分离到 `infrastructure` 子模块（保持行为不变）。
- ✅ **S0（结构性里程碑：`bc-ai-report` 折叠归位，阶段 2）**：将端口适配器（`edu.cuit.app.bcaireport.adapter.*`）、导出实现（`edu.cuit.app.poi.ai.*`）与 AI 基础设施（`edu.cuit.infra.ai.*`）搬运到 `bc-ai-report/infrastructure` 子模块，并在 `eva-app` 补齐对 `bc-ai-report-infra` 的依赖以保证装配（保持行为不变；最小回归通过；落地提交：`444c7aca`）。
- ✅ **S0（结构性里程碑：`bc-audit` 折叠归位，阶段 1）**：将 `bc-audit` 折叠为 `bc-audit-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-audit`；仅搬运/依赖收敛，不改业务语义；最小回归通过；落地提交：`81594308`）。
  - 说明：本次尝试使用 Serena 做符号级定位/引用分析，但 MCP 工具调用持续超时；已退化为使用本地 `rg` 做定位与引用复核。变更仅涉及 Maven/目录结构与源码物理路径（`package` 不变），不影响业务语义。
- ✅ **S0（结构性里程碑：`bc-audit` 折叠归位，阶段 2）**：将审计日志写链路的端口适配器 `edu.cuit.infra.bcaudit.adapter.LogInsertionPortImpl` 从 `eva-infra` 搬运到 `bc-audit/infrastructure` 子模块；并补齐 `eva-app` → `bc-audit-infra` 依赖以保证 Spring 装配（保持行为不变；最小回归通过；落地提交：`d7858d7a`）。
  - 说明：阶段 2 为保持行为不变，`bc-audit-infra` 过渡性依赖 `eva-infra` 以复用既有 DAL/Converter/Gateway；本会话已推进阶段 3，将依赖收敛为 `eva-infra-dal` + `eva-infra-shared`（见下条，保持行为不变）。
  - 证据化引用面（可复现）：本阶段同样尝试用 Serena 做符号级引用分析，但 MCP 工具调用持续 `TimeoutError`；已退化为使用本地 `rg` 复核关键装配/引用点（不改变语义，仅用于“定位证据”）。建议复核关键词：`LogInsertionPortImpl/LogInsertionPort/InsertLogUseCase/BcAuditConfiguration/LogGatewayImpl`。
- ✅ **S0（结构性里程碑：`bc-audit` 折叠归位，阶段 3，可选）**：将 `sys_log` 相关 DAL（`SysLog*DO/Mapper/XML`）迁移到 `eva-infra-dal`，将 `LogConverter` 迁移到 `eva-infra-shared`，并将 `bc-audit-infra` Maven 依赖由 `eva-infra` 收敛为 `eva-infra-dal` + `eva-infra-shared`（保持包名/namespace/SQL 不变；缓存/日志/异常文案/副作用顺序不变；最小回归通过；落地提交：`06ec6f3d`）。
- ✅ **评教读侧进一步解耦（后置，接口细化起步）**：细分统计 QueryPort：新增 `EvaStatisticsOverviewQueryPort/EvaStatisticsTrendQueryPort/EvaStatisticsUnqualifiedUserQueryPort`，并让 `EvaStatisticsQueryPort` `extends` 以上子端口（仅接口拆分，不改实现/不改装配；行为不变；最小回归通过；落地提交：`a1d6ccab`）。
- ✅ **条目 25（AI 报告写侧：组合根 wiring 归位）**：将 `BcAiReportConfiguration` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package edu.cuit.app.config` 不变；Bean 定义与 `@Lazy` 环断策略不变；保持行为不变；最小回归通过；落地提交：`58c2f055`）。
  - 行为快照（变更前后必须一致；用于下一步继续收敛“剩余写链路”时对照）：
    - 入口与链路顺序：`GET /evaluate/export/report`（`EvaStatisticsController.exportEvaReport`）→ `IAiCourseAnalysisService.exportDocData`（`AiCourseAnalysisService.exportDocData`）→ `ExportAiReportDocByUsernameUseCase.exportDocData`。
    - username→userId：`userIdQueryPort.findIdByUsername(username)` 若为空：记录日志 `系统异常`（`log.error`，携带 `SysException("用户数据查找失败，请联系管理员")`）→ 抛该 `SysException`（异常类型/文案不变）。
    - analysis（保持 `@CheckSemId` 切面触发链路不变）：`ExportAiReportDocByUsernameUseCase` 仍通过 `@Lazy IAiCourseAnalysisService` 调用 `aiCourseAnalysisService.analysis(semId, userId)`，以确保 `@CheckSemId` 对 `semId` 的既有语义保持不变。
    - 导出失败：`exportAiReportDocUseCase.exportDocData(analysis)` 抛 `IOException` 时：日志 `AI报告导出失败` → 抛 `SysException("报告导出失败，请联系管理员")`（异常类型/文案与日志顺序不变；触发点不变）。
- ✅ **条目 25（AI 报告写侧：`@CheckSemId` 注解下沉 shared-kernel）**：将 `edu.cuit.app.aop.CheckSemId` 从 `eva-app` 迁移到 `shared-kernel`（保持 `package` 不变；`AspectConfig` 的切面匹配表达式仍为 `@annotation(edu.cuit.app.aop.CheckSemId)`；保持行为不变；最小回归通过；落地提交：`1c595052`）。
- ✅ **条目 25（AI 报告写侧：旧入口 `AiCourseAnalysisService` 归位）**：将 `AiCourseAnalysisService` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package edu.cuit.app.service.impl.ai` 不变；保持 `@Service/@CheckSemId` 切面触发点不变；保持行为不变；最小回归通过；落地提交：`ca321a20`）。
  - 行为快照（变更前后必须一致）：
    - `analysis(semId, teacherId)`：仍由 `@CheckSemId` 处理 `semId` 的空值/非法值语义后，再委托 `AnalyseAiReportUseCase.analysis(semId, teacherId)`（不新增日志/异常）。
    - `exportDocData(semId)`：仍使用 `((String) StpUtil.getLoginId())` 获取 username（含潜在 `ClassCastException` 语义不变）→ 调用 `ExportAiReportDocByUsernameUseCase.exportDocData(semId, username)`。
- ✅ **条目 25（AI 报告写侧：AI 基础设施归位 + 依赖收敛）**：将 `edu.cuit.infra.ai.*`（模型 Bean 配置、提示词常量、消息工具、AI 服务接口等）从 `eva-infra` 迁移到 `bc-ai-report`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e2f2a7ff`），并移除 `bc-ai-report` → `eva-infra` 的编译期依赖，改为显式依赖 `langchain4j` + `langchain4j-community-dashscope-spring-boot-starter` + `bc-evaluation`（仅闭合编译依赖，不改变运行时行为）。
- ✅ **条目 25（AI 报告写侧：username→userId 链路实现归位）**：将用户名查询 userId 的端口适配器 `AiReportUserIdQueryPortImpl` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e2a608e2`）。
  - 行为快照（变更前后必须一致）：
    - 原样委托 `UserQueryGateway.findIdByUsername(username)`，返回 `Optional<Integer>` 的空值语义保持不变（不新增日志/异常）。
- ✅ **条目 25（AI 报告写侧：analysis 链路实现归位）**：将 AI 报告分析端口适配器 `AiReportAnalysisPortImpl` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`6f34e894`）。
  - 行为快照（变更前后必须一致）：
    - 使用的模型与调用顺序不变：按课程逐个构建 `CourseAiServices`（`qwenMaxChatModel`）生成优点/缺点/建议；最后再用 `deepseekChatModel` 汇总总体建议。
    - 记录并发与组装逻辑不变：`records.parallelStream()` 保持；评分计算仍依赖 `evaRecordQueryPort.getScoreFromRecord(...).get()` 的既有语义。
    - 用户不存在：日志 `根据用户id获取用户失败` → 抛 `BizException("导出报告失败，请联系管理员")`（触发点：`AiReportAnalysisPortImpl.analysis` 末尾 `userQueryGateway.findById(...).orElseThrow(...)`）。
- ✅ **条目 25（AI 报告写侧：剩余“落库/记录”链路盘点结论 / 证伪）**：本次用 Serena 对全仓库做“符号级引用分析 + 关键词检索”，未发现与 AI 报告相关的“落库/记录/缓存写入”链路（当前仅存在：导出/analysis/username→userId 查询 + 调用大模型的外部副作用；且均已归位 `bc-ai-report`）。因此条目 25 的后续重点切换为：**S0 折叠 `bc-ai-report`**（仅搬运/依赖收敛，保持行为不变）。
  - 证据清单（可复现的 Serena 查询，均在本仓库 `ddd` 分支执行）：
    - `find_referencing_symbols(IAiCourseAnalysisService)`：引用点仅在 `EvaStatisticsController` 与 `bc-ai-report` 组合根/旧入口/用例（无 DB 写链路外溢）。
    - `search_for_pattern(AiReport|AI报告|AiAnalysisBO|bcaireport|CourseAiServices)`：命中点均在 `bc-ai-report`（以及导出测试），未出现持久化/写入类。
    - `search_for_pattern(Ai\\w*(Gateway|Repository|Mapper|Dao|DO|Entity))`：无命中（未发现 AI 报告相关 DAL/Repository/Mapper）。
    - `search_for_pattern(.insert|.save\\()`（限制在 `bc-ai-report/src/main/java`）：无命中（未发现写入调用点）。
- ✅ **S0（结构性里程碑：`bc-template` 折叠归位）**：将 `bc-template` 折叠为 `bc-template-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-template`；包名不变；保持行为不变；最小回归通过；落地提交：`65091516`）。
- ✅ **S0（结构性里程碑：`bc-course` 折叠归位）**：将 `bc-course` 折叠为 `bc-course-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-course`；包名不变；保持行为不变；最小回归通过；落地提交：`e90ad03b`）。
- ✅ **条目 25（AI 报告写侧：导出链路实现归位）**：将 AI 报告导出端口适配器 `AiReportDocExportPortImpl` 与 Word 生成器 `AiReportExporter` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`d1262c32`）。
  - 行为快照（变更前后必须一致）：
    - 导出失败（`IOException`）：日志 `AI报告导出失败` → 抛 `SysException("报告导出失败，请联系管理员")`（触发点：`ExportAiReportDocByUsernameUseCase.exportDocData`）。
- ✅ **docs（交接与计划同步）**：补齐“当前总体进度汇报”口径，并重排 0.10/0.11 的“下一会话按顺序执行”清单，确保下个新会话可直接按顺序推进且不丢信息（保持行为不变；最小回归通过；落地提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准）。
- ✅ **S0.1（依赖路径继续收敛）**：移除 `eva-infra-shared` → `eva-client` Maven 直依赖（保持行为不变；最小回归通过；落地提交：`9437bb12`）。
- ✅ **S0.1（通用/跨域对象继续沉淀）**：将 `EvaProp` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`4feabdd0`）。
- ✅ **S0.1（AI 报告协议继续拆 `eva-client`）**：将 AI 接口与 BO（`IAiCourseAnalysisService/AiAnalysisBO/AiCourseSuggestionBO`）从 `eva-client` 迁移到 `bc-ai-report`，并移除 `bc-ai-report` → `eva-client` 依赖，改为显式依赖 `commons-lang3`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`badb9db6`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`，教室接口）**：将 `IClassroomService` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`59471a96`）；后续为支撑“依赖方去 `bc-course`”，已进一步下沉到 `shared-kernel`（保持 `package` 不变；保持行为不变；落地提交：`38f58e0a`）。
- ✅ **S0.1（消息协议继续拆 `eva-client`，clientobject）**：将 `EvaMsgCO` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`2f257a86`）。
- ✅ **S0.1（消息协议继续拆 `eva-client`，cmd）**：将 `SendWarningMsgCmd` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e6aa5913`）。
- ✅ **S0.1（消息协议继续拆 `eva-client`，cmd/interface/bo）**：将消息域 `IMsgService/SendMessageCmd/MessageBO` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`431a5a23`）。
- ✅ **S0.1（消息协议继续拆 `eva-client`，response DTO）**：将消息 response DTO（`GenericResponseMsg/EvaResponseMsg`）从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ecb8cee5`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`，clientobject/course 残留 CO）**：将课程域 `clientobject/course` 残留 CO（`ModifySingleCourseDetailCO/RecommendCourseCO/SelfTeachCourseCO/SelfTeachCourseTimeCO/SelfTeachCourseTimeInfoCO/SubjectCO/TeacherInfoCO`）以及 `SimpleCourseResultCO/SimpleSubjectResultCO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ce1a0a90`）。
- ✅ **S0.1（通用对象沉淀 shared-kernel，单课次 CO）**：将课程 CO `SingleCourseCO` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ccc82092`）。
- ✅ **S0.1（通用对象沉淀 shared-kernel，课程时间段/类型）**：将课程数据对象 `CoursePeriod/CourseType` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`5629bd2a`）。
- ✅ **S0.1（消息协议继续拆 `eva-client`）**：将消息入参 DTO `GenericRequestMsg` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`8fc7db99`）。
- ✅ **S0.1（状态复盘，保持行为不变）**：全仓库 Maven 依赖面已不再引用 `eva-client`；root reactor 已移除 `eva-client` 模块；仓库中已移除 `eva-client/` 目录（需要回滚通过 Git 提交点即可）；`eva-domain` 中仍存在 `import edu.cuit.client.*`，但对应类型均已由 `shared-kernel` / 各 BC contract / `eva-domain` 自身承载（包名保持不变）。
- ✅ **S0.1（收尾盘点，来源证伪；包名保持不变）**：使用 Serena 盘点 `eva-domain` 内所有 `import edu.cuit.client.*`（共 10 处导入），逐项确认类型定义文件**不在** `eva-client`（其模块/目录已退出主干），而分别落在以下模块：
  - `eva-domain`：`SysLogBO`
  - `shared-kernel`：`PagingQuery`、`GenericConditionalQuery`、`SimpleResultCO`、`PaginationQueryResultCO`、`SingleCourseCO`、`CourseTime`、`CoursePeriod`、`CourseType`
  - `bc-course`：`SemesterCO`、`CourseDetailCO`、`RecommendCourseCO`、`SelfTeachCourseCO`、`SelfTeachCourseTimeCO`、`SelfTeachCourseTimeInfoCO`、`CourseExcelBO`、`Term`、`CourseQuery`、`CourseConditionalQuery`、`MobileCourseQuery`、课程写侧命令 `AlignTeacherCmd/UpdateCourseCmd/UpdateCoursesCmd/UpdateSingleCourseCmd/UpdateCourseTypeCmd/UpdateCoursesToTypeCmd`
  - `bc-evaluation/contract`：`CourseScoreCO`、`EvaTeacherInfoCO`、`EvaConfig`
  - `bc-iam/contract`：`MenuConditionalQuery`
  - `bc-messaging-contract`：`GenericRequestMsg`
- ✅ **S0.1（`eva-client` 退出 root reactor；保持行为不变）**：选择方案 B：从 root `pom.xml` 的 `<modules>` 移除 `eva-client`（最小回归通过；落地提交：`ce07d75f`）。
- ✅ **S0.1（更彻底清理；保持行为不变）**：从仓库移除 `eva-client/` 目录（最小回归通过；落地提交：`de25e9fb`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`）**：将课程查询 Query 对象 `CourseQuery/CourseConditionalQuery/MobileCourseQuery` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`84a6a536`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`）**：将通用学期入参 `Term` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`f401dcb9`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`）**：将学期协议接口 `ISemesterService` 与学期 CO `SemesterCO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`7b5997c1`）。
- ✅ **S0.1（收敛依赖：`eva-domain` 去 `eva-client` 直依赖）**：在“可证实不再需要”的前提下移除 `eva-domain` → `eva-client` 的 Maven **直依赖**（保持行为不变；最小回归通过；落地提交：`9ff21249`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`）**：将课程写侧命令 `edu.cuit.client.dto.cmd.course/*` 与导入课表 BO `CourseExcelBO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变）；并为 `eva-infra-shared` 补齐 `bc-course` 显式依赖以闭合编译依赖（保持行为不变；最小回归通过；落地提交：`8a591703`）。
- ✅ **S0.1（IAM 继续推进，去 `eva-client` 残留）**：将 IAM 专属接口 `IDepartmentService` 从 `eva-client` 迁移到 `bc-iam-contract`（`edu.cuit.bc.iam.application.contract.api.department`），并更新 `DepartmentController/DepartmentServiceImpl` 引用（保持行为不变；最小回归通过；落地提交：`656dc36e`）。
- ✅ **P1.2（评教域继续拆 `eva-client`，盘点）**：Serena 盘点确认 `eva-client` 下评教专属目录（`api/eva`、`dto/cmd/eva`、`dto/clientobject/eva`）已迁空；评教 BC 内部引用的 `edu.cuit.client.*` 仅作为“协议包名”，其物理归属已在 `bc-evaluation-contract/shared-kernel`（保持行为不变；最小回归通过；落地提交：`e643bac9`）。
- ✅ **P1.2-3（审计日志协议继续拆 `eva-client`）**：Serena 盘点 `bc-audit` 对 `eva-client` 的实际依赖面：仅 `SysLogBO` 来自 `eva-client`（其余 `PagingQuery/GenericConditionalQuery/PaginationQueryResultCO` 已在 `shared-kernel`，`ILogService/OperateLogCO/LogModuleCO` 已在 `bc-audit`；保持行为不变；最小回归通过；落地提交：`ba21bbea`）。
- ✅ **P1.2-3（审计日志协议继续拆 `eva-client`）**：将 `SysLogBO` 从 `eva-client` 迁移到 `eva-domain`（保持 `package edu.cuit.client.bo` 不变；保持行为不变；最小回归通过；落地提交：`734a3741`）。
- ✅ **P1.2-3（审计日志协议继续拆 `eva-client`）**：在 `SysLogBO` 迁移后，移除 `bc-audit` → `eva-client` 的 Maven **直依赖**（保持行为不变；最小回归通过；落地提交：`2fcb257c`）。
- ✅ **P1.2-2（审计日志协议继续拆 `eva-client`）**：用 Serena 盘点 `ILogService/OperateLogCO/LogModuleCO` 引用面后，将其从 `eva-client` 迁移到 `bc-audit`（保持 `package` 不变；保持行为不变）；并为 `bc-audit` 补齐 `shared-kernel` 显式依赖以保证编译行为不变（落地提交：`e1dbf2d4`）。
- ✅ **S0.1-7（IAM application 去 `eva-client` 直依赖）**：Serena 盘点 `bc-iam/application` 对 `edu.cuit.client.*` 的引用面（仅通用类型，如 `PagingQuery/GenericConditionalQuery/SimpleResultCO`）；因此移除 `bc-iam/application` → `eva-client` 的直依赖（保持行为不变；落地提交：`7371ab96`）。
- ✅ **P1.2-1（评教 application 去 `eva-client` 直依赖）**：Serena 盘点评教 `bc-evaluation/application` 对 `edu.cuit.client.*` 的引用面（仅 Port 接口引用）；因此移除 `bc-evaluation/application` → `eva-client` 的直依赖（保持行为不变；落地提交：`10e8eb0b`）。
- ✅ **S0.1-4（评教 contract 收敛依赖）**：迁移 `DateEvaNumCO/TimeEvaNumCO/MoreDateEvaNumCO/SimpleEvaPercentCO/SimplePercentCO/FormPropCO` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；落地提交：`c2d8a8b1`）。
- ✅ **S0.1-5（评教 contract 收敛依赖）**：迁移 `dto/data/course/CourseTime` 到 `shared-kernel`（保持 `package` 不变；保持行为不变；落地提交：`5f21b5ce`）。
- ✅ **S0.1-6（评教 contract 去直依赖）**：移除 `bc-evaluation-contract` → `eva-client` 的直依赖；为保持编译行为不变，补齐 `bc-iam-contract` 的显式依赖（`eva-base-common`、`commons-lang3`，避免隐式经由其它模块传递；落地提交：`cf2001ef`）。
- ✅ 三文档同步（以 Git 为准，不在文内滚动固化 commitId）：使用 `git log -n 1 -- NEXT_SESSION_HANDOFF.md`、`git log -n 1 -- DDD_REFACTOR_PLAN.md`、`git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md` 获取最新同步提交。
- ✅ **docs（Backlog 路线口径对齐）**：修正 `docs/DDD_REFACTOR_BACKLOG.md` 第 6 节 bc-messaging 的“下一步”口径：基础设施端口适配器（`Message*PortImpl`）已归位完成，下一步转入“依赖收敛/结构折叠”（保持行为不变）。
- ✅ 最小回归通过（Java17）：命令见 0.10。

### 2025-12-25（上一会话摘要，保留）

- ✅ 提交点 0（纯文档闭环）：补齐“条目 25”的定义/边界与验收口径（只改文档，不改代码；落地提交：`1adc80bd`）。
- ✅ 提交点 A（结构落点，不迁业务）：启动 `bc-ai-report` / `bc-audit` 最小 Maven 子模块骨架并接入组合根（落地提交：`a30a1ff9`）。
- ✅ 提交点 B（写侧收敛，选审计日志链路）：审计日志写入 `LogGatewayImpl.insertLog` 收敛为“用例 + 端口 + 端口适配器 + 旧 gateway 委托壳”（落地提交：`b0b72263`）。
  - 行为快照（必须保持）：旧 `LogGatewayImpl.insertLog` 仍在同一处通过 `CompletableFuture.runAsync(..., executor)` 异步触发；异步线程内的字段补齐与 `logMapper.insert` 已搬运到 `bc-audit` 的端口适配器（无异常文案/缓存副作用变化）。
- ✅ 提交点 C（读侧继续拆，统计主题）：从 `EvaQueryRepo` 抽出 `EvaStatisticsQueryRepo`，并将 `EvaStatisticsQueryPortImpl` 依赖收敛到该接口（统计口径/异常文案不变；落地提交：`d5b07247`）。
- ✅ 提交点 C2（读侧继续拆，记录主题）：从 `EvaQueryRepo` 抽出 `EvaRecordQueryRepo`，并将 `EvaRecordQueryPortImpl` 依赖收敛到该接口（口径/异常文案不变；落地提交：`cae1a15c`）。
- ✅ 提交点 C3（读侧继续拆，任务主题）：从 `EvaQueryRepo` 抽出 `EvaTaskQueryRepo`，并将 `EvaTaskQueryPortImpl` 依赖收敛到该接口（口径/异常文案不变；落地提交：`82427967`）。
- ✅ 提交点 C4（读侧继续拆，模板主题）：从 `EvaQueryRepo` 抽出 `EvaTemplateQueryRepo`，并将 `EvaTemplateQueryPortImpl` 依赖收敛到该接口（口径/异常文案不变；落地提交：`889ec9b0`）。
- ✅ 提交点 B2（AI 报告写链路，导出）：`AiCourseAnalysisService.exportDocData` 收敛为“用例 + 端口 + 端口适配器 + 旧入口委托壳”（日志/异常文案不变；落地提交：`c68b3174`）。
- ✅ 提交点 B3（AI 报告写链路，导出）：`AiCourseAnalysisService.exportDocData` 进一步退化为“纯委托壳”（把 userId 解析/analysis 编排从旧入口迁出；保持行为不变；落地提交：`7f4b3358`）。
- ✅ 提交点 B4（AI 报告写链路，analysis）：`AiCourseAnalysisService.analysis` 收敛为“用例 + 端口 + 端口适配器 + 旧入口委托壳”（保持 `@CheckSemId` 切面触发点不变；日志/异常文案不变；落地提交：`a8150e7f`）。
- ✅ 提交点 B5（AI 报告写链路，用户名解析）：`ExportAiReportDocByUsernameUseCase` 内部依赖收敛：将 username → userId 的查询抽为 `bc-ai-report` 端口 + `eva-app` 端口适配器，保持异常文案与日志顺序不变（落地提交：`d7df8657`）。
- ✅ 需求变更（BC 模块组织方式）：BC 采用“**单顶层聚合模块 + 内部 `domain/application/infrastructure` 子模块**”组织方式，不再新增 `bc-*-infra` 平铺模块；历史平铺模块后续按里程碑折叠归位（仅改文档口径，不改代码语义；落地提交：`940b65ad`）。
- ✅ 结构性里程碑 S0（`bc-iam` 试点，阶段 1）：将 `bc-iam` 折叠为顶层聚合模块 `bc-iam-parent`，并在其内部落地 `domain/application/infrastructure` 子模块；同时将历史平铺模块 `bc-iam-infra` 折叠归位到 `bc-iam/infrastructure`（artifactId/包名保持不变；最小回归通过；落地提交：`0b5c5383`）。
- ✅ 结构性里程碑 S0.1（拆解 `eva-client`，`bc-iam` 先行）：将 `eva-client` 下 IAM 协议对象（`api/user/*` + `dto/cmd/user/*`）迁移到 `bc-iam/contract` 子模块（包名归位到 `edu.cuit.bc.iam.application.contract...`），并全仓库更新引用（保持行为不变；最小回归通过；落地提交：`dc3727fa`）。
- ✅ 结构性里程碑 S0（`bc-evaluation` 试点）：将 `bc-evaluation` 折叠为顶层聚合模块 `bc-evaluation-parent`，并在其内部落地 `domain/application/infrastructure` 子模块；同时将历史平铺模块 `bc-evaluation-infra` 折叠归位到 `bc-evaluation/infrastructure`（artifactId/包名保持不变；最小回归通过；落地提交：`4db04d1c`）。
- ✅ 结构性里程碑 S0.1（拆解 `eva-client`，`bc-iam` 继续推进）：将 `dto/clientobject/user/*` 中与 IAM 直接相关的 8 个 CO（`UserInfoCO/UserDetailCO/RoleInfoCO/SimpleRoleInfoCO/MenuCO/GenericMenuSectionCO/RouterDetailCO/RouterMeta`）从 `eva-client` 迁移到 `bc-iam-contract`（包名归位到 `edu.cuit.bc.iam.application.contract.dto.clientobject.user`），并全仓库更新引用（保持行为不变；最小回归通过；落地提交：`c1a51199`）。
- ✅ P1（评教统计协议归属，继续拆 `eva-client`）：新增 `bc-evaluation-contract` 子模块，并将 `IEvaStatisticsService` + 未达标用户协议对象 `UnqualifiedUserInfoCO/UnqualifiedUserResultCO` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package edu.cuit.client.*` 不变，仅物理归属与依赖收敛；保持行为不变；最小回归通过；落地提交：`978e3535`）。
- ✅ P1.1（评教域继续拆 `eva-client`，协议对象归属）：迁移 `edu.cuit.client.dto.clientobject.eva.*` + `UnqualifiedUserConditionalQuery` 到 `bc-evaluation/contract`（保持 `package` 不变）；并将评教 API 接口（`IEvaConfigService/IEvaRecordService/IEvaTaskService/IEvaTemplateService/IUserEvaService`）从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变）；同时将课程域协议接口与 CO（`edu.cuit.client.api.course.*`、`CourseDetailCO/CourseModelCO/SingleCourseDetailCO`）从 `eva-client` 迁移到 `bc-course`，以避免 `eva-client` 反向依赖评教 CO（保持行为不变；最小回归通过；落地提交：`6eb0125d`）。
- ✅ S0.1（通用对象沉淀 shared-kernel，避免误迁）：用 Serena 盘点 `PagingQuery/GenericConditionalQuery/SimpleResultCO/PaginationQueryResultCO` 的跨 BC 引用范围，确认跨 BC 复用后，新增 `shared-kernel` 子模块并将上述通用类型（含 `ConditionalQuery`）从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`a25815b2`）。
- ✅ S0.1（收敛依赖，第一步）：`bc-iam-contract` / `bc-evaluation-contract` 已增加对 `shared-kernel` 的直依赖（暂保留 `eva-client` 以可回滚；最小回归通过；落地提交：`3a0ac086`）。
- ✅ S0.1（IAM 继续推进）：迁移 IAM 查询条件 `MenuConditionalQuery` 到 `bc-iam-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`1eda37c9`）。
- ✅ P1.2（评教域继续拆 `eva-client`）：迁移评教查询条件 `EvaTaskConditionalQuery/EvaLogConditionalQuery` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`d02d5522`）。
- ✅ S0.1（通用对象沉淀 shared-kernel，继续收敛）：迁移 `ValidStatus/ValidStatusValidator` 到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`686de369`）。
- ✅ S0.1（收敛依赖，第二步）：`bc-iam-contract` 已去除对 `eva-client` 的直依赖（通过 `shared-kernel` + 必要的 BC contract 组合满足编译；保持行为不变；最小回归通过；落地提交：`8d673c17`）。
- ✅ S0.1-3（评教 contract 去依赖前置）：迁移评教 `dto/cmd/eva/*` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`2273ad61`）。
- ✅ S0.1-3（评教 contract 去依赖前置）：迁移 `EvaConfig` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`438d38bf`）。
- ✅ S0.1-4（评教 contract 收敛依赖，继续推进）：迁移 `DateEvaNumCO/TimeEvaNumCO/MoreDateEvaNumCO/SimpleEvaPercentCO/SimplePercentCO/FormPropCO` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`c2d8a8b1`）。
- ✅ S0.1-5（评教 contract 收敛依赖，课程时间模型）：迁移 `dto/data/course/CourseTime` 到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`5f21b5ce`）。
- ✅ S0.1-6（评教 contract 去 `eva-client` 直依赖）：移除 `bc-evaluation-contract` → `eva-client` 的直依赖，并补齐 `bc-iam-contract` 的显式依赖（避免隐式经由其它模块传递；保持行为不变；最小回归通过；落地提交：`cf2001ef`）。
- ✅ 本次会话提交链（按发生顺序，便于回溯/回滚）：`4db04d1c`（S0 代码）→ `135b9e6b`（S0 三文档同步）→ `c1a51199`（S0.1 代码）→ `e093900f`（S0.1 三文档同步）→ `978e3535`（P1 代码）→ `4e9e22f3`（P1 三文档同步）→ `6eb0125d`（P1.1 代码）→ `a25815b2`（S0.1 通用对象沉淀代码）→ `3a0ac086`（S0.1 contract 显式依赖 shared-kernel 代码）→ `4994b6d0`（S0.1 三文档同步）→ `1eda37c9`（S0.1 IAM QueryCondition 迁移代码）→ `c2d8a8b1`（S0.1-4 评教 CO 迁移代码）→ `5e0ba929`（S0.1-4 三文档同步）→ `5f21b5ce`（S0.1-5 CourseTime 迁移代码）→ `547cab40`（S0.1-5 三文档同步）→ `cf2001ef`（S0.1-6 评教 contract 去依赖；三文档同步提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `git log -n 1 -- DDD_REFACTOR_PLAN.md` / `git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md` 为准）。
- ✅ 需求补充（拆解 `eva-client`，并允许改包名）：后续重构将把 `edu.cuit.client.*` 下 BO/CO/DTO 等对象按业务归属迁入对应 BC（允许调整包名以归位到 `bc-xxx/application` 的 `contract/dto`）；跨 BC 复用对象再沉淀到 shared-kernel（落地提交：`a4c6bae8`）。
- ✅ 文档补强（结构性里程碑路线）：进一步明确“一个 BC 一个顶层聚合模块 + `domain/application/infrastructure` 子模块”落地方式，并补齐拆 `eva-client` 的推荐拆分步骤（只改文档口径；落地提交：`0c87c31a`）。
- ✅ 提交点 C5-1（读侧实现继续拆，统计主题）：新增 `EvaStatisticsQueryRepository` 承接 `EvaStatisticsQueryRepo` 实现，`EvaQueryRepository` 中对应方法退化为委托（口径/异常文案不变；落地提交：`9e0a8d28`；三文档同步：`61b0dfa4`）。
- ✅ 提交点 C5-2（读侧实现继续拆，记录主题）：新增 `EvaRecordQueryRepository` 承接 `EvaRecordQueryRepo` 实现，`EvaQueryRepository` 中对应方法退化为委托（口径/异常文案不变；落地提交：`985f7802`；三文档同步：`68895003`）。
- ✅ 提交点 C5-3（读侧实现继续拆，任务主题）：新增 `EvaTaskQueryRepository` 承接 `EvaTaskQueryRepo` 实现，`EvaQueryRepository` 中对应方法退化为委托（口径/异常文案不变；落地提交：`d467c65e`；三文档同步：`ebff7002`）。
- ✅ 提交点 C5-4（读侧实现继续拆，模板主题）：新增 `EvaTemplateQueryRepository` 承接 `EvaTemplateQueryRepo` 实现，`EvaQueryRepository` 中模板相关方法退化为委托（口径/异常文案不变；落地提交：`a550675a`）。
- ✅ 评教 BC 自包含三层结构试点（阶段 1：引入 `bc-evaluation-infra`）：迁移评教读侧查询（QueryPortImpl + QueryRepo/Repository）从 `eva-infra` 到 `bc-evaluation-infra`，并将 course/eva DAL（DO/Mapper/XML）迁移到 `eva-infra-dal`、将 `CourseConvertor`/`EvaConvertor`/`EvaCacheConstants`/`CourseFormat` 迁移到 `eva-infra-shared`（均保持包名不变；保持行为不变；落地提交：`be6dc05c`）。
- ✅ 评教 BC 自包含三层结构试点（阶段 2：写侧 Repo 迁移）：迁移评教写侧端口适配器/Repo（`eva-infra/src/main/java/edu/cuit/infra/bcevaluation/repository/*`）到 `bc-evaluation-infra`，并将 `CalculateClassTime` 迁移到 `eva-infra-shared` 以保持 `bc-evaluation-infra` 不依赖 `eva-infra`（均保持包名/行为不变；落地提交：`24e7f6c9`）。
- ✅ 提交点 C-1（读侧门面加固，可选）：清理 `EvaQueryRepository` 中已无引用的历史私有实现/冗余依赖，使其成为纯委托壳（保持行为不变；落地提交：`73fc6c14`；三文档同步：`083b5807`）。
- ✅ 提交点 C-2-1（读侧仓储瘦身，可选）：清理 `EvaRecordQueryRepository` 无用 import（保持行为不变；落地提交：`e2a2a717`）。
- ✅ 提交点 C-2-2（读侧仓储瘦身，可选）：清理 `EvaRecordQueryRepository` 冗余通配 import（保持行为不变；落地提交：`8b76375f`）。
- ✅ 提交点 C-2-3（读侧仓储瘦身，可选）：清理 `EvaQueryRepo` 冗余 import（保持行为不变；落地提交：`4a317344`）。
- ✅ 提交点 C-2-4（读侧仓储瘦身，可选）：清理 `EvaStatisticsQueryRepo` 通配 import（保持行为不变；落地提交：`dba6e31d`）。
- ✅ 提交点 C-2-5（读侧仓储瘦身，可选）：使用 Serena 盘点评教读侧四主题仓储（`bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/bcevaluation/query/*QueryRepository.java`）的私有字段/私有方法/内部类引用，未发现“仅定义未被调用”的可删项；因此将 C-2 视为“已无进一步可证实无引用项”并关闭（保持行为不变；落地提交：`5c1a03bc`）。
- ✅ 文档同步（以 Git 为准，不在文内滚动固化 commitId）：使用 `git log -n 1 -- NEXT_SESSION_HANDOFF.md`、`git log -n 1 -- DDD_REFACTOR_PLAN.md`、`git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md` 获取各文档的最新同步提交。
- ✅ 以上每步最小回归均已通过（Java17）：  
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

## 0.10 下一步拆分与里程碑/提交点（下一会话开始前先读完本节）

> 目标：确保“**能继续往前走**”且不丢失重构约束（只重构、不改语义；缓存/日志/异常文案/副作用顺序完全不变）。
>
> 规则提醒：每个小提交都必须做到：Serena 符号级定位/引用分析 → **最小回归** → `git commit` → 同步三份文档（本文件 + `DDD_REFACTOR_PLAN.md` + `docs/DDD_REFACTOR_BACKLOG.md`）。
> 提效补充（已试点通过，保持行为不变）：允许在“Serena 证据化盘点”之后，使用 **IDEA MCP** 做低风险的“快速预检/重构辅助”，例如 `mcp__idea__get_file_problems(errorsOnly=true)` 用于搬运后的编译错误早发现；但 **不替代** 最小回归与“每步闭环”。
>
> 阶段性策略微调（2025-12-29，持续有效）：允许“微调”（仅结构性重构；不改业务语义；缓存/日志/异常文案/副作用顺序完全不变）。在“评教统计导出基础设施归位”与“课程课表解析归位/端口化”闭环后，`bc-messaging` 的“归位 + 依赖收敛”已阶段性闭环（见 0.9）；`bc-course` 的 **S0（旧 gateway 压扁为委托壳）** 已推进到阶段性闭环（见 0.9/0.10）；`eva-domain` 已完成退场闭环（reactor 退场 + 依赖方清零）。当前主线切换为：**`eva-base` 退场收尾（优先 `eva-base-common`）**：按“逐 pom 去依赖 → reactor 退场 → 删除 pom/目录（可选）”推进，每次只改 1 个类/1 个资源/1 个 `pom.xml` 并严格闭环。
>

### 0.10.1 最新状态 & 下一步建议（滚动更新至 2026-02-20；聚焦：方案 B（严格）DoD 收口复核 + 主线切换，保持行为不变）

- 📌 **会话收口快照（更新至 2026-02-20；以 Git 为准，不在文内固化 commitId）**：
  - ✅ 分支/基线：分支必须为 `ddd`；当前交接基线以 `git rev-parse --short HEAD` 为准；本节写作时为 `d4d33081`（仅用于说明，不要在新会话提示词里固化）。
  - ✅ 目录退场：`fd -t d '^eva-' . -d 2` 无命中（`eva-infra/`、`eva-infra-dal/` 已完成目录清理；目录清理动作以空提交记录，详见 0.9）。
  - ✅ reactor 清零：`rg -n '<module>eva-' pom.xml` 无命中。
  - ⚠️ DoD 口径澄清：父 POM `artifactId` 为 `eva-backend`，直接执行 `rg -n '<artifactId>eva-' --glob '**/pom.xml' .` 会命中父坐标而导致“误判未收口”。建议用 `rg -n -P '<artifactId>eva-(?!backend)' --glob '**/pom.xml' .` 作为“除父坐标外无任何 `eva-*` 依赖/模块坐标残留”的可复现口径。
  - ✅ 工作区保护：`git stash list` 当前应为 3 条；未跟踪 `.mvnd/`、`.ssh_known_hosts` 不提交；禁止误操作 `reset/checkout`。

- 📊 **整体未完成清单（更新至 2026-02-20；保持行为不变）**：
  - ✅ `eva-infra-dal`：已完成退场闭环（保持行为不变）：root reactor 已移除 module、已删除 `eva-infra-dal/pom.xml`，且目录已清理（`fd -t d '^eva-' . -d 2` 无命中；目录清理动作以空提交记录，详见 0.9）。
  - ✅ `eva-infra-shared`：已完成退场闭环（保持行为不变）：源码已清零（Java=`0`）、依赖方 pom 引用已清零、root reactor 已移除 module，且已删除 `eva-infra-shared/pom.xml`。目录清理可后置（若为空目录，单独一刀 `rmdir eva-infra-shared`，避免口径漂移）。
  - `eva-base-common`：Java=`0`（`GenericPattern/LogModule` 已下沉到 `shared-kernel`，保持 `package edu.cuit.common.enums` 不变），但仍存在依赖方 `pom.xml` 的显式依赖点（例如 `bc-iam/contract`、`bc-iam/infrastructure`），且 root reactor 仍包含 `eva-base`（可复现口径：`fd -t f -e java . eva-base/eva-base-common/src/main/java | wc -l` + `rg -n "<artifactId>eva-base-common</artifactId>" --glob "**/pom.xml" . | sort` + `rg -n "<module>eva-" pom.xml`）。
  - `eva-infra-shared` 残留结构（Serena + `rg` 证据化，保持行为不变）：`UserConverter` 已完成“最小 Port 收敛跨 BC 使用点 → 单类搬运归位”，且 `eva-infra-shared` 已完成退场闭环；后续如需继续收口，可把相关“历史说明/回溯条目”逐步从“未完成提示”迁移到“已完成归档”。
  - 关键阻塞例（`course_type`，保持行为不变，历史说明）：此前 `CourseConvertor` 作为共享 Convertor 会扩大 `CourseTypeDO` 的引用面，因此不建议将 `CourseTypeDO` 直接归位到单一 BC `infrastructure` 以避免依赖/装配边界漂移；本会话已将 `CourseTypeDO` 下沉到 `shared-kernel`（见 0.9），该阻塞已解除。

- 🎯 **`eva-base-common` 退场下一步（建议顺序；保持行为不变；每步只改 1 个 `pom.xml` 闭环）**：
  - ✅ 1) `bc-iam/contract/pom.xml`：移除 `eva-base-common` 依赖（Serena 证伪无其它“仅存在于 eva-base-common”的类型引用；最小回归通过；落地：`fc801525`）。
  - ✅ 2) `bc-iam/infrastructure/pom.xml`：移除 `eva-base-common` 依赖（Serena 证伪无其它类型引用；最小回归通过；落地：`13398a74`）。
  - ✅ 3) `eva-base/pom.xml`：移除 `<module>eva-base-common</module>`（前置：依赖方已清零；最小回归通过；落地：`dc8d949d`）。
  - ✅ 4) 删除 `eva-base/eva-base-common/pom.xml`（前置：全仓库 `**/pom.xml` 不再出现 `<artifactId>eva-base-common</artifactId>`；最小回归通过；落地：`5ece67c3`）。
  - ✅ 5) root `pom.xml`：移除 `<module>eva-base</module>`（前置：`eva-base` 子模块已全部退场；最小回归通过；落地：`786fc543`）。

- 📦 **stash 快照（更新至 2026-02-16；保持行为不变）**：
  - 当前存在 3 条 stash（口径：`git stash list`；本会话未动）。
  - 后续如需恢复，只允许按“指定文件路径”逐刀 `restore`/`checkout -- <path>`（每刀仍需最小回归 + 提交 + 三文档同步 + push），禁止整包 pop 造成口径漂移。

- 🎯 **SysUserMapper 归位（已闭环，保持行为不变）**：
  - 背景：`SysUserMapper.xml` 与 `SysUserMapper.java` 均已归位到 `bc-iam/infrastructure`；并已删除 `eva-infra-dal` 旧位置同名类，避免 classpath 重复。
  - ✅ 已完成（保持行为不变）：`CourseConvertor` 去 `SysUserMapper` 无用 import（`1e0af235`）；`EvaUpdateGatewayImpl` 去 `SysUserMapper` 编译期依赖（`364c63a0`）；`DeleteEvaRecordRepositoryImpl` 去 `SysUserMapper` 编译期依赖（`c0f94380`）；`PostEvaTaskRepositoryImpl` 去 `SysUserMapper` 编译期依赖（`89fbc439`）；`SubmitEvaluationRepositoryImpl` 去 `SysUserMapper` 编译期依赖（`055db608`）；`EvaTaskQueryRepository` 去 `SysUserMapper` 编译期依赖（`7caaec02`）；`EvaStatisticsQueryRepository` 去 `SysUserMapper` 编译期依赖（`28338204`）；`EvaRecordQueryRepository` 去 `SysUserMapper` 编译期依赖（`0e190e6c`）；`AssignEvaTeachersRepositoryImpl` 去 `SysUserMapper` 编译期依赖（`712c4eb7`）；`DeleteCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖（`9dd8a7d1`）；`DeleteCoursesRepositoryImpl` 去 `SysUserMapper` 编译期依赖（`b193156d`）；`DeleteSelfCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖（`71060e69`）；`UpdateCourseInfoRepositoryImpl` 去 `SysUserMapper` 编译期依赖（`d5415b0a`）；`UpdateSelfCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖（`17c4bd19`）；`UpdateSingleCourseRepositoryImpl` 去 `SysUserMapper` 编译期依赖（`15135886`）；`CourseImportExce` 去 `SysUserMapper` 编译期依赖（`e3cf8426`）；`CourseRecommendExce` 去 `SysUserMapper` 编译期依赖（`cccf259b`）；`CourseQueryRepository` 去 `SysUserMapper` 编译期依赖（`ec1da722`）；`SysUserMapper.java` 归位到 `bc-iam/infrastructure`（`ff591e46`）。
  - ✅ 结论（保持行为不变）：非 IAM 模块对 `SysUserMapper` 的编译期依赖已清零，且 `SysUserMapper` 已完成归位。
- ✅ 目录退场已完成（保持行为不变）：`eva-infra-dal/` 已完成目录清理（详见 0.9 的空提交记录）；当前 `fd -t d '^eva-' . -d 2` 无命中，后续主线无需再围绕 `eva-infra-dal` 做目录收口动作。
- 下一刀建议（shared 瘦身，LDAP 子簇续接，保持行为不变；每刀只改 1 个文件闭环）：
  - 刀 1（单 pom，依赖前置）：✅ 已完成：`shared-kernel/pom.xml` 已增加 `spring-data-ldap(optional)`（或等价依赖）以闭合 `org.springframework.data.ldap.repository.LdapRepository` 编译期依赖（落地：`9dad61a8`；保持行为不变）。
  - 刀 2（单类搬运）：✅ 已完成：`LdapGroupRepo`：`eva-infra-shared` → `shared-kernel`（保持 `package` 与接口签名不变；保持行为不变；落地：`7ff087ad`）。
  - 刀 3（后置，多刀编排）：✅ 已闭环：`LdapConstants` / `EvaLdapUtils` 静态初始化互引风险已按“先证据化再分刀推进”完成拆解与下沉（保持行为不变）。
    - 刀 3-1（单类，解耦前置，保持行为不变）：✅ 已完成：`LdapConstants` 去 `EvaLdapUtils` 编译期互引（仍触发 `EvaLdapUtils` 类初始化，保持副作用顺序不变；落地：`45fa9651`）。
    - 刀 3-2（单类，搬运，保持行为不变）：✅ 已完成：将 `LdapConstants` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package` 与类内容不变；落地：`3dc2e8ff`）。
    - 刀 3-3（单类，后置，保持行为不变）：✅ 已完成：将 `EvaLdapUtils` 从 `eva-infra-shared` 下沉到 `shared-kernel`（保持 `package` 与类内容不变；落地：`05cc3039`）。

- 🧭 **策略选择（更新至 2026-02-12）：执行方案 B（严格）—— 彻底退场 `eva-*` 技术切片（保持行为不变）**：
  - 目标：最终仓库内仅保留 `bc-*` + `shared-kernel` + 组合根 `start`（以及必要的聚合父 pom）；跨 BC 只通过 `*-contract` + `shared-kernel` 对外接口调用；不再存在任何 `eva-*` 模块与 Maven 依赖。
  - DoD（可复现口径，任一不满足则视为未完成）：
    1) root reactor 清零：`rg -n '<module>eva-' pom.xml` 无命中
    2) Maven 依赖/坐标清零（排除父 POM `eva-backend`）：`rg -n -P '<artifactId>eva-(?!backend)' --glob '**/pom.xml' .` 无命中
    3) 目录退场：`fd -t d '^eva-' . -d 2` 无命中（目录按“每次只改 1 个文件”逐步删干净）
  - 路线（仍遵守“每次只改 1 个类 / 1 个 XML / 1 个 pom”）：先收敛依赖方 pom（让目标类可被新模块承接）→ 再逐类/逐资源搬运归位 → 再移除旧依赖 → 最后 reactor 退场与目录清理。

- 🎯 **下一步重构计划（建议，保持行为不变；每次只改 1 个类/1 个 XML/1 个 pom 闭环）**：
  0) ✅ IAM（阶段性关闭，保持行为不变）：Serena 证伪 `bc-*`（排除 `bc-iam/**`）范围内无 `SysUserRoleMapper`/`SysRoleMapper` 引用点；若后续新增/回归引用点，仍按“每次只改 1 个类”将直连装配/查询收敛为调用 `bc-iam-contract` 端口 `UserEntityObjectByIdDirectQueryPort`（异常文案/副作用顺序完全不变；不引入新缓存/切面副作用）。
  1) ✅ 主线优先（shared 瘦身，单类闭环，保持行为不变）：`EntityFactory` 已完成下沉链路：`eva-infra-shared` → `eva-infra-dal` → `shared-kernel`（保持 `package/类内容` 不变；最小回归通过；最新落地以 Git 为准）。
     - 文件：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/EntityFactory.java` → `eva-infra-dal/src/main/java/edu/cuit/infra/convertor/EntityFactory.java` → `shared-kernel/src/main/java/edu/cuit/infra/convertor/EntityFactory.java`
  2) ✅ 主线优先（dal 拆散，IAM，单资源闭环，保持行为不变）：已将 `SysUserMapper.xml` 从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`3dad6ef7`）。
     - `eva-infra-dal/src/main/resources/mapper/user/SysUserMapper.xml` → `bc-iam/infrastructure/src/main/resources/mapper/user/SysUserMapper.xml`
  3) 🎯 主线优先（方案 1：跨 BC 直连清零，课程域，保持行为不变）：已完成 `CourseIdByCourInfIdQueryPort` + `CourseIdByCourInfIdQueryPortImpl`，并已完成评教侧编译闭合前置（`bc-evaluation/infrastructure/pom.xml` 补齐对 `bc-course` 的 Maven 编译期依赖；落地：`f2188237`），且已将 `EvaTemplateQueryRepository.getTaskTemplate(...)` 的 `CourInfMapper.selectById` 直连收敛为端口调用（落地：`67755034`；异常文案/分支顺序不变）。补充进展：评教侧记录读仓储 `EvaRecordQueryRepository` 已去 `CourInfMapper.selectList(...)` 直连（落地：`13889255`）。下一刀建议：评教侧 `CourInfMapper` 跨 BC 直连已阶段性清零后，继续清理其它 BC（优先 `bc-template/**`）对课程域 `CourInfMapper` 的跨 BC 直连，收敛为调用 `bc-course` 的 `CourInfObjectDirectQueryPort`/`CourInfTimeSlotQueryPort`（保持行为不变）。
  4) 并行推进（dal 收尾）：`eva-infra-dal` 的 Java/XML 残留已清零；下一步建议转向“依赖方收敛 + reactor 退场”（每次只改 1 个 `pom.xml`，仍需 Serena 证据化与最小回归闭环）。
  5) 风险簇（保持行为不变）：LDAP 子簇互引与静态初始化耦合已闭环下沉到 `shared-kernel`（保持包名不变）。后续风险点转为 `eva-infra-shared` 剩余 `CourseBizConvertor/CourseConvertor/UserConverter`：需先 Serena 证据化引用面与编译闭合，再决定“归位到单 BC”或“下沉 shared-kernel”（仍每刀只改 1 个类闭环）。

- 📅 **下一步排期建议（按“刀”计，保持行为不变；按产能 2–4 刀/天粗估）**：
  - P0（0.5 天内）：统一证据口径（`eva-infra-dal/src/main/resources` 目录不存在按 0 计），并确认是否需要“独立一刀”补回空目录（仅为口径稳定；不影响运行时行为）。
  - P1（约 2–4 刀）：优先继续拆散 `eva-infra-dal`（当前 `1` 个 Java）→ 逐类归位/下沉 `DO` 与少量公共工具类（每刀 1 类；保持行为不变；`package` 不变）。建议顺序：评估 `EntityFactory`（仅当引用面可证伪为单 BC/单模块时再归位，否则继续留作共享）。
  - P2（约 1 周）：瘦身并拆散 `eva-infra-shared`（9 个 Java）→ 叶子 Convertor 优先，LDAP 子簇后置（需多刀编排顺序与过渡壳）。
  - P3（1–2 天）：处理 `eva-base-common`（2 个枚举）归位到 `shared-kernel` 或合适的共享模块，并逐 pom 清依赖。
  - P4（1–2 天）：当且仅当 `rg -n "<module>eva-" pom.xml`/`rg -n "<artifactId>eva-" --glob "**/pom.xml" .` 均可证伪后，再做 reactor 退场与目录清理（每次只改 1 个 `pom.xml`/1 个目录操作，避免回滚困难）。

- 🔍 **“什么时候可以把 eva-* 模块全部整合进 bc-*？”统一口径**：不是某个固定日期，而是一组可验证前置条件；统一判定标准与证据口径见 `DDD_REFACTOR_PLAN.md` 10.5（保持行为不变）。

- 🎯 **本次会话最新增量（2026-02-13，保持行为不变）**：
  - ✅ 证据化评估（保持行为不变）：`CourseConvertor` 迁移到 `shared-kernel` 当前仍受编译闭合阻塞（跨模块依赖含 `eva-infra-dal`，且存在 `shared-kernel ↔ eva-infra-dal` 循环依赖风险），本次按约束降级到“单资源 XML 归位”。
  - ✅ DAL 拆散试点（课程域，保持行为不变，单资源闭环）：`CourInfMapper.xml` → `bc-course/infrastructure/src/main/resources/mapper/course/CourInfMapper.xml`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`4eb6681c`）。
  - ✅ shared 瘦身前置（单 pom，保持行为不变）：`shared-kernel/pom.xml` 补齐 `zym-spring-boot-starter-jdbc` 与 `jackson-databind`，用于承接 `CourseFormat` 的 `QueryWrapper/ObjectMapper` 编译依赖（最小回归通过；落地：`322bb315`）。
  - ✅ Serena 证据化（保持行为不变）：`CourseFormat` 仍存在实例方法调用链（`ICourseDetailServiceImpl.pageCoursesInfo -> CourseFormat.selectCourOneEvaTemplateDO`）；并已完成“单类解耦前置”（落地：`8b4f69e2`）与“单类搬运到 `shared-kernel`”（落地：`dff4e751`）。
  - ✅ shared 瘦身（单类闭环，保持行为不变）：`CourseFormat` → `shared-kernel`（保持 `package/类内容` 不变；最小回归通过；落地：`dff4e751`）。
  - ✅ shared 瘦身（单类闭环，保持行为不变）：`EntityFactory` → `eva-infra-dal`（保持 `package/类内容` 不变；最小回归通过；落地：`eba15e92`；三文档同步：本提交）。
  - ✅ 跨 BC 直连清零前置（单 pom，保持行为不变）：`bc-evaluation/infrastructure/pom.xml` 补齐对 `bc-course` 的 Maven 编译期依赖（最小回归通过；落地：`f2188237`）。
  - ✅ 跨 BC 直连清零（单类，保持行为不变）：`EvaTemplateQueryRepository.getTaskTemplate(...)` 去 `CourInfMapper.selectById` 直连，改走 `CourseIdByCourInfIdQueryPort`（异常文案/分支顺序不变；最小回归通过；落地：`67755034`）。
  - ✅ 跨 BC 直连清零（单类，保持行为不变）：`EvaRecordQueryRepository` 去 `CourInfMapper.selectList(...)` 直连，改走 `CourInfObjectDirectQueryPort.findByIds/findByCourseIds(...)`（异常文案/分支/查询次数与顺序不变；最小回归通过；落地：`13889255`）。
  - ✅ websocket 归位前置（单 pom）：`bc-evaluation/infrastructure/pom.xml` 补齐对 `bc-messaging` 的 Maven 编译期依赖（最小回归通过；落地：`4dd1b34f`）。
  - ✅ websocket 支撑类归位（逐类归位）：`WebsocketManager` 从 `eva-infra-shared` 归位到 `bc-messaging`（保持 `package`/类内容不变；最小回归通过；落地：`bf78d276`）。
  - ✅ DAL 拆散试点（审计）：`SysLogModuleMapper` → `bc-audit/infrastructure`（保持 `package` 不变；最小回归通过；落地：`c901e3a6`；三文档同步：`7534337b`）。
  - ✅ DAL 拆散试点（审计）：`SysLogMapper` → `bc-audit/infrastructure`（保持 `package` 不变；最小回归通过；落地：`5de32a6c`；三文档同步：`221a4050`）。
  - ✅ 支撑类归位（审计）：`LogConverter` → `bc-audit/infrastructure`（保持 `package` 不变；最小回归通过；落地：`02c85909`；三文档同步：`84998087`）。
  - ✅ DAL 拆散试点（审计）：`SysLogDO` → `bc-audit/infrastructure`（保持 `package` 不变；最小回归通过；落地：`7de33487`；三文档同步：`ced4eee7`）。
  - ✅ DAL 拆散试点（审计）：`SysLogModuleDO` → `bc-audit/infrastructure`（保持 `package` 不变；最小回归通过；落地：`960d2bbb`；三文档同步：`f4ed59c6`）。
  - ✅ DAL 拆散试点（审计）：`SysLogMapper.xml` → `bc-audit/infrastructure`（保持 `namespace/type/SQL` 与资源路径 `mapper/**` 不变；最小回归通过；落地：`b6f05784`；三文档同步：`e339a3b2`）。
  - ✅ DAL 拆散试点（审计）：`SysLogModuleMapper.xml` → `bc-audit/infrastructure`（保持 `namespace/type/SQL` 与资源路径 `mapper/**` 不变；最小回归通过；落地：`f6c7897c`；三文档同步：`61321256`）。
  - ✅ 依赖收敛证伪（审计，单 pom，保持行为不变）：Serena 证据化确认 `bc-audit/infrastructure` 仍直接使用 `eva-infra-shared` 内类型（`QueryUtils/PaginationConverter/UserConverter/RoleConverter/EntityFactory`），因此暂不可移除该依赖；已将结论记录于 `bc-audit/infrastructure/pom.xml`（最小回归通过；落地：`a0073972`；三文档同步：`3516c1c5`）。
  - ✅ 支撑类归位（消息域，保持行为不变）：`MsgConvertor` → `bc-messaging`（保持 `package` 不变；Serena：引用面仅命中 `bc-messaging` 的 `MessageInsertionPortImpl/MessageQueryPortImpl`；最小回归通过；落地：`312756c7`；三文档同步：本提交）。
  - ✅ DAL 拆散试点（消息域，保持行为不变）：`MsgTipMapper` → `bc-messaging`（保持 `package` 不变；最小回归通过；落地：`4af9f9fc`；三文档同步：本提交）。
  - ✅ DAL 拆散试点（消息域，保持行为不变）：`MsgTipDO` → `bc-messaging`（保持 `package` 不变；Serena：引用面仅命中 `bc-messaging`；最小回归通过；落地：`87b38a55`；三文档同步：本提交）。
  - ✅ DAL 拆散试点（消息域，保持行为不变，单资源闭环）：`MsgTipMapper.xml` → `bc-messaging/src/main/resources/mapper/MsgTipMapper.xml`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归通过；落地：`5c5ab5e0`；三文档同步以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` 为准）。
  - ✅ DAL 拆散试点（IAM，保持行为不变，`sys_role_menu`）：`SysRoleMenuMapper` / `SysRoleMenuDO` / `SysRoleMenuMapper.xml` → `bc-iam/infrastructure`（保持 `package/namespace/resultMap type` 与资源路径 `mapper/**` 不变；最小回归通过；落地：`f98ee5c2` / `49fcbda7` / `db81d674`；三文档同步以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` 为准）。
  - ✅ DAL 拆散试点（课程域，保持行为不变，`course_type_course`）：`CourseTypeCourseMapper` / `CourseTypeCourseDO` / `CourseTypeCourseMapper.xml` → `bc-course/infrastructure`（保持 `package/namespace/resultMap type` 与资源路径 `mapper/**` 不变；最小回归通过；落地：`2e1cd36e` / `8f410b14` / `45bc05d6`；三文档同步以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` 为准）。
  - ✅ 支撑类归位（IAM，保持行为不变）：`SaTokenInterceptorConfig` → `bc-iam/infrastructure`（保持 `package` 不变，仅改变 Maven 模块归属；Serena：无显式引用面（由 Spring 扫描装配）；最小回归通过；落地：`78b831d9`；三文档同步以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` 为准）。
  - ✅ 支撑类归位（IAM，保持行为不变）：`StpInterfaceImpl` → `bc-iam/infrastructure`（保持 `package` 不变，仅改变 Maven 模块归属；Serena：无显式引用面（由 Spring 扫描装配）；最小回归通过；落地：`192e790c`；三文档同步以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` 为准）。
  - ✅ 支撑类归位（评教，保持行为不变）：`MsgBizConvertor` → `bc-evaluation/infrastructure`（保持 `package` 不变，仅改变 Maven 模块归属；Serena：引用面集中在评教旧入口壳 `MsgServiceImpl`（另有 `start` 单测 mock）；最小回归通过；落地：`7077924e`；三文档同步以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` 为准）。
  - ✅ 支撑类归位（评教，保持行为不变）：`EvaConfigBizConvertor` → `bc-evaluation/infrastructure`（保持 `package` 不变，仅改变 Maven 模块归属；Serena：引用面仅命中 `EvaConfigService`；最小回归通过；落地：`3d374b20`；三文档同步以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` 为准）。
  - ✅ 支撑类归位（评教，保持行为不变）：`EvaRecordBizConvertor` → `bc-evaluation/infrastructure`（保持 `package` 不变，仅改变 Maven 模块归属；Serena：未发现显式引用面；最小回归通过；落地：`6a5430cb`；三文档同步以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` 为准）。
  - ✅ 证据化结论（保持行为不变）：`SysRoleMapper` 当前引用面仅命中 `bc-iam/infrastructure`（Serena 证伪），已满足“仅单 BC 引用”约束；因此将其从 `eva-infra-dal` 搬运归位到 `bc-iam/infrastructure`（最小回归通过；落地：`60b87404`）。
  - ✅ 逐类归位：`CourInfTimeOverlapQuery` → `bc-course/infrastructure`（最小回归通过；代码落地：`ea6c99e9`；文档闭环：`2466ead3`）。
  - ✅ 逐类归位：`ClassroomOccupancyChecker` → `bc-course/infrastructure`（最小回归通过；代码落地：`b1db3422`）。
  - ✅ 依赖收敛（单 pom）：收敛 `bc-template/application/pom.xml`：在 Serena 证伪 `bc-template/application/src/main/java` 无 Lombok 引用后，移除冗余 `lombok(provided)` 依赖（编译/运行期 classpath 与行为不变；最小回归通过；代码落地：`e91844c2`；文档闭环：`57b2815a`）。
  - ✅ 快照证据（rg，保持行为不变）：根 `pom.xml` 不再出现 `<module>eva-domain</module>` / `<module>eva-infra</module>`；全仓库 `**/pom.xml` 不再出现 `<artifactId>eva-domain</artifactId>` / `<artifactId>eva-infra</artifactId>`；课程域支撑类 `CourInfTimeOverlapQuery` / `ClassroomOccupancyChecker` 各命中 1（均位于 `bc-course/infrastructure`）。
  - ✅ 编译闭合补强（单 pom）：`eva-infra-shared/pom.xml` 已补齐 `cola-component-exception` 编译期依赖（最小回归通过；代码落地：`776ab171`）。
  - ✅ 依赖收敛（单 pom）：`eva-infra-shared/pom.xml` 已去 `eva-domain` 编译期依赖，并补齐对 `bc-course-domain` / `bc-evaluation-domain` / `bc-audit-domain` / `bc-iam-domain` 的显式依赖（最小回归通过；代码落地：`0585d6fb`）。
  - ✅ reactor 退场（单 pom）：根 `pom.xml` 已移除 `<module>eva-domain</module>`（最小回归通过；代码落地：`6b907bc1`）。
  - ✅ 收尾清理（单 pom）：已删除 `eva-domain/pom.xml`（最小回归通过；代码落地：`c0035b03`）。
  - ✅ 收尾清理（单 pom）：已删除 `eva-infra/pom.xml`（最小回归通过；代码落地：`6c9b6224`）。
  - ✅ 审计实体逐类归位：已将 `SysLogModuleEntity` 从 `eva-domain` 搬运归位到 `bc-audit-domain`（保持 `package` 与类内容不变；最小回归通过；代码落地：`1f8675f1`；文档闭环：`31e4d11c`）。
  - ✅ 审计实体逐类归位：已将 `SysLogEntity` 从 `eva-domain` 搬运归位到 `bc-audit-domain`（保持 `package` 与类内容不变；最小回归通过；代码落地：`9efe8f6e`）。
  - ✅ 审计接口逐类归位：已将 `LogGateway` 从 `eva-domain` 搬运归位到 `bc-audit-domain`（保持 `package`/接口签名/注解不变；最小回归通过；代码落地：`44417e03`）。
  - ✅ 评教接口逐类归位：已将 `DynamicConfigGateway` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/接口签名/注解不变；最小回归通过；代码落地：`9e2096fc`）。
  - ✅ 消息域归位前置（单 pom）：已收敛 `bc-messaging-contract/pom.xml` 并补齐承接 `MsgEntity` 所需的最小编译期依赖（`bc-iam-domain`、`cola-component-domain-starter`；仅用于编译闭合；最小回归通过；代码落地：`51d5a042`）。
  - ✅ 消息域类型逐类归位：已将 `MsgEntity` 从 `eva-domain` 搬运归位到 `bc-messaging-contract`（保持 `package` 与类内容不变；最小回归通过；代码落地：`79d68fc3`）。
  - ✅ 消息域接口逐类归位：已将 `MsgGateway` 从 `eva-domain` 搬运归位到 `bc-messaging-contract`（保持 `package`/接口签名/注解不变；最小回归通过；代码落地：`eaf62606`）。
  - ✅ 消息域接口逐类归位：已将 `MessageUseCaseFacade` 从 `eva-domain` 搬运归位到 `bc-messaging-contract`（保持 `package`/接口签名不变；最小回归通过；代码落地：`31681de9`）。
  - ✅ 依赖收敛（单 pom）：`bc-messaging/pom.xml` 已去 `eva-domain` 编译期依赖（保持行为不变；最小回归通过；代码落地：`acecb5af`）。
  - ✅ 状态核验（Serena，保持行为不变）：`EvaUpdateGateway` 已归位 `bc-evaluation-domain`，本会话复核引用面后确认无需重复搬运（引用面：`EvaTaskServiceImpl` + `EvaUpdateGatewayImpl`）。
  - ✅ 依赖收敛（单 pom）：在 Serena 证伪 `eva-domain/src/main/java` 无课程域引用面后，已移除 `eva-domain/pom.xml` 对 `bc-course-domain` 的 Maven 编译期依赖（最小回归通过；落地：`ec4107e4`）。
  - ✅ 评教实体逐类归位：已将 `CourOneEvaTemplateEntity/EvaTaskEntity/EvaRecordEntity` 从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变，仅改变 Maven 模块归属；最小回归通过；落地：`616f925c`/`c6cb11c4`/`f4ceb140`）。
  - ✅ 编译闭合前置：已在 `bc-evaluation/domain/pom.xml` 补齐最小依赖以承接上述实体；并在 `eva-domain/pom.xml` 增加对 `bc-evaluation-domain` 的过渡依赖以保证逐类搬运过程中编译闭合（最小回归通过；落地：`c5117a1a`/`0bfbf450`）。
  - ✅ 编译闭合补强：已在 `bc-evaluation/domain/pom.xml` 增加 `spring-context(provided)`，用于承接后续逐类归位到 `bc-evaluation-domain` 的 `edu.cuit.domain.gateway.eva.*` 接口上的 `@Component` 注解（仅编译闭合；最小回归通过；落地：`132f6fc0`）。
  - ✅ 评教 gateway 逐类归位：`EvaDeleteGateway` 已从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/签名/注解不变；最小回归通过；落地：`b5f8f5fe`）。
  - ✅ 评教 gateway 逐类归位：`EvaUpdateGateway` 已从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/签名/注解不变；最小回归通过；落地：`ba43d0a4`）。
  - ✅ 评教实体逐类归位：`EvaConfigEntity` 已从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变；最小回归通过；落地：`0c7f6aae`）。
  - ✅ 评教 gateway 逐类归位：`EvaConfigGateway` 已从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package`/签名/注解不变；最小回归通过；落地：`1aaff7d5`）。
  - ✅ 评教实体逐类归位：`EvaTemplateEntity` 已从 `eva-domain` 搬运归位到 `bc-evaluation-domain`（保持 `package` 与类内容不变；最小回归通过；落地：`ee79ffac`）。
  - ✅ 可复现快照：`eva-domain` 残留 Java 文件数口径：`fd -t f -e java . eva-domain/src/main/java | wc -l`（当前为 0；清单口径：`fd -t f -e java . eva-domain/src/main/java`）。
  - ✅ 可复现快照：当前已无模块显式依赖 `eva-domain`（口径：`rg -n "<artifactId>eva-domain</artifactId>" --glob "**/pom.xml" .` 预期无命中）。
  - ⚠️ 工作区提醒（保持行为不变）：理想状态下闭环后应仅残留未跟踪 `.mvnd/`（本地 mvnd daemon/registry 目录；禁止提交）。若出现“已暂存新增 + 未暂存改动”的混合状态，禁止 `reset/checkout`，按 0.9 最新条目与本节“范围隔离提交法”处理。

- 🎯 **下一刀建议（保持行为不变；主线：审计 `sys_log` 的 DAL 按 BC 拆散试点；每次只改 1 个类/1 个资源文件闭环）**：
  1) 先跑快照证据（作为防回归；口径同 0.11）：
     - reactor：`rg -n "<module>eva-" pom.xml`（当前应仍包含 `eva-infra-dal/eva-infra-shared/eva-base`）
     - 审计 DAL 进度：`rg -n "interface\\s+SysLogModuleMapper\\b|interface\\s+SysLogMapper\\b" --glob "*.java" bc-audit/infrastructure/src/main/java`
     - `fd -t f -e java . eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/dataobject/log`（当前应为空：`SysLogDO/SysLogModuleDO` 均已归位）
     - `fd -t f -e xml . eva-infra-dal/src/main/resources/mapper/log`（当前应为空：`SysLogMapper.xml/SysLogModuleMapper.xml` 均已归位）
  2) Serena（必须）：证据化盘点 `bc-audit/infrastructure/pom.xml` 引用面（确认可收敛 `eva-infra-shared`）后，按“一次只改 1 个 `pom.xml`”的规则逐步收敛：
     - `bc-audit/infrastructure/pom.xml`
     - 约束（保持行为不变）：Java `package`、MyBatis XML `namespace`、`resultMap type` 指向的 FQCN、资源路径 `mapper/**` 均保持不变
  3) 每刀结束必跑最小回归（Java17 + mvnd）通过后：`git commit`（代码/资源）→ 同步三文档 → `git commit`（文档）→ `git push`。
  4) 并行次线（保持行为不变；每次只改 1 个 `pom.xml`）：继续做“依赖方 pom 依赖收敛”（优先 `bc-*/application/pom.xml` 或 `bc-*/contract/pom.xml`），尝试移除冗余 `lombok(provided)` / 无用 `junit-jupiter(test)`（移除前必须 Serena 证伪无引用面）。

- 🎯 **本次会话最新增量（2026-02-04，保持行为不变）**：
  - ✅ `EvaStatisticsExporter` 已去 `UserEntity` 编译期依赖（仍保持旧链路“仅当返回值为 UserEntity 才参与后续逻辑”的分支语义不变；详见 0.9；落地：`4f4b190b`）。
  - ✅ 为后续继续在评教读侧仓储去 `UserEntity` 编译期依赖做前置，在 `EvaConvertor` 增加桥接方法 `toEvaTaskEntityWithTeacherObject(...)`（仅做类型桥接，不改变 Supplier 调用时机/次数；详见 0.9；落地：`a8934ab1`）。
  - ✅ 为后续继续在评教读侧仓储去 `UserEntity` 编译期依赖做前置，在 `UserConverter` 增加桥接方法 `toUserEntityObject(...)` + `userIdOf(Object)`（仅做类型桥接，不改变 roles Supplier 的调用时机与次数；详见 0.9；落地：`c173c7c2`）。
  - ✅ 为后续继续在评教读侧仓储去 `UserEntity` 编译期依赖做前置，在 `CourseConvertor` 增加桥接方法 `toCourseEntityWithTeacherObject(...)`（仅做类型桥接，不改变 teacher Supplier 的调用时机与次数；详见 0.9；落地：`858521da`）。
  - ✅ 已清零：`bc-evaluation/infrastructure/src/main/java` 不再出现 `UserEntity` import（口径：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-evaluation/infrastructure/src/main/java` 应命中为 0；落地：`9cbcb858`）。
  - ✅ 为后续继续在消息查询适配器去 `UserEntity` 编译期依赖做前置，在 `MsgConvertor` 增加桥接方法 `toMsgEntityWithUserObject(...)`（仅做类型桥接，不改变 sender/recipient Supplier 的调用时机与次数；详见 0.9；落地：`8254a430`）。
  - ✅ 审计日志：已补齐 `LogConverter` 桥接方法 `toLogEntityWithUserObject(...)`，并完成 `bc-audit` 的 `LogGatewayImpl` 去 `UserEntity` 编译期依赖（详见 0.9；落地：`8fa053ed` / `a86f6520`）。
  - ✅ IAM：`RouterDetailFactory` 已去 `UserEntity` 编译期依赖（入参收敛为 `Object`；字段读取改走 `UserConverter` 桥接；保持行为不变；详见 0.9；落地：`5d2f7512`）。
  - ✅ IAM：`UserServiceImpl` 已去 `UserEntity` 编译期依赖（`getUserInfo` 入参收敛为 `Object`；字段读取改走 `UserBizConvertor/UserConverter` 桥接；保持行为不变；详见 0.9；落地：`d901223c`）。
  - ✅ 已清零：`bc-course/infrastructure/src/main/java` 不再出现 `UserEntity` import（口径：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-course/infrastructure/src/main/java` 应命中为 0；落地：`f0655267`）。
  - ✅ 最新快照（更新至 2026-02-04）：在 `bc-*` 范围内，除 `bc-iam/**` 的旧 `UserQueryGatewayImpl` 外，其余模块已清零 `UserEntity` import（口径：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-* --glob "*.java" | head`）。

- 🎯 **下一刀建议（保持行为不变；每次只改 1 个类闭环）**：
  1) ✅ A（类，前置）：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/user/UserConverter.java` 增加桥接方法 `toUserEntityObject(...)`（返回 `Object`）与 `userIdOf(Object)`（返回 `Integer`；内部仍使用 `UserEntity` 做强转，以保持历史空值/异常表现尽量一致）。已完成（落地：`c173c7c2`）。
  2) ✅ B（类，前置）：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/course/CourseConvertor.java` 增加桥接方法 `toCourseEntityWithTeacherObject(...)`（teacher Supplier 改为 `Supplier<?>`，内部桥接到既有 `toCourseEntity`）。已完成（落地：`858521da`）。
  3) ✅ C（类，主线）：`bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/bcevaluation/query/EvaTaskQueryRepository.java` 去 `UserEntity` import：调用侧改走上述桥接方法与 `userIdOf(Object)`，并尽量保持 DB 查询/遍历顺序不变。已完成（落地：`7f198610`）。
  4) ✅ D（类，主线）：`bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/bcevaluation/query/EvaRecordQueryRepository.java` 同上。已完成（落地：`9cbcb858`）。
  5) ✅ E（类，前置）：在 `MsgConvertor` 增加桥接方法 `toMsgEntityWithUserObject(...)`（内部仍强转为 `UserEntity`，以尽量保持历史空值/异常表现一致）。已完成（落地：`8254a430`；当前文件位于 `bc-messaging/src/main/java/edu/cuit/infra/convertor/MsgConvertor.java`，保持 `package` 不变）。
  6) ✅ F（类，主线）：`bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageQueryPortImpl.java` 已去 `UserEntity` import：调用侧改走 `msgConvertor.toMsgEntityWithUserObject(...)`，并保持 `userEntityByIdQueryPort.findById(...)` 的调用次数/顺序与异常文案不变（可复现口径：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-messaging/src/main/java` 命中应为 0；落地：`51301d23`）。
  7) ✅ G（类，前置）：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/LogConverter.java` 增加桥接方法 `toLogEntityWithUserObject(...)`，用于让 `bc-audit` 的 `LogGatewayImpl` 去 `UserEntity` 编译期依赖（仅做类型桥接，不改变 user 获取时机与次数）。已完成（落地：`8fa053ed`）。
  8) ✅ H（类，主线）：`bc-audit/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/LogGatewayImpl.java` 已去 `UserEntity` import：调用侧改走 `userConverter.toUserEntityObject(...)` + `logConverter.toLogEntityWithUserObject(...)`（异常文案/查询/遍历顺序不变；可复现口径：`rg -n "import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;" bc-audit/infrastructure/src/main/java` 命中应为 0；落地：`a86f6520`）。
  9) ✅ I（类，前置）：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/user/UserConverter.java` 已补齐“Spring Bean + setName”桥接方法 `springUserEntityWithNameObject(Object)`（内部仍 `SpringUtil.getBean(UserEntity.class)` + 强转后 `setName`，尽量保持历史异常形态与副作用顺序一致；落地：`687aea3e`）。
  10) ✅ J（类，主线）：`bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/query/CourseQueryRepository.java` 已去 `UserEntity` import：`UserEntity`/`Supplier<UserEntity>` → `Object`/`Supplier<?>`，并将 `SpringUtil.getBean(UserEntity.class)` + `setName` 改走 `userConverter.springUserEntityWithNameObject(...)`（保持缓存/查询/遍历/异常文案与副作用顺序完全不变；落地：`f0655267`）。
  11) ✅ K（类，主线）：`bc-iam/infrastructure/src/main/java/edu/cuit/app/factory/user/RouterDetailFactory.java` 已去 `UserEntity` 编译期依赖：入参收敛为 `Object` 并改走 `UserConverter` 桥接读取字段（保持过滤/递归逻辑与异常表现不变；落地：`5d2f7512`）。
  12) ✅ L（类，主线）：`bc-iam/infrastructure/src/main/java/edu/cuit/app/service/impl/user/UserServiceImpl.java` 已去 `UserEntity` 编译期依赖：`Optional<?>.map(UserEntity.class::cast)` 改走 `userConverter.castUserEntityObject(...)`，`getUserInfo` 入参收敛为 `Object` 并改走 `UserBizConvertor/UserConverter` 桥接读取字段（保持缓存/日志/异常文案/副作用顺序不变；落地：`d901223c`）。
  13) ⏳ M（类，风险高，建议最后一簇再动）：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/UserQueryGatewayImpl.java` 仍 `import UserEntity`。建议下一会话先用 Serena 证据化盘点该类“对外暴露类型/签名/调用面”后再决策：若其接口签名仍返回 `UserEntity`，则单类变更很难做到“完全去编译期依赖”，可能需要先做 Port/contract 替换方案（多步、多文件；仍需保持缓存/切面触发点不变）。

- ✅ **模板锁定能力的“可依赖面”已下沉到 `bc-template-domain`**：`CourseTemplateLockQueryPort`、`CourseTemplateLockService`、`TemplateLockedException` 均可由 `bc-template-domain` 提供（包名不变），从而让依赖方无需编译期绑定 `bc-template` 应用层 jar。
- ✅ **依赖收敛阶段性验收点**：`eva-infra` / `bc-template-infra` / `bc-course(application)` 均已将对 `bc-template` 的 Maven 依赖收敛为 `bc-template-domain`（保持行为不变；最小回归通过；详见 0.9 与 `docs/DDD_REFACTOR_BACKLOG.md` 4.2）。
- ✅ **快速证据口径（可在新会话复核）**：`rg -n "<artifactId>bc-template</artifactId>" --glob "**/pom.xml" .` 当前应仅剩 `bc-template/application/pom.xml` 的“模块自身 artifactId 声明”，不应再出现“依赖方对 bc-template 的 dependency 声明”。
- ✅ **IAM（S0.2 延伸：去 `eva-domain` 前置拆解，保持行为不变）进展**：已将 `MenuQueryGateway`、`LdapPersonEntity`、`LdapPersonGateway`（以及前置的 `MenuEntity/UserUpdateGateway/...`）逐类归位到 `bc-iam-domain`（均保持 `package`/签名/注解不变，仅改变 Maven 模块归属）；并已进一步归位 `RoleEntity/UserEntity/RoleQueryGateway`（见 0.9 的 2026-02-04 条目）。下一刀建议：继续按“引用面仅在 IAM”逐类归位剩余 `edu.cuit.domain.*` IAM-only 子集（保持行为不变）。
- ✅ **评教（S0.2 延伸）已闭环（保持行为不变）**：`eva-app/src/main/java` 对 `edu.cuit.bc.evaluation.*` 的直接引用面已清零，且 `eva-app/pom.xml` 已移除对 `bc-evaluation`（application jar）的编译期依赖（详见 0.9）。
- ✅ **审计（S0.2 延伸）已闭环（保持行为不变）**：`BcAuditConfiguration/LogBizConvertor/LogServiceImpl` 已归位 `bc-audit-infra`，且 `eva-app/pom.xml` 已移除对 `bc-audit` 与 `bc-audit-infra` 的编译期依赖；运行期装配由组合根 `start` 显式依赖 `bc-audit-infra(runtime)` 兜底（详见 0.9）。
- ✅ **AI 报告（S0.2 延伸）已闭环（保持行为不变）**：`eva-app/pom.xml` 已移除对 `bc-ai-report` 与 `bc-ai-report-infra` 的编译期依赖；运行期装配由组合根 `start` 显式依赖 `bc-ai-report-infra(runtime)` 兜底（详见 0.9）。
- ✅ **websocket（S0.2 延伸）已闭环（保持行为不变）**：已完成 `eva-app` →（共享/BC）归位：`WebSocketConfig/WebSocketInterceptor/UriUtils/MessageChannel` 均已从 `eva-infra-shared` 进一步归位到 `bc-messaging`（均保持 `package` 不变）；同时 `eva-app/src/main/java` 不再 `import org.springframework.web.socket.*`，并已移除 `eva-app/pom.xml` 对 `spring-boot-starter-websocket` 的编译期依赖（运行期由组合根 `start` 显式兜底；详见 0.9）。
- ✅ **已闭环（2026-01-16，保持行为不变，依赖收敛：消息契约）**：`eva-app/pom.xml` 已移除对 `bc-messaging-contract` 的 Maven 编译期依赖（详见 0.9，最小回归通过）。
- ✅ **S1（IAM Controller：DepartmentController 结构性收敛，保持行为不变）**：已抽取 `success(...)` 统一封装 `CommonResult.success(...)` 的返回表达（仍保持“先调用 service → 再返回包装”的执行顺序不变；不改 URL/注解/异常/副作用顺序；最小回归通过）；落地：`fbc5fb74`（三文档同步：`798c62b8`）。
- ✅ **S0.2 延伸（IAM：依赖收敛，保持行为不变）**：已完成 `EvaRecordCountQueryPort` 下沉至 `bc-evaluation-contract` + `bc-iam-infra` 去 `bc-evaluation` 编译期依赖（落地：`4c30b02c/42a9e96c`；详见 0.9）。
- 🎯 **下一刀（建议，保持行为不变；每次只改 1 个类或 1 个 pom）**：
  - A（已完成，pom，保持行为不变）：消息域依赖收敛 —— `eva-infra-shared/pom.xml` 已去 `eva-domain` 编译期依赖并补齐显式 domain 依赖（落地：`0585d6fb`）；且根 `pom.xml` 已从 reactor 移除 `<module>eva-domain</module>`（落地：`6b907bc1`）。
  - B（已完成，pom，保持行为不变）：收尾清理 —— 已删除 `eva-domain/pom.xml`（落地：`c0035b03`）。
  - C（优先，保持行为不变）：继续做“依赖方 pom 收敛”（每次只改 1 个 `pom.xml`）：优先从 `bc-*/infrastructure/pom.xml` 入手，证伪“无直引”后再收敛对 `eva-infra-(shared|dal)` 的编译期依赖边界（移除/降 scope 前必须 Serena + `rg` 双证据；最小回归闭环）。
  - C（次优先：类，每次只改 1 个类）：继续 IAM S0.2 延伸——收敛依赖方对 `edu.cuit.domain.entity.user.biz.UserEntity` 的编译期依赖，优先处理 `bc-course/infrastructure` 的读侧仓储 `CourseQueryRepository`（见上方 0.10.1 的 I/J 两刀）。
  - D（次优先：pom，每次只改 1 个 `pom.xml`）：继续做“依赖方 pom 收敛”，优先选择“仅类型引用”的依赖方模块；移除前必须 Serena + `rg` 双证据，避免增量编译/缓存导致误判（保持行为不变）。
  - E（并行注意事项，保持行为不变）：不要直接把 `bc-ai-report/infrastructure` 对 `bc-evaluation` 的依赖替换为 `bc-evaluation-contract` 来“省依赖”；评教导出端口签名当前会触发 Maven 循环依赖（原因见 0.9 / `DDD_REFACTOR_PLAN.md` 10.3）。
  - 📌 **当前状态快照（截至 2026-02-06，口径=可复现命令；保持行为不变）**：
    - ✅ `eva-adapter` 残留 `*Controller.java` 已清零（口径见 0.10.2；命令：`fd -t f 'Controller\\.java$' eva-adapter/src/main/java | wc -l` → 0）；组合根 `start` 已移除对 `eva-adapter` 的 Maven 依赖（落地：`92a70a9e`；见 `start/pom.xml`；保持行为不变）。
    - ✅ `eva-adapter` 已从 root reactor 退场且 `eva-adapter/pom.xml` 已删除：`rg -n "<module>eva-adapter</module>" pom.xml` 与 `rg -n "<artifactId>eva-adapter</artifactId>" --glob "**/pom.xml" .` 均应为空（保持行为不变；相关“依赖收敛”提交可视为历史闭环，不再新增/调整 `eva-adapter/pom.xml`）。
    - ⏳ “全量整合 `eva-*`（并从 reactor 移除）”仍未具备落地条件：root reactor 仍包含 `eva-infra-dal/eva-infra-shared/eva-base`（口径：`rg -n "<module>eva-" pom.xml`）；且仍有多个 `bc-*` 模块编译期依赖 `eva-infra-(shared|dal)`（口径：`rg -n "<artifactId>eva-infra-(shared|dal)</artifactId>" --glob "**/pom.xml" .`）。补充：`eva-domain` 已从 root reactor 退场（落地：`6b907bc1`）且其编译期依赖方已清零（口径：`rg -n "<artifactId>eva-domain</artifactId>" --glob "**/pom.xml" .` 预期无命中）。`eva-infra` 已从 root reactor 退场（落地：`0aab4516`），组合根 `start` 已移除 `eva-infra(runtime)` 兜底依赖（落地：`1e2ffa89`）。整合判定标准与路线见 `DDD_REFACTOR_PLAN.md` 10.5（坚持“每步只改 1 个类/1 个 pom + 最小回归 + 可回滚”）。
  - ✅ **完成条件（评估点）**：
    - ✅ 已满足（eva-app 退场）：组合根 `start` 已去 `eva-app` 依赖（`0a9ff564`）；根 `pom.xml` 已从 reactor 移除 `eva-app`（`b5f15a4b`）；已删除 `eva-app/pom.xml`（`4bfa9d40`）。其中 `eva-infra(runtime)` 过渡期兜底依赖已在 `1e2ffa89` 移除（保持行为不变）。
  - 快速证据口径（新会话先复核，避免口径漂移）：
    - reactor：`rg -n '<module>eva-app</module>' pom.xml`（当前应为空）。
    - Maven：`rg -n '<artifactId>eva-app</artifactId>' --glob '**/pom.xml' .`（当前应为空）。
    - 文件：`test -e eva-app/pom.xml && echo 1 || echo 0`（当前应输出 0）。
    - websocket：`rg -n \"<artifactId>spring-boot-starter-websocket</artifactId>\" --glob \"**/pom.xml\" .`（当前应仅命中 `start/pom.xml`）。

IDEA MCP 使用要点（可选，保持行为不变；不替代最小回归）：
- 建议时机：完成 `git mv`（或少量“编译闭合”改动）后、跑 `mvnd test` 前；用于**尽早暴露**搬运导致的导入/符号缺失/编译错误。
- 推荐调用：`mcp__idea__get_file_problems(errorsOnly=true)`（仅看 errors；warnings 不作为阻断）。
- 失败处理：若 IDEA 索引未就绪或 MCP 不可用，**不阻塞主线**；继续以 `mvnd -pl start -am test ...` 为唯一验收口径（必要时在 0.9 记录降级原因与可复现证据）。

- 当前重构进度汇报（截至当前 `HEAD`，用于“还没完成什么/下一步怎么排”）：
  - ✅ **bc-course 写侧（方向 A → B）**：入口用例归位已覆盖主干簇（见 0.9）；**S0（旧 gateway 压扁为委托壳）** 已覆盖 `CourseUpdateGatewayImpl` 的 `updateCourse/updateCourses/importCourseFile/updateSingleCourse/updateSelfCourse/addNotExistCoursesDetails/addExistCoursesDetails` 等核心方法簇（保持行为不变；见 0.9）。
  - ✅ **bc-evaluation 写侧**：发布评教任务、删除评教记录/模板等主链路已收敛；统计导出基础设施已阶段性归位（见 0.9/10.2）。
  - ✅ **bc-messaging**：组合根/监听器/端口适配器归位与“依赖收敛关键环节”已阶段性闭环（见 0.9/10.3）；后置仅做结构折叠与依赖证伪（保持行为不变）。
  - ✅ **`eva-client` 退场**：已从 reactor 移除并从仓库删除；跨 BC 通用对象已开始沉淀 `shared-kernel`（见 10.5）。

- 🎯 下一刀建议（保持行为不变；每次只改 1 个文件闭环）：
  1) ✅ 已完成（IAM，单资源 XML，保持行为不变）：将 `eva-infra-dal/src/main/resources/mapper/user/SysRoleMapper.xml` 搬运归位到 `bc-iam/infrastructure/src/main/resources/mapper/user/SysRoleMapper.xml`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；最小回归闭环；落地：`aa1d7c6b`）。
  2) 并行（IAM，依赖方去直连 role 表，保持行为不变）：用 Serena 找出仍直连 `SysUserRoleMapper/SysRoleMapper` 的非 IAM BC 类（`bc-*`，排除 `bc-iam/**`），每次只改 1 个类收敛为依赖 `bc-iam-contract` 端口 `UserEntityObjectByIdDirectQueryPort`（异常文案/副作用顺序完全不变；不引入新缓存/切面副作用）。
		  - ✅ **已闭环（本阶段目标 B：基础设施旧 gateway 归位，保持行为不变）**：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl` 下残留旧 `*GatewayImpl.java` 已清零（Serena 盘点口径）；归位闭环包含：
		    - IAM：`RoleUpdateGatewayImpl/UserQueryGatewayImpl/UserUpdateGatewayImpl` → `bc-iam/infrastructure`
		    - Evaluation：`EvaConfigGatewayImpl/EvaDeleteGatewayImpl/EvaUpdateGatewayImpl` → `bc-evaluation/infrastructure`
		    - 支撑类（编译闭合前置）：`StaticConfigProperties` → `eva-infra-shared`（保持 `package` 不变；用于闭合 `EvaConfigGatewayImpl` 归位）
			  - ⏳ **仍未完成（核心阻塞项）**：
					    1) `eva-app` 已完成退场闭环（源码清零 + 组合根去依赖 + reactor 移除 + 删除 `eva-app/pom.xml`；详见 0.9），不再是当前阻塞项；后续“整合 eva-*”的核心阻塞集中在 `eva-adapter`（入口层）与 `eva-domain/eva-infra*`（跨 BC 共享/过渡模块）的依赖边界收敛上（保持行为不变）。
			    2) ✅ `eva-adapter` 残留 `*Controller.java` 已收敛到 **0 个**（口径与清单见 0.10.2；保持行为不变）。
			    2.1) ✅ `eva-adapter/pom.xml` 已删除（保持行为不变；该 pom 的“依赖收敛”可视为历史闭环，不再新增/调整；口径见 0.10.1）。
			    2.2) ⏳ **`eva-*` 技术切片仍未具备“全量整合进各 BC”的前置条件**：root reactor 仍包含 `eva-infra-dal` / `eva-infra-shared` / `eva-base`（可复现口径：`rg -n "<module>eva-" pom.xml`）。保持行为不变前提下，当前仍不能直接移除这些模块。补充“可量化快照”（口径=可复现命令）：`eva-domain` 0 个（`fd -t f -e java . eva-domain/src/main/java | wc -l`）、`eva-infra-dal` 34 个、`eva-infra-shared` 44 个、`eva-base-common` 2 个（`eva-base-config` 当前无 Java 源码）。补充：`eva-infra` 已从 root reactor 退场（落地：`0aab4516`），目录下仅剩 3 个 `package-info.java`，可后置清理（独立提交）。粗略工作量估算：若坚持“root reactor 不含任何 `eva-*` module（含 `eva-domain/eva-infra-dal/eva-infra-shared/eva-base`）”，仅按当前 `eva-*` 下的 Java 文件计，至少约 80 次“单文件闭环”；若按 `DDD_REFACTOR_PLAN.md` 10.5 推荐先把共享技术模块（`eva-infra-dal/eva-infra-shared`）视为平台模块保留，则可优先聚焦“逐 BC 清空对共享技术模块的编译期边界”，阶段性收益更高且风险更低（保持行为不变）。
			    2.3) ✅ **`eva-domain` 已完成收尾清理（保持行为不变）**：根 `pom.xml` 已移除 `<module>eva-domain</module>`（落地：`6b907bc1`），且全仓库 `**/pom.xml` 不再出现 `<artifactId>eva-domain</artifactId>`（可复现口径：`rg -n "<artifactId>eva-domain</artifactId>" --glob "**/pom.xml" .`）。补充：已删除 `eva-domain/pom.xml`（落地：`c0035b03`）。下一刀建议（保持行为不变）：若 `eva-domain/` 目录仍存在，则评估是否删除空目录（建议独立提交）。
		       - 补充说明（保持行为不变）：当前组合根 `start` 已显式依赖各 `bc-*-infra(runtime)`（装配责任由组合根兜底；保持行为不变，仅上推依赖边界），且已移除 `eva-infra(runtime)` 过渡期兜底依赖（落地：`1e2ffa89`）。`eva-adapter` 依赖已移除（落地：`92a70a9e`）。若下一步要继续收敛“依赖方 `pom.xml` 的编译期依赖面”，仍按 **S0.2 延伸（每次只改 1 个 pom）** 推进：先 Serena + `rg` 证伪“仅类型引用”，再替换依赖（保持行为不变）。
		    3) ✅ **S0.2（收敛依赖，主目标已闭环）**：`eva-domain` 已移除对 `bc-course`（应用层 jar）的 Maven 依赖；且 `bc-ai-report-infra` 已不再需要显式依赖 `bc-course`（`IUserCourseService` 已沉 `shared-kernel`），均已在最小回归下验证闭合（细节见 0.9）。
		    4) ✅ **S0.2（延伸：继续收敛 `bc-course` 的“协议承载面”）**：已将 `edu.cuit.client.api.course` 下残留接口（`ICourseDetailService/ICourseService/ICourseTypeService`）以及其签名依赖的跨 BC 类型（`CourseScoreCO/EvaTeacherInfoCO`、`SingleCourseDetailCO`、`SimpleCourseResultCO` 等）逐步下沉到 `shared-kernel`（均保持 `package` 不变；细节以 0.9 为准）。
	    5) ⏳ **S0.2（延伸：收敛依赖方对 `bc-course` 的编译期依赖）**：在课程域 API/CO 已进入 `shared-kernel` 的前提下，下一会话需要逐个模块收敛 Maven 依赖：凡“仅使用 `edu.cuit.client.*` 类型”且不依赖 `bc-course` 内部实现的模块，优先把对 `bc-course` 的编译期依赖替换为显式依赖 `shared-kernel`（每步只改 1 个 `pom.xml`，保持行为不变）。
	    6) ✅ **S0.2 延伸（并行支线：课程域基础设施归位，保持行为不变）**：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 已按“仅搬运 + 编译闭合”推进归位；当前残留已清零（以 0.9 为准）。

- 下一会话优先建议（聚焦 **S0.2 延伸**，保持行为不变；每步只改 1 个小点，便于回滚）：
  0) ✅ **已完成：收敛 `eva-adapter` 残留 `*Controller`**（已清零；见 0.10.2；保持行为不变）。
  1) 🎯 **依赖方 `pom.xml` 收敛（单 pom，主线推荐）**：优先从 `bc-*/infrastructure/pom.xml` 入手，针对 `eva-infra-shared` / `eva-infra-dal` 做“Serena 证伪无引用面 → 再移除/降 scope”的收敛（避免误删：必须 Serena + `rg` 双证据，且跑最小回归）。
     - 快速入口（可复现口径）：`rg -n \"<artifactId>eva-infra-(shared|dal)</artifactId>\" --glob \"**/pom.xml\" . | sort`
  2) （可选，次主线）**继续瘦身 `eva-infra-shared` 的业务相关类**：仅当 Serena 证据化确认“引用面集中于单 BC”，再按“每次只搬 1 个类”把对应类归位到目标 `bc-*/infrastructure`（保持 `package`/类内容不变；确保全仓库同 FQCN 仅一份；每步闭环）。

### 0.10.2 `eva-adapter` 残留 Controller 清单（口径=可复现命令；更新至 2026-01-27）

> 口径：`fd -t f 'Controller\\.java$' eva-adapter/src/main/java | sort`（与 `| wc -l` 同步）。

**当前残留共 0 个**（✅ 已清零；`eva-adapter/src/main/java` 下无 `*Controller.java`）：

**下一刀建议（保持行为不变；每次只改 1 个 `pom.xml`）**：
1) ✅ **单 pom**：从根 `pom.xml` 的 reactor 中移除 `<module>eva-adapter</module>`（前置：Serena 证伪无代码级引用点/装配漂移风险；最小回归闭环；落地提交：`86842a1f`）。
2) （后置，可选）在 reactor 已移除且 `eva-adapter/pom.xml` 已删除后，再评估是否需要删除 `eva-adapter/` 目录（建议独立提交，避免与其它变更混在同一提交里）。

  1) ✅ **已闭环：课程域基础设施归位（RepositoryImpl 残留清零，保持行为不变）**：
     - 结果：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/*RepositoryImpl` 残留已清零（以 0.9 为准）。
     - 标准闭环：Serena 盘点依赖/引用 →（可选）IDEA MCP `get_file_problems(errorsOnly=true)` 预检 → `git mv` 搬运（必要时仅做“编译闭合”的 support 类归位/依赖补齐）→ 最小回归 → `git commit` → 三文档同步 → `git push` → Serena 证伪残留数变化。
  2) ✅ **课程 Controller 注入接口化（先解耦再搬运，保持行为不变）**：已对课程相关 Controller 子簇完成“注入类型从 `*ServiceImpl` 收窄为 `shared-kernel` 的 `edu.cuit.client.api.course.*Service` 接口”的改造，避免 Controller 编译期绑定实现类（落地：`47a6b06c`）。
  3) **继续收敛 `eva-app` 的课程域实现承载面（关键前置，保持行为不变）**：在 Controller 已不再注入实现类的前提下，下一步优先把课程旧入口实现类从 `eva-app` 归位到 `bc-course-infra`（保持 `package edu.cuit.app.service.impl.course` 不变；只做搬运/编译闭合，不改任何业务逻辑/异常文案/副作用顺序）。建议顺序：`ICourseServiceImpl` → `IUserCourseServiceImpl` → `ICourseDetailServiceImpl`（体量最大、依赖最多）。
     - 补充进展：`ICourseServiceImpl`（落地：`2b5bcecb`）、`IUserCourseServiceImpl`（落地：`79a351c3`）与 `ICourseDetailServiceImpl`（落地：`bd85a006`）均已完成归位到 `bc-course-infra`；至此课程旧入口三件套已从 `eva-app` 清零（保持行为不变）。
  4) ✅ **回到“依赖方收敛（每次只改 1 个 pom）”**：已用 Serena + `rg` 证伪：`eva-app` 不再引用 `edu.cuit.bc.course.*`，因此已完成将 `eva-app/pom.xml` 的 `bc-course` 编译期依赖替换为 `shared-kernel`（落地：`6fe8ffc8`；保持行为不变；最小回归通过）。下一步可继续按同套路评估其它模块的 `pom.xml`（每次只改 1 个 `pom.xml`）。
  5) **路线 B（后置，成本更高）**：若后续发现 `shared-kernel` 承载课程域接口/CO 规模继续膨胀，可再引入 `bc-course-contract`（或更中立的 contract 模块）承载这些接口/CO（保持 `package` 不变），并逐步把依赖方从 `shared-kernel` 切到该 contract（每步闭环；保持行为不变）。

	- Q：什么时候可以把 `eva-*` 技术切片“全部整合进各业务 bc-* 模块”？
	  - A：不要用固定日期衡量，按 **可验证的 DoD**（见 `DDD_REFACTOR_PLAN.md` 10.5）：
	    1) **可以开始整合（进入 S1）**：入口方法簇按 BC 迁移已持续推进且可证据化回滚；核心 BC 的 S0 结构折叠已完成或接近完成；组合根装配责任清晰（避免同一提交“迁入口 + 迁装配”）。
	    2) **可以移除 `eva-app/eva-adapter`**：`eva-app` 不再包含任何业务入口实现（只剩委托壳/装配胶水），`eva-adapter` 的 Controller 只做协议适配与参数校验且不直耦 `eva-infra` 实现。
	    3) **可以移除 `eva-infra`（或大幅缩减）**：旧 `*GatewayImpl` 已全部退化为委托壳或迁入各 BC 的 `infrastructure`（或过渡落点）；跨 BC 共享的 `eva-infra-dal/eva-infra-shared` 可保留为共享技术模块（不建议硬塞进单一 BC）。
	    4) **现状（更新至 2026-02-06，保持行为不变）**：root reactor 仍包含 `eva-infra-dal/eva-infra-shared/eva-base`（口径：`rg -n "<module>eva-" pom.xml`），且多个 `bc-*` 仍编译期依赖 `eva-infra-shared`（口径：`rg -n "<artifactId>eva-infra-shared</artifactId>" --glob "**/pom.xml" .`）。补充：`eva-domain` 已从 root reactor 退场且其编译期依赖方已清零（口径：`rg -n "<artifactId>eva-domain</artifactId>" --glob "**/pom.xml" .` 预期无命中）。因此短期目标应先聚焦：继续按 S0.2 延伸做“逐类归位（仅单 BC 引用的支撑类）+ 单 pom 依赖收敛”，把依赖面逐步压缩到“仅技术共享”后再评估“彻底整合进各 BC / 或平台化改名”等后置动作（保持行为不变）。
	
	- 回归环境提示（避免误判）：本机默认 `JAVA_HOME` 为 JDK 25 时可能触发编译器内部错误；建议在最小回归时显式使用 Java 17（仓库基线）并使用仓库内本地 Maven 仓库目录，示例命令见 0.9（保持行为不变）。

	- 本次会话最新闭环（更新至当前 `HEAD`，便于续接）：  
  1) ✅ bc-course（课程）写侧入口用例归位继续（方向 A → B）：`ICourseDetailServiceImpl.updateCourses/delete/addCourse` 已完成入口用例归位/调用点端口化（保持 `@CheckSemId`/事务边界/异常文案/副作用顺序完全不变；细节见 0.9）。
  2) ✅ bc-course（课程，S0 收尾：依赖收窄）：已清理旧入口 `ICourseServiceImpl` / `IUserCourseServiceImpl` 中可证实“仅声明无调用点”的残留注入依赖，并将 `isImported` 的依赖从 `CourseUpdateGateway` 收敛为直接依赖 `IsCourseImportedUseCase`（保持行为不变；细节见 0.9）。
  3) ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：已完成 `CourseUpdateGatewayImpl.updateCourseType/addCourseType` 压扁样例（旧 gateway 仅保留事务边界与委托调用；保持行为不变；细节见 0.9）。
  4) ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：已完成 `CourseUpdateGatewayImpl.updateCoursesType` 压扁（唯一调用点为 `ICourseTypeServiceImpl.updateCoursesType`；旧 gateway 仅保留事务边界与委托调用；保持行为不变；细节见 0.9）。
  5) ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：已进一步压扁 `CourseDeleteGatewayImpl.deleteCourseType`（旧 gateway 仅保留事务边界与委托调用；保持行为不变；细节见 0.9）。
  6) ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：已进一步压扁 `CourseDeleteGatewayImpl.deleteCourse`（Serena：调用点为 `DeleteCoursePortImpl.deleteCourse`；旧 gateway 仅保留事务边界与委托调用；保持行为不变；细节见 0.9）。
  7) ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：已进一步压扁 `CourseDeleteGatewayImpl.deleteCourses`（Serena：调用点为 `DeleteCoursesPortImpl.deleteCourses`；旧 gateway 仅保留事务边界与委托调用；保持行为不变；细节见 0.9）。
  8) ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：已进一步压扁 `CourseDeleteGatewayImpl.deleteSelfCourse`（Serena：调用点为 `DeleteSelfCoursePortImpl.deleteSelfCourse`；旧 gateway 仅保留事务边界与委托调用；保持行为不变；细节见 0.9）。
  9) ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：已压扁 `CourseUpdateGatewayImpl.assignTeacher`（Serena：调用点为 `AllocateTeacherPortImpl.allocateTeacher`；旧 gateway 不再构造命令；保持事务边界/异常文案/副作用顺序完全不变；细节见 0.9）。
  10) ✅ bc-course（课程，S0：旧 gateway 压扁为委托壳）：已完成压扁 `CourseUpdateGatewayImpl.addExistCoursesDetails`（Serena：调用点为 `AddExistCoursesDetailsPortImpl.addExistCoursesDetails`；旧 gateway 不再构造 Command，仅保留事务边界 + 委托调用；保持行为不变；落地：`de34a308`）。
  11) ✅ S0.2（依赖面收敛，保持行为不变）：已将 `SemesterCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`77126c4a`）。
  12) ✅ S0.2（依赖面收敛，保持行为不变）：已将 `Term` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`23bff82f`）。
  13) 下一步建议（S0.2，保持行为不变；每次只迁 1 个小包/小类簇）：继续把仍落在 `bc-course/application` 的 `edu.cuit.client.*` 协议对象迁移到 `shared-kernel`，建议顺序：`CourseQuery` → `CourseConditionalQuery` → `MobileCourseQuery` → `CourseExcelBO` →（视引用面）逐步处理 `dto/cmd/course/*` 与 `dto/clientobject/course/*`；最后在 Serena 证伪 `eva-domain` 不再需要 `bc-course` 提供这些类型后，独立提交移除 `eva-domain/pom.xml` 对 `bc-course` 的依赖（保持行为不变）。
  14) 避坑（保持行为不变）：不要选 `CourseUpdateGatewayImpl.addCourse` 作为“压扁样例”（当前为 TODO 空实现 `return null`，不适合作为行为对照链路）。

- 历史闭环（2025-12-30，便于续接；更早细节仍保留如下）：  
  1) ✅ 统计读侧 `pageUnqualifiedUser`：分页结果组装已归位到 `EvaStatisticsQueryUseCase`，旧入口 `EvaStatisticsServiceImpl` 已退化为纯委托壳并移除对 `PaginationBizConvertor` 的依赖（`e97615e1` / `f4f3fcde`）。  
  2) ✅ 统计读侧 `type` 分支判断去重复：在 `EvaStatisticsQueryUseCase` 内部收敛 `type` 分发为 `dispatchByType(...)`，避免后续继续归位时出现分支口径漂移（保持行为不变；`38ce9ece`）。
  3) ✅ 统计读侧导出 `exportEvaStatistics`：引入导出端口 `EvaStatisticsExportPort` 并让旧入口委托 `EvaStatisticsQueryUseCase.exportEvaStatistics`（保持 `@CheckSemId` 触发点不变；行为不变；`0d15de60`）。
  4) ✅ 导出基础设施归位准备：`ExcelUtils` 已从 `eva-app` 迁移到 `eva-infra-shared`（包名不变），并补齐 `poi/poi-ooxml` 依赖，避免后续导出类迁移出现循环依赖（保持行为不变；`04009c85`）。
  5) ✅ 导出基础设施归位（统计导出基类）：`EvaStatisticsExporter` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（包名不变），并补齐 `bc-evaluation-infra` 对 `bc-course/bc-iam-contract` 的编译依赖以闭合类型引用（保持行为不变；`e8ca391c`）。
  6) ✅ 导出基础设施归位（统计导出装饰器）：`FillAverageScoreExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（包名不变；`4e150984`）。
  7) ✅ 导出基础设施归位（统计导出装饰器）：`FillEvaRecordExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（包名不变；`b3afcb11`）。
  8) ✅ bc-messaging：已完成“后置规划证据化”（仅文档，不落地代码），散落点与路线见 `DDD_REFACTOR_PLAN.md` 第 10.3 节（`4b05f515`）。
  9) ✅ 导出基础设施归位（统计导出装饰器）：`FillUserStatisticsExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（包名不变；`e83600f6`）。
  10) ✅ 导出基础设施归位（统计导出工厂）：`EvaStatisticsExcelFactory` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（异常文案/日志输出完全一致；`5b2c2223`）。
  11) ✅ 装配切换（统计导出端口）：`BcEvaluationConfiguration.evaStatisticsExportPort()` 已切换为委托 `bc-evaluation-infra` 的 `EvaStatisticsExportPortImpl`（内部仍调用 `EvaStatisticsExcelFactory.createExcelData`；保持行为不变；`565552fa`）。
  12) ✅ bc-course（课程）：课表 Excel/POI 解析归位到 `bc-course-infra`（保持 `package` 不变；异常文案/日志输出完全不变；`383dbf33`）。
  13) ✅ bc-course（课程）：课表解析端口化与依赖收敛：`eva-app` 调用侧改为依赖 `CourseExcelResolvePort`（由 `bc-course-infra` 适配器实现）；`eva-app` 不再编译期依赖 `bc-course-infra`（改为 runtime）；`start` 测试侧补齐 `bc-course-infra` test 依赖（保持行为不变；`5a7cd0a0`）。
  14) ✅ bc-messaging（消息域）：组合根归位：`BcMessagingConfiguration` 已从 `eva-app` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.config` 不变；仅补齐 `bc-messaging` 对 `spring-context` 的依赖；保持行为不变；`4e3e2cf2`）。
  15) ✅ bc-messaging（消息域）：监听器归位（课程副作用）：`CourseOperationSideEffectsListener` 已从 `eva-app` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.bcmessaging` 不变；保持行为不变；`22ee30e7`）。
  16) ✅ bc-messaging（消息域）：监听器归位（课程教师任务消息）：`CourseTeacherTaskMessagesListener` 已从 `eva-app` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.bcmessaging` 不变；保持行为不变；`0987f96f`）。
  17) ✅ bc-messaging（消息域）：支撑类归位（消息发送组装）：`MsgResult` 已从 `eva-app` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.app.service.operate.course` 不变；保持行为不变；用于打破后续端口适配器迁移的 Maven 循环依赖；`31878b61`）。
  18) ✅ bc-messaging（消息域）：应用侧端口适配器归位（课程广播）：`CourseBroadcastPortAdapter` 已从 `eva-app` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.bcmessaging.adapter` 不变；保持行为不变；`84ee070a`）。
  19) ✅ bc-messaging（消息域）：应用侧端口适配器归位（教师任务消息）：`TeacherTaskMessagePortAdapter` 已从 `eva-app` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.bcmessaging.adapter` 不变；保持行为不变；`9ea14cff`）。
  20) ✅ bc-messaging（消息域）：应用侧端口适配器归位（评教消息清理）：`EvaMessageCleanupPortAdapter` 已从 `eva-app` 迁移到 `bc-messaging`，并将依赖类型从 `MsgServiceImpl` 收窄为 `IMsgService`（保持 `deleteEvaMsg(taskId, null)` 调用不变；事务语义由原 `MsgServiceImpl` 承接；保持行为不变；`73ab3f3c`）。

- 下一步建议（仍保持行为不变；每次只改 1 个类 + 1 个可运行回归）：  
  1) ✅ **评教统计导出基础设施归位**：本阶段已闭环（装饰器/工厂归位 + 导出端口装配切换均完成；且 `eva-app` 已移除 `poi/poi-ooxml` Maven 直依赖）。后续若要继续推进评教读侧解耦，请回到“统计用例归位（空对象兜底/默认值组装）每次迁 1 个方法簇”的节奏（仍保持行为不变）。
2) ✅ **bc-messaging（按 10.3 路线）**：该路线已闭环至“依赖收敛后半段证伪 + 运行时装配上推”（见 0.9）。后置如需继续，优先做结构折叠（S0，仅搬运/依赖收敛）或进一步收敛依赖面（每次只改 1 点；保持行为不变）。建议顺序（每步闭环=Serena→最小回归→commit→三文档同步）：
     - ✅ 已完成：组合根 `BcMessagingConfiguration` 归位（`4e3e2cf2`）
     - ✅ 已完成：监听器 `CourseOperationSideEffectsListener` 归位（`22ee30e7`）
     - ✅ 已完成：监听器 `CourseTeacherTaskMessagesListener` 归位（`0987f96f`）
     - ✅ 已完成：支撑类 `MsgResult` 归位（`31878b61`，当前位于 `bc-messaging-contract`）
     - ✅ 已完成：应用侧端口适配器 `CourseBroadcastPortAdapter` 归位（`84ee070a`）
     - ✅ 已完成：应用侧端口适配器 `EvaMessageCleanupPortAdapter` 归位（`73ab3f3c`）
     - ✅ 已完成：应用侧端口适配器 `TeacherTaskMessagePortAdapter` 归位（`9ea14cff`）
     - ✅ 已完成（前置，DAL/Convertor 归位）：`MsgTipDO/MsgTipMapper(+xml)` → `eva-infra-dal`；`MsgConvertor` → `eva-infra-shared`（保持 `package/namespace` 不变；保持行为不变；后续已进一步将 `MsgConvertor` 归位到 `bc-messaging`：`312756c7`，将 `MsgTipMapper` 归位到 `bc-messaging`：`4af9f9fc`，将 `MsgTipDO` 归位到 `bc-messaging`：`87b38a55`；均仅改变 Maven 模块归属）。
     - ✅ 已完成（基础设施端口适配器归位）：`MessageDeletionPortImpl/MessageReadPortImpl/MessageDisplayPortImpl/MessageInsertionPortImpl/MessageQueryPortImpl` → `bc-messaging`（保持 `package` 不变；保持行为不变）。
     - ✅ 依赖收敛准备：将事件枚举 `CourseOperationMessageMode` 下沉到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；`b2247e7f`）。
	     - ✅ 依赖收敛后半段：已完成 `eva-infra` 对 `bc-messaging` 编译期引用证伪，且运行时装配由组合根 `start` 承接（见 0.9）。

  3) ✅ **bc-course（课程）写侧入口用例归位继续（方向 A → B）**：已完成 `ICourseServiceImpl.updateSingleCourse/addNotExistCoursesDetails/addExistCoursesDetails`，以及 `IUserCourseServiceImpl.deleteSelfCourse/updateSelfCourse/importCourse` 与 `ICourseDetailServiceImpl.updateCourse/updateCourses/delete/addCourse` 的写侧入口收敛（见 0.9）。下一步建议：进入 **S0 收尾**，先清理旧入口残留的未使用依赖注入（保持行为不变；每次只改 1 个类；参考上条）。
  4) ✅ 🎯 **S0.2 延伸（课程旧 gateway 归位，保持行为不变）**：已将课程旧 gateway `CourseQueryGatewayImpl/CourseUpdateGatewayImpl` 从 `eva-infra` 归位到 `bc-course/infrastructure`（保持 `package` 不变；仅搬运文件/编译闭合；不改任何业务语义；落地：`d438e060`）。
     - ✅ 已闭环：`edu.cuit.infra.bccourse.query.CourseQueryRepository` 已归位到 `bc-course/infrastructure`（保持 `package` 不变；落地：`881e1d12`）。
     - 下一簇建议（保持行为不变）：如需继续收敛课程读侧实现承载面，优先用 Serena 盘点 `CourseQueryRepository` 的“跨域依赖闭包”（评教任务/表单/用户等 Mapper 与查询组装），作为后续“按查询主题拆分 QueryRepo/QueryService”与“进一步收敛共享 DAL”的输入（仍不改业务语义）。

- ✅ 提交点 0（纯文档闭环）：已完成（落地提交：`1adc80bd`）。
- ✅ 提交点 A（结构落点，不迁业务）：已完成（落地提交：`a30a1ff9`）。
- ✅ 提交点 B（写侧收敛，挑 1 条链路）：已完成（审计日志写入 `LogGatewayImpl.insertLog`；落地提交：`b0b72263`）。
- ✅ 提交点 C（读侧继续拆，统计主题）：已完成第一步（`EvaStatisticsQueryRepo` 抽取 + 端口依赖收敛；落地提交：`d5b07247`）。
- ✅ 提交点 C2（读侧继续拆，记录主题）：已完成（`EvaRecordQueryRepo` 抽取 + 端口依赖收敛；落地提交：`cae1a15c`）。
- ✅ 提交点 C3（读侧继续拆，任务主题）：已完成（`EvaTaskQueryRepo` 抽取 + 端口依赖收敛；落地提交：`82427967`）。
- ✅ 提交点 C4（读侧继续拆，模板主题）：已完成（`EvaTemplateQueryRepo` 抽取 + 端口依赖收敛；落地提交：`889ec9b0`）。
- ✅ 提交点 B2（AI 报告写链路，导出）：已完成（`AiCourseAnalysisService.exportDocData` 收敛；落地提交：`c68b3174`）。
- ✅ 提交点 B3（可选，AI 报告写链路继续瘦身委托壳）：已完成（`AiCourseAnalysisService.exportDocData` 进一步退化为纯委托壳；落地提交：`7f4b3358`）。
- ✅ 提交点 B4（AI 报告写链路，analysis）：已完成（`AiCourseAnalysisService.analysis` 收敛为“用例+端口+适配器+委托壳”；落地提交：`a8150e7f`）。
- ✅ 提交点 B5（AI 报告写链路，用户名解析）：已完成（username → userId 查询抽为 `bc-ai-report` 端口 + `eva-app` 端口适配器；落地提交：`d7df8657`）。
- ✅ 提交点 C5-1（读侧实现继续拆，统计主题）：已完成（抽出 `EvaStatisticsQueryRepository`；落地提交：`9e0a8d28`；三文档同步：`61b0dfa4`）。
- ✅ 提交点 C5-2（读侧实现继续拆，记录主题）：已完成（抽出 `EvaRecordQueryRepository`；落地提交：`985f7802`；三文档同步：`68895003`）。
- ✅ 提交点 C5-3（读侧实现继续拆，任务主题）：已完成（抽出 `EvaTaskQueryRepository`；落地提交：`d467c65e`；三文档同步：`ebff7002`）。
- ✅ 提交点 C5-4（读侧实现继续拆，模板主题）：已完成（抽出 `EvaTemplateQueryRepository`；落地提交：`a550675a`）。
- ✅ 评教 BC 自包含三层结构试点（阶段 1：引入 `bc-evaluation-infra`）：已完成（评教读侧查询迁移 + course/eva DAL 迁移 + shared 迁移；落地提交：`be6dc05c`）。
- ✅ 评教 BC 自包含三层结构试点（阶段 2：写侧 Repo 迁移）：已完成（迁移 `eva-infra/src/main/java/edu/cuit/infra/bcevaluation/repository/*` 到 `bc-evaluation-infra`；并迁移 `CalculateClassTime` 到 `eva-infra-shared`，保持 `bc-evaluation-infra` 不依赖 `eva-infra`；落地提交：`24e7f6c9`）。
- ✅ 提交点 C-1（读侧门面加固，可选）：已完成（清理 `EvaQueryRepository` 为纯委托壳；落地提交：`73fc6c14`；三文档同步：`083b5807`）。

下一会话建议（按顺序执行；历史已完成项见下方 0.12 “总体进度概览”）：
0) **bc-messaging（依赖收敛后半段，优先，保持行为不变）**：当前已完成“事件枚举/载荷下沉到 `bc-messaging-contract` + `eva-app` 依赖面收敛为 `bc-messaging-contract`”（见 0.9）。下一步建议以“组合根兜底运行时依赖”为目标继续收敛依赖边界（每步闭环）：
   - 目标：让消息域的运行时装配由组合根承接（建议 `start`），并尽量减少 `eva-app/eva-infra` 对 `bc-messaging` 的编译期耦合（仍保持行为不变）。
   - 建议拆分为 3 个可回滚提交（每步：Serena → 最小回归 → commit → 三文档同步）：
     1) ✅ 已完成：组合根补齐运行时依赖：在 `start/pom.xml` 增加对 `bc-messaging` 的 `runtime` 依赖，确保 Spring 装配不变（保持行为不变；落地提交：`f23254ec`）。
     2) ✅ 已完成：依赖上推：移除 `eva-infra/pom.xml` 对 `bc-messaging` 的 `runtime` 依赖，把“运行时装配责任”从 `eva-infra` 上推到组合根（保持行为不变；落地提交：`507f95b2`）。
     3) 注意：`bc-messaging-contract` 当前包含 `MsgResult`（`@Component`，保持行为不变），因此其对 `spring-context` 的依赖暂不建议移除；若后续要移除，必须先将 `MsgResult` 的装配责任迁回 `bc-messaging` 或其它合适模块并闭环验证。

1) **评教读侧进一步解耦（优先，方向 A → B，保持行为不变）**：当前已完成“统计 QueryPort 细分 + 依赖收窄 + UseCase 归位起步 + 首个分支归位”（见 0.9：统计 `a1d6ccab/c19d8801/9b3c4e6a/db09d87b/22dccc70`；其余记录/任务/模板/导出链路进展见 0.9 的提交清单）。下一步建议拆分为更小的可回滚提交：
   - **A（继续收窄依赖）**：按同套路继续推进读侧其它主题（优先记录/任务/模板）：先做“子端口接口 + `extends`（不改实现/不改装配）”，再逐个把 `eva-app` 中的注入类型收窄为子端口（每次只改 1 个类 + 对应测试）。
	     - 进展（统计导出链路，2025-12-28）：已补齐 `EvaStatisticsCountAbEvaQueryPort`，并将导出基类 `EvaStatisticsExporter` 静态初始化中获取统计端口的依赖类型收窄到该子端口（保持 `SpringUtil.getBean(...)` 次数与顺序不变；见 0.9：`24b13138/7337d378`，保持行为不变）。
	     - 进展（记录主题，2025-12-28）：已完成记录 QueryPort 细分（5 子端口）+ 聚合端口 `extends`，并已收窄 `EvaRecordServiceImpl` 与 `UserServiceImpl` 的依赖类型（见 0.9：`39a4bafe/8b24d2f8` 等，保持行为不变）。
		     - ✅ 补充进展（记录主题，2025-12-28）：已证伪“全仓库仍存在对记录聚合端口 `EvaRecordQueryPort` 的注入点/调用点”（除端口定义与 `EvaRecordQueryPortImpl` 实现外无其它引用；Serena 证据见会话内盘点结论）。因此记录主题的“端口细分 + 应用层依赖类型收窄”阶段可视为已闭合；后续若出现新引用点，再按同套路逐一收窄（每次只改 1 个类 + 1 个可运行单测）。
	     - 进展（任务主题，2025-12-28）：已新增任务读侧子端口 `EvaTaskInfoQueryPort/EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskCountQueryPort`，并让 `EvaTaskQueryPort` `extends` 这些子端口；已完成依赖类型收窄：`MsgServiceImpl` → `EvaTaskInfoQueryPort`、`EvaTaskServiceImpl` → `EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskInfoQueryPort`（见 0.9，保持行为不变）。
	     - 进展（模板主题，2025-12-28）：已新增模板读侧子端口 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort` 并让 `EvaTemplateQueryPort` `extends`；已完成依赖类型收窄：`EvaTemplateServiceImpl` → `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`；并已用 Serena 证伪 `eva-app` 仍存在其它对 `EvaTemplateQueryPort` 的注入点/调用点（见 0.9：`a14d3c53/b86db7e4/e67fc47d`，保持行为不变）。
		     - 下一步建议（任务/模板主题，保持行为不变）：（可选）为 `EvaTaskServiceImpl` 补齐可重复的用例级回归（重点：异常文案/日志拼接保持不变），但涉及 `StpUtil` 静态登录态时需先固化“可重复”的登录态注入/隔离策略；模板主题在“端口细分 + 服务层依赖类型收窄 + 引用面证伪”阶段已闭合，后续若出现新的应用层引用点再按同套路逐一收窄即可（每次只改 1 个类 + 1 个可运行单测）。
		   - **B（用例归位深化）**：将 `EvaStatisticsQueryUseCase` 从“委托壳”逐步演进为“统计读侧用例编排落点”：建议每次只迁 1 个方法簇。✅ 已完成：`getEvaData` 的阈值计算/参数组装归位（落地：`8f4c07c5`）；unqualifiedUser 链路的“参数组装重载”（读取 `EvaConfigGateway.getEvaConfig()`，落地：`0a2fec4d`），且旧入口 `EvaStatisticsServiceImpl` 已委托重载并去除对 `EvaConfigGateway` 的直接依赖（落地：`21f6ad5b`）。✅ 已完成：`evaScoreStatisticsInfo` 的空对象兜底归位到 UseCase（补齐 `evaScoreStatisticsInfoOrEmpty`：`bce01df2`；旧入口委托该重载：`1bf3a4fe`）。✅ 已完成：`evaTemplateSituation` 的空对象兜底归位到 UseCase（补齐 `evaTemplateSituationOrEmpty`：`89b6b1ee`；旧入口委托该重载：`78abf1a1`）。✅ 已完成：`evaWeekAdd` 的空对象兜底归位到 UseCase（补齐 `evaWeekAddOrEmpty`：`5a8ac076`；旧入口委托该重载：`2a92ca0b`）。下一步建议（仍保持行为不变）：继续挑选统计读侧下一簇“默认值兜底/空对象组装”进行归位（仍按“每次只迁 1 个方法簇”的节奏）。
1) **条目 25（优先，写侧）**：AI 报告“剩余落库/记录写链路”已完成盘点并证伪（见 0.9 的证据清单）。后续请将条目 25 的执行重点切换为：**S0 折叠 `bc-ai-report`**（仅搬运/依赖收敛，保持行为不变）。
   - 补充进展（2025-12-27）：已将导出链路实现（`AiReportDocExportPortImpl` + `AiReportExporter`）、analysis 链路实现（`AiReportAnalysisPortImpl`）与 username→userId 端口适配器（`AiReportUserIdQueryPortImpl`）从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；提交：`d1262c32`、`6f34e894`、`e2a608e2`），并进一步将 `edu.cuit.infra.ai.*` 从 `eva-infra` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；提交：`e2f2a7ff`）。
   - 补充进展（2025-12-27）：已将 `BcAiReportConfiguration` 与旧入口 `AiCourseAnalysisService` 归位到 `bc-ai-report`，并将 `@CheckSemId` 注解下沉到 `shared-kernel`（均保持 `package`/切面触发点/异常与日志行为不变；提交：`58c2f055`、`ca321a20`、`1c595052`）。
   - 补充进展（2025-12-27）：S0 已完成阶段 1/2：`bc-ai-report-parent` + 内部子模块已落地；端口适配器/导出实现/AI 基础设施已归位 `bc-ai-report/infrastructure`，并补齐 `eva-app` → `bc-ai-report-infra` 依赖（保持行为不变；提交：`e14f4f7a`、`444c7aca`）。
   - 说明：由于本次盘点已证伪，本条目的“证据清单”已补齐；无需再“凭感觉继续拆”，直接推进 S0 更能带来结构性收益且可回滚。
2) **结构性里程碑 S0（次优先）**：`bc-audit` 已完成阶段 1/2/3（提交：`81594308`、`d7858d7a`、`06ec6f3d`）。下一步建议（可选，保持行为不变）：若仍发现 `bc-audit-infra` 从 `eva-infra` 引用类型（理论上应为 0），则继续按“先抽离到 `eva-infra-dal`/`eva-infra-shared` 或归位到 `bc-audit/infrastructure`”的小步策略处理；否则将本项视为闭环并转入评教读侧解耦。
   - 实现路径（已落地，供后续复用；每步：符号级盘点 → 最小回归 → commit → 三文档同步）：
     - 盘点 `bc-audit-infra` 仍从 `eva-infra` 引用的类型清单（聚焦 `edu.cuit.infra.dal.database.*`、`edu.cuit.infra.convertor.*`、`edu.cuit.infra.gateway.*`），形成“依赖闭包”证据。
     - 将 `sys_log` 相关 DO/Mapper/XML 按“保持包名/namespace/SQL 不变”的策略迁移到 `eva-infra-dal`。
     - 将 `LogInsertionPortImpl` 仍依赖的转换/工具类按最小闭包迁移到 `eva-infra-shared`（或直接归位 `bc-audit/infrastructure`，二选一；保持行为不变）。
     - 最后将 `bc-audit-infra` 的 Maven 依赖由 `eva-infra` 收敛为更小的 `eva-infra-dal` + `eva-infra-shared`（或完全不依赖 `eva-infra`），并确保 `eva-app` 装配不变。
   - 参考：`bc-template`/`bc-course`/`bc-ai-report` 已完成折叠归位（提交：`65091516`、`e90ad03b`、`e14f4f7a/444c7aca`）。
3) （可选/后置）**评教读侧进一步解耦**：在不改变统计口径/异常文案前提下，按用例维度继续细化 QueryService/QueryPort（保持行为不变）。
   - 最小切入点建议（仍保持行为不变，便于可回滚）：优先从 `bc-evaluation/application/port/EvaStatisticsQueryPort` 着手，先按“用例簇”拆出 2~3 个更细粒度的子 Port（**仅新增接口 + `extends`，不改实现/不改装配**），再按“每次只迁 1 个用例方法”的节奏逐步收窄上层依赖类型（避免一次性大改导致口径/异常漂移）。
   - 风险提示：`eva-app` 中存在通过 `SpringUtil.getBean(...)` 获取 QueryPort 的静态初始化路径（例如导出相关类）；如需调整依赖类型或移动类归属，务必保持 Bean 初始化时机与副作用顺序不变。

## 0.12 当前总体进度概览（2025-12-29，更新至 `HEAD`）

> 用于回答“现在总进度到哪了”，避免每次会话重新盘点。

- **bc-iam（系统管理/IAM）**：已完成大量写侧/读侧收敛；历史上通过平铺过渡模块 `bc-iam-infra` 完成适配器归属与去 `eva-infra` 依赖闭环（见历史提交点）。**按 2025-12-24 需求变更**：已将该过渡模块折叠归位到 `bc-iam/infrastructure` 子模块（落地提交：`0b5c5383`），并新增 `bc-iam-contract` 子模块承接 IAM 协议对象迁移（落地提交：`dc3727fa`）。
- **bc-evaluation（评教）**：写侧主链路（任务发布/删除/模板）已按“用例+端口+适配器+委托壳”收敛；历史上通过平铺过渡模块 `bc-evaluation-infra` 完成读侧迁移与写侧 Repo 迁移，并通过 `eva-infra-shared`/`eva-infra-dal` 解决跨 BC 共享（均保持包名/行为不变）。**按 2025-12-24 需求变更**：已将该过渡模块折叠归位到 `bc-evaluation/infrastructure` 子模块（落地提交：`4db04d1c`）。补充进展（2025-12-29）：统计读侧用例归位深化已完成 `evaScoreStatisticsInfo/evaTemplateSituation/evaWeekAdd` 三簇空对象兜底归位到 `EvaStatisticsQueryUseCase`，并让旧入口 `EvaStatisticsServiceImpl` 退化为委托壳（见 0.9，保持行为不变）。
- **bc-evaluation（评教，contract）**：已新增 `bc-evaluation-contract` 并迁移评教统计接口 `IEvaStatisticsService` + 未达标用户协议对象 `UnqualifiedUserInfoCO/UnqualifiedUserResultCO`（保持 `package edu.cuit.client.*` 不变，仅物理归属与依赖收敛；保持行为不变；落地提交：`978e3535`）；并继续迁移评教统计/表单相关 CO（`DateEvaNumCO/TimeEvaNumCO/MoreDateEvaNumCO/SimpleEvaPercentCO/SimplePercentCO/FormPropCO`，保持 `package` 不变；保持行为不变；`c2d8a8b1`），以及课程时间模型 `CourseTime`（沉淀到 `shared-kernel`，保持 `package` 不变；保持行为不变；`5f21b5ce`）；并已在“可证实不再需要”的前提下移除 `bc-evaluation-contract` → `eva-client` 直依赖（`cf2001ef`）。
- **bc-audit（审计日志）**：已完成 `LogGatewayImpl.insertLog` 写链路收敛（异步触发点保留在旧入口，落库与字段补齐在端口适配器）；并已将审计日志协议对象 `ILogService/OperateLogCO/LogModuleCO` 从 `eva-client` 迁移到 `bc-audit`（保持 `package` 不变；保持行为不变；`e1dbf2d4`）。
- **bc-audit（审计日志，S0 折叠归位）**：已完成阶段 1/2/3：引入 `bc-audit-parent` + 内部子模块；将 `LogInsertionPortImpl` 归位 `bc-audit/infrastructure`；并已抽离 `sys_log` 相关 DAL/Converter 以移除 `bc-audit-infra` → `eva-infra` 过渡依赖（保持行为不变；提交：`81594308`、`d7858d7a`、`06ec6f3d`）。
- **bc-ai-report（AI 报告）**：已完成模块骨架接入组合根；导出写链路、analysis 与用户名解析已逐步收敛为“用例+端口+端口适配器+旧入口委托壳”（保持行为不变）。补充进展：导出链路实现（`AiReportDocExportPortImpl` + `AiReportExporter`）与 analysis 端口适配器（`AiReportAnalysisPortImpl`）已从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；提交：`d1262c32`、`6f34e894`）；username→userId 端口适配器（`AiReportUserIdQueryPortImpl`）亦已归位（提交：`e2a608e2`）；并进一步将 `edu.cuit.infra.ai.*` 从 `eva-infra` 归位到 `bc-ai-report` 且移除 `bc-ai-report` → `eva-infra` 编译期依赖（提交：`e2f2a7ff`）。按 2025-12-24 需求变更：后续不再新增 `bc-ai-report-infra` 平铺模块，新增适配器归位到 `bc-ai-report/` 内部 `infrastructure` 子模块（或先落在 `eva-app`，再按里程碑折叠归位）。
- **bc-ai-report（AI 报告，S0 折叠归位）**：已完成阶段 1：引入 `bc-ai-report-parent` + 内部 `domain/application/infrastructure` 子模块；应用层 artifactId 仍为 `bc-ai-report`（保持行为不变；提交：`e14f4f7a`）。
- **bc-ai-report（AI 报告，S0 折叠归位，补充）**：已完成阶段 2：端口适配器/导出实现/AI 基础设施归位 `bc-ai-report/infrastructure` 子模块，并补齐 `eva-app` → `bc-ai-report-infra` 依赖以保证装配（保持行为不变；提交：`444c7aca`）。
- **bc-ai-report（AI 报告，补充）**：旧入口 `AiCourseAnalysisService` 与组合根 `BcAiReportConfiguration` 已归位到 `bc-ai-report`；为保持 `@CheckSemId` 切面语义不变，`edu.cuit.app.aop.CheckSemId` 已下沉到 `shared-kernel`（均保持包名/异常/日志/副作用顺序不变；提交：`58c2f055`、`ca321a20`、`1c595052`）。
- **bc-template（模板）**：已完成 S0 结构折叠为 `bc-template-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-template`；包名不变；保持行为不变；落地提交：`65091516`）。
- **bc-course（课程）**：已完成 S0 折叠为 `bc-course-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-course`；包名不变；保持行为不变；提交：`e90ad03b`）。读侧 `CourseQueryGatewayImpl` 已退化委托壳并抽出 QueryRepo/Repository（保持行为不变）。
- **bc-messaging（消息）**：当前仍以平铺模块为主（`bc-messaging/src`），尚未完成“一个 BC 一个聚合模块 + 内部三层子模块”的结构折叠；但协议对象迁移已存在 `bc-messaging-contract`，后续建议优先把 `bc-messaging` 也按 S0 结构折叠（仅搬运/依赖收敛，保持行为不变）。

**阶段性判断（用于回答“什么时候能收敛 eva-* / 什么时候算 DDD”）**
- **结构上的 DDD（可验收）**：所有 `bc-*` 都具备稳定的 `domain/application/infrastructure` 结构落点（模块或至少 package 自包含），旧入口逐步退化为委托壳，出站依赖全部通过 Port 统一表达；此时可认为“结构 DDD”基本成型。
- **eva-* 技术切片可显著退场的门槛**：当 Controller/Service/UseCase 入口基本都落在各 BC（或仅保留极薄的兼容入口），且 `eva-app/eva-infra` 不再承载业务编排，仅保留 `shared-kernel` + 必要的 `eva-infra-dal/eva-infra-shared`（或等价的 BC 内 infrastructure）时，才适合开始“批量下线/合并 eva-* 模块”。
- **语义上的 DDD（长期）**：聚合边界、领域事件、一致性规则、读写分离（投影/查询模型）等逐步落地，才能称为“语义 DDD 完整”；建议在结构边界稳定后再推进（风险更低、回滚更容易）。

### 条目 25（定义 / 边界 / 验收口径）

> 背景：历史上“条目 24/25/26”作为会话推进索引使用；其中 24/26 已落地，但 25 的定义容易被“提交点 A/B/C”新拆分吸收，导致不同会话对边界理解不一致。

- **定义**：**AI 报告 / 审计日志模块化试点**。目标是为后续写侧收敛提供稳定落点（BC 模块骨架 + 组合根 wiring），并按既有套路先收敛 1 条写链路（行为保持不变）。
- **包含范围（本条目对应提交点 A + B）**：
  - **A：结构落点**：新增 `bc-ai-report` / `bc-audit` Maven 子模块骨架（只建目录/依赖/包结构/最小配置），并接入组合根（仅 wiring，不迁业务语义）。
  - **B：写侧收敛（挑 1 条链路）**：从 AI 报告或审计日志中选 1 条写链路，按“UseCase + Port + Port Adapter + 旧 gateway 委托壳”收敛，确保旧入口对外接口/注解触发点不变。
- **不包含范围**：
  - 不包含 `EvaQueryRepo` 的读侧拆分（提交点 C）。
  - 不做任何业务语义调整；**缓存/日志/异常文案/副作用顺序完全不变**。
- **验收口径（必须同时满足）**：
  1) `mvnd -pl start -am test ...` 最小回归通过（命令见本节下方）；  
  2) 组合根 wiring 完整（Spring Bean 可被扫描/装配，且不引入 Maven 循环依赖）；  
  3) 提交点 B 所选链路：旧 gateway 退化为委托壳后，**缓存注解/日志/异常文案/副作用顺序完全不变**（以变更前行为快照对照）。
- 最小回归命令（每步结束必跑，Java17）：  
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

## 0.11 新对话开启提示词（直接复制粘贴）

### 推荐版（2026-02-20 收口续接：方案 B（严格）DoD 已收口，准备切换到下一条业务主线；保持行为不变）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：`/home/lystran/programming/java/web/eva-backend`  
先确认（必须，禁止误操作 reset/checkout）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git status -sb`（重要：本会话可能存在“已暂存新增文件 + 未暂存代码改动”的混合状态；不要误操作 reset/checkout）
3) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
4) 当前交接基线：以 `git rev-parse --short HEAD` 输出为准
5) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在提示词里固化 commitId）

开始前按顺序阅读（必须）：
1) `NEXT_SESSION_HANDOFF.md`（重点看 0.0、0.9、0.10、0.11）
2) `DDD_REFACTOR_PLAN.md`（重点看 10.2/10.5）
3) `docs/DDD_REFACTOR_BACKLOG.md`（重点看 4.2、4.3、6）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析
- 每个小步骤闭环：Serena → 最小回归 → `git commit`（代码）→ 同步三文档 → `git commit`（文档）→ `git push`
- 每次只改 1 个类（允许新增/搬运单个类）或只改 1 个资源 XML 或只改 1 个 `pom.xml`
- 工作区可能存在未跟踪 `.mvnd/`、`.ssh_known_hosts`，不要提交

证据口径（先跑快照，避免口径漂移）：
1) 方案 B（严格）DoD 复核：
   - `rg -n "<module>eva-" pom.xml`（预期=0）
   - `rg -n -P "<artifactId>eva-(?!backend)" --glob "**/pom.xml" .`（预期=0；注意：父 POM `artifactId` 为 `eva-backend`，因此用该口径排除误判）
   - `fd -t d '^eva-' . -d 2 | sort`（预期=无输出）
2) 工作区保护与 WIP：
   - `git status -sb`（预期仅出现未跟踪 `.mvnd/`、`.ssh_known_hosts`）
   - `git stash list`（当前应为 3 条；后续只能按“指定文件路径”逐刀 restore，禁止整包 pop）

本轮主线（从“DoD 已收口”切换到下一条可执行主线；每刀只改 1 个类/1 个资源 XML/1 个 `pom.xml`；保持行为不变）：
1) 从 `docs/DDD_REFACTOR_BACKLOG.md` 的 4.3 里选 1 个“仍未收敛”的目标（写侧优先、影响面清晰、可做到单类/单 XML/单 pom），用 Serena 做入口定位 + 引用面证伪后开刀。
2) 若团队强制要求“彻底不出现任何 `eva-*` 前缀坐标”，需先明确是否要改名父工程 `artifactId=eva-backend`：该动作会影响所有子模块 parent 引用，**无法**按“每次只改 1 个 `pom.xml`”拆刀，建议先确认不做（避免破坏既有刀法与可回滚性）。
3) 每刀结束必须闭环：
   - 跑最小回归（优先 `mvnd`；若遇 `~/.m2/mvnd/.../registry.bin AccessDenied`，先确保 `MVND_REGISTRY` 指向工作区 `.mvnd/`，仍失败则降级用 `mvn`；若 `mvnd` 启动阶段报 `java.lang.ExceptionInInitializerError` 也同样降级 `mvn`）
   - `git commit`（代码）→ 同步三文档 → `git commit`（文档）→ `git push`

最小回归命令（Java17）：
- 优先（mvnd）：`export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && export MVND_DAEMON_STORAGE="$PWD/.mvnd/daemon" && export MVND_REGISTRY="$PWD/.mvnd/registry" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`
- 备选（mvn）：`export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl :start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

---

### 历史版（2026-02-12 主线：方案 B（严格）——清零 `eva-*` 技术切片，保持行为不变；已过期，仅备查）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认（必须）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
3) 当前交接基线：以 `git rev-parse --short HEAD` 输出为准
4) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在提示词里固化 commitId）

开始前按顺序阅读（必须）：
1) `NEXT_SESSION_HANDOFF.md`（重点看 0.0、0.9、0.10、0.11）
2) `DDD_REFACTOR_PLAN.md`（重点看 10.2/10.5）
3) `docs/DDD_REFACTOR_BACKLOG.md`（重点看 4.2、4.3、6）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析
- 每个小步骤闭环：Serena → 最小回归 → `git commit`（代码）→ 同步三文档 → `git commit`（文档）→ `git push`
- 每次只改 1 个类（允许新增/搬运单个类）或只改 1 个资源 XML 或只改 1 个 `pom.xml`
- 工作区可能存在未跟踪 `.mvnd/`、`.ssh_known_hosts`，不要提交

（历史提示词（2026-02-12 前状态，已过期，勿复制）：当时 `eva-infra-shared` 尚有大量残留，以下段落仅保留作为“历史口径/回溯”，不要用于新会话续接。）

本轮主线（严格单文件一刀，保持行为不变）：
1) 证据口径（状态确认）：  
   - `fd -t f -e java . eva-infra-shared/src/main/java | wc -l`（预期=9；历史值）  
   - `fd -t f -e java . eva-infra-shared/src/main/java | sort`（列出残留）  
2) 🎯 下一刀候选（单类，优先“无 `eva-infra-shared` 内部依赖”的叶子类）：优先处理 `CourseConvertor`（Serena 证据化：引用面跨 `bc-course/**` 与 `bc-evaluation/**`，且在 `eva-infra-shared` 内仅自引用，适合作为跨 BC Convertor 下沉候选）。  
   - 执行策略：先用 Serena 打开 `CourseConvertor` 的 import 依赖并核对 `shared-kernel/pom.xml` 编译闭合；若缺依赖先做“单 pom 前置”，否则直接做“单类搬运”；若搬运风险超阈值再降级执行 `eva-infra-dal` 单资源 XML 归位。

最小回归命令（每步结束必跑，Java17，优先 mvnd，失败降级 mvn）：  
- `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && export MVND_DAEMON_STORAGE="$PWD/.mvnd/daemon" && export MVND_REGISTRY="$PWD/.mvnd/registry" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`  
- `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl :start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

---

### 续接版（2026-02-12：方案 1 - cour_inf 跨 BC 直连清零收尾，保持行为不变）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认（必须）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
3) 当前交接基线：以 `git rev-parse --short HEAD` 输出为准
4) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在提示词里固化 commitId）

开始前按顺序阅读（必须）：
1) `NEXT_SESSION_HANDOFF.md`（重点看 0.0、0.9、0.10、0.11）
2) `DDD_REFACTOR_PLAN.md`（重点看 10.2/10.5）
3) `docs/DDD_REFACTOR_BACKLOG.md`（重点看 4.2、4.3、6）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析
- 每个小步骤闭环：Serena → 最小回归 → `git commit`（代码）→ 同步三文档 → `git commit`（文档）→ `git push`
- 每次只改 1 个类（允许新增/搬运单个类）或只改 1 个资源 XML 或只改 1 个 `pom.xml`
- 工作区可能存在未跟踪 `.mvnd/`、`.ssh_known_hosts`，不要提交

本轮主线（严格单文件一刀，保持行为不变）：
1) 证据口径：先用 `rg -n "infra\\.dal\\.database\\.mapper\\.course\\.CourInfMapper" --glob "*.java" bc-*` 找出仍在 `bc-course/**` 之外的 `CourInfMapper` 直连点。
2) 🎯 下一刀（单类，推荐从 `bc-template/**` 开始）：将该直连点收敛为调用 `bc-course` 端口（优先复用 `CourInfObjectDirectQueryPort` / `CourInfTimeSlotQueryPort`；若端口能力不足，则按“先补端口（单类）→ 再补适配器（单类）→ 再改调用侧（单类）”推进）。

最小回归命令（每步结束必跑，Java17，必须 mvnd）：  
- `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && export MVND_DAEMON_STORAGE="$PWD/.mvnd/daemon" && export MVND_REGISTRY="$PWD/.mvnd/registry" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

### 推荐版（2026-02-12 主线：S0.2（`eva-infra-dal` 按 BC 拆散 + `eva-infra-shared` 瘦身），保持行为不变）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认（必须）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
3) 当前交接基线：以 `git rev-parse --short HEAD` 输出为准
4) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在提示词里固化 commitId）

开始前按顺序阅读（必须）：
1) `NEXT_SESSION_HANDOFF.md`（重点看 0.0、0.9、0.10、0.11）
2) `DDD_REFACTOR_PLAN.md`（重点看 10.2/10.3/10.4/10.5）
3) `docs/DDD_REFACTOR_BACKLOG.md`（重点看 4.2、4.3、6）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析（若持续 TimeoutError：需在 0.9 记录“降级原因 + 可复现 rg 证据”，并在下一会话优先排查恢复）
- 每个小步骤闭环：Serena → 最小回归 → `git commit` → 同步 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` → `git commit` → `git push`
- 每次只改 1 个类（允许新增/搬运单个类）或只改 1 个资源 XML 或只改 1 个 `pom.xml`

补充提醒（协作约束）：
- 工作区可能存在未跟踪 `.mvnd/`、`.ssh_known_hosts`，不要提交。

本轮优先主线（保持行为不变；严格单文件一刀）：
1) 🎯 **下一刀（单类，跨 BC Mapper 收敛，课程 → 评教）**：优先清零 `bc-course/**` 对评教侧 `infra.dal.database.mapper.eva.*` 的编译期直连（当前残留 12 处 / 5 个文件；候选清单与回归命令见 0.11 推荐版）。
2) ✅ **阶段性完成（依赖方收敛，保持行为不变）**：其它 BC 对 IAM role 表（`sys_user_role/sys_role`）的跨 BC 直连已清零。
   - 证据口径：Serena 证伪 `bc-*`（排除 `bc-iam/**`）范围内无 `SysUserRoleMapper`/`SysRoleMapper` 引用点；如后续新增/回归引用点，仍按“每次只改 1 个类”将直连装配/查询收敛为调用 `bc-iam-contract` 端口 `UserEntityObjectByIdDirectQueryPort`（异常文案/副作用顺序完全不变；不引入新缓存/切面副作用）。
3) ✅ **状态提醒（避免重复劳动）**：`SysRoleMapper.xml` 已从 `eva-infra-dal` 归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；详见 0.9）。
4) ✅ **状态提醒（避免重复劳动）**：`SysUserMapper.xml` 已从 `eva-infra-dal` 归位到 `bc-iam/infrastructure`（保持 MyBatis `namespace/resultMap type`、SQL 与资源路径 `mapper/**` 不变；详见 0.9）。

最小回归命令（每步结束必跑，Java17，优先 mvnd，失败则 mvn）：  
- `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && export MVND_DAEMON_STORAGE="$PWD/.mvnd/daemon" && export MVND_REGISTRY="$PWD/.mvnd/registry" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`
- `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl :start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

闭环顺序（强制）：Serena → 最小回归 → `git commit`（代码）→ 同步三文档 → `git commit`（文档）→ `git push`

推送提示（保持行为不变，仅协作效率）：若 `git push` 卡住无输出，优先怀疑 GitHub SSH `22` 端口被阻断，可改用 `ssh.github.com:443`（示例：`GIT_TERMINAL_PROMPT=0 GIT_SSH_COMMAND='ssh -o BatchMode=yes -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o Hostname=ssh.github.com -p 443' git push origin ddd`）。

快速证据口径（新会话先跑一次，避免口径漂移）：
- `rg -n "<module>eva-" pom.xml`（root reactor 当前仍应命中 `eva-base`）
- `fd -t f -e java . eva-base/eva-base-common/src/main/java | wc -l`（统计 `eva-base-common` 源码余量，当前应为 0）
- `rg -n "<artifactId>eva-base-common</artifactId>" --glob "**/pom.xml" . | sort`（统计 `eva-base-common` 被依赖点）
- `git stash list`（当前应为 3 条；禁止整包 pop）

### 推荐版：续接主线（`eva-base-common` 退场收尾；保持行为不变）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认（必须，禁止误操作 reset/checkout）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git status -sb`（重要：可能存在 staged+unstaged 混合；禁止误操作 reset/checkout）
3) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
4) 当前交接基线：以 `git rev-parse --short HEAD` 输出为准
5) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不固化 commitId）

开始前按顺序阅读（必须）：
1) `NEXT_SESSION_HANDOFF.md`（重点看 0.0、0.9、0.10、0.11）
2) `DDD_REFACTOR_PLAN.md`（重点看 10.2/10.5）
3) `docs/DDD_REFACTOR_BACKLOG.md`（重点看 4.2、4.3、6）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析
- 每个小步骤闭环：Serena → 最小回归 → `git commit`（代码或配置）→ 同步三文档 → `git commit`（文档）→ `git push`
- 每次只改 1 个类（允许新增/搬运单个类）或只改 1 个资源 XML 或只改 1 个 `pom.xml`
- 工作区可能存在未跟踪 `.mvnd/`、`.ssh_known_hosts`，不要提交
- `git stash list` 当前应有 3 条 WIP；后续只能按“指定文件路径”逐刀 restore，禁止整包 pop

证据口径（先跑快照，避免口径漂移）：
- `rg -n "<module>eva-" pom.xml`（当前应仍命中 `eva-base`）
- `fd -t f -e java . eva-base/eva-base-common/src/main/java | wc -l`（预期=0）
- `rg -n "<artifactId>eva-base-common</artifactId>" --glob "**/pom.xml" . | sort`（预期仍有命中：待逐 pom 清零）
- `git stash list`

本轮主线（每次只改 1 个文件闭环；保持行为不变）：
1) 单 `pom.xml`：移除 `bc-iam/contract/pom.xml` 对 `eva-base-common` 的依赖（前置：Serena 证伪 contract 源码仅使用 `GenericPattern/LogModule` 且它们已在 `shared-kernel`）。
2) 单 `pom.xml`：移除 `bc-iam/infrastructure/pom.xml` 对 `eva-base-common` 的依赖。
3) 单 `pom.xml`：`eva-base/pom.xml` 移除 `<module>eva-base-common</module>`（前置：依赖方已清零）。
4) 单文件：删除 `eva-base/eva-base-common/pom.xml`（前置：全仓库 `**/pom.xml` 不再出现 `<artifactId>eva-base-common</artifactId>`）。
5) 单 `pom.xml`：root `pom.xml` 移除 `<module>eva-base</module>`。

最小回归命令（每步结束必跑，Java17，优先 mvnd，失败降级 mvn）：
- `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && export MVND_DAEMON_STORAGE="$PWD/.mvnd/daemon" && export MVND_REGISTRY="$PWD/.mvnd/registry" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`
- `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl :start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

闭环顺序（强制）：Serena → 最小回归 → `git commit`（代码/配置）→ 同步三文档 → `git commit`（文档）→ `git push`

风险提示（保持行为不变）：
- `pom.xml` 依赖收敛容易被增量编译/缓存掩盖误判：移除依赖前必须 Serena 证伪“无引用面”，必要时 `clean` 后再判断。

### 历史保留（IAM S0.2 延伸 —— 该段已过期：`RoleEntity/UserEntity/RoleQueryGateway` 均已归位 `bc-iam-domain`）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认（必须）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
3) 当前交接基线：以 `git rev-parse --short HEAD` 输出为准
4) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在提示词里固化 commitId）

开始前按顺序阅读（必须）：
1) `NEXT_SESSION_HANDOFF.md`（重点看 0.0、0.9、0.10、0.11）
2) `DDD_REFACTOR_PLAN.md`（重点看 10.2/10.3/10.4/10.5）
3) `docs/DDD_REFACTOR_BACKLOG.md`（重点看 4.2、4.3、6）
4) `data/` 与 `data/doc/`（如需核对表/字段语义）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析（若持续 TimeoutError：需在 0.9 记录“降级原因 + 可复现 rg 证据”，并在下一会话优先排查恢复）
- 每个小步骤闭环：Serena → 最小回归 → `git commit` → 同步 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` → `git commit` → `git push`
- 每次只改 1 个类（不限 Controller；允许新增/搬运单个类）或只改 1 个资源 XML 或只改 1 个 `pom.xml`

本轮目标（推荐版，优先；保持行为不变）：
1) ✅ **S1（入口壳收敛：BC Controller）已阶段性完成**：当前 `bc-*` 下 Controller 清单已收敛为固定集合（见“快速证据口径”中的 `fd ... bc-*`），且均已完成“仅结构性收敛”（收敛返回包装表达式/抽取 `success()` 等，行为不变）。后续如新增/归位新的 Controller，继续沿用同一收敛模板（每次只改 1 个类）。
2) 🎯 **S0.2 延伸（依赖方 pom 收敛，含纠偏）**：每次只改 1 个 `pom.xml`，移除依赖前必须先用 Serena 证据化“无类型引用面”，并用 `rg` 做可复现的兜底证据，避免增量编译/缓存导致误判。
3) ⚠️ 关键注意：`bc-iam-contract` 当前必须显式依赖 `bc-evaluation-contract`（`IUserService#getOneUserScore` 返回 `UserSingleCourseScoreCO`，定义于 `bc-evaluation/contract`；已纠偏恢复依赖，详见 0.9）。
4) 🎯 **依赖方收敛（本轮主线，保持行为不变）**：目标是让非 IAM 依赖方（如 `bc-evaluation/bc-messaging/bc-audit/bc-course`）逐步不再“编译期依赖”旧 `edu.cuit.domain.entity.user.biz.UserEntity`，而是仅编译期依赖 `bc-iam-contract` 的最小 Port/基础类型；端口适配器内部仍委托旧 gateway，确保缓存/切面触发点不变。现状：✅ `bc-evaluation/infrastructure` 已清零 `UserEntity` import；✅ `bc-messaging` 已清零；✅ `bc-audit(infrastructure)` 已清零；✅ `bc-course/infrastructure` 已清零；✅ `bc-iam/infrastructure` 已清零（见“快速证据口径”）。
5) 🎯 **下一刀聚焦（保持行为不变）**：`bc-iam/infrastructure` 已清零 `UserQueryGateway` 与 `UserEntity` 的直接 import（且 `UserQueryGatewayImpl` 已不再显式实现旧接口，`@LocalCached` 触发点保持不变）。下一步建议（低风险、单类闭环）：先用 Serena 证据化确认 `edu.cuit.domain.gateway.user.RoleQueryGateway` 的引用面是否仍仅在 `bc-iam/**`；若确认无跨 BC 引用，则将 `RoleQueryGateway` 从 `eva-domain` 搬运归位到 `bc-iam-domain`（保持 `package` 不变），以进一步缩小 `eva-domain` 表面积。后置再评估 `RoleEntity/UserEntity` 的 Maven 归属/替代方案（高风险；多步；每次仍只改 1 个类或 1 个 pom）。
6) ⚠️ **避免踩坑（保持行为不变）**：不要直接把 `bc-ai-report/infrastructure` 对 `bc-evaluation` 的依赖替换为 `bc-evaluation-contract` 来“省依赖”；评教导出端口签名当前会触发 Maven 循环依赖（原因见 0.9 的踩坑记录），需先做签名/实体归属拆解再推进。

下一刀（保持行为不变，优先顺序 A → B → C → D → E → F；每次只改 1 个 pom 或 1 个类）：
- ✅ A（类，已完成）：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/user/UserConverter.java` 已增加“Spring Bean 桥接 + setName 桥接”方法 `springUserEntityWithNameObject(Object)`（内部仍 `SpringUtil.getBean(UserEntity.class)` + 强转 `UserEntity#setName`，尽量保持历史异常形态与副作用顺序一致）。
- ✅ B（类，已完成）：`bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/query/CourseQueryRepository.java` 已去 `UserEntity` import：`UserEntity`/`Supplier<UserEntity>` → `Object`/`Supplier<?>`；调用侧已改走 `userConverter.toUserEntityObject(...)`、`courseConvertor.toCourseEntityWithTeacherObject(...)`；并将 `SpringUtil.getBean(UserEntity.class)` + `setName` 改为调用 `userConverter.springUserEntityWithNameObject(...)`（缓存/查询/遍历顺序与异常文案不变）。
- ✅ C（类，已完成）：`bc-iam/infrastructure/src/main/java/edu/cuit/app/factory/user/RouterDetailFactory.java` 已去 `UserEntity` 编译期依赖：入参从 `UserEntity` 收敛为 `Object`，并改走 `UserConverter` 桥接读取字段（过滤/递归逻辑与异常表现不变）。
- ✅ D（类，已完成）：`bc-iam/infrastructure/src/main/java/edu/cuit/app/service/impl/user/UserServiceImpl.java` 已去 `UserEntity` 编译期依赖：`getUserInfo` 入参收敛为 `Object`，并改走 `UserBizConvertor.toUserDetailCOObject(Object)` + `UserConverter.rolesOf/permsOf` 桥接读取字段（缓存/日志/异常文案/副作用顺序不变）。
- ✅ E（已完成，保持行为不变）：已完成 `UserQueryGatewayImpl` 调用面/缓存触发点证据化盘点，并落地“内部过渡接口 + 逐类替换注入类型”的收敛策略：新增 `UserQueryCacheGateway`，并让 `UserQueryGatewayImpl` 同时实现该接口，后续端口适配器可把注入从 `UserQueryGateway` 收敛为 `UserQueryCacheGateway` 而不改变委托路径与 `@LocalCached` 触发点（见 0.9）。
- ✅ F（已完成，保持行为不变）：`bc-iam/infrastructure` 端口适配器对旧 `UserQueryGateway`（eva-domain）的编译期依赖已清零：已统一将注入类型收敛为内部接口 `UserQueryCacheGateway`，但方法体仍委托旧 `UserQueryGatewayImpl` 以触发 `@LocalCached` 缓存/切面入口（行为不变）。

快速证据口径（新会话先跑一次，避免口径漂移）：
- `fd -t f 'Controller\\.java$' bc-iam/infrastructure/src/main/java | sort`（盘点 IAM Controller 清单，便于按“每次 1 个类”推进）
- `fd -t f 'Controller\\.java$' bc-* | sort`（盘点全仓库 BC Controller 清单；当前仅这些文件应存在）
- `rg -n \"UserSingleCourseScoreCO\" bc-iam/contract/src/main/java/edu/cuit/bc/iam/application/contract/api/user/IUserService.java`（确认跨 BC 协议类型引用仍存在）
- `rg -n \"<artifactId>bc-evaluation-contract</artifactId>\" bc-iam/contract/pom.xml`（确认 `bc-iam-contract` 显式依赖已恢复）
- `rg -n \"import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;\" bc-evaluation/infrastructure/src/main/java`（应命中为 0：评教读侧仓储已清零）
- `rg -n \"import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;\" bc-messaging/src/main/java`（应命中为 0：消息查询适配器已清零）
- `rg -n \"import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;\" bc-audit/infrastructure/src/main/java`（应命中为 0：审计读侧已清零）
- `rg -n \"import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;\" bc-course/infrastructure/src/main/java`（应命中为 0：课程读侧已清零）
- `rg -n \"\\bUserEntity\\b\" bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/query/CourseQueryRepository.java`（应命中为 0：该类已完成类型桥接清零）
- `rg -n \"import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;\" bc-* --glob \"*.java\" | head`（应命中为 0：BC 范围已清零；若非 0，优先定位“新引入依赖方”的变更点）
- `rg -n \"import\\s+edu\\.cuit\\.domain\\.entity\\.user\\.biz\\.UserEntity;\" bc-iam/infrastructure/src/main/java --glob \"*.java\"`（应命中为 0：IAM 基础设施已清零）
- `rg -n \"import\\s+edu\\.cuit\\.domain\\.gateway\\.user\\.UserQueryGateway;\" bc-iam/infrastructure/src/main/java --glob \"*.java\"`（应命中为 0：端口适配器已清零，且 `UserQueryGatewayImpl` 已不再显式实现旧接口）
- `rg -n \"\\bCourseQueryGateway\\b\" bc-* --glob \"*.java\" | head`（下一刀证据口径：确认课程域类型引用面分布，为 `eva-domain` → `bc-course-domain` 归位做前置）
- `rg -n \"<module>eva-adapter</module>\" pom.xml`（应为空：复核“reactor 已移除 eva-adapter 模块”）
- `rg -n \"<artifactId>eva-adapter</artifactId>\" --glob \"**/pom.xml\" .`（应为空：复核“全仓库已无 eva-adapter Maven 依赖/声明”）
- `rg -n \"<module>eva-\" pom.xml`（复核 root reactor 仍包含哪些 `eva-*` 技术切片模块）

执行模板（每步闭环，保持行为不变）：
1) Serena：证据化盘点（类/方法引用面、pom 依赖链），确认“只改 1 个类或只改 1 个 pom”不会引入 Maven 循环依赖或运行时装配漂移
2) 最小化改动：每次只改 1 个类（允许新增/搬运单个类）或只改 1 个 `pom.xml`（不改业务语义）
3) 最小回归 → `git commit`（pom/代码）→ 三文档同步 → `git commit`（文档）→ `git push`

最小回归命令（每步结束必跑，Java17）：  
`export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && export MVND_DAEMON_STORAGE="$PWD/.mvnd/daemon" && export MVND_REGISTRY="$PWD/.mvnd/registry" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

> 注意：`.mvnd/` 为本机 `mvnd` 的 daemon/registry 工作目录（用于避免默认写入 `~/.m2/mvnd/...` 时的权限问题），保持为未跟踪文件，不要提交到 Git。

---

### E 专用简版（✅ 已闭环：eva-adapter 残留 *Controller 已清零；用于下一阶段依赖收敛）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认（必须）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
3) 当前交接基线：以 `git rev-parse --short HEAD` 输出为准
4) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在提示词里固化 commitId）

开始前按顺序阅读（必须）：
1) `NEXT_SESSION_HANDOFF.md`（重点看 0.0、0.9、0.10、0.11）
2) `DDD_REFACTOR_PLAN.md`（重点看 10.2/10.3/10.4/10.5）
3) `docs/DDD_REFACTOR_BACKLOG.md`（重点看 4.2、4.3、6）
4) `data/` 与 `data/doc/`（如需核对表/字段语义）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析（若持续 TimeoutError：需在 0.9 记录“降级原因 + 可复现 rg 证据”，并在下一会话优先排查恢复）
- 每个小步骤闭环：Serena → 最小回归 → `git commit` → 同步 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` → `git commit` → `git push`
- 每次只改 1 个类（不限 Controller；允许新增/搬运单个类）或只改 1 个 `pom.xml`

本轮目标（E，优先；保持行为不变）：
1) ✅ 已完成：收敛 `eva-adapter` 残留 `*Controller`（已清零；见 0.10.2）
2) ⏳ 次优先：继续做 S0.2 延伸（依赖方 `pom.xml` 依赖收敛，每次只改 1 个 pom；保持行为不变）

下一刀（保持行为不变）：
- ✅ **S1（入口归位：LogController，保持行为不变）**：已将 `LogController` 从 `eva-adapter` 归位到 `bc-audit/infrastructure`（保持行为不变；详见 0.9/0.10.2）。
- **A（优先：pom）S0.2 延伸（依赖方 `pom.xml` 依赖收敛，每次只改 1 个 pom；保持行为不变）**：优先从“无源码测试/无测试引用”的模块开始做依赖收敛（流程上更可控）：例如收敛各 `*-contract`/`*-infra`/共享模块中**未使用的 `junit-jupiter(test)`**（每步仍需 Serena + `rg` 双证据，避免误删导致增量编译误判）。建议口径：
  - `rg -n "<artifactId>junit-jupiter</artifactId>" --glob "**/pom.xml" .`（盘点候选）
  - 对单个候选模块：`fd -t d src/test <module>`（应为空） + `rg -n "org\\.junit\\.jupiter" <module>/src`（应为空）
  - 通过后再改该模块 `pom.xml` 移除依赖（仅 1 个 pom），并跑最小回归闭环。
- **A.1（提醒：当前阻塞）**：本会话已证据化确认 `bc-ai-report/infrastructure` 暂不能将 `bc-evaluation` 收敛为 `bc-evaluation-contract`（原因见 0.9 的“AI 报告 infra：依赖收敛评估”），因此下一会话不要直接做“单 pom 替换”以免引入 `contract` 反向依赖或类型重复风险；需先设计并拆解端口/实体归属再推进。
- **B（可选：pom）S1（收尾清理，保持行为不变）**：`eva-infra` 已从 root reactor 退场且无依赖方（见 0.9/0.10），可独立提交评估是否删除 `eva-infra/` 目录与 `eva-infra/pom.xml`（当前仅剩 3 个 `package-info.java`）。
- **C（备选：类）IAM 去 `eva-domain` 前置搬运**：继续逐类归位“仅 IAM 引用”的 `edu.cuit.domain.*` 到 `bc-iam-domain`（保持 `package`/签名不变；不要提前动 `RoleEntity/RoleQueryGateway`）。

执行模板（每步闭环，保持行为不变）：
1) Serena：定位 Controller 方法 + 引用分析（确认仅 Controller 层变更，不引入新副作用）
2) 最小化重构：优先“抽私有方法/收敛重复表达式/收敛参数校验位置”，不改返回体结构、不改异常文案
3) 最小回归 → `git commit`（代码）→ 三文档同步 → `git commit`（文档）→ `git push`

最小回归命令（每步结束必跑，Java17）：  
`export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

---

### D 专用简版（历史：收敛 eva-app 残留 *ServiceImpl，✅ 已闭环）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认（必须）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
3) 当前交接基线：以 `git rev-parse --short HEAD` 输出为准
4) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在提示词里固化 commitId）

开始前按顺序阅读（必须）：
1) `NEXT_SESSION_HANDOFF.md`（重点看 0.0、0.9、0.10、0.11）
2) `DDD_REFACTOR_PLAN.md`（重点看 10.2/10.3/10.4/10.5）
3) `docs/DDD_REFACTOR_BACKLOG.md`（重点看 4.2、4.3、6）
4) `data/` 与 `data/doc/`（如需核对表/字段语义）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析（若持续 TimeoutError：需在 0.9 记录“降级原因 + 可复现 rg 证据”，并在下一会话优先排查恢复）
- 每个小步骤闭环：Serena → 最小回归 → `git commit` → 同步 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` → `git commit` → `git push`
- 每次只改 1 个类（或只改 1 个 pom）

本轮目标（E，优先；保持行为不变）：
1) 🎯 **IAM S0.2 延伸——依赖方收敛**：继续收敛依赖方（优先 `bc-evaluation/infrastructure` 读侧仓储）对旧 `UserEntity` 的编译期依赖，逐步让依赖方仅编译期依赖 `bc-iam-contract` 的最小 Port/基础类型；端口适配器内部仍委托旧 gateway，确保缓存/切面触发点不变。
2) ✅ 已完成：统计导出基类 `EvaStatisticsExporter` 已去 `UserEntity` 编译期依赖（见 0.9 与 0.10.1）。
3) 下一刀建议（每次只改 1 个类；保持行为不变；按“前置桥接 → 改调用侧”的顺序）：
   - ✅ A：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/user/UserConverter.java` 增加 `toUserEntityObject(...)` + `userIdOf(Object)`。（已完成，详见 0.9/0.10.1）
   - ✅ B：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/course/CourseConvertor.java` 增加 `toCourseEntityWithTeacherObject(...)`。（已完成，详见 0.9/0.10.1）
   - ✅ C：`bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/bcevaluation/query/EvaTaskQueryRepository.java` 去 `UserEntity` 编译期依赖。（已完成，详见 0.9/0.10.1）
   - ✅ D：`bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/bcevaluation/query/EvaRecordQueryRepository.java` 去 `UserEntity` 编译期依赖。（已完成，详见 0.9/0.10.1）

历史保留（D，已不再是当前主线，保留用于回溯）：收敛 `eva-app` 残留 `*ServiceImpl` / `eva-adapter` 残留 `*Controller`（清单与口径以 0.10 为准）。

建议拆分（每步只改 1 个类；保持行为不变；每步闭环=Serena→最小回归→commit(代码)→三文档同步→commit(文档)→push）：
1) ✅ 已完成（落地：`325f221a`）：在 `bc-course/application/usecase` 新增“课程类型”用例 `CourseTypeUseCase`（建议单类覆盖读写；不引入 `eva-infra-shared` 等重依赖；手写 `CourseTypeEntity` → `CourseType` 映射 + `PaginationQueryResultCO` 组装，逐行对齐旧入口逻辑）。
2) ✅ 已完成（落地：`55eb322e`）：在 `BcCourseConfiguration` 补齐该用例的 Bean 装配（保持行为不变）。
3) ✅ 已完成（落地：`1aebda24`）：将 `eva-app` 的 `ICourseTypeServiceImpl` 退化为纯委托壳（注意 `id==null` 语义与 `updateCoursesType` 返回 `null` 必须不变）。

最小回归命令（每步结束必跑，Java17，必须 mvnd）：  
`export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && export MVND_DAEMON_STORAGE="$PWD/.mvnd/daemon" && export MVND_REGISTRY="$PWD/.mvnd/registry" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

### B 专用简版（旧 *GatewayImpl 归位，✅ 已闭环，历史保留）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认（必须）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
3) 当前交接基线（用于对照）：以 `git rev-parse --short HEAD` 输出为准
4) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在提示词里固化 commitId）

开始前按顺序阅读（必须）：
1) `NEXT_SESSION_HANDOFF.md`（重点看 0.0、0.9、0.10、0.11，以及条目 24/25/26）
2) `DDD_REFACTOR_PLAN.md`（重点看 10.2/10.3/10.4/10.5）
3) `docs/DDD_REFACTOR_BACKLOG.md`（重点看 4.2、4.3、6）
4) `data/` 与 `data/doc/`（如需核对表/字段语义）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析
- 每个小步骤闭环：Serena → 最小回归 → `git commit` → 同步 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` → `git commit` → `git push`
- 每次只迁 1 个类（或只改 1 个 pom）

本轮目标（B）：继续将 `eva-infra/src/main/java/edu/cuit/infra/gateway/impl` 下残留旧 `*GatewayImpl` 归位到对应 BC 的 `infrastructure` 子模块（通常 artifactId 为 `bc-xxx-infra`；**保持 package 不变、行为不变**）。

当前残留（以 Serena 盘点为准）：✅ 已清零。

落点提示（保持 package 不变）：
- IAM：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/`
- Evaluation：`bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/eva/`

最小回归命令（每步结束必跑，Java17）：  
`export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

### C 专用简版（条目 26：IAM DAL 抽离，✅ 已闭环，历史保留）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认（必须）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
3) 当前交接基线（用于对照）：以 `git rev-parse --short HEAD` 输出为准
4) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在提示词里固化 commitId）

开始前按顺序阅读（必须）：
1) `NEXT_SESSION_HANDOFF.md`（重点看 0.0、0.9、0.10、0.11，以及条目 24/25/26）
2) `DDD_REFACTOR_PLAN.md`（重点看 10.2/10.3/10.4/10.5）
3) `docs/DDD_REFACTOR_BACKLOG.md`（重点看 4.2、4.3、6）
4) `data/` 与 `data/doc/`（如需核对表/字段语义）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析
- 每个小步骤闭环：Serena → 最小回归 → `git commit` → 同步 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md` → `git commit` → `git push`
- 每次只迁 1 个类（必要时携带其对应 XML 以保持闭合；或只改 1 个 pom）

本轮目标（C，历史）：按条目 26 的路线，逐步抽离 IAM DAL（DO/Mapper/XML）到共享模块 `eva-infra-dal`（保持 `package/namespace/SQL/异常/顺序` 完全不变），最终使 `bc-iam-infra` 去掉对 `eva-infra` 的 Maven 依赖（避免 Maven 环）。

最小回归命令（每步结束必跑，Java17）：  
`export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

### 推荐版（不固化 commitId，优先复制本段）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认（必须）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
3) 当前交接基线（用于对照）：以 `git rev-parse --short HEAD` 输出为准
4) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在提示词里固化 commitId）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 允许微调：仅限结构性重构（收窄依赖/拆接口/搬运默认值兜底/类归位），仍以“行为不变”为最高约束
- 必须使用 Serena 做符号级定位与引用分析（若 MCP 工具出现 `TimeoutError` 等不可用情况：需在 0.9 记录“降级原因 + 可复现的 `rg` 证据”，并在下一会话优先排查恢复）
- 每个小步骤闭环顺序：Serena（符号级定位/引用分析）→ 最小回归 → git commit → 同步 NEXT_SESSION_HANDOFF.md / DDD_REFACTOR_PLAN.md / docs/DDD_REFACTOR_BACKLOG.md（保持行为不变）
- 每次结束对话前：先写清“下一步拆分与里程碑/提交点”

开始前按顺序阅读（重点章节同旧要求）：
1) NEXT_SESSION_HANDOFF.md（重点看 0.0、0.9、0.10、0.11，以及条目 24/25/26）
2) DDD_REFACTOR_PLAN.md（重点看 10.2/10.3/10.4）
3) docs/DDD_REFACTOR_BACKLOG.md（重点看 4.2、4.3、6）
4) data/ 与 data/doc/（如需核对表/字段语义）
5) （新增重点）`DDD_REFACTOR_PLAN.md` 的 10.5 与 `docs/DDD_REFACTOR_BACKLOG.md` 的 4.3（`eva-*` 退场路线 + 写侧入口/旧网关清单）。

本会话目标（优先顺序；每步闭环=Serena→最小回归→commit→三文档同步→push；保持行为不变）：

1) 🎯 优先：收敛 `eva-app` 残留 `*ServiceImpl`（每次只改 1 个类；保持行为不变）
   - 目标：逐步把“业务编排”归位到各 BC 的 UseCase（`bc-*/application/usecase`），让 `eva-app` 的旧入口退化为“仅保留 `@CheckSemId` / 登录态解析 / 委托 UseCase”的壳。
   - 推荐起步（风险更可控）：优先从最小回归覆盖更高的评教链路相关 `*ServiceImpl` 入手（例如与 `EvaRecordServiceImplTest` / `EvaStatisticsServiceImplTest` 强关联的类），确保每步都能用现有回归命令快速验收。

2) ⏳ 次优先：收敛 `eva-adapter` 残留 `*Controller`（每次只改 1 个 Controller；保持行为不变）

3) ✅ 已闭环（避免重复劳动）：条目 26（IAM DAL 抽离到 `eva-infra-dal` / `eva-infra-shared`，`bc-iam-infra` 去 `eva-infra` 依赖；保持行为不变）

最小回归命令（每步结束必跑，Java17）：  
`export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

（需要定位最近会话做了什么：以 `NEXT_SESSION_HANDOFF.md` 的 0.9 增量总结为准；需要定位落地提交：用 `git log -n 1 -- <文档>` 或按日期范围查看 `git log --oneline`，不要把 commitId 写回提示词。）

---

### 旧版（含 commitId，不建议复制，仅历史参考）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认：分支必须是 ddd；HEAD 必须 >= 2e4c4923（运行 `git rev-parse HEAD` 确认）。
当前交接基线（用于对照）：以 `git rev-parse --short HEAD` 输出为准（本文档最后同步提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准，不在文内固化 commitId，避免后续会话滚动更新遗漏）。

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析
- 每个小步骤闭环顺序：Serena（符号级定位/引用分析）→ 最小回归 → git commit → 同步 NEXT_SESSION_HANDOFF.md / DDD_REFACTOR_PLAN.md / docs/DDD_REFACTOR_BACKLOG.md（保持行为不变）
- 每次结束对话前：先写清“下一步拆分与里程碑/提交点”

开始前按顺序阅读（重点章节同旧要求）：
1) NEXT_SESSION_HANDOFF.md（重点看 0.0、0.9、0.10、0.11，以及条目 24/25/26）
2) DDD_REFACTOR_PLAN.md（重点看 10.2/10.3/10.4）
3) docs/DDD_REFACTOR_BACKLOG.md（重点看 4.2、4.3、6）
4) data/ 与 data/doc/（如需核对表/字段语义）

本会话目标（优先做这个）：
- **S0.1（下一步优先：收敛 `eva-domain` → `eva-client` 依赖）**：以 `eva-domain/pom.xml` 的 `eva-client` 依赖为收敛口，优先迁移课程/学期/消息等仍留在 `eva-client` 的协议对象（先 Serena 盘点 `eva-domain` 的 `import edu.cuit.client.*` 清单），最终在“可证实不再需要”的前提下移除该依赖（保持行为不变；每步=最小回归+提交+三文档同步）。
- **S0.1（IAM 继续推进，后续）**：已推进：迁移 IAM 专属接口 `IDepartmentService` 到 `bc-iam-contract`（保持行为不变；`656dc36e`）；后续继续用 Serena 盘点 `eva-client` 残留对象在 IAM 的引用范围，若为 IAM 专属则继续迁移到 `bc-iam-contract`（或更合适的 BC）（保持行为不变）。
- ✅ **P1.2（评教域继续拆 `eva-client`，盘点）**：已闭环：Serena 盘点确认 `eva-client` 下评教专属目录已迁空（保持行为不变；`e643bac9`），后续更可能只剩“依赖面收敛/迁移其它 BC 残留对象”的推进。
- ✅ **P1.2-3（审计日志协议继续拆 `eva-client`）**：已闭环：迁移 `SysLogBO` 到 `eva-domain` 并移除 `bc-audit` → `eva-client` Maven 直依赖（保持行为不变；`734a3741/2fcb257c`）。

当前状态（已闭环）：
- 提交点 0：条目 25 定义/边界/验收口径已补齐（`1adc80bd`）
- 提交点 A：`bc-ai-report` / `bc-audit` 最小骨架已接入组合根（`a30a1ff9`）
- 提交点 B：审计日志写入 `LogGatewayImpl.insertLog` 已按“用例+端口+适配器+委托壳”收敛（`b0b72263`）
- P1.2-2：审计日志协议对象 `ILogService/OperateLogCO/LogModuleCO` 已迁移到 `bc-audit`（保持 `package` 不变；保持行为不变；`e1dbf2d4`）
- 提交点 C（统计主题）：已抽出 `EvaStatisticsQueryRepo` 并收敛端口依赖（`d5b07247`）
- 提交点 C2（记录主题）：已抽出 `EvaRecordQueryRepo` 并收敛端口依赖（`cae1a15c`）
- 提交点 C3（任务主题）：已抽出 `EvaTaskQueryRepo` 并收敛端口依赖（`82427967`）
- 提交点 C4（模板主题）：已抽出 `EvaTemplateQueryRepo` 并收敛端口依赖（`889ec9b0`）
- 提交点 B2（AI 报告导出）：`AiCourseAnalysisService.exportDocData` 已按“用例+端口+端口适配器+委托壳”收敛（`c68b3174`）
- 提交点 B3（AI 报告导出）：`AiCourseAnalysisService.exportDocData` 已进一步退化为纯委托壳（`7f4b3358`）
- 提交点 B4（AI 报告 analysis）：`AiCourseAnalysisService.analysis` 已按“用例+端口+端口适配器+委托壳”收敛（`a8150e7f`）
- 提交点 B5（AI 报告用户名解析）：`ExportAiReportDocByUsernameUseCase` 的 username → userId 查询已抽为 `bc-ai-report` 端口 + `eva-app` 端口适配器（`d7df8657`）
- 提交点 C5-1（统计主题实现拆分）：已抽出 `EvaStatisticsQueryRepository`（`9e0a8d28`）
- 提交点 C5-2（记录主题实现拆分）：已抽出 `EvaRecordQueryRepository`（`985f7802`）
- 提交点 C5-3（任务主题实现拆分）：已抽出 `EvaTaskQueryRepository`（`d467c65e`）
- 提交点 C5-4（模板主题实现拆分）：已抽出 `EvaTemplateQueryRepository`（`a550675a`）
- 提交点 D1（评教 BC 自包含三层结构试点，阶段 1）：已引入 `bc-evaluation-infra` 并迁移评教读侧查询实现（保持包名/行为不变；落地提交：`be6dc05c`）
  - 同步：course/eva DAL（DO/Mapper/XML）已迁移到 `eva-infra-dal`；`CourseConvertor`/`EvaConvertor`/`EvaCacheConstants`/`CourseFormat` 已迁移到 `eva-infra-shared`（均保持包名/namespace 不变）
  - 三文档同步提交：`4e52d74c/53832c45`
- 提交点 D2（评教 BC 自包含三层结构试点，阶段 2）：已迁移评教写侧 Repo（`eva-infra/src/main/java/edu/cuit/infra/bcevaluation/repository/*`）到 `bc-evaluation-infra`（保持包名/行为不变；落地提交：`24e7f6c9`）
  - 同步：为保持 `bc-evaluation-infra` 不依赖 `eva-infra`，已将 `CalculateClassTime` 迁移到 `eva-infra-shared`（保持包名不变）
- 提交点 C-1（读侧门面加固，可选）：已清理 `EvaQueryRepository` 为纯委托壳（移除已无引用的历史私有实现/冗余依赖；保持行为不变；落地提交：`73fc6c14`；三文档同步：`083b5807`）
- 提交点 C-2（读侧仓储瘦身，可选）：已完成 C-2-5 并关闭（盘点评教四主题 QueryRepository 未发现可证实无引用项，保持行为不变；落地：`e2a2a717/8b76375f/4a317344/dba6e31d/5c1a03bc`）
- S0.1：已新增 `shared-kernel` 子模块，并将 `PagingQuery/ConditionalQuery/GenericConditionalQuery/SimpleResultCO/PaginationQueryResultCO` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；`a25815b2`）
- S0.1：`bc-iam-contract` / `bc-evaluation-contract` 已增加对 `shared-kernel` 的直依赖（暂保留 `eva-client` 以可回滚；`3a0ac086`）
- S0.1：已迁移 IAM `MenuConditionalQuery` 到 `bc-iam-contract`（保持 `package` 不变；`1eda37c9`）
- P1.2：已迁移评教 `EvaTaskConditionalQuery/EvaLogConditionalQuery` 到 `bc-evaluation/contract`（保持 `package` 不变；`d02d5522`）
- S0.1：已迁移 `ValidStatus/ValidStatusValidator` 到 `shared-kernel`（保持 `package` 不变；`686de369`）
- S0.1：`bc-iam-contract` 已去除对 `eva-client` 的直依赖（`8d673c17`）
- S0.1-7：`bc-iam/application` 已去除对 `eva-client` 的直依赖（保持行为不变；`7371ab96`）
- P1.2-1：`bc-evaluation/application` 已去除对 `eva-client` 的直依赖（保持行为不变；`10e8eb0b`）
- P1.2-2：审计日志协议对象已迁移到 `bc-audit`（保持 `package` 不变；保持行为不变；`e1dbf2d4`）
- S0.1：已迁移评教 `dto/cmd/eva/*` 到 `bc-evaluation/contract`（保持 `package` 不变；`2273ad61`）
- S0.1：已迁移 `EvaConfig` 到 `bc-evaluation/contract`（保持 `package` 不变；`438d38bf`）
- S0.1：已迁移评教统计/表单相关 CO（`DateEvaNumCO/TimeEvaNumCO/MoreDateEvaNumCO/SimpleEvaPercentCO/SimplePercentCO/FormPropCO`）到 `bc-evaluation/contract`（保持 `package` 不变；`c2d8a8b1`）
- S0.1：已将课程时间模型 `dto/data/course/CourseTime` 迁移到 `shared-kernel`（保持 `package` 不变；`5f21b5ce`）
- S0.1：已在“可证实不再需要”的前提下移除 `bc-evaluation-contract` → `eva-client` 直依赖，并补齐 `bc-iam-contract` 的显式依赖（保持行为不变；`cf2001ef`）
- P1.1：已迁移 `edu.cuit.client.dto.clientobject.eva.*` + `UnqualifiedUserConditionalQuery` 到 `bc-evaluation/contract`（保持 `package` 不变）；并迁移评教 API 接口（`IEvaConfigService/IEvaRecordService/IEvaTaskService/IEvaTemplateService/IUserEvaService`）到 `bc-evaluation/contract`（保持 `package` 不变）；并将课程域协议接口与 CO（`edu.cuit.client.api.course.*`、`CourseDetailCO/CourseModelCO/SingleCourseDetailCO`）迁移到 `bc-course`（保持行为不变；`6eb0125d`）
- 结构性里程碑 S0：`bc-iam` 已折叠为 `bc-iam-parent` + `domain/application/contract/infrastructure` 子模块，且 `bc-iam-infra` 已折叠归位到 `bc-iam/infrastructure`（`0b5c5383`）
- 结构性里程碑 S0.1：`eva-client` 下 IAM 协议对象（`api/user/*` + `dto/cmd/user/*`）已迁移到 `bc-iam/contract`（`dc3727fa`）
- 结构性里程碑 S0：`bc-evaluation` 已折叠为 `bc-evaluation-parent` + `domain/application/contract/infrastructure` 子模块，且 `bc-evaluation-infra` 已折叠归位到 `bc-evaluation/infrastructure`（`4db04d1c`）
- 结构性里程碑 S0.1：已迁移 IAM `dto/clientobject/user/*` 中与 IAM 直接相关的 8 个 CO 到 `bc-iam-contract`（包名归位；`c1a51199`）
- P1：已新增 `bc-evaluation-contract` 并迁移 `IEvaStatisticsService` + `UnqualifiedUserInfoCO/UnqualifiedUserResultCO` 到 `bc-evaluation/contract`（保持 `package` 不变；`978e3535`）

下一步提交点（建议优先级）：
1) **S0.1（下一步优先：收敛 `eva-domain` → `eva-client` 依赖）**：
   - ✅ 进展：已移除 `eva-domain` → `eva-client` Maven **直依赖**（`9ff21249`）。
   - 下一步建议（保持行为不变；每步可回滚）：继续用 Serena 盘点 `eva-domain` 中 `import edu.cuit.client.*` 的类型清单，并逐一核对“类型来源是否已在 `shared-kernel` / 各 BC contract / `eva-domain`”。若发现仍有未归位的小簇类型，则按业务归属迁移到对应 BC（例如 `bc-course`/`bc-messaging`/`bc-ai-report`）或 `shared-kernel`（每步=最小回归+提交+三文档同步）。
2) **S0.1（IAM 继续推进）**：继续迁移 IAM 专属 query/condition/CO（保持行为不变；避免新代码回流 `eva-client`；必要时先用 Serena 盘点残留引用面再决定迁移/沉淀）。
   - ✅ 已完成：移除 `bc-iam/application` → `eva-client` 的直依赖（保持行为不变；`7371ab96`）。
   - ✅ 已完成：迁移 IAM 专属接口 `IDepartmentService` 从 `eva-client` 到 `bc-iam-contract`（包名归位到 `edu.cuit.bc.iam.application.contract.api.department`；保持行为不变；`656dc36e`）。
   - 下一步：Serena 继续盘点 IAM 模块引用的 `edu.cuit.client.*` 类型范围；若为 IAM 专属则迁移到 `bc-iam-contract`（或更合适的 BC），若为跨 BC 通用则继续沉淀到 `shared-kernel`（保持行为不变）。
3) ✅ **P1.2（评教域继续拆 `eva-client`，盘点）**：已闭环：评教专属目录已迁空；后续更可能只剩“依赖面收敛”推进（保持行为不变；`e643bac9`）。
4) **条目 25（后续）**：AI 报告继续挑选剩余写链路（保存/落库/记录等）按同套路收敛（保持行为不变；参考 `docs/DDD_REFACTOR_BACKLOG.md` 第 6 节）。

当前 `eva-client` 状态（以 Git 为准）：
- `eva-client/` 目录已从仓库移除（S0.1 已闭环；如需回滚通过 Git 提交点即可）。

每步最小回归命令（每步结束都跑）：
export JAVA_HOME=\"$HOME/.sdkman/candidates/java/17.0.17-zulu\" && export PATH=\"$JAVA_HOME/bin:$PATH\" \\
&& mvnd -pl start -am test \\
-Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest \\
-Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository

工具调用简报（用于继续 S0.1 的引用盘点）：
- Serena `search_for_pattern`：用于快速盘点跨 BC/跨模块引用（例如 `import\\s+edu\\.cuit\\.client\\.`），以便形成“迁移清单”（日期=2025-12-26）。
- Serena `find_symbol`/`find_referencing_symbols`：用于对目标类型（Query/CO/Cmd/Data/Validator）做符号级定位与引用分析（确保“只重构不改行为”）。
- Serena `find_referencing_symbols`：本次已用于盘点 `SemesterCO/UpdateCourseCmd/CourseExcelBO` 的跨模块引用面（日期=2025-12-26）。

## 0.7 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ 系统管理写侧继续收敛：**角色写侧剩余入口**收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 收敛范围（本次条目 24）：`RoleUpdateGatewayImpl.updateRoleInfo/updateRoleStatus/deleteRole/createRole`
  - 行为快照（供回归对照，顺序必须保持）：  
    - `updateRoleInfo`：`checkRoleId` →（`cmd.getStatus() == 0` 时）`checkDefaultRole` → `roleMapper.updateById` → `handleRoleUpdateCache` → `LogUtils.logContent(tmp.getRoleName() + "角色(" + tmp.getId() + ")的信息")`
    - `updateRoleStatus`：`checkRoleId` → `checkDefaultRole` → `roleMapper.update(roleUpdate)` → `handleRoleUpdateCache` → `LogUtils.logContent(tmp.getRoleName() + " 角色(" + tmp.getId() + ")的状态")`
    - `deleteRole`：`checkRoleId` → `checkDefaultRole` → `roleMapper.deleteById` → `userRoleMapper.delete` → `roleMenuMapper.delete` → `handleRoleUpdateCache` → `LogUtils.logContent(tmp.getRoleName() + "角色(" + tmp.getId() + ")")`
    - `createRole`：`roleConverter.toRoleDO` →（若重名）`throw BizException("角色名称已存在")` → `roleMapper.insert` → `handleRoleUpdateCache(roleId)`
    - `handleRoleUpdateCache`：依次失效 `ALL_ROLE/ONE_ROLE/ROLE_MENU`，再按 `sys_user_role.role_id -> user_id` 失效 `ONE_USER_ID/ONE_USER_USERNAME`（用户名 key 允许 `null`）。
  - 关键落地点（便于快速定位）：
    - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/UpdateRoleInfoUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/UpdateRoleStatusUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/DeleteRoleUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/CreateRoleUseCase.java`
    - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RoleInfoUpdatePort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RoleStatusUpdatePort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RoleDeletionPort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RoleCreationPort.java`
    - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/RoleWritePortImpl.java`
    - 旧 gateway（已退化委托壳）：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/RoleUpdateGatewayImpl.java`
    - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`
  - 落地提交：`64fadb20`
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

## 0.8 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ 中期里程碑推进：引入 `bc-iam-infra` Maven 子模块骨架，并完成 `bciam/adapter/*` 端口适配器迁移（保持行为不变）。
  - 组合根：`eva-app` 已依赖 `bc-iam-infra`，确保后续迁移的 Spring Bean 仍在运行时 classpath 上。
  - 落地提交：`42a6f66f`
- ✅ 迁移第一步：`UserBasicQueryPortImpl` 已从 `eva-infra` 迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 落地提交：`070068ec`
- ✅ 迁移第二步：`RoleWritePortImpl` 已从 `eva-infra` 迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 落地提交：`03ceb685`
- ✅ 迁移第三步：`UserMenuCacheInvalidationPortImpl` 已从 `eva-infra` 迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 落地提交：`02b3e8aa`
- ✅ 迁移第四步：`MenuWritePortImpl` 已从 `eva-infra` 迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 落地提交：`6b9d2ce7`
- ✅ 迁移第五步：用户写侧端口适配器已分批迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 范围：`UserCreationPortImpl/UserInfoUpdatePortImpl/UserStatusUpdatePortImpl/UserDeletionPortImpl`
  - 落地提交：`5aecc747`
- ✅ 迁移第六步：用户查询/分配端口适配器已迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 范围：`UserRoleAssignmentPortImpl/UserDirectoryQueryPortImpl/UserEntityQueryPortImpl`
  - 落地提交：`1c3d4b8c`
  - 下一步拆分与里程碑/提交点（每步一条 commit；每步跑最小回归；每步完成更新三份文档）：
    1) Serena：确认 `eva-infra/src/main/java/edu/cuit/infra/bciam/adapter` 不再残留 IAM 端口适配器（以及是否存在旧的 Spring 配置硬引用）。
    2) 验证：补充一次“启动期校验”（可选，若不跑全量启动则至少保持最小回归通过）。
    3) 下一阶段（后置）：评估将 IAM DAL（DO/Mapper）从 `eva-infra` 拆出到 `bc-iam-infra` 或独立 `shared-infra-dal`（仍保持行为不变、先搬运后整理）。

## 0.6 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ 系统管理写侧继续收敛：**菜单写侧主链路**收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 收敛范围（本次条目 23）：`MenuUpdateGatewayImpl.updateMenuInfo/deleteMenu/deleteMultipleMenu/createMenu`
  - 行为快照（供回归对照）：
    - 缓存注解不变：`@LocalCacheInvalidateContainer/@LocalCacheInvalidate` 仍保留在旧 `MenuUpdateGatewayImpl` 方法上，area/key 与触发时机不变。
    - `deleteMenu` 链路不变：递归删除顺序不变；根节点 `handleUserMenuCache(menuId)` 仍触发两次（一次来自递归末尾、一次来自 `deleteMenu` 方法末尾）。
    - 异常/日志不变：`BizException("父菜单ID: ... 不存在")`、`BizException("菜单id: ... 不存在")`；`LogUtils.logContent(...)` 文案与时机不变。
  - 关键落地点（便于快速定位）：
    - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/UpdateMenuInfoUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/DeleteMenuUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/DeleteMultipleMenuUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/CreateMenuUseCase.java`
    - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/MenuInfoUpdatePort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/MenuDeletionPort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/MenuBatchDeletionPort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/MenuCreationPort.java`
    - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/MenuWritePortImpl.java`
    - 旧 gateway（已退化委托壳）：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/MenuUpdateGatewayImpl.java`
    - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`
  - 落地提交：`f022c415`
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

## 0.5 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ 系统管理写侧继续收敛：**角色/菜单缓存与权限变更副作用**收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 收敛范围（本次条目 21）：`RoleUpdateGatewayImpl.assignPerms/deleteMultipleRole`、`MenuUpdateGatewayImpl.handleUserMenuCache`
  - 落地提交链：`f8838951/91d13db4/7fce88b8/d6e3bed1/4d650166/46e666f9`
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

## 0.4 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ 系统管理读侧继续收敛：`UserQueryGatewayImpl.findIdByUsername/findUsernameById/getUserStatus/isUsernameExist` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`9f664229/38384628/de662d1c/8a74faf5`
- ✅ 系统管理读侧继续收敛：`UserQueryGatewayImpl.findAllUserId/findAllUsername/allUser/getUserRoleIds` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`
- ✅ 系统管理写侧继续收敛：角色/菜单缓存与权限变更副作用收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`f8838951/91d13db4/7fce88b8/d6e3bed1/4d650166/46e666f9`
- ✅ 文档同步（交接/计划/Backlog）：
  - `15cbfefa/c312575c/c641f86b/f8838951`
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

- ✅ 条目 26（`bc-iam-infra` 阶段 2：IAM DAL 抽离）已完成：已用 Serena 盘点 IAM DAL 依赖清单并完成迁移闭环，最终移除 `bc-iam-infra` → `eva-infra` 依赖（保持行为不变）。
  - **Mapper（当前仍在 `eva-infra-dal`）**：`SysUserMapper`
  - **DO（当前仍在 `eva-infra-dal`）**：无（补充：`SysUserDO/SysRoleDO/SysMenuDO` 已下沉到 `shared-kernel`，落地：`31e157cd`/`c5ba98b1`/`a9141bfe`）
  - **XML（当前仍在 `eva-infra-dal`）**：`eva-infra-dal/src/main/resources/mapper/user/SysUserMapper.xml`
  - 依赖来源（便于后续搬迁对照）：Mapper/DO 均在 `eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/(mapper|dataobject)/user/`（部分已进一步归位到 `bc-iam/infrastructure`）；XML 均在 `eva-infra-dal/src/main/resources/mapper/user/`
  - ✅ 已完成（条目 26-2）：在 `bc-iam-infra` 创建 DAL 包骨架与资源目录（不迁代码；仅作为后续迁移落点；保持行为不变）。
    - Java 包骨架：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/dataobject/user/package-info.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/package-info.java`
    - 资源目录占位：`bc-iam/infrastructure/src/main/resources/mapper/user/.gitkeep`
  - ✅ 已完成（条目 26-3）：新增独立 DAL 子模块 `eva-infra-dal`，并先迁移 `SysUser*`（DO/Mapper/XML）到该模块（保持包名/namespace/SQL 不变；保持行为不变）。
    - 说明：Serena 引用分析确认 `SysUserMapper` 被 `eva-infra` 内多个模块（course/eva/log/department…）直接使用；若直接迁入 `bc-iam-infra` 并从 `eva-infra` 删除会引入 Maven 循环依赖风险，因此先抽离为共享 DAL 模块以最小可回滚方式推进。
    - 新模块：`eva-infra-dal/pom.xml`
    - Java：`shared-kernel/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysUserDO.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysUserRoleDO.java`；`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysUserMapper.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysUserRoleMapper.java`
    - XML：`eva-infra-dal/src/main/resources/mapper/user/SysUserMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysUserRoleMapper.xml`
  - ✅ 已完成（条目 26-4）：继续迁移 `SysRole*`/`SysRoleMenu*`（DO/Mapper/XML）到 `eva-infra-dal`（保持包名/namespace/SQL 不变；保持行为不变）。
    - Java：`shared-kernel/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysRoleDO.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysRoleMenuDO.java`；`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysRoleMapper.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysRoleMenuMapper.java`
    - XML：`bc-iam/infrastructure/src/main/resources/mapper/user/SysRoleMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysRoleMenuMapper.xml`
  - ✅ 已完成（条目 26-5）：继续迁移 `SysMenu*`（DO/Mapper/XML）到 `eva-infra-dal`（保持包名/namespace/SQL 不变；保持行为不变）。
    - Java：`shared-kernel/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysMenuDO.java`；`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysMenuMapper.java`
    - XML：`bc-iam/infrastructure/src/main/resources/mapper/user/SysMenuMapper.xml`
  - 下一步里程碑（每步一条 commit；每步跑最小回归；保持行为不变）：逐步把 `bc-iam-infra` 对 `eva-infra` 的依赖收敛为更小的 shared 模块集合（最终可移除）
	    - ✅ 已完成（条目 26-6-1）：盘点 `bc-iam-infra` 仍依赖 `eva-infra` 的类型清单（为后续去依赖做最小闭包拆分；保持行为不变）：
	      - 转换器：`edu.cuit.infra.convertor.PaginationConverter`（已迁至 `eva-infra-shared`，落地：`54d5fecd`）、`edu.cuit.infra.convertor.user.{MenuConvertor,RoleConverter,UserConverter}`（已迁至 `eva-infra-shared`，落地：`6c798f1b`）、`edu.cuit.infra.convertor.user.LdapUserConvertor`（已迁至 `eva-infra-shared`，落地：`0dc0ddc8`）
	      - 缓存常量：`edu.cuit.infra.enums.cache.{UserCacheConstants,CourseCacheConstants}`
	      - LDAP 相关：`edu.cuit.infra.dal.ldap.{dataobject,repo}.*`（已迁至 `eva-infra-shared`，落地：`aca70b8b`；后续已继续下沉到 `shared-kernel`，保持包名不变）、`edu.cuit.infra.util.EvaLdapUtils` + `edu.cuit.infra.{enums.LdapConstants,property.EvaLdapProperties}`（已迁至 `eva-infra-shared`，落地：`3165180c`；后续已继续下沉到 `shared-kernel`，保持包名不变）
	      - 查询工具：`edu.cuit.infra.util.QueryUtils`
    - ✅ 已完成（条目 26-6-2a）：新增 shared 子模块骨架 `eva-infra-shared`（不迁代码，仅作为后续抽离落点；保持行为不变）。
      - 新模块：`eva-infra-shared/pom.xml`
    - ✅ 已完成（条目 26-6-2b1）：迁移 IAM 相关缓存常量到 `eva-infra-shared`（保持包名不变；保持行为不变）。
      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/enums/cache/UserCacheConstants.java`、`CourseCacheConstants.java`
      - 依赖：`eva-infra-shared/pom.xml` 增加 `spring-context`（保持 `@Component` 语义不变）；`eva-infra/pom.xml` 与 `bc-iam-infra/pom.xml` 增加对 `eva-infra-shared` 的依赖
    - ✅ 已完成（条目 26-6-2b2）：迁移查询工具 `QueryUtils` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/util/QueryUtils.java`
      - 依赖：`eva-infra-shared/pom.xml` 增加对 `eva-client` 与 `zym-spring-boot-starter-jdbc` 的依赖（保持 MyBatis-Plus QueryWrapper/Page 等 API 不变）
	    - ✅ 已完成（条目 26-6-2c1）：迁移 `EntityFactory` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
	      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/EntityFactory.java`
	      - 后续（2026-02-12，保持行为不变）：已从 `eva-infra-shared` 下沉到 `eva-infra-dal`（Java：`eva-infra-dal/src/main/java/edu/cuit/infra/convertor/EntityFactory.java`；落地：`eba15e92`）。
	      - 依赖：`eva-infra-shared/pom.xml` 增加 `mapstruct-plus-spring-boot-starter`；并增加对 `eva-domain` 的依赖以保留 `hutool SpringUtil` 与 `cola SysException` 的依赖来源（行为不变）
	    - ✅ 已完成（条目 26-6-2c2）：迁移 `PaginationConverter` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
	      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/PaginationConverter.java`
	      - 依赖：`eva-infra-shared/pom.xml` 增加对 `eva-infra-dal` 的依赖以保持编译闭包（行为不变）
	      - 落地提交：`54d5fecd`
	    - ✅ 已完成（条目 26-6-2c3）：迁移 `convertor.user` 的非 LDAP 转换器到 `eva-infra-shared`（保持包名不变；保持行为不变）。
	      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/user/{MenuConvertor,RoleConverter,UserConverter}.java`
	      - 落地提交：`6c798f1b`
	    - ✅ 已完成（条目 26-6-2d1）：迁移 LDAP DO/Repo 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
	      - Java：`shared-kernel/src/main/java/edu/cuit/infra/dal/ldap/dataobject/LdapGroupDO.java`、`shared-kernel/src/main/java/edu/cuit/infra/dal/ldap/repo/LdapGroupRepo.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/ldap/dataobject/LdapPersonDO.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/ldap/repo/LdapPersonRepo.java`
	      - 依赖：`eva-infra-shared/pom.xml` 增加 `spring-boot-starter-data-ldap`
	      - 落地提交：`aca70b8b`
	    - ✅ 已完成（条目 26-6-2d2）：迁移 `EvaLdapUtils` 相关类型到 `eva-infra-shared`（保持包名不变；保持行为不变）。
		      - Java：`shared-kernel/src/main/java/edu/cuit/infra/util/EvaLdapUtils.java`（后续下沉 shared-kernel：`05cc3039`）、`shared-kernel/src/main/java/edu/cuit/infra/enums/LdapConstants.java`（后续下沉 shared-kernel：`3dc2e8ff`）、`shared-kernel/src/main/java/edu/cuit/infra/property/EvaLdapProperties.java`（后续下沉 shared-kernel：`666a1b6d`）
	      - 落地提交：`3165180c`
	    - ✅ 已完成（条目 26-6-2d3）：迁移 `LdapUserConvertor` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
	      - Java：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/convertor/user/LdapUserConvertor.java`
	      - 落地提交：`0dc0ddc8`
	    - 条目 26-6-2 拆分提交点（均已完成；每步均跑最小回归；保持行为不变）：
	      - ✅ 条目 26-6-2d1：迁移 `edu.cuit.infra.dal.ldap.*`（Repo + DO）到 `eva-infra-shared`（保持包名不变；落地：`aca70b8b`）
		      - ✅ 条目 26-6-2d2：迁移 `edu.cuit.infra.util.EvaLdapUtils` + `edu.cuit.infra.{enums.LdapConstants,property.EvaLdapProperties}` 到 `eva-infra-shared`（保持包名不变；落地：`3165180c`；后续 `EvaLdapProperties` 已继续下沉到 `shared-kernel`：`666a1b6d`）
	      - ✅ 条目 26-6-2d3：迁移 `edu.cuit.infra.convertor.user.LdapUserConvertor` 到 `eva-infra-shared`（保持包名不变；落地：`0dc0ddc8`）
	    - ✅ 已完成（条目 26-6-3）：移除 `bc-iam-infra` 对 `eva-infra` 的 Maven 依赖（保持行为不变）。
	      - 依赖：`bc-iam-infra/pom.xml` 移除 `eva-infra`，补齐 `zym-spring-boot-starter-cache` + `zym-spring-boot-starter-logging` 编译依赖（行为不变）
	      - 落地提交：`2ad911ea`

## 0.3 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ IAM 域写侧继续收敛：`UserUpdateGatewayImpl.deleteUser` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`5f08151c/e23c810a/cccd75a3/2846c689`
- ✅ 系统管理读侧渐进收敛：`UserQueryGatewayImpl.fileUserEntity` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`3e6f2cb2/8c245098/92a9beb3`（文档同步：`0a7802e2`）
  - 行为快照（供回归对照）：
    - 异常不变：菜单查询仍可能抛出 `SysException("菜单查询出错，请联系管理员")`，且仍会 `log.error("菜单查询出错", sysException)`；
    - 缓存不变：`findById/findByUsername/page` 的 `@LocalCached` 仍保留在旧 `UserQueryGatewayImpl` 上，缓存命中/回源语义不变；
    - API 不变：对外仍通过 `UserQueryGateway` 暴露 `findById/findByUsername/page`。
- ✅ 系统管理读侧继续收敛：`UserQueryGatewayImpl.findIdByUsername/findUsernameById/getUserStatus/isUsernameExist` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`9f664229/38384628/de662d1c/8a74faf5`
  - 行为快照（供回归对照）：
    - 缓存读取语义不变：仍优先读取 `LocalCacheManager.getCache`，area 分别为 `ONE_USER_USERNAME/ONE_USER_ID`，key 分别为 `username` 与 `String.valueOf(id)`；
    - `findIdByUsername/findUsernameById` 返回语义不变：缓存命中则 `Optional.ofNullable(cachedUser.get().getId()/getUsername())`；缓存未命中（含 `null` 或 `Optional.empty()`）则回源 DB 并保持 `Optional.ofNullable(selectOne(...)).map(...)`；
    - `getUserStatus` 返回语义不变：缓存命中则 `Optional.ofNullable(cachedUser.get().getStatus())`；缓存未命中回源 DB 并保持 `Optional.ofNullable(selectOne(...).getStatus())`（用户不存在时仍可能触发历史 NPE）；
    - `isUsernameExist` 语义不变：缓存命中直接返回 `true`；否则走 `userMapper.exists(...)`。
- ✅ 系统管理读侧继续收敛：`UserQueryGatewayImpl.findAllUserId/findAllUsername/allUser/getUserRoleIds` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`
  - 行为快照（供回归对照）：
    - 缓存不变：`@LocalCached` 仍保留在旧 `UserQueryGatewayImpl` 上（key/area 与命中语义不变）。
    - 查询字段不变：`findAllUserId` 仅 select `id`；`findAllUsername` 仅 select `username`；`allUser` 仅 select `id,name` 并沿用 `userConverter.toUserSimpleResult` 映射。
    - 角色查询不变：`getUserRoleIds` 仍为原 join 条件与返回映射（仅搬运到端口适配器）。
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

## 0.2 本次会话增量总结（2025-12-20，更新至 `HEAD`）

- ✅ 评教模板新增/修改写侧收敛到 `bc-evaluation`（新增用例 + 端口 + `eva-infra` 端口适配器，并切换 `eva-app` 入口；行为不变）。
  - 落地提交：`ea03dbd3 refactor(evaluation): 评教模板新增/修改写侧收敛到bc-evaluation`
- ✅ 清理旧 `EvaUpdateGatewayImpl.putEvaTemplate` 遗留实现（避免提交评教写侧旧代码回潮；行为不变）。
  - 落地提交：`12279f3f refactor(evaluation): 清理旧EvaUpdateGatewayImpl提交评教遗留实现`
- ✅ 消息域写侧阶段性收敛：先收敛“删除/已读”（保持行为不变）。
  - 消息删除：收敛到 `bc-messaging`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 `MsgGatewayImpl` 委托壳；提交：`22cb60eb`）。
  - 消息已读：收敛到 `bc-messaging`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 `MsgGatewayImpl` 委托壳；提交：`dd7483aa`）。
- ✅ 消息域读侧进一步收敛：收敛 `queryMsg/queryTargetAmountMsg` 到 `bc-messaging`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 `MsgGatewayImpl` 委托壳；保持行为不变）。
  - 落地提交：`05a2381b`
- ✅ 消息域写侧进一步收敛：收敛 `insertMessage` 到 `bc-messaging`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 `MsgGatewayImpl` 委托壳；保持行为不变）。
  - 落地提交：`8445bc41`
- ✅ 消息域写侧进一步收敛：收敛 `updateMsgDisplay` 到 `bc-messaging`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 `MsgGatewayImpl` 委托壳；保持行为不变）。
  - 落地提交：`c315fa22`
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvnd -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`
- ✅ 课程域查询/校验进一步收敛：`CourseUpdateGatewayImpl.isImported` 收敛到 `bc-course`（保持行为不变）。
  - 落地：`bc-course` 新增 QueryPort + UseCase；`eva-infra` 新增端口适配器原样搬运旧查询逻辑；旧 `CourseUpdateGatewayImpl.isImported` 退化为委托壳（落地提交：`4ed055a2/495287c8/f3e8e3cc`）。
- ✅ IAM 域写侧部分收敛：`UserUpdateGatewayImpl.assignRole` 收敛到 `bc-iam`（保持行为不变）。
  - 落地：`bc-iam` 新增用例 + 端口；`eva-infra` 新增端口适配器原样搬运旧写流程；旧 `UserUpdateGatewayImpl.assignRole` 退化为委托壳（落地提交：`16ff60b6/b65d311f/a707ab86`）。
- ✅ IAM 域写侧继续收敛：`UserUpdateGatewayImpl.createUser` 收敛到 `bc-iam`（保持行为不变）。
  - 落地：`bc-iam` 新增 `CreateUserUseCase` + `UserCreationPort` 与纯单测；`eva-infra` 新增 `UserCreationPortImpl` 端口适配器原样搬运旧流程；`eva-app` 组合根装配 Bean；旧 `UserUpdateGatewayImpl.createUser` 退化为委托壳（落地提交：`c3aa8739/a3232b78/a26e01b3/9e7d46dd`）。
- ✅ IAM 域写侧继续收敛：`UserUpdateGatewayImpl.updateInfo` 收敛到 `bc-iam`（保持行为不变）。
  - 落地：`bc-iam` 新增 `UpdateUserInfoUseCase` + `UserInfoUpdatePort` 与纯单测；`eva-infra` 新增 `UserInfoUpdatePortImpl` 端口适配器原样搬运旧流程；`eva-app` 组合根装配 Bean；旧 `UserUpdateGatewayImpl.updateInfo` 退化为委托壳（落地提交：`38c31541/6ce61024/db0fd6a3/cb789e21`）。
- ✅ IAM 域写侧继续收敛：`UserUpdateGatewayImpl.updateStatus` 收敛到 `bc-iam`（保持行为不变）。
  - 落地：`bc-iam` 新增用例 + 端口；`eva-infra` 新增端口适配器原样搬运旧流程（DB 更新 → 缓存失效 → 日志）；`eva-app` 组合根装配 Bean；旧 `UserUpdateGatewayImpl.updateStatus` 退化为委托壳（落地提交：`e3fcdbf0/8e82e01f/eb54e13e`）。
  - 行为快照（供回归对照）：
    - 异常不变：仍可能抛出 `BizException("初始管理员账户不允许此操作")`、`BizException("用户id不存在")`；
    - 副作用顺序不变：DB 更新 → 缓存失效（沿用 `handleUserUpdateCache` 清单）→ 日志（`LogUtils.logContent`）；
    - 事务边界不变：仍由 `UserServiceImpl.updateStatus` 的 `@Transactional` 承载；
    - `StpUtil.logout` 时机不变：仍在 gateway 调用之后执行（且仍依赖 `userQueryGateway.findUsernameById(...).orElseThrow(() -> new BizException("用户ID不存在"))` 的异常文案）。

## 0.1 本次会话增量总结（2025-12-19，更新至 `HEAD`）

- ✅ 评教任务发布写侧收敛到 `bc-evaluation`（用例 + 端口 + 旧 gateway 委托壳），并将“待办消息发送”改为事务提交后事件触发，避免回滚误推。
- ✅ 评教删除写侧收敛到 `bc-evaluation`（删除评教记录/模板两条写链路；旧 gateway 退化委托壳；行为不变）。
- ✅ 课程读侧渐进收敛：为 `CourseQueryGatewayImpl` 引入 QueryRepo（gateway 退化委托壳，行为不变）。
- ✅ 评教读侧渐进收敛：为 `EvaQueryGatewayImpl` 引入 `EvaQueryRepo`（gateway 退化委托壳，行为不变）。
- ✅ 评教读侧用例级回归测试：固化统计口径（`EvaStatisticsServiceImpl` / `EvaRecordServiceImpl`）。
- ✅ 测试稳定化：将依赖本地文件/外部服务的 `start` 测试改为内存回归用例，移除 Spring 上下文依赖。
- ✅ 评教读侧进一步解耦（统计/导出）：新增 `EvaStatisticsQueryPort`，应用层统计与导出查询改走端口；`eva-infra` 端口实现委托 `EvaQueryRepo`，行为保持不变。
- ✅ 评教读侧进一步解耦（任务查询）：新增 `EvaTaskQueryPort`，应用层任务查询改走端口；`eva-infra` 端口实现委托 `EvaQueryRepo`，行为保持不变。
- ✅ 评教读侧进一步解耦（记录/模板查询）：新增 `EvaRecordQueryPort` / `EvaTemplateQueryPort`，应用层记录与模板查询改走端口；统计导出补齐 `getCountAbEva` 统一由 `EvaStatisticsQueryPort` 提供，行为保持不变。
- ✅ `bc-evaluation` 补齐 `eva-client`/`eva-domain` 依赖，解决新 QueryPort 编译缺失问题。
- ✅ `bc-evaluation` 内部依赖补齐版本号（`eva-client`/`eva-domain`），修复 Maven 构建缺失版本报错。
- ✅ 修正 `EvaStatisticsQueryPort` 中 DTO 包名（`PastTimeEvaDetailCO` / `ScoreRangeCourseCO`）以通过编译。
- ✅ 修正 `EvaStatisticsQueryPortImpl` 中 DTO 包名，确保 `eva-infra` 编译通过。
- ✅ 旧 `EvaQueryGatewayImpl` 进一步收敛为 QueryPort 委托（任务/记录/模板/统计），避免直接依赖 `EvaQueryRepo`。
- ✅ 最小回归已通过并完成复验（同上命令）。
- ✅ 旧 `EvaQueryGateway` / `EvaQueryGatewayImpl` 已移除（应用层已完全切换到 QueryPort）。
- 新增提交（按时间顺序）：
  - `8e434fe1 feat(bc-evaluation): 增加评教任务发布用例骨架`
  - `ca69b131 feat(eva-infra): 实现评教任务发布端口适配器`
  - `e9043f96 refactor(eva-app): 评教任务发布收敛到bc-evaluation`
  - `ea928055 feat(bc-evaluation): 增加评教删除用例骨架`
  - `07b65663 feat(eva-infra): 实现评教删除端口适配器`
  - `05900142 refactor(eva-app): 评教删除写侧收敛到bc-evaluation`
  - `cc9af9ad docs: 更新交接与计划书（评教任务发布收敛）`
  - `76a89b78 docs: 更新交接与计划书（评教删除收敛）`
  - `ba8f2003 refactor(eva-infra): 课程读侧抽取QueryRepo`
  - `02f4167d refactor(eva-infra): 评教读侧抽取QueryRepo`
  - `a48cf044 test(eva-app): 补充评教读侧用例级回归`
  - `daf343ef test(start): 稳定化回归用例并去除外部依赖`
  - `3b215441 docs: 更新交接与计划书（测试稳定化）`
  - `daf343ef test(start): 稳定化回归用例并去除外部依赖`
  - `bb391698 refactor(evaluation): 拆分统计查询端口`
  - `570fecb6 refactor(evaluation): 拆分评教任务查询端口`
  - `ec5191a3 refactor(evaluation): 拆分评教记录与模板查询端口`
  - `6029a56e chore(bc-evaluation): 补齐查询端口依赖`
  - `9efe7b12 chore(bc-evaluation): 补齐内部依赖版本`
  - `198746fc fix(bc-evaluation): 修正统计查询端口DTO包名`
  - `cf493ef2 fix(eva-infra): 修正统计查询端口实现DTO包名`
  - `3e5da427 refactor(evaluation): 旧EvaQueryGateway委托到细分端口`
  - `c0d0b31a docs: 更新交接与Backlog（旧网关收敛）`
  - `9ab6512d docs: 更新回归结果`
  - `5c74083e refactor(evaluation): 移除旧EvaQueryGateway`
  - `49b80e0f docs: 更新交接与计划书（旧网关移除）`
  - `dd5245ed docs: 更新交接提交清单`
  - `35fb9e69 docs: 更新最小回归与规则`

## 0. 本轮会话增量总结（2025-12-18，更新至 `HEAD`，以 `git log -n 1` 为准）

本轮会话聚焦“DDD 渐进式重构（不做功能优化/不改业务语义）”，继续执行方案 B/C，并持续压扁课程相关的大泥球入口：

**本会话铁律（验收标准）**
- ✅ 只做重构：收敛调用链、抽离端口/用例、让旧 gateway 退化委托壳、事件化副作用；
- ✅ 行为不变：API 不变、异常类型/异常文案不变、消息文案与副作用时机（事务提交后）不变；
- ❌ 不做任何业务逻辑优化/语义调整（即便看到明显 bug/命名不佳，也先不动）。

**新增规则（给下一个会话）**
- ✅ 每个步骤结束必须跑最小回归用例（`EvaRecordServiceImplTest` / `EvaStatisticsServiceImplTest`）。

1) **课表导入/覆盖用例收敛到 bc-course（闭环 E）**  
   - 背景：历史实现中 `IUserCourseServiceImpl.importCourse()` 既做导入又做消息通知/撤回评教消息，属于典型“大泥球联动”。
   - 做法：把导入课表的核心写操作收敛到 `bc-course` 用例，把跨域副作用统一事件化交给 `bc-messaging`。
   - 关键改动点：
     - 用例与端口：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/ImportCourseFileUseCase.java`
     - infra 端口实现：`bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/ImportCourseFileRepositoryImpl.java`
     - 旧 gateway 退化委托壳：`bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`（`importCourseFile`）
     - 应用层入口事件化：`eva-app/src/main/java/edu/cuit/app/service/impl/course/IUserCourseServiceImpl.java`（`importCourse`）

2) **跨域副作用继续“事务提交后发布”统一化**  
   - 通过 `eva-app/src/main/java/edu/cuit/app/event/AfterCommitEventPublisher.java` 把事件发布固定为“提交后再执行副作用”，避免回滚不一致。
   - 导入课表复用现有 `CourseOperationSideEffectsEvent` + `bc-messaging` 用例处理（通知/撤回评教消息）。

3) **文档沉淀**  
   - 简历可用沉淀：`data/RESUME_BALL_OF_MUD_REFACTOR_SUMMARY.md`（新增了“课表导入/覆盖收敛”案例）。
   - 交接文档：本文件已补充闭环 E 与下一步行动建议。

4) **冲突校验收敛（教室占用/时间段重叠，保持行为不变）**
   - 背景：教室占用/时间段重叠的 QueryWrapper 片段在多个端口适配器里重复，容易出现 SQL 条件漂移与异常文案不一致。
   - 做法：提炼可复用的“底层校验/查询片段”，用最小改动迁移调用点；并清理已无引用的旧 gateway 遗留私有逻辑，防止回潮。
   - 落地：
     - `ClassroomOccupancyChecker`：统一“教室占用冲突”校验（异常文案保持不变）
     - `CourInfTimeOverlapQuery`：统一“时间段重叠”查询片段（`start_time <= end && end_time >= start`，保持 SQL 条件不变）

5) **教师自助课表：副作用事件化 + 主写逻辑收敛（保持行为不变）**  
   - 背景：历史实现中 `IUserCourseServiceImpl.deleteSelfCourse()/updateSelfCourse()` 同时包含：
     - 主写逻辑（删课/改课，涉及课程/课次/评教任务/缓存）；
     - 跨域副作用（消息通知、撤回评教消息）；
     - 造成应用服务“大泥球联动”与难以拆分。
   - 落地（两段式，小步提交）：
     1. **事件化跨域副作用（事务提交后发布）**：
        - `CourseOperationSideEffectsEvent` 增加 `messageMode`（`NORMAL` / `TASK_LINKED`）用于兼容历史消息模型差异：
          - `NORMAL`：历史上对应 `MsgResult.toNormalMsg`（taskId 固定 -1）；
          - `TASK_LINKED`：历史上对应 `MsgResult.toSendMsg`（taskId = map key）。
        - usecase `HandleCourseOperationSideEffectsUseCase` 根据 `messageMode` 路由到 `CourseBroadcastPort.sendNormal/sendTaskLinked`，并保持“撤回评教消息”逻辑不变。
        - 关键文件：
          - `bc-messaging-contract/src/main/java/edu/cuit/bc/messaging/application/event/CourseOperationMessageMode.java`
          - `bc-messaging-contract/src/main/java/edu/cuit/bc/messaging/application/event/CourseOperationSideEffectsEvent.java`
          - `bc-messaging/src/main/java/edu/cuit/bc/messaging/application/usecase/HandleCourseOperationSideEffectsUseCase.java`
          - `eva-app/src/main/java/edu/cuit/app/bcmessaging/adapter/CourseBroadcastPortAdapter.java`
          - `eva-app/src/main/java/edu/cuit/app/service/impl/course/IUserCourseServiceImpl.java`（删课=默认 NORMAL；改课=TASK_LINKED）
     2. **主写逻辑收敛到 bc-course 用例**（仍返回历史 `Map<String, Map<Integer,Integer>>`，以便复用事件化副作用）：
        - 自助删课：`DeleteSelfCourseUseCase` + `DeleteSelfCourseRepositoryImpl`，旧 `CourseDeleteGatewayImpl.deleteSelfCourse` 退化为委托壳；
        - 自助改课：`UpdateSelfCourseUseCase` + `UpdateSelfCourseRepositoryImpl`，旧 `CourseUpdateGatewayImpl.updateSelfCourse` 退化为委托壳；
        - 迁移内容包含：原先 gateway 内部的课程/课次/评教任务/缓存失效逻辑，全部按原样搬运（不做优化）。
        - 关键文件：
          - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/DeleteSelfCourseUseCase.java`
          - `bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/DeleteSelfCourseRepositoryImpl.java`
          - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/UpdateSelfCourseUseCase.java`
          - `bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/UpdateSelfCourseRepositoryImpl.java`
          - `bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`
          - `bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`

5) **课程类型修改链路收敛到 bc-course（保持行为不变）**  
   - 目标：✅ 已完成压扁 `CourseUpdateGatewayImpl.updateCourseType()/updateCoursesType()`，让 infra 不再承载业务流程（保持行为不变；进展见 0.9）。
   - 落地：
     - bc-course：新增 `UpdateCourseTypeUseCase/UpdateCoursesTypeUseCase`（仅委托端口，不新增校验），并补齐纯单测；
     - bc-course：端口实现 `UpdateCourseTypeRepositoryImpl/UpdateCoursesTypeRepositoryImpl`，迁移原 DB/缓存/日志逻辑（其中 `UpdateCoursesTypeRepositoryImpl` 已归位 `bc-course/infrastructure`，保持行为不变）；
     - 旧 gateway：方法退化为委托壳（行为不变）。
   - 关键文件：
     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/UpdateCourseTypeUseCase.java`
     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/UpdateCoursesTypeUseCase.java`
     - `bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/UpdateCourseTypeRepositoryImpl.java`
     - `bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/UpdateCoursesTypeRepositoryImpl.java`
     - `bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`

6) **删课链路收敛到 bc-course（保持行为不变）**  
   - 目标：✅ 已完成压扁 `CourseDeleteGatewayImpl.deleteCourse()/deleteCourses()`，把“删课 + 删除评教任务/记录 + 缓存/日志”收敛到 bc-course（保持行为不变；细节见 0.9）。
   - 落地：
     - bc-course：新增 `DeleteCourseUseCase/DeleteCoursesUseCase`（仅委托端口，不新增校验），并补齐纯单测；
     - eva-infra：新增端口实现 `DeleteCourseRepositoryImpl/DeleteCoursesRepositoryImpl`，迁移原 DB/缓存/日志逻辑（含 `isEmptiy` 条件拼装逻辑随端口实现私有化）；
     - 旧 gateway：方法退化为委托壳（行为不变）。
	   - 关键文件：
	     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/DeleteCourseUseCase.java`
	     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/DeleteCoursesUseCase.java`
	     - `bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/DeleteCourseRepositoryImpl.java`
	     - `bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/DeleteCoursesRepositoryImpl.java`
	     - `bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`

7) **删除课程类型链路收敛到 bc-course（保持行为不变）**
   - 目标：✅ 已完成压扁 `CourseDeleteGatewayImpl.deleteCourseType()`，让 infra 不再承载“删课程类型”的业务流程（保持行为不变；细节见 0.9）。
   - 行为不变约束（必须保持）：
     - 异常类型/异常文案不变：`UpdateException("请选择要删除的课程类型")`、`UpdateException("默认课程类型不能删除")`
     - 删除顺序不变：先删关联表 `course_type_course`，再删 `course_type`
     - 日志/缓存行为不变：`LogUtils.logContent(typeName + "课程类型")` + 失效 `COURSE_TYPE_LIST`
   - 落地：
     - bc-course：新增 `DeleteCourseTypeUseCase` + `DeleteCourseTypeRepository`（仅委托端口，不新增校验），并补齐纯单测；
     - bc-course：端口实现 `DeleteCourseTypeRepositoryImpl`，把旧逻辑原样搬运（含校验/删除顺序/日志/缓存）；
     - 旧 gateway：`CourseDeleteGatewayImpl.deleteCourseType` 退化为委托壳（返回 `null`，保持签名与行为不变）；并进一步压扁为“仅事务边界 + 委托调用”，由 `DeleteCourseTypeEntryUseCase` 承接命令构造（保持行为不变）。
   - 关键文件：
     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/DeleteCourseTypeUseCase.java`
     - `bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/DeleteCourseTypeRepositoryImpl.java`
     - `bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`

8) **新增课程类型链路收敛到 bc-course（保持行为不变）**
   - 目标：压扁 `CourseUpdateGatewayImpl.addCourseType()`，让 infra 不再承载“新增课程类型”的写流程。
   - 行为不变约束（必须保持）：
     - 异常类型/异常文案不变：`UpdateException("该课程类型已存在")`
     - 缓存行为不变：新增成功后失效 `COURSE_TYPE_LIST`
   - 落地：
     - bc-course：新增 `AddCourseTypeUseCase` + `AddCourseTypeRepository`（仅委托端口，不新增校验），并补齐纯单测；
     - eva-infra：新增端口实现 `AddCourseTypeRepositoryImpl`，把旧逻辑原样搬运（含存在性校验/插入/缓存失效）；
     - 旧 gateway：`CourseUpdateGatewayImpl.addCourseType` 退化为委托壳（返回 `null`，保持签名与行为不变）。
   - 关键文件：
     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/AddCourseTypeUseCase.java`
     - `bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/AddCourseTypeRepositoryImpl.java`
     - `bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`

9) **批量新建多节课（新课程）链路收敛到 bc-course（闭环 K，保持行为不变）**
   - 背景：`POST /course/batch/notExist` 入口最终落到 `CourseUpdateGatewayImpl.addNotExistCoursesDetails`，旧实现包含“科目插入/课程插入/默认模板与默认课程类型补齐/关联表写入/课次写入/缓存失效/教室冲突校验”等完整写流程，属于 infra 层承载业务。
   - 做法：新增 bc-course 用例骨架 + 端口，并在 eva-infra 端口适配器中原样搬运旧逻辑；旧 gateway 退化为委托壳。
   - 行为不变约束（必须保持）：
     - 默认模板/默认类型补齐策略不变：`templateId == null && nature in {0,1}` 时按 `is_default` 查询补齐；
     - 冲突校验与文案不变：教室冲突抛 `UpdateException("该时间段教室冲突，请修改时间")`；
     - 缓存失效不变：`SUBJECT_LIST`、`COURSE_LIST_BY_SEM`、`ALL_CLASSROOM`；
     - 课次写入时间字段不变：`createTime/updateTime` 的取值保持与旧实现一致。
   - 关键文件：
     - 用例与端口：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/AddNotExistCoursesDetailsUseCase.java`
     - infra 端口实现：`bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/AddNotExistCoursesDetailsRepositoryImpl.java`
     - 旧 gateway 退化委托壳：`bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`（`addNotExistCoursesDetails`）

10) ✅ **批量新建多节课（已有课程）链路收敛到 bc-course（闭环 L，保持行为不变）**
   - 背景：`POST /course/batch/exist/{courseId}` 入口最终落到 `CourseUpdateGatewayImpl.addExistCoursesDetails`，历史实现包含“逐周新增课次 + 教室冲突校验 + 课程/科目存在性校验 + 日志 + 缓存失效”等完整写流程，属于 infra 层承载业务。
   - 做法：新增 bc-course 用例骨架 + 端口，并在 eva-infra 端口适配器中原样搬运旧逻辑；新增 `AddExistCoursesDetailsGatewayEntryUseCase` 承接 Command 构造；旧 gateway 退化为委托壳（不再构造 Command；保持行为不变）。
   - 行为不变约束（必须保持）：
     - 冲突校验与文案不变：教室冲突抛 `UpdateException("该时间段教室冲突，请修改时间")`；
     - 课程/科目不存在异常不变：`QueryException("不存在对应的课程")`、`QueryException("不存在对应的课程的科目")`；
     - 日志文案不变：`subjectName + "(ID:" + courseId + ")的课程的课数"`；
     - 缓存失效不变：`ALL_CLASSROOM`；
     - 课次写入时间字段不变：`createTime/updateTime` 仍为 `LocalDateTime.now()`。
   - 关键文件：
     - 用例与端口：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/AddExistCoursesDetailsUseCase.java`
     - infra 端口实现：`bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/AddExistCoursesDetailsRepositoryImpl.java`
     - 旧 gateway 退化委托壳：`bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`（`addExistCoursesDetails`）

本轮新增提交（按时间顺序）：
- `a122ff58 feat(bc-course): 引入课程BC用例与基础设施端口实现`
- `285db180 feat(bc-messaging): 课程操作副作用事件化并收敛消息发送`
- `4a22fdaf fix(gitignore): 仅忽略仓库根 data 目录`（避免误伤 `eva-client/.../dto/data` 包路径）
- `60fe256d feat(eva-config): 支持高分阈值可配置`
- `a330d8e7 test(bc-template): 迁移模板锁定校验并清理旧实现`
- `35aca4eb docs: 更新会话交接与后续任务清单`
- `9a96a04a feat(bc-messaging): 自助课表操作副作用事件化`
- `1b2d8756 feat(bc-course): 收敛修改课程信息用例`
- `2b5cb49d feat(bc-course): 增加课程类型修改用例骨架`
- `255cb51f feat(eva-infra): 实现课程类型修改端口适配器`
- `b97a905b refactor(course): 课程类型修改收敛到bc-course`
- `45ca3b5c feat(bc-course): 收敛教师自助删课用例骨架`
- `e7ef502a refactor(course): 自助删课收敛到bc-course`
- `dde5ecf1 feat(bc-course): 收敛教师自助改课用例骨架`
- `87caef60 refactor(course): 自助改课收敛到bc-course`
- `7f055aa3 feat(bc-course): 收敛删课用例骨架`
- `bd85f734 feat(eva-infra): 实现删课端口适配器`
- `0a186d03 refactor(course): 删课链路收敛到bc-course`
- `cac3a97f docs: 更新会话交接（删课/自助课表收敛）`
- `cf7ef892 feat(bc-course): 增加删除课程类型用例骨架`
- `d5a8a0d3 feat(eva-infra): 实现删除课程类型端口适配器`
- `57c8bc5d refactor(course): 删除课程类型收敛到bc-course`
- `7df81f64 feat(bc-course): 增加新增课程类型用例骨架`
- `e612d92a feat(eva-infra): 实现新增课程类型端口适配器`
- `a5df07af refactor(course): 新增课程类型收敛到bc-course`
- `a73c7bed feat(bc-course): 收敛新建课程明细用例骨架`
- `685cfd8c feat(eva-infra): 实现新建课程明细端口适配器`
- `28dfdd49 refactor(course): 新建课程明细收敛到bc-course`
- `c91c4473 feat(bc-course): 收敛新增课次用例骨架`
- `b4b66261 feat(eva-infra): 实现新增课次端口适配器`
- `f447fd17 refactor(course): 新增课次收敛到bc-course`
- `080ae028 docs: 更新会话交接（新增课次收敛）`
- `8c9b7443 refactor(eva-infra): 收敛教室占用冲突校验`
- `969d606a refactor(eva-infra): 清理CourseUpdateGateway遗留私有逻辑`
- `2cfbfaee docs: 补充Java17构建基线说明`
- `e417fb8e refactor(eva-infra): 提炼课程时间段重叠查询片段`
- `2bb64aa1 refactor(eva-infra): 复用时间段重叠查询条件`
- `bcdd8564 refactor(eva-infra): 分配评教冲突校验复用时间重叠片段`
- `a53aafee docs: 记录冲突校验收敛进展与离线验证`
- `a84dc46d docs: 更新交接文档版本号`
- `94662c8a docs: 交接文档改为git log对齐`
- `0684a374 docs: 更新DDD重构Backlog进度与下一步`

验证（建议使用 Java17；网络受限时再加 `-o` 离线）：
- 切换 JDK（本机已安装）：`sdk use java 17.0.17-zulu`  
  或：`export JAVA_HOME=\"$HOME/.sdkman/candidates/java/17.0.17-zulu\" && export PATH=\"$JAVA_HOME/bin:$PATH\"`
 - `mvnd -o -pl bc-course -am test -q -Dmaven.repo.local=.m2/repository`
 - `mvnd -o -pl bc-messaging -am test -q -Dmaven.repo.local=.m2/repository`
 - `mvnd -o -pl eva-infra -am -DskipTests test -q -Dmaven.repo.local=.m2/repository`
 - 已验证：`export JAVA_HOME=\"$HOME/.sdkman/candidates/java/17.0.17-zulu\" && export PATH=\"$JAVA_HOME/bin:$PATH\" && mvnd -o -pl eva-infra -am test -q -Dmaven.repo.local=.m2/repository`
 - 注意：若直接使用系统默认 JDK（例如 GraalVM Java 25），本仓库可能在编译期报 `TypeTag :: UNKNOWN`，请优先切回 Java 17 再验证

## 1. 关键业务结论（必须记住）

用户已明确确认的业务语义：

1) `cour_inf.week`：表示学期从 `semester.start_date` 起算的第 **n 周**（单周周次）。  
2) `cour_inf.day`：`day=1` 表示 **周一**，`day=n` 表示周 n（周一是一周开始）。  
3) 同一课次允许多人评教（这是必须如此的业务前提）。  
4) 模板锁定规则：**只要有人评教过就锁定**，锁定后不允许切换课程模板。  
5) “高分次数”的阈值：已支持 **可配置**（`eva-config.json` 新增 `highScoreThreshold`，AI 报告统计已改为使用该阈值）。  
6) AI 报告维度：**教师 + 学期**（汇总该教师本学期全部课程；报告中可按课程分节）。

微服务演进约束：
- **先拆服务，暂时共享库**（过渡期），且要一小步一小步推进。

---

## 2. 架构/文档产物

- `AGENTS.md`：仓库贡献者指南（Repository Guidelines）。  
- `DDD_REFACTOR_PLAN.md`：非常详细的 DDD 重构计划书（领域划分、BC、六边形、CQRS、事件、微服务演进等）。

---

## 3. 本会话已完成的两条业务闭环（均已分阶段 commit）

### 3.1 闭环 A：提交评教（Submit Evaluation）DDD 化

现状链路（保持 API 不变）：
- Controller：`PUT /evaluate/task/form`  
  `eva-adapter/.../UpdateEvaController.putEvaTemplate()` 仍调用 `IEvaRecordService.putEvaTemplate()`。
- Service：`eva-app/.../EvaRecordServiceImpl.putEvaTemplate()` 已改为调用 bc-evaluation 用例（不再走旧 `evaUpdateGateway.putEvaTemplate`）。

新增业务模块：`bc-evaluation`
- 根模块加入：`pom.xml` 新增 `<module>bc-evaluation</module>`。
- bc-evaluation 关键代码：
  - 用例：`bc-evaluation/.../SubmitEvaluationUseCase.java`
  - 端口：`bc-evaluation/.../SubmitEvaluationRepository.java`
  - 领域事件：`bc-evaluation/.../EvaluationSubmittedEvent.java`
  - 状态/异常：`TaskStatus`、`SubmitEvaluationException`

基础设施实现（仍复用现有表/Mapper/缓存）：
- `eva-infra/.../SubmitEvaluationRepositoryImpl.java`
  - 插入 `form_record`
  - 更新 `eva_task.status=COMPLETED`
  - 若 `cour_one_eva_template` 不存在则创建快照（用于模板锁定依据）
  - 缓存失效：修复为 `invalidateCache(..., String.valueOf(taskId))`

事件化联动（事务提交后清理消息，避免回滚不一致）：
- 事件发布端口实现：`eva-app/.../SpringAfterCommitDomainEventPublisher.java`
- 监听器：`eva-app/.../EvaluationSubmittedMessageCleanupListener.java`
  - 收到 `EvaluationSubmittedEvent` 后执行 `msgService.deleteEvaMsg(taskId, null)`（删除待办+逾期提醒两类评教消息）
- 装配：`bc-evaluation/infrastructure/src/main/java/edu/cuit/app/config/BcEvaluationConfiguration.java` 提供 `SubmitEvaluationUseCase` Bean

测试：
- `bc-evaluation/src/test/.../SubmitEvaluationUseCaseTest.java`（纯单测，无 Spring）

### 3.2 闭环 B：模板锁定后禁止切换模板（Course Template Lock）

定位到的入口与实现：
- Controller：`eva-adapter/.../UpdateCourseController`  
  - `PUT /course`（单课程修改，含 templateId）  
  - `PUT /courses/template`（批量切换课程模板）
- 应用层：
  - `eva-app/.../ICourseDetailServiceImpl.updateCourse()` -> `bc-course` 用例 `ChangeSingleCourseTemplateUseCase`
  - `eva-app/.../ICourseDetailServiceImpl.updateCourses()` -> `bc-course` 入口用例 `UpdateCoursesEntryUseCase`（内部调用 `ChangeCourseTemplateUseCase`）
- 基础设施：`eva-infra/.../CourseUpdateGatewayImpl.updateCourse()/updateCourses()` 原先直接 update `course.template_id`，没有锁定校验。

已实现的锁定规则（最终形态：业务模块 bc-template）：
- 新增业务模块：`bc-template`
  - 根模块加入：`pom.xml` 新增 `<module>bc-template</module>`
  - 服务：`bc-template-domain/.../CourseTemplateLockService.java`（物理归属下沉到 `bc-template/domain`，包名不变）
  - 端口：`bc-template-domain/.../CourseTemplateLockQueryPort.java`（物理归属下沉到 `bc-template/domain`，包名不变）
  - 异常：`bc-template-domain/.../TemplateLockedException.java`
  - 单测：`bc-template/.../CourseTemplateLockServiceTest.java`
- 基础设施端口实现（读现有表判断锁定）：
  - `bc-template-infrastructure/.../CourseTemplateLockQueryPortImpl.java`（现归属：`bc-template-infra`；保持 `package` 不变）
  - 判定策略：
    1) 优先用 `cour_one_eva_template` 是否存在（`course_id + semester_id`）作为锁定证据
    2) 若快照缺失，则回退到 `form_record` 反查（`cour_inf` -> `eva_task` -> `form_record`）保守锁定
- 单体装配：
  - `bc-course-infra/.../BcTemplateConfiguration.java`
- 在 `CourseUpdateGatewayImpl` 接入：
  - `updateCourse()`：模板切换逻辑已上移到 `bc-course`，不再在 infra 内重复做“锁定不可切换”校验
  - `updateCourses()`：历史路径仍保留，但已委托到 `bc-course` 入口用例（内部调用 `ChangeCourseTemplateUseCase`；收敛重复逻辑，便于后续删掉旧 gateway）

本会话已清理遗留/重复实现：
- 已删除 `eva-infra/.../CourseTemplateLockChecker.java`（被 `CourseTemplateLockQueryPortImpl` 取代）
- 已删除 `start/src/test/.../CourseTemplateLockCheckerTest.java`，并新增 `CourseTemplateLockQueryPortImplTest` 直接验证端口实现
- 由于当前运行环境不支持 Mockito inline mock maker（ByteBuddy 自附加），增加了 `mockito-extensions/org.mockito.plugins.MockMaker=mock-maker-subclass`
- 已新增 `bc-course`，并把模板切换（批量/单个）统一收口到用例层（避免“前端总是带 templateId”导致锁定误判）

### 3.3 闭环 C：课程改课/删课联动事件化（Course -> Messaging）

目标：把“改课/删课”产生的跨域副作用（消息通知、撤回评教消息）从课程应用服务中剥离，收敛到 `bc-messaging` 处理，便于后续拆微服务/替换 MQ。

落地：
- 新增业务模块：`bc-messaging`
  - 事件：`CourseOperationSideEffectsEvent`
  - 用例：`HandleCourseOperationSideEffectsUseCase`
- 事件发布：`eva-app/.../AfterCommitEventPublisher`（事务提交后发布，避免回滚导致联动不一致）
- 监听器：`eva-app/.../CourseOperationSideEffectsListener`（收到事件后调用 `bc-messaging` 用例）
- 改造入口（行为保持不变，仅重构调用链）：
  - `eva-app/.../ICourseServiceImpl.updateSingleCourse()`（改课）
  - `eva-app/.../ICourseServiceImpl.deleteCourses()`（删除某课程某时段等）
  - `eva-app/.../ICourseDetailServiceImpl.delete()`（删除课程）
  - `eva-app/.../ICourseDetailServiceImpl.updateCourse()`（修改课程后通知全体）
  - `eva-app/.../ICourseServiceImpl.allocateTeacher()`（分配听课/评教老师，老师任务消息也已事件化）

补充：`allocateTeacher()` 的事件使用 `CourseTeacherTaskMessagesEvent`，并由 `bc-messaging` 用例 `HandleCourseTeacherTaskMessagesUseCase` 处理（底层仍复用 `MsgResult.sendMsgtoTeacher`，行为保持不变）。

### 3.4 闭环 D：分配评教老师用例收敛（Course -> bc-course）

目标：把“分配听课/评教老师”从旧 `CourseUpdateGatewayImpl.assignTeacher()` 的大段业务逻辑中抽离，收敛到 `bc-course` 用例层；旧 gateway 逐步退化为委托壳（保持行为不变）。

落地：
- 用例：`bc-course/.../AssignEvaTeachersUseCase`
- 端口：`bc-course/.../AssignEvaTeachersRepository`
- 基础设施端口实现：`bc-course/infrastructure/.../AssignEvaTeachersRepositoryImpl`（迁移原有冲突校验 + 任务创建 + 缓存失效逻辑，行为保持不变）
- 旧入口：`CourseUpdateGatewayImpl.assignTeacher()` 已退化为“事务边界 + 异常转换 + 委托调用”，命令构造下沉到 `AssignTeacherGatewayEntryUseCase`（保持行为不变）

### 3.5 闭环 E：课表导入用例收敛 + 副作用事件化（Import Course Table）

目标：把“导入课表/覆盖课表”的核心写操作从旧 gateway/service 中抽离，收敛到 `bc-course`；并把导入后产生的跨域副作用（通知全体、撤回评教消息）统一事件化交给 `bc-messaging` 处理，便于后续迁移到各 BC/微服务。

落地（保持 API/行为不变，仅重构调用链）：
- `bc-course` 新增用例：
  - 命令：`bc-course/.../ImportCourseFileCommand.java`
  - 端口：`bc-course/.../ImportCourseFileRepository.java`
  - 用例：`bc-course/.../ImportCourseFileUseCase.java`
- 基础设施端口实现（迁移原逻辑，行为保持不变）：
  - `bc-course/infrastructure/.../ImportCourseFileRepositoryImpl.java`
  - 迁移内容包含：学期 upsert、覆盖导入时的删除+新增、返回旧的 `Map<String, Map<Integer,Integer>>` 消息模型、教室缓存失效与日志记录
- 旧 gateway 退化为委托壳：
  - `eva-infra/.../CourseUpdateGatewayImpl.importCourseFile()` -> 委托 `ImportCourseFileUseCase`
- 导入入口侧副作用事件化：
  - `eva-app/.../IUserCourseServiceImpl.importCourse()` 不再直接循环调用 `msgResult/msgService.deleteEvaMsg`
  - 改为发布 `CourseOperationSideEffectsEvent`，由 `CourseOperationSideEffectsListener` -> `bc-messaging` 统一处理

### 3.6 闭环 F：教师自助课表链路收敛（Self Course）

目标：把“教师自助删课/改课”的主写逻辑与跨域副作用拆开：  
- 主写逻辑收敛到 `bc-course` 用例 + `eva-infra` 端口适配器（行为不变）；  
- 跨域副作用（消息通知、撤回评教消息）统一“事务提交后发布”交给 `bc-messaging`（行为不变）。

落地（保持 API/异常文案/副作用不变，仅重构调用链）：
- 自助删课：
  - bc-course 用例：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/DeleteSelfCourseUseCase.java`
- infra 端口实现：`bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/DeleteSelfCourseRepositoryImpl.java`
  - 旧入口退化委托壳：`bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`（`deleteSelfCourse`）
  - 应用层副作用事件化：`eva-app/src/main/java/edu/cuit/app/service/impl/course/IUserCourseServiceImpl.java`（`deleteSelfCourse`）
- 自助改课：
  - bc-course 用例：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/UpdateSelfCourseUseCase.java`
  - infra 端口实现：`bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/UpdateSelfCourseRepositoryImpl.java`
  - 旧入口退化委托壳：`bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`（`updateSelfCourse`）
  - 应用层副作用事件化：`eva-app/src/main/java/edu/cuit/app/service/impl/course/IUserCourseServiceImpl.java`（`updateSelfCourse`，注意需要 `CourseOperationMessageMode.TASK_LINKED` 保持历史消息格式）

补充：为保证“消息格式行为不变”，`CourseOperationSideEffectsEvent` 引入了 `CourseOperationMessageMode`（`NORMAL/TASK_LINKED`）：
- `NORMAL`：历史 `MsgResult.toNormalMsg`；
- `TASK_LINKED`：历史 `MsgResult.toSendMsg`；
对应适配已落地在 `eva-app/src/main/java/edu/cuit/app/bcmessaging/adapter/CourseBroadcastPortAdapter.java`。

### 3.7 闭环 G：修改课程信息用例收敛（Update Course Info）

目标：压扁 `CourseUpdateGatewayImpl.updateCourse()`，让 infra 不再承载“修改课程信息”的业务流程（行为不变）。

落地：
- bc-course 新增用例：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/UpdateCourseInfoUseCase.java`
- infra 端口实现：`bc-course/infrastructure/src/main/java/edu/cuit/infra/bccourse/adapter/UpdateCourseInfoRepositoryImpl.java`
- 旧 gateway 退化委托壳：`bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`（`updateCourse`）

### 3.8 闭环 H：课程类型修改链路收敛（Update Course Type）

目标：✅ 已完成压扁 `CourseUpdateGatewayImpl.updateCourseType/updateCoursesType`（行为不变；进展见 0.9）。

落地：
- bc-course 用例骨架：`UpdateCourseTypeUseCase` / `UpdateCoursesTypeUseCase`
- infra 端口实现：`UpdateCourseTypeRepositoryImpl` / `UpdateCoursesTypeRepositoryImpl`（其中 `UpdateCoursesTypeRepositoryImpl` 已归位 `bc-course/infrastructure`）
- 旧 gateway 退化为委托壳（仍由 domain gateway 对外暴露）

### 3.9 闭环 I：删课链路收敛（Delete Course）

目标：压扁 `CourseDeleteGatewayImpl.deleteCourse/deleteCourses`（行为不变）。

落地：
- bc-course 用例骨架：`DeleteCourseUseCase` / `DeleteCoursesUseCase`
- infra 端口实现：`DeleteCourseRepositoryImpl` / `DeleteCoursesRepositoryImpl`（原 `isEmptiy` 条件拼装逻辑已随端口实现私有化）
- 旧 gateway 退化为委托壳：`bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`

### 3.10 闭环 J：删除课程类型链路收敛（Delete Course Type）

目标：压扁 `CourseDeleteGatewayImpl.deleteCourseType`（行为不变）。

落地：
- bc-course 用例骨架：`DeleteCourseTypeUseCase`
- infra 端口实现：`DeleteCourseTypeRepositoryImpl`
- 旧 gateway 退化为委托壳：`bc-course/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`

验证命令（离线优先，避免网络受限）：
- `mvnd -o -pl bc-course -am test -q -Dmaven.repo.local=.m2/repository`
- `mvnd -o -pl bc-messaging -am test -q -Dmaven.repo.local=.m2/repository`
- `mvnd -o -pl eva-infra -am -DskipTests test -q -Dmaven.repo.local=.m2/repository`

---

## 4. 重要工程/环境约束（踩坑记录）

### 4.1 Java 版本必须使用 17
- Java 25（GraalVM）会触发编译异常（javac TypeTag UNKNOWN）。
- 建议命令（每次新 shell）：  
  - `source "$HOME/.sdkman/bin/sdkman-init.sh"`  
  - `sdk use java 17.0.17-zulu`

### 4.2 Maven 仓库写权限与网络
- 默认 `~/.m2` 在此环境会出现 `AccessDeniedException`（尤其是 junit-platform-launcher 目录/文件）。
- 解决策略：使用工作区本地仓库并已加入忽略：
  - 运行 Maven 时加：`-Dmaven.repo.local=.m2/repository`
  - `.gitignore` 已忽略 `.m2/`
- 网络受限：解析 GitHub Packages / 私有仓库依赖时，可能需要“开启网络（require_escalated）”。本会话多次因 DNS 失败/依赖拉取需要联网。

### 4.3 Git 提交签名（GPG）问题
- 初次提交失败原因：GPG/keyboxd 权限问题。
- 已在本仓库设置 `git config commit.gpgsign false`，提交均使用 `--no-gpg-sign`。

### 4.4 测试运行策略
- `start` 模块存在大量 `@SpringBootTest`，会尝试连接 DB/LDAP/AI 等，**全量测试容易失败**（非本会话引入）。
- 本会话验证新增测试的推荐方式：
  - 运行 bc 模块纯单测：  
    - `mvnd -pl bc-evaluation test -q -Dmaven.repo.local=.m2/repository`  
    - `mvnd -pl bc-template test -q -Dmaven.repo.local=.m2/repository`
    - `mvnd -pl bc-course -am test -q -Dmaven.repo.local=.m2/repository`
  - 只跑指定测试（多模块时需要忽略未命中模块）：  
    - `mvnd -pl start -am test -q -Dtest=CourseTemplateLockQueryPortImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

### 4.5 Linux 下误生成 Windows 路径目录
- 曾出现目录：`start/D:\Programming\Java\Projects\eva-backend\data\config/`（包含一个 `eva-config.json`）
- `.gitignore` 已增加忽略规则以避免污染提交（注意：该目录仍存在但会被忽略）。

---

## 5. 已完成的提交（按时间倒序，供回退/追踪）

当前 `git log --oneline -n 25` 关键提交如下（最新在上，更新至 `HEAD`，以 `git log -n 1` 为准）：

- `51dd315b docs: 补齐updateInfo收敛提交号`
- `cb789e21 refactor(iam): 收敛updateInfo到bc-iam`
- `db0fd6a3 feat(iam): 装配updateInfo组合根`
- `6ce61024 feat(eva-infra): 实现updateInfo端口适配器`
- `38c31541 feat(bc-iam): 增加updateInfo用例骨架`
- `1a3eb0b1 docs: 更新IAM收敛进度与下一步计划`
- `9e7d46dd refactor(iam): 收敛createUser到bc-iam`
- `a26e01b3 feat(iam): 装配createUser组合根`
- `a3232b78 feat(eva-infra): 实现createUser端口适配器`
- `c3aa8739 feat(bc-iam): 增加createUser用例骨架`
- `a707ab86 refactor(iam): 收敛assignRole到bc-iam`
- `b65d311f feat(iam): 装配assignRole端口适配器与组合根`
- `16ff60b6 feat(bc-iam): 初始化模块并增加assignRole用例骨架`
- `f3e8e3cc refactor(course): 收敛isImported到bc-course`
- `495287c8 feat(eva-infra): 实现课程导入状态查询端口适配器`
- `4ed055a2 feat(bc-course): 增加isImported查询用例骨架`
- `c315fa22 refactor(messaging): 收敛消息展示状态到bc-messaging`
- `8445bc41 refactor(messaging): 收敛消息插入到bc-messaging`
- `05a2381b refactor(messaging): 收敛消息查询到bc-messaging`
- `915d53ae docs: 更新计划/Backlog/交接（消息删除与已读收敛）`
- `dd7483aa refactor(messaging): 消息已读写侧收敛到bc-messaging`
- `22cb60eb refactor(messaging): 删除消息写侧收敛到bc-messaging`
- `bbad6802 docs: 更新交接与计划书（清理提交评教旧实现）`
- `12279f3f refactor(evaluation): 清理旧EvaUpdateGatewayImpl提交评教遗留实现`
- `fdac216b docs: 更新交接与计划书（评教模板写侧收敛）`

---

## 6. 当前工作区状态（需要处理/注意）

- 当前工作区除本文件外应保持干净（已按步骤完成 git 提交拆分）。
- 简历可用的“重构大泥球方法论”沉淀在：`data/RESUME_BALL_OF_MUD_REFACTOR_SUMMARY.md`
  - 注意：仓库根 `data/` 已被 `.gitignore` 忽略（设计如此，用于存放运行时/文档/样例，不进版本库）。
  - 若需要把该文档纳入版本库，请迁移到非 `data/` 目录（例如新建 `docs/`）。

---

## 7. 下一步建议任务（供新会话继续）

推荐按“继续模块化 + 小步 commit”的方式推进：

0) **新会话起手式（避免上下文浪费时间）**
   - 切 JDK 到 17（见上文），再跑一次 `mvnd -pl start -am test -Dmaven.repo.local=.m2/repository` 做全量回归。
   - 用 Serena 重新索引（用户要求“开始任务先更新一次索引”）。

1) ✅ **已完成：继续收敛“教师自助课表”链路（IUserCourseServiceImpl）**
   - 已完成事件化副作用 + 主写逻辑收敛到 `bc-course`（保持行为不变）。

2) ✅ **已完成：压扁旧 `CourseUpdateGatewayImpl.updateCourse()`**
   - 已新增 `UpdateCourseGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 异常转换 + 委托调用（保持行为不变；落地：`c31df92c`）。

3) ✅ **已完成：课程类型修改链路收敛**
   - `updateCourseType/updateCoursesType` 已收敛到 `bc-course`。

4) ✅ **已完成：删课链路收敛**
   - `deleteCourse/deleteCourses` 已收敛到 `bc-course`。

5) ✅ **已完成：压扁 `CourseDeleteGatewayImpl.deleteCourseType()`**
   - 已新增 `DeleteCourseTypeUseCase` + `DeleteCourseTypeRepositoryImpl`，旧 gateway 退化委托壳（保持行为不变）。
   - 进一步压扁：新增 `DeleteCourseTypeEntryUseCase`，旧 gateway 不再构造命令，仅保留事务边界与委托调用（保持行为不变）。

6) ✅ **已完成：压扁 `CourseUpdateGatewayImpl.addCourseType()`**
   - 已新增 `AddCourseTypeUseCase` + `AddCourseTypeRepositoryImpl`，旧 gateway 退化委托壳（保持行为不变）。

7) ✅ **已完成：压扁 `CourseUpdateGatewayImpl.addNotExistCoursesDetails()`**
   - 已新增 `AddNotExistCoursesDetailsUseCase` + `AddNotExistCoursesDetailsRepositoryImpl`，旧 gateway 退化委托壳（保持行为不变）。

8) ✅ **已完成：压扁 `CourseUpdateGatewayImpl.addExistCoursesDetails()`**
   - 已新增 `AddExistCoursesDetailsUseCase` + `AddExistCoursesDetailsRepositoryImpl` + `AddExistCoursesDetailsGatewayEntryUseCase`，旧 gateway 不再构造 Command，仅保留事务边界 + 委托调用（保持行为不变）。

9) ✅ **已完成：收敛“课程时间/教室冲突校验”的重复片段（保持行为不变）**
   - 已抽取：
     - `ClassroomOccupancyChecker`（教室占用冲突校验）
     - `CourInfTimeOverlapQuery`（时间段重叠查询片段）
   - 已迁移：
     - 新增课次/新建课程明细/自助改课：替换重复 `judgeAlsoHasLocation` 与重复 QueryWrapper（异常文案保持不变）
     - 改课/自助改课/分配评教：替换时间段重叠 QueryWrapper（异常文案保持不变）
   - 已清理：`CourseUpdateGatewayImpl` 遗留私有逻辑（防止旧 gateway 回潮承载业务流程）

10) ✅ **已完成：收敛评教写侧主链路（`EvaUpdateGatewayImpl.postEvaTask`）**
   - 目标：按“用例 + 端口 + 旧 gateway 委托壳”的标准步骤，把写侧流程收敛到 `bc-evaluation`（行为不变）。
   - 用例与端口：`bc-evaluation` 新增 `PostEvaTaskUseCase` + `PostEvaTaskRepository` + `EvaluationTaskPostedEvent`，并补齐纯单测。
   - infra 端口实现：`eva-infra` 新增 `PostEvaTaskRepositoryImpl`，旧 `EvaUpdateGatewayImpl.postEvaTask` 退化为委托壳（异常文案/类型保持不变）。
   - 应用层入口：`EvaTaskServiceImpl.postEvaTask` 改为调用用例；“待办评教消息发送”改为监听 `EvaluationTaskPostedEvent` 并在事务提交后发送（避免回滚误推）。

11) **事件载荷逐步语义化（中长期）**
   - 当前为了行为不变，事件仍携带 `Map<String, Map<Integer,Integer>>` 作为过渡载荷；
   - 后续可逐步替换为更明确的字段（广播文案、撤回任务列表等），并为 MQ + Outbox 做准备（先不做优化，等收敛完成后再演进）。

12) ✅ **已完成：收敛评教删除写侧（`EvaDeleteGatewayImpl.deleteEvaRecord/deleteEvaTemplate`）**
   - 背景：删除评教记录/模板涉及跨表校验、快照清理与缓存失效，是评教域写侧的重要入口。
   - 目标：按“用例 + 端口 + 旧 gateway 委托壳”的标准步骤，把写侧流程收敛到 `bc-evaluation`（行为不变）。
   - 用例与端口：`bc-evaluation` 新增 `DeleteEvaRecordUseCase/DeleteEvaTemplateUseCase` 与端口/异常，并补齐纯单测（提交：`ea928055`）。
   - infra 端口实现：`eva-infra` 新增 `DeleteEvaRecordRepositoryImpl/DeleteEvaTemplateRepositoryImpl`，旧 `EvaDeleteGatewayImpl` 两个方法退化为委托壳（提交：`07b65663`）。
   - 应用层入口：`eva-app` 的 `EvaRecordServiceImpl/EvaTemplateServiceImpl` 改为调用用例，并将异常映射回历史 `QueryException/UpdateException`（提交：`05900142`）。

13) ✅ **已完成：课程读侧渐进收敛（`CourseQueryGatewayImpl`）**
   - 背景：课程查询（分页/课表/评教统计/移动端周期课程等）存在大量 query 与组装逻辑，长期堆在 gateway 内。
   - 目标：按“先结构化 QueryRepo，再谈 CQRS 投影表”的策略，抽离查询仓储并让 gateway 退化为委托壳（行为不变）。
   - 落地：新增 `CourseQueryRepo/CourseQueryRepository`，旧 `CourseQueryGatewayImpl` 退化为委托壳（抽取：`ba8f2003`）；后续将 `CourseQueryRepository` 归位到 `bc-course/infrastructure`，并将其依赖的 `CourseRecommendExce` 归位到 `eva-infra-shared`（均保持 `package` 不变；归位：`881e1d12`）。

14) ✅ **已完成：评教读侧渐进收敛（`EvaQueryGatewayImpl`）**
   - 背景：评教读侧统计/分页/聚合仍大量集中在 `EvaQueryGatewayImpl`，是下一阶段“结构化读模型”的高收益目标。
   - 落地：抽取 `EvaQueryRepo`/`EvaQueryRepository`，`EvaQueryGatewayImpl` 退化为委托壳（保持统计口径与异常文案不变）。

15) **下一步推荐：评教读侧进一步解耦（保持行为不变）**
   - 进展：已完成“统计/导出、任务、记录、模板”查询端口拆分（`EvaStatisticsQueryPort` / `EvaTaskQueryPort` / `EvaRecordQueryPort` / `EvaTemplateQueryPort`），应用层相关查询改走新端口。
   - 进展补充：旧 `EvaQueryGatewayImpl` 已移除，应用层已完全切换到细分 QueryPort。
   - 未完成清单（下一步）：继续按用例维度细化 QueryService（任务/记录/模板），并考虑逐步收敛/拆分 `EvaQueryRepo` 内部实现（保持统计口径不变）。
   - 规则补充：下一会话**每个步骤结束**都需跑最小回归用例并记录结果。
   - 约束：每个小步完成后都执行 `mvnd -pl start -am test -Dmaven.repo.local=.m2/repository` 并据失败补强回归。

16) **当前未收敛清单（供下个会话优先处理）**
   - 系统管理写侧：菜单写侧主链路（`MenuUpdateGatewayImpl.updateMenuInfo/deleteMenu/deleteMultipleMenu/createMenu` 等）仍在旧 gateway（保持行为不变；可在后续会话按“入口用例化 → 端口搬运 → 委托壳”整体收敛到 `bc-iam`）。
   - 系统管理写侧：角色写侧其余入口（`RoleUpdateGatewayImpl.updateRoleInfo/updateRoleStatus/deleteRole/createRole` 等）仍在旧 gateway（保持行为不变；可按同套路继续收敛到 `bc-iam`）。
   - 评教读侧：`EvaQueryRepo` 仍为大聚合 QueryRepo，需继续拆分（保持统计口径不变）。
   - AI 报告 / 审计日志：已启动 `bc-ai-report` / `bc-audit` 骨架并接入组合根；审计日志写链路已收敛，且审计日志协议对象已开始从 `eva-client` 迁移到 `bc-audit`（保持行为不变；详见 0.9/0.10）。

17) ✅ **已完成：IAM 域 `UserUpdateGatewayImpl.deleteUser` 收敛到 `bc-iam`（保持行为不变）**
   - 落地提交链：`5f08151c/e23c810a/cccd75a3/2846c689`。
   - 关键约束（行为快照，必须保持）：
     - 异常类型/异常文案不变（尤其是 `checkAdmin` 的 `"初始管理员账户不允许此操作"`、以及 `checkIdExistence` 的 `"用户id不存在"`）。
     - 顺序与时机不变：DB 删除 → LDAP 删除 → 角色解绑 → 缓存失效 → 日志记录。
     - 缓存 key/area 不变：沿用旧 `handleUserUpdateCache` 失效清单（含 `COURSE_LIST_BY_SEM` 等）。
   - 关键落地点（便于快速定位）：
     - 旧 gateway（已退化委托壳）：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/UserUpdateGatewayImpl.java`
     - 用例与端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/DeleteUserUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/UserDeletionPort.java`
     - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserDeletionPortImpl.java`
     - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`

18) ✅ **已完成：系统管理读侧 `UserQueryGatewayImpl.fileUserEntity` 收敛到 `bc-iam`（保持行为不变）**
   - 背景：`fileUserEntity` 负责“用户实体装配”（角色 + 菜单），是典型的读侧聚合装配逻辑，长期堆在旧 gateway 内。
   - 目标：按“用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳”的套路把装配逻辑收敛到 `bc-iam`（行为不变）。
   - 落地提交链：`3e6f2cb2/8c245098/92a9beb3`（文档同步：`0a7802e2`）。
   - 关键约束（行为快照，必须保持）：
     - 异常类型/异常文案不变：菜单查询失败仍抛 `SysException("菜单查询出错，请联系管理员")`，且保持 `log.error("菜单查询出错", sysException)`。
     - 组合方式不变：角色来源仍为 `userQueryGateway.getUserRoleIds(userId)`；菜单来源仍为 `roleQueryGateway.getRoleMenuIds(roleId)` 并逐个 `menuQueryGateway.getOne(menuId)`。
     - 缓存不变：`findById/findByUsername/page` 的 `@LocalCached` 仍保留在旧 `UserQueryGatewayImpl` 上（入口不变）。
   - 关键落地点（便于快速定位）：
     - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindUserByIdUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindUserByUsernameUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/PageUserUseCase.java`
     - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/UserEntityQueryPort.java`
     - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserEntityQueryPortImpl.java`
     - 旧 gateway（已退化委托壳）：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/UserQueryGatewayImpl.java`
     - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`

19) ✅ **已完成：系统管理读侧 `UserQueryGatewayImpl.findIdByUsername/findUsernameById/getUserStatus/isUsernameExist` 收敛到 `bc-iam`（保持行为不变）**
   - 落地提交链：`9f664229/38384628/de662d1c/8a74faf5`。
   - 关键约束（行为快照，必须保持）：
     - 缓存读取语义不变：仍需先尝试读取 `LocalCacheManager` 中的 `ONE_USER_ID/ONE_USER_USERNAME`（key 分别为 `String.valueOf(id)` 与 `username`）。
     - 返回值语义不变：`findIdByUsername/findUsernameById/getUserStatus` 仍保持历史 `Optional/null/NPE` 表现（尤其 `getUserStatus` 在 DB 回源时仍可能触发历史 NPE）。
   - 关键落地点（便于快速定位）：
     - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindUserIdByUsernameUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindUsernameByIdUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/GetUserStatusUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/IsUsernameExistUseCase.java`
     - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/UserBasicQueryPort.java`
     - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserBasicQueryPortImpl.java`
     - 旧 gateway（已退化委托壳）：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/UserQueryGatewayImpl.java`
     - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`

20) ✅ **已完成：系统管理读侧 `UserQueryGatewayImpl.findAllUserId/findAllUsername/allUser/getUserRoleIds` 收敛到 `bc-iam`（保持行为不变）**
   - 落地提交链：`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`。
   - 关键约束（行为快照，必须保持）：
     - 缓存不变：`@LocalCached` 仍保留在旧 `UserQueryGatewayImpl` 上（key/area 与命中语义保持不变）。
     - 查询/映射不变：字段选择与 `userConverter.toUserSimpleResult` 映射不变；`getUserRoleIds` join 条件不变。
   - 关键落地点（便于快速定位）：
     - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindAllUserIdUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindAllUsernameUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/AllUserUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/GetUserRoleIdsUseCase.java`
     - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/UserDirectoryQueryPort.java`
     - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserDirectoryQueryPortImpl.java`
     - 旧 gateway（已退化委托壳）：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/UserQueryGatewayImpl.java`
     - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`

21) ✅ **已完成：系统管理写侧（角色/菜单）缓存与权限变更副作用收敛到 `bc-iam`（保持行为不变）**
   - 背景：权限/菜单/角色变更会触发多层缓存失效与审计日志，历史上长期堆在旧 gateway；属于 IAM 模块化单体的关键“副作用一致性”能力。
   - 目标：优先收敛 `RoleUpdateGatewayImpl.assignPerms/deleteMultipleRole`（含缓存失效与 `LogUtils.logContent`），再评估是否把菜单写侧（`MenuUpdateGatewayImpl`）整体收敛到 `bc-iam`。
   - 行为快照（必须保持）：
     - `assignPerms` 顺序不变：`checkRoleId` → 删除原 `role_menu` → 插入新 `role_menu` → `handleRoleUpdateCache` → `LogUtils.logContent(...)`。
     - `deleteMultipleRole` 顺序不变：先逐个 `checkDefaultRole`/`checkRoleId` 并提前 `handleRoleUpdateCache`，再逐个删除 `sys_role/sys_user_role/sys_role_menu`，最后 `LogUtils.logContent(tmp + " 角色")`。
     - `handleRoleUpdateCache/handleUserMenuCache` 规则不变：失效 `ALL_ROLE/ONE_ROLE/ROLE_MENU/ALL_MENU/ONE_USER_ID/ONE_USER_USERNAME` 等，并保持 `userQueryGateway.findUsernameById(...).orElse(null)` 的空值语义。
   - 行为快照补充（已用 Serena 复核，便于搬运时逐条对照）：
     - 角色权限分配（`eva-infra/.../user/RoleUpdateGatewayImpl.assignPerms`）：
       - `checkRoleId`：`select(id, roleName).eq(id)`；不存在抛 `BizException("角色id: " + id + " 不存在")`。
       - 删除旧权限：`roleMenuMapper.delete(Wrappers.lambdaUpdate().eq(roleId))`（注意是 update wrapper）。
       - 插入新权限：逐个 `roleMenuMapper.insert(new SysRoleMenuDO().setMenuId(id).setRoleId(roleId))`（顺序与入参 list 顺序一致）。
       - 缓存失效：`handleRoleUpdateCache(roleId)` 内部第一个失效为 `invalidateCache(null, userCacheConstants.ALL_ROLE)`；随后按 `ONE_ROLE/ROLE_MENU` 与 `ONE_USER_ID/ONE_USER_USERNAME` 失效（key 都是 `String.valueOf(...)`，用户名 key 允许 `null`）。
       - 日志：最后 `LogUtils.logContent(tmp.getRoleName() + " 角色(" + tmp.getId() + ")的权限")`（必须最后执行）。
     - 角色批量删除（`eva-infra/.../user/RoleUpdateGatewayImpl.deleteMultipleRole`）：
       - 两段循环：第一段逐个 `checkDefaultRole`（默认角色抛 `BizException("默认角色不允许此操作")`）→ `checkRoleId` → 立刻 `handleRoleUpdateCache(roleId)`；第二段再逐个执行删除（`sys_role` → `sys_user_role` → `sys_role_menu`）。
       - 日志：最后 `LogUtils.logContent(tmp + " 角色")`（`tmp` 为 `List<SysRoleDO>`，来源于第一段循环 `checkRoleId` 返回值）。
     - 菜单变更触发的用户缓存失效（`eva-infra/.../user/MenuUpdateGatewayImpl.handleUserMenuCache`）：
       - 先 `invalidateCache(null, userCacheConstants.ALL_MENU)`；再按 `menuId -> roleId -> userId` 链路失效 `ONE_ROLE/ONE_USER_ID/ONE_USER_USERNAME`（用户名 key 允许 `null`）。
       - 注意 `deleteMenu` 链路中 `handleUserMenuCache(menuId)` 会在 `deleteMenuAndChildren` 内部对每个节点调用一次，且在 `deleteMenu` 方法末尾对根节点再调用一次（即根节点会触发两次失效；必须保持）。
   - 调用方入口（便于回归对照）：
     - 角色：`eva-app/src/main/java/edu/cuit/app/service/impl/user/RoleServiceImpl.java` → `roleUpdateGateway.assignPerms/deleteMultipleRole`
     - 菜单：`eva-app/src/main/java/edu/cuit/app/service/impl/user/MenuServiceImpl.java` → `menuUpdateGateway.updateMenuInfo/deleteMenu/deleteMultipleMenu`
   - 落地提交链：`f8838951/91d13db4/7fce88b8/d6e3bed1/4d650166/46e666f9`
   - 关键落地点（便于快速定位）：
     - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/AssignRolePermsUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/DeleteMultipleRoleUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/HandleUserMenuCacheUseCase.java`
     - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RolePermissionAssignmentPort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RoleBatchDeletionPort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/UserMenuCacheInvalidationPort.java`
     - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/RoleWritePortImpl.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserMenuCacheInvalidationPortImpl.java`
     - 旧 gateway（已退化委托壳）：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/RoleUpdateGatewayImpl.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/gateway/impl/user/MenuUpdateGatewayImpl.java`
     - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`

22) **下一会话推荐重构任务：AI 报告 / 审计日志模块化（保持行为不变）**
   - 目标：启动 `bc-ai-report` / `bc-audit` 的最小骨架，并选择 1 条高价值写链路按“用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳”收敛（异常文案与副作用顺序保持不变）。

23) ✅ **已完成：系统管理写侧继续收敛（菜单写侧主链路整体收敛到 `bc-iam`，保持行为不变）**
   - 收敛范围：`MenuUpdateGatewayImpl.updateMenuInfo/deleteMenu/deleteMultipleMenu/createMenu`
   - 强约束保持：缓存注解 area/key 与触发时机不变；`deleteMenu` 根节点缓存失效触发两次与递归删除顺序不变；异常文案与日志顺序不变。
   - 落地提交：`f022c415`

24) ✅ **已完成：系统管理写侧继续收敛（角色写侧剩余入口收敛到 `bc-iam`，保持行为不变）**
   - 收敛范围：`RoleUpdateGatewayImpl.updateRoleInfo/updateRoleStatus/deleteRole/createRole`
   - 强约束保持：异常文案不变；缓存失效 key 与时机不变；日志文案与触发时机不变；副作用顺序不变。
   - 落地提交：`64fadb20`

25) ⏳ **中期里程碑：BC “自包含三层结构”落地（以 `bc-iam` 先试点）**
   - 目标：让 `bc-iam` 在模块边界上也更贴近最终形态（最少做到 package 结构完整；更进一步可拆 Maven 子模块）。
   - 推荐拆分路径（先小步、可回滚、行为不变）：
     1) 先统一文档与命名：将“旧 gateway = 委托壳 / 入口适配器”的约定固化（本条目已完成）。
     2) ✅ 以 `bc-iam` 为试点：已引入 `bc-iam-infra` 子模块骨架，并由 `eva-app` 装配（提交：`42a6f66f`）。
    3) ✅ 已完成：将 `bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/*` 作为新落点，并已完成从 `eva-infra` 的迁移（行为不变；提交链：`070068ec/03ceb685/02b3e8aa/6b9d2ce7/5aecc747/1c3d4b8c`）。
     4) 下一阶段（仍保持行为不变）：将 IAM 的 DAL（DO/Mapper/SQL）从 `eva-infra` 逐步抽离到 `bc-iam-infra`（或更通用的 shared 模块，后置决策），最终让 `bc-iam-infra` 去掉对 `eva-infra` 的依赖。
     5) 当 `bc-iam` 完成三层自包含后，再评估把 `eva-domain` 中与 IAM 强相关的类型逐步迁移到 `bc-iam-domain`（谨慎推进，避免牵连其它 BC；共享部分沉淀到 `shared-kernel`）。

26) ✅ **已完成：`bc-iam-infra` 依赖收敛（IAM DAL 抽离到 `eva-infra-dal`，保持行为不变）**
   - ✅ **状态更新（已闭环，避免重复劳动）**：条目 26 已按“每步最小回归 + 可回滚”完成闭环：IAM DAL（DO/Mapper/XML）已迁入共享模块 `eva-infra-dal`，共享支撑已迁入 `eva-infra-shared`，且 `bc-iam-infra` 已移除对 `eva-infra` 的 Maven 依赖（保持行为不变）。
   - 落地提交（收尾）：`2ad911ea`（移除 `bc-iam-infra` → `eva-infra` 依赖；其余分步提交见 `git log --oneline`）。
   - 历史拆分与里程碑（保留用于追溯）：
    1) ✅ Serena：盘点 `bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/*` 实际依赖的 Mapper/DO 清单（按“user/role/menu”分组）。
        - Mapper：`SysUserMapper`、`SysUserRoleMapper`、`SysRoleMapper`、`SysRoleMenuMapper`、`SysMenuMapper`
        - DO：`SysUserDO`、`SysUserRoleDO`、`SysRoleDO`、`SysRoleMenuDO`、`SysMenuDO`（其中 `SysUserDO/SysRoleDO/SysMenuDO` 已下沉到 `shared-kernel`，落地：`31e157cd`/`c5ba98b1`/`a9141bfe`）
        - XML：`eva-infra-dal/src/main/resources/mapper/user/SysUserMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysUserRoleMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysRoleMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysRoleMenuMapper.xml`、`bc-iam/infrastructure/src/main/resources/mapper/user/SysMenuMapper.xml`
     2) ✅ 新建 `bc-iam-infra` 内部 DAL 包路径（先空骨架 + 资源目录），不迁代码；确保编译通过。
     3) ✅ 迁移 `SysUser*` 相关 DO/Mapper/XML 到共享模块 `eva-infra-dal`（保持包名/namespace/SQL/异常/顺序一致）；跑最小回归。
     4) ✅ 迁移 `SysRole*`/`SysRoleMenu*` 相关 DO/Mapper/XML 到共享模块 `eva-infra-dal`；跑最小回归。
     5) ✅ 迁移 `SysMenu*` 相关 DO/Mapper/XML 到共享模块 `eva-infra-dal`；跑最小回归。
     6) ✅ 盘点 `bc-iam-infra` 仍依赖 `eva-infra` 的类型/Bean 清单（含 Converter/LDAP/缓存常量/工具等），作为后续 shared 模块拆分输入；跑最小回归。
     7) ✅ 新增 shared 子模块骨架 `eva-infra-shared`（不迁代码，仅提供后续抽离落点）；跑最小回归。
     8) ✅ 将 `bc-iam-infra` 仍依赖的 Converter/LDAP/缓存常量/工具等，从 `eva-infra` 逐步迁移到 `eva-infra-shared`（保持包名不变），并调整依赖；跑最小回归。
     9) ✅ 去掉 `bc-iam-infra` 对 `eva-infra` 的依赖（或至少只保留必须的 starter），确保仍能通过最小回归。
