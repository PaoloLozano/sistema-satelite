/* Cotizaciones: se lee el PDF en el navegador (pdf.js), se revisan los precios y se envía a ms-cotizaciones.
   ms-cotizaciones completa el precio actual de cada EAN llamando a ms-productos con OpenFeign y,
   al confirmar, publica "cotizacion.procesada" en RabbitMQ (ms-alertas la registra como alerta INFO). */
pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';

let catalogo    = [];   // productos de ms-productos
let proveedores = [];   // proveedores de ms-productos
let items       = [];   // filas de la cotización en edición
let archivo     = '';

const pdfDrop = document.getElementById('zona-pdf');
pdfDrop.addEventListener('dragover', e => { e.preventDefault(); pdfDrop.classList.add('arrastrando'); });
pdfDrop.addEventListener('dragleave', () => pdfDrop.classList.remove('arrastrando'));
pdfDrop.addEventListener('drop', e => {
  e.preventDefault(); pdfDrop.classList.remove('arrastrando');
  if (e.dataTransfer.files[0]) loadPDF(e.dataTransfer.files[0]);
});
document.getElementById('pdf-input').addEventListener('change', e => {
  if (e.target.files[0]) loadPDF(e.target.files[0]);
});

/* ====== LECTURA DEL PDF ====== */
async function extraerLineas(file) {
  const pdf = await pdfjsLib.getDocument({ data: await file.arrayBuffer() }).promise;
  const lineas = [];
  for (let n = 1; n <= pdf.numPages; n++) {
    const { items: textos } = await (await pdf.getPage(n)).getTextContent();
    const filas = new Map();
    textos.forEach(t => {
      const y = Math.round(t.transform[5]);
      if (!filas.has(y)) filas.set(y, []);
      filas.get(y).push(t);
    });
    [...filas.entries()].sort((a, b) => b[0] - a[0]).forEach(([, ts]) =>
      lineas.push(ts.sort((a, b) => a.transform[4] - b.transform[4]).map(t => t.str).join(' ')));
  }
  return lineas;
}

// Una línea con un EAN-13 y un precio (ej. "7751519000017  Leche Gloria 1L  Caja x12  3.20") se toma como ítem
// "Caja x 12", "Bolsa x 20", "Saco x 4", "Paquete x 6 kg"…: un envase seguido opcionalmente de "x cantidad" y unidad
const PRESENTACION = /\b(?:caja|bolsa|saco|fardo|paquete|pack|display|docena|unidad|botella|lata|bandeja|balde|cubeta)\b(?:\s*x\s*\d+(?:[.,]\d+)?(?:\s*(?:kg|g|ml|l|und|u)\b)?)?/i;

// Una línea con un EAN-13 y un precio es un producto; la presentación se toma del texto entre ambos
function parsearItems(lineas) {
  const encontrados = new Map();
  lineas.forEach(linea => {
    const ean = linea.match(/\b(\d{13})\b/)?.[1];
    if (!ean || encontrados.has(ean)) return;
    const sinEan = linea.replace(ean, '');
    const precios = [...sinEan.matchAll(/(\d+[.,]\d{2})(?!\d)/g)].map(m => parseFloat(m[1].replace(',', '.')));
    if (!precios.length) return;
    const prod = catalogo.find(p => p.ean === ean);
    const presentacion = (sinEan.match(PRESENTACION)?.[0] || '').replace(/\s+/g, ' ').trim();
    const texto = sinEan.replace(PRESENTACION, '').replace(/(S\/\.?\s*)?\d+[.,]\d{2}/g, '').replace(/\s+/g, ' ').trim();
    encontrados.set(ean, {
      ean,
      productoNombre: prod?.nombre || texto || ean,
      presentacion,
      precioActual: prod ? +prod.precio : null,
      precioCotizacion: precios[precios.length - 1]
    });
  });
  return [...encontrados.values()];
}

function detectarProveedor(lineas) {
  const texto = lineas.join(' ').toLowerCase();
  return proveedores.find(p => texto.includes(p.ruc)) ||
         proveedores.find(p => texto.includes(p.nombre.toLowerCase()));
}

