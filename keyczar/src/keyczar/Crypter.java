// Keyczar (http://code.google.com/p/keyczar/) 2008

package keyczar;

import java.nio.ByteBuffer;

import keyczar.enums.KeyPurpose;
import keyczar.interfaces.DecryptingStream;
import keyczar.interfaces.VerifyingStream;

/**
 * @author steveweis@gmail.com (Steve Weis)
 * 
 */
public class Crypter extends Encrypter {
  public Crypter(KeyczarReader reader) throws KeyczarException {
    super(reader);
  }

  public Crypter(String fileLocation) throws KeyczarException {
    super(fileLocation);
  }

  // TODO: Write JavaDocs
  public byte[] decrypt(byte[] input) throws KeyczarException {
    ByteBuffer output = ByteBuffer.allocate(input.length);
    decrypt(ByteBuffer.wrap(input), output);
    output.reset();
    byte[] outputBytes = new byte[output.remaining()];
    output.get(outputBytes);
    return outputBytes;
  }

  // TODO: Write JavaDocs
  public void decrypt(ByteBuffer input, ByteBuffer output)
      throws KeyczarException {
    if (input.remaining() < HEADER_SIZE) {
      throw new ShortCiphertextException(input.remaining());
    }
    input.mark();
    byte version = input.get();
    if (version != VERSION) {
      throw new BadVersionException(version);
    }

    byte[] hash = new byte[KEY_HASH_SIZE];
    input.get(hash);
    KeyczarKey key = getKey(hash);
    if (key == null) {
      throw new KeyNotFoundException(hash);
    }

    DecryptingStream cryptStream = (DecryptingStream) key.getStream();
    VerifyingStream verifyStream = cryptStream.getVerifyingStream();

    // Set the read limit to the end of the ciphertext
    if (verifyStream.digestSize() > 0) {
      if (input.remaining() < verifyStream.digestSize()) {
        throw new ShortCiphertextException(input.remaining());
      }

      input.position(input.limit() - verifyStream.digestSize());
      ByteBuffer signature = input.slice();

      // Reset the position of the input to the header
      input.reset();
      input.limit(input.limit() - verifyStream.digestSize());
      verifyStream.initVerify();
      verifyStream.updateVerify(input);
      if (!verifyStream.verify(signature)) {
        throw new InvalidSignatureException();
      }
      // Rewind back to the start of the ciphertext
      input.reset();
      input.position(input.position() + HEADER_SIZE);
    }

    cryptStream.initDecrypt(input);
    output.mark();
    cryptStream.doFinalDecrypt(input, output);
    output.limit(output.position());
  }

  public String decrypt(String ciphertext) throws KeyczarException {
    return new String(decrypt(Util.base64Decode(ciphertext)));
  }

  /*
   * (non-Javadoc)
   * 
   * @see keyczar.Keyczar#isAcceptablePurpose(keyczar.KeyPurpose)
   */
  @Override
  boolean isAcceptablePurpose(KeyPurpose purpose) {
    return purpose == KeyPurpose.DECRYPT_AND_ENCRYPT;
  }
}
