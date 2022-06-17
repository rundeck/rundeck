package rundeck.services

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenManager
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import com.dtolabs.rundeck.core.authentication.tokens.AuthenticationToken
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import org.rundeck.app.data.tokens.v1.Token
import org.rundeck.app.data.tokens.v1.TokenImpl
import org.rundeck.spi.data.DataManager
import org.rundeck.spi.data.DataProvider
import org.rundeck.spi.data.DataType
import rundeck.AuthToken

@Transactional
@GrailsCompileStatic
class RundeckAuthTokenManagerService implements AuthTokenManager {

    ApiService apiService
    DataManager rundeckDataManager

    private DataProvider<Token, DataType<Token>> getProvider() {
        rundeckDataManager.getProviderForType(Token)
    }

    @Override
    AuthenticationToken getToken(final String token) {
        def data = provider.getData(token)
        return new Wrap(data: data)
    }


    static class Wrap implements AuthenticationToken {
        Token data

        @Override
        String getToken() {
            return data.token
        }

        @Override
        Set<String> authRolesSet() {
            return data.getAuthRolesSet()
        }

        @Override
        String getUuid() {
            return data.uuid
        }

        @Override
        String getCreator() {
            return data.creator
        }

        @Override
        String getOwnerName() {
            return data.ownerName
        }

        @Override
        AuthTokenType getType() {
            return AuthTokenType.valueOf(data.type.toString())
        }

        @Override
        String getPrintableToken() {
            return data.printableToken
        }

        @Override
        Date getExpiration() {
            return data.expiration
        }

        @Override
        String getName() {
            return data.name
        }
    }

    @Override
    boolean updateAuthRoles(UserAndRolesAuthContext authContext, final String token, final Set<String> roleSet)
        throws Exception {
        def token1 = provider.getData(token)
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
            provider.update(token1.uuid, newToken)
            return true
        } catch (Exception ex) {
            log.error("Save token ${token} failed:", ex)
            throw ex
        }
    }

    @Override
    boolean deleteToken(final String token) {
        try {
            provider.delete(token)
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
