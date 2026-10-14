/* Ranking de proveedores por producto: último precio confirmado de cada proveedor (ms-cotizaciones) */
let catalogo = [];

function vacio(msg) {
  document.getElementById('ranking-container').innerHTML =
    `<div class="sin-datos" style="padding:60px 0"><p>${esc(msg)}</p></div>`;
}

// Sin argumentos oculta las tarjetas (no hay ranking que mostrar)
function setKPIs(saving, avg) {
  const fila = document.getElementById('kpis-ranking');
  fila.style.display = saving === undefined ? 'none' : '';
  if (saving === undefined) return;
  document.getElementById('kpi-saving').textContent = saving;
  document.getElementById('kpi-avg').textContent    = avg;
}

async function loadRanking() {
  const ean = document.getElementById('prod-select').value;
  if (!ean) { vacio('Selecciona un producto para ver el ranking de precios.'); setKPIs(); return; }

  let provs;
  try {
    provs = await api(`/api/cotizaciones/ranking/${encodeURIComponent(ean)}`);
  } catch (e) {
    vacio(e.message); return;
  }
  const desde = document.getElementById('date-from').value;
  const hasta = document.getElementById('date-to').value;
  provs = provs.filter(p => (!desde || p.fecha >= desde) && (!hasta || p.fecha <= hasta));

  if (!provs.length) { vacio('No hay cotizaciones confirmadas de este producto en el rango elegido.'); setKPIs(); return; }

  const precios = provs.map(p => +p.precio);
  const maxP = Math.max(...precios);
  const minP = Math.min(...precios);
  setKPIs(`${fmtMoney(maxP - minP)} /und`, fmtMoney(precios.reduce((a, b) => a + b, 0) / precios.length));

  document.getElementById('ranking-container').innerHTML = provs.map((p, i) => {
    const isBest = i === 0;
    return `<div class="tarjeta-ranking ${isBest ? 'ranking-mejor' : ''}">
      <div class="fila-ranking-item">
        <div class="numero-ranking ${i < 3 ? `rank-${i + 1}` : 'ranking-otro'}">#${i + 1}</div>
        <div style="flex:1">
          <div class="nombre-rank">${esc(p.proveedor)}${isBest ? '<span class="insignia-ahorro insignia-mejor">Mejor precio</span>' : ''}</div>
          <div class="detalle-rank">${esc(p.presentacion || '')} · cotizado el ${fmtDate(p.fecha)}</div>
          <div class="envoltura-barra-precio"><div class="barra-precio" style="width:${Math.round((p.precio / maxP) * 100)}%"></div></div>
        </div>
        <div style="text-align:right">
          <div class="precio-rank ${isBest ? 'precio-mejor' : ''}">${fmtMoney(p.precio)}</div>
          ${!isBest ? `<div class="insignia-ahorro insignia-diferencia">+${fmtMoney(p.precio - minP)}</div>` : ''}
        </div>
      </div>
    </div>`;
  }).join('');
}

(async function init() {
  document.getElementById('date-to').value = new Date().toISOString().slice(0, 10);
  try {
    catalogo = await api('/api/productos');
  } catch (e) {
    vacio(e.message); return;
  }
  const sel = document.getElementById('prod-select');
  sel.innerHTML = '<option value="">— Seleccionar producto —</option>' +
    catalogo.map(p => `<option value="${esc(p.ean)}">${esc(p.nombre)}</option>`).join('');
  const ean = new URLSearchParams(window.location.search).get('ean');
  if (ean) sel.value = ean;
  loadRanking();
})();
