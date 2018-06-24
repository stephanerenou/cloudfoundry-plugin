/**
 * © Copyright 2015 Hewlett Packard Enterprise Development LP
 * Copyright (c) ActiveState 2014 - ALL RIGHTS RESERVED.
 * © 2017 The original author or authors.
 */
package com.hpe.cloudfoundryjenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.hpe.cloudfoundryjenkins.CloudFoundryPushPublisher.ManifestChoice;
import hudson.ProxyConfiguration;
import hudson.model.ItemGroup;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.SSLPeerUnverifiedException;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;

/**
 * Utility methods for interacting with CloudFoundry.
 *
 * @author williamg
 * @author Steven Swor
 */
public class CloudFoundryUtils {

    /**
     * Default manifest file path. (<code>manifest.yml</code>)
     */
    static final String DEFAULT_MANIFEST_PATH = "manifest.yml";

    /**
     * Default plugin timeout (120).
     */
    static final int DEFAULT_PLUGIN_TIMEOUT = 120;

    /**
     * Builds a proxy configuration for the target URL.
     * @param targetURL the target url
     * @return either {@link Optional#empty()} or the proxy configuration.
     */
    static Optional<org.cloudfoundry.reactor.ProxyConfiguration> buildProxyConfiguration(URL targetURL) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) return Optional.empty();

        ProxyConfiguration proxyConfig = jenkins.proxy;
        if (proxyConfig == null) {
            return Optional.empty();
        }

        String host = targetURL.getHost();
        for (Pattern p : proxyConfig.getNoProxyHostPatterns()) {
            if (p.matcher(host).matches()) {
                return Optional.empty();
            }
        }

        return Optional.of(org.cloudfoundry.reactor.ProxyConfiguration.builder()
            .host(proxyConfig.name)
            .port(proxyConfig.port)
            .build());
    }

    /**
     * Tests the connection settings.
     * @param context the context
     * @param target the cloudfoundry target
     * @param credentialsId the ID of the credentials to use for cloudfoundry
     * @param organization the cloudfoundry organization to use
     * @param cloudSpace the cloudfoundry space to use
     * @param selfSigned {@code true} to ignore ssl validation errors
     * @return the form validation result
     */
    static FormValidation doTestConnection(final ItemGroup context,
                                               final String target,
                                               final String credentialsId,
                                               final String organization,
                                               final String cloudSpace,
                                               final boolean selfSigned) {
        try {
            List<StandardUsernamePasswordCredentials> standardCredentials = CredentialsProvider.lookupCredentials(
                    StandardUsernamePasswordCredentials.class,
                    context,
                    ACL.SYSTEM,
                    URIRequirementBuilder.fromUri(target).build());

            StandardUsernamePasswordCredentials credentials =
                    CredentialsMatchers.firstOrNull(standardCredentials, CredentialsMatchers.withId(credentialsId));

            CloudFoundryPushTask task = new CloudFoundryPushTask(target, organization, cloudSpace, credentialsId, selfSigned, DEFAULT_PLUGIN_TIMEOUT, Collections.emptyList(), ManifestChoice.defaultManifestFileConfig());

            ConnectionContext connectionContext = task.createConnectionContext(null, null, TaskListener.NULL);

            PasswordGrantTokenProvider.Builder tokenProviderBuilder = PasswordGrantTokenProvider.builder();
            if (credentials != null) {
              tokenProviderBuilder = tokenProviderBuilder.username(credentials.getUsername())
                .password(Secret.toString(credentials.getPassword()));
            }
            TokenProvider tokenProvider = tokenProviderBuilder.build();

            CloudFoundryClient client = ReactorCloudFoundryClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();

            client.info().get(GetInfoRequest.builder().build())
                .timeout(Duration.ofSeconds(DEFAULT_PLUGIN_TIMEOUT))
                .block();
            URL targetUrl = task.targetUrl(target);
            List<String> warnings = new ArrayList<>();
            if (!targetUrl.getHost().startsWith("api.")) {
              warnings.add("Your target's hostname does not start with \"api.\".<br />" +
                                "Make sure it is the real API endpoint and not a redirection, " +
                                "or it may cause some problems.");
            }
            if (!StringUtils.isEmpty(targetUrl.getPath()) && !targetUrl.getPath().equals("/")) {
              warnings.add("Your target specifies a path which will be ignored when making CloudFoundry API calls");
            }
            if (warnings.isEmpty()) {
                return FormValidation.okWithMarkup("<b>Connection successful!</b>");
            } else {
                return FormValidation.warningWithMarkup("Connection successful, but:<ul><li>" + warnings.stream().collect(Collectors.joining("</li><li>")) + "</li></ul>");
            }
        } catch (MalformedURLException e) {
            return FormValidation.error("Malformed target URL");
        } catch(RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ClientV2Exception) {
              return FormValidation.error("Client error. Code=%i, Error Code=%s, Description=%s", ((ClientV2Exception)e).getCode(), ((ClientV2Exception)e).getErrorCode(), ((ClientV2Exception)e).getDescription());
            } else if (cause instanceof UnknownHostException) {
              return FormValidation.error("Unknown host");
            } else if (cause instanceof SSLPeerUnverifiedException) {
              return FormValidation.error("Target's certificate is not verified " +
                        "(Add it to Java's keystore, or check the \"Allow self-signed\" box)");
            } else {
              return FormValidation.error(e, "Unknown error");
            }
        } catch (Exception e) {
            return FormValidation.error(e, "Unknown Exception");
        }
    }
}
