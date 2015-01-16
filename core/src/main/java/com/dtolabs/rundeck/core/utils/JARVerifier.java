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
 * JARVerifier.java
 * 
 * User: greg
 * Created: Jan 25, 2005 11:50:37 AM
 * $Id: JARVerifier.java 1079 2008-02-05 04:53:32Z ahonor $
 */
package com.dtolabs.rundeck.core.utils;


import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;


/**
 * JARVerifier verifies signed JAR files given a list of trusted CA certificates. See <a
 * href="http://java.sun.com/products/jce/doc/guide/HowToImplAProvider.html#MutualAuth">http://java.sun.com/products/jce/doc/guide/HowToImplAProvider.html#MutualAuth</a>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 1079 $
 */
public final class JARVerifier {
    private X509Certificate[] trustedCaCerts;

    /**
     * Create a JAR verifier with an array of trusted certificate authority certificates.
     *
     * @param trustedCaCerts certs
     */
    public JARVerifier(X509Certificate[] trustedCaCerts) {
        this.trustedCaCerts = null != trustedCaCerts ? trustedCaCerts.clone() : null;
    }

    /**
     * @return Construct a JARVerifier with a keystore and alias and password.
     *
     * @param keystore filepath to the keystore
     * @param alias    alias name of the cert chain to verify with
     * @param passwd   password to use to verify the keystore, or null

     * @throws IOException on io error
     * @throws KeyStoreException key store error
     * @throws NoSuchAlgorithmException algorithm missing
     * @throws CertificateException cert error
     */
    public static JARVerifier create(String keystore, String alias, char[] passwd) throws IOException, KeyStoreException,
            NoSuchAlgorithmException,
            CertificateException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        FileInputStream fileIn=null;
        try {
            fileIn = new FileInputStream(keystore);
            keyStore.load(fileIn, passwd);
        } finally {
            if(null!= fileIn){
                fileIn.close();
            }
        }
        Certificate[] chain = keyStore.getCertificateChain(alias);
        if (chain == null) {
            Certificate cert = keyStore.getCertificate(alias);
            if (cert == null) {
                throw new IllegalArgumentException("No trusted certificate or chain found for alias: " + alias);
            }
            chain = new Certificate[]{cert};

        }
        X509Certificate certChain[] = new X509Certificate[chain.length];


        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        for (int count = 0; count < chain.length; count++) {
            ByteArrayInputStream certIn = new ByteArrayInputStream(chain[count].getEncoded());
            X509Certificate cert = (X509Certificate) cf.generateCertificate(certIn);
            certChain[count] = cert;
        }

        JARVerifier jarVerifier = new JARVerifier(certChain);
        return jarVerifier;
    }

    /**
     * An Exception thrown during verification.
     */
    public static final class VerifierException extends Exception {
        public VerifierException(Throwable throwable) {
            super(throwable);
        }

        public VerifierException(String s, Throwable throwable) {
            super(s, throwable);
        }

        public VerifierException(String s) {
            super(s);
        }

        public VerifierException() {
            super();
        }
    }

    /**
     * Verify the JAR file signatures with the trusted CA certificates.
     *
     * @param jf jar file
     * @throws IOException on io error
     * @throws CertificateException on cert error
     * @throws VerifierException    If the jar file cannot be verified.
     */
    public final void verifySingleJarFile(JarFile jf)
            throws IOException, CertificateException, VerifierException {
        Vector entriesVec = new Vector();

        // Ensure there is a manifest file
        Manifest man = jf.getManifest();
        if (man == null) {
            throw new VerifierException("The JAR is not signed");
        }

        // Ensure all the entries' signatures verify correctly
        byte[] buffer = new byte[8192];
        Enumeration entries = jf.entries();

        while (entries.hasMoreElements()) {
            JarEntry je = (JarEntry) entries.nextElement();
            entriesVec.addElement(je);
            InputStream is = jf.getInputStream(je);
            int n;
            while ((n = is.read(buffer, 0, buffer.length)) != -1) {
                // we just read. this will throw a SecurityException
                // if  a signature/digest check fails.
            }
            is.close();
        }
        jf.close();

        // Get the list of signer certificates
        Enumeration e = entriesVec.elements();
        while (e.hasMoreElements()) {
            JarEntry je = (JarEntry) e.nextElement();

            if (je.isDirectory()) {
                continue;
            }
            // Every file must be signed - except
            // files in META-INF
            Certificate[] certs = je.getCertificates();
            if ((certs == null) || (certs.length == 0)) {
                if (!je.getName().startsWith("META-INF")) {
                    throw new VerifierException("The JAR file has unsigned files.");
                }
            } else {
                // Check whether the file
                // is signed as expected.
                // The framework may be signed by
                // multiple signers. At least one of
                // the signers must be a trusted signer.

                // First, determine the roots of the certificate chains
                Certificate[] chainRoots = getChainRoots(certs);
                boolean signedAsExpected = false;

                for (int i = 0; i < chainRoots.length; i++) {
                    if (isTrusted((X509Certificate) chainRoots[i],
                            trustedCaCerts)) {
                        signedAsExpected = true;
                        break;
                    }
                }

                if (!signedAsExpected) {
                    throw new VerifierException("The JAR file is not signed by a trusted signer");
                }
            }
        }
    }

    private static boolean isTrusted(X509Certificate cert,
                                     X509Certificate[] trustedCaCerts) {
        // Return true iff either of the following is true:
        // 1) the cert is in the trustedCaCerts.
        // 2) the cert is issued by a trusted CA.

        // Check whether the cert is in the trustedCaCerts
        for (int i = 0; i < trustedCaCerts.length; i++) {
            // If the cert has the same SubjectDN
            // as a trusted CA, check whether
            // the two certs are the same.
            if (cert.getSubjectDN().equals(trustedCaCerts[i].getSubjectDN())) {
                if (cert.equals(trustedCaCerts[i])) {
                    return true;
                }
            }
        }

        // Check whether the cert is issued by a trusted CA.
        // Signature verification is expensive. So we check
        // whether the cert is issued
        // by one of the trusted CAs iff the above loop failed.
        for (int i = 0; i < trustedCaCerts.length; i++) {
            // If the issuer of the cert has the same name as
            // a trusted CA, check whether that trusted CA
            // actually issued the cert.
            if (cert.getIssuerDN().equals(trustedCaCerts[i].getSubjectDN())) {
                try {
                    cert.verify(trustedCaCerts[i].getPublicKey());
                    return true;
                } catch (Exception e) {
                    // Do nothing.
                }
            }
        }

        return false;
    }

    private static Certificate[]
            getChainRoots(Certificate[] certs) {
        Vector result = new Vector(3);
        // choose a Vector size that seems reasonable
        for (int i = 0; i < certs.length - 1; i++) {
            if (!((X509Certificate) certs[i + 1]).getSubjectDN().equals(((X509Certificate) certs[i]).getIssuerDN())) {
                // We've reached the end of a chain
                result.addElement(certs[i]);
            }
        }
        // The final entry in the certs array is always
        // a "root" certificate
        result.addElement(certs[certs.length - 1]);
        Certificate[] ret = new Certificate[result.size()];
        result.copyInto(ret);

        return ret;
    }
}
