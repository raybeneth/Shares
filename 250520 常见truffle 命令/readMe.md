# 使用nvm管理node和npm版本
- nvm use 18

# 安装truffle（要求node 8.9.4版本以上）
- npm install -g truffle

# 通过truffle编译合约
- truffle compile --compile-all

# 通过truffle发布合约到链上
- truffle migrate --network mumbai

# 通过truffle验证链上合约（不一定非要通过truffle发布）
- truffle run verify DEIStablecoin@0x7a46D0348f0E64f4280E40669c4AAAc58be037e9 --network mumbai --debug

# 通过truffle进行控制台操作
- truffle console --network mumbai

# 通过truffle查看合约大小(64KB)
- truffle run contract-size --contract xxx
- 依赖库 truffle-contract-size, 依赖合约已经编译完成

# 通过truffle flatten合约（如果直接通过truffleVerify之后就没必要flatten了, flatten主要是多文件因为依赖操作很难验证，直接flatten成单独文件还方便一些）
- truffle-flattener contracts/LDEI/LDEI.sol > ./flattened.sol
- 依赖库    sudo npm install -g truffle-flattener

# truffle源代码编译，引用openzeppelin,提示缺什么就补什么, 不带版本就是安装最新
- npm install @openzeppelin/contracts-upgradeable@4.9.0
- npm install @openzeppelin/contracts@4.4.0

# truffle安装hdwallet, 用于部署私钥等链上交互
- @truffle/hdwallet-provider  或者 truffle-hdwallet-provider, 使用

# 常见异常场景
- 无法安装hdwallet, 使用这个 npm i @truffle/hdwallet-provider@next
https://blog.csdn.net/u012329294/article/details/125072418
- truffle migrate的时候找不到对应的contract, 就查看build目录下，目标合约的类的名字，而不是sol文件的名字，切记！
https://ethereum.stackexchange.com/questions/42711/the-reason-for-could-not-find-artifacts-for-in-truffle

# 参考文档
- 通过truffle部署和验证合约   https://blog.51cto.com/shijianfeng/5258340
- 通过truffle验证合约   https://blog.csdn.net/topc2000/article/details/121509560
