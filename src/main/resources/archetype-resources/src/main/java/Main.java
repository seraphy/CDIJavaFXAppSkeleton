package ${groupId};

import ${groupId}.util.*;

/**
 * アプリケーションエントリ
 * @param args
 */
public class Main {
	public static void main(String[] args) {
		MyBeans myBeans = new MyBeans();
		myBeans.setName("Foo");
		System.out.println("Hello " + myBeans.getName() + "!!!");
	}
}
