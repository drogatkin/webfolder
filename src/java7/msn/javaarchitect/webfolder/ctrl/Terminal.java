package msn.javaarchitect.webfolder.ctrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
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
	
	private static final String PROMPT = "$ ";
	static final String SLASH = FileSystems.getDefault().getSeparator();

	String pwd;
	
	String topFolder = "";
	
	OutputStream consoleStream;
	Process currentProcess = null;
	
	Executor executor = Executors.newSingleThreadExecutor();
	Executor streamProcessor = Executors.newFixedThreadPool(2);
	
	@OnOpen
	public void connect(Session s, @PathParam("path") String path) {
		String sep = FileSystems.getDefault().getSeparator();
		//topFolder = getConfigValue(Folder.TOPFOLDER, sep);
		pwd = "/"+path;
	}

	@OnMessage
	public void processCommand(String command, Session s) {
		if (command.length() == 1) {
			//System.out.printf("Received one char %d%n", (int)command.charAt(0));
			switch ((int)command.charAt(0)) {
			case 3:
				currentProcess.destroyForcibly();
				//consoleStream = null;
				//currentProcess = null;
				try {
					s.getBasicRemote().sendText("^C");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			case 26:
				if (consoleStream != null) {
					try {
						consoleStream.close();
						consoleStream = null;
						// echo
						s.getBasicRemote().sendText("^Z");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return;
			case 10:
				if (consoleStream != null) {
					try {
						consoleStream.write('\n');
						consoleStream.flush();
						// echo
						s.getBasicRemote().sendText("\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return;
			}
		}
		if (consoleStream != null && currentProcess != null && currentProcess.isAlive()) {
			try {
				consoleStream.write(command.getBytes("UTF-8"));
				// echo
				s.getBasicRemote().sendText(command);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		String cc[] = command.split(" ");
		String out = PROMPT+command+"\n";
		if ("cd".equals(cc[0])) {
			String newpwd = SLASH; // getTopDir()
			if (cc.length == 1) {
				pwd = System.getProperty("user.home");
			} else {
				if (cc[1].startsWith(SLASH))
					newpwd = cc[1];
				else
					newpwd = pwd+SLASH+cc[1];	
			}
			try {
				if (Files.isDirectory(Paths.get(newpwd))) {
					pwd = Paths.get(newpwd).toAbsolutePath().normalize().toString();
					out += pwd+"\n";
				} else 
					throw new InvalidPathException(newpwd, "The path isn't a directory");
			} catch(InvalidPathException e) {
				out += e.getReason()+"\n";
			}
		} else if ("pwd".equals(cc[0])) {
			if (cc.length == 1) {
				out += pwd+"\n";
			}
		} else {
			try {
				assert(currentProcess == null);
				currentProcess = Runtime.getRuntime().exec(command, null, new File(pwd));
				
				executor.execute(() -> {
					try {
						streamProcessor.execute(() -> {
							try (InputStreamReader in = new InputStreamReader(currentProcess.getInputStream())) {
				                char[] buf = new char[256];
				                int rc = 0;                      
				                while ((rc  = in.read(buf, 0, buf.length-1)) > 0) {
				                	s.getBasicRemote().sendText(new String(buf, 0, rc));                       
				                }
				            } catch (Exception e) {
				                e.printStackTrace();
				            }
						});
						streamProcessor.execute(() -> {
							 try (InputStreamReader in = new InputStreamReader(currentProcess.getErrorStream())) {
					                char[] buf = new char[256];
					                int rc = 0;                      
					                while ((rc  = in.read(buf, 0, buf.length-1)) > 0) {
					                	s.getBasicRemote().sendText(new String(buf, 0, rc));                       
					                }
					            } catch (Exception e) {
					                e.printStackTrace();
					            }
						});
						assert(consoleStream == null);
						consoleStream = currentProcess.getOutputStream();
						// make global var for output stream
						currentProcess.waitFor();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						// null global output stream
						consoleStream = null;
						//currentProcess = null;
					}
				});
			} catch (IOException e) {
				//e.printStackTrace();
				out += "" + e.getMessage() + "\n";
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
