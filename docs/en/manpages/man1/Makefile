include ../../../Makefile.inc

DIST = ../../dist
.SUFFIXES: .1.gz .1

manpages: dispatch.1.gz rd-jobs.1.gz rd-options.1.gz rd-setup.1.gz rd-queue.1.gz run.1.gz 

rundeck-guide.1:
	pandoc -s -w man ../0[1-9]-*/*.md -o rundeck-guide.1

%.1.gz: %.1
	gzip < $< > $@

%.1:
	pandoc -s -w man $@.md -o $@

PAGES = dispatch.1 rd-jobs.1 rd-options.1 rd-setup.1 rd-queue.1 run.1 

publish : roff html

roff : 
	$(ECHO) Generating system man pages
	mkdir -p $(DIST)/man/man1
	-for f in $(PAGES); do ( $(PANDOC) -s -w man $$f.md -o $(DIST)/man/man1/$$f; ); done

html :
	$(ECHO) Generating html for system man pages
	mkdir -p $(DIST)/html
	-for f in $(PAGES); do ( $(PANDOC) -s -w html $$f.md -o $(DIST)/html/$$f.html; ); done

clean :
	rm *.1 *.1.gz


