<template>
  <modal
    :id="modalId"
    class="modal"
    tabindex="-1"
    role="dialog"
    :aria-labelledby="`${modalId}_title`"
    :modal-size-class="modalSize"
    :title="modalTitle"
    aria-hidden="true"
  >
    <div :id="`${modalId}_content`" class="modal-body">
      <slot>
        {{ content }}
      </slot>
    </div>

    <template v-if="buttons || links" #footer>
      <div :id="`${modalId}_footer`" class="modal-footer">
        <button
          v-if="!noCancel"
          type="submit"
          class="btn btn-default"
          data-dismiss="modal"
        >
          {{ cancelCode ? $t(cancelCode) : $t("cancel") }}
        </button>
        <span :id="`${modalId}_buttons`">
          <button
            v-for="(button, index) in buttons"
            :id="button.id || `${modalId}_btn_${index}`"
            :key="`${modalId}_button_${index}`"
            class="btn"
            data-test="extra-buttons"
            :class="[button.css || 'btn-default']"
            @click="emitButtonEvent(button)"
          >
            {{ getText(button, "button") }}
          </button>

          <template v-if="links">
            <a
              v-for="(link, index) in links"
              :key="`${modalId}_link_${index}`"
              data-test="extra-links"
              class="btn"
              :class="[link.css ? link.css : 'btn-default']"
              :href="link.href || '#'"
              @click="emitLinkEvent(link)"
            >
              {{ getText(link, "link") }}
            </a>
          </template>
        </span>
      </div>
    </template>
  </modal>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { ModalButtons, ModalLinks } from "./types/commonTypes";

export default defineComponent({
  name: "CommonModal",
  props: {
    noCancel: {
      type: Boolean,
      default: false,
    },
    modalId: {
      type: String,
      required: true,
    },
    modalSize: {
      type: String,
      default: "",
    },
    title: {
      type: String,
      default: "",
    },
    titleCode: {
      type: String,
      default: "",
    },
    cancelCode: {
      type: String,
      default: "",
    },
    links: {
      type: Array as PropType<Array<ModalLinks>>,
      default: () => [],
    },
    buttons: {
      type: Array as PropType<Array<ModalButtons>>,
      default: () => [],
    },
    content: {
      type: String,
      default: "",
    },
  },
  emits: ["buttonClicked", "linkClicked"],
  computed: {
    modalTitle() {
      return this.title || this.titleCode
        ? this.title || this.$t(this.titleCode)
        : "";
    },
  },
  methods: {
    getText(elem: any, defaultMessage: string = "") {
      return elem.message || elem.messageCode
        ? elem.message || this.$t(elem.messageCode)
        : defaultMessage;
    },
    emitButtonEvent(button: ModalButtons) {
      this.$emit("buttonClicked", button.id);
    },
    emitLinkEvent(link: ModalLinks) {
      this.$emit("linkClicked", link);
    },
  },
});
</script>
