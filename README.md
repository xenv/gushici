## 一言·古诗词 API

<div style="text-align:center">
<img src="https://api.gushi.ci/all.svg">
</div>


### 简介

古诗词·一言API 是一个可以随机返回一句古诗词名句的接口。具有以下特点：

* 快：使用 Vert.x Java 全异步框架开发，使用Redis数据库，确保毫秒级稳定响应。
* 全：支持 svg / txt / json / png 调用，满足你在任何地方的调用需求
* 准：可以根据你的喜好，在指定的分类中进行随机返回

本仓库是该项目的开源代码仓库，仅提供测试数据，其余数据请自行采集。

项目首页：[https://gushi.ci](https://gushi.ci)


### 安装

1. 根据 Maven 安装依赖
2. maven package 可打成 Jar 包
3. 配置 redis，导入 dump.rdb 数据 （放到 redis 目录下，重启 redis，如果原来有这个文件请自行导出数据再替换，原有数据会全部清除）
4. 运行：
    1. 服务器运行 `java -server -jar yiyan-1.0-fat.jar -conf conf.json`  conf.json从下面可以复制出来自己配
    2. IDE运行 启动类 `io.vertx.core.Launcher` 参数 `run ma.luan.yiyan.MainVerticle -conf src/main/resources/conf.json`
        
5. 如果要自行采集数据，请仿照原有 redis 中的 key 格式采集，log自动添加，help为可选，需要自己创建 text 和 img 两个 key。古诗词数据每个分类一个 set，set的内容是 json。图片使用base64加密储存。其他细节请自行查看源码。

### 使用

#### API举例

* [https://api.gushi.ci/all.json](https://api.gushi.ci/all.json)
* [https://api.gushi.ci/all.svg](https://api.gushi.ci/all.svg)
* [https://api.gushi.ci/shuqing/libie.png](https://api.gushi.ci/shuqing/libie.png)
* [https://api.gushi.ci/rensheng.txt](https://api.gushi.ci/rensheng.txt)

#### API地址格式(仅支持https)

`https://api.gushi.ci/{一级分类}/{二级分类(可选)}.{返回格式(可选)}`

查看所有目前支持的分类：[https://api.gushi.ci/](https://api.gushi.ci/)

目前支持的后缀：.svg .txt .png .json 不加后缀默认返回 json

#### SVG 调用

```html
<img src="https://api.gushi.ci/all.svg">
```

SVG后缀是我们推荐的最优调用方案，可以在部分论坛、任何博客、小程序内无损直接调用，并且可以一定程度上控制样式。缺点是部分老旧浏览器不支持。

#### JSON 调用

```html
<script>
  var xhr = new XMLHttpRequest();
  xhr.open('get', 'https://api.gushi.ci/all.json');
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
<img src="https://api.gushi.ci/all.png">
```

我们会提供透明的PNG文件。PNG方法兼容性最好，可以在几乎任何地方插入。并且支持所有浏览器。缺点是不能控制样式，另外，由于流量限制，我们只会给您传送较小的图片源文件。

#### TXT 调用

```html
<script>
  var xhr = new XMLHttpRequest();
  xhr.open('get', 'https://api.gushi.ci/all.txt');
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

TXT调用和JSON调用基本一致，可以节省一些流量。或者，你甚至可以使用 iframe 来调用我们的接口

#### 获取七天点击量数据
[https://api.gushi.ci/log](https://api.gushi.ci/log)


### 技术说明

1. MainVerticle 用来部署其他 Verticle
2. ApiVerticle 为核心部分，负责处理请求
3. DataService 和 LogService 负责提供查询服务
4. ConvertUtil 负责转码
5. Service 没有使用 Service Proxy，因此无需额外生成代码。

### 待改进

1. 错误处理
2. 优化部分可能会阻塞的代码

### 关于项目

这是我的个人项目，目的是为了弘扬中国传统文化。

名句数据由古诗文网收录整理，特此感谢。

若有任何建议，或者有工作实习的机会（本人大学在读），请联系 meetlhx#qq.com

若您对本人的其他作品或者文章感兴趣，请访问我的博客：[https://luan.ma](https://luan.ma/)
