% Tutorials
% Alex Honor; Greg Schueler
% November 20, 2010

This chapter presents working examples reflecting a variety of
solutions you can create with Rundeck. Helping you apply concepts
and features introduced in earlier chapters is the focus of these examples.
To give them some context, the examples are about solutions used
by Acme Anvils, a fictitious organization that manages an online
e-commerce application. 

## Acme Anvils 

Acme Anvils is a hypothetical start up selling new and used anvils from
their web site.  Two teams inside the company are involved with the
development and support of the anvil sales application. Being a new
company, there isn't much control over access to
the live environment. Either team can push changes to systems which
has led to mistakes and outages. Because the senior management is so
enthusiastic, they drive the teams to deliver new features as
frequently as possible. Unfortunately, this has led to another
problem: the Acme Anvil web tier sometimes locks up and other
times runs out of memory requiring occasional restarts.  

There are actually two methods to the restart procedure depending
on the problem: "force" versus "normal". The "force" restart is required
when the application becomes totally unresponsive. The "normal" restart
occurs when the application needs to free memory.

Depending on the urgency or the staff on hand, either a developer or
an administrator conducts the restart, albeit differently. Because the
developers write the software, they understand the restart
requirements from an application perspective. The administrators on
the other hand, are not always informed of these requirements but are
well versed in restarting the application from a systems
perspective. This has led to a divergence in procedures and has become
the main source of problems that affect their customers.

An administrator, tired of the late night calls to restart the
application, and frustrated by the knowledge gap between operations and
development has decided to take the initiative come up with a better
approach.
