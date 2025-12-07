# Repository Guidelines

## Project Structure & Module Organization
Spring Boot 3.3 modules: `start/` wraps the runnable server (`edu.cuit.Application`) plus profile YAML. `eva-adapter/` owns HTTP controllers grouped by feature and returning `CommonResult`. `eva-app/` contains service orchestration, converters, security, AOP, and WebSocket glue. `eva-domain/` defines COLA entities and gateway ports while `eva-infra/` houses MapStruct convertors, DAL code, and property bindings. Shared DTOs/queries live in `eva-client/`, config/validation sits in `eva-base/*`, and tests currently live under `start/src/test/java`.

## Build, Test, and Development Commands
- `mvn clean install` – compile every module and refresh the managed dependency set.
- `mvn -pl start spring-boot:run -Dspring-boot.run.profiles=dev` – run the API using `application-dev.yml`.
- `mvn -pl start test` – execute the JUnit 5/Spring Boot suite.
- `mvn -pl start -DskipTests package` – create `start/target/eva-server.jar` for Docker/CI.
- `docker compose up mariadb` – start the local MariaDB (and companion services) defined in `docker-compose.yml`.
- `docker build --build-arg JVMOPTIONS="-Dspring.profiles.active=prod" -t eva-server:local .` – reproduce the GitLab publish stage.

## Coding Style & Naming Conventions
Use 4-space indentation and package names under `edu.cuit.<layer>.<feature>`. Controllers return `CommonResult` payloads and declare permissions with `@SaCheckPermission`/`@SaCheckLogin`. DTOs end with `*CO`, query classes with `*Query`, and application services implement the interfaces in `eva-client`. Favor Lombok for boilerplate, MapStruct(+)/convertors for mapping, and maintain the dependency flow Adapter → App → Domain → Infra → Base.

## Testing Guidelines
Tests live beside features as `*Test` classes (see `start/src/test/java/edu/cuit/app/ExcelExporterTest.java`). Use `@SpringBootTest` for cross-module flows and lighter slice tests (`@WebMvcTest`, repository tests) for focused logic to keep runtime short. Every new endpoint or gateway must add at least one happy-path and one guard test; verify converters with pure unit tests. Run `mvn -pl start test` locally before pushing to avoid breaking the GitLab `build` job.

## Commit & Pull Request Guidelines
Git history favors `<type>：<summary>` messages (`fix：课程筛选`, `feat: add batch edit`, `chore: bump pipeline version`). Keep each commit scoped to one change and prefer English summaries unless user-facing copy demands Chinese. Merge requests must describe context, link related GitLab issues (`Closes #123`), outline manual test steps, and add response screenshots when APIs change. Wait for CI to pass before requesting review.

## Security & Configuration Tips
Configuration defaults live in `start/src/main/resources/application*.yml`; change environments via `spring.profiles.active` or JVM `JVMOPTIONS`. Do not commit secrets—supply MariaDB/Redis/LDAP credentials through env vars or the compose override file. Keep `eva-config.json` and other runtime data immutable, and use the provided Dockerfile when shipping prod builds.
