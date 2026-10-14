/* CRUD de proveedores contra ms-productos (/api/proveedores).
   Estadísticas: productos del catálogo (ms-productos) y ahorro del mes (ms-reportes). */
const POR_PAGINA = 10;
let pagina      = 1;
let vista       = 'tarjetas';   // 'tarjetas' | 'tabla'
let searchQuery = '';
let editId      = null;
let deleteId    = null;
let proveedores = [];
let productosPorProv = {};
let ahorroPorProv    = {};

function getInitials(nombre) {
  return nombre.split(' ').filter(Boolean).slice(0, 2).map(w => w[0]).join('').toUpperCase();
}

const ICON_USER  = '<svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>';
const ICON_MAIL  = '<svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/></svg>';
const ICON_PHONE = '<svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/></svg>';

function tarjetaHTML(p) {
  return `
    <div class="tarjeta-proveedor">
      <div class="avatar-proveedor">${esc(getInitials(p.nombre))}</div>
      <div class="nombre-proveedor">${esc(p.nombre)}</div>
      <div class="ruc-proveedor">RUC ${esc(p.ruc)}</div>
      <div class="meta-proveedor">
        <div class="meta-proveedor-row">${ICON_USER} ${esc(p.contacto || '—')}</div>
        <div class="meta-proveedor-row">${ICON_MAIL} ${esc(p.email || '—')}</div>
        <div class="meta-proveedor-row">${ICON_PHONE} ${esc(p.telefono || '—')}</div>
      </div>
      <div style="display:flex;gap:8px;margin-bottom:14px">
        <div class="estadistica-proveedor"><div class="estadistica-proveedor-num">${productosPorProv[p.nombre] || 0}</div><div class="estadistica-proveedor-lbl">Productos</div></div>
        <div class="estadistica-proveedor"><div class="estadistica-proveedor-num">S/ ${(+ahorroPorProv[p.nombre] || 0).toFixed(0)}</div><div class="estadistica-proveedor-lbl">Ahorro del mes</div></div>
      </div>
      <div class="acciones-proveedor">
        <button class="boton-borde boton-chico" style="flex:1" onclick="editProv(${p.id})">Editar</button>
        <button class="boton-borde boton-chico" style="color:var(--sem-red)" onclick="askDelete(${p.id})">Eliminar</button>
      </div>
    </div>`;
}

function filaHTML(p) {
  return `<tr>
      <td><div style="display:flex;align-items:center;gap:10px"><div class="avatar-proveedor" style="width:34px;height:34px;font-size:12px;margin:0">${esc(getInitials(p.nombre))}</div><strong>${esc(p.nombre)}</strong></div></td>
      <td class="celda-precio">${esc(p.ruc)}</td>
      <td>${esc(p.contacto || '—')}</td>
      <td>${esc(p.email || '—')}</td>
      <td>${esc(p.telefono || '—')}</td>
      <td class="text-end">${productosPorProv[p.nombre] || 0}</td>
      <td class="text-end celda-precio">S/ ${(+ahorroPorProv[p.nombre] || 0).toFixed(2)}</td>
      <td><div style="display:flex;gap:6px;justify-content:flex-end">
        <button class="boton-borde boton-chico" onclick="editProv(${p.id})">Editar</button>
        <button class="boton-borde boton-chico" style="color:var(--sem-red)" onclick="askDelete(${p.id})">Eliminar</button>
      </div></td>
    </tr>`;
}

function setVista(v) {
  vista = v;
  try { localStorage.setItem('sat_vista_proveedores', v); } catch { /* sin almacenamiento */ }
  document.querySelectorAll('.chip-filtro[data-vista]').forEach(c => c.classList.toggle('active', c.dataset.vista === v));
  render();
}

