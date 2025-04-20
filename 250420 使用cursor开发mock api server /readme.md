# 背景
- 1 我司鼓励拥抱ai能力，建议大家尽量多用ai工具提高效率，解放生产力
- 2 项目过程中依赖部分外部第三方服务提供kyx screening的能力，但部分第三方不支持测试网络导致无法完整验证。

# 目的
用cursor尝试辅助进行开发，尽量减少人工coding工作量

# 项目特点
- 提供java实现的http api服务，模仿外部第三方kyx 服务商的api能力（无风险/中高风险）
- 测试人员通过直接修改python脚本动态调整自定义风险判定规则（而不影响http api服务本身）

# 项目url
https://github.com/TangCYxy/AI-Mock-Web3-Kyx-Api-Server

# 项目执行重点
- 通过rule进行项目结构等限制
- 通过动态沟通的方式让cursor调整代码
- 使用claude3.7模型

# 阶段划分
- 让ai在rule的限制下理解项目，划分项目框架和模块
- 让ai初步完成开发
- 让ai调试并修复bug
- 让ai优化功能实现
- 让ai进行测试case验证
- 让ai将测试case的编写和验证创建并补充到readme里

# 优化思路分享
- 尽量在rules里把规则限制得越明确越好
- 在每次修改的时候，明确的缩小沟通的范围，减少出错的概率
- 尽量在项目最开始的时候，由ai给出项目的结构并逐步给出rules，可以减少在rules上的时间开销
- 在发现ai逻辑不太对劲的时候，该reject就reject，如果偏差过大可以尝试返回特定check point

# 和cursor完整沟通对话内容
`
read these rules and build a java project that meets my requires. we have 2 type of providers "goplus" and "chainalysis". since you have no idea what apis and responses they provide, you can leave the api definitions behind first, then you can ask me some questions to complete your coding
`


`for the first and the second question, I put the api endpoints and formats in the folder with same name that I add to this context. one thing you need to be careful is goPlus can only execute kya test and get result within only one request. and the chainalysis can execute both kya and kyt test, and get final result with several api request(you can see the api request in the corelative endpoints and formats). and for the 3rd question, for as fast as we can to produce a mvp version, you can just simple implement a python logic: "if the amount in the request is greater than 5000 or the address starts with 1(after unboxing the hex prefix(0x)" is in risk, risk detail is "money laundry or frad". and for the question 4, no. you can update your code with my feedback, and ask me several question to complete your coding if needed
`


`for this file, I don't remember goPlus has an api endpoint requirements named "check"? check this folder and update your codes
`


`you have no need to maintain the post api for goPlus, the goplus api only provided a get request and that's all. reviewing the requirements and update your code
`


`@ChainalysisController.java @GoPlusController.java in the 2 controller files, we don't need to specify the url prefix "/api/chainalyssis" or "/api/goPlus", remove them. let's focus on the scenario: some java server request goplus or chainalysis for kyx screening. and if they want to produce customize test, all they need is to update the hostUrl in their configs. so we don't have to differ apis this project provided from the 2 outside kyx providers
`


`@ChainalysisController.java @GoPlusController.java in the 2 controller files, we don't need to specify the url prefix "/api/chainalyssis" or "/api/goPlus", remove the whole "/api/xxx" prefix and expose the same api as goPlus or chainalysis. all our api servers let's focus on the scenario: some java server request goplus or chainalysis for kyx screening. and if they want to produce customize test, all they need is to update the hostUrl in their configs. so we don't have to differ apis this project provided from the 2 outside kyx providers. 
`


`@ChainalysisController.java @chainalysis check and have an overview of the templates I defined, for chainalysis, the urls you expose is not exactly defined as we expected, update your codes. only update code associated with chainalysis, don't update goplus
`


`@ChainalysisController.java in this file we need to have a clearer ovewview for chainalysis logic. firstly, among all chainalysis url exposed, only the "alerts" url actually produce the kyx risk screening result. so only the 2 function you defined("getKyaAlerts" and "getKytAlerts") can actually call the method riskCheckService.checkRisk.remember only updates the codes related to logic of chainalysis
`


