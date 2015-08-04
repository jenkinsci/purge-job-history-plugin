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
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import hudson.security.Permission;
import hudson.util.HttpResponses;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import jenkins.model.TransientActionFactory;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * An {@link Action} to allow the user to purge the job history.
 */
public class PurgeJobHistoryAction implements Action {

    /**
     * The permission used to restrict access to the action.
     */
    public static Permission PERMISSION = Run.DELETE;

    /**
     * The {@link Job} we are attached to.
     */
    private final Job<?, ?> job;

    /**
     * Constructor.
     *
     * @param job the job we are an action for,
     */
    public PurgeJobHistoryAction(Job<?, ?> job) {
        this.job = job;
    }

    /**
     * Gets the job we are attached to.
     *
     * @return the job we are attached to.
     */
    public Job<?, ?> getJob() {
        return job;
    }

    /**
     * {@inheritDoc}
     */
    public String getIconFileName() {
        // if you don't have the permission on the last build then we cannot purge all, so you don't have permission
        Run<?, ?> lastBuild = job.getLastBuild();
        return lastBuild != null && lastBuild.hasPermission(PERMISSION) 
                ? "/plugin/purge-job-history/images/24x24/purge-job-history.png" 
                : null;
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        return Messages.PurgeJobHistoryAction_DisplayName();
    }

    /**
     * {@inheritDoc}
     */
    public String getUrlName() {
        return "purge-history";
    }

    /**
     * Does the actual purging when triggered via the UI.
     *
     * @param request        the request.
     * @param resetNextBuild the reset flag.
     * @return the response.
     * @throws IOException if something goes wrong.
     */
    @RequirePOST
    public HttpResponse doDoPurge(@QueryParameter boolean resetNextBuild) throws IOException {
        PurgeJobHistory.purge(job, resetNextBuild);
        return HttpResponses.redirectTo("..");
    }

    /**
     * Add the action to every job.
     */
    @Extension
    public static class TransientActionFactoryImpl extends TransientActionFactory<Job> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<Job> type() {
            return Job.class;
        }

        /**
         * {@inheritDoc}
         */
        @Nonnull
        @Override
        public Collection<? extends Action> createFor(Job target) {
            return Collections.singleton(new PurgeJobHistoryAction(target));
        }
    }
}
