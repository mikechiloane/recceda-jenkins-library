def call(Map config) {
    def defaultConfig = [
        runTest: true,
        runBuild: true,
        runDeploy: false
    ]
    config = defaultConfig + config

    pipeline {
        agent any

        stages {
            stage('Test') {
                when {
                    expression { config.runTest }
                }
                steps {
                    script {
                        echo "Running tests..."
                        // Replace with your actual test command
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
                        // Replace with your actual build command
                        sh 'mvn package'
                    }
                }
            }

            stage('Deploy') {
                when {
                    expression { config.runDeploy }
                }
                steps {
                    script {
                        echo "Deploying the application..."
                        // Replace with your actual deploy command
                        echo "Deployment step is a placeholder."
                    }
                }
            }
        }
    }
}
