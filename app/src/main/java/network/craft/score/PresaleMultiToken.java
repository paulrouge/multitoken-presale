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
import network.craft.score.util.EnumerableSet;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.List;
import score.DictDB;

public class PresaleMultiToken extends IRC31Basic {
    private static final BigInteger EXA = BigInteger.valueOf(1_000_000_000_000_000_000L);
    private static final Address CFT_ESCROW_ADDRESS = Address.fromString("cx9c4698411c6d9a780f605685153431dcda04609f");
    private static final Address CRAFT_TREASURY = Address.fromString("hxde4d8d2cff324c85565778b7c5baf2acf20b5522");
    // sejong testnet
    // private static final Address CFT_ESCROW_ADDRESS = Address.fromString("cx0c50240f364e8c8c5b67ce328e2c3eb09560dd36");
    private final String TOBEREVEALED_URI;
    private final BigInteger MAX_SALES;
    // presale states
    private final VarDB<BigInteger> presalePrice = Context.newVarDB("presale_price", BigInteger.class);
    private final VarDB<Boolean> presaleOpened = Context.newVarDB("presale_opened", Boolean.class);
    private final VarDB<BigInteger> mintId = Context.newVarDB("mint_id", BigInteger.class);
    private final VarDB<Address> craftEscrow = Context.newVarDB("craft_escrow_address", Address.class);
    private final VarDB<Address> treasury = Context.newVarDB("treasury_address", Address.class);

    private final VarDB<BigInteger> mintLimit = Context.newVarDB("mint_limit", BigInteger.class);
    private final DictDB<Address,BigInteger> mintCount = Context.newDictDB("mint_count", BigInteger.class);
    // private final VarDB<BigInteger> presaleLatestBlock = Context.newVarDB("presale_latest_block", BigInteger.class);
    private final VarDB<Boolean> requireWhitelist = Context.newVarDB("require_whitelist", Boolean.class);
    private final EnumerableSet<Address> whitelist = new EnumerableSet<>("whitelist", Address.class);

    // extension
    private final VarDB<BigInteger> regularPrice = Context.newVarDB("regular_price", BigInteger.class);
    private final VarDB<Boolean> regularSaleOpenend = Context.newVarDB("regular_sale_opened", Boolean.class);
    

    public PresaleMultiToken(String TOBEREVEALED_URI, int MAX_SALES) {
        this.TOBEREVEALED_URI = TOBEREVEALED_URI;
        this.MAX_SALES = BigInteger.valueOf(MAX_SALES);
    }

