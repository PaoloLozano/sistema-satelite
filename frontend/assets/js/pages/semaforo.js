/* Semáforo de caducidad. La fecha de vencimiento es del lote (ms-lotes calcula ROJO/AMARILLO/VERDE),
   así que la vista "Por producto" agrupa los lotes de cada EAN y toma el más próximo a vencer. */
let allLotes      = [];
let vista         = 'producto';   // 'producto' | 'lote'
let currentFilter = 'TODOS';
let searchQuery   = '';

const THEAD = {
  producto: ['Producto', 'Proveedor', 'Cant. total', 'Lotes', 'Próx. vencimiento', 'Días restantes', 'Estado', 'Valor en riesgo', ''],
  lote:     ['Producto', 'Proveedor', 'Cant.', 'Costo unit.', 'Vencimiento', 'Días restantes', 'Estado', 'Valor en riesgo', '']
};

const enRiesgo = l => l.estado !== 'VERDE' ? l.cantidad * (l.costoUnitario || 0) : 0;

// Un registro por producto: el lote más próximo a vencer define su color y sus días
function agruparPorProducto(lotes) {
  const porEan = new Map();
  lotes.forEach(l => {
    if (!porEan.has(l.ean)) porEan.set(l.ean, []);
    porEan.get(l.ean).push(l);
  });
  return [...porEan.values()].map(grupo => {
    const proximo = grupo.reduce((a, b) => (b.diasRestantes < a.diasRestantes ? b : a));
    return {
      ean: proximo.ean,
      productoNombre: proximo.productoNombre,
      proveedor: proximo.proveedor,
      cantidad: grupo.reduce((s, l) => s + l.cantidad, 0),
      nLotes: grupo.length,
      fechaVencimiento: proximo.fechaVencimiento,
      diasRestantes: proximo.diasRestantes,
      estado: proximo.estado,
      valorRiesgo: grupo.reduce((s, l) => s + enRiesgo(l), 0)
    };
  });
}

function registros() {
  return vista === 'producto' ? agruparPorProducto(allLotes)
                              : allLotes.map(l => ({ ...l, valorRiesgo: enRiesgo(l) }));
}

function getFiltered() {
  return registros().filter(r => {
    const matchFilter = currentFilter === 'TODOS' || r.estado === currentFilter;
    const matchSearch = !searchQuery ||
      r.productoNombre.toLowerCase().includes(searchQuery) ||
      (r.proveedor || '').toLowerCase().includes(searchQuery) ||
      r.ean.includes(searchQuery);
    return matchFilter && matchSearch;
  }).sort((a, b) => a.diasRestantes - b.diasRestantes);
}

function renderKPIs() {
  const counts = { ROJO:0, AMARILLO:0, VERDE:0 };
  registros().forEach(r => counts[r.estado]++);
  document.getElementById('kpi-rojo').textContent     = counts.ROJO;
  document.getElementById('kpi-amarillo').textContent = counts.AMARILLO;
  document.getElementById('kpi-verde').textContent    = counts.VERDE;
  // Barra de proporción: cada tramo crece según cuántos registros hay en ese estado
  document.getElementById('prop-rojo').style.flexGrow     = counts.ROJO;
  document.getElementById('prop-amarillo').style.flexGrow = counts.AMARILLO;
  document.getElementById('prop-verde').style.flexGrow    = counts.VERDE;
  document.getElementById('contador-alertas').textContent =
    `${counts.ROJO} ${vista === 'producto' ? 'producto' : 'lote'}${counts.ROJO !== 1 ? 's' : ''} por vencer`;
  document.getElementById('alerta-barra').style.display = counts.ROJO ? '' : 'none';
}

