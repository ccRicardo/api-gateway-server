package org.wyh.gateway.common.utils;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.utils
 * @Author: wyh
 * @Date: 2024-01-03 15:29
 * @Description: JSON工具类
 */
public class JSONUtil {
	
	public static final String CODE = "code";
	
	public static final String STATUS = "status";
	
	public static final String DATA = "data";
	
	public static final String MESSAGE = "message";
	//ObjectMapper对象负责JSON与java对象的转换(序列化和反序列化)
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonFactory jasonFactory = mapper.getFactory();
    //静态代码块,用于类加载时对静态属性mapper进行配置
    static {
//        	序列化时候，只序列化非空字段
//        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //设置日期格式
    	mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        //对mapper反序列化和序列化特征进行配置(都是一些固定写法)
    	//当反序列化出现未定义字段时候，不抛出异常
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        //以下代码的作用是去掉dubbo泛化调用返回的class字段(目前对dubbo泛化调用还不是很理解)
        mapper.addMixIn(Object.class, ExcludeFilter.class);
        mapper.setFilterProvider(new SimpleFilterProvider()
        		.addFilter("excludeFilter",SimpleBeanPropertyFilter.serializeAllExcept("class")));
    }
    /**
     * @date: 2024-01-03 16:24
     * @description: 序列化(将java对象转换为json串)
     * @Param obj:
     * @return: java.lang.String
     */
    public static String toJSONString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("object format to json error:" + obj, e);
        }
    }
    /**
     * @date: 2024-01-03 16:29
     * @description: 序列化,与上述方法的区别是没有返回值,而是将对象序列化后,直接写入字符输出流
     * @Param out:
     * @Param value:
     * @return: void
     */
    public static void outputToWriter(Writer out, Object value) {
        try {
            mapper.writeValue(out, value);
        } catch (Exception e) {
            throw new RuntimeException("output to writer error:" + value, e);
        }
    }

    /*
     * JsonNode反转为bean的时候，bean必须有缺省的构造函数，不然json直接用clz.getConstructor时候，无法找到默认构造
     */
    //以下一系列的重载方法parse实现的都是反序列化
    public static <T> T parse(JsonNode body, Class<T> clz) {
        try {
            return mapper.readValue(body.traverse(), clz);
        } catch (Exception e) {
            throw new RuntimeException("json node parse to object [" + clz + "] error:" + body, e);
        }
    }

    public static <T> T parse(String str, Class<T> clz) {
        try {
            return mapper.readValue(str == null ? "{}" : str, clz);
        } catch (Exception e) {
            throw new RuntimeException("json parse to object [" + clz + "] error:" + str, e);
        }
    }

    public static <T> T parse(Optional<String> json, Class<T> clz) {
        return json.map((str) -> parse(str, clz)).orElse(null);
    }

    public static <T> T parse(String str, TypeReference<T> tr) {
        try {
            return mapper.readValue(str, tr);
        } catch (Exception e) {
            throw new RuntimeException("json parse to object [" + tr + "] error:" + str, e);
        }
    }

    public static <T> T parse(JsonNode body, JavaType javaType) {
        try {
            return mapper.readValue(body.traverse(), javaType);
        } catch (Exception e) {
            throw new RuntimeException("json parse to object [" + body + "] error:" + body, e);
        }
    }

    public static <T> T parse(String str, JavaType javaType) {
        try {
            return mapper.readValue(str, javaType);
        } catch (Exception e) {
            throw new RuntimeException("json parse to object [" + str + "] error:" + str, e);
        }
    }
    /**
     * @date: 2024-01-03 16:44
     * @description: 反序列化为list
     * @Param json:
     * @Param clz:
     * @return: java.util.List<T>
     */
    public static <T> List<T> parseToList(String json, Class<T> clz){
        return parse(json,getCollectionType(List.class,clz));
    }
    /**
     * @date: 2024-01-03 16:48
     * @description: 将json串转换为JsonNode对象,JsonNode是json的树模型
                     JSON树模型就是将JSON数据组织成树形结构
     * @Param json:
     * @return: com.fasterxml.jackson.databind.JsonNode
     */
    public static JsonNode tree(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("object format to json error:" + json, e);
        }
    }
    /**
     * @date: 2024-01-04 9:16
     * @description: 排除不需要序列化的属性，将其他属性序列化
     * @Param obj:
     * @Param filterFields:
     * @return: java.lang.String
     */
    @SuppressWarnings("serial")
	public static String serializeAllExcept(Object obj, String... filterFields) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            //设置序列化行为:仅序列化非空属性
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            //以下filters的作用是,基于属性名,排除不需要序列化的属性
            //setAnnotationIntrospector作用是设置注解解析器
            FilterProvider filters = new SimpleFilterProvider()
            		.addFilter(obj.getClass().getName(),
                    SimpleBeanPropertyFilter.serializeAllExcept(filterFields));
            mapper.setFilterProvider(filters)
            .setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
                @Override
                public Object findFilterId(Annotated ac) {
                    return ac.getName();
                }
            });

            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("object format to json error:" + obj, e);
        }
    }
    /**
     * @date: 2024-01-04 9:16
     * @description: 指定需要序列化的属性，将其序列化
     * @Param obj:
     * @Param filterFields:
     * @return: java.lang.String
     */
    @SuppressWarnings("serial")
	public static String filterOutAllExcept(Object obj, String... filterFields) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            //设置序列化行为:仅序列化非空属性
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            //以下filters作用是,基于属性名,指定需要序列化的属性
            //setAnnotationIntrospector作用是设置注解解析器
            FilterProvider filters = new SimpleFilterProvider()
            		.addFilter(obj.getClass().getName(),
                    SimpleBeanPropertyFilter.filterOutAllExcept(filterFields));
            mapper.setFilterProvider(filters)
            .setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
                @Override
                public Object findFilterId(Annotated ac) {
                    return ac.getName();
                }
            });

            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("object format to json error:" + obj, e);
        }
    }
    /**
     * @date: 2024-01-04 9:22
     * @description: 反序列化单个指定属性
     * @Param str:
     * @Param fieldName:
     * @return: java.lang.String
     */
    public static String parseOneField(String str, String fieldName) {
        try {
            /*JsonParser是json解析器，负责将json串解析成一个JsonToke枚举对象的序列
             *JsonToken枚举对象可以表示对象开始/结束，数组开始/结束，属性名，以及各种类型的属性值
             */
            JsonParser jsonParser = jasonFactory.createParser(str);
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                // get the current token
                //如果当前JsonToken枚举值是FIELD_NAME，则获取属性名
                String fieldname = jsonParser.getCurrentName();
                if (fieldName.equals(fieldname)) {
                    // move to next token
                    //下一个JsonToken表示的是上述属性名对应的属性值，通过getText获取相应的String对象
                    jsonParser.nextToken();
                    return jsonParser.getText();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("object format to json error:", e);
        }
        return null;
    }
    /**
     * @date: 2024-01-04 9:54
     * @description: 创建ObjectNode对象
     * @return: com.fasterxml.jackson.databind.node.ObjectNode
     */
    public static ObjectNode createObjectNode() {
        //ObjectNode是JsonNode的子类，区别是前者可修改，一般用于写，后者不可修改，一般用于读
        return mapper.createObjectNode();
    }
    /**
     * @date: 2024-01-04 10:06
     * @description: 获取集合对应的类型（用JavaType类来表示）
     * @Param collectionClass: 集合类型
     * @Param elementClasses: 集合中元素的类型
     * @return: com.fasterxml.jackson.databind.JavaType
     */
    public static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return mapper.getTypeFactory().
        		constructParametricType(collectionClass, elementClasses);
    }
    /**
     * @date: 2024-01-04 10:09
     * @description: 类型转换
     * @Param fromValue:
     * @Param toValueType:
     * @return: T
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return mapper.convertValue(fromValue, toValueType);
    }
    //这个类的作用是与静态代码块中最后两行代码结合，用来去掉dubbo泛化调用返回的class字段
    @JsonFilter("excludeFilter")
    public static class ExcludeFilter {

    }
}
