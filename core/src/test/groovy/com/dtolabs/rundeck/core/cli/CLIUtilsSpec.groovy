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

        when: "Windows default case (no interpreter)"
        Converter<String, String> windowsDefaultConverter = CLIUtils.argumentQuoteForOperatingSystem("windows", null)
        then:
        windowsDefaultConverter.convert("foo bar") == '"foo bar"'
        windowsDefaultConverter.convert("foo&bar") == '"foo&bar"'
        windowsDefaultConverter.convert("`foobar`") == '"`foobar`"'
    }

    def "quoteWindowsCMDArg escapes internal double quotes"() {
        expect:
        CLIUtils.quoteWindowsCMDArg('"hello world"') == '"\\"hello world\\""'
    }

    def "quoteWindowsCMDArg escapes percent for env var protection"() {
        expect:
        CLIUtils.quoteWindowsCMDArg('%PATH%') == '"%%PATH%%"'
    }

    def "quoteWindowsCMDArg returns simple args unchanged"() {
        expect:
        CLIUtils.quoteWindowsCMDArg('simple') == 'simple'
    }

    def "quoteWindowsCMDArg handles UNC paths with spaces"() {
        expect:
        CLIUtils.quoteWindowsCMDArg('\\\\server\\share\\path with spaces\\script.ps1') == '"\\\\server\\share\\path with spaces\\script.ps1"'
    }

    def "quoteWindowsCMDArg blocks pipe injection"() {
        expect:
        CLIUtils.quoteWindowsCMDArg('80 | whoami') == '"80 | whoami"'
    }

    def "quoteWindowsCMDArg blocks redirect injection"() {
        expect:
        CLIUtils.quoteWindowsCMDArg('data > C:\\tmp\\file') == '"data > C:\\tmp\\file"'
    }

    def "quoteWindowsCMDArg blocks AND operator injection"() {
        expect:
        CLIUtils.quoteWindowsCMDArg('test && del /q C:\\') == '"test && del /q C:\\"'
    }

    def "quoteWindowsCMDArg blocks semicolon injection"() {
        expect:
        CLIUtils.quoteWindowsCMDArg('test; whoami') == '"test; whoami"'
    }
}