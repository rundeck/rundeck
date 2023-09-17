package com.dtolabs.rundeck.core.common;

/**
 * register, access, override services by name
 */
public interface IServicesRegistration {
    /**
     * @param name service name
     * @return a service by name
     */
    FrameworkSupportService getService(String name);

    /**
     * Set a service by name if not yet set, will not override existing registration
     *
     * @param name    name
     * @param service service, or null to remove the registration
     */
    void setService(String name, FrameworkSupportService service);

    /**
     * Override existing registration
     *
     * @param name    name
     * @param service service
     */
    void overrideService(String name, FrameworkSupportService service);
}
