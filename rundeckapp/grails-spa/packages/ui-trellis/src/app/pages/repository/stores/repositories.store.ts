import { defineStore } from "pinia";
import axios, { AxiosError, AxiosResponse } from "axios";
import { useOverlayStore } from "./overlay.store";

interface Repository {
  repositoryName: string;
  results: Plugin[];
}

interface Plugin {
  installId: string;
  name: string;
  display?: string;
  installed?: boolean;
  [key: string]: any;
}

interface InstallPluginProperties {
  repo: {
    repositoryName: string;
  };
  plugin: {
    installId: string;
    name: string;
    display?: string | null;
  };
}

interface UninstallPluginProperties {
  repo: {
    repositoryName: string;
  };
  plugin: {
    installId: string;
    display: string;
  };
}

interface RepositoriesState {
  repositories: Repository[] | null;
  errors: AxiosError["response"] | null;
  rdBase: string | null;
  canInstall: boolean | null;
  filterSupportType: string[] | null;
  filterPluginType: string[] | null;
  showWhichPlugins: boolean | null;
}

export const useRepositoriesStore = defineStore("repositories", {
  state: (): RepositoriesState => ({
    repositories: null,
    errors: null,
    rdBase: null,
    canInstall: null,
    filterSupportType: null,
    filterPluginType: null,
    showWhichPlugins: null,
  }),
  getters: {
    getRepos: (state): Repository[] | null => state.repositories,
  },
  actions: {
    setInstallStatusOfPluginsVisibility(
      showWhichPlugins: boolean | null,
    ): void {
      this.showWhichPlugins = showWhichPlugins;
    },
    setSupportTypeFilter(filters: string[]): void {
      this.filterSupportType = filters;
    },
    initData(): Promise<void> {
      return new Promise((resolve, reject) => {
        const overlayStore = useOverlayStore();
        overlayStore.openOverlay({
          loadingSpinner: true,
          loadingMessage: "loading plugins",
        });
        if (window._rundeck && window._rundeck.rdBase) {
          const rdBase = window._rundeck.rdBase;
          const canInstall = (window as any).repocaninstall;
          axios({
            method: "get",
            headers: {
              "x-rundeck-ajax": true,
            },
            url: `${rdBase}repository/artifacts/list`,
            withCredentials: true,
          }).then(
            (response: AxiosResponse<Repository[]>) => {
              this.rdBase = rdBase;
              this.canInstall = canInstall;
              if (response.data) {
                this.repositories = response.data;
              }
              setTimeout(() => {
                overlayStore.closeOverlay();
                resolve();
              }, 500);
            },
            (error: AxiosError) => {
              reject(error);
            },
          );
        }
      });
    },
    installPlugin(properties: InstallPluginProperties): void {
      this.errors = null;
      const overlayStore = useOverlayStore();
      overlayStore.openOverlay({
        loadingSpinner: true,
        loadingMessage: `installing ${properties.plugin.display != null ? properties.plugin.display : properties.plugin.name}`,
      });
      axios({
        method: "post",
        headers: {
          "x-rundeck-ajax": true,
        },
        url: `${this.rdBase}repository/${properties.repo.repositoryName}/install/${properties.plugin.installId}`,
        withCredentials: true,
      })
        .then((response: AxiosResponse) => {
          const repo = this.repositories?.find(
            (r) => r.repositoryName === properties.repo.repositoryName,
          );
          if (repo) {
            const plugin = repo.results.find(
              (r) => r.installId === properties.plugin.installId,
            );
            if (plugin) {
              plugin.installed = true;
            }
          }
          overlayStore.closeOverlay();
        })
        .catch((error: AxiosError) => {
          overlayStore.closeOverlay();
          this.errors = error.response || null;
          overlayStore.openOverlay({
            loadingSpinner: false,
            loadingMessage: "",
          });
        });
    },
    uninstallPlugin(properties: UninstallPluginProperties): void {
      this.errors = null;
      const overlayStore = useOverlayStore();
      overlayStore.openOverlay({
        loadingSpinner: true,
        loadingMessage: `uninstalling ${properties.plugin.display}`,
      });
      axios({
        method: "post",
        headers: {
          "x-rundeck-ajax": true,
        },
        url: `${this.rdBase}repository/uninstall/${properties.plugin.installId}`,
        withCredentials: true,
      })
        .then((response: AxiosResponse) => {
          const repo = this.repositories?.find(
            (r) => r.repositoryName === properties.repo.repositoryName,
          );
          if (repo) {
            const plugin = repo.results.find(
              (r) => r.installId === properties.plugin.installId,
            );
            if (plugin) {
              plugin.installed = false;
            }
          }
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

