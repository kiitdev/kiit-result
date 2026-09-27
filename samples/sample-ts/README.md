# sample-ts

Sample app for [`@kiitdev/result`](../../ports/kiit-result-ts) from real TypeScript, checked against the actual native port. Mirrors the scenarios in `samples/sample-kotlin`, plus a short async section.

## Run

Build the port once first (`npm install && npm run build` in `ports/kiit-result-ts`), since the sample imports its compiled output.

```bash
npm install
npm run typecheck
npm run build
npm run start
```
