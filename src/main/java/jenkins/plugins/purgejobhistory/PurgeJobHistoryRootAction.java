package jenkins.plugins.purgejobhistory;

import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Action;
import hudson.model.RootAction;
import hudson.util.HttpResponses;
import jenkins.model.TransientActionFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

@Extension
public class PurgeJobHistoryRootAction implements RootAction {

    private AbstractItem item;

    public PurgeJobHistoryRootAction() {}

    public PurgeJobHistoryRootAction(AbstractItem item) {
        this.item = item;
    }

    PurgeJobHistory purgeJobHistory = new PurgeJobHistory();

    @CheckForNull
    @Override
    public String getIconFileName() {
        if ( this.item != null)
            return null;
        else
            return "symbol-trash-bin-outline plugin-ionicons-api";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return StaticValues.displayName;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return StaticValues.urlName;
    }

    @RequirePOST
    @Restricted(NoExternalUse.class)
    public HttpResponse doDoPurge(@QueryParameter(value = "resetNextBuild") boolean resetNextBuild,
                                  @QueryParameter(value = "forceDelete") boolean forceDelete)
            throws IOException {
        purgeJobHistory.purge(resetNextBuild,forceDelete,true);
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
            return Collections.singleton(new PurgeJobHistoryRootAction(abstractItem));
        }
    }
}
