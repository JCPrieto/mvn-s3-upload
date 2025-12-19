package io.github.jcprieto.mvn;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@ExtendWith(MockitoExtension.class)
public class S3UploaderTest {

    private static File testFile;
    private String previousDisableDeprecationAnnouncementProperty;
    @InjectMocks
    S3Uploader s3Uploader;
    @Mock
    private MavenProject project;
    @Mock
    private S3Client s3Client;

    private static void createNewFile() {
        try {
            Path parent = testFile.toPath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(testFile.toPath(), "content".getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    public static void tearDown() {
        try {
            if (testFile != null) {
                Files.deleteIfExists(testFile.toPath());
            }
            testFile = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void init() {
        previousDisableDeprecationAnnouncementProperty = System.getProperty("aws.java.v1.disableDeprecationAnnouncement");
        Mockito.when(project.getName()).thenReturn("Fake");
        if (testFile == null) {
            testFile = new File("target" + FileSystems.getDefault().getSeparator() + "test-classes" + FileSystems.getDefault().getSeparator() + "testfile.jar");
            createNewFile();
        } else {
            if (!testFile.exists()) {
                createNewFile();
            }
        }
    }

    @AfterEach
    public void restoreSystemProperties() {
        if (previousDisableDeprecationAnnouncementProperty == null) {
            System.clearProperty("aws.java.v1.disableDeprecationAnnouncement");
        } else {
            System.setProperty("aws.java.v1.disableDeprecationAnnouncement", previousDisableDeprecationAnnouncementProperty);
        }
    }

    @Test
    @DisplayName("S3Uploader -> Subida del archivo al bucket")
    public void executeTest() {
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Region.EU_WEST_3.id());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        s3Uploader.setPath("folder/");
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        Mockito.when(s3Client.putObjectAcl(Mockito.any(PutObjectAclRequest.class)))
                .thenReturn(PutObjectAclResponse.builder().build());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> Desactiva aviso deprecación SDK v1 cuando se configura")
    public void disableSdkV1DeprecationAnnouncementTest() {
        s3Uploader.setDisableSdkV1DeprecationAnnouncement(true);
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Region.EU_WEST_3.id());
        s3Uploader.setBucket("bucket");
        s3Uploader.setPath("folder/");
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
        Assertions.assertEquals("true", System.getProperty("aws.java.v1.disableDeprecationAnnouncement"));
    }

    @Test
    @DisplayName("S3Uploader -> Usa request body con longitud cuando se solicita progreso")
    public void executeWithProgressListenerTest() {
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Region.EU_WEST_3.id());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        s3Uploader.setPath("folder/");
        s3Uploader.setShowProgress(true);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), bodyCaptor.capture()))
                .thenReturn(PutObjectResponse.builder().build());
        Mockito.when(s3Client.putObjectAcl(Mockito.any(PutObjectAclRequest.class)))
                .thenReturn(PutObjectAclResponse.builder().build());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
        Assertions.assertEquals(testFile.length(), bodyCaptor.getValue().optionalContentLength().orElse(-1L));
    }

    @Test
    @DisplayName("S3Uploader -> Error en la subida del archivo al bucket")
    public void executeErrorUploadingTest() {
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Region.EU_WEST_3.id());
        s3Uploader.setBucket("bucket");
        s3Uploader.setPath("folder/");
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(null);
        Assertions.assertThrows(MojoExecutionException.class, () -> s3Uploader.execute());
    }

    @Test
    @DisplayName("S3Uploader -> SdkClientException en la subida del archivo al bucket")
    public void executeSdkClientExceptionUploadingTest() {
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Region.EU_WEST_3.id());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        s3Uploader.setPath("folder/");
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenThrow(SdkClientException.builder().message("Error").build());
        Assertions.assertThrows(MojoFailureException.class, () -> s3Uploader.execute());
    }

    @Test
    @DisplayName("S3Uploader -> AmazonServiceException en la subida del archivo al bucket")
    public void executeAmazonServiceExceptionUploadingTest() {
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Region.EU_WEST_3.id());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        s3Uploader.setPath("folder/");
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Error").build());
        Assertions.assertThrows(MojoFailureException.class, () -> s3Uploader.execute());
    }

    @Test
    @DisplayName("S3Uploader -> SdkClientException al añadirle permisos al archivo subido")
    public void executeSdkClientExceptionAclTest() {
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Region.EU_WEST_3.id());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        s3Uploader.setPath("folder/");
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        Mockito.when(s3Client.putObjectAcl(Mockito.any(PutObjectAclRequest.class)))
                .thenThrow(SdkClientException.builder().message("Error").build());
        Assertions.assertThrows(MojoFailureException.class, () -> s3Uploader.execute());
    }

    @Test
    @DisplayName("S3Uploader -> AmazonServiceException al añadirle permisos al archivo subido")
    public void executeAmazonServiceExceptionAclTest() {
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Region.EU_WEST_3.id());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        s3Uploader.setPath("folder/");
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        Mockito.when(s3Client.putObjectAcl(Mockito.any(PutObjectAclRequest.class)))
                .thenThrow(S3Exception.builder().message("Error").build());
        Assertions.assertThrows(MojoFailureException.class, () -> s3Uploader.execute());
    }

    @Test
    @DisplayName("S3Uploader -> Archivo no encontrado")
    public void executeTestArtifactNotFound() {
        s3Uploader.setOutputDirectory(System.getProperty("user.home"));
        s3Uploader.setWarName("erpmodularizado");
        s3Uploader.setExtension("war");
        Assertions.assertThrows(MojoExecutionException.class, s3Uploader::execute);
    }

}
