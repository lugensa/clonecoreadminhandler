#!/bin/sh

SOLRBASE="/home/myname/bo_solr/parts/solr-instance/"
SOLRDIST="$SOLRBASE/dist"
SOLRWEBAPP="$SOLRBASE/solr-webapp/webapp/WEB-INF/lib"
SOLRCONTRIB="$SOLRBASE/contrib"

CLASSPATH=".:"

for directory in "$SOLRDIST $SOLRCONTRIB $SOLRWEBAPP" ; do
    for jar in `find $directory -name '*.jar'` ; do
        CLASSPATH="$CLASSPATH:$jar"
    done
done

export CLASSPATH

javac com/lugensa/solr/plugins/CloneCoreAdminHandler.java
jar cf clonecoreadminhandler.jar com
