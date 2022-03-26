/*
 * Copyright 2022 Craft Network
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

package network.craft.score;

import com.iconloop.score.token.irc31.IRC31Basic;
import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.External;

import java.math.BigInteger;

public class PresaleMultiToken extends IRC31Basic {
    private static final int ERROR_NOT_CONTRACT_OWNER = 1;

    // id ==> creator
    private final DictDB<BigInteger, Address> creators = Context.newDictDB("creators", Address.class);

    @External(readonly=true)
    public String name() {
        return "PresaleMultiToken";
    }

    private void checkOwnerOrThrow() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            Context.revert(ERROR_NOT_CONTRACT_OWNER, "NotContractOwner");
        }
    }
}
