import http from "node:http";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL(".", import.meta.url));
const files = {
  "/": ["index.html", "text/html; charset=utf-8"],
  "/app.js": ["app.js", "text/javascript; charset=utf-8"],
  "/trading.mjs": ["trading.mjs", "text/javascript; charset=utf-8"],
  "/styles.css": ["styles.css", "text/css; charset=utf-8"],
};
const marketUrl = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin,ethereum,solana,dogecoin,cardano,xrp&order=market_cap_desc&per_page=6&page=1&sparkline=false";

export function createApp({ fetchMarket = fetch, now = Date.now } = {}) {
  let cached;
  let pending;
  async function markets() {
    if (cached && now() - cached.fetchedAt < 30_000) return cached;
    if (!pending) {
      pending = (async () => {
        const response = await fetchMarket(marketUrl, {
          headers: { accept: "application/json" },
          signal: AbortSignal.timeout(10_000),
        });
        if (!response.ok) throw new Error(`Market provider returned HTTP ${response.status}`);
        const data = await response.json();
        if (!Array.isArray(data)) throw new Error("Market provider returned invalid data");
        const coins = data.filter(coin =>
          typeof coin.id === "string" && typeof coin.name === "string" &&
          typeof coin.symbol === "string" && Number.isFinite(coin.current_price) &&
          coin.current_price > 0
        ).map(coin => ({
          id: coin.id,
          name: coin.name,
          symbol: coin.symbol.toUpperCase(),
          price: coin.current_price,
          change: Number.isFinite(coin.price_change_percentage_24h) ? coin.price_change_percentage_24h : null,
        }));
        if (!coins.length) throw new Error("Market provider returned no valid prices");
        cached = { coins, fetchedAt: now() };
        return cached;
      })().finally(() => { pending = undefined; });
    }
    return pending;
  }

  return http.createServer(async (req, res) => {
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("Content-Security-Policy", "default-src 'self'; connect-src 'self'; style-src 'self'; script-src 'self'; img-src 'self'; base-uri 'none'; frame-ancestors 'none'");
    if (req.method !== "GET") {
      res.writeHead(405, { Allow: "GET" }).end();
      return;
    }
    const path = new URL(req.url, "http://localhost").pathname;
    if (path === "/api/markets") {
      try {
        const result = await markets();
        res.writeHead(200, { "Content-Type": "application/json", "Cache-Control": "no-store" });
        res.end(JSON.stringify(result));
      } catch (error) {
        console.error("Market data request failed:", error.message);
        res.writeHead(502, { "Content-Type": "application/json", "Cache-Control": "no-store" });
        res.end(JSON.stringify({ error: "Live prices are unavailable. Try again shortly." }));
      }
      return;
    }
    const file = files[path];
    if (!file) {
      res.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" }).end("Not found");
      return;
    }
    try {
      const contents = await readFile(new URL(file[0], `file://${root}`));
      res.writeHead(200, { "Content-Type": file[1], "Cache-Control": "no-cache" }).end(contents);
    } catch (error) {
      console.error("Static file read failed:", error.message);
      res.writeHead(500).end("Server error");
    }
  });
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const port = Number(process.env.PORT || 3000);
  createApp().listen(port, "0.0.0.0", () => console.log(`LG Digital web listening on ${port}`));
}
