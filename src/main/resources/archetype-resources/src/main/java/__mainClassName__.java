package ${package};

import ${package}.util.*;

/**
 * アプリケーションエントリ
 * @param args
 */
public class ${mainClassName} {
	public static void main(String[] args) {
		MyBeans myBeans = new MyBeans();
		myBeans.setName("Foo");
		System.out.println("Hello " + myBeans.getName() + "!!!");
	}
}
