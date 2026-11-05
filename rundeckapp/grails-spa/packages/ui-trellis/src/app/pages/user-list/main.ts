import { createApp } from "vue";
import * as uiv from "uiv";

import UserListPage from "./UserListPage.vue";
import { getRundeckContext } from "../../../library";
import {
  initI18n,
  commonAddUiMessages,
  type LocalizedMessages,
} from "../../utilities/i18n";
import { UiMessage } from "../../../library/stores/UIStore";
import type { UserListData } from "../../components/user-list/types/userListTypes";

const i18n = initI18n();

const elm = document.getElementById("userListPage");

const rundeckContext = getRundeckContext();
const userListData: UserListData = rundeckContext.data?.userListData || {
  users: [],
  appAdmin: false,
  currentUser: "",
};

const vue = createApp({
  name: "UserListApp",
  components: { UserListPage },
  data() {
    return {
      users: userListData.users || [],
      appAdmin: !!userListData.appAdmin,
      currentUser: userListData.currentUser || "",
    };
  },
  template: `<user-list-page :users="users" :app-admin="appAdmin" :current-user="currentUser" />`,
});
vue.use(uiv);
vue.use(i18n);
vue.provide(
  "addUiMessages",
  async (messages: UiMessage[] | LocalizedMessages) =>
    commonAddUiMessages(i18n, messages),
);
if (elm) {
  vue.mount(elm);
}
