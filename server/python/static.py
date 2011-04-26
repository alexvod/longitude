#!/usr/bin/python

import ioutils
import re

class StaticFilesHandler(object):
  def HandleJSRequest(self, args):
    name = args['name']
    if re.match('[^a-z_]', name):
      raise Exception('Bad file requested')
    return ioutils.ReadFile(name + '.js'), 'text/javascript'


