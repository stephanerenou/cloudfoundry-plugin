 /*
  * © 2017 The original author or authors.
  */
package com.hpe.cloudfoundryjenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Jenkins build step for CloudFoundry push.
 *
 * @author Steven Swor
 */
public class CloudFoundryPushBuilder extends Builder {

    public String target;
    public String organization;
    public String cloudSpace;
    public String credentialsId;
    public boolean selfSigned;
    public int pluginTimeout;
    public List<CloudFoundryPushPublisher.Service> servicesToCreate;
    public CloudFoundryPushPublisher.ManifestChoice manifestChoice;

    @DataBoundConstructor
    public CloudFoundryPushBuilder(String target, String organization, String cloudSpace,
                                   String credentialsId, boolean selfSigned,
                                   int pluginTimeout, List<CloudFoundryPushPublisher.Service> servicesToCreate,
                                   CloudFoundryPushPublisher.ManifestChoice manifestChoice) {
        this.target = target;
        this.organization = organization;
        this.cloudSpace = cloudSpace;
        this.credentialsId = credentialsId;
        this.selfSigned = selfSigned;
        if (pluginTimeout == 0) {
            this.pluginTimeout = CloudFoundryUtils.DEFAULT_PLUGIN_TIMEOUT;
        } else {
            this.pluginTimeout = pluginTimeout;
        }
        if (servicesToCreate == null) {
            this.servicesToCreate = new ArrayList<>();
        } else {
            this.servicesToCreate = servicesToCreate;
        }
        if (manifestChoice == null) {
            this.manifestChoice = CloudFoundryPushPublisher.ManifestChoice.defaultManifestFileConfig();
        } else {
            this.manifestChoice = manifestChoice;
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        CloudFoundryPushTask task = new CloudFoundryPushTask(target, organization, cloudSpace, credentialsId, selfSigned, pluginTimeout, servicesToCreate, manifestChoice);
        return task.perform(build, launcher, listener);
    }

    @Extension
    public static final class DescriptorImpl extends AbstractCloudFoundryPushDescriptor<Builder> {
    }
}
