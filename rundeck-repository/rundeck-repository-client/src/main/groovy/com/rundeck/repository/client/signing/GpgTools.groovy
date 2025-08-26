/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rundeck.repository.client.signing

import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.BCPGOutputStream
import org.bouncycastle.openpgp.PGPCompressedData
import org.bouncycastle.openpgp.PGPObjectFactory
import org.bouncycastle.openpgp.PGPPrivateKey
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder


class GpgTools {

    static void signDetached(boolean armor, InputStream privateKey, InputStream fileToSign, OutputStream signatureFile, GpgPassphraseProvider passphraseProvider) {
        OutputStream outputStream = null;
        if (armor) {
            outputStream = new ArmoredOutputStream(new BufferedOutputStream(signatureFile));
        }
        else {
            outputStream = new BufferedOutputStream(signatureFile);
        }

        PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(privateKey),new BcKeyFingerprintCalculator());
        PGPSecretKey pgpSecretKey = null;

        @SuppressWarnings("unchecked")
        Iterator<PGPSecretKeyRing> iter = pgpSec.getKeyRings();
        while (iter.hasNext() && pgpSecretKey == null) {
            PGPSecretKeyRing keyRing = iter.next();

            @SuppressWarnings("unchecked")
            Iterator<PGPSecretKey> keyIter = keyRing.getSecretKeys();
            while (keyIter.hasNext()) {
                PGPSecretKey key = keyIter.next();
                if (key.isSigningKey()) {
                    pgpSecretKey = key;
                    break;
                }
            }
        }

        if(!pgpSecretKey) {
            throw new IllegalArgumentException("Unable to find signing key");
        }
        PGPPrivateKey pgpPrivateKey = pgpSecretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(passphraseProvider.passphrase));
        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(pgpSecretKey.getPublicKey().getAlgorithm(), PGPUtil.SHA256).setProvider("BC"));
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, pgpPrivateKey);

        BCPGOutputStream bOut = new BCPGOutputStream(outputStream);
        InputStream fIn = new BufferedInputStream(fileToSign);

        int ch;
        while ((ch = fIn.read()) >= 0) {
            signatureGenerator.update((byte)ch);
        }

        fIn.close();

        signatureGenerator.generate().encode(bOut);

        outputStream.close();
        privateKey.close();
    }

    static boolean validateSignature(InputStream fileToValidate, InputStream publicKey, InputStream signature) {
        InputStream keyInputStream = new BufferedInputStream(publicKey);
        InputStream sigInputStream = PGPUtil.getDecoderStream(new BufferedInputStream(signature));

        BcKeyFingerprintCalculator fingerPrintCalc = new BcKeyFingerprintCalculator()
        PGPObjectFactory pgpObjFactory = new PGPObjectFactory(sigInputStream, fingerPrintCalc);
        PGPSignatureList pgpSigList;

        Object obj = pgpObjFactory.nextObject();
        if (obj instanceof PGPCompressedData) {
            PGPCompressedData c1 = (PGPCompressedData)obj;
            pgpObjFactory = new PGPObjectFactory(c1.getDataStream(),fingerPrintCalc);
            pgpSigList = (PGPSignatureList)pgpObjFactory.nextObject();
        }
        else {
            pgpSigList = (PGPSignatureList)obj;
        }

        PGPPublicKeyRingCollection pgpPubRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyInputStream), fingerPrintCalc);
        InputStream  fileInputStream = new BufferedInputStream(fileToValidate);
        PGPSignature sig = pgpSigList.get(0);
        PGPPublicKey pubKey = pgpPubRingCollection.getPublicKey(sig.getKeyID());
        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), pubKey);

        int BUF_SZ = 512
        byte[] buf = new byte[BUF_SZ]
        int ch;
        while ((ch = fileInputStream.read(buf,0,BUF_SZ)) >= 0) {
            sig.update(buf,0,ch);
        }

        fileInputStream.close();
        keyInputStream.close();
        sigInputStream.close();

        return sig.verify()
    }
}
