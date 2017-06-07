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
package org.apache.solr.analysis.ko;

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
import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilterFactory;
import org.apache.lucene.analysis.miscellaneous.KeywordRepeatFilterFactory;
import org.apache.lucene.analysis.miscellaneous.RemoveDuplicatesTokenFilterFactory;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.Test;

public class TestKoreanTokenizerFactory extends SolrTestCaseJ4 {

	KoreanTokenizerFactory kt;
	LowerCaseFilterFactory lc;
	KoreanFilterFactory kf;
	HanjaMappingFilterFactory hmf;
	KeywordMarkerFilterFactory kmf;
	PunctuationDelimitFilterFactory pdf;
	KeywordRepeatFilterFactory krf;
	EnglishPossessiveFilterFactory epf;
	PorterStemFilterFactory psf;
	RemoveDuplicatesTokenFilterFactory rdt;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		// initCore();
		Map<String, String> args = new HashMap<>();
		Map<String, String> kfArgs = new HashMap<>();
		{
			kfArgs.put("hasOrigin", "true");
			kfArgs.put("hasCNoun", "true");
			kfArgs.put("bigrammable", "false");
			kfArgs.put("queryMode", "false");
		}

		kt = new KoreanTokenizerFactory(args);
		lc = new LowerCaseFilterFactory(args);
		kf = new KoreanFilterFactory(kfArgs);
		hmf = new HanjaMappingFilterFactory(args);
		kmf = new KeywordMarkerFilterFactory(args);
		pdf = new PunctuationDelimitFilterFactory(args);
		krf = new KeywordRepeatFilterFactory(args);
		epf = new EnglishPossessiveFilterFactory(args);
		psf = new PorterStemFilterFactory(args);
		rdt = new RemoveDuplicatesTokenFilterFactory(args);
	}

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

	private void assertFilter(String input, String... tokens) throws IOException {
		TokenStream stream = filterK(tokenizerK(input));
		BaseTokenStreamTestCase.assertTokenStreamContents(stream, tokens);
	}

	private TokenStream forIndex(String s) {
		Tokenizer tokenizer = kt.create();
		TokenStream stream = lc.create(tokenizer);
		stream = kf.create(stream);
		stream = hmf.create(stream);
		stream = kmf.create(stream);
		stream = pdf.create(stream);
		stream = krf.create(stream);
		stream = epf.create(stream);
		stream = psf.create(stream);
		stream = rdt.create(stream);
		tokenizer.setReader(new StringReader(s));
		return stream;
	}

	@Test
	public void testKoreanTokenzier() throws IOException {
		analyze("한국을 빛낸 100명의 위인들", "한국을", "빛낸", "100명의", "위인들");
		analyze("C++ 프로그래밍 바이블", "C++", "프로그래밍", "바이블");
	}

	@Test
	public void testKoreanFilter() throws IOException {
		assertFilter("한국을 빛낸 100명의 위인들", "한국을", "한국", "빛낸", "100명의", "100", "명의", "위인들", "위"); // 위인?
		analyze("C++ 프로그래밍 바이블", "C++", "프로그래밍", "바이블");
	}
}
