package com.lugensa.solr.plugins;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.admin.CoreAdminHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CloneCoreAdminHandler extends CoreAdminHandler {
    private static Logger log = LoggerFactory.getLogger(CloneCoreAdminHandler.class);

    public CloneCoreAdminHandler() {
        super();
    }

    public CloneCoreAdminHandler(final CoreContainer coreContainer) {
        super(coreContainer);
    }

    protected void handleCustomAction(SolrQueryRequest req, SolrQueryResponse rsp) {
        SolrParams params = req.getParams();
        String action = params.get(CoreAdminParams.ACTION);
        if(action != null) {
            switch(action) {
                case "CLONE": {
                    this.handleCloneAction(req, rsp);
                    break;
                }

                default: {
                    break;
                }
            }
        }
    }

    protected void handleCloneAction(SolrQueryRequest req, SolrQueryResponse rsp) {
        log.info("handle custom action");

        SolrParams params = req.getParams();

        String targetCoreName = params.get("targetCore");
        String sourceCoreName = params.get("sourceCore");

        if  (targetCoreName == null || sourceCoreName == null) {
            String msg = "Either 'targetCore' or 'sourceCore' must be specified";
            throw new SolrException(ErrorCode.BAD_REQUEST, msg);
        }

        if (coreContainer.getAllCoreNames().contains(targetCoreName)) {
            String msg = "Creating a core with existing name (targetCore=" +
                    targetCoreName + ") is not allowed";
            log.error(msg);
            throw new SolrException(ErrorCode.SERVER_ERROR, msg);
        }

        CoreDescriptor source = coreContainer.getCoreDescriptor(
            sourceCoreName);

        if (source == null) {
            String msg = "The source " + sourceCoreName + " does not exists";
            log.error(msg);
            throw new SolrException(ErrorCode.SERVER_ERROR, msg);
        }

        String sourceInstanceDir = source.getInstanceDir();
        String targetInstanceDir = sourceInstanceDir.replace(
            sourceCoreName, targetCoreName);

        log.info("new targetInstanceDir is:" + targetInstanceDir);

        /*
        File targetDirectory = new File(targetInstanceDir);
        targetDirectory.mkdirs();
        */

        File sourceInstance = new File(sourceInstanceDir);
        File targetInstance = new File(targetInstanceDir);

        try {
            FileUtils.copyDirectory(sourceInstance, targetInstance);
        } catch (IOException e) {
            String msg = "Can not copy instance files from " +
                         sourceInstanceDir +
                         " to " + targetInstanceDir;

            log.error(msg);
            throw new SolrException(ErrorCode.SERVER_ERROR, msg);
        }

        SolrCore sourceCore = coreContainer.getCore(sourceCoreName);
        SolrConfig sourceConfig = sourceCore.getSolrConfig();

        String sourceDataDir = sourceCore.getDataDir();
        String targetDataDir = sourceDataDir.replace(sourceCoreName,
            targetCoreName);

        File targetDataDirectory = new File(targetDataDir);
        targetDataDirectory.mkdirs();

        /* Now replace document-2.0 occurrences in solrconfig.xml
         * and schema.xml (<schema name="..."> and <dataDir>...</dataDir>)
         */
        String configDir = targetInstanceDir + "conf/";
        String solrconfig = configDir + sourceConfig.getName();
        String schema = configDir + sourceCore.getSchemaResource();
        File solrconfigFile = new File(solrconfig);
        File schemaFile = new File(schema);

        try {
            replaceName(solrconfigFile, sourceCoreName, targetCoreName);
        } catch (IOException e) {
            String msg = "Can not replace names in " + solrconfig;
            log.error(msg);
            throw new SolrException(ErrorCode.SERVER_ERROR, msg);
        }

        try {
            replaceName(schemaFile, sourceCoreName, targetCoreName);
        } catch (IOException e) {
            String msg = "Can not replace names in " + solrconfig;
            log.error(msg);
            throw new SolrException(ErrorCode.SERVER_ERROR, msg);
        }

        // prepare a new request for CREATE action
        NamedList<Object> paramValues = new NamedList();
        paramValues.add("action", "CREATE");
        paramValues.add("name", targetCoreName);
        // instanceDir is the dir beneth solr home
        paramValues.add("instanceDir", targetCoreName);
        params = SolrParams.toSolrParams(paramValues);
        req.setParams(params);
        super.handleCreateAction(req, rsp);

    }

    public static void replaceName(File file, String source, String target)
            throws IOException {

        // we need to store all the lines
        List<String> lines = new ArrayList<String>();

        // first, read the file and store the changes
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line = in.readLine();
        while (line != null) {
            line = line.replace(source, target);
            lines.add(line);
            line = in.readLine();
        }
        in.close();

        // now, write the file again with the changes
        PrintWriter out = new PrintWriter(file);
        for (String l : lines)
            out.println(l);
        out.close();
    }
}
