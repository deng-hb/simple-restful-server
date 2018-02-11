package com.denghb.restful;

import com.denghb.restful.annotation.*;
import com.denghb.restful.utils.JSONUtils;
import com.denghb.restful.utils.LogUtils;
import com.denghb.restful.utils.ReflectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 先这样写着，有时间重构一下
 */
public class Application {


    static Map<String, Application.Info> _INFO = new ConcurrentHashMap<String, Application.Info>();
    static Map<Class, Object> _RESTful = new ConcurrentHashMap<Class, Object>();

    private Application() {

    }

    public static void run(Class clazz, String[] args) {

        init(clazz);

        Server server = new Server();
        // 在start之前
        server.setHandler(new Server.Handler() {
            public Server.Response execute(Server.Request request) {
                try {
                    LogUtils.info(getClass(), "{}\t{}", request.getMethod(), request.getUri());

                    Map<String, String> pathVar = new HashMap<String, String>();

                    String path = request.getMethod() + request.getUri();
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
                            return Server.Response.build("404");
                        }
                    }

                    Class cc = info.getClazz();
                    Object target = _RESTful.get(cc);
                    if (null == target) {
                        target = ReflectUtils.createInstance(cc);
                        _RESTful.put(cc, target);
                    }

                    // 参数赋值
                    int pcount = info.parameters.size();
                    Object[] ps = new Object[pcount];

                    if (0 < pcount) {

                        for (int i = 0; i < pcount; i++) {
                            Param param = info.parameters.get(i);
                            if (param.isSingle()) {
                                String value = null;
                                String name = param.getName();

                                int from = param.getFrom();
                                if (1 == from) {
                                    value = request.getParameters().get(name);
                                } else if (2 == from) {
                                    // path
                                    value = pathVar.get(name);
                                } else if (3 == from) {
                                    // header
                                    value = request.getHeaders().get(name);
                                }

                                if (param.getType() == String.class) {
                                    ps[i] = value;
                                } else {
                                    // 类型转换
                                    ps[i] = param.getType().getConstructor(String.class).newInstance(value);
                                }
                            } else {
                                ps[i] = JSONUtils.fromMap(param.getType(), request.getParameters());
                            }
                        }
                    }

                    // 执行方法
                    Method method = info.method;
                    method.setAccessible(true);
                    Object result = method.invoke(target, ps);
                    return Server.Response.build(result);
                } catch (Exception e) {
                    LogUtils.error(getClass(), e.getMessage(), e);
                }
                return Server.Response.build("ERROR");
            }
        });

        server.start(args);
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

    private static Set<Class> SINGLE_TYPES = new HashSet<Class>();

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

    private static class Info {

        private Class clazz;

        private Method method;

        private List<Param> parameters;// 所有参数名

        public Info(Class clazz, Method method) {
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
                if (!SINGLE_TYPES.contains(pc)) {// 非基本类型
                    this.parameters.add(new Param(pc, null, false, 0));
                    continue;
                }

                Annotation[] parameterAnnotations = method.getParameterAnnotations()[i];
                for (Annotation annotation : parameterAnnotations) {

                    if (annotation instanceof ParameterName) {
                        ParameterName a = (ParameterName) annotation;
                        this.parameters.add(new Param(pc, a.value(), true, 1));
                    }
                    //
                    if (annotation instanceof PathVariable) {
                        PathVariable a = (PathVariable) annotation;
                        this.parameters.add(new Param(pc, a.value(), true, 2));

                    }

                    if (annotation instanceof RequestHeader) {
                        RequestHeader a = (RequestHeader) annotation;
                        this.parameters.add(new Param(pc, a.value(), true, 3));
                    }

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


    private static class Param {
        private Class type;

        private String name;// 非基本类型没有名字

        private boolean single;// 基本类型

        private int from;// 1:form 2:path 3:header  枚举？

        public Param(Class type, String name, boolean single, int from) {
            this.type = type;
            this.name = name;
            this.single = single;
            this.from = from;
        }

        public Class getType() {
            return type;
        }

        public void setType(Class type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isSingle() {
            return single;
        }

        public void setSingle(boolean single) {
            this.single = single;
        }

        public int getFrom() {
            return from;
        }

        public void setFrom(int from) {
            this.from = from;
        }
    }
}
