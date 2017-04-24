package es.maltimor.genericDoc.dao;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.naming.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.maltimor.genericDoc.utils.FilesUtils;

public class AttachmentServiceDaoImpl implements AttachmentServiceDao {
	final Logger log = LoggerFactory.getLogger(AttachmentServiceDaoImpl.class);
	private DirContext dirContext;
	private String ruta;
	
	private static String separator = File.separator;
	
	public DirContext getDirContext() {
		return dirContext;
	}
	public void setDirContext(DirContext dirContext) {
		this.dirContext = dirContext;
	}

	public String getRuta() {
		return ruta;
	}
	public void setRuta(String ruta) {
		this.ruta = ruta;
	}
	public boolean hasAttachments(String dbid, String unid) throws Exception {
		try {
			log.debug("@@@@@@@@@@@@@@@@@@@@@ AttachService.hasAttachments:"+dbid+" "+unid);
			String rutaDir = dbid+separator+unid;
			rutaDir = rutaDir.replace("\\", "/");
			NamingEnumeration<NameClassPair> list = dirContext.list(rutaDir);
			return list.hasMore();
//			Object obj = dirContext.lookup(dbid+separator+unid);
//			return true;
		} catch (NamingException e) { }

		return false;
	}

	public boolean hasAttachment(String dbid, String unid, String name) throws Exception {
		try {
			log.debug("@@@@@@@@@@@@@@@@@@@@@ AttachService.hasAttachment:"+dbid+" "+unid+" "+name);
			Object obj = dirContext.lookup(dbid+separator+unid+separator+name);
			return true;
		} catch (NamingException e) { }
		return false;
	}

	public List<String> getAllAttachmentsNames(String dbid, String unid) throws Exception {
		log.debug("@@@@@@@@@@@@@@@@@@@@@ AttachService.getAllAttachmentsNames:"+ruta+separator+dbid+" "+unid);
		System.out.println("@@@@@@@@@@@@@@@@@@@@@ AttachService.getAllAttachmentsNames:"+ruta+separator+dbid+" "+unid);
		List<String> lres = new ArrayList<String>();
		try {
			NamingEnumeration<NameClassPair> list = dirContext.list(dbid+separator+unid);
			while (list.hasMore()) {
				NameClassPair nc = (NameClassPair)list.next();
				log.debug("nc="+nc);
				
				String fileName = nc.getName();
				try{
					//Cualquier error aqui no insertara el attachment y prosigue con el siguiente
					Object obj = dirContext.lookup(dbid+separator+unid+separator+fileName);
					log.debug("--->"+obj);
					Resource res= (Resource) obj;
					lres.add(fileName);
					log.debug("--->="+fileName);
				} catch (Exception e){
				}
			}
		} catch (NamingException e) {
		}

		return lres;
	}

	public List<Attachment> getAllAttachments(String dbid, String unid) throws Exception {
		log.debug("@@@@@@@@@@@@@@@@@@@@@ AttachService.getAllAttachments:"+dbid+" "+unid);
		List<Attachment> lres = new ArrayList<Attachment>();
		try {
			NamingEnumeration<NameClassPair> list = dirContext.list(dbid+separator+unid);
			while (list.hasMore()) {
				NameClassPair nc = (NameClassPair)list.next();
				log.debug("nc="+nc);

				String fileName = nc.getName();
				try{
					//Cualquier error aqui no insertara el attachment y prosigue con el siguiente
					Object obj = dirContext.lookup(dbid+separator+unid+separator+fileName);
					log.debug("--->"+obj);
					Resource res= (Resource) obj;
					Attachment attach = new Attachment();
					attach.setName(fileName);
					attach.setBytes(this.getBytesFromStream(res));
					lres.add(attach);
					log.debug("--->="+attach.getName());
				} catch (Exception e){
				}
				
			}
		} catch (NamingException e) {
		}

		return lres;
	}

