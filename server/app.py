from typing import List, AsyncGenerator
import datetime
import uuid
from contextlib import asynccontextmanager

from sqlmodel import select, distinct
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from response_type import (
    AddHoldingRequest,
    PortfolioHistoryResponse,
    PortfolioResponse,
    PortfolioUpdate,
    RegisterResponse,
    TokenVerify,
    Stock,
    StockCreate,
    StockHistorical,
    RegisterRequest,
    FCMUpdate,
    CreateAlertRequest,
    UserHoldingResponse,
    MappingResponse,
)
from database import get_db_session, create_tables
from models import (
    User,
    StockPrice,
    StockHistoricalData,
    StockHistoricalData_weekly,
    PriceAlert,
    UserHolding,
    UserPortfolio,
    NameTickerMap,
    PortfolioHistory,
)
from utils import (
    _portfolio_value,
    _validate_trade,
    create_data_fetcher,
    setup_database,
    send_fcm_notification,
    TICKERS,
)

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


@app.post("/users/{token}/add_history")
async def add_portfolio_history(token: str, value: float):
    with get_db_session() as db:
        new_history = PortfolioHistory(token=token, value=value)

        db.add(new_history)
        db.commit()
        db.refresh(new_history)

        return {
            "message": "Portfolio history added successfully",
            "id": new_history.id,
            "value": new_history.value,
            "timestamp": new_history.timestamp,
        }


@app.get(
    "/users/{token}/history", response_model=List[PortfolioHistoryResponse]
)  # Changed path and response_model
async def get_portfolio_history(token: str, limit: int = 30):
    with get_db_session() as db:
        statement = (
            select(
                PortfolioHistory.value, PortfolioHistory.timestamp
            )  # Select both fields
            .where(PortfolioHistory.token == token)
            .order_by(PortfolioHistory.timestamp.desc())
            .limit(limit=limit)
        )

        # Fetch the raw results (tuples of value, timestamp)
        raw_results = db.exec(statement).all()

        # Convert the raw results to the response model format
        # Assuming `timestamp` from the DB is a datetime object that needs to be stringified
        history_list = [
            PortfolioHistoryResponse(
                value=result.value,
                timestamp=result.timestamp.isoformat(),  # Convert datetime to ISO string
            )
            for result in raw_results
        ]

        return history_list  # Return the list of response model objects


