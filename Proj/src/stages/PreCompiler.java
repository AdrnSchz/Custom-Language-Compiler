package stages;

public class PreCompiler {

    public String commentRemover(String srcCode) {
        return srcCode.replaceAll("(?s)<<(.*?)>>|\\t", "");
    }
}
