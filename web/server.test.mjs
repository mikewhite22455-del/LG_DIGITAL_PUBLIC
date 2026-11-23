import test from "node:test";
import assert from "node:assert/strict";
import { createApp } from "./server.mjs";

async function usingServer(app, run) {
  await new Promise(resolve => app.listen(0, "127.0.0.1", resolve));
  try {
    await run(`http://127.0.0.1:${app.address().port}`);
  } finally {
    await new Promise(resolve => app.close(resolve));
  }
}

test("market data is filtered, cached, and served alongside the site", async () => {
  let calls = 0;
  let clock = 1_000;
  const app = createApp({
    now: () => clock,
    fetchMarket: async () => {
      calls++;
      return { ok: true, json: async () => [
        { id: "bitcoin", name: "Bitcoin", symbol: "btc", current_price: 50_000, price_change_percentage_24h: 2 },
        { id: "bad", name: "Bad", symbol: "bad", current_price: null },
      ] };
    },
  });
  await usingServer(app, async base => {
    assert.equal((await fetch(base)).status, 200);
    const first = await (await fetch(`${base}/api/markets`)).json();
    assert.deepEqual(first.coins, [{ id: "bitcoin", name: "Bitcoin", symbol: "BTC", price: 50_000, change: 2 }]);
    await fetch(`${base}/api/markets`);
    assert.equal(calls, 1);
    clock += 31_000;
    await fetch(`${base}/api/markets`);
    assert.equal(calls, 2);
    assert.equal((await fetch(`${base}/missing`)).status, 404);
  });
});

test("market provider failures are reported rather than returning fake prices", async () => {
  const app = createApp({ fetchMarket: async () => ({ ok: false, status: 429 }) });
  await usingServer(app, async base => {
    const response = await fetch(`${base}/api/markets`);
    assert.equal(response.status, 502);
    assert.match((await response.json()).error, /unavailable/);
  });
});
