package io.github.jcprieto.mvn;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectAclRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
    private String[] cannonicalIds = new String[0];

    @Parameter(property = "aws.s3.showProgress", defaultValue = "false")
    private boolean showProgress;

    @Parameter(property = "aws.s3.disableSdkV1DeprecationAnnouncement", defaultValue = "false")
    private boolean disableSdkV1DeprecationAnnouncement;

    private S3Client s3Client;

    public void execute() throws MojoExecutionException, MojoFailureException {
        maybeDisableAwsSdkV1DeprecationAnnouncement();
        validateConfiguration();
        path = normalizePath(path);
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
            S3Client s3Client3 = getS3Client();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(path + file.getName())
                    .build();
            if (showProgress) {
                if (file.length() <= 0) {
                    getLog().info("Artifact size is 0 bytes, skipping progress logging");
                }
            }
            getLog().info("Uploading artifact to: " + bucket + FileSystems.getDefault().getSeparator() + path + FileSystems.getDefault().getSeparator() + file.getName());
            RequestBody requestBody = buildRequestBody(file);
            if (s3Client3.putObject(request, requestBody) != null) {
                getLog().info("Artifact uploaded");
                applyAclIfConfigured(s3Client3, file);
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

    private void validateConfiguration() throws MojoExecutionException {
        if (isBlank(bucket)) {
            throw new MojoExecutionException("Bucket is required (aws.s3.bucket)");
        }
        if (isBlank(region)) {
            throw new MojoExecutionException("Region is required (aws.s3.region)");
        }
        if (isBlank(path)) {
            throw new MojoExecutionException("Path is required (aws.s3.path)");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizePath(String value) {
        String normalized = value.trim().replace('\\', '/');
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private void applyAclIfConfigured(S3Client s3Client3, File file) {
        if (cannonicalIds.length == 0) {
            return;
        }
        List<String> canonicalIds = new ArrayList<>();
        for (String cannonicalId : cannonicalIds) {
            if (cannonicalId == null) {
                continue;
            }
            String trimmed = cannonicalId.trim();
            if (!trimmed.isEmpty()) {
                canonicalIds.add(trimmed);
            }
        }
        if (canonicalIds.isEmpty()) {
            getLog().warn("Skipping ACL update: canonicalIds is empty after trimming");
            return;
        }
        PutObjectAclRequest putObjectAclRequest = PutObjectAclRequest.builder()
                .bucket(bucket)
                .key(path + file.getName())
                .grantRead(buildGrantReadHeader(canonicalIds))
                .build();
        s3Client3.putObjectAcl(putObjectAclRequest);
        getLog().info("Permissions added");
    }

    private String buildGrantReadHeader(List<String> canonicalIds) {
        return canonicalIds.stream()
                .map(id -> "id=\"" + id + "\"")
                .collect(Collectors.joining(","));
    }

    private RequestBody buildRequestBody(File file) {
        if (!showProgress || file.length() <= 0) {
            return RequestBody.fromFile(file.toPath());
        }
        ProgressTracker tracker = new ProgressTracker(getLog(), file.length());
        return RequestBody.fromContentProvider(() -> openProgressInputStream(file, tracker), file.length(), "application/octet-stream");
    }

    private InputStream openProgressInputStream(File file, ProgressTracker tracker) {
        try {
            return new ProgressInputStream(Files.newInputStream(file.toPath()), tracker);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open artifact for upload: " + file, e);
        }
    }

    private S3Client getS3Client() {
        if (s3Client != null) {
            return s3Client;
        }
        AwsBasicCredentials awsCreds = AwsBasicCredentials.builder()
                .accessKeyId(accessKey)
                .secretAccessKey(secretKey)
                .build();
        AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(awsCreds);
        return S3Client.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.of(region))
                .build();
    }

    private void maybeDisableAwsSdkV1DeprecationAnnouncement() {
        if (!disableSdkV1DeprecationAnnouncement) {
            return;
        }
        if (!"true".equalsIgnoreCase(System.getProperty("aws.java.v1.disableDeprecationAnnouncement"))) {
            System.setProperty("aws.java.v1.disableDeprecationAnnouncement", "true");
        }
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

    public void setPath(String path) {
        this.path = path;
    }

    private static class ProgressTracker {
        private final org.apache.maven.plugin.logging.Log log;
        private final long totalBytes;
        private final AtomicLong transferredBytes = new AtomicLong(0);
        private final AtomicInteger lastPercentageEmitted = new AtomicInteger(0);

        private ProgressTracker(org.apache.maven.plugin.logging.Log log, long totalBytes) {
            this.log = log;
            this.totalBytes = totalBytes;
        }

        private void onBytesRead(int bytesRead) {
            if (bytesRead <= 0) {
                return;
            }
            long current = Math.min(transferredBytes.addAndGet(bytesRead), totalBytes);
            int percentage = (int) Math.min((current * 100) / totalBytes, 100);
            if (percentage >= lastPercentageEmitted.get() + 10 || current == totalBytes) {
                lastPercentageEmitted.set(percentage);
                log.info("Upload progress: " + percentage + "% (" + current + "/" + totalBytes + " bytes)");
            }
        }
    }

    private static class ProgressInputStream extends FilterInputStream {
        private final ProgressTracker tracker;

        private ProgressInputStream(InputStream in, ProgressTracker tracker) {
            super(in);
            this.tracker = tracker;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                tracker.onBytesRead(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int bytesRead = super.read(buffer, offset, length);
            if (bytesRead > 0) {
                tracker.onBytesRead(bytesRead);
            }
            return bytesRead;
        }
    }

    public void setShowProgress(boolean showProgress) {
        this.showProgress = showProgress;
    }

    public void setDisableSdkV1DeprecationAnnouncement(boolean disableSdkV1DeprecationAnnouncement) {
        this.disableSdkV1DeprecationAnnouncement = disableSdkV1DeprecationAnnouncement;
    }
}
