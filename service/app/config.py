from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str = "postgresql://flighter:1234@localhost:5433/demo"
    default_lang: str = "ru"
    debug: bool = False


settings = Settings()
