pipeline {
    agent any

    environment {
        // Every branch is built (a compile check), but only main is deployed.
        // Other branches build under their own project name so their images
        // never replace the ones the live containers come from.
        COMPOSE_PROJECT_NAME = "${env.BRANCH_NAME == 'main' ? 'gymmanagement' : 'gymmanagement-ci'}"
        DOCKER_BUILDKIT = '1'
        COMPOSE_DOCKER_CLI_BUILD = '1'
        NEXT_PUBLIC_API_BASE_URL = 'https://api.gymsuit.app'
        APP_CORS_ALLOWED_ORIGINS = 'https://gymsuit.app,http://gymsuit.app,https://diet.gymsuit.app,http://localhost:3000,http://localhost:3001'
        SPRING_DATASOURCE_URL = 'jdbc:mysql://192.168.0.37:3306/gymmanagement'
        SPRING_DATASOURCE_USERNAME = 'athul'
        // TEST ONLY: fixed-OTP sign-in stays OFF unless the server deliberately sets
        // TEST_LOGIN_EMAIL (a dedicated QA address, never a real user's email) and a
        // 6-digit TEST_LOGIN_OTP. It used to be hardcoded here to the owner's email,
        // which silently stopped real OTP emails to that account (API still said sent).
    }

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build images') {
            steps {
                withCredentials([
                    string(credentialsId: 'gymmanagement-db-password', variable: 'SPRING_DATASOURCE_PASSWORD'),
                    string(credentialsId: 'gymmanagement-jwt-secret', variable: 'APP_JWT_SECRET'),
                    string(credentialsId: 'resend-api-key', variable: 'RESEND_API_KEY'),
                    string(credentialsId: 'resend-from-email', variable: 'RESEND_FROM_EMAIL'),
                    string(credentialsId: 'openrouter-api-key', variable: 'OPENROUTER_API_KEY'),
                    string(credentialsId: 'openai-api-key', variable: 'OPENAI_API_KEY')
                ]) {
                    sh 'docker compose build'
                }
            }
        }

        stage('Deploy') {
            when { branch 'main' }
            steps {
                withCredentials([
                    string(credentialsId: 'gymmanagement-db-password', variable: 'SPRING_DATASOURCE_PASSWORD'),
                    string(credentialsId: 'gymmanagement-jwt-secret', variable: 'APP_JWT_SECRET'),
                    string(credentialsId: 'resend-api-key', variable: 'RESEND_API_KEY'),
                    string(credentialsId: 'resend-from-email', variable: 'RESEND_FROM_EMAIL'),
                    string(credentialsId: 'openrouter-api-key', variable: 'OPENROUTER_API_KEY'),
                    string(credentialsId: 'openai-api-key', variable: 'OPENAI_API_KEY')
                ]) {
                    sh 'docker compose down --remove-orphans || true'
                    sh 'docker compose up -d --remove-orphans'
                }
            }
        }

        stage('Prune old images') {
            steps {
                sh 'docker image prune -f'
            }
        }
    }

    post {
        failure {
            echo 'Build or deploy failed — containers were left as they were before this run.'
        }
    }
}
