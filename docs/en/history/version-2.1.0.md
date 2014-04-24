% Version 2.1.0
% greg
% 04/23/2014

Release 2.1.0
=============

Date: 2014-04-23

This release enhances the Rundeck API and provides a facility for storing Key files that can be used for SSH.

* Project management improvements: 
    - Project Delete action in the Configuration page
    - [Project create/config/delete APIs](../api/index.html#changes)
- [Key storage facility](../administration/key-storage.html):
    + [Upload public or private keys via API](../api/index.html#changes)
    + store the key file data on disk or in the DB
    + Use private keys for SSH
    + Note: no encryption is performed by default, but you can use plugins to encrypt the data

*Upgrade notes:* 

1. If you are upgrading from 2.0.1 or earlier, see the notes about schema changes in the previous release notes: [2.0.2 Release Notes](../history/version-2.0.2.html).
2. See the [Upgrading Guide](../upgrading/index.html)

## Contributors

* Diomidis Spinellis (dspinellis)
* Greg Schueler (gschueler)

## Issues

* [add link about project.properties in project setup guide](https://github.com/rundeck/rundeck/issues/745)
* [After update to 2.0.4-1.15.GA ssh login fails with "bad ownership or modes for directory"](https://github.com/rundeck/rundeck/issues/743)
* [Key storage and management](https://github.com/rundeck/rundeck/issues/726)
* [deb requires java 6](https://github.com/rundeck/rundeck/issues/722)
* [Feature/project api](https://github.com/rundeck/rundeck/pull/693)
* [Allow script based plugins (-plugin.zip) to have just .zip extension instead](https://github.com/rundeck/rundeck/issues/556)
* [Allow user configuration of authentication methods available to built-in SSH provider](https://github.com/rundeck/rundeck/issues/551)
* [Update API to better manage rundeck administration](https://github.com/rundeck/rundeck/issues/492)
* [API to get a project configuration](https://github.com/rundeck/rundeck/issues/491)
* [API for api-key administration (list,create,remove)](https://github.com/rundeck/rundeck/issues/488)
* [API for project remove](https://github.com/rundeck/rundeck/issues/487)
* [API for project create ](https://github.com/rundeck/rundeck/issues/486)
* [API for project export and import](https://github.com/rundeck/rundeck/issues/485)