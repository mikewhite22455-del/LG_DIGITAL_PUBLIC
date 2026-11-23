# LG Digital Owner Desk

Private-first browser paper-trading edition. It shows live market prices and simulates orders locally in the browser; it never connects to exchanges or places real orders.

Run locally with Node.js 20 or newer:

```sh
cd web
npm start
```

Run the focused test suite with `npm test` from `web/`. Each browser has its own paper account, and clearing browser data removes that account.

The included `render.yaml` uses Render's private-service type so this edition is not publicly reachable by default. When it is ready for public release, change `type: pserv` to `type: web` and deploy the Blueprint. No exchange credentials are required.
