package es.jklabs.mvn;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class S3UploaderTest {

    private static final MockedStatic<AmazonS3ClientBuilder> mockStatic = Mockito.mockStatic(AmazonS3ClientBuilder.class);
    @InjectMocks
    S3Uploader s3Uploader;
    @Mock
    private MavenProject project;
    @Mock
    private AmazonS3ClientBuilder builder;
    @Mock
    private AmazonS3 amazonS3;

    @BeforeEach
    public void init() {
        Mockito.when(project.getName()).thenReturn("Fake");
    }

    private void initS3Client() {
        mockStatic
                .when(AmazonS3ClientBuilder::standard)
                .thenReturn(builder);
        Mockito
                .when(builder.withCredentials(Mockito.any()))
                .thenReturn(builder);
        Mockito
                .when(builder.withRegion(Mockito.anyString()))
                .thenReturn(builder);
        Mockito
                .when(builder.build())
                .thenReturn(amazonS3);
    }

    @Test
    @DisplayName("S3Uploader -> Subida del archivo al bucket")
    public void executeTest() {
        initS3Client();
        s3Uploader.setOutputDirectory(System.getProperty("user.home"));
        s3Uploader.setWarName("zipkin");
        s3Uploader.setExtension("jar");
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
    @DisplayName("S3Uploader -> Error en la subida del archivo al bucket")
    public void executeErrorUploadingTest() {
        initS3Client();
        s3Uploader.setOutputDirectory(System.getProperty("user.home"));
        s3Uploader.setWarName("zipkin");
        s3Uploader.setExtension("jar");
        s3Uploader.setAccessKey("accessKey");
        s3Uploader.setSecretKey("secretKey");
        s3Uploader.setRegion(Regions.EU_WEST_3.getName());
        s3Uploader.setBucket("bucket");
        Assertions.assertThrows(MojoExecutionException.class, () -> s3Uploader.execute());
    }

    @Test
    @DisplayName("S3Uploader -> SdkClientException en la subida del archivo al bucket")
    public void executeSdkClientExceptionUploadingTest() {
        initS3Client();
        s3Uploader.setOutputDirectory(System.getProperty("user.home"));
        s3Uploader.setWarName("zipkin");
        s3Uploader.setExtension("jar");
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
        initS3Client();
        s3Uploader.setOutputDirectory(System.getProperty("user.home"));
        s3Uploader.setWarName("zipkin");
        s3Uploader.setExtension("jar");
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
        initS3Client();
        s3Uploader.setOutputDirectory(System.getProperty("user.home"));
        s3Uploader.setWarName("zipkin");
        s3Uploader.setExtension("jar");
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
        initS3Client();
        s3Uploader.setOutputDirectory(System.getProperty("user.home"));
        s3Uploader.setWarName("zipkin");
        s3Uploader.setExtension("jar");
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
