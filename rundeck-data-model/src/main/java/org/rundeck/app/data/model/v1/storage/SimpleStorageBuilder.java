package org.rundeck.app.data.model.v1.storage;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
@Builder
public class SimpleStorageBuilder implements RundeckStorage {

  private Serializable id;
  private String namespace;
  private String dir;
  private String name;
  private String pathSha;
  private Map storageMeta;
  private byte[] data;
  private Date dateCreated;
  private Date lastUpdated;

  public static SimpleStorageBuilder with(RundeckStorage input) {
    return SimpleStorageBuilder.builder()
            .id(input.getId())
            .lastUpdated(new Date())
            .dateCreated(input.getDateCreated())
            .namespace(input.getNamespace())
            .dir(input.getDir())
            .name(input.getName())
            .pathSha(input.getPathSha())
            .storageMeta(input.getStorageMeta())
            .data(input.getData())
            .build();
  }
}
