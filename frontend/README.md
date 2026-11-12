# EarningsLens frontend

Angular 22 app (standalone components, Angular Material). Versions are in `../docs/VERSIONS.md`.

From this folder:

| Command | What it does |
|---|---|
| `npm ci` | Install the exact packages from `package-lock.json` |
| `npm start` | Dev server at http://localhost:4200 (reloads on save) |
| `npm run build` | Production build into `dist/frontend` |
| `npm test` | Unit tests once (Vitest, headless) |

The backend allows CORS from `http://localhost:4200` in local runs.
