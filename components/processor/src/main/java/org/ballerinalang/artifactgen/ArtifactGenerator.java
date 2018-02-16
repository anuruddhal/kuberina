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

import io.fabric8.utils.Files;
import org.ballerinalang.artifactgen.generators.DockerGenerator;
import org.ballerinalang.artifactgen.models.DockerAnnotation;
import org.ballerinalang.artifactgen.utils.ArtifactGenUtils;
import org.ballerinalang.util.codegen.AnnAttachmentInfo;
import org.ballerinalang.util.codegen.ServiceInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Process Annotations.
 */
public class ArtifactGenerator {

    private static final PrintStream out = System.out;

    /**
     * Process docker annotations for ballerina Service
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
        DockerAnnotation dockerAnnotation = new DockerAnnotation();
        dockerAnnotation.setService(true);
        String nameValue = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_NAME) != null ?
                dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_NAME).getStringValue() :
                balxFilePath.substring(balxFilePath.lastIndexOf(File.separator) + 1, balxFilePath.lastIndexOf("" +
                        ".balx"));
        dockerAnnotation.setName(nameValue);
        String balxFileName = nameValue + ".balx";
        dockerAnnotation.setBalxFileName(balxFileName);
        dockerAnnotation.setBalxFilePath(balxFileName);
        String tag = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_TAG) != null ?
                dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_TAG).getStringValue() :
                ArtifactGenConstants.DOCKER_TAG_LATEST;
        dockerAnnotation.setTag(tag);

        String registry = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_REGISTRY) != null ?
                dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants.DOCKER_REGISTRY).getStringValue() : null;
        dockerAnnotation.setRegistry(registry);

        String username = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_USERNAME) != null ? dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_USERNAME).getStringValue() : null;
        dockerAnnotation.setUsername(username);

        String password = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_PASSWORD) != null ? dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_PASSWORD).getStringValue() : null;
        dockerAnnotation.setPassword(password);

        boolean push = dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_PUSH) != null && dockerAnnotationInfo.getAttributeValue(ArtifactGenConstants
                .DOCKER_PUSH).getBooleanValue();
        dockerAnnotation.setPush(push);
        dockerAnnotation.setPorts(ArtifactGenUtils.extractPorts(serviceInfo));
        out.println(dockerAnnotation);
        String dockerContent = DockerGenerator.generate(dockerAnnotation);
        out.println("Dockerfile Content \n" + dockerContent);
        try {
            File balxFile = new File(balxFilePath);
            File destFile = new File(outputDir + File.separator + balxFileName);
            Files.copy(balxFile, destFile);
            ArtifactGenUtils.writeToFile(dockerContent, outputDir + File.separator + "Dockerfile");
            DockerGenerator.buildImage(null, dockerAnnotation.getName(), outputDir);
        } catch (IOException e) {
            out.println("Unable to write Dockerfile content to " + outputDir);
        } catch (InterruptedException e) {
            out.println("Unable to create docker images " + e.getMessage());
        }
    }


}
