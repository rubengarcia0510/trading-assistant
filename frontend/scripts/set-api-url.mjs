import { readFile, writeFile } from 'node:fs/promises';

const file = 'src/app/core/api-config.ts';
const apiUrl = process.env.TAI_API_URL?.trim();

if (!apiUrl) {
  console.log('TAI_API_URL not set; keeping localhost API URL.');
  process.exit(0);
}

if (!/^https?:\/\//.test(apiUrl)) {
  throw new Error('TAI_API_URL must start with http:// or https://');
}

const content = await readFile(file, 'utf8');
const updated = content.replace(
  /export const API_URL = '[^']*';/,
  `export const API_URL = '${apiUrl.replace(/'/g, "\\'")}';`
);

if (content === updated) {
  throw new Error('Could not update API_URL in api-config.ts');
}

await writeFile(file, updated, 'utf8');
console.log(`Configured API_URL=${apiUrl}`);
