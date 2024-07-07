package com.zero;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
@Slf4j
class SpringGroovyApplicationTests {

    @Autowired
    private ScriptTemplate scriptTemplate;

    private final String regex = "<code>(?<tag1>[a-zA-Z0-9]{6})</code>";

    String content = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>验证码邮件信息</title>
</head>
<body>
    <h1>标题</h1>
    <p>你好xxx，我们是xxx，您的验证码:</p>
    <code>862192</code>
</body>
</html>
                """;


    @Test
    void contextLoads() throws Exception {
        Object key = scriptTemplate.registryScript("script1", new File("ParseCode.groovy"), new HashMap<>());
        System.out.println(key);
        String code = scriptTemplate.call("script1", "parseCode", content);
        System.out.println(code);
    }


    @Test
    void pressureTest(){
        Pattern pattern = Pattern.compile(regex);
        scriptTemplate.registryScript("script1", new File("ParseCode.groovy"),new HashMap<>());

        long start1 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()){
                String code = matcher.group("tag1");
            }
        }
        long end1 = System.currentTimeMillis();
        log.info("Java原生正则，耗时: {}ms", end1 - start1);


        long start2 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            String code2 = scriptTemplate.call("script1", "parseCode", content);
        }
        long end2 = System.currentTimeMillis();
        log.info("脚本执行，耗时: {}ms", end2 - start2);
    }


    @Test
    void test2(){
        scriptTemplate.registryScript("script1", new File("ParseCode.groovy"), new HashMap<>());
        String code1 = scriptTemplate.call("script1", "parseCode", content);
        System.out.println(code1);
        scriptTemplate.registryScript("script1", new File("ParseCode2.groovy"), new HashMap<>());
        String code2 = scriptTemplate.call("script1", "parseCode", content);
        System.out.println(code2);
    }
}
