@Library('NUREG') _

pipeline {
    agent { label 'wonder' }

    stages {
        stage('SetupWorkspace') {
            steps {
                script {
                    properties([copyArtifactPermission('*')])
                }
                woCreateLibraryFolders(env.WORKSPACE)
                woCreateWoLipsProperties(env.WORKSPACE, true)
            }
        }

        stage('Build') {
            steps {
                withAnt(installation: '1.9.5') {
                    sh "ant -propertyfile ${env.WORKSPACE}/Root/wolips.properties -lib ${env.WORKSPACE}/Root/lib -Ddeployment.standalone=true clean docs-clean frameworks deployment.tools docs"
                }
            }
        }

        stage('PrepareArchives') {
            environment {
                ROOTS = "${env.WORKSPACE}/ExternalRoot"
                DIST = "${env.WORKSPACE}/dist"
                FILEPREFIX = "Wonder-${env.BUILD_NUMBER}-Frameworks"
            }
            steps {
                sh "mkdir -p ${DIST}"

                // make separate archives for frameworks and jars
                sh "ls ${ROOTS} | grep '.*framework\$' | tar -czf ${DIST}/${FILEPREFIX}.tar.gz -C ${ROOTS} -T -"
                sh "ls ${ROOTS} | grep '.*jar\$' | tar -czf ${DIST}/${FILEPREFIX}Jars.tar.gz -C ${ROOTS} -T -"

                // archive apps
                sh """
                    IFS=\$'\n'
                    for file in `ls ${ROOTS} | grep woa\$`; do
                        tar -czf ${DIST}/\$file.tar.gz -C ${ROOTS} \$file
                    done
                """

                sh "rm -Rf ${ROOTS}"

                // archive documentation
                sh "tar -czf ${DIST}/Wonder-${env.BUILD_NUMBER}-Documentation.tar.gz -C ${DIST}/wonder/Documentation api"
            }
        }
    }

    post {
        success {
            dir('dist') {
                archiveArtifacts artifacts: '*.tar.gz'
                step $class: 'JavadocArchiver', javadocDir: 'wonder/Documentation/api', keepAll: false
            }
            recordIssues tools: [java(), taskScanner(includePattern: '**/*.java', excludePattern: 'Dependencies/**', highTags: 'FIXME', normalTags: 'TODO', lowTags: '@Deprecated')]
        }
    }
}
