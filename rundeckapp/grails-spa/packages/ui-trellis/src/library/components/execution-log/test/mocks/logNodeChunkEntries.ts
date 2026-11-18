import { ExecutionOutputEntry } from "../../../../stores/ExecutionOutput";

/** Three log lines for one node; the first carries a rendered step path. */
export const mockEntries: ExecutionOutputEntry[] = [
  {
    executionOutput: {} as ExecutionOutputEntry["executionOutput"],
    meta: {},
    log: "Test log entry 1",
    time: "10:30:00",
    absoluteTime: "2023-05-10T10:30:00Z",
    level: "INFO",
    lineNumber: 1,
    node: "node1",
    renderedStep: [
      { stepNumber: "1", label: "Step One", type: "command" },
      { stepNumber: "2", label: "Step Two", type: "script" },
    ],
  },
  {
    executionOutput: {} as ExecutionOutputEntry["executionOutput"],
    meta: {},
    log: "Test log entry 2",
    time: "10:30:01",
    absoluteTime: "2023-05-10T10:30:01Z",
    level: "INFO",
    lineNumber: 2,
    node: "node1",
  },
  {
    executionOutput: {} as ExecutionOutputEntry["executionOutput"],
    meta: {},
    log: "Test log entry 3",
    time: "10:30:02",
    absoluteTime: "2023-05-10T10:30:02Z",
    level: "INFO",
    lineNumber: 3,
    node: "node1",
  },
];
