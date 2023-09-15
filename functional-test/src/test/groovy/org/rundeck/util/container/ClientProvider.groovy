package org.rundeck.util.container

interface ClientProvider {
    RdClient getClient()

    RdClient clientWithToken(String token)
}