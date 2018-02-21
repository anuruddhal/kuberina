package ballerina.docker;

@Description {value:"Docker configuration"}
@Field {value:"name: Name of the docker image"}
@Field {value:"registry: Docker registry"}
@Field {value:"tag: Docker image tag"}
@Field {value:"username: Username for docker registry"}
@Field {value:"password: Password for docker registry"}
@Field {value:"push: Push to remote registry"}
@Field {value:"imageBuild: Build docker image"}
public annotation configuration attach service, function {
    string name;
    string registry;
    string tag;
    string username;
    string password;
    boolean push;
    boolean imageBuild;
}
