package org.rundeck.app.data.providers

import org.rundeck.app.data.model.v1.AuthTokenMode
import grails.compiler.GrailsCompileStatic
import groovy.util.logging.Slf4j
import org.rundeck.app.data.model.v1.AuthenticationToken
import org.rundeck.app.data.providers.v1.TokenDataProvider
import org.rundeck.spi.data.DataAccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import rundeck.AuthToken
import rundeck.User
import rundeck.services.UserService
import rundeck.services.data.AuthTokenDataService

import javax.transaction.Transactional

@GrailsCompileStatic
@Slf4j
@Transactional
class GormTokenDataProvider implements TokenDataProvider {
    @Autowired
    AuthTokenDataService authTokenDataService
    @Autowired
    UserService userService
    @Autowired
    MessageSource messageSource

    @Override
    AuthenticationToken getData (final String id) {
        AuthenticationToken authToken = authTokenDataService.getByUuid(id)
        return authToken ?: null
    }

    @Override
    String create( final AuthenticationToken data) {
        return createWithId(
                data.uuid ?: UUID.randomUUID().toString(),
                data
        )
    }

    @Override
    String createWithId( final String id, final AuthenticationToken data) {
        User tokenOwner = userService.findOrCreateUser(data.ownerName)
        if (!tokenOwner) {
            throw new Exception("Couldn't find user: ${data.ownerName}")
        }

        def tokenType = AuthenticationToken.AuthTokenType.valueOf(data.type.toString())

        AuthToken token = new AuthToken(
                token: data.token,
                authRoles: AuthenticationToken.generateAuthRoles(data.getAuthRolesSet()),
                user: tokenOwner,
                expiration: data.expiration,
                uuid: id,
                creator: data.creator,
                name: data.name,
                type: tokenType,
                tokenMode: (tokenType == AuthenticationToken.AuthTokenType.WEBHOOK) ? AuthTokenMode.LEGACY : AuthTokenMode.SECURED
        )
        if (token.save(flush: true)) {
            return token.uuid
        } else {
            log.warn(token.errors.allErrors.collect { messageSource.getMessage(it, null) }.join(","))
            throw new DataAccessException("Failed to save token for User ${tokenOwner.login}")
        }
    }

    @Override
    void update(final String id, final AuthenticationToken data) {
        def token = authTokenDataService.getByUuid(id)
        if (!token) {
            throw new DataAccessException("Not found: token with ID: ${id}")
        }
        token.name = data.name
        token.setAuthRoles(AuthToken.generateAuthRoles(data.getAuthRolesSet()))

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
    List<AuthenticationToken> findAllByCreator(String creator) {
        List<AuthenticationToken> tokens = []
        List<AuthToken> authTokens = AuthToken.findAllByCreator(creator)
        authTokens.each{authToken ->
            tokens << authToken
        }
        tokens
    }

    @Override
    AuthenticationToken findByUuidAndCreator(String id, String creator) {
        def authToken = AuthToken.findByUuidAndCreator(id, creator)
        return authToken ?: null

    }

    @Override
    List<AuthenticationToken> findAllByCreatorAndExpirationLessThan(final String creator, final Date now) {
        List<AuthenticationToken> tokens = []

        List<AuthToken> authTokens = AuthToken.findAllByCreatorAndExpirationLessThan(creator, now)
        authTokens.each{authToken ->
            tokens << authToken
        }
        tokens
    }

    @Override
    List<AuthenticationToken> findAllByExpirationLessThan(final Date now) {
        List<AuthenticationToken> tokens = []
        List<AuthToken> authTokens = AuthToken.findAllByExpirationLessThan(now)
        authTokens.each{authToken ->
            tokens << authToken
        }
        tokens

    }
    @Override
    AuthenticationToken findByTokenAndType(final String token, AuthenticationToken.AuthTokenType type) {
        def tokenType = AuthenticationToken.AuthTokenType.valueOf(type.toString())
        AuthenticationToken authToken = AuthToken.findByTokenAndType(token, tokenType)
        return authToken ?: null

    }

    @Override
    AuthenticationToken findByTokenAndCreator(final String token, String creator) {
        AuthenticationToken authToken = AuthToken.findByTokenAndCreator(token, creator)
        return authToken ?: null

    }

    @Override
    List<AuthenticationToken> list() {
        List<AuthenticationToken> tokens = []
        List<AuthToken> authTokens = AuthToken.list()
        authTokens.each{authToken ->
            tokens << authToken
        }
        tokens
    }

    @Override
    AuthenticationToken tokenLookup(final String token){
        def authToken = AuthToken.tokenLookup(token);
        return authToken ?: null

    }

    @Override
    AuthenticationToken tokenLookupWithType(final String token, AuthenticationToken.AuthTokenType type){
        def authToken = AuthToken.tokenLookup(token, type);
        return authToken ?: null

    }

}
