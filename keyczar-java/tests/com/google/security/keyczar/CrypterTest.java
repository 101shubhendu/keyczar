/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.security.keyczar;

import com.google.keyczar.Crypter;
import com.google.keyczar.exceptions.KeyNotFoundException;
import com.google.keyczar.exceptions.KeyczarException;
import com.google.keyczar.exceptions.ShortCiphertextException;

import junit.framework.TestCase;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

/**
 * Tests Crypter class for encrypting and decrypting with RSA and AES. 
 *
 * @author steveweis@gmail.com (Steve Weis)
 *
 */

public class CrypterTest extends TestCase {
  private static final String TEST_DATA = "./testdata";
  private String input = "This is some test data";
  
  private final void testDecrypt(String subDir) throws Exception {
    Crypter crypter = new Crypter(TEST_DATA + subDir);
    RandomAccessFile activeInput =
      new RandomAccessFile(TEST_DATA + subDir + "/1out", "r");
    String activeCiphertext = activeInput.readLine(); 
    activeInput.close();
    RandomAccessFile primaryInput =
      new RandomAccessFile(TEST_DATA + subDir + "/2out", "r");
    String primaryCiphertext = primaryInput.readLine();
    primaryInput.close();
    String activeDecrypted = crypter.decrypt(activeCiphertext);
    assertEquals(input, activeDecrypted);
    String primaryDecrypted = crypter.decrypt(primaryCiphertext);
    assertEquals(input, primaryDecrypted);
  }
  
  @Test
  public final void testAesDecrypt() throws Exception {
    testDecrypt("/aes");
  }
  
  @Test
  public final void testRsaDecrypt() throws Exception  {
    testDecrypt("/rsa");
  }
  
  @Test
  public final void testAesEncryptAndDecrypt() throws KeyczarException {
    Crypter crypter = new Crypter(TEST_DATA + "/aes");
    String ciphertext = crypter.encrypt(input);
    System.out.println("Aes Ciphertext: " + ciphertext);
    String decrypted = crypter.decrypt(ciphertext);
    assertEquals(input, decrypted);
  }

  @Test
  public final void testRsaEncryptAndDecrypt() throws KeyczarException {
    Crypter crypter = new Crypter(TEST_DATA + "/rsa");
    String ciphertext = crypter.encrypt(input);
    System.out.println("Rsa Ciphertext: " + ciphertext);
    String decrypted = crypter.decrypt(ciphertext);
    assertEquals(input, decrypted);
  }
    
  @Test
  public final void testBadAesCiphertexts() throws KeyczarException {
    Crypter crypter = new Crypter(TEST_DATA + "/aes");
    try {
      byte[] decrypted = crypter.decrypt(new byte[0]);
    } catch (ShortCiphertextException e) {
      // Expected exception
    }
    byte[] ciphertext = crypter.encrypt(input.getBytes());
    // Munge the ciphertext
    ciphertext[1] ^= 44;
    try {
      byte[] decrypted = crypter.decrypt(ciphertext);
    } catch (KeyNotFoundException e) {
      // Expected exception
    }    
  }
}