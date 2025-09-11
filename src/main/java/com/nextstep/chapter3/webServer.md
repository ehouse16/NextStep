### 3.4 웹 서버 실습

```java
public class WebServer {
    private static final Logger log = LoggerFactory.getLogger(WebServer.class);
    private static final int DEFAULT_PORT = 8080;

    public static void main(String args[]) throws Exception {
        int port = 0;
        if (args == null || args.length == 0) {
            port = DEFAULT_PORT;
        } else {
            port = Integer.parseInt(args[0]);
        }

        // 서버소켓을 생성한다. 웹서버는 기본적으로 8080번 포트를 사용한다.
        try (ServerSocket listenSocket = new ServerSocket(port)) {
            log.info("Web Application Server started {} port.", port);

            // 클라이언트가 연결될때까지 대기한다.
            Socket connection;
            while ((connection = listenSocket.accept()) != null) {
                RequestHandler requestHandler = new RequestHandler(connection);
                requestHandler.start();
            }
        }
    }
}
```

⭐ WebServer 클래스
- 웹 서버를 시작
- 사용자의 요청이 있을 때까지 대기 상태에 있다가 사용자 요청이 있을 경우 사용자의 요청을 `RequestHandler` 클래스에 위임하는 역할
- 사용자 요청이 발생할 때까지 대기 상태에 있도록 지원하는 역할: `ServerSocket` = 자바에 포함
- `WebServer`클래스는 `ServerSocket`에 사용자 요청이 발생하는 순간 클라이언트와 연결을 담당하는 `Socket`을 `RequestHandler` 에 전달하면서 새로운 스레드를 실행하는 방식으로 멀티스레드 프로그래밍을 지원하고 있다

```java
public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (
	        InputStream in = connection.getInputStream(); 
	        OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = "Hello World".getBytes();
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
```

⭐ RequestHandler 클래스
- Thread를 상속
  - 상속하는 이유: 한 클라이언트 요청을 처리하는 동안 다른 클라이언트가 대기하지 않도록 하려고
- 사용자의 요청에 대한 처리와 응답에 대한 처리를 담당하는 가장 중심이 되는 클래스
- `InputStream`: 클라이언트에서 서버로 요청을 보낼 때 전달되는 데이터
- `OutputStream`: 서버에서 클라이언트에 응답을 보낼 때 전달되는 데이터

---

**몰랐던 거 정리**

📍 `dos.flush();`
- `DataOutputStream`이나 다른 OutputStream에 데이터를 쓴 뒤, **버퍼에 남아있는 데이터를 강제로 전송**하는 메서드
- `write()`만 호출하면 일부 데이터가 버퍼에 남아 실제 네트워크로 전송되지 않을 수 있기에, **응답 완료 후 반드시 `flush()`**를 호출

📍 ServerSocket
- 서버에서 클라이언트 연결을 기다리는 소켓
- `accept()`을 호출하면 클라이언트가 연결될 때까지 블록(block)됨
- **연결되면 Socket 객체를 반환**

📍 Socket
- 서버-클라이언트 간 **실제 통신 채널**
- `InputStream`으로 데이터를 읽고, `OutputStream`으로 데이터를 쓴다
- 사용후 `close()` 안하면 자원 누수 발생

```java
ServerSocket server = new ServerSocket(8080);
while(true) {
Socket client = server.accept(); // 연결 대기
    new RequestHandler(client).start(); // 새로운 스레드에서 처리
}
```

📍DataOutputStream
- 바이트 단위로 **원시 데이터** 또는 **기본형 데이터**를 쉽게 전송할 수 있는 출력 스트림
- 주요 메서드:
  - `write(byte[] b), write(byte[] b, int off, int len)` : 바이트 배열 쓰기 
  - `writeInt(int v), writeUTF(String s)` : int, 문자열 등 기본형 쓰기 
  - `writeBytes(String s)` : 문자열을 바이트로 변환 후 전송 (ASCII 기준)

📍 writeBytes
- `DataOutputStream.writeBytes(String s)`
  - **문자열의 각 문자를 1바이트로 변환**하여 전송
  - 장점: 간단하게 ASCII 문자열 전송 가능
  - 단점: 한글, 특수문자 등 멀티바이트 문자 깨질 수 있음 -> `write(body)`와 `getBytes("UTF-8")` 사용 권장

📍 Header 읽기 동작 방식
- Key: Value 형식
- HTTP 요청 시, 클라이언트는 다음과 같이 전송
```
GET /index.html HTTP/1.1
Host: localhost:8080
User-Agent: Chrome/xxx
...

[Body]
```
1. InputStream -> BufferedReader 
   - 소켓에서 InputStream으로 들어오는 바이트 스트림을 받음
```java
BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
```
2. readLine()으로 한 줄씩 읽기
- `BufferedReader.readLine()`으로 한줄씩 읽음
- 빈줄(\r\n) 나오면 header 끝

📍 Body 읽기 동작 방식
1. Content-Length 확인
  - 바디는 한 줄이 아니라 **지정된 길이**만큼 읽어야함

📍 InputStream vs BufferedReader
- InputStream: 바이트 단위 읽기
  - 원시 데이터, 이미지, 파일 전송 시 유용
- BufferedReader: 문자 단위 읽기
  - HTTP 요청 헤더 처리 시 적합
  - 내부적으로 버퍼링하여 효율적

📍 OutputStream vs DataOutputStream
- OutputStream: 바이트 쓰기
- DataOutputStream: int, UTF, byte 배열 등 쉽게 전송 가능