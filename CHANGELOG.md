# Changelog

## 0.3.5

- Actualización de AWS SDK v2 de la versión 2.41.7 a la 2.47.5.
- Integración del análisis de SonarQube en pushes a `master` y pull requests.
- Generación y envío a SonarQube de la cobertura de tests unitarios mediante JaCoCo.
- El análisis de SonarQube no bloquea la publicación; los tests unitarios y el empaquetado siguen siendo obligatorios.

## 0.3.4

- Credenciales por defecto (`DefaultCredentialsProvider`) y session token opcional.

## 0.3.3

- Key S3 estable en logs y en la subida.
- Tests sin acceso real a S3.
- Actualización de AWS SDK v2.

## 0.3.2

- Validación temprana de configuración (`bucket`/`region`/`path`) y normalización de `path`.
- Actualización de dependencias.
