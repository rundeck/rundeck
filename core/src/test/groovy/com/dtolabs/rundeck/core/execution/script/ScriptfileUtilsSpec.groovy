package com.dtolabs.rundeck.core.execution.script

import spock.lang.Specification

import static com.dtolabs.rundeck.core.execution.script.ScriptfileUtils.LineEndingStyle.UNIX
import static com.dtolabs.rundeck.core.execution.script.ScriptfileUtils.LineEndingStyle.WINDOWS

class ScriptfileUtilsSpec extends Specification {
    def "writeScriptFile inputs"() {
        given:
            def file = File.createTempFile("test", ".sh")
            file.deleteOnExit()
            def modifier = null
        when:
            ScriptfileUtils.writeScriptFile(
                input ? new ByteArrayInputStream(input.getBytes()) : null,
                script,
                read ? new StringReader(read) : null,
                UNIX,
                file,
                false,
                modifier
            )

        then:
            file.text == expected
        where:
            script         | input          | read           | expected
            'echo "hello"' | null           | null           | 'echo "hello"\n'
            null           | 'echo "hello"' | null           | 'echo "hello"\n'
            null           | null           | 'echo "hello"' | 'echo "hello"\n'
    }
    def "writeScriptFile windows line ending"() {
        given:
            def file = File.createTempFile("test", ".sh")
            file.deleteOnExit()
            def modifier = null
        when:
            ScriptfileUtils.writeScriptFile(
                input ? new ByteArrayInputStream(input.getBytes()) : null,
                script,
                read ? new StringReader(read) : null,
                WINDOWS,
                file,
                false,
                modifier
            )

        then:
            file.text == expected
        where:
            script         | input          | read           | expected
            'echo "hello"' | null           | null           | 'echo "hello"\r\n'
            null           | 'echo "hello"' | null           | 'echo "hello"\r\n'
            null           | null           | 'echo "hello"' | 'echo "hello"\r\n'
    }

    def "writeScriptFile with modifier"() {
        given:
            def file = File.createTempFile("test", ".sh")
            file.deleteOnExit()
            def modifier = { line, sink ->
                sink.writeLine('before')
                sink.writeLine(line)
                sink.writeLine('after')
                false
            }
        when:
            ScriptfileUtils.writeScriptFile(
                input ? new ByteArrayInputStream(input.getBytes()) : null,
                script,
                read ? new StringReader(read) : null,
                UNIX,
                file,
                false,
                modifier
            )

        then:
            file.text == expected
        where:
            script         | input          | read           | expected
            'echo "hello"' | null           | null           | 'before\necho "hello"\nafter\n'
            null           | 'echo "hello"' | null           | 'before\necho "hello"\nafter\n'
            null           | null           | 'echo "hello"' | 'before\necho "hello"\nafter\n'
    }
}
