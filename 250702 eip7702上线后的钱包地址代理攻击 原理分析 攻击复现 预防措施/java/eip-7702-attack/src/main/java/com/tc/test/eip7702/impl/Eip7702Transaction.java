package com.tc.test.eip7702.impl;

import com.tc.test.eip7702.consts.Eip7702TxType;
import com.tc.test.eip7702.model.SetAuthorizationListObject;
import lombok.Getter;
import org.web3j.crypto.Sign;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Bytes;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Getter
public class Eip7702Transaction {

    private long chainId;
    private List<SetAuthorizationListObject> authList;

    private BigInteger maxPriorityFeePerGas;
    private BigInteger maxFeePerGas;

    private Byte type;
    private BigInteger nonce;
    private BigInteger gasLimit;
    private String to;
    private BigInteger value;
    private String data;

    public Eip7702Transaction(
            long chainId,
            BigInteger nonce,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            List<SetAuthorizationListObject> authList) {

        this.type = Eip7702TxType.EIP7702TxType;
        this.nonce = nonce;
        this.gasLimit = gasLimit;
        this.to = to;
        this.value = value;
        this.data = data;

        this.maxPriorityFeePerGas = maxPriorityFeePerGas;
        this.maxFeePerGas = maxFeePerGas;

        this.chainId = chainId;
        this.authList = authList;
    }

    public List<RlpType> asRlpValues(Sign.SignatureData signatureData) {
        List<RlpType> result = new ArrayList<>();

        result.add(RlpString.create(getChainId()));
        result.add(RlpString.create(getNonce()));
        result.add(RlpString.create(getMaxPriorityFeePerGas()));
        result.add(RlpString.create(getMaxFeePerGas()));
        result.add(RlpString.create(getGasLimit()));

        // an empty to address (contract creation) should not be encoded as a numeric 0 value
        String to = getTo();
        if (to != null && !to.isEmpty()) {
            // addresses that start with zeros should be encoded with the zeros included, not
            // as numeric values
            result.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        } else {
            result.add(RlpString.create(""));
        }

        result.add(RlpString.create(getValue()));

        // value field will already be hex encoded, so we need to convert into binary first
        byte[] data = Numeric.hexStringToByteArray(getData());
        result.add(RlpString.create(data));

        // access list
        result.add(new RlpList(Collections.emptyList()));

        // auth list
        List<SetAuthorizationListObject> authList = getAuthList();
        List<RlpType> rlpAuthList = new ArrayList<>();
        authList.forEach(
                entry -> {
                    List<RlpType> rlpAuthListObject = new ArrayList<>();
                    rlpAuthListObject.add(RlpString.create(entry.getChainId()));
                    rlpAuthListObject.add(RlpString.create(Numeric.hexStringToByteArray(entry.getDelegateToAddress())));
                    rlpAuthListObject.add(RlpString.create(entry.getNonce()));

//                    result.add(RlpString.create(Sign.getRecId(signatureData, getChainId())));
                    rlpAuthListObject.add(RlpString.create(BigInteger.valueOf(entry.getV())));
                    rlpAuthListObject.add(RlpString.create(Bytes.trimLeadingZeroes(Numeric.hexStringToByteArray(entry.getR()))));
                    rlpAuthListObject.add(RlpString.create(Bytes.trimLeadingZeroes(Numeric.hexStringToByteArray(entry.getS()))));
                    rlpAuthList.add(new RlpList(rlpAuthListObject));
                });
        result.add(new RlpList(rlpAuthList));

        if (signatureData != null) {
            result.add(RlpString.create(Sign.getRecId(signatureData, getChainId())));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
        }
        return result;
    }

    public byte getRlpType() {
        return type;
    }
}
