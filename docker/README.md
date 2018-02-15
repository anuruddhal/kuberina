# Ballerina Docker Base Image

## Building the image

1. Copy Ballerina run time distribution zip file to this folder
1. Run the following command to build the base docker image.

```docker build --no-cache=true --build-arg BALLERINA_DIST=<ballerina-tools-0.96.1-SNAPSHOT.zip> -t ballerina-base:latest .```