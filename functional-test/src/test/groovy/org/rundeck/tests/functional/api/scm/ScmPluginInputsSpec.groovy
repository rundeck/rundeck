package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.ScmPluginInputFieldsResponse
import org.rundeck.util.container.BaseContainer

@APITest
class ScmPluginInputsSpec extends BaseContainer {

    static List<String> COMMON_INPUT_FIELD_NAMES = ['pathTemplate','dir','url','branch','strictHostKeyChecking','sshPrivateKeyPath','gitPasswordPath','format','fetchAutomatically','pullAutomatically']
    static List<String> EXPORT_INPUT_FIELD_NAMES = ['committerName','committerEmail','exportUuidBehavior','createBranch','baseBranch'] + COMMON_INPUT_FIELD_NAMES
    static List<String> IMPORT_INPUT_FIELD_NAMES = ['importUuidBehavior','useFilePattern','filePattern'] + COMMON_INPUT_FIELD_NAMES

    def "must retrieve the list of required fields to setup a plugin"(){
        given:
        String projectName = "${PROJECT_NAME}-P6"
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)

        when:
        ScmPluginInputFieldsResponse inputsResponse = scmClient.callGetInputFieldsForPlugin().response

        then:
        noExceptionThrown()
        inputsResponse.integration == integration
        inputsResponse.type == scmClient.pluginName
        inputsResponse.fields.size() == expectedInputFieldNames.size()
        inputsResponse.fields.name.containsAll(expectedInputFieldNames)

        where:
        integration | expectedInputFieldNames
        'import'    | IMPORT_INPUT_FIELD_NAMES
        'export'    | EXPORT_INPUT_FIELD_NAMES
    }
}
