package com.dtolabs.rundeck.app.api.marshall

interface ApiVersionSupplier {
    int getCurrentVersion()
    Map<String,Integer> getApiVersionNames()
}