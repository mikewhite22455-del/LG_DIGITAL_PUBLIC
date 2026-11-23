import { initialAccount, trade, validateAccount } from "./trading.mjs";

const $ = id => document.getElementById(id);
const storageKey = "lg-digital-paper-account-v1";
const money = value => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: value < 1 ? 6 : 2 }).format(value);
let account;
let coins = [];
let fetchedAt = 0;
let marketAvailable = false;
let busy = false;

function save(next) {
  localStorage.setItem(storageKey, JSON.stringify(next));
  account = next;
}

function loadAccount() {
  try {
    const saved = localStorage.getItem(storageKey);
    account = saved === null ? initialAccount() : validateAccount(JSON.parse(saved));
  } catch {
    account = null;
    $("trade-message").textContent = "Could not read the paper account. Check browser storage or reset the account.";
  }
}

function cell(row, text, className) {
  const td = document.createElement("td");
  td.textContent = text;
  if (className) td.className = className;
  row.append(td);
}

function render() {
  const fresh = marketAvailable && Date.now() - fetchedAt < 90_000;
  $("submit").disabled = !fresh || !account;
  $("coin").disabled = !fresh;
  $("status").textContent = fresh ? "LIVE" : "UNAVAILABLE";
  $("status").className = fresh ? "positive" : "negative";
  $("updated").textContent = fetchedAt ? `Updated ${new Date(fetchedAt).toLocaleTimeString()}` : "Waiting for live prices";
  $("cash").textContent = account ? money(account.cash) : "—";
  const known = new Set(coins.map(coin => coin.id));
  const missing = account && Object.entries(account.holdings).some(([id, units]) => units > 0 && !known.has(id));
  $("portfolio").textContent = account && fresh && !missing
    ? money(account.cash + coins.reduce((sum, coin) => sum + (account.holdings[coin.id] || 0) / 100_000_000 * coin.price, 0))
    : "—";
  const tbody = $("market-list");
  tbody.replaceChildren();
  if (!coins.length) {
    const row = tbody.insertRow();
    const td = row.insertCell();
    td.colSpan = 4;
    td.textContent = "No prices available.";
  }
  for (const coin of coins) {
    const row = tbody.insertRow();
    cell(row, `${coin.name} (${coin.symbol})`, "asset");
    cell(row, money(coin.price));
    cell(row, coin.change === null ? "—" : `${coin.change >= 0 ? "+" : ""}${coin.change.toFixed(2)}%`, coin.change === null ? "" : coin.change >= 0 ? "positive" : "negative");
    cell(row, account ? `${((account.holdings[coin.id] || 0) / 100_000_000).toFixed(8)} ${coin.symbol}` : "—");
  }
  const orders = $("orders");
  orders.replaceChildren();
  if (!account || !account.orders.length) {
    const row = orders.insertRow();
    const td = row.insertCell();
    td.colSpan = 4;
    td.textContent = account ? "No paper orders yet." : "Paper account unavailable.";
  } else {
    for (const order of account.orders) {
      const row = orders.insertRow();
      cell(row, new Date(order.time).toLocaleString());
      cell(row, `${order.side} ${order.symbol}`, order.side === "BUY" ? "positive" : "negative");
      cell(row, money(order.amount));
      cell(row, money(order.price));
    }
  }
}

async function refresh() {
  if (busy) return;
  busy = true;
  $("refresh").disabled = true;
  try {
    const response = await fetch("/api/markets", { cache: "no-store", signal: AbortSignal.timeout(15_000) });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || "Live prices are unavailable.");
    if (!Array.isArray(data.coins) || !data.coins.length || !Number.isFinite(data.fetchedAt)) throw new Error("Invalid market response.");
    coins = data.coins;
    fetchedAt = data.fetchedAt;
    marketAvailable = true;
    const selected = $("coin").value;
    $("coin").replaceChildren(...coins.map(coin => new Option(`${coin.name} (${coin.symbol})`, coin.id)));
    if (coins.some(coin => coin.id === selected)) $("coin").value = selected;
    $("market-error").hidden = true;
  } catch (error) {
    marketAvailable = false;
    $("market-error").textContent = `${error.message || "Live prices are unavailable."} Paper orders are paused.`;
    $("market-error").hidden = false;
  } finally {
    busy = false;
    $("refresh").disabled = false;
    render();
  }
}

$("trade-form").addEventListener("submit", event => {
  event.preventDefault();
  try {
    if (!marketAvailable || Date.now() - fetchedAt >= 90_000) throw new Error("Prices are stale. Refresh before placing an order.");
    if (!account) throw new Error("Paper account is unavailable.");
    const coin = coins.find(item => item.id === $("coin").value);
    const next = trade(account, coin, $("side").value, $("amount").value);
    save(next);
    $("trade-message").textContent = `Paper ${$("side").value.toLowerCase()} completed at ${money(coin.price)}. No real order was placed.`;
    $("amount").value = "";
    render();
  } catch (error) {
    $("trade-message").textContent = error.message || "Could not save your paper order. Check browser storage.";
  }
});

$("reset").addEventListener("click", () => {
  if (!confirm("Reset your paper account to $100,000 and delete all paper holdings and orders in this browser?")) return;
  try {
    save(initialAccount());
    $("trade-message").textContent = "Paper account reset.";
  } catch {
    $("trade-message").textContent = "Could not reset the paper account. Check browser storage.";
  }
  render();
});
$("refresh").addEventListener("click", refresh);
loadAccount();
render();
refresh();
setInterval(refresh, 60_000);
setInterval(render, 10_000);
