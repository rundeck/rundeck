include ../../Makefile.inc

DIST = ../dist
CSS=RunDeck-Guide.css
TEMPLATES=../templates

DIRS = man1 man5

.PHONY: clean subdirs $(DIRS) all

all: subdirs htmlindex

subdirs: $(DIRS)

$(DIRS) : 
	$(MAKE) VERSION=$(VERSION) -C $@

clean :
	@-for i in $(DIRS) ; do \
	    echo $(MAKE) -C $$i clean; \
	done

#generate md file for index for each man section
htmlindex: subdirs
	-rm -f man1.index.md man5.index.md
	echo "## Command Reference" >> man1.index.md
	for i in man1/*.md ; do \
		f=$$(basename $$i); \
		echo "* [$${f%.1.md}]($${f%.1.md}.html) " >> man1.index.md; \
	done;
	echo >> man1.index.md;
	echo "## File Formats" >> man5.index.md
	for i in man5/*.md ; do \
		f=$$(basename $$i); \
		echo "* [$${f%.5.md}]($${f%.5.md}.html) " >> man5.index.md; \
	done;
	# set version in index file
	sed "s/@VERSION@/$(VERSION)/" index.md.template > index.md
	#concat md files and generate index.html
	$(PANDOC) -s index.md man1.index.md man5.index.md  --css=$(CSS) --template=$(TEMPLATES)/html.template --include-before=$(TEMPLATES)/before.html --include-after=$(TEMPLATES)/after.html -o $(DIST)/html/index.html;
	-rm  -f man1.index.md man5.index.md index.md
