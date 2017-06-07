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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.KoreanFilter;
import org.apache.lucene.analysis.ko.dictionary.Dictionary;
import org.apache.lucene.analysis.ko.dictionary.DictionaryBuilder;
import org.apache.lucene.analysis.ko.dictionary.DictionaryType;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.rest.ManagedResource;
import org.apache.solr.rest.ManagedResourceObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link org.apache.lucene.analysis.ko.KoreanFilter}.
 * 
 * <pre class="prettyprint">
 * &lt;fieldType name="text_kr" class="solr.TextField"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.KoreanTokenizerFilterFactory"/&gt;
 *     &lt;filter class="solr.KoreanFilter"
 *       bigrammable="true"
 *       hasOrigin="true"
 *       hasCNoun="true"
 *       exactMatch="false"
 *       words="ko"
 *       compounds="compounds"
 *       uncompounds="uncompounds"
 *       emptyWords="false"       
 *     /&gt;
 *   &lt;/filter&gt;
 * &lt;/fieldType&gt;
 * </pre>
 */
@Deprecated
public class ManagedKoreanFilterFactory extends TokenFilterFactory
		implements ResourceLoaderAware, ManagedResourceObserver {

	// 공유하도록 한다.
	public static final String RESOURCEID_WORDS = "/schema/analysis/arirang/words/";
	public static final String RESOURCEID_COMPOUNDS = "/schema/analysis/arirang/compounds/";
	public static final String RESOURCEID_UNCOMPOUNDS = "/schema/analysis/arirang/uncompounds/";

	private static final String DECOMPOUND_PARAM = "decompound";

	private static final String BIGRAMMABLE_PARAM = "bigrammable";

	private static final String HAS_ORIGIN_PARAM = "hasOrigin";

	private static final String HAS_COMPOUND_NOUN_PARAM = "hasCNoun";

	private static final String IS_QUERY_MODE_PARAM = "queryMode";

	// Decides whether the original compound noun is returned or not if analyzed
	// morphologically
	private static final String EXACT_MATCH_PARAM = "exactMatch";

	private final boolean bigrammable;
	private final boolean hasOrigin;
	private final boolean hasCNoun;
	private final boolean exactMatch;
	private final boolean queryMode;
	private final boolean decompound;

	private String handleWords;
	private String handleCompounds;
	private String handleUnCompounds;

	private List<String> words;
	private List<String> compounds;
	private List<String> uncompounds;
	private boolean emptyWords = false;
	private Dictionary dictionary = null;

	private static Logger log = LoggerFactory.getLogger(ManagedKoreanFilterFactory.class);

	// Debugging
	public ManagedKoreanFilterFactory(Map<String, String> args, List<String> words,
			List<String> compounds, List<String> uncompounds) {
		this(args);

		this.words = words;
		this.compounds = compounds;
		this.uncompounds = uncompounds;
	}

	/**
	 * Initialize this factory via a set of key-value pairs.
	 */
	public ManagedKoreanFilterFactory(Map<String, String> args) {
		super(args);
		bigrammable = getBoolean(args, BIGRAMMABLE_PARAM, true);
		hasOrigin = getBoolean(args, HAS_ORIGIN_PARAM, true);
		exactMatch = getBoolean(args, EXACT_MATCH_PARAM, false);
		hasCNoun = getBoolean(args, HAS_COMPOUND_NOUN_PARAM, true);
		queryMode = getBoolean(args, IS_QUERY_MODE_PARAM, true);
		decompound = getBoolean(args, DECOMPOUND_PARAM, true);

		handleWords = get(args, "words");
		handleCompounds = get(args, "compounds");
		handleUnCompounds = get(args, "uncompounds");
		emptyWords = getBoolean(args, "emptyWords", false);
		if (!args.isEmpty()) {
			throw new IllegalArgumentException("Unknown parameters: " + args);
		}
	}

	@Override
	public TokenStream create(TokenStream tokenstream) {
		return new KoreanFilter(tokenstream, bigrammable, hasOrigin, exactMatch, hasCNoun,
				queryMode, decompound, getDictionary());
	}

	@Override
	public void inform(ResourceLoader loader) throws IOException {
		SolrResourceLoader solrResourceLoader = (SolrResourceLoader) loader;

		if (handleWords != null) {
			solrResourceLoader.getManagedResourceRegistry().registerManagedResource(
					RESOURCEID_WORDS + handleWords, //
					ManagedStringListResource.class, this);
		}
		if (handleCompounds != null) {
			solrResourceLoader.getManagedResourceRegistry().registerManagedResource(
					RESOURCEID_COMPOUNDS + handleCompounds, //
					ManagedStringListResource.class, this);
		}
		if (handleUnCompounds != null) {
			solrResourceLoader.getManagedResourceRegistry().registerManagedResource(
					RESOURCEID_UNCOMPOUNDS + handleUnCompounds, //
					ManagedStringListResource.class, this);
		}
	}

	private Dictionary getDictionary() {
		if (dictionary == null) {
			synchronized (this) {
				if (dictionary == null) {
					long start = System.currentTimeMillis();
					// 기본 사전 추가 TODO 기본 사전을 공유하는 방법이 필요할둣

					DictionaryBuilder builder = DictionaryBuilder.newBuilder();

					if (emptyWords) {
						// 단어 사전을 제외한다.
						log.info("기본 단어 사전을 사용하지 않습니다");
						builder.addSystemResource(DictionaryType.SyllableFeature, //
								// ArirangResourceType.Dictionary, //
								// ArirangResourceType.Extension, //
								DictionaryType.Josa, //
								DictionaryType.Eomi, //
								DictionaryType.Prefix, //
								DictionaryType.Suffix, //
								// ArirangResourceType.Compounds, //
								// ArirangResourceType.UnCompounds, //
								DictionaryType.Abbrev, //
								DictionaryType.Hanja); // );
					} else {
						builder.addSystemResource();
					}

					log.info("기본 사전 추가 {}ms", System.currentTimeMillis() - start);
					if (words != null && !words.isEmpty()) {
						builder.add(DictionaryType.Dictionary, words);
					}
					if (compounds != null && !compounds.isEmpty()) {
						builder.add(DictionaryType.Compounds, compounds);
					}
					if (uncompounds != null && !uncompounds.isEmpty()) {
						builder.add(DictionaryType.UnCompounds, uncompounds);
					}
					dictionary = builder.build();
					log.info("사전 준비 완료 {}ms", System.currentTimeMillis() - start);
				}
			}
		}

		return dictionary;
	}

	// private AtomicInteger c = new AtomicInteger();

	@Override
	public void onManagedResourceInitialized(NamedList<?> args, ManagedResource res)
			throws SolrException {
		List<String> list = ((ManagedStringListResource) res).getContents();

		// 두번 이상 호출?
		// log.info("{} 추가 사전[{}] 적용 [{}]", c.incrementAndGet(),
		// res.getResourceId(), list);
		if (res.getResourceId().startsWith(RESOURCEID_WORDS)) {
			words = new ArrayList<String>(list);
		} else if (res.getResourceId().startsWith(RESOURCEID_COMPOUNDS)) {
			compounds = new ArrayList<String>(list);
		} else if (res.getResourceId().startsWith(RESOURCEID_UNCOMPOUNDS)) {
			uncompounds = new ArrayList<String>(list);
		} else {
			// what?
		}
	}
}