    @External(readonly=true)
    public String name() {
        return "OLO Collection";
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
    public String unrevealedURI() {
        return this.TOBEREVEALED_URI;
    }

    @External(readonly=true)
    public BigInteger maxPresale() {
        return this.MAX_SALES;
    }

    @External(readonly=true)
    public boolean requireWhitelist() {
        return requireWhitelist.getOrDefault(false);
    }

    @External(readonly=true)
    public boolean isWhitelisted(Address _address) {
        return whitelist.contains(_address);
    }

    @External(readonly=true)
    public BigInteger mintLimit() {
        return mintLimit.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly=true)
    public BigInteger mintCount(Address _address) {
        return mintCount.getOrDefault(_address, BigInteger.ZERO);
    }

    @External(readonly=true)
    public BigInteger mintId() {
        return mintId.getOrDefault(BigInteger.ZERO);
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

    @External(readonly = true)
    public boolean regularSaleOpened() {
        return regularSaleOpenend.getOrDefault(false);
    }
    
    @External
    public void setRegularPrice(BigInteger _price) {
        checkOwnerOrThrow();
        regularPrice.set(_price);
    }

    @External
    public void setRegularSaleOpened(boolean _opened) {
        checkOwnerOrThrow();
        regularSaleOpenend.set(_opened);
    }

    @External(readonly=true)
    public BigInteger regularPrice() {
        return regularPrice.getOrDefault(BigInteger.ZERO);
    }

    @External
    public void setMintLimit(BigInteger _count) {
        checkOwnerOrThrow();
        mintLimit.set(_count);
    }

    @External
    public void addWhitelist(Address[] _adr) {
        checkOwnerOrThrow();
        for (int i = 0; i < _adr.length; i++) {
            whitelist.add(_adr[i]);
        }
    }

    @External
    public void removeWhitelist(Address[] _adr) {
        checkOwnerOrThrow();
        for (int i = 0; i < _adr.length; i++) {
            whitelist.remove(_adr[i]);
        }
    }

    @External(readonly=true)
    public List<Address> whitelist() {
        int length = whitelist.length();
        Address[] entries = new Address[length];
        for (int i = 0; i < length; i++) {
            entries[i] = whitelist.at(i);
        }
        return List.of(entries);
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

    // treasury is the address that will receive the minting payment ?
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
        if (mintId().compareTo(MAX_SALES) < 0) {
            presaleOpened.set(true);
        }
    }

    @External
    public void enableWhitelist() {
        checkOwnerOrThrow();
        Context.require(!requireWhitelist(), "Whitelist already required");
        requireWhitelist.set(true);
    }

    @External
    public void disableWhitelist() {
        checkOwnerOrThrow();
        Context.require(requireWhitelist(), "Whitelist already disabled");
        _disableWhitelist();
    }

    private void _disableWhitelist() {
        requireWhitelist.set(false);
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

    // @External(readonly=true)
    // public BigInteger presaleLatestBlock() {
    //     return presaleLatestBlock.getOrDefault(BigInteger.ZERO);
    // }

    @Payable
    @External
    public void presaleMint(BigInteger _amount) {
        Context.require(presaleOpened(), "Presale is closed");
        if (requireWhitelist()) {
            Context.require(whitelist.contains(Context.getCaller()), "Address not whitelisted");
        }
        if(mintLimit().compareTo(BigInteger.ZERO) > 0){
            Context.require(mintCount(Context.getCaller()).add(_amount).compareTo(mintLimit()) < 0, "Mint limit");
        }
        Context.require(_amount.signum() > 0, "Amount should be positive");
        Context.require(mintId().add(_amount).compareTo(MAX_SALES) <= 0, "Not enough items left");
        Context.require(Context.getValue().equals(presalePrice().multiply(_amount)), "Invalid price");

        int count = _amount.intValue();
        while (--count >= 0) {
            _presaleMint();
        }
    }

    private void _presaleMint() {
        var newId = mintId().add(BigInteger.ONE);
        Context.require(newId.compareTo(MAX_SALES) <= 0, "All items have been minted");

        final Address caller = Context.getCaller();
        super._mint(caller, newId, BigInteger.ONE);
        super._setTokenURI(newId, TOBEREVEALED_URI);
        mintId.set(newId);

        var serviceFee = presalePrice().multiply(BigInteger.valueOf(100)).divide(BigInteger.valueOf(10000));
        var netPrice = presalePrice().subtract(serviceFee);

        Context.transfer(PresaleMultiToken.CRAFT_TREASURY, serviceFee);
        // CRAFT PRESALE FEATURES LOGIC HERE
        Context.call(netPrice, craftEscrow(), "presaleTxRouter", caller, treasury());

        // presaleLatestBlock.set(BigInteger.valueOf(Context.getBlockHeight()));
        PresalePurchase(caller, newId);

        if (newId.equals(MAX_SALES)) {
            _closePresale();
        }
    }

    @Payable
    @External
    public void regularMint(BigInteger _amount) {
        Context.require(regularSaleOpened(), "Regular sale is closed");
        if(mintLimit().compareTo(BigInteger.ZERO) > 0){
            Context.require(mintCount(Context.getCaller()).add(_amount).compareTo(mintLimit()) < 0, "Mint limit");
        }
        Context.require(_amount.signum() > 0, "Amount should be positive");
        Context.require(mintId().add(_amount).compareTo(MAX_SALES) <= 0, "Not enough items left");
        Context.require(Context.getValue().equals(regularPrice().multiply(_amount)), "Invalid price");

        int count = _amount.intValue();
        while (--count >= 0) {
            _regularMint();
        }
    }

    private void _regularMint() {
        var newId = mintId().add(BigInteger.ONE);
        Context.require(newId.compareTo(MAX_SALES) <= 0, "All items have been minted");

        final Address caller = Context.getCaller();
        super._mint(caller, newId, BigInteger.ONE);
        super._setTokenURI(newId, TOBEREVEALED_URI); // TODO: change to real URI?
        mintId.set(newId);

        var serviceFee = regularPrice().multiply(BigInteger.valueOf(100)).divide(BigInteger.valueOf(10000));
        var netPrice = regularPrice().subtract(serviceFee);

        Context.transfer(PresaleMultiToken.CRAFT_TREASURY, serviceFee);
        // CRAFT PRESALE FEATURES LOGIC HERE
        Context.call(netPrice, craftEscrow(), "presaleTxRouter", caller, treasury());

        // presaleLatestBlock.set(BigInteger.valueOf(Context.getBlockHeight()));
        RegularPurchase(caller, newId);

        if (newId.equals(MAX_SALES)) {
            _closePresale();
        }
    }
    
    @External
    public void freeMint(BigInteger _amount, Address _address) {
        checkOwnerOrThrow();
        Context.require(_amount.signum() > 0, "Amount should be positive");
        Context.require(mintId().add(_amount).compareTo(MAX_SALES) <= 0, "Not enough items left");

        int count = _amount.intValue();
        while (--count >= 0) {
            _freeMint(_address);
        }
    }

    private void _freeMint(Address _address) {
        var newId = mintId().add(BigInteger.ONE);
        Context.require(newId.compareTo(MAX_SALES) <= 0, "All items have been minted");

        super._mint(_address, newId, BigInteger.ONE);
        super._setTokenURI(newId, TOBEREVEALED_URI);
        mintId.set(newId);

        PresalePurchase(_address, newId);

        if (newId.equals(MAX_SALES)) {
            _closePresale();
        }
    }

    @External
    public void nftReveal(BigInteger _id, String _uri) {
        checkOwnerOrThrow();
        Context.require(!presaleOpened(), "Presale should be closed");
        Context.require(mintId().equals(MAX_SALES), "All items should be minted");
        super._setTokenURI(_id, _uri);
    }

    // @External
    // public void mintRemaining() {
    //     checkOwnerOrThrow();
    //     Context.require(!presaleOpened(), "Presale should be closed");
    //     var newId = mintId().add(BigInteger.ONE);
    //     Context.require(newId.compareTo(MAX_SALES) <= 0, "All items have been minted");

    //     super._mint(treasury(), newId, BigInteger.ONE);
    //     super._setTokenURI(newId, TOBEREVEALED_URI);
    //     mintId.set(newId);
    // }

    @EventLog(indexed=1)
    public void PresalePurchase(Address buyer, BigInteger newId) {}

    @EventLog(indexed=1)
    public void RegularPurchase(Address buyer, BigInteger newId) {}
}
