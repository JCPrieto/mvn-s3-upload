package es.jklabs.mvn;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "s3uploader", defaultPhase = LifecyclePhase.DEPLOY)
public class S3Uploader extends AbstractMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        //ToDo
    }
}
