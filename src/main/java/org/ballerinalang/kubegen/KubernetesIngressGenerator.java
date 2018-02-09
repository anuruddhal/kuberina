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

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import org.ballerinalang.kubegen.models.IngressAnnotation;


/**
 * Generates kubernetes ingress from annotations.
 */
public class KubernetesIngressGenerator {

    /**
     * Generate kubernetes ingress definition from annotation.
     *
     * @param ingressAnnotation {@link IngressAnnotation} object
     * @return Generated kubernetes {@link Ingress} object
     */
    public Ingress generate(IngressAnnotation ingressAnnotation) {
        Ingress ingress = new IngressBuilder()
                .withNewMetadata()
                .withName(ingressAnnotation.getName())
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();
        return ingress;
    }
}
