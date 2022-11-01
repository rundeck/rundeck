package org.rundeck.app.data.model.v1;
import java.util.Date;
import java.util.Set;

public class SimpleTokenBuilder implements AuthenticationToken {

  private String token;
  private Set<String> authRolesSet;
  private String uuid;
  private String creator;
  private String ownerName;
  private AuthTokenType type;
  private Date expiration;
  private String name;
  private AuthTokenMode tokenMode;
  private String clearToken;



  @Override
  public String getToken() {
    return token;
  }

  @Override
  public String getClearToken() {
        return clearToken;
    }

  @Override
  public Set<String> getAuthRolesSet() {
    return authRolesSet;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getCreator() {
    return creator;
  }

  @Override
  public String getOwnerName() {
    return ownerName;
  }

  @Override
  public AuthTokenMode getTokenMode() {
        return tokenMode;
    }

  @Override
  public AuthTokenType getType() {
    return type;
  }

  @Override
  public String getPrintableToken() {
    return token;
  }

  @Override
  public Date getExpiration() {
    return expiration;
  }

  @Override
  public String getName() {
    return name;
  }

  public SimpleTokenBuilder setToken(String token) {
    this.token = token;
    return this;
  }

  public SimpleTokenBuilder setAuthRolesSet(Set<String> authRolesSet) {
    this.authRolesSet = authRolesSet;
    return this;
  }

  public SimpleTokenBuilder setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public SimpleTokenBuilder setCreator(String creator) {
    this.creator = creator;
    return this;
  }

  public SimpleTokenBuilder setClearToken(String clearToken) {
      this.clearToken = clearToken;
      return this;
  }

  public SimpleTokenBuilder setOwnerName(String ownerName) {
    this.ownerName = ownerName;
    return this;
  }

  public SimpleTokenBuilder setType(AuthTokenType type) {
    this.type = type;
    return this;
  }
  public SimpleTokenBuilder setTokenMode(AuthTokenMode mode) {
    this.tokenMode = mode;
    return this;
  }
  public SimpleTokenBuilder setExpiration(Date expiration) {
    this.expiration = expiration;
    return this;
  }

  public SimpleTokenBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public static SimpleTokenBuilder with(AuthenticationToken input) {
      SimpleTokenBuilder token1 = new SimpleTokenBuilder();
    token1.token = input.getToken();
    token1.authRolesSet = input.getAuthRolesSet();
    token1.uuid = input.getUuid();
    token1.creator = input.getCreator();
    token1.ownerName = input.getOwnerName();
    token1.name = input.getName();
    token1.expiration = input.getExpiration();
    token1.type = input.getType();
    token1.tokenMode = input.getTokenMode();
    return token1;
  }
}
