# mvn-s3-upload
Plugin de Maven para subir empaquetados a un bucket S3

Proyecto en curso....

Ejemplo de uso:

```
<plugins>
    <plugin>
        <artifactId>mvn-s3-upload</artifactId>
        <version>0.0.1</version>
        <configuration>
            <bucket>my-bucket</bucket>
            <region>eu-west-1</region>
            <path>folder1/folder2/</path>
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

ToDo