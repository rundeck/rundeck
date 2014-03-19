package org.rundeck.storage.data.file;

import org.rundeck.storage.api.Path;

import java.io.File;

/**
 * $INTERFACE is ... User: greg Date: 2/18/14 Time: 11:11 AM
 */
public interface FilepathMapper {
    File directoryForPath(Path path);
    File contentFileForPath(Path path);
    File metadataFileFor(Path path);
    Path pathForContentFile(File datafile);
    Path pathForMetadataFile(File metafile);
    Path pathForDirectory(File directory);
}
