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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPrivateCrtKeySpec;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

import org.keyczar.RsaPublicKey.Padding;
import org.keyczar.enums.KeyType;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.DecryptingStream;
import org.keyczar.interfaces.EncryptingStream;
import org.keyczar.interfaces.SigningStream;
import org.keyczar.interfaces.Stream;
import org.keyczar.interfaces.VerifyingStream;
import org.keyczar.util.Base64Coder;
import org.keyczar.util.Util;

import com.google.gson.annotations.Expose;

/**
 * Wrapping class for RSA Private Keys
 *
 * @author steveweis@gmail.com (Steve Weis)
 * @author arkajit.dey@gmail.com (Arkajit Dey)
 *
 */
class RsaPrivateKey extends KeyczarKey implements KeyczarPrivateKey {
  private static final String KEY_GEN_ALGORITHM = "RSA";

  @Expose private RsaPublicKey publicKey;
  @Expose private String privateExponent;
  @Expose private String primeP;
  @Expose private String primeQ;
  @Expose private String primeExponentP;
  @Expose private String primeExponentQ;
  @Expose private String crtCoefficient;
  
  private static final String SIG_ALGORITHM = "SHA1withRSA";

  private RSAPrivateCrtKey jcePrivateKey;

  private RsaPrivateKey() {
    publicKey = new RsaPublicKey();
  }

  @Override
  Stream getStream() throws KeyczarException {
    return new RsaPrivateStream();
  }

  @Override
  KeyType getType() {
    return KeyType.RSA_PRIV;
  }

  @Override
  byte[] hash() {
    return publicKey.hash();
  }

  static RsaPrivateKey read(String input) throws KeyczarException {
    RsaPrivateKey key = Util.gson().fromJson(input, RsaPrivateKey.class);
    key.init();
    return key;
  }

  static RsaPrivateKey generate(Padding padding) throws KeyczarException {
    return generate(KeyType.RSA_PRIV.defaultSize(), padding);
  }

  @Override
  public KeyczarPublicKey getPublic() {
    return publicKey;
  }
  
  private void init() throws KeyczarException {
    // Read all the JSON fields and use it to instantiate a RSAPrivateCrtKey
    try {
      KeyFactory factory = KeyFactory.getInstance(KEY_GEN_ALGORITHM);
      BigInteger mod = new BigInteger(Base64Coder.decodeWebSafe(publicKey.modulus));
      BigInteger pubExp =
        new BigInteger(Base64Coder.decodeWebSafe(publicKey.publicExponent));
      
      // Set the public key values
      publicKey.set(size, mod, pubExp);
      
      BigInteger privExp = new BigInteger(Base64Coder.decodeWebSafe(privateExponent));
      BigInteger p = new BigInteger(Base64Coder.decodeWebSafe(primeP));
      BigInteger q = new BigInteger(Base64Coder.decodeWebSafe(primeQ));
      BigInteger expP = new BigInteger(Base64Coder.decodeWebSafe(primeExponentP));
      BigInteger expQ = new BigInteger(Base64Coder.decodeWebSafe(primeExponentQ));
      BigInteger crt = new BigInteger(Base64Coder.decodeWebSafe(crtCoefficient));
      RSAPrivateCrtKeySpec spec =
        new RSAPrivateCrtKeySpec(mod, pubExp, privExp, p, q, expP, expQ, crt);
      jcePrivateKey = (RSAPrivateCrtKey) factory.generatePrivate(spec);
    } catch (GeneralSecurityException e) {
      throw new KeyczarException(e);
    }    
  }

