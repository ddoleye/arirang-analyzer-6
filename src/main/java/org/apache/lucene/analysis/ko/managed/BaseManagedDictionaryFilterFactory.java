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
import java.util.Map;

import org.apache.lucene.analysis.ko.dictionary.Dictionary;
import org.apache.lucene.analysis.ko.managed.ManagedKoreanDictionary;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.rest.ManagedResource;
import org.apache.solr.rest.ManagedResourceObserver;

public abstract class BaseManagedDictionaryFilterFactory extends TokenFilterFactory
		implements ResourceLoaderAware, ManagedResourceObserver {
	protected static final String MANAGED_DEFAULT_ID = "ko";
	protected static final String MANAGED_EMPTY_WORDS = "emptyWords";

	private Dictionary dictionary;
	protected String handle;

	protected BaseManagedDictionaryFilterFactory(Map<String, String> args) {
		super(args);

		handle = get(args, "managed", MANAGED_DEFAULT_ID);
	}

	/**
	 * Registers an endpoint with the RestManager so that this component can be
	 * managed using the REST API. This method can be invoked before all the
	 * resources the {@link org.apache.solr.rest.RestManager} needs to
	 * initialize a {@link ManagedResource} are available, so this simply
	 * registers the need to be managed at a specific endpoint and lets the
	 * RestManager deal with initialization when ready.
	 */
	@Override
	public void inform(ResourceLoader loader) throws IOException {
		SolrResourceLoader solrResourceLoader = (SolrResourceLoader) loader;

		// here we want to register that we need to be managed
		// at a specified path and the ManagedResource impl class
		// that should be used to manage this component
		solrResourceLoader.getManagedResourceRegistry().registerManagedResource(getResourceId(),
				ManagedKoreanDictionary.class, this);
	}

	protected String getResourceId() {
		return "/schema/analysis/arirang/" + handle;
	}

	// protected final Class<? extends ManagedResource>
	// getManagedResourceImplClass() {
	// return ManagedKoreanDictionary.class;
	// }

	@Override
	public final void onManagedResourceInitialized(NamedList<?> args, ManagedResource res)
			throws SolrException {

		// 사전 참조를 가져온다.
		dictionary = ((ManagedKoreanDictionary) res).getDictionary();
	}

	protected final Dictionary getDictionary() {
		if (dictionary == null) {
			throw new IllegalStateException("Managed dictionary not initialized correctly!");
		}

		return dictionary;
	}
}
