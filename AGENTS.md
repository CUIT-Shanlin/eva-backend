# Repository Guidelines

## Project Structure & Module Organization

This repository is a **Java 17 + Spring Boot** backend built as a Maven multi-module project.

- `start/`: Spring Boot entry module (`edu.cuit.Application`) and runtime configs in `start/src/main/resources/`
  - Key configs: `application.yml`, `application-dev.yml`, `application-test.yml`
- `eva-adapter/`: adapters (typically HTTP controllers and inbound/outbound integration glue)
- `eva-app/`: application layer (use-cases / application services that orchestrate domain logic)
- `eva-domain/`: domain layer (entities, domain services, repository interfaces)
- `eva-infra/`: infrastructure (MyBatis-Plus persistence, external system integrations, repository implementations)
- `eva-client/`: shared DTOs/clients for cross-service reuse (when applicable)
- `eva-base/`: shared utilities and configuration modules (e.g. `eva-base-common/`, `eva-base-config/`)
- `data/`: runtime-mounted data/config directory (used by container deployments)

Tests currently live primarily under `start/src/test/java/`.

## Build, Test, and Development Commands

- `mvn clean package`: builds all modules; the runnable jar is typically `start/target/eva-server.jar`
- `mvn test`: runs unit tests via Maven Surefire
- `mvn -pl start spring-boot:run`: runs the app locally (defaults to the `dev` profile from `application.yml`)
- `SPRING_PROFILES_ACTIVE=test mvn -pl start spring-boot:run`: runs using the `test` profile (closer to container wiring)
- `docker compose up -d`: starts the stack defined in `docker-compose.yml` (requires access to private registry images)

## Coding Style & Naming Conventions

- Indentation: Java uses 4 spaces; YAML/XML use 2 spaces.
- Naming: packages are lowercase (e.g. `edu.cuit...`), classes `UpperCamelCase`, methods/fields `lowerCamelCase`.
- Respect module boundaries: `eva-domain` should not depend on `eva-infra`; adapters should call into `eva-app`.
- Team convention: prefer Chinese comments/docstrings for new code (中文注释优先) to keep knowledge accessible.

## Testing Guidelines

- Frameworks: JUnit + Spring Boot Test (use mocks where appropriate).
- Naming: `*Test` and mirror the package structure of the code under test.
- Guidance: mock external systems (DB/LDAP/LLM) unless the test is explicitly an integration test.

## Commit & Pull Request Guidelines

- Commit messages follow the repo’s history: `feat: ...`, `fix: ...`, `chore: ...` (Chinese descriptions are common).
- PR/MR checklist: clear “what/why”, linked issue, config impact notes, and evidence (`mvn test` output).

## Security & Configuration Tips

- Never commit secrets. Use environment variables or a local override file (e.g. `application-local.yml`) and add it to `.gitignore`.
