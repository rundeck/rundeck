package org.rundeck.app.providers

import com.dtolabs.rundeck.core.config.Features
import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.user.LoginStatus
import org.rundeck.app.data.providers.GormUserDataProvider
import org.rundeck.spi.data.DataAccessException
import rundeck.User
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import rundeck.services.UserService
import rundeck.services.data.UserDataService
import com.dtolabs.rundeck.core.config.FeatureService
import spock.lang.Unroll
import testhelper.RundeckHibernateSpec

class GormUserDataProviderSpec extends RundeckHibernateSpec implements DataTest {
    GormUserDataProvider provider = new GormUserDataProvider()

    void setup() {
        mockDomain(User)
        mockDataService(UserDataService)
        provider.userDataService = applicationContext.getBean(UserDataService)
    }

    @Unroll
    def "Find or create User"() {
        given:
        User savedUser = new User(login: "saved")
        savedUser.save()
        when:
        User user = provider.findOrCreateUser(login)
        then:
        user.id
        user.is(savedUser) == expect
        where:
        login   | expect
        "saved" | true
        "login" | false
    }

    @Unroll
    def "Find or create User case sensitive #caseSensitiveParam"() {
        given:
        def loginName = "loginName1"
        User savedUser = new User(login: loginName)
        savedUser.save()

        and:
        provider.featureService = Mock(FeatureService){
            featurePresent(Features.CASE_INSENSITIVE_USERNAME)>>caseSensitiveParam
        }

        when:
        provider.findOrCreateUser(loginName.toUpperCase())

        then:
        User.findAll().size() == userCountSpec

        where:
        caseSensitiveParam << [true, false]
        userCountSpec << [1, 2]
    }

    def "Throw an error on creation"() {
        when:
        provider.findOrCreateUser("~name")
        then:
        DataAccessException e = thrown()
    }

    @Unroll
    def "Should register login"() {
        setup:
        provider.configurationService = Mock(ConfigurationService) {
            1 * getBoolean(UserService.SESSION_ID_ENABLED, false) >> sessionIdEnabled
            (sessionIdEnabled ? 1 : 0) * getString(UserService.SESSION_ID_METHOD, 'hash') >> method
        }
        provider.frameworkService = Mock(FrameworkService) {
            getServerHostname() >> { "server" }
        }
        User savedUser = new User(login: "saved")
        savedUser.save()
        when:
        User user = provider.registerLogin(login, sessionId)
        then:
        user.getLogin() == login
        user.getLastLogin()
        user.getLastLoggedHostName() == "server"
        user.getLastSessionId() == expect
        where:
        login      | method  | sessionIdEnabled | sessionId | expect
        "login"    | "plain" | true             | "session" | "session"
        "saved"    | "plain" | true             | "saved"   | "saved"
        "hashed"   | "hash"  | true             | "hashId"  | "4f9adf1a1e6419207ae6032600a608789e5757c656ff6177322d9ad71518dbfb"
        "disabled" | "plain" | false            | "nullId"  | null
    }

    def "Should throw error on registerLogin with bad login"() {
        setup:
        provider.configurationService = Mock(ConfigurationService) {
            0 * getBoolean(UserService.SESSION_ID_ENABLED, false) >> false
        }
        provider.featureService = Mock(FeatureService){
            featurePresent(Features.CASE_INSENSITIVE_USERNAME) >> false
        }
        provider.frameworkService = Mock(FrameworkService) {
            getServerHostname() >> { "server" }
        }
        when:
        provider.registerLogin("~name", "sessionId")
        then:
        DataAccessException e = thrown()
    }

    @Unroll
    def "Should registerLogout"() {
        given:
        User savedUser = new User(login: "saved")
        savedUser.save()
        when:
        User user = provider.registerLogout(login)
        then:
        user.getLastLogout()
        where:
        login   | _
        "login" | _
        "saved" | _
    }

    def "Should throw error on registerLogout with bad login"() {
        when:
        provider.registerLogout("~name")
        then:
        DataAccessException e = thrown()
    }

