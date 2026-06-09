from fastapi import APIRouter, Depends, HTTPException

from app.deps import get_airport_repo
from app.exceptions import CityNotFoundError
from app.models import AirportResponse, CityResponse
from app.repositories.airport_repository import AirportRepository

router = APIRouter(prefix="/cities", tags=["cities"])


@router.get("/", response_model=list[CityResponse])
async def list_cities(
    repo: AirportRepository = Depends(get_airport_repo),
):
    return [CityResponse.from_entity(c) for c in await repo.find_cities()]


@router.get("/{city_name}/airports", response_model=list[AirportResponse])
async def airports_in_city(
    city_name: str,
    repo: AirportRepository = Depends(get_airport_repo),
):
    try:
        return [AirportResponse.from_entity(a) for a in await repo.find_airports_by_city(city_name)]
    except CityNotFoundError:
        raise HTTPException(404, detail="City not found")
