import http.server
import os
import socketserver

APK_PATH = "/app/applet/.build-outputs/app-release.apk"
PORT = 3000


class ApkHandler(http.server.BaseHTTPRequestHandler):
    def do_HEAD(self):
        self.send_apk_headers()

    def do_GET(self):
        if self.path in ["/", "/index.html"]:
            size_bytes = os.path.getsize(APK_PATH)
            size_mb = size_bytes / (1024 * 1024)
            html = f"""<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Download Personal Keyboard v1.0.2 APK</title>
  <style>
    body {{ font-family: system-ui, sans-serif; background: #0f172a; color: #f8fafc; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 16px; }}
    .card {{ background: #1e293b; border: 1px solid #334155; border-radius: 16px; padding: 28px; max-width: 460px; width: 100%; text-align: center; box-shadow: 0 10px 30px rgba(0,0,0,0.4); }}
    h1 {{ font-size: 22px; margin: 0 0 8px; }}
    p {{ color: #94a3b8; font-size: 14px; margin: 6px 0; }}
    .meta {{ background: #0f172a; border-radius: 10px; padding: 12px; margin: 16px 0; text-align: left; font-family: monospace; font-size: 12px; color: #cbd5e1; word-break: break-all; }}
    .btn {{ display: inline-block; background: #2563eb; color: #ffffff; text-decoration: none; font-weight: 600; padding: 14px 24px; border-radius: 10px; margin-top: 12px; width: 100%; box-sizing: border-box; }}
    .btn:hover {{ background: #1d4ed8; }}
  </style>
</head>
<body>
  <div class="card">
    <h1>Personal Keyboard v1.0.2</h1>
    <p>Signed Release APK (Option A Compatible)</p>
    <div class="meta">
      <div><strong>File:</strong> app-release.apk</div>
      <div><strong>Package:</strong> com.aistudio.personalkeyboard.kqxwmv</div>
      <div><strong>Version:</strong> 1.0.2 (versionCode 3)</div>
      <div><strong>Size:</strong> {size_mb:.2f} MB ({size_bytes:,} bytes)</div>
      <div><strong>Cert SHA-256:</strong> c749dc26adcecb8988551c4589666451483a6a46ec30e309d769b0692c523a54</div>
    </div>
    <a class="btn" href="/app-release.apk" download="app-release.apk">Download app-release.apk (v1.0.2)</a>
  </div>
</body>
</html>""".encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(html)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(html)
            return

        if not os.path.exists(APK_PATH):
            self.send_error(404, "APK file not found")
            return

        self.send_apk_headers()
        with open(APK_PATH, "rb") as f:
            while True:
                chunk = f.read(65536)
                if not chunk:
                    break
                self.wfile.write(chunk)

    def send_apk_headers(self):
        size_bytes = os.path.getsize(APK_PATH)
        self.send_response(200)
        self.send_header("Content-Type", "application/vnd.android.package-archive")
        self.send_header("Content-Disposition", 'attachment; filename="app-release.apk"')
        self.send_header("Content-Length", str(size_bytes))
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()


class ReusableTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    allow_reuse_address = True


if __name__ == "__main__":
    with ReusableTCPServer(("0.0.0.0", PORT), ApkHandler) as httpd:
        print(f"Serving APK on port {PORT}", flush=True)
        httpd.serve_forever()
