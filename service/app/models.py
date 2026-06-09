from datetime import date, datetime

from pydantic import BaseModel, Field

from app import entities


class Coordinates(BaseModel):
    lon: float
    lat: float

    @classmethod
    def from_entity(cls, e: entities.Coordinates) -> "Coordinates":
        return cls(lon=e.lon, lat=e.lat)


class AirportResponse(BaseModel):
    airport_code: str
    airport_name: str
    city: str
    country: str
    coordinates: Coordinates
    timezone: str

    @classmethod
    def from_entity(cls, e: entities.Airport) -> "AirportResponse":
        return cls(
            airport_code=e.airport_code,
            airport_name=e.airport_name,
            city=e.city,
            country=e.country,
            coordinates=Coordinates.from_entity(e.coordinates),
            timezone=e.timezone,
        )


class CityResponse(BaseModel):
    city: str
    country: str

    @classmethod
    def from_entity(cls, e: entities.City) -> "CityResponse":
        return cls(city=e.city, country=e.country)


class ScheduleItem(BaseModel):
    days_of_week: list[int]
    flight_no: str
    origin: str | None = None
    destination: str | None = None

    @classmethod
    def from_entity(cls, e: entities.Schedule) -> "ScheduleItem":
        return cls(
            days_of_week=e.days_of_week,
            flight_no=e.flight_no,
            origin=e.origin,
            destination=e.destination,
        )


class SegmentResponse(BaseModel):
    flight_id: int
    route_no: str
    departure_airport: str
    arrival_airport: str
    scheduled_departure: datetime
    scheduled_arrival: datetime
    price: float

    @classmethod
    def from_entity(cls, e: entities.Segment) -> "SegmentResponse":
        return cls(
            flight_id=e.flight_id,
            route_no=e.route_no,
            departure_airport=e.departure_airport,
            arrival_airport=e.arrival_airport,
            scheduled_departure=e.scheduled_departure,
            scheduled_arrival=e.scheduled_arrival,
            price=e.price,
        )


class RouteSearchRequest(BaseModel):
    from_: str = Field(..., alias="from")
    from_type: str = Field(..., pattern=r"^(airport|city)$")
    to: str
    to_type: str = Field(..., pattern=r"^(airport|city)$")
    departure_date: date
    booking_class: str = Field(..., pattern=r"^(Economy|Comfort|Business)$")
    max_connections: str = Field(..., pattern=r"^(0|1|2|3|unbound)$")


class RouteSearchResponse(BaseModel):
    connections_count: int
    total_price: float
    segments: list[SegmentResponse]

    @classmethod
    def from_entity(cls, e: entities.RoutePath) -> "RouteSearchResponse":
        return cls(
            connections_count=e.connections,
            total_price=e.total_price,
            segments=[SegmentResponse.from_entity(s) for s in e.segments],
        )


class BookingRequest(BaseModel):
    passenger_id: str
    passenger_name: str
    flight_ids: list[int]
    fare_conditions: str = Field(..., pattern=r"^(Economy|Comfort|Business)$")


class BookingResponse(BaseModel):
    book_ref: str
    ticket_no: str
    total_amount: float
    book_date: datetime

    @classmethod
    def from_entity(cls, e: entities.BookingResult) -> "BookingResponse":
        return cls(
            book_ref=e.book_ref,
            ticket_no=e.ticket_no,
            total_amount=e.total_amount,
            book_date=e.book_date,
        )


class CheckInRequest(BaseModel):
    ticket_no: str
    flight_id: int
    seat_no: str


class CheckInResponse(BaseModel):
    ticket_no: str
    flight_id: int
    seat_no: str
    boarding_no: int
    boarding_time: datetime

    @classmethod
    def from_entity(cls, e: entities.CheckInResult) -> "CheckInResponse":
        return cls(
            ticket_no=e.ticket_no,
            flight_id=e.flight_id,
            seat_no=e.seat_no,
            boarding_no=e.boarding_no,
            boarding_time=e.boarding_time,
        )
