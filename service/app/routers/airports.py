from fastapi import APIRouter, Depends

from app.deps import get_airport_repo, get_route_repo
from app.models import AirportResponse, ScheduleItem
from app.repositories.airport_repository import AirportRepository
from app.repositories.route_repository import RouteRepository

router = APIRouter(prefix="/airports", tags=["airports"])


@router.get("/", response_model=list[AirportResponse])
async def list_airports(
    repo: AirportRepository = Depends(get_airport_repo),
):
    return [AirportResponse.from_entity(a) for a in await repo.find_all()]


@router.get("/{airport_code}/schedules/inbound", response_model=list[ScheduleItem])
async def inbound_schedule(
    airport_code: str,
    repo: RouteRepository = Depends(get_route_repo),
):
    return [ScheduleItem.from_entity(s) for s in await repo.find_inbound_schedules(airport_code)]


@router.get("/{airport_code}/schedules/outbound", response_model=list[ScheduleItem])
async def outbound_schedule(
    airport_code: str,
    repo: RouteRepository = Depends(get_route_repo),
):
    return [ScheduleItem.from_entity(s) for s in await repo.find_outbound_schedules(airport_code)]
