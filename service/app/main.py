from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.database import init_pool, close_pool
from app.routers import airports, bookings, checkin, cities, routes


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_pool()
    yield
    await close_pool()


app = FastAPI(
    lifespan=lifespan
)

app.include_router(cities.router)
app.include_router(airports.router)
app.include_router(routes.router)
app.include_router(bookings.router)
app.include_router(checkin.router)
