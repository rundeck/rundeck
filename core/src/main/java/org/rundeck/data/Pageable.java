package org.rundeck.data;

import lombok.Data;

@Data
public class Pageable {
    long offset = 0;
    long max = 200;
}
