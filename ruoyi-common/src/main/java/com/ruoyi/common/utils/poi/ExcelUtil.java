package com.ruoyi.common.utils.poi;

import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.ruoyi.common.convert.ExcelBigNumberConvert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import org.apache.poi.ss.formula.functions.T;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Excel相关处理
 *
 * @author Lion Li
 */
public class ExcelUtil {

	/**
	 * 对excel表单默认第一个索引名转换成list（EasyExcel）
	 *
	 * @param is 输入流
	 * @return 转换后集合
	 */
	public static <T> List<T> importExcel(InputStream is, Class<T> clazz) {
		return EasyExcel.read(is).head(clazz).autoCloseStream(false).sheet().doReadSync();
	}


    /**
     * 对excel表单默认第一个索引名转换成list（EasyExcel）
     *
     * @param is 输入流
     * @return 转换后集合
     */
    public static <T> ExcelResult<T> importExcel(InputStream is, Class<T> clazz, boolean isValidate, boolean skipException) {
        ExcelListener<T> listener = new ExcelListener<>(isValidate, skipException);
        EasyExcel.read(is, clazz, listener).sheet().doRead();
        return listener.getExcelResult();
    }

	/**
	 * 对list数据源将其里面的数据导入到excel表单（EasyExcel）
	 *
	 * @param list      导出数据集合
	 * @param sheetName 工作表的名称
	 * @return 结果
	 */
	public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, HttpServletResponse response) {
		try {
			String filename = encodingFilename(sheetName);
			response.reset();
			FileUtils.setAttachmentResponseHeader(response, filename);
			response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
			ServletOutputStream os = response.getOutputStream();
			EasyExcel.write(os, clazz)
				.autoCloseStream(false)
				// 自动适配
				.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
				// 大数值自动转换 防止失真
				.registerConverter(new ExcelBigNumberConvert())
				.sheet(sheetName).doWrite(list);
		} catch (IOException e) {
			throw new RuntimeException("导出Excel异常");
		}
	}

	/**
	 * 解析导出值 0=男,1=女,2=未知
	 *
	 * @param propertyValue 参数值
	 * @param converterExp  翻译注解
	 * @param separator     分隔符
	 * @return 解析后值
	 */
	public static String convertByExp(String propertyValue, String converterExp, String separator) {
		StringBuilder propertyString = new StringBuilder();
		String[] convertSource = converterExp.split(",");
		for (String item : convertSource) {
			String[] itemArray = item.split("=");
			if (StringUtils.containsAny(separator, propertyValue)) {
				for (String value : propertyValue.split(separator)) {
					if (itemArray[0].equals(value)) {
						propertyString.append(itemArray[1] + separator);
						break;
					}
				}
			} else {
				if (itemArray[0].equals(propertyValue)) {
					return itemArray[1];
				}
			}
		}
		return StringUtils.stripEnd(propertyString.toString(), separator);
	}

	/**
	 * 反向解析值 男=0,女=1,未知=2
	 *
	 * @param propertyValue 参数值
	 * @param converterExp  翻译注解
	 * @param separator     分隔符
	 * @return 解析后值
	 */
	public static String reverseByExp(String propertyValue, String converterExp, String separator) {
		StringBuilder propertyString = new StringBuilder();
		String[] convertSource = converterExp.split(",");
		for (String item : convertSource) {
			String[] itemArray = item.split("=");
			if (StringUtils.containsAny(separator, propertyValue)) {
				for (String value : propertyValue.split(separator)) {
					if (itemArray[1].equals(value)) {
						propertyString.append(itemArray[0] + separator);
						break;
					}
				}
			} else {
				if (itemArray[1].equals(propertyValue)) {
					return itemArray[0];
				}
			}
		}
		return StringUtils.stripEnd(propertyString.toString(), separator);
	}

	/**
	 * 编码文件名
	 */
	public static String encodingFilename(String filename) {
		return IdUtil.fastSimpleUUID() + "_" + filename + ".xlsx";
	}

}
