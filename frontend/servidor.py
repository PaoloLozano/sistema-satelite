# Sirve el frontend en http://localhost:3500 sin caché, para que el navegador
# siempre cargue la última versión de los HTML, CSS y JS.
import functools
import http.server
import os

PUERTO = 3500
CARPETA = os.path.dirname(os.path.abspath(__file__))


class SinCache(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Cache-Control', 'no-cache')
        super().end_headers()


if __name__ == '__main__':
    manejador = functools.partial(SinCache, directory=CARPETA)
    print(f'Frontend en http://localhost:{PUERTO}/pages/login.html')
    http.server.ThreadingHTTPServer(('', PUERTO), manejador).serve_forever()
