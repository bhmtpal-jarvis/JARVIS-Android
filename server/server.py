import os
from http.server import BaseHTTPRequestHandler, HTTPServer

class JarvisHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"JARVIS AI server online"}')
        else:
            self.send_response(404)
            self.end_headers()

server = HTTPServer(("0.0.0.0", 8000), JarvisHandler)
print("JARVIS server running on port 8000")
server.serve_forever()
