from pydantic import BaseModel
from datetime import datetime





class TokenResponse(BaseModel):
    token: str
    message: str
    
    
class PortfolioUpdate(BaseModel):
    value: float

class PortfolioResponse(BaseModel):
    value: float
    

class UserHoldingResponse(BaseModel):
    stock_ticker: str
    quantity: float

class AddHoldingRequest(BaseModel):
    stock_ticker: str
    quantity: float

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

class StockHistorical(BaseModel):
    symbol: str
    date: datetime
    open_price: float
    high_price: float
    low_price: float
    close_price: float
    volume: int

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

class RegisterRequest(BaseModel):
    email: str

class RegisterResponse(BaseModel):
    message: str
    is_new: bool

class FCMUpdate(BaseModel):
    email: str
    fcm_token: str

class CreateAlertRequest(BaseModel):
    email: str
    symbol: str
    target: float
    condition: str