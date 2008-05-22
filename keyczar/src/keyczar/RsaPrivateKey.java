// Keyczar (http://code.google.com/p/keyczar/) 2008

package keyczar;

import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

import keyczar.enums.KeyType;
import keyczar.interfaces.DecryptingStream;
import keyczar.interfaces.EncryptingStream;
import keyczar.interfaces.SigningStream;
import keyczar.interfaces.VerifyingStream;

/**
 * Wrapping class for RSA Private Keys
 * 
 * @author steveweis@gmail.com (Steve Weis)
 * 
 */
class RsaPrivateKey extends KeyczarPrivateKey {
  private static final String CRYPT_ALGORITHM = "RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING";
  private static final String KEY_GEN_ALGORITHM = "RSA";
  
  @Expose private RsaPublicKey publicKey;

  private static final String SIG_ALGORITHM = "SHA1withRSA";

  RsaPrivateKey() {
    publicKey = new RsaPublicKey();
  }

  @Override
  Stream getStream() throws KeyczarException {
    return new RsaPrivateStream();
  }

  @Override
  String getKeyGenAlgorithm() {
    return KEY_GEN_ALGORITHM;
  }


  @Override
  KeyType getType() {
    return KeyType.RSA_PRIV;
  }

  @Override
  KeyczarPublicKey getPublic() {
    return publicKey;
  }

  @Override
  void setPublic(KeyczarPublicKey pub) throws KeyczarException {
    publicKey = (RsaPublicKey) pub;
    publicKey.init();
  }

  private class RsaPrivateStream extends Stream implements SigningStream,
      VerifyingStream, DecryptingStream, EncryptingStream {
    private Cipher cipher;
    private EncryptingStream encryptingStream;
    private Signature signature;
    private VerifyingStream verifyingStream;

    public RsaPrivateStream() throws KeyczarException {
      try {
        signature = Signature.getInstance(SIG_ALGORITHM);
        verifyingStream = (VerifyingStream) publicKey.getStream();
        cipher = Cipher.getInstance(CRYPT_ALGORITHM);
        encryptingStream = (EncryptingStream) publicKey.getStream();
      } catch (GeneralSecurityException e) {
        throw new KeyczarException(e);
      }
    }

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
        cipher.init(Cipher.DECRYPT_MODE, getJcePrivateKey());
      } catch (InvalidKeyException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public byte[] initEncrypt() throws KeyczarException {
      return encryptingStream.initEncrypt();
    }

    @Override
    public void initSign() throws KeyczarException {
      try {
        signature.initSign(getJcePrivateKey());
      } catch (GeneralSecurityException e) {
        throw new KeyczarException(e);
      }
    }

    @Override
    public void initVerify() throws KeyczarException {
      verifyingStream.initVerify();
    }

    @Override
    public int ivSize() {
      return 0;
    }

    @Override
    public int maxOutputSize(int inputLen) {
      // TODO Auto-generated method stub
      return getType().getOutputSize() * 2;
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
