package es.maltimor.genericDoc.parser;

import java.io.Writer;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.carm.mydom.entity.Database;
import es.carm.mydom.entity.Document;
import es.carm.mydom.entity.DominoBean;
import es.carm.mydom.entity.DominoSession;
import es.carm.mydom.entity.ServerDao;
import es.carm.mydom.entity.View;
import es.carm.mydom.gebd.core.TDiccionario;
import es.carm.mydom.gebd.core.parser.GEBDCompiler;
import es.carm.mydom.gebd.core.parser.GEBDProgram;
import es.carm.mydom.gebd.core.parser.TokenArray;
import es.carm.mydom.gebd.core.parser.TokenSplit;
import es.carm.mydom.parser.BeanMethod;
import es.carm.mydom.parser.ParserException;
import es.carm.mydom.parser.ProgramContext;
import es.carm.mydom.parser.ViewDef;
import es.carm.mydom.security.UserInfo;
import es.carm.mydom.utils.URLComponents;

public class BasicParser {
	final static Logger log = LoggerFactory.getLogger(BasicParser.class);
	public static final String separador = "^?.#=:\"+-*/[]()<>|& ";
	
	public static String parse(String txt,Map<String,Object> data) throws Exception{
		System.out.println("=============== Basic PARSER! ======================");
		System.out.println(data);
		System.out.println(txt);
		
		//previene la perdida de la estructura xml y arregla dos casos conocidos de caracteres problematicos
		txt = textoToSTRinterna(txt);
		txt = txt.replaceAll("ì", "\"");
		txt = txt.replaceAll("î", "\"");
		System.out.println(txt);
		
		System.out.println("=====================================");
		data.putAll((Map<String,Object>) data.get("dep"));
		Map<String,String> map = MapUtils.getMap2Map(data);
		//para cada dependencia, hacer un replace en el texto (txt) donde me encuentre algo acabado en ?
		txt = transformText(txt,map);
		/*		//para cada entrada en los datos reemplazo
		Map<String,String> copy = new TreeMap<String,String>(map);
		
		for (Map.Entry<String,String> entry : copy.entrySet()) {
			//para cada entry hago la sustitucion
			if(entry.getKey().contains("?")){
				String aux = entry.getKey().replaceAll("[0-9A-Za-z¡…Õ”⁄·ÈÌÛ˙]+?\\?", entry.getValue());
				if( aux != null&& copy.get("data."+aux.toUpperCase()) != null)
				txt = txt.replace("#!Traer "+entry.getKey()+"#",copy.get("data."+aux.toUpperCase()));
			}
			else{
				//System.out.println(entry.getKey().substring(5));
				txt = txt.replace("#!Traer "+entry.getKey().substring(5)+"#",entry.getValue());
			}
		}
		*/
		System.out.println("=====================================");
		System.out.println(txt);
		
		//restaura la estructura xml original del documento
		txt = toXMLinterna(txt);
		System.out.println(txt);
		System.out.println("=============== Basic PARSER END ======================");
		
		return txt;
	}
	
	private static String transformText(String value, Map<String, String> map) {
		long initTime = System.currentTimeMillis();
		StringBuffer sb = new StringBuffer();

		int len = value.length();
		int pos = 0;
		String txt = "";
		String startTag = "#!";
		String endTag = "#";
		int startTagLen = startTag.length();
		int endTagLen = endTag.length();
		while (pos < len) {
			int i1 = value.indexOf(startTag, pos);
			// log.debug("Compiler.i1="+i1+" pos="+pos);
			if (i1 >= pos) {
				txt = value.substring(pos, i1);
				// log.debug("Compiler.txt="+txt);
				par_text(sb, txt);
				int i2 = value.indexOf(endTag, i1 + startTagLen);
				// log.debug("Compiler.i2="+i2+" i1+sl="+(i1+startTagLen));
				if (i2 >= i1 + startTagLen) {
					// obtengo un comando y lo proceso y avanzo el cursor
					int i3 = value.indexOf(startTag, i1 + startTagLen);
					// log.debug("Compiler.i3="+i3+" i1+sl="+(i1+startTagLen));
					if ((i3 < i2) && (i3 > pos)) {
						// este es otro error
						txt = value.substring(i1, i3);
						// log.debug("Compiler.txt="+txt);
						par_text(sb, txt);
						pos = i3;
					} else {
						// por fin un comando correcto. obtengo el comando
						// entero
						txt = value.substring(i1 + startTagLen, i2);
						// actualizo la posicion del puntero, antes de llamar a
						// la accion, para permitir bucles
						pos = i2 + endTagLen;
						par_accion(sb, txt, map);
						// ahora si que avanzo el comando
						// NOTA dejo en manos de par_accion el mover el puntero
						// pos = i2 + 2;
					}
				} else {
					// esto deberia ser un error, no hago nada y avanzo el
					// cursor
					pos = i1 + startTagLen;
				}
			} else {
				// se termino de buscar
				txt = value.substring(pos);
				// log.debug("Compiler.txt="+txt);
				par_text(sb, txt);
				pos = len;
			}
		}

		String res = sb.toString();

		long endTime = System.currentTimeMillis();
		System.out.println("Compiler.compile:" + ((endTime - initTime) / 1000.0) + " seg.");
		System.out.println("=============== Basic PARSER END ======================");

		return res;
	}

