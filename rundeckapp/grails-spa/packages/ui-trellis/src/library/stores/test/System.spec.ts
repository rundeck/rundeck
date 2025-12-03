import { describe, it, expect, beforeEach, jest } from "@jest/globals";
import { SystemStore } from "../System";
import { RootStore } from "../RootStore";

const mockApiGet = jest.fn();
jest.mock("../../services/api", () => {
  return {
    api: {
      get: jest.fn(),
    },
  };
});

jest.mock("../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    rdBase: "http://localhost:4440/",
    projectName: "testProject",
    apiVersion: "44",
  })),
}));

import { api } from "../../services/api";

describe("SystemStore", () => {
  let systemStore: SystemStore;
  let mockRootStore: RootStore;

  beforeEach(() => {
    jest.clearAllMocks();
    mockRootStore = {} as RootStore;
    systemStore = new SystemStore(mockRootStore);
    (api.get as any).mockImplementation(mockApiGet);
  });

  describe("load", () => {
    it("should load system info and populate versionInfo and serverInfo", async () => {
      const mockSystemInfo = {
        system: {
          rundeck: {
            version: "4.0.0",
            node: "test-node",
            serverUUID: "test-uuid-123",
          },
        },
      };

      (api.get as any).mockResolvedValueOnce({
        data: mockSystemInfo,
      } as any);

      await systemStore.load();

      expect(api.get).toHaveBeenCalledWith("system/info");
      expect(systemStore.loaded).toBe(true);
      expect(systemStore.versionInfo).toBeDefined();
      expect(systemStore.serverInfo).toBeDefined();
      expect(systemStore.serverInfo?.name).toBe("test-node");
      expect(systemStore.serverInfo?.uuid).toBe("test-uuid-123");
    });

    it("should not reload if already loaded", async () => {
      const mockSystemInfo = {
        system: {
          rundeck: {
            version: "4.0.0",
            node: "test-node",
            serverUUID: "test-uuid-123",
          },
        },
      };

      (api.get as any).mockResolvedValueOnce({
        data: mockSystemInfo,
      } as any);

      await systemStore.load();
      expect(api.get).toHaveBeenCalledTimes(1);

      await systemStore.load();
      expect(api.get).toHaveBeenCalledTimes(1); // Should not call again
    });
  });

  describe("loadMeta", () => {
    it("should load app metadata", () => {
      systemStore.loadMeta({
        title: "Custom Title",
        logocss: "custom-logo",
      });

      expect(systemStore.appInfo.title).toBe("Custom Title");
      expect(systemStore.appInfo.logocss).toBe("custom-logo");
    });
  });
});

