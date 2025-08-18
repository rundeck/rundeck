/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package com.rundeck.repository.client.signing

import org.bouncycastle.jce.provider.BouncyCastleProvider
import spock.lang.Specification

import java.security.Security


class GpgToolsTest extends Specification {
    def setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    def "SignDetached"() {
        when:
        File outfile = new File("/tmp/bc-test-${new Random().nextInt()}.sig")
        !outfile.exists()
        GpgTools.signDetached(true,res("gpg/priv.key"),res("gpg/gpgtest.txt"),outfile.newOutputStream(),new StringGpgPassphraseProvider("DevPlugin3\$"))

        then:
        outfile.exists()
        outfile.size() > 0

        cleanup:
        outfile.delete()

    }

    def "ValidateSignature"() {
        expect:
        GpgTools.validateSignature(res("gpg/gpgtest.txt"),res("gpg/pubkey.key"),res("gpg/gnugpg-gpgtest.txt.sig"))
    }

    private InputStream res(String name) {
        getClass().getClassLoader().getResourceAsStream(name)
    }
}
