import { RundeckVersion } from "./RundeckVersion";

describe("RundeckVersion", () => {
  it("parses semantic version fields without changing identity output", () => {
    const version = new RundeckVersion({ versionString: "6.2.1-3" });

    expect(version.data()).toMatchObject({
      major: 6,
      minor: 2,
      point: 1,
      release: 3,
      semantic: "6.2.1",
      tag: "",
    });
    expect(version.versionSemantic()).toBe("6.2.1");
    expect(version.versionName()).toEqual(expect.any(String));
    expect(version.versionIcon()).toEqual(expect.any(String));
  });

  it("keeps typed UUID segments and six-character groups", () => {
    const version = new RundeckVersion({});
    const data = version.splitUUID("abcdef-123456-7890ab");

    expect(data.uuid0).toBe(0xab);
    expect(data.sixes).toEqual(["abcdef", "123456"]);
  });
});
