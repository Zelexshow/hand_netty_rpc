package com.zelex.rpc.protocal;

import lombok.Data;

import java.io.Serializable;
@Data
public class InvokerProtocal implements Serializable {
    private String className;//服务名
    private String methodName;//方法名
    private Class<?>[] parames;//形参列表
    private Object[] values;//实参列表
}
