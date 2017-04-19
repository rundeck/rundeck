#!/bin/bash
#/ Update the rundeck-config.properties to define the dataSource settings
#/ DATABASE_URL
#/ DATABASE_DRIVER
#/ DATABASE_USER
#/ DATABASE_PASS
#/ DATABASE_DIALECT


if [[ ! -z "$DATABASE_URL"  ]];
then
  cat >>$HOME/server/config/rundeck-config.properties <<END
dataSource.driverClassName=$DATABASE_DRIVER
dataSource.url = $DATABASE_URL
dataSource.username=$DATABASE_USER
dataSource.password=$DATABASE_PASS
END
if [[ ! -z "$DATABASE_DIALECT" ]] ;
then
  cat >>$HOME/server/config/rundeck-config.properties <<END
dataSource.dialect = $DATABASE_DIALECT
END
fi
 echo "server config:"
 cat $HOME/server/config/rundeck-config.properties 
fi