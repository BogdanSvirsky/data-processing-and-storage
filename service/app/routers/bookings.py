from fastapi import APIRouter, Depends, HTTPException

from app.deps import get_booking_repo
from app.exceptions import FlightNotAvailableError
from app.models import BookingRequest, BookingResponse
from app.repositories.booking_repository import BookingRepository

router = APIRouter(prefix="/bookings", tags=["bookings"])


@router.post("/", status_code=201, response_model=BookingResponse)
async def create_booking(
    body: BookingRequest,
    repo: BookingRepository = Depends(get_booking_repo),
):
    try:
        return BookingResponse.from_entity(await repo.create_booking(
            body.passenger_id,
            body.passenger_name,
            body.flight_ids,
            body.fare_conditions,
        ))
    except FlightNotAvailableError:
        raise HTTPException(400, detail="Flight not available for booking")
