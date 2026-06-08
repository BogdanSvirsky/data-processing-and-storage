from datetime import datetime
from decimal import Decimal

import asyncpg
from fastapi import APIRouter, Depends

from app.deps import get_conn
from app.models import BookingRequest, BookingResponse

router = APIRouter(prefix="/bookings", tags=["bookings"])


@router.post("/", status_code=201, response_model=BookingResponse)
async def create_booking(
    body: BookingRequest,
    conn: asyncpg.Connection = Depends(get_conn),
):
    book_ref = await conn.fetchval(
        "SELECT upper(substr(md5(random()::text), 1, 6))"
    )
    ticket_no = await conn.fetchval(
        "SELECT '5552' || lpad(nextval('bookings.ticket_no_seq')::text, 9, '0')"
    )

    total_amount = Decimal("0")
    for fid in body.flight_ids:
        price = await conn.fetchval(
            """
            SELECT COALESCE(pr.price, 0)
            FROM bookings.flights f
            JOIN bookings.routes r ON r.route_no = f.route_no
                AND r.validity @> f.scheduled_departure
            LEFT JOIN bookings.pricing_rules pr
                ON pr.route_no = f.route_no AND pr.fare_conditions = $2
            WHERE f.flight_id = $1
            """,
            fid, body.fare_conditions,
        )
        total_amount += Decimal(str(price))

    now = datetime.now()
    await conn.execute(
        "INSERT INTO bookings.bookings (book_ref, book_date, total_amount) VALUES ($1, $2, $3)",
        book_ref, now, total_amount,
    )
    await conn.execute(
        "INSERT INTO bookings.tickets (ticket_no, book_ref, passenger_id, passenger_name, outbound) "
        "VALUES ($1, $2, $3, $4, true)",
        ticket_no, book_ref, body.passenger_id, body.passenger_name,
    )
    for fid in body.flight_ids:
        price = await conn.fetchval(
            """
            SELECT COALESCE(pr.price, 0)
            FROM bookings.flights f
            JOIN bookings.routes r ON r.route_no = f.route_no
                AND r.validity @> f.scheduled_departure
            LEFT JOIN bookings.pricing_rules pr
                ON pr.route_no = f.route_no AND pr.fare_conditions = $2
            WHERE f.flight_id = $1
            """,
            fid, body.fare_conditions,
        )
        await conn.execute(
            "INSERT INTO bookings.segments (ticket_no, flight_id, fare_conditions, price) "
            "VALUES ($1, $2, $3, $4)",
            ticket_no, fid, body.fare_conditions, price,
        )

    return BookingResponse(
        book_ref=book_ref,
        ticket_no=ticket_no,
        total_amount=float(total_amount),
        book_date=now,
    )
