DIST = ../dist

PAGES = dispatch.1 job-v20.1 rd-jobs.1 rd-options.1 rd-setup.1 rdq.1 resource-v10.1 run.1 rundeck.1

all : roff html

roff : 
	echo Generating system man pages
	mkdir -p $(DIST)/man/man1
	-for f in $(PAGES); do ( pandoc -s -w man $$f.md -o $(DIST)/man/man1/$$f; ); done

html :
	echo Generating html for system man pages
	mkdir -p $(DIST)/html
	-for f in $(PAGES); do ( pandoc -s -w html $$f.md -o $(DIST)/html/$$f.html; ); done

clean :
	rm -r $(DIST)


