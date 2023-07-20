package org.rundeck.app.job.option;

import lombok.Data;
import org.rundeck.app.data.model.v1.job.option.OptionData;

import java.util.Map;

@Data
public class RemoteOptionValuesResponse {
    OptionData optionSelect;
    Object values;
    String srcUrl;
    Map err;

}
