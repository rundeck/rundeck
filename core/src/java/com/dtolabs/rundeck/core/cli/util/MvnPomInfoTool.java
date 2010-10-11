/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* MvnPomInfoTool.java
* 
* User: greg
* Created: Apr 14, 2008 11:19:10 AM
* $Id$
*/
package com.dtolabs.rundeck.core.cli.util;

import org.apache.log4j.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.Project;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;


/**
 * <p>
 * MvnPomInfoTool Searches through a directory structure to find maven 2 pom files.  It then extracts relevant
 * information from those files (artifactId, groupId, packaging, version) and generates a data file to store that
 * information.  The data file is xml formatted, and allows mapping of package name (artifact file name) to
 * the appropriate pom.xml file for the package, as well as discovering the relative path in the maven repository
 * from the groupId turned into a path.
 * </p>
 * <p>
 * Example output:
 * </p>
 * <pre><code>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *     &lt;packages basedir="/Users/greg/Desktop/work/test/work"&gt;
 *     &lt;package name="myapp-9.2.rpm" pom-path="dir/path/to/pom.xml" repo-path="com/dtolabs/test"/&gt;
 *     &lt;package name="something-0.7.rpm" pom-path="dir/pom.xml" repo-path="com/test"/&gt;
 *     &lt;package name="server-1.0-SNAPSHOT.rpm" pom-path="/dir/another/pom.xml" repo-path="arf/woof/howdy"/&gt;
 * &lt;/packages&gt;
 * </code></pre>
 *
 * <p>
 * Usage:
 * </p>
 * <pre>
 * usage: java com.dtolabs.rundeck.core.cli.util.MvnPomInfoTool &lt;args...&gt;
 * -b,--basedir      base directory to being search. [required]
 * -h                display this help
 * -o,--outfile      output file path. [required]
 * -p,--packaging    packaging type, as specified in pom files. [required]
 * </pre>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class MvnPomInfoTool {
    public static final Logger logger = Logger.getLogger(MvnPomInfoTool.class);

    File basedir;
    String packaging;
    String destfile;
    HashMap data = new HashMap();
    HashMap tree = new HashMap();

    protected CommandLine cli;
    /**
     * reference to the command line {@link org.apache.commons.cli.Options} instance.
     */
    protected static Options options = new Options();

    /**
     * initialize options with the help arg
     */
    static {
        options.addOption("h", false, "display this help");
        options.addOption("b", "basedir", true, "base directory to begin search. [required]");
        options.addOption("p", "packaging", true, "packaging type, as specified in pom files. [required]");
        options.addOption("o", "outfile", true, "output file path. [required]");
    }

    public static void main(String[] args) throws IOException {
        MvnPomInfoTool tool = new MvnPomInfoTool();
        tool.parseArgs(args);
        tool.validate();
        tool.run();
    }

    void validate() {
        if (null == basedir || !basedir.exists()) {
            throw new IllegalArgumentException("basedir does not exist: " + basedir);
        }
    }

    void run() throws IOException {
        logger.debug("basedir: " + basedir + ", packaging: " + packaging + ", destfile: " + destfile);
        process();
        Document doc = generateData();
        storeDocument(doc);
    }

    void storeDocument(Document doc) throws IOException {
        OutputFormat format = OutputFormat.createPrettyPrint();
        OutputStream os;
        FileOutputStream fos = null;
        if("-".equals(destfile)) {
            os = System.out;
        }else {
            fos = new FileOutputStream(destfile);
            os = fos;
        }
        XMLWriter writer = new XMLWriter(os, format);
        writer.write(doc);
        if(null!=fos) {
            fos.close();
        }
    }

    void process() {
        //find all pom.xml files located in basedir
        FileSet fset = new FileSet();
        Project p = new Project();
        p.setBaseDir(basedir);
        fset.setProject(p);
        p.init();
        fset.setDir(basedir);
        PatternSet.NameEntry entry = fset.createInclude();
        entry.setName("**/pom.xml");
        Iterator i = fset.iterator();
        processFileResources(i);
    }

    Document generateData() {
        Document doc = DocumentHelper.createDocument();
        Element el = doc.addElement("packages");
        String basepath = basedir.getAbsolutePath();
        if(basepath.endsWith("/")) {
            basepath = basepath.substring(0, basepath.length() - 1);
        }
        el.addAttribute("basedir", basepath);
        for (Iterator iterator = data.keySet().iterator(); iterator.hasNext();) {
            String file = (String) iterator.next();
            Map data = (Map) this.data.get(file);
            logger.info(file + ": " + data);
            Element pkgelem = el.addElement("package");
            pkgelem.addAttribute("name",
                                 data.get("artifactId") + "-" + data.get("version") + "." + data.get("packaging"));
            File f = new File(file);

            String relpom = file.substring(basepath.length() + 1);
            pkgelem.addAttribute("pom-path", relpom);

            String groupId = (String) data.get("groupId");
            String relpath = groupId.replaceAll("\\.", "/");
            pkgelem.addAttribute("repo-path", relpath);

        }
        return doc;
    }

    private void processFileResources(Iterator i) {
        SAXReader reader = new SAXReader();


        while (i.hasNext()) {
            FileResource f = (FileResource) i.next();
            try {
                Document doc = reader.read(f.getInputStream());
                //check packaging value
                if (null != doc.selectSingleNode("/project/packaging")) {
                    Element e = (Element) doc.selectSingleNode("/project/packaging");
                    if (!packaging.equals(e.getStringValue())) {
                        continue;
                    }
                }else{
                    continue;
                }
                logger.debug("processing pom: " + f.getFile());
                Map fdata = processDocument(doc);
                if(null!=fdata){
                    data.put(f.getFile().getAbsolutePath(), fdata);
                }
            } catch (DocumentException e) {
                logger.error("Unable to read file: " + f.getFile() + ": " + e.getMessage());
            } catch (IOException e) {
                logger.error("Unable to read file: " + f.getFile() + ": " + e.getMessage());
            }
        }
    }

    Map processDocument(Document doc) {
        HashMap map = new HashMap();
        //discover pom detail
        Node groupNode = doc.selectSingleNode("/project/groupId");
        if (null != groupNode) {
            Element node = (Element) groupNode;
            map.put("groupId", node.getStringValue());
        }else{
            return null;
        }
        Node artifactNode = doc.selectSingleNode("/project/artifactId");
        if (null != artifactNode) {
            Element node = (Element) artifactNode;
            map.put("artifactId", node.getStringValue());
        } else {
            return null;
        }
        Node versionNode = doc.selectSingleNode("/project/version");
        if (null != versionNode) {
            Element node = (Element) versionNode;
            map.put("version", node.getStringValue());
        } else {
            return null;
        }
        Node packagingNode = doc.selectSingleNode("/project/packaging");
        if (null != packagingNode) {
            Element node = (Element) packagingNode;
            map.put("packaging", node.getStringValue());
        } else {
            return null;
        }

        return map;
    }

    void parseArgs(String[] args) {
        if(args.length<1){
            help();
            exit(2);
            return;
        }
        final CommandLineParser parser = new PosixParser();
        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            help();
            exit(2);
            return;
        }
        boolean argHelp = cli.hasOption("h");
        if (argHelp) {
            help();
            exit(2);
            return;
        }
        if (!cli.hasOption("b")) {
            throw new IllegalArgumentException("-b/--basedir is required.");
        }
        basedir = new File(cli.getOptionValue("b"));
        if (!cli.hasOption("o")) {
            throw new IllegalArgumentException("-o/--outfile is required.");
        }
        destfile = cli.getOptionValue("o");
        packaging = cli.getOptionValue("p");
        if (null == packaging) {
            throw new IllegalArgumentException("-p/--packaging is required");
        }

    }

    protected void exit(int err) {
        System.exit(err);
    }

    public void help() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(80, "java " + this.getClass().getName() + " <args...>", null, options, null);
    }
}