async function loadPDF(file) {
  if (!file.name.toLowerCase().endsWith('.pdf')) { showaviso('Solo se aceptan archivos PDF.', 'error'); return; }
  if (file.size > 5 * 1024 * 1024) { showaviso('El PDF supera los 5 MB.', 'error'); return; }
  archivo = file.name;
  document.getElementById('zona-pdf').style.display    = 'none';
  document.getElementById('pdf-loading').style.display = 'block';

  let lineas = [];
  try {
    lineas = await extraerLineas(file);
  } catch {
    showaviso('No se pudo leer el PDF. Puedes agregar los productos manualmente.', 'error');
  }
  items = parsearItems(lineas);
  const prov = detectarProveedor(lineas);
  document.getElementById('pdf-proveedor').value = prov ? prov.nombre : '';

  document.getElementById('pdf-loading').style.display = 'none';
  document.getElementById('pdf-result').style.display  = 'block';
  document.getElementById('pdf-filename').textContent  = file.name;
  document.getElementById('pdf-fecha').textContent     = new Date().toLocaleDateString('es-PE');
  if (!items.length) showaviso('No se detectaron EAN con precio en el PDF. Agrega los productos manualmente.', 'error');
  renderTable();
}

/* ====== TABLA EDITABLE ====== */
function renderTable() {
  document.getElementById('pdf-items').textContent = items.length;
  const tbody = document.getElementById('cotiz-tbody');
  if (!items.length) {
    tbody.innerHTML = '<tr><td colspan="6"><div class="sin-datos" style="padding:24px 0"><p>Sin productos. Usa "Agregar producto".</p></div></td></tr>';
    return;
  }
  tbody.innerHTML = items.map((item, i) => {
    let badge = '<span class="insignia-nuevo">Nuevo</span>';
    if (item.precioActual != null && item.precioCotizacion) {
      const diff = item.precioCotizacion - item.precioActual;
      const pct  = item.precioActual ? ((diff / item.precioActual) * 100).toFixed(1) : '0.0';
      if (diff > 0)      badge = `<span class="insignia-sube">▲ S/ ${diff.toFixed(2)} (+${pct}%)</span>`;
      else if (diff < 0) badge = `<span class="insignia-baja">▼ S/ ${Math.abs(diff).toFixed(2)} (${pct}%)</span>`;
      else               badge = `<span class="insignia-nuevo">Sin cambio</span>`;
    }
    return `<tr id="fila-${esc(item.ean)}">
      <td><div class="celda-nombre">${esc(item.productoNombre)}</div><div class="celda-detalle">${esc(item.ean)}</div></td>
      <td><input class="form-control form-control-sm" value="${esc(item.presentacion)}" placeholder="Caja x 12"
           onchange="items[${i}].presentacion = this.value"></td>
      <td class="text-end">${item.precioActual != null ? fmtMoney(item.precioActual) : '—'}</td>
      <td class="text-end"><input class="entrada-precio" type="number" step="0.01" min="0"
           value="${item.precioCotizacion ? item.precioCotizacion.toFixed(2) : ''}" onchange="updatePrice(${i}, this.value)"></td>
      <td class="text-center">${badge}</td>
      <td><button class="boton-borde boton-chico" title="Quitar" onclick="quitarItem(${i})">✕</button></td>
    </tr>`;
  }).join('');
}

function updatePrice(i, val) {
  items[i].precioCotizacion = parseFloat(val) || 0;
  renderTable();
}

function quitarItem(i) {
  items.splice(i, 1);
  renderTable();
}

// Lleva la vista a la fila de ese EAN y la resalta un momento
function resaltarFila(ean) {
  const fila = document.getElementById(`fila-${ean}`);
  if (!fila) return;
  fila.scrollIntoView({ behavior: 'smooth', block: 'center' });
  fila.classList.add('fila-resaltada');
  setTimeout(() => fila.classList.remove('fila-resaltada'), 2200);
}

