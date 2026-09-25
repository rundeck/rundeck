<template>
  <tbody>
    <tr :class="rowClass">
      <td class="expander-cell">
        <button
          type="button"
          class="expander-toggle"
          :aria-expanded="expanded"
          :data-testid="'user-expander-' + user.login"
          @click="toggleExpanded"
        >
          <i :class="expanderIconClass"></i>
        </button>
      </td>
      <td>
        <span class="userlogin">{{ user.login }}</span>
        <span class="username">{{ user.firstName }} {{ user.lastName }}</span>
        <span v-if="user.email" class="useremail">&lt;{{ user.email }}&gt;</span>
        <span v-if="appAdmin" class="useredit">
          <a
            class="textbtn textbtn-info textbtn-on-hover"
            :href="editHref"
            :data-testid="'user-edit-' + user.login"
          >
            <i class="glyphicon glyphicon-edit"></i>
            {{ $t("user.list.edit") }}
          </a>
        </span>
      </td>
    </tr>
    <tr v-if="expanded" :class="rowClass">
      <td></td>
      <td>
        <user-detail-panel
          :user="user"
          :show-groups="isSelfProfile"
          groups=""
        />
      </td>
    </tr>
  </tbody>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { getRundeckContext } from "../../../library";
import UserDetailPanel from "./UserDetailPanel.vue";
import type { UserListEntry } from "./types/userListTypes";

export default defineComponent({
  name: "UserListRow",
  components: { UserDetailPanel },
  props: {
    user: {
      type: Object as PropType<UserListEntry>,
      required: true,
    },
    index: {
      type: Number,
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
  data() {
    return {
      expanded: false,
    };
  },
  computed: {
    rowClass(): string {
      return this.index % 2 === 1 ? "alternateRow" : "";
    },
    expanderIconClass(): string {
      return this.expanded
        ? "glyphicon glyphicon-triangle-bottom"
        : "glyphicon glyphicon-triangle-right";
    },
    isSelfProfile(): boolean {
      return (
        !!this.currentUser &&
        this.currentUser.toLowerCase() === this.user.login.toLowerCase()
      );
    },
    editHref(): string {
      const rdBase = getRundeckContext().rdBase;
      return `${rdBase}/user/edit?login=${encodeURIComponent(this.user.login)}`;
    },
  },
  methods: {
    toggleExpanded() {
      this.expanded = !this.expanded;
    },
  },
});
</script>

<style scoped lang="scss">
.expander-cell {
  width: var(--sizes-4);
}

.expander-toggle {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
}

.username {
  margin-left: var(--sizes-2);
}

.useremail {
  margin-left: var(--sizes-2);
}

.useredit {
  margin-left: var(--sizes-4);
}
</style>
