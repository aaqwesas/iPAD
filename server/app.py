from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from datetime import datetime
import uuid

from response_type import TokenResponse, TokenVerify, Stock, StockCreate
from database import engine, get_db_session
from models import User, StockPrice, Base


# Create tables
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Stock API", version="1.0.0")


def generate_token() -> str:
    """Generate a random token"""
    token = uuid.uuid4()
    return str(token)

@app.post("/api/generate-token", response_model=TokenResponse)
def generate_token_endpoint():
    """Generate a new user token"""
    token = generate_token()

    with get_db_session() as db:
        existing_user = db.query(User).filter(User.token == token).first()

        db_user = User(token=token)
        db.add(db_user)
    return {"token": token, "message": "Token generated successfully"}


@app.post("/api/verify-token")
def verify_token_endpoint(token_data: TokenVerify):
    """Verify if a token exists in the database"""
    with get_db_session() as db:
        user = db.query(User).filter(User.token == token_data.token).first()
    if user:
        return {"valid": True, "message": "Token is valid"}
    else:
        return {"valid": False, "message": "Invalid token"}

@app.get("/api/stocks", response_model=List[Stock])
def get_stocks(db: Session = Depends(get_db_session)):
    """Get all stock prices"""
    stocks = db.query(StockPrice).order_by(StockPrice.timestamp.desc()).all()
    # Return the latest price for each symbol
    unique_stocks = {}
    for stock in stocks:
        if stock.symbol not in unique_stocks:
            unique_stocks[stock.symbol] = stock
    return list(unique_stocks.values())

@app.get("/api/stocks/{symbol}", response_model=Stock)
def get_stock(symbol: str, db: Session = Depends(get_db_session)):
    """Get specific stock price"""
    stock = db.query(StockPrice).filter(StockPrice.symbol == symbol.upper()).order_by(StockPrice.timestamp.desc()).first()
    if not stock:
        raise HTTPException(status_code=404, detail="Stock not found")
    return stock

@app.post("/api/stocks")
def create_stock(stock: StockCreate, db: Session = Depends(get_db_session)):
    """Create/update stock price (for testing - in production you'd have auth)"""
    db_stock = StockPrice(
        symbol=stock.symbol.upper(),
        price=stock.price,
        change=stock.change,
        change_percent=stock.change_percent,
        volume=stock.volume
    )
    db.add(db_stock)
    db.commit()
    db.refresh(db_stock)
    return db_stock

@app.get("/api/health")
def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "timestamp": datetime.utcnow()}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)