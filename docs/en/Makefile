include ../Makefile.inc

DIRS = manpages
CSS = RunDeck-Guide.css
DIST = dist

.PHONY: man html clean

all : dist man html

dist :
	mkdir -p $(DIST)
man : 
	$(MAKE) -C manpages

$(DIST)/html : dist
	mkdir -p $(DIST)/html

html : $(DIST)/html
	$(ECHO) Generating HTML book
	$(PANDOC) --number-sections --toc -s 0[1-9]-*/*.md --css=$(CSS) --template=templates/html.template --include-before=templates/before.html --include-after=templates/after.html -o $(DIST)/html/RunDeck-Guide.html
	cp $(CSS) $(DIST)/html

clean :
	rm -r $(DIST)
	$(MAKE) -C manpages clean
