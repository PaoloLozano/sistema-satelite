-- Datos de ejemplo para PROYECTO_SATELITE, para ejecutar con psql (usa \connect para cambiar de base).
-- Se puede correr varias veces: no duplica registros. Las fechas de los lotes son relativas a hoy.

\connect satelite_productos

INSERT INTO proveedores (nombre, ruc, contacto, email, telefono, direccion, creado_en) VALUES
  ('Gloria S.A.',     '20100190797', 'Juan Pérez',    'ventas@gloria.com.pe',    '01 5005000',  'Av. República de Panamá 2461, Lima',     now()),
  ('Alicorp S.A.A.',  '20100055237', 'María García',  'clientes@alicorp.com.pe', '01 3150800',  'Jr. Bartolomé Herrera 182, Magdalena',   now()),
  ('Bimbo Perú S.A.', '20100113455', 'Carlos Torres', 'ventas@bimbo.com.pe',     '01 6209000',  'Carretera Panamericana Sur Km 6.5',      now()),
  ('Laive S.A.',      '20101133146', 'Ana Romero',    'ventas@laive.com.pe',     '01 6103000',  'Av. Argentina 4793, Callao',             now()),
  ('Costeño Foods',   '20101019160', 'Luis Mendoza',  'pedidos@costeno.com.pe',  '01 6192000',  'Av. Materiales 2955, Lima',              now()),
  ('Cartavio S.A.A.', '20131565659', 'Rosa Vásquez',  'ventas@cartavio.com.pe',  '044 311000',  'Prolongación 28 de Julio, Trujillo',     now())
ON CONFLICT (ruc) DO NOTHING;

INSERT INTO productos (nombre, ean, categoria, proveedor, stock_actual, stock_minimo, precio, creado_en) VALUES
  ('Leche Gloria Entera 1L',      '7751519000017', 'Lácteos',   'Gloria S.A.',     48, 20, 3.40,  now()),
  ('Pan de Molde Bimbo 500g',     '7501030451033', 'Panadería', 'Bimbo Perú S.A.', 12, 10, 6.95,  now()),
  ('Yogurt Laive Fresa 200g',     '7754180010017', 'Lácteos',   'Laive S.A.',      36, 24, 1.80,  now()),
  ('Aceite Primor 1L',            '7751007000012', 'Aceites',   'Alicorp S.A.A.',  72, 12, 6.80,  now()),
  ('Arroz Costeño 5kg',           '7751007005512', 'Granos',    'Costeño Foods',   50, 20, 18.50, now()),
  ('Azúcar Rubia Cartavio 1kg',   '7750049000012', 'Azúcares',  'Cartavio S.A.A.', 80, 30, 3.20,  now())
ON CONFLICT (ean) DO NOTHING;


-- ============================================================
--

\connect satelite_lotes

INSERT INTO lotes (ean, producto_nombre, cantidad, costo_unitario, fecha_vencimiento, proveedor, estado, creado_en)
SELECT v.ean, v.producto_nombre, v.cantidad, v.costo, CURRENT_DATE + v.dias, v.proveedor,
       CASE WHEN v.dias <= 7 THEN 'ROJO' WHEN v.dias <= 30 THEN 'AMARILLO' ELSE 'VERDE' END,
       now()
FROM (VALUES
  ('7751519000017', 'Leche Gloria Entera 1L',    48, 3.40,   3, 'Gloria S.A.'),
  ('7751519000017', 'Leche Gloria Entera 1L',    60, 3.40,  45, 'Gloria S.A.'),
  ('7501030451033', 'Pan de Molde Bimbo 500g',   12, 6.95,   5, 'Bimbo Perú S.A.'),
  ('7754180010017', 'Yogurt Laive Fresa 200g',   36, 1.80,  20, 'Laive S.A.'),
  ('7751007000012', 'Aceite Primor 1L',          72, 6.80,  28, 'Alicorp S.A.A.'),
  ('7751007005512', 'Arroz Costeño 5kg',         50, 18.50, 120, 'Costeño Foods'),
  ('7750049000012', 'Azúcar Rubia Cartavio 1kg', 80, 3.20,  200, 'Cartavio S.A.A.')
) AS v(ean, producto_nombre, cantidad, costo, dias, proveedor)
WHERE NOT EXISTS (
  SELECT 1 FROM lotes l
  WHERE l.ean = v.ean AND l.cantidad = v.cantidad AND l.fecha_vencimiento = CURRENT_DATE + v.dias
);
