# LG Digital (Android)

Native Android trading desk, quantitative strategy lab, cross-exchange spot arbitrage workspace, and AI trading copilot built with Kotlin, Jetpack Compose, Material 3, Room, and Firebase AI Logic (`gemini-3.8-flash`).

## Features
- **Live Markets**: CoinGecko & KuCoin spot tickers, market breadth pulse, top movers, searchable coin catalog with quick-action sheets, and CoinDesk RSS market news.
- **Trade & Bots**: Interactive Compose `Canvas` OHLCV candlestick chart (`15m`, `1h`, `4h`, `1d`) with SMA, EMA, Bollinger Bands, and Supertrend overlays; 11 deterministic technical strategies; paper order ticket; chronological 70/30 backtester; and Adaptive Autopilot guardrails (risk profiles, stop-schedule windows, drawdown locks, higher-timeframe filter).
- **LG Arbitrage Workspace**: `BigDecimal` order-book depth walking across Demo Synthetic and Public Live Exchange books (Kraken, Coinbase, Bitstamp, Crypto.com), worst-case net USD/bps calculation (taker fees, slippage buffer, rebalance reserve), per-venue paper wallets, and execution journal.
- **Quant Labs**: 20-bar Breakout Screener, DEX Screener search, 24h Coin Research Projections, CoinSpectrum Mining Quotes (CC BY 4.0), Sports Analytics calculators (Two-Way Arbitrage, De-Vig, Fractional Kelly, Poisson Match Simulator), Daily Compound Staking APY & 50/50 Impermanent Loss simulators, and Position Risk Sizer.
- **AI Quant Copilot & Vault**: Streaming multi-turn trading advisor powered by Firebase AI Logic (`gemini-3.8-flash`) with App Check, local Room SQLite persistence for paper holdings, audit logs, research watchlist, and watch-only cold wallet tracking.
