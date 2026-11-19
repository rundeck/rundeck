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

import org.rundeck.jaas.PasswordCredential
import org.rundeck.jaas.RundeckPrincipal
import org.rundeck.jaas.RundeckRole
import org.rundeck.jaas.UserInfo
import rundeck.services.ConfigurationService
import spock.lang.Specification
import spock.lang.Unroll

import javax.naming.CompositeName
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
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        })  // Use setter instead of @field access (Groovy 4)
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
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        })  // Use setter instead of @field access (Groovy 4)
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

    def "constructUserDn builds dn from rdn attribute, username and base dn"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._userRdnAttribute = 'uid'
        module._userBaseDn = 'ou=people,dc=example,dc=com'
        expect:
        module.constructUserDn('auser') == 'uid=auser,ou=people,dc=example,dc=com'
    }

    def "escapeDnValue escapes special dn characters"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        module.escapeDnValue(null) == ''
        module.escapeDnValue('plain') == 'plain'
        module.escapeDnValue('a,b') == 'a\\,b'
        module.escapeDnValue('a+b') == 'a\\+b'
        module.escapeDnValue('a"b') == 'a\\"b'
        module.escapeDnValue('a<b') == 'a\\<b'
        module.escapeDnValue('a>b') == 'a\\>b'
        module.escapeDnValue('a;b') == 'a\\;b'
        module.escapeDnValue('a=b') == 'a\\=b'
        module.escapeDnValue('a\\b') == 'a\\\\b'
        module.escapeDnValue(' leading') == '\\ leading'
        module.escapeDnValue('trailing ') == 'trailing\\ '
        module.escapeDnValue('#leading') == '\\#leading'
        module.escapeDnValue('a\u0000b') == 'a\\00b'
    }

    def "escapeDnValue hex-escapes control characters to prevent log injection"() {
        // Regression test per Copilot review on PR #10530: constructUserDn()'s result is logged
        // via LOG.info("Attempting authentication: " + userDn) before the bind attempt succeeds or
        // fails, so a username containing raw CR/LF (or other ASCII control characters) previously
        // passed through escapeDnValue() unescaped, letting an unauthenticated caller inject or
        // forge log records. All ASCII control characters must now be hex-escaped per RFC 4514.
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        module.escapeDnValue("a\rb") == 'a\\0Db'
        module.escapeDnValue("a\nb") == 'a\\0Ab'
        module.escapeDnValue("a\r\nFAKE LOG LINE\r\nb") == 'a\\0D\\0AFAKE LOG LINE\\0D\\0Ab'
        module.escapeDnValue("a\tb") == 'a\\09b'
        module.escapeDnValue("a\u007Fb") == 'a\\7Fb'
    }

    def "bindingLogin with forceBindingLoginNoAnonymousSearch skips root context search"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true
        module._forceBindingLoginNoAnonymousSearch = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = false
        module._userRdnAttribute = 'cn'
        module._userBaseDn = 'dc=test,dc=com'
        module._roleBaseDn = 'roleBaseDn'
        module.rolePagination = false
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        })  // Use setter instead of @field access (Groovy 4)
        def expectedUserDn = "cn=$username,dc=test,dc=com"
        // no search should ever be issued against _rootContext, since the DN is constructed directly
        def rootContext = Mock(DirContext) {
            0 * _(*_)
        }
        module._rootContext = rootContext
        def stringRoles = ['role1', 'role2']
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> {new EnumImpl<String>(stringRoles)}
                }
            }
        }]
        DirContext userDir = Mock(DirContext) {
            // Regression coverage per Copilot review on PR #10530: fetchUserAttributes() must pass
            // userDn as a single CompositeName component, not a raw String -- DirContext.getAttributes(String)
            // parses its argument as a JNDI composite name, where '/' is a component separator, so a
            // DN containing a literal '/' (valid in an RDN value, e.g. username "a/b") would otherwise
            // be silently mis-parsed, targeting the wrong name and dropping demographic attributes.
            1 * getAttributes(new CompositeName().add(expectedUserDn)) >> new BasicAttributes()
            1 * search(
                'roleBaseDn',
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._roleObjectClass, 'roleUsernameMemberAttribute', username],
                _
            ) >> {new EnumImpl<SearchResult>(foundRoles)}
            0 * _(*_)
        }
        module.userBindDirContextCreator = { String user, Object pass ->
            assert user == expectedUserDn
            userDir
        }
        Subject testSubject = new Subject()
        when:
        boolean result = module.login()
        module.getCurrentUser().setJAASInfo(testSubject)  // Use getter instead of @field access (Groovy 4)

        then:
        result
        null != testSubject.getPrincipals(Principal)
        username == testSubject.getPrincipals(RundeckPrincipal).first().name
        null != testSubject.getPrincipals(RundeckRole)
        2 == testSubject.getPrincipals(RundeckRole).size()
        ['role1', 'role2'] == testSubject.getPrincipals(RundeckRole)*.name


        where:
        username | _
        'auser'  | _
        'a/b'    | _
    }

    def "bindingLogin with forceBindingLoginNoAnonymousSearch resolves paginated roles via the authenticated context"() {
        // Regression test per Copilot review on PR #10530: getPaginatedRoles() searched using the
        // module-level ldapContext field, which is built from _rootContext's (anonymous/bind-user)
        // environment at initialization, ignoring the authenticated user-bound dirContext passed to
        // getUserRolesByDn(). With the default rolePagination=true, this meant a directory that
        // disallows anonymous search would still fail to authenticate once roles are configured,
        // defeating forceBindingLoginNoAnonymousSearch's entire purpose.
        //
        // providerUrl is deliberately a space-separated multi-server failover list here (valid
        // JNDI config, per Context.PROVIDER_URL), to cover a second regression Copilot caught:
        // deriving the paging context via dirContext.lookup(_providerUrl) broke multi-URL configs,
        // since lookup() parses its argument as a single name/URL, not a failover list. The fix
        // uses dirContext.lookup("") (a self-lookup) instead, which never touches providerUrl.
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true
        module._forceBindingLoginNoAnonymousSearch = true
        module._contextFactory = "notnull"
        module._providerUrl = "ldap://host1.example.com ldap://host2.example.com"
        module._forceBindingLoginUseRootContextForRoles = false
        module._userRdnAttribute = 'cn'
        module._userBaseDn = 'dc=test,dc=com'
        module._roleBaseDn = 'roleBaseDn'
        module.rolePagination = true
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        })  // Use setter instead of @field access (Groovy 4)
        def expectedUserDn = "cn=$username,dc=test,dc=com"
        // no search should ever be issued against _rootContext, since the DN is constructed directly
        def rootContext = Mock(DirContext) {
            0 * _(*_)
        }
        module._rootContext = rootContext
        def stringRoles = ['role1', 'role2']
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> {new EnumImpl<String>(stringRoles)}
                }
            }
        }]
        def pagingContext = Mock(LdapContext) {
            1 * search(
                'roleBaseDn',
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._roleObjectClass, 'roleUsernameMemberAttribute', username],
                _
            ) >> {new EnumImpl<SearchResult>(foundRoles)}
            0 * search(*_)
        }
        // userDir is the authenticated context created by binding as the user; getPaginatedRoles
        // must derive its paging LdapContext from *this* context (via a lookup("") self-lookup,
        // which works with multi-server providerUrl failover lists too), never from the anonymous
        // module-level ldapContext field. getAttributes() must be called with userDn wrapped as a
        // single CompositeName component (not a raw String), so a '/' in the username (valid in an
        // RDN value) isn't mis-parsed as a JNDI composite-name separator.
        DirContext userDir = Mock(DirContext) {
            1 * getAttributes(new CompositeName().add(expectedUserDn)) >> new BasicAttributes()
            1 * lookup("") >> pagingContext
            0 * _(*_)
        }
        module.userBindDirContextCreator = { String user, Object pass ->
            assert user == expectedUserDn
            userDir
        }
        Subject testSubject = new Subject()
        when:
        boolean result = module.login()
        module.getCurrentUser().setJAASInfo(testSubject)  // Use getter instead of @field access (Groovy 4)

        then:
        result
        null != testSubject.getPrincipals(Principal)
        username == testSubject.getPrincipals(RundeckPrincipal).first().name
        null != testSubject.getPrincipals(RundeckRole)
        2 == testSubject.getPrincipals(RundeckRole).size()
        ['role1', 'role2'] == testSubject.getPrincipals(RundeckRole)*.name


        where:
        username | _
        'auser'  | _
        'a/b'    | _
    }

    def "initializeOptions parses forceBindingLoginNoAnonymousSearch and bindingLogin honors it"() {
        // Regression test per Copilot review on PR #10530: the other forceBindingLoginNoAnonymousSearch
        // tests set the protected _forceBindingLoginNoAnonymousSearch field directly, so a regression in
        // the public JAAS option's parsing or key wiring in initializeOptions() would leave those tests
        // green while the configured feature stayed silently disabled. This test instead drives the
        // option through initializeOptions() using its real option key, then exercises the binding path
        // through that parsed configuration.
        given:
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module.initializeOptions([
            forceBindingLogin                      : 'true',
            forceBindingLoginNoAnonymousSearch     : 'true',
            forceBindingLoginUseRootContextForRoles: 'false',
            contextFactory                         : 'notnull',
            providerUrl                            : 'notnull',
            userRdnAttribute                       : 'cn',
            userBaseDn                             : 'dc=test,dc=com',
            roleBaseDn                              : 'roleBaseDn',
            rolePagination                          : 'false',
            roleUsernameMemberAttribute             : 'roleUsernameMemberAttribute',
        ])

        expect:
        module._forceBindingLoginNoAnonymousSearch

        when:
        module._debug = true
        def expectedUserDn = "cn=auser,dc=test,dc=com"
        // no search should ever be issued against _rootContext, since the DN is constructed directly
        // -- this only happens if initializeOptions actually wired up _forceBindingLoginNoAnonymousSearch
        def rootContext = Mock(DirContext) {
            0 * _(*_)
        }
        module._rootContext = rootContext
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> {new EnumImpl<String>(['role1', 'role2'])}
                }
            }
        }]
        DirContext userDir = Mock(DirContext) {
            1 * getAttributes(new CompositeName().add(expectedUserDn)) >> new BasicAttributes()
            1 * search(
                'roleBaseDn',
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._roleObjectClass, 'roleUsernameMemberAttribute', 'auser'],
                _
            ) >> {new EnumImpl<SearchResult>(foundRoles)}
            0 * _(*_)
        }
        module.userBindDirContextCreator = { String user, Object pass ->
            assert user == expectedUserDn
            userDir
        }
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = 'auser'; it[0][1].object = 'apassword' }
        })
        boolean result = module.login()

        then:
        result
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
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        })  // Use setter instead of @field access (Groovy 4)
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
        module.getCurrentUser().setJAASInfo(testSubject)  // Use getter instead of @field access (Groovy 4)

        then:
        result
        null != testSubject.getPrincipals(Principal)
        username == testSubject.getPrincipals(Principal).first().name
        null != testSubject.getPrincipals(RundeckRole)
        2 == testSubject.getPrincipals(RundeckRole).size()
        ['role1', 'role2'] == testSubject.getPrincipals(RundeckRole)*.name


        where:
        username | _
        'auser'  | _
    }

    def "findUser requests demographic and password attributes explicitly"() {
        // Some directories (e.g. Active Directory over JNDI) return null for every attribute
        // when no returning-attribute list is set on the search, even though the same
        // attributes are readable via ldapsearch. findUser() must request them by name.
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true

        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = false
        module._roleBaseDn = 'roleBaseDn'
        module.rolePagination = false
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        })  // Use setter instead of @field access (Groovy 4)
        def found = [Mock(SearchResult) {
            getNameInNamespace() >> "cn=$username,dc=test,dc=com"
            getAttributes() >> Mock(Attributes) {
                get(module._userFirstNameAttribute) >> new BasicAttribute(module._userFirstNameAttribute, "First")
                get(module._userLastNameAttribute) >> new BasicAttribute(module._userLastNameAttribute, "Last")
                get(module._userEmailAttribute) >> new BasicAttribute(module._userEmailAttribute, "user@example.com")
            }
        }]
        def dirContext = Mock(DirContext) {
            1 * search(
                _,
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._userObjectClass, module._userIdAttribute, username],
                { SearchControls ctls ->
                    ctls.getReturningAttributes() as List == [
                        module._userIdAttribute,
                        module._userPasswordAttribute,
                        module._userFirstNameAttribute,
                        module._userLastNameAttribute,
                        module._userEmailAttribute,
                    ]
                }
            ) >> {new EnumImpl<SearchResult>(found)}
            0 * search(*_)
        }
        module._rootContext = dirContext
        DirContext userDir = Mock(DirContext) {
            _ * search(*_) >> {new EnumImpl<SearchResult>([])}
        }
        module.userBindDirContextCreator = { String user, Object pass ->
            userDir
        }
        when:
        boolean result = module.login()

        then:
        result
        module._userFirstName == "First"
        module._userLastName == "Last"
        module._userEmail == "user@example.com"

        where:
        username | _
        'auser'  | _
    }

    def "bindingLogin searches with the normalized username when case-insensitive matching is enabled"() {
        // Regression test: findUser() in the search-based bindingLogin path used the raw,
        // un-normalized username, while the role lookup and cached/returned UserInfo it feeds
        // into all use normalizedUsername. With case-insensitive username matching enabled, a
        // user logging in with different case than what's stored in LDAP would fail the search
        // (on directories with case-sensitive attribute matching), defeating the point of the
        // case-insensitive feature. Reported by Copilot review on PR #10530.
        //
        // Calls bindingLogin() directly with the raw (un-normalized) username, rather than
        // going through login(). login()'s authenticate() already normalizes the username
        // before ever calling bindingLogin(), so driving this test through login() would pass
        // regardless of whether bindingLogin() itself normalizes correctly -- it would only be
        // proving authenticate()'s normalization, not the fix under test. Calling bindingLogin()
        // directly with a raw username isolates and proves its own internal normalization.
        JettyCachingLdapLoginModule module = Spy(JettyCachingLdapLoginModule)
        module.isCaseInsensitiveUsernameEnabled() >> true
        module._debug = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = false
        module._roleBaseDn = 'roleBaseDn'
        module.rolePagination = false
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        def found = [Mock(SearchResult) {
            getNameInNamespace() >> "cn=$normalizedUsername,dc=test,dc=com"
            getAttributes() >> new BasicAttributes()
        }]
        String[] capturedFilterArgs = null
        def dirContext = Mock(DirContext) {
            1 * search(_, _, _, _) >> { args ->
                capturedFilterArgs = args[2] as String[]
                new EnumImpl<SearchResult>(found)
            }
        }
        module._rootContext = dirContext
        DirContext userDir = Mock(DirContext) {
            _ * search(*_) >> {new EnumImpl<SearchResult>([])}
        }
        module.userBindDirContextCreator = { String user, Object pass ->
            userDir
        }

        when:
        boolean result = module.bindingLogin(rawUsername, 'apassword')

        then:
        result
        // The key assertion: search() must be called with normalizedUsername, not
        // rawUsername. If bindingLogin regresses to passing the raw username, this
        // fails with the raw (wrong-case) value instead.
        capturedFilterArgs[2] == normalizedUsername
        where:
        rawUsername | normalizedUsername
        'AUser'     | 'auser'
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
        module.setCallbackHandler(Mock(CallbackHandler) {
            2 * handle(_) >> { it[0][0].name = username; it[0][1].object = passwordvalue }
        })  // Use setter instead of @field access (Groovy 4)
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
        module.getCurrentUser().setJAASInfo(testSubject)  // Use getter instead of @field access (Groovy 4)

        then:
        result
        result2
        null != testSubject.getPrincipals(Principal)
        username == testSubject.getPrincipals(Principal).first().name
        null != testSubject.getPrincipals(RundeckRole)
        2 == testSubject.getPrincipals(RundeckRole).size()
        ['role1', 'role2'] == testSubject.getPrincipals(RundeckRole)*.name


        where:
        username | passwordvalue
        'auser'  | 'apassword'
    }

    def "bindingLogin does not cache plaintext password credential"() {
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
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = passwordvalue }
        })  // Use setter instead of @field access (Groovy 4)
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

        when:
        boolean result = module.login()
        def cacheToken = PasswordCredential.md5Digest(username + ":" + passwordvalue)
        def cached = JettyCachingLdapLoginModule.USERINFOCACHE.get(cacheToken)

        then:
        result
        cached != null
        cached.userInfo.getCredential() == null

        where:
        username             | passwordvalue
        'credentialcacheuser' | 'credentialcachepassword'
    }

    def "bindingLogin cache key is stable when password arrives as char[] via PasswordCallback"() {
        // Regression test: outside of these unit tests, the password callback is populated by
        // Spring Security's default JaasPasswordCallbackHandler, which only fills the standard
        // PasswordCallback (not the custom ObjectCallback the other tests here use), so
        // bindingLogin() receives a char[]. Object.toString() on an array is identity-based and
        // differs per instance even for equal content, so the cache key derived from it used to
        // change on every login attempt -- meaning cached entries were written but never read
        // back, and LDAP caching never actually engaged. Each simulated login below gets its own
        // fresh char[] (as a real client's callback handler would produce), so a cache hit here
        // proves the key is derived from the password's content, not the array's identity.
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
        module.setCallbackHandler(Mock(CallbackHandler) {
            2 * handle(_) >> { it[0][0].name = username; it[0][2].password = passwordvalue.toCharArray() }
        })  // Use setter instead of @field access (Groovy 4); object callback left unset on purpose
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

        when:
        boolean result = module.login()
        boolean result2 = module.login()

        then:
        result
        result2

        where:
        username           | passwordvalue
        'charcachekeyuser'  | 'charcachekeypassword'
    }

    /**
     * Builds a module configured for binding login with caching enabled, scoped to the given role
     * base DN. The cache-scoping features below need two module instances that differ only in
     * configuration, so the shared wiring lives here.
     */
    private JettyCachingLdapLoginModule cachingBindingLoginModule(
            final String username,
            final String passwordvalue,
            final String roleBaseDn
    ) {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._cacheDuration = Integer.MAX_VALUE
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = false
        module._roleBaseDn = roleBaseDn
        module.rolePagination = false
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = passwordvalue }
        })  // Use setter instead of @field access (Groovy 4)
        return module
    }

    /**
     * Root context that resolves the user DN by exactly one user search, as a cache miss requires.
     */
    private DirContext userSearchRootContext(
            final JettyCachingLdapLoginModule module,
            final String username
    ) {
        def found = [Mock(SearchResult) {
            getNameInNamespace() >> "cn=$username,dc=test,dc=com"
            getAttributes() >> new BasicAttributes()
        }]
        return Mock(DirContext) {
            1 * search(
                _,
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._userObjectClass, module._userIdAttribute, username], _
            ) >> {new EnumImpl<SearchResult>(found)}

            0 * search(*_)
        }
    }

    /**
     * Bound user context that answers the role query for {@code roleBaseDn} with {@code roleNames}.
     */
    private DirContext roleSearchUserContext(
            final JettyCachingLdapLoginModule module,
            final String username,
            final String roleBaseDn,
            final List<String> roleNames
    ) {
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> {new EnumImpl<String>(roleNames)}
                }
            }
        }]
        return Mock(DirContext) {
            1 * search(
                roleBaseDn,
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._roleObjectClass, 'roleUsernameMemberAttribute', username],
                _
            ) >> {new EnumImpl<SearchResult>(foundRoles)}

            0 * _(*_)
        }
    }

    /**
     * Role names the module resolved for the authenticated user, read back the way a container
     * would: by letting the current user populate a Subject.
     */
    private List<String> resolvedRoleNames(final JettyCachingLdapLoginModule module) {
        Subject subject = new Subject()
        module.getCurrentUser().setJAASInfo(subject)  // Use getter instead of @field access (Groovy 4)
        return subject.getPrincipals(RundeckRole)*.name.sort()
    }

    /**
     * Module with every cache-key input set to a known, non-default value, so that a feature can
     * change exactly one of them and observe the effect on the token.
     */
    private JettyCachingLdapLoginModule cacheTokenModule() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._providerUrl = 'ldap://directory:389'
        module._userBaseDn = 'ou=users,dc=test,dc=com'
        module._userObjectClass = 'inetOrgPerson'
        module._userRdnAttribute = 'uid'
        module._userIdAttribute = 'cn'
        module._roleBaseDn = 'ou=roles,dc=test,dc=com'
        module._roleObjectClass = 'groupOfUniqueNames'
        module._roleMemberAttribute = 'uniqueMember'
        module._roleUsernameMemberAttribute = 'memberUid'
        module._roleNameAttribute = 'cn'
        module._rolePrefix = 'rundeck_'
        module._nestedGroups = false
        module._allGroups = true
        module._forceBindingLoginUseRootContextForRoles = false
        module._supplementalRoles = ['user']
        return module
    }

    def "bindingLogin cache entries are scoped per module config"() {
        // USERINFOCACHE is static, so it is shared by every instance of this login module. Two JAAS
        // entries can authenticate the same user against different role subtrees; if the cache key
        // did not cover the module config, the first module's roles would be satisfy every later
        // module's lookup, so the authenticate subject only ever received the first module's roles.
        given: 'two modules that differ only in the role subtree they query'
        JettyCachingLdapLoginModule firstModule = cachingBindingLoginModule(username, passwordvalue, 'roleBaseDnOne')
        firstModule._rootContext = userSearchRootContext(firstModule, username)
        DirContext firstUserDir = roleSearchUserContext(firstModule, username, 'roleBaseDnOne', ['scopedRoleOne'])
        firstModule.userBindDirContextCreator = { String user, Object pass -> firstUserDir }

        JettyCachingLdapLoginModule secondModule = cachingBindingLoginModule(username, passwordvalue, 'roleBaseDnTwo')
        secondModule._rootContext = userSearchRootContext(secondModule, username)
        DirContext secondUserDir = roleSearchUserContext(secondModule, username, 'roleBaseDnTwo', ['scopedRoleTwo'])
        secondModule.userBindDirContextCreator = { String user, Object pass -> secondUserDir }

        when: 'the same user authenticates through both modules'
        boolean firstResult = firstModule.login()
        boolean secondResult = secondModule.login()

        then: 'each module resolves roles from its own subtree, so neither reuses the other cache entry'
        firstResult
        secondResult
        ['scopedRoleOne'] == resolvedRoleNames(firstModule)
        ['scopedRoleTwo'] == resolvedRoleNames(secondModule)

        and: 'the two configs produce different cache keys'
        firstModule.buildUserInfoCacheToken(username, passwordvalue) !=
                secondModule.buildUserInfoCacheToken(username, passwordvalue)

        cleanup:
        JettyCachingLdapLoginModule.USERINFOCACHE.remove(
                firstModule.buildUserInfoCacheToken(username, passwordvalue))
        JettyCachingLdapLoginModule.USERINFOCACHE.remove(
                secondModule.buildUserInfoCacheToken(username, passwordvalue))

        where:
        username     | passwordvalue
        'scopeduser' | 'scopedpassword'
    }

    def "bindingLogin reuses cached entry across module instances with identical config"() {
        // The flip side of the scoping test: identically configured instances must still share the
        // static cache, otherwise adding the config to the key would disable LDAP caching entirely,
        // since a fresh login module instance is built per authentication.
        given: 'a module that performs the LDAP lookups and populates the cache'
        JettyCachingLdapLoginModule firstModule = cachingBindingLoginModule(username, passwordvalue, 'roleBaseDn')
        firstModule._rootContext = userSearchRootContext(firstModule, username)
        DirContext firstUserDir = roleSearchUserContext(
                firstModule, username, 'roleBaseDn', ['cachedRoleOne', 'cachedRoleTwo'])
        firstModule.userBindDirContextCreator = { String user, Object pass -> firstUserDir }

        and: 'a second, identically configured module whose contexts must never be touched'
        JettyCachingLdapLoginModule secondModule = cachingBindingLoginModule(username, passwordvalue, 'roleBaseDn')
        secondModule._rootContext = Mock(DirContext) {
            0 * _(*_)
        }
        secondModule.userBindDirContextCreator = { String user, Object pass ->
            throw new IllegalStateException('bind must not be attempted on a cache hit')
        }

        when: 'the same user authenticates through both modules'
        boolean firstResult = firstModule.login()
        boolean secondResult = secondModule.login()

        then: 'the second login is served from the cache, with the roles the first login resolved'
        firstResult
        secondResult
        ['cachedRoleOne', 'cachedRoleTwo'] == resolvedRoleNames(secondModule)

        and: 'both modules agree on the cache key'
        firstModule.buildUserInfoCacheToken(username, passwordvalue) ==
                secondModule.buildUserInfoCacheToken(username, passwordvalue)

        cleanup:
        JettyCachingLdapLoginModule.USERINFOCACHE.remove(
                firstModule.buildUserInfoCacheToken(username, passwordvalue))

        where:
        username         | passwordvalue
        'cachereuseuser' | 'cachereusepassword'
    }

    @Unroll
    def "buildUserInfoCacheToken varies with #setting"() {
        given: 'two fully configured modules that differ in exactly one cache-key input'
        JettyCachingLdapLoginModule baseline = cacheTokenModule()
        JettyCachingLdapLoginModule altered = cacheTokenModule()
        mutate.call(altered)

        expect: 'the altered setting changes the token'
        baseline.buildUserInfoCacheToken(user1, password) !=
                altered.buildUserInfoCacheToken(user1, password)

        where:
        setting                                   | mutate
        'providerUrl'                             | { it._providerUrl = 'ldap://other:389' }
        'userBaseDn'                              | { it._userBaseDn = 'ou=people,dc=test,dc=com' }
        'userObjectClass'                         | { it._userObjectClass = 'person' }
        'userRdnAttribute'                        | { it._userRdnAttribute = 'cn' }
        'userIdAttribute'                         | { it._userIdAttribute = 'uid' }
        'roleBaseDn'                              | { it._roleBaseDn = 'ou=groups,dc=test,dc=com' }
        'roleObjectClass'                         | { it._roleObjectClass = 'group' }
        'roleMemberAttribute'                     | { it._roleMemberAttribute = 'member' }
        'roleUsernameMemberAttribute'             | { it._roleUsernameMemberAttribute = 'memberOf' }
        'roleNameAttribute'                       | { it._roleNameAttribute = 'roleName' }
        'rolePrefix'                              | { it._rolePrefix = 'other_' }
        'nestedGroups'                            | { it._nestedGroups = true }
        'allGroups'                               | { it._allGroups = false }
        'forceBindingLoginUseRootContextForRoles' | { it._forceBindingLoginUseRootContextForRoles = true }
        'supplementalRoles'                       | { it._supplementalRoles = ['user', 'admin'] }
    }

    def "buildUserInfoCacheToken varies with the credentials it is given"() {
        given:
        JettyCachingLdapLoginModule module = cacheTokenModule()

        expect: 'a different user, or a different password for the same user, is keyed separately'
        module.buildUserInfoCacheToken(user1, password) != module.buildUserInfoCacheToken(user2, password)
        module.buildUserInfoCacheToken(user1, password) != module.buildUserInfoCacheToken(user1, 'otherpassword')

        and: 'the password is keyed by content, so a char[] matches the equivalent String'
        module.buildUserInfoCacheToken(user1, password) ==
                module.buildUserInfoCacheToken(user1, password.toCharArray())

        and: 'two equal char[] instances key the same, rather than by array identity'
        module.buildUserInfoCacheToken(user1, password.toCharArray()) ==
                module.buildUserInfoCacheToken(user1, password.toCharArray())
    }

    def "buildUserInfoCacheToken is unambiguous when adjacent values run together"() {
        given: 'two configs whose adjacent values concatenate to the same raw text'
        JettyCachingLdapLoginModule first = cacheTokenModule()
        first._userBaseDn = 'ou=a'
        first._userObjectClass = 'bPerson'

        JettyCachingLdapLoginModule second = cacheTokenModule()
        second._userBaseDn = 'ou=ab'
        second._userObjectClass = 'Person'

        expect: 'the length prefixes keep the two configs apart'
        'ou=a' + 'bPerson' == 'ou=ab' + 'Person'
        first.buildUserInfoCacheToken(user1, password) != second.buildUserInfoCacheToken(user1, password)

        and: 'the same holds for the username and password, which are also adjacent in the key'
        first.buildUserInfoCacheToken('ab', 'cd') != first.buildUserInfoCacheToken('a', 'bcd')
    }

    def "buildUserInfoCacheToken distinguishes an unset setting from an empty one"() {
        given: 'one module with an unset role member attribute and one with an empty string'
        JettyCachingLdapLoginModule unsetAttribute = cacheTokenModule()
        unsetAttribute._roleUsernameMemberAttribute = null

        JettyCachingLdapLoginModule emptyAttribute = cacheTokenModule()
        emptyAttribute._roleUsernameMemberAttribute = ''

        and: 'one module with no supplemental roles and one with an empty list of them'
        JettyCachingLdapLoginModule unsetSupplementalRoles = cacheTokenModule()
        unsetSupplementalRoles._supplementalRoles = null

        JettyCachingLdapLoginModule emptySupplementalRoles = cacheTokenModule()
        emptySupplementalRoles._supplementalRoles = []

        expect: 'an unset attribute is not keyed the same as an empty one'
        unsetAttribute.buildUserInfoCacheToken(user1, password) !=
                emptyAttribute.buildUserInfoCacheToken(user1, password)

        and: 'nor are absent supplemental roles keyed the same as an empty list of them'
        unsetSupplementalRoles.buildUserInfoCacheToken(user1, password) !=
                emptySupplementalRoles.buildUserInfoCacheToken(user1, password)
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
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        })  // Use setter instead of @field access (Groovy 4)
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
        // Paginated role lookup must derive its LdapContext from the authenticated
        // user-bound dirContext (via a lookup("") self-lookup, which works with
        // multi-server providerUrl failover lists too), not the module-level
        // anonymous/root ldapContext field, so it still works when no anonymous
        // search is available.
        DirContext userDir = Mock(DirContext) {
            1 * lookup("") >> ldapContext
            0 * _(*_)
        }
        module.userBindDirContextCreator = { String user, Object pass ->
            userDir
        }
        Subject testSubject = new Subject()
        when:
        boolean result = module.login()
        module.getCurrentUser().setJAASInfo(testSubject)  // Use getter instead of @field access (Groovy 4)

        then:
        result
        null != testSubject.getPrincipals(Principal)
        username == testSubject.getPrincipals(Principal).first().name
        null != testSubject.getPrincipals(RundeckRole)
        2 == testSubject.getPrincipals(RundeckRole).size()
        ['role1', 'role2'] == testSubject.getPrincipals(RundeckRole)*.name


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
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = user1; it[0][1].object = '' }
        })  // Use setter instead of @field access (Groovy 4)

        when:
        !module.login()

        then:
        thrown FailedLoginException
    }


    def "test disallow null password"() {
        given:
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = user1; it[0][1].object = null }
        })  // Use setter instead of @field access (Groovy 4)

        when:
        !module.login()

        then:
        thrown FailedLoginException
    }


    def "test disallow empty char array password"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        module._debug = true
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = user1; it[0][1].object = '' }
        })  // Use setter instead of @field access (Groovy 4)

        when:
        !module.login()

        then:
        thrown FailedLoginException
    }


    def "test disallow empty username"() {
        given:
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = ''; it[0][1].object = 'xyz' }
        })  // Use setter instead of @field access (Groovy 4)

        when:
        !module.login()

        then:
        thrown FailedLoginException
    }


    def "test disallow null username"() {
        given:
        def module = Spy(JettyCachingLdapLoginModule)
        module._debug = true
        module.setCallbackHandler(Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = null; it[0][1].object = 'xyz' }
        })  // Use setter instead of @field access (Groovy 4)
        when:
        !module.login()

        then:
        thrown FailedLoginException
    }


    // NOTE: "test get user attributes email first name last name" deleted as duplicate
    // This functionality is covered by JettyCachingLdapLoginModuleTest2.testGetUserAttributesEmail_FirstName_LastName() (Java test - passing)
    // and RundeckJaasAuthenticationSuccessEventListenerTest (integration flow - passing)

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
            module._allGroups = true
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

        // getPaginatedRoles derives its LdapContext via dirContext.lookup("") (a self-lookup, so
        // it works regardless of whether providerUrl is a single URL or a multi-server failover
        // list), while buildRoleMemberOfMap's nested-groups search still derives its own via
        // dirContext.lookup(providerUrl). Both are served by this single LdapContext mock,
        // returned from _rootContext's lookup() for either argument.
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
            search(
                    module._roleBaseDn,
                    module._roleMemberFilter,
                    _ as SearchControls
            ) >> { new EnumImpl<SearchResult>(nestedRoles) }
        }

        DirContext dirContext = Mock(DirContext) {
            2 * search(
                    module._userBaseDn,
                    JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                    [module._userObjectClass, module._userIdAttribute, user1],
                    _ as SearchControls
            ) >> { new EnumImpl<SearchResult>(foundRoles) }
            lookup("") >> ldapContext
            lookup(module._providerUrl) >> ldapContext
        }

        module._rootContext = dirContext

        return module
    }
}
