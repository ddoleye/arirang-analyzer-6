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

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.ko.HanjaMappingFilterFactory;
import org.apache.lucene.analysis.ko.KoreanFilterFactory;
import org.apache.lucene.analysis.ko.KoreanTokenizerFactory;
import org.apache.lucene.analysis.ko.PunctuationDelimitFilterFactory;
import org.apache.lucene.analysis.ko.managed.ManagedKoreanFilterFactory;
import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilterFactory;
import org.apache.lucene.analysis.miscellaneous.KeywordRepeatFilterFactory;
import org.apache.lucene.analysis.miscellaneous.RemoveDuplicatesTokenFilterFactory;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.Test;

/**
 * Test the REST API for managing stop words, which is pretty basic: GET:
 * returns the list of stop words or a single word if it exists PUT: add some
 * words to the current list
 */
public class TestManagedKoreanFilterFactory2 extends SolrTestCaseJ4 {

	Map<String, String> args = new HashMap<>();

	Map<String, String> kfArgs = new HashMap<>();
	{
		kfArgs.put("hasOrigin", "true");
		kfArgs.put("hasCNoun", "true");
		kfArgs.put("bigrammable", "false");
		kfArgs.put("queryMode", "false");
	}

	KoreanTokenizerFactory kt = new KoreanTokenizerFactory(args);
	LowerCaseFilterFactory lc = new LowerCaseFilterFactory(args);
	KoreanFilterFactory kf = new KoreanFilterFactory(kfArgs);
	ManagedKoreanFilterFactory mkf = new ManagedKoreanFilterFactory(kfArgs);
	HanjaMappingFilterFactory hmf = new HanjaMappingFilterFactory(args);
	KeywordMarkerFilterFactory kmf = new KeywordMarkerFilterFactory(args);
	PunctuationDelimitFilterFactory pdf = new PunctuationDelimitFilterFactory(args);
	KeywordRepeatFilterFactory krf = new KeywordRepeatFilterFactory(args);
	EnglishPossessiveFilterFactory epf = new EnglishPossessiveFilterFactory(args);
	PorterStemFilterFactory psf = new PorterStemFilterFactory(args);
	RemoveDuplicatesTokenFilterFactory rdt = new RemoveDuplicatesTokenFilterFactory(args);

	// IndexSchema schema;
	//
	// @BeforeClass
	// public static void beforeClass() throws Exception {
	// initCore("solrconfig.xml", "schema.xml");
	// }
	//
	// @Override
	// @Before
	// public void setUp() throws Exception {
	// super.setUp();
	// schema = IndexSchemaFactory.buildIndexSchema(getSchemaFile(),
	// solrConfig);
	// clearIndex();
	// assertU(commit());
	// }

	/**
	 * @param s
	 *            분해할 문자열
	 * @param tokens
	 *            분해된 토큰
	 * @throws IOException
	 */
	private void analyze(String s, String... tokens) throws IOException {
		TokenStream input = tokenizerK(s);
		int[] increments = new int[tokens.length];
		Arrays.fill(increments, 1);
		BaseTokenStreamTestCase.assertTokenStreamContents(input, tokens, increments);
	}

	private TokenStream tokenizerK(String s) {
		Tokenizer input = kt.create();
		input.setReader(new StringReader(s));
		return input;
	}

	private TokenStream filterK(TokenStream input) throws IOException {
		// hasOrigin="true" hasCNoun="true" bigrammable="false"
		// queryMode="false"
		TokenStream stream = kf.create(input);
		return stream;
	}

	private TokenStream compoundsFilterK(String input, String... compounds) throws IOException {
		ManagedKoreanFilterFactory mkf = new ManagedKoreanFilterFactory(kfArgs, null,
				Arrays.asList(compounds), null);
		TokenStream stream = mkf.create(tokenizerK(input));
		return stream;
	}

	private void assertFilter(String input, String... tokens) throws IOException {
		TokenStream stream = filterK(tokenizerK(input));
		BaseTokenStreamTestCase.assertTokenStreamContents(stream, tokens);
	}

	@Test
	public void testKoreanTokenzier() throws IOException {
		analyze("한국을 빛낸 100명의 위인들", "한국을", "빛낸", "100명의", "위인들");
		analyze("C++ 프로그래밍 바이블", "C++", "프로그래밍", "바이블");
	}

	@Test
	public void testKoreanFilter() throws IOException {
		assertFilter("한국을 빛낸 100명의 위인들", "한국을", "한국", "빛낸", "100명의", "100", "명의", "위인들", "위"); // 위인?
		assertFilter("아르고넷에서 한글형태소분석기를 공개하였습니다", "아르고넷에서", "아르고넷", "한글형태소분석기를", "한글형태소분석기", "한글",
				"형태소", "분석기", "공개하였습니다", "공개");
	}

	@Test
	public void testCompounds() throws IOException {
		TokenStream compoundsFilterK = compoundsFilterK("아르고넷에서 한글형태소분석기를 공개하였습니다",
				"아르고넷:아르고,넷:0000");
		BaseTokenStreamTestCase.assertTokenStreamContents(compoundsFilterK, new String[] { "아르고넷에서",
				"아르고넷", "아르고", "넷", "한글형태소분석기를", "한글형태소분석기", "한글", "형태소", "분석기", "공개하였습니다", "공개" });
	}
}
