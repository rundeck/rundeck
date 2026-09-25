<template>
  <div class="row">
    <div class="col-sm-12">
      <table class="table table-condensed table-striped">
        <tr>
          <th class="table-header">{{ $t("domain.User.email.label") }}</th>
          <th class="table-header">{{ $t("domain.User.firstName.label") }}</th>
          <th class="table-header">{{ $t("domain.User.lastName.label") }}</th>
          <th v-if="showGroups" class="table-header">
            {{ $t("security.groups.label") }}
            <tooltip
              :text="$t('security.groups.description')"
              placement="bottom"
            >
              <i
                class="glyphicon glyphicon-question-sign"
                data-testid="groups-help-icon"
              ></i>
            </tooltip>
          </th>
        </tr>
        <tr>
          <td>
            {{ user.email }}
            <span v-if="!user.email" class="text-strong small text-uppercase">
              {{ $t("not.set") }}
            </span>
          </td>
          <td>
            {{ user.firstName }}
            <span
              v-if="!user.firstName"
              class="text-strong small text-uppercase"
            >
              {{ $t("not.set") }}
            </span>
          </td>
          <td>
            {{ user.lastName }}
            <span
              v-if="!user.lastName"
              class="text-strong small text-uppercase"
            >
              {{ $t("not.set") }}
            </span>
          </td>
          <td v-if="showGroups">{{ groups }}</td>
        </tr>
      </table>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { UserListEntry } from "./types/userListTypes";

export default defineComponent({
  name: "UserDetailPanel",
  props: {
    user: {
      type: Object as PropType<UserListEntry>,
      required: true,
    },
    showGroups: {
      type: Boolean,
      default: false,
    },
    groups: {
      type: String,
      default: "",
    },
  },
});
</script>

<style scoped lang="scss">
.table-header {
  font-weight: var(--fontWeights-bold);
}
</style>
