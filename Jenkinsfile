pipeline {
    agent { label 'wonder' }

    stages {
        stage('SetupWorkspace') {
            environment {
                DEPS = "${env.WORKSPACE}/Dependencies"
                WO_VERSION = '54'
                ROOT = "${env.WORKSPACE}/Root"
            }
            steps {
                // Install and extract dependencies
                copyArtifacts projectName: '/Libraries/WebObjects/WebObjects-Dependencies/master', target: DEPS
                sh "tar -xzf ${DEPS}/WebObjects${WO_VERSION}.tar.gz -C ${DEPS}"

                // Make sure the Libraries folder exists
                sh "mkdir -p ${env.WORKSPACE}/Libraries"

                // Setup System and Library
                sh "mkdir -p ${ROOT}/lib"
                sh "cp ${DEPS}/woproject.jar ${ROOT}/lib"
                sh "mkdir -p ${ROOT}/Library/Frameworks"
                sh "mkdir -p ${ROOT}/Library/WebObjects/Extensions"
                sh "mkdir -p ${ROOT}/Network/Library/Frameworks"
                sh "mkdir -p ${ROOT}/User/Library/Frameworks"
                sh "ln -sf ${DEPS}/WebObjects${WO_VERSION}/System ${ROOT}/System"

                // Setup wolips.properties
                writeFile file: "${ROOT}/wolips.properties", text: """
                    wo.system.root=${ROOT}/System
                    wo.user.frameworks=${ROOT}/User/Library/Frameworks
                    wo.system.frameworks=${ROOT}/System/Library/Frameworks
                    wo.bootstrapjar=${ROOT}/System/Library/WebObjects/JavaApplications/wotaskd.woa/WOBootstrap.jar
                    wo.network.frameworks=${ROOT}/Network/Library/Frameworks
                    wo.api.root=/Developer/ADC%20Reference%20Library/documentation/WebObjects/Reference/API/
                    wo.network.root=${ROOT}/Network
                    wo.extensions=${ROOT}/Library/WebObjects/Extensions
                    wo.user.root=${ROOT}/User
                    wo.local.frameworks=${ROOT}/Library/Frameworks
                    wo.apps.root=${ROOT}/Library/WebObjects/Applications
                    wo.local.root=${ROOT}
                    wo.external.root=${env.WORKSPACE}/ExternalRoot
                """.stripIndent().trim() + "\n"
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
            archiveArtifacts artifacts: 'dist/*.tar.gz'
            step $class: 'JavadocArchiver', javadocDir: 'dist/wonder/Documentation/api', keepAll: false
            recordIssues tools: [java(), taskScanner(includePattern: '**/*.java', excludePattern: 'Dependencies/**', highTags: 'FIXME', normalTags: 'TODO', lowTags: '@Deprecated')]
        }
    }
}
