package org.apache.lucene.analysis.ko;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;

/**
 * Created by SooMyung(soomyung.lee@gmail.com) on 2014. 7. 30.
 */

public class HanjaMappingFilterFactory extends BaseDictionaryFilterFactory {
	/**
	 * Initialize this factory via a set of key-value pairs.
	 *
	 * @param args
	 */
	public HanjaMappingFilterFactory(Map<String, String> args) {
		super(args);
	}

	@Override
	public TokenStream create(TokenStream input) {
		return new HanjaMappingFilter(input, getDictionary());
	}

}
