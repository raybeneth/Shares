const crypto = require('crypto');

// 生成你和 B 的密钥对
const ec = crypto.createECDH('secp256k1');
ec.generateKeys();
const yourPrivateKey = ec.getPrivateKey();
const yourPublicKey = ec.getPublicKey();
console.log("yourPublicKey ", yourPublicKey);

const ecB = crypto.createECDH('secp256k1');
ecB.generateKeys();
const bPrivateKey = ecB.getPrivateKey();
const bPublicKey = ecB.getPublicKey();
console.log("bPublicKey ", bPublicKey);

// 你计算共享密钥
const yourSharedKey = ec.computeSecret(bPublicKey);

// B 计算共享密钥
const bSharedKey = ecB.computeSecret(yourPublicKey);

// 检查共享密钥是否相同
if (yourSharedKey.equals(bSharedKey)) {
    console.log('共享密钥相同');
} else {
    console.log('共享密钥不同');
}
