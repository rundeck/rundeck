import "jest";

import { RundeckVersion } from "../../src/library/utilities/RundeckVersion";

describe("RundeckVersion", () => {
  const mkvid = function (vers: string) {
    return new RundeckVersion({ versionString: vers });
  };
  test("basic", () => {
    const data = mkvid("0").data();
    expect(data.major).toEqual(0);
    expect(data.minor).toEqual(0);
    expect(data.point).toEqual(0);
    expect(data.release).toEqual(1);
    expect(data.tag).toEqual("");
  });

  test("fullTest", () => {
    const data = mkvid("2.3.4-5-SNAPSHOT").data();
    expect(data.major).toEqual(2);
    expect(data.minor).toEqual(3);
    expect(data.point).toEqual(4);
    expect(data.release).toEqual(5);
    expect(data.tag).toEqual("SNAPSHOT");
    expect(data.version).toEqual("2.3.4-5-SNAPSHOT");
  });
  test("noReleaseTest", () => {
    const data = mkvid("2.3.4-SNAPSHOT").data();
    expect(data.major).toEqual(2);
    expect(data.minor).toEqual(3);
    expect(data.point).toEqual(4);
    expect(data.release).toEqual(1);
    expect(data.tag).toEqual("SNAPSHOT");
  });
  test("noTagTest", () => {
    const data = mkvid("2.3.4-5").data();
    expect(data.major).toEqual(2);
    expect(data.minor).toEqual(3);
    expect(data.point).toEqual(4);
    expect(data.release).toEqual(5);
    expect(data.tag).toEqual("");
  });
  test("multiTest", () => {
    const data = mkvid("2.3.4-SNAPSHOT (other-data)").data();
    expect(data.major).toEqual(2);
    expect(data.minor).toEqual(3);
    expect(data.point).toEqual(4);
    expect(data.release).toEqual(1);
    expect(data.tag).toEqual("SNAPSHOT");
    expect(data.version).toEqual("2.3.4-SNAPSHOT");
  });

  test("5.x uses mountain codenames (majorMinor + tilt modulo names5)", () => {
    const v = mkvid("5.0.0");
    expect(v.versionName()).toEqual("Denali");
  });

  test("6.x uses constellation codenames (majorMinor + tilt modulo names6)", () => {
    expect(mkvid("6.0.0").versionName()).toEqual("Auriga");
    expect(mkvid("6.1.0").versionName()).toEqual("Carina");
  });
});
