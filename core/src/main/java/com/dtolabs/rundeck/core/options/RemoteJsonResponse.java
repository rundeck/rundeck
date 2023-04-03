package com.dtolabs.rundeck.core.options;

import lombok.Data;

import java.util.Date;

@Data
public class RemoteJsonResponse {
    Object json;
    Stats stats = new Stats();
    String error;

    @Data
    public static class Stats {
        String url;
        long startTime;
        long finishTime;
        long durationTime;
        int httpStatusCode;
        String httpStatusText;
        String contentSHA1;
        long contentLength;
        Date lastModifiedDate;
        long lastModifiedDateTime;
    }
}
