#!/bin/sh
# Versión de init.sql para producción: la contraseña del usuario "satelite" viene de la variable DB_PASS
# (PostgreSQL ejecuta este script solo la primera vez que se crea el volumen de datos).
set -e
: "${DB_PASS:?falta DB_PASS}"

BASES="satelite_auth satelite_productos satelite_lotes satelite_cotizaciones satelite_alertas"

# Usuario (si no existe) y las 5 bases, una por microservicio
psql -v ON_ERROR_STOP=1 -v clave="$DB_PASS" --username "$POSTGRES_USER" --dbname postgres <<'EOSQL'
SELECT format('CREATE ROLE satelite LOGIN PASSWORD %L', :'clave')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'satelite') \gexec

SELECT format('CREATE DATABASE %I OWNER satelite ENCODING ''UTF8'' TEMPLATE template0', d)
FROM unnest(ARRAY['satelite_auth','satelite_productos','satelite_lotes','satelite_cotizaciones','satelite_alertas']) AS d
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = d) \gexec
EOSQL

# Permisos sobre cada base y su esquema public
for base in $BASES; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$base" <<'EOSQL'
SELECT format('GRANT ALL PRIVILEGES ON DATABASE %I TO satelite', current_database()) \gexec
GRANT ALL ON SCHEMA public TO satelite;
EOSQL
done
