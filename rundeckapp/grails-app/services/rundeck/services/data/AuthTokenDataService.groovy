package rundeck.services.data

import grails.gorm.services.Service
import org.rundeck.app.data.model.v1.AuthenticationToken
import rundeck.AuthToken

@Service(AuthToken)
interface AuthTokenDataService {

    AuthToken get(Serializable id)

    AuthToken getByUuid(String uuid)

    AuthToken save(AuthToken authToken)

    //Long countByUser(String user)

    void deleteByUuid(String uuid)

    void delete(Serializable id)

}
