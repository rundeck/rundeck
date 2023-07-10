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
import org.eclipse.jetty.jaas.spi.UserInfo
import rundeck.services.ConfigurationService
import spock.lang.Specification
import spock.lang.Unroll

import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.Attribute
import javax.naming.directory.Attributes
import javax.naming.directory.BasicAttribute
import javax.naming.directory.BasicAttributes
import javax.naming.directory.DirContext
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult
import javax.naming.ldap.Control
import javax.naming.ldap.LdapContext
import javax.security.auth.Subject
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.login.FailedLoginException
import javax.security.auth.login.LoginException
import java.security.Principal


class JettyCachingLdapLoginModuleTest extends Specification {

    private final String user1 = 'user1'
    private final String user2 = 'user2'
    private final String password = 'password'
    private final String role1 = 'role1'
    private final String role2 = 'role2'
    private final String nestedRole1 = 'nestedRole1'

    def "DecodeBase64EncodedPwd"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        module.decodeBase64EncodedPwd("noencoding") == "noencoding"
        module.decodeBase64EncodedPwd("MD5:tmXytOxIA6rGWhEKPFfv3A==") == "MD5:b665f2b4ec4803aac65a110a3c57efdc"
        module.decodeBase64EncodedPwd("MD5:038703c7230ae012e3c783ace1d09d64") == "MD5:038703c7230ae012e3c783ace1d09d64"
    }

    def "IsBase64"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        !module.isBase64("notbase64")
        !module.isBase64("noencoding")
        module.isBase64("bXl0ZXN0c3RyaW5n")
        module.isBase64("bXl0ZXN0c3RyaW5nCg==")
    }

    def "IsHex"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        module.isHex("b665f2b4ec4803aac65a110a3c57efdc")
        !module.isHex("b665f2b4ec4803aac65a110a3c57efd")
        !module.isHex("dWJlcjE3NjAzdGFzdGljc3dlZXRuZXNz")
        !module.isHex("dWJlcjE5MjJ0YXN0aWNzd2VldG5lc3M=")
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

    def "bindingLogin invalid password"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = useRootContext
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
            ) >>{new EnumImpl<SearchResult>(found)}
            0 * search(*_)
        }
        module._rootContext = dirContext

        module.userBindDirContextCreator = { String user, Object pass ->
            throw new NamingException("Login failure")
        }
        when:
        boolean result = module.login()

        then:
        !result

        LoginException thrown = thrown()
        thrown.message == 'Error obtaining user info.'


        where:
        username | useRootContext
        'auser'  | true
        'auser'  | false
    }

    def "bindingLogin should set user roles without pagination"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = false
        module._roleBaseDn = 'roleBaseDn'
        module.rolePagination = false
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        }
        def found = [Mock(SearchResult) {
            getNameInNamespace() >> "cn=$username,dc=test,dc=com"
            getAttributes() >> new BasicAttributes()
        }]
        def dirContext = Mock(DirContext) {
            1 * search(
                _,
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._userObjectClass, module._userIdAttribute, username], _
            ) >> {new EnumImpl<SearchResult>(found)}
            0 * search(*_)
        }
        module._rootContext = dirContext
        def stringRoles = ['role1', 'role2']
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> {new EnumImpl<String>(stringRoles)}
                }
            }
        }]
        DirContext userDir = Mock(DirContext) {
            1 * search(
                'roleBaseDn',
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._roleObjectClass, 'roleUsernameMemberAttribute', username],
                _
            ) >> {new EnumImpl<SearchResult>(foundRoles)}
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
    class EnumImpl<T> implements NamingEnumeration<T>{
        List<T> list

        EnumImpl(final List<T> list) {
            this.list = new ArrayList<>(list)
        }

        @Override
        T next() throws NamingException {
            return list.remove(0)
        }

        @Override
        boolean hasMore() throws NamingException {
            return list.size()> 0
        }

        @Override
        void close() throws NamingException {

        }

        @Override
        boolean hasMoreElements() {
            hasMore()
        }

        @Override
        T nextElement() {
            next()
        }
    }

    def "bindingLogin while cached without pagination"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._cacheDuration = Integer.MAX_VALUE
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = false
        module._roleBaseDn = 'roleBaseDn'
        module.rolePagination = false
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.callbackHandler = Mock(CallbackHandler) {
            2 * handle(_) >> { it[0][0].name = username; it[0][1].object = passwordvalue }
        }
        def found = [Mock(SearchResult) {
            getNameInNamespace() >> "cn=$username,dc=test,dc=com"
            getAttributes() >> new BasicAttributes()
        }]
        def dirContext = Mock(DirContext) {
            1 * search(
                _,
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._userObjectClass, module._userIdAttribute, username], _
            ) >> {new EnumImpl<SearchResult>(found)}

            0 * search(*_)
        }
        module._rootContext = dirContext
        def stringRoles = ['role1', 'role2']
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> {new EnumImpl<String>(stringRoles)}
                }
            }
        }]
        DirContext userDir = Mock(DirContext) {
            1 * search(
                'roleBaseDn',
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._roleObjectClass, 'roleUsernameMemberAttribute', username],
                _
            ) >> {new EnumImpl<SearchResult>(foundRoles)}

            0 * _(*_)
        }
        module.userBindDirContextCreator = { String user, Object pass ->
            userDir
        }
        Subject testSubject = new Subject()
        when:
        boolean result = module.login()
        boolean result2 = module.login()
        module.currentUser.setJAASInfo(testSubject)

        then:
        result
        result2
        null != testSubject.getPrincipals(Principal)
        username == testSubject.getPrincipals(Principal).first().name
        null != testSubject.getPrincipals(JAASRole)
        2 == testSubject.getPrincipals(JAASRole).size()
        ['role1', 'role2'] == testSubject.getPrincipals(JAASRole)*.name


        where:
        username | passwordvalue
        'auser'  | 'apassword'
    }

    @Unroll
    def "get ldapBind pwd from configuration service"() {
        setup:
        ConfigurationService cfgSvc = null
        if(hasCfgService) {
            cfgSvc = Mock(ConfigurationService) {
                getString("security.ldap.bindPassword") >> ldapBindPwd
            }
        }

        when:
        TestJettyCachingLdapLoginModule ldapMod = new TestJettyCachingLdapLoginModule(cfgSvc)
        String actual = ldapMod.attemptBindPwdFromRdkConfig()

        then:
        actual == expected

        where:
        hasCfgService | ldapBindPwd   | expected
        false         | "123"         | null
        false         | "123"         | null
        true          | "123"         | "123"
        true          | null          | null
    }

    def "bindingLogin should set user roles paged"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = false
        module._roleBaseDn = 'roleBaseDn'
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.rolePagination = true
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        }
        def found = [Mock(SearchResult) {
            getNameInNamespace() >> "cn=$username,dc=test,dc=com"
            getAttributes() >> new BasicAttributes()
        }]
        def dirContext = Mock(DirContext) {
            1 * search(
                    _,
                    JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                    [module._userObjectClass, module._userIdAttribute, username], _
            ) >> {new EnumImpl<SearchResult>(found)}
            0 * search(*_)
        }
        module._rootContext = dirContext
        def stringRoles = ['role1', 'role2']
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> {new EnumImpl<String>(stringRoles)}
                }
            }
        }]
        def ldapContext = Mock(LdapContext){
            1 * search(
                    'roleBaseDn',
                    JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                    [module._roleObjectClass, 'roleUsernameMemberAttribute', username],
                    _
            ) >> {new EnumImpl<SearchResult>(foundRoles)}
            0 * search(*_)
        }
        module.ldapContext = ldapContext
        DirContext userDir = Mock(DirContext) {
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

    static class TestJettyCachingLdapLoginModule extends JettyCachingLdapLoginModule {

        private final ConfigurationService cfgSvc

        TestJettyCachingLdapLoginModule(ConfigurationService cfgSvc) {
            this.cfgSvc = cfgSvc
        }

        @Override
        def ConfigurationService getConfigurationService() {
            return cfgSvc
        }
    }

    def "test timeout defaults"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        module._contextFactory = 'foo'
        module._providerUrl = 'ldap://localhost'

        when:
        Hashtable env = module.environment
        then:
        assert env.containsKey('com.sun.jndi.ldap.read.timeout'): 'Expected ldap read timeout default'
        assert env['com.sun.jndi.ldap.read.timeout'] == '0': 'Expected ldap read timeout default'
        assert env.containsKey('com.sun.jndi.ldap.connect.timeout'): 'Expected ldap connect timeout default'
        assert env['com.sun.jndi.ldap.connect.timeout'] == '0': 'Expected ldap connect timeout default'
    }


    def "test timeout read initialize"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        when:
        module.initializeOptions([timeoutRead: '100'])

        then:
        assert module._timeoutRead == 100: 'Expected ldap read timeout value'
        assert module._timeoutConnect == 0: 'Expected ldap connect timeout default'
    }


    def "test timeout connect initialize"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        when:
        module.initializeOptions([timeoutConnect: '200'])

        then:
        assert module._timeoutRead == 0: 'Expected ldap read timeout default'
        assert module._timeoutConnect == 200: 'Expected ldap connect timeout value'
    }


    def "test timeout read"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        module._contextFactory = 'foo'
        module._providerUrl = 'ldap://localhost'
        module._timeoutRead = 1000

        when:
        Hashtable env = module.environment
        then:
        assert env.containsKey('com.sun.jndi.ldap.read.timeout'): 'Expected ldap read timeout default'
        assert env['com.sun.jndi.ldap.read.timeout'] == '1000': 'Expected ldap read timeout value'
        assert env.containsKey('com.sun.jndi.ldap.connect.timeout'): 'Expected ldap connect timeout default'
        assert env['com.sun.jndi.ldap.connect.timeout'] == '0': 'Expected ldap connect timeout default'
    }


    def "test timeout connect"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        module._contextFactory = 'foo'
        module._providerUrl = 'ldap://localhost'
        module._timeoutConnect = 5000

        when:
        Hashtable env = module.environment

        then:
        assert env.containsKey('com.sun.jndi.ldap.connect.timeout'): 'Expected ldap read timeout default'
        assert env['com.sun.jndi.ldap.connect.timeout'] == '5000': 'Expected ldap read timeout value'
        assert env.containsKey('com.sun.jndi.ldap.read.timeout'): 'Expected ldap connect timeout default'
        assert env['com.sun.jndi.ldap.read.timeout'] == '0': 'Expected ldap connect timeout default'
    }

    def "test get environment no ssl"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        module._contextFactory = 'foo'
        module._providerUrl = 'ldap://localhost'

        when:
        Hashtable env = module.environment
        then:
        assert !env.containsKey('java.naming.ldap.factory.socket'): 'Expected ldap socket factory to be unset'
    }


    def "test get environment ssl provider url"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        module._contextFactory = 'foo'
        String host = 'somehost'
        module._providerUrl = String.format('ldaps://%s', host)

        when:
        Hashtable env = module.environment
        then:
        assert env['java.naming.ldap.factory.socket'] == 'com.dtolabs.rundeck.jetty.jaas.HostnameVerifyingSSLSocketFactory':
                'Expected ldap socket factory to be unset'
        assert HostnameVerifyingSSLSocketFactory.targetHost == host: 'Expected target host to be localhost'
    }


    def "test should get nested groups"() {
        given:
        def module = getJettyCachingLdapLoginModule(false, true)

        when:
        UserInfo userInfo = module.getUserInfo(user1)

        then:
        userInfo.userName == user1
        userInfo.roleNames == [role1, role2, nestedRole1]
    }


    def "test should get nested groups with AD"() {
        given:
        def module = getJettyCachingLdapLoginModule(true, true)

        when:
        UserInfo userInfo = module.getUserInfo(user1)

        then:
        userInfo.userName == user1
        userInfo.roleNames == [role1, role2, nestedRole1]
    }


    def "test should get paginated groups with AD"() {
        given:
        def module = getJettyCachingLdapLoginModule(true, true)
        module.rolePagination = true

        when:
        UserInfo userInfo = module.getUserInfo(user1)

        then:
        userInfo.userName == user1
        userInfo.roleNames == [role1, role2, nestedRole1]
    }


    def "test should not get nested groups"() {
        given:
        def module = getJettyCachingLdapLoginModule(false, false)

        when:
        UserInfo userInfo = module.getUserInfo(user1)

        then:
        userInfo.userName == user1
        userInfo.roleNames == [role1, role2]
    }

    def "test disallow empty password"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        module._debug = true
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = user1; it[0][1].object = '' }
        }

        when:
        !module.login()

        then:
        thrown FailedLoginException
    }


    def "test disallow null password"() {
        given:
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = user1; it[0][1].object = null }
        }

        when:
        !module.login()

        then:
        thrown FailedLoginException
    }


    def "test disallow empty char array password"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        module._debug = true
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = user1; it[0][1].object = '' }
        }

        when:
        !module.login()

        then:
        thrown FailedLoginException
    }


    def "test disallow empty username"() {
        given:
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = ''; it[0][1].object = 'xyz' }
        }

        when:
        !module.login()

        then:
        thrown FailedLoginException
    }


    def "test disallow null username"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        module._debug = true
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = null; it[0][1].object = 'xyz' }
        }
        when:
        !module.login()

        then:
        thrown FailedLoginException
    }


    def "test get user attributes email first name last name"() {
        given:
        def module = getJettyCachingLdapLoginModule(false, false)
        module._debug = true
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = user1; it[0][1].object = password }
        }
        module.subject = new Subject()

        expect:
        module.login()
        module.commit()
        module.subject.principals.stream().anyMatch(p -> p instanceof LdapEmailPrincipal)
        module.subject.principals.stream().anyMatch(p -> p instanceof LdapFirstNamePrincipal)
        module.subject.principals.stream().anyMatch(p -> p instanceof LdapLastNamePrincipal)
    }


    def "test set options email first last"() {
        given:
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        HashMap options = new HashMap()
        options['userLastNameAttribute'] = 'lastAttrib'
        options['userFirstNameAttribute'] = 'firstAttrib'
        options['userEmailAttribute'] = 'emailAttrib'
        when:
        module.initializeOptions(options)
        then:
        module._userLastNameAttribute == 'lastAttrib'
        module._userFirstNameAttribute == 'firstAttrib'
        module._userEmailAttribute == 'emailAttrib'
    }


    private JettyCachingLdapLoginModule getJettyCachingLdapLoginModule(boolean activeDirectory, boolean nestedGroups) {
        def module = Spy(JettyCachingLdapLoginModule)
        module._userBaseDn = "ou=users,dc=example,dc=com";
        module._roleBaseDn = "ou=groups,dc=example,dc=com";
        module._providerUrl = 'ldap://localhost'
        def stringRoles = []

        if (nestedGroups) {
            stringRoles = [role1, role2, nestedRole1]
            module._nestedGroups = true
            module.rolesPerPage = 1000
        } else {
            stringRoles = [role1, role2]
            module.rolesPerPage = 1000
        }
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> { new EnumImpl<String>(stringRoles) }
                }
                get(module._userPasswordAttribute) >> Mock(Attribute) {
                    get() >> password.getBytes()
                }
                get(module._userEmailAttribute) >> new BasicAttribute(module._userEmailAttribute, "user@example.com")
                get(module._userFirstNameAttribute) >> new BasicAttribute(module._userFirstNameAttribute, "First")
                get(module._userLastNameAttribute) >> new BasicAttribute(module._userLastNameAttribute, "Last")
            }
        }]
        def nestedMemberRole = []
        if (activeDirectory) {
            nestedMemberRole = ['CN=' + user2 + ',' + module._roleBaseDn]
        } else {
            nestedMemberRole = ['cn=' + user2 + ',' + module._roleBaseDn]
        }
        def nestedRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> { new EnumImpl<String>([role1]) }
                }
                get(module._roleMemberAttribute) >> Mock(Attribute) {
                    getAll() >> { new EnumImpl<String>(nestedMemberRole) }
                }
            }
        }, Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> { new EnumImpl<String>([role2]) }
                }
                get(module._roleMemberAttribute) >> Mock(Attribute) {
                    getAll() >> { new EnumImpl<String>(nestedMemberRole) }
                }
            }
        }, Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> { new EnumImpl<String>([nestedRole1]) }
                }
                get(module._roleMemberAttribute) >> Mock(Attribute) {
                    getAll() >> { new EnumImpl<String>(nestedMemberRole) }
                }
            }
        }]

        DirContext dirContext = Mock(DirContext) {
            2 * search(
                    module._userBaseDn,
                    JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                    [module._userObjectClass, module._userIdAttribute, user1],
                    _ as SearchControls
            ) >> { new EnumImpl<SearchResult>(foundRoles) }
            lookup(module._providerUrl) >> Mock(LdapContext) {
                search(
                        module._roleBaseDn,
                        module._roleMemberFilter,
                        _ as SearchControls
                ) >> { new EnumImpl<SearchResult>(nestedRoles) }
            }
        }

        LdapContext ldapContext = Mock(LdapContext) {
            1 * search(
                    module._roleBaseDn,
                    JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                    [module._roleObjectClass, module._roleMemberAttribute, 'uid=user1,' + module._userBaseDn],
                    _ as SearchControls
            ) >> { new EnumImpl<SearchResult>(foundRoles) }
            getResponseControls() >> [Mock(Control) {
                0 * _(*_)
            }]
        }

        module._rootContext = dirContext
        module.ldapContext = ldapContext

        return module
    }
}
