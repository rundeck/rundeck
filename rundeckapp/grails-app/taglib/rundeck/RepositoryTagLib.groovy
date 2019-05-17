package rundeck

class RepositoryTagLib {
    //static defaultEncodeAs = [taglib:'html']

    def repoClient

    def listRepos={attrs,body->
        if(repoClient) {
            repoClient.listRepositories().findAll{ it.enabled }.each { repo ->
                out << "<li>${repo.repositoryName}</li>"
            }
        }
    }

}
