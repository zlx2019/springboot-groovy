import java.util.concurrent.TimeUnit

pattern = ~'<code>(?<tag1>[a-zA-Z0-9]{6})</code>'

def parseCode(String html){
    if (html.isBlank()){
        return null;
    }
    def matcher = pattern.matcher(html)
    if (matcher.find()){
        return matcher.group("tag1");
    }
    return null;
}