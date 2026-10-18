/* Gestión de usuarios contra ms-auth (/api/usuarios). Solo ADMIN: el backend responde 403 a otros roles
   y el menú oculta esta página; si otro rol entra por la URL, se le devuelve al dashboard. */
const esAdmin = user.rol === 'ADMIN';
if (!esAdmin) window.location.replace('dashboard.html');

let usuarios    = [];
let roleFilter  = '';
let searchQuery = '';
let editId      = null;
let deleteId    = null;

function getInitials(n) {
  return (n || '?').split(' ').filter(Boolean).slice(0, 2).map(w => w[0]).join('').toUpperCase();
}

function getFiltered() {
  return usuarios.filter(u => {
    const mr = !roleFilter || u.rol === roleFilter;
    const ms = !searchQuery || (u.nombre || '').toLowerCase().includes(searchQuery) || u.username.toLowerCase().includes(searchQuery);
    return mr && ms;
  });
}

function render() {
  const filtered = getFiltered();
  const tbody = document.getElementById('users-tbody');
  if (!filtered.length) {
    tbody.innerHTML = `<tr><td colspan="6"><div class="sin-datos" style="padding:28px 0"><p>No se encontraron usuarios.</p></div></td></tr>`;
    return;
  }
  tbody.innerHTML = filtered.map(u => {
    const roleCls = u.rol === 'ADMIN' ? 'insignia-admin' : u.rol === 'ANALISTA' ? 'insignia-analista' : 'insignia-operador';
    const statCls = u.enabled ? 'insignia-estado insignia-verde' : 'insignia-estado insignia-roja';
    return `<tr>
      <td>
        <div style="display:flex;align-items:center;gap:10px">
          <div class="avatar-usuario">${esc(getInitials(u.nombre))}</div>
          <div>
            <div style="font-weight:700;font-size:13.5px">${esc(u.nombre || u.username)}</div>
            <div style="font-size:12px;color:var(--text-muted)">@${esc(u.username)}</div>
          </div>
        </div>
      </td>
      <td><span class="${roleCls}">${u.rol}</span></td>
      <td><span class="${statCls}"><span class="punto-insignia"></span>${u.enabled ? 'ACTIVO' : 'INACTIVO'}</span></td>
      <td style="font-size:12.5px;color:var(--text-muted)">${fmtRelative(u.ultimoAcceso)}</td>
      <td>
        <div style="display:flex;gap:6px">
          <button class="boton-borde boton-chico" onclick="editUser(${u.id})">Editar</button>
          <button class="boton-borde boton-chico" style="color:var(--sem-red)" onclick="askDelete(${u.id})">Eliminar</button>
        </div>
      </td>
    </tr>`;
  }).join('');
}

function setRoleFilter(rol) {
  roleFilter = rol;
  document.getElementById('role-filter').value = rol;
  render();
}

function filterUsers(val) {
  searchQuery = val.toLowerCase();
  render();
}

async function cargarUsuarios() {
  if (!esAdmin) {
    document.getElementById('users-tbody').innerHTML =
      `<tr><td colspan="6"><div class="sin-datos" style="padding:28px 0"><p>Solo un ADMIN puede ver los usuarios.</p></div></td></tr>`;
    return;
  }
  try {
    usuarios = await api('/api/usuarios');
    render();
  } catch (e) {
    document.getElementById('users-tbody').innerHTML =
      `<tr><td colspan="6"><div class="sin-datos" style="padding:28px 0"><p>${esc(e.message)}</p></div></td></tr>`;
  }
}

function openNew() {
  editId = null;
  document.getElementById('modal-title').textContent = 'Nuevo usuario';
  document.getElementById('user-form').reset();
  document.getElementById('u-password').placeholder = 'Min. 8 caracteres';
  document.getElementById('pw-row').style.display = 'grid';
  openModal('user-modal');
}

function editUser(id) {
  const u = usuarios.find(x => x.id === id);
  if (!u) return;
  editId = id;
  document.getElementById('modal-title').textContent = 'Editar usuario';
  document.getElementById('u-nombre').value   = u.nombre || '';
  document.getElementById('u-username').value = u.username;
  document.getElementById('u-rol').value      = u.rol;
  document.getElementById('u-estado').value   = u.enabled ? 'ACTIVO' : 'INACTIVO';
  document.getElementById('u-password').value = '';
  document.getElementById('u-password').placeholder = 'Dejar vacío para no cambiarla';
  document.getElementById('pw-row').style.display = 'grid';
  openModal('user-modal');
}

async function saveUser() {
  const body = {
    nombre:   document.getElementById('u-nombre').value.trim(),
    username: document.getElementById('u-username').value.trim(),
    rol:      document.getElementById('u-rol').value,
    enabled:  document.getElementById('u-estado').value === 'ACTIVO',
    password: document.getElementById('u-password').value
  };
  if (!body.nombre || !body.username || !body.rol) { showaviso('Completa los campos obligatorios.', 'error'); return; }
  if (!editId && !body.password) { showaviso('La contraseña es obligatoria para nuevos usuarios.', 'error'); return; }

  try {
    if (editId) await api(`/api/usuarios/${editId}`, { method:'PUT', body });
    else        await api('/api/usuarios', { method:'POST', body });
    showaviso(editId ? 'Usuario actualizado.' : 'Usuario creado.', 'success');
    closeModal('user-modal');
    cargarUsuarios();
  } catch (e) {
    showaviso(e.message, 'error');
  }
}

function askDelete(id) {
  deleteId = id;
  const u = usuarios.find(x => x.id === id);
  document.getElementById('delete-name').textContent = u?.nombre || u?.username || '';
  openModal('delete-modal');
}

async function confirmDelete() {
  try {
    await api(`/api/usuarios/${deleteId}`, { method:'DELETE' });
    showaviso('Usuario eliminado.', 'success');
    closeModal('delete-modal');
    cargarUsuarios();
  } catch (e) {
    showaviso(e.message, 'error');
  }
}

cargarUsuarios();
