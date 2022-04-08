package msn.javaarchitect.webfolder.ctrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/terminal/{path}")
public class Terminal {
	
	String pwd;
	
	String topFolder = "";
	
	Executor executor = Executors.newSingleThreadExecutor();
	Executor streamProcessor = Executors.newFixedThreadPool(3);
	
	@OnOpen
	public void connect(Session s, @PathParam("path") String path) {
		String sep = FileSystems.getDefault().getSeparator();
		//topFolder = getConfigValue(Folder.TOPFOLDER, sep);
		pwd = "/"+path;
	}

	@OnMessage
	public void processCommand(String command, Session s) {
		String cc[] = command.split(" ");
		String out = null;
		if ("cd".equals(cc[0])) {
			if (cc.length == 1) {
				pwd = System.getProperty("user.home");
				
			} else {
				pwd = cc[1];
			}
			out = pwd+"\n";
		} else if ("pwd".equals(cc[0])) {
			if (cc.length == 1) {
				out = pwd+"\n";
			}
		} else {
			try {
				out = command+"\n";
				Process p = Runtime.getRuntime().exec(command, null, new File(pwd));
				
				executor.execute(() -> {
					try {
						streamProcessor.execute(() -> {
							try {
				                String line = null;           
				                BufferedReader inErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				                while ((line = inErr.readLine()) != null) {
				                	s.getBasicRemote().sendText(line);
				                }
				            } catch (Exception e) {
				                e.printStackTrace();
				            }
						});
						streamProcessor.execute(() -> {
							 try {
					                String line = null;
					                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));                        
					                while ((line = in.readLine()) != null) {
					                	s.getBasicRemote().sendText(line);                       
					                }
					            } catch (Exception e) {
					                e.printStackTrace();
					            }
						});
						// make global var for input stream
						p.waitFor();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						// null global input stream
					}
				});
			} catch (IOException e) {
				
				out += "Error : " + e.getMessage() + "\n";
			}
		}
		if (out != null)
			try {
				s.getBasicRemote().sendText(out);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	@OnClose
	public void cancel() {
		
	}
}
