package edu.kaist.uilab.contagts.server.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for handling request at "/copy".
 * 
 * TODO(trung): remove this servlet after testing
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class TestDataServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException {
		PrintWriter writer = resp.getWriter();
		resp.setContentType("text/plain");
		writer.close();
	}
}
