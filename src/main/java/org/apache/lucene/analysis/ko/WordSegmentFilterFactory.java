package org.apache.lucene.analysis.ko;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;

public class WordSegmentFilterFactory extends BaseDictionaryFilterFactory {

	private static final String HAS_ORIGIN_PARAM = "hasOrigin";

	private final boolean hasOrigin;

	public WordSegmentFilterFactory(Map<String, String> args) {
		super(args);
		hasOrigin = getBoolean(args, HAS_ORIGIN_PARAM, true);
	}

	@Override
	public TokenStream create(TokenStream input) {
		return new WordSegmentFilter(input, getDictionary(), hasOrigin);
	}

}
