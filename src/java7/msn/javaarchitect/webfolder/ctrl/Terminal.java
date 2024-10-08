package msn.javaarchitect.webfolder.ctrl;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.websocket.CloseReason;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.aldan3.annot.Inject;
import org.aldan3.model.Log;
import org.aldan3.util.DataConv;
import org.aldan3.util.inet.Base64Codecs;
import org.aldan3.servlet.Constant;

import com.beegman.webbee.base.BaseBlock;

@ServerEndpoint(value = "/terminal/{path}", configurator = Terminal.InjectConfigurator.class)
public class Terminal {

	private static final String PROMPT = "$ ";
	static final String SLASH = FileSystems.getDefault().getSeparator();

	String pwd;

	String topFolder = "";

	PrintWriter consoleStream;
	Process currentProcess = null;

	ExecutorService executor;
	ExecutorService streamProcessor;

	String TOP_DIRECTORY;

	@Inject(Folder.CONFIG_ATTR_NAME)
	public Properties properties;

	// @Inject("config")
	// public Properties properties1;

	@OnOpen
	public void connect(Session s, @PathParam("path") String path) {
		// consider ws(s)://user:password@host....
		try {
			ServletContext ctx = (ServletContext) s.getContainer().getClass()
					.getMethod("getAssociatedContext", Class.class).invoke(s.getContainer(), ServletContext.class);
			// System.out.printf("Context: %s%n", ctx);
			inject(this, ctx);
			TOP_DIRECTORY = properties.getProperty(Folder.TOPFOLDER);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String sep = FileSystems.getDefault().getSeparator();
		if (TOP_DIRECTORY == null) {
			try {
				// System.err.printf("Top directory wasn't set\n");
				s.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "The top directory wasn't set"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		try {
			Folder.RequestTransalated rt = Folder.translateReq(TOP_DIRECTORY, path);
			//System.out.printf("trans: %s%n",  rt);
			Path reqPath = rt.reqPath.isEmpty()? rt.transPath : Paths.get(rt.reqPath);
			
			if (Files.isRegularFile(reqPath))
				pwd = reqPath.getParent().toString();
			else
				pwd = reqPath.toString(); //TOP_DIRECTORY + path;
		} catch (Exception e) {
			pwd = TOP_DIRECTORY;
			e.printStackTrace();
		}
		// System.out.printf("Connected : %s%n", pwd);
		streamProcessor = Executors.newFixedThreadPool(2, (Runnable r) -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
		executor = Executors.newSingleThreadExecutor((Runnable r) -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
	}

	@OnMessage
	public void processCommand(String command, Session s) {
		if (command.length() == 1) {
			// System.out.printf("Received one char %d%n", (int)command.charAt(0));
			switch ((int) command.charAt(0)) {
			case 3:
				currentProcess.destroyForcibly();
				// consoleStream = null;
				// currentProcess = null;
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
						consoleStream.println();
						// consoleStream.flush();
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
				consoleStream.print(command);
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
		//command = command.trim();
		command = rtrim(ltrim(command));
		//System.err.println(">"+command);
		int bi = command.indexOf(" ");
		String cmd = bi < 0 ? command : command.substring(0, bi);
		String args = bi < 0 ? "" : command.substring(bi + 1).trim();
		String out = PROMPT + command + "\n";
		if ("cd".equals(cmd)) {
			String newpwd = SLASH; // getTopDir()
			if (properties != null)
				System.out.printf("Top %s%n", properties.getProperty(Folder.TOPFOLDER));
			if (args.isEmpty()) {
				newpwd = System.getProperty("user.home");
			} else {
				if (args.startsWith(SLASH))
					newpwd = args;
				else
					newpwd = pwd + SLASH + args;
			}
			try {
				Path newpath = Paths.get(newpwd);
				Paths.get(TOP_DIRECTORY).relativize(newpath);
				if (Files.exists(newpath))
					if (Files.isDirectory(newpath)) {
						pwd = newpath.toAbsolutePath().normalize().toString();
						out += pwd + "\n";
					} else
						throw new InvalidPathException(newpwd, "The path isn't a directory");
				else
					throw new InvalidPathException(newpwd, "The path doesn't exist");
			} catch (InvalidPathException e) {
				out += e.getReason() + "\n";
			} catch (IllegalArgumentException e) {
				out += e.getMessage() + "\n";
			}
		} else if ("pwd".equals(cmd)) {
			if (args.isEmpty()) {
				out += pwd + "\n";
			}
		} else {
			try {
				assert (currentProcess == null);
				currentProcess = Runtime.getRuntime().exec(splitCommand(command), null, new File(pwd));

				executor.execute(() -> {
					try {
						streamProcessor.execute(() -> {
							try (InputStreamReader in = new InputStreamReader(currentProcess.getInputStream(),
									"UTF-8")) {
								char[] buf = new char[256];
								int rc = 0;
								while ((rc = in.read(buf, 0, buf.length - 1)) > 0) {
									// System.out.printf("Sending %s\n", new String(buf, 0, rc));
									s.getBasicRemote().sendText(new String(buf, 0, rc));
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
						streamProcessor.execute(() -> {
							try (InputStreamReader in = new InputStreamReader(currentProcess.getErrorStream(),
									"UTF-8")) {
								char[] buf = new char[256];
								int rc = 0;
								while ((rc = in.read(buf, 0, buf.length - 1)) > 0) {
									s.getBasicRemote().sendText(new String(buf, 0, rc));
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
						assert (consoleStream == null);
						if (currentProcess.getOutputStream() != null)
							consoleStream = new PrintWriter(
									new OutputStreamWriter(currentProcess.getOutputStream(), "UTF-8"), true);
						if (consoleStream == null && System.console() != null)
							consoleStream = System.console().writer();
						// make global var for output stream
						int exitCode = currentProcess.waitFor();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						// null global output stream
						consoleStream = null;
						// currentProcess = null;
					}
				});
			} catch (IllegalArgumentException iae) {
				out += "" + iae.getMessage() + "\n";
			} catch (IOException e) {
				// e.printStackTrace();
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
		if (currentProcess != null && currentProcess.isAlive()) { // it can be not required for long running process and
			// a possibility to reconnect to it can be added
			currentProcess.destroyForcibly();
		}
		if (executor != null)
			executor.shutdown();
		if (streamProcessor != null)
			streamProcessor.shutdown();
	}

	enum States {
		startQuote, endQuote, blank, argument, quotedArgument, escape, quotedEscape
	}

	public String[] splitCommand(String command) {
		ArrayList<String> result = new ArrayList<>();
		States st = States.blank;
		char[] ca = command.toCharArray();
		int as = 0;
		String accum = null;
		char quoteChar = 0;
		for (int i = 0, n = ca.length; i < n; ++i) {
			// char ch
			switch (ca[i]) {
			case '\'':
			case '"':
				switch (st) {
				case blank:
					st = States.startQuote;
					quoteChar = ca[i];
					break;
				case quotedArgument:
					if (ca[i] == quoteChar) {
						st = States.endQuote;
						if (accum == null)
							result.add(new String(ca, as, i - as));
						else {
							result.add(accum + new String(ca, as, i - as));
							accum = null;
						}
						quoteChar = 0;
					}
					break;
				case escape:
					st = States.argument;
					as = i;
					break;
				case quotedEscape:
					st = States.quotedArgument;
					as = i;
					break;
				default:
					throw new IllegalArgumentException("Quote found in an argument at " + i);
				}
				break;
			case ' ':
				switch (st) {
				case blank:
				case quotedArgument:
					// ignore blanks
					break;
				case endQuote:
					st = States.blank;
					break;
				case startQuote:
					st = States.quotedArgument;
					as = i;
					break;
				case argument:
					st = States.blank;
					if (accum == null)
						result.add(new String(ca, as, i - as));
					else {
						result.add(accum + new String(ca, as, i - as));
						accum = null;
					}
					break;
				case escape:
					st = States.argument;
					as = i;
					break;
				case quotedEscape:
					st = States.quotedArgument;
					as = i-1;
					break;
				default:
					// throw new IllegalArgumentException("Blank found in an argument at "+i);
				}
				break;
			case '\\':
				switch (st) {
				case blank:
				case argument:
					st = States.escape;
					if (as < i)
						if (accum == null) {
							accum = new String(ca, as, i - as);
						} else {
							accum += new String(ca, as, i - as);
						}
					break;
				case quotedArgument:
					st = States.quotedEscape;
					if (as < i)
						if (accum == null) {
							accum = new String(ca, as, i - as);
						} else {
							accum += new String(ca, as, i - as);
						}
					break;
				case startQuote:
					st = States.quotedEscape;
					break;
				default:
					throw new IllegalArgumentException("An escape found in  at " + i);
				}
				break;
			default:
				switch (st) {
				case blank:
					st = States.argument;
					as = i;
					break;
				case argument:
				case quotedArgument:
					break;
				case startQuote:
					st = States.quotedArgument;
					as = i;
					break;
				case escape:
					st = States.argument;
					as = i;
					break;
				case quotedEscape:
					st = States.quotedArgument;
					as = i;
					break;
				default:

				}
			}
		}
		if (st == States.argument) {
			if (accum == null)
				result.add(new String(ca, as, ca.length - as));
			else {
				result.add(accum + new String(ca, as, ca.length - as));
			}
		}
		return result.toArray(new String[0]);
	}

	public static class InjectConfigurator extends ServerEndpointConfig.Configurator {

		@Override
		public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
			return inject(super.getEndpointInstance(endpointClass), null);
		}

		@Override
		public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
			// HttpSession httpSession = (HttpSession) request.getHttpSession();
			// getServletContext()
			if (DataConv.javaVersion() > 10)
				super.modifyHandshake(sec, request, response);
			String auth = null;
			try {
				// ((HttpServletRequest)request.getClass().getMethod("getHttpRequest").invoke(request)).getServletContext();
				auth = request.getHeaders().get("Authorization").get(0);
			} catch (Exception e) {

			}
			if (auth == null)
				auth = request.getParameterMap().get("Authorization").get(0); // Ok if exception
			// readExtConfig(req.getServletContext());

			if (auth != null && !auth.trim().isEmpty()) {
				auth = Base64Codecs.base64Decode(auth.substring(auth.indexOf(' ') + 1), Base64Codecs.UTF_8);
				int i = auth.indexOf(':');
				String u = auth.substring(0, i);
				String p = auth.substring(i + 1);
				/// System.err.println("us " + Console.USER + ",p " + Console.PASSWORD + " uc "
				/// + u + ", pc " + p);
				if (u.equals(Console.USER) && p.equals(Console.PASSWORD))
					return;
			}
			response.getHeaders().put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, new ArrayList<String>());
			// throw new RuntimeException();
		}
	}

	public static <T> T inject(T obj, ServletContext context) {
		if (obj == null) {
			return null;
		}
		if (context != null)
			for (Field fl : obj.getClass().getDeclaredFields()) { // use cl.getFields() for public with inheritance
				if (fl.getAnnotation(Inject.class) != null) {
					try {
						Class<?> type = fl.getType();
						if (type == Properties.class) {
							switch (fl.getAnnotation(Inject.class).value()) {
							case "config":
								fl.set(obj, context.getAttribute(Constant.ALDAN3_CONFIG));
								break;
							default:
								fl.set(obj, context.getAttribute(fl.getAnnotation(Inject.class).value()));
							}
							// System.out.printf("injecting baseblock%n", "");
						}
					} catch (Exception e) {
						Log.l.error("Exception in an injection for " + fl, e);
					}
				}
			}
		return obj;
	}
	
	public static String ltrim(String str) {
		//System.err.printf("%04x", (int)str.charAt(0));
		int i = 0;
		while (i < str.length() && (Character.isWhitespace(str.charAt(i)) || str.charAt(i) == 0xA0)) {
			i++;
		}
		return str.substring(i);
	}
	
	public static String rtrim(String str) {
		int i = str.length()-1;
		while (i >= 0 && (Character.isWhitespace(str.charAt(i)) || str.charAt(i) == 0xA0)) {
		    i--;
		}
		return str.substring(0,i+1);
	}

}
