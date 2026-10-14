-- Script de inicialización de PostgreSQL para PROYECTO_SATELITE
-- Crea las bases de datos por servicio y un usuario compartido

-- Crear usuario satelite si no existe
DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'satelite') THEN
      CREATE USER satelite WITH PASSWORD 'satelite123';
   END IF;
END
$$;

-- Crear bases de datos por microservicio
CREATE DATABASE satelite_auth WITH OWNER = satelite ENCODING = 'UTF8' TEMPLATE = template0;

CREATE DATABASE satelite_productos WITH OWNER = satelite ENCODING = 'UTF8' TEMPLATE = template0;

CREATE DATABASE satelite_lotes WITH OWNER = satelite ENCODING = 'UTF8' TEMPLATE = template0;

CREATE DATABASE satelite_cotizaciones WITH OWNER = satelite ENCODING = 'UTF8' TEMPLATE = template0;

CREATE DATABASE satelite_alertas WITH OWNER = satelite ENCODING = 'UTF8' TEMPLATE = template0;

-- Otorgar privilegios al usuario satelite en todas las bases
GRANT ALL PRIVILEGES ON DATABASE satelite_auth TO satelite;
GRANT ALL PRIVILEGES ON DATABASE satelite_productos TO satelite;
GRANT ALL PRIVILEGES ON DATABASE satelite_lotes TO satelite;
GRANT ALL PRIVILEGES ON DATABASE satelite_cotizaciones TO satelite;
GRANT ALL PRIVILEGES ON DATABASE satelite_alertas TO satelite;

-- Conectar a satelite_auth y otorgar privilegios de schema
\connect satelite_auth
GRANT ALL ON SCHEMA public TO satelite;

\connect satelite_productos
GRANT ALL ON SCHEMA public TO satelite;

\connect satelite_lotes
GRANT ALL ON SCHEMA public TO satelite;

\connect satelite_cotizaciones
GRANT ALL ON SCHEMA public TO satelite;

\connect satelite_alertas
GRANT ALL ON SCHEMA public TO satelite;
