<template>
  <div class="col-sm-10 col-sm-offset-1">
    <h3 class="user-list-heading">
      {{ $t("user.list.heading") }}
      <a
        v-if="createAuthAllowed"
        :href="createLink"
        class="btn btn-default btn-xs"
      >
        <i class="glyphicon glyphicon-plus"></i>
        {{ $t("user.list.newProfile") }}
      </a>
    </h3>

    <table cellpadding="0" cellspacing="0" width="100%" class="userlist">
      <tbody>
        <user-list-row
          v-for="(user, index) in users"
          :key="user.login"
          :user="user"
          :index="index"
          :edit-auth-allowed="editAuthAllowed"
        />
      </tbody>
    </table>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { getRundeckContext } from "@/library";
import UserListRow from "./UserListRow.vue";

/**
 * Shape of a single user row consumed by the list view. Only safe display
 * fields are serialized server-side (login, firstName, lastName, email);
 * password and session fields are never sent to the client.
 */
interface UserRow {
  login: string;
  firstName?: string;
  lastName?: string;
  email?: string;
}

/**
 * Vue port of the admin user list (`user/list.gsp`). Renders the page heading,
 * an auth-gated "New Profile" create link, and a table of users delegating
 * each row to {@link UserListRow}. All data is supplied server-side via the
 * embedded `userListData` JSON; the page performs no XHR.
 */
export default defineComponent({
  name: "UserListPage",
  components: { UserListRow },
  props: {
    /** Users to display, ordered by login. */
    users: {
      type: Array as PropType<UserRow[]>,
      default: () => [],
    },
    /** Whether the current admin may create users (gates the create link). */
    createAuthAllowed: {
      type: Boolean,
      default: false,
    },
    /** Whether the current admin may edit users (gates per-row edit links). */
    editAuthAllowed: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    /** Server-relative link to the user create action. */
    createLink(): string {
      const base = getRundeckContext().rdBase || "";
      const normalized = base.endsWith("/") ? base.slice(0, -1) : base;
      return `${normalized}/user/create`;
    },
  },
});
</script>

<style scoped lang="scss">
.user-list-heading {
  display: flex;
  align-items: center;
  gap: 8px;
}

.userlist {
  width: 100%;
}
</style>
