package com.dtolabs.rundeck.core.authentication;

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType;

import java.io.Serializable;
import java.security.Principal;

public class Token
        implements Principal, Serializable
{
    private static final long serialVersionUID = 1L;

    public Token(String tokenId, AuthTokenType type) {
        super();
        this.tokenId = tokenId;
        this.type = type;
    }

    private final String tokenId;
    private final AuthTokenType type;

    /**
     * @see java.security.Principal#getName()
     */
    public String getName() {
        return tokenId;
    }

    public AuthTokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Auth Token: " + this.tokenId + ", " + type;
    }

}