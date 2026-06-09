from dataclasses import dataclass
from datetime import datetime


@dataclass
class Coordinates:
    lon: float
    lat: float


@dataclass
class City:
    city: str
    country: str


@dataclass
class Airport:
    airport_code: str
    airport_name: str
    city: str
    country: str
    coordinates: Coordinates
    timezone: str


@dataclass
class Schedule:
    days_of_week: list[int]
    flight_no: str
    origin: str | None = None
    destination: str | None = None


@dataclass
class Segment:
    flight_id: int
    route_no: str
    departure_airport: str
    arrival_airport: str
    scheduled_departure: datetime
    scheduled_arrival: datetime
    price: float


@dataclass
class RoutePath:
    connections: int
    total_price: float
    segments: list[Segment]


@dataclass
class BookingResult:
    book_ref: str
    ticket_no: str
    total_amount: float
    book_date: datetime


@dataclass
class CheckInResult:
    ticket_no: str
    flight_id: int
    seat_no: str
    boarding_no: int
    boarding_time: datetime


@dataclass
class TicketSegment:
    flight_id: int
    fare_conditions: str
