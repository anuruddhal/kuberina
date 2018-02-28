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

import org.ballerinalang.artifactgen.utils.ArtifactGenUtils;
import org.ballerinalang.compiler.plugins.AbstractCompilerPlugin;
import org.ballerinalang.util.codegen.AnnAttachmentInfo;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.ProgramFile;
import org.ballerinalang.util.codegen.ProgramFileReader;
import org.ballerinalang.util.codegen.ServiceInfo;
import org.ballerinalang.util.diagnostic.DiagnosticLog;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.ballerinalang.artifactgen.utils.ArtifactGenUtils.printDebug;
import static org.ballerinalang.artifactgen.utils.ArtifactGenUtils.printError;
import static org.ballerinalang.artifactgen.utils.ArtifactGenUtils.printInfo;
import static org.ballerinalang.artifactgen.utils.ArtifactGenUtils.printWarn;

/**
 * Compiler plugin to generate kubernetes artifacts.
 */
public class KuberinaCompilerPlugin extends AbstractCompilerPlugin {
    @Override
    public void init(DiagnosticLog diagnosticLog) {
    }

    @Override
    public void codeGenerated(Path binaryPath) {
        String filePath = binaryPath.toAbsolutePath().toString();
        String userDir = new File(filePath).getParentFile().getAbsolutePath();
        try {
            byte[] bFile = Files.readAllBytes(Paths.get(filePath));
            ProgramFileReader reader = new ProgramFileReader();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bFile);
            ProgramFile programFile = reader.readProgram(byteArrayInputStream);
            PackageInfo packageInfos[] = programFile.getPackageInfoEntries();

            for (PackageInfo packageInfo : packageInfos) {
                ServiceInfo serviceInfos[] = packageInfo.getServiceInfoEntries();
                int dockerCount = 0;
                int deploymentCount = 0;
                ServiceInfo deploymentAnnotatedService = null;
                ServiceInfo dockerAnnotatedService = null;
                for (ServiceInfo serviceInfo : serviceInfos) {
                    AnnAttachmentInfo serviceAnnotation = serviceInfo.getAnnotationAttachmentInfo
                            (ArtifactGenConstants.KUBERNETES_ANNOTATION_PACKAGE,
                                    ArtifactGenConstants.SERVICE_ANNOTATION);
                    AnnAttachmentInfo dockerAnnotation = serviceInfo.getAnnotationAttachmentInfo
                            (ArtifactGenConstants.DOCKER_ANNOTATION_PACKAGE,
                                    ArtifactGenConstants.DOCKER_ANNOTATION);

                    if (serviceInfo.getAnnotationAttachmentInfo
                            (ArtifactGenConstants.KUBERNETES_ANNOTATION_PACKAGE,
                                    ArtifactGenConstants.DEPLOYMENT_ANNOTATION) != null) {
                        if (deploymentCount < 1) {
                            deploymentCount += 1;
                            deploymentAnnotatedService = serviceInfo;
                        } else {
                            printWarn("multiple deployment{} annotations detected. Ignoring annotation in" +
                                    " service: " + serviceInfo.getName());
                        }
                    }
                    if (serviceAnnotation != null) {
                        printInfo("Processing svc{} annotation for:" + serviceInfo.getName());
                        String targetPath = userDir + File.separator + "target" + File.separator + ArtifactGenUtils
                                .extractBalxName(filePath)
                                + File.separator;
                        AnnotationProcessor.processSvcAnnotationForService(serviceInfo, filePath, targetPath);
                    }
                    if (dockerAnnotation != null) {
                        if (dockerCount < 1) {
                            printInfo("Processing docker{} annotation for: " + serviceInfo.getName());
                            dockerCount += 1;
                            dockerAnnotatedService = serviceInfo;
                        } else {
                            printWarn("multiple docker{} annotations detected. Ignoring annotation in " +
                                    "service: " + serviceInfo.getName());
                        }
                    }
                }
                if (deploymentAnnotatedService != null) {
                    String targetPath = userDir + File.separator + "target" + File.separator + ArtifactGenUtils
                            .extractBalxName(filePath)
                            + File.separator;
                    AnnotationProcessor.
                            processDeploymentAnnotationForService(deploymentAnnotatedService, filePath, targetPath);
                }
                if (dockerAnnotatedService != null) {
                    String targetPath = userDir + File.separator + "target" + File.separator + "docker" + File
                            .separator;
                    printDebug("Output Directory " + targetPath);
                    AnnotationProcessor.
                            processDockerAnnotationForService(dockerAnnotatedService, filePath, targetPath);
                }
            }
        } catch (IOException e) {
            printError("error occurred while reading balx file" + e.getMessage());
        }
    }
}
