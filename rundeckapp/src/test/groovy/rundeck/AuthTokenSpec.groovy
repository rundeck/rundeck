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

package rundeck

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenMode
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import grails.testing.gorm.DataTest
import grails.testing.gorm.DomainUnitTest
import org.apache.commons.codec.digest.DigestUtils
import spock.lang.Specification

/**
 * @author greg
 * @since 3/16/17
 */
class AuthTokenSpec extends Specification implements DataTest {

    void setupSpec() {
        mockDomain User
        mockDomain AuthToken
    }

    def "parseAuthRoles"() {

        when:
        def result = AuthToken.parseAuthRoles(input)

        then:
        result == expected as Set

        where:
        input          | expected
        'a'            | ['a']
        'a,b'          | ['a', 'b']
        'a,b'          | ['b', 'a']
        'a  ,  b'      | ['a', 'b']
        '   a  ,  b  ' | ['a', 'b']
        null           | []
        ''             | []
        '  '           | []
    }

    def "generate"() {
        when:
        def result = AuthToken.generateAuthRoles(input)

        then:
        result == expected

        where:
        input                | expected
        ['a', 'b']           | 'a,b'
        ['asdf']             | 'asdf'
        ['', 'b']            | 'b'
        ['   ', 'b', '    '] | 'b'
    }

    /*
    Token hashing constants
     */
    static final String TOKEN_CLEAR_VALUE = "vyRi971dXButrpTWrMJ1EWyFKpnxzXlX"
    //static final String TOKEN_SHA256 = "2c6f4d9e958ae17d74b3e9578c9e4425134bf61641912904162b76ef1cc6e63e"
    static final String TOKEN_SHA256 = DigestUtils.sha256Hex(TOKEN_CLEAR_VALUE);

    // Token encoding test
    def "token encoding mode"() {
        given:
        final String clearTokenValue = TOKEN_CLEAR_VALUE

        when:
        def result = AuthToken.encodeTokenValue(clearTokenValue, mode)

        then:
        result == expected

        where:
        mode                   | expected
        null                   | TOKEN_CLEAR_VALUE
        AuthTokenMode.LEGACY | TOKEN_CLEAR_VALUE
        AuthTokenMode.SECURED | TOKEN_SHA256
    }

    // token gets hashed on save
    def "newly created token gets hashed on save by default"() {
        given:
        def user = new User(login: "admin").save()
        def newToken = new AuthToken(
                token: TOKEN_CLEAR_VALUE,
                user: user,
                authRoles: "admin"
        )
        def resultFromMem = newToken.save(flush: true, failOnError: true)

        when:
        def resultFromDb = AuthToken.findById(resultFromMem.id)

        then:
        resultFromMem.token == TOKEN_SHA256
        resultFromMem.clearToken == TOKEN_CLEAR_VALUE
        resultFromDb.token == TOKEN_SHA256
//        resultFromDb.clearToken == null


    }

    def "token save by mode"() {
        given:
        def user = new User(login: "admin").save()
        def newToken = new AuthToken(
                token: clear,
                user: user,
                authRoles: "admin",
                mode: mode
        );
        def resultOnMem = newToken.save(flush: true, failOnError: true)

        when:
        def resultFromDb = AuthToken.findById(resultOnMem.id)

        then:
        resultOnMem.token == encoded
        resultOnMem.clearToken == clear
        resultFromDb.token == encoded
//        resultFromDb.clearToken == null

        where:
        mode                  | clear             | encoded
        AuthTokenMode.SECURED | TOKEN_CLEAR_VALUE | TOKEN_SHA256
        AuthTokenMode.LEGACY  | TOKEN_CLEAR_VALUE | TOKEN_CLEAR_VALUE
        null                  | TOKEN_CLEAR_VALUE | TOKEN_CLEAR_VALUE
    }


    def "token dont get rehashed on update"() {
        given:
        def user = new User(login: "admin").save()
        def newToken = new AuthToken(
                user: user,
                authRoles: "admin",
                token: TOKEN_CLEAR_VALUE,
                type: AuthTokenType.USER,
                mode: AuthTokenMode.SECURED
        )
        newToken.save(flush: true, failOnError: true)
        def tokFromDb = AuthToken.findById(newToken.id)
        tokFromDb.setAuthRoles("admin,user")
        tokFromDb.save(flush: true, failOnError: true)

        when:
        def result = AuthToken.findById(newToken.id)

        then:
        newToken.token == TOKEN_SHA256
        newToken.clearToken == TOKEN_CLEAR_VALUE
        tokFromDb.token == TOKEN_SHA256
//        tokFromDb.clearToken == null
        result.token == TOKEN_SHA256
//        result.clearToken == null
        result.authRoles == "admin,user"
    }

    def "token lookup test"() {
        given:
        def user = new User(login: "admin")
        def tokenSec1   = "3y28pqenyp9p0834urn3p094urn30sdknf23onj8"
        def tokenSec2   = "a8oiy73xo7ybcf3l8cfnw3n58cyw4yn5cgf9w4y5"
        def tokenUnsec1 = "asdfdsfo87sdfn79ne734pw84ujrc4up9h3y93ho"
        def tokenUnsec2 = "d8up8y43p93ny2rp9dw8nyu3pt98mw4uyt89wn58"

        def newToken1 = new AuthToken(
                user: user,
                uuid: "sec1",
                authRoles: "admin",
                token: tokenSec1,
                mode: AuthTokenMode.SECURED
        )
        def newToken2 = new AuthToken(
                user: user,
                uuid: "sec2",
                authRoles: "admin",
                token: tokenSec2,
                mode: AuthTokenMode.SECURED
        )
        def newToken3 = new AuthToken(
                user: user,
                uuid: "unsec1",
                authRoles: "admin",
                token: tokenUnsec1,
                mode: AuthTokenMode.LEGACY
        )
        def newToken4 = new AuthToken(
                user: user,
                uuid: "unsec2",
                authRoles: "admin",
                token: tokenUnsec2,
                mode: AuthTokenMode.LEGACY
        )
        newToken1.save(flush: true, failOnError: true)
        newToken2.save(flush: true, failOnError: true)
        newToken3.save(flush: true, failOnError: true)
        newToken4.save(flush: true, failOnError: true)

        when:
        def st1 = AuthToken.tokenLookup(tokenSec1);
        def st2 = AuthToken.tokenLookup(tokenSec2);
        def ut1 = AuthToken.tokenLookup(tokenUnsec1);
        def ut2 = AuthToken.tokenLookup(tokenUnsec2);

        def hashLookup1 = AuthToken.tokenLookup(tokenSec1.encodeAsSHA256())
        def hashLookup2 = AuthToken.tokenLookup(tokenSec2.encodeAsSHA256())
        def clearLookup1 = AuthToken.findByToken(tokenSec1)
        def clearLookup2 = AuthToken.findByToken(tokenSec2)
        def clearLookup3 = AuthToken.findByToken(tokenUnsec1)
        def clearLookup4 = AuthToken.findByToken(tokenUnsec2)

        then:
        st1.uuid == "sec1"
        st2.uuid == "sec2"
        st1.token == tokenSec1.encodeAsSHA256()
        st2.token == tokenSec2.encodeAsSHA256()
        ut1.uuid == "unsec1"
        ut2.uuid == "unsec2"
        ut1.token == tokenUnsec1
        ut2.token == tokenUnsec2
        hashLookup1 == null
        hashLookup2 == null
        clearLookup1 == null
        clearLookup2 == null
        clearLookup3.uuid == "unsec1"
        clearLookup4.uuid == "unsec2"

    }


}
