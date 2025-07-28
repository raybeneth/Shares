import {
    Wallet,
    JsonRpcProvider,
    TransactionRequest,
    hexlify,
    toUtf8Bytes,
    toBigInt,
    ethers,
    sha256,
    keccak256
} from "ethers";

import * as dotenv from "dotenv";
dotenv.config();

function getProvider() {
    return new JsonRpcProvider("http://127.0.0.1:8547", {
        name: "sepolia",
        chainId: 11155111,
    })
}


const calcInitializeCallData = async () => {
    const abi = [
        "function owner()",
        "function getName()",
        "function directlyRevert()",
        "function initialize(address newOwner)",
    ];

    // 创建接口
    const iface = new ethers.Interface(abi);

    // 定义参数
    const owner = "0x1111111111111111111111111111111111111111";

    // 编码 calldata
    const calldata = iface.encodeFunctionData("initialize", [owner]);

    console.log("Calldata:", calldata);
    return calldata;
}

const main = async () => {

    const provider = getProvider()

    const wallet = new Wallet(process.env.VICTIM_PRIVATE_KEY!).connect(provider);
    const submitterWallet = new Wallet(process.env.ORIGIN_SUBMITTER_PRIVATE_KEY!).connect(provider);

    //
    const delegationAddress = process.env.EIP_7702_DELEGATION_CONTRACT_ADDRESS!;
    const auth = await wallet.authorize({
        address: delegationAddress,
        nonce: await wallet.getNonce(),
        chainId: provider._network.chainId,
    });

    const feeData = await provider.getFeeData();
    const tx: TransactionRequest = {
        type: 4,
        authorizationList: [auth],
        chainId: provider._network.chainId,
        to: wallet.address,
        nonce: await submitterWallet.getNonce(),
        value: 0,
        gasLimit: 80000,
        data: await calcInitializeCallData(),
        maxFeePerGas: toBigInt(feeData.maxFeePerGas || 42),
        maxPriorityFeePerGas: toBigInt(feeData.maxPriorityFeePerGas || 2),
    }

    // 构造并签名交易
    const signedTx = await wallet.signTransaction(tx)

    // 发送交易
    const txHash = await provider.send('eth_sendRawTransaction', [signedTx]);
    console.log("standard rpc provider result, expected tx hash/actual tx hash is  ", keccak256(signedTx), txHash)
}

main().then(async () => {
    process.exit(0)
})
