from typing import List, AsyncGenerator
import datetime
import uuid
from contextlib import asynccontextmanager

from sqlmodel import select
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from response_type import TokenResponse, TokenVerify, Stock, StockCreate, StockHistorical
from database import get_db_session, create_tables
from models import User, StockPrice, StockHistoricalData
from utils import create_data_fetcher, setup_database


# Create tables
create_tables()

@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    TICKERS = ["AAPL", "TSLA", "^GSPC", "0700.HK"]
    

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


def generate_token() -> str:
    """Generate a random token"""
    token = uuid.uuid4()
    return str(token)

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
def get_stock_history(symbol: str):
    """Get historical stock data for a specific symbol"""
    with get_db_session() as db:
        statement = (
            select(StockHistoricalData)
            .where(StockHistoricalData.symbol == symbol.upper())
            .order_by(StockHistoricalData.date.desc())
        )
        stocks = db.exec(statement).all()
        print(stocks)
        if not stocks:
            raise HTTPException(status_code=404, detail="Stock not found")
        return [StockHistorical.model_validate(stock) for stock in stocks]

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


if __name__ == "__main__":
    import uvicorn
    setup_database()
    uvicorn.run(app, host="0.0.0.0", port=8000)