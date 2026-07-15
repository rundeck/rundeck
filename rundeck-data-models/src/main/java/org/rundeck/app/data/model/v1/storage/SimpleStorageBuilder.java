/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
  private Map<String,String> storageMeta;
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
