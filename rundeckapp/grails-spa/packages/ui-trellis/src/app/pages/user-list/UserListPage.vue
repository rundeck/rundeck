<template>
  <div class="row user-list-page">
    <div class="col-sm-10 col-sm-offset-1">
      <h3>
        {{ $t("user.list.title") }}
        <a v-if="appAdmin" class="btn btn-default btn-xs" :href="createHref">
          <i class="glyphicon glyphicon-plus"></i>
          {{ $t("user.list.newProfile") }}
        </a>
      </h3>
      <user-table
        :users="users"
        :app-admin="appAdmin"
        :current-user="currentUser"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { getRundeckContext } from "../../../library";
import UserTable from "../../components/user-list/UserTable.vue";
import type { UserListEntry } from "../../components/user-list/types/userListTypes";

export default defineComponent({
  name: "UserListPage",
  components: { UserTable },
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
  computed: {
    createHref(): string {
      const rdBase = getRundeckContext().rdBase;
      return `${rdBase}/user/create`;
    },
  },
});
</script>

<style scoped lang="scss">
.btn-xs {
  margin-left: var(--sizes-2);
}
</style>
