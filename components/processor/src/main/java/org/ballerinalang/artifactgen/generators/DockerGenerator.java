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

package org.ballerinalang.artifactgen.generators;

import io.fabric8.docker.client.Config;
import io.fabric8.docker.client.ConfigBuilder;
import io.fabric8.docker.client.DockerClient;
import io.fabric8.docker.dsl.EventListener;
import io.fabric8.docker.dsl.OutputHandle;
import org.ballerinalang.artifactgen.models.DockerModel;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.ballerinalang.artifactgen.utils.ArtifactGenUtils.printError;

/**
 * Generates Docker artifacts from annotations.
 */
public class DockerGenerator {

    private static final String LOCAL_DOCKER_DAEMON_SOCKET = "unix:///var/run/docker.sock";
    private static final CountDownLatch buildDone = new CountDownLatch(1);

    /**
     * Generate Dockerfile based on annotations using velocity template.
     *
     * @param dockerModel {@link DockerModel} object
     * @return Dockerfile content as a string
     */
    public static String generate(DockerModel dockerModel) {
        String dockerBase = "# --------------------------------------------------------------------\n" +
                "# Copyright (c) 2018, Ballerina Inc. (http://wso2.com) All Rights Reserved.\n" +
                "#\n" +
                "# Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                "# you may not use this file except in compliance with the License.\n" +
                "# You may obtain a copy of the License at\n" +
                "#\n" +
                "# http://www.apache.org/licenses/LICENSE-2.0\n" +
                "#\n" +
                "# Unless required by applicable law or agreed to in writing, software\n" +
                "# distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                "# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                "# See the License for the specific language governing permissions and\n" +
                "# limitations under the License.\n" +
                "# -----------------------------------------------------------------------\n" +
                "\n" +
                "FROM ballerina/b7a:latest\n" +
                "MAINTAINER ballerina Maintainers \"dev@ballerina.io\"\n" +
                "\n" +
                "COPY " + dockerModel.getBalxFileName() + " /home/ballerina \n\n";

        StringBuilder stringBuffer = new StringBuilder(dockerBase);
        if (dockerModel.isService()) {
            stringBuffer.append("EXPOSE ");
            dockerModel.getPorts().forEach(port -> {
                stringBuffer.append(" ").append(port);
            });
            stringBuffer.append("\n\nCMD ballerina run -s ").append(dockerModel.getBalxFileName());
        } else {
            stringBuffer.append("CMD ballerina run ").append(dockerModel.getBalxFileName());
        }
        return stringBuffer.toString();
    }

    /**
     * Create docker image.
     *
     * @param imageName docker image name
     * @param dockerDir dockerfile directory
     * @throws InterruptedException When error with docker build process
     * @throws IOException          When error with docker build process
     */
    public static void buildImage(String imageName, String dockerDir) throws
            InterruptedException, IOException {
        DockerClient client = getDockerClient();
        OutputHandle buildHandle = client.image()
                .build()
                .withRepositoryName(imageName)
                .withNoCache()
                .alwaysRemovingIntermediate()
                .usingListener(new DockerBuilderEventListener())
                .fromFolder(dockerDir);
        buildDone.await();
        buildHandle.close();
        client.close();
    }

    /**
     * Creates a {@link DockerClient} from the given Docker host URL.
     *
     * @return {@link DockerClient} object.
     */
    private static DockerClient getDockerClient() {
        DockerClient client;
        Config dockerClientConfig = new ConfigBuilder()
                .withDockerUrl(LOCAL_DOCKER_DAEMON_SOCKET)
                .build();

        client = new io.fabric8.docker.client.DefaultDockerClient(dockerClientConfig);
        return client;
    }

    /**
     * An {@link EventListener} implementation to listen to Docker build events.
     */
    private static class DockerBuilderEventListener implements EventListener {

        @Override
        public void onSuccess(String successEvent) {
            buildDone.countDown();
        }

        @Override
        public void onError(String errorEvent) {
            printError("Error event occurred while building docker image: " + errorEvent);
            buildDone.countDown();
        }

        @Override
        public void onError(Throwable throwable) {
            printError("Error while building docker image: " + throwable.getMessage());
            buildDone.countDown();
        }

        @Override
        public void onEvent(String ignore) {
        }
    }
}
