import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class PurgeJobHistoryActionTest {

    @Rule
    JenkinsRule jenkins = new JenkinsRule();

    private String jobName = "PurgeJobHistoryTestJob";
    private String controlMessage = "Hello Test";
    private String shellScript = "echo " + controlMessage;
    private Integer numberOfBuilds = 3


    @Before
    public void setup() {

    }


    @Test
    public void testWithfreeStyleJob() throws Exception {

        FreeStyleProject project = jenkins.createFreeStyleProject(this.jobName);
        project.getBuildersList().add(new Shell(this.shellScript));
        for(int i = 0; i < 3; i++){
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            jenkins.waitUntilNoActivity();
            jenkins.assertLogContains(this.controlMessage,build);
        }



    }

}
