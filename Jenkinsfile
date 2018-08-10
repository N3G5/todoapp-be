def version, mvnCmd = "mvn -Dmaven.repo.local=/tmp/artifacts/m2 -s configuration/settings.xml -e -Popenshift"

          pipeline {
            agent {
              label 'maven'
            }
            stages {
              stage('Build App') {
                steps {
                  git branch: 'master', url: 'http://gogs:3000/gogs/todoapp-be.git'
                  script {
                      def pom = readMavenPom file: 'pom.xml'
                      version = pom.version
                  }
                  sh "${mvnCmd} -DskipTests -Dcom.redhat.xpaas.repo.redhatga -Dfabric8.skip=true package --batch-mode -Djava.net.preferIPv4Stack=true"
                }
              }
//              stage('Test') {
//                steps {
//                  sh "${mvnCmd} test"
//                  step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
//                }
//              }
//              stage('Code Analysis') {
//                steps {
//                  script {
//                    sh "${mvnCmd} sonar:sonar -Dsonar.host.url=http://sonarqube:9000 -DskipTests=true"
//                  }
//                }
//              }
              stage('Archive App') {
                steps {
                  sh "${mvnCmd} deploy -DskipTests=true -P nexus3"
                }
              }
              stage('Create Image Builder') {
                when {
                  expression {
                    openshift.withCluster() {
                      openshift.withProject(env.DEV_PROJECT) {
                        return !openshift.selector("bc", "backend").exists();
                      }
                    }
                  }
                }
                steps {
                  script {
                    openshift.withCluster() {
                      openshift.withProject(env.DEV_PROJECT) {
                        openshift.newBuild("--name=backend", "--image-stream=java:8", "--binary=true")
                      }
                    }
                  }
                }
              }
              stage('Build Image') {
                steps {
                  sh "cp target/*.jar oc-build/deployments/"
                  
                  script {
                    openshift.withCluster() {
                      openshift.withProject(env.DEV_PROJECT) {
                        openshift.selector("bc", "backend").startBuild("--from-dir=oc-build", "--wait=true")
                      }
                    }
                  }
                }
              }
              stage('Create DEV') {
                when {
                  expression {
                    openshift.withCluster() {
                      openshift.withProject(env.DEV_PROJECT) {
                        return !openshift.selector('dc', 'backend').exists()
                      }
                    }
                  }
                }
                steps {
                  script {
                    openshift.withCluster() {
                      openshift.withProject(env.DEV_PROJECT) {
                        def app = openshift.newApp(readFile('dcBackend.yaml'));
                        app.narrow("svc").expose();
                        
                        openshift.set("");

                        openshift.set("probe dc/backend --readiness --get-url=http://:8080/ws/demo/healthcheck --initial-delay-seconds=30 --failure-threshold=10 --period-seconds=10")
                        openshift.set("probe dc/backend --liveness  --get-url=http://:8080/ws/demo/healthcheck --initial-delay-seconds=180 --failure-threshold=10 --period-seconds=10")

                        def dc = openshift.selector("dc", "backend")
                        while (dc.object().spec.replicas != dc.object().status.availableReplicas) {
                            sleep 10
                        }
                        openshift.set("triggers", "dc/backend", "--manual")
                      }
                    }
                  }
                }
              }
              stage('Deploy DEV') {
                steps {
                  script {
                    openshift.withCluster() {
                      openshift.withProject(env.DEV_PROJECT) {
                        openshift.selector("dc", "backend").rollout().latest();
                      }
                    }
                  }
                }
              }
            }
          }