// Keyczar (http://code.google.com/p/keyczar/) 2008

package com.google.keyczar;

import com.google.gson.annotations.Expose;
import com.google.keyczar.enums.KeyType;
import com.google.keyczar.exceptions.KeyczarException;
import com.google.keyczar.interfaces.SigningStream;
import com.google.keyczar.interfaces.Stream;
import com.google.keyczar.interfaces.VerifyingStream;
import com.google.keyczar.util.Base64Coder;
import com.google.keyczar.util.Util;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


/**
 * Wrapping class for HMAC-SHA1 keys
 * 
 * @author steveweis@gmail.com (Steve Weis)
 * 
 */
class HmacKey extends KeyczarKey {
  private Integer hashCodeObject;
  private int hashCode;
  private Key hmacKey;
  private static final String MAC_ALGORITHM = "HMACSHA1";
  
  @Expose private byte[] hash = new byte[Keyczar.KEY_HASH_SIZE];
  @Expose private String hmacKeyString;
  @Expose private KeyType type = getType();

  @Override
  public Integer hashKey() {
    return hashCodeObject;
  }
  
  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public String toString() {
    return Util.gson().toJson(this);
  }

  @Override
  void generate() throws KeyczarException {
    byte[] keyBytes = Util.rand(getType().defaultSize() / 8);
    type = getType();
    hmacKeyString = Base64Coder.encode(keyBytes);
    byte[] fullHash = Util.prefixHash(keyBytes);
    System.arraycopy(fullHash, 0, hash, 0, hash.length);
    init();
  }
  
  void init() throws KeyczarException {
    byte[] keyBytes = Base64Coder.decode(hmacKeyString);
    byte[] fullHash = Util.prefixHash(keyBytes);
    for (int i = 0; i < hash.length; i++) {
      if (hash[i] != fullHash[i]) {
        throw new KeyczarException("Hash does not match");
      }
    }
    hashCode = Util.toInt(hash);
    hashCodeObject = new Integer(hashCode);
    hmacKey = new SecretKeySpec(keyBytes, MAC_ALGORITHM);
  }

  @Override
  Stream getStream() throws KeyczarException {
    return new HmacStream();
  }

  @Override
  KeyType getType() {
    return KeyType.HMAC_SHA1;
  }

  @Override
  byte[] hash() {
    return hash;
  }

  @Override
  void read(String input) throws KeyczarException {
    HmacKey copy = Util.gson().fromJson(input, HmacKey.class);
    if (copy.type != getType()) {
      throw new KeyczarException("Invalid type in input: " + copy.type);
    }
    type = copy.type;
    hmacKeyString = copy.hmacKeyString;
    hash = copy.hash;
    init();
  }

  private class HmacStream implements VerifyingStream, SigningStream {
    private Mac hmac;

    public HmacStream() throws KeyczarException {
      try {
        hmac = Mac.getInstance(MAC_ALGORITHM);
      } catch (GeneralSecurityException e) {
        throw new KeyczarException(e);
      }
    }

    public int digestSize() {
      return getType().getOutputSize();
    }

    @Override
    public void initSign() throws KeyczarException {
      try {
        hmac.init(hmacKey);
      } catch (GeneralSecurityException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public void initVerify() throws KeyczarException {
      initSign();
    }

    @Override
    public void sign(ByteBuffer output) {
      output.put(hmac.doFinal());
    }

    @Override
    public void updateSign(ByteBuffer input) {
      hmac.update(input);
    }

    @Override
    public void updateVerify(ByteBuffer input) {
      updateSign(input);
    }

    @Override
    public boolean verify(ByteBuffer signature) {
      byte[] sigBytes = new byte[digestSize()];
      signature.get(sigBytes);

      return Arrays.equals(hmac.doFinal(), sigBytes);
    }
  }
}
