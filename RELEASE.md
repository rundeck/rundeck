Release 3.0.13
===========

Date: 2019-01-23

Name: <span style="color: khaki"><span class="glyphicon glyphicon-headphones"></span> "jalapeño popper khaki headphones"</span>

## Notes

Security and bug fixes, and some enhancements.

Security fixes:

* potential stored XSS vulnerability (https://github.com/rundeck/rundeck/pull/4406)
* add Content-Security-Policy and other security HTTP headers to responses (see more info https://github.com/rundeck/rundeck/pull/4405)



## Contributors

* Alberto Hormazabal (ahormazabal)
* Greg Schueler (gschueler)
* Jaime Tobar (jtobard)
* Luis Toledo (ltamaster)
* Greg Zapp (ProTip)
* Stephen Joyner (sjrd218)

## Bug Reporters

* ProTip
* ahormazabal
* gschueler
* jtobard
* ltamaster
* sebastianbello
* sjrd218
* vinillum

## Issues

[Milestone 3.0.13](https://github.com/rundeck/rundeck/milestone/95)

* [new version of winrm plugin 1.0.10](https://github.com/rundeck/rundeck/pull/4415)
* [Fix Plugin list api by referencing correct plugin list information service](https://github.com/rundeck/rundeck/pull/4413)
* [Add CSP header control variables to Docker image](https://github.com/rundeck/rundeck/pull/4408)
* [Fix #4406: stored xss vulnerability](https://github.com/rundeck/rundeck/pull/4407)
* [Security: stored XSS vulnerability](https://github.com/rundeck/rundeck/issues/4406)
* [Add common web-app security headers](https://github.com/rundeck/rundeck/pull/4405)
* [Add new flag to enable UI plugins on all pages](https://github.com/rundeck/rundeck/pull/4404)
* [Remove environment variable that hijacks jvm ssl settings](https://github.com/rundeck/rundeck/pull/4398)
* [UI plugin install status fix](https://github.com/rundeck/rundeck/pull/4379)
* [Fix #4374. User and role set by AJP were not being properly set.](https://github.com/rundeck/rundeck/pull/4378)
* [Fixes #4376. Partial templates are now expanded and added to base property file.](https://github.com/rundeck/rundeck/pull/4377)
* [Update spring security plugin to last version.](https://github.com/rundeck/rundeck/pull/4371)
* [UUID validation on jobref](https://github.com/rundeck/rundeck/pull/4366)
* [email notification enhancement](https://github.com/rundeck/rundeck/pull/4365)
* [Fix #2975 multiple threads modify the map](https://github.com/rundeck/rundeck/pull/4355)
* [Fixes #115. ](https://github.com/rundeck/rundeck/pull/4347)
* [User profile information can be sync'd from LDAP](https://github.com/rundeck/rundeck/pull/4338)
* [UUID validation and Autocomplete in Job Reference Workflow step](https://github.com/rundeck/rundeck/issues/4337)
* [Feature/multi repository support](https://github.com/rundeck/rundeck/pull/4336)
* [Execution Metrics API](https://github.com/rundeck/rundeck/pull/4317)
* [java.lang.ClassCastException: java.util.HashMap$Node cannot be cast to java.util.HashMap$TreeNode](https://github.com/rundeck/rundeck/issues/2975)
* [LDAP login with empty password](https://github.com/rundeck/rundeck/issues/115)