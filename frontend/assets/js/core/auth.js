/* Sesión del usuario: token JWT, protección de páginas y cliente HTTP hacia el API Gateway */
const token = localStorage.getItem('sat_token');
const user  = JSON.parse(localStorage.getItem('sat_user') || '{}');

if (!token) window.location.href = 'login.html';

function authHeaders() {
  return { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' };
}

/* api('/api/lotes')  ·  api('/api/lotes', { method:'POST', body:{...} })
   Devuelve el JSON de la respuesta o lanza Error con el mensaje del backend. */
async function api(path, { method = 'GET', body } = {}, intento = 0) {
  let res;
  try {
    res = await fetch(`${API}${path}`, {
      method,
      headers: authHeaders(),
      body: body !== undefined ? JSON.stringify(body) : undefined
    });
  } catch {
    throw new Error(`No se pudo conectar con el API Gateway (${API}).`);
  }
  if (res.status === 401) { logout(); throw new Error('Tu sesión expiró. Ingresa de nuevo.'); }
  if (res.status === 204) return null;

  const data = await res.json().catch(() => null);
  // 429 = límite de peticiones del gateway: se espera lo que indica el servidor y se reintenta
  if (res.status === 429 && intento < 3 && data?.retryAfter <= 30) {
    if (typeof showaviso === 'function') showaviso(`Demasiadas peticiones, reintentando en ${data.retryAfter} s…`, 'error');
    await new Promise(r => setTimeout(r, data.retryAfter * 1000));
    return api(path, { method, body }, intento + 1);
  }
  if (!res.ok) {
    const detalle = data?.errors ? Object.values(data.errors)[0] : null;
    throw new Error(detalle || data?.error || data?.message || `Error ${res.status} en ${path}`);
  }
  return data;
}

function logout() {
  localStorage.removeItem('sat_token');
  localStorage.removeItem('sat_user');
  window.location.href = 'login.html';
}