  static RsaPrivateKey generate(int keySize, Padding padding) throws KeyczarException {
    RsaPrivateKey key = new RsaPrivateKey();
    try {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_GEN_ALGORITHM);
      key.size = keySize;
      key.publicKey.size = key.size;
      kpg.initialize(key.size());
      KeyPair pair = kpg.generateKeyPair();
      key.jcePrivateKey = (RSAPrivateCrtKey) pair.getPrivate();
      key.publicKey.set(key.size, key.jcePrivateKey.getModulus(),
          key.jcePrivateKey.getPublicExponent());
      key.publicKey.setPadding(padding);
    } catch (GeneralSecurityException e) {
      throw new KeyczarException(e);
    }
    // Set all the JSON fields for this RSA Private CRT key
    key.privateExponent =
      Base64Coder.encodeWebSafe(key.jcePrivateKey.getPrivateExponent().toByteArray());
    key.primeP =
      Base64Coder.encodeWebSafe(key.jcePrivateKey.getPrimeP().toByteArray());
    key.primeQ =
      Base64Coder.encodeWebSafe(key.jcePrivateKey.getPrimeQ().toByteArray());
    key.primeExponentP =
      Base64Coder.encodeWebSafe(key.jcePrivateKey.getPrimeExponentP().toByteArray());
    key.primeExponentQ = 
      Base64Coder.encodeWebSafe(key.jcePrivateKey.getPrimeExponentQ().toByteArray());
    key.crtCoefficient =
      Base64Coder.encodeWebSafe(key.jcePrivateKey.getCrtCoefficient().toByteArray());
    return key;
  }

  @Override
  protected RSAPrivateCrtKey getJceKey() {
    return jcePrivateKey;
  }

  private class RsaPrivateStream implements SigningStream, VerifyingStream,
      DecryptingStream, EncryptingStream {
    private Cipher cipher;
    private EncryptingStream encryptingStream;
    private Signature signature;
    private VerifyingStream verifyingStream;

    public RsaPrivateStream() throws KeyczarException {
      try {
        signature = Signature.getInstance(SIG_ALGORITHM);
        verifyingStream = (VerifyingStream) publicKey.getStream();
        cipher = Cipher.getInstance(publicKey.getPadding().getCryptAlgorithm());
        encryptingStream = (EncryptingStream) publicKey.getStream();
      } catch (GeneralSecurityException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public int digestSize() {
      return getType().getOutputSize();
    }

    @Override
    public int doFinalDecrypt(ByteBuffer input, ByteBuffer output)
        throws KeyczarException {
      try {
        return cipher.doFinal(input, output);
      } catch (GeneralSecurityException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public int doFinalEncrypt(ByteBuffer input, ByteBuffer output)
        throws KeyczarException {
      return encryptingStream.doFinalEncrypt(input, output);
    }

    @Override
    public SigningStream getSigningStream() throws KeyczarException {
      return encryptingStream.getSigningStream();
    }

    @Override
    public VerifyingStream getVerifyingStream() {
      return new VerifyingStream() {
        @Override
        public int digestSize() {
          return 0;
        }

        @Override
        public void initVerify() {
          // Do nothing
        }

        @Override
        public void updateVerify(ByteBuffer input) {
          // Do nothing
        }

        @Override
        public boolean verify(ByteBuffer signature) {
          // Do nothing
          return true;
        }
      };
    }

    @Override
    public void initDecrypt(ByteBuffer input) throws KeyczarException {
      try {
        cipher.init(Cipher.DECRYPT_MODE, jcePrivateKey);
      } catch (InvalidKeyException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public int initEncrypt(ByteBuffer output) throws KeyczarException {
      return encryptingStream.initEncrypt(output);
    }

    @Override
    public void initSign() throws KeyczarException {
      try {
        signature.initSign(jcePrivateKey);
      } catch (GeneralSecurityException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public void initVerify() throws KeyczarException {
      verifyingStream.initVerify();
    }

    @Override
    public int maxOutputSize(int inputLen) {
      return getType().getOutputSize(size);
    }

    @Override
    public void sign(ByteBuffer output) throws KeyczarException {
      try {
        byte[] sig = signature.sign();
        output.put(sig);
      } catch (SignatureException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public int updateDecrypt(ByteBuffer input, ByteBuffer output)
        throws KeyczarException {
      try {
        return cipher.update(input, output);
      } catch (ShortBufferException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public int updateEncrypt(ByteBuffer input, ByteBuffer output)
        throws KeyczarException {
      return encryptingStream.updateEncrypt(input, output);
    }

    @Override
    public void updateSign(ByteBuffer input) throws KeyczarException {
      try {
        signature.update(input);
      } catch (SignatureException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public void updateVerify(ByteBuffer input) throws KeyczarException {
      verifyingStream.updateVerify(input);
    }

    @Override
    public boolean verify(ByteBuffer sig) throws KeyczarException {
      return verifyingStream.verify(sig);
    }
  }
}