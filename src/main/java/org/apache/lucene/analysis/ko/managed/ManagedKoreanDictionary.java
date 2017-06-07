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
package org.apache.lucene.analysis.ko.managed;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.analysis.ko.dictionary.Dictionary;
import org.apache.lucene.analysis.ko.dictionary.DictionaryBuilder;
import org.apache.lucene.analysis.ko.dictionary.DictionaryType;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.rest.BaseSolrResource;
import org.apache.solr.rest.ManagedResource;
import org.apache.solr.rest.ManagedResourceStorage.StorageIO;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ManagedStringListResource 가 하나의 사전 내용을 관리한다면 ManagedKoreanDictionary는 전체
 * 사전(여기서는 단어사전, 복합명사 사전)을 관리한다. Dictionary의 전체 공유를 위한 클래스이다.
 */
public class ManagedKoreanDictionary extends ManagedResource
		implements ManagedResource.ChildResourceSupport {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static final String EMPTYWORDS_ARG = "emptyWords";

	public static final String RESOURCES = "resources";
	public static final String WORDSET = "words";
	public static final String COMPOUNDS = "compounds";
	public static final String UNCOMPOUNDS = "uncompounds";

	private List<String> words = new ArrayList<>();
	private List<String> compounds = new ArrayList<>();
	private List<String> uncompounds = new ArrayList<>();

	private Dictionary dictionary;
	// private static final AtomicInteger dictionaryCount = new AtomicInteger();

	public ManagedKoreanDictionary(String resourceId, SolrResourceLoader loader,
			StorageIO storageIO) throws SolrException {
		super(resourceId, loader, storageIO);
	}

	public boolean getEmptyWords(NamedList<?> initArgs) {
		Boolean emptyWords = initArgs.getBooleanArg(EMPTYWORDS_ARG);
		// ignoreCase = false by default
		return null == emptyWords ? false : emptyWords;
	}

	@Override
	public void onResourceDeleted() throws IOException {
		// log.info("사전 개수? {}", dictionaryCount.decrementAndGet());
		super.onResourceDeleted();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onManagedDataLoadedFromStorage(NamedList<?> managedInitArgs, Object managedData)
			throws SolrException {

		// boolean emptyWords = getEmptyWords(managedInitArgs);
		if (null == managedInitArgs.get(EMPTYWORDS_ARG)) {
			// Explicitly include the default value of ignoreCase
			((NamedList<Object>) managedInitArgs).add(EMPTYWORDS_ARG, false);
		}

		words.clear();
		compounds.clear();
		uncompounds.clear();

		if (managedData == null) {
			// TODO 아리랑 사전을 여기에 복사해야할까?
			// storeManagedData(resources);
			// ArirangResources resources = new ArirangResources();
			// resources.getResource(DictionaryType.Dictionary);
		} else {
			Map<String, List<String>> resources = (Map<String, List<String>>) managedData;
			List<String> a = resources.get(WORDSET);
			if (a != null) words.addAll(a);
			a = resources.get(COMPOUNDS);
			if (a != null) compounds.addAll(a);
			a = resources.get(UNCOMPOUNDS);
			if (a != null) uncompounds.addAll(a);

			log.info("Loaded [{}] words:{}, compounds:{}, uncompounds:{}", getResourceId(),
					words.size(), compounds.size(), uncompounds.size());
		}
	}

	@Override
	public void doGet(BaseSolrResource endpoint, String childId) {
		SolrQueryResponse response = endpoint.getSolrResponse();
		if (childId != null) {
			if (WORDSET.equals(childId)) {
				response.add(childId, words);
			} else if (COMPOUNDS.equals(childId)) {
				response.add(childId, compounds);
			} else if (UNCOMPOUNDS.equals(childId)) {
				response.add(childId, uncompounds);
			} else {
				throw new SolrException(ErrorCode.NOT_FOUND,
						String.format(Locale.ROOT, "%s not found in %s", childId, getResourceId()));
			}
		} else {
			Map<String, List<String>> managedResources = buildManagedMap();
			response.add(RESOURCES, buildMapToStore(managedResources));
		}
	}

	private Map<String, List<String>> buildManagedMap() {
		Map<String, List<String>> managedResources = new LinkedHashMap<>();
		managedResources.put(WORDSET, words);
		managedResources.put(COMPOUNDS, compounds);
		managedResources.put(UNCOMPOUNDS, uncompounds);
		return managedResources;
	}

	/**
	 * 해당 사전의 콘텐츠를 모두 삭제한다. REST에서 애매할 수도 있지만 endpoint가 삭제되는 것은 아니다.<br>
	 * DELETE /solr/analysis/korean/dic/compounds ..
	 */
	@Override
	public synchronized void doDeleteChild(BaseSolrResource endpoint, String childId) {
		if (WORDSET.equals(childId)) {
			words.clear();
		} else if (COMPOUNDS.equals(childId)) {
			compounds.clear();
		} else if (UNCOMPOUNDS.equals(childId)) {
			uncompounds.clear();
		} else {
			throw new SolrException(ErrorCode.NOT_FOUND,
					String.format(Locale.ROOT, "%s not found in %s", childId, getResourceId()));
		}

		storeManagedData(buildManagedMap());
		log.info("Clear resource: {}", childId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void doPut(BaseSolrResource endpoint, Representation entity, Object json) {
		log.info("Processing update to {}", getResourceId());

		boolean updatedInitArgs = false;
		Object managedData = null;

		boolean valid = false;

		if (json instanceof Map) {
			Map<String, Object> jsonMap = (Map<String, Object>) json;
			if (jsonMap.containsKey(INIT_ARGS_JSON_FIELD)) {
				Map<String, Object> initArgsMap = (Map<String, Object>) jsonMap
						.get(INIT_ARGS_JSON_FIELD);
				updatedInitArgs = updateInitArgs(new NamedList<>(initArgsMap));
				valid = true;
			}

			if (jsonMap.containsKey(WORDSET) //
					|| jsonMap.containsKey(COMPOUNDS) //
					|| jsonMap.containsKey(UNCOMPOUNDS)) {
				managedData = jsonMap;
				valid = true;
			}
		}

		if (!valid) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unsupported update format " + json.getClass().getName());
		}

		Object updated = null;
		if (managedData != null) {
			updated = applyUpdatesToManagedData(managedData);
		}

		if (updatedInitArgs || updated != null) {
			storeManagedData(buildManagedMap());
		}

	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object applyUpdatesToManagedData(Object updates) {
		boolean madeChanges = false;

		// 여기서는 항상 Map 이다.
		Map<String, ?> info = (Map<String, ?>) updates;
		if (applyUpdateTo(info, WORDSET, words)) madeChanges = true;
		if (applyUpdateTo(info, COMPOUNDS, compounds)) madeChanges = true;
		if (applyUpdateTo(info, UNCOMPOUNDS, uncompounds)) madeChanges = true;

		// return this? 임의의 object 를 반환한다.
		return madeChanges ? updates : null;
	}

	private boolean applyUpdateTo(Map<String, ?> info, String name, List<String> list) {
		boolean madeChanges = false;
		Object object = info.get(name);

		if (object == null) {
			//
		} else if (object instanceof String) {
			list.add((String) object); // 뒤로추가
			madeChanges = true;
		} else if (object instanceof List) {
			for (Object e : (List<?>) object) {
				// 문법적인 오류는 DictionaryBuilder 에서 처리될 것이다.
				// 데이터는 일단 리스트에 추가해 놓는다.
				String s = String.valueOf(e);
				// if (!list.contains(s)) {
				list.add(s);
				madeChanges = true;
				// }
			}
		}

		return madeChanges;
	}

	public Dictionary getDictionary() {
		if (dictionary == null) {
			synchronized (this) {
				if (dictionary == null) {
					dictionary = buildDictionary(getEmptyWords(managedInitArgs), words, compounds,
							uncompounds);

					// log.info("사전 개수? {}", dictionaryCount.incrementAndGet());
				}
			}
		}

		return dictionary;
	}

	private Dictionary buildDictionary(boolean emptyWords, List<String> words,
			List<String> compounds, List<String> uncompounds) {
		long start = System.currentTimeMillis();
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
		if (!words.isEmpty()) {
			builder.add(DictionaryType.Dictionary, words);
			log.info("사용자 단어 항목 추가 {}건", words.size());
		}
		if (!compounds.isEmpty()) {
			builder.add(DictionaryType.Compounds, compounds);
			log.info("사용자 복합명사 항목 추가 {}건", compounds.size());
		}
		if (!uncompounds.isEmpty()) {
			builder.add(DictionaryType.UnCompounds, uncompounds);
			log.info("사용자 복합명사분해 항목 추가 {}건", uncompounds.size());
		}

		Dictionary dictionary = builder.build();
		log.info("사전 준비 완료 {}ms", System.currentTimeMillis() - start);

		return dictionary;
	}
}
