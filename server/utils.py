from database import get_db_session
from models import User





def setup_default_user() -> None:
    token = "1234"
    with get_db_session() as db:
        existing_user = db.query(User).filter(User.token == token).first()
        if existing_user:
            print("test user exist")
            return
        test_user = User(token=token)
        db.add(test_user)

        print("test user added")

