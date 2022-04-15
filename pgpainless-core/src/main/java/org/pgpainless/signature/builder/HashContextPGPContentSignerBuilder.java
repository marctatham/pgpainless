// SPDX-FileCopyrightText: 2022 Paul Schaub <vanitasvitae@fsfe.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.pgpainless.signature.builder;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.DSADigestSigner;
import org.bouncycastle.crypto.signers.DSASigner;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.crypto.signers.Ed448Signer;
import org.bouncycastle.crypto.signers.RSADigestSigner;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.operator.PGPContentSigner;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyConverter;
import org.bouncycastle.util.Arrays;
import org.pgpainless.algorithm.HashAlgorithm;
import org.pgpainless.algorithm.PublicKeyAlgorithm;

/**
 * Implementation of {@link PGPContentSignerBuilder} using the BC API, which can be used to sign hash contexts.
 * This can come in handy to sign data, which was already processed to calculate the hash context, without the
 * need to process it again to calculate the OpenPGP signature.
 */
public class HashContextPGPContentSignerBuilder implements PGPContentSignerBuilder {

    private final BcPGPKeyConverter keyConverter = new BcPGPKeyConverter();
    private final MessageDigest messageDigest;
    private final HashAlgorithm hashAlgorithm;

    public HashContextPGPContentSignerBuilder(MessageDigest messageDigest) {
        this.messageDigest = messageDigest;
        this.hashAlgorithm = HashAlgorithm.fromName(messageDigest.getAlgorithm());
        if (hashAlgorithm == null) {
            throw new IllegalArgumentException("Cannot recognize OpenPGP Hash Algorithm: " + messageDigest.getAlgorithm());
        }
    }

    @Override
    public PGPContentSigner build(int signatureType, PGPPrivateKey privateKey) throws PGPException {
        PublicKeyAlgorithm keyAlgorithm = PublicKeyAlgorithm.requireFromId(privateKey.getPublicKeyPacket().getAlgorithm());
        AsymmetricKeyParameter privKeyParam = keyConverter.getPrivateKey(privateKey);
        final Signer signer = createSigner(keyAlgorithm, messageDigest, privKeyParam);
        signer.init(true, privKeyParam);

        return new PGPContentSigner() {
            public int getType() {
                return signatureType;
            }

            public int getHashAlgorithm() {
                return hashAlgorithm.getAlgorithmId();
            }

            public int getKeyAlgorithm() {
                return keyAlgorithm.getAlgorithmId();
            }

            public long getKeyID() {
                return privateKey.getKeyID();
            }

            public OutputStream getOutputStream() {
                return new SignerOutputStream(signer);
            }

            public byte[] getSignature() {
                try {
                    return signer.generateSignature();
                } catch (CryptoException e) {
                    throw new IllegalStateException("unable to create signature");
                }
            }

            public byte[] getDigest() {
                return messageDigest.digest();
            }
        };
    }

    static Signer createSigner(
            PublicKeyAlgorithm keyAlgorithm,
            MessageDigest messageDigest,
            CipherParameters keyParam)
            throws PGPException {
        ExistingMessageDigest staticDigest = new ExistingMessageDigest(messageDigest);
        switch (keyAlgorithm.getAlgorithmId()) {
            case PublicKeyAlgorithmTags.RSA_GENERAL:
            case PublicKeyAlgorithmTags.RSA_SIGN:
                return new RSADigestSigner(staticDigest);
            case PublicKeyAlgorithmTags.DSA:
                return new DSADigestSigner(new DSASigner(), staticDigest);
            case PublicKeyAlgorithmTags.ECDSA:
                return new DSADigestSigner(new ECDSASigner(), staticDigest);
            case PublicKeyAlgorithmTags.EDDSA:
                if (keyParam instanceof Ed25519PrivateKeyParameters || keyParam instanceof Ed25519PublicKeyParameters) {
                    return new EdDsaSigner(new Ed25519Signer(), staticDigest);
                }
                return new EdDsaSigner(new Ed448Signer(new byte[0]), staticDigest);
            default:
                throw new PGPException("cannot recognise keyAlgorithm: " + keyAlgorithm);
        }
    }

    static class ExistingMessageDigest implements Digest {

        private final MessageDigest digest;

        ExistingMessageDigest(MessageDigest messageDigest) {
            this.digest = messageDigest;
        }

        @Override
        public void update(byte in) {
            digest.update(in);
        }

        @Override
        public void update(byte[] in, int inOff, int len) {
            digest.update(in, inOff, len);
        }

        @Override
        public int doFinal(byte[] out, int outOff) {
            byte[] hash = digest.digest();
            System.arraycopy(hash, 0, out, outOff, hash.length);
            return getDigestSize();
        }

        @Override
        public void reset() {
            // Nope!
            // We cannot reset, since BCs signer classes are resetting in their init() methods, which would also reset
            // the messageDigest, losing its state. This would shatter our intention.
        }

        @Override
        public String getAlgorithmName() {
            return digest.getAlgorithm();
        }

        @Override
        public int getDigestSize() {
            return digest.getDigestLength();
        }
    }

    // Copied from BCs BcImplProvider - required since BCs class is package visible only :/
    private static class EdDsaSigner
            implements Signer {
        private final Signer signer;
        private final Digest digest;
        private final byte[] digBuf;

        EdDsaSigner(Signer signer, Digest digest) {
            this.signer = signer;
            this.digest = digest;
            this.digBuf = new byte[digest.getDigestSize()];
        }

        public void init(boolean forSigning, CipherParameters param) {
            this.signer.init(forSigning, param);
            this.digest.reset();
        }

        public void update(byte b) {
            this.digest.update(b);
        }

        public void update(byte[] in, int off, int len) {
            this.digest.update(in, off, len);
        }

        public byte[] generateSignature()
                throws CryptoException, DataLengthException {
            digest.doFinal(digBuf, 0);

            signer.update(digBuf, 0, digBuf.length);

            return signer.generateSignature();
        }

        public boolean verifySignature(byte[] signature) {
            digest.doFinal(digBuf, 0);

            signer.update(digBuf, 0, digBuf.length);

            return signer.verifySignature(signature);
        }

        public void reset() {
            Arrays.clear(digBuf);
            signer.reset();
            digest.reset();
        }
    }

    // Copied from BC, required since BCs class is package visible only
    static class SignerOutputStream
            extends OutputStream {
        private Signer sig;

        SignerOutputStream(Signer sig) {
            this.sig = sig;
        }

        public void write(byte[] bytes, int off, int len)
                throws IOException {
            sig.update(bytes, off, len);
        }

        public void write(byte[] bytes)
                throws IOException {
            sig.update(bytes, 0, bytes.length);
        }

        public void write(int b)
                throws IOException {
            sig.update((byte) b);
        }
    }
}
