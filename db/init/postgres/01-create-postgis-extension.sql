CREATE EXTENSION IF NOT EXISTS postgis;

CREATE INDEX IF NOT EXISTS idx_hospitals_location_geography
ON hospitals
USING GIST (
    ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
);
