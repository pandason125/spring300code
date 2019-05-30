/**
 * 
 */
package com.ylzinfo.mvcframework.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ylzinfo.mvcframework.annotation.ModelAutowired;
import com.ylzinfo.mvcframework.annotation.ModelController;
import com.ylzinfo.mvcframework.annotation.ModelService;
import com.ylzinfo.mvcframework.annotation.ModelRequestMapping;

/**
 * @author panda
 *
 */
public class ModelDispatcherServlet extends HttpServlet {
	private static final String LOCATION = "contextConfigLocation";

	private Properties properties = new Properties();

	private List<String> classNames = new ArrayList<String>();

	private Map<String, Object> ioc = new HashMap<String, Object>();

	private Map<String, Method> handleMapping = new HashMap<String, Method>();

	public ModelDispatcherServlet() {
		super();
	}

	public void init(ServletConfig config) throws ServletException {
		// 1.加载配置文件(被扫描包路径名)
		doLoadConfig(config.getInitParameter(LOCATION));
		// 2.扫描所有类
		doScanner(properties.getProperty("scanPackage"));
		// 3.初始化类对应实例，并保持到IOC容器中
		doInstance();
		// 4.依赖注入
		doAutowired();
		// 5.构造handlerMapping
		initHandlerMapping();

	}

	private void initHandlerMapping() {
		// TODO Auto-generated method stub
		if (ioc.isEmpty()) {
			return;
		}

		for (Entry<String, Object> entry : ioc.entrySet()) {
			Class<?> clazz = entry.getValue().getClass();
			if (!clazz.isAnnotationPresent(ModelController.class)) {
				continue;
			}
			String baseUrl = "";
			if (clazz.isAnnotationPresent(ModelRequestMapping.class)) {
				ModelRequestMapping requstmapping = clazz.getAnnotation(ModelRequestMapping.class);
				baseUrl = requstmapping.value();

			}
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (!method.isAnnotationPresent(ModelRequestMapping.class)) {
					continue;
				}
				ModelRequestMapping requstmapping = method.getAnnotation(ModelRequestMapping.class);
				String url = ("/" + baseUrl + "/" + requstmapping.value()).replaceAll("/+", "/");

				handleMapping.put(url, method);
				
				System.out.println("mapped"+url+","+method);

			}
		}

	}

	private void doAutowired() {
		// TODO Auto-generated method stub
		if (ioc.isEmpty()) {
			return;
		}
		for (Entry<String, Object> entry : ioc.entrySet()) {
			Field[] fields = entry.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {
				if (!field.isAnnotationPresent(ModelAutowired.class)) {
					continue;
				}
				ModelAutowired autowired = field.getAnnotation(ModelAutowired.class);
				String beanName = autowired.value().trim();
				if ("".equals(beanName)) {
					beanName = field.getType().getName();
				}
				field.setAccessible(true);// 设置私有属性的访问权限

				try {
					field.set(entry.getValue(), ioc.get(beanName));
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private String lowerFirstCase(String str) {
		char[] chars = str.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}

	private void doInstance() {
		// TODO Auto-generated method stub
		if (classNames.size() == 0) {
			return;
		}
		try {
			for (String className : classNames) {
				Class<?> clazz = Class.forName(className);
				if (clazz.isAnnotationPresent(ModelController.class)) {
					String beanName = lowerFirstCase(clazz.getSimpleName());
					ioc.put(beanName, clazz.newInstance());

				} else if (clazz.isAnnotationPresent(ModelService.class)) {
					ModelService service = clazz.getAnnotation(ModelService.class);
					String beanName = service.value();
					// 如果service注解设置了名字（@Service("xxxx")）用如下方法
					if (!"".equals(beanName.trim())) {
						ioc.put(beanName, clazz.newInstance());
						continue;
					}
					// 如果没设置就实例化这个接口类
					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> i : interfaces) {
						ioc.put(i.getName(), clazz.newInstance());

					}
				} else {
					continue;

				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void doScanner(String packageName) {
		// TODO Auto-generated method stub
		URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
		File dir = new File(url.getFile());
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				doScanner(packageName +"."+file.getName());
			} else {
				classNames.add(packageName + "." + file.getName().replaceAll(".class", "").trim());
			}

		}

	}

	private void doLoadConfig(String location) {
		// TODO Auto-generated method stub
		InputStream fis = null;
		try {
			fis = this.getClass().getClassLoader().getResourceAsStream(location);

			properties.load(fis);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if( fis!=null){
				fis.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		if (this.handleMapping.isEmpty()) {
			return;
		}
		String url = req.getRequestURI();
		String contextPath = req.getContextPath();
		url = url.replace(contextPath,"").replaceAll("/+", "/");
		if (!this.handleMapping.containsKey(url)) {
			resp.getWriter().write("404 not found");
			return;
		}

		Map<String, String> params = req.getParameterMap();

		Method method = this.handleMapping.get(url);
		// 获取方法参数列表
		Class<?>[] parameterTypes = method.getParameterTypes();
		// 获取请求的参数
		Map<String, String[]> paramsMap = req.getParameterMap();
		// 保存参数值
		Object[] paramValues = new Object[parameterTypes.length];
		// 方法的参数列表
		for (int i = 0; i < parameterTypes.length; i++) {
			// 根据参数名称，做处理
			Class paramterType = parameterTypes[i];
			if (paramterType == HttpServletRequest.class) {
				// 明确参数类型，强转类型
				paramValues[i] = req;
				continue;
			}
			if (paramterType == HttpServletResponse.class) {
				paramValues[i] = resp;
				continue;

			} else if (paramterType == String.class) {
				for (Entry<String, String[]> param : paramsMap.entrySet()) {
					String value = Arrays.toString(param.getValue())
					.replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
					paramValues[i] = value;
				}

			}
		}
		try {
			String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
			method.invoke(this.ioc.get(beanName), paramValues);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			doDispatch(req, resp);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

}
