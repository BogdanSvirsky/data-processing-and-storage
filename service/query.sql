CREATE TABLE IF NOT EXISTS bookings.pricing_rules (
    fare_conditions TEXT            NOT NULL,
    route_no        TEXT            NOT NULL,
    price           NUMERIC(10,2)   NOT NULL,
    UNIQUE (route_no, fare_conditions)
);
INSERT INTO bookings.pricing_rules(fare_conditions, route_no, price)
SELECT DISTINCT ON (fare_conditions, f.route_no)
    fare_conditions, 
    f.route_no, 
    LAST_VALUE(price) OVER (
        PARTITION BY fare_conditions, f.route_no
        ORDER BY f.actual_arrival DESC
    ) 
FROM segments
LEFT JOIN flights f ON f.flight_id = segments.flight_id
WHERE f.status = 'Arrived' OR f.status = 'Departed'
ORDER BY fare_conditions, f.route_no;
