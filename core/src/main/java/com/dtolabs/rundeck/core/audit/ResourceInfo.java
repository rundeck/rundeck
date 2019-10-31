package com.dtolabs.rundeck.core.audit;

/**
 * Information about the resource affected by this event.
 */
public interface ResourceInfo {
  /**
   * Gets the type of the resource associated with this event.
   * Supported values can be found at @{@link ResourceTypes}
   * @return Type of the resource that generated the event. Null if does not apply.
   */
  String getType();


  /**
   * @return Name or ID of the resource that generated the event. Null if does not apply.
   */
  String getName();

}
