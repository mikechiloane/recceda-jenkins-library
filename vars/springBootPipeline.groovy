def call(Map config) {
    def defaultConfig = [
        runTest: true,
        runBuild: true,
        runDeploy: false,
        dockerhubUser: 'your-dockerhub-username',
        imageName: 'your-image-name',
        dockerhubCredentialId: 'dockerhub-credentials'
    ]
    config = defaultConfig + config

    def appVersion // This will hold the determined version

    pipeline {
        agent any
        tools {
            maven 'M3' // Use the name of your Maven installation configured in Jenkins
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
                        echo "Building Docker image with version: ${appVersion}"
                        def imageName = "${config.dockerhubUser}/${config.imageName}:${appVersion}"
                        docker.build(imageName, '.')
                    }
                }
            }

            stage('Push to Docker Hub') {
                when {
                    expression { config.runDeploy }
                }
                steps {
                    script {
                        echo "Pushing image to Docker Hub with version: ${appVersion}"
                        docker.withRegistry('https://index.docker.io/v1/', config.dockerhubCredentialId) {
                            def imageName = "${config.dockerhubUser}/${config.imageName}:${appVersion}"
                            docker.image(imageName).push()
                        }
                    }
                }
            }
        }
    }
}
