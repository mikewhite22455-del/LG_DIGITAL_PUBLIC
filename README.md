# LG Digital (Android)

Native Android trading desk, quantitative strategy lab, cross-exchange spot arbitrage workspace, and AI trading copilot built with Kotlin, Jetpack Compose, Material 3, Room, and Firebase AI Logic (`gemini-3.8-flash`).

## Android build setup

Use JDK 21 and Gradle 9.3.1. Register the Android application ID `com.aistudio.lgdigital.kxrmqp` with Firebase and place its `google-services.json` in `app/`. For GitHub Actions, set the repository secret `GOOGLE_SERVICES_JSON` to the complete contents of that file. The debug build uses Android's default debug keystore.

### Sign the Play Store bundle

For a first Google Play upload, create an upload key on your own Windows computer. Install JDK 21, open PowerShell, and run:

```powershell
keytool -genkeypair -keystore "$HOME\lg-digital-upload.jks" -storetype PKCS12 -alias upload -keyalg RSA -keysize 3072 -validity 10000
```

Follow the prompts and use a strong password. Keep the keystore and password backed up securely outside the repository; you will need the same upload key for future updates. Do not send the keystore or passwords in chat. If an app has already been uploaded, use its existing upload key instead of creating a new one.

In the repository's **Settings → Secrets and variables → Actions**, create or update these repository secrets:

- `KEYSTORE_BASE64`: the base64-encoded contents of the keystore file. Copy it to the clipboard with `Set-Clipboard -Value ([Convert]::ToBase64String([IO.File]::ReadAllBytes("$HOME\lg-digital-upload.jks")))`, paste it into the secret field, and then clear the clipboard with `Set-Clipboard -Value ""`.
- `KEYSTORE_PASSWORD`: the password used to create the keystore.
- `KEY_ALIAS`: `upload`.
- `KEY_PASSWORD`: the same password as `KEYSTORE_PASSWORD` for the PKCS12 keystore above.

Run **Build Android APK and Play Store AAB** from the repository's Actions tab. When the run succeeds, download the `LG-Digital-PlayStore-AAB` artifact and extract the `.aab` file from the ZIP. A missing or invalid signing secret fails the build rather than producing an unsigned bundle.

## Features
- **Live Markets**: CoinGecko & KuCoin spot tickers, market breadth pulse, top movers, searchable coin catalog with quick-action sheets, and CoinDesk RSS market news.
- **Trade & Bots**: Interactive Compose `Canvas` OHLCV candlestick chart (`15m`, `1h`, `4h`, `1d`) with SMA, EMA, Bollinger Bands, and Supertrend overlays; 11 deterministic technical strategies; paper order ticket; chronological 70/30 backtester; and Adaptive Autopilot guardrails (risk profiles, stop-schedule windows, drawdown locks, higher-timeframe filter).
- **LG Arbitrage Workspace**: `BigDecimal` order-book depth walking across Demo Synthetic and Public Live Exchange books (Kraken, Coinbase, Bitstamp, Crypto.com), worst-case net USD/bps calculation (taker fees, slippage buffer, rebalance reserve), per-venue paper wallets, and execution journal.
- **Quant Labs**: 20-bar Breakout Screener, DEX Screener search, 24h Coin Research Projections, CoinSpectrum Mining Quotes (CC BY 4.0), Sports Analytics calculators (Two-Way Arbitrage, De-Vig, Fractional Kelly, Poisson Match Simulator), Daily Compound Staking APY & 50/50 Impermanent Loss simulators, and Position Risk Sizer.
- **AI Quant Copilot & Vault**: Streaming multi-turn trading advisor powered by Firebase AI Logic (`gemini-3.8-flash`) with App Check, local Room SQLite persistence for paper holdings, audit logs, research watchlist, and watch-only cold wallet tracking.
