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

package org.ballerinalang.artifactgen.utils;

import org.ballerinalang.artifactgen.ArtifactGenConstants;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Util methods used for doc generation.
 */
public class ArtifactGenUtils {

    private static final boolean debugEnabled = "true".equals(System.getProperty(
            ArtifactGenConstants.ENABLE_DEBUG_LOGS));
    private static final PrintStream out = System.out;

    public static void writeToFile(String context, String targetFilePath) throws IOException {
        Files.write(Paths.get(targetFilePath), context.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Visits a folder recursively and copy folders and files to a target directory.
     */
    static class RecursiveFileVisitor extends SimpleFileVisitor<Path> {
        Path source;
        Path target;

        public RecursiveFileVisitor(Path aSource, Path aTarget) {
            this.source = aSource;
            this.target = aTarget;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetdir = target.resolve(source.relativize(dir).toString());
            try {
                Files.copy(dir, targetdir);
            } catch (FileAlreadyExistsException e) {
                if (!Files.isDirectory(targetdir)) {
                    throw e;
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, target.resolve(source.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
            if (ArtifactGenUtils.isDebugEnabled()) {
                out.println("File copied: " + file.toString());
            }
            return FileVisitResult.CONTINUE;
        }
    }
}