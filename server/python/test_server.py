#!/usr/bin/python

import http_server
import location

_DEFAULT_PORT = 10000

def RunServer():
  server = http_server.MoreBaseHttpServer(_DEFAULT_PORT)

  location_manager = location.LocationManager()
  server.RegisterHandler('/', location_manager.HandleShowRequest)
  server.RegisterHandler('/update', location_manager.HandleUpdateRequest)

  server.Serve()


def main():
  RunServer()


if __name__ == '__main__':
  main()
