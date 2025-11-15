from pydantic import BaseModel
from datetime import datetime

class TokenResponse(BaseModel):
    token: str
    message: str

class Stock(BaseModel):
    symbol: str
    price: float
    change: float
    change_percent: float
    volume: int
    timestamp: datetime
    open_price: float
    high_price: float
    low_price: float
    close_price: float

    class Config:
        from_attributes = True

class StockCreate(BaseModel):
    symbol: str
    price: float
    change: float
    change_percent: float
    volume: int
    timestamp: datetime

class TokenVerify(BaseModel):
    token: str
