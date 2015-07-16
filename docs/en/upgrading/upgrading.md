% Upgrade Guide
% Greg Schueler
% April 15, 2015

## Upgrading to Rundeck 2.5


### Java 7 required

Java 7 is now required for Rundeck 2.5, and Java 8 can be used.  Java 6 will not work.

### Scheduled Jobs with required option default values

A bug in Rundeck previously allowed Jobs to saved with a schedule
even if an Option marked "required" did not have a default value.
The result was that the scheduler would fail to execute the job silently.

In Rundeck 2.5 this is now considered a validation error when creating or importing the job definition,
so any scheduled jobs with required options need to have a default value set.

### rd-project create of existing project

If `rd-project -a create -p projectname` is executed for an existing project, this will now fail.

### Database schema

This release adds some new DB tables but does not alter the schema of other tables.

### Project definitions stored in DB

Rundeck 2.4 and earlier used the filesystem to store Projects and their configuration.
Rundeck 2.5 can now use the DB to store project definition and configuration, 
but this is not enabled by default. 

If you have projects that exist on the filesystem, when you upgrade to Rundeck 2.5, these projects
and their configuration files can be automatically imported into the DB.  This means that
the contents of `project.properties` will be copied to the DB,
using Rundeck's [Storage Facility](../administration/storage-facility.html).

In addition, there is *no encryption by default*, if you want the contents of your project.properties
to be encrypted in the DB, you must configure 
[Storage Converter Plugins](../plugins-user-guide/configuring.html#storage-converter-plugins) 
to use an encryption plugin.  There is now a [Jasypt Encryption Plugin](../plugins-user-guide/storage-plugins.html#jasypt-encryption-converter-plugin) included with Rundeck which can be used.

**Enable project DB storage**:

You can configure Rundeck to use the Database by adding the following to 
`rundeck-config.properties` before starting it up:

    rundeck.projectsStorageType=db

When importing previously created filesystem projects, the contents of these files are imported to the DB:

* `etc/project.properties`
* `readme.md`
* `motd.md`

In addition, after importing, the `project.properties` file will be renamed to `project.properties.imported`.

If desired, you can switch back to using filesystem projects by doing this:

1. set `rundeck.projectsStorageType=filesystem` in `rundeck-config.properties`
2. rename each `project.properties.imported` file back to `project.properties`

### web.xml changes

The `web.xml` file has changed significantly from 2.4.x to Rundeck 2.5.x.

For this reason, if you have modified your `web.xml` file, located at
`/var/lib/rundeck/exp/webapp/WEB-INF/web.xml`,
for example to change the `<security-role><role-name>user</role-name><security-role>`,
then you may need to back it up, and re-apply the changes you made after upgrading to 2.5.x.

If you receive a "Service Unavailable" error on startup, and the service.log file contains this message:

    java.lang.ClassNotFoundException: org.codehaus.groovy.grails.web.sitemesh.GrailsPageFilter

Then that means your web.xml file is out of date.  Replace it with the one from 2.5 installation,
then re-apply your changes to `<role-name>`.

## Upgrading to Rundeck 2.1

### Database schema

If you are upgrading from 2.0.x, be sure to perform backups prior to upgrading.
This release adds a new DB table 
but does not alter the schema of other tables.

### ACL policy additions

Project access via API has been improved, and new authorizations are now required for project access.  See [Adminstration - Access Control Policy](../administration/access-control-policy.html#application-scope-resources-and-actions).

* project access adds `configure`,`delete`,`import`,`export` actions
* `admin` access still allows all actions

Example allowing explicit actions:

    context:
      application: 'rundeck'
    for:
      resource:
        - equals:
            kind: 'project'
          allow: [create] # allow creating new projects
      project:
        - equals:
            name: 'myproject'
          allow: [read,configure,delete,import,export,admin] # access to 'myproject'
    by:
      group: admin

The storage facility for uploading public/private keys requires authorization to use. The default `admin.aclpolicy` and `apitoken.aclpolicy` provide this access, but if you have custom policies you may want to allow access to these actions.

* `storage` can allow `create`,`update`,`read`, or `delete`
* you can match on `path` or `name` to narrow the access

The default apitoken aclpolicy file allows this access:

    context:
      application: 'rundeck'
    for:
      storage:
        - match:
            path: '(keys|keys/.*)'
          allow: '*' # allow all access to manage stored keys
    by:
      group: api_token_group

## Upgrading to Rundeck 2.0 from 1.6.x

Rundeck 2.0 has some under-the-hood changes, so please follow this guide when upgrading from Rundeck 1.6.x.

The first step is always to make a backup of all important data for your existing Rundeck installation.  Refer to the [Administration - Backup and Recovery](../administration/backup-and-recovery.html) section.

## Clean install

The most direct upgrade method is to use the project export/import method and a clean install of Rundeck 2.0.

Before shutting down your 1.6.x installation, perform **Project Export** for each project you wish to migrate:

1. Select your project
2. Click the *Configure* tab in the header.
3. Click the link under *Export Project Archive* to save it to your local disk.
4. Make a copy of all project files under the projects directory for the project, e.g. `$RDECK_BASE/projects/NAME` (launcher) or `/var/rundeck/projects/NAME` (RPM/Deb).  This includes the project.properties configuration as well as resources files.

Perform a *clean* install Rundeck 2.0 (no cheating!).

Then Import the projects you exported:

1. Create a new project, or select an existing project.
2. Click the *gear icon* for the Configure page in the header.
3. Click the *Import Archive* Tab
4. Under *Choose a Rundeck Archive* pick the archive file you downloaded earlier
5. Click *Import*

Finally, restore the project files for the imported project.

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
