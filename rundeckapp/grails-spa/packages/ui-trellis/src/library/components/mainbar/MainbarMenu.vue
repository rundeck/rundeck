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
    enabledLinks() {
      const filtered = this.links.filter(
        (link) =>
          link &&
          link.enabled !== false &&
          (link.url ||
            (link.links && link.links.length > 0) ||
            link.separator ||
            link.group),
      );
      return filtered.sort((a, b) => {
        if (b.order !== undefined && a.order === undefined) return 0 - b.order;
        if (a.order !== undefined && b.order === undefined) return b.order - 0;
        if (a.order !== undefined && b.order !== undefined) {
          return a.order - b.order;
        }

        return 0;
      });
    },
  },
  methods: {
    submenuClick(id: any, event: Event) {
      event.stopPropagation();
      this.submenuOpen = this.submenuOpen === id ? null : id;
    },
  },
});
</script>

<template>
  <dropdown tag="div" placement="bottom-right" menu-right>
    <btn class="dropdown-toggle btn-menu-item" size="med" type="link">
      <i v-if="iconCss" :class="iconCss"></i> {{ title }}
    </btn>
    <template #dropdown>
      <ul
        class="dropdown-menu dropdown-menu-right scroll-area"
        style="max-height: 95vh"
      >
        <li v-if="header" class="dropdown-header">{{ header }}</li>
        <li v-if="subHeader">
          <div style="padding: 10px 15px">{{ subHeader }}</div>
        </li>
        <template v-for="(link, x) in enabledLinks" :key="x">
          <li
            v-if="link.separator || link.group"
            :id="link.group?.id || null"
            role="separator"
            class="divider"
          ></li>
          <li v-if="link.url && (!link.links || link.links.length === 0)">
            <a :href="link.url">
              <i v-if="link.iconCss" :class="link.iconCss"></i>
              {{ link.title }}
            </a>
          </li>

          <li
            v-else-if="link.links && link.links.length > 0"
            class="dropdown-submenu"
          >
            <a
              id="plugins-menu-button"
              href="#"
              @click="submenuClick(x, $event)"
              >{{ link.title }} <span class="caret"></span
            ></a>

            <ul
              id="plugins-menu"
              class="dropdown-menu dropdown-menu-right"
              :style="{ display: submenuOpen === x ? 'block' : 'none' }"
            >
              <template v-for="link2 in link.links" :key="link2.url">
                <li v-if="link2.enabled !== false">
                  <a :href="link2.url">
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
