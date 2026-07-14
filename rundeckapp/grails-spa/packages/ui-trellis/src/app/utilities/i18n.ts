import { nextTick } from "vue";
import {
  UiMessage,
  type LocaleMessageValue,
  type LocalizedMessages,
} from "../../library/stores/UIStore";
import en_US from "./locales/en_US";
import de_DE from "./locales/de_DE";
import es_419 from "./locales/es_419";
import fr_FR from "./locales/fr_FR";
import ja_JP from "./locales/ja_JP";
import nl_NL from "./locales/nl_NL";
import pt_BR from "./locales/pt_BR";
import zh_CN from "./locales/zh_CN";
import { createI18n, type I18n } from "vue-i18n";

export type { LocaleMessageValue, LocalizedMessages };

const internationalization: Record<string, any> = {
  en_US: en_US,
  de_DE: de_DE,
  es_419: es_419,
  fr_FR: fr_FR,
  ja_JP: ja_JP,
  nl_NL: nl_NL,
  pt_BR: pt_BR,
  zh_CN: zh_CN,
};

const initI18n = (options = {}) => {
  const locale = window._rundeck?.locale || "en_US";
  const lang = window._rundeck?.language || "en";
  const messages = {
    [locale]: Object.assign(
      {},
      internationalization[locale] ||
        internationalization[lang] ||
        internationalization["en_US"] ||
        {},
    ),
    ["en_US"]: internationalization["en_US"],
  };

  // Create VueI18n instance with options
  return createI18n({
    silentTranslationWarn: true,
    fallbackLocale: "en_US",
    locale: locale, // set locale
    messages, // set locale messages
    ...options,
  });
};

const updateLocaleMessages = async (
  i18n: I18n,
  locale: string,
  _lang: string,
  messages: Record<string, any>,
) => {
  i18n.global.mergeLocaleMessage(locale, messages);
  return nextTick();
};

const isLocalizedMessages = (
  m: UiMessage[] | LocalizedMessages,
): m is LocalizedMessages => !Array.isArray(m);

/**
 * update locale messages for the i18n instance
 * @param i18n vue-i18n instance
 * @param messages new messages data — pass a LocalizedMessages object to register
 *   the active locale and, when an `en_US` bundle is present and the active locale
 *   is not already `en_US`, also register `en_US` so vue-i18n's built-in fallback
 *   chain resolves to English instead of raw keys; pass UiMessage[] for legacy behavior
 */
const commonAddUiMessages = async (
  i18n: I18n,
  messages: UiMessage[] | LocalizedMessages,
) => {
  const locale = window._rundeck.locale || "en_US";
  const lang = window._rundeck.language || "en";

  if (isLocalizedMessages(messages)) {
    const localeMessages =
      messages[locale] || messages[lang] || messages["en_US"] || {};
    await updateLocaleMessages(i18n, locale, lang, localeMessages);
    if (locale !== "en_US" && messages["en_US"]) {
      await updateLocaleMessages(i18n, "en_US", "en", messages["en_US"]);
    }
  } else {
    const flat = messages.reduce(
      (acc: any, m: UiMessage) => (m ? { ...acc, ...m } : acc),
      {},
    );
    await updateLocaleMessages(i18n, locale, lang, flat);
  }
};

export { initI18n, updateLocaleMessages, commonAddUiMessages };
