import asyncpg
from fastapi import APIRouter, Depends, HTTPException

from app.deps import get_conn, parse_coordinates
from app.models import AirportResponse, CityResponse

router = APIRouter(prefix="/cities", tags=["cities"])


@router.get("/", response_model=list[CityResponse])
async def list_cities(conn: asyncpg.Connection = Depends(get_conn)):
    rows = await conn.fetch(
        "SELECT DISTINCT city ->> 'en' AS city, country ->> 'en' AS country "
        "FROM bookings.airports_data ORDER BY city"
    )
    return [{"city": r["city"], "country": r["country"]} for r in rows]


@router.get("/{city_name}/airports", response_model=list[AirportResponse])
async def airports_in_city(
    city_name: str,
    conn: asyncpg.Connection = Depends(get_conn),
):
    rows = await conn.fetch(
        "SELECT airport_code, airport_name ->> 'en' AS airport_name, "
        "city ->> 'en' AS city, country ->> 'en' AS country, coordinates, timezone "
        "FROM bookings.airports_data WHERE city ->> 'en' = $1 ORDER BY airport_code",
        city_name,
    )
    if not rows:
        raise HTTPException(404, detail="City not found")
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
