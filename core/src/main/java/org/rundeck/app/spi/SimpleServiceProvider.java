package org.rundeck.app.spi;

import java.util.Map;

/**
 * A simple implementation of Services and ServicesProvider that returns
 * services from the map supplied during construction.
 */
public class SimpleServiceProvider implements Services, ServicesProvider {
    final private Map<Class<? extends AppService>, Object> services;

    /**
     * @param services Map of service types to service instances
     */
    public SimpleServiceProvider(Map<Class<? extends AppService>, Object> services) {
        this.services = services;
    }

    @Override
    public boolean hasService(final Class<? extends AppService> type) {
        return services.containsKey(type);
    }

    @Override
    public <T extends AppService> T getService(final Class<T> type) {
        if (hasService(type)) {
            return (T) services.get(type);
        }
        throw new IllegalStateException("Required service " + type.getName() + " was not available");
    }

    @Override
    public Services getServices() {
        return this;
    }
}
