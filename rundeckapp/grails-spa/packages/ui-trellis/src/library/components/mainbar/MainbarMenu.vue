<script lang="ts">
import { defineComponent } from "vue";
import { Link } from "../types/menuTypes";

export default defineComponent({
  name: "MainbarMenu",
  props: {
    links: {
      type: Array as () => Link[],
      required: true,
    },
    iconCss: {
      type: String,
      default: "fas fa-cog fa-lg",
    },
    title: {
      type: String,
      default: "",
    },
    header: {
      type: String,
      default: "",
    },
    subHeader: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      submenuOpen: null,
    };
  },
  computed: {
    enabledLinks(): Link[] {
      const filtered = this.links.filter(
        (link: Link) =>
          link &&
          link.enabled !== false &&
          (link.url ||
            (link.links && link.links.length > 0) ||
            link.separator ||
            link.group),
      );
      return filtered.sort((a, b) => {
        const ao = a.order ?? 0;
        const bo = b.order ?? 0;
        return ao - bo;
      });
    },
  },
  methods: {
    submenuClick(id: any, event: Event) {
      event.stopPropagation();
      event.preventDefault();
      this.submenuOpen = this.submenuOpen === id ? null : id;
    },
  },
});
</script>

<template>
  <dropdown tag="div" placement="bottom-right" menu-right>
    <btn
      class="dropdown-toggle btn-menu-item"
      size="med"
      type="link"
      data-testid="mainbar-menu-toggle"
    >
      <i v-if="iconCss" :class="iconCss" data-testid="mainbar-menu-icon"></i>
      {{ title }}
    </btn>
    <template #dropdown>
      <ul
        class="dropdown-menu dropdown-menu-right scroll-area"
        style="max-height: 95vh"
      >
        <li
          v-if="header"
          class="dropdown-header"
          data-testid="mainbar-menu-header"
        >
          {{ header }}
        </li>
        <li v-if="subHeader">
          <div style="padding: 10px 15px" data-testid="mainbar-menu-subheader">
            {{ subHeader }}
          </div>
        </li>
        <template v-for="(link, x) in enabledLinks" :key="x">
          <li
            v-if="link.separator || link.group"
            :id="link.group?.id || null"
            role="separator"
            class="divider"
            :data-testid="`mainbar-menu-separator-${x}`"
          ></li>
          <li v-if="link.url && (!link.links || link.links.length === 0)">
            <a :href="link.url" :data-testid="`mainbar-menu-link-${x}`">
              <i v-if="link.iconCss" :class="link.iconCss"></i>
              {{ link.title }}
            </a>
          </li>

          <li
            v-else-if="link.links && link.links.length > 0"
            class="dropdown-submenu"
            :data-testid="`mainbar-submenu-${x}`"
          >
            <a
              href="#"
              :data-testid="`mainbar-submenu-toggle-${x}`"
              @click="submenuClick(x, $event)"
              >{{ link.title }} <span class="caret"></span
            ></a>

            <ul
              class="dropdown-menu dropdown-menu-right"
              :style="{ display: submenuOpen === x ? 'block' : 'none' }"
              :data-testid="`mainbar-submenu-list-${x}`"
            >
              <template v-for="(link2, y) in link.links" :key="link2.url">
                <li v-if="link2.enabled !== false">
                  <a
                    :href="link2.url"
                    :data-testid="`mainbar-submenu-link-${x}-${y}`"
                  >
                    <i v-if="link2.iconCss" :class="link2.iconCss"></i>
                    {{ link2.title }}
                  </a>
                </li>
              </template>
            </ul>
          </li>
        </template>
      </ul>
    </template>
  </dropdown>
</template>

<style scoped lang="scss">
.dropdown-submenu {
  position: relative;
}

.dropdown-submenu .dropdown-menu {
  top: 0;
  right: 100% !important;
  margin-top: -1px;
  display: none;
}
.btn.btn-menu-item {
  padding: 0;
  border: 0;
}
</style>
