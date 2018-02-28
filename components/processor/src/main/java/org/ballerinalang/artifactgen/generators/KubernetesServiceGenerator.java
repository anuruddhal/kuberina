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

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import org.ballerinalang.artifactgen.ArtifactGenConstants;
import org.ballerinalang.artifactgen.exceptions.ArtifactGenerationException;
import org.ballerinalang.artifactgen.models.ServiceModel;

import java.io.IOException;

import static org.ballerinalang.artifactgen.utils.ArtifactGenUtils.printError;


/**
 * Generates kubernetes service from annotations.
 */
public class KubernetesServiceGenerator implements ArtifactGenerator {

    private ServiceModel serviceModel;

    public KubernetesServiceGenerator(ServiceModel serviceModel) {
        this.serviceModel = serviceModel;
    }

    /**
     * Generate kubernetes service definition from annotation.
     *
     * @return Generated kubernetes service yaml as a string
     * @throws ArtifactGenerationException If an error occurs while generating artifact.
     */
    public String generate() throws ArtifactGenerationException {
        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceModel.getName())
                .addToLabels(serviceModel.getLabels())
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                .withProtocol(ArtifactGenConstants.KUBERNETES_SVC_PROTOCOL)
                .withPort(serviceModel.getPort())
                .withNewTargetPort(serviceModel.getPort())
                .endPort()
                .addToSelector(ArtifactGenConstants.KUBERNETES_SELECTOR_KEY, serviceModel.getSelector())
                .withType(serviceModel.getServiceType())
                .endSpec()
                .build();
        String serviceYAML;
        try {
            serviceYAML = SerializationUtils.dumpWithoutRuntimeStateAsYaml(service);
        } catch (IOException e) {
            String errorMessage = "Error while generating yaml file for service: " + serviceModel.getName();
            printError(errorMessage);
            throw new ArtifactGenerationException(errorMessage, e);
        }
        return serviceYAML;
    }


}
