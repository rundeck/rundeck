package org.rundeck.util.api.scm.httpbody

import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo

interface GitSetupRequest<T> {

    T forProject(String project)

    T withRepo(GiteaApiRemoteRepo remoteRepo)

}