function renderTable() {
  document.getElementById('sem-thead').innerHTML = `<tr>${THEAD[vista].map(h => `<th>${h}</th>`).join('')}</tr>`;
  const filtered = getFiltered();
  const unidad = vista === 'producto' ? 'producto' : 'lote';
  document.getElementById('total-count').textContent = `Mostrando ${filtered.length} ${unidad}${filtered.length !== 1 ? 's' : ''}`;
  document.getElementById('valor-total').textContent =
    `Valor en riesgo: ${fmtMoney(filtered.reduce((s, r) => s + r.valorRiesgo, 0))}`;

  const tbody = document.getElementById('lotes-tbody');
  if (!filtered.length) {
    tbody.innerHTML = `<tr><td colspan="9"><div class="sin-datos"><p>${allLotes.length ? 'No hay resultados con ese filtro.' : 'Aún no hay lotes registrados. Impórtalos o créalos en Lotes.'}</p></div></td></tr>`;
    return;
  }
  tbody.innerHTML = filtered.map(r => {
    const cls = semClass(r.estado);
    const cuarta = vista === 'producto' ? `${r.nLotes} lote${r.nLotes !== 1 ? 's' : ''}` : fmtMoney(r.costoUnitario);
    const destino = vista === 'producto' ? `lotes.html?ean=${encodeURIComponent(r.ean)}` : `lotes.html?id=${r.id}`;
    return `<tr>
      <td><div class="celda-nombre">${esc(r.productoNombre)}</div><div class="celda-detalle">${esc(r.ean)}</div></td>
      <td>${esc(r.proveedor || '—')}</td>
      <td>${r.cantidad} und</td>
      <td>${cuarta}</td>
      <td>${fmtDate(r.fechaVencimiento)}</td>
      <td><strong class="text-${cls}">${r.diasRestantes}</strong></td>
      <td><span class="insignia-estado badge-${cls}"><span class="punto-insignia"></span>${r.estado}</span></td>
      <td class="celda-precio">${r.valorRiesgo ? fmtMoney(r.valorRiesgo) : '—'}</td>
      <td><button class="boton-borde boton-chico" onclick="window.location='${destino}'">Ver</button></td>
    </tr>`;
  }).join('');
}

function setVista(v) {
  vista = v;
  document.querySelectorAll('.chip-filtro[data-vista]').forEach(c => c.classList.toggle('active', c.dataset.vista === v));
  document.getElementById('sem-desc').textContent = v === 'producto'
    ? 'Estado de cada producto según su lote más próximo a vencer'
    : 'Estado de cada lote registrado';
  renderKPIs();
  renderTable();
}

function setFilter(estado) {
  currentFilter = estado;
  document.querySelectorAll('.chip-filtro[data-filter]').forEach(c => c.classList.toggle('active', c.dataset.filter === estado));
  renderTable();
}

function filterTable(val) {
  searchQuery = val.toLowerCase();
  renderTable();
}

function exportCSV() {
  const cols = vista === 'producto'
    ? ['Producto', 'EAN', 'Proveedor', 'Cantidad total', 'Lotes', 'Próx. vencimiento', 'Días restantes', 'Estado', 'Valor en riesgo']
    : ['Producto', 'EAN', 'Proveedor', 'Cantidad', 'Costo', 'Vencimiento', 'Días restantes', 'Estado', 'Valor en riesgo'];
  descargarCSV(`semaforo_${vista}_${new Date().toISOString().slice(0, 10)}.csv`, [
    cols,
    ...getFiltered().map(r => [r.productoNombre, r.ean, r.proveedor, r.cantidad,
      vista === 'producto' ? r.nLotes : (+r.costoUnitario || 0).toFixed(2),
      r.fechaVencimiento, r.diasRestantes, r.estado, r.valorRiesgo.toFixed(2)])
  ]);
  showaviso('CSV exportado correctamente.', 'success');
}

async function load() {
  try {
    allLotes = await api('/api/lotes');
  } catch (e) {
    document.getElementById('lotes-tbody').innerHTML =
      `<tr><td colspan="9"><div class="sin-datos"><p>${esc(e.message)}</p></div></td></tr>`;
    return;
  }
  const params = new URLSearchParams(window.location.search);
  if (params.get('vista') === 'lote') setVista('lote'); else setVista('producto');
  const color = params.get('color')?.toUpperCase();
  if (['ROJO', 'AMARILLO', 'VERDE'].includes(color)) setFilter(color);
}
load();
