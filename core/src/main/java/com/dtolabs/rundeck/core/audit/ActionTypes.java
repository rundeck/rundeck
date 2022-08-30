package com.dtolabs.rundeck.core.audit;

/**
 * Types of actions that generate audit events.
 */
public final class ActionTypes {

  public static final String LOGIN_SUCCESS = "login_success";
  public static final String LOGIN_FAILED = "login_failed";
  public static final String LOGOUT = "logout";
  public static final String VIEW = "view";
  public static final String READ = "read";
  public static final String CREATE = "create";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";
  public static final String RUN = "run";


  // unallow instantiation
  private ActionTypes() {
  }
}
