<template>
  <dropdown tag="div" placement="bottom-right" menu-right>
    <btn class="dropdown-toggle btn-menu-item" size="med" type="link">
      <i class="fas fa-user fa-lg"></i>
    </btn>
    <template #dropdown>
      <ul class="dropdown-menu dropdown-menu-right">
        <li>
          <div style="padding: 10px 15px">Hi {{ username }}!</div>
        </li>

        <li role="separator" class="divider"></li>
        <li>
          <a :href="profileLink">
            {{ $t("profile") }}
          </a>
        </li>
        <li>
          <a :href="logoutLink">
            {{ $t("logout") }}
          </a>
        </li>
      </ul>
    </template>
  </dropdown>
</template>

<script>
import { Dropdown } from "uiv";
import { getRundeckContext } from "@/library";

const context = getRundeckContext();
export default {
  name: "AppUserMenu",
  components: { Dropdown },
  inject: ["rootStore"],
  data() {
    return {
      username: "",
      profileLink: "",
      logoutLink: "",
    };
  },
  mounted() {
    this.username = context.profile?.username || "(Unknown User)";
    this.profileLink =
      context.profile?.links?.profile || `${context.rdBase}user/profile`;
    this.logoutLink =
      context.profile?.links?.logout || `${context.rdBase}user/logout`;
  },
};
</script>

<style scoped>
.btn.btn-menu-item {
  padding: 0;
  border: 0;
}
</style>
