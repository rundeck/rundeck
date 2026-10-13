<template>
  <table class="userlist" cellpadding="0" cellspacing="0" width="100%">
    <user-list-row
      v-for="(user, index) in users"
      :key="user.login"
      :user="user"
      :index="index"
      :app-admin="appAdmin"
      :current-user="currentUser"
    />
  </table>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import UserListRow from "./UserListRow.vue";
import type { UserListEntry } from "./types/userListTypes";

export default defineComponent({
  name: "UserTable",
  components: { UserListRow },
  props: {
    users: {
      type: Array as PropType<UserListEntry[]>,
      required: true,
    },
    appAdmin: {
      type: Boolean,
      default: false,
    },
    currentUser: {
      type: String,
      default: "",
    },
  },
});
</script>

<style scoped lang="scss">
.userlist {
  width: 100%;
}
</style>
