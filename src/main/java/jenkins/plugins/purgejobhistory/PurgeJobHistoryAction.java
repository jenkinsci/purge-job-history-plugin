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
import hudson.model.AbstractItem;
import hudson.model.Action;
import hudson.model.Job;
import hudson.util.HttpResponses;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import jenkins.model.TransientActionFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * An {@link Action} to allow the user to purge the job history.
 */
public class PurgeJobHistoryAction implements Action {

    private PurgeJobHistory purgeJobHistory = new PurgeJobHistory();

    /**
     * The {@link Job} we are attached to.
     */
    private AbstractItem item;

    public PurgeJobHistoryAction(AbstractItem item){
        this.item = item;
    }


    public AbstractItem getItem() {
        return item;
    }

    /**
     * {@inheritDoc}
     */
    public String getIconFileName() {
        //Check permission first.
        if(purgeJobHistory.checkPermission(this.item)) {
            return StaticValues.iconPath;
        } else {
            return null;
        }
    }

    public String getDisplayName() {
        return StaticValues.displayName;
    }

    public String getUrlName() {
        return StaticValues.urlName;
    }

    @RequirePOST
    @Restricted(NoExternalUse.class)
    public HttpResponse doDoPurge(@QueryParameter(value = "resetNextBuild") boolean resetNextBuild,
                                  @QueryParameter(value = "forceDelete") boolean forceDelete,
                                  @QueryParameter(value = "recurse") boolean recurse)
            throws IOException {
        purgeJobHistory.purge(item,resetNextBuild, forceDelete, recurse);
        return HttpResponses.redirectTo("..");
    }

    @Extension
    public static class ActionInjector extends TransientActionFactory<AbstractItem> {

        @Override
        public Class<AbstractItem> type() {
            return AbstractItem.class;
        }

        @Nonnull
        @Override
        public Collection<? extends Action> createFor(@Nonnull AbstractItem abstractItem) {
            return Collections.singleton(new PurgeJobHistoryAction(abstractItem));
        }
    }
}
