/* Historial de alertas de ms-alertas. Las alertas ROJO/AMARILLO/INFO las crea ms-alertas
   al consumir los eventos de RabbitMQ que publican ms-lotes y ms-cotizaciones. */
let currentFilter = 'TODAS';
let alertas = [];
const seleccionadas = new Set();

function getFiltered() {
  return currentFilter === 'TODAS' ? alertas : alertas.filter(a => a.tipo === currentFilter);
}

function render() {
  const filtered = getFiltered();
  document.getElementById('contador-alertas').textContent =
    `${filtered.length} de ${alertas.length} alerta${alertas.length !== 1 ? 's' : ''}`;
  const list = document.getElementById('lista-alertas');
  if (!filtered.length) {
    list.innerHTML = `<div class="sin-datos" style="padding:40px 0"><p>No hay alertas con ese filtro.</p></div>`;
    return;
  }
  list.innerHTML = filtered.map(a => {
    return `<div class="fila-alerta">
      <input type="checkbox" class="form-check-input m-0" style="flex-shrink:0" aria-label="Seleccionar alerta"
             ${seleccionadas.has(a.id) ? 'checked' : ''} onchange="toggleSeleccion(${a.id}, this.checked)">
      <div style="flex:1">
        <div class="mensaje-alerta">${esc(a.mensaje)}</div>
        <div class="meta-alerta"><span class="tipo-alerta tipo-${esc(a.tipo)}">${esc(a.tipo)}</span> · ${fmtRelative(a.enviadoAt)} · ${esc(a.fuente || 'sistema')}</div>
      </div>
      <button class="boton-borde boton-chico" title="Eliminar alerta" onclick="eliminarAlerta(${a.id})">✕</button>
    </div>`;
  }).join('');
  actualizarSeleccion();
}

/* ====== SELECCIÓN Y ENVÍO MANUAL A TELEGRAM ====== */
function toggleSeleccion(id, marcada) {
  if (marcada) seleccionadas.add(id); else seleccionadas.delete(id);
  actualizarSeleccion();
}

function seleccionarTodas(marcar) {
  getFiltered().forEach(a => marcar ? seleccionadas.add(a.id) : seleccionadas.delete(a.id));
  render();
}

function actualizarSeleccion() {
  const visibles = getFiltered();
  const marcadas = visibles.filter(a => seleccionadas.has(a.id)).length;
  const todas = document.getElementById('sel-todas');
  todas.checked = visibles.length > 0 && marcadas === visibles.length;
  todas.indeterminate = marcadas > 0 && marcadas < visibles.length;
  const boton = document.getElementById('btn-enviar-sel');
  boton.disabled = seleccionadas.size === 0;
  boton.textContent = seleccionadas.size ? `Enviar ${seleccionadas.size} a Telegram` : 'Enviar seleccionadas a Telegram';
}

async function enviarSeleccionadas() {
  const boton = document.getElementById('btn-enviar-sel');
  boton.disabled = true;
  try {
    const r = await api('/api/alertas/enviar', { method:'POST', body: { ids: [...seleccionadas] } });
    if (r.telegram === 'NO_CONFIGURADO') {
      showaviso('Telegram no está configurado en el servidor.', 'error');
    } else {
      showaviso(`${r.enviadas} alerta${r.enviadas !== 1 ? 's' : ''} enviada${r.enviadas !== 1 ? 's' : ''} a Telegram.`, 'success');
      seleccionadas.clear();
      render();
    }
  } catch (e) {
    showaviso(e.message, 'error');
  } finally {
    actualizarSeleccion();
  }
}

function setFilter(tipo) {
  currentFilter = tipo;
  seleccionadas.clear();   // no enviar alertas que quedaron ocultas por el filtro
  document.querySelectorAll('.chip-filtro').forEach(c => c.classList.toggle('active', c.dataset.f === tipo));
  render();
}

async function cargarAlertas() {
  try {
    alertas = await api('/api/alertas');
    render();
  } catch (e) {
    document.getElementById('lista-alertas').innerHTML =
      `<div class="sin-datos" style="padding:40px 0"><p>${esc(e.message)}</p></div>`;
  }
}

/* ====== RESUMEN AUTOMÁTICO (ms-lotes lo publica por RabbitMQ y ms-alertas lo envía) ====== */
async function enviarResumenAhora() {
  const boton = document.getElementById('btn-resumen');
  boton.disabled = true;
  try {
    const r = await api('/api/lotes/resumen', { method:'POST' });
    if (r.enviado) {
      showaviso(`Resumen enviado: ${r.rojo} en ROJO y ${r.amarillo} en AMARILLO.`, 'success');
      setTimeout(cargarAlertas, 1500);
    } else if (!r.rojo && !r.amarillo) {
      showaviso('No hay productos en ROJO ni AMARILLO: no se envió nada.', 'success');
    } else {
      showaviso('No se pudo publicar el resumen. ¿Está encendido RabbitMQ?', 'error');
    }
  } catch (e) {
    showaviso(e.message, 'error');
  } finally {
    boton.disabled = false;
  }
}

async function eliminarAlerta(id) {
  try {
    await api(`/api/alertas/${id}`, { method:'DELETE' });
    alertas = alertas.filter(a => a.id !== id);
    seleccionadas.delete(id);
    render();
  } catch (e) {
    showaviso(e.message, 'error');
  }
}

/* ====== ESTADO DE TELEGRAM (lo informa ms-alertas) ====== */
async function cargarEstadoTelegram() {
  const estado = document.getElementById('estado-telegram');
  const bot = document.getElementById('tg-bot');
  const chat = document.getElementById('tg-chat');
  try {
    const t = await api('/api/alertas/telegram/estado');
    if (!t.configurado) {
      bot.textContent = 'Sin configurar';
      estado.textContent = '● Desconectado';
      estado.className = 'estado-telegram';
    } else if (t.conectado) {
      bot.textContent = `Conectado a @${t.bot}`;
      estado.textContent = '● Conectado';
      estado.className = 'estado-telegram estado-conectado';
      chat.textContent = `Chat ID: ${t.chatId}`;
    } else {
      bot.textContent = 'No se pudo conectar con Telegram';
      estado.textContent = '● Error';
      estado.className = 'estado-telegram';
      chat.textContent = `Chat ID: ${t.chatId}`;
    }
  } catch (e) {
    bot.textContent = e.message;
    estado.textContent = '● Sin información';
  }
}

cargarAlertas();
cargarEstadoTelegram();
