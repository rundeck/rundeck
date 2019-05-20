/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.utils


import spock.lang.Specification

import java.nio.charset.Charset
import java.util.function.Consumer

/**
 * Created by greg on 2/18/16.
 */
class ThreadBoundLogOutputStreamSpec extends Specification {
    static class TestBuffer extends StringLogBuffer {
        TestBuffer(final Charset charset) {
            super(charset)
        }
    }

    static class TestManager implements LogBufferManager<String,TestBuffer> {
        List<TestBuffer> buffers = []

        @Override
        TestBuffer create(final Charset charset) {
            buffers << new TestBuffer()
            return buffers[-1]
        }

        @Override
        void flush(final Consumer<String> writer) {
            buffers.each { writer.accept(it.get()) }
            buffers = []
        }
    }
    def "write without newline"() {
        given:
            List<TestBuffer> buffs = []
            ThreadBoundLogOutputStream<String,TestBuffer> stream = new ThreadBoundLogOutputStream<String,TestBuffer>(
                    { TestBuffer buff -> buffs.push buff },
                    Charset.defaultCharset(),
                    { new TestManager() }
            )


        when:
        stream.write('abc no newline'.bytes)

        then:
            0 == buffs.size()

    }

    def "write multiple without newline"() {
        given:
            List<TestBuffer> buffs = []
            ThreadBoundLogOutputStream<String,TestBuffer> stream = new ThreadBoundLogOutputStream<String,TestBuffer>(
                    { TestBuffer buff -> buffs.push buff },
                    Charset.defaultCharset(),
                    { new TestManager() }
            )


        when:
        stream.write('abc no newline'.bytes)
        stream.write(' still not'.bytes)
        stream.write(' more not'.bytes)

        then:
            0 == buffs.size()

    }

    def "write multiple with flush without newline"() {
        given:
            List<TestBuffer> buffs = []
            ThreadBoundLogOutputStream<String,TestBuffer> stream = new ThreadBoundLogOutputStream<String,TestBuffer>(
                    { TestBuffer buff -> buffs.push buff },
                    Charset.defaultCharset(),
                    { new TestManager() }
            )


        when:
        stream.write('abc no newline'.bytes)
        stream.flush()
        stream.write(' still not'.bytes)
        stream.flush()
        stream.write(' more not'.bytes)
        stream.flush()

        then:
            0 == buffs.size()

    }

    def "write with newline"() {
        given:
            List<String> buffs = []
            ThreadBoundLogOutputStream<String,TestBuffer> stream = new ThreadBoundLogOutputStream<String,TestBuffer>(
                    { String buff -> buffs.push buff },
                    Charset.defaultCharset(),
                    { new TestManager() }
            )

        when:
        stream.write('abc yes newline\n'.bytes)

        then:
            1 == buffs.size()
    }

    def "write multi then newline"() {
        given:
            List<String> buffs = []
            ThreadBoundLogOutputStream<String,TestBuffer> stream = new ThreadBoundLogOutputStream<String,TestBuffer>(
                    { String buff -> buffs.push buff },
                    Charset.defaultCharset(),
                    { new TestManager() }
            )

        when:
        stream.write('no newline'.bytes)
        stream.write(' still not'.bytes)
        stream.write(' more not'.bytes)
        stream.write(' then\n'.bytes)

        then:
            1 == buffs.size()
    }

    def "write without newline then close"() {
        given:
            List<String> buffs = []
            ThreadBoundLogOutputStream<String,TestBuffer> stream = new ThreadBoundLogOutputStream<String,TestBuffer>(
                    { String buff -> buffs.push buff },
                    Charset.defaultCharset(),
                    { new TestManager() }
            )

        when:
        stream.write('no newline'.bytes)
        stream.close()

        then:
            1 == buffs.size()
    }

    def "write multi without newline then close"() {
        given:
            List<String> buffs = []
            ThreadBoundLogOutputStream<String,TestBuffer> stream = new ThreadBoundLogOutputStream<String,TestBuffer>(
                    { String buff -> buffs.push buff },
                    Charset.defaultCharset(),
                    { new TestManager() }
            )

        when:
        stream.write('no newline'.bytes)
        stream.write(' still not'.bytes)
        stream.write(' more not'.bytes)
        stream.close()

        then:
            1 == buffs.size()
    }

    def "write multi without newline with cr"() {
        given:
            List<String> buffs = []
            ThreadBoundLogOutputStream<String, TestBuffer> stream = new ThreadBoundLogOutputStream<String, TestBuffer>(
                    { String buff -> buffs.push buff },
                    Charset.defaultCharset(),
                    { new TestManager() }
            )

        when:
            stream.write('no newline'.bytes)
            stream.write(' with cr\rmonkey'.bytes)

        then:
            1 == buffs.size()
    }

}
