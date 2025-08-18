export const mockJobDefinition = [
  {
    defaultTab: "nodes",
    description: "",
    executionEnabled: true,
    id: "c738d66b-86fe-4d2b-98f2-6e15a87415be",
    loglevel: "INFO",
    name: "node filter with 'abc' value and editable set to false",
    nodeFilterEditable: false,
    nodefilters: {
      dispatch: {
        excludePrecedence: true,
        keepgoing: false,
        rankOrder: "ascending",
        successOnEmptyNodeFilter: false,
        threadcount: "1",
      },
      filter: "abc",
    },
    nodesSelectedByDefault: true,
    options: [
      {
        name: "seleniumOption1",
      },
    ],
    plugins: {
      ExecutionLifecycle: null,
    },
    runnerSelector: {
      runnerFilterMode: "LOCAL",
      runnerFilterType: "LOCAL_RUNNER",
    },
    scheduleEnabled: true,
    schedules: [],
    sequence: {
      commands: [
        {
          errorhandler: {
            configuration: {
              adhocLocalString: "echo ${result.resultCode}",
              interpreterArgsQuoted: "false",
            },
            nodeStep: true,
            type: "script-command",
          },
          exec: "echo selenium test",
        },
        {
          jobref: {
            group: "",
            name: "Sleep",
            uuid: "d5b2c2dd-cf59-43ab-9c04-b99cef2bfe3a",
          },
        },
      ],
      keepgoing: false,
      pluginConfig: {
        LogFilter: [
          {
            config: {
              fgcolor: "red",
              regex: "[a-z]",
            },
            type: "highlight-output",
          },
        ],
      },
      strategy: "node-first",
    },
    uuid: "c738d66b-86fe-4d2b-98f2-6e15a87415be",
  },
];
