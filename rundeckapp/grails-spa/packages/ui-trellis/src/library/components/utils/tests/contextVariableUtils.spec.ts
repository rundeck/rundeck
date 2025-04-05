import { contextVariables } from "../../../stores/context_variables";
import { isAutoCompleteField, getContextVariables } from "../contextVariableUtils";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440/",
    projectName: "testProject",
    apiVersion: "44",
    data: {
      options: {
        optionsData: [
          {
            name: "a_custom_variable",
            label: "a custom variable",
            description: "a variable representing something not supplied by rundeck by default",
          },
        ],
      },
    },
  })),
}));

jest.mock("../../../stores/JobsStore", () => ({
  useJobStore: jest.fn().mockImplementation(() => ({
    activeId: "",
    jobs: [],
    contextVariables: contextVariables(),
  })),
}));

const wrappedInBracesRegex = /^\$\{.*\}$/;
const startsWithEnvPrefix = /RD_.*/;

describe("contextVariableUtils", () => {
  describe("isAutoCompleteField", () => {
    it("returns 'true' when supplied step is 'WorkflowNodeStep", () => {
      expect(isAutoCompleteField("WorkflowNodeStep")).toEqual(true);
    });

    it("returns 'true' when supplied step is 'WorkflowStep", () => {
      expect(isAutoCompleteField("WorkflowStep")).toEqual(true);
    });

    it("returns 'true' when supplied step is 'Notification", () => {
      expect(isAutoCompleteField("Notification")).toEqual(true);
    });

    it("returns 'false' when supplied step is not a supported autocomplete step", () => {
      expect(isAutoCompleteField("FooBar")).toEqual(false);
    });
  });

  describe("getContextVariables", () => {
    describe("when fieldType is 'input'", () => {
      describe("when stepType isn't specified", () => {
        it("returns an empty array", () => {
          const variables = getContextVariables("input");
          expect(variables).toEqual([]);
        });
      });

      describe("when supplied stepType is 'WorkflowNodeStep'", () => {
        describe("when pluginType is not supplied", () => {
          const variables = getContextVariables("input", "WorkflowNodeStep");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'node'", () => {
            expect(
              variables.every((variable) => ["node", "job", "option"].includes(variable.type)),
            ).toBe(true);
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
            expect(variables.some((variable) => variable.type === "node")).toBe(true);
          });

          it("returns all variables wrapped in braces", () => {
            expect(variables.every((variable) => wrappedInBracesRegex.test(variable.name))).toBe(
              true,
            );
          });

          it("returns no variables in uppercase (in the ENV style)", () => {
            expect(
              variables.every((variable) => variable.name !== variable.name.toUpperCase()),
            ).toBe(true);
          });

          it("is not prefixed with env prefix 'RD_'", () => {
            expect(variables.every((variable) => !startsWithEnvPrefix.test(variable.name))).toBe(
              true,
            );
          });
        });

        describe("when pluginType is 'script-inline'", () => {
          const variables = getContextVariables("input", "WorkflowNodeStep", "script-inline");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'node'", () => {
            expect(
              variables.every((variable) => ["node", "job", "option"].includes(variable.type)),
            ).toBe(true);
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
            expect(variables.some((variable) => variable.type === "node")).toBe(true);
          });

          it("returns all variables wrapped in braces", () => {
            expect(variables.every((variable) => wrappedInBracesRegex.test(variable.name))).toBe(
              true,
            );
          });

          it("returns no variables in uppercase (in the ENV style)", () => {
            expect(
              variables.every((variable) => variable.name !== variable.name.toUpperCase()),
            ).toBe(true);
          });

          it("is not prefixed with env prefix 'RD_'", () => {
            expect(variables.every((variable) => !startsWithEnvPrefix.test(variable.name))).toBe(
              true,
            );
          });
        });

        describe("when pluginType is supplied but not 'script-inline", () => {
          const variables = getContextVariables("input", "WorkflowNodeStep", "foobar");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'node'", () => {
            expect(
              variables.every((variable) => ["node", "job", "option"].includes(variable.type)),
            ).toBe(true);
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
            expect(variables.some((variable) => variable.type === "node")).toBe(true);
          });

          it("returns all variables wrapped in braces", () => {
            expect(variables.every((variable) => wrappedInBracesRegex.test(variable.name))).toBe(
              true,
            );
          });

          it("returns no variables in uppercase (in the ENV style)", () => {
            expect(
              variables.every((variable) => variable.name !== variable.name.toUpperCase()),
            ).toBe(true);
          });

          it("is not prefixed with env prefix 'RD_'", () => {
            expect(variables.every((variable) => !startsWithEnvPrefix.test(variable.name))).toBe(
              true,
            );
          });
        });
      });

      describe("when supplied stepType is 'WorkflowStep'", () => {
        describe("when pluginType is not supplied", () => {
          const variables = getContextVariables("input", "WorkflowStep");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' and 'option", () => {
            expect(variables.every((variable) => ["job", "option"].includes(variable.type))).toBe(
              true,
            );
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
          });

          it("returns all variables wrapped in braces", () => {
            expect(variables.every((variable) => wrappedInBracesRegex.test(variable.name))).toBe(
              true,
            );
          });

          it("returns no variables in uppercase (in the ENV style)", () => {
            expect(
              variables.every((variable) => variable.name !== variable.name.toUpperCase()),
            ).toBe(true);
          });

          it("is not prefixed with env prefix 'RD_'", () => {
            expect(variables.every((variable) => !startsWithEnvPrefix.test(variable.name))).toBe(
              true,
            );
          });
        });

        describe("when pluginType is 'script-inline'", () => {
          const variables = getContextVariables("input", "WorkflowStep", "script-inline");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' and 'option", () => {
            expect(variables.every((variable) => ["job", "option"].includes(variable.type))).toBe(
              true,
            );
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
          });

          it("returns all variables wrapped in braces", () => {
            expect(variables.every((variable) => wrappedInBracesRegex.test(variable.name))).toBe(
              true,
            );
          });

          it("returns no variables in uppercase (in the ENV style)", () => {
            expect(
              variables.every((variable) => variable.name !== variable.name.toUpperCase()),
            ).toBe(true);
          });

          it("is not prefixed with env prefix 'RD_'", () => {
            expect(variables.every((variable) => !startsWithEnvPrefix.test(variable.name))).toBe(
              true,
            );
          });
        });

        describe("when pluginType is supplied but not 'script-inline", () => {
          const variables = getContextVariables("input", "WorkflowStep", "foobar");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' and 'option", () => {
            expect(variables.every((variable) => ["job", "option"].includes(variable.type))).toBe(
              true,
            );
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
          });

          it("returns all variables wrapped in braces", () => {
            expect(variables.every((variable) => wrappedInBracesRegex.test(variable.name))).toBe(
              true,
            );
          });

          it("returns no variables in uppercase (in the ENV style)", () => {
            expect(
              variables.every((variable) => variable.name !== variable.name.toUpperCase()),
            ).toBe(true);
          });

          it("is not prefixed with env prefix 'RD_'", () => {
            expect(variables.every((variable) => !startsWithEnvPrefix.test(variable.name))).toBe(
              true,
            );
          });
        });

        describe("when supplied stepType is 'Notification'", () => {
          describe("when pluginType is not supplied", () => {
            const variables = getContextVariables("input", "Notification");

            it("returns a populated array", () => {
              expect(variables.length).toBeGreaterThan(0);
            });

            it("returns only variables of type 'job' and 'execution'", () => {
              expect(
                variables.every((variable) =>
                  ["execution", "job", "option"].includes(variable.type),
                ),
              ).toBe(true);
              expect(variables.some((variable) => variable.type === "job")).toBe(true);
              expect(variables.some((variable) => variable.type === "option")).toBe(true);
              expect(variables.some((variable) => variable.type === "execution")).toBe(true);
            });

            it("returns all variables wrapped in braces", () => {
              expect(variables.every((variable) => wrappedInBracesRegex.test(variable.name))).toBe(
                true,
              );
            });

            it("returns no variables in uppercase (in the ENV style)", () => {
              expect(
                variables.every((variable) => variable.name !== variable.name.toUpperCase()),
              ).toBe(true);
            });

            it("is not prefixed with env prefix 'RD_'", () => {
              expect(variables.every((variable) => !startsWithEnvPrefix.test(variable.name))).toBe(
                true,
              );
            });
          });

          describe("when pluginType is 'script-inline'", () => {
            const variables = getContextVariables("input", "Notification", "script-inline");

            it("returns a populated array", () => {
              expect(variables.length).toBeGreaterThan(0);
            });

            it("returns only variables of type 'job' and 'execution'", () => {
              expect(
                variables.every((variable) =>
                  ["execution", "job", "option"].includes(variable.type),
                ),
              ).toBe(true);
              expect(variables.some((variable) => variable.type === "job")).toBe(true);
              expect(variables.some((variable) => variable.type === "option")).toBe(true);
              expect(variables.some((variable) => variable.type === "execution")).toBe(true);
            });

            it("returns all variables wrapped in braces", () => {
              expect(variables.every((variable) => wrappedInBracesRegex.test(variable.name))).toBe(
                true,
              );
            });

            it("returns no variables in uppercase (in the ENV style)", () => {
              expect(
                variables.every((variable) => variable.name !== variable.name.toUpperCase()),
              ).toBe(true);
            });

            it("is not prefixed with env prefix 'RD_'", () => {
              expect(variables.every((variable) => !startsWithEnvPrefix.test(variable.name))).toBe(
                true,
              );
            });
          });

          describe("when pluginType is supplied but not 'script-inline", () => {
            const variables = getContextVariables("input", "Notification", "foobar");

            it("returns a populated array", () => {
              expect(variables.length).toBeGreaterThan(0);
            });

            it("returns only variables of type 'job' and 'execution'", () => {
              expect(
                variables.every((variable) =>
                  ["execution", "job", "option"].includes(variable.type),
                ),
              ).toBe(true);
              expect(variables.some((variable) => variable.type === "job")).toBe(true);
              expect(variables.some((variable) => variable.type === "option")).toBe(true);
              expect(variables.some((variable) => variable.type === "execution")).toBe(true);
            });

            it("returns all variables wrapped in braces", () => {
              expect(variables.every((variable) => wrappedInBracesRegex.test(variable.name))).toBe(
                true,
              );
            });

            it("returns no variables in uppercase (in the ENV style)", () => {
              expect(
                variables.every((variable) => variable.name !== variable.name.toUpperCase()),
              ).toBe(true);
            });

            it("is not prefixed with env prefix 'RD_'", () => {
              expect(variables.every((variable) => !startsWithEnvPrefix.test(variable.name))).toBe(
                true,
              );
            });
          });
        });
      });
    });

    describe("when fieldType is 'script'", () => {
      describe("when stepType isn't specified", () => {
        it("returns an empty array", () => {
          const variables = getContextVariables("script");
          expect(variables).toEqual([]);
        });
      });

      describe("when supplied stepType is 'WorkflowNodeStep'", () => {
        describe("when pluginType is not supplied", () => {
          const variables = getContextVariables("script", "WorkflowNodeStep");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'node'", () => {
            expect(
              variables.every((variable) => ["node", "job", "option"].includes(variable.type)),
            ).toBe(true);
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
            expect(variables.some((variable) => variable.type === "node")).toBe(true);
          });

          it("returns non-environment variable varables=", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name !== variable.name.toUpperCase() &&
                  !startsWithEnvPrefix.test(variable.name) &&
                  wrappedInBracesRegex.test(variable.name),
              ),
            ).toBe(true);
          });

          it("returns environment variable varables", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "$",
              ),
            ).toBe(true);
          });

          it("does not return environment variables wrapped with '@'", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "@",
              ),
            ).toBe(false);
          });
        });

        describe("when pluginType is 'script-inline'", () => {
          const variables = getContextVariables("script", "WorkflowNodeStep", "script-inline");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'node'", () => {
            expect(
              variables.every((variable) => ["node", "job", "option"].includes(variable.type)),
            ).toBe(true);
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
            expect(variables.some((variable) => variable.type === "node")).toBe(true);
          });

          it("returns non-environment variable varables=", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name !== variable.name.toUpperCase() &&
                  !startsWithEnvPrefix.test(variable.name) &&
                  wrappedInBracesRegex.test(variable.name),
              ),
            ).toBe(true);
          });

          it("returns environment variable varables", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "$",
              ),
            ).toBe(true);
          });

          it("returns environment variables wrapped with '@'", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "@",
              ),
            ).toBe(true);
          });
        });

        describe("when pluginType is supplied but not 'script-inline", () => {
          const variables = getContextVariables("script", "WorkflowNodeStep", "foobar");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'node'", () => {
            expect(
              variables.every((variable) => ["node", "job", "option"].includes(variable.type)),
            ).toBe(true);
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
            expect(variables.some((variable) => variable.type === "node")).toBe(true);
          });

          it("returns non-environment variable varables=", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name !== variable.name.toUpperCase() &&
                  !startsWithEnvPrefix.test(variable.name) &&
                  wrappedInBracesRegex.test(variable.name),
              ),
            ).toBe(true);
          });

          it("returns environment variable varables", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "$",
              ),
            ).toBe(true);
          });

          it("does not return environment variables wrapped with '@'", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "@",
              ),
            ).toBe(false);
          });
        });
      });

      describe("when supplied stepType is 'WorkflowStep'", () => {
        describe("when pluginType is not supplied", () => {
          const variables = getContextVariables("script", "WorkflowStep");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'option'", () => {
            expect(variables.every((variable) => ["job", "option"].includes(variable.type))).toBe(
              true,
            );
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
          });

          it("returns non-environment variable varables=", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name !== variable.name.toUpperCase() &&
                  !startsWithEnvPrefix.test(variable.name) &&
                  wrappedInBracesRegex.test(variable.name),
              ),
            ).toBe(true);
          });

          it("returns environment variable varables", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "$",
              ),
            ).toBe(true);
          });

          it("does not return environment variables wrapped with '@'", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "@",
              ),
            ).toBe(false);
          });
        });

        describe("when pluginType is 'script-inline'", () => {
          const variables = getContextVariables("script", "WorkflowStep", "script-inline");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'option'", () => {
            expect(variables.every((variable) => ["job", "option"].includes(variable.type))).toBe(
              true,
            );
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
          });

          it("returns non-environment variable varables", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name !== variable.name.toUpperCase() &&
                  !startsWithEnvPrefix.test(variable.name) &&
                  wrappedInBracesRegex.test(variable.name),
              ),
            ).toBe(true);
          });

          it("returns environment variable varables", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "$",
              ),
            ).toBe(true);
          });

          it("returns environment variables wrapped with '@'", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "@",
              ),
            ).toBe(true);
          });
        });

        describe("when pluginType is supplied but not 'script-inline", () => {
          const variables = getContextVariables("script", "WorkflowStep", "foobar");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'option'", () => {
            expect(variables.every((variable) => ["job", "option"].includes(variable.type))).toBe(
              true,
            );
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
          });

          it("returns non-environment variable varables=", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name !== variable.name.toUpperCase() &&
                  !startsWithEnvPrefix.test(variable.name) &&
                  wrappedInBracesRegex.test(variable.name),
              ),
            ).toBe(true);
          });

          it("returns environment variable varables", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "$",
              ),
            ).toBe(true);
          });

          it("does not return environment variables wrapped with '@'", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "@",
              ),
            ).toBe(false);
          });
        });
      });

      describe("when supplied stepType is 'Notification'", () => {
        describe("when pluginType is not supplied", () => {
          const variables = getContextVariables("script", "Notification");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'execution'", () => {
            expect(
              variables.every((variable) => ["execution", "job", "option"].includes(variable.type)),
            ).toBe(true);
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
            expect(variables.some((variable) => variable.type === "execution")).toBe(true);
          });

          it("returns non-environment variable varables=", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name !== variable.name.toUpperCase() &&
                  !startsWithEnvPrefix.test(variable.name) &&
                  wrappedInBracesRegex.test(variable.name),
              ),
            ).toBe(true);
          });

          it("returns environment variable varables", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "$",
              ),
            ).toBe(true);
          });

          it("does not return environment variables wrapped with '@'", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "@",
              ),
            ).toBe(false);
          });
        });

        describe("when pluginType is 'script-inline'", () => {
          const variables = getContextVariables("script", "Notification", "script-inline");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'execution'", () => {
            expect(
              variables.every((variable) => ["execution", "job", "option"].includes(variable.type)),
            ).toBe(true);
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
            expect(variables.some((variable) => variable.type === "execution")).toBe(true);
          });

          it("returns non-environment variable varables", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name !== variable.name.toUpperCase() &&
                  !startsWithEnvPrefix.test(variable.name) &&
                  wrappedInBracesRegex.test(variable.name),
              ),
            ).toBe(true);
          });

          it("returns environment variable varables", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "$",
              ),
            ).toBe(true);
          });

          it("returns environment variables wrapped with '@'", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "@",
              ),
            ).toBe(true);
          });
        });

        describe("when pluginType is supplied but not 'script-inline", () => {
          const variables = getContextVariables("script", "Notification", "foobar");

          it("returns a populated array", () => {
            expect(variables.length).toBeGreaterThan(0);
          });

          it("returns only variables of type 'job' or 'execution'", () => {
            expect(
              variables.every((variable) => ["execution", "job", "option"].includes(variable.type)),
            ).toBe(true);
            expect(variables.some((variable) => variable.type === "job")).toBe(true);
            expect(variables.some((variable) => variable.type === "option")).toBe(true);
            expect(variables.some((variable) => variable.type === "execution")).toBe(true);
          });

          it("returns non-environment variable varables=", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name !== variable.name.toUpperCase() &&
                  !startsWithEnvPrefix.test(variable.name) &&
                  wrappedInBracesRegex.test(variable.name),
              ),
            ).toBe(true);
          });

          it("returns environment variable varables", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "$",
              ),
            ).toBe(true);
          });

          it("does not return environment variables wrapped with '@'", () => {
            expect(
              variables.some(
                (variable) =>
                  variable.name === variable.name.toUpperCase() &&
                  startsWithEnvPrefix.test(variable.name) &&
                  variable.name.charAt(0) === "@",
              ),
            ).toBe(false);
          });
        });
      });
    });
  });
});
