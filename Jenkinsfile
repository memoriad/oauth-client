pipeline {

    agent {
        docker {
            image 'maven:3.6.2-jdk-11-slim' 
            args '-v /root/jenkins_home/.m2:/root/.m2:z -u root' 
            reuseNode true
        }
    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean install -DskipTests=true'
            }
        }
        stage('Unit Test') {
            steps {
                sh 'mvn test'
            }
        }
    }
}
