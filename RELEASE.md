Release 3.0.12
===========

Date: 2019-01-14

Name: <span style="color: indianred"><span class="glyphicon glyphicon-grain"></span> "jalapeño popper indianred grain"</span>

## Notes

This release addresses unhandled concurrency exceptions(lock timeouts and deadlocks) that may occur
when running multiple instances of referenced jobs.

Using `rundeck.disable.ref.stats=true` in framework.properties to disable referenced job statistics updates
can further reduce waits and retry `WARN` log entries.

Increasing the connection pool size may still be required if many referenced jobs are being run conccurently
and/or across many nodes:
```
dataSource.properties.maxActive=200
```

## Contributors

* Greg Schueler (gschueler)
* Jaime Tobar (jtobard)

## Bug Reporters

* gschueler
* mlamutt

## Issues

[Milestone 3.0.12](https://github.com/rundeck/rundeck/milestone/96)

* [Fix #4302 deadlock and allow disabling jobref stats ](https://github.com/rundeck/rundeck/pull/4388)
* [Could not roll back Hibernate transaction / Unable to rollback against JDBC Connection](https://github.com/rundeck/rundeck/issues/4302)
