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
package com.dtolabs.rundeck.jetty.jaas

import org.eclipse.jetty.jaas.JAASRole
import spock.lang.Specification

import javax.naming.NamingEnumeration
import javax.naming.directory.Attribute
import javax.naming.directory.Attributes
import javax.naming.directory.DirContext
import javax.naming.directory.SearchResult
import javax.security.auth.Subject
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.login.LoginException
import java.security.Principal


class JettyCachingLdapLoginModuleTest extends Specification {
    def "DecodeBase64EncodedPwd"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        module.decodeBase64EncodedPwd("noencoding") == "noencoding"
        module.decodeBase64EncodedPwd("MD5:tmXytOxIA6rGWhEKPFfv3A==") == "MD5:b665f2b4ec4803aac65a110a3c57efdc"
    }

    def "IsBase64"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        !module.isBase64("notbase64")
        !module.isBase64("noencoding")
        module.isBase64("bXl0ZXN0c3RyaW5n")
        module.isBase64("bXl0ZXN0c3RyaW5nCg==")
    }

    def "bindingLogin user not found"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        }
        def dirContext = Mock(DirContext) {
            1 * search(
                _,
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._userObjectClass, module._userIdAttribute, username],
                _
            ) >>
            Mock(NamingEnumeration) {
                _ * hasMoreElements() >> false
            }
            0 * _(*_)
        }
        module._rootContext = dirContext
        Subject testSubject = new Subject()
        when:
        boolean result = module.login()

        then:
        LoginException thrown = thrown()
        thrown.message == 'User not found.'


        where:
        username | _
        'auser'  | _
    }

    def "bindingLogin should set user roles"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = false
        module._roleBaseDn = 'roleBaseDn'
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        }
        def found = [Mock(SearchResult) {
            getNameInNamespace() >> "cn=$username,dc=test,dc=com"
        }]
        def dirContext = Mock(DirContext) {
            1 * search(
                _,
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._userObjectClass, module._userIdAttribute, username], _
            ) >>
            Mock(NamingEnumeration) {
                hasMoreElements() >> {
                    found.size() > 0
                }
                nextElement() >> {
                    found.remove(0)
                }
            }
            0 * search(*_)
        }
        module._rootContext = dirContext
        def stringRoles = ['role1', 'role2']
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> Mock(NamingEnumeration) {
                        hasMore() >> {
                            stringRoles.size() > 0
                        }
                        next() >> {
                            stringRoles.remove(0)
                        }
                    }
                }
            }
        }]
        DirContext userDir = Mock(DirContext) {
            1 * search(
                'roleBaseDn',
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._roleObjectClass, 'roleUsernameMemberAttribute', username],
                _
            ) >> Mock(NamingEnumeration) {
                hasMoreElements() >> {
                    foundRoles.size() > 0
                }
                hasMore() >> {
                    foundRoles.size() > 0
                }
                nextElement() >> {
                    foundRoles.remove(0)
                }
            }
            0 * _(*_)
        }
        module.userBindDirContextCreator = { String user, Object pass ->
            userDir
        }
        Subject testSubject = new Subject()
        when:
        boolean result = module.login()
        module.currentUser.setJAASInfo(testSubject)

        then:
        result
        null != testSubject.getPrincipals(Principal)
        username == testSubject.getPrincipals(Principal).first().name
        null != testSubject.getPrincipals(JAASRole)
        2 == testSubject.getPrincipals(JAASRole).size()
        ['role1', 'role2'] == testSubject.getPrincipals(JAASRole)*.name


        where:
        username | _
        'auser'  | _
    }
}
