pattern = ~'(?<tag1>[0-9]{4})'

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