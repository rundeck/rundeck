import { describe, it, expect, jest, afterEach } from "@jest/globals";
import { api } from "../api";
import { getFeatureEnabled } from "../feature";

jest.mock("../api", () => ({
  api: {
    get: jest.fn(),
  },
}));

const mockedApi = api as jest.Mocked<typeof api>;

describe("getFeatureEnabled", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe("happy path", () => {
    it("returns true when API response has enabled: true", async () => {
      mockedApi.get.mockResolvedValue({ data: { enabled: true } });

      const result = await getFeatureEnabled("myFeature");

      expect(result).toBe(true);
      expect(mockedApi.get).toHaveBeenCalledWith("/feature/myFeature");
    });

    it("returns false when API response has enabled: false", async () => {
      mockedApi.get.mockResolvedValue({ data: { enabled: false } });

      const result = await getFeatureEnabled("myFeature");

      expect(result).toBe(false);
    });

    it("returns false when API response has enabled: undefined", async () => {
      mockedApi.get.mockResolvedValue({ data: { enabled: undefined } });

      const result = await getFeatureEnabled("myFeature");

      expect(result).toBe(false);
    });

    it("calls API with correct feature name", async () => {
      mockedApi.get.mockResolvedValue({ data: { enabled: true } });

      await getFeatureEnabled("enterpriseFeatureX");

      expect(mockedApi.get).toHaveBeenCalledWith("/feature/enterpriseFeatureX");
    });
  });

  describe("error handling", () => {
    it("returns false when API call throws an error", async () => {
      mockedApi.get.mockRejectedValue(new Error("Network error"));

      const result = await getFeatureEnabled("myFeature");

      expect(result).toBe(false);
    });

    it("logs a warning when API call fails", async () => {
      const consoleSpy = jest
        .spyOn(console, "warn")
        .mockImplementation(() => {});
      const error = new Error("Network error");
      mockedApi.get.mockRejectedValue(error);

      await getFeatureEnabled("myFeature");

      expect(consoleSpy).toHaveBeenCalledWith(
        "Failed to fetch feature flag: myFeature",
        error,
      );
      consoleSpy.mockRestore();
    });

    it("returns false when resp is null", async () => {
      mockedApi.get.mockResolvedValue(null);

      const result = await getFeatureEnabled("myFeature");

      expect(result).toBe(false);
    });
  });
});
