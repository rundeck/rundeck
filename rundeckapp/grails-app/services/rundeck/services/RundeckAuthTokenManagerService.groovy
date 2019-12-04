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
    boolean updateAuthRoles(final String token, final Set<String> roleSet) {
        AuthToken authToken = AuthToken.findByToken(token)
        if (!authToken) {
            return false
        }
        authToken.authRoles = AuthToken.generateAuthRoles(roleSet)
        try {
            authToken.save(failOnError: true)
            return true
        } catch(Exception ex) {
            log.error("Save token ${token} failed:",ex)
        }
        return false
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
    ) {
        if (AuthToken.findByToken(token)) {
            return updateAuthRoles(token,roleSet)
        }

        try {
            apiService.createUserToken(authContext, 0, token, user, roleSet, false, true)
        } catch(Exception ex) {
            log.error("Unable to import token", ex)
        }
        return false
    }
}
