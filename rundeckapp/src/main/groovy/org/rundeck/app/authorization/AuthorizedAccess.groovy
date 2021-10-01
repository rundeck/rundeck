package org.rundeck.app.authorization

import groovy.transform.CompileStatic

import javax.security.auth.Subject

@CompileStatic
interface AuthorizedAccess<D, T extends AuthorizedResource<D>, I> {
    T accessResource(Subject subject, I identifier)
}