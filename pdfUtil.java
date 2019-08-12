package util;

import core.util.Base64Util;
import core.util.FileReader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.JSONObject;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import java.net.URL;


public class PdfUtil {
     //多页pdf转1张图片
	public static String pdf2multiImage(String pdfFile, String outpath) {
		try {
			URL url = new URL(pdfFile);
			InputStream is = url.openStream();
			List<BufferedImage> piclist = new ArrayList<BufferedImage>();

			PDDocument pdDocument = PDDocument.load(is);
			org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(pdDocument);
			com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdfFile);

			BufferedImage image = null;

			for (int i = 0; i < reader.getNumberOfPages(); i++) {
				image = renderer.renderImageWithDPI(i, 144); // dpi越大转换后越清晰，相对转换速度越慢
				piclist.add(image);
			}

			imageIOWrite(piclist, outpath);
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return outpath;
	}

	private static void imageIOWrite(List<BufferedImage> piclist, String outPath) {
		if (piclist == null || piclist.size() <= 0) {
			System.out.println("图片数组为空!");
			return;
		}
		try {
			int height = 0, // 总高度
					width = 0, // 总宽度
					_height = 0, // 临时的高度 , 或保存偏移高度
					__height = 0, // 临时的高度，主要保存每个高度
					picNum = piclist.size();// 图片的数量
			int[] heightArray = new int[picNum]; // 保存每个文件的高度
			BufferedImage buffer = null; // 保存图片流
			List<int[]> imgRGB = new ArrayList<int[]>(); // 保存所有的图片的RGB
			int[] _imgRGB; // 保存一张图片中的RGB数据
			for (int i = 0; i < picNum; i++) {
				buffer = piclist.get(i);
				heightArray[i] = _height = buffer.getHeight();// 图片高度
				if (i == 0) {
					width = buffer.getWidth();// 图片宽度
				}
				height += _height; // 获取总高度
				_imgRGB = new int[width * _height];// 从图片中读取RGB
				_imgRGB = buffer.getRGB(0, 0, width, _height, _imgRGB, 0, width);
				imgRGB.add(_imgRGB);
			}
			_height = 0; // 设置偏移高度为0
			// 生成新图片
			BufferedImage imageResult = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			for (int i = 0; i < picNum; i++) {
				__height = heightArray[i];
				if (i != 0)
					_height += __height; // 计算偏移高度
				imageResult.setRGB(0, _height, width, __height, imgRGB.get(i), 0, width); // 写入流中
			}
			File outFile = new File(outPath);
			ImageIO.write(imageResult, "jpg", outFile);// 写图片
			imageResult.flush();
			imgRGB = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
        
	private static Map<String,Object> uploadSysFile(String url, String uploadUrl) {
		String httpUrl = null;
		int id=0;
		File file = null;
		Map<String,Object> rmap=null;
		file = new File(url);
		PostMethod filePost = new PostMethod(uploadUrl);
		FileReader fr = new FileReader(url);
		int status = -1;
		try {
			InputStream input = new FileInputStream(file);
			String strFileName = fr.getFileName();
			String base64FileName = Base64Util.getBase64(strFileName);

			filePost.setRequestBody(input);
			filePost.setRequestHeader("FileName", base64FileName);
			filePost.setRequestHeader("Content-Type", "multipart/form-data;charset=utf-8");
			filePost.setRequestHeader("Content-Length", String.valueOf(file.length()));

			HttpClient client = new HttpClient();
			client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
			status = client.executeMethod(filePost);

			if (status == HttpStatus.SC_OK) {
				input.close();
				deleteFile(url);
				rmap=new HashMap<String,Object>();
				try {
					byte[] reSlt = null;
					reSlt = filePost.getResponseBody();
					String strReSlt = new String(reSlt,"UTF-8");
					JSONObject jo = new JSONObject(strReSlt);
					if (!jo.isNull("status") && jo.getBoolean("status")) {
						if (!jo.isNull("data")) {
							String strDate = jo.getString("data");
							JSONObject date = new JSONObject(strDate);
							httpUrl = date.getString("SourceUrl");
                            id=date.getInt("Id");
							rmap.put("id",id);
							rmap.put("url",httpUrl);
							return rmap;
						}
					} else {
						// 上传失败
						System.out.println("上传失败");
					}
				} catch (UnsupportedEncodingException u) {
					//
					u.printStackTrace();
					System.out.println(u.getMessage());
				} catch (IOException ioe) {
					//
					ioe.printStackTrace();
					System.out.println(ioe.getMessage());
				} catch (Exception e) {
					//
					e.printStackTrace();
					System.out.println(e.getMessage());
				}
			} else {
				// 上传失败
				System.out.println("上传失败");
			}
		} catch (HttpException e) {
			//发生致命的异常，可能是协议不对或者返回的内容有问题
			e.printStackTrace();
		} catch (IOException e) {
			//发生网络异常
			e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			filePost.releaseConnection();
		}
		return null;
	}
	
	private static boolean deleteFile(String sPath) {
		System.out.println("删除文件开始");
		boolean flag = false;
		File file = new File(sPath);
	    // 路径为文件且不为空则进行删除
	    if (file.isFile() && file.exists()) {
	    	System.out.println("文件删除");
	        file.delete();
	        flag = true;
	    }
	    System.out.println("删除文件结束");
	    return flag;
	}
    public static void main(String args[]) {
    	Map<String, Object> map = null;
		try {
			map = uploadSysFile(pdf2multiImage("C:\\Users\\Desktop\\aaa.pdf", "C:\\Users\\Desktop\\aaa.jpg"), Constants.getUploadUrl());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	System.out.print(map.get("id").toString());
    }
}
