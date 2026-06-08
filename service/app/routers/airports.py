import asyncpg
from fastapi import APIRouter, Depends

from app.deps import get_conn, parse_coordinates
from app.models import AirportResponse, ScheduleItem

router = APIRouter(prefix="/airports", tags=["airports"])


@router.get("/", response_model=list[AirportResponse])
async def list_airports(conn: asyncpg.Connection = Depends(get_conn)):
    rows = await conn.fetch(
        "SELECT airport_code, airport_name ->> 'en' AS airport_name, "
        "city ->> 'en' AS city, country ->> 'en' AS country, coordinates, timezone "
        "FROM bookings.airports_data ORDER BY airport_code"
    )
    return [
        {
            "airport_code": r["airport_code"],
            "airport_name": r["airport_name"],
            "city": r["city"],
            "country": r["country"],
            "coordinates": parse_coordinates(r["coordinates"]),
            "timezone": r["timezone"],
        }
        for r in rows
    ]


@router.get("/{airport_code}/schedules/inbound",
            response_model=list[ScheduleItem])
async def inbound_schedule(
    airport_code: str,
    conn: asyncpg.Connection = Depends(get_conn),
):
    rows = await conn.fetch(
        "SELECT days_of_week, route_no, departure_airport "
        "FROM bookings.routes WHERE arrival_airport = $1 "
        "ORDER BY scheduled_time + duration",
        airport_code,
    )
    return [
        {
            "days_of_week": r["days_of_week"],
            "flight_no": r["route_no"],
            "origin": r["departure_airport"],
        }
        for r in rows
    ]


@router.get("/{airport_code}/schedules/outbound",
            response_model=list[ScheduleItem])
async def outbound_schedule(
    airport_code: str,
    conn: asyncpg.Connection = Depends(get_conn),
):
    rows = await conn.fetch(
        "SELECT days_of_week, route_no, arrival_airport "
        "FROM bookings.routes WHERE departure_airport = $1 "
        "ORDER BY scheduled_time",
        airport_code,
    )
    return [
        {
            "days_of_week": r["days_of_week"],
            "flight_no": r["route_no"],
            "destination": r["arrival_airport"],
        }
        for r in rows
    ]
