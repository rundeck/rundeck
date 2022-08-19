package org.rundeck.app.data.providers

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenMode
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import grails.compiler.GrailsCompileStatic
import groovy.util.logging.Slf4j
import org.rundeck.app.data.model.v1.Token
import org.rundeck.app.data.model.v1.TokenDataType
import org.rundeck.app.data.model.v1.TokenImpl
import org.rundeck.app.data.providers.v1.TokenDataProvider
import org.rundeck.spi.data.DataAccessException
import org.rundeck.spi.data.DataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import rundeck.AuthToken
import rundeck.User
import rundeck.services.UserService
import rundeck.services.data.AuthTokenDataService
import org.modelmapper.ModelMapper;

@GrailsCompileStatic
@Slf4j
class GormTokenDataProvider implements TokenDataProvider {
    @Autowired
    AuthTokenDataService authTokenDataService
    @Autowired
    UserService userService
    @Autowired
    MessageSource messageSource

    TokenDataType dataType = new TokenDataType()
    private static final ModelMapper modelMapper = new ModelMapper();

    @Override
    Token getData (final String id) {
        def authToken = authTokenDataService.getByUuid(id)
        return authToken ? convertAuthTokenToToken(authToken): null
    }

    @Override
    String create( final Token data) {
        return createWithId(
                data.uuid ?: UUID.randomUUID().toString(),
                data
        )
    }

    @Override
    String createWithId( final String id, final Token data) {
        User tokenOwner = userService.findOrCreateUser(data.ownerName)
        if (!tokenOwner) {
            throw new Exception("Couldn't find user: ${data.ownerName}")
        }

        def tokenType = AuthTokenType.valueOf(data.type.toString())

        AuthToken token = new AuthToken(
                token: data.token,
                authRoles: AuthToken.generateAuthRoles(data.getAuthRolesSet()),
                user: tokenOwner,
                expiration: data.expiration,
                uuid: id,
                creator: data.creator,
                name: data.name,
                type: tokenType,
                tokenMode: (tokenType == AuthTokenType.WEBHOOK) ? AuthTokenMode.LEGACY : AuthTokenMode.SECURED
        )
        if (token.save(flush: true)) {
            return token.uuid
        } else {
            log.warn(token.errors.allErrors.collect { messageSource.getMessage(it, null) }.join(","))
            throw new DataAccessException("Failed to save token for User ${tokenOwner.login}")
        }
    }

    @Override
    void update(final String id, final Token data) {
        def token = authTokenDataService.getByUuid(id)
        if (!token) {
            throw new DataAccessException("Not found: token with ID: ${id}")
        }
        token.name = data.name
        token.setAuthRoles(AuthToken.generateAuthRoles(data.authRolesSet))

        try {
            token.save(failOnError: true)
        } catch (Exception e) {
            throw new DataAccessException("Error: could not update token ${id}: ${e}", e)
        }
    }

    @Override
    void delete(final String id) throws DataAccessException {
        def token = authTokenDataService.getByUuid(id)
        if (!token) {
            throw new DataAccessException("Not found: token with ID: ${id}")
        }
        try {
            authTokenDataService.deleteByUuid(id)
        } catch (Exception e) {
            throw new DataAccessException("Could not delete token ${id}: ${e}", e)
        }
    }

    @Override
    List<Token> findAllByCreator(String creator) {
        List tokens = []
        List<AuthToken> authTokens = AuthToken.findAllByCreator(creator)
        authTokens.each {authToken->
            tokens << convertAuthTokenToToken(authToken)
        }
        tokens
    }

    @Override
    Token findByUuidAndCreator(String id, String creator) {
        def authToken = AuthToken.findByUuidAndCreator(id, creator)
        return authToken ? convertAuthTokenToToken(authToken): null

    }
    
    @Override
    List<Token> findAllByCreatorAndExpirationLessThan(final String creator, final Date now) {
        List tokens = []
        List<AuthToken> authTokens = AuthToken.findAllByCreatorAndExpirationLessThan(creator, now)
        authTokens.each {authToken->
            tokens << convertAuthTokenToToken(authToken)
        }
        tokens
    }

    @Override
    List<Token> findAllByExpirationLessThan(final Date now) {
        List tokens = []
        List<AuthToken> authTokens = AuthToken.findAllByExpirationLessThan(now)
        authTokens.each {authToken->
            tokens << convertAuthTokenToToken(authToken)
        }
        tokens
    }
    @Override
    Token findByTokenAndType(final String token, Token.AuthTokenType type) {
        def tokenType = AuthTokenType.valueOf(type.toString())
        def authToken = AuthToken.findByTokenAndType(token, tokenType)
        return authToken ? convertAuthTokenToToken(authToken): null

    }

    Token tokenLookup(final String token){
        def authToken = AuthToken.tokenLookup(token);
        return authToken ? convertAuthTokenToToken(authToken): null

    }
    private Token convertAuthTokenToToken(AuthToken authToken){
        TokenImpl token = modelMapper.map(authToken, TokenImpl.class)
        token.setAuthRolesSet(authToken.authRolesSet())
        token
    }

}
