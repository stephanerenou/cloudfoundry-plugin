/*
 * © 2017 The original author or authors.
 */
package com.hpe.cloudfoundryjenkins;

import hudson.model.TaskListener;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link CloudFoundryPushTask}.
 *
 * @author Steven Swor
 */
public class CloudFoundryPushTaskTest {

  @Test
  public void testCreateConnectionContextJustHost() throws Exception {
    String targetHost = "api.the.cloudfoundry.host";
    CloudFoundryPushTask task = new CloudFoundryPushTask(targetHost, null, null, null, "false", "0", null, null);
    ConnectionContext context = task.createConnectionContext(null, null, TaskListener.NULL);
    assertTrue("context is of the wrong class", context instanceof DefaultConnectionContext);
    DefaultConnectionContext c = (DefaultConnectionContext) context;
    assertFalse("should not explicitly set the secure field", c.getSecure().isPresent());
    assertEquals("should set the target host", targetHost, c.getApiHost());
    assertFalse("should imply the default https port", c.getPort().isPresent());
  }

  @Test
  public void testCreateConnectionContextHostAndPort() throws Exception {
    String targetHost = "api.the.cloudfoundry.host";
    Integer port = Integer.valueOf(12345);
    CloudFoundryPushTask task = new CloudFoundryPushTask(targetHost + ":" + port, null, null, null, "false", "0", null, null);
    ConnectionContext context = task.createConnectionContext(null, null, TaskListener.NULL);
    assertTrue("context is of the wrong class", context instanceof DefaultConnectionContext);
    DefaultConnectionContext c = (DefaultConnectionContext) context;
    assertFalse("should not explicitly set the secure field", c.getSecure().isPresent());
    assertEquals("should set the target host", targetHost, c.getApiHost());
    assertTrue("should explicitly set the port", c.getPort().isPresent());
    assertEquals("should explicitly set the port", port ,c.getPort().get());
  }

  @Test
  public void testCreateConnectionContextHttpWithImplicitPort() throws Exception {
    String targetHost = "api.the.cloudfoundry.host";
    CloudFoundryPushTask task = new CloudFoundryPushTask("http://" + targetHost, null, null, null, null, null, null, null);
    ConnectionContext context = task.createConnectionContext(null, null, TaskListener.NULL);
    assertTrue("context is of the wrong class", context instanceof DefaultConnectionContext);
    DefaultConnectionContext c = (DefaultConnectionContext) context;
    assertTrue("should explicitly set the secure field", c.getSecure().isPresent());
    assertFalse("should be insecure", c.getSecure().get().booleanValue());
    assertEquals("should set the target host", targetHost, c.getApiHost());
    assertTrue("should imply the default http port", c.getPort().isPresent());
    assertEquals("should imply the default http port", Integer.valueOf(80) ,c.getPort().get());
  }

  @Test
  public void testAllOptions() throws Exception {
    String targetHost = "api.the.cloudfoundry.host";
    Integer port = Integer.valueOf(12345);
    CloudFoundryPushTask task = new CloudFoundryPushTask("http://" + targetHost + ":" + port + "/foo/bar", null, null, null, null, null, null, null);
    ConnectionContext context = task.createConnectionContext(null, null, TaskListener.NULL);
    assertTrue("context is of the wrong class", context instanceof DefaultConnectionContext);
    DefaultConnectionContext c = (DefaultConnectionContext) context;
    assertTrue("should explicitly set the secure field", c.getSecure().isPresent());
    assertFalse("should be insecure", c.getSecure().get().booleanValue());
    assertEquals("should set the target host", targetHost, c.getApiHost());
    assertTrue("should explicitly set the port", c.getPort().isPresent());
    assertEquals("should explicitly set the port", port ,c.getPort().get());
  }

}
