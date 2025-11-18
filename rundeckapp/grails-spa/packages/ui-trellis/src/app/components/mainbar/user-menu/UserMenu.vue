<script lang="ts">
import { Dropdown } from "uiv";
import { defineComponent } from "vue";

export default defineComponent({
  name: "UserMenu",
  components: { Dropdown },
  props: {
    username: {
      type: String,
      required: true,
    },
    profileLink: {
      type: String,
      required: true,
    },
    logoutLink: {
      type: String,
      required: true,
    },
  },
});
</script>

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

<style scoped lang="scss">
.btn.btn-menu-item {
  padding: 0;
  border: 0;
}
</style>
