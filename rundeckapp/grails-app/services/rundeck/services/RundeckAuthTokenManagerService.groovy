package rundeck.services

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenManager
import org.rundeck.app.data.model.v1.AuthenticationToken
import org.rundeck.app.data.model.v1.SimpleTokenBuilder
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import org.rundeck.app.data.providers.v1.TokenDataProvider
import org.rundeck.spi.data.DataManager
import rundeck.AuthToken
import org.rundeck.app.data.providers.GormTokenDataProvider

@Transactional
@GrailsCompileStatic
class RundeckAuthTokenManagerService implements AuthTokenManager {

    ApiService apiService
    DataManager rundeckDataManager

    private TokenDataProvider getTokenProvider() {
        rundeckDataManager.getProviderForType(TokenDataProvider)
    }

    @Override
    AuthenticationToken getToken(final String token) {
        return tokenProvider.getData(token)
    }


    @Override
    boolean updateAuthRoles(UserAndRolesAuthContext authContext, final String token, final Set<String> roleSet)
        throws Exception {
        AuthenticationToken token1 = tokenProvider.findByTokenAndType(token, AuthenticationToken.AuthTokenType.WEBHOOK)
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
            tokenProvider.update(token1.uuid, newToken)
            return true
        } catch (Exception ex) {
            log.error("Save token ${token} failed:", ex)
            throw ex
        }
    }

    @Override
    boolean deleteToken(final String token) {
        try {
            tokenProvider.delete(token)
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
        if (tokenProvider.tokenLookup(token)) {
            throw new Exception("Cannot import webhook token")
        }
        AuthenticationToken webookToken = tokenProvider.findByTokenAndType(token, AuthenticationToken.AuthTokenType.WEBHOOK)
        if (tokenProvider.findByTokenAndType(token, AuthenticationToken.AuthTokenType.WEBHOOK)) {
            return updateAuthRoles(authContext, token, roleSet)
        }

        apiService.createUserToken(authContext, 0, token, user, roleSet, false, AuthenticationToken.AuthTokenType.WEBHOOK)

        return true
    }
}
