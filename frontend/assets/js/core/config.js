/* Configuración global del frontend: todas las llamadas pasan por el API Gateway.
   - En tu PC (servidor.py o docker compose, puerto 3500) se llama directo al gateway en localhost:8080.
   - En Azure el sitio y el API comparten dirección: nginx reenvía /api/ al gateway, así que la base queda vacía. */
const API = (location.hostname === 'localhost' && location.port === '3500') ? 'http://localhost:8080' : '';
