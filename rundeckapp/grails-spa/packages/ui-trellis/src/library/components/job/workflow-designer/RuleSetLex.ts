export enum TokenType {
  stepIdentifier = "stepIdentifier",
  conditionType = "conditionType",
  conditionExpression = "conditionExpression",
  directiveType = "directiveType",
  EOF = "EOF",
}

export const directiveTypes = ["run-after", "run-at-start", "run-in-sequence"];

const nonStepDirectiveTypes = ["run-at-start", "run-in-sequence"];

export const conditionTypes = ["if", "unless"];

type LexFunc = () => LexFunc;

export interface Token {
  type: TokenType;
  string: string;
  start: number;
}

export class RuleSetLex {
  characters: Array<string>;
  pos: number = 0;
  start: number = 0;
  items: Array<Token> = [];
  sep: string = ",";
  delim: string = " ";

  constructor(input: string) {
    // Break this up into a character array
    this.characters = [...input];
  }

  lex() {
    let lexFunc = this.lexStart;
    try {
      while (true) {
        lexFunc = lexFunc();
      }
    } catch (e) {
      const ex = e as Error;
      if (ex.message == "EOF") this.emit(TokenType.EOF);
      else throw ex;
    }
  }

  next(): string | null {
    if (this.pos >= this.characters.length) {
      this.pos++;
      return null;
    }

    const c = this.characters[this.pos];

    this.pos++;
    return c;
  }

  peek(): string | null {
    const c = this.next();
    this.backup();
    return c;
  }

  ignore() {
    this.start = this.pos;
  }

  backup() {
    this.pos--;
  }

  emit(type: TokenType): Token {
    let string;

    if (type == TokenType.EOF) string = "EOF";
    else string = this.characters.slice(this.start, this.pos).join("").trim();

    const item = {
      type,
      start: this.start,
      string,
    };
    this.items.push(item);
    this.start = this.pos;
    return item;
  }

  lexStart = (): (() => LexFunc) => {
    const c = this.next();

    switch (c) {
      case null:
        throw new Error("EOF");
      case " ":
        this.ignore();
        return this.lexStart;
      case '"':
        this.ignore();
        this.delim = '"';
        return this.lexStepIdentifier;
      case "[":
        this.ignore();
        this.delim = "]";
        return this.lexStepIdentifier;
      default:
        throw new Error("Invalid");
    }
  };

  /** Scan workflow step identifier, makes no attempt to distinguish between label
   * or step number.
   */
  lexStepIdentifier = (): (() => LexFunc) => {
    const c = this.next();

    switch (c) {
      case null:
        this.backup();
        this.emit(TokenType.stepIdentifier);
        throw new Error("EOF");
      case this.sep:
        this.pos--;
        this.emit(TokenType.stepIdentifier);
        this.pos++;
        this.ignore();
        return this.lexStepIdentifier;
      case this.delim:
        this.pos--;
        this.emit(TokenType.stepIdentifier);
        this.pos++;
        this.ignore();
        const next = this.peek();
        if (next == null) throw new Error("EOF");
        else return this.lexConditionOrDirective;
      default:
        return this.lexStepIdentifier;
    }
  };

  /**
   * Scans steps depending on the type of last directive item
   *
   */
  lexDirective = (): (() => LexFunc) => {
    const lastToken: Token = this.items.slice(-1)[0];
    if (nonStepDirectiveTypes.indexOf(lastToken.string) > -1) {
      return this.lexConditionOrDirective;
    }
    return this.lexStepIdentifier;
  };

  /** Scans a condition or directive keyword and continues scannig for the subsequent
   * value of type expression or step identifier list
   */
  lexConditionOrDirective = (): (() => LexFunc) => {
    let c = this.next();

    if (c == null) {
      throw new Error("EOF");
    }
    /** Burn whitespace */
    while (c == " ") {
      this.ignore();
      c = this.next();
    }

    this.delim = c == '"' ? '"' : " ";

    if (this.delim == '"') {
      this.ignore();
      this.next();
    }

    const index = this.getIndexEndOfRuleExpression(this.pos - 1);

    if (index == -1) {
      this.pos = this.characters.length;
      this.emitConditionOrDirective();
      return this.lexStart;
    } else {
      this.pos += index - 1;
      const type = this.emitConditionOrDirective();
      this.pos++;
      this.ignore();
      switch (type.type) {
        case TokenType.directiveType:
          return this.lexDirective;
        case TokenType.conditionType:
          return this.lexExpression;
        default:
          return this.lexStepIdentifier;
      }
    }
  };

  /** Scan condition expression as a single token */
  lexExpression = (): (() => LexFunc) => {
    const c = this.next();

    switch (c) {
      case null:
        this.backup();
        this.emit(TokenType.conditionExpression);
        throw new Error("EOF");
      case this.delim:
        this.backup();
        this.emit(TokenType.conditionExpression);
        this.next();
        this.ignore();
        return this.lexConditionOrDirective;
      default:
        return this.lexExpression;
    }
  };

  emitConditionOrDirective(): Token {
    const string = this.characters.slice(this.start, this.pos).join("");

    if (directiveTypes.indexOf(string) > -1)
      return this.emit(TokenType.directiveType);

    if (conditionTypes.indexOf(string) > -1)
      return this.emit(TokenType.conditionType);

    throw new Error(`Unknown condition or expression type: ${string}`);
  }

  /**
   * returns the end index of the given directive/condition expression
   * @param position starting pos of the expression to analyze
   */
  getIndexEndOfRuleExpression(position: number): number {
    const expression = this.characters.slice(position);
    let index;

    if (
      directiveTypes.indexOf(
        expression.slice(0, expression.indexOf(" ")).join(""),
      ) > -1
    )
      index = expression.indexOf(" ");
    else index = expression.indexOf(":");

    return index;
  }
}
