// @ts-nocheck
import {
  expect,
  jest,
  describe,
  beforeEach,
  afterEach,
  it,
} from "@jest/globals";
import {AxiosResponse} from 'axios'
import {apiClient, api} from '../../services/api'
import { Plugin, PluginStore, ServiceType } from "../Plugins";
import { RootStore } from "../RootStore";
import {
  mockWorkflowStepPlugins,
  mockWorkflowNodeStepPlugins,
  mockJobRefWorkflowStepPlugin,
  mockJobRefWorkflowNodeStepPlugin,
  mockConditionalLogicWorkflowStepPlugin,
  mockConditionalLogicWorkflowNodeStepPlugin,
  mockPluginDetail,
} from "./mocks/mockPluginData";
import { getPluginDetail } from "@/library/services/plugins";
jest.mock('@/library/services/plugins');
const mockedGetPluginDetail = getPluginDetail as jest.MockedFunction<
  typeof getPluginDetail
>;
import { cloneDeep } from "lodash";

jest.mock("../../services/api", () => {
  const api = {
    get: jest.fn(),
    put: jest.fn(),
    post: jest.fn(),
  };
  return {
    api,
    apiClient: jest.fn().mockImplementation((vers) => api),
  }
});
jest.mock('@/library/rundeckService', () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    rdBase: 'http://localhost:4440/',
    projectName: 'testProject',
    apiVersion: '44',
  })),
}))
const mockRootStore = {} as RootStore;

const createMockResponse = (plugins: Plugin[]): AxiosResponse => ({
  status: 200,
  data: plugins,
});