`@ChainalysisController.java and still in this  chainalysis file, I need to implement a logic that when a kya or kyt "register" request comes,we do not fill the "updatedAt" property immediately. instead, we produce a logic that is generate a "random seconds" within 0 and 10. if the "random seconds" is 0, we fill the "updatedAt" property immediately as what we do now, and if not, we compute a "validTimeStamp" property in the cachedRegistrationParams, the value is the currentTimeStamp plus the "random seconds" and leave the "updatedAt" property with unfilled. then if the "check" api request comes, we found the request with "externalId", and check if the currentTimeStamp is after the "validTimeStamp", if so, we fill the response with valid "updatedAt" property, and if not, we still don't fill the updatedAt. as what we discussed before, you should only updates the code related to the logic of chainalysis
`


`@ChainalysisController.java and still in this file, I want the data in "registrationParams" automatically expires after a configured specific time, such as 1 hour. or the data will grow infinitely as time goes. you should only updates code within this file
`


`run this api server and execute one of your test case
`


`this api server is already running and execute one of your test case
`


`according to this request, the server should response a risky alert, but seems no, you should check your code and fix it
`


`according to this request, the server should response a risky alert, but seems no, you should check your code and fix it. at least you will find a way to test the correction of python script. remember if you plan to run some python scripts, use python3 or pip3, and make it runs in a virtual environment
`


`according to this request, the server should response a risky alert, but seems no, you should check your code and fix it. at least you will find a way to test the correction of this python script @kyx_script.py . remember if you plan to run some python scripts, use python3 or pip3, and make it runs in a virtual environment. remember one thing, our mechanism of determine a kyt or kya request is in risk is implemented in the python script, so you don't need to update your java code and what you should do is update and test the python script instead
`


`according too the logs, we can see that java server still errors while initializing python interpreter and fix the problems
`


`don't add the fallback logic for the situation that python executor is not initialized properly, keep it in mind. and secondly, I need you to fix the python interpreter intializing problem, such as add some runtime environment or other method only if it can fix this problem
`


`from this error logs, still has problems with python script execution?
`


`seems that when java server still has some errors with "org.python.core.PyObject". try fix this. and if you want to restart this server, there are 2 things you should do . first is try to terminate the existed java instance and the second thing is to cd to folder "mock-kyx-api-server" firstly and then start the java server instance like you used to do
`


`i notice that there are 2 python files named kyx_script.py, one is in the root path, and another is in the java's resources dir. which one is actually invoked where java server calling?
`


`now from my sight, you need to copy ./kyx_script.py to the java project's resource path. and since the java script is important in our system, the python files needs to be collected together in a typical folder so that we can find it easier. and can we dynamically update and load python script while we update the python files immediately so that we don't have to restart the java server instance to reload and apply the new kyx assessment logic.
`


`you've got the wrong path. I need you to update the existed file @PythonScriptExecutorImpl.java in the origin java project, not to open a new one in the parent directory. now fix your problem and clean the mess up
`


`build the project and check the integrity of your test cases, and run all the test cases
`

`
since you have found the errors in goplus test cases, then fix it and re-test. you can build project without test cases and then start the server and do api test throughout "curl" like you used to do with chainalysis
`

`you should update your test case. rememeber not to add any url prefix to any controller`

`ok.forget about your test cases. I need you to build this project, and then run it, finally you make some curl requests to verify the correction of "goplus" and "chainalysis" kyx screening request`

`first. you should check the url declares in these 2 files, @GoPlusController.java @ChainalysisController.java . then start the server again and make api test for goPlus with correct url and params`

`it's ok. add your succeed goPlus cmd command for api test with risky response url in the readme file. and do a non-risky address kyx curl command request with goPlus api`

`java server instance is already running. please add risky kya and kyt curl command request to Chainalysis API endpoints documentation to readme file like you just do`

`now make a couple of curl requests to the already running java server instance . I want to see that the java server can correctly handle a risky kya request and return properly`

``

``

``

``

``

``
