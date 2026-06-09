from fastapi import APIRouter, Depends

from app.deps import get_route_service
from app.models import RouteSearchRequest, RouteSearchResponse
from app.services.route_service import RouteService

router = APIRouter(prefix="/routes", tags=["routes"])


@router.post("/search", response_model=list[RouteSearchResponse])
async def search_routes(
        body: RouteSearchRequest,
        service: RouteService = Depends(get_route_service),):
    return [RouteSearchResponse.from_entity(r) for r in await service.search_routes(body)]
