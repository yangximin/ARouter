package com.muyi.arouter.compiler;

import com.google.auto.service.AutoService;
import com.muyi.arouter.annotation.ARouter;
import com.muyi.arouter.annotation.model.RouterBean;
import com.muyi.arouter.compiler.utils.Constants;
import com.muyi.arouter.compiler.utils.EmptyUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
//指定处理器处理的注解
@SupportedAnnotationTypes({Constants.ROUTER_ANNOTATION_TYPES})
//指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
// 注解处理器接收的参数
@SupportedOptions({Constants.MODULE_NAME, Constants.PACKAGE_APT})
public class ARouterProcessor extends AbstractProcessor {
    //操作element工具类（类，函数，属性都是element）
    private Elements elementUtils;
    //用户操作typeMirror的工具方法
    private Types typeUtils;
    //打印信息
    private Messager messager;
    //文件生成器
    private Filer filer;

    private String packageNameForAPT;

    private String moduleName;
    //路由路径
    private Map<String, List<RouterBean>> pathMap = new HashMap<>();
    // 临时map存储，用来存放路由Group信息，生成路由组类文件时遍历
    // key:组名"app", value:类名"ARouter$$Path$$app.class"
    private Map<String, String> groupMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
        messager.printMessage(Diagnostic.Kind.NOTE, "--------开始编译");
        //获取参数
        Map<String, String> options = processingEnvironment.getOptions();
        if (!EmptyUtils.isEmpty(options)) {
            moduleName = options.get(Constants.MODULE_NAME);
            packageNameForAPT = options.get(Constants.PACKAGE_APT);
            messager.printMessage(Diagnostic.Kind.NOTE, "moduleName:" + moduleName);
            messager.printMessage(Diagnostic.Kind.NOTE, "packageNameForAPT:" + packageNameForAPT);
        }
        // 必传参数判空（乱码问题：添加java控制台输出中文乱码）
        if (EmptyUtils.isEmpty(moduleName) || EmptyUtils.isEmpty(packageNameForAPT)) {
            throw new RuntimeException("注解处理器需要的参数moduleName或者packageName为空，请在对应build.gradle配置参数");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
//        TypeName typeName = ParameterizedTypeName.get(ClassName.get(Map.class),);
        if (!EmptyUtils.isEmpty(set)) {
            Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(ARouter.class);
            if (!EmptyUtils.isEmpty(elements)) {
                try {
                    paresElements(elements);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    private void paresElements(Set<? extends Element> elements) throws IOException {
        //获取activity类型
        TypeMirror typeElement = elementUtils.getTypeElement(Constants.ACTIVITY).asType();
        // 遍历节点
        for (Element element : elements) {
            //元素的类型
            TypeMirror elementMirror = element.asType();
            messager.printMessage(Diagnostic.Kind.NOTE, "遍历元素信息：" + elementMirror.toString());

            // 获取每个类上的@ARouter注解中的注解值
            ARouter annotation = element.getAnnotation(ARouter.class);
            // 路由详细信息，最终实体封装类
            RouterBean bean = RouterBean.create(annotation.path(),
                    annotation.group(),
                    element);
            if (typeUtils.isSubtype(elementMirror, typeElement)) {
                bean.setType(RouterBean.Type.ACTIVITY);
            } else {
                throw new RuntimeException("@ARouter注解目前仅限用于Activity类之上");
            }
            // 赋值临时map存储，用来存放路由组Group对应的详细Path类对象
            valueOfPathMap(bean);
        }
        // 获取ARouterLoadGroup、ARouterLoadPath类型（生成类文件需要实现的接口）
        TypeElement groupType = elementUtils.getTypeElement(Constants.GROUP_TYPE);
        TypeElement pathType = elementUtils.getTypeElement(Constants.PATH_TYPE);
        // 第一步：生成路由组Group对应详细Path类文件，如：ARouter$$Path$$app
        createPathFile(pathType);

        // 第二步：生成路由组Group类文件（没有第一步，取不到类文件），如：ARouter$$Group$$app
        createGroupFile(groupType, pathType);
    }


    private void createPathFile(TypeElement pathType) throws IOException {
        // 判断是否有需要生成的类文件
        if (EmptyUtils.isEmpty(pathMap)) return;
        //返回参数Map<String, RouterBean>
        TypeName returnType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(RouterBean.class));
        // 遍历分组，每一个分组创建一个路径类文件，如：ARouter$$Path$$app
        for (Map.Entry<String, List<RouterBean>> entrySet : pathMap.entrySet()) {
            // 方法配置：public Map<String, RouterBean> loadPath() {
            MethodSpec.Builder methodBuidler = MethodSpec.methodBuilder(Constants.PATH_METHOD_NAME)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(returnType);
            // 遍历之前：Map<String, RouterBean> pathMap = new HashMap<>();
            methodBuidler.addStatement("$T<$T,$T> $N = new $T<>()",
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ClassName.get(RouterBean.class),
                    Constants.PATH_PARAMETER_NAME,
                    ClassName.get(HashMap.class));
            // 一个分组，如：ARouter$$Path$$app。有很多详细路径信息，如：/app/MainActivity、/app/OtherActivity
            List<RouterBean> pathList = entrySet.getValue();
//            String group = entrySet.getKey();
            for (RouterBean routerBean : pathList) {
                // pathMap.put("/app/MainActivity", RouterBean.create(
                //        RouterBean.Type.ACTIVITY, MainActivity.class, "/app/MainActivity", "app"));
                //$N是变量
                //$S是字符串
                //$L是常量
                //$T是类
                methodBuidler.addStatement("$N.put($S, $T.create( $T.$L, $T.class, $S, $S))",
                        Constants.PATH_PARAMETER_NAME,
                        routerBean.getPath(),
                        ClassName.get(RouterBean.class),
                        ClassName.get(RouterBean.Type.class),
                        routerBean.getType(),
                        ClassName.get((TypeElement) routerBean.getElement()),
                        routerBean.getPath(),
                        routerBean.getGroup()
                );
            }
            // 遍历之后：return pathMap;
            methodBuidler.addStatement("return $N", Constants.PATH_PARAMETER_NAME);
            //ARouter$$Path$$app
            String finalClassName = Constants.PATH_FILE_NAME + entrySet.getKey();
            messager.printMessage(Diagnostic.Kind.NOTE, "APT生成路由Path类文件：" +
                    packageNameForAPT + "." + finalClassName);
            // 生成类文件：ARouter$$Path$$app
            TypeSpec build = TypeSpec.classBuilder(finalClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(methodBuidler.build())
                    .addSuperinterface(ClassName.get(pathType))
                    .build();
            JavaFile.builder(packageNameForAPT, build).build().writeTo(filer);
            groupMap.put(entrySet.getKey(), finalClassName);
        }
    }

    private void createGroupFile(TypeElement groupType, TypeElement pathType) throws IOException {
        if (EmptyUtils.isEmpty(groupMap) || EmptyUtils.isEmpty(pathMap)) return;
        // Map<String, Class<? extends ARouterLoadPath>>
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(ClassName.get(Class.class)
                        , WildcardTypeName.subtypeOf(ClassName.get(pathType))));
        //生命方法
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Constants.GROUP_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(returnType);
        // 遍历之前：Map<String, Class<? extends ARouterLoadPath>> groupMap = new HashMap<>();
        methodBuilder.addStatement("$T<$T,$T> $N = new $T<>()", ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(ClassName.get(pathType))),
                Constants.GROUP_PARAMETER_NAME,
                ClassName.get(HashMap.class)
        );
        for (Map.Entry<String, String> entry : groupMap.entrySet()) {
            // groupMap.put("main", ARouter$$Path$$app.class);
            methodBuilder.addStatement("$N.put($S,$T.class)",
                    Constants.GROUP_PARAMETER_NAME,
                    entry.getKey(),
                    ClassName.get(packageNameForAPT, entry.getValue())
            );
        }
        methodBuilder.addStatement("return $N", Constants.GROUP_PARAMETER_NAME);
        String finalClassName = Constants.GROUP_FILE_NAME + moduleName;
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(finalClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(groupType))
                .addMethod(methodBuilder.build());
        JavaFile.builder(packageNameForAPT, classBuilder.build()).build().writeTo(filer);
    }

    private void valueOfPathMap(RouterBean bean) {
        if (checkRouterPath(bean)) {
            List<RouterBean> routerBeans = pathMap.get(bean.getPath());
            if (EmptyUtils.isEmpty(routerBeans)) {
                routerBeans = new ArrayList<>();
                routerBeans.add(bean);
                pathMap.put(bean.getGroup(), routerBeans);
            } else {
                routerBeans.add(bean);
            }
        } else {
            messager.printMessage(Diagnostic.Kind.ERROR, "@ARouter注解未按规范配置，如：/app/MainActivity");
        }
    }

    /**
     * 检测路径合法性
     */
    private boolean checkRouterPath(RouterBean bean) {
        String group = bean.getGroup();
        String path = bean.getPath();
        // @ARouter注解中的path值，必须要以 / 开头（模仿阿里Arouter规范）
        if (EmptyUtils.isEmpty(path) || !path.startsWith("/")) {
            messager.printMessage(Diagnostic.Kind.ERROR, "@ARouter注解中的path值，必须要以 / 开头");
            return false;
        }
        // 比如开发者代码为：path = "/MainActivity"，最后一个 / 符号必然在字符串第1位
        if (path.lastIndexOf("/") == 0) {
            messager.printMessage(Diagnostic.Kind.ERROR, "@ARouter注解中的path值，必须要以 / 开头");
            return false;
        }
        // 从第一个 / 到第二个 / 中间截取，如：/app/MainActivity 截取出 app 作为group
        String pathGroup = path.substring(1, path.indexOf("/", 1));
        // @ARouter注解中的group有赋值情况
        if (!EmptyUtils.isEmpty(group) && !group.equals(moduleName)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "@ARouter注解中的path值，必须要以 / 开头");
            return false;
        }
        bean.setGroup(pathGroup);
        return true;
    }
}
