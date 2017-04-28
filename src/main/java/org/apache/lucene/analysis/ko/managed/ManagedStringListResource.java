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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.rest.BaseSolrResource;
import org.apache.solr.rest.ManagedResource;
import org.apache.solr.rest.ManagedResourceStorage.StorageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedStringListResource extends ManagedResource
		implements ManagedResource.ChildResourceSupport {
	public static final String JSON_FIELD = "contents";
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private List<String> managedContents = null;

	public ManagedStringListResource(String resourceId, SolrResourceLoader loader, StorageIO storageIO)
			throws SolrException {
		super(resourceId, loader, storageIO);
	}

	/**
	 * Returns the set of words in this managed word set.
	 */
	public List<String> getContents() {
		return Collections.unmodifiableList(managedContents);
	}

	/**
	 * Invoked when loading data from storage to initialize the list of words
	 * managed by this instance. A load of the data can happen many times
	 * throughout the life cycle of this object.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void onManagedDataLoadedFromStorage(NamedList<?> initArgs, Object data)
			throws SolrException {
		managedContents = new ArrayList<>();
		if (data != null) {
			List<String> list = (List<String>) data;
			for (String line : list) {
				managedContents.add(line);
			}
		} else {
			storeManagedData(new ArrayList<String>(0));
		}
		
		log.info("Loaded " + managedContents.size() + " lines for " + getResourceId());
	}

	/**
	 * Implements the GET request to provide the list of words to the client.
	 * Alternatively, if a specific word is requested, then it is returned or a
	 * 404 is raised, indicating that the requested word does not exist.
	 */
	@Override
	public void doGet(BaseSolrResource endpoint, String childId) {
		SolrQueryResponse response = endpoint.getSolrResponse();
		if (childId != null) {
			// downcase arg if we're configured to ignoreCase
			
			// TODO unsupported
		} else {
			response.add(JSON_FIELD, buildMapToStore(managedContents));
		}
	}

	/**
	 * Deletes words managed by this resource.
	 */
	@Override
	public synchronized void doDeleteChild(BaseSolrResource endpoint, String childId) {
		// downcase arg if we're configured to ignoreCase
		// TODO unsupported
	}

	/**
	 * Applies updates to the word set being managed by this resource.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Object applyUpdatesToManagedData(Object updates) {
		List<String> list = (List<String>) updates;

		log.info("Applying updates: " + list);
		
		managedContents.clear();
		for (String line : list) {
			if (managedContents.add(line)) {
				log.debug("Added word: {}", line);
			}
		}
		return managedContents;
	}

	@Override
	protected boolean updateInitArgs(NamedList<?> updatedArgs) {
		// nothing todo
		
		// otherwise currentIgnoreCase == updatedIgnoreCase: nothing to do
		return super.updateInitArgs(updatedArgs);
	}
}
