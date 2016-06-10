package org.rundeck.plugin.filetransfer.endpoints;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.rundeck.plugin.filetransfer.DestEndpointHandler;
import org.rundeck.plugin.filetransfer.SourceEndpointHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Created by Alberto Hormazabal C. on 10-06-16.
 */
public class FTPEndpoint {


  /**
   * Returns a SourceEndpointHandler for FTP protocol, already connected and logged in.
   *
   * @param url Complete Server/File URL
   * @param username Server username
   * @param password Server password
   * @return The newly created source handler.
   * @throws IOException On any comm error.
   */
  public static SourceEndpointHandler buildSourceHandler(final URL url, final String username, final String password) throws IOException {

    final FTPClient ftpClient = new FTPClient();
    ftpClient.connect(url.getHost(), url.getPort() < 0 ? url.getDefaultPort() : url.getPort());
    ftpClient.login(username, password);
    ftpClient.enterLocalPassiveMode();
    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

    return new SourceEndpointHandler() {

      @Override
      public InputStream getInputStream() throws IOException {
        InputStream is = ftpClient.retrieveFileStream(url.getPath());
        if (is == null) {
          throw new IOException(String.format("Error (%d) creating stream: %s", ftpClient.getReplyCode(), ftpClient.getReplyString()));

        }
        return is;
      }

      @Override
      public boolean finish() throws IOException {
        boolean ret = ftpClient.completePendingCommand();
        ftpClient.logout();
        ftpClient.disconnect();
        return ret;
      }
    };

  }


  /**
   * Returns a DestEndpointHandler for FTP protocol, already connected and logged in.
   *
   * @param url Complete Server/file URL
   * @param username Server username
   * @param password Server password
   * @return The newly created dest handler.
   * @throws IOException On any comm error.
   */
  public static DestEndpointHandler buildDestHandler(final URL url, final String username, final String password) throws IOException {

    final FTPClient ftpClient = new FTPClient();
    ftpClient.connect(url.getHost(), url.getPort() < 0 ? url.getDefaultPort() : url.getPort());
    ftpClient.login(username, password);
    ftpClient.enterLocalPassiveMode();
    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

    return new DestEndpointHandler() {

      @Override
      public OutputStream getOutputStream() throws IOException {
        OutputStream os = ftpClient.storeFileStream(url.getPath());
        if (os == null) {
          throw new IOException(String.format("Error (%d) creating stream: %s", ftpClient.getReplyCode(), ftpClient.getReplyString()));
        }

        return os;
      }

      @Override
      public boolean finish() throws IOException {
        boolean ret = ftpClient.completePendingCommand();
        ftpClient.logout();
        ftpClient.disconnect();
        return ret;
      }
    };

  }
}
