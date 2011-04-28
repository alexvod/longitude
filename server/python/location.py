import urllib
import ioutils
import location_pb2

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

  def HandleGetLocRequest(self, args):
    output = args.get('output', 'json')
    print output
    if output == 'proto':
      return self._GetLocationsProto(), 'application/octet-stream'
    return self._GetLocationsJson(), 'text/javascript'

  def _GetLocationsJson(self):
    result = []
    for name, location in self._locations.iteritems():
      result.append('\'%s\': {\'lat\': %.5f, \'lng\': %.5f}' % (urllib.quote(name), location[0], location[1]))
    return '{' + ', '.join(result) + '}'

  def _GetLocationsProto(self):
    result = location_pb2.LocationInfo()
    for name, location in self._locations.iteritems():
      loc = result.location.add()
      loc.name = name
      loc.lat = location[0]
      loc.lng = location[1]
    return result.SerializeToString()

