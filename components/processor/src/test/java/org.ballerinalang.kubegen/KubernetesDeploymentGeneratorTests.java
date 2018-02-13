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

package org.ballerinalang.kubegen;

import org.ballerinalang.kubegen.exceptions.ArtifactGenerationException;
import org.ballerinalang.kubegen.models.DeploymentAnnotation;
import org.ballerinalang.kubegen.utils.KuberinaUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates kubernetes Service from annotations.
 */
public class KubernetesDeploymentGeneratorTests {

    private final Logger log = LoggerFactory.getLogger(KubernetesDeploymentGeneratorTests.class);

    @Test
    public void testServiceGenerate() {
        DeploymentAnnotation deploymentAnnotation = new DeploymentAnnotation();
        deploymentAnnotation.setName("MyDeployment");
        Map<String, String> labels = new HashMap<>();
        labels.put(KuberinaConstants.KUBERNETES_SELECTOR_KEY, "TestAPP");
        List<Integer> ports = new ArrayList<>();
        ports.add(9090);
        ports.add(9091);
        ports.add(9092);
        deploymentAnnotation.setLabels(labels);
        deploymentAnnotation.setImage("SampleImage:v1.0.0");
        deploymentAnnotation.setImagePullPolicy("Always");
        deploymentAnnotation.setReplicas(3);
        deploymentAnnotation.setPorts(ports);

        KubernetesDeploymentGenerator kubernetesDeploymentGenerator = new KubernetesDeploymentGenerator();
        try {
            String deploymentYAML = kubernetesDeploymentGenerator.generate(deploymentAnnotation);
            Assert.assertNotNull(deploymentYAML);

            File tempFile = File.createTempFile("temp", deploymentAnnotation.getName() + ".yaml", new File("target"));
            KuberinaUtils.writeToFile(deploymentYAML, tempFile.getPath());
            log.info("Generated YAML: \n" + deploymentYAML);
            Assert.assertTrue(tempFile.exists());
            tempFile.deleteOnExit();
        } catch (IOException e) {
            Assert.fail("Unable to write to file");
        } catch (ArtifactGenerationException e) {
            Assert.fail("Unable to generate yaml from service");
        }
    }
}
