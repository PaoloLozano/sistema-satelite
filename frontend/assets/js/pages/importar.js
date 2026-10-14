/* Importación desde el POS. El archivo (.csv / .xlsx) se lee en el navegador y, según sus columnas:
   - con fecha_vencimiento → lotes: cada fila se envía a ms-lotes (alimenta el semáforo y las alertas)
   - sin fecha_vencimiento → productos: POST a ms-productos si el EAN es nuevo, PUT si ya existe */
let modo  = 'productos';   // 'productos' | 'lotes'
let filas = [];

const ALIAS_PRODUCTOS = {
  ean:       ['ean13', 'ean', 'codigo', 'codigo_barras'],
  nombre:    ['nombre', 'producto', 'descripcion'],
  categoria: ['categoria'],
  stock:     ['stock', 'stock_actual', 'cantidad'],
  precio:    ['costo_unitario', 'precio', 'costo'],
  stockMin:  ['stock_minimo', 'stock_min', 'minimo'],
  proveedor: ['proveedor']
};
const ALIAS_LOTES = {
  ean:       ['ean13', 'ean', 'codigo', 'codigo_barras'],
  cantidad:  ['cantidad', 'unidades', 'stock'],
  fecha:     ['fecha_vencimiento', 'vencimiento', 'fecha'],
  precio:    ['costo_unitario', 'costo', 'precio'],
  proveedor: ['proveedor']
};
const ALIAS_FECHA = ['fecha_vencimiento', 'vencimiento'];

const dropZone = document.getElementById('zona-carga');
dropZone.addEventListener('dragover', e => { e.preventDefault(); dropZone.classList.add('arrastrando'); });
dropZone.addEventListener('dragleave', () => dropZone.classList.remove('arrastrando'));
dropZone.addEventListener('drop', e => {
  e.preventDefault(); dropZone.classList.remove('arrastrando');
  if (e.dataTransfer.files[0]) handleFile(e.dataTransfer.files[0]);
});

function setPaso(n) {
  [1, 2, 3].forEach(i => document.getElementById(`sn-${i}`).classList.toggle('active', i <= n));
}

function setProgreso(pct, label) {
  document.getElementById('progress-wrap').style.display = 'block';
  document.getElementById('progress-fill').style.width   = `${pct}%`;
  document.getElementById('progress-pct').textContent    = `${Math.round(pct)}%`;
  document.getElementById('progress-label').textContent  = label;
}

/* ====== LECTURA ====== */
function normalizar(t) {
  return String(t ?? '').trim().toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/\s+/g, '_');
}

function aISO(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// Celda de Excel → texto. Las fechas se redondean al día (SheetJS puede devolverlas con minutos de desfase)
function celdaATexto(v) {
  if (v instanceof Date) return aISO(new Date(v.getTime() + 12 * 3600 * 1000));
  return String(v ?? '').trim();
}

function parseCSV(texto) {
  const primera = texto.split('\n')[0];
  const sep = (primera.match(/;/g) || []).length > (primera.match(/,/g) || []).length ? ';' : ',';
  return texto.replace(/^﻿/, '').split(/\r?\n/).filter(l => l.trim()).map(linea => {
    const celdas = []; let actual = ''; let comillas = false;
    for (const ch of linea) {
      if (ch === '"') comillas = !comillas;
      else if (ch === sep && !comillas) { celdas.push(actual); actual = ''; }
      else actual += ch;
    }
    celdas.push(actual);
    return celdas.map(c => c.trim());
  });
}

async function leerArchivo(file) {
  if (file.name.toLowerCase().endsWith('.csv')) return parseCSV(await file.text());
  const libro = XLSX.read(await file.arrayBuffer(), { cellDates: true });
  return XLSX.utils.sheet_to_json(libro.Sheets[libro.SheetNames[0]], { header: 1, raw: true, defval: '' })
    .map(fila => fila.map(celdaATexto));
}

// "2026-10-15" o "15/10/2026" → "2026-10-15" (null si no es una fecha real)
function fechaAISO(t) {
  let y, m, d;
  let r = t.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);
  if (r) [y, m, d] = [+r[1], +r[2], +r[3]];
  else if ((r = t.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/))) [d, m, y] = [+r[1], +r[2], +r[3]];
  else return null;
  const f = new Date(y, m - 1, d);
  return f.getFullYear() === y && f.getMonth() === m - 1 && f.getDate() === d ? aISO(f) : null;
}

