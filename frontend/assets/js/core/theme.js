/* Tema claro / oscuro.
   Se carga en <head> para aplicar el tema antes de pintar la página y evitar el parpadeo. */
if (localStorage.getItem('sat_tema') === 'oscuro') {
  document.documentElement.setAttribute('data-tema', 'oscuro');
  document.documentElement.setAttribute('data-bs-theme', 'dark');
}

function toggleTema() {
  const html     = document.documentElement;
  const esOscuro = html.getAttribute('data-tema') === 'oscuro';
  if (esOscuro) {
    html.removeAttribute('data-tema');
    html.removeAttribute('data-bs-theme');
  } else {
    html.setAttribute('data-tema', 'oscuro');
    html.setAttribute('data-bs-theme', 'dark');
  }
  localStorage.setItem('sat_tema', esOscuro ? 'claro' : 'oscuro');
  actualizarIconoTema();
}

function actualizarIconoTema() {
  const esOscuro = document.documentElement.getAttribute('data-tema') === 'oscuro';
  const luna = document.getElementById('icono-luna');
  const sol  = document.getElementById('icono-sol');
  if (luna) luna.style.display = esOscuro ? 'none'  : 'block';
  if (sol)  sol.style.display  = esOscuro ? 'block' : 'none';
}

document.addEventListener('DOMContentLoaded', actualizarIconoTema);
