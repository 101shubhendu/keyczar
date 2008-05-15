// Keyczar (http://code.google.com/p/keyczar/) 2008

package keyczar;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import keyczar.internal.DataPacker;
import keyczar.internal.DataUnpacker;
import keyczar.internal.Util;

/**
 * Manages a Keyczar key set. Keys will not be read from a KeyczarReader until
 * the read() method is called.
 *
 * @author steveweis@gmail.com (Steve Weis)
 */
public abstract class Keyczar {
  private final KeyMetadata kmd; 
  private KeyVersion primaryVersion;
  //final ArrayList<KeyczarKey> keys = new ArrayList<KeyczarKey>();  
  private final HashMap<Integer, KeyczarKey> keyMap =
    new HashMap<Integer, KeyczarKey>();
  private final HashMap<KeyVersion, KeyczarKey> versionMap =
    new HashMap<KeyVersion, KeyczarKey>();
  

  /**
   * Instantiates a new Keyczar object with a KeyczarFileReader instantiated
   * with the given file location 
   * 
   * @param fileLocation 
   * @throws KeyczarException 
   */
  public Keyczar(String fileLocation) throws KeyczarException {
    this(new FileReader(fileLocation));
  }
  
  /**
   * Instantiates a new Keyczar object by passing it a Keyczar reader object 
   * 
   * @param reader A KeyczarReader to read keys from
   * @throws KeyczarException 
   */
  public Keyczar(KeyczarReader reader) throws KeyczarException {
    // Reads keys from the KeyczarReader
    InputStream metaData = reader.getMetadata();
    DataUnpacker metaDataUnpacker = new DataUnpacker(metaData);
    this.kmd = KeyMetadata.getMetadata(metaDataUnpacker);
    if (!isAcceptablePurpose(kmd.getPurpose())) {
      throw new KeyczarException("Unacceptable purpose: "
          + kmd.getPurpose());
    }
    for (KeyVersion version : kmd.getVersions()) {
      if (version.getStatus() == KeyStatus.PRIMARY) {
        if (primaryVersion != null) {
          throw new KeyczarException(
              "Key sets may only have a single primary version");
        }
        primaryVersion = version;
      }
      InputStream keyData = reader.getKey(version.getVersionNumber());
      DataUnpacker keyDataUnpacker = new DataUnpacker(keyData);
      KeyczarKey key = KeyczarKey.fromType(kmd.getType());
      key.read(keyDataUnpacker);
      if (keyMap.containsKey(key.hashCode())) {
        throw new KeyczarException("Key identifiers cannot collide");
      }
      keyMap.put(key.hashCode(), key);
      versionMap.put(version, key);
    }
  }
  
  protected KeyczarKey getKey(byte[] hash) {
    return keyMap.get(Util.toInt(hash));
  }

  protected KeyczarKey getPrimaryKey() {
    if (primaryVersion == null) {
      return null;
    }
    return getKey(primaryVersion);
  }
    
  protected abstract boolean isAcceptablePurpose(KeyPurpose purpose);

  // For KeyczarTool only
  void addVersion(KeyStatus status) throws KeyczarException {
    KeyVersion version = new KeyVersion(numVersions() + 1, status, false);
    if (status == KeyStatus.PRIMARY) {
      if (primaryVersion != null) {
        primaryVersion.setStatus(KeyStatus.ACTIVE);
      }
      primaryVersion = version;
    }
    KeyczarKey key = KeyczarKey.fromType(kmd.getType());
    do {
      // Make sure no keys collide on their identifiers
      key.generate();
    } while (getKey(key.hash()) != null);
    addKey(version, key);
  }
  
  int numVersions() {
    return versionMap.size();
  }
  
  void addKey(KeyVersion version, KeyczarKey key) {
    keyMap.put(key.hashCode(), key);
    versionMap.put(version, key);
    kmd.addVersion(version);
  }
  
  Iterator<KeyVersion> getVersions() {
    return Collections.unmodifiableSet(versionMap.keySet()).iterator();
  }
  
  KeyczarKey getKey(KeyVersion v) {
    return versionMap.get(v);
  }
  
  int writeMetadata(DataPacker packer) throws KeyczarException {
    return kmd.write(packer);
  }
  
  int writeVersion(KeyVersion v, DataPacker packer) throws KeyczarException {
    return versionMap.get(v).write(packer);
  }
}