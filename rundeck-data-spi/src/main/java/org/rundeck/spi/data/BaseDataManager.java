package org.rundeck.spi.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BaseDataManager
        implements DataManager
{
    private final Map<Class<?>, Object> providers = new HashMap<>();

    public <D> void registerDataProvider(Class<D> clazz, D provider)
    {
        providers.put(
                clazz,
                provider
        );

    }

    public <D> D getProviderForType(Class<D> clazz)  {
        return (D)providers.get(clazz);
    }

}
