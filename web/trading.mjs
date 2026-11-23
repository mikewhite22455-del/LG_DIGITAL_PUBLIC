export const INITIAL_CASH = 100_000;

export function initialAccount() {
  return { cash: INITIAL_CASH, holdings: {}, orders: [] };
}

export function validateAccount(account) {
  if (!account || !Number.isFinite(account.cash) || account.cash < 0 ||
      !account.holdings || typeof account.holdings !== "object" || Array.isArray(account.holdings) ||
      !Object.values(account.holdings).every(value => Number.isSafeInteger(value) && value >= 0) ||
      !Array.isArray(account.orders) || account.orders.length > 100 ||
      !account.orders.every(order => order && typeof order.symbol === "string" &&
        (order.side === "BUY" || order.side === "SELL") &&
        Number.isFinite(order.amount) && order.amount > 0 &&
        Number.isFinite(order.price) && order.price > 0 &&
        typeof order.time === "string" && !Number.isNaN(Date.parse(order.time)))) {
    throw new Error("Saved paper account is invalid. Reset it to continue.");
  }
  return account;
}

export function trade(account, coin, side, input) {
  validateAccount(account);
  const text = String(input).trim();
  if (!/^\d+(?:\.\d{1,2})?$/.test(text)) throw new Error("Enter a USD amount with up to two decimal places.");
  const amount = Number(text);
  if (!Number.isFinite(amount) || amount < 1 || amount > 1_000_000) throw new Error("Enter an amount between $1 and $1,000,000.");
  if (!coin || typeof coin.id !== "string" || typeof coin.symbol !== "string" ||
      !Number.isFinite(coin.price) || coin.price <= 0) throw new Error("A valid live price is required.");
  if (side !== "BUY" && side !== "SELL") throw new Error("Invalid order type.");
  const units = Math.floor((amount / coin.price) * 100_000_000);
  if (!Number.isSafeInteger(units) || units < 1) throw new Error("Amount is too small for this asset.");
  const available = account.holdings[coin.id] || 0;
  if (side === "BUY" && amount > account.cash) throw new Error("Not enough paper cash.");
  if (side === "SELL" && units > available) throw new Error("Not enough of this asset to sell.");
  const nextUnits = side === "BUY" ? available + units : available - units;
  if (!Number.isSafeInteger(nextUnits)) throw new Error("Position is too large.");
  return {
    cash: Math.round((account.cash + (side === "BUY" ? -amount : amount)) * 100) / 100,
    holdings: { ...account.holdings, [coin.id]: nextUnits },
    orders: [{ symbol: coin.symbol, side, amount, price: coin.price, time: new Date().toISOString() }, ...account.orders].slice(0, 100),
  };
}
