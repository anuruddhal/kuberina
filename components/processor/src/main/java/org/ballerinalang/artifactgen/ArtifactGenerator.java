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
import org.ballerinalang.artifactgen.generators.KubernetesIngressGenerator;
import org.ballerinalang.artifactgen.generators.KubernetesServiceGenerator;
import org.ballerinalang.artifactgen.models.DeploymentModel;
import org.ballerinalang.artifactgen.models.DockerModel;
import org.ballerinalang.artifactgen.models.IngressModel;
import org.ballerinalang.artifactgen.models.ServiceModel;
import org.ballerinalang.artifactgen.utils.ArtifactGenUtils;
import org.ballerinalang.net.http.Constants;
import org.ballerinalang.util.codegen.AnnAttachmentInfo;
import org.ballerinalang.util.codegen.AnnAttributeValue;
import org.ballerinalang.util.codegen.ServiceInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Process Annotations and generate Artifacts.
 */
public class ArtifactGenerator {

    private static final PrintStream out = System.out;
    private static final PrintStream error = System.err;
    private static final String KUBERNETES = "kubernetes";
    private static final String DOCKER = "docker";
    private static final String BALX = ".balx";
    private static final String DEPLOYMENT_POSTFIX = "-deployment.yaml";
    private static final String SVC_POSTFIX = "-svc.yaml";
    private static final String INGRESS_POSTFIX = "-ingress.yaml";
    private static final String SVC_TYPE_NODE_PORT = "NodePort";
    private static final String DOCKER_LATEST_TAG = ":latest";
    private static final String INGRESS_CLASS_NGINX = "nginx";
    private static final String INGRESS_HOSTNAME_POSTFIX = ".com";

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
        String balxFileName = ArtifactGenUtils.extractBalxName(balxFilePath) + BALX;
        dockerModel.setBalxFileName(balxFileName);
        dockerModel.setBalxFilePath(balxFilePath);
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

