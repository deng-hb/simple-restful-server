package com.denghb.restful;


import com.denghb.eorm.Eorm;
import com.denghb.eorm.impl.EormMySQLImpl;
import com.denghb.json.JSON;
import com.denghb.restful.annotation.*;
import com.denghb.restful.annotation.Error;
import com.denghb.utils.ConfigUtils;
import com.denghb.utils.ReflectUtils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
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

    static Server _SERVER = new Server();

    private Application() {

    }

    private static void outLog(Class clazz, String format, Object... arguments) {
        String log = clazz.getName() + "\t";
        log += format;

        for (Object object : arguments) {
            log = log.replaceFirst("\\{\\}", String.valueOf(object));
        }

        System.out.println(log);
    }

    private static void outLog(Class clazz, String msg, Throwable t) {

        String log = clazz.getName() + "\t";
        System.err.println(log + msg);
        t.printStackTrace();
    }

    public static void run(Class clazz, String[] args) {

        init(clazz);

        // 在start之前
        _SERVER.setHandler(new Server.Handler() {
            public Server.Response execute(Server.Request request) {


                String uri = request.getUri();

                // TODO 运行状态
                if (uri.equals("/status")) {

                    return Server.Response.build(_OBJECT_METHOD);
                }

                outLog(getClass(), "{}\t{}", request.getMethod(), uri);

                // 过滤
                Object object = handlerFilter(request);
                if (null != object) {
                    return Server.Response.build(object);
                }

                String path = request.getMethod() + uri;
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

                    // 文件匹配
                    URL url = this.getClass().getResource("/static" + uri);
                    if (null != url) {
                        File file = new File(url.getFile());
                        if (file.exists() && !file.isDirectory()) {
                            return Server.Response.build(file);
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
                    Object result = method.invoke(target, buildParams(info, request, pathVariables));

                    return Server.Response.build(result);
                } catch (InvocationTargetException e) {
                    // 调用方法抛出异常
                    Object result = handlerError(e.getTargetException());
                    if (null != result) {
                        return Server.Response.build(result);
                    }
                } catch (Exception e) {
                    outLog(getClass(), e.getMessage(), e);

                    // 内部错误
                    Object result = handlerError(new RESTfulException(e.getMessage(), 500));
                    if (null != result) {
                        return Server.Response.build(result);
                    }
                }

                return Server.Response.buildError(500);
            }
        });

        _SERVER.start(args);
    }

    /**
     * 停止服务
     */
    public static void stop() {
        if (null != _SERVER) {
            _SERVER.shutdown();
            _SERVER = null;
        }
    }

    /**
     * 参数列表赋值
     */
    private static Object[] buildParams(Application.MethodInfo info, Server.Request request, Map<String, String> pathVariables) {

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
                value = request.getParameters().get(name);
            } else if (a instanceof PathVariable) {
                String name = ((PathVariable) a).value();
                value = pathVariables.get(name);
            } else if (a instanceof RequestHeader) {
                String name = ((RequestHeader) a).value();
                value = request.getHeaders().get(name);
            } else if (a instanceof RequestBody) {
                // 整个是对象
                ps[i] = JSON.map2Object(param.getType(), request.getParameters());
            } else if (param.getType() == Server.Request.class) {
                ps[i] = request;
            } else if (param.getType() == Eorm.class) {
                ps[i] = getObject(param.getType());
            } else {
                // TODO
            }

            if (null != value) {
                // TODO 日期格式
                if (param.getType() == String.class) {
                    ps[i] = value;
                } else {
                    // 构造函数实例化
                    ps[i] = ReflectUtils.constructorInstance(param.getType(), String.class, value);
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
            if (clazz == Eorm.class) {
                // 数据库实例化
                String url = ConfigUtils.getValue("db.url");
                String username = ConfigUtils.getValue("db.username");
                String password = ConfigUtils.getValue("db.password");
                Eorm eorm = new EormMySQLImpl(url, username, password);
                _OBJECT.put(clazz, eorm);
                return eorm;
            }

            target = ReflectUtils.createInstance(clazz);
            if (null != target)
                _OBJECT.put(clazz, target);
        }
        return target;
    }

    private static Object handlerFilter(Server.Request request) {
        try {
            String key = Filter.class.getSimpleName() + request.getMethod() + request.getUri();

            Application.MethodInfo info = null;
            for (String key1 : _OBJECT_METHOD.keySet()) {
                boolean b = comparePath(key1, key);
                if (b) {
                    info = _OBJECT_METHOD.get(key1);
                    break;
                }

            }
            if (null == info) {
                return null;
            }

            Object target = getObject(info.getClazz());
            Object[] ps = buildParams(info, request, null);

            Method method = info.getMethod();
            method.setAccessible(true);
            return method.invoke(target, ps);
        } catch (InvocationTargetException e1) {
            return handlerError(e1);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    /**
     * /* -> /x/xx || /xxx/xx/xx..  true
     * /*a/aa -> /xxxa/aa  true
     */
    private static boolean comparePath(String origin, String uri) {

        String[] tmp1s = origin.split("\\/");
        String[] tmp2s = uri.split("\\/");

        int len1 = tmp1s.length;
        int len2 = tmp2s.length;

        for (int i = 0; i < len1; i++) {
            String s1 = tmp1s[i];

            if ("*".equals(s1)) {
                return true;
            }
            String s2 = tmp2s[i];

            int start = s1.indexOf('*');
            if (-1 < start) {

                String s1start = s1.substring(0, start);

                if (!s2.startsWith(s1start)) {
                    return false;
                }
                int end = s1.lastIndexOf('*');
                String s1end = s1.substring(end + 1, s1.length());

                if (!s2.endsWith(s1end)) {
                    return false;
                }

            } else if (!s1.equals(s2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 错误处理
     */
    private static Object handlerError(Throwable e) {


        try {
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
            return method.invoke(target, ps);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }


    private static void init(Class clazz) {

        Set<Class> set = ReflectUtils.getSubClasses(clazz);
        for (Class c : set) {
            RESTful rest = (RESTful) c.getAnnotation(RESTful.class);
            if (null == rest) {
                continue;
            }
            String url = rest.value();
            // 获取方法
            List<Method> methods = ReflectUtils.getAllMethods(c);

            for (Method method : methods) {

                GET get = method.getAnnotation(GET.class);
                if (null != get) {
                    add(GET.class.getSimpleName(), url + get.value(), new MethodInfo(c, method));
                }
                POST post = method.getAnnotation(POST.class);
                if (null != post) {
                    add(POST.class.getSimpleName(), url + post.value(), new MethodInfo(c, method));
                }
                PUT put = method.getAnnotation(PUT.class);
                if (null != put) {
                    add(PUT.class.getSimpleName(), url + put.value(), new MethodInfo(c, method));
                }

                DELETE delete = method.getAnnotation(DELETE.class);
                if (null != delete) {
                    add(DELETE.class.getSimpleName(), url + delete.value(), new MethodInfo(c, method));
                }

                Error error = method.getAnnotation(Error.class);
                if (null != error) {
                    add(Error.class.getSimpleName(), error.throwable().getSimpleName(), new MethodInfo(c, method));
                }

                Filter filter = method.getAnnotation(Filter.class);
                if (null != filter) {

                    Class[] ms = filter.method();
                    for (Class cl : ms) {
                        // GET/*  POST/*
                        String path = cl.getSimpleName() + filter.value();
                        add(Filter.class.getSimpleName(), path, new MethodInfo(c, method));
                    }
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
