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

package org.ballerinalang.artifactgen.generators;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.ballerinalang.artifactgen.ArtifactGenConstants;
import org.ballerinalang.artifactgen.exceptions.ArtifactGenerationException;
import org.ballerinalang.artifactgen.models.ServiceAnnotation;

import java.io.IOException;
import java.io.PrintStream;


/**
 * Generates kubernetes Service from annotations.
 */
public class KubernetesServiceGenerator {

    private static final PrintStream out = System.out;

    /**
     * Generate kubernetes service definition from annotation.
     *
     * @param serviceAnnotation {@link ServiceAnnotation} object
     * @return Generated kubernetes service yaml as a string
     */
    public static String generate(ServiceAnnotation serviceAnnotation) throws ArtifactGenerationException {
        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceAnnotation.getName())
                .addToLabels(serviceAnnotation.getLabels())
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                .withProtocol(ArtifactGenConstants.KUBERNETES_SVC_PROTOCOL)
                .withPort(serviceAnnotation.getPort())
                .withNewTargetPort(serviceAnnotation.getPort())
                .endPort()
                .addToSelector(ArtifactGenConstants.KUBERNETES_SELECTOR_KEY, serviceAnnotation.getSelector())
                .withType(serviceAnnotation.getServiceType())
                .endSpec()
                .build();
        String serviceYAML;
        try {
            serviceYAML = KubernetesHelper.toYaml(service);
        } catch (IOException e) {
            String errorMessage = "Error while generating yaml file for service: " + serviceAnnotation.getName();
            out.println(errorMessage);
            throw new ArtifactGenerationException(errorMessage, e);
        }
        return serviceYAML;
    }


}