function estadoPorDias(dias) { return dias <= 7 ? 'ROJO' : dias <= 30 ? 'AMARILLO' : 'VERDE'; }

function diasHasta(iso) {
  const hoy = new Date(); hoy.setHours(0, 0, 0, 0);
  return Math.round((parseFecha(iso) - hoy) / 86400000);
}

async function handleFile(file) {
  if (!file) return;
  const ext = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
  if (ext !== '.xlsx' && ext !== '.csv') { showaviso('Solo se aceptan .xlsx o .csv', 'error'); return; }
  if (file.size > 10 * 1024 * 1024)      { showaviso('El archivo supera los 10 MB.', 'error'); return; }

  document.getElementById('drop-title').textContent = file.name;
  setPaso(2);
  setProgreso(30, 'Leyendo archivo…');

  let tabla, catalogo;
  try {
    [tabla, catalogo] = await Promise.all([leerArchivo(file), api('/api/productos')]);
  } catch (e) {
    showaviso(e.message || 'No se pudo leer el archivo.', 'error');
    resetImport(); return;
  }
  if (!tabla.length) { showaviso('El archivo está vacío.', 'error'); resetImport(); return; }

  const cabecera = tabla[0].map(normalizar);
  modo = cabecera.some(h => ALIAS_FECHA.includes(h)) ? 'lotes' : 'productos';
  const alias = modo === 'lotes' ? ALIAS_LOTES : ALIAS_PRODUCTOS;
  const col = Object.fromEntries(Object.entries(alias).map(([k, al]) => [k, cabecera.findIndex(h => al.includes(h))]));
  const obligatorias = modo === 'lotes' ? ['ean', 'cantidad', 'fecha'] : ['ean', 'nombre'];
  const faltan = obligatorias.filter(k => col[k] < 0);
  if (faltan.length) {
    showaviso(`Faltan columnas obligatorias: ${faltan.map(k => alias[k][0]).join(', ')}.`, 'error');
    resetImport(); return;
  }

  setProgreso(60, `Archivo de ${modo} detectado. Validando filas…`);
  document.getElementById('paso2-txt').textContent = `Detectado: archivo de ${modo}`;
  const porEan = new Map(catalogo.map(p => [p.ean, p]));
  const get = (r, k) => col[k] >= 0 ? String(r[col[k]] ?? '').trim() : '';
  const datos = tabla.slice(1).filter(r => r.some(c => String(c).trim()));

  if (modo === 'lotes') {
    let existentes = [];
    try { existentes = await api('/api/lotes'); }
    catch (e) { showaviso(e.message, 'error'); resetImport(); return; }
    const clave = (ean, fecha, cant) => `${ean}|${fecha}|${cant}`;
    const yaRegistrados = new Set(existentes.map(l => clave(l.ean, l.fechaVencimiento, l.cantidad)));
    const vistos = new Set();
    filas = datos.map((r, i) => {
      const f = { fila: i + 2, ean: get(r, 'ean'), cantidad: parseInt(get(r, 'cantidad')),
                  proveedor: get(r, 'proveedor'), fecha: fechaAISO(get(r, 'fecha')) };
      const costoTxt = get(r, 'precio').replace(',', '.');
      const prod = porEan.get(f.ean);
      f.costo = costoTxt === '' ? (prod ? +prod.precio : NaN) : parseFloat(costoTxt);
      f.nombre = prod?.nombre || '';
      f.proveedor ||= prod?.proveedor || '';
      if (f.fecha) { f.dias = diasHasta(f.fecha); f.estado = estadoPorDias(f.dias); }
      const k = clave(f.ean, f.fecha, f.cantidad);
      if (!/^\d{13}$/.test(f.ean))              { f.accion = 'rechazar'; f.motivo = 'EAN-13 inválido'; }
      else if (!prod)                           { f.accion = 'rechazar'; f.motivo = 'EAN no registrado en Productos'; }
      else if (!(f.cantidad > 0))               { f.accion = 'rechazar'; f.motivo = 'Cantidad inválida'; }
      else if (!f.fecha)                        { f.accion = 'rechazar'; f.motivo = 'Fecha inválida (usa AAAA-MM-DD o DD/MM/AAAA)'; }
      else if (!(f.costo >= 0))                 { f.accion = 'rechazar'; f.motivo = 'Costo inválido'; }
      else if (yaRegistrados.has(k))            { f.accion = 'rechazar'; f.motivo = 'Lote ya registrado'; }
      else if (vistos.has(k))                   { f.accion = 'rechazar'; f.motivo = 'Fila repetida en el archivo'; }
      else                                      { f.accion = 'insertar'; vistos.add(k); }
      return f;
    });
  } else {
    const vistos = new Set();
    const num = v => parseFloat(String(v ?? '').replace(',', '.'));
    filas = datos.map((r, i) => {
      const f = {
        fila: i + 2, ean: get(r, 'ean'), nombre: get(r, 'nombre'), categoria: get(r, 'categoria'),
        stock: parseInt(get(r, 'stock')) || 0, precio: num(get(r, 'precio')), stockMin: parseInt(get(r, 'stockMin')) || 0,
        proveedor: get(r, 'proveedor')
      };
      const existente = porEan.get(f.ean);
      if (!/^\d{13}$/.test(f.ean))        { f.accion = 'rechazar'; f.motivo = 'EAN-13 inválido'; }
      else if (vistos.has(f.ean))         { f.accion = 'rechazar'; f.motivo = 'EAN repetido en el archivo'; }
      else if (!f.nombre)                 { f.accion = 'rechazar'; f.motivo = 'Falta el nombre'; }
      else if (!(f.precio > 0))           { f.accion = 'rechazar'; f.motivo = 'Costo unitario inválido'; }
      else if (existente)                 { f.accion = 'actualizar'; f.id = existente.id; f.proveedor ||= existente.proveedor; f.categoria ||= existente.categoria; }
      else                                { f.accion = 'insertar'; }
      vistos.add(f.ean);
      return f;
    });
  }

  setProgreso(100, `${filas.length} filas validadas`);
  mostrarResultado();
}

