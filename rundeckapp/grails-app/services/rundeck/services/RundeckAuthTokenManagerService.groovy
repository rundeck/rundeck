package rundeck.services

import org.rundeck.app.util.spi.AuthTokenManager
import org.rundeck.app.data.model.v1.AuthenticationToken
import org.rundeck.app.data.model.v1.AuthenticationToken.AuthTokenType
import org.rundeck.app.data.model.v1.SimpleTokenBuilder
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import org.rundeck.app.data.providers.v1.TokenDataProvider

@Transactional
@GrailsCompileStatic
class RundeckAuthTokenManagerService implements AuthTokenManager {

    ApiService apiService
    TokenDataProvider tokenDataProvider

    @Override
    AuthenticationToken getToken(final String token) {
        return tokenDataProvider.tokenLookup(token)
    }

    @Override
    AuthenticationToken getTokenWithType(final String token, final AuthTokenType type) {
        return tokenDataProvider.tokenLookupWithType(token,type)
    }

    @Override
    boolean updateAuthRoles(UserAndRolesAuthContext authContext, final String token, final Set<String> roleSet)
        throws Exception {
        AuthenticationToken token1 = tokenDataProvider.findByTokenAndType(token, AuthenticationToken.AuthTokenType.WEBHOOK)
        if (!token1) {
            return false
        }
        def username = token1.getOwnerName()
        def check = apiService.checkTokenAuthorization(authContext, username, roleSet)
        if (!check.authorized) {
            throw new Exception("Unauthorized: modify token roles for ${token1.uuid} failed: ${check.message}")
        }
        SimpleTokenBuilder newToken = SimpleTokenBuilder.with(token1)

        newToken.authRolesSet = check.roles
         try {
            tokenDataProvider.update(token1.uuid, newToken)
            return true
        } catch (Exception ex) {
            log.error("Save token ${token} failed:", ex)
            throw ex
        }
    }

    @Override
    boolean deleteToken(final String uuid) {
        try {
            tokenDataProvider.delete(uuid)
            return true
        } catch(Exception ex) {
            log.error("Delete token ${uuid} failed:",ex)
        }
        return false
    }

    @Override
    boolean deleteByTokenWithType(final String token, AuthTokenType type) {
        try {
            AuthenticationToken authToken = getTokenWithType(token, type)
            if(authToken) {
                tokenDataProvider.delete(authToken.getUuid())
                return true
            }
        } catch(Exception ex) {
            log.error("Delete token ${token} failed:",ex)
        }
        return false
    }
    @Override
    Set<String> parseAuthRoles(final String authRoles) {
        return AuthenticationToken.parseAuthRoles(authRoles)
    }

    @Override
    boolean importWebhookToken(
            UserAndRolesAuthContext authContext,
            final String token,
            final String user,
            final Set<String> roleSet
    ) throws Exception {
        if (tokenDataProvider.tokenLookup(token)) {
            throw new Exception("Cannot import webhook token")
        }
        AuthenticationToken webhookToken = tokenDataProvider.findByTokenAndType(token, AuthenticationToken.AuthTokenType.WEBHOOK)
        if (webhookToken) {
            return updateAuthRoles(authContext, token, roleSet)
        }

        apiService.createUserToken(authContext, 0, token, user, roleSet, false, AuthenticationToken.AuthTokenType.WEBHOOK)

        return true
    }
}