    @Unroll
    def "Should update profile"() {
        given:
        User savedUser = new User(login: "saved")
        savedUser.save()
        when:
        provider.updateUserProfile(login, lastName, firstName, email)
        then:
        User user = User.findByLogin(login)
        user.id
        user.getLogin() == login
        user.getLastName() == lastName
        user.getFirstName() == firstName
        user.getEmail() == email
        where:
        login   | lastName | firstName | email
        "login" | "last"   | "first"   | "email@company.com"
        "saved" | "last2"  | "first2"  | "email2@company.com"
    }

    @Unroll
    def "Should update profile instead of creating a new user"() {
        given:
        provider.featureService = Mock(FeatureService){
            featurePresent(Features.CASE_INSENSITIVE_USERNAME) >> true
        }
        User savedUser = new User(login: "user1")
        savedUser.save()
        def login = "USER1"
        def lastName = "last1"
        def firstName = "first1"
        def email = "email1@company.com"

        when:
        provider.updateUserProfile(login, lastName, firstName, email)
        then:
        provider.findAll().size() == 1
    }

    @Unroll
    def "Should update profile caseSensitive create a new user"() {
        given:
        User savedUser = new User(login: "user1", lastName: "otherLastName", firstName: "otherFirstName", email: "otherEmail@company.com")
        savedUser.save()
        def login = "USER1"
        def lastName = "last1"
        def firstName = "first1"
        def email = "email1@company.com"
        provider.featureService = Mock(FeatureService){
            featurePresent(Features.CASE_INSENSITIVE_USERNAME) >> false
        }
        when:
        provider.updateUserProfile(login, lastName, firstName, email)
        then:
        def firstUser = User.findByLogin("user1")
        firstUser.firstName == "otherFirstName"
        firstUser.lastName == "otherLastName"
        firstUser.email == "otherEmail@company.com"
        def userCreatedAfterUpdate = User.findByLogin(login)
        userCreatedAfterUpdate.firstName == firstName
        userCreatedAfterUpdate.lastName == lastName
        userCreatedAfterUpdate.email == email
        provider.findAll().size() == 2
    }

    def "Should throw error on updateUserProfile with bad login"() {
        when:
        provider.updateUserProfile("~name", "last", "first", "email@company.com")
        then:
        DataAccessException e = thrown()
    }

    @Unroll
    def "Should create user with profile"() {
        given:
        User savedUser = new User(login: "saved")
        savedUser.save()
        when:
        provider.createUserWithProfile(login, lastName, firstName, email)
        then:
        User.countByLogin(login) == userCount
        User user = User.findByLoginAndIdNotEqual(login, savedUser.id)
        user.id
        user.getLogin() == login
        user.getLastName() == lastName
        user.getFirstName() == firstName
        user.getEmail() == email
        where:
        login   | lastName | firstName | userCount | email
        "login" | "last"   | "first"   | 1         | "email@company.com"
        "saved" | "last2"  | "first2"  | 2         | "email2@company.com"
    }

    @Unroll
    def "Should getLoginStatus with execTime"() {
        setup:
        String login = "theusername"
        provider.findOrCreateUser(login)

        provider.configurationService = Mock(ConfigurationService) {
            getInteger(GormUserDataProvider.SESSION_ABANDONED_MINUTES, _) >> timeout
        }
        when:
        User user = User.findByLogin(login)
        user.lastLogin = lastLogin
        user.lastLogout = logout
        user.save(flush: true)
        String loginStatus = provider.getLoginStatus(user)
        then:
        loginStatus
        loginStatus == expect.getValue()
        where:
        execTime   | lastLogin                                        | logout           | timeout | expect
        null       | null                                             | null             | 30      | LoginStatus.NOTLOGGED
        null       | (new Date(System.currentTimeMillis() - 1000000)) | null             | 15      | LoginStatus.ABANDONED
        null       | (new Date() - 1)                                 | null             | 30      | LoginStatus.ABANDONED
        null       | (new Date() - 1)                                 | null             | 7200    | LoginStatus.LOGGEDIN
        null       | (new Date() - 2)                                 | (new Date() - 1) | 7200    | LoginStatus.LOGGEDOUT
        new Date() | (new Date() - 3)                                 | null             | 30      | LoginStatus.ABANDONED
        new Date() | null                                             | null             | 30      | LoginStatus.NOTLOGGED
    }

