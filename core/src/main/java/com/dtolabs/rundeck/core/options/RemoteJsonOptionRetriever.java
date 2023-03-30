package com.dtolabs.rundeck.core.options;

public interface RemoteJsonOptionRetriever {
    /**
     * Make a remote URL request and return the parsed JSON data and statistics for http requests in a map.
     * if an error occurs, a map with a single 'error' entry will be returned.
     * the stats data contains:
     *
     * url: requested url
     * startTime: start time epoch ms
     * httpStatusCode: http status code (int)
     * httpStatusText: http status text
     * finishTime: finish time epoch ms
     * durationTime: duration time in ms
     * contentLength: response content length bytes (long)
     * lastModifiedDate: Last-Modified header (Date)
     * contentSHA1: SHA1 hash of the content
     *
     * @param url URL to request
     * @param timeout request timeout in seconds
     * @param contimeout connection timeout in seconds
     * @param retry number of times to retry the request
     * @param disableRemoteOptionJsonCheck require return type to be 'application/json'
     * @return RemoteJsonResponse of data, [json: parsed json or null, stats: stats data, error: error message]
     *
     */
    RemoteJsonResponse getRemoteJson(String url, int timeout, int contimeout, int retry,boolean disableRemoteOptionJsonCheck);

}
