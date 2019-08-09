package rundeck.services

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenManager
import com.dtolabs.rundeck.core.authentication.tokens.AuthenticationToken
import grails.gorm.transactions.Transactional
import rundeck.AuthToken

@Transactional
class RundeckAuthTokenManagerService implements AuthTokenManager {

    @Override
    AuthenticationToken getToken(final String token) {
        return AuthToken.findByToken(token)
    }

    @Override
    boolean updateAuthRoles(final String token, final String roleString) {
        AuthToken authToken = AuthToken.findByToken(token)
        if(!authToken) return false
        authToken.authRoles = roleString
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
}
