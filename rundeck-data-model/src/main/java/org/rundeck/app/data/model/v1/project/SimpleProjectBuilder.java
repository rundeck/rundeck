package org.rundeck.app.data.model.v1.project;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

public class SimpleProjectBuilder implements RdProject {

  private Serializable id;
  private String name;
  private String description;
  private Date dateCreated;
  private Date lastUpdated;
  private State state;

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

  @Override
  public State getState() {
      return this.state;
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

  public SimpleProjectBuilder setState(State state) {
    this.state = state;
    return this;
  }

  public static SimpleProjectBuilder with(RdProject input) {
    SimpleProjectBuilder project1 = new SimpleProjectBuilder();
    project1.id = input.getId();
    project1.name = input.getName();
    project1.description = input.getDescription();
    project1.dateCreated = input.getDateCreated();
    project1.lastUpdated = input.getLastUpdated();
    project1.state = input.getState();

    return project1;
  }
}
