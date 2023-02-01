package org.rundeck.app.data.model.v1.user.dto;

import lombok.Data;
import org.rundeck.app.data.model.v1.user.RdUser;

import java.util.List;

@Data
public class UserFilteredResponse {
    Integer totalRecords;
    List<RdUser> users;
    boolean showLoginStatus;
}
