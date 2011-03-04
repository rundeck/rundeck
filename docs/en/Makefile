include ../Makefile.inc

DIRS = manpages
CSS = RunDeck-Guide.css
DIST = dist

.PHONY: man html clean

all : dist man html figures

dist :
	mkdir -p $(DIST)
man : 
	$(MAKE) VERSION=$(VERSION) -C manpages

$(DIST)/html : dist
	mkdir -p $(DIST)/html

html : $(DIST)/html
	@ $(ECHO) Generating HTML book version $(VERSION)
	# set version
	@sed "s/@VERSION@/$(VERSION)/" title.txt.template > title.txt
	$(PANDOC) --number-sections --toc -s title.txt 0[0-9]-*/*.md --css=$(CSS) --template=templates/html.template --include-before=templates/before.html --include-after=templates/after.html -o $(DIST)/html/RunDeck-Guide.html
	-rm title.txt
	cp $(CSS) $(DIST)/html

$(DIST)/html/figures : dist
	mkdir -p $(DIST)/html/figures

figures : $(DIST)/html/figures
	- cp ../figures/*.png $(DIST)/html/figures

clean :
	rm -fr $(DIST)
	$(MAKE) -C manpages clean
