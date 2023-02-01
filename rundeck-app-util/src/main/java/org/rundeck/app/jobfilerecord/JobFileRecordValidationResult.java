package org.rundeck.app.jobfilerecord;

import lombok.Data;

import java.util.ArrayList;
@Data
public class JobFileRecordValidationResult {
    boolean valid;
    String error;
    ArrayList args;
}
