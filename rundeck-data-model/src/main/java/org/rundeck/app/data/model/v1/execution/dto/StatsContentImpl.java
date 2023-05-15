package org.rundeck.app.data.model.v1.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsContentImpl implements StatsContent{
    Long execCount;
    Long totalTime;
    Long refExecCount;
}
