#!/usr/bin/python

import BaseHTTPServer
import re
import urllib
import sys
import traceback

def ParseArgs(uri):
  if not uri:
    return {}
  kvpairs = uri.split('&')
  result = {}
  for kvpair in kvpairs:
    k, v = kvpair.split('=')
    result[k] = urllib.unquote(v)
  return result

class RequestDispatcher(BaseHTTPServer.BaseHTTPRequestHandler):
  # Workaround for stupidity in BaseHTTPServer: it creates new handler
  # for each request.
  #
  # Warning: it is NOT safe to call methods of this object in parallel.
  def __init__(self):
    self.handlers = {}

  def RegisterHandler(self, uri, handler):
    assert uri not in self.handlers
    self.handlers[uri] = handler

  def HandleRequest(self, path):
    """Returns tuple (content, mime-type).

    Returns None for unknown requests.
    """
    m = re.match('(/[^?]*)\??(.*)', path)
    uri = m.group(1)
    handler = self.handlers.get(uri)
    if not handler:
      raise Exception('No handler for uri %s' % uri)
    args = ParseArgs(m.group(2))
    return handler(args)

  def DoHandle(self, request, client_address, socket_server):
    self.request = request
    self.client_address = client_address
    self.server = socket_server
    self.setup()
    self.handle()
    self.finish()

  def do_GET(self):
    try:
      content, mimetype = self.HandleRequest(self.path)
      self.send_response(200)
      self.send_header('Content-type', mimetype)
      self.send_header('Content-length', len(content))
      self.end_headers()
      self.wfile.write(content)
    except Exception, exc:
      print 'Occured exception', exc
      traceback.print_exc()
      error = str(exc)
      self.send_response(404)
      self.send_header('Content-type', 'text/html')
      self.end_headers()
      self.wfile.write(error)

  def do_POST(self):
    try:
      content, mimetype = self.HandleRequest(self.path)
      self.send_response(200)
      self.send_header('Content-type', mimetype)
      self.send_header('Content-length', len(content))
      self.end_headers()
      self.wfile.write(content)
    except Exception, exc:
      print 'Occured exception', exc
      traceback.print_exc()
      error = str(exc)
      self.send_response(404)
      self.send_header('Content-type', 'text/html')
      self.end_headers()
      self.wfile.write(error)

class MoreBaseHttpServer(object):
  def __init__(self, port):
    self.dispatcher = RequestDispatcher()
    self.server = BaseHTTPServer.HTTPServer(('', port),
                                            self.dispatcher.DoHandle)

  def Serve(self):
    self.server.serve_forever()

  def RegisterHandler(self, uri, handler):
    self.dispatcher.RegisterHandler(uri, handler)