package org.ballerinalang.artifactgen;

import org.ballerinalang.util.codegen.AnnAttachmentInfo;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.ProgramFile;
import org.ballerinalang.util.codegen.ProgramFileReader;
import org.ballerinalang.util.codegen.ServiceInfo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main class for demo purposes.
 */
public class Main {
    private static final PrintStream out = System.out;

    public static void main(String[] args) {

        //String filePath = "/Users/anuruddha/Desktop/workspace/hello/sample/hello-world.balx";
        String filePath = args[0];
        String userDir = System.getProperty("user.dir");
        try {
            byte[] bFile = Files.readAllBytes(Paths.get(filePath));
            ProgramFileReader reader = new ProgramFileReader();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bFile);
            ProgramFile programFile = reader.readProgram(byteArrayInputStream);
            PackageInfo packageInfos[] = programFile.getPackageInfoEntries();
            ServiceInfo serviceInfos[];

            for (int i = 0; i < packageInfos.length; i++) {
                PackageInfo packageInfo = packageInfos[i];
                serviceInfos = packageInfo.getServiceInfoEntries();
                int dockerCount = 0;
                int deploymentCount = 0;
                for (int j = 0; j < serviceInfos.length; j++) {
                    ServiceInfo serviceInfo = serviceInfos[j];
                    AnnAttachmentInfo deploymentAnnotation = serviceInfo.getAnnotationAttachmentInfo
                            (ArtifactGenConstants.KUBERNETES_ANNOTATION_PACKAGE,
                                    ArtifactGenConstants.DEPLOYMENT_ANNOTATION);
                    AnnAttachmentInfo serviceAnnotation = serviceInfo.getAnnotationAttachmentInfo
                            (ArtifactGenConstants.KUBERNETES_ANNOTATION_PACKAGE,
                                    ArtifactGenConstants.SERVICE_ANNOTATION);
                    AnnAttachmentInfo ingressAnnotation = serviceInfo.getAnnotationAttachmentInfo
                            (ArtifactGenConstants.KUBERNETES_ANNOTATION_PACKAGE,
                                    ArtifactGenConstants.INGRESS_ANNOTATION);
                    AnnAttachmentInfo dockerAnnotation = serviceInfo.getAnnotationAttachmentInfo
                            (ArtifactGenConstants.DOCKER_ANNOTATION_PACKAGE,
                                    ArtifactGenConstants.DOCKER_ANNOTATION);

                    if (deploymentAnnotation != null) {
                        out.println("Deployment annotation detected.");
                        deploymentCount += 1;
                        if (deploymentCount > 1) {
                            out.println("Warning : multiple deployment annotations detected. Ignoring annotation in " +
                                    "service: " + serviceInfo.getName());
                            continue;
                        }
                        out.println("Deployment " + deploymentAnnotation.getAttributeValueMap());
                        String targetPath = userDir + File.separator + "target" + File.separator + serviceInfo
                                .getName() + File.separator;
                        ArtifactGenerator.processDeploymentAnnotationForService(serviceInfo, filePath, targetPath);
                    }
                    if (serviceAnnotation != null) {
                        out.println("Service " + serviceAnnotation.getAttributeValueMap());
                    }
                    if (ingressAnnotation != null) {
                        out.println("Ingress " + ingressAnnotation.getAttributeValueMap());
                    }
                    if (dockerAnnotation != null) {
                        out.println("Docker annotation detected.");
                        dockerCount += 1;
                        if (dockerCount > 1) {
                            out.println("Warning : multiple docker annotations detected. Ignoring annotation in " +
                                    "service: " + serviceInfo.getName());
                            continue;
                        }
                        String targetPath = userDir + File.separator + "target" + File.separator + "docker" + File
                                .separator;
                        out.println("Output Directory " + targetPath);
                        ArtifactGenerator.processDockerAnnotationForService(serviceInfo, filePath, targetPath);
                    }

                }
            }
        } catch (IOException e) {
            out.println(e.getMessage());
        }


    }
}
