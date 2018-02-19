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

import org.ballerinalang.artifactgen.generators.DockerGenerator;
import org.ballerinalang.artifactgen.models.DockerModel;
import org.ballerinalang.artifactgen.utils.ArtifactGenUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Docker generator tests.
 */
public class DockerGeneratorTests {

    private final Logger log = LoggerFactory.getLogger(DockerGeneratorTests.class);

    @Test
    public void testDockerGenerate() throws IOException {
        DockerModel dockerModel = new DockerModel();
        List<Integer> ports = new ArrayList<>();
        ports.add(9090);
        ports.add(9091);
        ports.add(9092);
        dockerModel.setPorts(ports);
        dockerModel.setService(true);
        dockerModel.setBalxFileName("example.balx");

        String dockerfileContent = DockerGenerator.generate(dockerModel);
        File dockerfile = new File("target/kubernetes/docker");
        dockerfile.mkdirs();
        dockerfile = new File("target/kubernetes/docker/Dockerfile");
        ArtifactGenUtils.writeToFile(dockerfileContent, dockerfile.getPath());
        log.info("Dockerfile Content:\n" + dockerfileContent);
        Assert.assertTrue(dockerfile.exists());
        //dockerfile.deleteOnExit();
    }
}
