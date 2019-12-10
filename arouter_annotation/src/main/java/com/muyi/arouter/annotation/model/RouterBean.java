package com.muyi.arouter.annotation.model;

import javax.lang.model.element.Element;

/**
 * 类信息bean
 */
public class RouterBean {

    public RouterBean(Type type, Class<?> clazz, String path, String group) {
        this.type = type;
        this.clazz = clazz;
        this.group = group;
        this.path = path;
    }

    public RouterBean(String path, String group, Element element) {
        this.group = group;
        this.path = path;
        this.element = element;
    }

    public enum Type {
        ACTIVITY
    }

    public static RouterBean create(Type type, Class<?> clazz, String path, String group) {
        return new RouterBean(type, clazz, path, group);
    }

    public static RouterBean create(String path, String group, Element element) {
        return new RouterBean(path, group, element);
    }

    //枚举类型
    private Type type;
    //类结点
    private Element element;
    //被注解的类对象
    private Class<?> clazz;
    //路由组名
    private String group;
    //路由地址
    private String path;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
