package com.dtolabs.rundeck.core.audit;


/**
 * Contains information related to the request which triggered the event.
 */
public interface RequestInfo {

  /**
   * @return The hostname of the rundeck server which triggered the event.
   */
  String getServerHostname();

  /**
   * @return The UUID of the rundeck server which triggered the event.
   */
  String getServerUUID();

  /**
   * @return The ID of the session which triggered the event.
   */
  String getSessionID();

  /**
   * @return The User-Agent header content, from the request which triggered the event.
   */
  String getUserAgent();

}
