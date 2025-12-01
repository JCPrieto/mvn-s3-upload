package es.jklabs.mvn;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@ExtendWith(MockitoExtension.class)
public class S3UploaderTest {

    @InjectMocks
    S3Uploader s3Uploader;
    @Mock
    private MavenProject project;
    @Mock
    private AmazonS3 amazonS3;
    private static File testFile;

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

    @Test
    @DisplayName("S3Uploader -> Subida del archivo al bucket")
    public void executeTest() {
        s3Uploader.setAmazonS3(amazonS3);
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Regions.EU_WEST_3.getName());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        Mockito.when(amazonS3.putObject(Mockito.any(PutObjectRequest.class))).thenReturn(new PutObjectResult());
        Mockito.when(amazonS3.getObjectAcl(Mockito.anyString(), Mockito.anyString())).thenReturn(new AccessControlList());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
    }

    @Test
    @DisplayName("S3Uploader -> Configura listener de progreso cuando se solicita")
    public void executeWithProgressListenerTest() {
        s3Uploader.setAmazonS3(amazonS3);
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Regions.EU_WEST_3.getName());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        s3Uploader.setShowProgress(true);
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        Mockito.when(amazonS3.putObject(captor.capture())).thenReturn(new PutObjectResult());
        Mockito.when(amazonS3.getObjectAcl(Mockito.anyString(), Mockito.anyString())).thenReturn(new AccessControlList());
        Assertions.assertDoesNotThrow(s3Uploader::execute);
        ProgressListener listener = captor.getValue().getGeneralProgressListener();
        Assertions.assertNotEquals(ProgressListener.NOOP, listener);
    }

    @Test
    @DisplayName("S3Uploader -> Error en la subida del archivo al bucket")
    public void executeErrorUploadingTest() {
        s3Uploader.setAmazonS3(amazonS3);
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Regions.EU_WEST_3.getName());
        s3Uploader.setBucket("bucket");
        Assertions.assertThrows(MojoExecutionException.class, () -> s3Uploader.execute());
    }

    @Test
    @DisplayName("S3Uploader -> SdkClientException en la subida del archivo al bucket")
    public void executeSdkClientExceptionUploadingTest() {
        s3Uploader.setAmazonS3(amazonS3);
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Regions.EU_WEST_3.getName());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        Mockito.when(amazonS3.putObject(Mockito.any(PutObjectRequest.class))).thenThrow(new SdkClientException("Error"));
        Assertions.assertThrows(MojoFailureException.class, () -> s3Uploader.execute());
    }

    @Test
    @DisplayName("S3Uploader -> AmazonServiceException en la subida del archivo al bucket")
    public void executeAmazonServiceExceptionUploadingTest() {
        s3Uploader.setAmazonS3(amazonS3);
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Regions.EU_WEST_3.getName());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        Mockito.when(amazonS3.putObject(Mockito.any(PutObjectRequest.class))).thenThrow(new AmazonServiceException("Error"));
        Assertions.assertThrows(MojoFailureException.class, () -> s3Uploader.execute());
    }

    @Test
    @DisplayName("S3Uploader -> SdkClientException al añadirle permisos al archivo subido")
    public void executeSdkClientExceptionAclTest() {
        s3Uploader.setAmazonS3(amazonS3);
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Regions.EU_WEST_3.getName());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        Mockito.when(amazonS3.putObject(Mockito.any(PutObjectRequest.class))).thenReturn(new PutObjectResult());
        Mockito.when(amazonS3.getObjectAcl(Mockito.anyString(), Mockito.anyString())).thenThrow(new SdkClientException("Error"));
        Assertions.assertThrows(MojoFailureException.class, () -> s3Uploader.execute());
    }

    @Test
    @DisplayName("S3Uploader -> AmazonServiceException al añadirle permisos al archivo subido")
    public void executeAmazonServiceExceptionAclTest() {
        s3Uploader.setAmazonS3(amazonS3);
        s3Uploader.setOutputDirectory(testFile.getParent());
        String[] filename = testFile.getName().split("\\.");
        s3Uploader.setWarName(testFile.getName().replace("." + filename[filename.length - 1], ""));
        s3Uploader.setExtension(filename[filename.length - 1]);
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Regions.EU_WEST_3.getName());
        s3Uploader.setCannonicalIds(new String[]{"cannonicalIds"});
        s3Uploader.setBucket("bucket");
        Mockito.when(amazonS3.putObject(Mockito.any(PutObjectRequest.class))).thenReturn(new PutObjectResult());
        Mockito.when(amazonS3.getObjectAcl(Mockito.anyString(), Mockito.anyString())).thenThrow(new AmazonServiceException("Error"));
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
