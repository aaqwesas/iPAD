from sqlmodel import create_engine, Session, SQLModel
from contextlib import contextmanager

from config import settings

if settings.DATABASE_URL.startswith("sqlite"):
    # For SQLite
    connect_args = {"check_same_thread": False}
else:
    # For PostgreSQL, MySQL, etc.
    connect_args = {}

engine = create_engine(
    settings.DATABASE_URL,
    echo=settings.SQL_ECHO,
    connect_args=connect_args
)
@contextmanager
def get_db_session():
    session = Session(engine)
    try:
        yield session
        session.commit()
    except Exception as e:
        session.rollback()
        # logger.error(f"Database error: {e}", exc_info=True)
        print(f"Database error: {e}")
        raise  # Re-raise so caller (endpoint) can handle HTTP response
    finally:
        session.close()

def create_tables():
    """Create all tables"""
    SQLModel.metadata.create_all(bind=engine)