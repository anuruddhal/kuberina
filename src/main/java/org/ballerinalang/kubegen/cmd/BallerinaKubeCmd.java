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

package org.ballerinalang.kubegen.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.ballerinalang.kubegen.BallerinaKubeGenerator;
import org.ballerinalang.kubegen.KuberinaConstants;
import org.ballerinalang.launcher.BLauncherCmd;

import java.io.PrintStream;
import java.util.List;

/**
 * kube command for ballerina which generates kubernetes for Ballerina packages.
 */
@Parameters(commandNames = "kube", commandDescription = "generate kubernetes artifacts")
public class BallerinaKubeCmd implements BLauncherCmd {
    private final PrintStream out = System.out;

    private JCommander parentCmdParser;

    @Parameter(arity = 1, description = "either the path to the directories where Ballerina source files reside or a "
            + "path to a Ballerina file which does not belong to a package")
    private List<String> argList;

    @Parameter(names = {"--output", "-o"},
            description = "path to the output directory where the API documentation will be written to", hidden = false)
    private String outputDir;

    @Parameter(names = {"--verbose", "-v"},
            description = "enable debug level logs", hidden = false)
    private boolean debugEnabled;

    @Parameter(names = {"--help", "-h"}, hidden = true)
    private boolean helpFlag;

    @Override
    public void execute() {
        if (helpFlag) {
            String commandUsageInfo = BLauncherCmd.getCommandUsageInfo(parentCmdParser, "kube");
            out.println(commandUsageInfo);
            return;
        }

        if (argList == null || argList.size() == 0) {
            StringBuilder sb = new StringBuilder("kube: no valid Ballerina source given."
                    + System.lineSeparator());
            sb.append(BLauncherCmd.getCommandUsageInfo(parentCmdParser, "kube"));
            out.println(sb);
            return;
        }

        if (debugEnabled) {
            System.setProperty(KuberinaConstants.ENABLE_DEBUG_LOGS, "true");
        }

        String[] sources = argList.toArray(new String[argList.size()]);
        BallerinaKubeGenerator.generateKubernetesArtifacts(outputDir, sources);
    }

    @Override
    public String getName() {
        return "kube";
    }

    @Override
    public void printLongDesc(StringBuilder out) {
        out.append("Generates the Kubernetes artifacts of given Ballerina programs." + System.lineSeparator());
        out.append(System.lineSeparator());
    }

    @Override
    public void printUsage(StringBuilder stringBuilder) {
        stringBuilder
                .append("ballerina kube <sourcepath>...  [-o outputdir] [-v]"
                        + System.lineSeparator())
                .append("  sourcepath:" + System.lineSeparator())
                .append("  Paths to the directories where Ballerina source files reside or a path to"
                        + System.lineSeparator());
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
        this.parentCmdParser = parentCmdParser;
    }

    @Override
    public void setSelfCmdParser(JCommander selfCmdParser) {
    }
}
