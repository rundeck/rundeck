import { buildInstanceLink } from "../services/instanceLink";

describe("buildInstanceLink", () => {
  it("returns the link for a valid https instance with an encoded project", () => {
    expect(buildInstanceLink("https://other:4440", "my proj")).toBe(
      "https://other:4440/?project=my%20proj",
    );
  });

  it("returns the link for a plain http instance", () => {
    expect(buildInstanceLink("http://other", "p")).toBe(
      "http://other/?project=p",
    );
  });

  it("returns null for a javascript: instance", () => {
    expect(buildInstanceLink("javascript:alert(1)", "p")).toBeNull();
  });

  it("returns null for a malformed instance", () => {
    expect(buildInstanceLink("not a url", "p")).toBeNull();
  });

  it("returns null when the instance is empty", () => {
    expect(buildInstanceLink(null, "p")).toBeNull();
  });
});
