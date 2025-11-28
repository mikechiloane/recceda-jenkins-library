def call(Map config = [:]) {
    pipeline {
        agent any

        parameters {
            booleanParam(name: 'runTest', defaultValue: true, description: 'Run tests?')
            booleanParam(name: 'runBuild', defaultValue: true, description: 'Run build?')
            booleanParam(name: 'runDeploy', defaultValue: false, description: 'Run deploy?')
        }

        stages {
            stage('Test') {
                when {
                    expression { params.runTest }
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
                    expression { params.runBuild }
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
                    expression { params.runDeploy }
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
