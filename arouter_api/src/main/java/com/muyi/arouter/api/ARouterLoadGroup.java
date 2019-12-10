package com.muyi.arouter.api;

import java.util.Map;

/**
 * 路由分组抽象类
 */
public interface ARouterLoadGroup {

    Map<String,Class<? extends ARouterLoadPath>> loadGroup();
}
