import fs from 'fs';
import path from 'path'
import { createRequire } from 'node:module';
import { nodeResolve } from '@rollup/plugin-node-resolve';

const OUT_DIR = 'app/src/main/assets';

/**
 * Inspired by https://lihautan.com/12-line-rollup-plugin/
 * @param {(string|{ path: string, replace: [string|RegExp, string][]})[]} files 
 * @returns 
 */
const copyFilesPlugin = function(files) {
  return {
    name: 'copy-files',

    load() {
      for (const file of files) {
        const filepath = typeof file === 'string' ? file : file.path;
        if (filepath.startsWith('.')) {
          this.addWatchFile(path.resolve(filepath));
        }
      }
    },

    generateBundle() {
      const require = createRequire(import.meta.url);
      fs.mkdirSync(OUT_DIR, { recursive: true });
      for (const file of files) {
        // Use the nodejs resolver to get a file path. Yarn PnP will work if
        // rollup is invoked via yarn, e.g. `yarn rollup -c`.
        const filepath = typeof file === 'object' ? file.path : file;
        const src = require.resolve(filepath);
        const srcFilename = path.basename(src);
        const dst = path.join(OUT_DIR, srcFilename);

        fs.copyFileSync(src, dst);
      }
    }
  };
}

export default [{
  input: "web/src/app-main.js",
  output: [
    {
      file: `${OUT_DIR}/app-main.js`,
      format: "iife",
      sourcemap: true
    },
  ],
  plugins: [
    copyFilesPlugin([
      './web/src/index.html',
    ]),
    nodeResolve()
  ]
}];
