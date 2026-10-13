/**
 * A single user record as embedded by UserController.list() and rendered by the
 * user list page. Fields may be blank/undefined depending on the backing
 * FeatureAwareUserDataProvider implementation (local Gorm vs. remote RBA service).
 */
export interface UserListEntry {
  login: string;
  firstName?: string;
  lastName?: string;
  email?: string;
}

/**
 * Shape of the server-embedded data contract for the user list page, read via
 * getRundeckContext().data.userListData.
 */
export interface UserListData {
  users: UserListEntry[];
  appAdmin: boolean;
  currentUser: string;
}
