package org.rundeck.spi.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BaseDataManager
        implements DataManager
{
    private final Map<String, Object> providers = new HashMap<>();

    public void registerDataProvider(
            Object dataProvider
    )
    {
        providers.put(
                dataProvider.getClass().getSimpleName(),
                dataProvider
        );

    }

    public Object getProviderForType(String className) {
        return (Object) providers.get(className);
    }
}
