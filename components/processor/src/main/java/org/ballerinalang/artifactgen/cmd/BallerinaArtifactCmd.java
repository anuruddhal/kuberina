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

package org.ballerinalang.artifactgen.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.ballerinalang.launcher.BLauncherCmd;

import java.io.File;
import java.io.PrintStream;

/**
 * Artifact command for ballerina which generates docker/kubernetes artifacts from Ballerina program.
 */
@Parameters(commandNames = "artifacts", commandDescription = "generate kubernetes/docker artifacts")
public class BallerinaArtifactCmd implements BLauncherCmd {
    private final PrintStream out = System.out;

    private JCommander parentCmdParser;

    @Parameter(arity = 1, description = "Path to the .balx file")
    private String balxFilePath;

    @Parameter(names = {"--output", "-o"},
            description = "path to the output directory where the artifacts will be saved to", hidden =
            false)
    private String outputDir;

    @Parameter(names = {"--verbose", "-v"},
            description = "enable debug level logs", hidden = false)
    private boolean debugEnabled;

    @Parameter(names = {"--help", "-h"}, hidden = true)
    private boolean helpFlag;

    @Override
    public void execute() {
        if (helpFlag) {
            String commandUsageInfo = BLauncherCmd.getCommandUsageInfo(parentCmdParser, "artifacts");
            out.println(commandUsageInfo);
            return;
        }

        if (balxFilePath == null || balxFilePath.length() == 0) {
            StringBuilder sb = new StringBuilder("artifacts: Valid .balx file is not provided."
                    + System.lineSeparator());
            sb.append(BLauncherCmd.getCommandUsageInfo(parentCmdParser, "doc"));
            out.println(sb);
            return;
        }

        if (debugEnabled) {
            out.println("debug enabled");
        }

        if (outputDir == null || outputDir.length() == 0) {
            outputDir = System.getProperty("user.dir") + File.separator + "target";
            out.println("Saving artifacts to " + outputDir);
        }
        out.println("Starting artifact generation ...");
    }

    @Override
    public String getName() {
        return "artifacts";
    }

    @Override
    public void printLongDesc(StringBuilder out) {
        out.append("Generates the docker/kubernetes  artifacts from a given Ballerina program." + System.lineSeparator
                ());
        out.append(System.lineSeparator());
    }

    @Override
    public void printUsage(StringBuilder stringBuilder) {
        stringBuilder
                .append("ballerina artifacts <.balx path> [-o outputdir]  [-v]"
                        + System.lineSeparator())
                .append("  .balx path:" + System.lineSeparator())
                .append("  Paths to the directories where Ballerina source files reside or a path to"
                        + System.lineSeparator())
                .append("  a Ballerina file which does not belong to a package" + System.lineSeparator());
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
        this.parentCmdParser = parentCmdParser;
    }

    @Override
    public void setSelfCmdParser(JCommander selfCmdParser) {
    }
}
