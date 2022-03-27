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
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;

public class PresaleMultiToken extends IRC31Basic {
    private static final BigInteger EXA = BigInteger.valueOf(1_000_000_000_000_000_000L);
    private static final BigInteger MAX_PRESALES = BigInteger.valueOf(9999);
    private static final String TOBEREVEALED_URI = "TO_BE_REVEALED_URI";
    private static final Address CFT_ESCROW_ADDRESS = Address.fromString("cx9c4698411c6d9a780f605685153431dcda04609f");

    // presale states
    private final VarDB<BigInteger> presalePrice = Context.newVarDB("presale_price", BigInteger.class);
    private final VarDB<Boolean> presaleOpened = Context.newVarDB("presale_opened", Boolean.class);
    private final VarDB<BigInteger> presaleId = Context.newVarDB("presale_id", BigInteger.class);
    private final VarDB<Address> craftEscrow = Context.newVarDB("craft_escrow_address", Address.class);
    private final VarDB<Address> treasury = Context.newVarDB("treasury_address", Address.class);
    private final VarDB<BigInteger> presaleLatestBlock = Context.newVarDB("presale_latest_block", BigInteger.class);

    @External(readonly=true)
    public String name() {
        return "PresaleMultiToken";
    }

    private void checkOwnerOrThrow() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            Context.revert(1, "NotContractOwner");
        }
    }

    @External(readonly=true)
    public boolean presaleOpened() {
        return presaleOpened.getOrDefault(false);
    }

    @External(readonly=true)
    public BigInteger presaleId() {
        return presaleId.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly=true)
    public BigInteger presalePrice() {
        return presalePrice.getOrDefault(BigInteger.ZERO);
    }

    @External
    public void setPresalePrice(BigInteger _price) {
        checkOwnerOrThrow();
        Context.require(!presaleOpened(), "Price cannot be changed during presale");
        presalePrice.set(_price);
    }

    @External(readonly=true)
    public Address craftEscrow() {
        return craftEscrow.getOrDefault(CFT_ESCROW_ADDRESS);
    }

    @External
    public void setCraftEscrow(Address _address) {
        checkOwnerOrThrow();
        craftEscrow.set(_address);
    }

    @External(readonly=true)
    public Address treasury() {
        return treasury.getOrDefault(Context.getOwner());
    }

    @External
    public void setTreasury(Address _address) {
        checkOwnerOrThrow();
        treasury.set(_address);
    }

    @External
    public void openPresale() {
        checkOwnerOrThrow();
        Context.require(!presaleOpened(), "Presale already opened");
        // presale price should be at least one ICX
        Context.require(presalePrice().compareTo(EXA) >= 0, "Presale price is not properly set");
        if (presaleId().compareTo(MAX_PRESALES) < 0) {
            presaleOpened.set(true);
        }
    }

    @External
    public void closePresale() {
        checkOwnerOrThrow();
        Context.require(presaleOpened(), "Presale already closed");
        _closePresale();
    }

    private void _closePresale() {
        presaleOpened.set(false);
    }

    @External(readonly=true)
    public BigInteger presaleLatestBlock() {
        return presaleLatestBlock.getOrDefault(BigInteger.ZERO);
    }

    @Payable
    @External
    public void presaleMint(BigInteger _amount) {
        Context.require(presaleOpened(), "Presale is closed");
        Context.require(_amount.signum() > 0, "Amount should be positive");
        Context.require(presaleId().add(_amount).compareTo(MAX_PRESALES) <= 0, "Not enough items left");
        Context.require(Context.getValue().equals(presalePrice().multiply(_amount)), "Invalid price");

        int count = _amount.intValue();
        while (--count >= 0) {
            _presaleMint();
        }
    }

    private void _presaleMint() {
        var newId = presaleId().add(BigInteger.ONE);
        Context.require(newId.compareTo(MAX_PRESALES) <= 0, "All items have been minted");

        final Address caller = Context.getCaller();
        super._mint(caller, newId, BigInteger.ONE);
        super._setTokenURI(newId, TOBEREVEALED_URI);
        presaleId.set(newId);

        // CRAFT PRESALE FEATURES LOGIC HERE
        Context.call(presalePrice(), craftEscrow(), "presaleTxRouter", caller, treasury());

        presaleLatestBlock.set(BigInteger.valueOf(Context.getBlockHeight()));
        PresalePurchase(caller, newId);

        if (newId.equals(MAX_PRESALES)) {
            _closePresale();
        }
    }

    @External
    public void nftReveal(BigInteger _id, String _uri) {
        checkOwnerOrThrow();
        Context.require(!presaleOpened(), "Presale should be closed");
        Context.require(presaleId().equals(MAX_PRESALES), "All items should be minted");
        super._setTokenURI(_id, _uri);
    }

    @External
    public void mintRemaining() {
        checkOwnerOrThrow();
        Context.require(!presaleOpened(), "Presale should be closed");
        var newId = presaleId().add(BigInteger.ONE);
        Context.require(newId.compareTo(MAX_PRESALES) <= 0, "All items have been minted");

        super._mint(treasury(), newId, BigInteger.ONE);
        super._setTokenURI(newId, TOBEREVEALED_URI);
        presaleId.set(newId);
    }

    @EventLog(indexed=1)
    public void PresalePurchase(Address buyer, BigInteger newId) {}
}
