package com.tc.test.eip7702;

import com.tc.test.eip7702.impl.Eip7702Transaction;
import org.web3j.crypto.Sign;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;

import java.nio.ByteBuffer;
import java.util.List;

public class TransactionEncoder {

    public static byte[] encode(Eip7702Transaction rawTransaction) {
        return encode(rawTransaction, null);
    }


    public static byte[] encode(Eip7702Transaction rawTransaction, Sign.SignatureData signatureData) {
        List<RlpType> values = asRlpValues(rawTransaction, signatureData);
        RlpList rlpList = new RlpList(values);
        byte[] encoded = RlpEncoder.encode(rlpList);

        // 非legacy交易，都有1个字节的transaction类型
        return ByteBuffer.allocate(encoded.length + 1)
                .put(rawTransaction.getRlpType())
                .put(encoded)
                .array();
    }


    public static List<RlpType> asRlpValues(
            Eip7702Transaction rawTransaction, Sign.SignatureData signatureData) {
        return rawTransaction.asRlpValues(signatureData);
    }
}
