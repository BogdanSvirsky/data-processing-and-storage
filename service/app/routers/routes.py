import json
from datetime import datetime
from decimal import Decimal
import asyncpg
from fastapi import APIRouter, Depends

from pydantic import BaseModel

from app.deps import get_conn
from app.models import RouteSearchRequest, RouteSearchResponse, SegmentResponse

router = APIRouter(prefix="/routes", tags=["routes"])


@router.post("/search", response_model=list[RouteSearchResponse])
async def search_routes(
    body: RouteSearchRequest,
    conn: asyncpg.Connection = Depends(get_conn),
):
    from_codes = await _resolve_location(conn, body.from_, body.from_type)

    to_codes = await _resolve_location(conn, body.to, body.to_type)

    if not from_codes or not to_codes:
        return []

    max_connections = None if body.max_connections == "unbound" else int(
        body.max_connections)

    query = """
    WITH RECURSIVE flight_paths AS (
        SELECT
            f.flight_id, f.route_no, r.departure_airport, r.arrival_airport,
            f.scheduled_departure, f.scheduled_arrival,

            0 AS connections,
            ARRAY[r.departure_airport, r.arrival_airport] AS visited,
            COALESCE(pr.price, 0) AS total_price,
            jsonb_build_array(jsonb_build_object(
                'flight_id', f.flight_id,

                'route_no', f.route_no,
                'departure_airport', r.departure_airport,
                'arrival_airport', r.arrival_airport,
                'scheduled_departure', to_char(f.scheduled_departure, 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
                'scheduled_arrival', to_char(f.scheduled_arrival, 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
                'price', COALESCE(pr.price, 0)::float
            )) AS segments_json
        FROM bookings.flights f
        JOIN bookings.routes r ON f.route_no = r.route_no
        LEFT JOIN bookings.pricing_rules pr
            ON pr.route_no = f.route_no AND pr.fare_conditions = $1
        WHERE r.departure_airport = ANY($2)
          AND f.scheduled_departure >= $3::timestamp
          AND f.scheduled_departure < $3::timestamp + INTERVAL '1 day'
          AND f.status != 'Cancelled'
          AND EXISTS (
              SELECT 1 FROM bookings.seats s
              WHERE s.airplane_code = r.airplane_code AND s.fare_conditions = $1
          )

        UNION ALL


        SELECT
            f.flight_id, f.route_no, p.departure_airport, r.arrival_airport,
            p.scheduled_departure, f.scheduled_arrival,
            p.connections + 1,
            p.visited || r.arrival_airport,

            p.total_price + COALESCE(pr.price, 0),
            p.segments_json || jsonb_build_object(
                'flight_id', f.flight_id,
                'route_no', f.route_no,
                'departure_airport', r.departure_airport,
                'arrival_airport', r.arrival_airport,
                'scheduled_departure', to_char(f.scheduled_departure, 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
                'scheduled_arrival', to_char(f.scheduled_arrival, 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
                'price', COALESCE(pr.price, 0)::float
            )
        FROM flight_paths p
        JOIN bookings.routes r ON r.departure_airport = p.arrival_airport
        JOIN bookings.flights f ON f.route_no = r.route_no
        LEFT JOIN bookings.pricing_rules pr
            ON pr.route_no = f.route_no AND pr.fare_conditions = $1
        WHERE f.scheduled_departure > p.scheduled_arrival

          AND f.scheduled_departure <= p.scheduled_arrival + INTERVAL '24 hours'
          AND NOT (r.arrival_airport = ANY(p.visited))
          AND f.status != 'Cancelled'

          AND ($4::int IS NULL OR p.connections + 1 <= $4)
          -- Проверка мест для следующего сегмента
          AND EXISTS (
              SELECT 1 FROM bookings.seats s
              WHERE s.airplane_code = r.airplane_code AND s.fare_conditions = $1
          )

    )
    SELECT connections, total_price, segments_json
    FROM flight_paths
    WHERE arrival_airport = ANY($5)
    ORDER BY connections ASC, total_price ASC;
    """

    rows = await conn.fetch(
        query,
        body.booking_class,
        from_codes,
        body.departure_date,
        max_connections,
        to_codes,

    )

    return [
        RouteSearchResponse(
            connections_count=r["connections"],
            total_price=float(r["total_price"]),
            segments=[SegmentResponse(**seg)
                      for seg in json.loads(r["segments_json"])],
        )
        for r in rows
    ]


async def _resolve_location(
    conn: asyncpg.Connection, value: str, loc_type: str
) -> list[str]:

    if loc_type == "airport":
        return [value]

    rows = await conn.fetch(
        """
        SELECT DISTINCT airport_code
        FROM bookings.airports_data
        WHERE city ->> 'en' = $1 OR city ->> 'ru' = $1

        """,
        value,
    )
    return [r["airport_code"] for r in rows]
