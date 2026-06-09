from datetime import time

import asyncpg
from fastapi import Depends

import app.database as db
from app.repositories.airport_repository import AirportRepository
from app.repositories.booking_repository import BookingRepository
from app.repositories.checkin_repository import CheckinRepository
from app.repositories.route_repository import RouteRepository
from app.services.route_service import RouteService


async def get_conn():
    async with db.pool.acquire() as conn:
        yield conn


async def get_airport_repo(conn: asyncpg.Connection = Depends(get_conn)):
    yield AirportRepository(conn)


async def get_route_repo(conn: asyncpg.Connection = Depends(get_conn)):
    yield RouteRepository(conn)


async def get_booking_repo(conn: asyncpg.Connection = Depends(get_conn)):
    yield BookingRepository(conn)


async def get_checkin_repo(conn: asyncpg.Connection = Depends(get_conn)):
    yield CheckinRepository(conn)


async def get_route_service(
    airport_repo: AirportRepository = Depends(get_airport_repo),
    route_repo: RouteRepository = Depends(get_route_repo),
):
    yield RouteService(airport_repo, route_repo)


def fmt_time(t: time | None) -> str | None:
    return t.strftime("%H:%M:%S") if t else None
