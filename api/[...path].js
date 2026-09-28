module.exports = async function handler(req, res) {
  // Gérer immédiatement les requêtes pré-vol CORS (OPTIONS)
  if (req.method === 'OPTIONS') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS, PATCH');
    res.setHeader('Access-Control-Allow-Headers', '*');
    res.setHeader('Access-Control-Max-Age', '86400');
    return res.status(200).end();
  }

  // Reconstruire le chemin cible
  const pathParam = req.query.path;
  const pathStr = Array.isArray(pathParam) ? pathParam.join('/') : (pathParam || '');

  // Conserver les paramètres de requête éventuels (hors 'path')
  const queryEntries = Object.entries(req.query || {}).filter(([k]) => k !== 'path');
  const queryString = queryEntries.length > 0
    ? '?' + new URLSearchParams(queryEntries).toString()
    : '';

  const targetUrl = `https://moteur-rapports-v2-production.up.railway.app/api/${pathStr}${queryString}`;

  // Cloner les en-têtes en supprimant Origin et Host pour éliminer tout blocage CORS côté Spring Boot
  const headers = {};
  for (const [key, value] of Object.entries(req.headers || {})) {
    const k = key.toLowerCase();
    if (k !== 'host' && k !== 'origin' && k !== 'referer' && !k.startsWith('x-vercel-')) {
      headers[key] = value;
    }
  }

  try {
    let body = undefined;
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      if (typeof req.body === 'object') {
        body = JSON.stringify(req.body);
        headers['content-type'] = 'application/json';
      } else {
        body = req.body;
      }
    }

    const response = await fetch(targetUrl, {
      method: req.method,
      headers,
      body
    });

    res.status(response.status);

    // Transférer les en-têtes de réponse utiles
    response.headers.forEach((val, key) => {
      const k = key.toLowerCase();
      if (k !== 'transfer-encoding' && k !== 'content-encoding' && k !== 'content-length') {
        res.setHeader(key, val);
      }
    });

    // Forcer les en-têtes CORS permissifs pour le navigateur
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS, PATCH');
    res.setHeader('Access-Control-Allow-Headers', '*');
    res.setHeader('Access-Control-Expose-Headers', 'Authorization, Content-Disposition, X-Entreprise-Code');

    const buffer = await response.arrayBuffer();
    return res.send(Buffer.from(buffer));
  } catch (err) {
    return res.status(502).json({
      message: 'Erreur proxy backend: ' + err.message
    });
  }
};
