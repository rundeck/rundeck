Release 1.6.2
===========

Date: 2013-09-19

Notable Changes:

* Bug fixes:
    * Job references could not be edited after upgrading from 1.6.0 to 1.6.1
    * using node rank attribute with the same value on two nodes would skip nodes
    * error running jobs with no options defined
    * LDAPS certificate validation fixes
    * Secure option data should not be echoed in DEBUG logs

Many thanks to Kim Ho for his contributions for this release!

Contributors:

* Greg Schueler
* Kim Ho

Issues:

* [LDAP SSL CN validation checks fail to validate wildcard SSL cert names](https://github.com/dtolabs/rundeck/issues/547)
* [No stacktraces in logs when plugins throw exceptions](https://github.com/dtolabs/rundeck/pull/545)
* [Improve execution page info when option variables are used in node filters](https://github.com/dtolabs/rundeck/issues/543)
* [Rundeck prints secure option data in DEBUG into execution logs](https://github.com/dtolabs/rundeck/pull/542)
* [Java notification plugin Configuration map does not contain all values](https://github.com/dtolabs/rundeck/issues/540)
* [Cannot edit job references if upgrading from 1.6.0 to 1.6.1](https://github.com/dtolabs/rundeck/issues/539)
* [Can't change current project if not authorized for last viewed project](https://github.com/dtolabs/rundeck/issues/537)
* [using node rank attribute when dispatching will skip nodes if rank values are the same](https://github.com/dtolabs/rundeck/issues/535)
* [ Failed request: runJobInline . Result: Internal Server Error ](https://github.com/dtolabs/rundeck/issues/534)
* [rundeck does not remove remote dispatch files](https://github.com/dtolabs/rundeck/issues/531)
* [ldaps authentication CN validation fails when using alias providerUrl](https://github.com/dtolabs/rundeck/pull/482)