	private static void par_text(StringBuffer sb, String txt) {
		sb.append(txt);
	}

	private static void par_accion(StringBuffer sb, String accion, Map<String, String> map) {
		String actionName = accion;
		String actionText = "";
		int i1 = accion.indexOf(" ");
		if (i1 > 0) {
			actionName = accion.substring(0, i1).toLowerCase();
			actionText = accion.substring(i1).trim();
		} else {
			actionName = accion.toLowerCase();
		}

		// agm88x 18-1-16 en caso de innerxml no puede haber codigo xml dentro
		// de una accion
		if (accion.contains("$mayorque$") || accion.contains("$menorque$")) {
			sb.append("???" + accion + "???");
			return;
		}

		TokenSplit ts = new TokenSplit(actionText);
		TokenArray ta = ts.split(separador);
		System.out.println("ACCION:" + actionName + "|" + actionText + "|" + ta.getListaTokens());

		// de momento solo me encargo de hacer la transformacion de tokens
		// acabados en ? por su equivalente en el map
		/*
		 * sb.append("#!"+actionName+" "); String expr=""; while (!ta.isEof()){
		 * String token = ta.consumeToken(); String act = ta.getAct(); if
		 * (!act.equals("?")) sb.append(token); else { //intento ver si puedo
		 * transformarlo ta.consumeToken();//salto el ? String aux =
		 * map.get("dep."+token); if (aux!=null) sb.append(aux); else
		 * sb.append(token+"!!!"); } } sb.append("#");
		 */
		String tokenAux="";
		String indexAux="";
		String expr = "";
		while (!ta.isEof()) {
			String token = ta.consumeToken();
			String act = ta.getAct();
			
			if(act.equals("("))
				tokenAux+=token;
			else if(act.equals(")"))
				indexAux+=map.get(tokenAux+"("+token+")");
			if (!act.equals("?")){
				//Para los casos que llevan (X)
				if(token.equals("(") || token.equals(")"))
					expr += ".";
				else if(token.equals("X"))
					expr += indexAux;
				else
					expr += token;
			}
			else {
				// intento ver si puedo transformarlo
				ta.consumeToken();// salto el ?
				
				String aux="";
				if(!token.equals(")"))
					aux = map.get(token);
				
				//String aux = map.get(token);
				if (aux != null)
					expr += aux;
				else
					expr += token + "!!!";
			}
		}
		// vuelvo a procesar la expresion una vez que he traducido las
		// dependencias
		// ts = new TokenSplit(expr);
		// ta = ts.split(separador);
		System.out.println("EXPR->:" + expr);

		try {
			if (actionName.equals("traer"))
				par_traer(sb, expr, map);
			else
				par_text(sb, "???ACCION:" + actionName + " RESTO:" + actionText + "???");
		} catch (Exception e) {
			log.error(
					"Error de compilaciÛn del parser: " + sb.toString() + ":AcciÛn: " + accion + ":" + e.getMessage());
			sb.append("|" + accion + ": " + e.getMessage() + "|");
			e.printStackTrace();
		}

	}

	private static void par_traer(StringBuffer sb, String expr, Map<String, String> map) {
		// para cada entrada en los datos reemplazo
		System.out.println("*************************** "+expr+" **********************");

		//if (map.containsKey(expr))
		if (map.containsKey("data."+expr.toUpperCase()))
			sb.append(map.get("data."+expr.toUpperCase()));
		else
			sb.append(expr);
		// TODO evaluador de expresiones y semantica valor-simple-ref....
		System.out.println("************************------------------***************************");
	}
		 
	public static String textoToSTRinterna(String value) {
		String[] org = new String[8];
		String[] dst = new String[8];
		org[0] = "'";
		dst[0] = "$comillasimple$";
		org[1] = "<";
		dst[1] = "$menorque$";
		org[2] = ">";
		dst[2] = "$mayorque$";
	
		org[3] = "&apos;";
		dst[3] = "'";
		org[4] = "&lt;";
		dst[4] = "<";
		org[5] = "&gt;";
		dst[5] = ">";
	
		org[6] = "&";
		dst[6] = "$ampersan$";
		org[7] = "$ampersan$amp;";
		dst[7] = "&";
	
		for (int i = 0; i < org.length; i++) {
			value=value.replace(org[i],dst[i]);
		}
		return value;
	}
	
	public static String toXMLinterna(String texto) {
		String[] org = new String[10];
		String[] dst = new String[10];
		org[0] = "&";
		dst[0] = "&amp;";
		org[1] = "'";
		dst[1] = "&apos;";
		org[2] = "<";
		dst[2] = "&lt;";
		org[3] = ">";
		dst[3] = "&gt;";
		org[4] = "\n\r"; //cstr(chr(13)+chr(10))
		dst[4] = "<text:line-break></text:line-break>";

		org[5] = "$comillasimple$";
		dst[5] = "'";
		org[6] = "$menorque$";
		dst[6] = "<";
		org[7] = "$mayorque$";
		dst[7] = ">";
		org[8] = "$ampersan$";
		dst[8] = "&";
		org[9] = "&quot;";
		dst[9] = "\"\"";
		//org[10] = "text:span";		//bug de los span?
		//dst[10] = "text:p";
		for (int i=0; i< org.length; i++) {
			texto = texto.replace(org[i], dst[i]);
		}
		return texto;
	}	
}
