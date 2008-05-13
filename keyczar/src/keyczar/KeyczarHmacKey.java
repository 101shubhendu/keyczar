// Keyczar (http://code.google.com/p/keyczar/) 2008

package keyczar;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import keyczar.internal.*;

/**
 * Wrapping class for HMAC-SHA1 keys
 * 
 * @author steveweis@gmail.com (Steve Weis)
 *
 */
public class KeyczarHmacKey extends KeyczarKey {
  private static final String MAC_ALGORITHM = "HMACSHA1";
  private Key hmacKey;
  private final byte[] hash = new byte[Constants.KEY_HASH_SIZE];
  private int hashCode;
    
  private void init(byte[] keyBytes) {
    byte[] fullHash = Util.hash(Util.fromInt(keyBytes.length), keyBytes);
    System.arraycopy(fullHash, 0, hash, 0, hash.length);
    hashCode = Util.toInt(hash);
    this.hmacKey = new SecretKeySpec(keyBytes, MAC_ALGORITHM);
  }

  @Override
  protected byte[] hash() {
    return hash;
  }  
  
  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  protected void read(DataUnpacker unpacker)
      throws KeyczarException {
    int typeValue = unpacker.getInt();
    if (typeValue != getType().getValue()) {
      throw new KeyczarException("Invalid key type for HMAC: " +
          KeyType.getType(typeValue));
    }
    byte[] keyMaterial = unpacker.getArray();
    init(keyMaterial);
  }

  @Override
  protected void generate() {
    init(Util.rand(getType().defaultSize()));
  }

  @Override
  protected int write(DataPacker packer) throws KeyczarException {
    if (hmacKey == null) {
      throw new KeyczarException("Cannot write uninitialized key");
    }
    int written = packer.putInt(getType().getValue());
    written += packer.putArray(hmacKey.getEncoded());
    return written;
  }

  @Override
  protected KeyType getType() {
    return KeyType.HMAC_SHA1;
  }
  
  @Override
  protected Stream getStream() throws KeyczarException {
    return new HmacStream();
  }

  private class HmacStream extends Stream implements VerifyingStream, SigningStream {
    private Mac hmac;

    public int digestSize() {
      return hmac.getMacLength();
    }
    
    public HmacStream() throws KeyczarException {
      try {
        this.hmac = Mac.getInstance(MAC_ALGORITHM);
      } catch (GeneralSecurityException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public void initVerify() throws KeyczarException {
      initSign();
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

    @Override
    public void initSign() throws KeyczarException {
      try {
        hmac.init(hmacKey);
      } catch (GeneralSecurityException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public void sign(ByteBuffer output) {
      output.put(hmac.doFinal());
    }

    @Override
    public void updateSign(ByteBuffer input) {
      hmac.update(input);
    }    
  }
}
