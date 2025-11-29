# Recceda Jenkins Library

This repository contains a Jenkins Shared Library designed to streamline the CI/CD process for Spring Boot applications. It provides a reusable pipeline that can be easily integrated into any Spring Boot project managed by Jenkins.

## How to Configure Jenkins

To use this shared library, you must configure it as a "Global Pipeline Library" in your Jenkins instance:

1.  Navigate to **Manage Jenkins** > **Configure System**.
2.  Scroll down to the **Global Pipeline Libraries** section.
3.  Click **Add**.
4.  Set the following:
    *   **Name:** `recceda-jenkins-library` (This name is used in your project's `Jenkinsfile`).
    *   **Default version:** `main` (This field is mandatory. It tells Jenkins which branch or tag to use, e.g., `main`, `master`, `v1.0`).
    *   **Retrieval method:** `Modern SCM`.
    *   **Source Code Management:** `Git`.
    *   **Project Repository:** Enter the URL to this Git repository (e.g., `https://github.com/your-org/recceda-jenkins-library.git`).
    *   Add credentials if your repository is private.
5.  Click **Save**.

## How to Use in Your Spring Boot Project

Once the shared library is configured in Jenkins, you can use it in your Spring Boot application by creating a `Jenkinsfile` in the root of your project's repository.

Here's an example `Jenkinsfile`:

```groovy
@Library('recceda-jenkins-library') _

springBootPipeline(
    runTest: true,
    runBuild: true,
    runDeploy: true,
    imageName: 'your-docker-image-name',
    dockerhubCredentialId: 'YOUR_DOCKERHUB_CREDENTIAL_ID',
    dockerhubUsernameSecretId: 'YOUR_DOCKERHUB_USERNAME_SECRET_ID',
    pushLatestTag: true,
    dockerBuildPlatform: 'linux/amd64'
)
```

### Pipeline Parameters

The `springBootPipeline` accepts the following parameters to control which stages are executed:

*   `runTest` (default: `true`): If `true`, the "Test" stage will execute `mvn test`.
*   `runBuild` (default: `true`): If `true`, the "Build" stage will execute `mvn package`.
*   `runDeploy` (default: `false`): If `true`, the pipeline will build a Docker image and push it to Docker Hub.
*   `imageName` (default: `'your-image-name'`): The name of the Docker image to build.
*   `dockerhubCredentialId` (default: `'DOCKER_HUB_CREDS'`): The ID of the Jenkins credentials for Docker Hub (username/password).
*   `dockerhubUsernameSecretId` (default: `'DOCKER_USERNAME'`): The ID of the Jenkins secret text credential for the Docker Hub username. This is used for tagging the image.
*   `pushLatestTag` (default: `true`): If `true`, the pipeline will push a `latest` tag to Docker Hub.
*   `dockerBuildPlatform` (default: `''`): The platform to build the Docker image for (e.g., `'linux/amd64'`, `'linux/arm64'`). If empty, it uses the build agent's default platform.

### Pipeline Stages

The `springBootPipeline` includes the following stages:

1.  **Resolve Version**: Determines the application version from the Git tag or pom.xml and commit hash.
2.  **Test**:
    *   Executes `mvn test`.
    *   Runs only if `runTest` is `true`.
3.  **Build**:
    *   Executes `mvn package`.
    *   Runs only if `runBuild` is `true`.
4.  **Check Docker**:
    *   Checks if the Docker daemon is running and accessible.
    *   Runs only if `runDeploy` is `true`.
5.  **Build Docker Image**:
    *   Builds a Docker image for your application.
    *   Runs only if `runDeploy` is `true`.
6.  **Push to Docker Hub**:
    *   Pushes the Docker image to Docker Hub.
    *   Runs only if `runDeploy` is `true`.

## Customization

You can modify the `vars/springBootPipeline.groovy` file to adjust the commands used in each stage or to add new stages as needed for your specific CI/CD requirements.
