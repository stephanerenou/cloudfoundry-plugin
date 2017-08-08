/*
 * © 2017 The original author or authors.
 */
package com.hpe.cloudfoundryjenkins;

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
    CloudFoundryPushTask task = new CloudFoundryPushTask(targetHost, null, null, null, false, 0, null, null);
    ConnectionContext context = task.createConnectionContext();
    assertTrue("context is of the wrong class", context instanceof DefaultConnectionContext);
    DefaultConnectionContext c = (DefaultConnectionContext) context;
    assertFalse("should not explicitly set the secure field", c.getSecure().isPresent());
    assertEquals("should imply https", "https", c.getScheme());
    assertEquals("should set the target host", targetHost, c.getApiHost());
    assertEquals("should imply the default https port", Integer.valueOf(443) ,c.getPort());
  }

  @Test
  public void testCreateConnectionContextHostAndPort() throws Exception {
    String targetHost = "api.the.cloudfoundry.host";
    Integer port = Integer.valueOf(12345);
    CloudFoundryPushTask task = new CloudFoundryPushTask(targetHost + ":" + port, null, null, null, false, 0, null, null);
    ConnectionContext context = task.createConnectionContext();
    assertTrue("context is of the wrong class", context instanceof DefaultConnectionContext);
    DefaultConnectionContext c = (DefaultConnectionContext) context;
    assertFalse("should not explicitly set the secure field", c.getSecure().isPresent());
    assertEquals("should imply https", "https", c.getScheme());
    assertEquals("should set the target host", targetHost, c.getApiHost());
    assertEquals("should explicitly set the port", port ,c.getPort());
  }

  @Test
  public void testCreateConnectionContextHttpWithImplicitPort() throws Exception {
    String targetHost = "api.the.cloudfoundry.host";
    CloudFoundryPushTask task = new CloudFoundryPushTask("http://" + targetHost, null, null, null, false, 0, null, null);
    ConnectionContext context = task.createConnectionContext();
    assertTrue("context is of the wrong class", context instanceof DefaultConnectionContext);
    DefaultConnectionContext c = (DefaultConnectionContext) context;
    assertTrue("should explicitly set the secure field", c.getSecure().isPresent());
    assertFalse("should be insecure", c.getSecure().get().booleanValue());
    assertEquals("should explicitly set the scheme", "http", c.getScheme());
    assertEquals("should set the target host", targetHost, c.getApiHost());
    assertEquals("should imply the default http port", Integer.valueOf(80) ,c.getPort());
  }

  @Test
  public void testAllOptions() throws Exception {
    String targetHost = "api.the.cloudfoundry.host";
    Integer port = Integer.valueOf(12345);
    CloudFoundryPushTask task = new CloudFoundryPushTask("http://" + targetHost + ":" + port + "/foo/bar", null, null, null, false, 0, null, null);
    ConnectionContext context = task.createConnectionContext();
    assertTrue("context is of the wrong class", context instanceof DefaultConnectionContext);
    DefaultConnectionContext c = (DefaultConnectionContext) context;
    assertTrue("should explicitly set the secure field", c.getSecure().isPresent());
    assertFalse("should be insecure", c.getSecure().get().booleanValue());
    assertEquals("should explicitly set the scheme", "http", c.getScheme());
    assertEquals("should set the target host", targetHost, c.getApiHost());
    assertEquals("should explicitly set the port", port ,c.getPort());
  }

}
