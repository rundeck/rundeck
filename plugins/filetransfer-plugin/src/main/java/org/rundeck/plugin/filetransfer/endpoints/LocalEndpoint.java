package org.rundeck.plugin.filetransfer.endpoints;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.rundeck.plugin.filetransfer.DestEndpointHandler;
import org.rundeck.plugin.filetransfer.SourceEndpointHandler;

import java.io.*;
import java.net.URL;

/**
 * Created by Alberto Hormazabal C. on 10-06-16.
 */
public class LocalEndpoint {


  public static SourceEndpointHandler buildSourceHandler(final URL url) throws IOException {

    final File sourceFile = new File(url.getPath());
    if(!sourceFile.exists() || ! sourceFile.canRead() || !sourceFile.isFile()) {
      throw new IOException("Invalid File supplied: " + url.getPath());
    }

    return new SourceEndpointHandler() {
      @Override
      public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(sourceFile));
      }

      @Override
      public boolean finish() throws IOException {
        return true;
      }
    };

  }

  public static DestEndpointHandler buildDestHandler(final URL url) throws IOException {

    final File destFile = new File(url.getPath());

    return new DestEndpointHandler() {
      @Override
      public OutputStream getOutputStream() throws IOException {
        return new BufferedOutputStream(new FileOutputStream(destFile));
      }

      @Override
      public boolean finish() throws IOException {
        return true;
      }
    };

  }


}
