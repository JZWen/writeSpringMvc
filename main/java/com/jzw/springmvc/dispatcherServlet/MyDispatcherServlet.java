package com.jzw.springmvc.dispatcherServlet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jzw.common.ClassUtil;
import com.jzw.controller.MyController;
import com.jzw.controller.MyRequestMapping;

public class MyDispatcherServlet extends HttpServlet {
	
	/*
	 * 解释一下 这三个map集合
	 * springmvcBean 这是存放 扫描到有注解的类    key 小写类名（String）  value 该类的实例对象（Object）
	 * urlBean       这是存放 扫描到注解上类和方法对应的地址  key 地址 url （String） vlaue 该类的实例对象
	 * urlMethod     这是存放扫描后地址的，这个地址是全地址 包括了类上的和方法上的 key 全地址(String) value方法名 (String)
	 */
	
	private ConcurrentHashMap<String, Object> springmvcBean = new ConcurrentHashMap<String, Object>();
	private ConcurrentHashMap<String , Object> urlBean = new ConcurrentHashMap<String, Object>();
	private ConcurrentHashMap<String, String> urlMethod = new ConcurrentHashMap<String, String>();
	private Object method1;
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException{
		//获取controller包下的类 然后再去判断这个包下面有没有对应的注解
		List<Class<?>> classes = ClassUtil.getClasses("com.jzw.controller");
		//再将这个classes判断一下有没有注解 有的话那就存在springmvcBean map集合中
		try {
			findClassAnnotation(classes);
			handerMapping();
			System.out.println("初始化完成");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void handerMapping () {
		//遍历整个springmvcBean  map集合
		for(Map.Entry<String, Object> springBean : springmvcBean.entrySet()) {
			/*
			 * 得到这个map集合之后，需要去看看这个对象上有没有那个MyRequestMapping注解  有的话加上这个注解
			 */
			//首先得到对象
			Object object = springBean.getValue();
			//根据对象去获取Class对象
			Class<?> classInfo =  object.getClass();
			//然后想上面一样 根据class对象去判断有没有注解
			MyRequestMapping mapping = classInfo.getDeclaredAnnotation(MyRequestMapping.class);
			//得到这个注解之后再去看看有没有呗
			String rootUrl = "/springmvc";  //这里加上项目名
			if(mapping !=null) {
				//如果不为null的话那就将他在存在 urlBean集合里面去 
				rootUrl = rootUrl + mapping.value();//这个就是类上面的那个MyRequestMapping 注解的value值 就是第一个value
				//System.out.println("输出了"+mapping.value());
				//然后将这个url存入urlbean里面去
			}
			//通过反射  用这个classInfo  可以得到他的所有的方法 在判断方法是不是加了注解
			Method[] methods =  classInfo.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++) {
				MyRequestMapping myRequestMapping = methods[i].getDeclaredAnnotation(MyRequestMapping.class);
				if(myRequestMapping!=null) {
					String url = rootUrl + myRequestMapping.value();  //两个url拼接起来.
					
					urlBean.put(url, object);
					urlMethod.put(url, methods[i].getName());
					System.out.println("存在urlMethod的url " + url);
					System.out.println("存在urlMethod的method   " + methods[i].getName());
				}
			}
		}
		/**
		 * 整个带有注解的类 循环之后 就会得到类上面带有MyRequestMapping 的url
		 */
		
	}
	
	private void findClassAnnotation(List<Class<?>> classes) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		
		for (Class<?> classInfo : classes) {
			MyController  myController = classInfo.getDeclaredAnnotation(MyController.class);
			if(myController != null) {
				//小写的类名
				String beanId = ClassUtil.toLowerCaseFirstOne(classInfo.getSimpleName());
				//获取一个该类的实力对象
				Object object = ClassUtil.newInstance(classInfo);
				springmvcBean.put(classInfo.getName(), object);
			}
		}
		System.out.println("解析完所有的类");
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		//获取请求的url
		String url = req.getRequestURI();
		System.out.println("获取地址url    "+url);
		//然后再去urlBean里面看看有没有这个对应的value
		Object object = urlBean.get(url);
		if(object == null) {
			resp.getWriter().println("no page");
			return ;
		}
		else {
			System.out.println("object!=null");
			//然后就是说可以找到这个对应的对象 ，然后我们就要去找相应的方法并且调用这个方法。
			String method = urlMethod.get(url); // 获取方法名
			//首先通过反射机制获取那个object对应的类   在用类通过反射机制获取Method
			Class<?> classInfo = object.getClass();
			try {
				Method method1 = classInfo.getMethod(method);
				Object result = method1.invoke(object);
				//然后再去对这个进行解析。
				//进行页面转发,这里是这样的 为了简易，我没有去做前置 和 后置的配置 就是直接来。
				String page = "/"+(String)result+".jsp";
				System.out.println(page);
				req.getRequestDispatcher(page).forward(req,resp);
				return ;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}
	
	
}
