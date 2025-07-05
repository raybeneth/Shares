package com.tc.test;

import com.tc.test.eip7702.impl.Eip7702Transaction;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.util.FileUtil;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Web3Util {

    public static String signByCredentials(Credentials credentials, String hexString, Boolean needHash) {
        Sign.SignatureData signatureData = null;
        if (!needHash) {
            byte[] message = Numeric.hexStringToByteArray(hexString);
            if (message.length != 32) {
                throw new RuntimeException("invalid msg to sign " + hexString);
            }
            signatureData = Sign.signMessage(message, credentials.getEcKeyPair(), false);

        } else {
            byte[] message = Numeric.hexStringToByteArray(hexString);
            signatureData = Sign.signMessage(message, credentials.getEcKeyPair(), true);
        }
        return toSignatureHexString(signatureData);
    }

    public static String toSignatureHexString(Sign.SignatureData signatureData) {
        String r = Numeric.cleanHexPrefix(Numeric.toHexString(signatureData.getR()));
        String s = Numeric.cleanHexPrefix(Numeric.toHexString(signatureData.getS()));
        String v = Numeric.cleanHexPrefix(Numeric.toHexString(signatureData.getV()));
        return r + s + v;
    }

    public static Sign.SignatureData parseSignatureHexString(String hexString) throws Exception {
        hexString = Numeric.cleanHexPrefix(hexString);
        byte[] r = Numeric.hexStringToByteArray(hexString.substring(0, 64));
        byte[] s = Numeric.hexStringToByteArray(hexString.substring(64, 128));
        byte v = Numeric.hexStringToByteArray(hexString.substring(128))[0];

        return new Sign.SignatureData(v, r, s);
    }

    public static String signRawTransaction(Map<String, String> privateKeyPairs, String senderAddress, RawTransaction rawTransaction) throws Exception {
        // 序列化tx数据
        byte[] txRlpBytes = TransactionEncoder.encode(rawTransaction);

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
        byte[] signedTxBytes = TransactionEncoder.encode(rawTransaction, signatureData);
        return Numeric.toHexString(signedTxBytes);
    }

    public static Map<String, String> loadPrivateKeyPairsFromLocalFile() throws IOException {
        ConcurrentHashMap<String, String> evmAccounts = new ConcurrentHashMap<>();
        String evmSKs = FileUtil.readAsString(new File("/Users/tc/.evmSK"));
        for (String accountPairs : StringUtils.split(evmSKs, "\n")) {
            String[] accountInfo = StringUtils.split(accountPairs, ",");
            evmAccounts.put(accountInfo[0], accountInfo[1]);
        }
        return evmAccounts;
    }

    public static Map<Long, List<String>> loadHttpRpcUrlFromLocalFile() throws IOException {
        ConcurrentHashMap<Long, List<String>> urls = new ConcurrentHashMap<>();
        String evmSKs = FileUtil.readAsString(new File("/Users/tc/.web3HttpRpcUrl"));
        for (String urlPair : StringUtils.split(evmSKs, "\n")) {
            String[] accountInfo = StringUtils.split(urlPair, ",");
            Long chainId = Long.parseLong(accountInfo[0]);
            List<String> url = urls.get(chainId);
            if (url == null) {
                urls.put(chainId, new ArrayList<>(Arrays.asList(accountInfo[1])));
            } else {
                url.add(accountInfo[1]);
            }
        }
        return urls;
    }

    public static void createAccount() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        // 生成新的密钥对
        ECKeyPair keyPair = Keys.createEcKeyPair();

        // 从密钥对创建凭证
        Credentials credentials = Credentials.create(keyPair);

        // 获取私钥（十六进制字符串）
        String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);

        // 获取公钥（十六进制字符串）
        String publicKey = credentials.getEcKeyPair().getPublicKey().toString(16);

        // 获取以太坊地址
        String address = credentials.getAddress();

        System.out.println("Private Key: 0x" + privateKey);
        System.out.println("Public Key: 0x" + publicKey);
        System.out.println("Ethereum Address: " + address);
    }



    public static String buildSafeTxHashByContractInteract(Web3j web3j, String toAddress, BigInteger ethValue, String calldata, String safeContractAddress) throws IOException {
        // 构造input param
        List<Type> inputParameters = new ArrayList<>();
        Address to = new Address(toAddress);
        Uint256 value = new Uint256(ethValue);
        DynamicBytes callData = new DynamicBytes(Numeric.hexStringToByteArray(calldata));
        Uint8 operation = new Uint8(1);
        Uint256 safeTxGas = new Uint256(0);
        Uint256 baseGas = new Uint256(0);
        Uint256 gasPrice = new Uint256(0);
        Address gasToken = new Address("0x0000000000000000000000000000000000000000");
        Address refundReceiver = new Address("0x0000000000000000000000000000000000000000");
        Uint256 _nonce = new Uint256(getSafeContractNonce(safeContractAddress, web3j, null));
        inputParameters.add(to);
        inputParameters.add(value);
        inputParameters.add(callData);
        inputParameters.add(operation);
        inputParameters.add(safeTxGas);
        inputParameters.add(baseGas);
        inputParameters.add(gasPrice);
        inputParameters.add(gasToken);
        inputParameters.add(refundReceiver);
        inputParameters.add(_nonce);
        // 构造output param
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        TypeReference<Bytes32> typeReference = new TypeReference<Bytes32>() {
        };
        outputParameters.add(typeReference);
        // 构造function数据
        Function function = new Function("getTransactionHash", inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);
        // 构造eth call
        Transaction transaction = Transaction.createEthCallTransaction(safeContractAddress, safeContractAddress, data);
        // call
        EthCall ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
        List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        return Numeric.toHexString(((Bytes32) results.get(0)).getValue());
    }

    public static BigInteger getSafeContractNonce(String safeContractAddress, Web3j web3j, BigInteger blockNumber) throws IOException {
        // 构造input param
        List<Type> inputParameters = new ArrayList<>();
        // 构造output param
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        TypeReference<Uint256> typeReference = new TypeReference<Uint256>() {
        };
        outputParameters.add(typeReference);
        // 构造function数据
        Function function = new Function("nonce", inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);
        // 构造eth call
        Transaction transaction = Transaction.createEthCallTransaction(safeContractAddress, safeContractAddress, data);
        // call
        EthCall ethCall = web3j.ethCall(transaction, blockNumber == null ? DefaultBlockParameterName.LATEST : DefaultBlockParameter.valueOf(blockNumber)).send();
        // 一定会失败，从失败message里尝试解析gasCount
        List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        return (BigInteger) results.get(0).getValue();
    }
}
