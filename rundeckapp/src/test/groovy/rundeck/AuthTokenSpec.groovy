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

import org.rundeck.app.data.model.v1.AuthTokenMode

import grails.testing.gorm.DataTest
import org.apache.commons.codec.digest.DigestUtils
import org.rundeck.app.data.model.v1.AuthenticationToken
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
        def result = AuthenticationToken.parseAuthRoles(input)

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
        def result = AuthenticationToken.generateAuthRoles(input)

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
            tokenMode: mode
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
            type: AuthenticationToken.AuthTokenType.USER,
            tokenMode: AuthTokenMode.SECURED
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

}
