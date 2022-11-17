package org.rundeck.app.data.providers


import grails.compiler.GrailsCompileStatic
import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.hibernate.StaleStateException
import org.rundeck.app.data.model.v1.user.LoginStatus
import org.rundeck.app.data.model.v1.user.RdUser
import org.rundeck.app.data.model.v1.user.dto.SaveUserResponse
import org.rundeck.app.data.model.v1.user.dto.UserFilteredResponse
import org.rundeck.app.data.providers.v1.UserDataProvider
import org.rundeck.spi.data.DataAccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.validation.Errors
import rundeck.NodeFilter
import rundeck.User
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import rundeck.services.data.UserDataService

import javax.transaction.Transactional

@GrailsCompileStatic
@Slf4j
@Transactional
class GormUserDataProvider implements UserDataProvider {
    @Autowired
    UserDataService userDataService
    @Autowired
    FrameworkService frameworkService
    @Autowired
    ConfigurationService configurationService

    public static final String SESSION_ID_ENABLED = 'userService.login.track.sessionId.enabled'
    public static final String SESSION_ID_METHOD = 'userService.login.track.sessionId.method'
    public static final int DEFAULT_TIMEOUT = 30
    public static final String SESSION_ABANDONED_MINUTES = 'userService.login.track.sessionAbandoned'
    public static final String SHOW_LOGIN_STATUS = 'gui.userSummaryShowLoginStatus'

    @Override
    User findOrCreateUser(String login) throws DataAccessException {
        User user = User.findByLogin(login)
        if (!user) {
            User newUser = new User(login: login)
            if (!newUser.save(flush: true)) {
                throw new DataAccessException("unable to save user: ${login}")
            }
            user = newUser
        }
        return user
    }

    static User getUserByLoginOrCreate(String login) {
        User user = User.findByLogin(login)
        if (!user) {
            user = new User(login: login)
        }
        return user
    }

    @Override
    User registerLogin(String login, String sessionId) throws DataAccessException {
        User user = getUserByLoginOrCreate(login)
        user.lastLogin = new Date()
        user.lastLoggedHostName = frameworkService.getServerHostname()
        user.lastSessionId = null
        if (isSessionIdRegisterEnabled()) {
            user.lastSessionId = (sessionIdRegisterMethod == 'plain') ? sessionId : sessionId.encodeAsSHA256()
        }
        try {
            if (!user.save(flush: true)) {
                throw new DataAccessException("unable to save user: ${user}, ${user.errors.allErrors.join(',')}")
            }
            return user
        } catch (StaleStateException exception) {
            log.warn("registerLogin: for ${login}, caught StaleStateException: $exception")
            return null
        } catch (OptimisticLockingFailureException exception) {
            log.warn("registerLogin: for ${login}, caught OptimisticLockingFailureException: $exception")
            return null
        }
    }

    @Override
    User registerLogout(String login) throws DataAccessException {
        User user = getUserByLoginOrCreate(login)
        user.lastLogout = new Date()
        if (!user.save(flush: true)) {
            throw new DataAccessException("unable to save user: ${user}, ${user.errors.allErrors.join(',')}")
        }
        return user
    }

    @Override
    SaveUserResponse updateUserProfile(String username, String lastName, String firstName, String email) {
        User u = findOrCreateUser(username)
        u.setFirstName(firstName)
        u.setLastName(lastName)
        u.setEmail(email)
        Boolean isUpdated = u.save(flush: true)
        Errors errors = u.errors
        return new SaveUserResponse(user: u, isSaved: isUpdated, errors: errors)
    }

    @Override
    SaveUserResponse createUserWithProfile(String login, String lastName, String firstName, String email) {
        User u = new User(login: login, firstName: firstName, lastName: lastName, email: email)
        Boolean isUpdated = u.save(flush: true)
        Errors errors = u.errors
        return new SaveUserResponse(user: u, isSaved: isUpdated, errors: errors)
    }

