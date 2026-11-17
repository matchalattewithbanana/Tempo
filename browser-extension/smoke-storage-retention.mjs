// Regression policy for offline playback retention. This test is kept pure so
// CI can check the safety rule without an IndexedDB or Chrome API mock.
import * as esbuild from 'esbuild';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';

const dir = mkdtempSync(join(tmpdir(), 'tempo-retention-'));
try {
  const output = join(dir, 'storage.mjs');
  await esbuild.build({
    entryPoints: ['src/background/storage.ts'],
    bundle: true, format: 'esm', platform: 'node',
    target: 'node18', outfile: output, logLevel: 'silent',
  });
  const { isAutoPrunablePlay } = await import(pathToFileURL(output).href);
  if (isAutoPrunablePlay('queued')) throw Error('queued play would be deleted');
  if (isAutoPrunablePlay('failed')) throw Error('retryable failed play would be deleted');
  if (!isAutoPrunablePlay('synced')) throw Error('synced play could not be pruned');
  console.log('Play retention policy: queued and failed preserved; synced may be pruned.');
} finally {
  rmSync(dir, { recursive: true, force: true });
}
