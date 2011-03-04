include ../../../Makefile.inc

DIST = ../../dist
CSS=RunDeck-Guide.css
TEMPLATES=../../templates
.SUFFIXES: .1.gz .1
.PHONY: all

all: manpages html roff

manpages: dispatch.1.gz rd-jobs.1.gz rd-options.1.gz rd-setup.1.gz rd-queue.1.gz run.1.gz 

rundeck-guide.1:
	pandoc -s -w man ../0[1-9]-*/*.md -o rundeck-guide.1

%.1.gz: %.1
	gzip < $< > $@

%.1:
	sed "s/@VERSION@/$(VERSION)/" $@.md | pandoc -s -w man -o $@

PAGES = dispatch.1 rd-jobs.1 rd-options.1 rd-setup.1 rd-queue.1 run.1 

publish : roff html

roff : 
	$(ECHO) Generating system man pages
	mkdir -p $(DIST)/man/man1
	-for f in $(PAGES); do ( sed "s/@VERSION@/$(VERSION)/" $$f.md | $(PANDOC) -s -w man -o $(DIST)/man/man1/$$f; ); done

html :
	$(ECHO) Generating html for system man pages
	mkdir -p $(DIST)/html
	-for f in $(PAGES); do ( sed "s/@VERSION@/$(VERSION)/" $$f.md | $(PANDOC) -s  --css=$(CSS) --template=$(TEMPLATES)/html.template --include-before=$(TEMPLATES)/before.html --include-after=$(TEMPLATES)/after.html -o $(DIST)/html/$${f%.1}.html); done

clean :
	rm -f *.1 *.1.gz


