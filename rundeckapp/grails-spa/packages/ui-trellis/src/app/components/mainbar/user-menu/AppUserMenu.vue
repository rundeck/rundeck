<template>
  <user-menu
    :username="username"
    :profile-link="profileLink"
    :logout-link="logoutLink"
  />
</template>

<script>
import UserMenu from "./UserMenu.vue";
import { getRundeckContext } from "../../../../library";

/**
 * AppUserMenu component - wraps UserMenu and provides data from Rundeck context
 */
export default {
  name: "AppUserMenu",
  components: { UserMenu },
  inject: ["rootStore"],
  data() {
    return {
      username: "",
      profileLink: "",
      logoutLink: "",
    };
  },
  mounted() {
    const context = getRundeckContext();
    this.username = context.profile?.username || "(Unknown User)";
    this.profileLink =
      context.profile?.links?.profile || `${context.rdBase}user/profile`;
    this.logoutLink =
      context.profile?.links?.logout || `${context.rdBase}user/logout`;
  },
};
</script>

<style scoped></style>
