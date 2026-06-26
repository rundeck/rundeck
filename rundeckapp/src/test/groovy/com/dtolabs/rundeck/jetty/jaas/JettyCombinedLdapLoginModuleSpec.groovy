package com.dtolabs.rundeck.jetty.jaas

import org.rundeck.jaas.RundeckPrincipal
import org.rundeck.jaas.RundeckRole
import org.rundeck.jaas.AbstractLoginModule
import spock.lang.Specification

import javax.naming.NamingEnumeration
import javax.naming.directory.*
import javax.security.auth.Subject
import java.util.stream.Collectors

class JettyCombinedLdapLoginModuleSpec extends Specification {

    private static final String user1 = "user1";
    private static final String user2 = "user2";
    private static final String password = "password";
    private static final String role1 = "role1";
    private static final String role2 = "role2";
    private static final String nestedRole1 = "nestedRole1";

    def "ignoreRoles works in binding context"() {
        given:
            JettyCombinedLdapLoginModule module = getJettyCachingLdapLoginModule(false)
            module._userBaseDn = "ou=users,dc=example,dc=com"
            module._roleBaseDn = "ou=groups,dc=example,dc=com"
            module._ignoreRoles = ignoreRoles
            module._forceBindingLogin = true
            module._forceBindingLoginUseRootContextForRoles = true
            module._contextFactory = "foo"
            module._hostname = "foo"
            module._port = 111
            module._bindDn = "someDn"
            module.rolePagination=false


            module.userBindDirContextCreator = { final String userDn, final Object password ->
                Mock(DirContext)
            }

        when:
            boolean authSuccess = module.bindingLogin(user1, password);
        then:
            authSuccess
        when:
            final AbstractLoginModule.JAASUserInfo userInfo = module.getCurrentUser();
            Subject subject = new Subject();
            userInfo.setJAASInfo(subject);
            // Java 17: Use lambda instead of method reference to avoid Groovy type inference issues
            List<String> actualRoles = subject
                .getPrincipals(RundeckRole.class).stream().map(p -> p.getName()).toList()
        then:
            actualRoles == expected
        where:
            ignoreRoles | expected
            true        | []
            false       | ["role1", "role2"]
    }

    JettyCombinedLdapLoginModule getJettyCachingLdapLoginModule(boolean activeDirectory) {
        def module = new JettyCombinedLdapLoginModule()
        module._userBaseDn = "ou=users,dc=example,dc=com"
        module._roleBaseDn = "ou=groups,dc=example,dc=com"

        def rootContext = Mock(DirContext)

        // User search setup
        rootContext.search(module._userBaseDn, _ as String, _ as Object[], _ as SearchControls) >> {
            Mock(NamingEnumeration) {
                hasMoreElements() >> true
                nextElement() >> Mock(SearchResult) {
                    getNameInNamespace() >> user1
                    getAttributes() >> Mock(Attributes) {
                        get(module._userPasswordAttribute) >> Mock(Attribute) {
                            get() >> "password".bytes
                        }
                    }
                }
            }
        }

        // Role search setup
        rootContext.search(module._roleBaseDn, _ as String, _ as Object[], _ as SearchControls) >> {
            Mock(NamingEnumeration) {
                hasMoreElements() >> true >> false
                nextElement() >> Mock(SearchResult) {
                    getAttributes() >> Mock(Attributes) {
                        get(module._roleNameAttribute) >> Mock(Attribute) {
                            getAll() >> Mock(NamingEnumeration) {
                                hasMore() >> true >> true >> false
                                next() >> role1 >> role2
                            }
                        }
                    }
                }
            }
        }

        // All roles search setup
        rootContext.search(module._roleBaseDn, module._roleMemberFilter, _ as SearchControls) >> {
            Mock(NamingEnumeration) {
                hasMoreElements() >> true >> true >> true >> false
                nextElement() >> setupRoleSearchResult(role1, module) >>
                setupRoleSearchResult(role2, module) >>
                setupNestedRoleSearchResult(nestedRole1, role1, user2, module, activeDirectory)
            }
        }

        module._rootContext = rootContext
        return module
    }

    SearchResult setupRoleSearchResult(String roleName, JettyCombinedLdapLoginModule module) {
        Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> Mock(NamingEnumeration) {
                        next() >> roleName
                    }
                }
                get(module._roleMemberAttribute) >> Mock(Attribute)
            }
        }
    }

    SearchResult setupNestedRoleSearchResult(
        String nestedRoleName,
        String role1,
        String user2,
        JettyCombinedLdapLoginModule module,
        boolean activeDirectory
    ) {
        Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> Mock(NamingEnumeration) {
                        hasMore() >> true >> false
                        next() >> nestedRoleName
                    }
                }
                get(module._roleMemberAttribute) >> Mock(Attribute) {
                    getAll() >> Mock(NamingEnumeration) {
                        hasMore() >> true >> false
                        next() >> {
                            activeDirectory ?
                            "CN=${role1},${module._roleBaseDn}" :
                            "cn=${role1},${module._roleBaseDn}"
                        }
                    }
                }
            }
        }
    }
}
