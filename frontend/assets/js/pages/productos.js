/* CRUD de productos contra ms-productos (GET/POST/PUT/DELETE /api/productos) */
let catFilter   = 'TODAS';
let searchQuery = '';
let editId      = null;
let deleteId    = null;
let productos   = [];

const CATS = {
  'Lácteos':'🥛','Panadería':'🍞','Aceites':'🫙','Granos':'🌾','Abarrotes':'🛒',
  'Azúcares':'🍬','Fideos':'🍝','Condimentos':'🧂','Bebidas':'🥤',
  'Carnes':'🥩','Snacks':'🍿','Limpieza':'🧹'
};

function getFiltered() {
  return productos.filter(p => {
    const mc = catFilter === 'TODAS' || p.categoria === catFilter;
    const ms = !searchQuery || p.nombre.toLowerCase().includes(searchQuery) || p.ean.includes(searchQuery) ||
      (p.proveedor || '').toLowerCase().includes(searchQuery);
    return mc && ms;
  });
}

function render() {
  const filtered = getFiltered();
  document.getElementById('total-count').textContent = filtered.length;
  const grid = document.getElementById('grilla-productos');
  if (!filtered.length) {
    grid.innerHTML = `<div class="sin-datos" style="grid-column:1/-1;padding:40px 0"><p>No se encontraron productos.</p></div>`;
    return;
  }
  grid.innerHTML = filtered.map(p => {
    const lowStock = p.stockActual <= p.stockMinimo;
    return `<div class="tarjeta-producto">
      <div class="imagen-producto">${CATS[p.categoria] || '📦'}</div>
      <div class="info-producto">
        <div class="nombre-producto">${esc(p.nombre)}${lowStock ? '<span class="insignia-bajo-stock">Stock bajo</span>' : ''}</div>
        <div class="codigo-producto">${esc(p.ean)}</div>
        <div class="meta-producto">
          <span class="categoria-producto">${esc(p.categoria || 'Sin categoría')}</span>
          <span class="proveedor-producto">${esc(p.proveedor || '—')}</span>
        </div>
        <div class="pie-producto">
          <span class="precio-producto">${fmtMoney(p.precio)}</span>
          <span class="stock-producto ${lowStock ? 'low' : ''}">Stock: ${p.stockActual} / mín ${p.stockMinimo ?? 0}</span>
        </div>
        <div class="acciones-producto">
          <button class="boton-borde boton-chico" onclick="editProd(${p.id})">Editar</button>
          <button class="boton-borde boton-chico" style="color:var(--sem-red)" onclick="askDelete(${p.id})">Eliminar</button>
        </div>
      </div>
    </div>`;
  }).join('');
}

function setCat(cat) {
  catFilter = cat;
  document.querySelectorAll('.chip-categoria').forEach(c => c.classList.toggle('active', c.dataset.cat === cat));
  render();
}

function filterProds(val) {
  searchQuery = val.toLowerCase();
  render();
}

async function cargarProductos() {
  try {
    productos = await api('/api/productos');
    render();
  } catch (e) {
    document.getElementById('grilla-productos').innerHTML =
      `<div class="sin-datos" style="grid-column:1/-1;padding:40px 0"><p>${esc(e.message)}</p></div>`;
  }
}

async function cargarProveedores() {
  try {
    const provs = await api('/api/proveedores');
    document.getElementById('proveedor-producto').innerHTML = '<option value="">— Seleccionar proveedor —</option>' +
      provs.map(p => `<option value="${esc(p.nombre)}">${esc(p.nombre)}</option>`).join('');
  } catch { /* sin proveedores registrados */ }
}

function openNew() {
  editId = null;
  document.getElementById('modal-title').textContent = 'Nuevo producto';
  document.getElementById('prod-form').reset();
  document.getElementById('ean-error').classList.remove('visible');
  openModal('prod-modal');
}

function editProd(id) {
  const p = productos.find(x => x.id === id);
  if (!p) return;
  editId = id;
  document.getElementById('modal-title').textContent = 'Editar producto';
  document.getElementById('codigo-producto').value    = p.ean;
  document.getElementById('prod-nombre').value        = p.nombre;
  document.getElementById('categoria-producto').value = p.categoria || '';
  const prov = document.getElementById('proveedor-producto');
  if (p.proveedor && ![...prov.options].some(o => o.value === p.proveedor)) prov.add(new Option(p.proveedor, p.proveedor));
  prov.value = p.proveedor || '';
  document.getElementById('prod-precio').value       = p.precio;
  document.getElementById('stock-producto').value    = p.stockActual;
  document.getElementById('stock-productomin').value = p.stockMinimo ?? 0;
  openModal('prod-modal');
}

async function saveProd() {
  const ean      = document.getElementById('codigo-producto').value.trim();
  const nombre   = document.getElementById('prod-nombre').value.trim();
  const cat      = document.getElementById('categoria-producto').value;
  const prov     = document.getElementById('proveedor-producto').value;
  const precio   = +document.getElementById('prod-precio').value;
  const stock    = +document.getElementById('stock-producto').value || 0;
  const stockMin = +document.getElementById('stock-productomin').value || 0;

  const okEan = /^\d{13}$/.test(ean);
  document.getElementById('ean-error').classList.toggle('visible', !okEan);
  document.getElementById('codigo-producto').classList.toggle('campo-invalido', !okEan);
  if (!okEan) return;
  if (!nombre || !cat || !prov || !precio) { showaviso('Completa todos los campos.', 'error'); return; }

  const body = { ean, nombre, categoria: cat, proveedor: prov, precio, stockActual: stock, stockMinimo: stockMin };
  try {
    if (editId) await api(`/api/productos/${editId}`, { method:'PUT', body });
    else        await api('/api/productos', { method:'POST', body });
    showaviso(editId ? 'Producto actualizado.' : 'Producto registrado.', 'success');
    closeModal('prod-modal');
    cargarProductos();
  } catch (e) {
    showaviso(e.message, 'error');
  }
}

function askDelete(id) {
  deleteId = id;
  const p = productos.find(x => x.id === id);
  document.getElementById('delete-name').textContent = p?.nombre || '';
  openModal('delete-modal');
}

async function confirmDelete() {
  try {
    await api(`/api/productos/${deleteId}`, { method:'DELETE' });
    showaviso('Producto eliminado.', 'success');
    closeModal('delete-modal');
    cargarProductos();
  } catch (e) {
    showaviso(e.message, 'error');
  }
}

function exportCSV() {
  descargarCSV(`productos_${new Date().toISOString().slice(0, 10)}.csv`, [
    ['EAN', 'Nombre', 'Categoría', 'Proveedor', 'Precio', 'Stock', 'Stock mínimo'],
    ...getFiltered().map(p => [p.ean, p.nombre, p.categoria, p.proveedor, (+p.precio || 0).toFixed(2), p.stockActual, p.stockMinimo])
  ]);
  showaviso('CSV exportado.', 'success');
}

cargarProductos();
cargarProveedores();
