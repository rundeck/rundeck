<template>
  <tr :class="{ alternateRow: isAlternate }">
    <td>
      <span class="userlogin">{{ user.login }}</span>
      <span class="username">{{ fullName }}</span>
      <span v-if="user.email" class="useremail">{{ emailDisplay }}</span>

      <span v-if="editAuthAllowed" class="useredit">
        <a :href="editLink" class="textbtn textbtn-info textbtn-on-hover">
          <i class="glyphicon glyphicon-edit"></i>
          {{ $t("button.action.edit") }}
        </a>
      </span>
    </td>
  </tr>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { getRundeckContext } from "@/library";

/**
 * Shape of a single user row consumed by the list view. Only safe display
 * fields are exposed by the server (login, firstName, lastName, email).
 */
interface UserRow {
  login: string;
  firstName?: string;
  lastName?: string;
  email?: string;
}

/**
 * Renders a single row of the admin user list, porting the display behaviour
 * of `_userListItem.gsp`: login, full name, optional email (wrapped in angle
 * brackets), and an auth-gated link to the edit action.
 *
 * The legacy GSP also rendered an expander toggle that revealed the heavy
 * `_user.gsp` profile/token partial; in the list action that partial has no
 * backing data and is dead markup, so it is intentionally not ported here.
 */
export default defineComponent({
  name: "UserListRow",
  props: {
    /** The user to display. */
    user: {
      type: Object as PropType<UserRow>,
      required: true,
    },
    /** Whether the current admin may edit users (gates the edit link). */
    editAuthAllowed: {
      type: Boolean,
      default: false,
    },
    /** Zero-based row index, used only for zebra striping. */
    index: {
      type: Number,
      default: 0,
    },
  },
  computed: {
    /** Full name composed of first and last name, trimmed of stray spaces. */
    fullName(): string {
      return [this.user.firstName, this.user.lastName]
        .filter((part) => !!part)
        .join(" ");
    },
    /** Email presented in angle brackets to match the legacy GSP rendering. */
    emailDisplay(): string {
      return `<${this.user.email}>`;
    },
    /** Whether this row should receive the alternate (zebra) styling. */
    isAlternate(): boolean {
      return this.index % 2 === 1;
    },
    /** Server-relative link to the user edit action for this login. */
    editLink(): string {
      const base = getRundeckContext().rdBase || "";
      const normalized = base.endsWith("/") ? base.slice(0, -1) : base;
      return `${normalized}/user/edit?login=${encodeURIComponent(
        this.user.login,
      )}`;
    },
  },
});
</script>

<style scoped lang="scss">
.alternateRow {
  background-color: var(--background-color-accent, #f7f7f7);
}

.userlogin {
  font-weight: bold;
}

.username {
  margin-left: 8px;
}

.useremail {
  margin-left: 8px;
  color: var(--text-secondary-color, #777);
}

.useredit {
  margin-left: 8px;
}
</style>
