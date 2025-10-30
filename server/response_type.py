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
