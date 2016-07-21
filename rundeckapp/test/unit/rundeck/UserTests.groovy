package rundeck

import org.springframework.context.support.StaticMessageSource

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
	void testMessageForDefaultLocale() {
		StaticMessageSource messageSource = getMessageSource()
		messageSource.addMessage("gui.menu.Workflows", Locale.default, "Jobs")

		assert "Jobs" == messageSource.getMessage("gui.menu.Workflows", [] as Object[], Locale.default)
	}
	void testMessageForLocale() {
		def defaultLocale = new Locale("es_419","es_419");
		java.util.Locale.setDefault(defaultLocale)

		StaticMessageSource messageSource = getMessageSource()
		messageSource.addMessage("gui.menu.Workflows", defaultLocale, "Trabajosme")

		assert "Trabajosme" == messageSource.getMessage("gui.menu.Workflows", [] as Object[], defaultLocale)
	}
}
