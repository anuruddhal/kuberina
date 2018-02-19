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
import org.ballerinalang.artifactgen.generators.DockerGenerator;
import org.ballerinalang.artifactgen.generators.KubernetesDeploymentGenerator;
import org.ballerinalang.artifactgen.models.DeploymentModel;
import org.ballerinalang.artifactgen.models.DockerModel;
import org.ballerinalang.artifactgen.utils.ArtifactGenUtils;
import org.ballerinalang.util.codegen.AnnAttachmentInfo;
import org.ballerinalang.util.codegen.ServiceInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Process Annotations.
 */
public class ArtifactGenerator {

    private static final PrintStream out = System.out;
    private static final PrintStream error = System.err;
    private static final String KUBERNETES = "kubernetes";
    private static final String DEPLOYMENT_POSTFIX = "-deployment.yaml";
    private static final String DOCKER_LATEST_TAG = ":latest";

    /**
     * Process docker annotations for ballerina Service.
     *
     * @param serviceInfo  ServiceInfo Object
     * @param balxFilePath ballerina file name
     * @param outputDir    target output directory
     */
    public static void processDockerAnnotationForService(ServiceInfo serviceInfo, String balxFilePath, String
            outputDir) {
        AnnAttachmentInfo dockerAnnotationInfo = serviceInfo.getAnnotationAttachmentInfo
                (ArtifactGenConstants.DOCKER_ANNOTATION_PACKAGE, ArtifactGenConstants.DOCKER_ANNOTATION);
        if (dockerAnnotationInfo == null) {
            return;
        }
        DockerModel dockerModel = new DockerModel();
        dockerModel.setService(true);
        String nameValue = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_NAME) != null ?
                dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_NAME).getStringValue() :
                extractBalxName(balxFilePath);
        dockerModel.setName(nameValue);
        String balxFileName = nameValue + ".balx";
        dockerModel.setBalxFileName(balxFileName);
        dockerModel.setBalxFilePath(balxFileName);
        String tag = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_TAG) != null ?
                dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_TAG).getStringValue() :
                ArtifactGenConstants.DOCKER_TAG_LATEST;
        dockerModel.setTag(tag);

        String registry = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_REGISTRY) != null ?
                dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_REGISTRY).getStringValue() : null;
        dockerModel.setRegistry(registry);

        String username = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_USERNAME) != null ? dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_USERNAME).getStringValue() : null;
        dockerModel.setUsername(username);

        String password = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_PASSWORD) != null ? dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_PASSWORD).getStringValue() : null;
        dockerModel.setPassword(password);

        boolean push = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_PUSH) != null && dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_PUSH).getBooleanValue();
        dockerModel.setPush(push);
        dockerModel.setPorts(ArtifactGenUtils.extractPorts(serviceInfo));
        dockerModel.setImageBuild(true);
        out.println(dockerModel);
        createDockerArtifacts(dockerModel, balxFilePath, outputDir);

    }

    /**
     * Process deployment annotations for ballerina Service.
     *
     * @param serviceInfo  ServiceInfo Object
     * @param balxFilePath ballerina file name
     * @param outputDir    target output directory
     */
    public static void processDeploymentAnnotationForService(ServiceInfo serviceInfo, String balxFilePath, String
            outputDir) {
        AnnAttachmentInfo deploymentAnnotationInfo = serviceInfo.getAnnotationAttachmentInfo
                (ArtifactGenConstants.KUBERNETES_ANNOTATION_PACKAGE, ArtifactGenConstants.DEPLOYMENT_ANNOTATION);
        if (deploymentAnnotationInfo == null) {
            return;
        }
        DeploymentModel deploymentModel = getDeploymentModel(deploymentAnnotationInfo, balxFilePath);
        List<Integer> ports = ArtifactGenUtils.extractPorts(serviceInfo);
        deploymentModel.setPorts(ports);
        String image = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_IMAGE)
                != null ?
                deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_IMAGE).getStringValue() :
                extractBalxName(balxFilePath) + DOCKER_LATEST_TAG;
        boolean imageBuild = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_IMAGE_BUILD)
                != null && deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_IMAGE_BUILD)
                .getBooleanValue();
        deploymentModel.setImage(image);
        DockerModel dockerModel = new DockerModel();
        String imageNameWithoutTag = image.substring(0, image.lastIndexOf(":"));
        String imageTag = image.substring(image.lastIndexOf(":") + 1, image.length());
        dockerModel.setName(imageNameWithoutTag);
        dockerModel.setTag(imageTag);
        String balxFileName = extractBalxName(balxFilePath) + ".balx";
        dockerModel.setBalxFileName(balxFileName);
        dockerModel.setBalxFilePath(balxFileName);
        dockerModel.setPorts(ports);
        dockerModel.setService(true);
        dockerModel.setImageBuild(imageBuild);
        createDockerArtifacts(dockerModel, balxFilePath, outputDir + File.separator + KUBERNETES + File
                .separator + "docker");
        out.println(deploymentModel);
        try {
            String deploymentContent = KubernetesDeploymentGenerator.generate(deploymentModel);
            ArtifactGenUtils.writeToFile(deploymentContent, outputDir + File.separator + KUBERNETES + File
                    .separator + extractBalxName(balxFilePath) + DEPLOYMENT_POSTFIX);
            out.println("Deployment yaml generated.");
        } catch (IOException e) {
            error.println("Unable to write deployment content to " + outputDir);
        } catch (ArtifactGenerationException e) {
            error.println("Unable to generate deployment  " + e.getMessage());
        }
    }

    private static Map<String, String> getLabels(String lables) {
        Map<String, String> labels = Pattern.compile("\\s*,\\s*")
                .splitAsStream(lables.trim())
                .map(s -> s.split(":", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
        return labels;
    }

    private static String extractBalxName(String balxFilePath) {
        return balxFilePath.substring(balxFilePath.lastIndexOf(File.separator) + 1, balxFilePath.lastIndexOf("" +
                ".balx"));
    }

    private static DeploymentModel getDeploymentModel(AnnAttachmentInfo deploymentAnnotationInfo, String balxFilePath) {
        DeploymentModel deploymentModel = new DeploymentModel();
        String outputFileName = extractBalxName(balxFilePath);
        String deploymentName = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_NAME) !=
                null ?
                deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_NAME).getStringValue() :
                outputFileName + "-deployment";
        deploymentModel.setName(deploymentName);

        String namespace = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_NAMESPACE) !=
                null ?  deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_NAMESPACE)
                .getStringValue() :
                ArtifactGenConstants.DEPLOYMENT_NAMESPACE_DEFAULT;
        deploymentModel.setNamespace(namespace);

        String imagePullPolicy = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DEPLOYMENT_IMAGE_PULL_POLICY)
                != null ?  deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DEPLOYMENT_IMAGE_PULL_POLICY).getStringValue() :
                ArtifactGenConstants.DEPLOYMENT_IMAGE_PULL_POLICY_DEFAULT;
        deploymentModel.setImagePullPolicy(imagePullPolicy);

        //TODO:handle liveness probe
        String liveness = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_LIVENESS)
                != null ?
                deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_LIVENESS).getStringValue() :
                ArtifactGenConstants.DEPLOYMENT_LIVENESS_DISABLE;
        deploymentModel.setLiveness(liveness);

        int replicas = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_REPLICAS) != null ?
                Math.toIntExact(deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_REPLICAS)
                        .getIntValue()) : 1;
        deploymentModel.setReplicas(replicas);

        String labels = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_LABELS) != null ?
                deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_LABELS).getStringValue() :
                null;
        Map<String, String> labelMap;
        if (labels == null) {
            labelMap = new HashMap<>();
        } else {
            labelMap = getLabels(labels);
        }
        labelMap.put(ArtifactGenConstants.KUBERNETES_SELECTOR_KEY, outputFileName);
        deploymentModel.setLabels(labelMap);

        return deploymentModel;
    }

    private static void createDockerArtifacts(DockerModel dockerModel, String balxFilePath, String outputDir) {
        String dockerContent = DockerGenerator.generate(dockerModel);
        try {
            ArtifactGenUtils.writeToFile(dockerContent, outputDir + File.separator + "Dockerfile");
            out.println("Dockerfile generation completed.");
            ArtifactGenUtils.copyFile(balxFilePath, outputDir + File.separator + extractBalxName(balxFilePath) +
                    ".balx");
            if (dockerModel.isImageBuild()) {
                DockerGenerator.buildImage(null, dockerModel.getName(), outputDir);
                out.println("Docker image generation completed.");
            }
        } catch (IOException e) {
            error.println("Unable to write Dockerfile content to " + outputDir);
        } catch (InterruptedException e) {
            error.println("Unable to create docker images " + e.getMessage());
        }
    }
}
