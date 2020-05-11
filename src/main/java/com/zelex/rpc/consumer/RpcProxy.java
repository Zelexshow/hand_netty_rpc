package com.zelex.rpc.consumer;

import com.zelex.rpc.protocal.InvokerProtocal;
import com.zelex.rpc.registry.RegistryHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RpcProxy {
    public static <T> T create(Class<?> clazz){
        MethodProxy proxy=new MethodProxy(clazz);
        return (T)Proxy.newProxyInstance(clazz.getClassLoader(),new Class[]{clazz},proxy);
    }

    //将本地调用，通过代理的形式变成网络调用
    public static class MethodProxy implements InvocationHandler{
        private Class<?> clazz;
        public MethodProxy(Class<?> clazz){this.clazz = clazz;}
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            //首先判断类是不是接口，如果是接口才做处理
            if (Object.class.equals(method.getDeclaringClass())){
                return method.invoke(this,args);
            }else{//是接口
                return rpcInvoker(proxy,method,args);
            }
        }

        private Object rpcInvoker(Object proxy, Method method, Object[] args) {
            //首先要构造一个协议内容，消息
            InvokerProtocal msg = new InvokerProtocal();
            msg.setClassName(this.clazz.getName());
            msg.setMethodName(method.getName());
            msg.setParames(method.getParameterTypes());
            msg.setValues(args);
            //发起网络请求
            NioEventLoopGroup workergroup = new NioEventLoopGroup();
            final RpcProxyHandler rpcProxyHandler = new RpcProxyHandler();
            try{

                Bootstrap client = new Bootstrap();
                client.group(workergroup)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY,true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
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
                                pipeline.addLast(rpcProxyHandler);
                            }
                        });
                ChannelFuture future = client.connect("localhost", 8080).sync();
                future.channel().writeAndFlush(msg).sync();
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                workergroup.shutdownGracefully();
            }
            return rpcProxyHandler.getResponse();
        }
    }
}

