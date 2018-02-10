package com.denghb.restful;

import com.denghb.restful.annotation.*;
import com.denghb.restful.utils.JSONUtils;
import com.denghb.restful.utils.LogUtils;
import com.denghb.restful.utils.ReflectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 先这样写着，有时间重构一下
 */
public class Application {


    static Map<String, Application.Info> _INFO = new ConcurrentHashMap<String, Application.Info>();
    static Map<Class, Object> _RESTful = new ConcurrentHashMap<Class, Object>();

    static Set<Class> SINGLE_TYPES = new HashSet<Class>();

    static {
        SINGLE_TYPES.add(int.class);
        SINGLE_TYPES.add(long.class);
        SINGLE_TYPES.add(float.class);
        SINGLE_TYPES.add(double.class);

        SINGLE_TYPES.add(Integer.class);
        SINGLE_TYPES.add(String.class);
        SINGLE_TYPES.add(Float.class);
        SINGLE_TYPES.add(Long.class);
        SINGLE_TYPES.add(Double.class);
        SINGLE_TYPES.add(Number.class);
        SINGLE_TYPES.add(Boolean.class);

        SINGLE_TYPES.add(java.math.BigDecimal.class);
        SINGLE_TYPES.add(java.math.BigInteger.class);

        SINGLE_TYPES.add(java.util.Date.class);
    }

    private Application() {

    }

    public static void run(Class clazz, String[] args) {

        aa(clazz);

        Server server = new Server();

        server.setHandler(new Server.Handler() {
            public String execute(String request) {
                try {


                    Map<String, String> param = new HashMap<String, String>();
                    Map<String, String> pathVar = new HashMap<String, String>();

                    String httpMethod = request.substring(0, request.indexOf(" "));
                    String uri = request.substring(request.indexOf(" ") + 1, request.indexOf(" ", request.indexOf(" ") + 1));
                    //有问号表示后面跟有参数
                    if (uri.contains("?")) {
                        String attr = uri.substring(uri.indexOf("?") + 1, uri.length());
                        uri = uri.substring(0, uri.indexOf("?"));

                        buildParam(param, attr);
                    }

                    String p = request.substring(request.indexOf("\r\n\r\n") + 4, request.length());
                    buildParam(param, p);

                    for (String key : param.keySet()) {
                        LogUtils.info(getClass(), key + ":" + param.get(key));
                    }

                    String path = httpMethod + uri;
                    Info info = _INFO.get(path);

                    if (null == info) {
                        for (String path1 : _INFO.keySet()) {
                            buildPath(path1, path, pathVar);
                            if (!pathVar.isEmpty()) {
                                info = _INFO.get(path1);
                                break;
                            }
                        }

                        if (pathVar.isEmpty()) {
                            LogUtils.info(getClass(), httpMethod + "\t" + uri + "\t" + 404);
                            return "404";
                        }
                    }
                    LogUtils.info(getClass(), httpMethod + "\t" + uri);

                    Class cc = info.getClazz();
                    Object target = _RESTful.get(cc);
                    if (null == target) {
                        target = ReflectUtils.createInstance(cc);
                        _RESTful.put(cc, target);
                    }


                    Method method = info.method;

                    // 缓存？ 适配1.5
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    Object[] ps = new Object[parameterTypes.length];
                    for (int i = 0; i < parameterTypes.length; i++) {

                        Class pc = parameterTypes[i];
                        if (!SINGLE_TYPES.contains(pc)) {
                            ps[i] = JSONUtils.fromJson(pc, p);
                        } else {

                            Annotation[] parameterAnnotations = method.getParameterAnnotations()[i];
                            for (Annotation annotation : parameterAnnotations) {

                                if (!pathVar.isEmpty()) {
                                    // 链接上的
                                    if (annotation instanceof PathVariable) {
                                        PathVariable pathVariable = (PathVariable) annotation;
                                        String s = pathVar.get(pathVariable.value());
                                        if (pc == String.class) {
                                            ps[i] = s;
                                        } else {
                                            ps[i] = pc.getConstructor(String.class).newInstance(s);
                                        }
                                    }
                                }

                                if (annotation instanceof ParameterName) {
                                    ParameterName parameterName = (ParameterName) annotation;
                                    String s = param.get(parameterName.value());
                                    if (pc == String.class) {
                                        ps[i] = s;
                                    } else {
                                        // 不是字符串类型参数重构赋值
                                        ps[i] = pc.getConstructor(String.class).newInstance(s);
                                    }
                                }
                            }
                        }
                    }

                    method.setAccessible(true);
                    Object result = method.invoke(target, ps);
                    if (result instanceof String) {
                        return String.valueOf(result);
                    }
                    return JSONUtils.toJson(result);

                } catch (Exception e) {
                    LogUtils.error(getClass(), e.getMessage(), e);
                }
                return "error";
            }
        });

        server.start(args);
    }


    private static void buildParam(Map<String, String> param, String p) {
        if (null == param) {
            param = new HashMap<String, String>();
        }

        if ("".equals(p)) {
            return;
        }

        // JSON ?
        if (p.startsWith("{")) {
            Map a = JSONUtils.fromJson(Map.class, p);

            param.putAll(a);

            return;
        }

        String[] attrs = p.split("&");
        for (String string : attrs) {
            String key = string.substring(0, string.indexOf("="));
            String value = string.substring(string.indexOf("=") + 1);

            try {
                value = URLDecoder.decode(value, "utf-8");
            } catch (Exception e) {

            }
            param.put(key, value);
        }
    }

    private static void aa(Class clazz) {

        Set<Class> set = ReflectUtils.getSubClasses(clazz);
        for (Class c : set) {
            if (null != c.getAnnotation(RESTful.class)) {
                // 获取方法
                List<Method> methods = ReflectUtils.getAllMethods(c);

                for (Method method : methods) {


                    GET get = method.getAnnotation(GET.class);
                    if (null != get) {
                        add(GET.class.getSimpleName(), get.value(), new Info(c, method));
                        continue;
                    }
                    POST post = method.getAnnotation(POST.class);
                    if (null != post) {
                        add(POST.class.getSimpleName(), post.value(), new Info(c, method));
                        continue;
                    }


                }

            }
        }
    }

    private static void add(String method, String path, Info info) {

        String key = method + path;
        if (_INFO.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate @" + method + "(\"" + path + "\")");
        }
        LogUtils.debug(Application.class, "Method:{} Path:{}", method, path);
        _INFO.put(key, info);
    }

    /**
     * 正则搞不懂，硬解析
     *
     * @param path1 /x/ss{id}
     * @param path2 /x/ss234
     *              {id=234}
     */
    private static void buildPath(String path1, String path2, Map<String, String> pathVar) {
        int start = path1.indexOf("{");
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

            int start1 = key.indexOf("{");
            int end1 = key.indexOf("}");

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
                    String endValueStr = value.substring(value.indexOf(endKeyStr), value.length());
                    if (!endKeyStr.equals(endValueStr)) {
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

    private static class Info {

        private Class clazz;

        private Method method;

        public Info(Class clazz, Method method) {
            this.clazz = clazz;
            this.method = method;
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
    }
}
