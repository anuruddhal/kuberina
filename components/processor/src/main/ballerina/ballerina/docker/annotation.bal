package ballerina.docker;

@Description {value:"Docker configuration"}
@Field {value:"name: Name of the docker image"}
@Field {value:"registry: Docker registry"}
@Field {value:"tag: Docker image tag"}
@Field {value:"username: Username for docker registry"}
@Field {value:"password: Password for docker registry"}
@Field {value:"baseImage: Base image for docker image building"}
@Field {value:"push: Push to remote registry"}
@Field {value:"imageBuild: Build docker image"}
@Field {value:"debugEnable: Enable debug for ballerina program"}
@Field {value:"debugPort: Remote debug port for ballerina program"}
public annotation configuration attach service, function {
    string name;
    string registry;
    string tag;
    string username;
    string password;
    string baseImage;
    boolean push;
    boolean imageBuild;
    boolean debugEnable;
    int debugPort;
}
