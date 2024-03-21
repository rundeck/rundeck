package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobOption
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage

import java.text.SimpleDateFormat

@SeleniumCoreTest
class DuplicateOptionsSpec extends SeleniumBase {

    final static String PROJECT_NAME = 'DuplicateOptionsSpec'

    def setupSpec(){
        setupProject(PROJECT_NAME)
    }

    def setup(){
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    def "duplicate option and check both are present in job show page as inputs with the corresponding option type"(){
        given:
        JobCreatePage jobCreatePage = go(JobCreatePage, PROJECT_NAME)
                .withName('job-duplicate-default-option')
                .addOption(new JobOption(name: optionName, optNumber: 0, optionType: optionType))
                .addSimpleCommandStep('echo hello ', 0)

        when:
        JobShowPage jobShowPage = jobCreatePage.duplicateOption(optionName, 1).saveJob()

        then:
        jobShowPage.getOptionsFields().collect { [it.getAttribute('name'), it.getAttribute('type')] } == [
                [String.valueOf("extra.option.${optionName}"), optionType.toLowerCase()],
                [String.valueOf("extra.option.${optionName}_1"), optionType.toLowerCase()]
        ]

        where:
        optionName | optionType
        'textOpt'  | 'Text'
        'fileOpt'  | 'File'
    }

    def "duplicate date option and check both are present in job show page as text inputs with date format and calendar selector"(){
        given:
        String optionName = 'textOpt'
        JobCreatePage jobCreatePage = go(JobCreatePage, PROJECT_NAME)
                .withName('job-duplicate-default-option')
                .addOption(new JobOption(name: optionName, optNumber: 0, inputType: 'date'))
                .addSimpleCommandStep('echo hello ', 0)

        when:
        JobShowPage jobShowPage = jobCreatePage.duplicateOption(optionName, 1).saveJob()

        then:
        List<WebElement> optionInputs = jobShowPage.getOptionsFields()
        optionInputs.collect { [it.getAttribute('name'), it.getAttribute('value')] } == [
                [String.valueOf("extra.option.${optionName}"), ''],
                [String.valueOf("extra.option.${optionName}_1"), '']
        ]

        when:
        List<String> optionsValues = []
        optionInputs.each {optionsValues << clickCalendarButtonTwice(jobShowPage, it) }

        then:
        SimpleDateFormat expectedDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")

        optionsValues.size() == 2
        expectedDateFormat.parse(optionsValues[0])
        expectedDateFormat.parse(optionsValues[1])
    }

    /**
     * This will click the calendar button one time to show calendar widget and fill option value
     * and a second time to close the widget
     * @param jobShowPage
     * @param optionInputField
     * @return the inserted date
     */
    String clickCalendarButtonTwice(JobShowPage jobShowPage, WebElement optionInputField){
        WebElement optionParent = optionInputField.findElement(By.xpath("./.."))
        WebElement calendarButton = optionParent.findElement(By.cssSelector(".glyphicon.glyphicon-calendar"))
        calendarButton.click()

        jobShowPage.waitForElementVisible(By.cssSelector(".bootstrap-datetimepicker-widget.dropdown-menu.timepicker-sbs.bottom"))
        calendarButton.click()

        return optionInputField.getAttribute("value")
    }
}
