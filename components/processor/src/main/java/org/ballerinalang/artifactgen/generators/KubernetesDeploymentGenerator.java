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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import org.ballerinalang.artifactgen.ArtifactGenConstants;
import org.ballerinalang.artifactgen.exceptions.ArtifactGenerationException;
import org.ballerinalang.artifactgen.models.DeploymentAnnotation;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates kubernetes deployment from annotations.
 */
public class KubernetesDeploymentGenerator {

    private static final PrintStream out = System.out;

    /**
     * Generate kubernetes deployment definition from annotation.
     *
     * @param deploymentAnnotation {@link DeploymentAnnotation} object
     * @return Generated kubernetes @{@link Deployment} definition
     */
    public static String generate(DeploymentAnnotation deploymentAnnotation) throws ArtifactGenerationException {
        List<ContainerPort> containerPorts = null;
        if (deploymentAnnotation.getPorts() != null) {
            containerPorts = populatePorts(deploymentAnnotation.getPorts());
        }
        Container container = generateContainer(deploymentAnnotation, containerPorts);
        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(deploymentAnnotation.getName())
                .endMetadata()
                .withNewSpec()
                .withReplicas(deploymentAnnotation.getReplicas())
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(deploymentAnnotation.getLabels())
                .endMetadata()
                .withNewSpec()
                .withContainers(container)
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
        String deploymentYAML;
        try {
            deploymentYAML = SerializationUtils.dumpWithoutRuntimeStateAsYaml(deployment);
        } catch (JsonProcessingException e) {
            String errorMessage = "Error while parsing yaml file for deployment: " + deploymentAnnotation.getName();
            out.println(errorMessage);
            throw new ArtifactGenerationException(errorMessage, e);
        }
        return deploymentYAML;
    }

    private static List<ContainerPort> populatePorts(List<Integer> ports) {
        List<ContainerPort> containerPorts = new ArrayList<>();
        for (int port : ports) {
            ContainerPort containerPort = new ContainerPortBuilder()
                    .withContainerPort(port)
                    .withProtocol(ArtifactGenConstants.KUBERNETES_SVC_PROTOCOL)
                    .build();
            containerPorts.add(containerPort);
        }
        return containerPorts;
    }

    private static Container generateContainer(DeploymentAnnotation deploymentAnnotation, List<ContainerPort>
            containerPorts) {
        return new ContainerBuilder()
                .withName(deploymentAnnotation.getName())
                .withImage(deploymentAnnotation.getImage())
                .withImagePullPolicy(deploymentAnnotation.getImagePullPolicy())
                .withPorts(containerPorts)
                .build();
    }
}
