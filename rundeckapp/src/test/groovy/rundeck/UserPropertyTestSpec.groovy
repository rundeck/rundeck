package rundeck

import grails.testing.gorm.DataTest
import spock.lang.Specification

class UserPropertyTestSpec extends Specification implements DataTest {
    
    void setupSpec() {
        mockDomain User
    }
    
    def "test User creation and save"() {
        given:
        def user = new User(login: "testuser")
        
        when:
        def result = user.save(flush: true, failOnError: true)
        
        then:
        result != null
        // In mocked GORM, ID might be null - just verify save() works
        
        when:
        def foundUser = User.findByLogin("testuser")
        
        then:
        foundUser != null
        foundUser.login == "testuser"
    }
    
    def "test User with explicit ID"() {
        given:
        def user = new User(login: "testuser2")
        user.id = 1L  // Try to set ID explicitly
        
        when:
        def result = user.save(flush: true, failOnError: true)
        
        then:
        result != null
        user.id == 1L
    }
}

