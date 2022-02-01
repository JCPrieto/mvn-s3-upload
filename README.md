# mvn-s3-upload

Plugin de Maven para subir el artifact a un bucket S3

#### Motivación

En la empresa para la que trabajo durante la implementacón de este proyecto, utilizamos bucket de S3 para almacenar los 
war de nuestras aplicaciones web para que sean desplegado en los entornos de Pre-producción y Producción.
Para facilitar y agilizar los despliegues al resto del equipo técnico, pensé en implementar un plugin para maven y no 
depender der terceras aplicaciones (Ej: [BeyondDeploy](https://jcprieto.ml/portfolio.html), tambien implementada por mí 
con anterioridad) o AWS Console.

#### Ejemplo de uso

```xml

<plugins>
    <plugin>
        <groupId>es.jklabs.mvn</groupId>
        <artifactId>mvn-s3-upload</artifactId>
        <version>0.0.22</version>
        <configuration>
            <bucket>my-bucket</bucket>
            <region>eu-west-1</region>
            <path>folder1/folder2</path>
            <extension>war</extension>
            <accessKey>XXXXXXXXXXX</accessKey>
            <secretKey>XXXXXXXXXXX</secretKey>
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

##### ToDo

- Mostrar información de progreso de la subida
