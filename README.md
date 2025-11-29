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

springBootPipeline {
    runTest = true
    runBuild = true
    runDeploy = false
}
```

### Pipeline Parameters

The `springBootPipeline` accepts the following boolean parameters to control which stages are executed:

*   `runTest` (default: `true`): If `true`, the "Test" stage will execute `mvn test`.
*   `runBuild` (default: `true`): If `true`, the "Build" stage will execute `mvn package`.
*   `runDeploy` (default: `false`): If `true`, the "Deploy" stage will run. **Note: This stage is a placeholder and requires implementation specific to your deployment environment.**

### Pipeline Stages

The `springBootPipeline` includes the following stages:

1.  **Test:**
    *   Executes `mvn test`.
    *   Runs only if `runTest` is `true`.
2.  **Build:**
    *   Executes `mvn package`.
    *   Runs only if `runBuild` is `true`.
3.  **Deploy:**
    *   Currently, this stage contains a placeholder `echo "Deployment step is a placeholder."`.
    *   Runs only if `runDeploy` is `true`.
    *   **You must customize the `vars/springBootPipeline.groovy` file to add your actual deployment logic.**

## Customization

You can modify the `vars/springBootPipeline.groovy` file to adjust the commands used in each stage or to add new stages as needed for your specific CI/CD requirements.
