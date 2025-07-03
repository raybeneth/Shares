package com.tc.test.eip7702.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class SetAuthorizationListObject {
    private BigInteger chainId;
    private String delegateToAddress;
    private BigInteger nonce;
    private byte v;
    private String r;
    private String s;

    public SetAuthorizationListObject(Long chainId, String delegateToAddress, BigInteger nonce) {
        this.chainId = BigInteger.valueOf(chainId);
        this.delegateToAddress = delegateToAddress;
        this.nonce = nonce;
    }

    public void updateSignature(Sign.SignatureData victimSignature) {
        this.v = victimSignature.getV()[0];
        if (this.v >= (byte)27) {
            this.v = (byte) (this.v - (byte)27);
        }
        this.r = Numeric.toHexString(victimSignature.getR());
        this.s = Numeric.toHexString(victimSignature.getS());
    }

    public String toMessage() {
        List<RlpType> result = new ArrayList<>();

        result.add(RlpString.create(this.getChainId()));
        result.add(RlpString.create(Numeric.hexStringToByteArray(this.getDelegateToAddress())));
        result.add(RlpString.create(this.getNonce()));

        //
        RlpList rlpList = new RlpList(result);
        byte[] encoded = RlpEncoder.encode(rlpList);

        //
        return Hash.sha3(Numeric.toHexString(ByteBuffer.allocate(encoded.length + 1)
                .put((byte)(0x05)) // magic word
                .put(encoded)
                .array()));

    }
}
