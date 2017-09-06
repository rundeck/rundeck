package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.User
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

@TestFor(UserController)
@Mock([User])
class UserControllerSpec extends Specification{

    @Unroll
    def "get info same user"(){
        given:
        def userToSearch = 'admin'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_)>>auth
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            0 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }
        }
        when:
        request.method='GET'
        request.format='xml'
        //request.content=('<contents>'+text+'</contents>').bytes
        def result=controller.apiUserData()

        then:
        response.status==200
    }
}
