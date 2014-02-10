% Remote option provider

## Yum package list CGI

[Yum] is a great tool for automating [RPM] package management. With Yum,
administrators can publish packages to the repository and then use the
yum client tool to automate the installation of packages along with
their declared dependencies. Yum includes a command
called [repoquery] useful for
querying Yum repositories similarly to rpm queries.

Acme Anvils set up their own Yum repository to distribute application release
packages. The Acme administrator wants to provide an option model to Jobs that
need to know what packages provide a given capability.

The code listing below shows it is a simple wrapper around the
repoquery command that formats the results as [JSON] data.

File listing: repoquery.cgi
    
~~~~~~~~ {.bash .numberLines}    
#!/bin/bash
# Requires: repoquery
# 
# Query Params and their defaults
repo=acme-staging
label="Anvils Release"
package=anvils
max=30
#
echo Content-type: application/json
echo ""
for VAR in `echo $QUERY_STRING | tr "&" "\t"`
do
  NAME=$(echo $VAR | tr = " " | awk '{print $1}';);
  VALUE=$(echo $VAR | tr = " " | awk '{ print $2}' | tr + " ");
  declare $NAME="$VALUE";
done

echo '{'
repoquery --enablerepo=$repo --show-dupes \
  --qf='"${label} %{VERSION}-%{RELEASE}":"%{NAME}-%{VERSION}-%{RELEASE}",' \
  -q --whatprovides ${package} | sort -t - -k 4,4nr | head -n${max}
echo '}'
~~~~~~~~

After deploying this script to the CGI enabled directory on the
operations web server, it can be tested directly by requesting it using [curl].

~~~~~~~~ {.bash}
curl -d "repo=acme&label=Anvils&package=anvils" \
    --get http://yum.acme.com/cgi-bin/repoquery.cgi
~~~~~~~~

The server response should return JSON data resembling the example below:

~~~~~~~~ {.json}
[ 
  {"name":"anvils-1.1.rpm", "value":"/dist/RPMS/noarch/anvils-1.1.rpm"}, 
  {"name":"anvils-1.2.rpm", "value":"/dist/RPMS/noarch/anvils-1.2.rpm"} 
]
~~~~~~~~
    
Jobs can request the option model data like so:

~~~~~~~~ {.xml}
<option name="package" enforcedvalues="true" required="true"
    valuesUrl="http://yum.acme.com/cgi-bin/repoquery.cgi?package=anvils"/> 
~~~~~~~~

The Rundeck UI will display the package names in the menu and once
selected, the Job will have the matching package versions.
 



[Yum]: http://yum.baseurl.org/
[RPM]: http://www.rpm.org/
[curl]: http://linux.die.net/man/1/curl
[repoquery]: http://linux.die.net/man/1/repoquery
[json]: http://www.json.org
