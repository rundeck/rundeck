package org.rundeck.app.data.model.v1.user.dto;

import lombok.Data;
import org.rundeck.app.data.model.v1.user.RdUser;

@Data
public class SaveUserResponse {
    RdUser user;
    Boolean isSaved;
    Object errors;
}
