from datetime import time
from typing import Any

import asyncpg
from fastapi import Depends

import app.database as db


async def get_conn():
    async with db.pool.acquire() as conn:
        yield conn


def parse_coordinates(raw: Any) -> dict:
    if isinstance(raw, str):
        raw = raw.strip("()")
        parts = raw.split(",")
        return {"lon": float(parts[0].strip()), "lat": float(parts[1].strip())}
    if hasattr(raw, "x"):
        return {"lon": float(raw.x), "lat": float(raw.y)}
    if isinstance(raw, (tuple, list)):
        return {"lon": float(raw[0]), "lat": float(raw[1])}
    return {"lon": 0.0, "lat": 0.0}


def fmt_time(t: time | None) -> str | None:
    return t.strftime("%H:%M:%S") if t else None
