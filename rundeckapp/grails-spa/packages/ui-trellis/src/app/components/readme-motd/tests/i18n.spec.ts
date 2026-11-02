import messages from "../i18n";

describe("Readme/MOTD public translation overlay", () => {
  it("uses the canonical value when the same flat key differs", () => {
    expect(messages.en_US["translation.poc.core.precedence"]).toBe("public");
    expect(messages.en_US["translation.poc.core.flat.dotted"]).toBe(
      "canonical-flat-value",
    );
  });

  it("resolves canonical-only and legacy-only keys", () => {
    expect(messages.en_US["translation.poc.core.canonical-only"]).toBe(
      "canonical-only-value",
    );
    expect(messages.en_US["translation.poc.core.legacy-only"]).toBe(
      "legacy-only-value",
    );
  });

  it("deep merges nested conflicts and legacy fallback keys", () => {
    expect(messages.en_US.edit).toMatchObject({
      readme: { label: "Canonical Edit Readme" },
    });
    expect(messages.en_US.edit).toMatchObject({
      motd: { label: "Edit Message of the Day" },
    });
  });

  it("overlays a locale while retaining locales absent from canonical", () => {
    expect(messages.de_DE.edit).toMatchObject({
      readme: { label: "Kanonische Readme bearbeiten" },
      motd: { label: "Meldung des Tages bearbeiten" },
    });
    expect(messages.nl_NL.edit).toMatchObject({
      readme: { label: "Readme bewerken" },
    });
  });
});
