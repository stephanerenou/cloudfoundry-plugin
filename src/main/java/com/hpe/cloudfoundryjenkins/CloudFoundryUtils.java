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
import hudson.ProxyConfiguration;
import hudson.model.Hudson;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
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

    static final String DEFAULT_MANIFEST_PATH = "manifest.yml";
    static final int DEFAULT_PLUGIN_TIMEOUT = 120;

    /**
     * Builds a proxy configuration for the target URL.
     * @param targetURL the target url
     * @return either {@link Optional#empty()} or the proxy configuration.
     */
    static Optional<org.cloudfoundry.reactor.ProxyConfiguration> buildProxyConfiguration(URL targetURL) {
        Hudson hudson = Hudson.getInstance();
        if (hudson == null) return Optional.empty();

        ProxyConfiguration proxyConfig = hudson.proxy;
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

    static FormValidation doTestConnection(final ItemGroup context,
                                               final String target,
                                               final String credentialsId,
                                               final String organization,
                                               final String cloudSpace,
                                               final boolean selfSigned) {
        try {
            URL targetUrl = new URL("https://" + target);
            List<StandardUsernamePasswordCredentials> standardCredentials = CredentialsProvider.lookupCredentials(
                    StandardUsernamePasswordCredentials.class,
                    context,
                    ACL.SYSTEM,
                    URIRequirementBuilder.fromUri(target).build());

            StandardUsernamePasswordCredentials credentials =
                    CredentialsMatchers.firstOrNull(standardCredentials, CredentialsMatchers.withId(credentialsId));
            // TODO: move this into a CloudFoundryOperations factory method and
            // share it with perform.
            ConnectionContext connectionContext = DefaultConnectionContext.builder()
                .apiHost(target)
                .proxyConfiguration(CloudFoundryUtils.buildProxyConfiguration(targetUrl))
                .skipSslValidation(selfSigned)
                .build();

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
            if (targetUrl.getHost().startsWith("api.")) {
                return FormValidation.okWithMarkup("<b>Connection successful!</b>");
            } else {
                return FormValidation.warning(
                        "Connection successful, but your target's hostname does not start with \"api.\".\n" +
                                "Make sure it is the real API endpoint and not a redirection, " +
                                "or it may cause some problems.");
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
