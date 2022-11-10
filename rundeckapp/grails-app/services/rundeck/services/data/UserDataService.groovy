package rundeck.services.data

import grails.gorm.services.Service
import rundeck.User

@Service(User)
interface UserDataService {
    User get(Serializable id)

    User getByLogin(String login)

    User save(User user)

    void delete(Serializable id)
}
