clonecoreadminhandler
=====================

A solr plugin which allows to clone an existing core.

The plugin extends the solr CoreAdmin (http://wiki.apache.org/solr/CoreAdmin)
with an additional CLONE action.

We use this plugin in functional tests fixture setup
for running multiple tests in parallel.

Usage:

http://localhost:8983/solr/admin/cores?action=CLONE&sourceCore=myexistingcore&targetCore=newcore

sourceCore: name of an existing core
targetCore: name of the cloned core

The configuration (solrconfig.xml, schema.xml) from the sourceCore
will be copied to a newly created instance dir.

All occurrences of sourceCore in theses config files will be
replaced with the new name.

To make this plugin available do the following:

- change into the solr home directory (e.g. solr)
- create a lib in solr home directory (e.g. solr/lib)
- copy the jar clonecoreadminhandler.jar to solr/lib
- edit solr/solr.xml and set the 'adminHandler':
    <cores adminHandler="com.lugensa.solr.plugins.CloneCoreAdminHandler" ...

This plugin requires solr >= 4.5
