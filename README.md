## 一言·古诗词 API

<img src="https://v1.jinrishici.com/all.svg">

### 简介

古诗词·一言API 是一个可以随机返回一句古诗词名句的接口。具有以下特点：

* 快：使用 Vert.x Java 全异步框架开发，使用 Redis 数据库，确保毫秒级稳定响应。
* 全：支持 svg / txt / json / png 调用，满足你在任何地方的调用需求
* 准：可以根据你的喜好，在指定的分类中进行随机返回
* 稳：提供完整单元测试，已经过数百万次调用

本仓库是该项目的开源代码仓库，仅提供测试数据，其余数据请自行采集。

项目首页：[https://gushi.ci](https://gushi.ci)


### 新版接口上线：今日诗词API

现已推出新版在线一句话 API：今日诗词 可供调用。不开源。

今日诗词API 可以根据不同地点、时间、节日、季节、天气、景观、城市进行智能推荐古诗词。

官网：[https://www.jinrishici.com](https://www.jinrishici.com)

本 Github 项目继续开源并维护。


### 安装

1. 根据 Maven 安装依赖
2. maven package 可打成 Jar 包
3. 配置 redis，导入 dump.rdb 数据 （放到 redis 目录下，重启 redis，如果原来有这个文件请自行导出数据再替换，原有数据会全部清除）
4. 运行：
    1. 服务器运行 `java -server -jar yiyan-1.0-fat.jar -conf conf.json`  conf.json从下面可以复制出来自己配
    2. IDE运行 启动类 `io.vertx.core.Launcher` 参数 `run ma.luan.yiyan.MainVerticle -conf src/main/resources/conf.json`
        
5. 如果要自行采集数据，请仿照原有 redis 中的 key 格式采集，log自动添加，help为可选，需要自己填充 json 和 img 两个 key 集。古诗词数据每个分类一个 set，set的内容是 json 或者 base64 加密的图片。其他细节请自行查看源码。

### 使用

#### API举例

* [https://v1.jinrishici.com/all.json](https://v1.jinrishici.com/all.json)
* [https://v1.jinrishici.com/all.svg](https://v1.jinrishici.com/all.svg)
* [https://v1.jinrishici.com/shuqing/libie.png](https://v1.jinrishici.com/shuqing/libie.png)
* [https://v1.jinrishici.com/rensheng.txt](https://v1.jinrishici.com/rensheng.txt)

#### API地址格式(仅支持https)

`https://v1.jinrishici.com/{一级分类}/{二级分类(可选)}.{返回格式(可选)}`

查看所有目前支持的分类：[https://v1.jinrishici.com/](https://v1.jinrishici.com/)

目前支持的后缀：.svg .txt .png .json 不加后缀默认返回 json

#### SVG 调用

```html
<img src="https://v1.jinrishici.com/all.svg">
```

SVG后缀是我们推荐的最优调用方案，可以在部分论坛、任何博客、小程序内无损直接调用，并且可以一定程度上控制样式。缺点是部分老旧浏览器不支持。

你可以直接修改svg控制最大长度
```html
<img src="https://v1.jinrishici.com/all.svg" style="max-width:100%;">
```

或者使用我们的<b>svg专用</b>的自定义参数
个性化参数

| 说明 | 参数名 | 默认值 | 合法范围 |
| --- | --- | --- | --- |
| 字体大小（px） | font-size | 20 | [8,50] |
| 字体间隔（px） | spacing | 1.5 | [0,30] |

调用示例
```html
<img src="https://v1.jinrishici.com/all.svg?font-size=18&spacing=4">
```

#### JSON 调用

```html
<script>
  var xhr = new XMLHttpRequest();
  xhr.open('get', 'https://v1.jinrishici.com/all.json');
  xhr.onreadystatechange = function () {
    if (xhr.readyState === 4) {
      var data = JSON.parse(xhr.responseText);
      var gushici = document.getElementById('gushici');
      gushici.innerText = data.content;
    }
  };
  xhr.send();
</script>
```

JSON调用可以获取来源、作者、分类等信息，可以供你自定义拼接显示效果。

#### PNG 调用

```html
<img src="https://v1.jinrishici.com/all.png">
```

我们会提供透明的PNG文件。PNG方法兼容性最好，可以在几乎任何地方插入。并且支持所有浏览器。缺点是不能控制样式，另外，由于流量限制，我们只会给您传送较小的图片源文件。

#### TXT 调用

```html
<script>
  var xhr = new XMLHttpRequest();
  xhr.open('get', 'https://v1.jinrishici.com/all.txt');
  xhr.onreadystatechange = function () {
    if (xhr.readyState === 4) {
      var gushici = document.getElementById('gushici');
      gushici.innerText = xhr.responseText;
    }
  };
  xhr.send();
</script>
```

TXT调用和JSON调用基本一致，可以节省一些流量。或者，你甚至可以使用 iframe 来调用我们的接口

#### 获取七天点击量数据
[https://v1.jinrishici.com/log](https://v1.jinrishici.com/log)


### 技术说明

1. MainVerticle 用来部署其他 Verticle
2. ApiVerticle 为核心部分，负责处理请求
3. DataService 和 LogService 负责提供查询服务
4. ConvertUtil 负责转码
5. Service 没有使用 Service Proxy，因此无需额外生成代码。

### 单机压测

CPU: E5 2660 8核16线程 16GB内存 JVM默认配置 Windows10系统 

Jmeter: 100线程数 每线程循环 1000次，走HTTP

| Samples | Average | Median | 90% Line | 95% Line | 99% Line | Min | Max | Error % | Throughput/sec | Received KB/sec | Sent KB/sec |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1000000 | 4 | 4 | 5 | 8 | 9 | 0 | 37 | 0.00% | 20458.68 | 3622.91 | 2497.4 |

### 更新历史

* 2018.08.09 1.4:
  1. 补全单元测试
  2. 优化启动流程
  3. 升级依赖版本

* 2018.08.09 1.3:
  1. svg可以定义字体大小和间隔

* 2018.08.08 1.2:
  1. 细节优化 

* 2018.08.06 1.1: 
  1. 引入前缀树，使分类检索效率由 O(n) (n为所有分类数) 变为 O(L) (L为分类级数)。
  缺点是空间复杂度由 O(n) 变为 O(nL)，代码复杂度增加60行
  2. 优化了正则匹配获取地址参数的逻辑
  3. 正确加入了全局的错误处理（包括 Router 和 EventBus）
* 2018.08.05 1.0：初始版本，支持 4 种格式返回，支持按分类搜索 



### 关于项目

名句数据由古诗文网收录整理，特此感谢。

若您对本人的其他作品或者文章感兴趣，请访问我的博客：[https://luan.ma](https://luan.ma/)
