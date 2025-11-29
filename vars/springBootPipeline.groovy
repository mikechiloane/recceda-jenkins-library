def call(Map config) {
    def defaultConfig = [
            runTest                  : true,
            runBuild                 : true,
            runDeploy                : false,
            imageName                : 'your-image-name',
            dockerhubCredentialId    : 'DOCKER_HUB_CREDS',
            dockerhubUsernameSecretId: 'DOCKER_USERNAME',
            pushLatestTag            : true,
            dockerBuildPlatform      : ''
    ]

    config = defaultConfig + config

    def appVersion

    pipeline {
        agent any

        tools {
            maven 'maven'
        }

        environment {
            PATH = "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${env.PATH}"
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

            stage('Check Docker') {
                when {
                    expression { config.runDeploy }
                }
                steps {
                    script {
                        echo "Checking Docker daemon connection..."
                        def dockerInfoResult = sh(script: 'docker info', returnStatus: true)
                        if (dockerInfoResult != 0) {
                            error "Could not connect to Docker daemon. Please ensure Docker is running and accessible."
                        }
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
                                string(credentialsId: config.dockerhubUsernameSecretId, variable: 'DOCKER_USERNAME')
                        ]) {
                            echo "Building Docker image with version: ${appVersion}"

                            // Clean the username in case it has extra characters
                            def cleanUsername = DOCKER_USERNAME.trim()
                            def imageName = "${cleanUsername}/${config.imageName}"

                            echo "Image name: ${imageName}:${appVersion}"

                            // Verify Docker is available
                            sh 'docker --version'

                            // Build and tag with version
                            def platformFlag = ''
                            if (config.dockerBuildPlatform) {
                                platformFlag = '--platform=' + config.dockerBuildPlatform
                            }
                            sh 'docker build ' + platformFlag + ' -t ' + imageName + ':' + appVersion + ' .'

                            // Also tag as latest if enabled
                            if (config.pushLatestTag) {
                                sh 'docker tag ' + imageName + ':' + appVersion + ' ' + imageName + ':latest'
                            }
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
                                string(credentialsId: config.dockerhubUsernameSecretId, variable: 'DOCKER_USERNAME'),
                                usernamePassword(credentialsId: config.dockerhubCredentialId,
                                        usernameVariable: 'DOCKER_USER',
                                        passwordVariable: 'DOCKER_PASS')
                        ]) {
                            echo "Verifying Docker credentials..."

                            // Clean the username
                            def cleanUsername = env.DOCKER_USERNAME.trim()
                            def imageName = "${cleanUsername}/${config.imageName}"

                            echo "Logging in to Docker Hub as: ${env.DOCKER_USER}"
                            def loginResult = sh(
                                    script: 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin',
                                    returnStatus: true
                            )

                            if (loginResult != 0) {
                                error "Docker login failed! Check your credentials."
                            }

                            echo "Login successful. Pushing ${imageName}:${appVersion}"

                            // Push versioned tag using sh with multi-line string to avoid interpolation warning
                            sh 'docker push ' + imageName + ':' + appVersion

                            // Push latest tag if enabled
                            if (config.pushLatestTag) {
                                sh 'docker push ' + imageName + ':latest'
                            }

                            echo "Logging out from Docker Hub..."
                            sh 'docker logout'
                        }
                    }
                }
            }
        }

        post {
            always {
                script {
                    // Clean up Docker images to save space
                    withCredentials([
                            string(credentialsId: config.dockerhubUsernameSecretId, variable: 'DOCKER_USERNAME')
                    ]) {
                        def imageName = "${DOCKER_USERNAME}/${config.imageName}"
                        sh 'docker rmi ' + imageName + ':' + appVersion + ' || true'
                        sh 'docker rmi ' + imageName + ':latest || true'
                    }
                }
            }
            success {
                echo "Pipeline completed successfully!"
                echo "Docker image pushed: ${appVersion}"
            }
            failure {
                echo "Pipeline failed. Check the logs for details."
            }
        }
    }
}