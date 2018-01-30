/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.kubegen.docs;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import org.ballerinalang.compiler.CompilerOptionName;
import org.ballerinalang.compiler.CompilerPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ballerinalang.compiler.Compiler;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangService;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.CompilerOptions;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Main class to generate a ballerina documentation.
 */
public class BallerinaKubeGenerator {

    private static final Logger log = LoggerFactory.getLogger(BallerinaKubeGenerator.class);
    private static final PrintStream out = System.out;

    private static final String BSOURCE_FILE_EXT = ".bal";
    private static final Path BAL_BUILTIN = Paths.get("ballerina/builtin");
    private static final Path BAL_BUILTIN_CORE = Paths.get("ballerina/builtin/core");
    private static final String HTML = ".html";

    /**
     * API to generate Kubernetes artifacts
     *
     * @param output  path to the output directory where the API documentation will be written to.
     * @param sources either the path to the directories where Ballerina source files reside or a
     *                path to a Ballerina file which does not belong to a package.
     */
    public static void generateKubernetesArtifacts(String output, String... sources) {
        out.println("kuberina: Kubernetes artifact generation for sources - " + Arrays.toString(sources));
        for (String source : sources) {
            try {
                Map<String, BLangPackage> docsMap;

                if (source.endsWith(".bal")) {
                    Path sourceFilePath = Paths.get(source);
                    Path parentDir = sourceFilePath.getParent();
                    Path fileName = sourceFilePath.getFileName();

                    if (fileName == null) {
                        log.warn("Skipping the source generation for invalid path: " + sourceFilePath);
                        continue;
                    }
                    if (parentDir == null) {
                        parentDir = Paths.get(".");
                    }
                    docsMap = generatePackageKubernetesFromBallerina(parentDir.toString(), fileName);
                } else {
                    Path dirPath = Paths.get(source);
                    docsMap = generatePackageKubernetesFromBallerina(dirPath.toString(), dirPath);
                }
                if (docsMap.size() == 0) {
                    out.println("kuberina: no package definitions found!");
                    return;
                }

                String userDir = System.getProperty("user.dir");
                // If output directory is empty
                if (output == null) {
                    output = System.getProperty(BallerinaKubernetesConstants.HTML_OUTPUT_PATH_KEY, userDir +
                            File.separator + "kube-artifacts" + File.separator);
                }

                // Create output directories
                Files.createDirectories(Paths.get(output));

                // Sort packages by package path
                List<BLangPackage> packageList = new ArrayList<>(docsMap.values());
                packageList.sort(Comparator.comparing(pkg -> pkg.getPackageDeclaration().toString()));

                //Iterate over the packages to generate the kubernetes definitions
                for (BLangPackage bLangPackage : packageList) {
                    List<BLangService> services = bLangPackage.getServices();
                    for (BLangService service : services) {
                        generateServiceDefinition(service);
                    }
                }
                out.println("End of kubernetes artifacts generation");
            } catch (IOException e) {
                out.println(String.format("kuberina: Kubernetes artifact generation failed for %s: %s", source,
                        e.getMessage()));
                log.error(String.format("Kubernetes artifact generation failed for %s", source), e);
            }
        }
    }


    private static void generateServiceDefinition(BLangService bLangService) {
        out.println("Generate service definition for " + bLangService.getName());
        List<BLangAnnotationAttachment> annotationAttachments = bLangService.getAnnotationAttachments();
        for (BLangAnnotationAttachment annotationAttachment : annotationAttachments) {
            if (annotationAttachment.annotationSymbol.toString().equals(BallerinaKubernetesConstants
                    .ANNOTATION_SYMBOL_NAME)) {
                out.println(annotationAttachment.annotationSymbol + " " + annotationAttachment.getAttributes());
                Service service = new ServiceBuilder()
                        .withNewMetadata()
                        .withName(bLangService.getName().toString())
                        .endMetadata()
                        .withNewSpec()
                        .addNewPort()
                        .withProtocol("TCP")
                        .withPort(80)
                        .withNewTargetPort(8080)
                        .endPort()
                        .addToSelector("key1", "value1")
                        .withType("ClusterIP")
                        .endSpec()
                        .build();
                try {
                    out.println("Service " + SerializationUtils.dumpAsYaml(service));
                } catch (JsonProcessingException e) {
                    out.println(String.format("kuberina: Kubernetes artifact generation failed for %s: %s",
                            bLangService.getName(),
                            e.getMessage()));
                }

            }
        }

    }

    /**
     * Generates {@link BLangPackage} objects for each Ballerina package from the given ballerina files.
     *
     * @param sourceRoot  points to the folder relative to which package path is given
     * @param packagePath a {@link Path} object pointing either to a ballerina file or a folder with ballerina files.
     * @return a map of {@link BLangPackage} objects. Key - Ballerina package name Value - {@link BLangPackage}
     */
    protected static Map<String, BLangPackage> generatePackageKubernetesFromBallerina(
            String sourceRoot, Path packagePath) throws IOException {
        final List<Path> packagePaths = new ArrayList<>();
        if (Files.isDirectory(packagePath)) {
            BallerinaSubPackageVisitor subPackageVisitor = new BallerinaSubPackageVisitor(packagePath, packagePaths);
            Files.walkFileTree(packagePath, subPackageVisitor);
        } else {
            packagePaths.add(packagePath);
        }
        BallerinaKubeDataHolder dataHolder = BallerinaKubeDataHolder.getInstance();

        BLangPackage bLangPackage;

        for (Path path : packagePaths) {
            CompilerContext context = new CompilerContext();
            CompilerOptions options = CompilerOptions.getInstance(context);
            options.put(CompilerOptionName.SOURCE_ROOT, sourceRoot);
            options.put(CompilerOptionName.COMPILER_PHASE, CompilerPhase.DESUGAR.toString());
            options.put(CompilerOptionName.PRESERVE_WHITESPACE, "false");

            Compiler compiler = Compiler.getInstance(context);

            // compile the given file
            compiler.compile(getPackageNameFromPath(path));
            bLangPackage = (BLangPackage) compiler.getAST();


            if (bLangPackage == null) {
                out.println(String.format("kuberina: invalid Ballerina package: %s", packagePath));
            } else {
                String packageName = bLangPackage.symbol.pkgID.name.value;
                dataHolder.getPackageMap().put(packageName, bLangPackage);
            }
        }
        return dataHolder.getPackageMap();
    }


    private static String getPackageNameFromPath(Path path) {
        StringJoiner sj = new StringJoiner(".");
        Iterator<Path> pathItr = path.iterator();
        while (pathItr.hasNext()) {
            sj.add(pathItr.next().toString());
        }
        return sj.toString();
    }

    /**
     * Visits sub folders of a ballerina package.
     */
    static class BallerinaSubPackageVisitor extends SimpleFileVisitor<Path> {
        private Path source;
        private List<Path> subPackages;

        public BallerinaSubPackageVisitor(Path source, List<Path> aList) {
            this.source = source;
            this.subPackages = aList;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toString().endsWith(BSOURCE_FILE_EXT)) {
                Path relativePath = source.relativize(file.getParent());
                if (!subPackages.contains(relativePath)) {
                    subPackages.add(relativePath);
                }
            }
            return FileVisitResult.CONTINUE;
        }

    }
}
