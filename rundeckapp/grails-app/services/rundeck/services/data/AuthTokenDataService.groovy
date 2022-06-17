package rundeck.services.data

import grails.gorm.services.Service
import rundeck.AuthToken

@Service(AuthToken)
interface AuthTokenDataService {

    AuthToken get(Serializable id)

    AuthToken getByUuid(String uuid)

    AuthToken save(AuthToken execution)

    Long countByUser(String user)

    void deleteByUuid(String uuid)
}