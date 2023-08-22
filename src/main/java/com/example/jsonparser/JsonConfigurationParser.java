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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class JsonConfigurationParser {
    final static String SEPARATOR = ".";
    final static String ARRAY_SIZE = "count";

    public JsonConfigurationParser() {}

    public Object read(final DocumentContext documentContext, Map<String, Object> path) {
        boolean isRootArray = (path.get("class") != null && path.get(ARRAY_SIZE) != null);
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
            if(isJsonArray(o)) {
                f.set(t, readArray(documentContext, (Map<String, Object>)o, basePath));
            }

            if(isJsonObject(o)) {
                f.set(t, readObject(documentContext, (Map<String, Object>)o, basePath));
            }

            if(o instanceof String) {
                log.debug("Field : " + clazz + SEPARATOR + f.getName() + ", value : " + o);
                f.set(t, documentContext.read("$." + basePath + SEPARATOR + o));
            }
        }

        return t;
    }

    @SuppressWarnings("unchecked")
    public <T> T readObject(final DocumentContext documentContext, Map<String, Object> path, String parentBasePath)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String clazz = (String)path.get("class");
        String basePath = parentBasePath + SEPARATOR + path.get("basePath");
        T t = (T) Class.forName(clazz).getDeclaredConstructors()[0].newInstance();
        Field[] field = t.getClass().getDeclaredFields();

        for (Field f : field) {
            f.setAccessible(true);
            Object o = path.get(f.getName());
            if(o instanceof String) {
                log.debug("Append Base Path and Current Path : {}", (basePath + SEPARATOR + o));
                try {
                    f.set(t, documentContext.read(basePath + SEPARATOR + o));
                } catch (Exception e) {
                    log.error("Missing Json path : {}", basePath + SEPARATOR + o);
                }
            }
        }

        return t;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> readArray(final DocumentContext documentContext, Map<String, Object> path, String parentBasePath)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<T> list = new ArrayList<>();
        String countPath = parentBasePath + SEPARATOR + path.get(ARRAY_SIZE);
        String basePath = parentBasePath + SEPARATOR + path.get("basePath");
        String clazz = (String)path.get("class");

        log.debug("Base Path : {}, Count Path : {}", basePath, countPath);
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
                    log.debug("Field : {}, value : {}", clazz + SEPARATOR + f.getName(), pathValue + SEPARATOR + o);
                    f.set(t, documentContext.read(pathValue + SEPARATOR + o));
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

    public static void main(String[] args) throws IOException {
        Object order = parseJsonUsingSchema("orders.json","orderSchema.json");
        log.info(order.toString());

        Object person = parseJsonUsingSchema("person.json","personSchema.json");
        log.info(person.toString());
    }

    static Object parseJsonUsingSchema(final String jsonFile, final String schema) throws IOException {
        JsonConfigurationParser jsonConfigurationParser = new JsonConfigurationParser();
        return jsonConfigurationParser.read(JsonPath.parse(readFile(jsonFile)), schema(schema));
    }

    static String readFile(final String file) throws IOException {
        return new String(Files.readAllBytes(new ClassPathResource(file).getFile().toPath()));
    }

    static Map<String, Object> schema(final String file) throws IOException {
        return JsonSchemaReader.readSchema(new String(Files.readAllBytes(new ClassPathResource(file).getFile().toPath())));
    }

}
