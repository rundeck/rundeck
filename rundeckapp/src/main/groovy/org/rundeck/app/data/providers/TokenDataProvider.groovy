package org.rundeck.app.data.providers


import grails.compiler.GrailsCompileStatic
import groovy.util.logging.Slf4j
import org.rundeck.spi.data.DataAccessException
import org.rundeck.spi.data.DataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import rundeck.AuthToken
import rundeck.services.UserService
import rundeck.services.data.AuthTokenDataService

@GrailsCompileStatic
@Slf4j
class TokenDataProvider implements DataProvider<AuthToken> {
    @Autowired
    AuthTokenDataService authTokenDataService
    @Autowired
    UserService userService
    @Autowired
    MessageSource messageSource

    AuthToken getData (final String id) {
        def token = authTokenDataService.getByUuid(id)
        return token ?: null
    }

    @Override
    AuthToken get(Serializable id) {
        def token = authTokenDataService.get(id)
        return token ?: null
    }

    @Override
    AuthToken create(AuthToken token){
        if (token.save(flush:true)) {
            log.info(
                    "GENERATE TOKEN: ID:${token.uuid} creator:${token.creator} username:${token.user.login} roles:"
                            + "${token.authRoles} expiration:${token.expiration}"
            )
            return token
        } else {
            log.warn(token.errors.allErrors.collect { messageSource.getMessage(it, null) }.join(","))
            throw new DataAccessException("Failed to save token for User ${token.user.login}")
        }
    }

    @Override
    void update(final Serializable id, final AuthToken data) {
        def token = authTokenDataService.get(id)
        if (!token) {
            throw new DataAccessException("Not found: token with ID: ${id}")
        }
        token.name = data.name
        try {
            token.save(failOnError: true)
        } catch (Exception e) {
            throw new DataAccessException("Error: could not update token ${id}: ${e}", e)
        }
    }

    void updateByUuid(final String uuid, final AuthToken data) {
        def token = authTokenDataService.getByUuid(uuid)
        if (!token) {
            throw new DataAccessException("Not found: token with ID: ${uuid}")
        }
        token.name = data.name
        try {
            token.save(failOnError: true)
        } catch (Exception e) {
            throw new DataAccessException("Error: could not update token ${uuid}: ${e}", e)
        }
    }

    void deleteByUuid(final String id) throws DataAccessException {
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
    void delete(AuthToken authToken) throws DataAccessException {
        try {
            authTokenDataService.delete(authToken.id)

        } catch (Exception e) {
            throw new DataAccessException("Could not delete token ${authToken.id}: ${e}", e)
        }
    }

    List<AuthToken> findAllByCreator(String creator) {
        AuthToken.findAllByCreator(creator)
    }

    AuthToken findUserTokenId(String creator, String id) {
        AuthToken.findByUuidAndCreator(id, creator)
    }

    List<AuthToken> findAllByCreatorAndExpirationLessThan(String creator, Date now) {
        AuthToken.findAllByCreatorAndExpirationLessThan(creator, now)
    }

    List<AuthToken> findAllByExpirationLessThan(Date now) {
        AuthToken.findAllByExpirationLessThan(now)
    }

}
