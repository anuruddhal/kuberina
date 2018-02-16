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
import org.ballerinalang.net.http.Constants;
import org.ballerinalang.util.codegen.AnnAttachmentInfo;
import org.ballerinalang.util.codegen.AnnAttributeValue;
import org.ballerinalang.util.codegen.ServiceInfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Util methods used for artifact generation.
 */
public class ArtifactGenUtils {

    private static final boolean debugEnabled = "true".equals(System.getProperty(
            ArtifactGenConstants.ENABLE_DEBUG_LOGS));

    public static void writeToFile(String context, String targetFilePath) throws IOException {
        File dockerfile = new File(targetFilePath);
        dockerfile.getParentFile().mkdirs();
        Files.write(Paths.get(targetFilePath), context.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static List<Integer> extractPorts(ServiceInfo serviceInfo) {
        List<Integer> ports = new ArrayList<>();
        AnnAttachmentInfo annotationInfo = serviceInfo.getAnnotationAttachmentInfo(Constants
                .HTTP_PACKAGE_PATH, Constants.ANN_NAME_CONFIG);
        AnnAttributeValue portAttrVal = annotationInfo.getAttributeValue(Constants.ANN_CONFIG_ATTR_PORT);
        if (portAttrVal != null && portAttrVal.getIntValue() > 0) {
            ports.add(Math.toIntExact(portAttrVal.getIntValue()));
        }
        return ports;
    }
}
