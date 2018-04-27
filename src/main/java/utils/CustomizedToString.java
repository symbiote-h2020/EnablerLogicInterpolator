package utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CustomizedToString {
	static public String toString(Object o) {
		
		if (o==null) {
			return "null";
		}
		
		// TODO: What to do if this is not a POJO but something like an array or a generic type
		Class<?> claus=o.getClass();
		String className=claus.getCanonicalName();
		
		String toStringHandlerName=className+"_toString";
		
		
		Class<?> handlerclass=null;
		try {
			handlerclass=Class.forName(toStringHandlerName);
		} catch (ClassNotFoundException e) {
			return "No handler available for class "+className;
		}

		try {
			Method m=handlerclass.getMethod("toString", claus);
			Object r=m.invoke(null, o);
			String result=(String) r;
			return result;
		} catch (NoSuchMethodException e) {
			return "no suitable toString method found for "+toStringHandlerName;
		} catch (SecurityException e) {
			return "no suitable toString method found for "+toStringHandlerName;
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
}
