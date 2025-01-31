package com.dtolabs.rundeck.core.execution.impl.common

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path


class DefaultFileCopierSpec extends Specification {
    def "writeScriptTempFile inputs"() {
        given:
            Path tempdir = Files.createTempDirectory('test-default-file-copier-spec')

            def fileCopier = new DefaultFileCopierUtil()
            def context = Mock(ExecutionContext) {
                _ * getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    _ * createFrameworkNode() >> new NodeEntryImpl()
                    _ * getPropertyLookup() >> Mock(IPropertyLookup) {
                        _ * getProperty('framework.tmp.dir') >> tempdir.toString()
                    }
                }
            }
            File original = null
            if(infile){
                original = new File(tempdir.toFile(), 'original')
                original.text = infile
                original.deleteOnExit()
            }
            InputStream inputstream = stream?new ByteArrayInputStream(stream.getBytes()):null
            def node = Mock(INodeEntry)
            boolean expand = false
            FileCopierUtil.ContentModifier modifier = null
        when:
            def result = fileCopier.writeScriptTempFile(
                context,
                original,
                inputstream,
                script,
                node,
                expand,
                modifier
            )
        then:
            result.text == expect

            result.deleteOnExit()
        cleanup:
            result.delete()
        where:
            script   | stream   | infile   | expect
            'asdf'   | null     | null     | 'asdf\n'
            'asdf\n' | null     | null     | 'asdf\n'
            null     | 'asdf'   | null     | 'asdf\n'
            null     | 'asdf\n' | null     | 'asdf\n'
            null     | null     | 'asdf'   | 'asdf\n'
            null     | null     | 'asdf\n' | 'asdf\n'
    }

    def "writeScriptTempFile with modifier"() {
        given:
            Path tempdir = Files.createTempDirectory('test-default-file-copier-spec')

            def fileCopier = new DefaultFileCopierUtil()
            def context = Mock(ExecutionContext) {
                _ * getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    _ * createFrameworkNode() >> new NodeEntryImpl()
                    _ * getPropertyLookup() >> Mock(IPropertyLookup) {
                        _ * getProperty('framework.tmp.dir') >> tempdir.toString()
                    }
                }
            }
            File original = null
            if(infile){
                original = new File(tempdir.toFile(), 'original')
                original.text = infile
                original.deleteOnExit()
            }
            InputStream inputstream = stream?new ByteArrayInputStream(stream.getBytes()):null
            def node = Mock(INodeEntry)
            boolean expand = false
            FileCopierUtil.ContentModifier modifier = (String input, FileCopierUtil.ContentModifier.Sink sink) -> {
                sink.writeLine('test')
                sink.writeLine(input)
                return false
            } as FileCopierUtil.ContentModifier
        when:
            def result = fileCopier.writeScriptTempFile(
                context,
                original,
                inputstream,
                script,
                node,
                expand,
                modifier
            )
        then:
            result.text == expect

            result.deleteOnExit()
        cleanup:
            result.delete()
            original?.delete()
        where:
            script   | stream   | infile   | expect
            'asdf'   | null     | null     | 'test\nasdf\n'
            'asdf\n' | null     | null     | 'test\nasdf\n'
            null     | 'asdf'   | null     | 'test\nasdf\n'
            null     | 'asdf\n' | null     | 'test\nasdf\n'
            null     | null     | 'asdf'   | 'test\nasdf\n'
            null     | null     | 'asdf\n' | 'test\nasdf\n'
    }

}