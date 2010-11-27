DIRS = manpages
CSS = RunDeck-Guide.css
DIST = dist

.PHONY: dist man html clean

all : dist man

dist :
	mkdir -p $(DIST)
	
man : 
	$(MAKE) -C manpages

html :
	pandoc --number-sections --toc -s 0[1-9]-*/*.md --css=$(CSS) -o $(DIST)/html/RunDeck-Guide.html
	cp $(CSS) $(DIST)/html

clean :
	rm -r $(DIST)
	$(MAKE) -C manpages clean