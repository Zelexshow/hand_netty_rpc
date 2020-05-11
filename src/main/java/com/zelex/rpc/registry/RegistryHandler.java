package com.zelex.rpc.registry;

import com.zelex.rpc.protocal.InvokerProtocal;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryHandler extends ChannelInboundHandlerAdapter {

    private List<String> classNames=new ArrayList();
    private Map<String,Object> registryMap=new ConcurrentHashMap<>();

    public RegistryHandler(){
        //1、根据一个包名讲所有符合条件的class全部扫描出来，放到一个容器中（简化版本）
        //如果是分布式，就是读配置文件
        scannerClass("com.zelex.rpc.provider");
        doRigistry();
    }

    private void doRigistry() {
        if (classNames.isEmpty()) return;
        for (String className:classNames){
            try {
                Class<?> clazz = Class.forName(className);
                Class<?> i = clazz.getInterfaces()[0];
                String serviceName=i.getName();
                //本来这里存的应该是网络路径，从配置文件中读取
                //在调用的时候再去解析，用反射调用
                registryMap.put(serviceName,clazz.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    //正常来说应该是读配置文件
    //简单粗暴，扫描本地文件
    private void scannerClass(String packageName) {
        //2、给每一个对应的Class起一个唯一的名字，作为服务名称，保存到一个容器中
        URL url = this.getClass().getClassLoader().getResource(packageName.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file:classPath.listFiles()){
            if (file.isDirectory()){//如果是目录
                scannerClass(packageName+"."+file.getName());
            }else {
                classNames.add(packageName+"."+file.getName().replace(".class",""));
            }
        }
    }
    //有客户端连接上的时候，会回调
    //3、当有客户端链接过来之后，就会获取协议内容 InvokerProtocal的对象
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Object result=null;
        InvokerProtocal request=(InvokerProtocal)msg;
        //4、要去注册好的容器中找到符合条件的服务
        if (registryMap.containsKey(request.getClassName())){
            Object service = registryMap.get(request.getClassName());
            Method method = service.getClass().getMethod(request.getMethodName(), request.getParames());
            result=method.invoke(service,request.getValues());
        }
        //5、通过远程调用Provider得到返回结果，并回复给客户端
        ctx.write(result);
        ctx.flush();
        ctx.close();
    }
    //连接发生异常时回调
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
