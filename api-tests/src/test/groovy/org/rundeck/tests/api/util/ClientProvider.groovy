package org.rundeck.tests.api.util

interface ClientProvider {
    RdClient getClient()

    RdClient clientWithToken(String token)
}