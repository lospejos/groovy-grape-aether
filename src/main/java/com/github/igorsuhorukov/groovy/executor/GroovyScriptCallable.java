package com.github.igorsuhorukov.groovy.executor;

import com.github.igorsuhorukov.groovy.GroovyMain;
import com.github.igorsuhorukov.groovy.executor.settings.ScriptSettings;
import com.github.igorsuhorukov.springframework.util.Assert;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.github.igorsuhorukov.groovy.executor.SecurityManagerUtils.applyNoExitSecurityManager;

public class GroovyScriptCallable<T extends Serializable> implements Callable<T>, Serializable{

    private ScriptSettings scriptSettings;

    public GroovyScriptCallable(ScriptSettings scriptSettings) {
        Assert.notNull(scriptSettings, "scriptSettings is null");
        this.scriptSettings = scriptSettings;
    }

    @SuppressWarnings("unchecked")
    public T call() throws Exception {
        SecurityManager securityManager = System.getSecurityManager();
        try {
            scriptSettings.setSystemProperties();
            GroovyClassLoader groovyClassLoader = GroovyMain.getGroovyClassLoader();
            Class scriptClass = groovyClassLoader.parseClass(scriptSettings.getGroovySource());
            Binding context = new Binding(scriptSettings.getArgs());
            setScriptVariables(context);
            Constructor constructor = scriptClass.getConstructor(Binding.class);
            Script script = (Script) constructor.newInstance(context);
            applyNoExitSecurityManager();
            return (T) script.run();
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            System.setSecurityManager(securityManager);
        }
    }

    protected void setScriptVariables(Binding context) {
        if (scriptSettings.getVariables()!=null && !scriptSettings.getVariables().isEmpty()){
            for (Map.Entry<String, Serializable> variable: scriptSettings.getVariables().entrySet()){
                context.setProperty(variable.getKey(), variable.getValue());
            }
        }
    }
}
