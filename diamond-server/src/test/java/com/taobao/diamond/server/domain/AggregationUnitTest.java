package com.taobao.diamond.server.domain;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.taobao.diamond.domain.ConfigInfo;

public class AggregationUnitTest {
	
	public static void assertListEquals(List left,List right){
		for(int i=0;i<left.size();i++){
			for(int j=0;j<right.size();){
				if(right.get(j).equals(left.get(i))){
					right.remove(j);
				}else{
					j++;
				}
			}
		}
		assertEquals(right.size(),0);
		
	}

	@Test
	public void testGenerateContent() {
		
		int n = 3;
		Aggregation aggregation = new Aggregation();
		String group = "test.group";
		List<ConfigInfo> items1 = new ArrayList<ConfigInfo>();
		for(int i=0;i<n;i++){
			ConfigInfo configInfo = new ConfigInfo();
			configInfo.setDataId("test.dataid."+i);
			configInfo.setGroup(group);
			aggregation.addItem(configInfo);
			items1.add(configInfo);
		}
		String content = aggregation.generateContent();	
		
		List<ConfigInfo> items2 = aggregation.parse(content);		
		assertListEquals(items2,items1);
		
	}

	/*@Test
	public void testParseAsMap() {
		int n = 3;
		Aggregation aggregation = new Aggregation();
		String group = "test.group";
		List<ConfigInfo> items1 = new ArrayList<ConfigInfo>();
		for(int i=0;i<n;i++){
			ConfigInfo configInfo = new ConfigInfo();
			configInfo.setDataId("test.dataid."+i);
			configInfo.setGroup(group);
			aggregation.addItem(configInfo);
			items1.add(configInfo);
		}
		String content = aggregation.generateContent();	
		
		Map<String,ConfigInfo> map = aggregation.parseAsMap(content);		
		System.out.println(map);
		
	}*/

}
