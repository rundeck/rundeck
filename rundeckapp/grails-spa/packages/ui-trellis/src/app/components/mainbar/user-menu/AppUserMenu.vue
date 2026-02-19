<template>
  <mainbar-menu
    :sub-header="subHeader"
    icon-css="fas fa-user fa-lg"
    :links="links"
  />
</template>

<script>
import { getRundeckContext } from "../../../../library";
import MainbarMenu from "../../../..//library/components/mainbar/MainbarMenu.vue";

/**
 * AppUserMenu component - wraps MainbarMenu and provides data from Rundeck context
 */
export default {
  name: "AppUserMenu",
  components: { MainbarMenu },
  inject: ["rootStore"],
  data() {
    return {
      username: "",
      profileLink: "",
      logoutLink: "",
    };
  },
  computed: {
    subHeader() {
      return `Hi ${this.username}!`;
    },
    links() {
      return [
        {
          title: this.$t("profile"),
          url: this.profileLink,
          separator: true,
        },
        {
          title: this.$t("logout"),
          url: this.logoutLink,
        },
      ];
    },
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
