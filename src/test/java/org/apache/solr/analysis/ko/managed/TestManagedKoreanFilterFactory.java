/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.analysis.ko.managed;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.ko.managed.ManagedKoreanFilterFactory;
import org.apache.solr.util.RestTestBase;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.noggit.JSONUtil;
import org.restlet.ext.servlet.ServerServlet;

/**
 * TestManagedStopFilterFactory 참조
 * 
 */
public class TestManagedKoreanFilterFactory extends RestTestBase {
	private static File tmpSolrHome;
	private static File tmpConfDir;

	private static final String collection = "collection1";
	private static final String confDir = collection + "/conf";

	@Before
	public void before() throws Exception {
		tmpSolrHome = createTempDir().toFile();
		tmpConfDir = new File(tmpSolrHome, confDir);
		FileUtils.copyDirectory(new File(TEST_HOME()), tmpSolrHome.getAbsoluteFile());

		final SortedMap<ServletHolder, String> extraServlets = new TreeMap<>();
		final ServletHolder solrRestApi = new ServletHolder("SolrSchemaRestApi",
				ServerServlet.class);
		solrRestApi.setInitParameter("org.restlet.application",
				"org.apache.solr.rest.SolrSchemaRestApi");
		extraServlets.put(solrRestApi, "/schema/*"); // '/schema/*' matches
														// '/schema',
														// '/schema/', and
														// '/schema/whatever...'

		System.setProperty("managed.schema.mutable", "true");
		System.setProperty("enable.update.log", "false");

		createJettyAndHarness(tmpSolrHome.getAbsolutePath(), "solrconfig.xml", "schema.xml",
				"/solr", true, extraServlets);
	}

	@After
	private void after() throws Exception {
		jetty.stop();
		jetty = null;
		System.clearProperty("managed.schema.mutable");
		System.clearProperty("enable.update.log");

		if (restTestHarness != null) {
			restTestHarness.close();
		}
		restTestHarness = null;
	}

	/**
	 * Test adding managed stopwords to an endpoint defined in the schema, then
	 * adding docs containing a stopword before and after removing the stopword
	 * from the managed stopwords set.
	 */
	@Test
	public void testManagedCompounds() throws Exception {
		String endpoint = ManagedKoreanFilterFactory.RESOURCEID_COMPOUNDS + "compounds";
		String newFieldName = "managed_ko_field";

		// 복합명사사전 목록이 비었는지 확인
		assertJQ(endpoint, "/contents/initArgs=={}", "/contents/managedList==[]");

		// 새로운 필드 추가

		// 존재하지 않는 필드인지 확인
		assertQ("/schema/fields/" + newFieldName + "?indent=on&wt=xml",
				"count(/response/lst[@name='field']) = 0",
				"/response/lst[@name='responseHeader']/int[@name='status'] = '404'",
				"/response/lst[@name='error']/int[@name='code'] = '404'");

		// 새로운 필드 등록
		assertJPost("/schema/fields",
				"{add-field : { name :" + newFieldName + ", type : managed_ko}}",
				"/responseHeader/status==0");

		// 새 필드가 존재하는지 확인
		assertQ("/schema/fields/" + newFieldName + "?indent=on&wt=xml",
				"count(/response/lst[@name='field']) = 1",
				"/response/lst[@name='responseHeader']/int[@name='status'] = '0'");

		// 테스트 문서 색인 - 복합명사 사전 적용전
		assertU(adoc(newFieldName, "아르고넷에서 한글형태소분석기를 공개하였습니다", "id", "6"));
		assertU(commit());

		// 1건 있는지 확인
		assertQ("/select?q=" + p("*:*"),
				"/response/lst[@name='responseHeader']/int[@name='status'] = '0'",
				"/response/result[@name='response'][@numFound='1']");

		// 아르고로 검색 가능?
		assertQ("/select?q=" + p(newFieldName + ":아르고"),
				"/response/lst[@name='responseHeader']/int[@name='status'] = '0'",
				"/response/result[@name='response'][@numFound='0']");

		// 새로운 단어 등록
		assertJPut(endpoint, JSONUtil.toJSON(Arrays.asList("빅스타:빅,스타:0000", "아르고넷:아르고,넷")),
				"/responseHeader/status==0");

		// 복합명사 등록 확인
		assertJQ(endpoint, "/contents/initArgs=={}",
				"/contents/managedList==['빅스타:빅,스타:0000', '아르고넷:아르고,넷']");

		// 복합명사 등록 확인
		assertQ(endpoint, "count(/response/lst[@name='contents']/arr[@name='managedList']/*) = 2",
				"(/response/lst[@name='contents']/arr[@name='managedList']/str)[1] = '빅스타:빅,스타:0000'",
				"(/response/lst[@name='contents']/arr[@name='managedList']/str)[2] = '아르고넷:아르고,넷'");

		// 테스트 문서 다시 색인 - 복합명사 사전 적용전
		assertU(adoc(newFieldName, "아르고넷에서 한글형태소분석기를 공개하였습니다", "id", "6"));
		assertU(commit());

		// 1건 있는지 확인
		assertQ("/select?q=" + p("*:*"),
				"/response/lst[@name='responseHeader']/int[@name='status'] = '0'",
				"/response/result[@name='response'][@numFound='1']");

		assertQ("/select?q=" + p(newFieldName + ":아르고넷"),
				"/response/lst[@name='responseHeader']/int[@name='status'] = '0'",
				"/response/result[@name='response'][@numFound='1']");

		// 아르고로 검색 가능? 복합명사 적용전이기 때문에 0건
		assertQ("/select?q=" + p(newFieldName + ":아르고"),
				"/response/lst[@name='responseHeader']/int[@name='status'] = '0'",
				"/response/result[@name='response'][@numFound='0']");

		restTestHarness.reload(); // make the word set available

		// 복합명사 등록 확인
		assertQ(endpoint, "count(/response/lst[@name='contents']/arr[@name='managedList']/*) = 2",
				"(/response/lst[@name='contents']/arr[@name='managedList']/str)[1] = '빅스타:빅,스타:0000'",
				"(/response/lst[@name='contents']/arr[@name='managedList']/str)[2] = '아르고넷:아르고,넷'");

		// 문서 재색인 - 복합명사가 적용되어야 함
		assertU(adoc(newFieldName, "아르고넷에서 한글형태소분석기 arirang을 공개하였습니다", "id", "7"));
		assertU(commit());
		assertQ("/select?q=" + p("*:*"),
				"/response/lst[@name='responseHeader']/int[@name='status'] = '0'",
				"/response/result[@name='response'][@numFound='2']");

		assertQ("/select?debug=true&q=" + p(newFieldName + ":아르고넷"),
				"/response/lst[@name='responseHeader']/int[@name='status'] = '0'",
				"/response/result[@name='response'][@numFound='2']");

		// 아르고 검색 가능?
		assertQ("/select?q=" + p(newFieldName + ":아르고"),
				"/response/lst[@name='responseHeader']/int[@name='status'] = '0'",
				"/response/result[@name='response'][@numFound='1']",
				"/response/result[@name='response']/doc/str[@name='id'][.='7']");

		// verify delete works
		// assertJDelete(endpoint + "/the", "/responseHeader/status==0");

		// should fail with 404 as foo doesn't exist
		// assertJDelete(endpoint + "/foo", "/error/code==404");
	}

	private String p(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// NEVER?
			throw new RuntimeException(e);
		}
	}
}
