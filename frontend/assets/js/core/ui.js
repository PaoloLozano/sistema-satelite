/* Interfaz compartida: avisos, modales y fecha del día */

function showaviso(msg, type = 'success') {
  let c = document.getElementById('contenedor-avisos');
  if (!c) { c = document.createElement('div'); c.id = 'contenedor-avisos'; document.body.appendChild(c); }
  const t = document.createElement('div');
  t.className = `aviso aviso-${type === 'success' ? 'exito' : type}`;
  t.textContent = msg;
  c.appendChild(t);
  setTimeout(() => t.remove(), 3800);
}

function openModal(id)  { document.getElementById(id)?.classList.add('open');    document.body.style.overflow = 'hidden'; }
function closeModal(id) { document.getElementById(id)?.classList.remove('open'); document.body.style.overflow = ''; }

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') document.querySelectorAll('.fondo-modal.open').forEach(m => closeModal(m.id));
});

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.fondo-modal').forEach(fondo => {
    fondo.addEventListener('click', e => { if (e.target === fondo) closeModal(fondo.id); });
  });

  const fecha = document.getElementById('today-date');
  if (fecha) fecha.textContent = new Date().toLocaleDateString('es-PE', { weekday:'long', year:'numeric', month:'long', day:'numeric' });
});

/* Paginación: dibuja "Mostrando X–Y de N" y los botones de página dentro del contenedor indicado.
   No dibuja nada si todo cabe en una sola página. */
function renderPaginacion(idContenedor, total, pagina, tamano, alCambiar) {
  const cont = document.getElementById(idContenedor);
  const totalPaginas = Math.ceil(total / tamano);
  if (totalPaginas <= 1) { cont.innerHTML = ''; return; }

  const desde = (pagina - 1) * tamano + 1;
  const hasta = Math.min(pagina * tamano, total);
  const boton = (n, texto, deshabilitado = false, activo = false) =>
    `<li class="page-item${activo ? ' active' : ''}${deshabilitado ? ' disabled' : ''}">` +
    `<button type="button" class="page-link" data-pag="${n}"${activo ? ' aria-current="page"' : ''}>${texto}</button></li>`;

  let botones = boton(pagina - 1, 'Anterior', pagina === 1);
  for (let n = 1; n <= totalPaginas; n++) botones += boton(n, n, false, n === pagina);
  botones += boton(pagina + 1, 'Siguiente', pagina === totalPaginas);

  cont.innerHTML = `<span class="small text-muted">Mostrando ${desde}–${hasta} de ${total}</span>` +
    `<nav aria-label="Paginación"><ul class="pagination pagination-sm mb-0">${botones}</ul></nav>`;
  cont.querySelectorAll('button[data-pag]').forEach(b =>
    b.addEventListener('click', () => alCambiar(+b.dataset.pag)));
}
