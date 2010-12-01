include ../../../Makefile.inc

DIST = ../../dist
.SUFFIXES: .5.gz .5

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
	-for f in $(PAGES); do ( $(PANDOC) -s -w html $$f.md -o $(DIST)/html/$$f.html; ); done

clean :
	rm -f *.5 *.5.gz


