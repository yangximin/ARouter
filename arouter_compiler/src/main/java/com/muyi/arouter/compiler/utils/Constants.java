package com.muyi.arouter.compiler.utils;

public class Constants {

    public static final String ROUTER_ANNOTATION_TYPES = "com.muyi.arouter.annotation.ARouter";

    public static final String MODULE_NAME = "moduleName";

    public static final String PACKAGE_APT = "packageNameForAPT";

    public static final String ACTIVITY = "android.app.Activity";

    public static final String PATH_TYPE = "com.muyi.arouter.api.ARouterLoadPath";

    public static final String GROUP_TYPE = "com.muyi.arouter.api.ARouterLoadGroup";

    public static final String PATH_METHOD_NAME = "loadPath";
    // 路由组Group对应的详细Path，参数名
    public static final String PATH_PARAMETER_NAME = "pathMap";
    //分组方法名
    public static final String GROUP_METHOD_NAME = "loadGroup";
    //参数名
    public static final String GROUP_PARAMETER_NAME = "groupMap";


    // APT生成的路由组Group源文件名
    public static final String GROUP_FILE_NAME = "ARouter$$Group$$";
    // APT生成的路由组Group对应的详细Path源文件名
    public static final String PATH_FILE_NAME = "ARouter$$Path$$";


}
