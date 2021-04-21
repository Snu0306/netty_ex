package netty_ex04;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List; 
import java.util.Map; 


public class NonBlockingServer {
	private Map<SocketChannel, List<byte[]>> keepDataTrack = new HashMap();
	private ByteBuffer buffer = ByteBuffer.allocate(2 * 1024);

	private void startEchoServer() {
		try (Selector selector = Selector.open(); // 자바 nio 컴포넌트 중 하나 selector 자신에게 등록된 채널에 변경사항이 발생했는지 검사함 또 그 채널에 대한 접근을 가능하게 해준다.
				ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {

			if ((serverSocketChannel.isOpen()) && (selector.isOpen())) {
				serverSocketChannel.configureBlocking(false); // 소켓 채널의 블로킹 모드 기본값음 true 이다. 아무것도 안하면 블로킹모드로 감
				serverSocketChannel.bind(new InetSocketAddress(8888)); // 클라이언트와 연결을 대기할 포트 지정하고 생성된 서버소켓채널 객체에 할당. 이거 다되면 서버소켓채널 객체가 지정된 포트로 클라이언트 연결을 생성할 수 있음

				serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); // 서버소켓채널 객체를 selector에 등록한다. selector가 연결요청하는 이벤트 selectionkey.op_accept임
				System.out.println("접속 대기중");

				while (true) {
					selector.select();
					Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

					while (keys.hasNext()) {
						SelectionKey key = (SelectionKey) keys.next();
						keys.remove();

						if (!key.isValid()) {
							continue;
						}
						if (key.isAcceptable()) {
							this.acceptOP(key, selector);
						} else if (key.isReadable()) {
							this.readOP(key);
						} else if (key.isWritable()) {
							this.writeOP(key);
						} else {
							System.out.println("서버 소켓을 생성하지 못했습니다.");
						}

					}

				}
			}

		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	private void acceptOP(SelectionKey key, Selector selector) throws IOException{
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel =  serverChannel.accept();
		socketChannel.configureBlocking(false);
		
		System.out.println("클라이언트 연결됨 : "+ socketChannel.getRemoteAddress());
		
		keepDataTrack.put(socketChannel, new ArrayList<byte[]>());
		socketChannel.register(selector, SelectionKey.OP_READ);
	}
	
	private void readOP(SelectionKey key) {
		try {
			SocketChannel socketChannel = (SocketChannel) key.channel();
			buffer.clear();
			int numRead = -1;
			try {
				numRead = socketChannel.read(buffer);
			}catch (IOException x) {
				System.out.println("데이터 읽기 에러~!");
			}
			
			if(numRead ==-1) {
				this.keepDataTrack.remove(socketChannel);
				System.out.println("클라이언트 연결 종료 :" + socketChannel.getRemoteAddress());
				
				socketChannel.close();
				key.cancel();
				return;
				
			}
			
			byte[] data =new byte[numRead];
			System.arraycopy(buffer.array(),0,data,0,numRead);
			System.out.println(new String(data,"UTF-8"+ " from "+socketChannel.getRemoteAddress()));
			
			doEchoJob(key,data);
			
			
			
		}catch (Exception e) {
			System.out.println(e);
		}
	}
	
	private void writeOP(SelectionKey key) throws IOException{
		SocketChannel socketChannel = (SocketChannel) key.channel();
		List<byte[]> channelData = keepDataTrack.get(socketChannel);
		Iterator<byte[]> its = channelData.iterator();
		
		while(its.hasNext()) {
			byte[] it = its.next();
			its.remove();
			socketChannel.write(ByteBuffer.wrap(it));
		}
		
		key.interestOps(SelectionKey.OP_READ);
	}
	
	private void doEchoJob(SelectionKey key, byte[] data) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		List<byte[]> channelData = keepDataTrack.get(socketChannel);
		channelData.add(data);
		
		key.interestOps(SelectionKey.OP_WRITE);
	}
	
	
	public static void main(String[] args) {
		NonBlockingServer main = new NonBlockingServer();
		main.startEchoServer();
	}

}
