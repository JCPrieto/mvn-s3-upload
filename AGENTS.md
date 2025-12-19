# Repository Guidelines

## Project Structure & Module Organization

- Maven plugin project with Java sources under `src/main/java/es/jklabs/...`; add new plugin goals or helpers there.
- Tests live in `src/test/java/es/jklabs/...` using JUnit 5/Mockito; mirror production package paths.
- Build output goes to `target/`, including the plugin JAR `mvn-s3-upload-<version>.jar`.
- `pom.xml` declares dependencies and plugin metadata; keep groupId/artifactId/version in sync with README usage
  examples.

## Build, Test, and Development Commands

- `mvn clean package` — compile, run tests, and build the plugin artifact.
- `mvn test` — run unit tests only.
- `mvn verify` — full lifecycle checks; use before publishing.
- `mvn clean package -DskipTests` — fast build when tests are already covered elsewhere.
- Install locally for manual trials with `mvn install`, then reference the version in a sample project’s `pom.xml`.

## Coding Style & Naming Conventions

- Java 8 target (`maven.compiler.source/target` set to 1.8); prefer language features compatible with that level.
- Use 4-space indentation, brace-on-same-line for control blocks, and descriptive class/method names under `es.jklabs`.
- Follow Maven plugin conventions: goal classes annotated with `@Mojo`, parameters with `@Parameter`, and keep
  configuration names aligned with README examples (`bucket`, `region`, `path`, etc.).
- Keep logging via SLF4J; avoid `System.out` except in tests.

## Testing Guidelines

- Frameworks: JUnit Jupiter 5 and Mockito (core/inline/junit-jupiter available).
- Name tests after behavior (e.g., `S3UploaderMojoTest` with methods like `uploadsArtifactToConfiguredBucket`).
- Stub AWS interactions; do not reach real S3 in unit tests. Use mocks/fakes and temporary files under
  `target/test-classes` or JUnit temp directories.
- Aim to cover configuration parsing, path resolution, and error handling for missing credentials.
- After code modifications, review unit tests to see if they need updates for API or behavior changes.

## Commit & Pull Request Guidelines

- Commit messages in repo history are short and imperative (often Spanish); keep them concise and focused (e.g.,
  “Actualiza versión a 0.0.71”).
- For pull requests: describe purpose, link related issue/ticket, note testing performed (`mvn test`/`mvn verify`), and
  include any manual validation steps.
- Mention version bumps explicitly when updating `pom.xml` and README usage snippets together.

## Security & Configuration Tips

- Never commit AWS keys; load via environment variables, settings.xml, or CI secrets. Avoid hardcoding in tests.
- Validate bucket/region values and fail fast with clear messages to prevent accidental uploads.
- If sharing artifacts, ensure S3 ACL settings and canonical IDs match deployment needs; document defaults in code
  comments when they change.
