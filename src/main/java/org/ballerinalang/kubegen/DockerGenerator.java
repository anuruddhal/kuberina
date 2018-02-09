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

package org.ballerinalang.kubegen;

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
import org.ballerinalang.kubegen.models.DockerAnnotation;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * Generates Docker artifacts from annotations.
 */
public class DockerGenerator {

    private static final String ENV_SVC_MODE = "SVC_MODE";
    private static final String LOCAL_DOCKER_DAEMON_SOCKET = "unix:///var/run/docker.sock";
    private static final String ENV_FILE_MODE = "FILE_MODE";
    private static final String DOCKER_VELOCITY_TEMPLATE = "templates/Dockerfile.template";
    private static final CountDownLatch buildDone = new CountDownLatch(1);

    /**
     * Generate Dockerfile based on annotations using velocity template.
     *
     * @param dockerAnnotation {@link DockerAnnotation} object
     */
    public String generate(DockerAnnotation dockerAnnotation) {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();
        Template template = velocityEngine.getTemplate(DOCKER_VELOCITY_TEMPLATE);
        VelocityContext context = new VelocityContext();
        context.put("fileName", "example.balx");
        context.put("isService", false);
        context.put("ports", dockerAnnotation.getPorts());

        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    public void buildImage(String dockerEnv, String imageName, Path tmpDir, boolean isService)
            throws InterruptedException, IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'h:m:ssXX").format(new Date());
        String buildArgs = "{\"" + ENV_SVC_MODE + "\":\"" + String.valueOf(isService) + "\", " +
                "\"BUILD_DATE\":\"" + timestamp + "\"}";
        DockerClient client = getDockerClient(dockerEnv);
        OutputHandle buildHandle = client.image()
                .build()
                .withRepositoryName(imageName)
                .withNoCache()
                .alwaysRemovingIntermediate()
                .withBuildArgs(buildArgs)
                .usingListener(new DockerBuilderEventListener())
                .fromFolder(tmpDir.toString());

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
    private DockerClient getDockerClient(String env) {
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
            buildDone.countDown();
        }

        @Override
        public void onError(Throwable throwable) {
            buildDone.countDown();
        }

        @Override
        public void onEvent(String ignore) {
        }
    }
}
