import messages from "../i18n";

describe("Readme/MOTD public translation overlay", () => {
  it("uses a key supplied by the public catalog", () => {
    expect(messages.en_US["translation.poc.core.precedence"]).toBe("public");
  });

  it("retains a nested key supplied only by the original catalog", () => {
    expect(messages.en_US.edit).toMatchObject({
      motd: { label: "Edit Message of the Day" },
    });
  });
});
