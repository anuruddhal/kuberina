/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.artifactgen;

import org.ballerinalang.artifactgen.exceptions.ArtifactGenerationException;
import org.ballerinalang.artifactgen.generators.KubernetesIngressGenerator;
import org.ballerinalang.artifactgen.models.IngressModel;
import org.ballerinalang.artifactgen.utils.ArtifactGenUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates kubernetes Service from annotations.
 */
public class KubernetesIngressGeneratorTests {

    private final Logger log = LoggerFactory.getLogger(KubernetesIngressGeneratorTests.class);

    @Test
    public void testIngressGenerator() {
        IngressModel ingressModel = new IngressModel();
        ingressModel.setName("MyIngress");
        ingressModel.setHostname("abc.com");
        ingressModel.setPath("/helloworld");
        ingressModel.setServicePort(9090);
        ingressModel.setIngressClass("nginx");
        ingressModel.setServiceName("HelloWorldService");
        Map<String, String> labels = new HashMap<>();
        labels.put(ArtifactGenConstants.KUBERNETES_SELECTOR_KEY, "TestAPP");
        ingressModel.setLabels(labels);
        KubernetesIngressGenerator kubernetesIngressGenerator = new KubernetesIngressGenerator();
        try {
            String ingressYaml = kubernetesIngressGenerator.generate(ingressModel);
            Assert.assertNotNull(ingressYaml);
            File artifactLocation = new File("target/kubernetes");
            artifactLocation.mkdir();
            File tempFile = File.createTempFile("temp", ingressModel.getName() + ".yaml", artifactLocation);
            ArtifactGenUtils.writeToFile(ingressYaml, tempFile.getPath());
            log.info("Generated YAML: \n" + ingressYaml);
            Assert.assertTrue(tempFile.exists());
            // tempFile.deleteOnExit();
        } catch (IOException e) {
            Assert.fail("Unable to write to file");
        } catch (ArtifactGenerationException e) {
            Assert.fail("Unable to generate yaml from ingress");
        }
    }
}
