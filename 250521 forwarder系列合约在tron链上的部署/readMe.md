# 背景
将之前提到过的
- 资金退款逻辑还需要进一步优化（避免无法往后回退，退款流程可能要更细致，需要指定某个退款地址作为退款逻辑，或者收入进来再退）

# 重点功能
- 部署factory和forwarder系列合约
- 提前计算forwarder合约地址给用户充值
- 当用户充值完毕后，创建该合约并回收上地址上资产到parentAddress
- 测试链 shasta

# 重点区别
- tron链有自己的trc10/trc20合约，如果用户转账到这个地址上，可能导致用户体验异常，需要进行针对性
- 
# 重点关注
- 流程是否畅通（关注tvm地址转换部分）
- 实际消耗的gas费用数量

# 结论
- **合约代码方面**，forwarder合约可以无缝部署到tron链, 没有问题
- **java sdk对接方面**，使用trident 操作ok，
- **重点功能方面**，均表现正常
  - 接受trc20转账，通过factory创建并init合约，flush主币和token到parent地址均ok
- **交易成本方面**
  - 几乎不可行，相比直接eoa地址转账而言，在目标合约不是热门合约的前提下，需要接近8万的能量（shasta测试网络）

# 成本计算详情
- 成本统计
- 当前时点能量和带宽兑换比例
  - 测试网络
    - 1 trx = 1000带宽。 质押获取0.5带宽
    - 1 trx = 2381能量。 质押获取0.3能量
    - 质押能量获取效率 
  - 主网
    - 1 trx = 1000带宽。 质押获取1.5带宽
    - 1 trx = 4761能量。 质押获取10能量
- 通过常规eoa地址接受并trc20 token 转账的成本
  - 先转账trx给临时地址（激活账号，否则无法签名转出）
    - 还没测试，但是小于后续2者相加。
    - 固定消耗1个trx，并且有trx管理或能量(带宽)管理成本
  - 直接转出token
    - 能量 13132
    - 带宽 346
    - 示例交易 9945e63c66ace69d03c1e86bdd0578ac77a5077c1de09b486eeda5a480f0b851
    - 
- 通过create2信令部署并转移token信令的成本
  - 单独创建并部署合约
  - 单独调用flushToken方法转出资产
    - 能量 15381
    - 带宽 324 
    - 示例交易 9c682491c8fd3b76e433cb631710d28d6f51a78ecd56f760321facb4ad62f0fd
  - 部署合约并转出token
    - 能量 444
    - 带宽 65915
    - 示例交易 f3dfc2e35e7f8c2a6e1554839fef5e0c5213096ccd17fbf08e1aa660bf99d552
       

# 部署
## 部署自定义ERC20合约（USDD）
- 在tronide中部署
- TT99HGyWbpFYebGP4F4xkf5KyYB51nsrer
- [USDD(Mock)](./src/contract/USDD.sol)
- 
## 部署模板合约WanelForwarder
- 在tronide中部署
- TV1cnTHxQohk1BgVucrKGhM1sAfzufPkXz
- [WanelForwarder](./src/contract/WanelForwarder.sol)
- 
## 部署工厂合约 ForwarderFactory
- 在tronide中部署
- TCXQtBxVLHvkzjKZsBFuBG2BZioFo21Npj
- [ForwarderFactory](./src/contract/WanelForwarder.sol)

## java sdk使用
- 使用java代码编写并实现功能
- [WanelForwarder.java](./src/java/WanelForwarder.java)