package com.dtolabs.rundeck.core.audit;

import java.util.List;

/**
 * Contains information related to the user which triggered the event.
 */
public interface UserInfo {
  /**
   * @return The username which triggered the event.
   */
  String getUsername();

  /**
   * @return The roles associated with the user
   */
  List<String> getUserRoles();
}
