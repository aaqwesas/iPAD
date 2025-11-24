from typing import List, AsyncGenerator
import datetime
import uuid
from contextlib import asynccontextmanager

from sqlmodel import select, distinct
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from response_type import PortfolioResponse, PortfolioUpdate, RegisterResponse, TokenVerify, Stock, StockCreate, StockHistorical, RegisterRequest, FCMUpdate, CreateAlertRequest, UserHoldingResponse
from database import get_db_session, create_tables
from models import User, StockPrice, StockHistoricalData, StockHistoricalData_weekly, PriceAlert, UserHolding, UserPortfolio
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

@app.get("/users/{user_id}/portfolio", response_model=PortfolioResponse)
async def get_portfolio_value(user_id: int):
    with get_db_session() as db:
        portfolio = db.exec(
            select(UserPortfolio)
            .where(UserPortfolio.user_id == user_id)
        ).first()
        
        if not portfolio:
            # Return default value if no portfolio exists
            return PortfolioResponse(value=0.0)
        
        return PortfolioResponse(value=portfolio.value)

# 2. Update portfolio value
@app.put("/users/{user_id}/portfolio", response_model=PortfolioResponse)
async def update_portfolio_value(user_id: int, update: PortfolioUpdate):
    with get_db_session() as db:
        portfolio = db.exec(
            select(UserPortfolio)
            .where(UserPortfolio.user_id == user_id)
        ).first()
        
        if portfolio:
            # Update existing portfolio
            portfolio.value = update.value
        else:
            # Create new portfolio if doesn't exist
            portfolio = UserPortfolio(user_id=user_id, value=update.value)
            db.add(portfolio)
        
        db.commit()
        db.refresh(portfolio)
        return PortfolioResponse(value=portfolio.value)


@app.get("/users/{user_id}/holdings", response_model=List[UserHoldingResponse])
async def get_user_holdings(user_id: int):
    with get_db_session() as db:
        # Query all holdings for this user
        holdings = db.exec(
            select(UserHolding.stock_ticker, UserHolding.quantity)
            .where(UserHolding.user_id == user_id)
        ).all()
        
        if not holdings:
            return []  
        
        # Convert to response model
        return [UserHoldingResponse(stock_ticker=ticker, quantity=quantity) 
                for ticker, quantity in holdings]


@app.post("/api/generate-token", response_model=RegisterResponse)
def register(data: RegisterRequest):
    email = data.email.strip().lower()
    
    with get_db_session() as db:
        # Check if user exists by "token" (which is email)
        user = db.exec(select(User).where(User.token == email)).first()
        if not user:
            user = User(token=email)  # ← token = email
            db.add(user)
            db.commit()
            db.refresh(user)
            return {"message": "User created", "is_new": True}
        else:
            return {"message": "Welcome back", "is_new": False}

# def generate_token_endpoint():
#     """Generate a new user token"""
#     token = generate_token()

#     with get_db_session() as db:
#         statement = select(User).where(User.token == token)
#         existing_user = db.exec(statement).first()
#         if existing_user:
#             token = generate_token()
#             statement = select(User).where(User.token == token)
#             existing_user = db.exec(statement).first()


#         db_user = User(token=token)
#         db.add(db_user)
        
#     return {"token": token, "message": "Token generated successfully"}


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
def set_fcm_token(data: FCMUpdate):
    email = data.email.strip().lower()
    
    with get_db_session() as db:
        # ← token column now holds the email!
        user = db.exec(select(User).where(User.token == email)).first()
        if not user:
            raise HTTPException(404, "User not found – register first")
        
        user.fcm_token = data.fcm_token
        db.add(user)
        db.commit()
    
    return {"status": "ok"}

@app.post("/api/alerts")
def create_alert(req: CreateAlertRequest):
    email = req.email.strip().lower()
    
    with get_db_session() as db:
        user = db.exec(select(User).where(User.token == email)).first()
        if not user:
            raise HTTPException(status_code=404, detail="User not found")
        
        alert = PriceAlert(
            user_id=user.id,
            user_token=email,       
            symbol=req.symbol.upper(),
            target_price=req.target_price,
            condition=req.condition
        )
        db.add(alert)
        db.commit()
        db.refresh(alert)
    
    return {"status": "alert created"}

if __name__ == "__main__":
    import uvicorn
    setup_database()
    uvicorn.run(app, host="0.0.0.0", port=8000)