include ../../../Makefile.inc

DIST = ../../dist
MANSFX=1
.SUFFIXES: .1
.PHONY: all

all: roff

PAGES = *.md

roff : 
	$(ECHO) Generating system man pages
	mkdir -p $(DIST)/man/man1
	-for f in $(PAGES); do ( sed "s/$${VERSION}/$(VERSION)/" $$f | $(PANDOC) -s -w man -o $(DIST)/man/man1/$${f%.md}.$(MANSFX); ); done

clean :
	rm -rf $(DIST)/man/man1

