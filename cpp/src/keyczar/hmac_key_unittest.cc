// Copyright 2009 Sebastien Martini (seb@dbzteam.org)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
#include <string>
#include <vector>

#include "base/base64w.h"
#include "base/logging.h"
#include "base/ref_counted.h"
#include "base/file_path.h"
#include "base/file_util.h"
#include "base/scoped_ptr.h"
#include "base/values.h"
#include "testing/gtest/include/gtest/gtest.h"

#include "keyczar/hmac_key.h"
#include "keyczar/key.h"
#include "keyczar/key_type.h"
#include "keyczar/keyczar_test.h"
#include "keyczar/keyset_file_reader.h"

namespace keyczar {

class HMACTest : public KeyczarTest {
 protected:
  // Loads HMAC key from JSON file.
  scoped_refptr<HMACKey> LoadHMACKey(const FilePath& path,
                                     int key_version) {
    KeysetFileReader reader(path);
    scoped_ptr<Value> value(reader.ReadKey(key_version));
    EXPECT_NE(static_cast<Value*>(NULL), value.get());
    scoped_refptr<HMACKey> hmac_key(HMACKey::CreateFromValue(*value));
    CHECK(hmac_key);
    return hmac_key;
  }
};

TEST_F(HMACTest, GenerateKeyAndSign) {
#ifdef COMPAT_KEYCZAR_06B
  scoped_ptr<KeyType> key_type(KeyType::Create("HMAC_SHA1"));
#else
  scoped_ptr<KeyType> key_type(KeyType::Create("HMAC"));
#endif
  ASSERT_TRUE(key_type.get());
  const std::vector<int> sizes = key_type->sizes();

  scoped_refptr<HMACKey> hmac_key;
  std::string signature;
  for (std::vector<int>::const_iterator iter = sizes.begin();
       iter != sizes.end(); ++iter) {
    hmac_key = HMACKey::GenerateKey(*iter);
    ASSERT_TRUE(hmac_key.get());

    EXPECT_TRUE(hmac_key->Sign(input_data_, &signature));
    EXPECT_TRUE(hmac_key->Verify(input_data_, signature));
  }
}

TEST_F(HMACTest, LoadKeyAndVerify) {
  FilePath hmac_path = data_path_.AppendASCII("hmac");
  scoped_refptr<HMACKey> hmac_key = LoadHMACKey(hmac_path, 1);

  std::string b64w_signature;
  EXPECT_TRUE(file_util::ReadFileToString(hmac_path.AppendASCII("1.out"),
                                          &b64w_signature));
  std::string signature;
  EXPECT_TRUE(Base64WDecode(b64w_signature, &signature));

  // Checks signature
  input_data_.push_back(Key::GetVersionByte());
  EXPECT_TRUE(hmac_key->Verify(input_data_,
                               signature.substr(Key::GetHeaderSize())));
}

}  // namespace keyczar