/* ====== VISTA PREVIA ====== */
function mostrarResultado() {
  const cuenta = a => filas.filter(f => f.accion === a).length;
  const esLotes = modo === 'lotes';
  document.getElementById('lbl-ins').textContent = esLotes ? 'Lotes nuevos' : 'Insertados';
  document.getElementById('item-upd').style.display = esLotes ? 'none' : '';
  document.getElementById('r-ins').textContent = cuenta('insertar');
  document.getElementById('r-upd').textContent = cuenta('actualizar');
  document.getElementById('r-rej').textContent = cuenta('rechazar');
  document.getElementById('result-wrap').style.display = 'block';

  const etiqueta = { insertar:['etiqueta-ok', 'Nuevo'], actualizar:['etiqueta-actualizar', 'Actualizar'], rechazar:['etiqueta-rechazado', 'Rechazado'] };
  const estadoCelda = f => f.estado
    ? `<span class="insignia-estado badge-${semClass(f.estado)}"><span class="punto-insignia"></span>${f.estado} · ${f.dias} d</span>` : '—';
  const celdaEstado = f => {
    const [cls, txt] = etiqueta[f.accion];
    return `<td><span class="${cls}">${txt}</span>${f.motivo ? `<div class="small text-muted">${esc(f.motivo)}</div>` : ''}</td>`;
  };

  document.getElementById('tabla-prevista').innerHTML = esLotes
    ? `<thead class="table-light"><tr><th>Fila</th><th>EAN</th><th>Producto</th><th>Cant.</th><th>Costo</th><th>Vence</th><th>Semáforo</th><th>Estado</th></tr></thead>
       <tbody>${filas.slice(0, 20).map(f => `<tr>
         <td>${f.fila}</td><td class="celda-precio">${esc(f.ean || '—')}</td><td>${esc(f.nombre || '—')}</td>
         <td>${f.cantidad > 0 ? f.cantidad : '—'}</td><td>${f.costo >= 0 ? fmtMoney(f.costo) : '—'}</td>
         <td>${f.fecha ? fmtDate(f.fecha) : '—'}</td><td>${estadoCelda(f)}</td>${celdaEstado(f)}
       </tr>`).join('')}</tbody>`
    : `<thead class="table-light"><tr><th>Fila</th><th>EAN</th><th>Nombre</th><th>Categoría</th><th>Stock</th><th>Costo</th><th>Estado</th></tr></thead>
       <tbody>${filas.slice(0, 20).map(f => `<tr>
         <td>${f.fila}</td><td class="celda-precio">${esc(f.ean || '—')}</td><td>${esc(f.nombre || '—')}</td>
         <td>${esc(f.categoria || '—')}</td><td>${f.stock}</td><td>${f.precio > 0 ? fmtMoney(f.precio) : '—'}</td>${celdaEstado(f)}
       </tr>`).join('')}</tbody>`;
}

