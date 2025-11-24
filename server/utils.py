
import asyncio
from sqlmodel import Session, select
from database import get_db_session
from models import User, StockPrice, StockHistoricalData, StockHistoricalData_weekly, PriceAlert
import datetime
from firebase_admin import messaging
from sqlalchemy import and_

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

def clear_stock_history() -> None:
    """Clear all existing stock historical data"""
    with get_db_session() as db:
        statement = select(StockHistoricalData)
        records = db.exec(statement).all()
        for record in records:
            db.delete(record)
    print("Cleared existing stock historical data")

def setup_default_stock_data() -> None:
    tickers = ["AAPL", "TSLA", "VOO", "3115.HK"]
    with get_db_session() as db:
        for ticker in tickers:
            data = fetch_1y_stock_data(ticker)
            process_1y_data(ticker, data, db)

            data_weekly = fetch_1y_stock_data_weekly(ticker)
            process_1y_data_weekly(ticker, data_weekly, db)

            # data_last_trading_day = fetch_stock_data_last(ticker)
            # process_last(ticker, data_last_trading_day, db)

    print("Default stock historical data added")

def fetch_1y_stock_data_weekly(ticker: str) -> pd.DataFrame:
    data: pd.DataFrame = yf.download(
        tickers=ticker,
        period="1y",
        interval="1wk",
        auto_adjust=True,
        multi_level_index=False
    ) # type: ignore
    return data

# def fetch_stock_data_last(ticker: str) -> pd.DataFrame:
#     data: pd.DataFrame = yf.download(
#         tickers=ticker,
#         period="1y",
#         interval="1d",
#         auto_adjust=True,
#         multi_level_index=False
#     ) # type: ignore
#     return data

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

def process_1y_data_weekly(ticker: str, data: pd.DataFrame, db: Session) -> None:
    for index, row in data.iterrows():
        stock_record = StockHistoricalData_weekly(
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
    try:
        data: pd.DataFrame = yf.download(
            tickers=ticker,
            period=period,
            interval=interval,
            auto_adjust=True,
            multi_level_index=False
        ) # type: ignore

        if data.empty:
            print(f"[{ticker}] No data from yfinance")
            return None
        
        stock_record = preprocess_data(ticker=ticker, data=data)
        
        db.add(stock_record)
        db.commit()

        return stock_record
    
    except Exception as e:
        print(f"[{ticker}] yfinance failed: {e}")
        db.rollback()
        return None

    
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
            sleep_time = max(0, 70 - elapsed)
            await asyncio.sleep(sleep_time)

# def create_data_fetcher(tickers: list[str]):
#     return asyncio.create_task(fetch_and_store_stock_data(tickers=tickers))

def create_data_fetcher(tickers: list[str]):
    return asyncio.create_task(stock_price_watcher(tickers=tickers))

# Async FCM sender (fire and forget, never blocks)
async def send_notification_async(token: str, title: str, body: str):
    loop = asyncio.get_running_loop()
    await loop.run_in_executor(None, send_fcm_notification, token, title, body)
    
def send_fcm_notification(token: str, title: str, body: str):
    message = messaging.Message(
        notification=messaging.Notification(title=title, body=body),
        token=token,
    )
    try:
        response = messaging.send(message)
        print("Push sent successfully:", response)
    except Exception as e:
        print("Failed to send push:", e)

# The full background task (fetch → store → check alerts)
async def stock_price_watcher(tickers: list[str]):
    while True:
        start_time = asyncio.get_event_loop().time()

        for ticker in tickers:
            db = next(get_db_session())  # fresh session per ticker
            try:
                # 1. Fetch + store new price (blocking → run in thread)
                loop = asyncio.get_event_loop()
                result = await loop.run_in_executor(
                    None, process_ticker, ticker, db, "1m", "1d"
                )

                if not result:
                    continue

                current_price = result.price  # ← this is the latest price you just saved

                # 2. IMMEDIATELY AFTER saving → check alerts for this ticker
                alerts = db.exec(
                    select(PriceAlert).where(
                        and_(
                            PriceAlert.symbol == ticker.upper(),
                            PriceAlert.is_active == True,
                            PriceAlert.notified == False
                        )
                    )
                ).all()

                for alert in alerts:
                    triggered = False

                    if alert.condition in ("above", "rises_above") and current_price > alert.target_price:
                        triggered = True
                    elif alert.condition in ("below", "drops_below") and current_price < alert.target_price:
                        triggered = True

                    if triggered:
                        user = db.exec(select(User).where(User.id == alert.user_id)).first()
                        if user and user.fcm_token:
                            title = f"{ticker} Alert Triggered!"
                            direction = "above" if "above" in alert.condition else "below"
                            body = f"{ticker} is now ${current_price:.2f} ({direction} ${alert.target_price})"

                            # Fire and forget (non-blocking)
                            asyncio.create_task(
                                send_notification_async(user.fcm_token, title, body)
                            )

                        # Mark as done
                        alert.notified = True
                        alert.is_active = False
                        alert.triggered_at = datetime.utcnow()
                        alert.triggered_price = current_price
                        db.add(alert)

                db.commit()  # commit price + alert updates together

            except Exception as e:
                print(f"Error processing {ticker}: {e}")
                db.rollback()
            finally:
                db.close()

        # Sleep until next minute
        elapsed = asyncio.get_event_loop().time() - start_time
        await asyncio.sleep(max(0, 60 - elapsed))

def insert_stock_data():
    """Download and insert latest stock data as new records"""
    tickers = ["AAPL", "TSLA", "VOO", "3115.HK"]
    
    with get_db_session() as db:
        for ticker in tickers:
            process_ticker(ticker=ticker, db=db, period="5d", interval="1m")

def setup_database():
    """Setup both default user and stock data"""
    setup_default_user()
    clear_stock_history()
    setup_default_stock_data()
    insert_stock_data()

if __name__ == "__main__":
    setup_database()