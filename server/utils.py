
import asyncio
from sqlmodel import Session, select
from database import get_db_session
from models import User, StockPrice, StockHistoricalData
import datetime

import yfinance as yf
import pandas as pd

def setup_default_user() -> None:
    token = "1234"
    with get_db_session() as db:
        existing = db.exec(select(User).where(User.token == token)).first()
        if not existing:
            db.add(User(token=token))
            print("Test user added")
            return
        print("Test user exists")

def setup_default_stock_data() -> None:
    tickers = ['0700.HK', 'TSLA', '^HSI', '^GSPC']
    with get_db_session() as db:
        for ticker in tickers:
            data = fetch_1y_stock_data(ticker)
            process_1y_data(ticker, data, db)
    print("Default stock historical data added")

def fetch_1y_stock_data(ticker: str) -> pd.DataFrame:
    data: pd.DataFrame = yf.download(
        tickers=ticker,
        period="1y",
        interval="1d",
        auto_adjust=True,
        multi_level_index=False
    ) # type: ignore
    return data

def process_1y_data(ticker: str, data: pd.DataFrame, db: Session) -> None:
    for index, row in data.iterrows():
        stock_record = StockHistoricalData(
            symbol=ticker,
            date=index.to_pydatetime(),
            open_price=round(float(row['Open']), 2),
            high_price=round(float(row['High']), 2),
            low_price=round(float(row['Low']), 2),
            close_price=round(float(row['Close']), 2),
            volume=int(row['Volume']) if pd.notna(row['Volume']) else 0
        )
        db.add(stock_record)
        
def preprocess_data(ticker: str, data: pd.DataFrame) -> StockPrice:
    latest = data.iloc[-1]
    current_price = float(latest['Close'])

    open_price = float(latest['Open'])
    high_price = float(latest['High'])
    low_price = float(latest['Low'])
    close_price = float(latest['Close'])
    
    if len(data) >= 2:
        prev_close = float(data.iloc[-2]['Close'])
        change = current_price - prev_close
        change_percent = (change / prev_close) * 100
    else:
        change = 0.0
        change_percent = 0.0
        
    volume = int(latest['Volume']) if pd.notna(latest['Volume']) else 0
    
    
    stock_record = StockPrice(
        symbol=ticker,
        price=round(current_price, 2),
        change=round(change, 2),
        change_percent=round(change_percent, 2),
        volume=volume,
        timestamp=datetime.datetime.now(datetime.timezone.utc),
        open_price=round(open_price, 2),
        high_price=round(high_price, 2),
        low_price=round(low_price, 2),
        close_price=round(close_price, 2)
    )
    
    return stock_record
    

def process_ticker(ticker: str, db: Session, interval: str, period: str) -> None:
    data: pd.DataFrame = yf.download(
        tickers=ticker,
        period=period,
        interval=interval,
        auto_adjust=True,
        multi_level_index=False
    ) # type: ignore
    
    stock_record = preprocess_data(ticker=ticker, data=data)
    
    db.add(stock_record)
    
    
async def fetch_and_store_stock_data(tickers: list[str]):
    """Fetch latest 1-min data for all tickers and store in DB."""
    with get_db_session() as db:
        while True:
            start = asyncio.get_running_loop().time()

            for ticker in tickers:
                # Run blocking yfinance call in thread pool
                loop = asyncio.get_running_loop()
                await loop.run_in_executor(
                    None,
                    process_ticker,
                    ticker,
                    db,
                    "1m",
                    "1d",
                    
                ) # type: ignore
            


            # Sleep for remainder of interval
            elapsed = asyncio.get_running_loop().time() - start
            sleep_time = max(0, 60 - elapsed)
            await asyncio.sleep(sleep_time)

def create_data_fetcher(tickers: list[str]):
    return asyncio.create_task(fetch_and_store_stock_data(tickers=tickers))
    
def insert_stock_data():
    """Download and insert latest stock data as new records"""
    tickers = ['0700.HK', 'TSLA', '^HSI', '^GSPC']
    
    with get_db_session() as db:
        for ticker in tickers:
            process_ticker(ticker=ticker, db=db, period="5d", interval="1m")

def setup_database():
    """Setup both default user and stock data"""
    setup_default_user()
    setup_default_stock_data()
    insert_stock_data()

if __name__ == "__main__":
    setup_database()