describe("PluginStore", () => {
  let pluginStore: PluginStore;


  beforeEach(() => {
    apiClient.mockImplementation((vers) => api)
    pluginStore = new PluginStore(mockRootStore, null);
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.resetAllMocks();
  });

  describe("load", () => {
    it("should load plugins for a given service", async () => {
      api.get.mockResolvedValueOnce(createMockResponse(mockWorkflowStepPlugins))
      const expectedResponse = [
        mockJobRefWorkflowStepPlugin,
        mockConditionalLogicWorkflowStepPlugin,
        ...mockWorkflowStepPlugins,
      ];

      await pluginStore.load(ServiceType.WorkflowStep);

      expect(pluginStore.plugins).toEqual(expectedResponse);
      expect(pluginStore.pluginsByService[ServiceType.WorkflowStep]).toEqual(expectedResponse);

      const pluginNames = mockWorkflowStepPlugins.map(
        (plugin) => `${plugin.name}-${plugin.service}`,
      );

      expect(api.get).toHaveBeenCalledWith("plugin/list", {params:{service:ServiceType.WorkflowStep}});
      expect(pluginStore.pluginsById[pluginNames[0]]).toEqual([
        mockWorkflowStepPlugins[0],
      ]);
      expect(pluginStore.pluginsById[pluginNames[1]]).toEqual([
        mockWorkflowStepPlugins[1],
      ]);
    });

    it("should load job reference job and conditional logic plugin when service is WorkflowStep", async () => {
      api.get.mockResolvedValueOnce(createMockResponse(mockWorkflowStepPlugins))
      const expectedResponse = [
        mockJobRefWorkflowStepPlugin,
        mockConditionalLogicWorkflowStepPlugin,
        ...mockWorkflowStepPlugins,
      ];

      await pluginStore.load(ServiceType.WorkflowStep);

      expect(pluginStore.plugins).toEqual(expectedResponse);
      expect(pluginStore.pluginsByService[ServiceType.WorkflowStep]).toEqual(
          expectedResponse,
      );

      const pluginNames = expectedResponse.map(
          (plugin) => `${plugin.name}-${plugin.service}`,
      );

      expect(api.get).toHaveBeenCalledWith("plugin/list", {params:{service:ServiceType.WorkflowStep}});
      expect(pluginStore.pluginsById[pluginNames[0]]).toEqual([
        expectedResponse[0],
      ]);
      expect(pluginStore.pluginsById[pluginNames[1]]).toEqual([
        expectedResponse[1],
      ]);
      expect(pluginStore.pluginsById[pluginNames[2]]).toEqual([
        expectedResponse[2],
      ]);
      expect(pluginStore.pluginsById[pluginNames[3]]).toEqual([
        expectedResponse[3],
      ]);
    })

    it("should not reload plugins if already loaded for the service", async () => {
      api.get.mockResolvedValueOnce(createMockResponse(mockWorkflowStepPlugins))
      await pluginStore.load(ServiceType.WorkflowStep);

      // Clear the mock to ensure we can check if it's called again
      await pluginStore.load(ServiceType.WorkflowStep);

      expect(api.get).toHaveBeenCalledWith("plugin/list", {params:{service:ServiceType.WorkflowStep}});
      expect(api.get).toHaveBeenCalledTimes(1)
    });
    //
    it("should not add duplicate plugins", async () => {
      api.get.mockResolvedValue(
        createMockResponse([mockWorkflowStepPlugins[0]]),
      );
      await pluginStore.load(ServiceType.WorkflowStep);
      await pluginStore.load(ServiceType.WorkflowStep);
      await pluginStore.load(ServiceType.WorkflowStep);

      // it is 3: jobRef + conditionalLogic (both static) + 1 API plugin
      expect(pluginStore.plugins).toHaveLength(3);
      expect(
        pluginStore.pluginsByService[ServiceType.WorkflowStep],
      ).toHaveLength(3);
    });
  });

  describe("getServicePlugins", () => {
    it("should return sorted plugins for a given service", async () => {
      api.get.mockResolvedValue(
        createMockResponse(mockWorkflowNodeStepPlugins),
      );
      await pluginStore.load(ServiceType.WorkflowNodeStep);

      const result = pluginStore.getServicePlugins(
        ServiceType.WorkflowNodeStep,
      );

      expect(result).toEqual([
        mockJobRefWorkflowNodeStepPlugin,               // highlighted, order 5
        mockConditionalLogicWorkflowNodeStepPlugin,     // highlighted, order 6
        mockWorkflowNodeStepPlugins[1],                 // "Local Command" (L < S alphabetically)
        mockWorkflowNodeStepPlugins[0],                 // "SSH Command"
      ]);
    });

    it("should inject both static plugins for WorkflowNodeStep service", async () => {
      api.get.mockResolvedValueOnce(createMockResponse([]));

      await pluginStore.load(ServiceType.WorkflowNodeStep);

      expect(pluginStore.plugins).toContainEqual(mockJobRefWorkflowNodeStepPlugin);
      expect(pluginStore.plugins).toContainEqual(mockConditionalLogicWorkflowNodeStepPlugin);
    });

    it("should not inject static plugins for LogFilter service", async () => {
      api.get.mockResolvedValueOnce(createMockResponse([]));

      await pluginStore.load(ServiceType.LogFilter);

      expect(pluginStore.plugins).toHaveLength(0);
    });

    it("should return an empty array for a service with no plugins", () => {
      const result = pluginStore.getServicePlugins(ServiceType.LogFilter);

      expect(result).toEqual([]);
    });
  });
  describe("getPluginDetail", () => {
    //test the function getPluginDetail from Plugins.ts
    it("should return the plugin detail for a service provider", async () => {
      mockedGetPluginDetail.mockResolvedValue(mockPluginDetail);

      const result = await pluginStore.getPluginDetail(
        ServiceType.WorkflowStep,
        mockWorkflowStepPlugins[0].name,
      );

      expect(result).toEqual(mockPluginDetail);
    });
    it("should return the cached plugin detail for a service provider", async () => {
      mockedGetPluginDetail.mockResolvedValueOnce(mockPluginDetail);

      const result1 = await pluginStore.getPluginDetail(
        ServiceType.WorkflowStep,
        mockWorkflowStepPlugins[0].name,
      );
      const result2 = await pluginStore.getPluginDetail(
        ServiceType.WorkflowStep,
        mockWorkflowStepPlugins[0].name,
      );

      expect(result1).toEqual(mockPluginDetail);
      expect(result2).toEqual(mockPluginDetail);
    });
  });
});
