package rundeck.services

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenManager
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import com.dtolabs.rundeck.core.authentication.tokens.AuthenticationToken
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import org.rundeck.spi.data.DataManager
import org.rundeck.spi.data.DataProvider
import rundeck.AuthToken
import org.rundeck.app.data.providers.TokenDataProvider

@Transactional
@GrailsCompileStatic
class RundeckAuthTokenManagerService implements AuthTokenManager {

    ApiService apiService
    DataManager rundeckDataManager

    private TokenDataProvider getTokenProvider() {
        (TokenDataProvider)rundeckDataManager.getProviderForType(TokenDataProvider.class.getSimpleName())
    }

    @Override
    AuthenticationToken getToken(final String token) {
        def data = tokenProvider.getData(token)
        return data
    }


    @Override
    boolean updateAuthRoles(UserAndRolesAuthContext authContext, final String token, final Set<String> roleSet)
        throws Exception {
        AuthToken token1 = tokenProvider.getData(token)
        if (!token1) {
            return false
        }
        def username = token1.user.login
        def check = apiService.checkTokenAuthorization(authContext, username, roleSet)
        if (!check.authorized) {
            throw new Exception("Unauthorized: modify token roles for ${token1.uuid} failed: ${check.message}")
        }
        token1.authRoles = AuthToken.generateAuthRoles(check.roles)
        try {
            tokenProvider.updateByUuid(token1.uuid, token1)
            return true
        } catch (Exception ex) {
            log.error("Save token ${token} failed:", ex)
            throw ex
        }
    }

    @Override
    boolean deleteToken(final String token) {
        try {
            tokenProvider.deleteByUuid(token)
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
