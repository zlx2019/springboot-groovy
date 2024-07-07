package com.zero;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import javax.script.*;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脚本引擎
 *
 * @author Zero.
 * @date 2024/7/6 17:35
 */
@Configuration
@Slf4j
public class ScriptTemplate implements ApplicationContextAware {

    private final String SCRIPT_TYPE = "groovy";
    private ApplicationContext applicationContext;
    private ScriptEngineManager engineManager;
    private ScriptEngine engine;
    private Map<String, CompiledScript> container;
    private boolean isPreCompile;

    @PostConstruct
    public void init(){
        registryScript("testMem",new File("Memory.groovy"), new HashMap<>());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        engineManager = new ScriptEngineManager(applicationContext.getClassLoader());
        engine = engineManager.getEngineByName(SCRIPT_TYPE);
        log.info("Script Template init success.");
        container = new ConcurrentHashMap<>(16);
        if (engine instanceof Compilable){
            isPreCompile = true;
        }
    }

    /**
     * 根据脚本标识，获取脚本实例
     * @param key   脚本标识
     */
    public CompiledScript getInstance(String key){
        return container.get(key);
    }

    /**
     * 执行缓存中的脚本的函数
     * @param key       要执行的脚本标识
     * @param funcName  要执行的脚本函数名称
     * @param args      函数参数
     * @return          函数结果
     */
    public <T> T call(String key, String funcName, Object... args){
        if (!container.containsKey(key)){
            log.error("该脚本未注册. key: {}", key);
            return null;
        }
        CompiledScript script = container.get(key);
        if (script.getEngine() instanceof Invocable invocable){
            try {
                Object value = invocable.invokeFunction(funcName, args);
                if (value == null) return null;
                return (T)value;
            } catch (ScriptException | NoSuchMethodException e) {
                log.error("执行脚本函数异常. key: {}", key, e);
                return null;
            }
        }
        log.error("该脚本不可调用. key: {}", key);
        return null;
    }


    /**
     * 执行整个脚本文件
     * @param script    脚本文件内容
     * @param params    脚本参数
     * @return          脚本返回结果
     */
    public <T> T eval(String script,Map<String, Object> params) throws ScriptException {
        SimpleBindings bindings = new SimpleBindings(params);
        Object value = engine.eval(script, bindings);
        if (value == null){
            return null;
        }
        return (T)value;
    }

    public <T> T eval(File scriptFile, Map<String, Object> params, String funcName, Object... args){
        if (!scriptFile.exists()){
            log.error("脚本文件不存在 : {}", scriptFile.getPath());
            return null;
        }
        try {
            String script = Files.readString(scriptFile.toPath());
            return eval(script, params, funcName, args);
        } catch (IOException e) {
            log.error("执行脚本异常： ", e);
        }
        return null;
    }

    /**
     * 编译脚本文件，并且调用指定函数，将脚本函数返回值作为当前函数返回值
     * @param script    脚本文件内容
     * @param params    脚本参数
     * @param funcName  函数名
     * @param args      函数参数
     * @param <T>       返回值类型
     */
    public <T> T eval(String script, Map<String, Object> params, String funcName, Object... args) {
        SimpleBindings bindings = new SimpleBindings(params);
        try {
            // 编译脚本
            engine.eval(script, bindings);
            if (engine instanceof Invocable invocable){
                // 执行目标函数
                Object value = invocable.invokeFunction(funcName, args);
                if (value == null)return null;
                return (T)value;
            }
        } catch (ScriptException e) {
            log.error("执行脚本异常，请检查脚本内容: {}.",script,e);
        } catch (NoSuchMethodException e) {
            log.error("执行脚本错误，没有 {} 函数", funcName, e);
        }
        return null;
    }

    /**
     * 注册脚本实例
     * @param key           脚本唯一标识
     * @param scriptText    脚本内容
     * @param params        脚本全局作用域参数
     */
    public Object registryScript(String key, String scriptText, Map<String, Object> params){
        try {
            // 创建脚本实例
            CompiledScript script = ((Compilable) engine).compile(scriptText);
            // 注册，并进行预处理
            return registry(key, script, params);
        } catch (ScriptException e) {
            log.error("脚本注册异常，key: {} script: {}", key, scriptText, e);
            return null;
        }
    }

    public Object registryScript(String key, Reader scriptStream, Map<String, Object> params){
        try {
            if (!scriptStream.ready()){
                log.error("脚本文件不可读. key:{}", key);
                return null;
            }
            CompiledScript script = ((Compilable) engine).compile(scriptStream);
            return registry(key, script, params);
        } catch (ScriptException | IOException e) {
            log.error("脚本注册异常，key: {}", key, e);
            return null;
        }finally {
            if (null != scriptStream){
                try {
                    scriptStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Object registryScript(String key, File scriptFile, Map<String, Object> params){
        try {
            return registryScript(key, new FileReader(scriptFile), params);
        } catch (FileNotFoundException e) {
            log.error("{} 脚本文件不存在. key: {}",key, scriptFile.getPath(),e);
            return null;
        }
    }

    /**
     * 脚本预处理，并添加到缓存容器
     * @param key       脚本唯一标识
     * @param script    创建好的脚本实例
     * @param params    脚本参数
     * @return          脚本返回的数据
     */
    private Object registry(String key, CompiledScript script, Map<String,Object> params) throws ScriptException {
        // 定义脚本上下文所需变量
        SimpleScriptContext context = new SimpleScriptContext();
        context.setBindings(new SimpleBindings(params), ScriptContext.ENGINE_SCOPE);
        // 脚本预处理
        Object eval = script.eval(context);
        // 添加缓存
        container.put(key,script);
        log.info("注册脚本实例完成，key: {}", key);
        return eval;
    }
}
