package jenkins.plugins.purgejobhistory;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.HttpResponses;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import jenkins.model.TransientActionFactory;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * @author Stephen Connolly
 */
public class PurgeJobHistoryAction implements Action {

    private final Job<?, ?> job;

    public PurgeJobHistoryAction(Job<?, ?> job) {
        this.job = job;
    }

    public Job<?, ?> getJob() {
        return job;
    }

    public String getIconFileName() {
        return "/plugin/purge-job-history/images/24x24/purge-job-history.png";
    }

    public String getDisplayName() {
        return Messages.PurgeJobHistoryAction_DisplayName();
    }

    public String getUrlName() {
        return "purge-history";
    }

    @RequirePOST
    public HttpResponse doDoPurge(StaplerRequest request, @QueryParameter boolean resetNextBuild) throws IOException {
        job.checkPermission(Item.DELETE);
        for (Run<?, ?> run : new ArrayList<Run<?, ?>>(job.getBuilds())) {
            run.delete();
        }
        if (resetNextBuild) {
            job.updateNextBuildNumber(1);
        }
        return HttpResponses.redirectTo("..");
    }

    @Extension
    public static class TransientActionFactoryImpl extends TransientActionFactory<Job> {

        @Override
        public Class<Job> type() {
            return Job.class;
        }

        @Nonnull
        @Override
        public Collection<? extends Action> createFor(Job target) {
            return Collections.singleton(new PurgeJobHistoryAction(target));
        }
    }
}
