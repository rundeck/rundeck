package com.dtolabs.rundeck.core.cli

import com.dtolabs.rundeck.core.utils.Converter
import spock.lang.Specification

class CLIUtilsSpec extends Specification {

    def "testGenerateArglineUnsafe"() {
        expect:
        CLIUtils.generateArgline("test", ["1", "2"] as String[], true) == "test 1 2"
        CLIUtils.generateArgline("test", ["1", "2", "3 4"] as String[], true) == "test 1 2 '3 4'"
        CLIUtils.generateArgline("test", ["1", "2", "\"3 4\""] as String[], true) == "test 1 2 '\"3 4\"'"
        CLIUtils.generateArgline("test", ["1", "2", "\"34\""] as String[], true) == "test 1 2 \"34\""
        CLIUtils.generateArgline("test", ["1", "2", "'3 4'"] as String[], true) == "test 1 2 '3 4'"
        CLIUtils.generateArgline("test", null, true) == "test"
        CLIUtils.generateArgline("test", [] as String[], true) == "test"
        CLIUtils.generateArgline("test", ["rm", "*", "&&", "do\tthings\t>/etc/passwd"] as String[], true) == "test rm * && do\tthings\t>/etc/passwd"
    }

    def "testGenerateArglineSafe"() {
        expect:
        CLIUtils.generateArgline("test", ["1", "2"] as String[], false) == "test 1 2"
        CLIUtils.generateArgline("test", ["1", "2", "3 4"] as String[], false) == "test 1 2 '3 4'"
        CLIUtils.generateArgline("test", ["1", "2", "\"3 4\""] as String[], false) == "test 1 2 '\"3 4\"'"
        CLIUtils.generateArgline("test", ["1", "2", "\"34\""] as String[], false) == "test 1 2 '\"34\"'"
        CLIUtils.generateArgline("test", ["1", "2", "'3 4'"] as String[], false) == "test 1 2 ''\"'\"'3 4'\"'\"''"
        CLIUtils.generateArgline("test", null, false) == "test"
        CLIUtils.generateArgline("test", [] as String[], false) == "test"
        CLIUtils.generateArgline("test", ["rm", "*", "&&", "do\tthings\t>/etc/passwd"] as String[], false) == "test rm '*' '&&' 'do\tthings\t>/etc/passwd'"
    }

    def "testContainsWhitespace"() {
        expect:
        !CLIUtils.containsSpace("")
        !CLIUtils.containsSpace("asdf1234")
        CLIUtils.containsSpace("asdf123 4")
        CLIUtils.containsSpace("   ")
        !CLIUtils.containsSpace("asdf123\t4")
        !CLIUtils.containsSpace("asdf123\n4")
        !CLIUtils.containsSpace("asdf123\r4")
    }

    def "testArgumentQuoteForOperatingSystem"() {
        when: "Unix case"
        Converter<String, String> unixConverter = CLIUtils.argumentQuoteForOperatingSystem("unix", null)
        then:
        unixConverter.convert("foo bar") == "'foo bar'"
        unixConverter.convert("foo&bar") == "'foo&bar'"
        unixConverter.convert("`foobar`") == "'`foobar`'"

        when: "Windows CMD case"
        Converter<String, String> windowsCmdConverter = CLIUtils.argumentQuoteForOperatingSystem("windows", "cmd")
        then:
        windowsCmdConverter.convert("&foo|bar") == "^&foo^|bar"
        windowsCmdConverter.convert("foo&bar") == "foo^&bar"
        windowsCmdConverter.convert("`foobar`") == "^`foobar^`"

        when: "Windows PowerShell case"
        Converter<String, String> windowsPsConverter = CLIUtils.argumentQuoteForOperatingSystem("windows", "powershell")
        then:
        windowsPsConverter.convert("foo bar") == "'foo bar'"
        windowsPsConverter.convert("foo&bar") == "'foo&bar'"
        windowsPsConverter.convert("`foobar`") == "'`foobar`'"
    }
}