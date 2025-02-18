// @ts-nocheck
import {
  expect,
  jest,
  describe,
  beforeEach,
  afterEach,
  it,
} from "@jest/globals";
import { Plugin, PluginStore, ServiceType } from "../Plugins";
import { RootStore } from "../RootStore";
import { RundeckClient } from "@rundeck/client";
import {
  mockWorkflowStepPlugins,
  mockWorkflowNodeStepPlugins,
  mockJobRefWorkflowStepPlugin,
  mockJobRefWorkflowNodeStepPlugin
} from "./mocks/mockPluginData";
import { HttpOperationResponse } from "@azure/ms-rest-js/es/lib/httpOperationResponse";
import { cloneDeep } from "lodash";

const mockRootStore = {} as RootStore;

const createMockResponse = (plugins: Plugin[]): HttpOperationResponse => ({
  headers: undefined,
  request: undefined,
  status: 200,
  parsedBody: plugins,
});

describe("PluginStore", () => {
  let pluginStore: PluginStore;
  const mockApiRequest: jest.MockedFunction<RundeckClient["apiRequest"]> =
    jest.fn();
  const mockClient: Partial<RundeckClient> = {
    apiRequest: mockApiRequest,
  };

  beforeEach(() => {
    pluginStore = new PluginStore(mockRootStore, mockClient as RundeckClient);
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.resetAllMocks();
  });

  describe("load", () => {
    it("should load plugins for a given service", async () => {
      mockApiRequest.mockResolvedValueOnce(
        createMockResponse(mockWorkflowStepPlugins),
      );
      const expectedResponse = [mockJobRefWorkflowStepPlugin, ...mockWorkflowStepPlugins]

      await pluginStore.load(ServiceType.WorkflowStep);

      expect(pluginStore.plugins).toEqual(expectedResponse);
      expect(pluginStore.pluginsByService[ServiceType.WorkflowStep]).toEqual(expectedResponse);

      const pluginNames = mockWorkflowStepPlugins.map(
        (plugin) => `${plugin.name}-${plugin.service}`,
      );

      expect(pluginStore.pluginsById[pluginNames[0]]).toEqual([
        mockWorkflowStepPlugins[0],
      ]);
      expect(pluginStore.pluginsById[pluginNames[1]]).toEqual([
        mockWorkflowStepPlugins[1],
      ]);
    });

    it("should load job reference job when service is WorkflowStep", async () => {
      mockApiRequest.mockResolvedValueOnce(
          createMockResponse(mockWorkflowStepPlugins),
      );
      const expectedResponse = [mockJobRefWorkflowStepPlugin, ...mockWorkflowStepPlugins];

      await pluginStore.load(ServiceType.WorkflowStep);

      expect(pluginStore.plugins).toEqual(expectedResponse);
      expect(pluginStore.pluginsByService[ServiceType.WorkflowStep]).toEqual(
          expectedResponse,
      );

      const pluginNames = expectedResponse.map(
          (plugin) => `${plugin.name}-${plugin.service}`,
      );

      expect(pluginStore.pluginsById[pluginNames[0]]).toEqual([
        expectedResponse[0],
      ]);
      expect(pluginStore.pluginsById[pluginNames[1]]).toEqual([
        expectedResponse[1],
      ]);
      expect(pluginStore.pluginsById[pluginNames[2]]).toEqual([
        expectedResponse[2],
      ]);
    })

    it("should not reload plugins if already loaded for the service", async () => {
      mockApiRequest.mockResolvedValueOnce(
        createMockResponse(mockWorkflowStepPlugins),
      );
      await pluginStore.load(ServiceType.WorkflowStep);

      // Clear the mock to ensure we can check if it's called again
      mockApiRequest.mockClear();
      await pluginStore.load(ServiceType.WorkflowStep);

      expect(mockClient.apiRequest).not.toHaveBeenCalled();
    });
    //
    it("should not add duplicate plugins", async () => {
      mockApiRequest.mockResolvedValue(
        createMockResponse([mockWorkflowStepPlugins[0]]),
      );
      await pluginStore.load(ServiceType.WorkflowStep);
      await pluginStore.load(ServiceType.WorkflowStep);
      await pluginStore.load(ServiceType.WorkflowStep);

      // it is 2 as workflowStep will add the plugin returned by the API + job reference
      expect(pluginStore.plugins).toHaveLength(2);
      expect(
        pluginStore.pluginsByService[ServiceType.WorkflowStep],
      ).toHaveLength(2);
    });
  });

  describe("getServicePlugins", () => {
    it("should return sorted plugins for a given service", async () => {
      mockApiRequest.mockResolvedValue(
        createMockResponse(mockWorkflowNodeStepPlugins),
      );
      await pluginStore.load(ServiceType.WorkflowNodeStep);

      const result = pluginStore.getServicePlugins(
        ServiceType.WorkflowNodeStep,
      );

      expect(result).toEqual([
        mockJobRefWorkflowNodeStepPlugin, // "Job ref"
        mockWorkflowNodeStepPlugins[1], // "Local Command"
        mockWorkflowNodeStepPlugins[0], // "SSH Command"
      ]);
    });

    it("should return an empty array for a service with no plugins", () => {
      const result = pluginStore.getServicePlugins(ServiceType.LogFilter);

      expect(result).toEqual([]);
    });
  });
});
