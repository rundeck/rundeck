#!/bin/bash
#/ Update the rundeck-config.properties to define the dataSource settings
#/ DATABASE_URL
#/ DATABASE_DRIVER
#/ DATABASE_USER
#/ DATABASE_PASS
#/ DATABASE_DIALECT

if [[ ! -z "$DATABASE_URL"  ]] ; then
	sed -i "s#dataSource.url.*#dataSource.url = ${DATABASE_URL//&/\\&}#g" $HOME/server/config/rundeck-config.properties

	cat >>$HOME/server/config/rundeck-config.properties <<-END
		dataSource.username = $DATABASE_USER
		dataSource.password = $DATABASE_PASS
	END

	if [[ ! -z "$DATABASE_DRIVER" ]] ; then
		cat >>$HOME/server/config/rundeck-config.properties <<-END
			dataSource.driverClassName = $DATABASE_DRIVER
		END
	else
		>&2 echo 'Must set database driver via DATABASE_DRIVER envar.'
		exit 1
	fi

	if [[ ! -z "$DATABASE_DIALECT" ]] ; then
		cat >>$HOME/server/config/rundeck-config.properties <<-END
			dataSource.dialect = $DATABASE_DIALECT
		END
	fi

	echo "server config:"
	cat $HOME/server/config/rundeck-config.properties 
fi