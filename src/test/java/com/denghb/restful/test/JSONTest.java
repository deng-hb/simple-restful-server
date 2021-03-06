package com.denghb.restful.test;

import com.denghb.json.JSON;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class JSONTest {

    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;

    private static String generateQRCode(String text, int width, int height) throws Exception {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeBase64String(bytes);
    }

    @Test
    public void test1() throws Exception {
        String base64 = generateQRCode("hello world", 100, 100);
        System.out.println(base64);
    }

    public static class Book {
        private String name;

        private Float price;

        public Book() {

        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Float getPrice() {
            return price;
        }

        public void setPrice(Float price) {
            this.price = price;
        }

        @Override
        public String toString() {
            return "Book{" +
                    "name='" + name + '\'' +
                    ", price=" + price +
                    '}';
        }
    }

    public static class User {
        private String name;

        private Long luckNumber;

        private int age;

        private Date birthday;

        private String[] likes;

        private List list;

        private Map<String, String> book1;

        private List<Book> books;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getLuckNumber() {
            return luckNumber;
        }

        public void setLuckNumber(Long luckNumber) {
            this.luckNumber = luckNumber;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Date getBirthday() {
            return birthday;
        }

        public void setBirthday(Date birthday) {
            this.birthday = birthday;
        }

        public String[] getLikes() {
            return likes;
        }

        public void setLikes(String[] likes) {
            this.likes = likes;
        }

        public List getList() {
            return list;
        }

        public void setList(List list) {
            this.list = list;
        }

        public Map<String, String> getBook1() {
            return book1;
        }

        public void setBook1(Map<String, String> book1) {
            this.book1 = book1;
        }

        public List<Book> getBooks() {
            return books;
        }

        public void setBooks(List<Book> books) {
            this.books = books;
        }

        @Override
        public String toString() {
            return "User{" +
                    "name='" + name + '\'' +
                    ", luckNumber=" + luckNumber +
                    ", age=" + age +
                    ", birthday=" + birthday +
                    ", likes=" + Arrays.toString(likes) +
                    ", list=" + list +
                    ", book1=" + book1 +
                    ", books=" + books +
                    '}';
        }
    }

    public static void main(String[] args) {
        String json1 = "{ \"name\" :    \"张三\" ,\"birthday\":\"1990-01-01\"}";
        String json2 = "[ { \"name\" :    \"张三\" ,\"age\":24,\"luckNumber\":100000},{\"birthday\":\"1990-01-01\"}]";
        String json3 = "[  \"name\", \"张三\" ,\"birthday\",\"1990-01-01\"]";
        String json4 = "{ \"name\" :    \"张三\" ,\"birthday\":\"1990-01-01 09:23\", \"book1\":{ \"name\":\"西游记\" }}";
        String json5 = "{ \"name\" :    \"张三\" ,\"birthday\":\"1990-01-01\", \"books\":[{ \"name\":\"西游记\" },{ \"name\":\"水浒传\" }]}";
        String json6 = "{ \"name\" :    \"张三\" ,\"birthday\":\"1990-01-01\", \"books\":[{ \"name\":\"西游记\" },{ \"name\":\"水浒传\" }],\"list\":[\"1\",\"2\"]}";

        String json7 = "{ \"name\" :    \"张三\" ,\"birthday\":\"1990-01-01\", \"books\":[{ \"name\":\"西游记\" },{ \"name\":\"水浒传\" }],\"list\":[\"1\",\"2\"],   \"likes\":[\"music\",\"看剧\"]}";


        System.out.println("---------------------------parse");

        Map obj1 = JSON.parseJSON(Map.class, json1);
        System.out.println("1:" + obj1);
        List<User> obj2 = JSON.parseArrayJSON(User.class, json2);
        System.out.println("2:" + obj2);

        List<String> obj3 = JSON.parseArrayJSON(String.class, json3);
        System.out.println("3:" + obj3);

        User obj4 = JSON.parseJSON(User.class, json4);
        System.out.println("4:" + obj4);

        User obj5 = JSON.parseJSON(User.class, json5);
        System.out.println("5:" + obj5);

        User obj6 = JSON.parseJSON(User.class, json6);
        System.out.println("6:" + obj6);

        User obj7 = JSON.parseJSON(User.class, json7);
        System.out.println("7:" + obj7);

        System.out.println("---------------------------toJSON");


        String res1 = JSON.toJSON(obj1);
        System.out.println("1:" + res1);

        String res2 = JSON.toJSON(obj2);
        System.out.println("2:" + res2);

        String res3 = JSON.toJSON(obj3);
        System.out.println("3:" + res3);

        String res4 = JSON.toJSON(obj4, "yyyy-MM-dd");
        System.out.println("4:" + res4);

        String res5 = JSON.toJSON(obj5);
        System.out.println("5:" + res5);

        String res6 = JSON.toJSON(obj6);
        System.out.println("6:" + res6);

        String res7 = JSON.toJSON(obj7);
        System.out.println("7:" + res7);
    }
}
