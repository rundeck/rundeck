package org.rundeck.app.data.model.v1.project;
import org.rundeck.app.data.model.v1.AuthTokenMode;
import org.rundeck.app.data.model.v1.AuthenticationToken;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public class SimpleProjectBuilder implements RdProject {

  private Serializable id;
  private String name;
  private String description;
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
  public String getDescription() {
        return description;
    }

  @Override
  public Date getDateCreated() {
    return dateCreated;
  }

  @Override
  public Date getLastUpdated() {
        return lastUpdated;
    }


  public SimpleProjectBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public SimpleProjectBuilder setDescription(String description) {
    this.description = description;
    return this;
  }


  public SimpleProjectBuilder setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
    return this;
  }



  public static SimpleProjectBuilder with(RdProject input) {
    SimpleProjectBuilder project1 = new SimpleProjectBuilder();
    project1.id = input.getId();
    project1.name = input.getName();
    project1.description = input.getDescription();
    project1.lastUpdated = input.getLastUpdated();

    return project1;
  }
}
