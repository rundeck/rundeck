import { glyphiconBadges } from "./nodeUi";

describe("glyphiconBadges", () => {
  test("parses well-formed input string correctly", () => {
    const input = "glyphicon-user1, fa-badge2, fab-badge1, glyphicon-lock-1";
    expect(glyphiconBadges({ "ui:badges": input })).toEqual([
      "glyphicon-user1",
      "fa-badge2",
      "fab-badge1",
      "glyphicon-lock-1",
    ]);
  });

  test("filters out invalid badge names", () => {
    const input = "glyphicon-user, invalid-badge, fa2-car, fab_badge";
    expect(glyphiconBadges({ "ui:badges": input })).toEqual([
      "glyphicon-user", // Assuming 'fa2-car' and 'fab_badge' are invalid
    ]);
  });

  test("returns an empty array for an empty string", () => {
    const input = "";
    expect(glyphiconBadges({ "ui:badges": input })).toEqual([]);
  });

  test("returns an empty array when no valid badges are provided", () => {
    const input = "invalid-badge, another-invalid";
    expect(glyphiconBadges({ "ui:badges": input })).toEqual([]);
  });

  test("handles null or undefined input gracefully", () => {
    expect(glyphiconBadges({ "ui:badges": null })).toEqual([]);
    expect(glyphiconBadges({ "ui:badges": undefined })).toEqual([]);
  });
});
