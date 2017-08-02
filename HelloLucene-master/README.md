HelloLucene
===========
```
Run parentchild.StrangeErrorTest

and you will get

Exception in thread "main" java.lang.NullPointerException
	at org.apache.lucene.search.join.BlockJoinSelector.wrap(BlockJoinSelector.java:161)
	at org.apache.lucene.search.join.ToParentBlockJoinSortField$3.getNumericDocValues(ToParentBlockJoinSortField.java:168)
	at org.apache.lucene.search.FieldComparator$NumericComparator.doSetNextReader(FieldComparator.java:153)
	at org.apache.lucene.search.SimpleFieldComparator.getLeafComparator(SimpleFieldComparator.java:36)
	at org.apache.lucene.search.FieldValueHitQueue.getComparators(FieldValueHitQueue.java:180)
	at org.apache.lucene.search.TopFieldCollector$SimpleFieldCollector.getLeafCollector(TopFieldCollector.java:100)
	at org.apache.lucene.search.IndexSearcher.search(IndexSearcher.java:659)
	at org.apache.lucene.search.IndexSearcher.search(IndexSearcher.java:472)
	at org.apache.lucene.search.IndexSearcher.search(IndexSearcher.java:591)
	at org.apache.lucene.search.IndexSearcher.searchAfter(IndexSearcher.java:576)
	at org.apache.lucene.search.IndexSearcher.search(IndexSearcher.java:491)
	at parentchild.StrangeErrorTest.main(StrangeErrorTest.java:87)
```
