Release 2.7.3
===========

Date: 2017-03-10

Name: <span style="color: Tomato"><span class="glyphicon glyphicon-paperclip"></span> "cafecito tomato paperclip"</span>

## Upgrading from 2.6

* See the [Upgrading Guide](http://rundeck.org/docs/upgrading/index.html)

## Notes

This is primarily a bugfix release, with a bonus enhancement.

* Fixed some plugin loader problems which were causing bugs with the Jasypt encryption for key storage, and Git SCM plugins
* Added autocomplete support to the Job Workflow editor text fields and text areas.

## Contributors

* Albert Casademont (acasademont)
* GitHub (web-flow)
* Greg Schueler (gschueler)
* jtobard

## Bug Reporters

* ChiefAlexander
* acasademont
* daikirinet
* gschueler
* isuftin
* schans
* seunaw
* willemdh

## Issues

[Milestone 2.7.3](https://github.com/rundeck/rundeck/milestone/51)

* [Error; Save Command as a Job ](https://github.com/rundeck/rundeck/issues/2362)
* [Fix plugin loader issues](https://github.com/rundeck/rundeck/pull/2361)
* [GUI: Autocomplete job/context vars in step editors](https://github.com/rundeck/rundeck/pull/2355)
* [Abort execution can fail in cluster mode without explanation (wrong cluster node)](https://github.com/rundeck/rundeck/issues/2327)
* [Add apitoken.aclpolicy to Debian conffile](https://github.com/rundeck/rundeck/pull/2320)
* [Missing dependency on uuid-runtime in debian package since 2.7.2](https://github.com/rundeck/rundeck/issues/2316)
* [Error scm git commit job ](https://github.com/rundeck/rundeck/issues/2294)
* [User and result columns in Activity page are mixed into each other](https://github.com/rundeck/rundeck/issues/2207)
* [SCM Export Fails - ClassNotFoundException](https://github.com/rundeck/rundeck/issues/2031)
* [SCM import fails](https://github.com/rundeck/rundeck/issues/1854)
* [Jasypt exception thrown, no further information provided](https://github.com/rundeck/rundeck/issues/1785)
