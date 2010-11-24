DIRS = manpages
CSS = RunDeck-Guide.css
DIST = dist

all : dist man html

dist :
	mkdir -p $(DIST)
man : 
	echo Generating man pages for shell tools
	cd manpages; $(MAKE) $(MFLAGS)
	echo Generating guide as a single man page
	pandoc -s -w man 0[1-9]-*/*.md -o $(DIST)/man/man1/rundeck-guide.1

html :
	echo Generating HTML book
	pandoc --number-sections --toc -s 0[1-9]-*/*.md --css=$(CSS) -o $(DIST)/html/RunDeck-Guide.html
	cp $(CSS) $(DIST)/html
clean :
	echo cleaning up in .
	rm -r $(DIST)
	-for d in $(DIRS); do (cd $$d; $(MAKE) clean ); done