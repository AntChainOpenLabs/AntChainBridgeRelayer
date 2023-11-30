/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.relayer.core.service.validation;

import javax.annotation.Resource;

import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageTrustLevelEnum;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageV2;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UniformCrosschainPacketStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.UniformCrosschainPacketContext;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UniformCrosschainPacketValidator {

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    public boolean doProcess(UniformCrosschainPacketContext ucpContext) {
        // TODO : get the tp-proof from the PTC
        log.debug("for now we skip tp-proof part");

        crossChainMessageRepository.updateUniformCrosschainPacketState(
                ucpContext.getUcpId(),
                UniformCrosschainPacketStateEnum.PROVED
        );
        if (ucpContext.getUcp().getSrcMessage().getType() == CrossChainMessage.CrossChainMessageType.AUTH_MSG) {
            IAuthMessage authMessage = AuthMessageFactory.createAuthMessage(ucpContext.getUcp().getSrcMessage().getMessage());
            if (AuthMessageV2.MY_VERSION == authMessage.getVersion()) {
                if (((AuthMessageV2) authMessage).getTrustLevel() != AuthMessageTrustLevelEnum.NEGATIVE_TRUST) {
                    return true;
                }
            }
        }
        crossChainMessageRepository.updateAuthMessageState(
                ucpContext.getUcpId(),
                AuthMsgProcessStateEnum.PROVED
        );

        return true;
    }
}
