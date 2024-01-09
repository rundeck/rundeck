<template>
    <nav>
        <ul class="nav-bar__list" ref="list" v-if="navBar">
            <li>
                <ul class="nav-bar__list-group" ref="group-main">
                    <template v-for="item in getItems('root', 'main')">
                        <NavBarItem v-if="item.type === 'link'" :item="item" :key="item.id" itemStyle="icon" />
                        <NavBarContainer v-if="item.type === 'container'" :item="(item as NavContainer)" :key="item.id" />
                    </template>
                </ul>
            </li>
            <li style="margin-top: auto;width: 100%">
                <ul class="nav-bar__list-group nav-bar__list-group--bottom" ref="group-bottom">
                    <NavBarContainer v-if="navBar.isOverflowing" :item="navBar.overflowItem" />
                    <template v-for="item in getItems('root', 'bottom')">
                        <NavBarItem v-if="item.type === 'link'" :item="item" :key="item.id" itemStyle="icon" />
                        <NavBarContainer v-if="item.type === 'container'" :item="(item as NavContainer)" :key="item.id" />
                    </template>
                </ul>
            </li>
        </ul>
    </nav>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue'

import {NavBar, NavContainer, NavItem} from '../../stores/NavBar'

import NavBarItem from './NavBarItem.vue'
import NavBarContainer from './NavBarContainer.vue'

export default defineComponent({
  name: 'NavBar',
  components: {
    NavBarItem,
    NavBarContainer
  },
  mounted() {
    window.addEventListener('resize', this.overflow)
    /** After layout and before render handle overflow */
    window.requestAnimationFrame(this.overflow)
  },
  data() {
    return {
      navBar: window._rundeck.rootStore.navBar

    }
  },
  methods: {
    getItems(ctr: string, grp: string): NavItem[] {
      return this.navBar.containerGroupItems(ctr, grp)
    },
    /**
     * Moves menu items into the "more" container until the navbar is no
     * longer overflowing.
     */
    overflow() {
      const el = this.$el as HTMLElement
      const mainGroup = this.$refs['group-main'] as HTMLElement

      /**
       * Check for overflow. At different zoom levels this could be off by one in the positive
       * even though no overflow is occuring. Testing indicates checking for negative difference
       * works at all zoom levels.
       */
      if (el.offsetHeight - el.scrollHeight < 0) {
        this.navBar.overflowOne()
        /**
         * Continue to force layout until no longer overflowing.
         * This provides for the least amount of flicker on page load.
         **/
        this.$forceUpdate()
        window.requestAnimationFrame(this.overflow)
      } else if (mainGroup.offsetTop + mainGroup.offsetHeight + 240 < el.offsetHeight) {
        this.navBar.showOne()
        window.requestAnimationFrame(this.overflow)
      }
    }
  }
})
</script>

<style lang="scss" scoped>
nav {
    overflow-y: hidden;
    overflow-x: hidden;
    position: relative;
    height: 100%;
    background-color: var(--sidebar-background-color);
    padding-top: 20px;
    padding-bottom: 20px;

    scrollbar-color: transparent transparent;
    scrollbar-width: thin;

    &:hover {
        scrollbar-color: transparent slategray;
    }

    &::-webkit-scrollbar {
        background-color: transparent;
        width: 5px;
        height: 5px;
    }

    &:hover::-webkit-scrollbar {
        background-color: transparent;
        width: 5px;
        height: 5px;
    }

    &:hover::-webkit-scrollbar-thumb {
        background-color: slategray;
        width: 5px;
        height: 5px;
    }
}

.nav-bar__list {
    list-style: none;
    height: 100%;
    display: flex;
    flex-direction: column;
    padding-left: 5px;
    padding-right: 5px;
    padding-bottom: 10px;
    margin: 0;
}

.nav-bar__list-group {
    height: 100%;
    display: flex;
    flex-direction: column;
    margin: 0;
    padding: 0;
}

.nav-bar__list-group--bottom {
    width: 100%;
    border-top-style: solid;
    border-top-width: 1px;
    border-top-color: #414141;
}

</style>