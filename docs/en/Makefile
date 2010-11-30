include ../Makefile.inc

DIRS = manpages
CSS = RunDeck-Guide.css
DIST = dist

all : dist man html

dist :
	mkdir -p $(DIST)
man : 
	$(ECHO) Generating man pages for shell tools
	cd manpages; $(MAKE) $(MFLAGS)
	$(ECHO) Generating guide as a single man page
	$(PANDOC) -s -w man 0[0-9]-*/*.md -o $(DIST)/man/man1/rundeck.1

html :
	$(ECHO) Generating HTML book
	$(PANDOC) --number-sections --toc --css=$(CSS) -s 0[0-9]-*/*.md -o $(DIST)/html/RunDeck-Guide.html
	cp $(CSS) $(DIST)/html

clean :
	$(ECHO) cleaning up in .
	rm -r $(DIST)
	-for d in $(DIRS); do (cd $$d; $(MAKE) clean ); done
