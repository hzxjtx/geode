/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.management;

import static org.apache.geode.distributed.ConfigurationProperties.ENABLE_CLUSTER_CONFIGURATION;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_PORT;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_CIPHERS;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_ENABLED;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_KEYSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_KEYSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_KEYSTORE_TYPE;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_PROTOCOLS;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_TRUSTSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_SSL_TRUSTSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_CIPHERS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_ENABLED_COMPONENTS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_JMX_ALIAS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_KEYSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_KEYSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_KEYSTORE_TYPE;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_PROTOCOLS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_TRUSTSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_TRUSTSTORE_PASSWORD;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Maps;

import org.apache.geode.internal.AvailablePortHelper;
import org.apache.geode.internal.security.SecurableCommunicationChannel;
import org.apache.geode.test.dunit.rules.LocatorServerStartupRule;
import org.apache.geode.test.dunit.rules.MBeanServerConnectionRule;
import org.apache.geode.test.junit.categories.DistributedTest;
import org.apache.geode.util.test.TestUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.rmi.ssl.SslRMIClientSocketFactory;

/**
 * All the non-ssl enabled locators need to be in a different VM than the ssl enabled locators in
 * these tests, otherwise, some tests would fail. Seems like dunit vm tear down did not clean up the
 * ssl settings cleanly.
 */
@Category(DistributedTest.class)
public class JMXMBeanDUnitTest {
  @Rule
  public LocatorServerStartupRule lsRule = new LocatorServerStartupRule();

  @Rule
  public MBeanServerConnectionRule jmxConnector = new MBeanServerConnectionRule();

  private int jmxPort;
  private Properties locatorProperties = null;
  private Map<String, Object> clientEnv = null;
  private static Properties legacySSLProperties, sslProperties, sslPropertiesWithMultiKey;
  private static String singleKeystore, multiKeystore, multiKeyTruststore;


  @BeforeClass
  public static void beforeClass() {
    singleKeystore = TestUtil.getResourcePath(JMXMBeanDUnitTest.class, "/ssl/trusted.keystore");
    multiKeystore = TestUtil.getResourcePath(JMXMBeanDUnitTest.class,
        "/org/apache/geode/internal/net/multiKey.jks");
    multiKeyTruststore = TestUtil.getResourcePath(JMXMBeanDUnitTest.class,
        "/org/apache/geode/internal/net/multiKeyTrust.jks");

    // setting up properties used to set the ssl properties used by the locators
    legacySSLProperties = new Properties();
    legacySSLProperties.setProperty(JMX_MANAGER_SSL_CIPHERS, "any");
    legacySSLProperties.setProperty(JMX_MANAGER_SSL_PROTOCOLS, "any");
    legacySSLProperties.setProperty(JMX_MANAGER_SSL_ENABLED, "true");
    legacySSLProperties.setProperty(JMX_MANAGER_SSL_KEYSTORE, singleKeystore);
    legacySSLProperties.setProperty(JMX_MANAGER_SSL_KEYSTORE_PASSWORD, "password");
    legacySSLProperties.setProperty(JMX_MANAGER_SSL_KEYSTORE_TYPE, "JKS");
    legacySSLProperties.setProperty(JMX_MANAGER_SSL_TRUSTSTORE, singleKeystore);
    legacySSLProperties.setProperty(JMX_MANAGER_SSL_TRUSTSTORE_PASSWORD, "password");

    sslProperties = new Properties();
    sslProperties.setProperty(SSL_CIPHERS, "any");
    sslProperties.setProperty(SSL_KEYSTORE_PASSWORD, "password");
    sslProperties.setProperty(SSL_TRUSTSTORE_PASSWORD, "password");
    sslProperties.setProperty(SSL_KEYSTORE, singleKeystore);
    sslProperties.setProperty(SSL_KEYSTORE_TYPE, "JKS");
    sslProperties.setProperty(SSL_TRUSTSTORE, singleKeystore);
    sslProperties.setProperty(SSL_ENABLED_COMPONENTS,
        SecurableCommunicationChannel.JMX.getConstant());
    sslProperties.setProperty(SSL_PROTOCOLS, "TLSv1.2,TLSv1.1");

    sslPropertiesWithMultiKey = new Properties();
    sslPropertiesWithMultiKey.putAll(Maps.fromProperties(sslProperties));
    sslPropertiesWithMultiKey.setProperty(SSL_KEYSTORE, multiKeystore);
    sslPropertiesWithMultiKey.setProperty(SSL_TRUSTSTORE, multiKeyTruststore);
    sslPropertiesWithMultiKey.setProperty(SSL_JMX_ALIAS, "jmxkey");
  }

