package org.rundeck.app.data.model.v1;

import lombok.Data;

import java.util.Date;
import java.util.Set;

@Data
public class TokenImpl
        implements Token
{
    private String token;
    private Set<String> authRolesSet;
    private String uuid;
    private String creator;
    private String ownerName;
    private String printableToken;
    private String name;
    private Date expiration;
    private AuthTokenType type;

    public static TokenImpl with(Token input) {
        TokenImpl token1 = new TokenImpl();
        token1.token = input.getToken();
        token1.authRolesSet = input.getAuthRolesSet();
        token1.uuid = input.getUuid();
        token1.creator = input.getCreator();
        token1.ownerName = input.getOwnerName();
        token1.printableToken = input.getPrintableToken();
        token1.name = input.getName();
        token1.expiration = input.getExpiration();
        token1.type = input.getType();
        return token1;
    }

}
