package rundeck.services

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenManager
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import com.dtolabs.rundeck.core.authentication.tokens.AuthenticationToken
import com.dtolabs.rundeck.core.authentication.tokens.SimpleTokenBuilder
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import org.rundeck.app.data.model.v1.Token
import org.rundeck.app.data.model.v1.TokenImpl
import org.rundeck.spi.data.DataManager
import rundeck.AuthToken
import org.rundeck.app.data.providers.GormTokenDataProvider

@Transactional
@GrailsCompileStatic
class RundeckAuthTokenManagerService implements AuthTokenManager {

    ApiService apiService
    DataManager rundeckDataManager

    private GormTokenDataProvider getTokenProvider() {
        (GormTokenDataProvider)rundeckDataManager.getProviderForType(GormTokenDataProvider.class.getSimpleName())
    }

    @Override
    AuthenticationToken getToken(final String token) {
        def data = tokenProvider.getData(token)
        return new SimpleTokenBuilder()
                .setToken(token)
                .setCreator(data.creator)
                .setOwnerName(data.ownerName)
                .setAuthRolesSet(data.getAuthRolesSet())
                .setExpiration(data.expiration)
                .setType(AuthTokenType.valueOf(data.type.toString()))
                .setName(data.name)
    }


    @Override
    boolean updateAuthRoles(UserAndRolesAuthContext authContext, final String token, final Set<String> roleSet)
        throws Exception {
        Token token1 = tokenProvider.getData(token)
        if (!token1) {
            return false
        }
        def username = token1.creator
        def check = apiService.checkTokenAuthorization(authContext, username, roleSet)
        if (!check.authorized) {
            throw new Exception("Unauthorized: modify token roles for ${token1.uuid} failed: ${check.message}")
        }
        TokenImpl newToken = TokenImpl.with(token1)

        newToken.authRolesSet = check.roles
         try {
            tokenProvider.update(token1.uuid, token1)
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
        org.rundeck.app.data.model.v1.Token.AuthTokenType webhookToken =
                             org.rundeck.app.data.model.v1.Token.AuthTokenType.valueOf(AuthTokenType.WEBHOOK.toString())

        if (tokenProvider.findByTokenAndType(token, webhookToken)) {
            return updateAuthRoles(authContext, token, roleSet)
        }

        apiService.createUserToken(authContext, 0, token, user, roleSet, false, AuthTokenType.WEBHOOK)

        return true
    }
}