function render() {
  const filtered = proveedores.filter(p =>
    !searchQuery || p.nombre.toLowerCase().includes(searchQuery) || p.ruc.includes(searchQuery) ||
    (p.contacto || '').toLowerCase().includes(searchQuery)
  );
  document.getElementById('total-count').textContent = filtered.length;
  const grid  = document.getElementById('grilla-proveedores');
  const tabla = document.getElementById('tabla-proveedores');
  const enTabla = vista === 'tabla';
  grid.style.display  = enTabla ? 'none' : '';
  tabla.style.display = enTabla ? '' : 'none';

  if (!filtered.length) {
    const vacio = '<div class="sin-datos" style="padding:40px 0"><p>No se encontraron proveedores.</p></div>';
    grid.innerHTML = `<div style="grid-column:1/-1">${vacio}</div>`;
    document.getElementById('proveedores-tbody').innerHTML = `<tr><td colspan="8">${vacio}</td></tr>`;
    renderPaginacion('paginacion', 0, 1, POR_PAGINA, () => {});
    return;
  }
  pagina = Math.min(pagina, Math.ceil(filtered.length / POR_PAGINA));
  const visibles = filtered.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA);
  renderPaginacion('paginacion', filtered.length, pagina, POR_PAGINA, n => { pagina = n; render(); });
  if (enTabla) document.getElementById('proveedores-tbody').innerHTML = visibles.map(filaHTML).join('');
  else grid.innerHTML = visibles.map(tarjetaHTML).join('');
}

function filterProvs(val) {
  searchQuery = val.toLowerCase();
  pagina = 1;
  render();
}

async function cargar() {
  const [provs, prods, ahorro] = await Promise.allSettled([
    api('/api/proveedores'),
    api('/api/productos'),
    api(`/api/reportes/ahorro?mes=${ultimosMeses(1)[0]}`)
  ]);
  if (provs.status === 'rejected') {
    document.getElementById('grilla-proveedores').innerHTML =
      `<div class="sin-datos" style="grid-column:1/-1;padding:40px 0"><p>${esc(provs.reason.message)}</p></div>`;
    return;
  }
  proveedores = provs.value;
  productosPorProv = {};
  if (prods.status === 'fulfilled') prods.value.forEach(p => { productosPorProv[p.proveedor] = (productosPorProv[p.proveedor] || 0) + 1; });
  ahorroPorProv = ahorro.status === 'fulfilled' ? (ahorro.value.porProveedor || {}) : {};
  render();
}

function openNew() {
  editId = null;
  document.getElementById('modal-title').textContent = 'Nuevo proveedor';
  document.getElementById('prov-form').reset();
  openModal('prov-modal');
}

function editProv(id) {
  const p = proveedores.find(x => x.id === id);
  if (!p) return;
  editId = id;
  document.getElementById('modal-title').textContent = 'Editar proveedor';
  document.getElementById('prov-nombre').value   = p.nombre;
  document.getElementById('ruc-proveedor').value = p.ruc;
  document.getElementById('prov-contacto').value = p.contacto || '';
  document.getElementById('prov-email').value    = p.email || '';
  document.getElementById('prov-tel').value      = p.telefono || '';
  document.getElementById('prov-dir').value      = p.direccion || '';
  openModal('prov-modal');
}

async function saveProv() {
  const body = {
    nombre:    document.getElementById('prov-nombre').value.trim(),
    ruc:       document.getElementById('ruc-proveedor').value.trim(),
    contacto:  document.getElementById('prov-contacto').value.trim(),
    email:     document.getElementById('prov-email').value.trim(),
    telefono:  document.getElementById('prov-tel').value.trim(),
    direccion: document.getElementById('prov-dir').value.trim()
  };
  if (!body.nombre || !body.ruc) { showaviso('Nombre y RUC son obligatorios.', 'error'); return; }
  if (!/^\d{11}$/.test(body.ruc)) { showaviso('El RUC debe tener 11 dígitos.', 'error'); return; }

  try {
    if (editId) await api(`/api/proveedores/${editId}`, { method:'PUT', body });
    else        await api('/api/proveedores', { method:'POST', body });
    showaviso(editId ? 'Proveedor actualizado.' : 'Proveedor registrado.', 'success');
    closeModal('prov-modal');
    cargar();
  } catch (e) {
    showaviso(e.message, 'error');
  }
}

function askDelete(id) {
  deleteId = id;
  const p = proveedores.find(x => x.id === id);
  document.getElementById('delete-name').textContent = p?.nombre || '';
  openModal('delete-modal');
}

async function confirmDelete() {
  try {
    await api(`/api/proveedores/${deleteId}`, { method:'DELETE' });
    showaviso('Proveedor eliminado.', 'success');
    closeModal('delete-modal');
    cargar();
  } catch (e) {
    showaviso(e.message, 'error');
  }
}

try { if (localStorage.getItem('sat_vista_proveedores') === 'tabla') setVista('tabla'); } catch { /* sin almacenamiento */ }
cargar();
