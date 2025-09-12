### 3.4.3.2 Get 방식으로 회원가입하기

get방식으로 회원가입을 하면

`user/create?userId=soyun&password=1234&name=soyun&email=test@test.com%40slipp.net`으로 서버에 전달된다

이제 이걸 model.User 클래스에 사용자가 입력한 값을 파싱해 저장해야함

에러

```java
Exception in thread "Thread-0" java.lang.StringIndexOutOfBoundsException: begin 0, end -1, length 1
	at java.base/java.lang.String.checkBoundsBeginEnd(String.java:4606)
	at java.base/java.lang.String.substring(String.java:2709)
	at webserver.RequestHandler.run(RequestHandler.java:46)
```

```java
/*
password: 1234
name: soyun
/user/create?userId: test
email: test%40test.com
requestUrl: /user/create
requestParams: userId=test&password=1234&name=soyun&email=test%40test.com
*/
int index = url.indexOf('?');
String requestUrl = url.substring(0, index);
String requestParams = url.substring(index + 1);
Map<String, String> params = HttpRequestUtils.parseQueryString(requestParams);

String userId = "";
String password = "";
String name = "";
String email = "";

for (Map.Entry<String, String> entry : params.entrySet()) {
    if(entry.getKey().equals("userId"))
        userId = entry.getValue();
    if(entry.getKey().equals("password"))
        password = entry.getValue();
    if(entry.getKey().equals("name"))
        name = entry.getValue();
    if(entry.getKey().equals("email"))
        email = entry.getValue();
    }

User user = new User(userId, password, name, email);
System.out.println(user.toString());

```

냅다 이렇게 RequestHandler에 구현을 해서 당연히 “/”로 갔을때 index에 ?가 없으니 에러가 터지는 것임

따로 클래스를 구현해야할 거 같음

- 요청 보내는 거니까 controller 확인? 해야하나?
    - controller 대신 일단 `if(url.startsWith("/user/create"))` 로 작업하기
- RequestHandler run()에 구현이 가능한가?
    - `UserRequestParser` 구현

    ```java
    public class UserRequestParser {
        public static User parser(String url){
            int index = url.indexOf('?');
    
            if(index != -1){
                throw new IllegalArgumentException("Invalid URL");
            }
    
            String requestParams = url.substring(index + 1);
            Map<String, String> params = HttpRequestUtils.parseQueryString(requestParams);
    
            String userId = params.getOrDefault("userId", "");
            String name = params.getOrDefault("userName", "");
            String password = params.getOrDefault("password", "");
            String email = params.getOrDefault("email", "");
    
            return new User(userId, password, name, email);
         }
    }
    
    ```

  10:21:58.433 [DEBUG] [Thread-0] [webserver.RequestHandler] - New User Created : User [userId=ehouse16, password=1234, name=%EB%B0%95%EC%86%8C%EC%9C%A4, email=test%40test.com]
  
  <br>한글이 변환이 안되서 decode 추가

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
            String url = request.getUrl();

            // 2. 동적 요청 처리
            if(url.startsWith("/user/create")) {
                User user = UserRequestParser.parser(url);
                log.debug("New User Created : {}", user);
            }

            // 3. 동적 요청 처리
            if(url.equals("/"))
                url = "/index.html";

            try{
                File file = new File("./webapp" + url);
                byte[] bytes = Files.readAllBytes(file.toPath());

                response200Header(dos, bytes.length, url);
                responseBody(dos, bytes);
            } catch(NoSuchFileException e) {
                log.warn(e.getMessage());
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
  
    public class UserRequestParser {
        public static User parser(String url) {
            int index = url.indexOf('?');
    
            if (index == -1) {
                throw new IllegalArgumentException("Invalid URL");
            }
    
            String requestParams = url.substring(index + 1);
            Map<String, String> params = HttpRequestUtils.parseQueryString(requestParams);
    
            String userId = decode(params.getOrDefault("userId", ""));
            String name = decode(params.getOrDefault("name", ""));
            String password = decode(params.getOrDefault("password", ""));
            String email = decode(params.getOrDefault("email", ""));
    
            return new User(userId, password, name, email);
        }
    
        private static String decode(String value) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
    }
    ```

  10:26:41.109 [DEBUG] [Thread-0] [webserver.RequestHandler] - New User Created : User [userId=ehouse16, password=1234, name=박소윤, email=test@test.com]
  <br>이제 굿!


**근데 궁금한 점**  
Spring Boot 프로젝트에서는 별도로 decode 안해도 한글이 db에 잘 들어가던데 어떻게?
- Spring MVC + Servlet 덕분

동작 과정

1. Servlet 컨테이너(tomcat, jetty 등)
   1. HTTP 요청이 들어오면, Servlet 컨테이너가 URL → UTF-8 문자열로 디코딩
   2. 쿼리 파라미터, 폼 데이터 등을 자동으로 파싱하고 인코딩 변환
2. Spring MVC DispatcherServlet
   1. 디코딩된 값을 `HttpServletRequest.getParameter()`로 꺼내서 컨트롤러 메서드 파라미터에 매핑
3. JDBC/JPA
   1. Java String 그대로 DB에 저장
  
--- 

local에서는 되었는데 다시 배포하고 들어가보니 index.html을 못가져오는 현상 발생

**문제점 1.** 동적 URL 처리 후 정적 파일 처리를 막지 않음
`/user/create` 요청을 처리하는 부분을 보면, User 객체 생성 후 메서드가 종료되지 않고 계속 아래로 진행<br>
결국 `/user/create` 요청은 사용자를 생성하고, 존재하지 않는 파일을 찾으려다 실패하고 클라이언트에게는 아무런 응답을 보내지 않게 됨

**문제점 2.** 파일이 없을 때 (404 Not Found)에 대한 처리가 없음
`try-catch`문 보면 NoSuchFileException이 발생했을 때 로그만 남기고 아무 처리도 안함<br>
아무것도 처리하지 않으니 curl http://{탄력적 IP}:8080 했을 때 "Empty reply from server" 에러 발생

그래서 추가 한 코드 2개

```java
private void response404Header(DataOutputStream dos, int lengthOfBodyContent) {
    try {
        dos.writeBytes("HTTP/1.1 404 Not Found \r\n");
        dos.writeBytes("Content-Type: text/plain;charset=utf-8\r\n");
        dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
        dos.writeBytes("\r\n");
    } catch (IOException e) {
        log.error(e.getMessage());
    }
}

private void response302Header(DataOutputStream dos, String location) {
    try {
        dos.writeBytes("HTTP/1.1 302 Found \r\n");
        dos.writeBytes("Location: " + location + "\r\n");
        dos.writeBytes("\r\n");
    } catch (IOException e) {
        log.error(e.getMessage());
    }
}
```

- 회원가입 처리 후 `reutrn: /user/create` 로직 처리 후 response302()를 호출하여 `index.html`로 리다이랙트 시키고, `return;`으로 메서드를 즉시 종료해 불필요한 파일 검색을 막는다
- 404 응답을 내려주기도 추가하였다

그리고 다시 ssh 들어가서 git pull master 하고 재빌드하고 다시 nohup으로 jar 파일을 띄우니까 잘 뜬다! 휴
아마 내가 바로 `http://localhost:8080/index.html`로 들어가서 한게 아닐까,, 생각