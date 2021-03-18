package com.dtolabs.rundeck.core.storage

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import org.rundeck.storage.api.PathUtil
import spock.lang.Specification

class ProjectKeyStorageContextProviderSpec extends Specification {
    def "test environmentforPath app level"() {
        given:
            def provider = new ProjectKeyStorageContextProvider()
        expect:

        AuthorizationUtil.RUNDECK_APP_ENV == provider.environmentForPath(PathUtil.asPath(path))
        where:
            path << [
                "test1",
                "project/",
                "project/milkdud",
                "project/milkdud/asdf"
            ]
    }

    def "test environmentForPath projectLevel"() {
        given:
            def provider = new ProjectKeyStorageContextProvider()
        expect:
             provider.environmentForPath(PathUtil.asPath(path)).contains(AuthorizationUtil.projectContext(project)[0])
        where:
            project = "milkdud"
            path << [
                "keys/project/milkdud",
                "keys/project/milkdud/",
                "keys/project/milkdud/tooth",
                "keys/project/milkdud/tooth/something/blah"
            ]
    }
}
