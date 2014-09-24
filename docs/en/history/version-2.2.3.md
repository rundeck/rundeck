% Version 2.2.3
% greg
% 09/24/2014

Release 2.2.3
=============

Date: 2014-09-24

Fix several issues found in 2.2.2:

* [2.2.2: Workflow editor drag/drop or step delete doesn't work](https://github.com/rundeck/rundeck/issues/943)
* [Documentation: Sudo password option type incorrect: should specify Secure Remote Authentication option](https://github.com/rundeck/rundeck/issues/940)
* [plugin development: plugin properties using rendering options should allow String values](https://github.com/rundeck/rundeck/issues/939)

Release notes from 2.2.2 follow:

This release fixes a number of bugs and addresses several potential security issues:

1. Require a unique token for each form request from the GUI, which prevents replay and CSRF attacks
2. Updated all pages to prevent unencoded data from being written to the response, preventing XSS style attacks.
3. Prevent access to the /api URLs via the web GUI.
4. Some plugins (Resource model, Node Executor and File Copier) now support using Password fields displayed in the Project config page. The field values once set are never revealed in clear text via the GUI.

Please see the Notes below for some configuration information
related to these changes.

**A big Thank You to one of our clients for sponsoring the work for these enhancements.**

*Security Notes:*

The new form tokens used in all form requests
by default will expire in 30 minutes.
This means that if your session timeout is larger than 30 minutes
and you attempt to e.g. run a job
after your web page has been sitting open for longer than that,
you will see an "Invalid token" error.
If this becomes a problem for you
you can either change the expiration time for these tokens,
or switch to using non-expiring tokens.
See [Administration - Configuration File Reference - Security](http://rundeck.org/2.2.2/administration/configuration-file-reference.html#security).

To add a Password field definition to your plugin, 
see [Plugin Development - Description Properties](http://rundeck.org/2.2.2/developer/plugin-development.html#description-properties). 
(Note that currently using property annotations is not supported 
for the three plugin types that can use Password properties.)

*Upgrade notes:* 

See the [Upgrading Guide](http://rundeck.org/2.2.2/upgrading/index.html).


## Contributors

* Andreas Knifh (knifhen)
* Daniel Serodio (dserodio)
* Greg Schueler (gschueler)

## Bug Reporters

* adolfocorreia
* ahonor
* arjones85
* danpilch
* dennis-benzinger-hybris
* dserodio
* garyhodgson
* gschueler
* jerome83136
* knifhen
* majkinetor
* rfletcher
* schicky

## Issues

* [dynamic node filter string incorrectly includes name: prefix](https://github.com/rundeck/rundeck/issues/934)
* [aclpolicy files are listed in random order in Configure page](https://github.com/rundeck/rundeck/issues/931)
* [Improve "Authenticating Users" docs re. logging](https://github.com/rundeck/rundeck/pull/925)
* [Security: allow plugins to specify password properties that are obscured in project config page](https://github.com/rundeck/rundeck/pull/919)
* [Job Variable Length is too low](https://github.com/rundeck/rundeck/issues/915)
* [Config toggle: Hide error page stacktrace](https://github.com/rundeck/rundeck/pull/910)
* [Security: CSRF prevention](https://github.com/rundeck/rundeck/pull/909)
* [Security: prevent XSS issues](https://github.com/rundeck/rundeck/pull/908)
* [Cannot pass multiple values to multivalued option with enforced values](https://github.com/rundeck/rundeck/issues/907)
* [Rundeck 2.1.1 scheduling bug](https://github.com/rundeck/rundeck/issues/905)
* [Selectively Disable metrics servlets features](https://github.com/rundeck/rundeck/pull/904)
* [Broken Link in Documentation](https://github.com/rundeck/rundeck/issues/903)
* [Machine tag style attributes don't get replaced ](https://github.com/rundeck/rundeck/issues/901)
* [Scheduled job with retry never completes 2.2.1](https://github.com/rundeck/rundeck/issues/900)
* [API docs state latest version is 11, but it is 12](https://github.com/rundeck/rundeck/issues/898)
* [NPE: Cannot get property 'nodeSet' on null object since upgrade to 2.2.1-1](https://github.com/rundeck/rundeck/issues/896)
* [Powershell and script-exec - extension problem](https://github.com/rundeck/rundeck/issues/894)
* [Ldap nestedGroup examples](https://github.com/rundeck/rundeck/pull/892)
* ["Retry failed nodes" does not seem to work, when using dynamic nodes filters](https://github.com/rundeck/rundeck/issues/883)
* [UI job status incorrect](https://github.com/rundeck/rundeck/issues/861)
* [Odd page when not allowing node info access](https://github.com/rundeck/rundeck/issues/844)