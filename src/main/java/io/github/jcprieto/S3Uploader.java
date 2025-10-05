package io.github.jcprieto;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.Arrays;

@Mojo(name = "s3uploader", defaultPhase = LifecyclePhase.DEPLOY)
public class S3Uploader extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private String outputDirectory;

    @Parameter(defaultValue = "${project.build.finalName}", required = true, readonly = true)
    private String warName;

    @Parameter(property = "artifact.extension", defaultValue = "jar", required = true)
    private String extension;

    @Parameter(property = "aws.s3.bucket", required = true)
    private String bucket;

    @Parameter(property = "aws.s3.region", required = true)
    private String region;

    @Parameter(property = "aws.s3.path", required = true)
    private String path;

    @Parameter(property = "aws.s3.accesskey", required = true)
    private String accessKey;

    @Parameter(property = "aws.s3.secretkey", required = true)
    private String secretKey;

    @Parameter(property = "aws.s3.cannonicalIds")
    private String[] cannonicalIds;

    public void execute() throws MojoExecutionException, MojoFailureException {
        String ruta = outputDirectory + FileSystems.getDefault().getSeparator() + warName + "." + extension;
        getLog().info("Uploading " + project.getName() + " : " + ruta);
        File file = new File(ruta);
        if (file.exists()) {
            getLog().info("Getting artifact: " + file);
            upload(file);
        } else {
            throw new MojoExecutionException("Artifact not found");
        }
    }

    private void upload(File file) throws MojoFailureException, MojoExecutionException {
        try {
            AmazonS3 s3 = getAmazonS3();
            PutObjectRequest request = new PutObjectRequest(bucket, path + file.getName(), file);
            getLog().info("Uploading artifact to: " + bucket + FileSystems.getDefault().getSeparator() + path + FileSystems.getDefault().getSeparator() + file.getName());
            if (s3.putObject(request) != null) {
                getLog().info("Artifact uploaded");
                if (cannonicalIds.length != 0) {
                    AccessControlList acl = s3.getObjectAcl(bucket, path + file.getName());
                    Arrays.stream(cannonicalIds).forEach(c ->
                            acl.grantPermission(new CanonicalGrantee(c), Permission.Read));
                    s3.setObjectAcl(bucket, path + file.getName(), acl);
                    getLog().info("Permissions added");
                }
                getLog().info("Upload succesfull");
            } else {
                throw new MojoExecutionException(path + file.getName() + " not uploaded to " + bucket);
            }
        } catch (MojoExecutionException m) {
            throw m;
        } catch (Exception e) {
            throw new MojoFailureException(path + file.getName() + " not uploaded to " + bucket, e);
        }
    }

    private AmazonS3 getAmazonS3() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(region)
                .build();
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void setWarName(String warName) {
        this.warName = warName;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setCannonicalIds(String[] cannonicalIds) {
        this.cannonicalIds = cannonicalIds;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
