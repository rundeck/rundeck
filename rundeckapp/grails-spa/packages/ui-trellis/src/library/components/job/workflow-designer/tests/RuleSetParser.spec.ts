import Util from "util";

import "jest";

import {
  RuleSetParser,
  RuleBuilder,
  isCondition,
  Directive,
} from "../RuleSetParser";

describe("RuleSet Parser", () => {
  it("parses", () => {
    const rulesString = "[1] run-at-start\n[2] run-after:1";

    const rulesSet = RuleSetParser.ParseRules(rulesString);

    const [rule1, rule2] = rulesSet.rules;

    expect(rule1.identifiers).toEqual(["1"]);
    expect(rule1.rules[0].type).toEqual("run-at-start");

    expect(rule2.identifiers).toEqual(["2"]);
    expect(rule2.rules[0].type).toEqual("run-after");
  });
  it("parses with labels", () => {
    const rulesString = "[a] run-at-start\n[b] run-after:a";

    const rulesSet = RuleSetParser.ParseRules(rulesString);

    const [rule1, rule2] = rulesSet.rules;

    expect(rule1.identifiers).toEqual(["a"]);
    expect(rule1.rules[0].type).toEqual("run-at-start");

    expect(rule2.identifiers).toEqual(["b"]);
    expect(rule2.rules[0].type).toEqual("run-after");
    expect((rule2.rules[0] as Directive).identifiers).toEqual(["a"]);
  });
  it("toString quotes spaces in condiitions", () => {
    const rulesString = '[1] "if:option.a==a b c"';

    const rulesSet = RuleSetParser.ParseRules(rulesString);

    const [rule1] = rulesSet.rules;

    expect(rule1.identifiers).toEqual(["1"]);
    let parsedRule = rule1.rules[0];
    expect(parsedRule.type).toEqual("if");

    expect(isCondition(parsedRule)).toBeTruthy();
    if (isCondition(parsedRule)) {
      expect(parsedRule.expression).toEqual("option.a==a b c");
    }

    expect(rule1.toString()).toEqual(rulesString);
  });
});

describe("RuleBuilder", () => {
  it("expands range", () => {
    const builder = new RuleBuilder();

    const range = builder.expandRange("1-5");

    expect(range).toEqual(["1", "2", "3", "4", "5"]);
  });

  it("returns id if not range-like", () => {
    const builder = new RuleBuilder();

    let range = builder.expandRange("1-5-6");
    expect(range).toEqual(["1-5-6"]);

    range = builder.expandRange("Not-Range");
    expect(range).toEqual(["Not-Range"]);
  });
});
