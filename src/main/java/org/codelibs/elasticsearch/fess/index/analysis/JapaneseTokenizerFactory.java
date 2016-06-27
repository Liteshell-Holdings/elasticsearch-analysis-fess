/*
 * Copyright 2009-201 the CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.codelibs.elasticsearch.fess.index.analysis;

import java.lang.reflect.Constructor;

import org.apache.lucene.analysis.Tokenizer;
import org.codelibs.elasticsearch.fess.analysis.EmptyTokenizer;
import org.codelibs.elasticsearch.fess.service.FessAnalysisService;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractTokenizerFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.index.settings.IndexSettingsService;

public class JapaneseTokenizerFactory extends AbstractTokenizerFactory {

    private static final String KUROMOJI_TOKENIZER_FACTORY =
            "org.codelibs.elasticsearch.kuromoji.neologd.index.analysis.KuromojiTokenizerFactory";

    private TokenizerFactory tokenizerFactory = null;

    @Inject
    public JapaneseTokenizerFactory(Index index, IndexSettingsService indexSettingsService, Environment env, @Assisted String name,
            @Assisted Settings settings, FessAnalysisService fessAnalysisService) {
        super(index, indexSettingsService.getSettings(), name, settings);

        Class<?> tokenizerFactoryClass = fessAnalysisService.loadClass(KUROMOJI_TOKENIZER_FACTORY);
        if (logger.isInfoEnabled()) {
            logger.info("{} is not found.", KUROMOJI_TOKENIZER_FACTORY);
        }
        if (tokenizerFactoryClass != null) {
            try {
                final Constructor<?> constructor = tokenizerFactoryClass.getConstructor(Index.class, IndexSettingsService.class,
                        Environment.class, String.class, Settings.class);
                tokenizerFactory = (TokenizerFactory) constructor.newInstance(index, indexSettingsService, env, name, settings);
            } catch (final Exception e) {
                throw new ElasticsearchException("Failed to load " + KUROMOJI_TOKENIZER_FACTORY, e);
            }
        }
    }

    @Override
    public Tokenizer create() {
        if (tokenizerFactory != null) {
            return tokenizerFactory.create();
        }
        return new EmptyTokenizer();
    }

}
