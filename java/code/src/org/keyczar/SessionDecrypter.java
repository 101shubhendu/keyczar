/*
 * Copyright 2010 Google Inc.
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

package org.keyczar;

import org.keyczar.annotations.Experimental;
import org.keyczar.exceptions.Base64DecodingException;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.util.Base64Coder;

/**
 * A SessionDecrypter will be instantiated with session material containing an
 * encrypted symmetric key. That key will be decrypted with the given
 * {@link Crypter} and used to instantiate another {@link Crypter}.
 * 
 * @author steveweis@gmail.com (Steve Weis)
 *
 */
@Experimental
public class SessionDecrypter {
  private final Crypter symmetricCrypter;
  
  /**
   * @param crypter The crypter to decrypt session material with
   * @param sessionString An encrypted symmetric key to decrypt
   * @throws KeyczarException If there is an error during decryption
   * @throws Base64DecodingException If there is an error decoding the session string. 
   */
  public SessionDecrypter(Crypter crypter, String sessionString)
      throws Base64DecodingException, KeyczarException {
    this(crypter, Base64Coder.decodeWebSafe(sessionString));
  }

  /**
   * @param crypter The crypter to decrypt session material with
   * @param sessionMaterial An encrypted symmetric key to decrypt
   * @throws KeyczarException If there is an error during decryption
   */
  public SessionDecrypter(Crypter crypter, byte[] sessionMaterial)
      throws KeyczarException {
    byte[] packedKeys = crypter.decrypt(sessionMaterial);
    AesKey aesKey = AesKey.fromPackedKey(packedKeys);
    ImportedKeyReader importedKeyReader = new ImportedKeyReader(aesKey);
    this.symmetricCrypter = new Crypter(importedKeyReader);
  }

  /**
   * Decrypts a ciphertext byte array using the session key.
   */
  public byte[] decrypt(byte[] ciphertext) throws KeyczarException {
    return symmetricCrypter.decrypt(ciphertext);
  }
  
  /**
   * Decrypts a ciphertext string using the session key.
   */
  public String decrypt(String ciphertext) throws KeyczarException {
    return symmetricCrypter.decrypt(ciphertext);
  }
}
