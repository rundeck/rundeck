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
package org.rundeck.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken


class RundeckUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    @Override
    UserDetails loadUserDetails(final PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        return new RundeckUserDetails(token.principal?.toString(),token.credentials?.toString())
    }

    class RundeckUserDetails implements UserDetails {

        private String username
        private SimpleGrantedAuthority authority
        RundeckUserDetails(String username, String roles) {
            this.username = username
            this.authority = new SimpleGrantedAuthority(roles)
        }

        @Override
        Collection<? extends GrantedAuthority> getAuthorities() {
            return [this.authority]
        }

        @Override
        String getPassword() {
            return null
        }

        @Override
        String getUsername() {
            return username
        }

        @Override
        boolean isAccountNonExpired() {
            return true
        }

        @Override
        boolean isAccountNonLocked() {
            return true
        }

        @Override
        boolean isCredentialsNonExpired() {
            return true
        }

        @Override
        boolean isEnabled() {
            return true
        }
    }
}
