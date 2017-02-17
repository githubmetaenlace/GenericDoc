package es.maltimor.genericDoc.dao;

import java.util.Map;

public class GenericDocServiceMapperProvider {

	public String updateHTMLyTXT(Map<String,Object> params){
		//("UPDATE #{table} SET TXT=#{txt}, HTML=#{html} WHERE ID_DB=#{dbid} AND ID=#{unid}")
		String table = (String) params.get("table");
		table = table.replace("'", "");
		String sql = "UPDATE "+table+" SET TXT=#{txt}, HTML=#{html} WHERE ID_DB=#{dbid} AND ID=#{unid}";
		return sql;
	}

}