    @Override
    String getLoginStatus(RdUser user) {
        String status = LoginStatus.NOTLOGGED.value
        if (user) {
            Date lastDate = user.lastLogin
            if (lastDate != null) {
                int minutes = configurationService.getInteger(SESSION_ABANDONED_MINUTES, DEFAULT_TIMEOUT)
                Calendar calendar = Calendar.getInstance()
                calendar.setTime(lastDate)
                calendar.add(Calendar.MINUTE, minutes)
                if (user.lastLogout != null) {
                    if (lastDate.after(user.lastLogout)) {
                        if (calendar.getTime().before(new Date())) {
                            status = LoginStatus.ABANDONED.value
                        } else {
                            status = LoginStatus.LOGGEDIN.value
                        }
                    } else {
                        status = LoginStatus.LOGGEDOUT.value
                    }
                } else if (calendar.getTime().after(new Date())) {
                    status = LoginStatus.LOGGEDIN.value
                } else {
                    status = LoginStatus.ABANDONED.value
                }
            } else {
                status = LoginStatus.NOTLOGGED.value
            }
        }
        return status
    }

    @Override
    UserFilteredResponse findWithFilters(boolean loggedInOnly, HashMap<String, String> filters, Integer offset, Integer max) {
        int timeOutMinutes = configurationService.getInteger(SESSION_ABANDONED_MINUTES, DEFAULT_TIMEOUT)
        boolean showLoginStatus = configurationService.getBoolean(SHOW_LOGIN_STATUS, false)
        Calendar calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -timeOutMinutes)

        Integer totalRecords = new DetachedCriteria(User).build {
            if (showLoginStatus && loggedInOnly) {
                or {
                    and {
                        isNotNull("lastLogin")
                        isNotNull("lastLogout")
                        gtProperty("lastLogin", "lastLogout")
                        gt("lastLogin", calendar.getTime())
                    }
                    and {
                        isNotNull("lastLogin")
                        isNull("lastLogout")
                        gt("lastLogin", calendar.getTime())
                    }
                }
            }
            if (filters) {
                filters.each { k, v ->
                    eq(k, v)
                }
            }

        }.count() as Integer

        List<RdUser> users = []
        if (totalRecords > 0) {
            users = User.createCriteria().list(max: max, offset: offset) {
                if (showLoginStatus && loggedInOnly) {
                    or {
                        and {
                            isNotNull("lastLogin")
                            isNotNull("lastLogout")
                            gtProperty("lastLogin", "lastLogout")
                            gt("lastLogin", calendar.getTime())
                        }
                        and {
                            isNotNull("lastLogin")
                            isNull("lastLogout")
                            gt("lastLogin", calendar.getTime())
                        }
                    }
                }

                if (filters) {
                    filters.each { k, v ->
                        eq(k, v)
                    }
                }
                order("login", "asc")
            } as List<RdUser>
        }
        def response = new UserFilteredResponse()
        response.setTotalRecords(totalRecords)
        response.setUsers(users)
        response.setShowLoginStatus(showLoginStatus)
        return response
    }

    @Override
    boolean validateUserExists(String username) {
        return User.countByLogin(username) > 0
    }

    @Override
    List<RdUser> listAllOrderByLogin() {
        List<RdUser> response = User.listOrderByLogin() as List<RdUser>
        return response
    }

    @Override
    List<RdUser> findAll() {
        List<RdUser> response = User.findAll() as List<RdUser>
        return response
    }

    @Override
    RdUser findByLogin(String login) {
        return User.findByLogin(login)
    }

    @Override
    RdUser buildUser(String login) {
        return new User(login: login)
    }

    @Override
    SaveUserResponse updateFilterPref(String login, String filterPref) {
        User user = User.findByLogin(login)
        user.filterPref = filterPref
        Boolean isSaved = user.save()
        return new SaveUserResponse(user: user, isSaved: isSaved, errors: user.errors)
    }

    @Override
    String getEmailWithNewSession(String login) {
        if (!login) { return "" }
        User.withNewSession {
            def userLogin = User.findByLogin(login)
            if (!userLogin || !userLogin.email) { return "" }
            return userLogin.email
        }
    }

    @Override
    RdUser buildUser() {
        return new User()
    }

    /**
     * It looks for property to enable session id related data to be stored at DB.
     * @return boolean
     */
    def isSessionIdRegisterEnabled() {
        configurationService.getBoolean(SESSION_ID_ENABLED, false)
    }

    /**
     * It looks for property that set method for session id to be stored at DB.
     * @return string
     */
    def getSessionIdRegisterMethod() {
        configurationService.getString(SESSION_ID_METHOD, 'hash')
    }
}
