include ../../../Makefile.inc

DIST = ../../dist
CSS=RunDeck-Guide.css
TEMPLATES=../../templates
.SUFFIXES: .5.gz .5
.PHONY: all

all: manpages html roff

manpages: aclpolicy-v10.5.gz job-v20.5.gz resource-v10.5.gz

%.5.gz: %.5
	gzip < $< > $@

%.5:
	pandoc -s -w man $@.md -o $@

PAGES = aclpolicy-v10.5 job-v20.5 resource-v10.5

publish : roff html

roff : 
	$(ECHO) Generating system man pages
	mkdir -p $(DIST)/man/man5
	-for f in $(PAGES); do ( $(PANDOC) -s -w man $$f.md -o $(DIST)/man/man5/$$f; ); done

html :
	$(ECHO) Generating html for system man pages
	mkdir -p $(DIST)/html
	-for f in $(PAGES); do ( $(PANDOC) -s   --css=$(CSS) --template=$(TEMPLATES)/html.template --include-before=$(TEMPLATES)/before.html --include-after=$(TEMPLATES)/after.html $$f.md -o $(DIST)/html/$${f%.5}.html; ); done

clean :
	rm -f *.5 *.5.gz


