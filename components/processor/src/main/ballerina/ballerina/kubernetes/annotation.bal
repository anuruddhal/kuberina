package ballerina.kubernetes;

@Description {value:"Kubernetes deployment configuration"}
@Field {value:"name: Name of the deployment"}
@Field {value:"labels: Labels for deployment"}
@Field {value:"replicas: Number of replicas"}
@Field {value:"liveness: Enable or disable liveness probe"}
@Field {value:"initialDelaySeconds: Initial delay in seconds before performing the first probe"}
@Field {value:"periodSeconds: Liveness probe interval"}
@Field {value:"imagePullPolicy: Docker image pull policy"}
@Field {value:"namespace: Kubernetes namespace"}
@Field {value:"image: Docker image with tag"}
@Field {value:"envVars: Environment varialbes for container"}
@Field {value:"imageBuild: Docker image to be build or not"}
public annotation deployment attach service, function {
    string name;
    string labels;
    int replicas;
    string liveness;
    int initialDelaySeconds;
    int periodSeconds;
    string imagePullPolicy;
    string namespace;
    string image;
    string env;
    boolean imageBuild;
}

@Description {value:"Kubernetes service configuration"}
@Field {value:"name: Name of the Service"}
@Field {value:"labels: Labels for service"}
@Field {value:"serviceType: Service type of the service"}
@Field {value:"port: Service port"}
public annotation svc attach service {
    string name;
    string labels;
    string serviceType;
    int port;
}

@Description {value:"Kubernetes ingress configuration"}
@Field {value:"name: Name of the Service"}
@Field {value:"labels: Labels for service"}
@Field {value:"hostname: Host name of the ingress"}
@Field {value:"path: Resource path"}
@Field {value:"ingressClass: Ingress class"}
public annotation ingress attach service {
    string name;
    string labels;
    string hostname;
    string path;
    string ingressClass;
}
