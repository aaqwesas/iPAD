from typing import List, AsyncGenerator
import datetime
import uuid
from contextlib import asynccontextmanager

from sqlmodel import select, distinct
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from response_type import TokenResponse, TokenVerify, Stock, StockCreate, StockHistorical
from database import get_db_session, create_tables
from models import User, StockPrice, StockHistoricalData, StockHistoricalData_weekly, PriceAlert
from utils import create_data_fetcher, setup_database, send_fcm_notification

import firebase_admin
from firebase_admin import credentials

# Initialize Firebase Admin SDK (only once)
if not firebase_admin._apps:
    cred = credentials.Certificate("firebase-adminsdk.json")
    firebase_admin.initialize_app(cred)

# Create tables
create_tables()

@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    TICKERS = ["AAPL", "TSLA", "VOO", "3115.HK"]
    

    data_fetcher = create_data_fetcher(tickers=TICKERS)
    
    try:
        yield
    finally:
        data_fetcher.cancel()

def create_app() -> FastAPI:

    app = FastAPI(title="Stock API", version="1.0.0", lifespan=lifespan)
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    
    return app

app = create_app()

@app.get("/test-push")
async def test_push():
    # ←←← REPLACE THIS WITH A REAL TOKEN FROM YOUR PHONE LOGS ←←←
    test_token = "dp9Xoj_5RH29NBdELTJoTj:APA91bGSVH_hRb3t2OSNeXXk__KxNJoauX3xpAv3EJOYkvl8V3KmC8x_yprjEgigE0VDDIZCejme0ef1wcVjMEWT5QVKbzFesHw1n3jO5KQvY3hnK8aOBZE"
    
    send_fcm_notification(
        token=test_token,
        title="IT WORKS!",
        body="Your price alert system is 100% ready!"
    )
    return {"status": "sent"}



def generate_token() -> str:
    """Generate a random token"""
    token = uuid.uuid4()
    return str(token)


@app.get("/api/symbols", response_model=List[str])
async def get_all_symbols():
    """
    Get just the list of unique stock symbols.
    """
    with get_db_session() as db:
        stmt = select(distinct(StockPrice.symbol)).order_by(StockPrice.symbol)
        symbols = db.exec(stmt).all()
        return symbols

@app.post("/api/generate-token", response_model=TokenResponse)
def generate_token_endpoint():
    """Generate a new user token"""
    token = generate_token()

    with get_db_session() as db:
        statement = select(User).where(User.token == token)
        existing_user = db.exec(statement).first()
        if existing_user:
            token = generate_token()
            statement = select(User).where(User.token == token)
            existing_user = db.exec(statement).first()


        db_user = User(token=token)
        db.add(db_user)
        
    return {"token": token, "message": "Token generated successfully"}


@app.post("/api/verify-token")
def verify_token_endpoint(token_data: TokenVerify):
    """Verify if a token exists in the database"""
    with get_db_session() as db:
        statement = select(User).where(User.token == token_data.token)
        user = db.exec(statement).first()
    if user:
        return {"valid": True, "message": "Token is valid"}
    else:
        return {"valid": False, "message": "Invalid token"}
    
    

@app.get("/api/stocks", response_model=List[Stock])
def get_stocks():
    """Get all stock prices (latest per symbol)"""
    with get_db_session() as db:
        statement = select(StockPrice).order_by(StockPrice.timestamp.desc())
        stocks = db.exec(statement).all()

        unique_stocks = {}
        for stock in stocks:
            if stock.symbol not in unique_stocks:
                unique_stocks[stock.symbol] = Stock.model_validate(stock)
        return list(unique_stocks.values())

@app.get("/api/stocks/history/{symbol}", response_model=List[StockHistorical])
def get_stock_history_daily(symbol: str):
    """Get historical stock data for a specific symbol"""
    with get_db_session() as db:
        statement = (
            select(StockHistoricalData)
            .where(StockHistoricalData.symbol == symbol.upper())
            .order_by(StockHistoricalData.date.desc())
        )
        stocks = db.exec(statement).all()
        if not stocks:
            raise HTTPException(status_code=404, detail="Stock not found")
        return [StockHistorical.model_validate(stock) for stock in stocks]
    
@app.get("/api/stocks/history/weekly/{symbol}", response_model=List[StockHistorical])
def get_stock_history_weekly(symbol: str):
    """Get historical weekly stock data for a specific symbol"""
    with get_db_session() as db:
        statement = (
            select(StockHistoricalData_weekly)
            .where(StockHistoricalData_weekly.symbol == symbol.upper())
            .order_by(StockHistoricalData_weekly.date.desc())
        )
        stocks = db.exec(statement).all()
        if not stocks:
            raise HTTPException(status_code=404, detail="Stock not found")
        return [StockHistoricalData_weekly.model_validate(stock) for stock in stocks]

@app.get("/api/stocks/{symbol}", response_model=Stock)
def get_stock(symbol: str):
    """Get specific stock price (latest)"""
    with get_db_session() as db:
        statement = (
            select(StockPrice)
            .where(StockPrice.symbol == symbol.upper())
        )
        stock = db.exec(statement).first()
        if not stock:
            raise HTTPException(status_code=404, detail="Stock not found")
        return Stock.model_validate(stock)

@app.post("/api/stocks", response_model=Stock)
def create_stock(stock: StockCreate):
    """Create new stock price entry"""
    with get_db_session() as db:
        db_stock = StockPrice(
            symbol=stock.symbol.upper(),
            price=stock.price,
            change=stock.change,
            change_percent=stock.change_percent,
            volume=stock.volume,
            timestamp=stock.timestamp
        )
        db.add(db_stock)
        db.refresh(db_stock)
        return Stock.model_validate(db_stock)
        
        

@app.get("/api/health")
def health_check():
    return {"status": "healthy", "timestamp": datetime.datetime.now()}


@app.post("/api/set-fcm-token")
async def set_fcm_token(token: str, user_token: str):
    with get_db_session() as db:
        user = db.exec(select(User).where(User.token == user_token)).first()
        if not user:
            raise HTTPException(404, "User not found")
        user.fcm_token = token
        db.add(user)
        db.commit()
    return {"status": "fcm token saved"}

@app.post("/api/alerts")
async def create_alert(alert: dict):  # or use Pydantic model
    # validate user_token, etc.
    with get_db_session() as db:
        db_alert = PriceAlert(**alert, user_id=User.id)
        db.add(db_alert)
        db.commit()
    return {"status": "alert created"}

if __name__ == "__main__":
    import uvicorn
    setup_database()
    uvicorn.run(app, host="0.0.0.0", port=8000)