package cn.hn.java.summer.freebatis.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class FileUtils extends org.apache.commons.io.FileUtils{

	/**
	 * 从输入流中读取字符串
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public static String readString(InputStream input) throws IOException {
		return IOUtils.toString(input, "utf-8");
	}

}
