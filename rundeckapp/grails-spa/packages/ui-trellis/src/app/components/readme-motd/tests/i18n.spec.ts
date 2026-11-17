import messages from "../i18n";
import { mergeTranslationCatalogs } from "../../../../../../../../../tools/i18n/mergeCatalogs";

describe("Readme/MOTD public translation overlay", () => {
  it("prefers canonical values at a genuine production key", () => {
    const merged = mergeTranslationCatalogs(
      { en_US: { edit: { readme: { label: "legacy-test-marker" } } } },
      { en_US: { edit: { readme: { label: "canonical-test-marker" } } } },
    );

    expect(merged.en_US.edit).toMatchObject({
      readme: { label: "canonical-test-marker" },
    });
  });

  it("uses genuine canonical and retained legacy production values", () => {
    expect(messages.en_US.edit).toMatchObject({
      readme: { label: "Edit Readme" },
    });
    expect(messages.en_US.edit).toMatchObject({
      motd: { label: "Edit Message of the Day" },
    });
  });

  it("overlays a locale while retaining locales absent from canonical", () => {
    expect(messages.de_DE.edit).toMatchObject({
      readme: { label: "Readme bearbeiten" },
      motd: { label: "Meldung des Tages bearbeiten" },
    });
    expect(messages.nl_NL.edit).toMatchObject({
      readme: { label: "Readme bewerken" },
    });
  });
});
