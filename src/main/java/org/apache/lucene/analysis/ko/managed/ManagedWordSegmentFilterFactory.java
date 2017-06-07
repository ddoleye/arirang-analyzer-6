package org.apache.lucene.analysis.ko.managed;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.WordSegmentFilter;

public class ManagedWordSegmentFilterFactory extends BaseManagedDictionaryFilterFactory {

	private static final String HAS_ORIGIN_PARAM = "hasOrigin";

	private final boolean hasOrigin;

	public ManagedWordSegmentFilterFactory(Map<String, String> args) {
		super(args);
		hasOrigin = getBoolean(args, HAS_ORIGIN_PARAM, true);
	}

	@Override
	public TokenStream create(TokenStream input) {
		return new WordSegmentFilter(input, getDictionary(), hasOrigin);
	}

}
