% Node Filters
% Greg Schueler
% February 10, 2014

## Node Filter Syntax

The basic format is a sequence of `attributename: value` pairs to select nodes which match.  You can negate a match by using `!attributename: value`.  If you do not specify an attribute name, then the `nodename` is matched against the bare value.

The `value` can be:

* a regular expression
* a comma-separated list of values, in which case each item is treated as a regular expression
* for the special `tags` attribute, a set of values:
    - a comma-separated list of values to OR together
    - a `+`-separated list of values to AND together

*Examples:*

Match a set of nodes by name, includes all the node names listed:

    mynode1 mynode2

Include mynode and exclude mynode2:

    mynode1 !nodename: mynode2

Include nodes matching the tags `www` and `prod` or the hostnames:

    tags: www+prod hostname: dev1.example.com,dev2.example.com
