package rundeck

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

/**
 */
@TestFor(User)
class UserTests {

    void setUp() {
        // Setup logic here
    }

    void tearDown() {
        // Tear down logic here
    }

    void testBasic() {
        def user = new User(login: 'login')
        user.validate()
        assertFalse(user.errors.allErrors.collect { it.toString() }.join("; "),user.hasErrors())
    }
    void testValidationChars() {
        def user = new User(login: 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ  @ 1234567890 .,(-) \\/_')
        user.validate()
        assertFalse(user.errors.allErrors.collect { it.toString() }.join("; "),user.hasErrors())
    }
    void testValidationAccountName() {
        def user = new User(login: 'Lastname, Firstname (1234560)')
        user.validate()
        assertFalse(user.errors.allErrors.collect { it.toString() }.join("; "),user.hasErrors())
    }
    void testValidationLastname() {
        def user = new User(login: 'lastname',lastName: 'abcdEFGHI12390 ,.- ()')
        user.validate()
        assertFalse(user.errors.allErrors.collect { it.toString() }.join("; "),user.hasErrors())
    }
    void testValidationFirstname() {
        def user = new User(login: 'firstname',firstName: 'abcdEFGHI12390 ,.- ()')
        user.validate()
        assertFalse(user.errors.allErrors.collect { it.toString() }.join("; "),user.hasErrors())
    }
}
