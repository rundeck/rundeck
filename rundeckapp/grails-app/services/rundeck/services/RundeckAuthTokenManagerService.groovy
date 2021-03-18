package rundeck.services

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenManager
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import com.dtolabs.rundeck.core.authentication.tokens.AuthenticationToken
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.gorm.transactions.Transactional
import rundeck.AuthToken
import rundeck.User

@Transactional
class RundeckAuthTokenManagerService implements AuthTokenManager {

    ApiService apiService

    @Override
    AuthenticationToken getToken(final String token) {
        return AuthToken.findByToken(token)
    }

    @Override
    boolean updateAuthRoles(UserAndRolesAuthContext authContext, final String token, final Set<String> roleSet)
            throws Exception {
        AuthToken authToken = AuthToken.findByToken(token)
        if (!authToken) {
            return false
        }
        def username = authToken.user.login
        def check = apiService.checkTokenAuthorization(authContext, username, roleSet)
        if (!check.authorized) {
            throw new Exception("Unauthorized: modify token roles for ${authToken.uuid} failed: ${check.message}")
        }
        authToken.authRoles = AuthToken.generateAuthRoles(check.roles)
        try {
            authToken.save(failOnError: true)
            return true
        } catch (Exception ex) {
            log.error("Save token ${token} failed:", ex)
            throw ex
        }
    }

    @Override
    boolean deleteToken(final String token) {
        AuthToken authToken = AuthToken.findByToken(token)
        try {
            authToken.delete(failOnError: true)
            return true
        } catch(Exception ex) {
            log.error("Delete token ${token} failed:",ex)
        }
        return false
    }

    @Override
    Set<String> parseAuthRoles(final String authRoles) {
        return AuthToken.parseAuthRoles(authRoles)
    }

    @Override
    boolean importWebhookToken(
            UserAndRolesAuthContext authContext,
            final String token,
            final String user,
            final Set<String> roleSet
    ) throws Exception {
        if (AuthToken.tokenLookup(token)) {
            throw new Exception("Cannot import webhook token")
        }
        if (AuthToken.findByTokenAndType(token, AuthTokenType.WEBHOOK)) {
            return updateAuthRoles(authContext, token, roleSet)
        }

        apiService.createUserToken(authContext, 0, token, user, roleSet, false, AuthTokenType.WEBHOOK)

        return true
    }
}
