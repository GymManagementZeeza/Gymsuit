pipeline {
    agent any

    environment {
        COMPOSE_PROJECT_NAME = 'gymmanagement'
        DOCKER_BUILDKIT = '1'
        COMPOSE_DOCKER_CLI_BUILD = '1'
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
