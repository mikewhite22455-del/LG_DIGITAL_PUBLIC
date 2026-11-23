import test from "node:test";
import assert from "node:assert/strict";
import { initialAccount, trade, validateAccount } from "./trading.mjs";

const bitcoin = { id: "bitcoin", symbol: "BTC", price: 50_000 };

test("paper buys and sells update cash and holdings without mutating the old account", () => {
  const start = initialAccount();
  const bought = trade(start, bitcoin, "BUY", "100.00");
  assert.equal(start.cash, 100_000);
  assert.equal(bought.cash, 99_900);
  assert.equal(bought.holdings.bitcoin, 200_000);
  const sold = trade(bought, bitcoin, "SELL", "50");
  assert.equal(sold.cash, 99_950);
  assert.equal(sold.holdings.bitcoin, 100_000);
  assert.deepEqual(sold.orders.map(order => order.side), ["SELL", "BUY"]);
  validateAccount(JSON.parse(JSON.stringify(sold)));
});

test("rejects invalid or unaffordable orders and malformed accounts", () => {
  assert.throws(() => trade(initialAccount(), bitcoin, "SELL", "1"), /Not enough/);
  assert.throws(() => trade(initialAccount(), bitcoin, "BUY", "100001"), /Not enough/);
  assert.throws(() => trade(initialAccount(), bitcoin, "BUY", "1.001"), /two decimal/);
  assert.throws(() => trade(initialAccount(), { ...bitcoin, price: NaN }, "BUY", "1"), /valid live price/);
  assert.throws(() => validateAccount({ cash: -1, holdings: {}, orders: [] }), /invalid/);
});
