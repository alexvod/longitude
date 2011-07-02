GO_CC = 6g
GO_LINK = 6l

all: server

server: server.6 query.6 util.6 auth.6
	$(GO_LINK) -o $@ $<

server.6: auth.6 util.6 query.6

auth.6: util.6

%.6: %.go
	$(GO_CC) -o $@ $<

check-syntax:
	$(GO_CC) $(CHK_SOURCES)

clean:
	rm -rf *.6 server
