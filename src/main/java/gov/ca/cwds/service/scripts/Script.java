package gov.ca.cwds.service.scripts;

import javax.script.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.IntStream;

/**
 * Created by dmitry.rudenko on 5/9/2017.
 */
class Script {
    private String[] variables;
    private String script;
    private ScriptEngine scriptEngine;

    Script(String filePath, String... variables) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        script = new String(bytes);
        this.variables = variables;
        int dotIndex = filePath.lastIndexOf(".");
        String fileExtension = dotIndex == -1 ? "" : filePath.substring(dotIndex + 1);
        ScriptEngineManager factory = new ScriptEngineManager();
        scriptEngine = factory.getEngineByName(fileExtension);
    }

    Object eval(Object... objects) throws ScriptException {
        ScriptContext scriptContext = new SimpleScriptContext();
        IntStream.range(0, variables.length).forEach(i -> {
            scriptContext.setAttribute(variables[i], objects[i], ScriptContext.ENGINE_SCOPE);
        });
        return scriptEngine.eval(script, scriptContext);
    }
}
