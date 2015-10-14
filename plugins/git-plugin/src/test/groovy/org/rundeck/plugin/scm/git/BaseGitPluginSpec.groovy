package org.rundeck.plugin.scm.git

import org.rundeck.plugin.scm.git.config.Common
import spock.lang.Specification

/**
 * Created by greg on 10/14/15.
 */
class BaseGitPluginSpec extends Specification {
    def "getSshConfig"() {
        given:
        Common config = new Common(configInput)
        def base = new BaseGitPlugin(config)
        when:
        def sshConfig = base.sshConfig

        then:
        sshConfig == sshResult

        where:
        sshResult                      | configInput
        [:]                            | [:]
        [StrictHostKeyChecking: 'yes'] | [strictHostKeyChecking: 'yes']
        [StrictHostKeyChecking: 'no']  | [strictHostKeyChecking: 'no']
        [:]                            | [strictHostKeyChecking: 'ask']
        [:]                            | [strictHostKeyChecking: 'other']
    }

    
}
