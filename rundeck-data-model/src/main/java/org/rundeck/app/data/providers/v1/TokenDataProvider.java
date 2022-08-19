package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.Token;
import org.rundeck.spi.data.DataAccessException;

import java.util.Date;
import java.util.List;

public interface TokenDataProvider {
    Token getData(String id);
    String create(Token data);
    String createWithId(String id, Token data);
    void update(String id, Token data);
    void delete(final String id) throws DataAccessException;
    List<Token> findAllByCreator(String creator);
    List<Token> findAllByCreatorAndExpirationLessThan(String creator, Date now);
    List<Token> findAllByExpirationLessThan(Date now);
    Token findByUuidAndCreator(String uuid, String creator);
    Token findByTokenAndType(String token, Token.AuthTokenType type);
    Token tokenLookup(String token);


}