	public void deleteAllAttachments(String dbid, String unid) throws Exception {
		log.debug("@@@@@@@@@@@@@@@@@@@@@ AttachService.deleteAllAttachments:"+dbid+" "+unid);
		try{
			//TODO comprobar primero si existe el directorio, asi me ahorro una excepcion
			NamingEnumeration<NameClassPair> list = dirContext.list(dbid+separator+unid);
			while (list.hasMore()) {
				NameClassPair nc = (NameClassPair)list.next();
				try{
					log.debug("Borrando:"+nc.getName());
					dirContext.unbind(dbid+separator+unid+separator+nc.getName());
				} catch (Exception e){
					e.printStackTrace();
				}
			}
			dirContext.destroySubcontext(dbid+separator+unid);
			log.debug("OK");
		} catch (NamingException e){
			log.warn("@@@@@@@@@@@@@@@@@@@@@ AttachService.deleteAllAttachments: NO HAY ATTACHMENTS: "+dbid+" "+unid);
		}
	}

	public void deleteAttachment(String dbid, String unid, String fileName) throws Exception {
		log.debug("@@@@@@@@@@@@@@@@@@@@@ AttachService.deleteAttachmentsName:"+dbid+separator+unid+" "+fileName);
		try{
			dirContext.unbind(dbid+separator+unid+separator+fileName);
			log.debug("OK");
			NamingEnumeration<NameClassPair> list = dirContext.list(dbid+separator+unid);
			if (!list.hasMore()){
				try{
					log.debug("Borrando dir:"+dbid+separator+unid);
					dirContext.destroySubcontext(dbid+separator+unid);
					log.debug("OK");
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		} catch (NamingException e){
			e.printStackTrace();
		}
	}

	public void addAttachment(String dbid, String unid, Attachment attach) throws Exception {
		log.debug("@@@@@@@@@@@@@@@@@@@@@ AttachService.addAttachmentsName:"+dbid+" "+unid+" "+attach);
		try {
			
			Resource resource = new Resource(attach.getBytes());
			//preparo el sistema
			FilesUtils.preparaPath(ruta+dbid+separator+unid+separator, false);
			log.debug("Antes de create");
			try { 
				dirContext.createSubcontext(ruta+separator+dbid+separator+unid); 

				} catch (Exception e){}
			log.info("Ruta: "+ruta);
			File theDir = new File(ruta+separator+dbid+separator+unid);
			theDir.mkdir();
			String name = attach.getName();
			name = name.replace("\\", "/");
			int i1 = name.lastIndexOf("/");
			if (i1>0) name = name.substring(i1+1);
			
			log.debug("Antes de rebind:name="+name);
			dirContext.rebind(dbid+separator+unid+separator+name, resource);
			log.debug("OK");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Attachment getAttachment(String dbid, String unid, String fileName) throws Exception {
		log.debug("@@@@@@@@@@@@@@@@@@@@@ AttachService.getAttachment:"+dbid+" "+unid+" "+fileName);
		try{
			Object obj = dirContext.lookup(dbid+separator+unid+separator+fileName);
			log.debug("--->"+obj);
			if (obj==null) return null;
			try{
				Resource res= (Resource) obj;
				Attachment attach = new Attachment();
				attach.setName(fileName);
				attach.setBytes(this.getBytesFromStream(res));
				return attach;
			} catch (Exception e){
				System.out.println("@@@@@@@@@@@@@@@@@@@@@ AttachService.getAttachment:ERROR:"+e.getMessage());
			}
		} catch (NamingException e) {
			System.out.println("@@@@@@@@@@@@@@@@@@@@@ AttachService.getAttachment:ERROR2:"+e.getMessage());
		}
		return null;
	}
	
	private byte[] getBytesFromStream(Resource resource){
		byte[] res = null;
		try{
			InputStream in = resource.streamContent();
			res = new byte[in.available()];
			byte[] buff = new byte[16384];
			log.debug("getBytes.available="+in.available());
			int b = 1;
			int dstPos = 0;
			while (b>0){
				b = in.read(buff);
				if (b>0) {
					System.arraycopy(buff, 0, res, dstPos, b);
					dstPos+=b;
				}
			}
			in.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		return res;
	}


}
