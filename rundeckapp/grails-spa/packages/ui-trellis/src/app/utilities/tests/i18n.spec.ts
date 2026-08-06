jest.mock("vue-i18n", () => ({ createI18n: jest.fn() }));

import { commonAddUiMessages, updateLocaleMessages } from "../i18n";
import type { UiMessage } from "../../../library/stores/UIStore";

const createMockI18n = () => {
  const store: Record<string, Record<string, any>> = {};
  return {
    global: {
      mergeLocaleMessage: jest.fn(
        (locale: string, messages: Record<string, any>) => {
          store[locale] = { ...(store[locale] || {}), ...messages };
        },
      ),
      getLocaleMessage: jest.fn((locale: string) => store[locale] || {}),
      setLocaleMessage: jest.fn(
        (locale: string, messages: Record<string, any>) => {
          store[locale] = messages;
        },
      ),
    },
  };
};

const setWindowLocale = (locale: string, language: string) => {
  (window as any)._rundeck = { locale, language };
};

describe("updateLocaleMessages", () => {
  it("calls mergeLocaleMessage on the i18n instance", async () => {
    const i18n = createMockI18n();
    const messages = { "key.one": "One" };

    await updateLocaleMessages(i18n as any, "en_US", "en", messages);

    expect(i18n.global.mergeLocaleMessage).toHaveBeenCalledWith(
      "en_US",
      messages,
    );
  });
});

describe("commonAddUiMessages", () => {
  beforeEach(() => {
    setWindowLocale("ja_JP", "ja");
  });

  it("registers en_US when passed a LocalizedMessages object and locale is non-English", async () => {
    const i18n = createMockI18n();
    const messages = {
      ja_JP: { "button.save": "保存" },
      en_US: { "button.save": "Save" },
    };

    await commonAddUiMessages(i18n as any, messages as any);

    expect(i18n.global.getLocaleMessage("en_US")["button.save"]).toBe("Save");
  });

  it("registers the active locale when passed a LocalizedMessages object", async () => {
    const i18n = createMockI18n();
    const messages = {
      ja_JP: { "button.save": "保存" },
      en_US: { "button.save": "Save" },
    };

    await commonAddUiMessages(i18n as any, messages as any);

    expect(i18n.global.getLocaleMessage("ja_JP")["button.save"]).toBe("保存");
  });

  it("does not register en_US twice when locale is already en_US", async () => {
    setWindowLocale("en_US", "en");
    const i18n = createMockI18n();
    const messages = {
      en_US: { "button.save": "Save" },
    };

    await commonAddUiMessages(i18n as any, messages as any);

    const en_US_calls = i18n.global.mergeLocaleMessage.mock.calls.filter(
      ([locale]) => locale === "en_US",
    );
    expect(en_US_calls.length).toBe(1);
  });

  it("does not blow up when LocalizedMessages has no en_US key", async () => {
    const i18n = createMockI18n();
    const messages = {
      ja_JP: { "button.save": "保存" },
    };

    await expect(
      commonAddUiMessages(i18n as any, messages as any),
    ).resolves.not.toThrow();

    expect(i18n.global.getLocaleMessage("ja_JP")["button.save"]).toBe("保存");
    expect(Object.keys(i18n.global.getLocaleMessage("en_US")).length).toBe(0);
  });

  it("handles legacy UiMessage[] flat array — only registers active locale", async () => {
    const i18n = createMockI18n();
    const messages: UiMessage[] = [
      { "button.save": "Save" },
      { "button.cancel": "Cancel" },
    ];

    await commonAddUiMessages(i18n as any, messages);

    expect(i18n.global.getLocaleMessage("ja_JP")["button.save"]).toBe("Save");
    expect(i18n.global.getLocaleMessage("ja_JP")["button.cancel"]).toBe(
      "Cancel",
    );
    expect(Object.keys(i18n.global.getLocaleMessage("en_US")).length).toBe(0);
  });
});
