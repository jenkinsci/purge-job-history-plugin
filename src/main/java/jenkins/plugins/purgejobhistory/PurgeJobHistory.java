package jenkins.plugins.purgejobhistory;

import hudson.Extension;
import hudson.cli.CLICommand;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import java.util.ArrayList;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * @author Stephen Connolly
 */
@Extension
public class PurgeJobHistory extends CLICommand {
    
    /**
     * Follows the progress of the operation
     */
    @Option(name = "-r", usage = "Also reset the next build number.")
    public boolean reset = false;

    /**
     * The source item.
     */
    @Argument(metaVar = "JOB", usage = "Name of the job to purge", required = true)
    public TopLevelItem job;

    
    @Override
    public String getShortDescription() {
        return Messages.PurgeJobHistory_ShortDescription();
    }

    @Override
    protected int run() throws Exception {
        job.checkPermission(Item.DELETE);
        Job<?,?> j = (Job<?, ?>) job;
        for (Run<?,?> run: new ArrayList<Run<?, ?>>(j.getBuilds())) {
            run.delete();
        }
        if (reset) {
            j.updateNextBuildNumber(1);
        }
        return 0;
    }
}
