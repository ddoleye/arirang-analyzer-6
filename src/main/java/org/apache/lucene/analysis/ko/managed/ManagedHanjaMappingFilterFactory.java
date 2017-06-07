package org.apache.lucene.analysis.ko.managed;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.HanjaMappingFilter;

/**
 * Created by SooMyung(soomyung.lee@gmail.com) on 2014. 7. 30.
 */

public class ManagedHanjaMappingFilterFactory extends BaseManagedDictionaryFilterFactory {
	/**
	 * Initialize this factory via a set of key-value pairs.
	 *
	 * @param args
	 */
	public ManagedHanjaMappingFilterFactory(Map<String, String> args) {
		super(args);
	}

	@Override
	public TokenStream create(TokenStream input) {
		return new HanjaMappingFilter(input, getDictionary());
	}

}
