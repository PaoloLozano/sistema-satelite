/* Formato de fechas, montos, estados del semáforo y escape de HTML */

// Las fechas "YYYY-MM-DD" se leen como fecha local (new Date() las tomaría como UTC y restaría un día en Lima)
function parseFecha(v) {
  if (typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v)) {
    const [y, m, d] = v.split('-').map(Number);
    return new Date(y, m - 1, d);
  }
  return new Date(v);
}

function fmtDate(v) {
  return parseFecha(v).toLocaleDateString('es-PE', { day:'2-digit', month:'short', year:'numeric' });
}

function fmtRelative(v) {
  if (!v) return 'Nunca';
  const fecha = parseFecha(v);
  const min = Math.round((Date.now() - fecha) / 60000);
  const hora = fecha.toLocaleTimeString('es-PE', { hour:'2-digit', minute:'2-digit' });
  if (min < 1)  return 'Hace un momento';
  if (min < 60) return `Hace ${min} min`;
  const hoy = new Date(); hoy.setHours(0, 0, 0, 0);
  if (fecha >= hoy) return `Hoy ${hora}`;
  if (fecha >= hoy - 86400000) return `Ayer ${hora}`;
  return `${fmtDate(v)} ${hora}`;
}

function fmtMoney(n) { return `S/ ${(+n || 0).toFixed(2)}`; }

function semClass(estado) { return estado === 'ROJO' ? 'red' : estado === 'AMARILLO' ? 'yellow' : 'green'; }

function esc(v) {
  return String(v ?? '').replace(/[&<>"']/g, c => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;' }[c]));
}

// Mes actual y anteriores en formato YYYY-MM (el que esperan ms-reportes y ms-cotizaciones)
function ultimosMeses(n) {
  const out = [];
  const d = new Date(); d.setDate(1);
  for (let i = 0; i < n; i++) {
    out.unshift(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
    d.setMonth(d.getMonth() - 1);
  }
  return out;
}

function descargarCSV(nombre, filas) {
  const csv  = filas.map(r => r.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(',')).join('\n');
  const blob = new Blob(['﻿' + csv], { type:'text/csv;charset=utf-8;' });
  const url  = URL.createObjectURL(blob);
  const a    = document.createElement('a'); a.href = url; a.download = nombre; a.click();
  URL.revokeObjectURL(url);
}
