import "jest";
import fs from "fs";
import path from "path";

// jobedit.js is a legacy, non-modular Grails asset script (no exports, and
// its top-level code assumes a full browser environment), so it can't be
// imported directly. Extract just the self-contained autocompleteBase
// function and evaluate it with a minimal jQuery stub instead, to keep this
// focused on the behavior in question rather than standing up the whole
// script's browser dependencies.
type AutocompleteBase = (...args: unknown[]) => void;

function loadAutocompleteBase(jQueryMock: unknown): AutocompleteBase {
  const source = fs.readFileSync(
    path.resolve(
      __dirname,
      "../../../../../grails-app/assets/javascripts/jobedit.js",
    ),
    "utf8",
  );
  const start = source.indexOf("function autocompleteBase(");
  const end = source.indexOf("\nfunction _initJobPickerAutocomplete");
  if (start === -1 || end === -1) {
    throw new Error("autocompleteBase function not found in jobedit.js");
  }
  const fnSource = source.slice(start, end);
  const factory = new Function(
    "jQuery",
    `${fnSource}\nreturn autocompleteBase;`,
  );
  return factory(jQueryMock);
}

describe("jobedit.js autocompleteBase's context_var_autocomplete binding", () => {
  it("dispatches a bubbling native input event from onSelect", () => {
    const input = document.createElement("input");
    let capturedConfig: any;

    const jQuery: any = (elem: any) =>
      elem === input
        ? {
            devbridgeAutocomplete: (config: any) => {
              capturedConfig = config;
            },
          }
        : { each: () => {} };

    const autocompleteBase = loadAutocompleteBase(jQuery);

    const liitem = {
      find: (selector: string) =>
        selector === ".context_env_autocomplete,.context_var_autocomplete"
          ? { each: (fn: (i: number, elem: Element) => void) => fn(0, input) }
          : { each: () => {} },
    };

    autocompleteBase([], liitem, false, null, null, null, null);

    expect(typeof capturedConfig?.onSelect).toBe("function");

    let received: Event | null = null;
    input.addEventListener("input", (e) => {
      received = e;
    });

    capturedConfig.onSelect();

    expect(received).not.toBeNull();
    expect(received!.bubbles).toBe(true);
  });
});
