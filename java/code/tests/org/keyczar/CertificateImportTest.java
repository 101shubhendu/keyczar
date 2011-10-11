// Copyright 2011 Google Inc. All Rights Reserved.

package org.keyczar;

import junit.framework.TestCase;

import org.keyczar.enums.KeyPurpose;

import java.io.FileInputStream;

/**
 * @author swillden@google.com (Shawn Willden)
 */
public class CertificateImportTest extends TestCase {
  private static final String TEST_DATA = "./testdata/certificates/";
  private static final String[] FILE_FORMATS = { "pem", "der" };
  private static final String[] KEY_TYPES = { "rsa", "dsa" };
  private String input = "This is some test data";

  private void doTestCryptImport(String fileFormat) throws Exception {
    Encrypter encrypter = new Encrypter(new X509CertificateReader(
        KeyPurpose.ENCRYPT, new FileInputStream(TEST_DATA + "rsa-crypt-crt." + fileFormat)));

    String ciphertext = encrypter.encrypt(input);
    String plaintext = new Crypter(TEST_DATA + "rsa-crypt").decrypt(ciphertext);
    assertEquals(input, plaintext);
  }

  public void testCryptImport() throws Exception {
    for (String format : FILE_FORMATS) {
      doTestCryptImport(format);
    }
  }

  private void doTestSignImport(String keyType, String fileFormat) throws Exception {
    String signature = new Signer(TEST_DATA + keyType + "-sign").sign(input);

    Verifier verifier = new Verifier(new X509CertificateReader(
        KeyPurpose.VERIFY, new FileInputStream(TEST_DATA + keyType + "-sign-crt." + fileFormat)));
    assertTrue(verifier.verify(input, signature));
  }

  public void testSignerImport() throws Exception {
    for (String format : FILE_FORMATS) {
      for (String keyType : KEY_TYPES) {
        doTestSignImport(keyType, format);
      }
    }
  }
}
