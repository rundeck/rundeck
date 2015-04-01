package com.dtolabs.rundeck.jetty.jaas;

import org.eclipse.jetty.plus.jaas.spi.UserInfo;
import org.junit.Test;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by greg on 4/1/15.
 */
public class JettyCombinedLdapLoginModuleTest {

    private final String user1 = "user1";
    private final String user2 = "user2";
    private final String password = "password";
    private final String role1 = "role1";
    private final String role2 = "role2";
    private final String nestedRole1 = "nestedRole1";

    private JettyCombinedLdapLoginModule getJettyCachingLdapLoginModule(boolean activeDirectory) {
        JettyCombinedLdapLoginModule module = new JettyCombinedLdapLoginModule();

        module._userBaseDn = "ou=users,dc=example,dc=com";
        module._roleBaseDn = "ou=groups,dc=example,dc=com";

        DirContext rootContext = mock(DirContext.class);
        NamingEnumeration<SearchResult> userSearchResults = mock(NamingEnumeration.class);
        when(userSearchResults.hasMoreElements()).thenReturn(true);
        SearchResult userSearchResult = mock(SearchResult.class);
        Attributes userAttributes = mock(Attributes.class);
        Attribute passwordAttribute = mock(Attribute.class);
        when(userAttributes.get(module._userPasswordAttribute)).thenReturn(passwordAttribute);
        when(userSearchResult.getAttributes()).thenReturn(userAttributes);
        when(userSearchResults.nextElement()).thenReturn(userSearchResult);

        try {
            when(passwordAttribute.get()).thenReturn(password.getBytes());
            when(rootContext.search(eq(module._userBaseDn), anyString(), any(Object[].class), any(SearchControls.class))).thenReturn(userSearchResults);
        } catch (NamingException e) {
            e.printStackTrace();
        }

        NamingEnumeration<SearchResult> roleSearchResults = mock(NamingEnumeration.class);
        when(roleSearchResults.hasMoreElements()).thenReturn(true, false);
        SearchResult rolesSearchResult = mock(SearchResult.class);
        Attributes roleAttributes = mock(Attributes.class);
        when(rolesSearchResult.getAttributes()).thenReturn(roleAttributes);
        Attribute roleAttribute = mock(Attribute.class);
        when(roleAttributes.get(module._roleNameAttribute)).thenReturn(roleAttribute);
        NamingEnumeration roles = mock(NamingEnumeration.class);

        NamingEnumeration<SearchResult> allRolesSearchResults = mock(NamingEnumeration.class);
        when(allRolesSearchResults.hasMoreElements()).thenReturn(true, true, true, false);

        SearchResult role1SearchResult = mock(SearchResult.class);
        Attributes role1Attributes = mock(Attributes.class);
        when(role1SearchResult.getAttributes()).thenReturn(role1Attributes);
        Attribute role1NameAttribute = mock(Attribute.class);
        Attribute role1MemberAttribute = mock(Attribute.class);
        when(role1Attributes.get(eq(module._roleNameAttribute))).thenReturn(role1NameAttribute);
        when(role1Attributes.get(eq(module._roleMemberAttribute))).thenReturn(role1MemberAttribute);
        NamingEnumeration role1Roles = mock(NamingEnumeration.class);


        SearchResult role2SearchResult = mock(SearchResult.class);
        Attributes role2Attributes = mock(Attributes.class);
        when(role2SearchResult.getAttributes()).thenReturn(role2Attributes);
        Attribute role2NameAttribute = mock(Attribute.class);
        Attribute role2MemberAttribute = mock(Attribute.class);
        when(role2Attributes.get(eq(module._roleNameAttribute))).thenReturn(role2NameAttribute);
        when(role2Attributes.get(eq(module._roleMemberAttribute))).thenReturn(role2MemberAttribute);
        NamingEnumeration role2Roles = mock(NamingEnumeration.class);


        SearchResult nestedRole1SearchResult = mock(SearchResult.class);
        Attributes nestedRole1Attributes = mock(Attributes.class);
        when(nestedRole1SearchResult.getAttributes()).thenReturn(nestedRole1Attributes);
        Attribute nestedRole1NameAttribute = mock(Attribute.class);
        Attribute nestedRole1MemberAttribute = mock(Attribute.class);
        when(nestedRole1Attributes.get(eq(module._roleNameAttribute))).thenReturn(nestedRole1NameAttribute);
        when(nestedRole1Attributes.get(eq(module._roleMemberAttribute))).thenReturn(nestedRole1MemberAttribute);
        NamingEnumeration nestedRole1Roles = mock(NamingEnumeration.class);
        NamingEnumeration nestedRole1Members = mock(NamingEnumeration.class);

        try {
            when(rootContext.search(eq(module._roleBaseDn), anyString(), any(Object[].class), any(SearchControls.class))).thenReturn(roleSearchResults);
            when(roleSearchResults.nextElement()).thenReturn(rolesSearchResult);
            when(roleAttribute.getAll()).thenReturn(roles);
            when(roles.hasMore()).thenReturn(true, true, false);
            when(roles.next()).thenReturn(role1, role2);

            when(rootContext.search(eq(module._roleBaseDn), eq(module._roleMemberFilter), any(SearchControls.class))).thenReturn(allRolesSearchResults);
            when(allRolesSearchResults.nextElement()).thenReturn(role1SearchResult, role2SearchResult, nestedRole1SearchResult);

            when(role1NameAttribute.getAll()).thenReturn(role1Roles);
            when(role1Roles.next()).thenReturn(role1);

            when(role2NameAttribute.getAll()).thenReturn(role2Roles);
            when(role2Roles.next()).thenReturn(role2);

            when(nestedRole1NameAttribute.getAll()).thenReturn(nestedRole1Roles);
            when(nestedRole1Roles.hasMore()).thenReturn(true);
            when(nestedRole1Roles.next()).thenReturn(nestedRole1);
            when(nestedRole1MemberAttribute.getAll()).thenReturn(nestedRole1Members);
            when(nestedRole1Members.hasMore()).thenReturn(true, true, true, false);
            if(activeDirectory) {
                when(nestedRole1Members.next()).thenReturn("CN=" + role1 + "," + module._roleBaseDn, "uid=" + user2 + "," + module._roleBaseDn);
            } else {
                when(nestedRole1Members.next()).thenReturn("cn=" + role1 + "," + module._roleBaseDn, "uid=" + user2 + "," + module._roleBaseDn);
            }


        } catch (NamingException e) {
            e.printStackTrace();
        }

        module._rootContext = rootContext;
        return module;
    }


    @Test
    public void shouldIgnoreRoles() throws Exception{
        JettyCombinedLdapLoginModule module = getJettyCachingLdapLoginModule(false);
        module._ignoreRoles = true;
        UserInfo userInfo = module.getUserInfo(user1);
        assertThat(userInfo.getUserName(), is(user1));

        List<String> actualRoles = userInfo.getRoleNames();
        List<String> expectedRoles = Arrays.asList();
        assertThat(actualRoles, is(expectedRoles));
    }
    @Test
    public void ignoreRolesShouldIncludeSupplementalRoles() throws Exception{
        JettyCombinedLdapLoginModule module = getJettyCachingLdapLoginModule(false);
        module._ignoreRoles = true;
        module._supplementalRoles = Arrays.asList("test1", "Test2");
        UserInfo userInfo = module.getUserInfo(user1);
        assertThat(userInfo.getUserName(), is(user1));

        List<String> actualRoles = userInfo.getRoleNames();
        List<String> expectedRoles = Arrays.asList("test1", "Test2");
        assertThat(actualRoles, is(expectedRoles));
    }
}
