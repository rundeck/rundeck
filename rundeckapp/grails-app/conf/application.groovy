hibernate {
    cache.queries = true
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.provider_class = "net.sf.ehcache.hibernate.EhCacheProvider"
    cache.region.factory_class = "org.hibernate.cache.ehcache.EhCacheRegionFactory"
}
dataSource {
    pooled = true
    jmxExport = true
    driverClassName= "org.h2.Driver"
    username = "sa"
    password = ''
}

environments {
    development {
        dataSource {
            dbCreate = "create-drop" // one of 'create', 'create-drop','update'
            url = "jdbc:h2:file:db/devDb"
        }
    }
    test {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:file:./db/testDb"
        }
    }
    production {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:file:rundeck/grailsh2"
            properties {
                jmxEnabled= true
                initialSize= 5
                maxActive= 50
                minIdle= 5
                maxIdle= 25
                maxWait= 10000
                maxAge= 600000
                timeBetweenEvictionRunsMillis= 5000
                minEvictableIdleTimeMillis= 60000
                validationQuery= "SELECT 1"
                validationQueryTimeout= 3
                validationInterval= 15000
                testOnBorrow= true
                testWhileIdle= true
                testOnReturn= false
                jdbcInterceptors= "ConnectionState"
                defaultTransactionIsolation= 2 // TRANSACTION_READ_COMMITTED
            }
        }
    }
}