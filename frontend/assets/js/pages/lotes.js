/* CRUD de lotes contra ms-lotes (GET/POST/PUT/DELETE /api/lotes).
   Al guardar un lote ROJO o AMARILLO, ms-lotes publica un evento en RabbitMQ que ms-alertas convierte en alerta. */
let lotes         = [];
let currentFilter = 'TODOS';
let searchQuery   = '';
let editId        = null;
let deleteId      = null;

function getFiltered() {
  return lotes.filter(l => {
    const mf = currentFilter === 'TODOS' || l.estado === currentFilter;
    const ms = !searchQuery || l.productoNombre.toLowerCase().includes(searchQuery) ||
      l.ean.includes(searchQuery) || (l.proveedor || '').toLowerCase().includes(searchQuery);
    return mf && ms;
  });
}

function render() {
  const filtered = getFiltered();
  document.getElementById('total-count').textContent = filtered.length;
  const tbody = document.getElementById('lotes-tbody');
  if (!filtered.length) {
    tbody.innerHTML = `<tr><td colspan="8"><div class="sin-datos"><p>No hay lotes con ese filtro.</p></div></td></tr>`;
    return;
  }
  tbody.innerHTML = filtered.map(l => {
    const cls = semClass(l.estado);
    return `<tr>
      <td><div class="celda-nombre">${esc(l.productoNombre)}</div><div class="celda-detalle">${esc(l.ean)}</div></td>
      <td>${esc(l.proveedor || '—')}</td>
      <td>${l.cantidad} und</td>
      <td>${fmtMoney(l.costoUnitario)}</td>
      <td>${fmtDate(l.fechaVencimiento)}</td>
      <td><strong class="text-${cls}">${l.diasRestantes}</strong></td>
      <td><span class="insignia-estado badge-${cls}"><span class="punto-insignia"></span>${l.estado}</span></td>
      <td style="display:flex;gap:6px">
        <button class="boton-borde boton-chico" onclick="editLote(${l.id})">Editar</button>
        <button class="boton-borde boton-chico" style="color:var(--sem-red)" onclick="askDelete(${l.id})">Eliminar</button>
      </td>
    </tr>`;
  }).join('');
}

function setFilter(estado) {
  currentFilter = estado;
  document.querySelectorAll('.chip-estado').forEach(c => c.classList.toggle('active', c.dataset.f === estado));
  render();
}

function filterTable(val) {
  searchQuery = val.toLowerCase();
  render();
}

async function cargarLotes() {
  try {
    lotes = await api('/api/lotes');
    render();
  } catch (e) {
    document.getElementById('lotes-tbody').innerHTML =
      `<tr><td colspan="8"><div class="sin-datos"><p>${esc(e.message)}</p></div></td></tr>`;
  }
}

async function cargarProveedores() {
  try {
    const provs = await api('/api/proveedores');
    document.getElementById('lote-prov').innerHTML = '<option value="">— Seleccionar proveedor —</option>' +
      provs.map(p => `<option value="${esc(p.nombre)}">${esc(p.nombre)}</option>`).join('');
  } catch { /* el select queda vacío; el proveedor es opcional */ }
}

// Completa nombre y proveedor desde el catálogo (ms-productos) al escribir el EAN
async function autocompletarProducto() {
  const ean = document.getElementById('lote-ean').value.trim();
  if (!/^\d{13}$/.test(ean)) return;
  try {
    const p = await api(`/api/productos/ean/${ean}`);
    const nombre = document.getElementById('lote-prod');
    if (!nombre.value) nombre.value = p.nombre;
    const prov = document.getElementById('lote-prov');
    if (!prov.value && p.proveedor) prov.value = p.proveedor;
  } catch { /* EAN no registrado: se completa a mano */ }
}

function openNew() {
  editId = null;
  document.getElementById('modal-title').textContent = 'Nuevo lote';
  document.getElementById('lote-form').reset();
  document.getElementById('ean-error').classList.remove('visible');
  openModal('lote-modal');
}

function editLote(id) {
  const l = lotes.find(x => x.id === id);
  if (!l) return;
  editId = id;
  document.getElementById('modal-title').textContent = 'Editar lote';
  document.getElementById('lote-ean').value   = l.ean;
  document.getElementById('lote-prod').value  = l.productoNombre;
  document.getElementById('lote-prov').value  = l.proveedor || '';
  document.getElementById('lote-qty').value   = l.cantidad;
  document.getElementById('lote-cost').value  = l.costoUnitario;
  document.getElementById('lote-fecha').value = l.fechaVencimiento;
  openModal('lote-modal');
}

async function saveLote() {
  const ean   = document.getElementById('lote-ean').value.trim();
  const prod  = document.getElementById('lote-prod').value.trim();
  const prov  = document.getElementById('lote-prov').value;
  const qty   = +document.getElementById('lote-qty').value;
  const cost  = +document.getElementById('lote-cost').value;
  const fecha = document.getElementById('lote-fecha').value;
  const okEan = /^\d{13}$/.test(ean);
  document.getElementById('ean-error').classList.toggle('visible', !okEan);
  document.getElementById('lote-ean').classList.toggle('campo-invalido', !okEan);
  if (!okEan) return;
  if (!prod || !qty || !cost || !fecha) { showaviso('Completa todos los campos.', 'error'); return; }

  const body = { ean, productoNombre: prod, proveedor: prov || null, cantidad: qty, costoUnitario: cost, fechaVencimiento: fecha };
  try {
    if (editId) await api(`/api/lotes/${editId}`, { method:'PUT', body });
    else        await api('/api/lotes', { method:'POST', body });
    showaviso(editId ? 'Lote actualizado.' : 'Lote registrado.', 'success');
    closeModal('lote-modal');
    cargarLotes();
  } catch (e) {
    showaviso(e.message, 'error');
  }
}

function askDelete(id) {
  deleteId = id;
  const l = lotes.find(x => x.id === id);
  document.getElementById('delete-name').textContent = l?.productoNombre || '';
  openModal('delete-modal');
}

async function confirmDelete() {
  try {
    await api(`/api/lotes/${deleteId}`, { method:'DELETE' });
    showaviso('Lote eliminado.', 'success');
    closeModal('delete-modal');
    cargarLotes();
  } catch (e) {
    showaviso(e.message, 'error');
  }
}

function exportCSV() {
  descargarCSV(`lotes_${new Date().toISOString().slice(0, 10)}.csv`, [
    ['Producto', 'EAN', 'Proveedor', 'Cantidad', 'Costo', 'Fecha Vencimiento', 'Días', 'Estado'],
    ...getFiltered().map(l => [l.productoNombre, l.ean, l.proveedor, l.cantidad,
      (+l.costoUnitario || 0).toFixed(2), l.fechaVencimiento, l.diasRestantes, l.estado])
  ]);
  showaviso('CSV exportado.', 'success');
}

(async function init() {
  await Promise.all([cargarLotes(), cargarProveedores()]);
  const params = new URLSearchParams(window.location.search);
  const ean = params.get('ean');
  if (ean) {
    document.getElementById('search').value = ean;
    filterTable(ean);
  }
  const id = +params.get('id');
  if (id) editLote(id);
})();
