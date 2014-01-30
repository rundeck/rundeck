% Upgrade Guide
% Greg Schueler
% January 30, 2014

## Upgrading to Rundeck 2.0 from 1.6.x

Rundeck 2.0 has some under-the-hood changes, so please follow this guide when upgrading from Rundeck 1.6.x.

## Clean install

The most direct upgrade method is to use the project export/import method and a clean install of Rundeck 2.0.

Before shutting down your 1.6.x installation, perform **Project Export** for each project you wish to migrate:

1. Select your project
2. Click the *Configure* tab in the header.
3. Click the link under *Export Project Archive* to save it to your local disk.

Perform a *clean* install Rundeck 2.0 (no cheating!).

Then Import the projects you exported:

1. Create a new project, or select an existing project.
2. Click the *gear icon* for the Configure page in the header.
3. Click the *Import Archive* Tab
4. Under *Choose a Rundeck Archive* pick the archive file you downloaded earlier
5. Click *Import*

## Upgrading JAAS properties file

If you are not doing a clean install, and you want to maintain your JAAS login module configuration, you may have to change your jaas.conf file.

The default jaas-loginmodule.conf file included with Rundeck 1.6.x uses the `org.mortbay.jetty.plus.jaas.spi.PropertyFileLoginModule` class.  You will have to change your file to specify `org.eclipse.jetty.plus.jaas.spi.PropertyFileLoginModule` ("org.eclipse").

Modify the `$RDECK_BASE/server/config/jaas-loginmodule.conf` (launcher install) or `/etc/rundeck/jaas-loginmodule.conf` (RPM/Deb install).

## Upgrading an existing H2 Database

If you want to migrate your existing H2 Database, you will have to download an additional jar file to enable upgrading to the newer H2 version used in Rundeck 2.0.

Download the `h2mig_pagestore_addon.jar` file linked on this page:

* [H2 Database Upgrade](http://www.h2database.com/html/advanced.html#database_upgrade)
* Direct link: <http://h2database.com/h2mig_pagestore_addon.jar>

Copy the file to `$RDECK_BASE/server/lib` (launcher jar) or `/var/lib/rundeck/bootstrap` (RPM/Deb install).

## Upgrading an existing Mysql or other Database

Rundeck 2.0 will add some columns to the existing tables, but should allow in-place migration of the mysql database.  

However, make sure you take appropriate backups of your data prior to upgrading.
