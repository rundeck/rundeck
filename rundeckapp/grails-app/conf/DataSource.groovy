dataSource {
    pooled = false; //it is recommended not to use connection pool unless file encryption is enabled
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
}
hibernate {
    cache.use_second_level_cache=true
    cache.use_query_cache=true
    cache.provider_class='net.sf.ehcache.hibernate.EhCacheProvider'
}
// environment specific settings
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
			url = "jdbc:h2:file:db/testDb"
		}
	}
	production {
		dataSource {
			dbCreate = "update"
			url = "jdbc:h2:file:rundeck/grailsh2"
		}
	}
}
