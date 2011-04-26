#!/usr/bin/python

import ioutils
import re

_START_PAGE = 'index.html'

class StaticFilesHandler(object):
  def Register(self, server):
    server.RegisterHandler('/js', self._HandleJSRequest)
    server.RegisterHandler('/', self._HandleRootRequest)

  def _HandleJSRequest(self, args):
    name = args['name']
    if re.match('[^a-z_]', name):
      raise Exception('Bad file requested')
    return ioutils.ReadFile(name + '.js'), 'text/javascript'

  def _HandleRootRequest(self, args):
    return ioutils.ReadFile(_START_PAGE), 'text/html'


