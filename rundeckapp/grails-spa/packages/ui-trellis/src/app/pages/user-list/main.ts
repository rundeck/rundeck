import { createApp, h } from "vue";
import * as uiv from "uiv";

import UserListPage from "./UserListPage.vue";
import { initI18n } from "../../utilities/i18n";
import { loadJsonData } from "../../utilities/loadJsonData";

const i18n = initI18n();

const elm = document.getElementById("userListPage");

const data = loadJsonData("userListData") || {};

const vue = createApp({
  name: "UserListApp",
  render() {
    return h(UserListPage, {
      users: data.users || [],
      createAuthAllowed: data.createAuthAllowed === true,
      editAuthAllowed: data.editAuthAllowed === true,
    });
  },
});
vue.use(uiv);
vue.use(i18n);
if (elm) {
  vue.mount(elm);
}
