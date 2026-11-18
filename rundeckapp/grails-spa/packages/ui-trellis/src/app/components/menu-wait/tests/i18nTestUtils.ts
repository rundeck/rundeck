import { defineComponent, h } from "vue";
import messages from "../../../utilities/locales/en_US.js";

const catalogue = messages as Record<string, string>;

/**
 * Test translate function backed by the en_US catalogue with {n} list interpolation.
 * @param key message key
 * @param params positional parameters
 * @returns the interpolated message
 */
export const $t = (key: string, params: unknown[] = []): string =>
  (catalogue[key] || key).replace(/\{(\d+)\}/g, (_, i) =>
    String(params[Number(i)]),
  );

/**
 * Minimal stand-in for vue-i18n's `<i18n-t>` component: renders the en_US
 * message, replacing each NAMED placeholder `{name}` with the content of the slot of the
 * same name, as the real component does. Slots never populate positional
 * placeholders such as `{0}`. Slot content is rendered as vnodes, never parsed
 * as HTML. This is a stand-in; real rendering is covered by the manual
 * vue/server-renderer check and in-browser validation.
 */
export const I18nTStub = defineComponent({
  name: "I18nT",
  props: {
    keypath: { type: String, required: true },
    tag: { type: String, default: "span" },
  },
  setup(props, { slots }) {
    return () => {
      const parts = (catalogue[props.keypath] || props.keypath).split(
        /(\{[A-Za-z_]\w*\})/,
      );
      const children = parts.map((part) => {
        const m = part.match(/^\{([A-Za-z_]\w*)\}$/);
        const slot = m ? slots[m[1]] : undefined;
        return slot ? slot() : part;
      });
      return h(props.tag, children);
    };
  },
});

export const i18nGlobal = {
  mocks: { $t },
  stubs: { "i18n-t": I18nTStub, PtProgressBar: { template: "<div />" } },
};
