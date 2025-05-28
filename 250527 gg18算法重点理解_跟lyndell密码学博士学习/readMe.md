# 背景
- 之前通过lyndell老师的视频入门了gg18的算法，有了整体上，入门级的理解
- 希望再过一遍lyndell老师的视频，深化相关技术的理解
- 仍然还有很多密码学的地方似懂非懂，但是逐步深入

# 视频链接
【密码学系列课程6 GG18门限签名-p1】 https://www.bilibili.com/video/BV1ds4y1c7Xa/?share_source=copy_web&vd_source=ddf35a2beeb153b9285efc6e3973cf05

# gg18算法中的重点密码学概念

## paillier同态加密
- ![img_2.png](images/readMe/img_2.png)
- 大素数p，q
- 密文计算输入

### 简单的，同态乘法,加法推导
- encPK(x)方法即为对x进行同态加密 
  - 简单理解为g^x就行
- 加法同态 ⊕ （密文 + 密文）
  - ![img.png](images/readMe/img.png)
  - 重点是在于直接把2个密文c1和c2相乘，相当于就是，底数相同指数相加
    - c1 = encPK(m1)
    - c2 = encPK(m2)
    - 所以c1 ⊕ c2 = c1 * c2 = g^m1 * g^m2 = g^(m1 + m2) = EncPK(m1 + m2 mod n)
- 乘法同态 ⊗（随机数 * 密文）
  - ![img_1.png](images/readMe/img_1.png)
  - 重点在于直接把随机数作为指数和密文进行运算
    - c = encPK(m) 
    - 所以 a ⊗ c = c^a = (g^m)^a = g^(am) = encPK(a * m mod n) 

## mta协议
### 重点
- 乘法转加法协议，还是先看老师的图
  - 1 暂时不过于关注过程中的zk证明，先搞明白

  - 2 mta是个协议，比较抽象，可以有很多个算法来实现
    - paillier只是其中之一的实现，并且不一定是最优的算法
  - 3 重点是：同态加法和同态乘法的应用
  - ![img_3.png](images/readMe/img_3.png)
### 场景
- A和B手里分别有一个秘密a，b，通过计算后，希望安全的得到ab的值
  - (A广播⍺,B广播β)

### 步骤
- 1 A构造c1 = encPK(a) —>
  - 相当于用paillier加密算法的公钥PK加密了a这个秘密，最终可以用A手里对应的sk进行解密。

- 2 B构造 c2 = (b ⊗ c1) ⊕ encPK(β') 
  - = (b ⊕ encPK(a)) ⊕ encPK(β') 
  - = encPK(ab) ⊕ encPK(β') 
  - = encPK(ab + β')
    - 此时B计算得到β = -β', 并将c2发送给A
    - 实际值的计算就是(c1^b)*encPK(β')

- 3 A直接用私钥sk解密b发送过来的数据c2，得到⍺ = ab + β'.

- 此时 ⍺ + β = (ab + β') + (-β') = ab, 完美
