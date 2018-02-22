package org.ballerinalang.artifactgen;

import org.ballerinalang.artifactgen.utils.ArtifactGenUtils;
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
    private static final PrintStream error = System.err;

    public static void main(String[] args) {

        String filePath = "/Users/anuruddha/Repos/ballerina-k8s-demo/hello-world-k8s/hello-world-k8s.balx";
        //String filePath = args[0];
        String userDir = new File(filePath).getParentFile().getAbsolutePath();
        try {
            byte[] bFile = Files.readAllBytes(Paths.get(filePath));
            ProgramFileReader reader = new ProgramFileReader();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bFile);
            ProgramFile programFile = reader.readProgram(byteArrayInputStream);
            PackageInfo packageInfos[] = programFile.getPackageInfoEntries();
            ServiceInfo serviceInfos[];

            for (PackageInfo packageInfo : packageInfos) {
                serviceInfos = packageInfo.getServiceInfoEntries();
                int dockerCount = 0;
                int deploymentCount = 0;
                ServiceInfo deploymentService = null;
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
                            deploymentService = serviceInfo;
                        } else {
                            out.println("Warning : multiple deployment{} annotations detected. Ignoring annotation in" +
                                    " service: " + serviceInfo.getName());
                        }
                    }
                    if (serviceAnnotation != null) {
                        out.println("Processing svc{} for :" + serviceInfo.getName());
                        String targetPath = userDir + File.separator + "target" + File.separator + ArtifactGenUtils
                                .extractBalxName(filePath)
                                + File.separator;
                        ArtifactGenerator.processSvcAnnotationForService(serviceInfo, filePath, targetPath);
                    }
                    if (dockerAnnotation != null) {
                        if (dockerCount < 1) {
                            out.println("Processing docker{} for : " + serviceInfo.getName());
                            dockerCount += 1;
                            String targetPath = userDir + File.separator + "target" + File.separator + "docker" + File
                                    .separator;
                            out.println("Output Directory " + targetPath);
                            ArtifactGenerator.processDockerAnnotationForService(serviceInfo, filePath, targetPath);
                        } else {
                            out.println("warning : multiple docker{} annotations detected. Ignoring annotation in " +
                                    "service: " + serviceInfo.getName());
                        }
                    }

                }
                if (deploymentService != null) {
                    String targetPath = userDir + File.separator + "target" + File.separator + ArtifactGenUtils
                            .extractBalxName(filePath)
                            + File.separator;
                    ArtifactGenerator.processDeploymentAnnotationForService(deploymentService, filePath, targetPath);
                }
            }
        } catch (IOException e) {
            error.println("Error: error occurred while reading balx file" + e.getMessage());
        }


    }
}
