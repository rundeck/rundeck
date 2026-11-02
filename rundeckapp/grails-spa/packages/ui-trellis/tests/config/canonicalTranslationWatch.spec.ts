import fs from "node:fs";
import path from "node:path";
import Watchpack = require("watchpack");
import appConfig = require("../../vue.config.app");

describe("canonical translation development watch", () => {
  it("configures and observes a mutation under the canonical source path", async () => {
    const canonicalPath = path.resolve(
      __dirname,
      "../../../../../../translations/core/ui/readme-motd.ts",
    );
    const configuredGlobs = appConfig.configureWebpack.devServer.watchFiles;
    expect(configuredGlobs).toContain(
      path.resolve(__dirname, "../../../../../../translations/**/*.ts"),
    );

    const original = fs.readFileSync(canonicalPath, "utf8");
    const watcher = new Watchpack({ aggregateTimeout: 10 });
    const observed = new Promise<void>((resolve, reject) => {
      const timeout = setTimeout(
        () =>
          reject(new Error("canonical translation mutation was not observed")),
        5000,
      );
      watcher.once("change", (changedPath: string) => {
        if (path.resolve(changedPath) === canonicalPath) {
          clearTimeout(timeout);
          resolve();
        }
      });
    });

    try {
      watcher.watch({ files: [canonicalPath], startTime: Date.now() });
      await new Promise((resolve) => setTimeout(resolve, 100));
      fs.appendFileSync(canonicalPath, "\n");
      await observed;
    } finally {
      watcher.close();
      fs.writeFileSync(canonicalPath, original);
    }
  });
});
