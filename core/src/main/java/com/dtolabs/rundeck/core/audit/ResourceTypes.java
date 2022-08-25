package com.dtolabs.rundeck.core.audit;

/**
 * Types of resources reported by audit events.
 */
public final class ResourceTypes {

  public static final String USER = "user";
  public static final String PROJECT = "project";
  public static final String JOB = "job";
  public static final String SYSTEM_ACL = "system_acl";
  public static final String PROJECT_ACL = "project_acl";


  // unallow instantiation.
  private ResourceTypes() {
  }
}
