from sqlmodel import SQLModel, Field
from typing import Optional
from datetime import datetime

class User(SQLModel, table=True):
    __tablename__: str = "users"

    id: Optional[int] = Field(default=None, primary_key=True)
    token: str = Field(unique=True, index=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now())

class StockPrice(SQLModel, table=True):
    __tablename__: str = "stock_prices"

    id: Optional[int] = Field(default=None, primary_key=True)
    symbol: str = Field(index=True)
    price: float
    change: float
    change_percent: float
    volume: int
    timestamp: datetime