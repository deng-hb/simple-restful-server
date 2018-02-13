package com.denghb.restful;


import com.denghb.restful.annotation.*;
import com.denghb.restful.annotation.Error;
import com.denghb.restful.utils.JSONUtils;
import com.denghb.restful.utils.LogUtils;
import com.denghb.restful.utils.ReflectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 先这样写着，有时间重构一下
 */
public class Application {

    /**
     * 所有创建的RESTful对象
     */
    static Map<Class, Object> _OBJECT = new ConcurrentHashMap<Class, Object>();

    /**
     * 所有请求方法
     * <p>
     * <pre>
     * @GET("/user") -> <"GET/user",MethodInfo>
     * @Filter("/") -> <"Filter/",MethodInfo>
     * </pre>
     */
    static Map<String, Application.MethodInfo> _OBJECT_METHOD = new ConcurrentHashMap<String, Application.MethodInfo>();

    private Application() {

    }

    public static void run(Class clazz, String[] args) {

        init(clazz);

        Server server = new Server();
        // 在start之前
        server.setHandler(new Server.Handler() {
            public Server.Response execute(Server.Request request) {
                LogUtils.info(getClass(), "{}\t{}", request.getMethod(), request.getUri());

                // 过滤
                Object object = handlerFilter(request);
                if (null != object) {
                    return Server.Response.build(object);
                }

                String path = request.getMethod() + request.getUri();
                Application.MethodInfo info = _OBJECT_METHOD.get(path);
                Map<String, String> pathVariables = new HashMap<String, String>();

                if (null == info) {
                    // 参数在path上的匹配
                    for (String path1 : _OBJECT_METHOD.keySet()) {
                        buildPath(path1, path, pathVariables);
                        if (!pathVariables.isEmpty()) {
                            info = _OBJECT_METHOD.get(path1);
                            break;
                        }
                    }

                    if (pathVariables.isEmpty()) {
                        Object result = handlerError(new RESTfulException("404 Not Found[" + path + "]", 404));
                        if (null != result) {
                            return Server.Response.build(result);
                        }
                        return Server.Response.buildError(404);
                    }
                }

                try {

                    Object target = getObject(info.getClazz());

                    // 执行path对应方法
                    Method method = info.getMethod();
                    method.setAccessible(true);
                    Object result = method.invoke(target, buildParams(info, request.getParameters(), pathVariables));

                    return Server.Response.build(result);
                } catch (InvocationTargetException e) {
                    // 调用方法抛出异常
                    Object result = handlerError(e.getTargetException());
                    if (null != result) {
                        return Server.Response.build(result);
                    }
                } catch (Exception e) {
                    LogUtils.error(getClass(), e.getMessage(), e);

                    // 内部错误
                    Object result = handlerError(new RESTfulException(e.getMessage(), 500));
                    if (null != result) {
                        return Server.Response.build(result);
                    }
                }

                return Server.Response.buildError(500);
            }
        });

        server.start(args);
    }


