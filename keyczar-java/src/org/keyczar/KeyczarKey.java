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

package org.keyczar;


import org.keyczar.enums.KeyType;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.exceptions.UnsupportedTypeException;
import org.keyczar.i18n.Messages;
import org.keyczar.interfaces.Stream;
import org.keyczar.util.Util;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Common base wrapper class for different types of KeyczarKeys (e.g. AesKey).
 * Allows generating arbitrary key types or parsing key info from JSON
 * string representations. Binds each key to a hash identifier and exposes
 * the Stream used to access the key material.
 *
 * @author steveweis@gmail.com (Steve Weis)
 * @author arkajit.dey@gmail.com (Arkajit Dey)
 *
 */
abstract class KeyczarKey {
  int size = getType().defaultSize();
  
  void copyHeader(ByteBuffer dest) {
    dest.put(Keyczar.VERSION);
    dest.put(hash());
  }
  
  @Override
  public boolean equals(Object o) {
    try {
      KeyczarKey key = (KeyczarKey) o;
      return Arrays.equals(key.hash(), this.hash());
    } catch (ClassCastException e) {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    return Util.toInt(this.hash());
  }

  abstract Stream getStream() throws KeyczarException;

  /**
   * Return this key's type
   * 
   * @return KeyType of this key
   */
  abstract KeyType getType();

  /**
   * Return this key's hash value
   * 
   * @return A byte array hash of this key material
   */
  abstract byte[] hash();
  
  int size() {
    return size;
  }
  
  /**
   * Generates private key of the desired type. Cannot generate public
   * key, instead must export public key set from private keys.
   * 
   * @param type KeyType desired
   * @return KeyczarKey of desired type
   * @throws KeyczarException for unsupported key types
   */
  static KeyczarKey genKey(KeyType type) throws KeyczarException {
    switch (type) {
      case AES:
        return AesKey.generate();
      case HMAC_SHA1:
        return HmacKey.generate();
      case DSA_PRIV:
        return DsaPrivateKey.generate();
      case RSA_PRIV:
        return RsaPrivateKey.generate();
      case RSA_PUB: case DSA_PUB:
        throw new KeyczarException(
            Messages.getString("KeyczarKey.PublicKeyExport", type));
    }
    throw new UnsupportedTypeException(type);
  }
  
  /**
   * Converts a JSON string representation of a KeyczarKey into the appropriate
   * KeyczarKey object.
   * 
   * @param type KeyType being read from JSON input
   * @param key JSON String representation of a KeyczarKey
   * @return KeyczareKey of given type
   * @throws KeyczarException if type mismatch with JSON input or unsupported
   * key type
   */
  static KeyczarKey readKey(KeyType type, String key) throws KeyczarException {
    switch (type) {
    case AES:
      return AesKey.read(key);
    case HMAC_SHA1:
      return HmacKey.read(key);
    case DSA_PRIV:
      return DsaPrivateKey.read(key);
    case DSA_PUB:
      return DsaPublicKey.read(key);
    case RSA_PRIV:
      return RsaPrivateKey.read(key);
    case RSA_PUB:
      return RsaPublicKey.read(key);
    }

    throw new UnsupportedTypeException(type);
  }
}