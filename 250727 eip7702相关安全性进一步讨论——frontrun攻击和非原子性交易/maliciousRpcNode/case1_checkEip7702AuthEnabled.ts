import {Wallet, JsonRpcProvider, TransactionRequest, hexlify, toUtf8Bytes, toBigInt, ethers} from "ethers";

import * as dotenv from "dotenv";
dotenv.config();

function getProvider() {
    return new JsonRpcProvider(process.env.FORWARD_TO_RPC!, {
        name: "sepolia",
        chainId: 11155111,
    })
}

const main = async () => {

    const provider = getProvider()

    const abi = [
        "function owner() view returns (address)",
        "function getName() view returns (string)",
        "function directlyRevert()",
        "function initialize(address newOwner)",
    ];

    const contract = new ethers.Contract(process.env.VICTIM_ADDRESS!, abi, provider);

    const greets = await contract.getName();
    console.log("contract greets message is ", greets);
}

main().then(async () => {
    process.exit(0)
})
