package com.wabacus.extra;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.WabacusFacade;
import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.database.datasource.AbsDataSource;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;
import com.wabacus.util.WabacusClassLoader;

public final class WabacusServlet extends HttpServlet {
	private static final long serialVersionUID = 715456159702221404L;

	private static final Log LOG = LogFactory.getLog(WabacusServlet.class);

	private ServletContext context;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		this.context = config.getServletContext();

		closeAllDatasources();

		// URL baseURL=context.getResource("/");

		// }catch(MalformedURLException e)

		Config.homeAbsPath = context.getRealPath("/");
		Config.homeAbsPath = Tools.standardFilePath(Config.homeAbsPath + "\\");
		Config.configpath = config.getInitParameter("configpath");// 配置文件存放的物理路径
		if (Config.configpath == null || Config.configpath.trim().equals("")) {
			LOG.info("没有配置存放配置文件的根路径，将使用路径：" + Config.homeAbsPath + "做为配置文件的根路径");
			Config.configpath = Config.homeAbsPath;
		} else {
			Config.configpath = WabacusAssistant.getInstance().parseConfigPathToRealPath(Config.configpath,
					Config.homeAbsPath);
		}
		loadReportConfigFiles();
	}

	public static void loadReportConfigFiles() {
		LOG.info("正在加载配置文件wabacus.cfg.xml及所有报表配置文件...");
		ConfigLoadManager.currentDynClassLoader = new WabacusClassLoader(Thread.currentThread()
				.getContextClassLoader());
		int flag = ConfigLoadManager.loadAllReportSystemConfigs();
		if (flag == -1) {
			LOG.error("加载报表配置文件wabacus.cfg.xml失败");
		} else if (flag == 0) {
			LOG.warn("报表配置文件wabacus.cfg.xml内容为空，或没有配置报表");
		}
	}

	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		String contentType = request.getHeader("Content-type");
		if (contentType != null && contentType.startsWith("multipart/")) {
			WabacusFacade.uploadFile(request, response);
		} else {
			   response.setContentType("text/html;charset=UTF-8");
			String action = Tools.getRequestValue(request, "ACTIONTYPE", "");
			if (action.equalsIgnoreCase("updateconfig")) {
				//response.setContentType("text/plain;charset=UTF-8");
				PrintWriter out = response.getWriter();
				final long start = System.currentTimeMillis();
				out.println("配置文件更新,请稍后...");
				out.flush();
				loadReportConfigFiles();
				out.println("完成配置文件更新. 时间:" + (System.currentTimeMillis() - start));
			} else if (action.equalsIgnoreCase("invokeServerAction")) {
				String resultStr = WabacusFacade.invokeServerAction(request, response);
				if (resultStr != null && !resultStr.trim().equals("")) {
					PrintWriter out = response.getWriter();
					out.println(resultStr);
				}
			} else if (action.equalsIgnoreCase("download")) {
				WabacusFacade.downloadFile(request, response);
			} else if (action.equalsIgnoreCase("GetFilterDataList")) {// 获取某个字段的所有数据列表
				response.reset();
				response.setContentType("text/xml;charset=" + Config.encode);
				StringBuffer sbuffer = new StringBuffer("<?xml version=\"1.0\" encoding=\"" + Config.encode
						+ "\"?><items>");
				sbuffer.append(WabacusFacade.getFilterDataList(request, response));
				sbuffer.append("</items>");
				PrintWriter out = response.getWriter();
				out.println(sbuffer.toString().trim());
			} else if (action.equalsIgnoreCase("GetTypePromptDataList")) {
				response.reset();
				response.setContentType("text/xml;charset=" + Config.encode);
				StringBuffer sbuffer = new StringBuffer("<?xml version=\"1.0\" encoding=\"" + Config.encode
						+ "\"?><items>");
				sbuffer.append(WabacusFacade.getTypePromptDataList(request, response));
				sbuffer.append("</items>");
				PrintWriter out = response.getWriter();
				out.println(sbuffer.toString().trim());
			} else if (action.equalsIgnoreCase("GetSelectBoxDataList")) {
				response.reset();
				response.setContentType("text/html;charset=" + Config.encode);
				String resultStr = WabacusFacade.getSelectBoxDataList(request, response);
				PrintWriter out = response.getWriter();
				out.print(resultStr);
			} else if (action.equalsIgnoreCase(Consts.GETAUTOCOMPLETEDATA_ACTION)) {
				PrintWriter out = response.getWriter();
				out.print(WabacusFacade.getAutoCompleteColValues(request, response));
			} else if (action.equalsIgnoreCase("ShowUploadFilePage")) {// 显示文件上传界面
				PrintWriter out = response.getWriter();
				out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset="
						+ Config.encode + "\">");
				WabacusFacade.showUploadFilePage(request, out);
			} else {
				String type = Tools.getRequestValue(request, Consts.DISPLAYTYPE_PARAMNAME,
						String.valueOf(Consts.DISPLAY_ON_PAGE));
				int itype = Consts.DISPLAY_ON_PAGE;
				try {
					itype = Integer.parseInt(type);
				} catch (NumberFormatException e) {
					LOG.error("传入的显示类型" + type + "不合法", e);
				}
				if (itype == Consts.DISPLAY_ON_PRINT) {
					WabacusFacade.printComponents(request, response);
				} else if (itype == Consts.DISPLAY_ON_PLAINEXCEL) {
					response.reset();
					response.setContentType("application/vnd.ms-excel;charset=" + Config.encode);
					WabacusFacade.exportReportDataOnPlainExcel(request, response);
				} else if (itype == Consts.DISPLAY_ON_RICHEXCEL) {
					response.reset();
					response.setContentType("application/vnd.ms-excel;charset=" + Config.encode);
					WabacusFacade.exportReportDataOnWordRichexcel(request, response,
							Consts.DISPLAY_ON_RICHEXCEL);
				} else if (itype == Consts.DISPLAY_ON_WORD) {
					response.reset();
					response.setContentType("application/vnd.ms-word;charset=" + Config.encode);
					WabacusFacade
							.exportReportDataOnWordRichexcel(request, response, Consts.DISPLAY_ON_WORD);
				} else if (itype == Consts.DISPLAY_ON_PDF) {
					response.reset();
					response.setContentType("application/pdf;charset=" + Config.encode);
					WabacusFacade.exportReportDataOnPDF(request, response, Consts.DISPLAY_ON_PDF);

				} else {
					WabacusFacade.displayReport(request, response);
				}
			}
		}
	}

	public void destroy() {
		this.context = null;

		closeAllDatasources();
	}

	public void contextInitialized(ServletContextEvent event) {
		// FileUpDataImportThread.getInstance().start();
		// TimingDataImportThread.getInstance().start();
	}

	public void contextDestroyed(ServletContextEvent event) {
		closeAllDatasources();
		// FileUpDataImportThread.getInstance().stopRunning();
		// TimingDataImportThread.getInstance().stopRunning();
	}

	private void closeAllDatasources() {
		Map<String, AbsDataSource> mDataSourcesTmp = Config.getInstance().getMDataSources();
		if (mDataSourcesTmp != null) {
			for (Entry<String, AbsDataSource> entry : mDataSourcesTmp.entrySet()) {
				if (entry.getValue() != null)
					entry.getValue().closePool();
			}
		}
	}
}
