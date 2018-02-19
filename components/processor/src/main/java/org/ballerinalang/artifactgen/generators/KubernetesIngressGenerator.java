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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressBackend;
import io.fabric8.kubernetes.api.model.extensions.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressTLS;
import io.fabric8.kubernetes.api.model.extensions.IngressTLSBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import org.ballerinalang.artifactgen.exceptions.ArtifactGenerationException;
import org.ballerinalang.artifactgen.models.IngressModel;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;


/**
 * Generates kubernetes ingress from annotations.
 */
public class KubernetesIngressGenerator {
    private static final String INGRESS_CLASS = "kubernetes.io/ingress.class";
    private static final String INGRESS_SSL_PASS_THROUGH = "nginx.ingress.kubernetes.io/ssl-passthrough";
    private static final String INGRESS_REWRITE_TARGET = "nginx.ingress.kubernetes.io/rewrite-target";
    private static final PrintStream out = System.out;

    /**
     * Generate kubernetes ingress definition from annotation.
     *
     * @param ingressModel {@link IngressModel} object
     * @return Generated kubernetes {@link Ingress} definition
     */
    public static String generate(IngressModel ingressModel) throws ArtifactGenerationException {
        //generate ingress backend
        IngressBackend ingressBackend = new IngressBackendBuilder()
                .withServiceName(ingressModel.getServiceName())
                .withNewServicePort(ingressModel.getServicePort())
                .build();

        //generate ingress path
        HTTPIngressPath ingressPath = new HTTPIngressPathBuilder()
                .withBackend(ingressBackend)
                .withPath(ingressModel
                        .getPath()).build();

        //generate TLS
        IngressTLS ingressTLS = new IngressTLSBuilder()
                .withHosts(ingressModel.getHostname())
                .build();

        //generate annotationMap
        Map<String, String> annotationMap = new HashMap<>();
        annotationMap.put(INGRESS_CLASS, ingressModel.getIngressClass());
        annotationMap.put(INGRESS_SSL_PASS_THROUGH, "true");
        if (ingressModel.getTargetPath() != null) {
            annotationMap.put(INGRESS_REWRITE_TARGET, ingressModel.getTargetPath());
        }

        //generate ingress
        Ingress ingress = new IngressBuilder()
                .withNewMetadata()
                .withName(ingressModel.getName())
                .addToLabels(ingressModel.getLabels())
                .addToAnnotations(annotationMap)
                .endMetadata()
                .withNewSpec()
                .withTls(ingressTLS)
                .addNewRule()
                .withHost(ingressModel.getHostname())
                .withNewHttp()
                .withPaths(ingressPath)
                .endHttp()
                .endRule()
                .endSpec()
                .build();
        String ingressYAML;
        try {
            ingressYAML = SerializationUtils.dumpWithoutRuntimeStateAsYaml(ingress);
        } catch (JsonProcessingException e) {
            String errorMessage = "Error while generating yaml file for ingress: " + ingressModel.getName();
            out.println(errorMessage);
            throw new ArtifactGenerationException(errorMessage, e);
        }
        return ingressYAML;
    }
}
