/* ====== REDIRECT IF ALREADY LOGGED IN ====== */
if (localStorage.getItem('sat_token')) window.location.href = 'dashboard.html';

/* ====== TOGGLE PASSWORD ====== */
document.getElementById('toggle-pw').addEventListener('click', () => {
  const inp  = document.getElementById('password');
  const icon = document.getElementById('eye-icon');
  if (inp.type === 'password') {
    inp.type = 'text';
    icon.innerHTML = `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
      d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 4.411m0 0L21 21"/>`;
  } else {
    inp.type = 'password';
    icon.innerHTML = `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
      d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
      d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>`;
  }
});

/* ====== FORM SUBMIT ====== */
document.getElementById('login-form').addEventListener('submit', async e => {
  e.preventDefault();
  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value;
  if (!username || !password) { showError('Completa todos los campos.'); return; }
  setLoading(true);
  try {
    const res = await fetch(`${API}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const data = await res.json().catch(() => ({}));
    if (res.ok && data.token) {
      localStorage.setItem('sat_token', data.token);
      localStorage.setItem('sat_user', JSON.stringify({
        nombre: data.nombre || data.username, username: data.username, rol: data.rol
      }));
      document.getElementById('etiqueta-acceso').style.display = 'flex';
      setTimeout(() => window.location.href = 'dashboard.html', 700);
    } else {
      showError(res.status === 401 ? 'Credenciales incorrectas. Intente nuevamente.' : (data.error || `Error ${res.status} del servidor.`));
      setLoading(false);
    }
  } catch {
    showError(`No se pudo conectar con el API Gateway (${API || location.host}). ¿Están levantados los microservicios?`);
    setLoading(false);
  }
});

/* ====== HELPERS ====== */
function setLoading(on) {
  const btn  = document.getElementById('submit-btn');
  const txt  = document.getElementById('btn-text');
  const spin = document.getElementById('btn-spinner');
  btn.disabled = on;
  txt.textContent = on ? 'Ingresando…' : 'Ingresar';
  spin.style.display = on ? 'block' : 'none';
}

let temporizadorError;

function ocultarError() {
  clearTimeout(temporizadorError);
  document.getElementById('error-global').style.display = 'none';
}

function showError(msg) {
  document.getElementById('mensaje-error').textContent = msg;
  document.getElementById('error-global').style.display = 'flex';
  document.getElementById('etiqueta-acceso').style.display = 'none';
  clearTimeout(temporizadorError);
  temporizadorError = setTimeout(ocultarError, 5000);
  const form = document.getElementById('login-form');
  form.classList.add('sacudir');
  setTimeout(() => form.classList.remove('sacudir'), 500);
}

/* ====== CLEAR ERROR ON INPUT ====== */
['username','password'].forEach(id => {
  document.getElementById(id).addEventListener('input', () => {
    ocultarError();
  });
});
