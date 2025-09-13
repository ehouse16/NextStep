```java
public class HttpRequest {
    String method;
    String url;
    String body;
    Map<String, String> headers = new HashMap<>();

    public HttpRequest(BufferedReader reader) throws IOException{
        // 1. 요청 라인 파싱 ("GET /index.html HTTP/1.1")
        String line = reader.readLine();
        if(line == null || line.isEmpty()){
            throw new IOException("Empty Request line");
        }

        String[] header = line.split(" ");
        this.method = header[0]; // method 저장
        this.url = header[1]; // URL 저장

        if("/favicon.ico".equals(url)) {
            throw new IOException("Favicon 요청 무시하기");
        }

        // Header 파싱
        while((line = reader.readLine()) != null && !line.isEmpty()){
            String[] headerToken = line.split(": ");

            headers.put(headerToken[0], headerToken[1]);
        }

        // 3. Body 파싱
        if(headers.containsKey("Content-Length")){
            int length = Integer.parseInt(headers.get("Content-Length"));
            char[] chars = new char[length];
            int read = reader.read(chars, 0, length);
            body = new String(chars, 0, read);
        }
    }

    public String getUrl(){
        return url;
    }

    public String getMethod(){
        return method;
    }

    public Map<String,String> getHeader(){
        return headers;
    }

    public String getBody(){
        return body;
    }
}
```
1. 요청 라인 파싱
- 첫 줄은 "GET /index.html HTTP/1.1" 같은 형태
- it(" ") → [“GET”, “/index.html”, “HTTP/1.1”]
- header[0] → 요청 메서드
- header[1] → URL

2. 헤더 파싱
- 빈줄(\r\n)이 나올 때까지 헤더를 읽음
- 예: "Content-Length: 27" → headerToken[0] = "Content-Length", headerToken[1] = "27"
- 모두 headers 맵에 저장

3. 바디 파싱
- POST 요청일 때 Body가 있을 수 있음
- Content-Length를 보고 body의 길이를 알아낸 후 그만큼 읽어서 body 변수에 저장

```java
public void run() {
    log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
            connection.getPort());

    try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
        // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
        bf = new BufferedReader(new InputStreamReader(in));
        DataOutputStream dos = new DataOutputStream(out);

        // 1. 요청 파싱
        HttpRequest request = new HttpRequest(bf);

        // 2. HTTP method에 따라 분기
        if("GET".equals(request.getMethod())){
            doGet(request, dos);
        } else if("POST".equals(request.getMethod())){
            doPost(request, dos);
        } else{
            log.info(request.getMethod() + " not supported");
        }
    } catch (IOException e) {
        log.error(e.getMessage());
    }
}

private void doGet(HttpRequest request, DataOutputStream dos) throws IOException {
    String url = request.getUrl();

    if(url.equals("/"))
        url = "/index.html";

    try{
        File file = new File("./webapp" + url);
        byte[] bytes = Files.readAllBytes(file.toPath());

        response200Header(dos, bytes.length, url);
        responseBody(dos, bytes);
    } catch(NoSuchFileException e) {
        log.warn(e.getMessage());
        response404Header(dos, e.getMessage().getBytes().length);
        responseBody(dos, e.getMessage().getBytes());
    }
}

private void doPost(HttpRequest request, DataOutputStream dos) throws IOException {
    String url = request.getUrl();

    if(url.startsWith("/user/create")) {
        User user = UserRequestParser.parserFromBody(request.getBody());
        log.debug("New User Created : {}", user);

        response302Header(dos, "/index.html");
    } else{
        log.info(request.getMethod() + " not supported");
        responseBody(dos, "Not Found".getBytes());
    }
}
```
`run()` 메서드
- GET 요청이면 `doGet()` 호출
- POST 요청이면 `doPost()` 호출
- 그 외 HTTP 메서드는 일단 안된다고 로그 출력

`doGet()` 메서드 (GET 요청 처리) -> 정적 파일 제공 역할
- URL이 /이면 기본 페이지로 /index.html 반환
- ./webapp 폴더에서 해당 파일을 찾아서 읽어온다
- 있으면 200 OK 헤더 + 파일 내용 응답
- 없으면 404 Not Found 헤더 + 에러 메시지 응답

`doPost()` 메서드 (POST 요청 처리) -> 동적 요청 처리
- request.getBody()에서 회원가입 정보 파싱 -> User 객체 생성
- 로그 출력
- 302 Redirect 응답 -> /index.html로 이동
- 그 외 URL -> Not Found 메시지 반환

**고민**
- `run()` 메서드를 최대한 간결하게 하고 싶은데 `doGet()`이나 `doPost()`를 `RequestHandler`에 두는게 맞는가?

---

배포하고나서 문제 생김

회원가입 버튼을 누르면 
```text
Exception in thread "Thread-270" java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
	at util.HttpRequest.<init>(HttpRequest.java:18)
	at webserver.RequestHandler.run(RequestHandler.java:33)
```

이유가 무엇인가?
```java
String[] header = line.split(" ");
this.method = header[0]; // method 저장
this.url = header[1]; // URL 저장
```
- 헤더 라인이나 요청 라인이 예상과 달라서 split(" ") 후 길이가 2 미만이라 그럼
  - 브라우저가 /favicon.ico 요청을 보내거나
  - 빈 라인 혹은 이상한 요청이 들어오는 경우

```java
if(header.length < 2){
    log.debug("Headers 2개보다 적음");
}
this.method = header[0]; // method 저장
this.url = header[1]; // URL 저장
```
HttpRequest에 추가하니 동작 잘함!