/* ====== CONFIRMAR ====== */
async function confirmImport() {
  const validas = filas.filter(f => f.accion !== 'rechazar');
  if (!validas.length) { showaviso('No hay filas válidas para importar.', 'error'); return; }
  setPaso(3);

  let ok = 0; const errores = [];
  for (const [i, f] of validas.entries()) {
    try {
      if (modo === 'lotes') {
        await api('/api/lotes', { method:'POST', body: {
          ean: f.ean, productoNombre: f.nombre, cantidad: f.cantidad, costoUnitario: f.costo,
          fechaVencimiento: f.fecha, proveedor: f.proveedor || null } });
      } else {
        const body = { ean: f.ean, nombre: f.nombre, categoria: f.categoria || null, proveedor: f.proveedor || null,
                       precio: f.precio, stockActual: f.stock, stockMinimo: f.stockMin };
        if (f.accion === 'actualizar') await api(`/api/productos/${f.id}`, { method:'PUT', body });
        else                           await api('/api/productos', { method:'POST', body });
      }
      ok++;
    } catch (e) {
      errores.push(`Fila ${f.fila}: ${e.message}`);
    }
    setProgreso(((i + 1) / validas.length) * 100, `Guardando ${modo}… ${i + 1}/${validas.length}`);
  }

  if (errores.length) {
    showaviso(`${ok} guardados, ${errores.length} con error. ${errores[0]}`, 'error');
  } else {
    showaviso(`${ok} ${modo} importados correctamente.`, 'success');
    setTimeout(() => window.location.href = modo === 'lotes' ? 'semaforo.html' : 'productos.html', 1500);
  }
}

function resetImport() {
  filas = [];
  document.getElementById('file-input').value = '';
  document.getElementById('drop-title').textContent = 'Arrastra tu archivo .xlsx o .csv aquí';
  document.getElementById('paso2-txt').textContent = 'Verificar columnas y datos';
  document.getElementById('progress-wrap').style.display = 'none';
  document.getElementById('result-wrap').style.display = 'none';
  setPaso(1);
}

function downloadTemplate(tipo) {
  if (tipo === 'lotes') {
    const en = dias => { const d = new Date(); d.setDate(d.getDate() + dias); return aISO(d); };
    descargarCSV('plantilla_lotes.csv', [
      ['ean13', 'cantidad', 'costo_unitario', 'fecha_vencimiento', 'proveedor'],
      ['7751519000017', '48', '3.40', en(3), 'Gloria S.A.'],
      ['7501030451033', '12', '6.95', en(5), 'Bimbo Perú'],
      ['7754180010017', '36', '1.80', en(20), 'Laive S.A.'],
      ['7751007000012', '72', '6.80', en(28), 'Alicorp'],
      ['7751007005512', '50', '18.50', en(120), 'Costeño']
    ]);
  } else {
    descargarCSV('plantilla_productos.csv', [
      ['ean13', 'nombre', 'categoria', 'stock', 'costo_unitario', 'stock_minimo', 'proveedor'],
      ['7751519000017', 'Leche Gloria Entera 1L', 'Lácteos', '48', '3.40', '20', 'Gloria S.A.']
    ]);
  }
}
