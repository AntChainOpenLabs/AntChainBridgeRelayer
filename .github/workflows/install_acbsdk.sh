#
# Copyright 2023 Ant Group
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#!/bin/bash

CURR_DIR="$(cd `dirname $0`; pwd)"

echo "install BID SDK ..."
git clone -b release/1.0.0 https://github.com/caict-4iot-dev/BID-SDK-JAVA.git
cd BID-SDK-JAVA/BID-SDK
mvn install -Dmaven.test.skip=true
cd -
git clone -b release/1.0.2 https://github.com/caict-4iot-dev/BIF-Core-SDK.git
cd BIF-Core-SDK/bif-chain-sdk
mvn install -Dmaven.test.skip=true
cd -
echo "install BID SDK finished"

echo "install ACB SDK ..."
git clone -b feat/bcdns_support https://github.com/AntChainOpenLabs/AntChainBridgePluginSDK.git
cd AntChainBridgePluginSDK/antchain-bridge-commons
mvn install -Dmaven.test.skip=true
cd -
cd AntChainBridgePluginSDK/antchain-bridge-spi
mvn install -Dmaven.test.skip=true
cd -
cd AntChainBridgePluginSDK/antchain-bridge-bcdns
mvn install -Dmaven.test.skip=true
cd -
echo "install ACB SDK finished"