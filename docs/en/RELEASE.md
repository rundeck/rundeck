Release 1.4.5
===========

Date: 1/10/2013

This release is a minor bugfix update, with some bonus features.

Notable Changes:

* bug fixes 
    * dispatch -s, some API project filtering was wrong, node dispatch threadcount can be set blank
* Bulk delete jobs via GUI
* Job page group filters now apply to Now Running and History areas
* History views don't use 1 day as a filter by default anymore

Issues: 

* [maint-1.4.5: dispatch -s scriptfile is broken](https://github.com/dtolabs/rundeck/issues/266)
* [maint-1.4.5: API: Now running execution project filter is not exact](https://github.com/dtolabs/rundeck/issues/265)
* [Bulk delete of jobs via GUI](https://github.com/dtolabs/rundeck/issues/245)
* [Node dispatch threadcount can be set to blank. export+import fails.](https://github.com/dtolabs/rundeck/issues/244)
* [Allow group path in URL of jobs page to filter groups](https://github.com/dtolabs/rundeck/issues/243)
* [History project filter is not exact](https://github.com/dtolabs/rundeck/issues/242)
* [History views default to recentFilter=1d, should be all events](https://github.com/dtolabs/rundeck/issues/240)
* [Now running and History views don't use Job view filter](https://github.com/dtolabs/rundeck/issues/239)