function agregarProducto() {
  const select = document.getElementById('agregar-producto');
  const ean = select.value;
  if (!ean) { showaviso('Elige un producto de la lista y luego pulsa Agregar.', 'error'); return; }
  const p = catalogo.find(x => x.ean === ean);
  if (items.some(x => x.ean === ean)) {
    showaviso(`"${p.nombre}" ya está en la cotización. Edita su precio en la tabla.`, 'error');
    resaltarFila(ean);
    return;
  }
  items.push({ ean, productoNombre: p.nombre, presentacion: '', precioActual: +p.precio, precioCotizacion: 0 });
  select.value = '';
  renderTable();
  showaviso(`"${p.nombre}" agregado. Escribe el precio cotizado.`, 'success');
  resaltarFila(ean);
  const campo = document.querySelector(`#fila-${CSS.escape(ean)} .entrada-precio`);
  if (campo) campo.focus({ preventScroll: true });
}

async function confirmCotizacion() {
  const proveedorNombre = document.getElementById('pdf-proveedor').value;
  const validos = items.filter(x => x.precioCotizacion > 0);
  if (!proveedorNombre) { showaviso('Selecciona el proveedor de la cotización.', 'error'); return; }
  if (!validos.length)  { showaviso('Ingresa al menos un precio.', 'error'); return; }

  try {
    const cot = await api('/api/cotizaciones', { method:'POST', body: {
      proveedorNombre,
      nombreArchivo: archivo,
      items: validos.map(x => ({ ean: x.ean, productoNombre: x.productoNombre, presentacion: x.presentacion, precioCotizacion: x.precioCotizacion }))
    }});
    await api(`/api/cotizaciones/${cot.id}/confirmar`, { method:'POST' });
    showaviso('Cotización confirmada. Redirigiendo al ranking…', 'success');
    setTimeout(() => window.location.href = `ranking.html?ean=${validos[0].ean}`, 1500);
  } catch (e) {
    showaviso(e.message, 'error');
  }
}

function resetPDF() {
  items = [];
  document.getElementById('zona-pdf').style.display   = 'block';
  document.getElementById('pdf-result').style.display = 'none';
  document.getElementById('pdf-input').value = '';
}

/* ====== HISTORIAL ====== */
async function cargarHistorial() {
  const cont = document.getElementById('lista-historial');
  try {
    const cots = (await api('/api/cotizaciones'))
      .sort((a, b) => (b.creadoEn || '').localeCompare(a.creadoEn || '')).slice(0, 8);
    cont.innerHTML = cots.length ? cots.map(c => `
      <div class="fila-historial">
        <div>
          <div style="font-weight:700;font-size:14px">${esc(c.proveedorNombre)}</div>
          <div style="font-size:12px;color:var(--text-muted)">${fmtDate(c.fechaCotizacion)} · ${c.items.length} productos</div>
        </div>
        <span class="insignia-nuevo">${esc(c.estado)}</span>
      </div>`).join('')
      : '<div class="sin-datos" style="padding:20px 0"><p>Aún no hay cotizaciones.</p></div>';
  } catch (e) {
    cont.innerHTML = `<div class="sin-datos" style="padding:20px 0"><p>${esc(e.message)}</p></div>`;
  }
}

(async function init() {
  const [prods, provs] = await Promise.allSettled([api('/api/productos'), api('/api/proveedores')]);
  if (prods.status === 'fulfilled') {
    catalogo = prods.value;
    document.getElementById('agregar-producto').innerHTML = '<option value="">— Agregar un producto a la cotización —</option>' +
      catalogo.map(p => `<option value="${esc(p.ean)}">${esc(p.nombre)} (${esc(p.ean)})</option>`).join('');
  }
  if (provs.status === 'fulfilled') {
    proveedores = provs.value;
    document.getElementById('pdf-proveedor').innerHTML = '<option value="">— Proveedor —</option>' +
      proveedores.map(p => `<option value="${esc(p.nombre)}">${esc(p.nombre)}</option>`).join('');
  }
  cargarHistorial();
})();
