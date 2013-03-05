#!/bin/bash

FILES=$*

for i in $FILES ; do
    #tidy -asxml ${i} 2>/dev/nullj
    xml sel -T  -t -m "//h2|//h1|//h3|//h4|//h5|//h6|//div"  -i "@id" -f -o "#" -v "@id" -n ${i} # >> $PDIR/links.index
done
