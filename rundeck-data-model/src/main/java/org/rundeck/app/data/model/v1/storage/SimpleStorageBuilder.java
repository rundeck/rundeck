package org.rundeck.app.data.model.v1.storage;
import lombok.Builder;
import lombok.Data;
import org.rundeck.storage.api.Path;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
@Builder
public class SimpleStorageBuilder implements RundeckStorage {

  private Serializable id;
  private String namespace;
  private Map storageMeta;
  private byte[] data;
  private Date dateCreated;
  private Date lastUpdated;
  private Path path;

  public static SimpleStorageBuilder with(RundeckStorage input) {
    return SimpleStorageBuilder.builder()
            .id(input.getId())
            .lastUpdated(new Date())
            .dateCreated(input.getDateCreated())
            .namespace(input.getNamespace())
            .path(input.getPath())
            .storageMeta(input.getStorageMeta())
            .data(input.getData())
            .build();
  }
}
