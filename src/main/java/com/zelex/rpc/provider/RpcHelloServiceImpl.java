package com.zelex.rpc.provider;

import com.zelex.rpc.api.IRpcHelloService;

public class RpcHelloServiceImpl implements IRpcHelloService {
    @Override
    public String hello(String name) {
        return "hello!"+name;
    }
}
