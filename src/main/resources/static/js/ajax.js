/**
 * config = {url:"/",method:"GET",data:{},success:function(res){},response:function(xhr){}}
 */
var ajax = function(config) {

    var method = config.method;

    if (undefined == method) {
        method = "GET";
    }

    var xhr = new XMLHttpRequest();  // XMLHttpRequest对象用于在后台与服务器交换数据
    xhr.open(method, config.url, true);
    xhr.onreadystatechange = function() {
        if (!xhr.readyState == 4) { // readyState == 4说明请求已完成
            return;
        }

        if (config.response) {
            config.response(this);
            return;
        }

        if (xhr.status == 200 || xhr.status == 304) {
            config.success(xhr.responseText);  //从服务器获得数据
        } else {
            alert("未知错误");
        }
    };
    if ("POST" === method) {
        xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");  // 添加http头，发送信息至服务器时内容编码类型
    }

// {name:"asd",age=18} => name=asd&age=18
    var data = "";
    var map = config.data;
    if (map) {
        var i = 0;
        for(var key in map){
          if (i != 0){
            data += "&";
          }
          data += key+"="+encodeURIComponent(map[key]);
          i = 1;
        }
    }
    xhr.send(data);
};