    /**
     * 参数列表赋值
     */
    private static Object[] buildParams(Application.MethodInfo info, Map<String, String> requestParameters, Map<String, String> pathVariables) {
        // 参数赋值
        int pcount = info.parameters.size();
        Object[] ps = new Object[pcount];

        if (0 == pcount) {
            return ps;
        }
        for (int i = 0; i < pcount; i++) {
            Param param = info.parameters.get(i);
            Annotation a = param.getAnnotation();
            String value = null;

            if (a instanceof RequestParameter) {
                String name = ((RequestParameter) a).value();
                value = requestParameters.get(name);
            } else if (a instanceof PathVariable) {
                String name = ((PathVariable) a).value();
                value = pathVariables.get(name);
            } else if (a instanceof RequestHeader) {
                String name = ((RequestHeader) a).value();
                value = requestParameters.get(name);
            } else {
                // TODO
            }

            if (null != value) {
                // TODO 日期格式
                if (param.getType() == String.class) {
                    ps[i] = value;
                } else {
                    try {
                        // 类型转换
                        ps[i] = param.getType().getConstructor(String.class).newInstance(value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (a instanceof RequestBody) {
                String name = ((RequestBody) a).value();
                if (!"".equals(name)) {
                    ps[i] = JSONUtils.fromJson(param.getType(), requestParameters.get(name));
                } else {
                    // 整个是对象
                    ps[i] = JSONUtils.fromMap(param.getType(), requestParameters);
                }
            }
        }


        return ps;
    }

    /**
     * 获取类对象实例
     */
    private static Object getObject(Class clazz) {

        Object target = _OBJECT.get(clazz);
        if (null == target) {
            target = ReflectUtils.createInstance(clazz);
            if (null != target)
                _OBJECT.put(clazz, target);
        }
        return target;
    }

    private static Object handlerFilter(Server.Request request) {
        try {
            String key = Filter.class.getSimpleName() + request.getUri();
            Application.MethodInfo info = _OBJECT_METHOD.get(key);

            if (null == info) {
                // TODO
                // /* -> /a | /ab | /c/d/e
                // /a/* -> /a/ | /a/b | /a/b/c
                // /a/*/c -> /a/b/c | /a/bbb/c
            }

            Object target = getObject(info.getClazz());
            Object[] ps = buildParams(info,request.getParameters(),null);

            Method method = info.getMethod();
            method.setAccessible(true);
            return method.invoke(target, ps);
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    /**
     * 将错误信息输出
     *
     * @param e
     */
    private static Object handlerError(Throwable e) {


        String key = Error.class.getSimpleName() + e.getClass().getSimpleName();
        Application.MethodInfo info = _OBJECT_METHOD.get(key);
        if (null == info) {
            return null;
        }

        Object target = getObject(info.getClazz());

        // 参数赋值
        int pcount = info.parameters.size();
        Object[] ps = new Object[pcount];

        if (0 < pcount) {

            for (int i = 0; i < pcount; i++) {
                Param param = info.parameters.get(i);
                if (param.getType() == e.getClass()) {
                    ps[i] = e;
                }
            }
        }
        Method method = info.getMethod();
        method.setAccessible(true);
        try {
            return method.invoke(target, ps);
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }
        return null;
    }


    private static void init(Class clazz) {

        Set<Class> set = ReflectUtils.getSubClasses(clazz);
        for (Class c : set) {
            if (null == c.getAnnotation(RESTful.class)) {
                continue;
            }
            // 获取方法
            List<Method> methods = ReflectUtils.getAllMethods(c);

            for (Method method : methods) {

                GET get = method.getAnnotation(GET.class);
                if (null != get) {
                    add(GET.class.getSimpleName(), get.value(), new MethodInfo(c, method));
                }
                POST post = method.getAnnotation(POST.class);
                if (null != post) {
                    add(POST.class.getSimpleName(), post.value(), new MethodInfo(c, method));
                }

                DELETE delete = method.getAnnotation(DELETE.class);
                if (null != delete) {
                    add(DELETE.class.getSimpleName(), delete.value(), new MethodInfo(c, method));
                }

                Error error = method.getAnnotation(Error.class);
                if (null != error) {
                    add(Error.class.getSimpleName(), error.throwable().getSimpleName(), new MethodInfo(c, method));
                }

                Filter filter = method.getAnnotation(Filter.class);
                if (null != filter) {
                    add(Filter.class.getSimpleName(), filter.value(), new MethodInfo(c, method));
                }
            }
        }
    }

    // 添加到方法对象
    private static void add(String method, String path, MethodInfo info) {

        String key = method + path;
        if (_OBJECT_METHOD.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate @" + method + "(\"" + path + "\")");
        }
        LogUtils.debug(Application.class, "Method:{} Path:{}", method, path);
        _OBJECT_METHOD.put(key, info);
    }

    /**
     * 正则搞不懂，硬解析
     *
     * @param path1 /x/ss{id}
     * @param path2 /x/ss234
     *              {id=234}
     */
    private static void buildPath(String path1, String path2, Map<String, String> pathVar) {
        int start = path1.indexOf('{');
        if (-1 == start) {
            return;
        }

        String tmp1 = path1.substring(0, start);
        if (!path2.startsWith(tmp1)) {
            return;// 不属于
        }
        String[] tmp1s = path1.substring(start, path1.length()).split("\\/");
        String[] tmp2s = path2.substring(start, path2.length()).split("\\/");
        if (tmp1s.length != tmp2s.length || 0 == tmp1s.length) {
            return;
        }

        // 假定他们是一样的
        for (int i = 0; i < tmp1s.length; i++) {
            String key = tmp1s[i];
            String value = tmp2s[i];

            int start1 = key.indexOf('{');
            int end1 = key.indexOf('}');

            if (0 != start1 || end1 != key.length() - 1) {
                // 需要掐头去尾
                if (start1 > 0) {
                    String startStr = key.substring(0, start1);
                    if (!value.startsWith(startStr)) {
                        pathVar.clear();
                        return;// 不匹配
                    }
                    value = value.substring(start1, value.length());
                }

                // 去尾
                if (end1 != key.length() - 1) {
                    String endKeyStr = key.substring(end1 + 1, key.length());
                    if (!value.endsWith(endKeyStr)) {
                        pathVar.clear();
                        return;// 不匹配
                    }
                    value = value.substring(0, value.indexOf(endKeyStr));
                }
            }

            key = key.substring(start1 + 1, end1);
            pathVar.put(key, value);

        }


    }

    private static class MethodInfo {

        private Class clazz;

        private Method method;

        private List<Param> parameters;// 所有参数名

        public MethodInfo(Class clazz, Method method) {
            this.clazz = clazz;
            this.method = method;

            this.parameters = new ArrayList<Param>();

            // 解析参数适配1.5、1.8有新方法
            Class<?>[] types = method.getParameterTypes();
            int length = types.length;
            if (length == 0) {
                return;
            }

            for (int i = 0; i < length; i++) {

                Class pc = types[i];
                this.parameters.add(new Param(pc, null));// 先把类型填入

                Annotation[] parameterAnnotations = method.getParameterAnnotations()[i];
                for (Annotation annotation : parameterAnnotations) {
                    this.parameters.set(i, new Param(pc, annotation));
                }
            }

        }

        public Class getClazz() {
            return clazz;
        }

        public void setClazz(Class clazz) {
            this.clazz = clazz;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public List<Param> getParameters() {
            return parameters;
        }

        public void setParameters(List<Param> parameters) {
            this.parameters = parameters;
        }
    }


    // 一个参数只支持一个注解
    private static class Param {
        private Class type;// 参数类型

        private Annotation annotation;// 参数注解

        public Param(Class type, Annotation annotation) {
            this.type = type;
            this.annotation = annotation;
        }

        public Class getType() {
            return type;
        }

        public void setType(Class type) {
            this.type = type;
        }

        public Annotation getAnnotation() {
            return annotation;
        }

        public void setAnnotation(Annotation annotation) {
            this.annotation = annotation;
        }
    }


}
