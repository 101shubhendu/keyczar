// Copyright 2011 Google Inc. All Rights reserved.
//
// Author: Shawn Willden (swillden@google.com)
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

#include <keyczar/base/scoped_ptr.h>
#include <keyczar/keyczar.h>
#include <keyczar/keyczar_test.h>

namespace keyczar {

void KeyczarTest::TestSignAndVerify(const std::string& sign_key,
                                    const std::string& verify_key) const {
  std::string signature;

  const FilePath private_path = data_path_.Append(sign_key);
  scoped_ptr<Signer> signer(Signer::Read(private_path.value()));
  ASSERT_TRUE(signer.get());
  EXPECT_TRUE(signer->Sign(input_data_, &signature));

  const FilePath public_path = data_path_.Append(verify_key);
  scoped_ptr<Verifier> verifier(Verifier::Read(public_path.value()));
  ASSERT_TRUE(verifier.get());
  EXPECT_TRUE(verifier->Verify(input_data_, signature));
}

void KeyczarTest::TestSignAndVerifyUnversioned(
    const std::string& sign_key,
    const std::string& verify_key) const {
  std::string signature;

  const FilePath private_path = data_path_.Append(sign_key);
  scoped_ptr<UnversionedSigner> signer(
      UnversionedSigner::Read(private_path.value()));
  ASSERT_TRUE(signer.get());
  EXPECT_TRUE(signer->Sign(input_data_, &signature));

  const FilePath public_path = data_path_.Append(verify_key);
  scoped_ptr<UnversionedVerifier> verifier(
      UnversionedVerifier::Read(public_path.value()));
  ASSERT_TRUE(verifier.get());
  EXPECT_TRUE(verifier->Verify(input_data_, signature));
}

}  // namespace keyczar
