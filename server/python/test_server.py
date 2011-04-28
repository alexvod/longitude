#!/usr/bin/python

import http_server
import location
import static

_DEFAULT_PORT = 46940

def RunServer():
  server = http_server.MoreBaseHttpServer(_DEFAULT_PORT)

  location_manager = location.LocationManager()
  server.RegisterHandler('/', location_manager.HandleShowRequest)
  server.RegisterHandler('/getloc', location_manager.HandleGetLocRequest)
  server.RegisterHandler('/update', location_manager.HandleUpdateRequest)

  static_handler = static.StaticFilesHandler()
  server.RegisterHandler('/js', static_handler.HandleJSRequest)

  server.Serve()


def main():
  RunServer()


if __name__ == '__main__':
  main()
