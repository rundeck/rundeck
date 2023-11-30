<template>
    <div class="form-group" v-if="!loading">
        <div class="checkbox-inline" :title="'Toggle a new UI for this page'">
            <input type="checkbox" v-model="nextUi" id="nextUi" name="nextUi" />
            <label for="nextUi">{{ $t("widget.nextUi.title") }}</label>
        </div>
    </div>
    <div v-else>
      <b class="fa fa-spinner fa-spin"></b> {{ $t("loading.text") }}
    </div>
</template>

<script lang="ts">
import { defineComponent, inject } from "vue";
// import VueCookies from "vue-cookies";
const COOKIE_NAME="nextUi"
export default defineComponent({
    name: "NextUiToggle",
    data() {
        // const $cookies = inject<VueCookies>("$cookies");
        return {
            nextUi: this.$cookies.get(COOKIE_NAME),
          loading:false
        };
    },
    watch: {
        nextUi(newVal: any) {
            console.log(`toggle: ${newVal})`);
            if (newVal) {
                this.$cookies.set(COOKIE_NAME, "true", "1y", "/", "", false, "Strict")
            } else {
              this.$cookies.set(COOKIE_NAME, "false", "1y", "/", "", false, "Strict")
            }
            this.loading=true
            window.location.reload()
        },
    },
});
</script>

<style scoped lang="scss"></style>