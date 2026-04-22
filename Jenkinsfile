// =============================================================================
// Jenkinsfile - Pipeline CI/CD Backend MySchool (Spring Boot Microservices)
// =============================================================================
// Objectif : automatiser Build, Tests unitaires et analyse SonarQube
// Outils configures dans Jenkins :
//   - Maven3  : installation Maven declaree dans Global Tool Configuration
//   - JDK17   : installation JDK 17 declaree dans Global Tool Configuration
//   - SonarQubeServer : serveur Sonar configure dans Manage Jenkins > System
// =============================================================================

pipeline {

    // Le pipeline s'execute sur n'importe quel agent Jenkins disponible
    agent any

    // Declaration des outils necessaires (resolus depuis la configuration Jenkins)
    tools {
        maven 'Maven3'
        jdk   'JDK17'
    }

    // Options globales du pipeline
    options {
        // On conserve uniquement les 10 derniers builds pour limiter l'espace disque
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Timeout global du pipeline (30 minutes)
        timeout(time: 30, unit: 'MINUTES')
        // Affichage du timestamp dans les logs de la console
        timestamps()
    }

    // Variables d'environnement partagees entre les etapes
    environment {
        // Cle du projet cote SonarQube (doit correspondre a celle creee sur Sonar)
        SONAR_PROJECT_KEY = 'myschool-ms'
    }

    stages {

        // ---------------------------------------------------------------------
        // STAGE 1 : Checkout
        // ---------------------------------------------------------------------
        // Recupere le code source depuis le repository Git configure sur le job
        stage('Checkout') {
            steps {
                echo '>>> [1/4] Recuperation du code source depuis GitHub...'
                checkout scm
                echo '>>> Code source recupere avec succes.'
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 2 : Build
        // ---------------------------------------------------------------------
        // Compile tous les modules Maven sans executer les tests (plus rapide)
        stage('Build') {
            steps {
                echo '>>> [2/4] Compilation des microservices (multi-modules Maven)...'
                // -B : mode batch (pas d'interactions), -DskipTests : on saute les tests ici
                bat 'mvn clean compile -DskipTests -B'
                echo '>>> Compilation terminee avec succes.'
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 3 : Unit Tests
        // ---------------------------------------------------------------------
        // Execute les tests unitaires JUnit et publie les rapports dans Jenkins
        stage('Unit Tests') {
            steps {
                echo '>>> [3/4] Execution des tests unitaires (JUnit + JaCoCo)...'
                // JaCoCo est branche via prepare-agent (defini dans le pom parent)
                bat 'mvn test -B'
                echo '>>> Tests unitaires termines.'
            }
            post {
                // Toujours publier les rapports de tests, meme si un test a echoue
                always {
                    echo '>>> Publication des rapports JUnit dans Jenkins...'
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 4 : SonarQube Analysis
        // ---------------------------------------------------------------------
        // Envoie le code + rapports JaCoCo a SonarQube pour analyse qualite
        stage('SonarQube Analysis') {
            steps {
                echo '>>> [4/4] Analyse qualite du code via SonarQube...'
                // withSonarQubeEnv injecte l'URL + le token du serveur "SonarQubeServer"
                withSonarQubeEnv('SonarQubeServer') {
                    bat "mvn sonar:sonar -Dsonar.projectKey=${SONAR_PROJECT_KEY} -B"
                }
                echo '>>> Analyse SonarQube envoyee. Resultats visibles sur http://localhost:9000'
            }
        }
    }

    // -------------------------------------------------------------------------
    // Post-actions : executees a la fin du pipeline (succes ou echec)
    // -------------------------------------------------------------------------
    post {
        success {
            echo '============================================================'
            echo 'PIPELINE BACKEND : SUCCES - Build + Tests + Sonar OK'
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
