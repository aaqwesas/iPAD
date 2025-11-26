import yfinance as yf



data = yf.download(tickers="TSLA", interval="1d", period="2d", multi_level_index=False)


print(data)