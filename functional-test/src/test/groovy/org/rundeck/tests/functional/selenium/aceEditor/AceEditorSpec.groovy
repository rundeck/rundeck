package org.rundeck.tests.functional.selenium.aceEditor

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class AceEditorSpec extends SeleniumBase{

    def "Edit json file resource model with indented text"(){

        // 1. Logs, create a project
        // 2. Adds a json file resource model
        // 3. Attempt to edit the file, test the ace editor rendered lines based on JSON text (must be > 1).

        setup:

        when:

        then:


    }

}
