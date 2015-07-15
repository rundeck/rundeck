% Node Filters
% Greg Schueler
% February 10, 2014

## Node Filter Syntax

The basic format is a sequence of `attributename: value` pairs to select nodes which match.  You can negate a match by using `!attributename: value`.  If you do not specify an attribute name, then the `nodename` is matched against the bare value.

The `value` can be:

* a [regular-expression][] results in all nodes matching the regular expression for the attribute.
	- e.g. `www.*`
* a comma-separated list of values, in which case each item is treated as an *exact match*, results in all nodes matching any of the exact values for the attribute.
	- e.g.: `web1,web2`
* for the special `tags` attribute, a set of values, results in all nodes with a set of tags that match.
    - a comma-separated list of values to OR together
    - a `+`-separated list of values to AND together
    - e.g. `a+b` (match node with both tags `a` and `b`)
    - e.g. `a+b,x` (match node with both tags `a` and `b`, or tag `x`)

The result is all nodes which match all of the clauses.

You can include multiple attribute name/value pairs with the same attribute name,
in which case it effectively concatenates all of the values into a comma-separated list,
which will not use regular-expression matching for any of those values.

    name: web1 name: web2

Is the same as:

	name: web1,web2

This *does not* use regular-expression matching.  If you want to combine multiple regular expressions,
refer to the [regular-expression][] syntax. For example, you could use `anode.*|bnode.*` to match all
nodes starting with "anode" or "bnode".

*Examples:*

Match a set of nodes by name, includes all the node names listed:

    mynode1 mynode2

Include mynode and exclude mynode2:

    mynode1 !nodename: mynode2

Include nodes with both of the tags `www` and `prod` or either of the given hostnames:

    tags: www+prod hostname: dev1.example.com,dev2.example.com

[regular-expression]: https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html