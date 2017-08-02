package parentchild;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.search.join.ToParentBlockJoinQuery;
import org.apache.lucene.search.join.ToParentBlockJoinSortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class StrangeErrorTest {
	public static void main(String[] args) throws IOException, ParseException {
		Analyzer analyzer = new StandardAnalyzer(new CharArraySet(Arrays.asList(";", ":", "-", "_", " "), true));
		Directory directory = new RAMDirectory();

		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setRAMBufferSizeMB(128);

		IndexWriter indexWriter = new IndexWriter(directory, config);

		SearcherManager searcherManager = new SearcherManager(indexWriter, new SearcherFactory());

		BookStore bs1 = new BookStore(1, "first");
		BookStoreInfo bookStoreInfo1 = new BookStoreInfo(30, "book 1");

		index(searcherManager, indexWriter, bs1, bookStoreInfo1);

		BookStore bs2 = new BookStore(2, "second");
		BookStoreInfo bookStoreInfo2 = new BookStoreInfo(20, "book 2");
		index(searcherManager, indexWriter, bs2, bookStoreInfo2);


		String searchString = "first";

		IndexSearcher indexSearcher = searcherManager.acquire();
		Builder storeQueryBuilder = new Builder();

		storeQueryBuilder.add(new TermQuery(new Term("docType", "store")), Occur.MUST);

		QueryParser queryParser = new QueryParser("name", analyzer);
		queryParser.setDefaultOperator(Operator.AND);
		storeQueryBuilder.add(queryParser.parse(QueryParser.escape(searchString)), Occur.MUST);


		Builder bookInfoQueryBuilder = new Builder();
		bookInfoQueryBuilder.add(new TermQuery(new Term("docType", "bookInfo")), Occur.MUST);

		Query parentQuery = storeQueryBuilder.build();
		Query childQuery =  bookInfoQueryBuilder.build();

		ToParentBlockJoinQuery bookStoreQuery = new ToParentBlockJoinQuery(childQuery, new QueryBitSetProducer(parentQuery), ScoreMode.None);

		Sort sort = new Sort(new ToParentBlockJoinSortField("sold", Type.LONG, true,
				new QueryBitSetProducer(parentQuery), new QueryBitSetProducer(childQuery)));

		TopDocs search = indexSearcher.search(bookStoreQuery, 100, sort, true, false);
		System.out.println("Search using query [" + bookStoreQuery + "] returned [" + search.scoreDocs.length + "] hits (total [" + search.totalHits + "])");
	}

	public static void index(SearcherManager searcherManager, IndexWriter indexWriter, BookStore bookStore, BookStoreInfo bookStoreInfo) throws IOException {

		IndexSearcher indexSearcher = searcherManager.acquire();

		try {
			List<Long> storeIds = new ArrayList<>();
			storeIds.add(bookStore.storeId);

			Query storeIdQuery = LongPoint.newSetQuery("storeId", storeIds);
			indexWriter.deleteDocuments(storeIdQuery);


			List<Document> documentList = new ArrayList<>();

			documentList.add(indexBookStoreInfo(bookStoreInfo));
			documentList.add(indexBookStore(bookStore));

			indexWriter.addDocuments(documentList);
			indexWriter.flush();
			indexWriter.commit();
			searcherManager.maybeRefresh();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			searcherManager.release(indexSearcher);
		}
	}

	private static Document indexBookStore(BookStore bookStore) {
		Document document = new Document();

		document.add(new LongPoint("storeId", bookStore.storeId));
		document.add(new Field("docType", "store", StringField.TYPE_NOT_STORED));
		document.add(new TextField("name", bookStore.storeName, Store.YES));

		return document;
	}

	private static Document indexBookStoreInfo(BookStoreInfo bookStoreInfo) {
		Document document = new Document();

		document.add(new NumericDocValuesField("sold", bookStoreInfo.booksSold));
		document.add(new Field("docType", "bookInfo", StringField.TYPE_NOT_STORED));
		document.add(new TextField("name", bookStoreInfo.bookName, Store.YES));

		return document;
	}

	public static class BookStore implements Serializable {
		public long storeId;
		public String storeName;

		public BookStore(long storeId, String storeName) {
			this.storeId = storeId;
			this.storeName = storeName;
		}

		public BookStore() {
		}
	}

	public static class BookStoreInfo implements Serializable {
		public long booksSold;
		public String bookName;

		public BookStoreInfo(long booksSold, String bookName) {
			this.booksSold = booksSold;
			this.bookName = bookName;
		}

		public BookStoreInfo() {
		}
	}
}
