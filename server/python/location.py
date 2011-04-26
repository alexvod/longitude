import urllib
import ioutils

_START_PAGE = 'index.html'

class LocationManager(object):
  def __init__(self):
    self._locations = {}

  def HandleUpdateRequest(self, args):
    name = args['id']
    lat = float(args['lat'])
    lng = float(args['lng'])
    self._locations[name] = (lat, lng)
    return 'ok', 'text/plain'
    
  def HandleShowRequest(self, args):
    html = ioutils.ReadFile(_START_PAGE)
    html = html.replace('@@LOCATIONS@@', self._GetLocationsJson())
    return html, 'text/html'

  def _GetLocationsJson(self):
    result = []
    # TODO: show index page if first-time loading
    for name, location in self._locations.iteritems():
      result.append('\'%s\': {\'lat\': %.5f, \'lng\': %.5f}' % (urllib.quote(name), location[0], location[1]))
    return '{' + ', '.join(result) + '}'
