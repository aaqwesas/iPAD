
from sqlmodel import Session, select
from database import get_db_session
from models import User, StockPrice
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

def process_ticker(ticker: str, db: Session) -> None:
    data: pd.DataFrame = yf.download(
        tickers=ticker,
        period="1d",
        interval="1m",
        auto_adjust=True,
        multi_level_index=False
    ) # type: ignore
    
    latest = data.iloc[-1]
    current_price = float(latest['Close'])
    
    
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
        timestamp=datetime.datetime.now(datetime.timezone.utc)
    )
    
    db.add(stock_record)
    
def insert_stock_data():
    """Download and insert latest stock data as new records"""
    tickers = ['0700.HK', 'TSLA', '^HSI', '^GSPC']
    
    with get_db_session() as db:
        for ticker in tickers:
            process_ticker(ticker=ticker, db=db)





def setup_database():
    """Setup both default user and stock data"""
    setup_default_user()
    insert_stock_data()

if __name__ == "__main__":
    setup_database()