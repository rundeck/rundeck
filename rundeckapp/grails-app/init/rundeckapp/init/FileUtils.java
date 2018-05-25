package rundeckapp.init;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

/**
 * @author Alberto Hormazabal Cespedes
 * @author exaTech Ingenieria SpA. (info@exatech.cl)
 */
public class FileUtils {
  public static void appendFile(final File test, final File origName) throws IOException {

    try (
        FileChannel inc = FileChannel.open(test.toPath(), StandardOpenOption.READ);
        FileChannel outc = FileChannel.open(
            origName.toPath(),
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND
        );
    ) {
      inc.transferTo(0, inc.size(), outc);
    }
  }

}
