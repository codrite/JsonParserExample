package com.example.jsonparser;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.*;

@Slf4j
public class JsonConfigurationParser {

    public JsonConfigurationParser() {}

    public Object read(final DocumentContext documentContext, Map<String, Object> path) {
        boolean isRootArray = (path.get("class") != null && path.get("count") != null);
        try {
            return (isRootArray ?
                this.readArray(documentContext, path, "$")
                :
                this.readObject(documentContext, path));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T readObject(final DocumentContext documentContext, Map<String, Object> path)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String clazz = (String)path.get("class");
        T t = (T) Class.forName(clazz).getDeclaredConstructors()[0].newInstance();
        Field[] field = t.getClass().getDeclaredFields();
        final String basePath = (String)path.get("basePath");
        for (Field f : field) {
            f.setAccessible(true);
            Object o = path.get(f.getName());
            if(isJsonArray(o))
                f.set(t, readArray(documentContext, (Map<String, Object>)o, basePath));

            if(isJsonObject(o))
                f.set(t, readObject(documentContext, (Map<String, Object>)o, basePath));

            if(o instanceof String) {
                log.info("Field : " + clazz + "." + f.getName() + ", value : " + o);
                f.set(t, documentContext.read("$." + basePath + "." + o));
            }
        }

        return t;
    }

    @SuppressWarnings("unchecked")
    public <T> T readObject(final DocumentContext documentContext, Map<String, Object> path, String parentBasePath)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String clazz = (String)path.get("class");
        String basePath = parentBasePath + "." + path.get("basePath");
        T t = (T) Class.forName(clazz).getDeclaredConstructors()[0].newInstance();
        Field[] field = t.getClass().getDeclaredFields();

        for (Field f : field) {
            f.setAccessible(true);
            Object o = path.get(f.getName());
            if(o instanceof String) {
                log.info("Append Base Path and Current Path : {}", (basePath + "." + o));
                try {
                    f.set(t, documentContext.read(basePath + "." + o));
                } catch (Exception e) {
                    log.error("Missing Json path : {}", basePath + "." + o);
                }
            }
        }

        return t;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> readArray(final DocumentContext documentContext, Map<String, Object> path, String parentBasePath)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<T> list = new ArrayList<>();
        String countPath = parentBasePath + "." + path.get("count");
        String basePath = parentBasePath + "." + path.get("basePath");
        String clazz = (String)path.get("class");

        log.info("Base Path : {}, Count Path : {}", basePath, countPath);
        int count;
        Object object;
        try {
            object = documentContext.read(countPath);
        } catch (Exception e) {
            log.error("Missing Json path : {}", countPath);
            return null;
        }

        if(!(object instanceof JSONArray))
            return Collections.EMPTY_LIST;

        count = ((JSONArray) object).size();
        for(int i = 0; i < count; i++) {
            T t = (T) Class.forName(clazz).getDeclaredConstructors()[0].newInstance();
            Field[] field = t.getClass().getDeclaredFields();
            final String pathValue = String.format(basePath, i);
            for (Field f : field) {
                f.setAccessible(true);
                Object o = path.get(f.getName());
                if (isJsonArray(o))
                    f.set(t, readArray(documentContext, (Map<String, Object>) o, pathValue));

                if (isJsonObject(o))
                    f.set(t, readObject(documentContext, (Map<String, Object>) o, pathValue));

                if(o instanceof String) {
                    log.info("Field : {}, value : {}", clazz + "." + f.getName(), pathValue + "." + o);
                    f.set(t, documentContext.read(pathValue + "." + o));
                }
            }
            list.add(t);
        }

        return list;
    }

    boolean isJsonArray(Object o) {
        return o instanceof Map && ((Map<?, ?>) o).containsKey("count");
    }

    boolean isJsonObject(Object o) {
        return o instanceof Map && !((Map<?, ?>) o).containsKey("count");
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Map<String, Object> map = JsonSchemaReader.readSchema(new String(Files.readAllBytes(new ClassPathResource("personSchema.json").getFile().toPath())));
        final String json = new String(Files.readAllBytes(new ClassPathResource("input.json").getFile().toPath()));
        JsonConfigurationParser jsonConfigurationParser = new JsonConfigurationParser();
        Object person = jsonConfigurationParser.read(JsonPath.parse(json), map);
        System.out.println(person);
    }

}
