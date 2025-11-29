def call(Map config) {
    def defaultConfig = [
            runTest                  : true,
            runBuild                 : true,
            runDeploy                : false,
            imageName                : 'your-image-name',
            dockerhubCredentialId    : env.DOCKER_HUB_LOGIN_CREDENTIAL_ID ?: 'dockerhub-login-creds', // ID of Username/Password cred
            dockerhubUsernameSecretId: env.DOCKER_HUB_USERNAME_SECRET_ID ?: 'dockerhub-username-secret' // ID of Secret Text cred with username
    ]
}

config = defaultConfig + config

def appVersion // This will hold the determined version

pipeline {
    agent any
    tools {
        maven 'maven'
    }

    stages {
        stage('Resolve Version') {
            steps {
                script {
                    if (env.TAG_NAME) {
                        echo "Build triggered by Git tag ${env.TAG_NAME}. Using it as the version."
                        appVersion = env.TAG_NAME
                    } else {
                        echo "No Git tag found. Resolving version from pom.xml and Git commit."
                        def pomVersion = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                        def commitHash = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                        appVersion = "${pomVersion}-${commitHash}"
                    }
                    echo "Application version is: ${appVersion}"
                }
            }
        }

        stage('Test') {
            when {
                expression { config.runTest }
            }
            steps {
                script {
                    echo "Running tests..."
                    sh 'mvn test'
                }
            }
        }

        stage('Build') {
            when {
                expression { config.runBuild }
            }
            steps {
                script {
                    echo "Building the application..."
                    sh 'mvn package'
                }
            }
        }

        stage('Build Docker Image') {
            when {
                expression { config.runDeploy }
            }
            steps {
                script {
                    withCredentials([
                            string(credentialsId: config.dockerhubUsernameSecretId, variable: 'DOCKER_USERNAME_FOR_IMAGE'),
                            usernamePassword(credentialsId: config.dockerhubCredentialId, usernameVariable: 'UNUSED_USERNAME', passwordVariable: 'UNUSED_PASSWORD') // Need a dummy variable for usernamePassword if not directly used in this block.
                    ]) {
                        echo "Building Docker image with version: ${appVersion}"
                        def imageName = "${DOCKER_USERNAME_FOR_IMAGE}/${config.imageName}:${appVersion}"
                        docker.build(imageName, '.')
                    }
                }
            }
        }

        stage('Push to Docker Hub') {
            when {
                expression { config.runDeploy }
            }
            steps {
                script {
                    withCredentials([
                            string(credentialsId: config.dockerhubUsernameSecretId, variable: 'DOCKER_USERNAME_FOR_IMAGE'),
                            usernamePassword(credentialsId: config.dockerhubCredentialId, usernameVariable: 'DOCKER_LOGIN_USERNAME', passwordVariable: 'DOCKER_LOGIN_PASSWORD')
                    ]) {
                        echo "Pushing image to Docker Hub with version: ${appVersion}"
                        docker.withRegistry('https://index.docker.io/v1/', config.dockerhubCredentialId) {
                            def imageName = "${DOCKER_USERNAME_FOR_IMAGE}/${config.imageName}:${appVersion}"
                            docker.image(imageName).push()
                        }
                    }
                }
            }
        }
    }

}

