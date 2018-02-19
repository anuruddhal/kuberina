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

/**
 * Constants used in kuberina.
 */
public class ArtifactGenConstants {
    public static final String ENABLE_DEBUG_LOGS = "debugKuberina";
    public static final String KUBERNETES_SVC_PROTOCOL = "TCP";
    public static final String KUBERNETES_SELECTOR_KEY = "app";

    // Annotation package constants
    public static final String KUBERNETES_ANNOTATION_PACKAGE = "ballerina.kubernetes";
    public static final String DOCKER_ANNOTATION_PACKAGE = "ballerina.docker";
    public static final String DEPLOYMENT_ANNOTATION = "deployment";
    public static final String SERVICE_ANNOTATION = "svc";
    public static final String INGRESS_ANNOTATION = "ingress";
    public static final String DOCKER_ANNOTATION = "configuration";

    //Docker annotation constants
    public static final String DOCKER_NAME = "name";
    public static final String DOCKER_REGISTRY = "registry";
    public static final String DOCKER_TAG = "tag";
    public static final String DOCKER_USERNAME = "username";
    public static final String DOCKER_PASSWORD = "password";
    public static final String DOCKER_PUSH = "push";
    public static final String DOCKER_TAG_LATEST = "latest";

    //Deployment annotation constants
    public static final String DEPLOYMENT_NAME = "name";
    public static final String DEPLOYMENT_LABELS = "labels";
    public static final String DEPLOYMENT_REPLICAS = "replicas";
    public static final String DEPLOYMENT_LIVENESS = "liveness";
    public static final String DEPLOYMENT_INITIAL_DELAY_SECONDS = "initialDelaySeconds";
    public static final String DEPLOYMENT_PERIOD_SECONDS = "periodSeconds";
    public static final String DEPLOYMENT_IMAGE_PULL_POLICY = "imagePullPolicy";
    public static final String DEPLOYMENT_NAMESPACE = "namespace";
    public static final String DEPLOYMENT_IMAGE = "image";
    public static final String DEPLOYMENT_IMAGE_BUILD = "imageBuild";
    public static final String DEPLOYMENT_NAMESPACE_DEFAULT = "default";
    public static final String DEPLOYMENT_IMAGE_PULL_POLICY_DEFAULT = "IfNotPresent";
    public static final String DEPLOYMENT_LIVENESS_DISABLE = "disable";
}
