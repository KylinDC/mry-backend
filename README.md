## 项目信息
- **码如云**是一个免费的"一物一码"管理平台，可以为每一件“物品”生成一个二维码，并以该二维码为入口展开对“物品”的相关操作，典型的应用场景包括固定资产管理、设备巡检以及物品标签等
- 在技术上，码如云是一个无代码平台，全程采用DDD、整洁架构和事件驱动架构思想完成开发，更多详情可参考笔者的[DDD落地文章系列](https://docs.mryqr.com/ddd-introduction/)
- 本代码库为码如云后端代码，与之匹配的前端代码可访问：[https://github.com/mryqr-com/mry-frontend](https://github.com/mryqr-com/mry-frontend)


## 如何访问
- 访问地址：[https://www.mryqr.com](https://www.mryqr.com)


## 为什么开发码如云
- 为了证明DDD能真实落地，也为了开发出一套能让自己满意的软件系统
- 更多信息请参考笔者的文章《[构建自己的软件大厦](https://docs.mryqr.com/build-your-own-software-skyscraper/)》


## 本地运行
- 确保本地已安装Java 17+及Docker
- 本地启动：`./local-run.sh`，该命令将通过docker-compose自动运行MongoDB和Redis，再启动Spring Boot主程序，启动后访问 http://localhost:8080/about ，如可正常访问则表示启动成功
- 本地构建：`./ci-build.sh`，该命令将通过docker-compose自动运行MongoDB和Redis，再运行单元测试，API测试以及动态代码检查等构建步骤
- 如需在本地进行前后端联调，请参考[码如云前端-本地环境搭建](https://github.com/mryqr-com/mry-frontend#%E6%9C%AC%E5%9C%B0%E7%8E%AF%E5%A2%83%E6%90%AD%E5%BB%BA)


## 所有命令

| 功能                 | 命令                                                                                   | 说明                                       |
|--------------------|--------------------------------------------------------------------------------------|------------------------------------------|
| 在IntelliJ中打开工程     | `./idea.sh`                                                                          | 将自动启动IntelliJ，无需另行在IntelliJ中做导入操作        |
| 本地启动               | `./local-run.sh`                                                                     | API端口：8080, 调试端口：5005                    |
| 清空所有本地数据后再启动       | `clear-and-local-run.sh`                                                                | API端口：8080, 调试端口：5005                    |
| 本地构建               | `./ci-build`                                                                         | 将运行单元测试，API测试以及静态代码检查                    |
| 单独停止docker-compose | `./gradlew composeDown`                                                              | 将清除所有本地数据，包括MongoDB和Redis                |
| 单独启动docker-compose | `./gradlew composeUp`                                                                | 通过docker-compose启动MongoDB和Redis，如已经启动则跳过 |


## 技术栈
- Java 17，Spring Boot 3，MongoDB 4.x，Redis 6.x等


## 赞赏作者

码如云的开发和运维都由作者在业余时间完成，一路走来并不容易，如果您认为本代码库能为您带去用处与价值，您可微信扫描以下二维码赞赏作者，以请作者喝杯咖啡。

![赞赏作者](./donation.jpeg)


## 寻求微信服务号合作方

我们正在寻找能够提供微信公众号（需要是**服务号**类型）的合作方，以让码如云能够入驻在该公众号中，从而为用户提供更多更完善的功能，有意者可联系作者