    @Unroll
    def "Should findWithFilters"() {
        given:
        def userToSearch = 'admin'
        def lastSessionId = 'exampleSessionId01'
        User u = new User(login: userToSearch, lastSessionId: lastSessionId, lastLogin: new Date(), lastLogout: new Date() - 1)
        u.save()
        User u2 = new User(login: userToSearch + '_other', lastSessionId: lastSessionId, lastLogin: new Date(), lastLogout: new Date() + 1)
        u2.save()

        provider.configurationService = Mock(ConfigurationService) {
            getInteger(_, _) >> { it[1] }
            getBoolean(UserService.SHOW_LOGIN_STATUS, false) >> showLoginStatus
        }
        when:
        def result = provider.findWithFilters(loggedInOnly, filters, 0, 100)
        then:
        result.totalRecords == expect
        result.users.size() == expect
        where:
        showLoginStatus | loggedInOnly | filters                | expect
        true            | true         | [login: "admin_other"] | 0
        true            | true         | [:]                    | 1
        false           | true         | [login: "admin"]       | 1
        false           | true         | [:]                    | 2
        false           | false        | [:]                    | 2
        true            | false        | [login: "admin"]       | 1
    }

    @Unroll
    def "Should validate user exists"() {
        given:
        User savedUser = new User(login: login)
        if(persitUser){
            savedUser.save()
        }
        provider.featureService = Mock(FeatureService){
            1 * featurePresent(Features.CASE_INSENSITIVE_USERNAME) >> caseSensitive
        }
        when:
        Boolean userExists = provider.validateUserExists(login.toUpperCase())
        then:
        userExists == expect
        where:
        login   | expect   | caseSensitive | persitUser
        "user1" | false    | false         |    true
        "user2" | true     | true          |    true
        "user3" | false    | true          |    false
        "user4" | false    | false         |    false

    }

    def "Should list all by order by login"() {
        given:
        User u = new User(login: "key")
        u.save()
        User u2 = new User(login: "user")
        u2.save()
        User u3 = new User(login: 'admin')
        u3.save()
        when:
        def result = provider.listAllOrderByLogin()
        then:
        result.size() == 3
        result.get(0).getLogin() == "admin"
        result.get(1).getLogin() == "key"
        result.get(2).getLogin() == "user"
    }

    def "Should find all"() {
        given:
        User u = new User(login: "key")
        u.save()
        User u2 = new User(login: "user")
        u2.save()
        User u3 = new User(login: 'admin')
        u3.save()
        when:
        def result = provider.findAll()
        then:
        result.size() == 3
        result.get(0).getLogin() == "key"
        result.get(1).getLogin() == "user"
        result.get(2).getLogin() == "admin"
    }

    def "Should find user by login"() {
        given:
        User u = new User(login: "user")
        u.save(flush: true)
        when:
        def result = provider.findByLogin(login)
        then:
        !result == !found
        where:
        login   | found
        "user"  | true
        "admin" | false
    }

    def "Should build user"() {
        given:
        User u = new User(login: "user")
        u.save()
        when:
        def result = provider.buildUser(login)
        then:
        !result.id
        where:
        login   | _
        "user"  | _
        "admin" | _
    }

    def "Test isLoginNameCaseSensitiveEnabled()"() {
        given:
        provider.featureService = Mock(FeatureService){
            featurePresent(Features.CASE_INSENSITIVE_USERNAME) >> expectedEnabled
        }
        when:
        Boolean enabled = provider.isLoginNameCaseInsensitiveEnabled()
        then:
        enabled == expectedEnabled
        where:
        expectedEnabled << [true, false]
    }
}
