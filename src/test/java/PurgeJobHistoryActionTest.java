import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.Build;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.tasks.LogRotator;
import hudson.tasks.Shell;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PurgeJobHistoryActionTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private String jobName = "PurgeJobHistoryTestJob";
    private String controlMessage = "Hello Test";
    private String shellScript = "echo " + controlMessage;
    private Integer numberOfBuilds = 3;


    @Before
    public void setup() {

    }

    @Test
    public void testWithFreeStyleJobWithResetTrueForceTrue() throws Exception {
        FreeStyleProject project = this.createProject();
        this.generateBuilds(project);
        performDeleteJobBuildHistory(project, true, true);
        Assert.assertEquals(0, project.getBuilds().size());
    }

    @Test
    public void testWithFreeStyleJobWithResetFalseForceFalse() throws Exception {
        FreeStyleProject project = this.createProject();
        this.generateBuilds(project);
        performDeleteJobBuildHistory(project, false, false);
        Assert.assertEquals((long) this.numberOfBuilds, project.getBuilds().size());
        FreeStyleBuild build = this.buildJob(project);
        Assert.assertEquals(4, build.getNumber());
    }

    @Test
    public void testWithFreeStyleJobWithResetTrueForceFalse() throws Exception {
        FreeStyleProject project = this.createProject();
        this.generateBuilds(project);
        performDeleteJobBuildHistory(project, false, false);
        Assert.assertEquals((long) this.numberOfBuilds, project.getBuilds().size());
        FreeStyleBuild build = this.buildJob(project);
        Assert.assertEquals(1, build.getNumber());
    }

    @Test
    public void testWithFreeStyleJobWithResetFalseForceTrue() throws Exception {
        FreeStyleProject project = this.createProject();
        this.generateBuilds(project);
        performDeleteJobBuildHistory(project, false, false);
        Assert.assertEquals((long) 0, project.getBuilds().size());
        FreeStyleBuild build = this.buildJob(project);
        Assert.assertEquals(4, build.getNumber());
    }

    private void generateBuilds(Project project) throws Exception {
        for (int i = 0; i < this.numberOfBuilds; i++) {
            FreeStyleBuild build = this.buildJob(project);
            jenkins.assertLogContains(this.controlMessage, build);
            markBuildKeptForever(project,build);
        }
        Assert.assertEquals((long) this.numberOfBuilds, project.getBuilds().size());
    }

    private FreeStyleProject createProject() throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject(this.jobName + UUID.randomUUID().toString());
        project.setLogRotator(new LogRotator(null, null, null, null));
        project.getBuildersList().add(new Shell(this.shellScript));
        return project;
    }

    private FreeStyleBuild buildJob(Project project) throws Exception {
        FreeStyleBuild build = (FreeStyleBuild) project.scheduleBuild2(0).get();
        jenkins.waitUntilNoActivity();
        return build;
    }

    private void performDeleteJobBuildHistory(Project project, boolean resetBuildNumner, boolean force) throws Exception {
        HtmlForm form = jenkins.createWebClient().getPage(project, "purge-job-history").getFormByName("purge");
        form.getInputByName("resetNextBuild").setChecked(resetBuildNumner);
        form.getInputByName("forceDelete").setChecked(force);
        jenkins.submit(form);
    }

    private void markBuildKeptForever(Project project, Build build) throws IOException, SAXException {
        HtmlPage page = jenkins.createWebClient().getPage(project, String.valueOf(build.getNumber()));
        List<HtmlElement> keepBuildForEverButton = page.getByXPath("//button[contains(text(),'this build forever')]");
        keepBuildForEverButton.get(0).click();
        page = jenkins.createWebClient().getPage(project, String.valueOf(build.getNumber()));
        Assert.assertTrue(page.getBody().getTextContent().contains("Don't keep this build forever"));

    }

}
