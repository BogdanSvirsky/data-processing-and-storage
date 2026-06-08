import asyncpg

from app.config import settings

pool: asyncpg.Pool | None = None


async def init_pool():
    global pool
    pool = await asyncpg.create_pool(
        settings.database_url,
        min_size=2,
        max_size=10,
    )

    async with pool.acquire() as conn:
        await conn.execute("""
            CREATE SEQUENCE IF NOT EXISTS bookings.ticket_no_seq START WITH 1
        """)


async def close_pool():
    global pool
    if pool:
        await pool.close()
        pool = None
