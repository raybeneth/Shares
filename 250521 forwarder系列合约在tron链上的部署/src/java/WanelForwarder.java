import org.apache.commons.lang3.StringUtils;
import org.aspectj.util.FileUtil;
import org.tron.trident.abi.FunctionEncoder;
import org.tron.trident.abi.FunctionReturnDecoder;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.DynamicArray;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.Type;
import org.tron.trident.abi.datatypes.generated.Bytes32;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.exceptions.IllegalException;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.core.transaction.TransactionBuilder;
import org.tron.trident.crypto.Hash;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Numeric;

import java.io.Console;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WanelForwarderCaller {

    // origin and initialized
//    private static final String FORWARDER_CONTRACT = "TV1cnTHxQohk1BgVucrKGhM1sAfzufPkXz"; // Forwarder 合约地址
//     auto created ones(random owner)
//    private static final String FORWARDER_CONTRACT = "TNuoyWCK8tC3GkPf5REuTkW2JykwU95YaW";
    // auto created another one
//    private static final String FORWARDER_CONTRACT = "TMnV3b5j3VAxTg2RZfnBApd3ANERmLMizw";
    // auto created another one
//    private static final String FORWARDER_CONTRACT = "TMLm3KHY7kUzNWwggwgWppQry42r6HnWkW";
    // auto created another one
    private static final String FORWARDER_CONTRACT = "TWkWptzSRj45qPCRSssm9jJz37eTpaAaiK";

    private static final String TOKEN_ADDRESS_USDD = "TT99HGyWbpFYebGP4F4xkf5KyYB51nsrer";
    private static final String FORWARDER_FACTORY_CONTRACT = "TCXQtBxVLHvkzjKZsBFuBG2BZioFo21Npj";

    private static ApiWrapper wrapper;

    private static  String PRIVATE_KEY = "";

    public static void main(String[] args) throws Exception {

        // 设置合约地址和私钥
        PRIVATE_KEY = StringUtils.trim(FileUtil.readAsString(new File("/Users/tc/.tronSK")));
        KeyPair keyPair = new KeyPair(PRIVATE_KEY);
        String fromAddress = keyPair.toBase58CheckAddress();
        wrapper = ApiWrapper.ofShasta(PRIVATE_KEY);

        // 读取合约数据
//        readParentAddress(fromAddress);

        // 提前计算新合约地址

        // 通过工厂合约部署新合约
//        callDeployAndInit(fromAddress, Arrays.asList(TOKEN_ADDRESS_USDD), Hash.sha3(String.valueOf(System.currentTimeMillis())));

        // 初始化
//        callInit(fromAddress, Arrays.asList(TOKEN_ADDRESS_USDD));

        /**
         * 调试通过
         */
//        getTxDetail("0a37b39bc6b8c6d1b029be88afd44fe907f28d38b840d761e026d50a506d659d");


//        callFlush(fromAddress);

        callFlushToken(fromAddress, TOKEN_ADDRESS_USDD);
    }

    private static void callDeployAndInit(String fromAddress, List<String> tokenAddresses, String saltInHex) throws Exception {
        Function createForwarder = new Function("createForwarder", Arrays.asList(
                new Address(fromAddress),
                new DynamicArray(Address.class, tokenAddresses.stream().map(Address::new).collect(Collectors.toList())),
                new Bytes32(Numeric.hexStringToByteArray(saltInHex))
        ), Collections.emptyList());

        // 相当于是构造交易
        Response.TransactionExtention txn = wrapper.triggerContract(fromAddress, FORWARDER_FACTORY_CONTRACT, FunctionEncoder.encode(createForwarder),
                0L, 0L, "", 500000000L);

        TransactionBuilder tb = new TransactionBuilder(txn.getTransaction());
        tb.setFeeLimit(1000000000L);
        tb.setMemo("tcCreateForwarder");
        Chain.Transaction tx = tb.build();

        tx = wrapper.signTransaction(tx);

        String sentResult = wrapper.broadcastTransaction(tx);
        System.out.println(sentResult);

        printResult("createForwarder", txn);
    }

    private static void readParentAddress(String fromAddress) {
        Function getParentAddress = new Function("parentAddress", Collections.emptyList(), Arrays.asList(new TypeReference<Address>() {}));

        // 相当于是构造交易
        Response.TransactionExtention txn = wrapper.triggerConstantContract(fromAddress, FORWARDER_CONTRACT, getParentAddress);
        System.out.println(Numeric.toHexString(txn.getConstantResultList().get(0).toByteArray()));
        String addressOut = Numeric.toHexString(txn.getConstantResult(0).toByteArray());
        List<Type> decode = FunctionReturnDecoder.decode(addressOut, getParentAddress.getOutputParameters());
        System.out.println(decode);

    }

    private static void getTxDetail(String txHash) throws IllegalException {
        Response.TransactionInfo txInfo = wrapper.getTransactionInfoById(txHash);

        Chain.Transaction tx = wrapper.getTransactionById(txHash);

        System.out.println(txInfo);
    }

    // 调用 init(address parent, address[] tokens)
    public static void callInit(String fromAddress, List<String> tokenAddresses) throws Exception {

        Function init = new Function("init", Arrays.asList(
                new Address(fromAddress),
                new DynamicArray(Address.class, tokenAddresses.stream().map(Address::new).collect(Collectors.toList()))

        ), Collections.emptyList());

        // 相当于是构造交易
        Response.TransactionExtention txn = wrapper.triggerContract(fromAddress, FORWARDER_CONTRACT, FunctionEncoder.encode(init),
                0L, 0L, "", 500000000L);

        TransactionBuilder tb = new TransactionBuilder(txn.getTransaction());
        tb.setFeeLimit(400000000L);
        tb.setMemo("abcd1234");
        Chain.Transaction tx = tb.build();

        tx = wrapper.signTransaction(tx);

        String sentResult = wrapper.broadcastTransaction(tx);
        System.out.println(sentResult);

        printResult("init", txn);
    }

    // 调用 flush()
    public static void callFlush(String fromAddress) throws Exception {
        Function flushToken = new Function("flush",
                Collections.emptyList(),
                Collections.emptyList()
        );

        // 相当于是构造交易
        Response.TransactionExtention txn = wrapper.triggerContract(fromAddress, FORWARDER_CONTRACT, FunctionEncoder.encode(flushToken),
                0L, 0L, "", 500000000L);

        TransactionBuilder tb = new TransactionBuilder(txn.getTransaction());
        tb.setFeeLimit(600000022L);
        tb.setMemo("66668888");
        Chain.Transaction tx = tb.build();

        tx = wrapper.signTransaction(tx);

        String sentResult = wrapper.broadcastTransaction(tx);
        System.out.println(sentResult);

        printResult("flush", txn);
    }

    // 调用 flushToken(address token)
    public static void callFlushToken(String fromAddress, String tokenAddress) throws Exception {
        Function flushToken = new Function("flushTokens",
                Arrays.asList(new Address(tokenAddress)),
                Collections.emptyList()
        );

        // 相当于是构造交易
        Response.TransactionExtention txn = wrapper.triggerContract(fromAddress, FORWARDER_CONTRACT, FunctionEncoder.encode(flushToken),
                0L, 0L, "", 500000000L);

        TransactionBuilder tb = new TransactionBuilder(txn.getTransaction());
        tb.setFeeLimit(600000022L);
        tb.setMemo("4321dcba");
        Chain.Transaction tx = tb.build();

        tx = wrapper.signTransaction(tx);

        String sentResult = wrapper.broadcastTransaction(tx);
        System.out.println(sentResult);

        printResult("flushToken", txn);
    }

    private static void printResult(String method, Response.TransactionExtention txn) {
        if (txn.getResult().getResult()) {
            String txid = Numeric.toHexString(txn.getTxid().toByteArray());
            System.out.println(method + " txid: " + txid);
        } else {
            System.err.println(method + " failed: " + txn.getResult().getMessage().toStringUtf8());
        }
    }
}
