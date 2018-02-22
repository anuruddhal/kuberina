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
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.ballerinalang.artifactgen.models.DockerModel;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;

import static org.ballerinalang.artifactgen.utils.ArtifactGenUtils.printError;

/**
 * Generates Docker artifacts from annotations.
 */
public class DockerGenerator {

    private static final String LOCAL_DOCKER_DAEMON_SOCKET = "unix:///var/run/docker.sock";
    private static final String DOCKER_VELOCITY_TEMPLATE = "templates/Dockerfile.template";
    private static final CountDownLatch buildDone = new CountDownLatch(1);
    private static final String VELOCITY_FILE_NAME_VARIABLE = "fileName";
    private static final String VELOCITY_FILE_PATH_VARIABLE = "filePath";
    private static final String VELOCITY_SERVICE_VARIABLE = "isService";
    private static final PrintStream out = System.out;

    /**
     * Generate Dockerfile based on annotations using velocity template.
     *
     * @param dockerModel {@link DockerModel} object
     * @return Dockerfile content as a string
     */
    public static String generate(DockerModel dockerModel) {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();
        Template template = velocityEngine.getTemplate(DOCKER_VELOCITY_TEMPLATE);
        VelocityContext context = new VelocityContext();
        context.put(VELOCITY_FILE_NAME_VARIABLE, dockerModel.getBalxFileName());
        context.put(VELOCITY_SERVICE_VARIABLE, dockerModel.isService());
        context.put(VELOCITY_FILE_PATH_VARIABLE, dockerModel.getBalxFileName());
        if (dockerModel.isService()) {
            context.put("ports", dockerModel.getPorts());
        }
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    /**
     * Create docker image.
     *
     * @param dockerEnv docker env
     * @param imageName docker image name
     * @param dockerDir dockerfile directory
     * @throws InterruptedException When error with docker build process
     * @throws IOException          When error with docker build process
     */
    public static void buildImage(String dockerEnv, String imageName, String dockerDir) throws
            InterruptedException, IOException {
        DockerClient client = getDockerClient(dockerEnv);
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
     * @param env The URL of the Docker host. If this is null, a {@link DockerClient} pointed to the local Docker
     *            daemon will be created.
     * @return {@link DockerClient} object.
     */
    private static DockerClient getDockerClient(String env) {
        DockerClient client;
        if (env == null) {
            env = LOCAL_DOCKER_DAEMON_SOCKET;
        }

        Config dockerClientConfig = new ConfigBuilder()
                .withDockerUrl(env)
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
