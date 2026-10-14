/* Dashboard: combina ms-reportes (KPIs vía OpenFeign), ms-lotes, ms-alertas, ms-productos y ms-cotizaciones */

function renderSemaforo(rojo, amarillo, verde) {
  const total = rojo + amarillo + verde;
  document.getElementById('sem-red').textContent    = rojo;
  document.getElementById('sem-yellow').textContent = amarillo;
  document.getElementById('sem-green').textContent  = verde;
  const pct = n => total ? `${(n / total) * 100}%` : '0%';
  document.getElementById('tramo-rojo').style.width     = pct(rojo);
  document.getElementById('tramo-amarillo').style.width = pct(amarillo);
  document.getElementById('tramo-verde').style.width    = pct(verde);

  document.getElementById('contador-alertas').textContent = `${rojo} lote${rojo !== 1 ? 's' : ''} por vencer`;
  document.getElementById('alerta-barra').style.display = rojo ? '' : 'none';
}

function renderCriticalTable(lotes) {
  const tbody = document.getElementById('critical-tbody');
  const rojos = lotes.filter(l => l.estado === 'ROJO').sort((a, b) => a.diasRestantes - b.diasRestantes).slice(0, 8);
  if (!rojos.length) {
    tbody.innerHTML = `<tr><td colspan="8"><div class="sin-datos" style="padding:28px 0">
      <p>No hay lotes en estado ROJO.</p></div></td></tr>`;
    return;
  }
  tbody.innerHTML = rojos.map(l => `<tr>
      <td><div class="celda-nombre">${esc(l.productoNombre)}</div><div class="celda-detalle">${esc(l.ean)}</div></td>
      <td>${esc(l.proveedor || '—')}</td>
      <td>${l.cantidad} und</td>
      <td>${fmtDate(l.fechaVencimiento)}</td>
      <td><strong class="text-red">${l.diasRestantes}</strong></td>
      <td><span class="insignia-estado badge-red"><span class="punto-insignia"></span>ROJO</span></td>
      <td class="celda-precio">${fmtMoney(l.cantidad * (l.costoUnitario || 0))}</td>
      <td><button class="boton-borde boton-chico" onclick="window.location='lotes.html?id=${l.id}'">Ver</button></td>
    </tr>`).join('');
}

function renderActivity(alertas) {
  const feed = document.getElementById('activity-feed');
  if (!alertas.length) {
    feed.innerHTML = '<div class="sin-datos" style="padding:28px 0"><p>Sin actividad reciente.</p></div>';
    return;
  }
  feed.innerHTML = alertas.slice(0, 5).map(a => {
    return `<div class="item-actividad">
      <div><div class="texto-actividad">${esc(a.mensaje)}</div>
      <div class="tiempo-actividad"><span class="tipo-alerta tipo-${esc(a.tipo)}">${esc(a.tipo)}</span> · ${fmtRelative(a.enviadoAt)} · ${esc(a.fuente || 'sistema')}</div></div>
    </div>`;
  }).join('');
}

function errorEn(id, msg) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = `<div class="sin-datos" style="padding:20px 0"><p>${esc(msg)}</p></div>`;
}

