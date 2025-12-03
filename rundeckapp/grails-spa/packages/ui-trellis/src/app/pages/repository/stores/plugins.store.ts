import { defineStore } from "pinia";
import axios, { AxiosError, AxiosResponse } from "axios";
import _ from "lodash";
import { useOverlayStore } from "./overlay.store";
import { useModalStore } from "./modal.store";

interface Plugin {
  id: string;
  name: string;
  title?: string;
  display?: string;
  [key: string]: any;
}

interface Service {
  name: string;
  value: string;
}

interface Provider {
  serviceName: string;
  providerName: string;
  [key: string]: any;
}

interface ProviderDetail {
  provider: Provider;
  response: any;
}

interface ServiceWithProviders {
  service: string;
  providers: Plugin[];
}

interface GetProviderInfoProperties {
  serviceName: string;
  providerName: string;
}

interface UninstallPluginProperties {
  id: string;
  service: string;
  name: string;
}

interface PluginsState {
  errors: AxiosError["response"] | null;
  plugins: Plugin[];
  searchResultPlugins: Plugin[];
  pluginsByService: ServiceWithProviders[];
  provider: any | null;
  providersDetails: ProviderDetail[] | null;
  rdBase: string | null;
  services: Service[];
  serviceName: string | null;
  selectedServiceFacet: string | null;
}

export const usePluginsStore = defineStore("plugins", {
  state: (): PluginsState => ({
    errors: null,
    plugins: [],
    searchResultPlugins: [],
    pluginsByService: [],
    provider: null,
    providersDetails: null,
    rdBase: null,
    services: [],
    serviceName: null,
    selectedServiceFacet: null,
  }),
  getters: {
    getServices: (state): Service[] => state.services,
  },
  actions: {
    closeOverlay(): void {
      // No-op, kept for compatibility
    },
    setServiceFacet(serviceName: string): void {
      this.selectedServiceFacet = serviceName;
    },
    async getProvidersInfo(providers: Provider[]): Promise<void> {
      const overlayStore = useOverlayStore();
      overlayStore.openOverlay({
        loadingSpinner: true,
        loadingMessage: "loading...",
      });
      const providersDetails: ProviderDetail[] = [];

      await Promise.all(
        providers.map(async (provider) => {
          const response = await axios.request({
            method: "get",
            headers: {
              "x-rundeck-ajax": true,
            },
            url: `${this.rdBase}plugin/detail/${provider.serviceName}/${provider.providerName}`,
            withCredentials: true,
          });
          providersDetails.push({
            provider: provider,
            response: response.data,
          });
        }),
      );
      this.providersDetails = providersDetails;
      setTimeout(() => {
        overlayStore.closeOverlay();
      }, 500);
    },
    getProviderInfo(properties: GetProviderInfoProperties): void {
      const overlayStore = useOverlayStore();
      const modalStore = useModalStore();
      overlayStore.openOverlay({
        loadingSpinner: true,
        loadingMessage: "loading...",
      });
      axios
        .request({
          method: "get",
          headers: {
            "x-rundeck-ajax": true,
          },
          url: `${this.rdBase}plugin/detail/${properties.serviceName}/${properties.providerName}`,
          withCredentials: true,
        })
        .then((response: AxiosResponse) => {
          this.provider = response.data;
          this.serviceName = properties.serviceName;
          setTimeout(() => {
            overlayStore.closeOverlay();
            modalStore.openModal(true);
          }, 500);
        });
    },
    fetchServices(): Promise<Service[]> {
      return new Promise((resolve) => {
        if (
          window._rundeck &&
          window._rundeck.rdBase &&
          window._rundeck.apiVersion
        ) {
          const rdBase = window._rundeck.rdBase;
          const apiVersion = window._rundeck.apiVersion;
          axios
            .request({
              method: "get",
              headers: {
                "x-rundeck-ajax": true,
              },
              url: `${rdBase}api/${apiVersion}/plugins/types`,
              withCredentials: true,
            })
            .then((response: AxiosResponse<Service[]>) => {
              this.services = response.data;
              resolve(response.data);
            });
        }
      });
    },
    getProvidersListByService(): void {
      if (window._rundeck && window._rundeck.rdBase) {
        const rdBase = window._rundeck.rdBase;
        axios
          .request({
            method: "get",
            headers: {
              "x-rundeck-ajax": true,
            },
            url: `${rdBase}plugin/listByService`,
            withCredentials: true,
          })
          .then((response: AxiosResponse<ServiceWithProviders[]>) => {
            this.pluginsByService = response.data;
          });
      }
    },
    initData(): Promise<void> {
      return new Promise((resolve) => {
        const overlayStore = useOverlayStore();
        overlayStore.openOverlay({
          loadingSpinner: true,
          loadingMessage: "loading plugins",
        });
        if (window._rundeck && window._rundeck.rdBase) {
          const rdBase = window._rundeck.rdBase;
          axios
            .request({
              method: "get",
              headers: {
                "x-rundeck-ajax": true,
              },
              url: `${rdBase}plugin/list`,
              withCredentials: true,
            })
            .then((response: AxiosResponse<Plugin[]>) => {
              this.rdBase = rdBase;
              this.plugins = response.data;
              setTimeout(() => {
                overlayStore.closeOverlay();
                resolve();
              }, 500);
            });
        }
      });
    },
    setSearchResultPlugins(plugins: Plugin[]): void {
      this.searchResultPlugins = plugins;
    },
    uninstallPlugin(properties: UninstallPluginProperties): void {
      this.errors = null;
      const overlayStore = useOverlayStore();
      overlayStore.openOverlay({
        loadingSpinner: true,
        loadingMessage: `uninstalling ${properties.id}`,
      });
      axios
        .request({
          method: "post",
          headers: {
            "x-rundeck-ajax": true,
          },
          url: `${window._rundeck.rdBase}repository/uninstall/${properties.id}/${properties.service}/${properties.name}`,
          withCredentials: true,
        })
        .then((response: AxiosResponse) => {
          const pluginCollectionWithoutTheRemovedPlugin = _.reject(
            this.plugins,
            {
              id: properties.id,
            },
          );
          const searchResultsCollectionWithoutTheRemovedPlugin = _.reject(
            this.searchResultPlugins,
            {
              id: properties.id,
            },
          );
          this.plugins = pluginCollectionWithoutTheRemovedPlugin;
          this.searchResultPlugins = searchResultsCollectionWithoutTheRemovedPlugin;
          overlayStore.closeOverlay();
        })
        .catch((error: AxiosError) => {
          this.errors = error.response || null;
          overlayStore.openOverlay({
            loadingSpinner: true,
          });
        });
    },
  },
});

