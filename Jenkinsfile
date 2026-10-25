@Library('jenkins-shared-library@v1.0.1') _
pipeline {
  agent { label 'build' }          // jamais le controller

  options {
    timeout(time: 60, unit: 'MINUTES')   // inclut l'attente de validation manuelle
    buildDiscarder(logRotator(numToKeepStr: '20'))
    disableConcurrentBuilds(abortPrevious: true)
  }

  environment {
    DEPLOY_BRANCH = 'feature/ci-jenkins'   // branche qui déclenche staging et production
    NEXUS_URL = 'http://nexus:8081'
    GITHUB_REPO = 'mariamfathallah/job-application-tracker'   // pour publier le plan d'infra sur les PR
    NEXUS_CREDS = credentials('nexus-creds')   // expose NEXUS_CREDS_USR / NEXUS_CREDS_PSW
  }

  stages {
    // Conventional Commits imposés sur les PR (nécessaires à semantic-release)
    stage('Commit lint') {
        when { changeRequest() }
        steps {
          sh 'npm ci'
          sh 'git fetch --no-tags origin +refs/heads/$CHANGE_TARGET:refs/remotes/origin/$CHANGE_TARGET'
          sh 'npx commitlint --from origin/$CHANGE_TARGET --to HEAD --verbose'
        }
    }
    stage('Lint & tests') {
      parallel {
        stage('Backend') {
          steps {
            sh 'chmod +x mvnw && ./mvnw -B verify'
          }
          post {
            always {
              junit 'target/surefire-reports/*.xml'
              recordCoverage(
                tools: [[parser: 'JACOCO', pattern: 'target/site/jacoco/jacoco.xml']],
                sourceCodeRetention: 'EVERY_BUILD'
              )
            }
          }
        }
        stage('Frontend') {
          steps {
            dir('frontend') {
              sh 'npm ci'
              sh 'npm run lint'
              sh 'npm run test:run -- --reporter=default --reporter=junit --outputFile.junit=junit.xml'
              sh 'npm run build'
            }
          }
          post { always { junit allowEmptyResults: true, testResults: 'frontend/junit.xml' } }
        }
      }
    }

    stage('SonarQube') {
      steps {
        withSonarQubeEnv('sonar') {
          sh './mvnw -B -DskipTests org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=job-application-tracker'
        }
      }
    }
    stage('Quality Gate') {
      steps {
        timeout(time: 5, unit: 'MINUTES') { waitForQualityGate abortPipeline: true }
      }
    }

    stage('Integration tests') {
      steps {
        sh 'IT_HOST=host.docker.internal sh ci/scripts/integration-test.sh'
      }
      post {
        always {
          sh 'docker compose -f docker-compose.test.yml -p tracker-it-${EXECUTOR_NUMBER:-0} down -v || true'
        }
      }
    }

    stage('Package & publish') {
      steps {
        script {
          env.VERSION = "0.0.1-${sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()}"
        }
        sh './mvnw -B -DskipTests package'
        sh '''
          JAR=$(ls target/*.jar | grep -v '\\.original$' | head -n1)
          cp "$JAR" "target/job-application-tracker-${VERSION}.jar"
          curl -fsS -u "$NEXUS_CREDS_USR:$NEXUS_CREDS_PSW" --upload-file "target/job-application-tracker-${VERSION}.jar" \
            "$NEXUS_URL/repository/artifacts/job-application-tracker/${VERSION}/job-application-tracker-${VERSION}.jar"
        '''
        archiveArtifacts artifacts: 'target/job-application-tracker-*.jar', fingerprint: true
      }
    }

    // Environnements as Code : OpenTofu décrit staging/production (infra/terraform),
    // Ansible y installe l'application (infra/ansible). Le plan est calculé sur chaque branche.
    stage('Infra plan') {
      steps {
        sh 'sh ci/scripts/infra.sh plan staging'
        sh 'sh ci/scripts/infra.sh plan production'
        archiveArtifacts artifacts: 'infra/terraform/plan-*.txt'
      }
    }

    stage('Publish plan on PR') {
      when { changeRequest() }
      steps {
        withCredentials([usernamePassword(credentialsId: 'github-token', usernameVariable: 'GH_USER', passwordVariable: 'GH_TOKEN')]) {
          sh 'sh ci/scripts/comment-plan.sh'
        }
      }
    }

    stage('Deploy staging') {
      when { expression { env.BRANCH_NAME == env.DEPLOY_BRANCH } }
      steps {
        sh 'sh ci/scripts/infra.sh apply staging'
        sh 'NEXUS_URL=$NEXUS_URL NEXUS_USER=$NEXUS_CREDS_USR NEXUS_PASSWORD=$NEXUS_CREDS_PSW sh ci/scripts/deploy.sh staging "$VERSION"'
      }
    }

    stage('Deploy production') {
      when { expression { env.BRANCH_NAME == env.DEPLOY_BRANCH } }
      steps {
        timeout(time: 30, unit: 'MINUTES') {
          input message: "Déployer ${env.VERSION} en production ? (plan d'infra : artefact plan-production.txt)", submitter: 'admin'
        }
        sh 'sh ci/scripts/infra.sh apply production'   // applique exactement le plan validé
        sh 'NEXUS_URL=$NEXUS_URL NEXUS_USER=$NEXUS_CREDS_USR NEXUS_PASSWORD=$NEXUS_CREDS_PSW sh ci/scripts/deploy.sh production "$VERSION"'
      }
    }
  }
  post {
    always {
      notifyDiscord()
    }
  }
}
