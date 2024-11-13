import org.springframework.web.multipart.MultipartFile;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AttrCheckUtil {

    /**
     * 可自定义改成需要的自定义异常
     */
    private static final int baseWrongCode = 1;   // 默认抛出的异常code
    private static final Class<? extends RuntimeException> baseWrongClass = ValueRuntimeException.class;   // 默认抛出的异常类，可改成自定义异常类，继承RuntimeException，需要有一个int/Object参数放错误码，或者改动throwYourOwnExceptionStep构造函数
    private static void throwYourOwnException(int wrongCode){
        throwYourOwnException(baseWrongClass,wrongCode);
    }
    private static void throwYourOwnException(Class<? extends RuntimeException> wrongClass, int wrongCode){
        Constructor<? extends RuntimeException> constructor = null;
        try {
            constructor = wrongClass.getConstructor(Object.class);  // 可改动错误类构造函数
            RuntimeException runtimeException = constructor.newInstance(wrongCode);
            throw runtimeException;               // 可改动错误类构造函数
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判空逻辑函数  可根据需求自定义增改
     * @return
     */
    private static boolean isBlank(Object value, Field field) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        if (value instanceof Optional) {
            return !((Optional<?>) value).isPresent();
        }
        if (value instanceof URL || value instanceof MultipartFile) {
            return false;
        }
        if (field.getType().isArray()) {
            return Array.getLength(value) == 0;
        }
        if (value instanceof Object[]) {
            return ((Object[]) value).length == 0;
        }
        if (field.getType().isPrimitive() || field.getType().equals(Integer.class) || field.getType().equals(Double.class) || field.getType().equals(Float.class) || field.getType().equals(Long.class) || field.getType().equals(Short.class) || field.getType().equals(Character.class) || field.getType().equals(Byte.class) || field.getType().equals(Boolean.class)) {
            return false;
        }
        return false;
    }


    /**
     * （静态调用主方法）
     * 检查该类的属性中被 AttrNotBlank注解指定的字段，是否为空，为空则抛出异常，异常码在注解中定义
     *      （基础类型int,float等，会跳过不检测，因为这些类型必定有值）
     * @param object
     * @author pyf
     */
    public static void check(Object object) {
        if (object == null) {
            throwYourOwnException(baseWrongCode);
        }
        Class<?> clazz = object.getClass();
        // 递归检查类及其父类
        checkClass(clazz, object);
    }

    /**
     * 实例化构造检测工具
     *      检查targetObject类的属性中 指定了getter方法的属性是否为空，为空则抛出错误
     *
     * @param targetObject 待检测对象
     * @param fn 比如： Object::getXxxx()
     *
     * @author pyf
     */
    @SafeVarargs
    public static <T> AttrCheckUtil buildWith(T targetObject, FieldGetter<T>... fn) {
        List<String> fieldNames = new ArrayList<>();
        for (FieldGetter<T> tFieldGetter : fn) {
            String fieldName = convertToFieldName(tFieldGetter);
            fieldNames.add(fieldName);
        }
        return new AttrCheckUtil(targetObject, fieldNames);
    }

    private static void checkClass(Class<?> clazz, Object object) {
        // 遍历当前类的字段
        for (Field field : clazz.getDeclaredFields()) {
            // 检查字段是否有AttrNotBlank注解
            if (field.isAnnotationPresent(AttrNotBlank.class)) {
                AttrNotBlank annotation = field.getAnnotation(AttrNotBlank.class);
                field.setAccessible(true); // 设置字段为可访问
                Object value = null; // 获取字段的值
                try {
                    value = field.get(object);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                if (isBlank(value, field)) {
//                    System.out.println("参数缺失 -> 参数名:"+field.getName()+"   value:"+value);
                    throwYourOwnException(annotation.wrongCode());
                }
            }
        }
        // 如果当前类有父类，递归检查父类
        if (clazz.getSuperclass() != null) {
            checkClass(clazz.getSuperclass(), object);
        }
    }

    private static final Map<Class, SerializedLambda> CLASS_LAMBDA_CACHE = new ConcurrentHashMap<>();
    private final Object targetObject;
    private final List<String> fieldNames;
    private Class<? extends RuntimeException> exceptionClass = null;
    private Integer wrongCode = null;

    private AttrCheckUtil(Object targetObject, List<String> fieldNames) {
        this.targetObject = targetObject;
        this.fieldNames = fieldNames;
    }

    public AttrCheckUtil setException(Class<? extends RuntimeException> exceptionClassIn){
        this.exceptionClass = exceptionClassIn;
        return this;
    }
    public AttrCheckUtil setWrongCode(Integer code){
        this.wrongCode = code;
        return this;
    }

    /**
     * （实例化调用主方法）
     */
    public void check() {
        for (String fieldName : this.fieldNames) {
            Field field = null;
            Class<?> clazz = targetObject.getClass();
            while (clazz != null) {
                try {
                    field = clazz.getDeclaredField(fieldName);  // 查找属性字段
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();  // 如果当前类没有找到字段，检查父类
                }
            }
            field.setAccessible(true);
            Object value = null;
            try {
                value = field.get(targetObject);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (AttrCheckUtil.isBlank(value, field)) {
//                System.out.println("参数缺失 -> 参数名:"+field.getName()+"   value:"+value);
                if (this.exceptionClass==null){
                    if (wrongCode == null) throwYourOwnException(baseWrongCode);
                    else throwYourOwnException(this.wrongCode);
                }else{
                    if (wrongCode == null) throwYourOwnException(this.exceptionClass, baseWrongCode);
                    else throwYourOwnException(this.exceptionClass, this.wrongCode);
                }

            }
        }
    }

    /***
     * 转换方法引用为属性名
     * @param fn
     * @return
     */
    private static <T> String convertToFieldName(FieldGetter<T> fn) {
        SerializedLambda lambda = getSerializedLambda(fn);
        // 获取方法名
        String methodName = lambda.getImplMethodName();
        String prefix = null;
        if (methodName.startsWith("get")) {
            prefix = "get";
        } else if (methodName.startsWith("is")) {
            prefix = "is";
        }
        if (prefix == null) {
            System.out.println("无效的getter方法: " + methodName);
        }
        // 截取get/is之后的字符串并转换首字母为小写
        return toLowerCaseFirstOne(methodName.replace(prefix, ""));
    }

    /**
     * 首字母转小写
     *
     * @param s
     * @return
     */
    private static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }
    }

    private static SerializedLambda getSerializedLambda(Serializable fn) {
        SerializedLambda lambda = CLASS_LAMBDA_CACHE.get(fn.getClass());
        // 先检查缓存中是否已存在
        if (lambda == null) {
            try {
                // 提取SerializedLambda并缓存
                Method method = fn.getClass().getDeclaredMethod("writeReplace");
                method.setAccessible(Boolean.TRUE);
                lambda = (SerializedLambda) method.invoke(fn);
                CLASS_LAMBDA_CACHE.put(fn.getClass(), lambda);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return lambda;
    }
}