        boolean imageBuild = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_IMAGE_BUILD) == null || dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_IMAGE_BUILD).getBooleanValue();
        dockerModel.setImageBuild(imageBuild);

        boolean push = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_PUSH) != null && dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_PUSH).getBooleanValue();
        dockerModel.setPush(push);
        dockerModel.setPorts(ArtifactGenUtils.extractPorts(serviceInfo));

        String nameValue = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_NAME) != null ?
                dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_NAME).getStringValue() :
                ArtifactGenUtils.extractBalxName(balxFilePath);
        nameValue = (registry != null) ? registry + "/" + nameValue + ":" + tag : nameValue + ":" + tag;
        dockerModel.setName(nameValue);

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
                ArtifactGenUtils.extractBalxName(balxFilePath) + DOCKER_LATEST_TAG;
        boolean imageBuild = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_IMAGE_BUILD)
                != null && deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_IMAGE_BUILD)
                .getBooleanValue();
        deploymentModel.setImage(image);
        DockerModel dockerModel = new DockerModel();
        String imageNameWithoutTag = image.substring(0, image.lastIndexOf(":"));
        String imageTag = image.substring(image.lastIndexOf(":") + 1, image.length());
        dockerModel.setName(imageNameWithoutTag);
        dockerModel.setTag(imageTag);
        String balxFileName = ArtifactGenUtils.extractBalxName(balxFilePath) + BALX;
        dockerModel.setBalxFileName(balxFileName);
        dockerModel.setBalxFilePath(balxFileName);
        dockerModel.setPorts(ports);
        dockerModel.setService(true);
        dockerModel.setImageBuild(imageBuild);
        createDockerArtifacts(dockerModel, balxFilePath, outputDir + File.separator + KUBERNETES + File
                .separator + DOCKER);
        out.println(deploymentModel);
        try {
            String deploymentContent = KubernetesDeploymentGenerator.generate(deploymentModel);
            ArtifactGenUtils.writeToFile(deploymentContent, outputDir + File.separator + KUBERNETES + File
                    .separator + ArtifactGenUtils.extractBalxName(balxFilePath) + DEPLOYMENT_POSTFIX);
            out.println("Deployment yaml generated.");
        } catch (IOException e) {
            error.println("Unable to write deployment content to " + outputDir);
        } catch (ArtifactGenerationException e) {
            error.println("Unable to generate deployment  " + e.getMessage());
        }
    }


    /**
     * Process svc annotations for ballerina Service.
     *
     * @param serviceInfo  ServiceInfo Object
     * @param balxFilePath ballerina file name
     * @param outputDir    target output directory
     */
    public static void processSvcAnnotationForService(ServiceInfo serviceInfo, String balxFilePath, String
            outputDir) {
        AnnAttachmentInfo svcAnnotationInfo = serviceInfo.getAnnotationAttachmentInfo
                (ArtifactGenConstants.KUBERNETES_ANNOTATION_PACKAGE, ArtifactGenConstants.SERVICE_ANNOTATION);
        if (svcAnnotationInfo == null) {
            return;
        }
        ServiceModel serviceModel = new ServiceModel();

        String serviceName = svcAnnotationInfo.getAttributeValue(ArtifactGenConstants.SVC_NAME)
                != null ?
                svcAnnotationInfo.getAttributeValue(ArtifactGenConstants.SVC_NAME).getStringValue() :
                serviceInfo.getName();
        //TODO: validate service name with regex.
        serviceModel.setName(serviceName.toLowerCase(Locale.ENGLISH));

        String labels = svcAnnotationInfo.getAttributeValue(ArtifactGenConstants.SVC_LABELS) != null ?
                svcAnnotationInfo.getAttributeValue(ArtifactGenConstants.SVC_LABELS).getStringValue() :
                null;
        serviceModel.setLabels(getEnvVars(labels, ArtifactGenUtils.extractBalxName(balxFilePath)));

        String serviceType = svcAnnotationInfo.getAttributeValue(ArtifactGenConstants.SVC_SERVICE_TYPE)
                != null ?
                svcAnnotationInfo.getAttributeValue(ArtifactGenConstants.SVC_SERVICE_TYPE).getStringValue() :
                SVC_TYPE_NODE_PORT;
        serviceModel.setServiceType(serviceType);
        serviceModel.setSelector(ArtifactGenUtils.extractBalxName(balxFilePath));
        AnnAttachmentInfo annotationInfo = serviceInfo.getAnnotationAttachmentInfo(Constants
                .HTTP_PACKAGE_PATH, Constants.ANN_NAME_CONFIG);
        AnnAttributeValue portAttrVal = annotationInfo.getAttributeValue(Constants.ANN_CONFIG_ATTR_PORT);
        if (portAttrVal != null && portAttrVal.getIntValue() > 0) {
            serviceModel.setPort(Math.toIntExact(portAttrVal.getIntValue()));
        } else {
            //TODO: default port hardcoded.
            serviceModel.setPort(9090);
        }

        out.println(serviceModel);
        try {
            String svcContent = KubernetesServiceGenerator.generate(serviceModel);
            ArtifactGenUtils.writeToFile(svcContent, outputDir + File.separator + KUBERNETES + File
                    .separator + serviceInfo.getName() + SVC_POSTFIX);
            out.println("Service yaml generated.");
        } catch (IOException e) {
            error.println("Unable to write service content to " + outputDir);
        } catch (ArtifactGenerationException e) {
            error.println("Unable to generate service  " + e.getMessage());
        }
        // Process Ingress Annotation only if svc annotation is present
        AnnAttachmentInfo ingressAnnotationInfo = serviceInfo.getAnnotationAttachmentInfo
                (ArtifactGenConstants.KUBERNETES_ANNOTATION_PACKAGE, ArtifactGenConstants.INGRESS_ANNOTATION);
        if (ingressAnnotationInfo != null) {
            processIngressAnnotationForService(serviceInfo, serviceModel, balxFilePath, outputDir);
        }
    }

    /**
     * Process ingress annotations for ballerina Service.
     *
     * @param serviceInfo  ServiceInfo Object
     * @param balxFilePath ballerina file name
     * @param outputDir    target output directory
     */
    private static void processIngressAnnotationForService(ServiceInfo serviceInfo, ServiceModel svc, String
            balxFilePath, String outputDir) {
        AnnAttachmentInfo ingressAnnotationInfo = serviceInfo.getAnnotationAttachmentInfo
                (ArtifactGenConstants.KUBERNETES_ANNOTATION_PACKAGE, ArtifactGenConstants.INGRESS_ANNOTATION);
        IngressModel ingressModel = new IngressModel();

        String ingressName = ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_NAME)
                != null ?
                ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_NAME).getStringValue() :
                serviceInfo.getName();
        //TODO: validate ingress name with regex.
        ingressModel.setName(ingressName.toLowerCase(Locale.ENGLISH));
        String labels = ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_LABELS) != null ?
                ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_LABELS).getStringValue() :
                null;
        ingressModel.setLabels(getEnvVars(labels, ArtifactGenUtils.extractBalxName(balxFilePath)));

        String ingressClass = ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_CLASS)
                != null ?
                ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_CLASS).getStringValue() :
                INGRESS_CLASS_NGINX;
        ingressModel.setIngressClass(ingressClass);

        String hostname = ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_HOSTNAME) != null ?
                ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_HOSTNAME).getStringValue() :
                serviceInfo.getName() + INGRESS_HOSTNAME_POSTFIX;
        //TODO:validate hostname
        ingressModel.setHostname(hostname.toLowerCase(Locale.ENGLISH));

        String path = ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_PATH) != null ?
                ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_PATH).getStringValue() :
                "/";
        ingressModel.setPath(path);

        String targetPath = ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_TARGET_PATH) != null ?
                ingressAnnotationInfo.getAttributeValue(ArtifactGenConstants.INGRESS_TARGET_PATH).getStringValue() :
                null;
        ingressModel.setTargetPath(targetPath);
        ingressModel.setServiceName(svc.getName());
        ingressModel.setServicePort(svc.getPort());

        out.println(ingressModel);
        try {
            String svcContent = KubernetesIngressGenerator.generate(ingressModel);
            ArtifactGenUtils.writeToFile(svcContent, outputDir + File.separator + KUBERNETES + File
                    .separator + serviceInfo.getName() + INGRESS_POSTFIX);
            out.println("Ingress yaml generated.");
        } catch (IOException e) {
            error.println("Unable to write ingress content to " + outputDir);
        } catch (ArtifactGenerationException e) {
            error.println("Unable to generate ingress content  " + e.getMessage());
        }
    }

    /**
     * Generate label map by splitting the labels string.
     *
     * @param labels         labels string.
     * @param outputFileName output file name parameter added to the selector.
     * @return Map of labels with selector.
     */
    private static Map<String, String> getEnvVars(String labels, String outputFileName) {
        Map<String, String> labelMap = new HashMap<>();
        if (labels != null) {
            labelMap = Pattern.compile("\\s*,\\s*")
                    .splitAsStream(labels.trim())
                    .map(s -> s.split(":", 2))
                    .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
        }
        labelMap.put(ArtifactGenConstants.KUBERNETES_SELECTOR_KEY, outputFileName);
        return labelMap;
    }

    /**
     * Generate env map by splitting the env string.
     *
     * @param env env string.
     * @return Map of env variables.
     */
    private static Map<String, String> getEnvVars(String env) {
        if (env == null) {
            return null;
        }
        Map<String, String> envMap = Pattern.compile("\\s*,\\s*")
                .splitAsStream(env.trim())
                .map(s -> s.split(":", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
        return envMap;
    }

    private static DeploymentModel getDeploymentModel(AnnAttachmentInfo deploymentAnnotationInfo, String balxFilePath) {
        DeploymentModel deploymentModel = new DeploymentModel();
        String outputFileName = ArtifactGenUtils.extractBalxName(balxFilePath);
        String deploymentName = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_NAME) !=
                null ?
                deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_NAME).getStringValue() :
                outputFileName + "-deployment";
        deploymentModel.setName(deploymentName);

        String namespace = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_NAMESPACE) !=
                null ? deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_NAMESPACE)
                .getStringValue() :
                ArtifactGenConstants.DEPLOYMENT_NAMESPACE_DEFAULT;
        deploymentModel.setNamespace(namespace);

        String imagePullPolicy = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DEPLOYMENT_IMAGE_PULL_POLICY)
                != null ? deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DEPLOYMENT_IMAGE_PULL_POLICY).getStringValue() :
                ArtifactGenConstants.DEPLOYMENT_IMAGE_PULL_POLICY_DEFAULT;
        deploymentModel.setImagePullPolicy(imagePullPolicy);

        //TODO:handle liveness probe.
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

        deploymentModel.setLabels(getEnvVars(labels, ArtifactGenUtils.extractBalxName(balxFilePath)));

        String envVars = deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_ENV_VARS) != null ?
                deploymentAnnotationInfo.getAttributeValue(ArtifactGenConstants.DEPLOYMENT_ENV_VARS).getStringValue() :
                null;

        deploymentModel.setEnv(getEnvVars(envVars));

        return deploymentModel;
    }

    private static void createDockerArtifacts(DockerModel dockerModel, String balxFilePath, String outputDir) {
        String dockerContent = DockerGenerator.generate(dockerModel);
        try {
            ArtifactGenUtils.writeToFile(dockerContent, outputDir + File.separator + "Dockerfile");
            out.println("Dockerfile generation completed.");
            ArtifactGenUtils.copyFile(balxFilePath, outputDir + File.separator + ArtifactGenUtils.extractBalxName
                    (balxFilePath) + BALX);
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
