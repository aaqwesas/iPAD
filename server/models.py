from sqlalchemy.sql.roles import TruncatedLabelRole
from sqlmodel import SQLModel, Field
from typing import Optional
from datetime import datetime


class User(SQLModel, table=True):
    __tablename__: str = "users"
    id: Optional[int] = Field(default=None, primary_key=True)
    token: str = Field(unique=True, index=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now())
    fcm_token: Optional[str] = None


class UserPortfolio(SQLModel, table=True):
    __tablename__: str = "user_portfolios"
    id: Optional[int] = Field(default=None, primary_key=True)
    token: str = Field(foreign_key="users.token")
    value: float
    # previous_day_value: float = Field(default=0.0)


class UserHolding(SQLModel, table=True):
    __tablename__ = "user_holdings"
    id: Optional[int] = Field(default=None, primary_key=True)
    token: str = Field(foreign_key="users.token")
    stock_ticker: str = Field(index=True)
    quantity: float


class PortfolioHistory(SQLModel, table=True):
    __tablename__ = "portfolio_history"
    id: Optional[int] = Field(default=None, primary_key=True)
    token: str = Field(foreign_key="users.token")
    value: float
    timestamp: datetime = Field(default_factory=lambda: datetime.now())


class StockPrice(SQLModel, table=True):
    __tablename__: str = "stock_prices"

    id: Optional[int] = Field(default=None, primary_key=True)
    symbol: str = Field(index=True)
    price: float
    change: float
    change_percent: float
    volume: int
    timestamp: datetime
    open_price: Optional[float] = None
    high_price: Optional[float] = None
    low_price: Optional[float] = None
    close_price: Optional[float] = None


class StockHistoricalData(SQLModel, table=True):
    __tablename__: str = "stock_historical_data"

    id: Optional[int] = Field(default=None, primary_key=True)
    symbol: str = Field(index=True)
    date: datetime
    open_price: float
    high_price: float
    low_price: float
    close_price: float
    volume: int


class StockHistoricalData_weekly(SQLModel, table=True):
    __tablename__: str = "stock_historical_data_weekly"

    id: Optional[int] = Field(default=None, primary_key=True)
    symbol: str = Field(index=True)
    date: datetime
    open_price: float
    high_price: float
    low_price: float
    close_price: float
    volume: int


class PriceAlert(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True, sa_column_kwargs={"autoincrement": True})
    user_token: str = Field(foreign_key="users.token", index=True)
    symbol: str
    target: float
    condition: str  # "above", "below", "dayup", "daydown", "volume above", "CUSTOMIZED PORTFOLIO"
    is_active: bool = True
    notified: bool = False
    created_at: datetime = Field(default_factory=lambda: datetime.now())
    triggered_at: Optional[datetime] = None
    triggered_price: Optional[float] = None


class NameTickerMap(SQLModel, table=True):
    __tablename__: str = "company_name"

    id: Optional[int] = Field(default=None, primary_key=True)
    symbol: str = Field(index=True)
    companyName: str
