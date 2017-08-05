 /*
  * © 2017 The original author or authors.
  */
package com.hpe.cloudfoundryjenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.model.AbstractProject;
import hudson.model.Describable;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.net.MalformedURLException;
import java.net.URL;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

/**
 * Extension point descriptor for cloudfoundry push.
 *
 * @author williamg
 * @author Steven Swor
 */
public abstract class AbstractCloudFoundryPushDescriptor<T extends BuildStep & Describable<T>> extends BuildStepDescriptor<T> {

    public static final int DEFAULT_MEMORY = 512;
    public static final int DEFAULT_INSTANCES = 1;
    public static final int DEFAULT_TIMEOUT = 60;
    public static final String DEFAULT_STACK = null; // null stack means it uses the default stack of the target

    protected AbstractCloudFoundryPushDescriptor(Class<? extends T> clazz) {
        super(clazz);
    }

    protected AbstractCloudFoundryPushDescriptor() {
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Push to Cloud Foundry";
    }

    /**
     * This method is called to populate the credentials list on the Jenkins config page.
     */
    @SuppressWarnings(value = "unused")
    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @QueryParameter(value = "target") final String target) {
        StandardListBoxModel result = new StandardListBoxModel();
        result.withEmptySelection();
        result.withMatching(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class), CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, context, ACL.SYSTEM, URIRequirementBuilder.fromUri(target).build()));
        return result;
    }

    /**
     * This method is called when the "Test Connection" button is clicked on the Jenkins config page.
     */
    @SuppressWarnings(value = "unused")
    public FormValidation doTestConnection(@AncestorInPath ItemGroup context, @QueryParameter(value = "target") final String target, @QueryParameter(value = "credentialsId") final String credentialsId, @QueryParameter(value = "organization") final String organization, @QueryParameter(value = "cloudSpace") final String cloudSpace, @QueryParameter(value = "selfSigned") final boolean selfSigned) {
        return CloudFoundryUtils.doTestConnection(context, target, credentialsId, organization, cloudSpace, selfSigned);
    }

    @SuppressWarnings(value = "unused")
    public FormValidation doCheckTarget(@QueryParameter String value) {
        if (!value.isEmpty()) {
            try {
                new URL("https://" + value);
            } catch (MalformedURLException e) {
                return FormValidation.error("Malformed URL");
            }
        }
        return FormValidation.validateRequired(value);
    }

    @SuppressWarnings(value = "unused")
    public FormValidation doCheckCredentialsId(@QueryParameter String value) {
        return FormValidation.validateRequired(value);
    }

    @SuppressWarnings(value = "unused")
    public FormValidation doCheckOrganization(@QueryParameter String value) {
        return FormValidation.validateRequired(value);
    }

    @SuppressWarnings(value = "unused")
    public FormValidation doCheckCloudSpace(@QueryParameter String value) {
        return FormValidation.validateRequired(value);
    }

    @SuppressWarnings(value = "unused")
    public FormValidation doCheckPluginTimeout(@QueryParameter String value) {
        return FormValidation.validatePositiveInteger(value);
    }

    @SuppressWarnings(value = "unused")
    public FormValidation doCheckMemory(@QueryParameter String value) {
        return FormValidation.validatePositiveInteger(value);
    }

    @SuppressWarnings(value = "unused")
    public FormValidation doCheckInstances(@QueryParameter String value) {
        return FormValidation.validatePositiveInteger(value);
    }

    @SuppressWarnings(value = "unused")
    public FormValidation doCheckTimeout(@QueryParameter String value) {
        return FormValidation.validatePositiveInteger(value);
    }

    @SuppressWarnings(value = "unused")
    public FormValidation doCheckAppName(@QueryParameter String value) {
        return FormValidation.validateRequired(value);
    }

    @SuppressWarnings(value = "unused")
    public FormValidation doCheckHostname(@QueryParameter String value) {
        return FormValidation.validateRequired(value);
    }

}