@app.get("/test-push")
async def test_push():
    # ←←← REPLACE THIS WITH A REAL TOKEN FROM YOUR PHONE LOGS ←←←
    test_token = "dp9Xoj_5RH29NBdELTJoTj:APA91bGSVH_hRb3t2OSNeXXk__KxNJoauX3xpAv3EJOYkvl8V3KmC8x_yprjEgigE0VDDIZCejme0ef1wcVjMEWT5QVKbzFesHw1n3jO5KQvY3hnK8aOBZE"
    send_fcm_notification(
        token=test_token,
        title="IT WORKS!",
        body="Your price alert system is 100% ready!",
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


@app.get("/users/{token}/portfolio", response_model=PortfolioResponse)
async def get_portfolio_value(token: str):
    with get_db_session() as db:
        portfolio = db.exec(
            select(UserPortfolio).where(UserPortfolio.token == token)
        ).first()

        if not portfolio:
            # Return default value if no portfolio exists
            return PortfolioResponse(value=0.0)

        return PortfolioResponse(value=portfolio.value)


# 2. Update portfolio value
@app.put("/users/{token}/portfolio", response_model=PortfolioResponse)
async def update_portfolio_value(token: str, update: PortfolioUpdate):
    with get_db_session() as db:
        portfolio = db.exec(
            select(UserPortfolio).where(UserPortfolio.token == token)
        ).first()

        if portfolio:
            # Update existing portfolio
            portfolio.value = update.value
        else:
            # Create new portfolio if doesn't exist
            portfolio = UserPortfolio(token=token, value=update.value)
            db.add(portfolio)

        db.commit()
        db.refresh(portfolio)
        return PortfolioResponse(value=portfolio.value)


@app.get("/users/{token}/percentage_change")
async def get_portfolio_change(token: str) -> float:
    with get_db_session() as db:
        new_portfolio_val = _portfolio_value(db=db, token=token)
        portfolio = db.exec(
            select(UserPortfolio).where(UserPortfolio.token == token)
        ).first()
        print(new_portfolio_val, portfolio)
        if not portfolio:
            return 0

        old_val = portfolio.value

        if old_val == 0:
            if new_portfolio_val == 0:
                return 0
            else:
                return 100
    if new_portfolio_val == old_val:
        return 0.0

    percentage_change = ((new_portfolio_val - old_val) / old_val) * 100

    return round(percentage_change, 2)


@app.get("/users/{token}/holdings", response_model=List[UserHoldingResponse])
async def get_user_holdings(token: str):
    with get_db_session() as db:
        # Query all holdings for this user
        holdings = db.exec(
            select(UserHolding.stock_ticker, UserHolding.quantity).where(
                UserHolding.token == token
            )
        ).all()

        if not holdings:
            return []

        # Convert to response model
        return [
            UserHoldingResponse(stock_ticker=ticker, quantity=quantity)
            for ticker, quantity in holdings
        ]


@app.get("/users/{token}/holding", response_model=UserHoldingResponse)
async def get_user_holding(token: str, ticker: str):
    with get_db_session() as db:
        holding = db.exec(
            select(UserHolding.stock_ticker, UserHolding.quantity).where(
                UserHolding.token == token, UserHolding.stock_ticker == ticker
            )
        ).first()
        if not holding:
            return

        return holding


@app.post("/users/{token}/holdings", response_model=UserHoldingResponse)
async def add_user_holding(token: str, request: AddHoldingRequest):
    with get_db_session() as db:
        holding = db.exec(
            select(UserHolding).where(
                UserHolding.token == token,
                UserHolding.stock_ticker == request.stock_ticker,
            )
        ).first()

        current_qty = holding.quantity if holding else 0.0

        # Validate the trade
        _validate_trade(current_qty, request.quantity)

        new_qty = current_qty + request.quantity

        if new_qty <= 0:
            if holding:
                db.delete(holding)
                db.commit()
            return UserHoldingResponse(stock_ticker=request.stock_ticker, quantity=0.0)

        elif holding:
            holding.quantity = new_qty
            db.commit()
            db.refresh(holding)
            return UserHoldingResponse(
                stock_ticker=holding.stock_ticker, quantity=holding.quantity
            )

        else:
            new_holding = UserHolding(
                token=token,
                stock_ticker=request.stock_ticker,
                quantity=new_qty,
            )
            db.add(new_holding)
            db.commit()
            db.refresh(new_holding)
            return UserHoldingResponse(
                stock_ticker=new_holding.stock_ticker, quantity=new_holding.quantity
            )


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


@app.get("/api/companyname/{symbol}", response_model=MappingResponse)
def get_company_name(symbol):
    """Get company name by symbol"""
    with get_db_session() as db:
        statement = select(distinct(NameTickerMap.companyName)).where(
            NameTickerMap.symbol == symbol.upper()
        )
        get_company_name = db.exec(statement).first()

        return MappingResponse(companyName=get_company_name)


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
            .order_by(StockPrice.timestamp.desc())
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
            timestamp=stock.timestamp,
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
            id=user.id,
            user_token=email,
            symbol=req.symbol.upper(),
            target=req.target,
            condition=req.condition,
        )
        db.add(alert)
        db.commit()
        db.refresh(alert)

    return {"status": "alert created"}


if __name__ == "__main__":
    import uvicorn

    setup_database()
    uvicorn.run(app, host="0.0.0.0", port=8000)
