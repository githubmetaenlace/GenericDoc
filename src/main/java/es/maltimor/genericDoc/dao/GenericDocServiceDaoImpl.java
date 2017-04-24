package es.maltimor.genericDoc.dao;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.maltimor.genericDoc.openOffice.ComponerOO2;
import es.maltimor.genericDoc.openOffice.Compressor;
import es.maltimor.genericDoc.openOffice.GetPDF2;
import es.maltimor.genericDoc.openOffice.TOpenOffice;
import es.maltimor.genericDoc.parser.BasicParser;
import es.maltimor.genericDoc.utils.FilesUtils;
import es.maltimor.genericRest.GenericServiceDao;
import es.maltimor.genericUser.User;

public class GenericDocServiceDaoImpl implements GenericDocServiceDao {
	private AttachmentServiceDao attachService;
	private GenericDocServiceMapper mapper;
	private GenericServiceDao gservice;
	private String tempPath;
	private String attachDocBase;
	private static String separator = File.separator;
	
	
	public AttachmentServiceDao getAttachService() {
		return attachService;
	}
	public void setAttachService(AttachmentServiceDao attachService) {
		this.attachService = attachService;
	}
	public GenericServiceDao getGservice() {
		return gservice;
	}
	public void setGservice(GenericServiceDao gservice) {
		this.gservice = gservice;
	}
	public String getTempPath() {
		return tempPath;
	}
	public void setTempPath(String tempPath) {
		this.tempPath = tempPath;
	}
	public String getAttachDocBase() {
		return attachDocBase;
	}
	public void setAttachDocBase(String attachDocBase) {
		this.attachDocBase = attachDocBase;
	}
	public GenericDocServiceMapper getMapper() {
		return mapper;
	}
	public void setMapper(GenericDocServiceMapper mapper) {
		this.mapper = mapper;
	}

	public Map<String, Object> newSeccion(User user, Map<String, Object> data) throws Exception {
		return null;
	}

	public void getPDF(String dbid,String unid,String name,String table) throws Exception {
		//-------------------------------------------
		// 4º Llamada a un servlet para generar el PDF, el HTML y el TXT en base al sxw, o bien, alguno de los tres.
		//-------------------------------------------
		TOpenOffice too = TOpenOffice.getOffice();
		too.inicializaOO();
		
		//descargo el adjunto en el temporal
		Attachment attach = attachService.getAttachment(dbid, unid, name);
		Attachment attach2 = attachService.getAttachment(dbid, unid, name);
		if (attach==null) throw new Exception("No existe el adjunto");
		if (attach2==null) throw new Exception("No existe el adjunto");
		
		String path = tempPath+separator+dbid+separator+unid+separator;
		//G:\PROYECTO JAVA\GENERICDOC\GenericDoc\Attachments\1\689
		String path2 = "H:"+separator+"Proyectos Java"+separator+"CARM EDUCACION"+separator+"GenericDoc"+separator+"Attachments"+separator+dbid+separator+unid+separator;
		attach.extractFile(path+name);
		attach2.extractFile(path2+name);
		
		//Código que llama al servidor de tomcat y ejecuta el servlet de generacion del PDF, HTML y TXT (ademas inserta el content y el styles)
		if (GetPDF2.ejecutar(too, 0, path, name, "", "S", "S", "S", "N")){
			int i1 = name.lastIndexOf(".");
			if (i1==-1) i1=name.length();
			String nameTXT = name.substring(0,i1)+".txt";
			String nameHTML = name.substring(0,i1)+".html";
			
			System.out.println("updateElement: outputfile="+path + nameTXT);
			byte[] buff = FilesUtils.getBytesFromFile(path + nameTXT);
			String txt = new String(buff);

			System.out.println("updateElement: outputfile="+path + nameHTML);
			buff = FilesUtils.getBytesFromFile(path + nameHTML);
			String html = new String(buff);
			System.out.println("updateElement....");
			Map<String,Object> params = new HashMap<String,Object>();
			params.put("table", table);
			params.put("dbid", dbid);
			params.put("unid", unid);
			params.put("txt", txt.substring(0,txt.length()<4000?txt.length():4000));
			params.put("html", html);
			mapper.updateHTMLyTXT(params);
		} else throw new Exception("No se ha podido generar el PDF");
		
		//Path nuevo
		if (GetPDF2.ejecutar(too, 0, path2, name, "", "S", "S", "S", "N")){
			int i1 = name.lastIndexOf(".");
			if (i1==-1) i1=name.length();
			String nameTXT = name.substring(0,i1)+".txt";
			String nameHTML = name.substring(0,i1)+".html";
			
			System.out.println("updateElement: outputfile="+path2 + nameTXT);
			byte[] buff = FilesUtils.getBytesFromFile(path2 + nameTXT);
			String txt = new String(buff);

			System.out.println("updateElement: outputfile="+path2 + nameHTML);
			buff = FilesUtils.getBytesFromFile(path2 + nameHTML);
			String html = new String(buff);
			System.out.println("updateElement....");
			Map<String,Object> params2 = new HashMap<String,Object>();
			params2.put("table", table);
			params2.put("dbid", dbid);
			params2.put("unid", unid);
			params2.put("txt", txt.substring(0,txt.length()<4000?txt.length():4000));
			params2.put("html", html);
			mapper.updateHTMLyTXT(params2);
		} else throw new Exception("No se ha podido generar el PDF");
	}

