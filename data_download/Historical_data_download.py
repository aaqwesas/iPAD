import yfinance as yf
import pandas as pd

# Define tickers (using Yahoo Finance symbols)
tickers = {
    '0700.HK': 'Tencent Holdings',
    'TSLA': 'Tesla Inc',
    '^HSI': 'Hang Seng Index',  # Standard symbol for HSI
    '^GSPC': 'S&P 500 Index'    # Standard symbol for SPX
}

# Download 1-year daily data for each
for symbol, name in tickers.items():
    data = yf.download(symbol, period="1y", interval="1d")
    filename = f"{symbol.replace('^', '')}_{name.replace(' ', '_').replace('.', '')}.csv"
    data.to_csv(filename)
    print(f"Downloaded {len(data)} rows for {name} ({symbol}) to {filename}")