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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import org.ballerinalang.kubegen.models.ServiceAnnotation;
import org.ballerinalang.kubegen.utils.KuberinaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates kubernetes Service from annotations.
 */
public class KubernetesServiceGeneratorTests {

    private final Logger log = LoggerFactory.getLogger(KubernetesServiceGeneratorTests.class);

    @Test
    public void testServiceGenerate() {
        ServiceAnnotation serviceAnnotation = new ServiceAnnotation();
        serviceAnnotation.setName("MyService");
        serviceAnnotation.setPort(9090);
        serviceAnnotation.setServiceType("NodePort");
        serviceAnnotation.setSelector("MyAPP");
        Map<String, String> labels = new HashMap<>();
        labels.put(KuberinaConstants.KUBERNETES_SELECTOR_KEY, "TestAPP");
        serviceAnnotation.setLabels(labels);
        KubernetesServiceGenerator kubernetesServiceGenerator = new KubernetesServiceGenerator();
        Service service = kubernetesServiceGenerator.generate(serviceAnnotation);
        Assert.assertNotNull(service);
        try {
            String serviceYAML = SerializationUtils.dumpAsYaml(service);
            File tempFile = File.createTempFile("temp", serviceAnnotation.getName() + ".yaml", new File
                    ("target"));
            KuberinaUtils.writeToFile(serviceYAML, tempFile.getPath());
            log.info("Generated YAML: \n" + serviceYAML);
            Assert.assertTrue(tempFile.exists());
            tempFile.deleteOnExit();
        } catch (JsonProcessingException e) {
            Assert.fail("Unable to generate yaml from service", e);
        } catch (IOException e) {
            Assert.fail("Unable to generate yaml from service", e);
        }
    }
}
