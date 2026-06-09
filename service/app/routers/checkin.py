from fastapi import APIRouter, Depends, HTTPException

from app.deps import get_checkin_repo
from app.exceptions import (
    AlreadyCheckedInError,
    SeatAlreadyTakenError,
    SeatNotAvailableError,
    TicketNotFoundError,
)
from app.models import CheckInRequest, CheckInResponse
from app.repositories.checkin_repository import CheckinRepository

router = APIRouter(prefix="/check-in", tags=["check-in"])


@router.post("/", response_model=CheckInResponse)
async def check_in(
        body: CheckInRequest,
        repo: CheckinRepository = Depends(get_checkin_repo),):
    try:
        return CheckInResponse.from_entity(await repo.check_in(
            body.ticket_no,
            body.flight_id,
            body.seat_no,
        ))
    except TicketNotFoundError:
        raise HTTPException(400, detail="Ticket not found for this flight")
    except SeatNotAvailableError:
        raise HTTPException(
            400, detail="Seat not available or wrong fare class")
    except SeatAlreadyTakenError:
        raise HTTPException(400, detail="Seat already taken")
    except AlreadyCheckedInError:
        raise HTTPException(400, detail="Already checked in for this flight")
