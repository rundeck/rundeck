% Upgrade Guide
% Greg Schueler
% April 15, 2015

## Upgrading to Rundeck 2.11

(If you are upgrading from a version earlier than 2.10.x, please peruse the rest of this document for any other issues regarding intermediate versions.)

Potentially breaking changes:

**RPM spec:**

The `rundeck` user/group is now created within system UID ranges [#3195](https://github.com/rundeck/rundeck/pull/3195).

**ACLs: Edit Project Configuration/Nodes GUI access level requirements changed:**

Previously: GUI actions "Project > Edit Configuration" and "Project > Edit Nodes" required `admin` project access. Now: only `configure` level access is required.

NOTE: API behavior was always this way, so this change simply aligns the access requirements.

Potential security implications:

* users/roles granted `configure` access to a project will now be able to modify Project Nodes or Configuration via the GUI
* the same users/roles would already have this access if using the API

See: [#3084](https://github.com/rundeck/rundeck/pull/3084)

**ACLs: Job Definition visibility**

A new ACL access level `view` is a subset of the `read` access level for jobs, and does not allow users to view the "Definition" tab of a Job, or download the XML/YAML definitions.

ACLs which allow `read` to Jobs, will work as before. To disallow Job Definition viewing/downloading, you should change your ACLs to only allow `view` access.

**Project Storage Type is now `db` by default:**

If you want to continue using filesystem storage for project config/readme/motd files, you will need to set this in your `rundeck-config.properties` before upgrading:

    rundeck.projectsStorageType=filesystem

Upgrading an existing `filesystem` configuration to `db` is automatic, and project configs/readme/motd will be loaded into DB storage at system startup.

To encrypt the DB storage, you will need to [enable encryption for the "Project Configuration" storage layer](http://rundeck.org/docs/plugins-user-guide/bundled-plugins.html#jasypt-encryption-plugin).

## Upgrading to Rundeck 2.8.1 from 2.8.0

### Important Note 

If you have previously installed Rundeck 2.8.0, using Mysql or H2 database, the 2.8.1 update 
will not work correctly. A DB schema change was required to fix a bug with Postgres and other databases:
 (`user` is a reserved word).

If you upgrade from 2.7.x to 2.8.1 (skipping 2.8.0), or install 2.8.1 from scratch, you will *not* encounter this problem.

Use the following methods to update your DB schema for Mysql or H2 if you are upgrading from 2.8.0 to 2.8.1:

#### Mysql upgrade to 2.8.1 from 2.8.0

1. Stop Rundeck
2. Run the following SQL script, to rename the `user` column in the `job_file_record` table to `rduser`.

~~~{.sql}
use rundeck;
alter table job_file_record change column user rduser varchar(255) not null;
~~~

After running this script, you can proceed with the 2.8.1 upgrade.

#### H2 upgrade to 2.8.1 from 2.8.0

For H2, you will need to do the following:

1. Shut down Rundeck.
2. (backup your H2 database contents, see [Backup and Recovery][]).
3. Use the h2 [`RunScript`](http://h2database.com/javadoc/org/h2/tools/RunScript.html) command
to run the following SQL script.

To run the script you will need:

* URL defining the location of your H2 database.  This is the same as the `dataSource.url` defined in
   your `rundeck-config.properties`.

    * For RPM/DEB installs it is `jdbc:h2:file:/var/lib/rundeck/data/rundeckdb`.
    * For a Launcher install, it will include your `$RDECK_BASE` path.  It can be relative to your current
   working directory, such as `jdbc:h2:file:$RDECK_BASE/server/data/grailsdb`.
* File path to the `h2-1.4.x.jar` jar file, which is in the expanded war contents of the Rundeck install.
    * For RPM/DEB installs it is `/var/lib/rundeck/exp/webapp/WEB-INF/lib/h2-1.4.193.jar`.
    * For launcher install it will under your `$RDECK_BASE`, such as: 
    `$RDECK_BASE/server/exp/webapp/WEB-INF/lib/h2-1.4.193.jar`

Save this into a file `upgrade-2.8.1.sql`:

~~~ {.sql}
alter table job_file_record rename column user to rduser;
~~~

Run this command:

~~~ {.bash}
H2_JAR_FILE=... #jar file location
H2_URL=...      #jdbc URL
java -cp $H2_JAR_FILE org.h2.tools.RunScript  \
  -url $H2_URL \
  -user sa \
  -script upgrade-2.8.1.sql
~~~

This command should complete with a 0 exit code (and no output).

You can now upgrade to 2.8.1.

**Error output**

If you see output containing `Column "USER" not found;` then you have already run this script successfully.

If you see output containing `Table "JOB_FILE_RECORD" not found`, then you probably did not have 2.8.0 installed,
you should be able to upgrade from 2.7 without a problem.

[Backup and Recovery]: ../administration/backup-and-recovery.html


## Upgrading to Rundeck 2.8 from earlier versions

### Java 8 is required

Rundeck server now requires Java 8.

## Upgrading to Rundeck 2.7 from 2.6.x


### Java 8 is required

Well, not technically *required* for Rundeck server so much as heavily frowned upon. You should upgrade, consider Java 7 no longer supported.  We may switch to actually requiring it soon.

### Default database (H2) upgraded to 1.4.x
    
The new version uses a different storage format ("mv_store") so switching back to 2.6 after creating a DB with 2.7 may not work.

In-place upgrade with the old storage format do seem to work, however if necessary to keep compatibility with an existing older h2 database, you can update your dataSource.url in rundeck-config.properties to add `;mv_store=false`

    dataSource.url = jdbc:h2:file:/path;MVCC=true;mv_store=false

* You can remove `;TRACE_LEVEL_FILE=4` from the dataSource.url in rundeck-config.properties

### CLI tools are gone
      
We have removed the "rd-*" and "dispatch" and "run" tools from the install, although the "rd-acl" tool is still available.

You should use the new "rd" tool available separately, see <https://rundeck.github.io/rundeck-cli/>.

However, `rd` *does* require Java 8.  (See, gotcha.)

### Debian/RPM startup script changes
      
The file `/etc/rundeck/profile` was modified and will probably not work with your existing install.
(This change was snafu'd into 2.6.10 and reverted in 2.6.11)

If you have customized `/etc/rundeck/profile`, look at the new contents and move your custom env var changes to a file in `/etc/sysconfig/rundeckd`.

### Inline script token expansion changes
      
(This is another change tha had some hiccups in the 2.6.10 release.)

You must now use `@@` (two at-signs) to produce a literal `@` in an inline script when it might be interpreted as a token, i.e. `@word@` looks like a token, but `@word space@` is ok.

You can globally disable inline script token expansion, see [framework.properties](../administration/configuration-file-reference.html#framework.properties).

### Jetty embedded server was upgraded to 9.0.x

If you are using the default "realm.properties" login mechanism, the default JAAS configuration for file-based authentication will need to be modified to use correct class name in your `jaas-loginmodule.conf`:

* **old value**: `org.eclipse.jetty.plus.jaas.spi.PropertyFileLoginModule`
* Replace with: `org.eclipse.jetty.jaas.spi.PropertyFileLoginModule`


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
