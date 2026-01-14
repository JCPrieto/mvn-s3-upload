# mvn-s3-upload

Plugin de Maven para subir el artifact a un bucket S3 usando AWS SDK v2.

#### Motivación

En la empresa para la que trabajo durante la implementacón de este proyecto, utilizamos bucket de S3 para almacenar los 
war de nuestras aplicaciones web para que sean desplegado en los entornos de Pre-producción y Producción.
Para facilitar y agilizar los despliegues al resto del equipo técnico, pensé en implementar un plugin para maven y no
depender der terceras aplicaciones (Ej: [BeyondDeploy](https://curriculum-a2a80.web.app/portfolio.html), tambien
implementada por mí
con anterioridad) o AWS Console.

#### Cómo añadirlo a tu proyecto

1. Asegúrate de usar la última versión publicada en Maven Central (reemplaza `0.3.3` si aparece una más reciente).
2. Declara el plugin en tu `pom.xml` dentro de la sección `<build><plugins>`.
3. Configura bucket, región, ruta y credenciales según tu caso.
4. Invoca el goal `s3uploader` en la fase donde quieras que se ejecute (por ejemplo, con una ejecución sin fase se
   ejecutará al lanzar `mvn package`).

#### Ejemplo de uso

```xml

<plugins>
    <plugin>
        <groupId>io.github.jcprieto</groupId>
        <artifactId>mvn-s3-upload</artifactId>
      <version>0.3.3</version>
        <configuration>
            <bucket>my-bucket</bucket>
            <region>eu-west-1</region>
           <path>folder1/folder2/</path>
            <extension>war</extension>
            <accessKey>XXXXXXXXXXX</accessKey>
            <secretKey>XXXXXXXXXXX</secretKey>
            <showProgress>true</showProgress>
            <cannonicalIds>
                <cannonicalId>XXXXXXXXXXX</cannonicalId>
                <cannonicalId>XXXXXXXXXXX</cannonicalId>
                <cannonicalId>XXXXXXXXXXX</cannonicalId>
            </cannonicalIds>
        </configuration>
        <executions>
            <execution>
                <goals>
                    <goal>s3uploader</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
</plugins>
```

#### Progreso de subida

Si activas `<showProgress>true</showProgress>`, el plugin registra el avance cada 10% con el total de bytes subidos.
El plugin normaliza `<path>` para que use `/` y termine con barra.

#### Changelog

- 0.3.3:
  - Key S3 estable en logs y en la subida.
  - Tests sin acceso real a S3.
  - Actualización de AWS SDK v2.
- 0.3.2:
   - Validación temprana de configuración (bucket/region/path) y normalización de `path`.
   - Actualización de dependencias.
