package com.example.jsonparser;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.*;

public class JsonConfigurationParser {

    public JsonConfigurationParser() {}

    @SuppressWarnings("unchecked")
    public <T> T readObject(final DocumentContext documentContext, Map<String, Object> path)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String clazz = (String)path.get("class");
        T t = (T) Class.forName(clazz).getDeclaredConstructors()[0].newInstance();
        Field[] field = t.getClass().getDeclaredFields();

        for (Field f : field) {
            f.setAccessible(true);
            Object o = path.get(f.getName());
            if(o instanceof Map)
                f.set(t, readTree(documentContext, (Map<String, Object>)o));
            if(o instanceof String) {
                System.out.println("Field : " + f.getName() + ", value : " + o);
                f.set(t, documentContext.read("$." + o));
            }
        }

        return t;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> readTree(final DocumentContext documentContext, Map<String, Object> path)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<T> list = new ArrayList<>();
        String clazz = (String)path.get("class");

        int count = 0;
        Object object = documentContext.read("$." + path.get("count"));
        if(!(object instanceof JSONArray))
            return Collections.EMPTY_LIST;

        count = ((JSONArray) object).size();
        for(int i = 0; i < count; i++) {
            T t = (T) Class.forName(clazz).getDeclaredConstructors()[0].newInstance();
            Field[] field = t.getClass().getDeclaredFields();
            for (Field f : field) {
                f.setAccessible(true);
                Object o = path.get(f.getName());
                if (o instanceof Map)
                    f.set(t, readTree(documentContext, (Map<String, Object>) o));

                if(o instanceof String) {
                    System.out.println("Field : " + f.getName() + ", value : " + o);
                    f.set(t, documentContext.read("$." + String.format((String)o, i)));
                }
            }
            list.add(t);
        }

        return list;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Map<String, Object> map = new HashMap<>();

        map.put("class", "com.example.jsonparser.Person");
        map.put("name", "person.name");
        map.put("age", "person.age");

        Map<String, String> orderMap = new HashMap<>();
        orderMap.put("class", "com.example.jsonparser.Order");
        orderMap.put("count", "person.orders");
        orderMap.put("id", "person.orders[%d].id");
        orderMap.put("name", "person.orders[%d].item");

        map.put("orders", orderMap);

        final String json = new String(Files.readAllBytes(new ClassPathResource("input.json").getFile().toPath()));
        JsonConfigurationParser jsonConfigurationParser = new JsonConfigurationParser();
        Person person = jsonConfigurationParser.readObject(JsonPath.parse(json), map);
        System.out.println(person);
    }

}
