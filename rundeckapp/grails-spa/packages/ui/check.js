const Path = require('path')
const checker = require('license-checker');

const packageJson = require('./package.json');

const dependencies = packageJson.dependencies;
const devDependencies = packageJson.devDependencies;

checker.init({
      start: '.',
}, function (err, packages) {
      const output = {
              dependencies: {},
              devDependencies: {},
            };

      if (err) {
              console.error(err);
            } else {
                    Object.keys(packages).forEach((pkg) => {
                          const pkgName = pkg.replace(/@[^@]+$/, '');
                          const [_,version] = pkg.match(/@([^@]+)/)
                          if (dependencies[pkgName]) {
                                      output.dependencies[pkgName] = packages[pkg];
                                      output.dependencies[pkgName].version = version
                                    }
                          if (devDependencies[pkgName]) {
                                      output.devDependencies[pkgName] = packages[pkg];
                                      output.devDependencies[pkgName].version = version
                                    }
                        });

                    console.log("Prod:")

                    for (let [k, v] of Object.entries(output.dependencies)) {
                        console.log(`${k},yes,no,${v.version},${v.licenses},${v.repository}/blob/master/LICENSE`)
                    }

                    console.log("\n\nDev:")

                    for (let [k, v] of Object.entries(output.devDependencies)) {
                        console.log(`${k},no,yes,${v.version},${v.licenses},${v.repository}/blob/master/LICENSE`)
                    }
                  }
});
