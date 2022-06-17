package org.rundeck.spi.data;

import java.util.List;

public interface DataType<T> {
    String getName();

    String getVersion();

//    List<DataType<?>> getRelationTypes();

    Class<T> getJavaType();
}
