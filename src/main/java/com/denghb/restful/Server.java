package com.denghb.restful;

import com.denghb.restful.utils.JSONUtils;
import com.denghb.restful.utils.LogUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Server {

    private static int DEFAULT_PORT = 8888;

    public interface Handler {
        Response execute(Request request);
    }

    private Handler handler;


    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public Server() {
    }

    public void start() {
        start(null);
    }

    /**
     * -p=8080
     *
     * @param args
     */
    public void start(String[] args) {

        int port = DEFAULT_PORT;

        try {
            for (String p : args) {
                if (p.startsWith("-p")) {
                    p = p.substring(p.indexOf("=") + 1, p.length()).trim();
                    port = Integer.parseInt(p);
                }
            }

            run(port);
        } catch (IOException e) {
            LogUtils.error(getClass(), e.getMessage(), e);
        }
    }

    private void run(int port) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));
        //将通道设置为非阻塞模式
        serverSocketChannel.configureBlocking(false);
        //将serverSocketChannel注册给选择器,并绑定ACCEPT事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        LogUtils.info(getClass(), "Server started http://localhost:{}", port);

        while (true) {
            //查询就绪的通道数量
            int readyChannels = selector.select();
            //没有就绪的则继续进行循环
            if (readyChannels == 0)
                continue;
            //获得就绪的selectionKey的set集合
            Set<SelectionKey> keys = selector.selectedKeys();
            //获得set集合的迭代器
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    //该key有ACCEPT事件
                    //将监听得到的channel强转为ServerSocketChannel
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    //得到接收到的SocketChannel
                    SocketChannel socketChannel = server.accept();
                    if (socketChannel != null) {
                        // System.out.println("收到了来自" + ((InetSocketAddress) socketChannel.getRemoteAddress()).getHostString() + "的请求");
                        //将socketChannel设置为阻塞模式
                        socketChannel.configureBlocking(false);
                        //将socketChannel注册到选择器
                        socketChannel.register(selector, SelectionKey.OP_READ);
                    }
                } else if (key.isReadable()) {
                    //该key有Read事件
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    String message = "";
                    //拿出通道中的Http头请求
                    try {
                        message = receive(socketChannel);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    //启动线程处理该请求,if条件判断一下，防止心跳包
                    if (!"".equals(message)) {
                        Response response = new Response();
                        if (null != handler) {
                            response = handler.execute(new Request(message));
                        }

                        socketChannel.register(selector, SelectionKey.OP_WRITE);
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        buffer.put(response.bytes());
                        //从写模式，切换到读模式
                        buffer.flip();
                        socketChannel.write(buffer);
                    }
                } else if (key.isWritable()) {
                    //该key有Write事件
                    SocketChannel socketChannel = (SocketChannel) key.channel();
//                    socketChannel.shutdownInput();
                    socketChannel.close();
                }
                //从key集合中删除key，这一步很重要，就是因为没写这句，Selector.select()方法一直返回的是0
                //原因分析可能是不从集合中删除，就不会回到I/O就绪事件中
                iterator.remove();
            }
        }

    }

    private String receive(SocketChannel socketChannel) throws Exception {
        //声明一个1024大小的缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        byte[] bytes = null;
        int size = 0;
        //定义一个字节数组输出流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //将socketChannel中的数据写入到buffer中，此时的buffer为写模式，size为写了多少个字节
        while ((size = socketChannel.read(buffer)) > 0) {
            //将写模式改为读模式
            //The limit is set to the current position and then the position is set to zero.
            //将limit设置为之前的position，而将position置为0，更多java nio的知识会写成博客的
            buffer.flip();
            bytes = new byte[size];
            //将Buffer写入到字节数组中
            buffer.get(bytes);
            //将字节数组写入到字节缓冲流中
            baos.write(bytes);
            //清空缓冲区
            buffer.clear();
        }
        //将流转回字节数组
        bytes = baos.toByteArray();
        baos.close();
        return new String(bytes);
    }

    // 这真不是servlet
    class Request {

        private String method;

        private String uri;

        private Map<String, String> parameters;

        private Map<String, String> headers;

        /**
         * 解析报文，待优化
         *
         * @param message
         */
        public Request(String message) {

            // GET /xxx?a=aa HTTP/1.1
            int firstStart = message.indexOf(" ");
            this.method = message.substring(0, firstStart);
            String uri = message.substring(firstStart + 1, message.indexOf(" ", message.indexOf(" ") + 1));
            //有问号表示后面跟有参数
            int start = uri.indexOf("?");
            if (-1 != start) {
                String attr = uri.substring(start + 1, uri.length());
                uri = uri.substring(0, start);

                buildParameter(this.parameters, attr);
            }
            this.uri = uri;

            String headerStr = message.substring(message.indexOf("\r\n") + 2, message.indexOf("\r\n\r\n"));

            this.headers = new HashMap<String, String>();
            for (String header : headerStr.split("\r\n")) {
                String[] heads = header.split(": ");
                if (heads.length != 2) {
                    continue;
                }
                String key = heads[0];
                String value = heads[1];
                this.headers.put(key, value.trim());
            }

            // 请求参数
            String p = message.substring(message.indexOf("\r\n\r\n") + 4, message.length());
            buildParameter(this.parameters, p);
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }

    static class Response {

        // 先默认都返回成功
        private static final String RESPONSE_HTML = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nConnection: close\r\n\r\n%s";
        private int code = 200;// TODO

        private Object body;

        private Response() {

        }

        private Response(Object body) {
            this.body = body;
        }

        /**
         * 响应字节
         *
         * @return
         */
        public byte[] bytes() {
            String result = "";
            if (body instanceof String) {
                result = String.valueOf(body);
            } else {
                result = JSONUtils.toJson(body);
            }

            result = String.format(RESPONSE_HTML, result);

            return result.getBytes();
        }

        public static Response build(Object body) {
            return new Response(body);
        }

        public static Response build(int code) {
            code = code;
            return new Response("");
        }
    }

    private static void buildParameter(Map<String, String> param, String p) {
        if (null == param) {
            param = new HashMap<String, String>();
        }

        if ("".equals(p)) {
            return;
        }

        // JSON ?
        if (p.startsWith("{")) {
            Map a = JSONUtils.fromJson(Map.class, p);

            param.putAll(a);

            return;
        }

        String[] attrs = p.split("&");
        for (String string : attrs) {
            String key = string.substring(0, string.indexOf("="));
            String value = string.substring(string.indexOf("=") + 1);

            try {
                value = URLDecoder.decode(value, "utf-8");
            } catch (Exception e) {

            }
            param.put(key, value);
        }
    }
}
