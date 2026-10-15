import { createApp } from "vue";
import * as uiv from "uiv";

import { getRundeckContext } from "../../../library";
import { initI18n } from "../../utilities/i18n";
import type { AclCreatePageData } from "../../components/menu-create-project-acl-file/types/aclFileTypes";
import MenuCreateProjectAclFilePage from "./MenuCreateProjectAclFilePage.vue";

const element = document.getElementById("menu-create-project-acl-file");
const context = getRundeckContext();
const pageData = context.data.aclCreatePageData as unknown as AclCreatePageData;

if (element && pageData) {
  const app = createApp(MenuCreateProjectAclFilePage, { pageData });
  app.use(uiv);
  app.use(initI18n());
  app.mount(element);
}
