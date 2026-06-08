from datetime import datetime

import asyncpg
from fastapi import APIRouter, Depends, HTTPException

from app.deps import get_conn
from app.models import CheckInRequest, CheckInResponse

router = APIRouter(prefix="/check-in", tags=["check-in"])


@router.post("/", response_model=CheckInResponse)
async def check_in(
    body: CheckInRequest,
    conn: asyncpg.Connection = Depends(get_conn),
):
    segment = await conn.fetchrow(
        "SELECT flight_id, fare_conditions FROM bookings.segments "
        "WHERE ticket_no = $1 AND flight_id = $2",
        body.ticket_no, body.flight_id,
    )
    if not segment:
        raise HTTPException(400, detail="Ticket not found for this flight")

    airplane_code = await conn.fetchval(
        """
        SELECT r.airplane_code
        FROM bookings.flights f
        JOIN bookings.routes r ON r.route_no = f.route_no
            AND r.validity @> f.scheduled_departure
        WHERE f.flight_id = $1
        """,
        body.flight_id,
    )

    seat_ok = await conn.fetchval(
        "SELECT 1 FROM bookings.seats "
        "WHERE airplane_code = $1 AND seat_no = $2 AND fare_conditions = $3",
        airplane_code, body.seat_no, segment["fare_conditions"],
    )
    if not seat_ok:
        raise HTTPException(
            400, detail="Seat not available or wrong fare class")

    taken = await conn.fetchval(
        "SELECT 1 FROM bookings.boarding_passes WHERE flight_id = $1 AND seat_no = $2",
        body.flight_id, body.seat_no,
    )
    if taken:
        raise HTTPException(400, detail="Seat already taken")

    boarding_no = await conn.fetchval(
        "SELECT COALESCE(MAX(boarding_no), 0) + 1 FROM bookings.boarding_passes "
        "WHERE flight_id = $1",
        body.flight_id,
    )

    now = datetime.now()
    await conn.execute(
        "INSERT INTO bookings.boarding_passes (ticket_no, flight_id, seat_no, boarding_no, boarding_time) "
        "VALUES ($1, $2, $3, $4, $5)",
        body.ticket_no, body.flight_id, body.seat_no, boarding_no, now,
    )

    return CheckInResponse(
        ticket_no=body.ticket_no,
        flight_id=body.flight_id,
        seat_no=body.seat_no,
        boarding_no=boarding_no,
        boarding_time=now,
    )
