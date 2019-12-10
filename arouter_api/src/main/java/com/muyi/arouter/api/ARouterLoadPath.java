package com.muyi.arouter.api;

import com.muyi.arouter.annotation.model.RouterBean;

import java.util.Map;

/**
 * 路由分组中具体的路由表
 */
public interface ARouterLoadPath {

    Map<String, RouterBean> loadPath();
}
