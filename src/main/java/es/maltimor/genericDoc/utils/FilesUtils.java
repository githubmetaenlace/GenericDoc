package es.maltimor.genericDoc.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class FilesUtils {
	public static boolean preparaPath(String fich,boolean esFichero) {
		if ((fich==null)||(fich.equals(""))) return false;
		File fichero = new File(fich);
		if (!fichero.exists()) {
			if (esFichero) return fichero.getParentFile().mkdirs();
			else return fichero.mkdirs();
		}
		return true;
	}
	
	public static byte[] getBytesFromFile(String file) throws Exception {
		byte[] res = null;
		try{
			File f = new File(file);
			FileInputStream fin = new FileInputStream(f);
			res = new byte[fin.available()];
			int b=fin.read(res);
			if (b!=res.length){
				return null;
			}
			fin.close();
			return res;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static void sendListString(String file,List<String> arr,String charset) throws Exception {
		FileOutputStream fout = new FileOutputStream(file);
		for(String cad:arr){
			fout.write(cad.getBytes(charset));
		}
		fout.close();
	}
	
	public static void sendString(String file,String txt,String charset) throws Exception {
		FileOutputStream fout = new FileOutputStream(file);
		fout.write(txt.getBytes(charset));
		fout.close();
	}

	public static boolean fileCopy(String sourceFile, String destinationFile) {
		boolean resultado = true;
		try {
			File inFile = new File(sourceFile);
			File outFile = new File(destinationFile);
			FileInputStream in = new FileInputStream(inFile);
			FileOutputStream out = new FileOutputStream(outFile);
			BufferedInputStream bis = new BufferedInputStream(in);
			BufferedOutputStream bos = new BufferedOutputStream(out);
			byte[] buffer = new byte[32*1024];
			int leido = 0;
			while( (leido=bis.read(buffer) ) != -1) {
				bos.write(buffer,0,leido);
			}
			
			bis.close();
			bos.close();
			in.close();
			out.close();
		} catch(IOException e) {
			System.err.println("Hubo un error de entrada/salida!!! " + e);
			resultado = false;
		}
		return resultado;
	}
	
	/**
	 * M�todo que copia completamente una carpeta usando recursividad 
	 * @param dirOrigen Fichero o carpeta que se desea copiar 
	 * @param dirDestino Carpeta destino 
	 */
	public static void xcopy(String sOrigen, String sDestino) throws Exception { 
		try {
			File dirOrigen = new File(sOrigen);
			File dirDestino = new File(sDestino);
			if (dirOrigen.isDirectory()) { 
				if (!dirDestino.exists())
					dirDestino.mkdir(); 
	 
				String[] hijos = dirOrigen.list(); 
				for (int i=0; i < hijos.length; i++) { 
					xcopy(new File(dirOrigen, hijos[i]).getPath(), new File(dirDestino, hijos[i]).getPath()); 
				}
			} else { 
				fileCopy(dirOrigen.getPath(), dirDestino.getPath()); 
			}
		} catch (Exception e) {
			throw e;
		}
	}
}
