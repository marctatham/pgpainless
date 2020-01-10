/*
 * Copyright 2018 Paul Schaub.
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
package org.pgpainless.key;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.junit.Test;
import org.pgpainless.util.TestUtils;

public class TestKeysTest {

    private final PGPSecretKeyRing julietSecRing;
    private final PGPSecretKeyRing romeoSecRing;
    private final PGPPublicKeyRing julietPubRing;
    private final PGPPublicKeyRing romeoPubRing;

    public TestKeysTest() throws IOException, PGPException {
        this.julietSecRing = TestKeys.getJulietSecretKeyRing();
        this.romeoSecRing = TestKeys.getRomeoSecretKeyRing();
        this.julietPubRing = TestKeys.getJulietPublicKeyRing();
        this.romeoPubRing = TestKeys.getRomeoPublicKeyRing();
    }

    @Test
    public void assertJulietsPublicKeyIsSameInPubRingAndSecRing() throws IOException {
        assertArrayEquals(julietSecRing.getPublicKey().getEncoded(), julietPubRing.getPublicKey().getEncoded());
    }

    @Test
    public void assertJulietsKeysIdEquals() {
        assertEquals(TestKeys.JULIET_KEY_ID, julietSecRing.getSecretKey().getKeyID());
        assertEquals(TestKeys.JULIET_KEY_ID, julietSecRing.getPublicKey().getKeyID());
        assertEquals(TestKeys.JULIET_KEY_ID, julietPubRing.getPublicKey().getKeyID());
    }

    @Test
    public void assertJulietsKeyUIDEquals() {
        assertEquals(TestKeys.JULIET_UID, julietSecRing.getPublicKey().getUserIDs().next());
        assertEquals(1, TestUtils.getNumberOfItemsInIterator(julietSecRing.getPublicKey().getUserIDs()));
    }

    @Test
    public void assertJulietsKeyRingFingerprintMatches() {
        assertEquals(TestKeys.JULIET_FINGERPRINT, new OpenPgpV4Fingerprint(julietSecRing));
    }

    @Test
    public void assertJulietsPublicKeyFingerprintMatchesHerSecretKeyFingerprint() {
        assertEquals(new OpenPgpV4Fingerprint(julietSecRing.getPublicKey()), new OpenPgpV4Fingerprint(julietSecRing.getSecretKey()));
    }

    @Test
    public void assertJulietsFingerprintGetKeyIdMatches() {
        assertEquals("calling getKeyId() on juliet's fingerprint must return her key id.",
                TestKeys.JULIET_KEY_ID, TestKeys.JULIET_FINGERPRINT.getKeyId());
    }

    @Test
    public void assertRomeosPublicKeyIsSameInPubRingAndSecRing() throws IOException {
        assertArrayEquals(romeoSecRing.getPublicKey().getEncoded(), romeoPubRing.getPublicKey().getEncoded());
    }

    @Test
    public void assertRomeosKeyIdEquals() {
        assertEquals("Key ID of Romeo's secret key must match his key id.",
                TestKeys.ROMEO_KEY_ID, romeoSecRing.getSecretKey().getKeyID());
    }

    @Test
    public void assertRomeosKeyUIDMatches() {
        assertEquals(TestKeys.ROMEO_UID, romeoSecRing.getPublicKey().getUserIDs().next());
    }

    @Test
    public void assertRomeosKeyRingFingerprintMatches() {
        assertEquals(TestKeys.ROMEO_FINGERPRINT, new OpenPgpV4Fingerprint(romeoSecRing));
    }

    @Test
    public void assertRomeosPublicKeyFingerprintMatchesHisSecretKeyFingerprint() {
        assertEquals(new OpenPgpV4Fingerprint(romeoSecRing.getPublicKey()), new OpenPgpV4Fingerprint(romeoSecRing.getSecretKey()));
    }

    @Test
    public void assertRomesKeysFingerprintMatches() {
        assertEquals(TestKeys.ROMEO_KEY_ID, TestKeys.ROMEO_FINGERPRINT.getKeyId());
    }

    @Test
    public void assertRomeosSecretKeyRingHasSamePublicKeyId() throws IOException {
        PGPPublicKeyRing julietsPublicKeys = TestKeys.getJulietPublicKeyRing();
        assertEquals(julietSecRing.getPublicKey().getKeyID(), julietsPublicKeys.getPublicKey().getKeyID());
    }
}
