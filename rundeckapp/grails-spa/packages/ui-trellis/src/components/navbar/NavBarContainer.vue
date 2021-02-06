<template>
    
        <li :class="{active: item.active}" sytle="display: flex;">
            <VPopover
                baseClass
                offset="10"
                popoverBaseClass="navbar-popover"
                popoverWrapperClass="navbar-popover-wrapper"
                popoverInnerClass="navbar-popover-inner"
                placement="right" :boundariesElement="body()">
            <a :href="item.link">
                <i style="" :class="item.class"/>
                <div>{{label}}</div>
            </a>
            <template slot="popover">
                <div class="navbar-container-popover">
                    Foo
                </div>
            </template>
            </VPopover>
        </li>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'

import {VPopover} from 'v-tooltip'

import {NavItem} from '../../stores/NavBar'

export default Vue.extend({
    components: {
        VPopover
    },
    props: {
        item: Object as PropType<NavItem>
    },
    methods: {
        body() { 
            return document.body
        }
    },
    computed: {
        label(): string {
            return this.item!.label!.toUpperCase()
        }
    }
})
</script>

<style lang="scss" scoped>

.active * {
    color: white;
}

li {
    position: relative;
    list-style-type: none;
    display: flex;
    flex-direction: column;
    align-content: center;
    align-items: center;
    margin-top: 20px;
    margin-bottom: 20px;
    font-size: 8px;
    text-align: center;

    * {
        color: #BCBCBC
    }

    :hover * {
        color: white;
    }

    @media(max-width: 768px) {
        margin: 0px 5px 0px;
        width: 65px;
    }

}

i {
    height: 24px !important;
    width: 24px !important;
    font-size: 24px;
    margin-bottom: 5px;
    background-size: 24px !important;
}

.navbar-container-popover {
    border-radius: 5px 10px 10px 5px;
    border-style: solid;
    border-color: #414141;
    border-width: 1px;
    background-color: #303030;
    width: 200px;
    max-height: 400px;
    padding: 10px;
}
</style>

<style lang="scss">
.navbar-popover, .navbar-popover * {
    outline: none !important;
    :focus {
        outline: none !important;
    }
}
</style>