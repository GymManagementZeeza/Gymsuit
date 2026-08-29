pipeline {
    agent any

    environment {
        COMPOSE_PROJECT_NAME = 'gymmanagement'
        // .env lives outside the repo on the server (it holds real secrets and
        // is not checked into git). Create it once from .env.example.
        ENV_FILE = '/opt/gymmanagement/.env'
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

        stage('Verify env file') {
            steps {
                sh '''
                    if [ ! -f "$ENV_FILE" ]; then
                        echo "Missing $ENV_FILE — copy .env.example to it on the server and fill in real values."
                        exit 1
                    fi
                '''
            }
        }

        stage('Build images') {
            steps {
                sh 'docker compose --env-file "$ENV_FILE" build'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker compose --env-file "$ENV_FILE" up -d --remove-orphans'
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
