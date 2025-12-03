import { describe, it, expect, beforeEach, jest } from "@jest/globals";

const mockApiGet = jest.fn();
const mockApiPost = jest.fn();

jest.mock("../../services/api", () => {
  return {
    api: {
      get: jest.fn(),
      post: jest.fn(),
    },
  };
});

import {
  getPluginProvidersForService,
  getServiceProviderDescription,
  validatePluginConfig,
} from "../pluginService";
import { api } from "../../services/api";

// Mock window._rundeck
const mockWindow = {
  _rundeck: {
    rdBase: "http://localhost:4440/",
    apiVersion: "44",
    projectName: "testProject",
  },
};

beforeEach(() => {
  jest.clearAllMocks();
  (global as any).window = mockWindow;
  (api.get as any).mockImplementation(mockApiGet);
  (api.post as any).mockImplementation(mockApiPost);
});

describe("pluginService", () => {
  describe("getPluginProvidersForService", () => {
    it("should fetch plugin providers for a service", async () => {
      const svcName = "WebhookEvent1";
      const mockProviders = {
        providers: ["provider1", "provider2"],
      };

      (api.get as any).mockResolvedValueOnce({
        data: mockProviders,
      } as any);

      const result = await getPluginProvidersForService(svcName);

      expect(api.get).toHaveBeenCalledWith(`plugin/providers/${svcName}`);
      expect(result).toEqual(mockProviders);
    });

    it("should return cached providers on subsequent calls", async () => {
      const svcName = "WebhookEvent2";
      const mockProviders = {
        providers: ["provider1", "provider2"],
      };

      (api.get as any).mockResolvedValueOnce({
        data: mockProviders,
      } as any);

      await getPluginProvidersForService(svcName);
      const result = await getPluginProvidersForService(svcName);

      expect(api.get).toHaveBeenCalledTimes(1); // Should use cache
      expect(result).toEqual(mockProviders);
    });

    it("should handle errors gracefully", async () => {
      const svcName = "InvalidService1";
      const consoleSpy = jest.spyOn(console, "warn").mockImplementation(() => {});

      (api.get as any).mockRejectedValueOnce(new Error("API Error"));

      const result = await getPluginProvidersForService(svcName);

      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });

  describe("getServiceProviderDescription", () => {
    it("should fetch service provider description", async () => {
      const svcName = "WebhookEvent3";
      const provider = "test-provider";
      const mockDescription = {
        name: "Test Provider",
        description: "Test description",
      };

      (api.get as any).mockResolvedValueOnce({
        data: mockDescription,
      } as any);

      const result = await getServiceProviderDescription(svcName, provider);

      expect(api.get).toHaveBeenCalledWith(
        `plugin/detail/${svcName}/${provider}`,
        { params: { project: "testProject" } },
      );
      expect(result).toEqual(mockDescription);
    });

    it("should include project parameter when available", async () => {
      const svcName = "WebhookEvent4";
      const provider = "test-provider";
      const mockDescription = {
        name: "Test Provider",
        description: "Test description",
      };

      (api.get as any).mockResolvedValueOnce({
        data: mockDescription,
      } as any);

      await getServiceProviderDescription(svcName, provider);

      expect(api.get).toHaveBeenCalledWith(
        `plugin/detail/${svcName}/${provider}`,
        { params: { project: "testProject" } },
      );
    });

    it("should cache provider descriptions", async () => {
      const svcName = "WebhookEvent5";
      const provider = "test-provider";
      const mockDescription = {
        name: "Test Provider",
        description: "Test description",
      };

      (api.get as any).mockResolvedValueOnce({
        data: mockDescription,
      } as any);

      await getServiceProviderDescription(svcName, provider);
      const result = await getServiceProviderDescription(svcName, provider);

      expect(api.get).toHaveBeenCalledTimes(1); // Should use cache
      expect(result).toEqual(mockDescription);
    });
  });

  describe("validatePluginConfig", () => {
    it("should validate plugin configuration", async () => {
      const svcName = "WebhookEvent6";
      const provider = "test-provider";
      const config = { key: "value" };
      const mockValidation = {
        valid: true,
        errors: [],
      };

      (api.post as any).mockResolvedValueOnce({
        data: mockValidation,
      } as any);

      const result = await validatePluginConfig(svcName, provider, config);

      expect(api.post).toHaveBeenCalledWith(
        `plugin/validate/${svcName}/${provider}`,
        { config },
        { params: {} },
      );
      expect(result).toEqual(mockValidation);
    });

    it("should include ignoredScope parameter when provided", async () => {
      const svcName = "WebhookEvent7";
      const provider = "test-provider";
      const config = { key: "value" };
      const ignoredScope = "project";
      const mockValidation = {
        valid: true,
        errors: [],
      };

      (api.post as any).mockResolvedValueOnce({
        data: mockValidation,
      } as any);

      await validatePluginConfig(svcName, provider, config, ignoredScope);

      expect(api.post).toHaveBeenCalledWith(
        `plugin/validate/${svcName}/${provider}`,
        { config },
        { params: { ignoredScope: "project" } },
      );
    });

    it("should throw error when validation fails", async () => {
      const svcName = "WebhookEvent8";
      const provider = "test-provider";
      const config = { key: "value" };

      (api.post as any).mockResolvedValueOnce({
        data: null,
      } as any);

      await expect(
        validatePluginConfig(svcName, provider, config),
      ).rejects.toThrow("Error validating plugin config for WebhookEvent");
    });
  });
});

