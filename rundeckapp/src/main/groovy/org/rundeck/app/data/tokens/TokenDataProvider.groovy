package org.rundeck.app.data.tokens

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenMode
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.grails.datastore.mapping.query.api.Criteria
import org.rundeck.app.data.BaseAppContext
import org.rundeck.app.data.tokens.v1.Token
import org.rundeck.app.data.tokens.v1.TokenDataType
import org.rundeck.spi.data.ContextDataProvider
import org.rundeck.spi.data.DataAccessException
import org.rundeck.spi.data.DataQuery
import org.rundeck.spi.data.QueryPaging
import org.rundeck.spi.data.QueryResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import rundeck.AuthToken
import rundeck.User
import rundeck.services.UserService
import rundeck.services.data.AuthTokenDataService

@GrailsCompileStatic
@Slf4j
class TokenDataProvider implements ContextDataProvider<BaseAppContext, Token, TokenDataType> {
    @Autowired
    AuthTokenDataService authTokenDataService
    @Autowired
    UserService userService
    @Autowired
    MessageSource messageSource
    TokenDataType dataType = new TokenDataType()

    @Override
    Token getData(BaseAppContext context, final String id) {
        def token = authTokenDataService.getByUuid(id)
        return token ? new Wrap(del: token) : null
    }

    @Override
    String create(BaseAppContext context, final Token data) {
        return createWithId(
            context,
            data.uuid ?: UUID.randomUUID().toString(),
            data
        )
    }

    @Override
    String createWithId(BaseAppContext context, final String id, final Token data) {
        User tokenOwner = userService.findOrCreateUser(data.ownerName)
        if (!tokenOwner) {
            throw new Exception("Couldn't find user: ${data.ownerName}")
        }

        def tokenType = AuthTokenType.valueOf(data.type.toString())
        AuthToken token = new AuthToken(
            token: data.token,
            authRoles: data.getAuthRolesSet(),
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
    void update(BaseAppContext context, final String id, final Token data) {
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
    void delete(final BaseAppContext context, final String id) throws DataAccessException {
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
    QueryResult<Token, TokenDataType> query(BaseAppContext context, final DataQuery<TokenDataType> query) {
        BuildableCriteria criteria = AuthToken.createCriteria()
        Closure queryClos = createCriteriaClosure(query)

        if (query.queryType == DataQuery.QueryType.Exists) {
            int result = countResults(criteria, queryClos)
            return QueryResult.exists(
                result > 0
            )
        } else if (query.queryType == DataQuery.QueryType.Count) {
            int result = countResults(criteria, queryClos)
            return QueryResult.count(result)
        } else if (query.queryType == DataQuery.QueryType.List) {
            List<AuthToken> list = (List<AuthToken>) criteria.list(queryClos)
            List<Token> collect = list.collect { new Wrap(del: it) }
            int total = countResults(criteria, queryClos)
            return QueryResult
                .list(collect, QueryPaging.with(query.paging.offset + list.size(), total, query.paging.pageSize))
        } else if (query.queryType == DataQuery.QueryType.Get) {
            AuthToken result = (AuthToken) criteria.get(queryClos)
            return QueryResult.single(
                new Wrap(del: result)
            )
        } else {
            throw new DataAccessException("Unsupported query type: ${query.queryType}")
        }
    }

    @CompileDynamic
    private int countResults(BuildableCriteria criteria, Closure<List<DataQuery.Criterion>> queryClos) {
        criteria.count(queryClos)
    }

    private Closure createCriteriaClosure(final DataQuery<TokenDataType> query) {
        return {
            Criteria del = (Criteria) delegate

            if (del instanceof org.hibernate.Criteria && query.paging &&
                query.queryType in
                [DataQuery.QueryType.List]) {
                del.setMaxResults(query.paging?.pageSize)
                if (query.paging.offset) {
                    del.setFirstResult(query.paging.offset)
                }
            }

            if (query.sortProperty && query.queryType in [DataQuery.QueryType.Count, DataQuery.QueryType.List]) {
                del.order(query.sortProperty, query.sortAscending ? 'asc' : 'desc')
            }

            query.criteria.each { DataQuery.Criterion crit ->
                if (crit.type == DataQuery.Criterion.MatchType.Eq) {
                    if (crit.logicalNot) {
                        del.ne(crit.property, crit.value)
                    } else {
                        del.eq(crit.property, crit.value)
                    }
                } else if (crit.type == DataQuery.Criterion.MatchType.Like) {
                    if (crit.logicalNot) {
                        del.not {
                            del.like(crit.property, crit.value)
                        }
                    } else {
                        del.like(crit.property, crit.value)
                    }
                } else if (crit.type == DataQuery.Criterion.MatchType.InList) {
                    if (crit.logicalNot) {
                        del.not {
                            del.inList(crit.property, crit.value)
                        }
                    } else {
                        del.inList(crit.property, crit.value)
                    }
                } else {
                    throw new DataAccessException("Unsupported criteria type: ${crit.type}")
                }
            }
        }
    }


    /**
     * could be simpler if the types were moved out of core wholly
     */
    @CompileStatic
    static class Wrap implements Token {
        private AuthToken del

        @Override
        org.rundeck.app.data.tokens.v1.Token.AuthTokenType getType() {
            return org.rundeck.app.data.tokens.v1.Token.AuthTokenType.valueOf(
                del.type.toString()
            )
        }

        @Override
        String getToken() {
            return del.token
        }

        @Override
        Set<String> getAuthRolesSet() {
            return del.authRolesSet()
        }

        @Override
        String getUuid() {
            return del.uuid
        }

        @Override
        String getCreator() {
            return del.creator
        }

        @Override
        String getOwnerName() {
            return del.ownerName
        }

        @Override
        String getPrintableToken() {
            return del.printableToken
        }

        @Override
        Date getExpiration() {
            return del.expiration
        }

        @Override
        String getName() {
            return del.name
        }
    }
}
