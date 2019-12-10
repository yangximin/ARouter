package com.muyi.arouter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 路由注解
 * <ul>
 * <li>@Target(ElementType.TYPE) //接口，类，枚举注解 </li>
 * <li>@Target(ElementType.FIELD) //属性，枚举的常量 </li>
 * <li>@Target(ElementType.METHOD) //方法 </li>
 * <li>@Target(ElementType.PARAMETER) //方法参数 </li>
 * <li>@Target(ElementType.CONSTRUCTOR) //构造函数 </li>
 * <li>@Target(ElementType.LOCAL_VARIABLE) //局部变量 </li>
 * <li>@Target(ElementType.ANNOTATION_TYPE) //注解在另外一个注解上 </li>
 * <li>@Target(ElementType.PACKAGE) //包 </li>
 * <li>@Retention(RetentionPolicy.RUNTIME)<br>注解会在class字节码文件中存在，jvm加载时可以通过反射获取到该注解的内容 </li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)//要在编译时进行一些预处理操作
public @interface ARouter {
    String path();

    String group() default "";
}