  @Before
  public void before() {
    jmxPort = AvailablePortHelper.getRandomAvailableTCPPort();
    locatorProperties = new Properties();
    locatorProperties.put(JMX_MANAGER_PORT, jmxPort + "");
    locatorProperties.setProperty(ENABLE_CLUSTER_CONFIGURATION, "false");
    clientEnv = new HashMap<>();
  }

  @Test
  public void testJMXOverNonSSL() throws Exception {
    lsRule.startLocatorVM(1, locatorProperties);
    jmxConnector.connect(jmxPort);
    validateJmxConnection();
  }

  @Test
  public void testJMXOverNonSSLWithClientUsingIncorrectPort() throws Exception {
    lsRule.startLocatorVM(1, locatorProperties);

    assertThatThrownBy(() -> jmxConnector.connect(9999))
        .hasRootCauseExactlyInstanceOf(java.net.ConnectException.class);
  }

  @Test
  public void testJMXOverSSL() throws Exception {
    locatorProperties.putAll(Maps.fromProperties(sslProperties));
    lsRule.startLocatorVM(0, locatorProperties);
    clientEnv = getClientEnvironment(false);
    jmxConnector.connect(jmxPort, clientEnv);

    validateJmxConnection();
  }


  @Test
  public void testJMXOverSSLWithMultiKey() throws Exception {
    locatorProperties.putAll(Maps.fromProperties(sslPropertiesWithMultiKey));
    lsRule.startLocatorVM(0, locatorProperties);
    clientEnv = getClientEnvironment(true);
    jmxConnector.connect(jmxPort, clientEnv);
    validateJmxConnection();
  }

  @Test
  public void testJMXOverLegacySSL() throws Exception {
    locatorProperties.putAll(Maps.fromProperties(legacySSLProperties));
    lsRule.startLocatorVM(0, locatorProperties);
    clientEnv = getClientEnvironment(false);
    jmxConnector.connect(jmxPort, clientEnv);
    validateJmxConnection();
  }


  private Map<String, Object> getClientEnvironment(boolean withAlias) {
    System.setProperty("javax.net.ssl.keyStore", withAlias ? multiKeystore : singleKeystore);
    System.setProperty("javax.net.ssl.keyStoreType", "JKS");
    System.setProperty("javax.net.ssl.keyStorePassword", "password");
    System.setProperty("javax.net.ssl.trustStore", withAlias ? multiKeyTruststore : singleKeystore);
    System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    System.setProperty("javax.net.ssl.trustStorePassword", "password");
    Map<String, Object> environment = new HashMap();
    environment.put("com.sun.jndi.rmi.factory.socket", new SslRMIClientSocketFactory());
    return environment;
  }


  private void validateJmxConnection() throws Exception {
    MBeanServerConnection mbeanServerConnection = jmxConnector.getMBeanServerConnection();
    // Get MBean proxy instance that will be used to make calls to registered MBean
    DistributedSystemMXBean distributedSystemMXBean =
        jmxConnector.getProxyMBean(DistributedSystemMXBean.class);
    assertEquals(1, distributedSystemMXBean.getMemberCount());
    assertEquals(1, distributedSystemMXBean.getLocatorCount());
  }
}