	public void actualizarModelo(String dbid, String unid, String fileName) throws Exception {
		//extraigo los documentos de las secciones que componen el modelo
		//llamo a modificar para concatenarlos
		//adjunto el resultado en el modelo
		String basePath = tempPath + separator;
		String pathModelos = attachDocBase+separator+dbid+separator+unid+separator;
		basePath=basePath.replace("\\", "/");
		pathModelos = pathModelos.replace("\\", "/");
		String listaCuerpos = "";
		
		System.out.println("Actualiza Modelo:"+dbid+" "+unid+" "+fileName+" "+basePath);
		
		List<Map<String,Object>> secciones = gservice.getAll(null, "MODELO_SECCION", "[ID_DB]=="+dbid+" AND [ID_MODELO]=="+unid, 99999, 0, "ORDEN", "ASC", "*", null);
		if (secciones==null) throw new Exception("El modelo no tiene secciones");

		System.out.println("Secciones:"+secciones);

		for(Map<String,Object> seccion:secciones){
			String unid_seccion = (String) seccion.get("ID_SECCION");
			List<String> lnames = attachService.getAllAttachmentsNames(dbid, unid_seccion);
			System.out.println("lnames:"+lnames);
			for(String name:lnames){
				//me quedo con la que es el odt
				if (name.toLowerCase().endsWith(".odt")){
					Attachment att = attachService.getAttachment(dbid, unid_seccion, name);
					if (att==null) throw new Exception("No puedo descargar el adjunto");
					String fileName_seccion = unid_seccion+name;
					att.extractFile(basePath+fileName_seccion);
					if (!listaCuerpos.equals("")) listaCuerpos+="~";
					listaCuerpos+=fileName_seccion.replace("\\", "/");
				}
			}
		}
		
		TOpenOffice too = TOpenOffice.getOffice();
		System.out.println("TOO-> " + too + " ---- " + "BASEPATH-> " + basePath + " ---- " + "LISTACUERPOS-> " + listaCuerpos + " ---- " + "FILENAME-> " + fileName);
		
		too.inicializaOO();
		
		fileName+=".odt";
		File theDir = new File(pathModelos);
		theDir.mkdir();
		if (ComponerOO2.yoCompongo(too, 1, basePath, pathModelos, listaCuerpos, fileName)){
			//me cargo todos los adjuntos del modelo e inserto este nuevo
			attachService.deleteAllAttachments(dbid, unid);
			//ahora inserto el adjunto
			byte[] buff = FilesUtils.getBytesFromFile(basePath+ separator +fileName);
			Attachment attach = new Attachment();
			attach.setBytes(buff);
			attach.setName(fileName);
			attachService.addAttachment(dbid, unid, attach);
			
		} else throw new Exception("No se ha podido generar el modelo");
	}
	
