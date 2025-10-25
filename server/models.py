from sqlalchemy import Column, Integer, String, DateTime, Float
from database import Base
from datetime import datetime

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    token = Column(String, unique=True, index=True)
    created_at = Column(DateTime, default=datetime.utcnow)

class StockPrice(Base):
    __tablename__ = "stock_prices"

    id = Column(Integer, primary_key=True, index=True)
    symbol = Column(String, index=True)
    price = Column(Float)
    change = Column(Float)
    change_percent = Column(Float)
    volume = Column(Integer)
    timestamp = Column(DateTime, default=datetime.utcnow)