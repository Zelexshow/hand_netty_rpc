package com.zelex.rpc.registry;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolver;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class RpcRegistry {
    private int port;
    public RpcRegistry(int port){
        this.port=port;
    }
    public void start(){
        try {
            //ServerSocket/ServerSocketChannel
            //主线程初始化
            NioEventLoopGroup bossgroup = new NioEventLoopGroup();
            //子线程池初始化
            NioEventLoopGroup workgroup = new NioEventLoopGroup();

            ServerBootstrap server = new ServerBootstrap();
            server.group(bossgroup,workgroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //在netty中，吧所有的业务逻辑全部都归总到一个队列中
                            //这个队列中包含了各种各样的业务逻辑，对这些处理逻辑在Netty中有一个封装
                            //封装成一个对象，无锁化串行任务队列
                            //pipeline
                            ChannelPipeline pipeline = ch.pipeline();

                            //这两个：对自定义的协议内容编解码，即还原出InvokerProtocal对象
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,
                                    4,0,4));
                            //自定义编码器
                            pipeline.addLast(new LengthFieldPrepender(4));


                            //这两个：还原出 private Class<?>[] parames;//形参列表 、private Object[] values;//实参列表
                            //实参处理
                            pipeline.addLast("encoder",new ObjectEncoder());
                            pipeline.addLast("decoder",new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));

                            //最后一步，完成逻辑的处理
                            //1、注册，给每一个对象起一个名字，对外提供服务的名字
                            //2、服务位置要做一个登记
                            pipeline.addLast(new RegistryHandler());
                        }

                    })
                    .option(ChannelOption.SO_BACKLOG,128)
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
            //正式启动服务，相当于用一个死循环开始轮询
            ChannelFuture future = server.bind(this.port).sync();
            System.out.println("RPC Registry start listen at "+ this.port);
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        new RpcRegistry(8080).start();
    }
}
