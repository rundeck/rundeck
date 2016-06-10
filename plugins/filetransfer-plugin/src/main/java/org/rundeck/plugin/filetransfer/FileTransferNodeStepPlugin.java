package org.rundeck.plugin.filetransfer;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.Password;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import org.apache.commons.net.io.Util;
import org.apache.log4j.Logger;
import org.rundeck.plugin.filetransfer.endpoints.FTPEndpoint;
import org.rundeck.plugin.filetransfer.endpoints.LocalEndpoint;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;


/**
 * $INTERFACE is ... User: greg Date: 10/31/13 Time: 2:58 PM
 */
@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = FileTransferNodeStepPlugin.TYPE)
@PluginDescription(title = "Transfer File", description = "Transfer a file to or from a remote node.")
public class FileTransferNodeStepPlugin implements NodeStepPlugin {

  public static final String TYPE = "filetransfer";

  private static final Logger LOG = Logger.getLogger(FileTransferNodeStepPlugin.class.getName());

  @PluginProperty(title = "Source URL", required = true, defaultValue = "file:///", description = "URL for the source host. Supported protocols are: file, ftp")
  private String sourceURLString;

  @PluginProperty(title = "Source Username", required = false, description = "Username for the source server. Required only if protocol is not file://")
  private String sourceUsername;

  @Password
  @PluginProperty(title = "Source Password", required = false, description = "Password for the source ftp client. Required only if protocol is not file://")
  private String sourcePassword;

  @PluginProperty(title = "Destination URL", required = true, defaultValue = "ftp:///", description = "URL for the destination file. Supported protocols are: file, ftp")
  private String destURLString;

  @PluginProperty(title = "Destination Username", required = false, description = "Username for the destination server. Required only if protocol is not file://")
  private String destUsername;

  @Password
  @PluginProperty(title = "Destination Password", required = false, description = "Password for the source ftp client. Required only if protocol is not file://")
  private String destPassword;


  public enum Reason implements FailureReason {
    BADURL
  }

  @Override
  public void executeNodeStep(PluginStepContext context, Map<String, Object> configuration,
                              INodeEntry entry) throws NodeStepException {

    try {

      URL sourceURL = new URL(sourceURLString);
      URL destURL = new URL(destURLString);

      SourceEndpointHandler sourceEndpoint;
      DestEndpointHandler destEndpoint;

      // Create source endpoint.
      switch (sourceURL.getProtocol()) {
        case "file":
          sourceEndpoint = LocalEndpoint.buildSourceHandler(sourceURL);
          break;

        case "ftp":
          sourceEndpoint = FTPEndpoint.buildSourceHandler(sourceURL, sourceUsername, sourcePassword);
          break;

        default:
          throw new IllegalArgumentException("Invalid protocol specified in source URL");
      }

      // Create source endpoint.
      switch (destURL.getProtocol()) {
        case "file":
          destEndpoint = LocalEndpoint.buildDestHandler(destURL);
          break;

        case "ftp":
          destEndpoint = FTPEndpoint.buildDestHandler(destURL, destUsername, destPassword);
          break;

        default:
          throw new IllegalArgumentException("Invalid protocol specified in dest URL");
      }


      // Start Copy
      try (
          InputStream sourceInputStream = sourceEndpoint.getInputStream();
          OutputStream destOutputStream = destEndpoint.getOutputStream()
      ) {
        Util.copyStream(sourceInputStream, destOutputStream);
      }
      // At this point the streams were closed automatically.

      sourceEndpoint.finish();
      destEndpoint.finish();


    } catch (Exception e) {
      LOG.error("Error: " + e.getMessage());
      throw new NodeStepException(e, Reason.BADURL, entry.getNodename());

    }

  }


}
