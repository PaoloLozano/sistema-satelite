/* ============================================================
   sidebar.js — Inserta partials/sidebar.html en <aside id="sidebar">
   El menú se edita en sidebar.html; este archivo solo lo carga.
   ============================================================ */
window.sidebarListo = (async function (rutaScript) {
  const barra = document.getElementById('sidebar');
  if (!barra) return;

  // La ruta se resuelve desde este script, así funciona desde cualquier carpeta de páginas
  const respuesta = await fetch(new URL('../../../partials/sidebar.html', rutaScript));
  const doc = new DOMParser().parseFromString(await respuesta.text(), 'text/html');
  // Live Server agrega su script de recarga a los .html que sirve
  doc.body.querySelectorAll('script').forEach(s => s.remove());
  barra.innerHTML = doc.body.innerHTML;

  const pagina = location.pathname.split('/').pop() || 'dashboard.html';
  barra.querySelector(`a[href="${pagina}"]`)?.classList.add('active');

  if (user.nombre) {
    document.getElementById('nombre-usuario').textContent = user.nombre;
    document.getElementById('avatar-usuarioatar').textContent =
      user.nombre.split(' ').filter(Boolean).map(p => p[0]).join('').slice(0, 2).toUpperCase();
  }
  if (user.rol) document.getElementById('rol-usuario').textContent = user.rol;

  const badge = document.getElementById('nb-rojo');
  if (badge) {
    badge.textContent = '';
    api('/api/lotes/semaforo/resumen')
      .then(r => { badge.textContent = r.ROJO || ''; badge.style.display = r.ROJO ? '' : 'none'; })
      .catch(() => { badge.style.display = 'none'; });
  }
})(document.currentScript.src);
