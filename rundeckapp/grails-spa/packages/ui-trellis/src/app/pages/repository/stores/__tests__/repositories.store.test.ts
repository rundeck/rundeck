import { setActivePinia, createPinia } from "pinia";
import { useRepositoriesStore } from "../repositories.store";
import { useOverlayStore } from "../overlay.store";
import axios, { AxiosResponse } from "axios";

jest.mock("axios");
jest.mock("../overlay.store", () => ({
  useOverlayStore: jest.fn(),
}));

describe("RepositoriesStore", () => {
  let overlayStoreMock: {
    openOverlay: jest.Mock;
    closeOverlay: jest.Mock;
  };

  beforeEach(() => {
    setActivePinia(createPinia());
    overlayStoreMock = {
      openOverlay: jest.fn(),
      closeOverlay: jest.fn(),
    };
    (useOverlayStore as unknown as jest.Mock).mockReturnValue(
      overlayStoreMock,
    );
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe("state", () => {
    it("should have initial state", () => {
      const store = useRepositoriesStore();
      expect(store.repositories).toBe(null);
      expect(store.errors).toBe(null);
      expect(store.rdBase).toBe(null);
      expect(store.canInstall).toBe(null);
      expect(store.filterSupportType).toBe(null);
      expect(store.filterPluginType).toBe(null);
      expect(store.showWhichPlugins).toBe(null);
    });
  });

  describe("getters", () => {
    it("getRepos should return repositories", () => {
      const store = useRepositoriesStore();
      const mockRepos = [{ repositoryName: "test", results: [] }];
      store.repositories = mockRepos;

      expect(store.getRepos).toEqual(mockRepos);
    });
  });

  describe("actions", () => {
    describe("setInstallStatusOfPluginsVisibility", () => {
      it("should set showWhichPlugins", () => {
        const store = useRepositoriesStore();
        store.setInstallStatusOfPluginsVisibility(true);

        expect(store.showWhichPlugins).toBe(true);
      });
    });

    describe("setSupportTypeFilter", () => {
      it("should set filterSupportType", () => {
        const store = useRepositoriesStore();
        const filters = ["Community", "Rundeck Supported"];
        store.setSupportTypeFilter(filters);

        expect(store.filterSupportType).toEqual(filters);
      });
    });

    describe("initData", () => {
      beforeEach(() => {
        (window as any)._rundeck = {
          rdBase: "http://localhost:4440/",
        };
        (window as any).repocaninstall = true;
      });

      afterEach(() => {
        delete (window as any)._rundeck;
        delete (window as any).repocaninstall;
      });

      it("should fetch repositories and update state", async () => {
        const store = useRepositoriesStore();
        const mockResponse: AxiosResponse = {
          data: [{ repositoryName: "official", results: [] }],
        } as AxiosResponse;

        (axios as jest.MockedFunction<typeof axios>).mockResolvedValue(
          mockResponse,
        );

        const promise = store.initData();
        expect(overlayStoreMock.openOverlay).toHaveBeenCalledWith({
          loadingSpinner: true,
          loadingMessage: "loading plugins",
        });

        await promise;

        expect(store.rdBase).toBe("http://localhost:4440/");
        expect(store.canInstall).toBe(true);
        expect(store.repositories).toEqual(mockResponse.data);
        expect(overlayStoreMock.closeOverlay).toHaveBeenCalled();
      });

      it("should reject on error", async () => {
        const store = useRepositoriesStore();
        const error = new Error("Network error");
        (axios as jest.MockedFunction<typeof axios>).mockRejectedValue(error);

        await expect(store.initData()).rejects.toThrow("Network error");
      });
    });

    describe("installPlugin", () => {
      beforeEach(() => {
        (window as any)._rundeck = {
          rdBase: "http://localhost:4440/",
        };
      });

      afterEach(() => {
        delete (window as any)._rundeck;
      });

      it("should install plugin and update state", async () => {
        const store = useRepositoriesStore();
        store.rdBase = "http://localhost:4440/";
        store.repositories = [
          {
            repositoryName: "test-repo",
            results: [
              { installId: "plugin-1", name: "plugin-1", installed: false },
            ],
          },
        ];

        const properties = {
          repo: { repositoryName: "test-repo" },
          plugin: {
            installId: "plugin-1",
            name: "plugin-1",
            display: "Test Plugin",
          },
        };

        (axios as jest.MockedFunction<typeof axios>).mockResolvedValue({
          data: {},
        } as AxiosResponse);

        store.installPlugin(properties);

        await new Promise((resolve) => setTimeout(resolve, 100));

        expect(overlayStoreMock.openOverlay).toHaveBeenCalledWith({
          loadingSpinner: true,
          loadingMessage: "installing Test Plugin",
        });
        expect(axios).toHaveBeenCalledWith(
          expect.objectContaining({
            method: "post",
            url: "http://localhost:4440/repository/test-repo/install/plugin-1",
          }),
        );
      });
    });

    describe("uninstallPlugin", () => {
      beforeEach(() => {
        (window as any)._rundeck = {
          rdBase: "http://localhost:4440/",
        };
      });

      afterEach(() => {
        delete (window as any)._rundeck;
      });

      it("should uninstall plugin and update state", async () => {
        const store = useRepositoriesStore();
        store.rdBase = "http://localhost:4440/";
        store.repositories = [
          {
            repositoryName: "test-repo",
            results: [
              { installId: "plugin-1", name: "plugin-1", installed: true },
            ],
          },
        ];

        const properties = {
          repo: { repositoryName: "test-repo" },
          plugin: {
            installId: "plugin-1",
            name: "plugin-1",
            display: "Test Plugin",
          },
        };

        (axios as jest.MockedFunction<typeof axios>).mockResolvedValue({
          data: {},
        } as AxiosResponse);

        store.uninstallPlugin(properties);

        await new Promise((resolve) => setTimeout(resolve, 100));

        expect(overlayStoreMock.openOverlay).toHaveBeenCalledWith({
          loadingSpinner: true,
          loadingMessage: "uninstalling Test Plugin",
        });
        expect(axios).toHaveBeenCalledWith(
          expect.objectContaining({
            method: "post",
            url: "http://localhost:4440/repository/uninstall/plugin-1",
          }),
        );
      });
    });
  });
});

