package com.denghb.restful.test;

import com.denghb.demo.domain.User;
import com.denghb.restful.utils.JSONUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestApp {

    static String HOST = "http://localhost:8888";

    @Before
    public void setUp() {
        // 先启动RESTful服务
    }

    @Test
    public void test() {
        String res = HttpUtils.get(HOST);

        System.out.println(res);
    }

    @Test
    public void testXX() {
        String res = HttpUtils.get(HOST + "/xx");

        System.out.println(res);
    }

    @Test
    public void testXX3() {
        String res = HttpUtils.get(HOST + "/xx/asdasdas/basdasdasdaa");

        System.out.println(res);
    }
    @Test
    public void testError() {
        String res = HttpUtils.get(HOST + "/error");

        System.out.println(res);
    }
    @Test
    public void testError2() {
        String res = HttpUtils.get(HOST + "/error2");

        System.out.println(res);
    }
    @Test
    public void testHeader() {
        String res = HttpUtils.get(HOST + "/test/header");

        System.out.println(res);
    }

    @Test
    public void pullUsers() {
        String res = HttpUtils.get(HOST + "/user");

        System.out.println(res);
    }
    @Test
    public void pullUsers2() {
        String res = HttpUtils.get(HOST + "/userList");

        System.out.println(res);
    }

    @Test
    public void createUser() {
        User user = new User();
        user.setMobile("199999993");
        user.setName("tom");

        String body = JSONUtils.toJson(user);
        String res = HttpUtils.post(HOST + "/user", body);

        System.out.println(res);
    }

    @Test
    public void createUser2() {
        Map<String, String> param = new HashMap<String, String>();
        param.put("mobile", "199999995");
        param.put("name", "老王");

        String res = HttpUtils.post(HOST + "/user2", param);

        System.out.println(res);
    }

    @Test
    public void queryUser() {

        String res = HttpUtils.get(HOST + "/user/1");

        System.out.println(res);
    }

    @Test
    public void queryUser2() {

        String res = HttpUtils.get(HOST + "/user2?id=1");

        System.out.println(res);
    }
    @Test
    public void deleteUser1() {

        String res = HttpUtils.delete(HOST + "/user/1");

        System.out.println(res);
    }

    private static List<String> descFormat(String desc) {
        List<String> list = new ArrayList<String>();
        Pattern pattern = Pattern.compile("\\{([^\\}]+)\\}");
        Matcher matcher = pattern.matcher(desc);
        while (matcher.find()) {
            String t = matcher.group(1);
            list.add(t);
        }
        return list;
    }

    @Test
    public void test1() {

        String p = "GET/user/{id}/";

        p = p.replaceAll("\\/", "\\\\/");
        while (true) {
            if (-1 != p.indexOf('{')) {
                p = p.replaceAll("\\{", "(?<");
                p = p.replaceAll("\\}", ">[^\\\\/]+)");
                continue;
            }
            break;
        }

        String a = "GET/user/1/";

        p = "^" + p + "$";
//        p = "^\\/user\\/(?<id>[^\\/]+)$";

        Pattern pattern = Pattern.compile(p);
        Matcher m = pattern.matcher(a);
//        String s = m.group("id"); // JDK1.7才有 放弃

        boolean b = m.matches();

        System.out.println(b);
    }

    @Test
    public void test2() {

        // 怎么样将 id=22  str=haha 获取出来，怎么判断s2和s1类似
        String path1 = "GET/user/xx{id}yyz/{str}";

        String path2 = "GET/user/xx2sdsds2yyz/22";

        Map<String, String> p = buildPath(path1, path2);
        System.out.println(p);
    }

    // 硬搞。。
    private static Map<String, String> buildPath(String path1, String path2) {
        int start = path1.indexOf("{");
        if (-1 < start) {
            String tmp1 = path1.substring(0, start);
            if (path2.startsWith(tmp1)) {
                // 属于
                String[] tmp1s = path1.substring(start, path1.length()).split("\\/");

                String[] tmp2s = path2.substring(start, path2.length()).split("\\/");
                if (tmp1s.length == tmp2s.length && 0 != tmp1s.length) {
                    // 假定他们是一样的
                    Map<String, String> p = new HashMap<String, String>();
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
                                    return null;// 不匹配
                                }
                                value = value.substring(start1, value.length());
                            }

                            // 去尾
                            if (end1 != key.length() - 1) {
                                String endKeyStr = key.substring(end1 + 1, key.length());
                                String endValueStr = value.substring(value.indexOf(endKeyStr), value.length());
                                if (!endKeyStr.equals(endValueStr)) {
                                    return null;// 不匹配
                                }
                                value = value.substring(0, value.indexOf(endKeyStr));
                            }
                        }

                        key = key.substring(start1 + 1, end1);
                        p.put(key, value);

                    }
                    return p;
                }

            }
        }
        return null;
    }

}
