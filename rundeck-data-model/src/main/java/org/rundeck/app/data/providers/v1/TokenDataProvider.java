package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.AuthenticationToken;
import org.rundeck.spi.data.DataAccessException;

import java.util.Date;
import java.util.List;

public interface TokenDataProvider {
    AuthenticationToken getData(String id);
    String create(AuthenticationToken data);
    String createWithId(String id, AuthenticationToken data);
    void update(String id, AuthenticationToken data);
    void delete(final String id) throws DataAccessException;
    List<AuthenticationToken> findAllByCreator(String creator);
    List<AuthenticationToken> findAllByCreatorAndExpirationLessThan(String creator, Date now);
    List<AuthenticationToken> findAllByExpirationLessThan(Date now);
    AuthenticationToken findByUuidAndCreator(String uuid, String creator);
    AuthenticationToken findByTokenAndType(String token, AuthenticationToken.AuthTokenType type);
    AuthenticationToken tokenLookup(String token);
    AuthenticationToken tokenLookupWithType(String token, AuthenticationToken.AuthTokenType type);
    AuthenticationToken findByTokenAndCreator(final String token, String creator) ;
    List<AuthenticationToken> list();

    }
