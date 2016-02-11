% Setting up a RDB Datasource

You can configure Rundeck to use a RDB instead of the default file-based data storage.

You must modify the `server/config/rundeck-config.properties` file, to change the `dataSource` configuration, and you will have to add the appropriate JDBC driver JAR file to the lib directory.

For Mysql-specific instructions, jump to: [Mysql Setup Guide](#mysql-setup-guide).

## Customize the Datasource

The default dataSource is configured for filesystem storage using HSQLDB:

~~~~~~ {.java}
dataSource.url = jdbc:hsqldb:file:/var/lib/rundeck/data/grailsdb;shutdown=true
~~~~~~ 

Here is an example configuration to use an Oracle backend:

~~~~~~ {.java .numberLines }
dataSource.url = jdbc:oracle:thin:@localhost:1521:XE
dataSource.driverClassName = oracle.jdbc.driver.OracleDriver
dataSource.username = dbuser
dataSource.password = dbpass
dataSource.dialect = org.hibernate.dialect.Oracle10gDialect
~~~~~~~~

Here is an example configuration to use Mysql:

~~~~~~ {.java .numberLines }
dataSource.url = jdbc:mysql://myserver/rundeckdb?autoReconnect=true
dataSource.username = dbuser
dataSource.password = dbpass
~~~~~~

NB: for Mysql, the `autoReconnect=true` will fix a common problem where the Rundeck server's connection to Mysql is dropped after a period of inactivity, resulting in an error message: "Message: Can not read response from server. Expected to read 4 bytes, read 0 bytes before connection was unexpectedly lost."

See the [Mysql Setup Guide](#mysql-setup-guide) for 
instructions on creating the rundeck database and granting access.

See more about [configuring the Mysql JDBC Connector/J URL](http://dev.mysql.com/doc/refman/5.1/en/connector-j-reference-configuration-properties.html).

## Add the JDBC Driver

Rundeck includes a JDBC driver for Mysql and H2. If you are using another database
copy the appropriate JDBC driver, such as "ojdbc14.jar" for Oracle into the server `lib` dir:

~~~~~~ {.bash}
cp ojdbc14.jar $RDECK_BASE/server/lib
~~~~~~

# Mysql setup guide

This is a simple guide for setting up Mysql for use with Rundeck.

## Install Mysql

You can "yum install" or "apt-get install" the server, or you can download rpms manually if you like. See [mysql linux installation](http://dev.mysql.com/doc/refman/5.5/en/linux-installation-native.html)

After install, run the [mysql_secure_installation script](http://dev.mysql.com/doc/refman/5.5/en/mysql-secure-installation.html). This will let prompt you to set the root password for mysql, as well as disable anonymous access.

## Setup Rundeck Database

Now you want to create a database and user access for the Rundeck server.

If it is not running, start mysqld with "service mysqld start"

Use the 'mysql' commandline tool to access the db as the root user:

    $ mysql -u root -p

Enter your root password to connect.  Once you have the mysql> prompt, enter the following commands to create the rundeck database:

    mysql> create database rundeck;
    Query OK, 1 row affected (0.00 sec)

Then "grant" access for a new user/password, and specify the hostname the Rundeck server will connect from.  if it is the same server, you can use "localhost".

    mysql> grant ALL on rundeck.* to 'rundeckuser'@'localhost' identified by 'rundeckpassword';
    Query OK, 1 row affected (0.00 sec)

You can then exit the mysql prompt.

Test access (if it's from localhost) by running:

    $ mysql -u rundeckuser -p

You can verify you can see the "rundeck" database with:

    mysql> show databases;
    +--------------------+
    | Database           |
    +--------------------+
    | information_schema |
    | rundeck            |
    +--------------------+
    2 rows in set (0.00 sec)

## Configure Rundeck

Now you need to configure Rundeck to connect to this DB as described earlier in this document: [Customize the Datasource](#customize-the-datasource).

Update your `rundeck-config.properties` and configure the datasource:

    dataSource.url = jdbc:mysql://myserver/rundeck?autoReconnect=true
    dataSource.username=rundeckuser
    dataSource.password=rundeckpassword

With recent Rundeck version, MySQL connector is already there.
You can check if present in this path: `$RDECK_BASE/exp/webapp/WEB-INF/lib/`

Else:
* [Download the mysql connector jar](http://dev.mysql.com/downloads/connector/j/).
* Copy the mysql-connector-java-5.x.x-bin.jar to `$RDECK_BASE/server/lib` (for launcher install) or `$WEBAPPS/rundeck/WEB-INF/lib` (for Tomcat).

Finally you can start rundeck.  If you see a startup error about database access, make sure that the hostname that the Mysql server sees from the client is the same one you granted access to.

# Mysql migration guide

This section describes how to migrate a set of Rundeck projects from
the built-in H2 database, to use Mysql.

## Project export all projects

For each project you want to retain, Export the archive via the GUI:

* Navigate to Configure page for the project
    * Either click the cog icon in the header, then choose your project from the list
    * OR click the Configure button next to the project name in the home page
* Click on the "Export Archive" tab
* Click "Export project.rdproject.jar"
* Wait for export process to complete
* Click the link to download the file

Note: make sure to click the final link that is presented to download the file.

Save each project archive file (named "[project name]-XXXX.rdproject.jar") 
in a place you can upload it later.

## Backup your Rundeck data

* Make a backup of your Projects
    * RPM/Debian install location: `/var/rundeck/projects`
    * Launcher location: `$RDECK_BASE/prjoects`
* Make a backup of your H2 database, which you can revert back to in case of error, 
    * RPM/Debian install location: `/var/lib/rundeck/data`
    * Launcher location: `$RDECK_BASE/server/data`

## Stop rundeck

Unix:

    sudo service rundeckd stop


## Setup mysql

Install Mysql according to the instructions for your platform.

## Prepare Mysql database

Perform this command to log in as root:

    mysql -u root -p

Then execute this sql:

    > create database rundeckdb
    > grant ALL on rundeckdb.* to ‘rundeckuser’@‘localhost’ identified by ‘rundeckpassword’


## Configure rundeck-config.properties

Set the datasource URL to point to your Mysql host, with appropriate database name, 
username and password.

Modify the rundeck config file.

* RPM/Debian location: `/etc/rundeck/rundeck-config.properties`
* Launcher location: `$RDECK_BASE/server/config/rundeck-config.properties`

Replace the `dataSource.url` entry with these lines:

    dataSource.url=jdbc:mysql://mysqlhost/rundeckdb?autoReconnect=true
    dataSource.username=rundeckuser
    dataSource.password=rundeckpassword
    dataSource.driverClassName=com.mysql.jdbc.Driver


## Configure project config in DB

Enable DB storage for Project configurations, and Key Storage. Optionally enable encryption. 

For more info refer to:

* [Administrator Guide - Key Storage](http://rundeck.org/docs/administration/key-storage.html)
* [Plugins User Guide - Storage Plugins - Jasypt Encryption](http://rundeck.org/docs/plugins-user-guide/storage-plugins.html#jasypt-encryption-converter-plugin)
* [Plugins User Guide - Configuring - Storage Plugins](http://rundeck.org/docs/plugins-user-guide/configuring.html#storage-plugins)

Modify `rundeck-config.properties`
    
    # Enables DB for Project configuration storage
    rundeck.projectsStorageType=db

    # Encryption for project config storage
    rundeck.config.storage.converter.1.type=jasypt-encryption
    rundeck.config.storage.converter.1.path=projects
    rundeck.config.storage.converter.1.config.password=mysecret

    # Enable DB for Key Storage
    rundeck.storage.provider.1.type=db
    rundeck.storage.provider.1.path=keys

    # Encryption for Key Storage
    rundeck.storage.converter.1.type=jasypt-encryption
    rundeck.storage.converter.1.path=keys
    rundeck.storage.converter.1.config.password=mysecret

## Start up Rundeck

Start the Rundeck server again.

Unix:

    sudo service rundeckd start

View the "service.log" or "rundeck.log" file for any error messages.

* Project definitions/configs will be imported to DB automatically
* Resources.xml remain in the same location

## Import archives

For each project you wish to import, go to the Configure page for the project:

* Click the "Import Archive" tab
* Upload the project archive with the corresponding name
* Optionally choose to Import All Executions
* Click Import

