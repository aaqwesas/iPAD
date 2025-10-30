import logging
from logging.handlers import RotatingFileHandler
import os
import sys

def setup_logging(name) -> logging.Logger:
    os.makedirs("logs",exist_ok=True)
    logger = logging.getLogger(name)
    
    if logger.handlers:
        return logger
    logger.setLevel(logging.DEBUG)
    

    formatter = logging.Formatter(
        '{"time": "%(asctime)s", "level": "%(levelname)s", '
        '"logger": "%(name)s", "message": %(message)s}',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    handler = RotatingFileHandler(
        f'logs/{name}.log',
        maxBytes=10_000_000,
        backupCount=5
    )

    handler.setLevel(logging.INFO)
    handler.setFormatter(formatter)
    logger.addHandler(handler)

    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)

    logger.propagate = False
    
    return logger    