// ============================================================================
// Jenkinsfile - Pipeline CI pour myschool-ms
// Sprint 3 DevOps - Validation S11
// ============================================================================
// Ce pipeline automatise :
//   1. Checkout du code depuis GitHub
//   2. Build Maven des 5 microservices
//   3. Exécution des tests unitaires avec JaCoCo (couverture)
//   4. Analyse qualité du code avec SonarQube
// ============================================================================

pipeline {
    agent any

    // Outils configurés dans Jenkins (Manage Jenkins > Tools)
    tools {
        maven 'Maven3'
        jdk   'JDK17'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {

        // ====================================================================
        // STAGE 1 : Récupération du code source
        // ====================================================================
        stage('Checkout') {
            steps {
                echo '>>> [1/4] Recuperation du code source depuis GitHub...'
                checkout scm
                echo '>>> Code source recupere avec succes.'
            }
        }

        // ====================================================================
        // STAGE 2 : Compilation Maven multi-modules
        // ====================================================================
        stage('Build') {
            steps {
                echo '>>> [2/4] Compilation des microservices (multi-modules Maven)...'
                sh 'mvn clean compile -DskipTests'
                echo '>>> Compilation terminee avec succes.'
            }
        }

        // ====================================================================
        // STAGE 3 : Tests unitaires avec JaCoCo (couverture de code)
        // ====================================================================
        stage('Unit Tests') {
            steps {
                echo '>>> [3/4] Execution des tests unitaires avec couverture JaCoCo...'
                sh 'mvn test'
                echo '>>> Tests unitaires termines.'
            }
            post {
                always {
                    // Publier les rapports de test dans l'UI Jenkins
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        // ====================================================================
        // STAGE 4 : Analyse qualité du code avec SonarQube
        // ====================================================================
        stage('SonarQube Analysis') {
            steps {
                echo '>>> [4/4] Analyse qualite du code avec SonarQube...'
                withSonarQubeEnv('SonarQubeServer') {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=MySchool-ms \
                          -Dsonar.projectName="MySchool Backend"
                    '''
                }
                echo '>>> Analyse SonarQube terminee.'
            }
        }
    }

    // ========================================================================
    // Actions de fin de pipeline
    // ========================================================================
    post {
        success {
            echo '============================================================'
            echo 'PIPELINE BACKEND : SUCCES !'
            echo 'Voir les resultats SonarQube : http://localhost:9000'
            echo '============================================================'
        }
        failure {
            echo '============================================================'
            echo 'PIPELINE BACKEND : ECHEC - Verifier les logs ci-dessus'
            echo '============================================================'
        }
        always {
            echo '>>> Nettoyage du workspace termine.'
        }
    }
}