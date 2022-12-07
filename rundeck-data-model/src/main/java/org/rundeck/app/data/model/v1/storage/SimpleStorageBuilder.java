package org.rundeck.app.data.model.v1.storage;
import org.rundeck.app.data.model.v1.project.RdProject;

import java.io.Serializable;
import java.util.Date;

public class SimpleStorageBuilder implements RundeckStorage {

  private Serializable id;
  private String namespace;
  private String dir;
  private String name;
  private String jsonData;
  private String pathSha;
  private byte[] data;
  private Date dateCreated;
  private Date lastUpdated;


  @Override
  public Serializable getId() {
        return id;
    }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getNamespace() { return namespace;}

  @Override
  public String getDir() { return dir;}

  @Override
  public String getJsonData() { return jsonData; }

  @Override
  public String getPathSha() { return pathSha; }

  @Override
  public byte[] getData() { return data; }

  @Override
  public Date getDateCreated() {
    return dateCreated;
  }

  @Override
  public Date getLastUpdated() {
        return lastUpdated;
    }


  public SimpleStorageBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public SimpleStorageBuilder setNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public SimpleStorageBuilder setDir(String dir) {
    this.dir = dir;
    return this;
  }
  public SimpleStorageBuilder setData(byte[] data) {
    this.data = data;
    return this;
  }
  public SimpleStorageBuilder setJsonData(String jsonData) {
    this.jsonData = jsonData;
    return this;
  }
  public SimpleStorageBuilder setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
    return this;
  }



  public static SimpleStorageBuilder with(RundeckStorage input) {
    SimpleStorageBuilder storage1 = new SimpleStorageBuilder();
    storage1.id = input.getId();
    storage1.name = input.getName();
    storage1.lastUpdated = new Date();
    storage1.dateCreated = input.getDateCreated();
    storage1.namespace = input.getNamespace();
    storage1.dir = input.getDir();
    storage1.jsonData = input.getJsonData();
    storage1.pathSha = input.getPathSha();
    storage1.data = input.getData();
    return storage1;
  }
}
