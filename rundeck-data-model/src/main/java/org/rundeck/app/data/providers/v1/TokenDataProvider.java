package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.AuthenticationToken;
import org.rundeck.spi.data.DataAccessException;

import java.util.Date;
import java.util.List;

/**
 * TokenDataProvider defines a base set of AuthenticationToken datastore methods.
 *
 */
public interface TokenDataProvider {
    /**
     * Retrieves an AuthenticationToken based on the id/uuid provided.
     *
     * @param id of the AuthenticationToken, format String
     * @return AuthenticationToken if found, otherwise null
     */
    AuthenticationToken getData(String id);

    /**
     * Creates an AuthenticationToken with a generated id
     *
     * @param data AuthenticationToken attributes
     *
     * @return id of the created AuthenticationToken
     * @throws DataAccessException on error
     */
    String create(AuthenticationToken data) throws DataAccessException;

    /**
     * Creates an AuthenticationToken with the supplied id
     *
     * @param data AuthenticationToken attributes
     * @param id id
     * @return id of the created AuthenticationToken
     * @throws DataAccessException on error
     */
    String createWithId(String id, AuthenticationToken data) throws DataAccessException;

    /**
     * Updates an AuthenticationToken with the supplied attributes
     *
     * @param data AuthenticationToken attributes
     * @param id id
     * @throws DataAccessException on error
     */
    void update(String id, AuthenticationToken data) throws DataAccessException;

    /**
     * Removes an AuthenticationToken
     *
     * @param id AuthenticationToken id
     * @throws DataAccessException on error
     */
    void delete(final String id) throws DataAccessException;

    /**
     * Retrieves a List of AuthenticationToken for the creator.
     *
     * @param creator of the AuthenticationTokens, format String
     * @return list of AuthenticationTokens
     */
    List<AuthenticationToken> findAllByCreator(String creator);

    /**
     * Retrieves a List of AuthenticationTokens for the creator that are expired.
     *
     * @param creator of the AuthenticationTokens, format String
     * @param now date, format; java.util.Date
     * @return list of AuthenticationTokens
     */
    List<AuthenticationToken> findAllByCreatorAndExpirationLessThan(String creator, Date now);

    /**
     * Retrieves a List of All AuthenticationTokens that are expired.
     *
     * @param now date, format; java.util.Date
     * @return list of AuthenticationTokens
     */
    List<AuthenticationToken> findAllByExpirationLessThan(Date now);

    /**
     * Retrieves an AuthenticationToken by uuid and creator.
     *
     * @param uuid, format; String
     * @param creator of the AuthenticationTokens, format String
     * @return authenticationToken if found, otherwise null
     */
    AuthenticationToken findByUuidAndCreator(String uuid, String creator);

    /**
     * Retrieves an AuthenticationToken by token and type.
     *
     * @param token, format; String
     * @param type of the AuthenticationToken, format AuthTokenType
     * @return authenticationToken if found, otherwise null
     */
    AuthenticationToken findByTokenAndType(String token, AuthenticationToken.AuthTokenType type);

    /**
     * Finds a user token from the provided value.
     *
     * @param token, format; String
     * @return authenticationToken if found, otherwise null
     */
    AuthenticationToken tokenLookup(String token);

    /**
     * Finds a token from the provided value and type.
     *
     * @param token, format; String
     * @param type of the AuthenticationToken, format AuthTokenType
     * @return authenticationToken if found, otherwise null
     */
    AuthenticationToken tokenLookupWithType(String token, AuthenticationToken.AuthTokenType type);

    /**
     * Retrieves an AuthenticationToken by token and creator.
     *
     * @param token, format; String
     * @param creator of the AuthenticationTokens, format String
     * @return authenticationToken if found, otherwise null
     */

    AuthenticationToken findByTokenAndCreator(final String token, String creator) ;
    /**
     * Retrieves a List of All AuthenticationTokens.
     *
     * @return list of AuthenticationTokens
     */
    List<AuthenticationToken> list();

    }
