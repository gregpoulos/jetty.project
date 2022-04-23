#!groovy

pipeline {
  agent {
    node { label 'linux' }
  }
  // save some io during the build
  options { durabilityHint('PERFORMANCE_OPTIMIZED') }
  stages {
    stage("Checkout Jetty") {
      steps {
        container('jetty-build') {
          dir("${env.WORKSPACE}/buildy") {
            checkout scm
          }
        }
      }
    }
    stage("Build & Test - JDK17") {
      stages {
        stage("Setup") {
          steps {
            container('jetty-build') {
              timeout(time: 120, unit: 'MINUTES') {
                dir("${env.WORKSPACE}/buildy") {
                  echo "Install org.eclipse.jetty:build-resources"
                  mavenBuild("jdk17", "clean install -f build", "maven3")
                  echo "Install org.eclipse.jetty:jetty-project"
                  mavenBuild("jdk17", "-N clean install", "maven3")
                }
              }
            }
          }
        }
        stage("Module : /jetty-core/") {
          steps {
            container('jetty-build') {
              timeout(time: 120, unit: 'MINUTES') {
                dir("${env.WORKSPACE}/buildy") {
                  mavenBuild("jdk17", "clean install -f jetty-core", "maven3")
                }
              }
            }
          }
        }
        stage("Module : /jetty-ee9/") {
          steps {
            container('jetty-build') {
              timeout(time: 120, unit: 'MINUTES') {
                dir("${env.WORKSPACE}/buildy") {
                  mavenBuild("jdk17", "clean install -f jetty-ee9", "maven3")
                }
              }
            }
          }
        }
        stage("Module : /jetty-ee10/") {
          steps {
            container('jetty-build') {
              timeout(time: 120, unit: 'MINUTES') {
                dir("${env.WORKSPACE}/buildy") {
                  mavenBuild("jdk17", "clean install -f jetty-ee10", "maven3")
                }
              }
            }
          }
        }
        /*stage("Module : /jetty-integrations/") {
          steps {
            container('jetty-build') {
              timeout(time: 120, unit: 'MINUTES') {
                dir("${env.WORKSPACE}/buildy") {
                  mavenBuild("jdk17", "clean install -f jetty-integrations", "maven3")
                }
              }
            }
          }
        }*/
        /*stage("Module : /jetty-home/") {
          steps {
            container('jetty-build') {
              timeout(time: 120, unit: 'MINUTES') {
                dir("${env.WORKSPACE}/buildy") {
                  mavenBuild("jdk17", "clean install -f jetty-home", "maven3")
                }
              }
            }
          }
        }*/
        /*
        stage("Module : /tests/") {
          steps {
            container('jetty-build') {
              timeout(time: 120, unit: 'MINUTES') {
                dir("${env.WORKSPACE}/buildy") {
                  mavenBuild("jdk17", "clean install -f tests", "maven3")
                }
              }
            }
          }
        }*/
        /*stage("Module : /documentation/") {
          steps {
            container('jetty-build') {
              timeout(time: 120, unit: 'MINUTES') {
                dir("${env.WORKSPACE}/buildy") {
                  mavenBuild("jdk17", "clean install -f documentation", "maven3")
                }
              }
            }
          }
        }*/
      }
    }
  }
}

/**
 * To other developers, if you are using this method above, please use the following syntax.
 *
 * mavenBuild("<jdk>", "<profiles> <goals> <plugins> <properties>"
 *
 * @param jdk the jdk tool name (in jenkins) to use for this build
 * @param cmdline the command line in "<profiles> <goals> <properties>"`format.
 * @return the Jenkinsfile step representing a maven build
 */
def mavenBuild(jdk, cmdline, mvnName) {
    try {
      withEnv(["JAVA_HOME=${ tool "$jdk" }",
               "PATH+MAVEN=${ tool "$jdk" }/bin:${tool "$mvnName"}/bin",
               "MAVEN_OPTS=-Xms2g -Xmx4g -Djava.awt.headless=true"]) {
        configFileProvider(
                [configFile(fileId: 'oss-settings.xml', variable: 'GLOBAL_MVN_SETTINGS')]) {
          sh "mvn --no-transfer-progress -s $GLOBAL_MVN_SETTINGS -Pci --show-version --batch-mode --errors -Djetty.testtracker.log=true -Dmaven.test.failure.ignore=true $cmdline"
        }
      }
    }
    finally
    {
      junit testResults: '**/target/surefire-reports/*.xml,**/target/invoker-reports/TEST*.xml', allowEmptyResults: true
    }
}

// vim: et:ts=2:sw=2:ft=groovy
