package io.github.jcprieto.mvn;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@ExtendWith(MockitoExtension.class)
public class S3UploaderTest {

    @TempDir
    Path tempDir;

    private File testFile;
    private String previousDisableDeprecationAnnouncementProperty;

    @InjectMocks
    S3Uploader s3Uploader;

    @Mock
    private MavenProject project;

    @Mock
    private S3Client s3Client;

    @BeforeEach
    public void init() {
        previousDisableDeprecationAnnouncementProperty = System.getProperty("aws.java.v1.disableDeprecationAnnouncement");
        Mockito.lenient().when(project.getName()).thenReturn("Fake");
        s3Uploader.setS3Client(s3Client);
        testFile = createTestFile(tempDir);
    }

    @AfterEach
    public void restoreSystemProperties() {
        if (previousDisableDeprecationAnnouncementProperty == null) {
            System.clearProperty("aws.java.v1.disableDeprecationAnnouncement");
        } else {
            System.setProperty("aws.java.v1.disableDeprecationAnnouncement", previousDisableDeprecationAnnouncementProperty);
        }
    }

    private File createTestFile(Path parent) {
        try {
            Path filePath = parent.resolve("testfile.jar");
            Files.write(filePath, "content".getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return filePath.toFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void configureArtifactDefaults() {
        s3Uploader.setOutputDirectory(testFile.getParent());
        String fileName = testFile.getName();
        int dotIndex = fileName.lastIndexOf('.');
        s3Uploader.setWarName(dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName);
        s3Uploader.setExtension(dotIndex > 0 ? fileName.substring(dotIndex + 1) : "jar");
    }

    private void configureRequiredSettings() {
        configureArtifactDefaults();
        s3Uploader.setRegion(Region.EU_WEST_3.id());
        s3Uploader.setBucket("bucket");
        s3Uploader.setPath("folder/");
    }

    private void configureStaticCredentials() {
        configureRequiredSettings();
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
    }

    @Test
    @DisplayName("S3Uploader -> Subida del archivo al bucket")
    public void executeTest() {
        configureStaticCredentials();
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        Mockito.when(s3Client.putObjectAcl(Mockito.any(PutObjectAclRequest.class)))
                .thenReturn(PutObjectAclResponse.builder().build());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> Usa grantRead con canonical ids configurados")
    public void executeAddsGrantReadAclTest() {
        configureStaticCredentials();
        s3Uploader.setCannonicalIds(new String[]{" id1 ", "id2"});
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        ArgumentCaptor<PutObjectAclRequest> aclCaptor = ArgumentCaptor.forClass(PutObjectAclRequest.class);
        Mockito.when(s3Client.putObjectAcl(aclCaptor.capture()))
                .thenReturn(PutObjectAclResponse.builder().build());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
        Assertions.assertEquals("id=\"id1\",id=\"id2\"", aclCaptor.getValue().grantRead());
    }

    @Test
    @DisplayName("S3Uploader -> Usa grantRead con canonicalIds alias configurado")
    public void executeAddsGrantReadAclWithCanonicalIdsAliasTest() {
        configureStaticCredentials();
        s3Uploader.setCanonicalIds(new String[]{" id1 ", "id2"});
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        ArgumentCaptor<PutObjectAclRequest> aclCaptor = ArgumentCaptor.forClass(PutObjectAclRequest.class);
        Mockito.when(s3Client.putObjectAcl(aclCaptor.capture()))
                .thenReturn(PutObjectAclResponse.builder().build());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
        Assertions.assertEquals("id=\"id1\",id=\"id2\"", aclCaptor.getValue().grantRead());
    }

    @Test
    @DisplayName("S3Uploader -> Omite ACL cuando los canonical ids están vacíos")
    public void executeSkipsAclWhenCanonicalIdsEmptyTest() {
        configureStaticCredentials();
        s3Uploader.setCannonicalIds(new String[]{"   ", ""});
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
        Mockito.verify(s3Client, Mockito.never()).putObjectAcl(Mockito.any(PutObjectAclRequest.class));
    }

    @Test
    @DisplayName("S3Uploader -> Desactiva aviso deprecación SDK v1 cuando se configura")
    public void disableSdkV1DeprecationAnnouncementTest() {
        s3Uploader.setDisableSdkV1DeprecationAnnouncement(true);
        configureStaticCredentials();
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
        Assertions.assertEquals("true", System.getProperty("aws.java.v1.disableDeprecationAnnouncement"));
    }

    @Test
    @DisplayName("S3Uploader -> Usa request body con longitud cuando se solicita progreso")
    public void executeWithProgressListenerTest() {
        configureStaticCredentials();
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
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
        configureStaticCredentials();
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(null);
        Assertions.assertThrows(MojoExecutionException.class, s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> SdkClientException en la subida del archivo al bucket")
    public void executeSdkClientExceptionUploadingTest() {
        configureStaticCredentials();
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenThrow(SdkClientException.builder().message("Error").build());
        Assertions.assertThrows(MojoFailureException.class, s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> AmazonServiceException en la subida del archivo al bucket")
    public void executeAmazonServiceExceptionUploadingTest() {
        configureStaticCredentials();
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Error").build());
        Assertions.assertThrows(MojoFailureException.class, s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> SdkClientException al añadirle permisos al archivo subido")
    public void executeSdkClientExceptionAclTest() {
        configureStaticCredentials();
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        Mockito.when(s3Client.putObjectAcl(Mockito.any(PutObjectAclRequest.class)))
                .thenThrow(SdkClientException.builder().message("Error").build());
        Assertions.assertThrows(MojoFailureException.class, s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> AmazonServiceException al añadirle permisos al archivo subido")
    public void executeAmazonServiceExceptionAclTest() {
        configureStaticCredentials();
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        Mockito.when(s3Client.putObjectAcl(Mockito.any(PutObjectAclRequest.class)))
                .thenThrow(S3Exception.builder().message("Error").build());
        Assertions.assertThrows(MojoFailureException.class, s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> Archivo no encontrado")
    public void executeTestArtifactNotFound() {
        configureArtifactDefaults();
        s3Uploader.setWarName("missing");
        s3Uploader.setExtension("war");
        Assertions.assertThrows(MojoExecutionException.class, s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> Error cuando bucket está vacío")
    public void executeFailsWhenBucketEmpty() {
        configureRequiredSettings();
        s3Uploader.setBucket("  ");
        Assertions.assertThrows(MojoExecutionException.class, s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> Error cuando region está vacío")
    public void executeFailsWhenRegionEmpty() {
        configureRequiredSettings();
        s3Uploader.setRegion(" ");
        Assertions.assertThrows(MojoExecutionException.class, s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> Error cuando path está vacío")
    public void executeFailsWhenPathEmpty() {
        configureRequiredSettings();
        s3Uploader.setPath("  ");
        Assertions.assertThrows(MojoExecutionException.class, s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> Error cuando accessKey o secretKey faltan")
    public void executeFailsWhenOnlyAccessKeyPresent() {
        configureRequiredSettings();
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey(" ");
        Assertions.assertThrows(MojoExecutionException.class, s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> Error cuando sessionToken no tiene credenciales")
    public void executeFailsWhenSessionTokenWithoutCredentials() {
        configureRequiredSettings();
        s3Uploader.setSessionToken("token");
        Assertions.assertThrows(MojoExecutionException.class, s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> Permite credenciales por defecto si no hay accessKey/secretKey")
    public void executeAllowsDefaultCredentials() {
        configureRequiredSettings();
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> Normaliza path con separadores y barra final")
    public void executeNormalizesPath() {
        configureStaticCredentials();
        s3Uploader.setPath("folder\\nested");
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        Mockito.when(s3Client.putObject(requestCaptor.capture(), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
        Assertions.assertEquals("folder/nested/" + testFile.getName(), requestCaptor.getValue().key());
    }

    @Test
    @DisplayName("S3Uploader -> Reutiliza el cliente S3")
    public void getS3ClientReusesInstance() {
        S3Client client = Mockito.mock(S3Client.class);
        CountingS3Uploader uploader = new CountingS3Uploader(client);
        S3Client first = uploader.getS3Client();
        S3Client second = uploader.getS3Client();
        Assertions.assertSame(first, second);
        Assertions.assertEquals(1, uploader.getCreateCount());
    }

    private static class CountingS3Uploader extends S3Uploader {
        private final S3Client client;
        private int createCount;

        private CountingS3Uploader(S3Client client) {
            this.client = client;
        }

        @Override
        protected S3Client createS3Client() {
            createCount++;
            return client;
        }

        private int getCreateCount() {
            return createCount;
        }
    }
}
