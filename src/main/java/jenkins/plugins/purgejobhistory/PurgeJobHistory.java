/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.purgejobhistory;

import hudson.Extension;
import hudson.cli.CLICommand;
import hudson.model.Job;
import hudson.model.Run;
import hudson.security.ACL;
import java.io.IOException;
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
    @Option(name = "-r", usage = "Also reset the next build number to 1.")
    public boolean reset = false;

    /**
     * Force delete even builds marked to be kept forever.
     *
     * @since 1.1
     */
    @Option(name = "-f", usage = "Force delete even builds marked to be kept forever.")
    public boolean force = false;

    /**
     * The source item.
     */
    @Argument(metaVar = "JOB", usage = "Name of the job whose history should be purged", required = true)
    public Job<?, ?> job;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getShortDescription() {
        return Messages.PurgeJobHistory_ShortDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int run() throws Exception {
        purge(job, reset, force);
        return 0;
    }

    /**
     * Purges the build history of the specified job.
     *
     * @param job                  the job to purge
     * @param resetNextBuildNumber {@code true} if the next build number should be reset to {@code 1} after the purge
     * @throws IOException if something went wrong.
     * @deprecated use {@link #purge(Job, boolean, boolean)}
     */
    @Deprecated
    public static void purge(Job<?, ?> job, boolean resetNextBuildNumber) throws IOException {
        purge(job, resetNextBuildNumber, false);
    }
    /**
     * Purges the build history of the specified job.
     *
     * @param job                  the job to purge
     * @param resetNextBuildNumber {@code true} if the next build number should be reset to {@code 1} after the purge
     * @param force                {@code true} to delete even builds marked to be kept forever
     * @throws IOException if something went wrong.
     * @since 1.1
     */
    public static void purge(Job<?, ?> job, boolean resetNextBuildNumber, boolean force) throws IOException {
        ACL lastACL = null;
        for (Run<?, ?> run : new ArrayList<Run<?, ?>>(job.getBuilds())) {
            ACL acl = run.getACL();
            if (acl != lastACL) {
                // all known implementations of Run will only do this check once, so this is a worthwhile
                // optimization.
                acl.checkPermission(Run.DELETE);
                lastACL = acl;
            }
            if (!force && run.isKeepLog()) {
                continue;
            }
            if( !run.isBuilding())
                run.delete();
        }
        if (resetNextBuildNumber && job.getLastBuild() == null) {
            job.updateNextBuildNumber(1);
        }
    }
}
