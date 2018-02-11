package com.denghb.restful.test;

import com.denghb.json.JSON;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class JSONTest {

    @Test
    public void test1() {

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

        private Date birthday;

        private Map<String, String> book1;

        private List<Book> books;

        public User() {

        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Date getBirthday() {
            return birthday;
        }

        public void setBirthday(Date birthday) {
            this.birthday = birthday;
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
                    ", birthday=" + birthday +
                    ", book1=" + book1 +
                    ", books=" + books +
                    '}';
        }
    }

    public static void main(String[] args) {
        String json1 = "{ \"name\" :    \"张三\" ,\"birthday\":\"1990-01-01\"}";
        String json2 = "[ { \"name\" :    \"张三\" },{\"birthday\":\"1990-01-01\"}]";
        String json3 = "[  \"name\", \"张三\" ,\"birthday\",\"1990-01-01\"]";
        String json4 = "{ \"name\" :    \"张三\" ,\"birthday\":\"1990-01-01 09:23\", \"book1\":{ \"name\":\"西游记\" }}";
        String json5 = "{ \"name\" :    \"张三\" ,\"birthday\":\"1990-01-01\", \"books\":[{ \"name\":\"西游记\" },{ \"name\":\"水浒传\" }]}";

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
    }
}
