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

package rundeck

import com.dtolabs.rundeck.app.support.DomainIndexHelper
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenMode
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import com.dtolabs.rundeck.core.authentication.tokens.AuthenticationToken

import java.time.Clock

class AuthToken implements AuthenticationToken {
    String token
    String authRoles
    String uuid
    String creator
    Date expiration
    Date dateCreated
    Date lastUpdated
    String name
    AuthTokenType type = AuthTokenType.USER
    AuthTokenMode mode = AuthTokenMode.SECURED
    private transient String clearToken = null

    static belongsTo = [user:User]
    static transients = ['printableToken','ownerName', 'clearToken']
    static constraints = {
        token(nullable:false,unique:true)
        authRoles(nullable:false)
        uuid(nullable: true)
        user(nullable:false)
        creator(nullable: true)
        expiration(nullable: true)
        lastUpdated(nullable: true)
        dateCreated(nullable: true)
        type(nullable: true)
        name(nullable: true)
        mode(nullable: true)
    }
    static mapping = {
        authRoles type: 'text'
        'type' defaultValue: "'USER'"

        DomainIndexHelper.generate(delegate) {
            index 'IDX_TOKEN', ['token']
            index 'IDX_TYPE', ['type']
        }
    }

    def beforeInsert() {
        encodeToken()
    }


    /**
     * Encodes the token value according to the mode set.
     */
    private void encodeToken() {
        this.clearToken = token
        this.mode = this.mode ?: AuthTokenMode.LEGACY
        this.token = encodeTokenValue(this.clearToken, this.mode)
    }

    /**
     * Encodes a clear token value acording to the mode supplied.
     */
    public static String encodeTokenValue(String clearValue, AuthTokenMode mode){
        if(!clearValue)
            throw new IllegalArgumentException("Illegal token value supplied: " + clearValue)

        switch (mode) {
            case AuthTokenMode.SECURED:
                return clearValue.encodeAsSHA256()

            case AuthTokenMode.LEGACY:
                return clearValue

            default:
                return clearValue
        }
    }

    /**
     * Finds a user token from the provided value.
     */
    public static AuthToken tokenLookup(String tokenValue) {
        def tokenHash = encodeTokenValue(tokenValue, AuthTokenMode.SECURED)
        return createCriteria().get {
            or {
                and {
                    eq("mode", AuthTokenMode.SECURED)
                    eq("token", tokenHash)
                }
                and {
                    or {
                        isNull("mode")
                        eq("mode", AuthTokenMode.LEGACY)
                    }
                    eq("token", tokenValue)
                }
            }
            or {
                isNull("type")
                eq("type", AuthTokenType.USER)
            }
        }
    }

    /**
     * Finds a token from the provided value and type
     */
    public static AuthToken tokenLookup(String tokenValue, AuthTokenType tokenType) {
        def tokenHash = encodeTokenValue(tokenValue, AuthTokenMode.SECURED)
        return createCriteria().get {
            eq("type", tokenType)
            or {
                and {
                    eq("mode", AuthTokenMode.SECURED)
                    eq("token", tokenHash)
                }
                and {
                    or {
                        isNull("mode")
                        eq("mode", AuthTokenMode.LEGACY)
                    }
                    eq("token", tokenValue)
                }
            }
        }
    }

    String getClearToken() {
        return clearToken
    }

    @Override
    Set<String> authRolesSet() {
        return parseAuthRoles(authRoles)
    }

    static String generateAuthRoles(Collection<String> roles) {
        new HashSet(roles.collect { it.trim() }.findAll { it }).join(',')
    }

    static Set<String> parseAuthRoles(String authRoles) {
        if (!authRoles) {
            return []
        }
        new HashSet(authRoles.split(' *, *').collect { it.trim() }.findAll { it } as List)
    }

    boolean tokenIsExpired() {
        expiration!=null && (expiration < Date.from(Clock.systemUTC().instant()))
    }


    /**
     * @return Printable value for token: the uuid, or a truncated token value
     */
    String getPrintableToken() {
        uuid ? "[ID: $uuid]" : (printable(token))
    }

    /**
     * @return Printable truncated token value
     */
    static String printable(String authtoken) {
        (authtoken.size() > 5 ? authtoken.substring(0, 5) : '') + "****"
    }

    @Override
    String toString() {
        "Auth Token: ${printableToken}"
    }

    @Override
    String getOwnerName() {
        return user.login
    }

}
