package com.denghb.restful;

import com.denghb.restful.utils.LogUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Server {
    private static final String RESPONSE_HTML = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n%s";

    private static int DEFAULT_PORT = 8888;

    public interface Handler {
        String execute(String request);
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
                    String requestHeader = "";
                    //拿出通道中的Http头请求
                    try {
                        requestHeader = receive(socketChannel);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    //启动线程处理该请求,if条件判断一下，防止心跳包
                    if (!"".equals(requestHeader)) {
                        String response = "";
                        if (null != handler) {
                            response = handler.execute(requestHeader);
                        }

                        socketChannel.register(selector, SelectionKey.OP_WRITE);
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        buffer.put(String.format(RESPONSE_HTML, response).getBytes());
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
}
