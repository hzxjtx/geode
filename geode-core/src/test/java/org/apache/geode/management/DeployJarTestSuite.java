package org.apache.geode.management;

import org.apache.geode.internal.ClassPathLoaderIntegrationTest;
import org.apache.geode.internal.ClassPathLoaderTest;
import org.apache.geode.internal.DeployedJarJUnitTest;
import org.apache.geode.internal.JarDeployerIntegrationTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({DeployedJarJUnitTest.class, JarDeployerIntegrationTest.class,
    ClassPathLoaderIntegrationTest.class, ClassPathLoaderTest.class})
public class DeployJarTestSuite {
}
