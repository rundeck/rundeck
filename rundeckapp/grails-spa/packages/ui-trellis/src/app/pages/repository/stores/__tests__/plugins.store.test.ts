import { setActivePinia, createPinia } from "pinia";
import { usePluginsStore } from "../plugins.store";
import { useOverlayStore } from "../overlay.store";
import { useModalStore } from "../modal.store";
import axios, { AxiosResponse } from "axios";
import _ from "lodash";

jest.mock("axios");
jest.mock("lodash");
jest.mock("../overlay.store", () => ({
  useOverlayStore: jest.fn(),
}));
jest.mock("../modal.store", () => ({
  useModalStore: jest.fn(),
}));

describe("PluginsStore", () => {
  let overlayStoreMock: {
    openOverlay: jest.Mock;
    closeOverlay: jest.Mock;
  };
  let modalStoreMock: {
    openModal: jest.Mock;
  };

  beforeEach(() => {
    setActivePinia(createPinia());
    overlayStoreMock = {
      openOverlay: jest.fn(),
      closeOverlay: jest.fn(),
    };
    modalStoreMock = {
      openModal: jest.fn(),
    };
    (useOverlayStore as unknown as jest.Mock).mockReturnValue(
      overlayStoreMock,
    );
    (useModalStore as unknown as jest.Mock).mockReturnValue(modalStoreMock);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe("state", () => {
    it("should have initial state", () => {
      const store = usePluginsStore();
      expect(store.errors).toBe(null);
      expect(store.plugins).toEqual([]);
      expect(store.searchResultPlugins).toEqual([]);
      expect(store.pluginsByService).toEqual([]);
      expect(store.provider).toBe(null);
      expect(store.providersDetails).toBe(null);
      expect(store.rdBase).toBe(null);
      expect(store.services).toEqual([]);
      expect(store.serviceName).toBe(null);
      expect(store.selectedServiceFacet).toBe(null);
    });
  });

  describe("getters", () => {
    it("getServices should return services", () => {
      const store = usePluginsStore();
      const mockServices = [{ name: "test", value: "Test Service" }];
      store.services = mockServices;

      expect(store.getServices).toEqual(mockServices);
    });
  });

  describe("actions", () => {
    describe("setServiceFacet", () => {
      it("should set selectedServiceFacet", () => {
        const store = usePluginsStore();
        store.setServiceFacet("test-service");

        expect(store.selectedServiceFacet).toBe("test-service");
      });
    });

    describe("setSearchResultPlugins", () => {
      it("should set searchResultPlugins", () => {
        const store = usePluginsStore();
        const plugins = [{ id: "1", name: "Plugin 1" }];
        store.setSearchResultPlugins(plugins);

        expect(store.searchResultPlugins).toEqual(plugins);
      });
    });

    describe("initData", () => {
      beforeEach(() => {
        (window as any)._rundeck = {
          rdBase: "http://localhost:4440/",
        };
      });

      afterEach(() => {
        delete (window as any)._rundeck;
      });

      it("should fetch plugins and update state", async () => {
        const store = usePluginsStore();
        const mockResponse: AxiosResponse = {
          data: [{ id: "1", name: "Plugin 1" }],
        } as AxiosResponse;

        (axios.request as jest.MockedFunction<typeof axios.request>).mockResolvedValue(
          mockResponse,
        );

        const promise = store.initData();
        expect(overlayStoreMock.openOverlay).toHaveBeenCalledWith({
          loadingSpinner: true,
          loadingMessage: "loading plugins",
        });

        await promise;

        expect(store.rdBase).toBe("http://localhost:4440/");
        expect(store.plugins).toEqual(mockResponse.data);
        expect(overlayStoreMock.closeOverlay).toHaveBeenCalled();
      });
    });

    describe("getServices", () => {
      beforeEach(() => {
        (window as any)._rundeck = {
          rdBase: "http://localhost:4440/",
          apiVersion: "41",
        };
      });

      afterEach(() => {
        delete (window as any)._rundeck;
      });

      it("should fetch services and update state", async () => {
        const store = usePluginsStore();
        const mockResponse: AxiosResponse<{ name: string; value: string }[]> = {
          data: [{ name: "test", value: "Test Service" }],
        } as AxiosResponse<{ name: string; value: string }[]>;

        (axios.request as jest.MockedFunction<typeof axios.request>).mockResolvedValue(
          mockResponse,
        );

        const promise = store.fetchServices();
        expect(promise).toBeInstanceOf(Promise);
        const result = await promise;

        expect(store.services).toEqual(mockResponse.data);
        expect(result).toEqual(mockResponse.data);
      });
    });

    describe("getProviderInfo", () => {
      beforeEach(() => {
        (window as any)._rundeck = {
          rdBase: "http://localhost:4440/",
        };
      });

      afterEach(() => {
        delete (window as any)._rundeck;
      });

      it("should fetch provider info and open modal", async () => {
        const store = usePluginsStore();
        store.rdBase = "http://localhost:4440/";
        const mockResponse: AxiosResponse = {
          data: { name: "test-provider", title: "Test Provider" },
        } as AxiosResponse;

        (axios.request as jest.MockedFunction<typeof axios.request>).mockResolvedValue(
          mockResponse,
        );

        store.getProviderInfo({
          serviceName: "test-service",
          providerName: "test-provider",
        });

        await new Promise((resolve) => setTimeout(resolve, 600));

        expect(overlayStoreMock.openOverlay).toHaveBeenCalledWith({
          loadingSpinner: true,
          loadingMessage: "loading...",
        });
        expect(store.provider).toEqual(mockResponse.data);
        expect(store.serviceName).toBe("test-service");
        expect(overlayStoreMock.closeOverlay).toHaveBeenCalled();
        expect(modalStoreMock.openModal).toHaveBeenCalledWith(true);
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

      it("should remove plugin from collections", async () => {
        const store = usePluginsStore();
        store.plugins = [
          { id: "1", name: "Plugin 1" },
          { id: "2", name: "Plugin 2" },
        ];
        store.searchResultPlugins = [
          { id: "1", name: "Plugin 1" },
          { id: "2", name: "Plugin 2" },
        ];

        (axios.request as jest.MockedFunction<typeof axios.request>).mockResolvedValue({
          data: {},
        } as AxiosResponse);

        (_.reject as jest.MockedFunction<typeof _.reject>).mockImplementation(
          (array: any[], predicate: any) => {
            if (typeof predicate === "object" && predicate.id) {
              return array.filter((item) => item.id !== predicate.id);
            }
            return array;
          },
        );

        store.uninstallPlugin({
          id: "1",
          service: "test-service",
          name: "test-plugin",
        });

        await new Promise((resolve) => setTimeout(resolve, 100));

        expect(overlayStoreMock.openOverlay).toHaveBeenCalledWith({
          loadingSpinner: true,
          loadingMessage: "uninstalling 1",
        });
        expect(axios.request).toHaveBeenCalledWith(
          expect.objectContaining({
            method: "post",
            url: "http://localhost:4440/repository/uninstall/1/test-service/test-plugin",
          }),
        );
      });
    });
  });
});

