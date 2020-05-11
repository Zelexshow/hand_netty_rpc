package com.zelex.rpc.consumer;

import com.zelex.rpc.api.IRpcHelloService;
import com.zelex.rpc.api.IRpcService;
import com.zelex.rpc.provider.RpcHelloServiceImpl;
import com.zelex.rpc.provider.RpcServiceImpl;

public class RpcConsumer {
    public static void main(String[] args) {

        IRpcService serivce=(IRpcService)RpcProxy.create(IRpcService.class);
        System.out.println(serivce.add(7,8));
        System.out.println(serivce.div(0,4));
        IRpcHelloService s = (IRpcHelloService)RpcProxy.create(IRpcHelloService.class);
        System.out.println(s.hello("jason"));
        /*IRpcService service=new RpcServiceImpl();
        RpcHelloServiceImpl s = new RpcHelloServiceImpl();
        System.out.println(s.hello("jack"));
        int add = service.add(8,2);
        System.out.println(add);*/
    }
}
