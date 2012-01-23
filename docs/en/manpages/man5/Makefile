include ../../../Makefile.inc

DIST = ../../dist
MANSFX=5
.SUFFIXES: .5
.PHONY: all

PAGES = *.md

all: roff

roff : 
	$(ECHO) Generating system man pages
	mkdir -p $(DIST)/man/man5
	-for f in $(PAGES); do ( $(PANDOC) -s -w man $$f -o $(DIST)/man/man5/$${f%.md}.$(MANSFX); ); done

clean :
	rm -rf $(DIST)/man/man5

