import { glyphiconBadges } from "../nodeUi";

describe("glyphiconBadges", () => {
  it("parses well-formed input string correctly", () => {
    const input = "glyphicon-user1, fa-badge2, fab-badge1, glyphicon-lock-1";
    expect(glyphiconBadges({ "ui:badges": input })).toEqual([
      "glyphicon-user1",
      "fa-badge2",
      "fab-badge1",
      "glyphicon-lock-1",
    ]);
  });
  it("parses input string with badges that do not end with a number", () => {
    const input = "fa-house-user, glyphicon-download-alt";
    expect(glyphiconBadges({ "ui:badges": input })).toEqual([
      "fa-house-user",
      "glyphicon-download-alt",
    ]);
  });

  it("filters out invalid badge names", () => {
    const input = "glyphicon-user, invalid-badge, fa2-car, fab_badge";
    expect(glyphiconBadges({ "ui:badges": input })).toEqual([
      "glyphicon-user", // Assuming 'fa2-car' and 'fab_badge' are invalid
    ]);
  });

  it("returns an empty array for an empty string", () => {
    const input = "";
    expect(glyphiconBadges({ "ui:badges": input })).toEqual([]);
  });

  it("returns an empty array when no valid badges are provided", () => {
    const input = "invalid-badge, another-invalid";
    expect(glyphiconBadges({ "ui:badges": input })).toEqual([]);
  });

  it("handles null or undefined input gracefully", () => {
    expect(glyphiconBadges({ "ui:badges": null })).toEqual([]);
    expect(glyphiconBadges({ "ui:badges": undefined })).toEqual([]);
  });
});
