import com.cloudbees.hudson.plugins.folder.computed.DefaultOrphanedItemStrategy;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.*;
import hudson.tasks.LogRotator;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RunWith(Parameterized.class)
public class PurgeJobHistoryActionTest {

    private static Logger logger = Logger.getLogger(PurgeJobHistoryActionTest.class);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public GitSampleRepoRule gitRepo;

    private Integer numberOfBuilds = 3;
    private boolean resetBuildNum;
    private boolean forceDelete;
    private boolean recurse;
    private boolean keepItForever;
    private int expectedBuildNumbers;
    private int expectedNextBuildNumber;


    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {false, false, false, false, 0, 4},
                {false, false, false, true, 3, 4},
                {false, false, true, false, 0, 1},
                {false, false, true, true, 3, 4},
                {false, true, false, false, 0, 4},
                {false, true, false, true, 0, 4},
                {false, true, true, false, 0, 1},
                {false, true, true, true, 0, 1},
                {true, false, false, false, 0, 4},
                {true, false, false, true, 3, 4},
                {true, false, true, false, 0, 1},
                {true, false, true, true, 3, 4},
                {true, true, false, false, 0, 4},
                {true, true, false, true, 0, 4},
                {true, true, true, false, 0, 1},
                {true, true, true, true, 0, 1},
        });
    }

    public PurgeJobHistoryActionTest(boolean recurse, boolean forceDelete, boolean resetBuildNum, boolean keepItForever, int expectedBuildNumbers, int expectedNextBuildNumber) {
        this.recurse = recurse;
        this.resetBuildNum = resetBuildNum;
        this.forceDelete = forceDelete;
        this.keepItForever = keepItForever;
        this.expectedBuildNumbers = expectedBuildNumbers;
        this.expectedNextBuildNumber = expectedNextBuildNumber;
    }

    @Before
    public void setup() {

    }

    @Test
    public void testFreeStyleProject() throws Exception {
        FreeStyleProject freeStyleProject = this.initFreeStyleProject(this.keepItForever);
        this.performTest(freeStyleProject);
    }

    @Test
    public void testWorkflowJob() throws Exception {
        WorkflowJob workflowJob = this.initWorkflowJob(this.keepItForever);
        this.performTest(workflowJob);
    }

    private void performTest(Job job) throws Exception {
        this.performDeleteJobBuildHistory(job, this.resetBuildNum, this.forceDelete, this.recurse);
        List<BuildStatus> builds = this.getBuildStatus(job);
        for (BuildStatus buildStatus : builds) {
            Assert.assertEquals(this.expectedBuildNumbers, buildStatus.getNumberOfBuilds());
            Assert.assertEquals(this.expectedNextBuildNumber, buildStatus.getNextBuildNumber());
        }
    }


    private List<BuildStatus> getBuildStatus(AbstractItem item) {
        if (item instanceof FreeStyleProject) {
            FreeStyleProject freeStyleProject = (FreeStyleProject) item;
            return Arrays.asList(new BuildStatus(freeStyleProject.getBuilds().size(), freeStyleProject.getNextBuildNumber()));
        } else if (item instanceof WorkflowJob) {
            WorkflowJob workflowJob = (WorkflowJob) item;
            return Arrays.asList(new BuildStatus(workflowJob.getBuilds().size(), workflowJob.getNextBuildNumber()));
        } else if (item instanceof WorkflowMultiBranchProject) {
            WorkflowMultiBranchProject workflowMultiBranchProject = (WorkflowMultiBranchProject) item;
            ArrayList<BuildStatus> buildStatuses = new ArrayList<>();
            for (Job job : workflowMultiBranchProject.getAllJobs()) {
                BuildStatus buildStatus = new BuildStatus(job.getBuilds().size(), job.getNextBuildNumber());
                buildStatuses.add(buildStatus);
            }
            return buildStatuses;
        } else if (item instanceof MockFolder) {
            MockFolder folder = (MockFolder) item;
            ArrayList<BuildStatus> buildStatuses = new ArrayList<>();
            for (Item innerItem : folder.getAllItems()) {
                buildStatuses.addAll(getBuildStatus((AbstractItem) innerItem));
            }
            return buildStatuses;
        }
        return new ArrayList<>();

    }


    private FreeStyleProject initFreeStyleProject(boolean keepItForever) throws Exception {
        FreeStyleProject freeStyleProject = this.jenkins.createFreeStyleProject(UUID.randomUUID().toString());
        freeStyleProject.setLogRotator(new LogRotator(null, null, null, null));
        this.generateBuilds(freeStyleProject, keepItForever);
        return freeStyleProject;
    }

    private WorkflowJob initWorkflowJob(boolean keepItForever) throws Exception {
        WorkflowJob workflowJob = this.jenkins.createProject(WorkflowJob.class, UUID.randomUUID().toString());
        workflowJob.setLogRotator(new LogRotator(null, null, null, null));
        this.generateBuilds(workflowJob, keepItForever);
        return workflowJob;
    }

    private WorkflowMultiBranchProject initWorkflowMultiBranchProject(boolean keepItForever) throws Exception {
        GitSampleRepoRule gitRepo = new GitSampleRepoRule();
        gitRepo = new GitSampleRepoRule();
        gitRepo.init();
        gitRepo.write("Jenkinsfile", "");
        gitRepo.git("add", "Jenkinsfile");
        gitRepo.git("commit", "--all", "--message=InitRepoWithFile");
        WorkflowMultiBranchProject workflowMultiBranchProject = this.jenkins.createProject(WorkflowMultiBranchProject.class, UUID.randomUUID().toString());
        workflowMultiBranchProject.getSourcesList().add(new BranchSource(new GitSCMSource(null, this.gitRepo.toString(), "", "none", "", false)));
        workflowMultiBranchProject.setOrphanedItemStrategy(new DefaultOrphanedItemStrategy(true, null, null));
        workflowMultiBranchProject.scheduleBuild2(0);
        this.jenkins.waitUntilNoActivity();
        for (WorkflowJob job : workflowMultiBranchProject.getAllItems(WorkflowJob.class)) {
            this.generateBuilds(job, keepItForever);
        }
        return workflowMultiBranchProject;
    }

    private MockFolder initFolder(boolean keepItForever) throws Exception {
        MockFolder folder = this.jenkins.createFolder(UUID.randomUUID().toString());
        FreeStyleProject folderFreeStyleProject = this.initFreeStyleProject(keepItForever);
        WorkflowJob folderWorkflowJob = this.initWorkflowJob(keepItForever);
        WorkflowMultiBranchProject folderWorkflowMultiBranchProject = this.initWorkflowMultiBranchProject(keepItForever);
        folder.add(folderFreeStyleProject, folderFreeStyleProject.getName());
        folder.add(folderWorkflowJob, folderWorkflowJob.getName());
        folder.add(folderWorkflowMultiBranchProject, folderWorkflowMultiBranchProject.getName());
        return folder;
    }


    private void generateBuilds(Job job, boolean keepItForever) throws Exception {

        int currentNumberOfBuilds = job.getBuilds().size();
        for (int i = currentNumberOfBuilds; i < this.numberOfBuilds; i++) {
            Run run = this.buildJob(job);
            if (keepItForever)
                markBuildKeptForever(run);
        }
        Assert.assertEquals((long) this.numberOfBuilds, job.getBuilds().size());
    }

    private Run buildJob(AbstractItem item) throws Exception {
        Run run = null;
        if (item instanceof FreeStyleProject) {
            run = ((FreeStyleProject) item).scheduleBuild2(0).get();
        } else if (item instanceof WorkflowJob) {
            run = ((WorkflowJob) item).scheduleBuild2(0).get();
        }
        jenkins.waitUntilNoActivity();
        return run;
    }

    private void performDeleteJobBuildHistory(Job job, boolean resetBuildNumber, boolean force, boolean recurse) throws Exception {
        HtmlForm form = jenkins.createWebClient().getPage(job, "purge-job-history").getFormByName("purge");
        form.getInputByName("resetNextBuild").setChecked(resetBuildNumber);
        form.getInputByName("forceDelete").setChecked(force);
        form.getInputByName("recurse").setChecked(recurse);
        jenkins.submit(form);
    }

    private void markBuildKeptForever(Run run) throws IOException, SAXException {
        HtmlPage page = jenkins.createWebClient().getPage(run.getParent(), String.valueOf(run.getNumber()));
        List<HtmlElement> keepBuildForEverButton = page.getByXPath("//button[contains(text(),'this build forever')]");
        keepBuildForEverButton.get(0).click();
        page = jenkins.createWebClient().getPage(run.getParent(), String.valueOf(run.getNumber()));
        Assert.assertTrue(page.getBody().getTextContent().contains("Don't keep this build forever"));
    }

}
