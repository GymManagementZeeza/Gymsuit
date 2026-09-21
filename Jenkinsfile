pipeline {
    agent any

    environment {
        COMPOSE_PROJECT_NAME = 'gymmanagement'
        DOCKER_BUILDKIT = '1'
        COMPOSE_DOCKER_CLI_BUILD = '1'
        NEXT_PUBLIC_API_BASE_URL = 'https://api.gymsuit.app'
        APP_CORS_ALLOWED_ORIGINS = 'https://gymsuit.app,http://gymsuit.app,http://localhost:3000,http://localhost:3001'
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
                    string(credentialsId: 'gymmanagement-jwt-secret', variable: 'APP_JWT_SECRET')
                ]) {
                    sh 'docker compose build'
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([
                    string(credentialsId: 'gymmanagement-db-password', variable: 'SPRING_DATASOURCE_PASSWORD'),
                    string(credentialsId: 'gymmanagement-jwt-secret', variable: 'APP_JWT_SECRET')
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
