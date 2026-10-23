pipeline {
  agent { label 'build' }          // jamais le controller

  options {
    timeout(time: 10, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '20'))
    disableConcurrentBuilds(abortPrevious: true)
  }

  environment {
    DEPLOY_BRANCH = 'feature/ci-jenkins'   // branche qui déclenche staging et production
    NEXUS_URL = 'http://nexus:8081'
    NEXUS_CREDS = credentials('nexus-creds')   // expose NEXUS_CREDS_USR / NEXUS_CREDS_PSW
  }

  stages {
    stage('Lint & tests') {
      parallel {
        stage('Backend') {
          steps {
            sh 'chmod +x mvnw && ./mvnw -B test'
          }
          post { always { junit 'target/surefire-reports/*.xml' } }
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
          sh './mvnw -B -DskipTests verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=job-application-tracker'
        }
      }
    }
    stage('Quality Gate') {
      steps {
        timeout(time: 5, unit: 'MINUTES') { waitForQualityGate abortPipeline: true }
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

    stage('Deploy staging') {
      when { expression { env.BRANCH_NAME == env.DEPLOY_BRANCH } }
      steps {
        sh 'NEXUS_URL=$NEXUS_URL NEXUS_USER=$NEXUS_CREDS_USR NEXUS_PASSWORD=$NEXUS_CREDS_PSW sh ci/scripts/deploy.sh staging "$VERSION"'
      }
    }

    stage('Deploy production') {
      when { expression { env.BRANCH_NAME == env.DEPLOY_BRANCH } }
      steps {
        input message: "Déployer ${env.VERSION} en production ?", submitter: 'admin'
        sh 'NEXUS_URL=$NEXUS_URL NEXUS_USER=$NEXUS_CREDS_USR NEXUS_PASSWORD=$NEXUS_CREDS_PSW sh ci/scripts/deploy.sh production "$VERSION"'
      }
    }
  }
}