async function loadDashboard() {
  const [kpis, lotes, alertas, ahorro, productos, proveedores] = await Promise.allSettled([
    api('/api/reportes/dashboard/kpis'),
    api('/api/lotes'),
    api('/api/alertas'),
    api(`/api/reportes/ahorro?mes=${ultimosMeses(1)[0]}`),
    api('/api/productos'),
    api('/api/proveedores')
  ]);

  if (kpis.status === 'fulfilled') {
    const k = kpis.value;
    renderSemaforo(+k.lotesRojo || 0, +k.lotesAmarillo || 0, +k.lotesVerde || 0);
    document.getElementById('kpi-productos').textContent = (+k.totalProductos || 0).toLocaleString('es-PE');
    document.getElementById('kpi-prov').textContent      = k.totalProveedores ?? 0;
  } else {
    showaviso(`KPIs: ${kpis.reason.message}`, 'error');
  }

  if (lotes.status === 'fulfilled') {
    renderCriticalTable(lotes.value);
    const riesgo = lotes.value.filter(l => l.estado === 'ROJO')
      .reduce((s, l) => s + l.cantidad * (l.costoUnitario || 0), 0);
    document.getElementById('kpi-riesgo').textContent = `S/ ${Math.round(riesgo).toLocaleString('es-PE')}`;
  } else {
    document.getElementById('critical-tbody').innerHTML =
      `<tr><td colspan="8"><div class="sin-datos"><p>${esc(lotes.reason.message)}</p></div></td></tr>`;
  }


  document.getElementById('kpi-ahorro').textContent = ahorro.status === 'fulfilled'
    ? `S/ ${Math.round(ahorro.value.ahorroTotal || 0).toLocaleString('es-PE')}` : '—';

  if (alertas.status === 'fulfilled') renderActivity(alertas.value);
  else errorEn('activity-feed', alertas.reason.message);

  if (productos.status === 'fulfilled') {
    const sel = document.getElementById('rank-product');
    sel.innerHTML = '<option value="">— Seleccionar producto —</option>' + productos.value
      .map(p => `<option value="${esc(p.ean)}">${esc(p.nombre)}</option>`).join('');
  }

  if (proveedores.status === 'fulfilled') {
    document.getElementById('lote-prov').innerHTML = '<option value="">— Proveedor del producto —</option>' +
      proveedores.value.map(p => `<option value="${esc(p.nombre)}">${esc(p.nombre)}</option>`).join('');
  }
}

/* ====== RANKING (ms-cotizaciones) ====== */
async function loadRanking() {
  const ean  = document.getElementById('rank-product').value;
  const list = document.getElementById('ranking-list');
  if (!ean) { list.innerHTML = ''; return; }
  list.innerHTML = '<div class="text-center py-3 text-muted small">Cargando…</div>';
  try {
    const items = await api(`/api/cotizaciones/ranking/${encodeURIComponent(ean)}`);
    if (!items.length) {
      list.innerHTML = '<div class="sin-datos" style="padding:20px 0"><p>No hay cotizaciones confirmadas para este producto.</p></div>';
      return;
    }
    list.innerHTML = items.map((item, i) => `
      <div class="fila-ranking">
        <div class="posicion-rank">#${i + 1}</div>
        <div class="info-rank">
          <div class="nombre-rank">${esc(item.proveedor)}${i === 0 ? '<span class="chip-mejor-precio">Mejor precio</span>' : ''}</div>
          <div class="detalle-rank">${esc(item.presentacion || '')} ${item.fecha ? '· ' + fmtDate(item.fecha) : ''}</div>
        </div>
        <div class="precio-rank ${i === 0 ? 'best' : i === items.length - 1 ? 'worst' : 'mid'}">${fmtMoney(item.precio)}</div>
      </div>`).join('');
  } catch (e) {
    errorEn('ranking-list', e.message);
  }
}

/* ====== NUEVO LOTE (ms-productos para validar el EAN + ms-lotes para guardar) ====== */
async function submitLote() {
  const ean   = document.getElementById('lote-ean').value.trim();
  const qty   = +document.getElementById('lote-qty').value;
  const cost  = +document.getElementById('lote-cost').value;
  const fecha = document.getElementById('lote-fecha').value;
  const okEan = /^\d{13}$/.test(ean);
  document.getElementById('ean-error').classList.toggle('visible', !okEan);
  document.getElementById('lote-ean').classList.toggle('campo-invalido', !okEan);
  if (!okEan) return;
  if (!qty || !cost || !fecha) { showaviso('Completa todos los campos obligatorios.', 'error'); return; }

  try {
    const producto = await api(`/api/productos/ean/${ean}`).catch(() => null);
    if (!producto) { showaviso('Ese EAN no está registrado en Productos.', 'error'); return; }
    await api('/api/lotes', { method:'POST', body: {
      ean, productoNombre: producto.nombre, cantidad: qty, costoUnitario: cost, fechaVencimiento: fecha,
      proveedor: document.getElementById('lote-prov').value || producto.proveedor
    }});
    showaviso('Lote registrado correctamente.', 'success');
    document.getElementById('lote-form').reset();
    closeModal('lote-modal');
    loadDashboard();
  } catch (e) {
    showaviso(e.message, 'error');
  }
}

loadDashboard();
