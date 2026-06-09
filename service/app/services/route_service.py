from app.entities import RoutePath
from app.models import RouteSearchRequest
from app.repositories.airport_repository import AirportRepository
from app.repositories.route_repository import RouteRepository


class RouteService:
    def __init__(
        self,
        airport_repo: AirportRepository,
        route_repo: RouteRepository,
    ):
        self._airport_repo = airport_repo
        self._route_repo = route_repo

    async def search_routes(
        self, body: RouteSearchRequest
    ) -> list[RoutePath]:
        from_codes = await self._resolve_location(body.from_, body.from_type)
        to_codes = await self._resolve_location(body.to, body.to_type)

        if not from_codes or not to_codes:
            return []

        max_connections = (
            None
            if body.max_connections == "unbound"
            else int(body.max_connections)
        )

        return await self._route_repo.search_flight_paths(
            body.booking_class,
            from_codes,
            body.departure_date,
            max_connections,
            to_codes,
        )

    async def _resolve_location(
        self, value: str, loc_type: str
    ) -> list[str]:
        if loc_type == "airport":
            return [value]

        return await self._airport_repo.find_airport_codes_by_city(value)
