package com.dtolabs.rundeck.util;

import java.util.List;

public class BlacklistYamlClass {
    List<BlacklistEntry> entries;
}

public class BlacklistEntry{
      String providerName;
      String fileName;
      String serviceType;
}
