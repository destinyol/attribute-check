import lombok.Data;

public class TestMain {
    /**
     * 使用示例
     */
    public static void main(String[] args) {
        // 实例化链式构造调用
        @Data
        class Test {
            private String name;
            private String id;
        }
        Test test = new Test();
        test.setName("测试1");
        test.setId("");

        try {
            AttrCheckUtil.buildWith(test,Test::getId,Test::getName).check();    // 用法1  抛默认异常
        }catch (ValueRuntimeException e){
            System.out.println("拿到报错了，错误码是："+e.getValue());
        }

        try {
            AttrCheckUtil.buildWith(test,Test::getId,Test::getName).setException(ValueRuntimeException.class).check();  // 用法2  抛自定义异常
        }catch (ValueRuntimeException e){
            System.out.println("拿到报错了，错误码是："+e.getValue());
        }

        try {
            AttrCheckUtil.buildWith(test,Test::getId,Test::getName).setWrongCode(1).check();  // 用法3  抛自定义错误code
        }catch (ValueRuntimeException e){
            System.out.println("拿到报错了，错误码是："+e.getValue());
        }

        try {
            AttrCheckUtil.buildWith(test,Test::getId,Test::getName).setException(ValueRuntimeException.class).setWrongCode(1).check();  // 用法4  自定义异常+自定义错误
        }catch (ValueRuntimeException e){
            System.out.println("拿到报错了，错误码是："+e.getValue());
        }


        // 注解调用
        @Data
        class Test2 {
            @AttrNotBlank
            private String name;
            @AttrNotBlank(wrongCode = 10086)
            private String id;
            @AttrNotBlank(wrongCode = 1233211)
            private String code;
        }
        try {
            Test2 test2 = new Test2();
            test2.setName("测试2");
            test2.setId("xxxxxx");
            AttrCheckUtil.check(test2); // 用法5  用注解
        }catch (ValueRuntimeException e){
            System.out.println("拿到报错了，错误码是："+e.getValue());
        }
    }
}