	public Map<String,Object> instanciarModelo(String dbid, String unid, String fileName, Map<String,Object> data) throws Exception {
		//creo el documento
		//extraigo el documento del modelo, lo parseo y lo devuelvo los documentos de las secciones que componen el modelo
		//adjunto el resultado en el modelo
		System.out.println("@@@@@@ Instanciar:"+dbid+" "+unid+" "+fileName+" "+data);

		//obtengo un numero de secuencia
		String unid_doc = mapper.getNextVal();
				
		//extraigo el documento del modelo, lo parseo y lo devuelvo los documentos de las secciones que componen el modelo
		//se supone que hay uno y lo tomo como valido
		if (!attachService.hasAttachments(dbid, unid)) throw new Exception("El modelo no está preparado");
		
		String basePath = tempPath+separator+dbid+separator+unid_doc+separator;
		basePath=basePath.replace("\\", "/");
		String fileNameDoc=basePath+fileName+".odt";		//TODO poner algo en funcion de parametros del modelo
		//FILENAMEDOC es la ruta y el nombre del doc a crear
		for(String name:attachService.getAllAttachmentsNames(dbid, unid)){
			Attachment att = attachService.getAttachment(dbid, unid, name);
			if (att==null) throw new Exception("No encuentro el adjunto asociado al modelo");
			att.extractFile(fileNameDoc);
			break;
		}
		
		//lo parseo TODO
		//hago una copia del original
		FilesUtils.fileCopy(fileNameDoc, basePath+fileName+"ORG.odt");
		
		//lo descomprimo
		String filePath = basePath+"temp/";
		Compressor.unzip(filePath, fileNameDoc);
		//me traigo los dos ficheros
		FilesUtils.fileCopy(filePath + "styles.xml",  basePath + "stylesORG.xml");
		FilesUtils.fileCopy(filePath + "content.xml", basePath + "contentORG.xml");
		
		//parseo los dos ficheros
		//styles.xml
		byte[] buff = FilesUtils.getBytesFromFile(basePath + "stylesORG.xml");
		if (buff!=null){
			String txt = new String(buff,"UTF-8");		//TODO ver la codificacion
			txt = BasicParser.parse(txt,data);
			FilesUtils.sendString(basePath + "content.xml", txt, "UTF-8");
		} else throw new Exception("No se ha podido componer Styles");
		
		//content.xml
		buff = FilesUtils.getBytesFromFile(basePath + "contentORG.xml");
		if (buff!=null){
			String txt = new String(buff,"UTF-8");		//TODO ver la codificacion
			txt = BasicParser.parse(txt,data);
			FilesUtils.sendString(basePath + "content.xml", txt, "UTF-8");
		} else throw new Exception("No se ha podido componer Content");
		
		//vuelco los dos ficheros
		FilesUtils.fileCopy(basePath + "styles.xml",  filePath + "styles.xml");
		FilesUtils.fileCopy(basePath + "content.xml", filePath + "content.xml");
		
		//los vuelvo a comprimir
		Compressor.zip(filePath, basePath, fileName+".odt"); 
		
		//creo el documento
		Map<String,Object> params = new HashMap<String,Object>();
		params.put("dbid",dbid);
		params.put("unid_doc",unid_doc);
		params.put("unid_modelo",unid);
		params.put("fileName",fileName);
		mapper.insertDoc(params);
		
		//lo adjunto al documento
		buff = FilesUtils.getBytesFromFile(fileNameDoc);
		Attachment attach = new Attachment();
		attach.setBytes(buff);
		attach.setName(fileNameDoc);
		attachService.addAttachment(dbid, unid_doc, attach);
		
		//calculo el html y el txt
		//getPDF(dbid,unid_doc,fileName+".odt","DOC");

		//devuelvo el documento recien insertado
		params = new HashMap<String,Object>();
		params.put("dbid",dbid);
		params.put("unid",unid_doc);
		Map<String,Object> doc = mapper.getDoc(params);

		//Map<String,Object> doc = gservice.getById(null, "DOC", "[ID_DB]=="+dbid+" AND [ID]=="+unid_doc, null);
		if (doc==null) throw new Exception("No se ha podido localizar el documento");
		return doc;
	}

}
