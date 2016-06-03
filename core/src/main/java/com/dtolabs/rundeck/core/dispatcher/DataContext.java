package com.dtolabs.rundeck.core.dispatcher;

import java.util.Map;

/**
 * Created by greg on 5/25/16.
 */
public interface DataContext extends Map<String, Map<String, String>>, Mergable<DataContext>{
    Map<String,Map<String,String>> getData();

}
