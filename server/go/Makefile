GO_CC = 6g
GO_LINK = 6l

all: server

server: server.6
	$(GO_LINK) -o $@ $<

%.6: %.go
	$(GO_CC) -o $@ $<

check-syntax:
	$(GO_CC) $(CHK_SOURCES)
