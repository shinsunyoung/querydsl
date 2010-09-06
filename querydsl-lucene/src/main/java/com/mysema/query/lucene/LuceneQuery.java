/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;

import com.mysema.query.QueryException;
import com.mysema.query.QueryMetadata;
import com.mysema.query.QueryModifiers;
import com.mysema.query.SearchResults;
import com.mysema.query.SimpleProjectable;
import com.mysema.query.SimpleQuery;
import com.mysema.query.support.QueryMixin;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Param;
import com.mysema.query.types.expr.EBoolean;

/**
 * LuceneQuery is a Querydsl query implementation for Lucene queries.
 *
 * @author vema
 */
public class LuceneQuery implements SimpleQuery<LuceneQuery>, SimpleProjectable<Document> {

    private final QueryMixin<LuceneQuery> queryMixin;

    private final Searcher searcher;

    private final LuceneSerializer serializer;

    public LuceneQuery(LuceneSerializer serializer, Searcher searcher) {
        queryMixin = new QueryMixin<LuceneQuery>(this);
        this.serializer = serializer;
        this.searcher = searcher;
    }

    public LuceneQuery(Searcher searcher) {
        this(LuceneSerializer.DEFAULT, searcher);
    }

    @Override
    public long count() {
        try {
            int maxDoc = searcher.maxDoc();
            if (maxDoc == 0) {
                return 0;
            }
            return searcher.search(createQuery(), maxDoc).totalHits;
        } catch (IOException e) {
            throw new QueryException(e);
        }
    }

    @Override
    public long countDistinct() {
        return count();
    }

    private Query createQuery() {
        if (queryMixin.getMetadata().getWhere() == null) {
            throw new QueryException("Where clause was null.");
        }
        return serializer.toQuery(queryMixin.getMetadata().getWhere(), queryMixin.getMetadata());
    }

    @Override
    public LuceneQuery limit(long limit) {
        return queryMixin.limit(limit);
    }

    @Override
    public List<Document> list() {
        QueryMetadata metadata = queryMixin.getMetadata();
        List<OrderSpecifier<?>> orderBys = metadata.getOrderBy();
        Long queryLimit = metadata.getModifiers().getLimit();
        Long queryOffset = metadata.getModifiers().getOffset();
        int limit;
        int offset = queryOffset != null ? queryOffset.intValue() : 0;
        try {
            limit = searcher.maxDoc();
        } catch (IOException e) {
            throw new QueryException(e);
        }
        if (queryLimit != null && queryLimit.intValue() < limit) {
            limit = queryLimit.intValue();
        }
        if (!orderBys.isEmpty()) {
            return listSorted(orderBys, limit, offset);
        }

        List<Document> documents = null;
        try {
            ScoreDoc[] scoreDocs = searcher.search(createQuery(), limit + offset).scoreDocs;
            documents = new ArrayList<Document>(scoreDocs.length - offset);
            for (int i = offset; i < scoreDocs.length; ++i) {
                documents.add(searcher.doc(scoreDocs[i].doc));
            }
        } catch (IOException e) {
            throw new QueryException(e);
        }
        return documents;
    }

    @Override
    public List<Document> listDistinct() {
        return list();
    }

    @Override
    public SearchResults<Document> listDistinctResults() {
        return listResults();
    }

    @Override
    public SearchResults<Document> listResults() {
        List<Document> documents = list();
        /*
         * TODO Get rid of count(). It could be implemented by iterating the
         * list results in list* from n to m.
         */
        return new SearchResults<Document>(documents, queryMixin.getMetadata().getModifiers(), count());
    }

    private List<Document> listSorted(List<OrderSpecifier<?>> orderBys, int limit, int offset) {
        try {
            Sort sort = serializer.toSort(orderBys);
            ScoreDoc[] scoreDocs = searcher.search(createQuery(), null, limit + offset, sort).scoreDocs;
            List<Document> documents = new ArrayList<Document>(scoreDocs.length - offset);
            for (int i = offset; i < scoreDocs.length; ++i) {
                documents.add(searcher.doc(scoreDocs[i].doc));
            }
            return documents;
        } catch (IOException e) {
            throw new QueryException(e);
        }
    }

    @Override
    public LuceneQuery offset(long offset) {
        return queryMixin.offset(offset);
    }

    @Override
    public LuceneQuery orderBy(OrderSpecifier<?>... o) {
        return queryMixin.orderBy(o);
    }

    @Override
    public LuceneQuery restrict(QueryModifiers modifiers) {
        return queryMixin.restrict(modifiers);
    }

    @Override
    public <T> LuceneQuery set(Param<T> param, T value) {
        return queryMixin.set(param, value);
    }

    @Override
    public Document uniqueResult() {
        try {
            int maxDoc = searcher.maxDoc();
            if (maxDoc == 0) {
                return null;
            }
            ScoreDoc[] scoreDocs = searcher.search(createQuery(), maxDoc).scoreDocs;
            if (scoreDocs.length > 1) {
                throw new QueryException("More than one result found!");
            } else if (scoreDocs.length == 1) {
                return searcher.doc(scoreDocs[0].doc);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new QueryException(e);
        }
    }

    @Override
    public LuceneQuery where(EBoolean... e) {
        return queryMixin.where(e);
    }

}
