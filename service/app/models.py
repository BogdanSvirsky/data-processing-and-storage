from datetime import date, datetime

from pydantic import BaseModel, Field


class Coordinates(BaseModel):
    lon: float
    lat: float


class AirportResponse(BaseModel):
    airport_code: str
    airport_name: str
    city: str
    country: str
    coordinates: Coordinates
    timezone: str


class CityResponse(BaseModel):
    city: str
    country: str


class ScheduleItem(BaseModel):
    days_of_week: list[int]
    flight_no: str
    origin: str | None = None
    destination: str | None = None


class SegmentResponse(BaseModel):
    flight_id: int
    route_no: str
    departure_airport: str
    arrival_airport: str
    scheduled_departure: datetime
    scheduled_arrival: datetime
    price: float


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
