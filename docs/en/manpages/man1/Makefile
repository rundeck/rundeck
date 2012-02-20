include ../../../Makefile.inc

DIST = ../../dist
MANSFX=1
.SUFFIXES: .1
.PHONY: all
PAGES = *.md

all: roff manpages

manpages: $(patsubst %.md,%.1.gz,$(wildcard $(PAGES)))

%.1.gz: %.1
	gzip < $< > $@

%.1: %.md
	sed "s/\$${VERSION}/$(VERSION)/" $*.md | pandoc -s -w man -o $@

roff :
	$(ECHO) Generating system man pages
	mkdir -p $(DIST)/man/man1
	-for f in $(PAGES); do ( sed "s/$${VERSION}/$(VERSION)/" $$f | $(PANDOC) -s -w man -o $(DIST)/man/man1/$${f%.md}.$(MANSFX); ); done

clean :
	@rm -rf $(DIST)/man/man1
	@-rm *.gz
